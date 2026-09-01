package com.hjmmd_8.createoreexpansion.content.skill.strategy;

import com.hjmmd_8.createoreexpansion.content.skill.config.HoeConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.AreaStrategy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.ConfigStrategy;
import com.hjmmd_8.createoreexpansion.foundation.util.FarmlandHelper;
import com.hjmmd_8.createoreexpansion.foundation.IParams;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/**
 * 锄头策略 —— 计算技能作用范围内的方块，供渲染预览使用。
 */
public class HoeStrategy extends ConfigStrategy<BlockPos, HoeConfig> implements AreaStrategy {

    @Override
    public Set<BlockPos> calculate(HoeConfig config, IParams params) {
        BlockPos center = params.get("Center", BlockPos.class);
        return new HashSet<>(FarmlandHelper.collectArea(center, config.rangeWidth, config.rangeDepth));
    }

    @Override
    public boolean shouldRender(HoeConfig config, Level world, IParams params) {
        BlockState state = params.get("CenterState", BlockState.class);
        // 目标为成熟作物 / 耕地 / 可犁地时展示预览
        return FarmlandHelper.resolvePriority(state) != 0;
    }

    @Override
    protected Class<HoeConfig> getConfigType() {
        return HoeConfig.class;
    }
}
