package com.hjmmd_8.createoreexpansion.foundation.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * AOE 范围计算与方块过滤工具类
 *
 * <p>所有 AOE 技能共用，新建工具时<b>无需修改此文件</b>。</p>
 *
 * <p>方法说明：<br>
 * {@link #getAreaOfEffect(BlockPos, Direction, int, int)} — 根据玩家视线方向<br>
 * 返回一个 BoundingBox，后续用 BlockPos.betweenClosedStream() 遍历。<br>
 * radius=3 depth=1  => 3x3 平面<br>
 * radius=5 depth=3  => 5x5 纵深 3 层<br>
 * radius=3 depth=3  => 3x3x3 立体</p>
 */
public class AreaUtil {

    /**
     * 计算 AOE 范围 BoundingBox，方向由 BlockHitResult.getDirection() 提供
     * @param blockPos 玩家点击的方块中心位置
     * @param direction 被击中的面方向（UP/DOWN/NORTH/SOUTH/WEST/EAST）
     * @param radius AOE 半径（如 3 表示 3x3）
     * @param depth AOE 纵深（如 1 表示单层）
     */
    public static BoundingBox getAreaOfEffect(BlockPos blockPos, Direction direction, int radius, int depth) {
        int size = radius / 2;
        int offset = size - 1;

        return switch (direction) {
            case DOWN, UP -> new BoundingBox(
                    blockPos.getX() - size, blockPos.getY() - (direction == Direction.UP ? depth - 1 : 0),
                    blockPos.getZ() - size,
                    blockPos.getX() + size, blockPos.getY() + (direction == Direction.DOWN ? depth - 1 : 0),
                    blockPos.getZ() + size);
            case NORTH, SOUTH -> new BoundingBox(
                    blockPos.getX() - size, blockPos.getY() - size + offset,
                    blockPos.getZ() - (direction == Direction.SOUTH ? depth - 1 : 0),
                    blockPos.getX() + size, blockPos.getY() + size + offset,
                    blockPos.getZ() + (direction == Direction.NORTH ? depth - 1 : 0));
            case WEST, EAST -> new BoundingBox(
                    blockPos.getX() - (direction == Direction.EAST ? depth - 1 : 0),
                    blockPos.getY() - size + offset, blockPos.getZ() - size,
                    blockPos.getX() + (direction == Direction.WEST ? depth - 1 : 0),
                    blockPos.getY() + size + offset, blockPos.getZ() + size);
        };
    }

    /**
     * 判断方块能否被 AOE 破坏。新建工具时<b>无需修改</b>。
     * 规则：不破坏流体、不破坏带 TileEntity 的方块（箱子、熔炉等）。
     * 硬度判断在 BlockBreaker 中由创造/生存模式决定。
     */
    public static boolean canDestroy(BlockState targetState, Level level, BlockPos pos) {
        if (!targetState.getFluidState().isEmpty()) return false;
        return level.getBlockEntity(pos) == null;
    }
}