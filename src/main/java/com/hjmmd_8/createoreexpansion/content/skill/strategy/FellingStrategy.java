package com.hjmmd_8.createoreexpansion.content.skill.strategy;

import com.hjmmd_8.createoreexpansion.content.skill.attribute.TreeCounter;
import com.hjmmd_8.createoreexpansion.content.skill.config.FellingConfig;
import com.hjmmd_8.createoreexpansion.foundation.util.params.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.AreaStrategy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.ConfigStrategy;
import com.hjmmd_8.createoreexpansion.foundation.util.BlockSearcher;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;
import java.util.function.Predicate;

/**
 * 砍伐策略 - 用于斧头的连锁砍树
 *
 * <p>使用BFS算法搜索相连的原木和树叶
 */
public class FellingStrategy extends ConfigStrategy<BlockPos, FellingConfig> implements AreaStrategy {

    private int searchRange;
    private int maxBlocks;
    private Predicate<BlockState> predicate;

    @Override
    public Set<BlockPos> calculate(IParams params) {
        Player player = params.get("Player", Player.class);
        BlockPos center = params.get("Center", BlockPos.class);
        Level level = player.level();
        return calculateTreeBlocks(level, center);
    }

    /**
     * BFS搜索相连的方块
     */
    private Set<BlockPos> calculateTreeBlocks(Level level, BlockPos startPos) {
        Set<BlockPos> result = BlockSearcher.searchBlocks(
                level, startPos, maxBlocks, searchRange, predicate);
        result.remove(startPos);
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
    public boolean shouldRender(ClientLevel world, IParams params) {
        BlockState state = params.get("CenterState", BlockState.class);
        return FellingConfig.BlockPredicate.IS_LOG.test(state);
    }

    @Override
    public void load(FellingConfig config, DataSkill data) {
        this.searchRange = config.searchRange;
        this.maxBlocks = config.maxBlocks;
        this.predicate = config.predicate;
    }

    @Override
    protected Class<FellingConfig> getConfigType() {
        return FellingConfig.class;
    }
}
