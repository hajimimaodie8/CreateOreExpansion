package com.hjmmd_8.createoreexpansion.content.wave.api;

import java.util.List;
import java.util.Set;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

/**
 * <b>波引擎（第一层）向"加工机联动"（第二层）要结果的唯一入口</b>（W6-a）。
 *
 * <p><b>它解决什么问题</b>：波引擎（{@code content/charger/**}、{@code content/wave/api/**}）
 * 是<b>第一层</b>（COE），加工机联动（{@code content/machine/stellarwavetransmuter/**} 的
 * {@code StellarWaveMachineIntegrations} / {@code registry.StellarWaveMachineRegistry} 与
 * {@code compat/createaddition/TeslaCoilWaveCharger}）是<b>第二层</b>（CEWS）。引擎件原先直接
 * 调它们的静态方法，机械搬运后那就是一条 {@code L1 → L2} 硬依赖（见
 * {@code build/patch/w6-restructure-PLAN.md} §2.2 的 E6–E12）。</p>
 *
 * <p><b>范式照抄本仓既有先例</b>：与 {@code common/registry/WaveRecipeCapabilities}（P3i 用它
 * 消掉 {@code CEWS → TRANS}）完全同形——把"读别人的类"改成"<b>谁有谁登记</b>"：</p>
 * <ul>
 *   <li>本类只持有一个 {@link Sink} 引用，<b>零编译期类型引用</b>到第二层；</li>
 *   <li>第二层在自己的 {@code @Mod} 构造器里 {@link #install(Sink)} 一份实现
 *       （方向恒为「L2 → 本表」，合法方向）；</li>
 *   <li>本类对第一层暴露的静态方法与原先第二层门面的方法<b>逐字同名同签名</b>，因此调用点
 *       只改类名不改形状（行为零变化）。</li>
 * </ul>
 *
 * <p><b>未装第二层时</b>：全部方法返回"空/0/false/什么都不做"——这正是
 * {@code WaveRecipeCapabilities} 已确立的降级口径（只装第一层 = 没有这些加工机联动，
 * 而不是崩）。第一层的调用点原本也各自 try/catch 兜底，这里不改变那层保护。</p>
 *
 * <p><b>W6-c 之后</b>：本类随 {@code content/wave/api/**} 整体进 {@code :coe}，第二层的实现类
 * 仍在 {@code :cews}，登记动作不变（方向仍是 L2 → L1）。</p>
 */
public final class WaveMachineIntegrationPoints {

	/**
	 * 第二层（CEWS）提供的联动实现。
	 *
	 * <p>方法名与签名与原 {@code StellarWaveMachineIntegrations} /
	 * {@code StellarWaveMachineRegistry} 保持一致，便于逐个比对。</p>
	 */
	public interface Sink {

		/** 注册全部加工机档案（幂等）。 */
		void ensureRegistered();

		/** 抽特斯拉线圈内部电量至多 {@code max} FE，返回实抽量。 */
		int consumeTeslaCoil(Level level, BlockPos pos, int max);

		/** CC&amp;A 全部充电配方里最大的一条的耗电量（FE）。 */
		int maxStrikeChargingEnergyFe(Level level);

		/** 只读特斯拉线圈内部电量（FE）。 */
		int teslaCoilEnergy(Level level, BlockPos pos);

		/** 配方条目的电量需求（FE）。 */
		int recipeEnergyRequired(Recipe<?> recipe);

		/** 配方条目的转速档要求（0 = 无要求）。 */
		int recipeSpeedMode(Recipe<?> recipe);

		/** 按 RPM 计算转速档。 */
		int speedModeFor(float rpm);

		/** 携带电量的波可追加执行的耗电配方类型。 */
		List<IRecipeTypeInfo> energyExtraRecipeTypes(boolean payloadHasEnergy);

		/** "雷击落地统一加工也会顺带执行"的配方类型 id。 */
		Set<ResourceLocation> strikeHandledTypeIds();

		/** 某台已登记加工机的配方类型（未登记返回空表）。 */
		List<IRecipeTypeInfo> typesFor(ResourceLocation machineId);

		/** 全部已登记加工机的配方类型。 */
		List<IRecipeTypeInfo> allRecipeTypes();

		/** 该位置是否已登记的加工机（启发式动能处理机也算）。 */
		boolean isMachinery(Level level, BlockPos pos);

		/** CC&amp;A 特斯拉线圈赋电荷（波不带电且贴近线圈时）。 */
		void chargeNearbyCoil(AbstractChargerWaveEntity wave);
	}

	/** 第二层登记进来的实现；{@code null} = 本环境没有第二层。 */
	private static volatile Sink sink;

	private WaveMachineIntegrationPoints() {
	}

	/**
	 * 登记第二层的实现（由第二层的 {@code @Mod} 构造器调用，方向 L2 → L1）。
	 *
	 * <p>幂等：重复登记只覆盖同一个引用（FML 的 mod 构造是并行派发的，登记动作不得有副作用）。</p>
	 */
	public static void install(Sink implementation) {
		if (implementation == null)
			throw new IllegalArgumentException("sink must not be null");
		sink = implementation;
	}

	/** 本环境是否装了第二层（诊断用；判定逻辑一律走下面的默认值）。 */
	public static boolean isInstalled() {
		return sink != null;
	}

	// ================= 第一层读取口（未装第二层时返回空/0/false） =================

	public static void ensureRegistered() {
		Sink s = sink;
		if (s != null)
			s.ensureRegistered();
	}

	public static int consumeTeslaCoil(Level level, BlockPos pos, int max) {
		Sink s = sink;
		return s == null ? 0 : s.consumeTeslaCoil(level, pos, max);
	}

	public static int maxStrikeChargingEnergyFe(Level level) {
		Sink s = sink;
		return s == null ? 0 : s.maxStrikeChargingEnergyFe(level);
	}

	public static int teslaCoilEnergy(Level level, BlockPos pos) {
		Sink s = sink;
		return s == null ? 0 : s.teslaCoilEnergy(level, pos);
	}

	public static int recipeEnergyRequired(Recipe<?> recipe) {
		Sink s = sink;
		return s == null ? 0 : s.recipeEnergyRequired(recipe);
	}

	public static int recipeSpeedMode(Recipe<?> recipe) {
		Sink s = sink;
		return s == null ? 0 : s.recipeSpeedMode(recipe);
	}

	public static int speedModeFor(float rpm) {
		Sink s = sink;
		return s == null ? 0 : s.speedModeFor(rpm);
	}

	public static List<IRecipeTypeInfo> energyExtraRecipeTypes(boolean payloadHasEnergy) {
		Sink s = sink;
		return s == null ? List.of() : s.energyExtraRecipeTypes(payloadHasEnergy);
	}

	public static Set<ResourceLocation> strikeHandledTypeIds() {
		Sink s = sink;
		return s == null ? Set.of() : s.strikeHandledTypeIds();
	}

	public static List<IRecipeTypeInfo> typesFor(ResourceLocation machineId) {
		Sink s = sink;
		return s == null ? List.of() : s.typesFor(machineId);
	}

	public static List<IRecipeTypeInfo> allRecipeTypes() {
		Sink s = sink;
		return s == null ? List.of() : s.allRecipeTypes();
	}

	public static boolean isMachinery(Level level, BlockPos pos) {
		Sink s = sink;
		return s != null && s.isMachinery(level, pos);
	}

	public static void chargeNearbyCoil(AbstractChargerWaveEntity wave) {
		Sink s = sink;
		if (s != null)
			s.chargeNearbyCoil(wave);
	}
}
