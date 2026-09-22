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
 * <p>第一方向 = 主朝向（决定 AOE 的深度方向）<br>
 * 第二方向 = 副朝向（总是水平方向 N/E/S/W）</p>
 *
 * <p>两套方案：<br>
 * {@link #fromBlockFace} — 方块面为主，副方向按规则计算<br>
 * {@link #fromPlayerYaw} — 两个方向统一按偏航角</p>
 *
 * <p>上面两套吃的是<b>世界空间</b>的玩家/命中结果；所在坐标系不是世界时（物理结构
 * sub-level 的局部系），调用方先用桥接把两个朝向来源换算过去，再交给 {@link #fromLocal}——
 * 判定规则逐条相同，只是"水平面"换成了目标坐标系自己的 XZ 面。</p>
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

    // ========== 工厂方法（朝向已本地化） ==========

    /**
     * 本地化工厂：**判定规则与 {@link #from(Player, BlockHitResult, From)} 逐条相同**，
     * 区别只在于两个朝向来源已经在目标坐标系里（物理结构 sub-level 的局部系：调用方先用
     * 桥接的 {@code toLocalDir} 把「世界视线」与「世界方块面法线」各换算一次再传进来）。
     *
     * <p>规则对应关系：
     * <ul>
     *   <li>{@code BLOCK_FACE} ↔ {@link #fromBlockFace(Player, BlockHitResult)}：
     *       主 = 离 {@code localFaceNormal} 最近的本地轴向（即旧 {@code hit.getDirection()} 的本地化）；
     *       主在本系里是水平的（轴 ≠ Y）→ 副 = 主，主竖直 → 副 = 视线在本系<b>水平面</b>上的投影方向
     *       （即旧 {@code Direction.fromYRot(player.getYRot())} 的本地化——偏航角本来就是
     *       "视线投影到水平面"，这里只把那个水平面换成目标系自己的 XZ 面）。</li>
     *   <li>{@code PLAYER_YAW} ↔ {@link #fromPlayerYaw(Player)}：
     *       主 = 副 = 视线在本系水平面上的投影方向。</li>
     * </ul>
     *
     * <p><b>主世界（未装 Sable / 没命中结构）不要调本方法</b>：那条路径照旧走
     * {@link #from(Player, BlockHitResult, From)}，与引入本方法之前<b>逐字一致</b>。
     *
     * @param from            配置里的朝向来源枚举（语义不变）
     * @param localFaceNormal 世界方块面法线经 {@code toLocalDir} 换算后的向量（无需单位化）
     * @param localLook       世界视线向量 {@code player.getLookAngle()} 经 {@code toLocalDir} 换算后的向量
     * @return 目标坐标系下的双方向；拿去 {@link #collect} 得到的即"该系里的矩形"
     *         （结构水平 → 平面水平，结构倾斜 → 平面跟着倾斜）
     */
    public static DualDirection fromLocal(From from, Vec3 localFaceNormal, Vec3 localLook) {
        if (from == From.BLOCK_FACE) {
            Direction face = Direction.getNearest(localFaceNormal);
            return face.getAxis() != Direction.Axis.Y
                    ? new DualDirection(face, face)
                    : new DualDirection(face, horizontalYaw(localLook));
        }
        Direction yaw = horizontalYaw(localLook);
        return new DualDirection(yaw, yaw);
    }

    /**
     * 视线方向 → 该坐标系里的"偏航"方向（落在本系水平面内的 N/E/S/W）。
     *
     * <p>丢掉竖直分量再取最近轴向，因此结果<b>永远不会</b>是 UP/DOWN，与旧路径
     * {@link Direction#fromYRot(double)}"只看水平、不看俯仰"的性质一致——副朝向
     * "总是水平方向"这条类注释里的不变量在目标坐标系里同样成立。</p>
     *
     * <p>视线与竖直轴重合时投影退化（例如结构水平、玩家正上正下地看），退回 {@code NORTH}：
     * 旧路径此处也是由偏航角给出的"任意但确定"的值，性质相同（该值只影响副朝向，
     * 而本工程的 AOE 范围只消费 {@link #primary}）。</p>
     */
    private static Direction horizontalYaw(Vec3 look) {
        if (look.x * look.x + look.z * look.z < 1.0E-8D) {
            return Direction.NORTH;
        }
        return Direction.getNearest(look.x, 0.0D, look.z);
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