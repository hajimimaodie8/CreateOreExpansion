package com.hjmmd_8.createoreexpansion.content.equipment.armor;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

/**
 * <b>「旋转轴 + 转向」的中立载体</b>（用户 2026-10-02 的粒子要求）。
 *
 * <p>用户原话："控制手摇曲柄时，粒子的效果应该沿着手摇曲柄的转动方向，也就是位于手摇曲柄的
 * 转动角速度矢量垂直的平面，旋转方向与角速度的方向一致。" 于是临域充力那两圈环绕粒子不能再
 * 画在"世界水平面 + 写死方向"上，而要画在<b>该动力源此刻真实转轴</b>所定的平面里。</p>
 *
 * <h2>语义（右手定则，符号约定写在类型上而不是散在调用点）</h2>
 * <ul>
 *   <li>{@link #axis()}：<b>单位</b>旋转轴向量（{@code |axis| = 1}）。</li>
 *   <li>{@link #sign()}：转向符号，恰好 {@code ±1}。它表示角速度矢量
 *       {@code ω = sign · |ω| · axis}，其中"绕 {@code axis} 转 {@code +|ω|}"按<b>右手定则</b>
 *       理解（右手拇指指向 {@code axis} 时四指的方向）—— 这与 Create 两个渲染路径的约定逐字一致：
 *       {@code HandCrankVisual#rotateCrank} 用
 *       {@code rotate(rad(independentAngle), Direction.get(POSITIVE, facing.getAxis()))}，
 *       {@code KineticBlockEntityRenderer#kineticRotationTransform} 用
 *       {@code rotateCentered(angle, Direction.get(POSITIVE, axis))}，且角度都由
 *       {@code independentAngle += convertToAngular(getSpeed())} 累出（{@code getSpeed()} 即
 *       {@code getGeneratedSpeed()}），所以 {@code sign} 直接取生成转速的符号就对得上手柄。</li>
 * </ul>
 *
 * <p><b>谁生产它</b>：{@code StressSourceKind#rotationAxis} 的实现者（每方块自己的知识，
 * 例如 {@code HandCrankStressSource} 读 Create 的曲柄 FACING + {@code getGeneratedSpeed()}）。
 * <b>谁消费它</b>：{@link ArmorSkillFx#fieldChargeRing} —— 它只拿 {@code axis}/{@code sign} 做
 * 正交基参数化 {@code p(θ) = center + cos θ·r·u + sin θ·r·v}（{@code θ = 2πt + sign·phase}），
 * 不需要知道任何具体模组的朝向字段怎么读。</p>
 *
 * <p>{@code null} 的语义（见 {@link #of}）：<b>"此刻拿不到旋转信息"</b> —— 调用方应当这一 tick
 * 不画环，<b>绝不</b>回落到某个写死的平面或方向。</p>
 *
 * @param axis 单位旋转轴（构造时归一化；零向量是编程错误）
 * @param sign 转向符号（构造时折成 {@code ±1}）
 * @since 1.0.0
 */
public record RotationAxis(Vec3 axis, double sign) {

    /**
     * 规范化：轴折成单位向量、符号折成 {@code ±1}（放在紧凑构造器里，任何构造路径都逃不掉）。
     *
     * @throws IllegalArgumentException 轴是零向量（调用方应当先走 {@link #of}，它会返回 {@code null}）
     */
    public RotationAxis {
        double length = axis.length();
        if (length < 1.0E-6D) {
            throw new IllegalArgumentException("RotationAxis needs a non-zero axis vector");
        }
        axis = axis.scale(1.0D / length);
        sign = sign < 0.0D ? -1.0D : 1.0D;
    }

    /**
     * 安全构造：轴为空/零向量 ⇒ {@code null}（="这一刻没有可信的旋转信息"）。
     *
     * @param axis 旋转轴向量（不必是单位向量，本方法会归一化）
     * @param sign 转向符号（{@code < 0} 视为 {@code -1}，否则 {@code +1}；{@code 0} 视为 {@code +1}，
     *             因为"没有角速度"的那一刻调用方本来就该决定要不要画环，而不是让环随机定个向）
     */
    public static @Nullable RotationAxis of(@Nullable Vec3 axis, double sign) {
        if (axis == null || axis.length() < 1.0E-6D) {
            return null;
        }
        return new RotationAxis(axis, sign);
    }
}
