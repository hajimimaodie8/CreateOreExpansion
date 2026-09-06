package com.hjmmd_8.createoreexpansion.content.energyfield;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 一块<b>匀强区域能量场</b>（不可变值对象）。
 *
 * <p>由 {type, region, direction, strength} 完整描述一块匀强场：
 * <ul>
 *   <li>{@code region} —— 场作用的立方区域（世界坐标 AABB）；</li>
 *   <li>{@code direction} —— 场方向（单位向量；加速场=受力方向，偏转场=磁场轴方向）；</li>
 *   <li>{@code strength} —— 场强（加速场单位：格/秒²；偏转场单位：角速度 rad/s，见 {@link #apply}）。</li>
 * </ul>
 * 方向/强度均可由方块参数自定义（蓝宝石场域控制器提供），区域通常随方块位置生成。
 *
 * <p><b>source</b>：可选的所有者标识（如 {@code "controller:<pos>"}）。调试命令建的场为 null；
 * 控制器配对产出的场带 source，<b>不入档</b>（方块位置决定、重进世界后由 tick 重新产出，
 * 见 {@link EnergyFieldSavedData}）。本类实现<b>按值相等</b>（region/type/direction/strength/source），
 * 供注册表按 tick 幂等刷新去重与变更检测。</p>
 */
public final class EnergyField {

	private final EnergyFieldType type;
	private final AABB region;
	private final Vec3 direction;
	private final double strength;
	private final String source;
	/**
	 * 旋转框世界角点（客户端渲染用，仅结构场）：8 个世界坐标角点，索引按
	 * {@code idx=(xHigh?4:0)|(yHigh?2:0)|(zHigh?1:0)} 位序排列（渲染端按位邻接连 12 条棱，
	 * 框随结构姿态旋转）。真实/调试场为 null（走轴对齐 AABB 渲染）。<b>不参与相等判定/存档</b>。
	 */
	private final double[] frame;

	public EnergyField(EnergyFieldType type, AABB region, Vec3 direction, double strength) {
		this(type, region, direction, strength, null, null);
	}

	public EnergyField(EnergyFieldType type, AABB region, Vec3 direction, double strength, String source) {
		this(type, region, direction, strength, source, null);
	}

	public EnergyField(EnergyFieldType type, AABB region, Vec3 direction, double strength, String source,
		double[] frame) {
		this.type = type;
		this.region = region;
		Vec3 d = direction.normalize();
		this.direction = isFinite(d) && d.lengthSqr() > 1.0E-4 ? d : new Vec3(0, 1, 0);
		this.strength = strength;
		this.source = source;
		this.frame = frame;
	}

	private static boolean isFinite(Vec3 v) {
		return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
	}

	public EnergyFieldType type() {
		return type;
	}

	public AABB region() {
		return region;
	}

	public Vec3 direction() {
		return direction;
	}

	public double strength() {
		return strength;
	}

	/** 所有者标识；调试命令建的场为 null，机器配对的场形如 {@code controller:...}。 */
	public String source() {
		return source;
	}

	/** 旋转框世界角点（24 doubles，结构场渲染用；真实/调试场为 null）。 */
	public double[] frame() {
		return frame;
	}

	/** 该场是否由某控制器方块产出（源标识非空）。 */
	public boolean isMachineProduced() {
		return source != null;
	}

	/** 按值相等（客户端重建/服务端跨 tick 刷新都按同一几何判定同一场；frame 不参与）。 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof EnergyField that))
			return false;
		return type == that.type && Double.compare(strength, that.strength) == 0
			&& java.util.Objects.equals(source, that.source)
			&& region.minX == that.region.minX && region.minY == that.region.minY
			&& region.minZ == that.region.minZ && region.maxX == that.region.maxX
			&& region.maxY == that.region.maxY && region.maxZ == that.region.maxZ
			&& direction.x == that.direction.x && direction.y == that.direction.y
			&& direction.z == that.direction.z;
	}

	@Override
	public int hashCode() {
		int h = type.hashCode();
		long bits;
		bits = Double.doubleToLongBits(region.minX); h = 31 * h + (int) (bits ^ (bits >>> 32));
		bits = Double.doubleToLongBits(region.minY); h = 31 * h + (int) (bits ^ (bits >>> 32));
		bits = Double.doubleToLongBits(region.minZ); h = 31 * h + (int) (bits ^ (bits >>> 32));
		bits = Double.doubleToLongBits(region.maxX); h = 31 * h + (int) (bits ^ (bits >>> 32));
		bits = Double.doubleToLongBits(region.maxY); h = 31 * h + (int) (bits ^ (bits >>> 32));
		bits = Double.doubleToLongBits(region.maxZ); h = 31 * h + (int) (bits ^ (bits >>> 32));
		bits = Double.doubleToLongBits(direction.x); h = 31 * h + (int) (bits ^ (bits >>> 32));
		bits = Double.doubleToLongBits(direction.y); h = 31 * h + (int) (bits ^ (bits >>> 32));
		bits = Double.doubleToLongBits(direction.z); h = 31 * h + (int) (bits ^ (bits >>> 32));
		bits = Double.doubleToLongBits(strength); h = 31 * h + (int) (bits ^ (bits >>> 32));
		h = 31 * h + (source == null ? 0 : source.hashCode());
		return h;
	}

	/** 该场是否覆盖某世界点。 */
	public boolean contains(Vec3 pos) {
		return region.contains(pos);
	}

	/**
	 * 对位于本场内的<b>带电</b>运动实体施加一 tick 的速度增量（格/秒）。
	 *
	 * @param vel       实体当前速度向量（格/秒，运动方向）
	 * @param charge    电荷极性（区分正负受力）
	 * @param strengthScale 本场对当前实体的强度缩放（可留 1.0）
	 * @return 施加场作用后的新速度向量（格/秒）
	 */
	public Vec3 apply(Vec3 vel, ChargePolarity charge, double strengthScale) {
		double q = charge.sign();
		double s = strength * strengthScale;
		switch (type) {
			case ACCELERATION -> {
				// 电场式：沿场方向加速（正电荷沿 direction，负电荷反向）。
				// 力度 = 每 tick 增量 ≈ s/20 格/秒（1 秒内把沿场速度分量推到 s）。
				return vel.add(direction.scale(q * s / 20.0));
			}
			case DEFLECTION -> {
				// 磁场式（洛伦兹）：力 = q·v×B，恒 ⊥ 速度 → 磁场不做功，速率严格不变。
				// 每 tick 把速度向量绕场轴（direction）旋转一个小角 θ，轨迹为匀速率圆弧：
				//   · 速度沿场轴的分量不变（平行运动不受横向力，与旧实现一致）；
				//   · 垂直分量在 ⊥B 平面内匀速转动，角速度 ω = s/20 (rad/s) → 半径 r = v⊥·20/s；
				//   · 纯旋转不改变 |v|，不会像旧实现（vel + ⊥Δv 的二阶增能）那样越转越快、
				//     曲率半径不断变大（外旋螺旋）。
				Vec3 axis = direction; // 单位向量（构造时已归一化）
				double vPar = vel.dot(axis);
				// 平行分量原样保留；仅旋转垂直分量（等价于整体旋转后自动保留平行分量）
				double theta = -charge.sign() * (s / 20.0) / 20.0; // 每 tick 旋转角 = ω·(1/20s)
				double cos = Math.cos(theta);
				double sin = Math.sin(theta);
				// Rodrigues：v' = v·cosθ + (axis×v)·sinθ + axis·(axis·v)·(1−cosθ)
				Vec3 axisCrossV = axis.cross(vel);
				Vec3 rotated = vel.scale(cos)
					.add(axisCrossV.scale(sin))
					.add(axis.scale(vPar * (1.0 - cos)));
				return rotated;
			}
		}
		return vel;
	}
}
