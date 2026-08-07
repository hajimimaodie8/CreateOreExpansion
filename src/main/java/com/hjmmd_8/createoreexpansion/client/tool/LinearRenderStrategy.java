package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.foundation.util.DualDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashSet;
import java.util.Set;

/**
 * 线性渲染策略 - 用于铲子的一列挖掘
 */
public class LinearRenderStrategy implements RenderStrategy {
    private final int maxDepth;

    public LinearRenderStrategy(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public Set<BlockPos> calculatePositions(BlockPos center, BlockHitResult hit, Player player) {
        Set<BlockPos> positions = new HashSet<>();

        // 使用DualDirection基于方块面计算方向
        DualDirection dualDirection = DualDirection.fromBlockFace(player, hit);
        Direction digDir = dualDirection.primary;

        for (int i = 1; i <= maxDepth; i++) {
            positions.add(center.relative(digDir, i));
        }

        return positions;
    }
}
