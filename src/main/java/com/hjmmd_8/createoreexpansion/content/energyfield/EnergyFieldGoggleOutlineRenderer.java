package com.hjmmd_8.createoreexpansion.content.energyfield;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.client.AllRenderTypes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 能量场客户端指示（Dist.CLIENT，携带工程师护目镜时生效）——<b>自绘 + 平滑淡入淡出</b>。
 *
 * <p>为什么自绘而不是 Create {@code Outliner}：Outliner 只能做"条目过期后 8 tick 淡出"，
 * 淡入无法控制，remove 又会瞬消——这正是之前"淡入淡出不自然"的根源。本类改为：</p>
 * <ul>
 *   <li><b>每场维护一个 alpha（0→1）</b>：每 tick 向目标值线性逼近。准星射线命中场
 *       区域 → 目标 1（淡入）；移开/摘镜/清场 → 目标 0（淡出）。全程平滑渐变，无瞬现瞬消；</li>
 *   <li><b>渲染</b>：在 {@link RenderLevelStageEvent} 半透明阶段用带 alpha 的线条
 *       自绘区域棱线框 + 方向推进风片/方块串（复用项目技能框同款透明线管线）；</li>
 *   <li><b>淡出完成</b>：alpha 收敛到 0 后才从状态表移除，渲染自然消失。</li>
 * </ul>
 */
@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID, value = Dist.CLIENT)
public final class EnergyFieldGoggleOutlineRenderer {

	/** 加速场 = 天蓝；偏转场 = 品红。 */
	private static final int ACCEL_COLOR = 0x33D9FF;
	private static final int DEFLECT_COLOR = 0xFF4DE6;

	/** 准星命中场区域的最大距离（格）。 */
	private static final double AIM_RANGE = 64;
	/** 风片推进速度（格/tick）。 */
	private static final double FLOW_SPEED = 0.35;
	/** 轴向判定阈值。 */
	private static final double AXIS_EPS = 0.9;
	/** 淡入/淡出每 tick 变化量（0.12 → 约 8 tick ≈ 0.4s 完成，肉眼平滑）。 */
	private static final float FADE_SPEED = 0.12f;

	/** 场内容 key → 当前显示强度（0 完全隐藏，1 完全显示）。 */
	private static final Map<Object, Float> VISIBILITY = new HashMap<>();

	private EnergyFieldGoggleOutlineRenderer() {
	}

	// ========== 状态推进（每 tick） ==========

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;

		boolean goggles = player != null
			&& com.simibubi.create.content.equipment.goggles.GogglesItem.isWearingGoggles(player);

		if (player == null || level == null || !goggles) {
			fadeAll(false);
			return;
		}

		List<EnergyField> fields = EnergyFieldClientState.in(level);
		if (fields.isEmpty()) {
			fadeAll(false);
			return;
		}

		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();

		for (EnergyField field : fields) {
			AABB box = field.region();
			Object key = fieldKey(field);
			double hit = rayHitDistance(box, eye, look);
			boolean aimed = hit >= 0 && hit <= AIM_RANGE;
			Float cur = VISIBILITY.getOrDefault(key, 0f);
			float next = aimed ? Math.min(1f, cur + FADE_SPEED) : Math.max(0f, cur - FADE_SPEED);
			if (next <= 0.001f)
				VISIBILITY.remove(key);
			else
				VISIBILITY.put(key, next);
		}

