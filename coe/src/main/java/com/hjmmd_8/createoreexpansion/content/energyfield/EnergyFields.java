package com.hjmmd_8.createoreexpansion.content.energyfield;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.hjmmd_8.createoreexpansion.content.wave.bridge.SableBridges;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 能量场注册表（每维度激活场的集中管理，纯服务端逻辑）。
 *
 * <p>匀强场由方块实体（如蓝宝石场域控制器）在 tick 时<b>注册/刷新</b>：
 * 注册新场、或按维度移除过期场。能量场的应用器按维度查询覆盖某点的所有场，
 * 逐个对带电实体施加速度修正。</p>
 *
 * <p>设计为"每 tick 全量刷新"而非增量事件：方块少、场少（典型场景 1~几十块），
 * 简单可靠；若未来规模增大再改空间索引。</p>
 */
public final class EnergyFields {

	/** 维度 key → 该维度当前激活场列表。 */
	private static final Map<String, List<EnergyField>> BY_DIM = new ConcurrentHashMap<>();

	private EnergyFields() {
	}

	/** 把场加入指定维度（重复注册同引用则忽略）。同步写入维度存档（退出重进保留）。 */
	public static void add(Level level, EnergyField field) {
		List<EnergyField> list = BY_DIM.computeIfAbsent(level.dimension()
			.location()
			.toString(), k -> new ArrayList<>());
		if (list.contains(field))
			return;
		list.add(field);
		persist(level);
	}

	/** 从指定维度移除某场（按值相等匹配；不存在则无操作）。 */
	public static void remove(Level level, EnergyField field) {
		if (field == null)
			return;
		List<EnergyField> list = BY_DIM.get(level.dimension()
			.location()
			.toString());
		if (list != null && list.remove(field))
			persist(level);
	}

	/**
	 * 控制器按 tick 的幂等刷新：把"上一个由本机产出的场"替换为"当前应产出的场"。
	 *
	 * <p>配对成立的机器每 tick 以相同几何调用（场的值相等 → 无变化时不改动、不广播）；
	 * 配对失效的机器调用 {@code replace(null, 上一场)} 把旧场移除。变化发生时落盘并
	 * 向客户端广播（护目镜场域指示随之增/删/变色）。</p>
	 *
	 * @param current  当前应保持的场；null = 本机当前不产场
	 * @param previous 上一 tick 本机产出的场（可能 null）
	 */
	public static void replace(Level level, EnergyField current, EnergyField previous) {
		if (current == null && previous == null)
			return;
		boolean changed = false;
		if (previous != null && !previous.equals(current)) {
			List<EnergyField> list = BY_DIM.get(level.dimension()
				.location()
				.toString());
			if (list != null && list.remove(previous))
				changed = true;
		}
		if (current != null) {
			List<EnergyField> list = BY_DIM.computeIfAbsent(level.dimension()
				.location()
				.toString(), k -> new ArrayList<>());
			if (!list.contains(current)) {
				list.add(current);
				changed = true;
			}
		}
		if (!changed)
			return;
		persist(level);
		if (level instanceof ServerLevel server) {
			if (EnergyFields.count(server) == 0)
				EnergyFieldSyncPayload.broadcastClear(server);
			else
				EnergyFieldSyncPayload.broadcastToDimension(server);
		}
	}

	/** 清空某维度的全部场（方块卸载/维度保存时调用）。同步清空维度存档。 */
	public static void clear(Level level) {
		BY_DIM.remove(level.dimension()
			.location()
			.toString());
		persist(level);
	}

	/** 场表变化后把该维度内存状态落盘（仅服务端有存档）。 */
	private static void persist(Level level) {
		if (level instanceof net.minecraft.server.level.ServerLevel server) {
			EnergyFieldSavedData.get(server)
				.saveFrom(server);
		}
	}

