package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.content.skill.config.AreaAoeConfig;
import com.hjmmd_8.createoreexpansion.content.skill.strategy.AreaAoeStrategy;
import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.hjmmd_8.createoreexpansion.foundation.ParamsPool;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.TypedItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.impl.ExcavationSkillContext;
import com.hjmmd_8.createoreexpansion.foundation.util.BlockBreaker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
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
 * 范围AOE技能 - 强类型依赖AreaAoeStrategy
 *
 * <p>此技能通过 {@link AreaAoeStrategy} 计算需要破坏的方块位置，
 * 与渲染系统共享相同的策略实现，确保渲染预览与实际破坏一致。</p>
 *
 * <p>通过继承 {@link AbstractStrategySkill} 确保类型安全，
 * 防止将错误的Strategy类型传入。</p>
 */
public class AreaAoeSkill extends AbstractStrategySkill<BlockPos, AreaAoeStrategy, AreaAoeConfig> implements TypedItemSkill<ExcavationSkillContext> {

    private int energyCost;
    private TagKey<Block> mineableTag;

    /**
     * 创建范围AOE技能
     * @param strategy 区域策略（必须是AreaAoeStrategy类型）
     */
    public AreaAoeSkill(AreaAoeStrategy strategy) {
        super(strategy);
        this.energyCost = 0;
        this.mineableTag = BlockTags.MINEABLE_WITH_PICKAXE;
    }

    protected void causeAoe(Level level, BlockPos pos, BlockState state,
                         ItemStack pickaxe, LivingEntity livingEntity, DataSkill data) {
        if (!(livingEntity instanceof ServerPlayer player)) return;
        if (level.isClientSide) return;

        // 生存模式跳过硬度为 0 的方块；创造模式允许破坏任何方块
        if (!player.isCreative() && state.getDestroySpeed(level, pos) == 0.0F) return;
        if (!AllKeys.SKILL_RELEASE.isPressed()) return;

        HitResult pick = player.pick(20D, 0.0F, false);
        if (!(pick instanceof BlockHitResult hit)) return;

        IParams params = ParamsPool.DEFAULT_POOL.borrow()
                .put("Center", pos)
                .put("CenterState", state)
                .put("BlockHitResult", hit)
                .put("Player", player);

        // 使用Strategy计算位置
        Set<BlockPos> positions = calculate(data, params);

        ParamsPool.DEFAULT_POOL.returnParams(params);

        if (positions.isEmpty()) return;

        // 检查能量
        if (energyCost != 0 && ToolEnergy.hasEnergy(pickaxe) && !ToolEnergy.consumeForSkill(pickaxe, this))
            return;

        // 破坏方块
        BlockBreaker.breakPositions(positions, pos, pickaxe, level, player, mineableTag);
    }

    @Override
    public void releaseTyped(ExcavationSkillContext ctx, DataSkill data) {
        causeAoe(ctx.level(), ctx.pos(), ctx.level().getBlockState(ctx.pos()), ctx.tool(), ctx.entity(), data);
    }

    @Override
    public SkillType getType() {
        return SkillType.EXCAVATION_SKILL;
    }

    @Override
    public int getCost() {
        return energyCost;
    }

    public TagKey<Block> getMineableTag() {
        return mineableTag;
    }

    @Override
    public void load(AreaAoeConfig config) {
        this.energyCost = config.energyCost;
        this.mineableTag = config.mineableTag;
    }
}