		// 已被清场/换维度、但还残留着 alpha 的旧 key → 淡出
		Iterator<Object> it = VISIBILITY.keySet()
			.iterator();
		while (it.hasNext()) {
			Object key = it.next();
			boolean stillExists = false;
			for (EnergyField field : fields) {
				if (fieldKey(field).equals(key)) {
					stillExists = true;
					break;
				}
			}
			if (!stillExists) {
				float next = Math.max(0f, VISIBILITY.get(key) - FADE_SPEED);
				if (next <= 0.001f)
					it.remove();
				else
					VISIBILITY.put(key, next);
			}
		}
	}

	private static void fadeAll(boolean on) {
		if (on) {
			for (Map.Entry<Object, Float> e : VISIBILITY.entrySet())
				e.setValue(Math.min(1f, e.getValue() + FADE_SPEED));
			return;
		}
		Iterator<Map.Entry<Object, Float>> it = VISIBILITY.entrySet()
			.iterator();
		while (it.hasNext()) {
			Map.Entry<Object, Float> e = it.next();
			float next = Math.max(0f, e.getValue() - FADE_SPEED);
			if (next <= 0.001f)
				it.remove();
			else
				e.setValue(next);
		}
	}

	// ========== 自绘渲染（每帧） ==========

	@SubscribeEvent
	public static void onRender(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
			return;
		if (VISIBILITY.isEmpty())
			return;

		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null)
			return;

		// 所有场的最新几何（仅保留仍可见者）
		List<EnergyField> fields = EnergyFieldClientState.in(level);
		if (fields.isEmpty())
			return;

		PoseStack ms = event.getPoseStack();
		Vec3 cam = event.getCamera()
			.getPosition();

		SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();
		VertexConsumer quads = buffer.getBuffer(AllRenderTypes.CUBOID_QUADS_TRANSLUCENT);

		ms.pushPose();
		ms.translate(-cam.x, -cam.y, -cam.z);
		PoseStack.Pose pose = ms.last();
		long time = level.getGameTime();

		// 棱线粗度：Create 机械臂/选区框同款 (1/12f ~ 0.083 格截面)，看得清又不糊
		float lineWidth = 1 / 12f;

		for (EnergyField field : fields) {
			Object key = fieldKey(field);
			Float a = VISIBILITY.get(key);
			if (a == null || a <= 0.001f)
				continue;

			int color = field.type() == EnergyFieldType.ACCELERATION ? ACCEL_COLOR : DEFLECT_COLOR;
			float r = ((color >> 16) & 0xFF) / 255f;
			float g = ((color >> 8) & 0xFF) / 255f;
			float b = (color & 0xFF) / 255f;

			if (field.frame() != null) {
				// 结构场：旋转框（12 条棱按位邻接）+ 沿场方向的推进环 —— 随结构姿态旋转
				drawFrameField(pose, quads, field, time, lineWidth, r, g, b, a);
				continue;
			}

			AABB box = field.region();
			drawBoxEdges(pose, quads, box, lineWidth, r, g, b, a);

			Vec3 dir = field.direction();
			boolean axisAligned = Math.abs(dir.x) > AXIS_EPS || Math.abs(dir.y) > AXIS_EPS || Math.abs(dir.z) > AXIS_EPS;
			if (axisAligned)
				drawSlabFlow(pose, quads, box, dir, time, lineWidth, r, g, b, a);
			else
				drawChipFlow(pose, quads, box, dir, time, lineWidth, r, g, b, a);
		}

		ms.popPose();
		buffer.draw(AllRenderTypes.CUBOID_QUADS_TRANSLUCENT);
	}

	/**
	 * 结构场渲染：frame = 8 个世界角点（索引位序 (xHigh?4)|(yHigh?2)|(zHigh?1)）。
	 * <ul>
	 *   <li><b>12 条棱</b>：任意一对仅差一角的角点连成方截面粗线 → 框随结构姿态旋转；</li>
	 *   <li><b>推进环</b>：两块与场方向垂直的环沿场方向扫过整个框（方向也随姿态旋转，
	 *       不再是固定竖直/轴向的小方块）。</li>
	 * </ul>
	 */
	private static void drawFrameField(PoseStack.Pose pose, VertexConsumer quads, EnergyField field, long time,
		float width, float r, float g, float b, float a) {
		double[] fr = field.frame();
		Vec3[] p = new Vec3[8];
		for (int i = 0; i < 8; i++)
			p[i] = new Vec3(fr[3 * i], fr[3 * i + 1], fr[3 * i + 2]);

		// 12 条棱
		for (int k = 0; k < 8; k++) {
			for (int bit = 1; bit <= 4; bit <<= 1) {
				if ((k & bit) == 0)
					drawSegment(pose, quads, p[k], p[k | bit], width, r, g, b, a);
			}
		}

		// 场轴 = 框三根全跨度向量中与 direction 夹角最小的一根
		Vec3 e0 = p[0];
		Vec3[] spans = { p[4].subtract(e0), p[2].subtract(e0), p[1].subtract(e0) };
		Vec3 dir = field.direction();
		int alongIdx = 0;
		double bestDot = -1;
		for (int i = 0; i < 3; i++) {
			double d = Math.abs(spans[i].normalize().dot(dir));
			if (d > bestDot) {
				bestDot = d;
				alongIdx = i;
			}
		}
		int i1 = (alongIdx + 1) % 3;
		int i2 = (alongIdx + 2) % 3;
		Vec3 along = spans[alongIdx];
		Vec3 sA = spans[i1];
		Vec3 sB = spans[i2];
		double len = along.length();
		if (len < 1.0)
			return;
		Vec3 alongUnit = along.scale(1.0 / len);
		Vec3 uA = sA.normalize();
		Vec3 uB = sB.normalize();
		// 垂直向两块矩形环（尺寸 = 另两轴整跨），沿场轴扫过 → 动态、随姿态
		int rings = 2;
		for (int i = 0; i < rings; i++) {
			double off = (time * FLOW_SPEED + i * len / rings) % len;
			Vec3 base = e0.add(alongUnit.scale(off));
			Vec3 rc = base.add(sA.scale(0.5)).add(sB.scale(0.5));
			double hA = sA.length() / 2;
			double hB = sB.length() / 2;
			Vec3 c00 = rc.subtract(uA.scale(hA)).subtract(uB.scale(hB));
			Vec3 c10 = rc.add(uA.scale(hA)).subtract(uB.scale(hB));
			Vec3 c11 = rc.add(uA.scale(hA)).add(uB.scale(hB));
			Vec3 c01 = rc.subtract(uA.scale(hA)).add(uB.scale(hB));
			drawSegment(pose, quads, c00, c10, width, r, g, b, a * 0.9f);
			drawSegment(pose, quads, c10, c11, width, r, g, b, a * 0.9f);
			drawSegment(pose, quads, c11, c01, width, r, g, b, a * 0.9f);
			drawSegment(pose, quads, c01, c00, width, r, g, b, a * 0.9f);
		}
	}

	/** 任意方向的方截面粗线段（12 条棱 / 推进环共用；旋转用，非主轴专用）。 */
	private static void drawSegment(PoseStack.Pose pose, VertexConsumer quads, Vec3 from, Vec3 to, float width,
		float r, float g, float b, float a) {
		Vec3 axis = to.subtract(from);
		double len = axis.length();
		if (len < 1.0E-6)
			return;
		Vec3 u = axis.scale(1.0 / len);
		Vec3 ref = Math.abs(u.x) < 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
		Vec3 bVec = u.cross(ref).normalize();
		Vec3 cVec = u.cross(bVec).normalize();
		Vec3 mid = from.add(to).scale(0.5);
		double halfW = width / 2.0;
		drawOrientedBox(pose, quads, mid, u, bVec, cVec, len / 2.0, halfW, halfW, r, g, b, a);
	}

	/** 任意姿态方盒（6 面 QUADS；渲染类型 NO_CULL，无需在意绕序）。 */
	private static void drawOrientedBox(PoseStack.Pose pose, VertexConsumer quads, Vec3 center,
		Vec3 ax0, Vec3 ax1, Vec3 ax2, double h0, double h1, double h2, float r, float g, float b, float a) {
		Vec3[] units = { ax0, ax1, ax2 };
		double[] halfs = { h0, h1, h2 };
		for (int fi = 0; fi < 3; fi++) {
			for (int s = -1; s <= 1; s += 2) {
				int o1 = (fi + 1) % 3;
				int o2 = (fi + 2) % 3;
				Vec3 v0 = cornerOf(center, units, halfs, fi, s, o1, 1, o2, 1);
				Vec3 v1 = cornerOf(center, units, halfs, fi, s, o1, -1, o2, 1);
				Vec3 v2 = cornerOf(center, units, halfs, fi, s, o1, -1, o2, -1);
				Vec3 v3 = cornerOf(center, units, halfs, fi, s, o1, 1, o2, -1);
				quad(pose, quads, v0.x, v0.y, v0.z, v1.x, v1.y, v1.z, v2.x, v2.y, v2.z, v3.x, v3.y, v3.z, r, g, b, a);
			}
		}
	}

	/** 姿态方盒的一个角：fixed 轴取 sFixed，另两轴分别取 sa/sb（±1）。 */
	private static Vec3 cornerOf(Vec3 center, Vec3[] units, double[] halfs, int fixed, int sFixed, int a, int sa,
		int bIdx, int sb) {
		double[] signs = { 0, 0, 0 };
		signs[fixed] = sFixed;
		signs[a] = sa;
		signs[bIdx] = sb;
		return center.add(units[0].scale(signs[0] * halfs[0]))
			.add(units[1].scale(signs[1] * halfs[1]))
			.add(units[2].scale(signs[2] * halfs[2]));
	}

	/** 轴向：全场截面风片推进（两块错开，与整体框尺寸匹配）。 */
	private static void drawSlabFlow(PoseStack.Pose pose, VertexConsumer quads, AABB box, Vec3 dir, long time,
		float width, float r, float g, float b, float a) {
		int slabAxis = Math.abs(dir.x) > AXIS_EPS ? 0 : Math.abs(dir.y) > AXIS_EPS ? 1 : 2;
		double[] lo = { box.minX, box.minY, box.minZ };
		double[] hi = { box.maxX, box.maxY, box.maxZ };
		double span = hi[slabAxis] - lo[slabAxis];
		if (span < 0.6)
			return;

		double halfThick = 0.25;
		int sheets = 2;
		for (int i = 0; i < sheets; i++) {
			double offset = (time * FLOW_SPEED + i * span / sheets) % span;
			double[] sl = lo.clone();
			double[] sh = hi.clone();
			sl[slabAxis] = lo[slabAxis] + offset - halfThick;
			sh[slabAxis] = lo[slabAxis] + offset + halfThick;
			drawBoxEdges(pose, quads, new AABB(sl[0], sl[1], sl[2], sh[0], sh[1], sh[2]), width, r, g, b, a * 0.9f);
		}
	}

	/** 斜向：沿 direction 一列等距小方块推进。 */
	private static void drawChipFlow(PoseStack.Pose pose, VertexConsumer quads, AABB box, Vec3 dir, long time,
		float width, float r, float g, float b, float a) {
		Vec3 center = box.getCenter();
		double back = extentToFace(center, box, dir.scale(-1));
		double front = extentToFace(center, box, dir);
		double span = back + front;
		if (span < 1.0)
			return;
		int count = (int) Math.max(2, Math.min(4, span / 3));
		double side = Math.max(0.6, Math.min(1.2,
			Math.min(box.getXsize(), Math.min(box.getYsize(), box.getZsize())) * 0.18));
		Vec3 base = center.add(dir.scale(-back));
		for (int i = 0; i < count; i++) {
			double offset = (time * FLOW_SPEED + i * span / count) % span;
			Vec3 pos = base.add(dir.scale(offset));
			drawBoxEdges(pose, quads,
				new AABB(pos.x - side / 2, pos.y - side / 2, pos.z - side / 2,
					pos.x + side / 2, pos.y + side / 2, pos.z + side / 2),
				width, r, g, b, a * 0.9f);
		}
	}

	/** 把 AABB 的 12 条棱画成方截面的粗棱线（Create 风格，非 1px 线）。 */
	private static void drawBoxEdges(PoseStack.Pose pose, VertexConsumer quads, AABB box, float width,
		float r, float g, float b, float a) {
		double hw = width / 2;
		double minX = box.minX, minY = box.minY, minZ = box.minZ;
		double maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;
		// 沿 X 的 4 条棱
		cuboidAlong(pose, quads, minX, minY, minZ, maxX, minY, minZ, hw, hw, r, g, b, a);
		cuboidAlong(pose, quads, minX, minY, maxZ, maxX, minY, maxZ, hw, hw, r, g, b, a);
		cuboidAlong(pose, quads, minX, maxY, minZ, maxX, maxY, minZ, hw, hw, r, g, b, a);
		cuboidAlong(pose, quads, minX, maxY, maxZ, maxX, maxY, maxZ, hw, hw, r, g, b, a);
		// 沿 Y 的 4 条棱
		cuboidAlong(pose, quads, minX, minY, minZ, minX, maxY, minZ, hw, hw, r, g, b, a);
		cuboidAlong(pose, quads, maxX, minY, minZ, maxX, maxY, minZ, hw, hw, r, g, b, a);
		cuboidAlong(pose, quads, minX, minY, maxZ, minX, maxY, maxZ, hw, hw, r, g, b, a);
		cuboidAlong(pose, quads, maxX, minY, maxZ, maxX, maxY, maxZ, hw, hw, r, g, b, a);
		// 沿 Z 的 4 条棱
		cuboidAlong(pose, quads, minX, minY, minZ, minX, minY, maxZ, hw, hw, r, g, b, a);
		cuboidAlong(pose, quads, maxX, minY, minZ, maxX, minY, maxZ, hw, hw, r, g, b, a);
		cuboidAlong(pose, quads, minX, maxY, minZ, minX, maxY, maxZ, hw, hw, r, g, b, a);
		cuboidAlong(pose, quads, maxX, maxY, minZ, maxX, maxY, maxZ, hw, hw, r, g, b, a);
	}

	/** 沿主轴的棱 → 方截面小长方体（QUADS 六面）。dyHalf/dzHalf 为垂直截面半宽。 */
	private static void cuboidAlong(PoseStack.Pose pose, VertexConsumer quads,
		double x1, double y1, double z1, double x2, double y2, double z2,
		double dyHalf, double dzHalf, float r, float g, float b, float a) {
		// 判断主轴
		int axis = Math.abs(x2 - x1) > 1e-9 ? 0 : Math.abs(y2 - y1) > 1e-9 ? 1 : 2;
		double hx = 0, hy = 0, hz = 0;
		if (axis == 0) { hx = 0; hy = dyHalf; hz = dzHalf; }
		else if (axis == 1) { hx = dyHalf; hy = 0; hz = dzHalf; }
		else { hx = dyHalf; hy = dzHalf; hz = 0; }

		double loX = Math.min(x1, x2) - hx, hiX = Math.max(x1, x2) + hx;
		double loY = Math.min(y1, y2) - hy, hiY = Math.max(y1, y2) + hy;
		double loZ = Math.min(z1, z2) - hz, hiZ = Math.max(z1, z2) + hz;
		cuboid(pose, quads, loX, loY, loZ, hiX, hiY, hiZ, r, g, b, a);
	}

	/** 输出一个 AABB 的六面（QUADS，NO_CULL，全部朝向画）。 */
	private static void cuboid(PoseStack.Pose pose, VertexConsumer quads,
		double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
		float r, float g, float b, float a) {
		// -X
		quad(pose, quads, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
		// +X
		quad(pose, quads, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);
		// -Y
		quad(pose, quads, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
		// +Y
		quad(pose, quads, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);
		// -Z
		quad(pose, quads, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a);
		// +Z
		quad(pose, quads, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer quads,
		double x0, double y0, double z0, double x1, double y1, double z1,
		double x2, double y2, double z2, double x3, double y3, double z3,
		float r, float g, float b, float a) {
		quads.addVertex(pose, (float) x0, (float) y0, (float) z0).setColor(r, g, b, a);
		quads.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a);
		quads.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a);
		quads.addVertex(pose, (float) x3, (float) y3, (float) z3).setColor(r, g, b, a);
	}

	// ========== 几何工具 ==========

	/** 视线射线与 AABB 的最近命中距离；未命中返回 -1。 */
	private static double rayHitDistance(AABB box, Vec3 o, Vec3 d) {
		double tMin = 0, tMax = AIM_RANGE;
		double[] ori = { o.x, o.y, o.z };
		double[] dir = { d.x, d.y, d.z };
		double[] lo = { box.minX, box.minY, box.minZ };
		double[] hi = { box.maxX, box.maxY, box.maxZ };
		for (int i = 0; i < 3; i++) {
			double di = dir[i];
			if (Math.abs(di) < 1.0E-6) {
				if (ori[i] < lo[i] || ori[i] > hi[i])
					return -1;
				continue;
			}
			double t1 = (lo[i] - ori[i]) / di;
			double t2 = (hi[i] - ori[i]) / di;
			if (t1 > t2) {
				double tmp = t1;
				t1 = t2;
				t2 = tmp;
			}
			tMin = Math.max(tMin, t1);
			tMax = Math.min(tMax, t2);
			if (tMin > tMax)
				return -1;
		}
		return tMin >= 0 ? tMin : -1;
	}

	/** 从 center 沿 unitDir 到 AABB 面的距离。 */
	private static double extentToFace(Vec3 center, AABB box, Vec3 unitDir) {
		double best = Double.MAX_VALUE;
		double[] c = { center.x, center.y, center.z };
		double[] d = { unitDir.x, unitDir.y, unitDir.z };
		double[] lo = { box.minX, box.minY, box.minZ };
		double[] hi = { box.maxX, box.maxY, box.maxZ };
		for (int i = 0; i < 3; i++) {
			if (Math.abs(d[i]) < 1.0E-6)
				continue;
			double t = d[i] > 0 ? (hi[i] - c[i]) / d[i] : (lo[i] - c[i]) / d[i];
			if (t > 0 && t < best)
				best = t;
		}
		return best == Double.MAX_VALUE ? 1.0 : best;
	}

	private static Object fieldKey(EnergyField field) {
		// 优先稳定键：真实/结构场都带 source（controller:… / structure:…），结构移动不换 key，
		// 淡入淡出不抖动；仅调试命令建的场无 source → 退回几何键。
		if (field.source() != null)
			return field.source();
		AABB r = field.region();
		return "field_" + field.type() + "_" + (long) (r.minX * 16) + "_"
			+ (long) (r.minY * 16) + "_" + (long) (r.minZ * 16) + "_" + (long) (r.maxX * 16) + "_"
			+ (long) (r.maxY * 16) + "_" + (long) (r.maxZ * 16);
	}
}
