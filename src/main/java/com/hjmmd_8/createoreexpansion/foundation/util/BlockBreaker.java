package com.hjmmd_8.createoreexpansion.foundation.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AOE 方块破坏器 — 所有 AOE 技能共用的方块遍历/破坏/掉落/耐久逻辑。
 *
 * <p>新建工具时<b>无需修改此文件</b>。</p>
 *
 * <p>提供两个入口方法：</p>
 * <ul>
 *   <li>{@link #breakBlocks(BlockHitResult, BlockPos, ItemStack, Level, LivingEntity, int, int)}
 *   — 基于 BoundingBox 遍历破坏（供镐子 3x3 等范围技能使用，自动跳过玩家点击的方块）</li>
 *   <li>{@link #breakPositions(Set, BlockPos, ItemStack, Level, LivingEntity)}
 *   — 基于预计算的位置集合破坏（供铲子一列、斧头连锁树等使用）</li>
 * </ul>
 *
 * <p>通用规则：<br>
 * - 生存模式跳过硬度 <= 0 的方块（基岩）；创造模式允许破坏任何方块<br>
 * - 跳过需要更高等级工具的方块（强化深板岩等）<br>
 * - 耐久消耗 = ceil(破坏总数 / 4) + 1<br>
 * - 创造模式不扣耐久、不掉落物</p>
 */
public class BlockBreaker {
    /** 向后兼容版本（不传 mineableTag） */
    public static void breakBlocks(BlockHitResult pick, BlockPos blockPos, ItemStack tool,
                                   Level level, LivingEntity livingEntity, int radius, int depth) {
        breakBlocks(pick, blockPos, tool, level, livingEntity, radius, depth, null);
    }


    /**
     * 基于 BoundingBox 遍历破坏方块（供镐子 3x3 使用）。
     *
     * @param pick         光线追踪结果（提供被击中的面方向）
     * @param blockPos     玩家点击的方块位置（会跳过自身）
     * @param tool         当前手持工具
     * @param level        世界
     * @param livingEntity 挖掘者
     * @param radius       AOE 半径
     * @param depth        AOE 纵深
     */
    public static void breakBlocks(BlockHitResult pick, BlockPos blockPos, ItemStack tool,
                                   Level level, LivingEntity livingEntity, int radius, int depth,
                                   TagKey<Block> mineableTag) {
        if (!(livingEntity instanceof ServerPlayer player)) return;

        Direction direction = pick.getDirection();
        BoundingBox boundingBox = AreaUtil.getAreaOfEffect(blockPos, direction, radius, depth);

        if (!player.isCreative() && (tool.getDamageValue() >= tool.getMaxDamage() - 1)) return;

        int damage = 0;
        Iterator<BlockPos> iterator = BlockPos.betweenClosedStream(boundingBox).iterator();
        Set<BlockPos> removedPos = new HashSet<>();

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (pick.getBlockPos().equals(pos)) continue;

            boolean isBroken = (tool.getDamageValue() + (damage + 1)) >= tool.getMaxDamage() - 1;
            if (!player.isCreative() && isBroken) break;

            BlockState targetState = level.getBlockState(pos);
                        if (removedPos.contains(pos) || !AreaUtil.canDestroy(targetState, level, pos)) continue;
            if (mineableTag != null && !targetState.is(mineableTag)) continue;
            if (!player.isCreative() && targetState.getDestroySpeed(level, pos) <= 0) continue;
            if (targetState.requiresCorrectToolForDrops() && !player.hasCorrectToolForDrops(targetState)) continue;

            if (!player.isCreative()) {
                boolean correctToolForDrops = player.hasCorrectToolForDrops(targetState);
                if (correctToolForDrops) {
                    targetState.spawnAfterBreak((ServerLevel) level, pos, tool, true);
                    List<ItemStack> drops = Block.getDrops(targetState, (ServerLevel) level, pos,
                            level.getBlockEntity(pos), livingEntity, tool);
                    List<ItemEntity> dropEntities = drops.stream()
                            .map(e -> new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), e))
                            .collect(Collectors.toList());
                    for (ItemEntity entity : dropEntities) level.addFreshEntity(entity);

                    if (level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
                        int xp = targetState.getBlock().getExpDrop(targetState, (ServerLevel) level, pos,
                                level.getBlockEntity(pos), livingEntity, tool);
                        if (xp > 0) ExperienceOrb.award((ServerLevel) level, Vec3.atCenterOf(pos), xp);
                    }
                }
            }

            removedPos.add(pos);
            targetState.getBlock().destroy(level, pos, targetState);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.gameEvent(GameEvent.BLOCK_DESTROY, blockPos, GameEvent.Context.of(livingEntity, targetState));
            player.awardStat(Stats.BLOCK_MINED.get(targetState.getBlock()));
            player.causeFoodExhaustion(0.005F);
            damage++;
        }

        if (damage != 0 && !player.isCreative()) {
            int durabilityCost = (int) Math.ceil(damage / 4.0) + 1;
            tool.hurtAndBreak(durabilityCost, livingEntity, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
    }

    /**
     * 基于预计算的位置集合破坏方块（供铲子一列/斧头连锁使用）。
     *
     * @param positions   要破坏的方块位置集合（需预先计算好）
     * @param clickedPos  玩家点击的方块位置（collection 中会跳过此位置）
     * @param tool        当前手持工具
     * @param level       世界
     * @param livingEntity 挖掘者
     */
    public static void breakPositions(Set<BlockPos> positions, BlockPos clickedPos,
                                      ItemStack tool, Level level, LivingEntity livingEntity) {
        if (!(livingEntity instanceof ServerPlayer player)) return;

        int damage = 0;
        for (BlockPos pos : positions) {
            if (pos.equals(clickedPos)) continue;

            BlockState targetState = level.getBlockState(pos);
            if (!AreaUtil.canDestroy(targetState, level, pos)) continue;
            if (!player.isCreative() && targetState.getDestroySpeed(level, pos) <= 0) continue;
            if (targetState.requiresCorrectToolForDrops() && !player.hasCorrectToolForDrops(targetState)) continue;

            if (!player.isCreative()) {
                if (player.hasCorrectToolForDrops(targetState)) {
                    targetState.spawnAfterBreak((ServerLevel) level, pos, tool, true);
                    List<ItemStack> drops = Block.getDrops(targetState, (ServerLevel) level, pos,
                            level.getBlockEntity(pos), livingEntity, tool);
                    List<ItemEntity> dropEntities = drops.stream()
                            .map(e -> new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), e))
                            .collect(Collectors.toList());
                    for (ItemEntity entity : dropEntities) level.addFreshEntity(entity);

                    if (level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
                        int xp = targetState.getBlock().getExpDrop(targetState, (ServerLevel) level, pos,
                                level.getBlockEntity(pos), livingEntity, tool);
                        if (xp > 0) ExperienceOrb.award((ServerLevel) level, Vec3.atCenterOf(pos), xp);
                    }
                }
            }

            targetState.getBlock().destroy(level, pos, targetState);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.gameEvent(GameEvent.BLOCK_DESTROY, clickedPos, GameEvent.Context.of(livingEntity, targetState));
            player.awardStat(Stats.BLOCK_MINED.get(targetState.getBlock()));
            player.causeFoodExhaustion(0.005F);
            damage++;

            if (!player.isCreative() && tool.getDamageValue() >= tool.getMaxDamage() - 1) break;
        }

        if (damage > 0 && !player.isCreative()) {
            int durabilityCost = (int) Math.ceil(damage / 4.0) + 1;
            tool.hurtAndBreak(durabilityCost, livingEntity, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
    }
}
