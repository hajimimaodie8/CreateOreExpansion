package com.hjmmd_8.createoreexpansion.client.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Set;

/**
 * 渲染策略接口 - 计算需要渲染的方块位置
 */
@FunctionalInterface
public interface RenderStrategy {
    /**
     * 计算需要渲染的方块位置
     * @param center 玩家点击的中心方块
     * @param hit 玩家看向的结果
     * @param player 玩家
     * @return 需要渲染的方块位置集合（不包括中心方块）
     */
    Set<BlockPos> calculatePositions(BlockPos center, BlockHitResult hit, Player player);
}
