package com.hjmmd_8.createoreexpansion.content.skill.strategy;

import com.hjmmd_8.createoreexpansion.content.skill.config.FellingConfig;
import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.AreaStrategy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.ConfigStrategy;
import com.hjmmd_8.createoreexpansion.foundation.util.BlockSearch;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * 砍伐策略 - 用于斧头的连锁砍树。
 *
 * <p>BFS 搜索相连的原木/树叶，与渲染预览共用同一实现。</p>
 */
public class FellingStrategy extends ConfigStrategy<BlockPos, FellingConfig> implements AreaStrategy {

    @Override
    public Set<BlockPos> calculate(FellingConfig config, IParams params) {
        Player player = params.get("Player", Player.class);
        BlockPos center = params.get("Center", BlockPos.class);
        return calculateTreeBlocks(player.level(), center, config);
    }

    /** BFS搜索相连的方块 */
    private Set<BlockPos> calculateTreeBlocks(Level level, BlockPos startPos, FellingConfig config) {
        Set<BlockPos> result = BlockSearch.collect(
                level, startPos, config.maxBlocks, config.searchRange, config.predicate);
        result.remove(startPos);
        return result;
    }

    @Override
    public boolean shouldRender(FellingConfig config, ClientLevel world, IParams params) {
        BlockState state = params.get("CenterState", BlockState.class);
        return FellingConfig.BlockPredicate.IS_LOG.test(state);
    }

    @Override
    protected Class<FellingConfig> getConfigType() {
        return FellingConfig.class;
    }
}
