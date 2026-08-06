package com.hjmmd_8.createoreexpansion.foundation.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 双方向解析器
 *
 * <p>第一个方向 = 方块被点击的面<br>
 * 第二个方向 = 玩家视角在垂直平面上的投影<br>
 * 当玩家视线接近垂直时，两个方向相同</p>
 */
public class DualDirection {

    public final Direction primary;
    public final Direction secondary;

    private DualDirection(Direction primary, Direction secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    /**
     * 从玩家点击的方块面解析双方向
     */
    public static DualDirection of(Player player, BlockHitResult hit) {
        Vec3 look = player.getLookAngle();
        Direction blockFace = hit.getDirection();

        if (isVerticalLook(look)) {
            Direction vertical = look.y > 0 ? Direction.UP : Direction.DOWN;
            return new DualDirection(vertical, vertical);
        }

        return new DualDirection(blockFace, project(blockFace, look, player));
    }

    // ========== 投影计算 ==========

    private static boolean isVerticalLook(Vec3 look) {
        return Math.abs(look.y) > 0.9;
    }

    private static Direction project(Direction face, Vec3 look, Player player) {
        return switch (face) {
            case UP, DOWN -> Direction.fromYRot(player.getYRot());

            case NORTH, SOUTH -> Math.abs(look.y) > 0.7
                    ? (look.y > 0 ? Direction.UP : Direction.DOWN)
                    : (look.x > 0 ? Direction.EAST : Direction.WEST);

            case WEST, EAST -> Math.abs(look.y) > 0.7
                    ? (look.y > 0 ? Direction.UP : Direction.DOWN)
                    : (look.z > 0 ? Direction.SOUTH : Direction.NORTH);
        };
    }

    // ========== 便捷方法 ==========

    /**
     * 用双方向消费 AOE 范围（例：镐子挖掘范围、渲染范围）
     */
    public void apply(BlockPos center, int width, int height, int depth,
                      Consumer<BlockPos> consumer) {
        var bb = AreaUtil.getAreaOfEffect(center, primary, width, height, depth);

        BlockPos.betweenClosedStream(bb).forEach(pos -> {  // 直接 forEach，不 toList
            BlockPos immutable = pos.immutable();          // 每次循环复制
            if (!immutable.equals(center)) {
                consumer.accept(immutable);
            }
        });
    }

    /**
     * 用双方向收集所有位置（例：渲染预览）
     */
    public Set<BlockPos> collect(BlockPos center, int width, int height, int depth) {
        Set<BlockPos> result = new HashSet<>();
        apply(center, width, height, depth, result::add);
        return result;
    }
}