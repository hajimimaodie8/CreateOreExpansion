package com.hjmmd_8.createoreexpansion.foundation.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 双方向解析器
 *
 * <p>第一方向 = 主朝向（决定 AOE 的深度方向）<br>
 * 第二方向 = 副朝向（总是水平方向 N/E/S/W）</p>
 *
 * <p>两套方案：<br>
 * {@link #fromBlockFace} — 方块面为主，副方向按规则计算<br>
 * {@link #fromPlayerYaw} — 两个方向统一按偏航角</p>
 */
public class DualDirection {

    public final Direction primary;
    public final Direction secondary;

    private DualDirection(Direction primary, Direction secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    // ========== 工厂方法 ==========

    /**
     * 方案一：以方块面为主朝向
     *
     * <p>主 = 方块面<br>
     * 副 = 如果主是水平方向 → 和主一样<br>
     * 如果主是 UP/DOWN → 玩家偏航角朝向</p>
     */
    public static DualDirection fromBlockFace(Player player, BlockHitResult hit) {
        Direction face = hit.getDirection();

        if (face.getAxis() != Direction.Axis.Y) {
            // 水平方向 → 两个方向相同
            return new DualDirection(face, face);
        } else {
            // 垂直方向 → 副方向用偏航角
            return new DualDirection(face, Direction.fromYRot(player.getYRot()));
        }
    }

    /**
     * 方案二：两个方向统一按偏航角
     *
     * <p>主 = 副 = 玩家偏航角方向</p>
     */
    public static DualDirection fromPlayerYaw(Player player) {
        Direction dir = Direction.fromYRot(player.getYRot());
        return new DualDirection(dir, dir);
    }

    public static DualDirection from(Player player, BlockHitResult hit, From from) {
        return switch (from) {
            case BLOCK_FACE -> fromBlockFace(player, hit);
            case PLAYER_YAW -> fromPlayerYaw(player);
        };
    }

    public static From fromBlockFace() {
        return From.BLOCK_FACE;
    }

    public static From fromPlayerYaw() {
        return From.PLAYER_YAW;
    }

    // ========== 便捷方法 ==========

    public void apply(BlockPos center, int width, int height, int depth,
                      Consumer<BlockPos> consumer) {
        var bb = AreaUtil.getAreaOfEffect(center, primary, width, height, depth);

        BlockPos.betweenClosedStream(bb).forEach(pos -> {
            BlockPos immutable = pos.immutable();
            if (!immutable.equals(center)) {
                consumer.accept(immutable);
            }
        });
    }

    public Set<BlockPos> collect(BlockPos center, int width, int height, int depth) {
        Set<BlockPos> result = new HashSet<>();
        apply(center, width, height, depth, result::add);
        return result;
    }

    public enum From {
        BLOCK_FACE,
        PLAYER_YAW
    }
}