package com.hjmmd_8.createoreexpansion.compat.optical;

import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.StellarWaveMachineCatalog;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.StellarWaveMachineRegistry;

import net.neoforged.fml.ModList;

/**
 * Create Optical（机械动力：光学，modid {@code create_optical}）与星辉波变器的联动注册。
 *
 * <p>把聚光器（{@code BeamFocuser}，唯一执行聚焦配方的加工机，kinetic）登记为加工机
 * 并挂配方类型 {@code create_optical:focusing}——配方条目自带 4 光频条件
 * （RADIO/MICROWAVE/VISIBLE/GAMMA），命中引擎按条目匹配（波视为具备全部光频）。</p>
 *
 * <p><b>统一目录</b>：经 {@link StellarWaveMachineCatalog#defer} 追加进集中目录；
 * 本类只保留 ModList 守卫（未安装该 mod 时不加载任何 Optical 类）。</p>
 */
public final class OpticalMachineIntegration {

	private static boolean attempted;

	private OpticalMachineIntegration() {
	}

	/** 注册光学加工机及其配方类型（幂等；未安装 mod 时静默返回）。 */
	public static void ensureRegistered() {
		if (attempted)
			return;
		attempted = true;
		if (ModList.get() == null || !ModList.get().isLoaded("create_optical"))
			return;
		try {
			StellarWaveMachineCatalog.defer(OpticalMachineIntegration::registerMachine);
		} catch (Throwable ignored) {
			// 类缺失/注册时序异常：静默跳过（波照常工作，仅少识别一台机器）
		}
	}

	/** 实际登记语句（目录追加执行时运行；此时已确认 Optical 已加载）。 */
	private static void registerMachine() {
		StellarWaveMachineRegistry.register(
			net.lpcamors.optical.blocks.COBlocks.BEAM_FOCUSER.get())
			.addTypes(net.lpcamors.optical.CORecipeTypes.FOCUSING)
			.register();
	}
}
