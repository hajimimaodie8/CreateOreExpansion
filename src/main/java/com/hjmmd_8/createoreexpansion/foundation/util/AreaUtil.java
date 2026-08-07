package com.hjmmd_8.createoreexpansion.foundation.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * AOE 范围计算与方块过滤工具类
 *
 * <p>所有 AOE 技能共用，新建工具时<b>可能需要修改此文件</b>。</p>
 *
 * <p>方法说明：<br>
 * {@link #getAreaOfEffect(BlockPos, Direction, int, int, int)} — 根据玩家视线方向<br>
 * 返回一个 BoundingBox，后续用 BlockPos.betweenClosedStream() 遍历。<br>
 * width=3 height=3 depth=1  => 3x3 平面<br>
 * width=5 height=5 depth=3  => 5x5 纵深 3 层<br>
 * width=3 height=3 depth=3  => 3x3x3 立体</p>
 */
public class AreaUtil {

    /**
     * 计算 AOE 范围 BoundingBox，支持长宽完全自定义
     * @param blockPos 玩家点击的方块中心位置
     * @param direction 被击中的面方向（UP/DOWN/NORTH/SOUTH/WEST/EAST）
     * @param width 横向宽度（如 3 表示 3 格宽）
     * @param height 纵向高度（如 3 表示 3 格高）
     * @param depth 挖掘深度（沿挖掘方向，如 1 表示 1 格深）
     */
    public static BoundingBox getAreaOfEffect(BlockPos blockPos, Direction direction, int width, int height, int depth) {
        return switch (direction) {
            case DOWN, UP -> new BoundingBox(
                    blockPos.getX() - width / 2,
                    blockPos.getY() - (direction == Direction.DOWN ? depth - 1 : 0),
                    blockPos.getZ() - height / 2,
                    blockPos.getX() + (width - width / 2 - 1),
                    blockPos.getY() + (direction == Direction.UP ? depth - 1 : 0),
                    blockPos.getZ() + (height - height / 2 - 1));
            case NORTH, SOUTH -> new BoundingBox(
                    blockPos.getX() - width / 2,
                    blockPos.getY() - height / 2,
                    blockPos.getZ() - (direction == Direction.NORTH ? depth - 1 : 0),
                    blockPos.getX() + (width - width / 2 - 1),
                    blockPos.getY() + (height - height / 2 - 1),
                    blockPos.getZ() + (direction == Direction.SOUTH ? depth - 1 : 0));
            case WEST, EAST -> new BoundingBox(
                    blockPos.getX() - (direction == Direction.WEST ? depth - 1 : 0),
                    blockPos.getY() - height / 2,
                    blockPos.getZ() - width / 2,
                    blockPos.getX() + (direction == Direction.EAST ? depth - 1 : 0),
                    blockPos.getY() + (height - height / 2 - 1),
                    blockPos.getZ() + (width - width / 2 - 1)
            );
        };
    }

    /**
     * 计算 AOE 范围 BoundingBox（简化版，仅指定范围）
     * @param blockPos 玩家点击的方块中心位置
     * @param direction 被击中的面方向
     * @param size 范围大小（宽度和高度相同）
     * @param depth 挖掘深度
     */
    public static BoundingBox getAreaOfEffect(BlockPos blockPos, Direction direction, int size, int depth) {
        return getAreaOfEffect(blockPos, direction, size, size, depth);
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
