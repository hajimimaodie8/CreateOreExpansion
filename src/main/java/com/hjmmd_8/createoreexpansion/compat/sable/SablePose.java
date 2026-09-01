package com.hjmmd_8.createoreexpansion.compat.sable;

import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge.Hit;

import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Sable 位姿变换工具 —— Hit（sub-level 引用）↔ 世界/本地坐标的数学换算。
 *
 * <p><b>核心</b>：Sable 的 {@link Pose3dc} 内置位姿矩阵（"数学矩阵运算函数"）：
 * {@code transformPosition}（本地→世界）、{@code transformPositionInverse}（世界→本地）、
 * {@code transformNormal}/{@code transformNormalInverse}（方向向量）。</p>
 *
 * <p><b>坐标基准（踩坑总结）</b>：Pose 本地系 = plot 坐标（与 BE.getBlockPos() 同空间，大数）。
 * {@code EmbeddedPlotLevelAccessor} 查询需「绝对坐标 - plot 中心」——见 {@link #accessorPos}。</p>
 */
final class SablePose {

	private SablePose() {
	}

	/** Hit → ServerSubLevel（不泄漏 Sable 类型到接口层）。 */
	static ServerSubLevel sub(Hit hit) {
		return (ServerSubLevel) hit.subLevel();
	}

	/** sub-level 当前位姿（逻辑位姿：平移 + 四元数 + 旋转中心 + 缩放）。 */
	static Pose3dc pose(Hit hit) {
		return sub(hit).logicalPose();
	}

	/** plot 中心（BlockPos）：EmbeddedPlotLevelAccessor 查询坐标 = 绝对坐标 - center。 */
	static BlockPos centerOf(ServerSubLevel sub) {
		return sub.getPlot()
			.getCenterBlock();
	}

	/** 把 Pose 本地坐标（绝对 plot 坐标）转换为 EmbeddedPlotLevelAccessor 期望的偏移坐标。 */
	static BlockPos accessorPos(Hit hit, BlockPos localPos) {
		return localPos.subtract(centerOf(sub(hit)));
	}

	/** 本地坐标 → 世界坐标（位置变换）。 */
	static Vec3 toWorld(Hit hit, Vec3 localPos) {
		return pose(hit).transformPosition(localPos);
	}

	/** 世界坐标 → 本地坐标（位置逆变换）。 */
	static Vec3 toLocal(Hit hit, Vec3 worldPos) {
		return pose(hit).transformPositionInverse(worldPos);
	}

	/** 本地方向向量 → 世界方向向量（仅旋转/缩放，无平移）。 */
	static Vec3 toWorldDir(Hit hit, Vec3 localDir) {
		return pose(hit).transformNormal(localDir);
	}

	/** 世界方向向量 → 本地方向向量。 */
	static Vec3 toLocalDir(Hit hit, Vec3 worldDir) {
		return pose(hit).transformNormalInverse(worldDir);
	}

	/** sub-level 对应的主世界 Level（波出生/飞行所在）。 */
	static Level worldLevel(Hit hit) {
		return sub(hit).getLevel();
	}
}
