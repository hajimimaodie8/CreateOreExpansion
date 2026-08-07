package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.foundation.util.DualDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashSet;
import java.util.Set;

/**
 * 方向性渲染策略 - 用于翡翠镐（左右各1格）
 */
public class DirectionalRenderStrategy implements RenderStrategy {

    @Override
    public Set<BlockPos> calculatePositions(BlockPos center, BlockHitResult hit, Player player) {
        Set<BlockPos> positions = new HashSet<>();

        // 使用DualDirection基于玩家偏航角计算方向
        DualDirection dualDirection = DualDirection.fromPlayerYaw(player);
        Direction primary = dualDirection.primary;

        // 计算左右两侧
        Direction left = primary.getCounterClockWise();
        Direction right = primary.getClockWise();

        positions.add(center.relative(left));
        positions.add(center.relative(right));

        return positions;
    }
}