	/**
	 * 世界加载时调用：把维度存档中的场重新灌回内存表。
	 * 游戏退出重进后，调试命令建的场经此恢复。
	 */
	public static void restore(net.minecraft.server.level.ServerLevel level) {
		String key = level.dimension()
			.location()
			.toString();
		EnergyFieldSavedData data = EnergyFieldSavedData.get(level);
		BY_DIM.put(key, new ArrayList<>(data.getFields()));
	}

	/** 查询某维度当前注册的场数量（诊断日志用）。 */
	public static int count(Level level) {
		List<EnergyField> fields = BY_DIM.get(level.dimension()
			.location()
			.toString());
		return fields == null ? 0 : fields.size();
	}

	/** 返回某维度全部场的只读快照（诊断/可视化用）。 */
	public static List<EnergyField> in(Level level) {
		List<EnergyField> fields = BY_DIM.get(level.dimension()
			.location()
			.toString());
		return fields == null ? java.util.List.of() : java.util.List.copyOf(fields);
	}

	/** 查询某点是否被该维度任一场覆盖（诊断日志用）。 */
	public static boolean isInAnyField(Level level, Vec3 pos) {
		List<EnergyField> fields = BY_DIM.get(level.dimension()
			.location()
			.toString());
		if (fields == null)
			return false;
		for (EnergyField field : fields) {
			if (field.contains(pos))
				return true;
		}
		return false;
	}

	/**
	 * 对指定维度某点执行一次场作用：把该点覆盖的所有场依次作用于带电实体的速度。
	 *
	 * <p><b>物理结构（Sable sub-level）适配</b>：带电实体（能量波）位于某物理结构内时，
	 * 优先用<b>该结构自身的场</b>（plot 本地坐标/方向，结构随位姿运动自然跟随）；
	 * 否则用所在主世界维度的场。结构↔真实场互不可见（控制器只在同侧配对产场）。</p>
	 *
	 * @param entity 带电实体（实现 {@link FieldedEntity}）
	 * @param level  实体所在世界
	 * @return 修正后的速度（格/秒，世界坐标）；无场或实体不带电则返回原速度
	 */
	public static Vec3 applyFields(Level level, FieldedEntity entity) {
		ChargePolarity charge = entity.getChargePolarity();
		if (charge == null)
			return entity.fieldVelocity();
		Vec3 pos = entity.fieldPosition();
		Vec3 vel = entity.fieldVelocity();

		// 1) 物理结构（sub-level）：波在主世界飞行，无需"压到结构实体方块"——对每个含场的
		//    结构把波的世界坐标反算成本地坐标，落入该结构任一本地场区域即作用（结构场随位姿运动）。
		SubLevelBridge bridge = SableBridges.get();
		if (bridge != null && bridge.isActive() && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
			for (Object sub : bridge.subLevels(serverLevel)) {
				List<EnergyField> subFields = BY_SUB.get(sub);
				if (subFields == null || subFields.isEmpty())
					continue;
				SubLevelBridge.Hit hit = new SubLevelBridge.Hit(sub);
				try {
					Vec3 local = bridge.toLocal(hit, pos);
					Vec3 localVel = null;
					boolean hitAny = false;
					for (EnergyField field : subFields) {
						if (field.contains(local)) {
							hitAny = true;
							localVel = localVel == null ? bridge.toLocalDir(hit, vel) : localVel;
							localVel = field.apply(localVel, charge, 1.0);
						}
					}
					if (hitAny)
						return bridge.toWorldDir(hit, localVel);
				} catch (Throwable ignored) {
					// 结构已移除：忽略
				}
			}
		}

		// 2) 真实世界：主世界维度注册的场
		List<EnergyField> fields = BY_DIM.get(level.dimension()
			.location()
			.toString());
		if (fields == null || fields.isEmpty())
			return vel;
		for (EnergyField field : fields) {
			if (field.contains(pos)) {
				vel = field.apply(vel, charge, 1.0);
			}
		}
		return vel;
	}

	// ========== 物理结构（Sable sub-level）上的场（plot 本地坐标，瞬态不入档） ==========

