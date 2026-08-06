package com.hjmmd_8.createoreexpansion.client.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * 树砍伐渲染策略 - 用于斧子的连锁砍树
 */
public class TreeChoppingRenderStrategy implements RenderStrategy {
    private final int searchRange;
    private final boolean includeLeaves;
    private final int renderLimit;

    public TreeChoppingRenderStrategy(int searchRange, boolean includeLeaves, int renderLimit) {
        this.searchRange = searchRange;
        this.includeLeaves = includeLeaves;
        this.renderLimit = renderLimit;
    }

    @Override
    public Set<BlockPos> calculatePositions(BlockPos center, BlockHitResult hit, Player player) {
        Level level = player.level();
        Set<BlockPos> treeBlocks = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(center);
        treeBlocks.add(center);

        while (!queue.isEmpty() && treeBlocks.size() < renderLimit) {
            BlockPos current = queue.poll();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (treeBlocks.contains(neighbor)) continue;

                        // 范围检查
                        if (Math.abs(neighbor.getX() - center.getX()) > searchRange) continue;
                        if (Math.abs(neighbor.getY() - center.getY()) > searchRange) continue;
                        if (Math.abs(neighbor.getZ() - center.getZ()) > searchRange) continue;

                        BlockState neighborState = level.getBlockState(neighbor);
                        boolean isLog = neighborState.is(BlockTags.LOGS);
                        boolean isLeaves = includeLeaves && neighborState.is(BlockTags.LEAVES);

                        if (isLog || isLeaves) {
                            treeBlocks.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        treeBlocks.remove(center); // 不渲染中心方块
        return treeBlocks;
    }
}
