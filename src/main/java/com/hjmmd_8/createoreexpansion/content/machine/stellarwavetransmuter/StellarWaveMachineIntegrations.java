package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.compat.createaddition.CreateAdditionTransmuterSupport;
import com.hjmmd_8.createoreexpansion.compat.optical.OpticalMachineIntegration;
import com.hjmmd_8.createoreexpansion.compat.vintageimprovements.VintageImprovementsMachineIntegration;
import com.hjmmd_8.createoreexpansion.compat.vintageimprovements.VintageRecipeEnergy;
import com.hjmmd_8.createoreexpansion.compat.vintageimprovements.VintageRecipeSpeed;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

/**
 * 星辉波变器"加工机注册表"的联动汇总：统一入口，把各可选 mod 的加工机登记进
 * {@code registry.StellarWaveMachineRegistry}（未安装对应 mod 时对应模块静默跳过）。
 *
 * <p>幂等（各模块内部保证只尝试一次）；在变器 BE 首次扫描时调用。</p>
 *
 * <p>同时承载<b>载荷侧联动入口</b>（本包唯一对外门面，避免 content 直接引用第三方类）：
 * <ul>
 *   <li>{@link #drainTeslaCoilFully(Level, BlockPos)}——CC&amp;A 特斯拉线圈全抽（载荷电量源）；</li>
 *   <li>{@link #recipeEnergyRequired(Recipe)}——配方条目的 FE 电量需求（CC&amp;A charging /
 *       Vintage 激光 energy 条目）；</li>
 *   <li>{@link #energyExtraRecipeTypes()}——携带电量波可追加执行的耗电配方类型（CCA charging）。</li>
 * </ul>
 * 各方法内部 try/catch + 各 compat 模块 ModList 守卫：未安装对应 mod 时静默返回空/0。</p>
 */
public final class StellarWaveMachineIntegrations {

	private StellarWaveMachineIntegrations() {
	}

	/** 注册全部加工机档案（Create 原生目录 + 各可选 mod 追加；幂等）。
	 *  各可选 mod 的 {@code ensureRegistered} 在 ModList 守卫内经 {@code Catalog.defer}
	 *  把注册语句追加进集中目录；唤醒后统一 {@code init()} 落表。 */
	public static void ensureRegistered() {
		if (integrationsAttempted)
			return;
		integrationsAttempted = true;
		OpticalMachineIntegration.ensureRegistered();
		VintageImprovementsMachineIntegration.ensureRegistered();
		CreateAdditionTransmuterSupport.registerMachinesIntoCatalog();
		// 后续：其它可选 mod 按需在此追加（各自 ensureRegistered 内 defer 即可）
		com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.StellarWaveMachineCatalog.init();
	}

	private static boolean integrationsAttempted;

	/** 抽干特斯拉线圈内部电量（CC&amp;A 全抽；未安装/非线圈返回 0）。 */
	public static int drainTeslaCoilFully(Level level, BlockPos pos) {
		try {
			return CreateAdditionTransmuterSupport.drainTeslaCoilFully(level, pos);
		} catch (Throwable ignored) {
			return 0;
		}
	}

	/** <b>按需</b>抽特斯拉线圈内部电量至多 {@code max} FE，返回实抽量（未安装/非线圈返回 0）。
	 *  变体波"电量双通道"的邻域侧扣电入口（穿波载荷阶段仍用 {@link #drainTeslaCoilFully} 全抽）。 */
	public static int consumeTeslaCoil(Level level, BlockPos pos, int max) {
		try {
			return CreateAdditionTransmuterSupport.consumeTeslaCoil(level, pos, max);
		} catch (Throwable ignored) {
			return 0;
		}
	}

	/** <b>只读</b>读特斯拉线圈内部电量（FE；未安装/非线圈返回 0）——电量门槛估算用，绝不动其储能。 */
	public static int teslaCoilEnergy(Level level, BlockPos pos) {
		try {
			return CreateAdditionTransmuterSupport.teslaCoilEnergy(level, pos);
		} catch (Throwable ignored) {
			return 0;
		}
	}

	/** 配方条目的电量需求（FE；CC&amp;A charging / Vintage 激光 energy，未安装相关 mod 返回 0）。 */
	public static int recipeEnergyRequired(Recipe<?> recipe) {
		int required = 0;
		try {
			required += CreateAdditionTransmuterSupport.energyRequiredOf(recipe);
		} catch (Throwable ignored) {
			// 单个联动异常不影响其它联动
		}
		try {
			required += VintageRecipeEnergy.energyRequiredOf(recipe);
		} catch (Throwable ignored) {
			// 单个联动异常不影响其它联动
		}
		return Math.max(0, required);
	}

