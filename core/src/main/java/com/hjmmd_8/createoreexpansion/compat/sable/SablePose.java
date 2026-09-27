package com.hjmmd_8.createoreexpansion.compat.sable;

import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge.Hit;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;

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
 *
 * <p><b>双端</b>：服务端/客户端的 sub-level 共同基类是 {@link SubLevel}
 * （服务端实现 {@code ServerSubLevel}、客户端实现 {@code ClientSubLevel}），本类一律按基类写，
 * 并且<b>不引用任何 {@code net.minecraft.client.*} 或 Sable 客户端类</b>——
 * 本类是 common 源码，专用服务器上一样要能加载（渲染位姿用基类的
 * {@code lastPose()}/{@code logicalPose()} 自己插值，见 {@link #renderPose}）。</p>
 */
final class SablePose {

	private SablePose() {
	}

	/** Hit → SubLevel（不泄漏 Sable 类型到接口层；服务端/客户端实现都是它的子类）。 */
	static SubLevel sub(Hit hit) {
		return (SubLevel) hit.subLevel();
	}

	/** sub-level 当前位姿（逻辑位姿：平移 + 四元数 + 旋转中心 + 缩放）。 */
	static Pose3dc pose(Hit hit) {
		return sub(hit).logicalPose();
	}

	/**
	 * sub-level 的<b>渲染位姿</b>（上一 tick 位姿 → 当前 tick 位姿，按 partialTick 插值）。
	 *
	 * <p>逐条对齐 Sable 自己的 {@code ClientSubLevel#renderPose(float)}
	 * （同样是 {@code set(lastPose)} 之后 position/rotationPoint/scale 用 {@code lerp}、
	 * orientation 用 {@code slerp}），这样预览框与被绘制的结构本体同一条位姿，
	 * 结构移动时不会领先/落后一个 tick。</p>
	 *
	 * <p>这里刻意走基类 API 而<b>不</b>调 {@code ClientSubLevel#renderPose}：
	 * 后者是客户端类，本类是 common 源码，专用服务器上不能出现客户端类引用。
	 * 服务端（{@code lastPose} 与 {@code logicalPose} 同步更新）退化为当前位姿，等价于旧行为。</p>
	 */
	static Pose3dc renderPose(Hit hit, float partialTick) {
		SubLevel sub = sub(hit);
		Pose3dc to = sub.logicalPose();
		Pose3dc from = sub.lastPose();
		if (from == null || to == null || partialTick <= 0.0F || from == to) {
			return to;
		}
		float t = Math.min(partialTick, 1.0F);
		Pose3d pose = new Pose3d(from);
		pose.position().lerp(to.position(), t);
		pose.orientation().slerp(to.orientation(), t);
		pose.rotationPoint().lerp(to.rotationPoint(), t);
		pose.scale().lerp(to.scale(), t);
		return pose;
	}

	/** plot 中心（BlockPos）：EmbeddedPlotLevelAccessor 查询坐标 = 绝对坐标 - center。 */
	static BlockPos centerOf(SubLevel sub) {
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

	/** 本地坐标 → 世界坐标（渲染位姿，含亚刻插值）。 */
	static Vec3 toWorld(Hit hit, Vec3 localPos, float partialTick) {
		return renderPose(hit, partialTick).transformPosition(localPos);
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