	/** 结构对象(sub-level) → 该结构上当前激活场列表（本地坐标）。 */
	private static final Map<Object, List<EnergyField>> BY_SUB = new ConcurrentHashMap<>();

	/** 控制器按 tick 的幂等刷新（结构场）：键 = sub-level 对象，坐标 = plot 本地。 */
	public static void replaceSub(Object subLevel, EnergyField current, EnergyField previous) {
		if (subLevel == null) {
			// 兜底：无结构句柄按真实场处理（正常不会走到）
			return;
		}
		if (current == null && previous == null)
			return;
		if (previous != null && !previous.equals(current)) {
			List<EnergyField> list = BY_SUB.get(subLevel);
			if (list != null && list.remove(previous)) {
				if (list.isEmpty())
					BY_SUB.remove(subLevel);
			}
		}
		if (current != null) {
			List<EnergyField> list = BY_SUB.computeIfAbsent(subLevel, k -> new ArrayList<>());
			if (!list.contains(current))
				list.add(current);
		}
		// 结构场随结构/机器存在而再生：不入档、不广播客户端（护目镜指示仅真实世界场）
	}

	/** 某结构上当前注册的场（只读快照，诊断用）。 */
	public static List<EnergyField> inSub(Object subLevel) {
		List<EnergyField> fields = BY_SUB.get(subLevel);
		return fields == null ? java.util.List.of() : java.util.List.copyOf(fields);
	}

	/** 某结构上场的数量（诊断用）。 */
	public static int countSub(Object subLevel) {
		List<EnergyField> fields = BY_SUB.get(subLevel);
		return fields == null ? 0 : fields.size();
	}

	/** 移除某结构对应的全部场（结构卸载时调用；目前结构卸载随机器 invalidate 自然清空）。 */
	public static void clearSub(Object subLevel) {
		BY_SUB.remove(subLevel);
	}

	/** 当前注册了场的全部结构句柄（服务端结构场世界同步调度用）。 */
	public static List<Object> structureHosts() {
		return new ArrayList<>(BY_SUB.keySet());
	}

	/**
	 * 把某结构的全部本地场换算成<b>世界坐标视图</b>（客户端渲染用）：
	 * 本地 AABB 的 8 个角经位姿变换为世界角点（索引按
	 * {@code (xHigh?4:0)|(yHigh?2:0)|(zHigh?1:0)} 位序，渲染端按位邻接连 12 条棱 →
	 * <b>框随结构姿态旋转</b>）；方向随位姿旋转；source 加 {@code structure:} 前缀作稳定键。
	 */
	public static List<EnergyField> structureWorldView(SubLevelBridge bridge, SubLevelBridge.Hit hit) {
		List<EnergyField> out = new ArrayList<>();
		List<EnergyField> local = BY_SUB.get(hit.subLevel());
		if (local == null)
			return out;
		for (EnergyField f : local) {
			AABB r = f.region();
			double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
			double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
			double[] frame = new double[24];
			for (int k = 0; k < 8; k++) {
				double x = (k & 4) != 0 ? r.maxX : r.minX;
				double y = (k & 2) != 0 ? r.maxY : r.minY;
				double z = (k & 1) != 0 ? r.maxZ : r.minZ;
				Vec3 w = bridge.toWorld(hit, new Vec3(x, y, z));
				frame[3 * k] = w.x;
				frame[3 * k + 1] = w.y;
				frame[3 * k + 2] = w.z;
				minX = Math.min(minX, w.x);
				minY = Math.min(minY, w.y);
				minZ = Math.min(minZ, w.z);
				maxX = Math.max(maxX, w.x);
				maxY = Math.max(maxY, w.y);
				maxZ = Math.max(maxZ, w.z);
			}
			Vec3 dirWorld = bridge.toWorldDir(hit, f.direction());
			String src = "structure:" + (f.source() == null ? "sub" : f.source());
			out.add(new EnergyField(f.type(), new AABB(minX, minY, minZ, maxX, maxY, maxZ),
				dirWorld, f.strength(), src, frame));
		}
		return out;
	}
}
