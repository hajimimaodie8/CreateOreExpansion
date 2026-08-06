package com.hjmmd_8.createoreexpansion.client.tool;

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

        // 将玩家偏航角映射为朝向方向
        float yaw = player.getYRot();
        int dirIndex = (int) Math.floor((((yaw % 360) + 360) % 360) / 90.0 + 0.5) % 4;
        Direction[] dirs = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
        Direction facing = dirs[dirIndex];
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();

        positions.add(center.relative(left));
        positions.add(center.relative(right));

        return positions;
    }
}
