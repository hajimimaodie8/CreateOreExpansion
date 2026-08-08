package com.hjmmd_8.createoreexpansion.foundation.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * AOE 方块破坏器 — 所有 AOE 技能共用。
 *
 * <p>统一依赖 {@link DualDirection} 计算范围，单个方法覆盖所有场景。</p>
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class BlockBreaker {

    /**
     * 基于 DualDirection 破坏方块
     *
     * @param dd        双方向（从玩家点击解析）
     * @param center    中心方块（会被跳过）
     * @param tool      手持工具
     * @param level     世界
     * @param entity    挖掘者
     * @param width     横向宽度
     * @param height    纵向高度
     * @param depth     挖掘深度
     */
    public static void breakBlocks(DualDirection dd, BlockPos center, ItemStack tool,
                                   Level level, LivingEntity entity,
                                   int width, int height, int depth) {
        breakBlocks(dd, center, tool, level, entity, width, height, depth, null);
    }

    /**
     * 基于 DualDirection 破坏方块（带 mineableTag 过滤）
     */
    public static void breakBlocks(DualDirection dd, BlockPos center, ItemStack tool,
                                   Level level, LivingEntity entity,
                                   int width, int height, int depth,
                                   TagKey<Block> mineableTag) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!player.isCreative() && tool.getDamageValue() >= tool.getMaxDamage() - 1) return;

        Set<BlockPos> positions = dd.collect(center, width, height, depth);
        int damage = 0;

        for (BlockPos pos : positions) {
            if (!player.isCreative() && tool.getDamageValue() + damage >= tool.getMaxDamage() - 1) break;

            BlockState state = level.getBlockState(pos);
            if (!canBreak(state, level, pos, player, mineableTag)) continue;

            breakSingle(state, pos, level, player, tool);
            damage++;
        }

        applyDurability(tool, entity, damage, player.isCreative());
    }

    /**
     * 破坏预先计算的位置集合（带 mineableTag 过滤）
     */
    public static void breakPositions(Set<BlockPos> positions, BlockPos center,
                                      ItemStack tool, Level level, LivingEntity entity,
                                      @Nullable TagKey<Block> mineableTag) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!player.isCreative() && tool.getDamageValue() >= tool.getMaxDamage() - 1) return;

        int damage = 0;

        for (BlockPos pos : positions) {
            if (pos.equals(center)) continue;
            if (!player.isCreative() && tool.getDamageValue() + damage >= tool.getMaxDamage() - 1) break;

            BlockState state = level.getBlockState(pos);
            if (!canBreak(state, level, pos, player, mineableTag)) continue;

            breakSingle(state, pos, level, player, tool);
            damage++;
        }

        applyDurability(tool, entity, damage, player.isCreative());
    }

    /**
     * 破坏预先计算的位置集合
     */
    public static void breakPositions(Set<BlockPos> positions, BlockPos center,
                                      ItemStack tool, Level level, LivingEntity entity) {
        breakPositions(positions, center, tool, level, entity, null);
    }

    // ========== 内部逻辑 ==========

    @SuppressWarnings("deprecation")
    private static boolean canBreak(BlockState state, Level level, BlockPos pos,
                                    ServerPlayer player, @Nullable TagKey<Block> mineableTag) {
        if (state.isAir()) return false;
        if (!state.getFluidState().isEmpty()) return false;
        if (level.getBlockEntity(pos) != null) return false;
        if (mineableTag != null && !state.is(mineableTag)) return false;
        if (!player.isCreative() && state.getDestroySpeed(level, pos) <= 0) return false;
        if (state.requiresCorrectToolForDrops() && !player.hasCorrectToolForDrops(state)) return false;
        return true;
    }

    @SuppressWarnings("deprecation")
    private static void breakSingle(BlockState state, BlockPos pos, Level level,
                                    ServerPlayer player, ItemStack tool) {
        if (!player.isCreative() && player.hasCorrectToolForDrops(state)) {
            spawnDrops(state, pos, level, player, tool);
        }

        state.getBlock().destroy(level, pos, state);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
        player.causeFoodExhaustion(0.005F);
    }

    private static void spawnDrops(BlockState state, BlockPos pos, Level level,
                                   ServerPlayer player, ItemStack tool) {
        ServerLevel serverLevel = (ServerLevel) level;
        state.spawnAfterBreak(serverLevel, pos, tool, true);

        List<ItemStack> drops = Block.getDrops(state, serverLevel, pos,
                level.getBlockEntity(pos), player, tool);
        for (ItemStack drop : drops) {
            ItemEntity entity = new ItemEntity(level,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
            level.addFreshEntity(entity);
        }

        if (level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            int xp = state.getBlock().getExpDrop(state, serverLevel, pos,
                    level.getBlockEntity(pos), player, tool);
            if (xp > 0) {
                ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), xp);
            }
        }
    }

    private static void applyDurability(ItemStack tool, LivingEntity entity,
                                        int brokenCount, boolean creative) {
        if (brokenCount > 0 && !creative) {
            int cost = (int) Math.ceil(brokenCount / 4.0) + 1;
            tool.hurtAndBreak(cost, entity,
                    net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
    }
}