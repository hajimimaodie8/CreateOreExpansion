package com.hjmmd_8.createoreexpansion.foundation.util;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 方块 BFS 搜索工具 —— 技能系统统一入口。
 *
 * 替代原先 {@code BlockSearcher} 与 {@code TreeCounter} 两份重复的 BFS 实现：
 * <ul>
 *     <li>{@link #collect} —— 收集范围内所有匹配谓词的相邻方块；</li>
 *     <li>{@link #countTree} —— 统计相连树木的原木/树叶数量（砍伐技能用）。</li>
 * </ul>
 */
public final class BlockSearch {

    private BlockSearch() {
    }

    /**
     * 从起点 BFS 收集所有匹配 {@code predicate} 的相邻方块（含起点）。
     *
     * @param maxBlocks   方块数量上限
     * @param searchRange 曼哈顿搜索半径
     * @return 匹配方块集合；树超过上限时返回已收集的部分（砍满上限即停，剩余保留），
     *         不会因超限而整体放弃
     */
    public static Set<BlockPos> collect(Level level, BlockPos start, int maxBlocks, int searchRange,
                                        Predicate<BlockState> predicate) {
        return collect(start, maxBlocks, searchRange, predicate, level::getBlockState);
    }

    /**
     * 与 {@link #collect(Level, BlockPos, int, int, Predicate)} 同算法，但方块状态由调用方提供。
     *
     * <p>用于<b>物理结构（sub-level）</b>场景：结构上的方块在结构本地子世界里，
     * 主世界 {@code Level} 在那些坐标上读不到（会得到空气），必须由上层用
     * {@code ExcavationSkillContext#blockState(...)}（经 Sable 桥接）来读。
     * 旧签名保留不动，旧调用方行为零变化。</p>
     *
     * @param stateAt 坐标 → 方块状态（结构场景传 {@code context::blockState}）
     */
    public static Set<BlockPos> collect(BlockPos start, int maxBlocks, int searchRange,
                                        Predicate<BlockState> predicate,
                                        Function<BlockPos, BlockState> stateAt) {
        Set<BlockPos> result = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(start);
        result.add(start);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            BlockPos current = queue.poll();
            for (BlockPos neighbor : neighbors(current)) {
                if (result.contains(neighbor) || !withinRange(neighbor, start, searchRange)) continue;
                if (predicate.test(stateAt.apply(neighbor))) {
                    result.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return result;
    }

    /**
     * 统计起点所在树木的原木/树叶数量（起点本身不计数，与旧实现保持一致）。
     *
     * @param includeLeaves 是否统计树叶
     */
    public static TreeStats countTree(Level level, BlockPos start, int maxBlocks, int searchRange,
                                      boolean includeLeaves) {
        int logs = 0;
        int leaves = 0;
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && visited.size() < maxBlocks) {
            BlockPos current = queue.poll();
            for (BlockPos neighbor : neighbors(current)) {
                if (visited.contains(neighbor) || !withinRange(neighbor, start, searchRange)) continue;
                BlockState state = level.getBlockState(neighbor);
                if (state.is(BlockTags.LOGS)) {
                    logs++;
                    visited.add(neighbor);
                    queue.add(neighbor);
                } else if (includeLeaves && state.is(BlockTags.LEAVES)) {
                    leaves++;
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return new TreeStats(logs, leaves);
    }

    /** 返回 3x3x3 邻域（去掉自身）的 26 个邻居 */
    private static List<BlockPos> neighbors(BlockPos pos) {
        List<BlockPos> result = new ArrayList<>(26);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    result.add(pos.offset(dx, dy, dz));
                }
            }
        }
        return result;
    }

    /**
     * 判断方块是否在搜索范围内。
     *
     * 范围 {@code <= 0} 表示无限（不限制搜索半径，仅受 maxBlocks 上限约束），
     * 供「无视范围限制」的高级技能等级使用。
     */
    private static boolean withinRange(BlockPos pos, BlockPos start, int range) {
        if (range <= 0) return true; // 无限范围
        return Math.abs(pos.getX() - start.getX()) <= range
                && Math.abs(pos.getY() - start.getY()) <= range
                && Math.abs(pos.getZ() - start.getZ()) <= range;
    }

    /** 树的统计结果 */
    public record TreeStats(int logs, int leaves) {
        public int total() {
            return logs + leaves;
        }
    }
}
