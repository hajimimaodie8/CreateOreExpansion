package com.hjmmd_8.createoreexpansion.content.skill.attribute;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 树计数器 - 用于统计树木的原木和树叶数量
 *
 * <p>使用BFS算法遍历相连的原木和树叶，提供精确的计数统计。</p>
 */
public class TreeCounter {

    private final int searchRange;
    private final int maxBlocks;
    private final boolean includeLeaves;

    private int logsCount = 0;
    private int leavesCount = 0;

    /**
     * 创建树计数器
     * @param searchRange 搜索半径
     * @param maxBlocks 最大方块数限制
     * @param includeLeaves 是否统计树叶
     */
    public TreeCounter(int searchRange, int maxBlocks, boolean includeLeaves) {
        this.searchRange = searchRange;
        this.maxBlocks = maxBlocks;
        this.includeLeaves = includeLeaves;
    }

    /**
     * 统计指定位置树木的原木和树叶数量
     * @param level 世界
     * @param startPos 起始位置（通常是被点击的原木）
     * @return this 计数器实例
     */
    public TreeCounter count(Level level, BlockPos startPos) {
        logsCount = 0;
        leavesCount = 0;

        var visited = new java.util.HashSet<BlockPos>();
        var queue = new java.util.LinkedList<BlockPos>();
        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty() && visited.size() < maxBlocks) {
            BlockPos current = queue.poll();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (visited.contains(neighbor)) continue;

                        // 范围检查
                        if (Math.abs(neighbor.getX() - startPos.getX()) > searchRange) continue;
                        if (Math.abs(neighbor.getY() - startPos.getY()) > searchRange) continue;
                        if (Math.abs(neighbor.getZ() - startPos.getZ()) > searchRange) continue;

                        BlockState neighborState = level.getBlockState(neighbor);
                        if (neighborState.is(BlockTags.LOGS)) {
                            logsCount++;
                            visited.add(neighbor);
                            queue.add(neighbor);
                        } else if (includeLeaves && neighborState.is(BlockTags.LEAVES)) {
                            leavesCount++;
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        return this;
    }

    /**
     * 获取原木数量
     * @return 原木数量
     */
    public int logs() {
        return logsCount;
    }

    /**
     * 获取树叶数量
     * @return 树叶数量
     */
    public int leaves() {
        return leavesCount;
    }

    /**
     * 获取总方块数
     * @return 原木 + 树叶数量
     */
    public int total() {
        return logsCount + leavesCount;
    }

    /**
     * 判断是否超过限制
     * @return 如果超过最大方块数返回true
     */
    public boolean exceedsLimit() {
        return (logsCount + leavesCount) >= maxBlocks;
    }

    @Override
    public String toString() {
        return "TreeCounter{" +
                "logs=" + logsCount +
                ", leaves=" + leavesCount +
                ", total=" + total() +
                '}';
    }
}
