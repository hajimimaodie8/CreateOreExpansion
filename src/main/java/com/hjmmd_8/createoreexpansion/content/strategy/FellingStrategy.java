package com.hjmmd_8.createoreexpansion.content.strategy;

import com.hjmmd_8.createoreexpansion.content.skill.attribute.TreeCounter;
import com.hjmmd_8.createoreexpansion.foundation.util.AreaStrategy;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 砍伐策略 - 用于斧头的连锁砍树
 *
 * <p>使用BFS算法搜索相连的原木和树叶，支持：
 * <ul>
 *   <li>仅原木 - {@link #IS_LOG}</li>
 *   <li>原木+树叶 - {@link #IS_TREE}</li>
 * </ul>
 */
public class FellingStrategy implements AreaStrategy {

    private final int searchRange;
    private final int maxBlocks;
    private final int renderLimit;
    private final Predicate<BlockState> predicate;

    public static final Predicate<BlockState> IS_LOG =
        state -> state.is(BlockTags.LOGS);
    public static final Predicate<BlockState> IS_TREE =
        state -> state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);

    /**
     * 创建砍伐策略
     * @param searchRange 搜索半径（8 = 16x16x16 区域）
     * @param maxBlocks 最大方块数限制
     * @param renderLimit 渲染限制（防止渲染过多方块）
     * @param predicate 方块匹配条件
     */
    public FellingStrategy(int searchRange, int maxBlocks, int renderLimit, Predicate<BlockState> predicate) {
        this.searchRange = searchRange;
        this.maxBlocks = maxBlocks;
        this.renderLimit = renderLimit;
        this.predicate = predicate;
    }

    /**
     * 简化构造器 - 自动计算最大方块数
     */
    public FellingStrategy(int searchRange, int renderLimit, Predicate<BlockState> predicate) {
        this(searchRange, searchRange * searchRange * searchRange + 1, renderLimit, predicate);
    }

    /**
     * 默认构造器 - 使用默认限制
     */
    public FellingStrategy(int searchRange, Predicate<BlockState> predicate) {
        this(searchRange, 200, 100, predicate);
    }

    @Override
    public Set<BlockPos> calculatePositions(BlockPos center, BlockHitResult hit, Player player) {
        Level level = player.level();
        return calculateTreeBlocks(level, center);
    }

    /**
     * BFS搜索相连的方块
     */
    private Set<BlockPos> calculateTreeBlocks(Level level, BlockPos startPos) {
        Set<BlockPos> result = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        result.add(startPos);

        while (!queue.isEmpty() && result.size() < renderLimit) {
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

        // 如果超过最大方块数，返回空集合（技能会判断不执行）
        if (result.size() >= maxBlocks) {
            return new HashSet<>();
        }

        result.remove(startPos); // 不包含中心方块
        return result;
    }

    /**
     * 创建树计数器 - 用于统计和属性修饰
     * @return TreeCounter实例
     */
    public TreeCounter createCounter() {
        return new TreeCounter(searchRange, maxBlocks, predicate == IS_TREE);
    }

    @Override
    public boolean shouldRender(ClientLevel world, BlockPos pos, BlockState state, Player player) {
        return IS_LOG.test(state);
    }
}
