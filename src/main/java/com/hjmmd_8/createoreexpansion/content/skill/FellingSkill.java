package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.common.AllModifiableAttributes;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolSkillCooldown;
import com.hjmmd_8.createoreexpansion.content.skill.attribute.BreakBlockSpeedModifiableAttribute;
import com.hjmmd_8.createoreexpansion.content.skill.attribute.TreeCounter;
import com.hjmmd_8.createoreexpansion.content.skill.config.FellingConfig;
import com.hjmmd_8.createoreexpansion.content.skill.strategy.FellingStrategy;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Set;

/**
 * 砍伐技能 - 强类型依赖FellingStrategy
 *
 * <p>此技能通过 {@link FellingStrategy} 计算需要破坏的树木方块，
 * 与渲染系统共享相同的策略实现。</p>
 *
 * <p>通过继承 {@link AbstractStrategySkill} 确保类型安全。</p>
 */
public class FellingSkill extends AbstractStrategySkill<BlockPos, FellingStrategy, FellingConfig> implements TypedItemSkill<ExcavationSkillContext> {

    private int energyCost;
    private float logResistance;
    private float leafResistance;
    private FellingConfig.BlockPredicate predicate;

    /**
     * 创建砍伐技能
     * @param strategy 砍伐策略（必须是FellingStrategy类型）
     */
    public FellingSkill(FellingStrategy strategy) {
        super(strategy);
    }

    public void causeAoe(Level level, BlockPos pos, BlockState state,
                                ItemStack axe, LivingEntity livingEntity, DataSkill data) {
        if (!(livingEntity instanceof ServerPlayer player)) return;
        if (level.isClientSide) return;

        if (!player.isCreative() && state.getDestroySpeed(level, pos) == 0.0F) return;
        if (!AllKeys.SKILL_RELEASE.isPressed()) return;
        if (!state.is(BlockTags.LOGS)) return;
        if (!ToolSkillCooldown.isReady(player, axe)) return;

        HitResult pick = player.pick(20D, 0.0F, false);
        if (!(pick instanceof BlockHitResult hit)) return;

        IParams params = ParamsPool.DEFAULT_POOL.borrow()
                .put("Center", pos)
                .put("CenterState", state)
                .put("BlockHitResult", hit)
                .put("Player", player);

        // 使用Strategy计算位置
        Set<BlockPos> toDestroy = calculate(data, params);
        ParamsPool.DEFAULT_POOL.returnParams(params);
        if (toDestroy.isEmpty()) return;

        // 检查能量
        if (energyCost != 0 && ToolEnergy.hasEnergy(axe) && !ToolEnergy.consumeForSkill(axe, this))
            return;

        BlockBreaker.breakPositions(toDestroy, pos, axe, level, player);
        ToolSkillCooldown.start(player, axe, 3);
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

    @Override
    public void load(FellingConfig config) {
        this.energyCost = config.energyCost;
        this.logResistance = config.logResistance;
        this.leafResistance = config.leafResistance;
        this.predicate = config.predicate;

        addModifier(AllModifiableAttributes.BREAK_BLOCK_SPEED, attribute -> {
            if (!(attribute instanceof BreakBlockSpeedModifiableAttribute speedAttribute)) return;

            Level level = speedAttribute.getLevel();
            BlockPos pos = speedAttribute.getPos();

            // 使用FellingStrategy创建TreeCounter进行统计
            TreeCounter tree = strategy().createCounter();
            tree.count(level, pos);

            int treeSize = tree.total();
            if (treeSize == 0) return;
            float multiplier = Math.max(.1f, 1f / (1f + tree.logs() * logResistance +
                    ((predicate == FellingConfig.BlockPredicate.IS_TREE)
                            ? tree.leaves() * leafResistance
                            : 0)));
            speedAttribute.setValue(speedAttribute.getValue() * multiplier);
        });
    }
}
