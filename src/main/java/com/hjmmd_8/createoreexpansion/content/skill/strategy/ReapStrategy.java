package com.hjmmd_8.createoreexpansion.content.skill.strategy;

import com.hjmmd_8.createoreexpansion.client.tool.StrategyRenderer;
import com.hjmmd_8.createoreexpansion.common.AllStrategies;
import com.hjmmd_8.createoreexpansion.content.skill.config.ReapConfig;
import com.hjmmd_8.createoreexpansion.foundation.util.params.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.ConfigStrategy;
import com.hjmmd_8.createoreexpansion.foundation.util.BlockSearcher;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;
import java.util.function.Predicate;

public class ReapStrategy extends ConfigStrategy<BlockPos, ReapConfig> {

    ReapConfig config;

    private static final Predicate<BlockState> IS_CROP =
            blockState -> blockState.hasProperty(CropBlock.AGE);

    @Override
    public void load(ReapConfig config, DataSkill data) {
        this.config = config;
    }

    @Override
    protected Class<ReapConfig> getConfigType() {
        return ReapConfig.class;
    }

    @Override
    protected Set<BlockPos> calculate(IParams params) {
        return BlockSearcher.searchBlocks(
                params.get("Player", Player.class).level(),
                params.get("Center", BlockPos.class),
                config.maxBlocks,
                config.searchRange,
                IS_CROP);
    }

    @Override
    protected boolean shouldRender(ClientLevel world, IParams params) {
        return IS_CROP.test(params.get("CenterState", BlockState.class));
    }

    @Override
    public StrategyRenderer getRenderer() {
        return AllStrategies.Renderers.BLOCK;
    }
}
