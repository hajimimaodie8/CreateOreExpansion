package com.hjmmd_8.createoreexpansion.content.charger.entity;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.content.wave.api.WaveMachineIntegrationPoints;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.resources.ResourceLocation;

/**
 * <b>变体波"携带到的加工能力 → 可执行配方类型集合"</b>（2026-10-06 行为零变化拆分，从
 * {@link StellarWaveEntity} 的 {@code activeRecipeTypes} / {@code allowedTypeIds} /
 * {@code recipeTypeById} / {@code waveSpeedMode} 四件<b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>这枚波携带的加工能力展开成哪些配方类型</b>——变器扫描半径内
 * 机器能力快照（含状态选择器裁剪，见 {@code StellarWaveTransmuterPass}）＋载荷电量带来的额外类型
 * （如 CC&amp;A charging），以及"本次命中已经引了一道雷"时把闪电类从类型门里摘掉。</p>
 *
 * <p>搬运口径：方法体逐字相同，差异只有两类 —— {@code private} → 包级私有、宿主字段被搬动
 * 逼出的 {@code host.} 限定（{@code recipeTypes} / {@code attributes} / {@code payloadEnergy} /
 * {@code strikeOwnsTypes} / {@code carriedRpm}，五个字段仍住在 {@link StellarWaveEntity}，
 * 只是由 {@code private} 放宽到包级私有）。<b>"快照优先、为空才按机器 id 展开"的顺序、
 * "电量 &gt; 0 才补额外类型"的门、异常按空表保守处理、以及转速档取值口径一个字未动。</b></p>
 */
final class WaveCarriedTypes {

	private WaveCarriedTypes() {
		throw new AssertionError("This class should not be instantiated");
	}

	/**
	 * 当前应执行的配方类型：优先扫描快照（含机器实时状态选择）；旧波无快照则按机器 id
	 * 展开静态档案。携带电量的波（{@code payloadEnergy > 0}）额外补入 CC&amp;A charging
	 * 等耗电配方类型（"飞行特斯拉线圈"角色，见 compat 联动）——各类配方条目的电量需求
	 * 仍由逐条候选检测另行门控（见 {@link StellarWaveEntity} 的远程加工路径）。
	 */
	static List<IRecipeTypeInfo> activeRecipeTypes(StellarWaveEntity host) {
		List<IRecipeTypeInfo> types = new ArrayList<>();
		if (!host.recipeTypes.isEmpty()) {
			types.addAll(host.recipeTypes);
		} else {
			for (ResourceLocation machineId : host.attributes)
				for (IRecipeTypeInfo t : WaveMachineIntegrationPoints.typesFor(machineId))
					if (!types.contains(t))
						types.add(t);
		}
		if (host.payloadEnergy > 0) {
			try {
				for (IRecipeTypeInfo extra : WaveMachineIntegrationPoints.energyExtraRecipeTypes(true))
					if (!types.contains(extra))
						types.add(extra);
			} catch (Throwable ignored) {
				// CC&amp;A 缺失等异常：不追加额外类型
			}
		}
		return types;
	}

	/**
	 * 波当前"携带到的"配方类型 id 集合（= 配方类型门的白名单）。
	 *
	 * <p>来源：{@link #activeRecipeTypes}——变器扫描半径内机器能力快照（含状态选择器裁剪）
	 * ＋载荷电量带来的额外类型（如 CC&amp;A 充电）。空集合 = 该波没有任何加工能力
	 * （退化波，根本不会走到候选检索）。</p>
	 */
	static java.util.Set<ResourceLocation> allowedTypeIds(StellarWaveEntity host) {
		java.util.Set<ResourceLocation> ids = new java.util.HashSet<>();
		try {
			for (IRecipeTypeInfo type : activeRecipeTypes(host)) {
				if (type != null && type.getId() != null)
					ids.add(type.getId());
			}
		} catch (Throwable ignored) {
			// 类型表读取异常：返回空表（保守：本 tick 不执行任何配方，而不是放行全库）
		}
		// 本次命中已经引了一道雷 → 闪电落地统一加工负责的配方类型（CC&A charging 等）从本波
		// 的类型门里摘掉：同一件物品只由"雷"加工一遍，不再由波再来一遍（用户 2026-09 指出的重复执行）。
		if (host.strikeOwnsTypes) {
			try {
				ids.removeAll(WaveMachineIntegrationPoints.strikeHandledTypeIds());
			} catch (Throwable ignored) {
				// 排除失败：按不排除处理（宁可留旧行为，也不要因异常吃掉整张类型表）
			}
		}
		return ids;
	}

	/** 按注册表 id 找回配方类型档案（读档恢复用；找不到的类型跳过，不影响其它字段）。 */
	static IRecipeTypeInfo recipeTypeById(ResourceLocation id) {
		for (IRecipeTypeInfo type : WaveMachineIntegrationPoints.allRecipeTypes())
			if (type != null && id.equals(type.getId()))
				return type;
		return null;
	}

	/** 波当前转速档（Vintage 口径：0 停转 / 1 低 / 2 中 / 3 高；未装 Vintage 返回 0 = 无档位概念）。 */
	static int waveSpeedMode(StellarWaveEntity host) {
		return WaveMachineIntegrationPoints.speedModeFor(host.carriedRpm);
	}
}
