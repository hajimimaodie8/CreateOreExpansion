package com.hjmmd_8.createoreexpansion.foundation.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

public class BlockSearcher {
    public static Set<BlockPos> searchBlocks(Level level, BlockPos startPos,
                                      int maxBlocks, int searchRange, Predicate<BlockState> predicate) {
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

                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (result.contains(neighbor)) continue;

                        // 范围检查
                        if (Math.abs(neighbor.getX() - startPos.getX()) > searchRange) continue;
                        if (Math.abs(neighbor.getY() - startPos.getY()) > searchRange) continue;
                        if (Math.abs(neighbor.getZ() - startPos.getZ()) > searchRange) continue;

                        BlockState neighborState = level.getBlockState(neighbor);
                        if (predicate.test(neighborState)) {
                            result.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        // 如果超过最大方块数，返回空集合
        if (result.size() >= maxBlocks) {
            return new HashSet<>();
        }

        return result;
    }
}
