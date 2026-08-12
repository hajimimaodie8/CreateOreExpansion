package com.hjmmd_8.createoreexpansion.content.skill.strategy;

import com.hjmmd_8.createoreexpansion.content.skill.config.PlantConfig;
import com.hjmmd_8.createoreexpansion.foundation.util.params.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.AreaStrategy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.ConfigStrategy;
import com.hjmmd_8.createoreexpansion.foundation.util.BlockSearcher;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;
import java.util.function.Predicate;

public class PlantStrategy extends ConfigStrategy<BlockPos, PlantConfig> implements AreaStrategy {

    private PlantConfig config;

    // 可种植方块：耕地
    private static final Predicate<BlockState> IS_FARMLAND =
            blockState -> blockState.is(Blocks.FARMLAND);

    @Override
    public void load(PlantConfig config, DataSkill data) {
        this.config = config;
    }

    @Override
    protected Class<PlantConfig> getConfigType() {
        return PlantConfig.class;
    }

    @Override
    public Set<BlockPos> calculate(IParams params) {
        return BlockSearcher.searchBlocks(
                params.get("Player", Player.class).level(),
                params.get("Center", BlockPos.class),
                config.maxBlocks,
                config.searchRange,
                IS_FARMLAND);
    }

    @Override
    public boolean shouldRender(ClientLevel world, IParams params) {
        return IS_FARMLAND.test(params.get("CenterState", BlockState.class));
    }
}