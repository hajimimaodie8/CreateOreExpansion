package com.hjmmd_8.createoreexpansion.content.skill.strategy;

import com.hjmmd_8.createoreexpansion.content.skill.config.AreaAoeConfig;
import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.AreaStrategy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.ConfigStrategy;
import com.hjmmd_8.createoreexpansion.foundation.util.DualDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Set;

/**
 * 范围AOE策略 - 用于镐子和铲子的范围挖掘
 *
 * <p>支持平面范围（3x3、5x5）、深度范围与线性挖掘，
 * 基于 {@link DualDirection} 计算范围，与渲染预览共用同一实现。</p>
 */
public class AreaAoeStrategy extends ConfigStrategy<BlockPos, AreaAoeConfig> implements AreaStrategy {

    @Override
    public Set<BlockPos> calculate(AreaAoeConfig config, IParams params) {
        Player player = params.get("Player", Player.class);
        BlockHitResult hit = params.get("BlockHitResult", BlockHitResult.class);
        BlockPos center = params.get("Center", BlockPos.class);
        DualDirection dualDirection = DualDirection.from(player, hit, config.directionSource);
        return dualDirection.collect(center, config.width, config.height, config.depth);
    }

    @Override
    public boolean shouldRender(AreaAoeConfig config, Level world, IParams params) {
        BlockState state = params.get("CenterState", BlockState.class);
        return state.is(config.mineableTag);
    }

    @Override
    protected Class<AreaAoeConfig> getConfigType() {
        return AreaAoeConfig.class;
    }
}