	/**
	 * 配方条目的<b>转速档要求</b>（Vintage 砂带打磨机 {@code speed_limits}：1 低 / 2 中 / 3 高；
	 * 0 = 无要求 / 非该类配方 / 未安装 Vintage）。
	 *
	 * <p>用途是<b>候选排序优先级</b>而非硬门槛——Vintage 自身也是"档位匹配的优先进优选桶，
	 * 一条都不匹配才退化执行"（见 {@link VintageRecipeSpeed}）；变体波用携带的
	 * <b>变器转速档</b>与之比对。</p>
	 */
	public static int recipeSpeedMode(Recipe<?> recipe) {
		try {
			return VintageRecipeSpeed.requiredSpeedMode(recipe);
		} catch (Throwable ignored) {
			return 0; // Vintage 缺失：按"无转速要求"处理
		}
	}

	/** 按 RPM 计算 Vintage 转速档（与 {@code GrinderBlockEntity#getCurrentSpeedMode()} 同口径同配置）。 */
	public static int speedModeFor(float rpm) {
		try {
			return VintageRecipeSpeed.speedModeOf(rpm);
		} catch (Throwable ignored) {
			return 0;
		}
	}

	/** 携带电量的波可追加执行的耗电配方类型（CC&amp;A charging；未安装/未带电返回空表）。 */
	public static List<IRecipeTypeInfo> energyExtraRecipeTypes(boolean payloadHasEnergy) {
		List<IRecipeTypeInfo> out = new ArrayList<>();
		try {
			out.addAll(CreateAdditionTransmuterSupport.extraEnergyRecipeTypes(payloadHasEnergy));
		} catch (Throwable ignored) {
			// CC&amp;A 缺失：不追加任何类型
		}
		return out;
	}

	/**
	 * <b>CC&amp;A 全部充电配方里最大的那条的耗电量（FE）</b>——变体波"电量载荷上限"的数据来源。
	 *
	 * <p>用户 2026-09 口径：波的电量载荷上限 = CC&amp;A 充电时消耗电量的所有配方的最大值
	 * （够跑最贵的那条即可，不再把四周储能抽干）。未安装 CC&amp;A / 无此类配方 → 0，
	 * 调用方据此回退到"不设上限"。</p>
	 */
	public static int maxStrikeChargingEnergyFe(net.minecraft.world.level.Level level) {
		try {
			return CreateAdditionTransmuterSupport.maxChargingEnergy(level);
		} catch (Throwable ignored) {
			return 0;
		}
	}

	/**
	 * <b>"雷击落地统一加工也会顺带执行"的配方类型 id</b>（CC&amp;A 的 charging）。
	 *
	 * <p><b>先把口径摆正（用户 2026-09 明确）</b>：CC&amp;A 的充电（{@code createaddition:charging}）
	 * 与本模组的雷电加工（{@code createoreexpansion:lightning} / {@code lightning_block}）是
	 * <b>两码事</b>——两套独立机制、两个不同配方类型，不要混为一谈。
	 * 本模组的充电加工（应力充能器 / 星辉波变器，{@code createoreexpansion:charging}）
	 * <b>可以执行 CC&amp;A 的全部充电配方</b>（见 {@link #energyExtraRecipeTypes} 与
	 * {@code ChargingRecipeAssemblyMixin}），所以玩家不必依赖特斯拉线圈。</p>
	 *
	 * <p><b>那为什么还要这个表</b>：{@code LightningEventHandler} 在落点除了本模组的 lightning 配方，
	 * 还会<b>兼容性地</b>执行 CC&amp;A 充电配方；而携带电量的波自己也能执行它们。波在命中点
	 * <b>引了一道雷</b>时，这些类型就交给那道雷，本波从类型门里摘掉，避免同一件物品被加工两遍
	 * （用户 2026-09 指出的重复执行问题）。未安装 CC&amp;A 时返回空表。</p>
	 */
	public static java.util.Set<net.minecraft.resources.ResourceLocation> strikeHandledTypeIds() {
		try {
			return CreateAdditionTransmuterSupport.strikeHandledTypeIds();
		} catch (Throwable ignored) {
			return java.util.Set.of();
		}
	}
}
