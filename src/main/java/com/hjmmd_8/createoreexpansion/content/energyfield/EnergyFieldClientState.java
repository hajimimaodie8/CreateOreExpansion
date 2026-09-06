package com.hjmmd_8.createoreexpansion.content.energyfield;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * 客户端侧能量场镜像（护目镜场域指示的数据源）。
 *
 * <p>服务端 {@link EnergyFields} 的场不会自动出现在客户端 —— 渲染端（Create Outliner
 * 指示框）每 tick 从这里读取"当前维度有哪些场"。镜像由 {@link EnergyFieldSyncPayload}
 * 在服务端场集合变化（建场/清场/登录补发）时全量刷新。</p>
 *
 * <p>仅在物理客户端使用（渲染端 Dist.CLIENT 事件驱动）。</p>
 */
public final class EnergyFieldClientState {

	/** 维度 → 场列表（全量镜像，每次收包整体替换）。 */
	private static final Map<ResourceLocation, List<EnergyField>> BY_DIM = new ConcurrentHashMap<>();

	private EnergyFieldClientState() {
	}

	/** 收包刷新：整维替换。 */
	public static void receive(ResourceLocation dimension, List<EnergyField> fields) {
		BY_DIM.put(dimension, List.copyOf(fields));
	}

	/** 某维度当前已知场（渲染端读取；无则空表）。 */
	public static List<EnergyField> in(Level level) {
		return BY_DIM.getOrDefault(level.dimension().location(), List.of());
	}

	/** 清空全部（客户端退出世界/重进维度时调用，防残留）。 */
	public static void clearAll() {
		BY_DIM.clear();
	}

	/** 调试用：当前镜像内场总数。 */
	public static int totalCount() {
		int n = 0;
		for (List<EnergyField> list : new ArrayList<>(BY_DIM.values()))
			n += list.size();
		return n;
	}
}
