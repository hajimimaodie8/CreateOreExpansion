package com.hjmmd_8.createoreexpansion.content.skill.strategy;

import com.hjmmd_8.createoreexpansion.content.skill.attribute.TreeCounter;
import com.hjmmd_8.createoreexpansion.content.skill.config.FellingConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.ConfigStrategy;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
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
 * <p>使用BFS算法搜索相连的原木和树叶
 */
public class FellingStrategy extends ConfigStrategy<FellingConfig> {

    private int searchRange;
    private int maxBlocks;
    private Predicate<BlockState> predicate;

    @Override
    public Set<BlockPos> calculatePositions(DataSkill skill, BlockPos center, BlockHitResult hit, Player player) {
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
        return new TreeCounter(searchRange, maxBlocks, predicate == FellingConfig.BlockPredicate.IS_TREE);
    }

    @Override
    public boolean shouldRender(DataSkill skill, ClientLevel world, BlockPos pos, BlockState state, Player player) {
        return FellingConfig.BlockPredicate.IS_LOG.test(state);
    }

    @Override
    public void load(FellingConfig config) {
        this.searchRange = config.searchRange;
        this.maxBlocks = config.maxBlocks;
        this.predicate = config.predicate;
    }
}
