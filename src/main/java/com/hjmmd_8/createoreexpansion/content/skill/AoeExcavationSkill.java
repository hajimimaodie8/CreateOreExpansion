package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolSkillCooldown;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ConfigSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.ExcavationSkillContext;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.SkillStrategy;
import com.hjmmd_8.createoreexpansion.foundation.util.BlockBreaker;
import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.hjmmd_8.createoreexpansion.foundation.ParamsPool;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Set;

/**
 * 挖掘类 AOE 技能抽象基类。
 *
 * 统一了「破坏方块类」技能（砍伐 / 范围挖掘）的完整释放流水线：
 * 服务器端检查 → 按键检查 → 射线拾取 → 策略计算 → 能量消耗 → 方块破坏。
 * 差异点通过钩子方法定制：
 * <ul>
 *     <li>{@link #isTargetBlock} - 中心方块是否为目标（砍伐限定原木，范围挖掘不限定）</li>
 *     <li>{@link #getMineableTag} - 可破坏方块 tag（null 表示不限制）</li>
 *     <li>{@link #getCooldownSeconds} - 释放后冷却秒数（0 表示无冷却）</li>
 * </ul>
 *
 * @param <C> 配置类型
 * @param <S> 策略类型
 */
public abstract class AoeExcavationSkill<C extends SkillConfig, S extends SkillStrategy<BlockPos>>
        extends AbstractStrategySkill<BlockPos, S> implements ConfigSkill<ExcavationSkillContext, C> {

    /** 射线拾取距离 */
    private static final double PICK_DISTANCE = 20.0D;

    protected DataSkill data;

    protected AoeExcavationSkill(S strategy) {
        super(strategy);
    }

    @Override
    public void release(ExcavationSkillContext ctx) {
        causeAoe(ctx.level(), ctx.pos(), ctx.level().getBlockState(ctx.pos()), ctx.tool(), ctx.entity());
    }

    /** 统一释放流水线 */
    protected void causeAoe(Level level, BlockPos pos, BlockState state,
                            ItemStack tool, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (level.isClientSide) return;

        // 生存模式跳过硬度为 0 的方块；创造模式允许破坏任何方块
        if (!player.isCreative() && state.getDestroySpeed(level, pos) == 0.0F) return;
        if (!isTargetBlock(state)) return;
        // 任一技能键（键一/键二/键三）按下即允许释放（槽位选择由触发端 Mixin 完成）
        if (!AllKeys.SKILL_RELEASE.isPressed()
                && !AllKeys.SKILL_RELEASE_2.isPressed()
                && !AllKeys.SKILL_RELEASE_3.isPressed()) return;
        // 创造模式无冷却（其余模式按技能配置冷却）
        if (!player.isCreative() && getCooldownSeconds() > 0 && !ToolSkillCooldown.isReady(player, tool)) return;

        HitResult pick = player.pick(PICK_DISTANCE, 0.0F, false);
        if (!(pick instanceof BlockHitResult hit)) return;

        IParams params = ParamsPool.DEFAULT_POOL.borrow()
                .put("Center", pos)
                .put("CenterState", state)
                .put("BlockHitResult", hit)
                .put("Player", player);
        try {
            Set<BlockPos> toDestroy = strategy().calculate(data, params);
            if (toDestroy.isEmpty()) return;

            // 真正生效前消耗能量
            if (!ToolEnergy.tryConsume(player, tool, this)) return;

            BlockBreaker.breakPositions(toDestroy, pos, tool, level, player, getMineableTag());
            // 创造模式无冷却
            if (!player.isCreative() && getCooldownSeconds() > 0) {
                ToolSkillCooldown.start(player, tool, getCooldownSeconds());
            }
        } finally {
            ParamsPool.DEFAULT_POOL.returnParams(params);
        }
    }

    /** 中心方块是否为目标方块（默认全部接受，子类可限定） */
    protected boolean isTargetBlock(BlockState state) {
        return true;
    }

    /** 可破坏方块的 tag，null 表示不限制 */
    protected TagKey<Block> getMineableTag() {
        return null;
    }

    /** 释放后冷却秒数，0 表示无冷却 */
    @Override
    public int getCooldownSeconds() {
        return 0;
    }
}
