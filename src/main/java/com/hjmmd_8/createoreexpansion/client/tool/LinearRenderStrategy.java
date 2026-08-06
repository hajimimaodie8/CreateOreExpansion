package com.hjmmd_8.createoreexpansion.client.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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

        Direction digDir;
        // 视角朝下时水平挖
        if (player.getXRot() > 45.0F) {
            float yaw = player.getYRot();
            int idx = (int) Math.floor((((yaw % 360) + 360) % 360) / 90.0 + 0.5) % 4;
            Direction[] dirs = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
            digDir = dirs[idx];
        } else {
            digDir = hit.getDirection().getOpposite();
        }

        for (int i = 1; i <= maxDepth; i++) {
            positions.add(center.relative(digDir, i));
        }

        return positions;
    }
}
