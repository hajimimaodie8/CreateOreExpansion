package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.common.AllModifiableAttributes;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolSkillCooldown;
import com.hjmmd_8.createoreexpansion.content.skill.attribute.BreakBlockSpeedModifiableAttribute;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.AbstractSkill;
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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class FellingSkill extends AbstractSkill implements TypedItemSkill<ExcavationSkillContext> {

    private final int searchRange;
    private final int maxBlocks;
    private final int energyCost;

    private final Predicate<BlockState> predicate;

    public static final Predicate<BlockState> IS_LOG = (state) -> state.is(BlockTags.LOGS);
    public static final Predicate<BlockState> IS_TREE = (state) -> state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);

    public FellingSkill(int searchRange, int maxBlocks, int energyCost, Predicate<BlockState> predicate) {
        super();
        this.searchRange = searchRange;
        this.maxBlocks = maxBlocks;
        this.energyCost = energyCost;
        this.predicate = predicate;
    }

    public FellingSkill(int searchRange, int energyCost, Predicate<BlockState> predicate) {
        this(searchRange, searchRange * searchRange * searchRange + 1, energyCost, predicate);
    }

    public FellingSkill(int searchRange, Predicate<BlockState> predicate) {
        this(searchRange, 100, predicate);
    }

    public FellingSkill breakBlockSpeedCorrection(Function<Integer, Float> speedCorrection) {
        return (FellingSkill) addModifier(AllModifiableAttributes.BREAK_BLOCK_SPEED, attribute -> {
            if (!(attribute instanceof BreakBlockSpeedModifiableAttribute speedAttribute)) return;
            Level level = speedAttribute.getLevel();
            BlockPos pos = speedAttribute.getPos();

            Set<BlockPos> treeBlocks = calculateTreeLogs(level, pos);
            int treeSize = treeBlocks.size();
            if (treeSize == 0) return;
            float multiplier = speedCorrection.apply(treeSize);
            speedAttribute.setValue(speedAttribute.getValue() * multiplier);
        });
    }

    public Set<BlockPos> calculateTreeBlocks(Level level, BlockPos startPos) {
        Set<BlockPos> result = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        result.add(startPos);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            BlockPos current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos nb = current.offset(dx, dy, dz);
                        if (Math.abs(nb.getX() - startPos.getX()) > searchRange) continue;
                        if (Math.abs(nb.getY() - startPos.getY()) > searchRange) continue;
                        if (Math.abs(nb.getZ() - startPos.getZ()) > searchRange) continue;
                        if (result.contains(nb)) continue;
                        BlockState ns = level.getBlockState(nb);
                        if (predicate.test(ns)) {
                            result.add(nb);
                            queue.add(nb);
                        }
                    }
                }
            }
        }

        if (result.size() >= maxBlocks) return new HashSet<>();
        return result;
    }

    public Set<BlockPos> calculateTreeLogs(Level level, BlockPos startPos) {
        Set<BlockPos> result = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        result.add(startPos);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            BlockPos current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos nb = current.offset(dx, dy, dz);
                        if (Math.abs(nb.getX() - startPos.getX()) > searchRange) continue;
                        if (Math.abs(nb.getY() - startPos.getY()) > searchRange) continue;
                        if (Math.abs(nb.getZ() - startPos.getZ()) > searchRange) continue;
                        if (result.contains(nb)) continue;
                        BlockState ns = level.getBlockState(nb);
                        if (IS_LOG.test(ns)) {
                            result.add(nb);
                            queue.add(nb);
                        }
                    }
                }
            }
        }

        if (result.size() >= maxBlocks) return new HashSet<>();
        return result;
    }

    public void causeAoe(Level level, BlockPos pos, BlockState state,
                                ItemStack axe, LivingEntity livingEntity) {
        if (!(livingEntity instanceof ServerPlayer player)) return;
        if (level.isClientSide) return;
        if (!player.isCreative() && state.getDestroySpeed(level, pos) == 0.0F) return;
        if (!AllKeys.SKILL_RELEASE.isPressed()) return;
        if (!state.is(BlockTags.LOGS)) return;
        if (!ToolSkillCooldown.isReady(player, axe)) return;

        if (energyCost != 0 && ToolEnergy.hasEnergy(axe) && !ToolEnergy.consumeForSkill(player, axe, this)) return;

        Set<BlockPos> toDestroy = calculateTreeBlocks(level, pos);
        if (toDestroy.size() > 1) {
            BlockBreaker.breakPositions(toDestroy, pos, axe, level, player);
            ToolSkillCooldown.start(player, axe, 3);
        }
    }

    @Override
    public void releaseTyped(ExcavationSkillContext ctx) {
        causeAoe(ctx.level(), ctx.pos(), ctx.level().getBlockState(ctx.pos()), ctx.tool(), ctx.entity());
    }

    @Override
    @Deprecated
    public void release(Object context) {
        // 通过 TypedItemSkill 实现，此方法保留用于向后兼容
        TypedItemSkill.super.release(context);
    }

    @Override
    public SkillType getType() {
        return SkillType.EXCAVATION_SKILL;
    }

    @Override
    public int getCost() {
        return energyCost;
    }
}
