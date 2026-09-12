package com.hjmmd_8.createoreexpansion.compat.createaddition;

import java.util.List;

import com.mrh0.createaddition.blocks.tesla_coil.TeslaCoilBlockEntity;
import com.mrh0.createaddition.recipe.charging.ChargingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.fml.ModList;

/**
 * CC&amp;A（Create Crafts &amp; Additions）与<b>星辉波变器</b>的载荷侧联动（新文件，替换
 * 旧注释里"CCA 阶段接入"占位）。特斯拉线圈在变器扫描中不作为加工机，只作为<b>电量载荷源</b>。
 *
 * <p>职责：</p>
 * <ul>
 *   <li><b>全抽线圈电量</b>：特斯拉线圈是"纯输入"储能设备（对外 capability
 *       {@code canExtract=false}），只能走 CC&amp;A 内部 {@code internalConsumeEnergy}
 *       全抽（反射取 {@code AbstractElectricBlockEntity.localEnergy}，范式同
 *       {@link TeslaCoilWaveCharger}），抽到的 FE 由变器附着为变体波电量载荷；</li>
 *   <li><b>配方电量需求</b>：CC&amp;A {@code charging} 类放电配方条目（书→附魔书、
 *       除锈等）每条自带 {@code energy} FE 总耗——波命中此类条目时扣减携带电量；</li>
 *   <li><b>额外配方类型</b>：携带电量的变体波视作"飞行特斯拉线圈"，把 CC&amp;A
 *       charging 类型补进波的可执行类型（不占扫描快照，未带电时不补）。</li>
 * </ul>
 *
 * <p><b>完全可选集成</b>：所有方法第一行做 {@code ModList.isLoaded} 判断，CC&amp;A
 * 未安装时直接返回，不触碰任何 CC&amp;A 类（沿用 {@link TeslaCoilWaveCharger} 隔离范式）。</p>
 */
public final class CreateAdditionTransmuterSupport {

	/** 反射缓存：CC&amp;A 受保护字段 {@code localEnergy}（延迟初始化，未装 CC&amp;A 时不触碰）。 */
	private static volatile java.lang.reflect.Field localEnergyField;

	private CreateAdditionTransmuterSupport() {
	}

	private static boolean isLoaded() {
		return net.neoforged.fml.ModList.get() != null
			&& net.neoforged.fml.ModList.get().isLoaded("createaddition");
	}

	/**
	 * 登记 CC&amp;A 的动能加工机（轧机 Rolling Mill）进变器目录。
	 * 仅在 CC&amp;A 已加载时调用（由 {@code StellarWaveMachineIntegrations} 守卫后唤醒）；
	 * 注册语句经 {@code StellarWaveMachineCatalog.defer} 追加，执行走全库引擎（types 仅展示）。
	 */
	public static void registerMachinesIntoCatalog() {
		if (!isLoaded())
			return;
		try {
			com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.StellarWaveMachineCatalog
				.defer(() -> com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.StellarWaveMachineRegistry
					.register(com.mrh0.createaddition.index.CABlocks.ROLLING_MILL.get())
					.addTypes(com.mrh0.createaddition.recipe.rolling.RollingRecipe.TYPE_INFO)
					.register());
		} catch (Throwable ignored) {
			// CC&amp;A 类缺失/时序异常：跳过（全库引擎仍可执行 rolling 配方）
		}
	}

	/**
	 * 抽干一整台 CC&amp;A 特斯拉线圈的内部储能并返回实际抽到的 FE（0 = 未抽到）。
	 * 非线圈方块 / CC&amp;A 未安装 / 无电量时返回 0。
	 */
	public static int drainTeslaCoilFully(Level level, BlockPos pos) {
		return consumeTeslaCoil(level, pos, Integer.MAX_VALUE);
	}

	/**
	 * 从 CC&amp;A 特斯拉线圈内部储能<b>按需</b>抽取至多 {@code max} FE，返回实际抽到的量。
	 *
	 * <p>线圈对外 capability {@code canExtract=false}（{@code getMaxOut()==0}），只能走内部接口
	 * {@code internalConsumeEnergy(int)}。该方法字节码为
	 * {@code energy = max(0, energy - max)} 并返回新旧差值，即<b>自带钳制</b>——
	 * 故 {@code consumeTeslaCoil(level, pos, 1000)} 只扣 1000（存量不足则扣存量），
	 * 不会像"全抽"那样把线圈掏空。（{@code drainTeslaCoilFully} 即 {@code max = Integer.MAX_VALUE}
	 * 的特例，行为与旧实现一致。）</p>
	 *
	 * @return 实际抽到的 FE（非线圈 / 未安装 / 无电量 / 反射失败均为 0，静默不抛）
	 */
	public static int consumeTeslaCoil(Level level, BlockPos pos, int max) {
		if (level == null || pos == null || max <= 0 || !isLoaded())
			return 0;
		try {
			BlockEntity be = level.getBlockEntity(pos);
			if (!(be instanceof TeslaCoilBlockEntity))
				return 0;
			com.mrh0.createaddition.energy.InternalEnergyStorage internal = internalEnergyOf(be);
			if (internal == null)
				return 0;
			int drained = internal.internalConsumeEnergy(max); // 内部自带 max(0, stored−max) 钳制
			if (drained > 0)
				be.setChanged();
			return Math.max(0, drained);
		} catch (Throwable ignored) {
			// CC&amp;A 类缺失/反射失败：静默按未抽到处理
			return 0;
		}
	}

	/**
	 * <b>只读</b>读取 CC&amp;A 特斯拉线圈内部当前电量（FE）——供变体波"电量门槛估算"用，
	 * <b>绝不改动其储能</b>（估算阶段禁止调用 {@link #consumeTeslaCoil}/{@link #drainTeslaCoilFully}）。
	 * 非线圈方块 / 未安装 CC&amp;A / 反射失败均返回 0。
	 */
	public static int teslaCoilEnergy(Level level, BlockPos pos) {
		if (level == null || pos == null || !isLoaded())
			return 0;
		try {
			BlockEntity be = level.getBlockEntity(pos);
			if (!(be instanceof TeslaCoilBlockEntity))
				return 0;
			com.mrh0.createaddition.energy.InternalEnergyStorage internal = internalEnergyOf(be);
			return internal == null ? 0 : Math.max(0, internal.getEnergyStored());
		} catch (Throwable ignored) {
			return 0;
		}
	}

	/** 反射取 CC&amp;A 线圈内部能量存储（字段在 {@code AbstractElectricBlockEntity} 上）。 */
	private static com.mrh0.createaddition.energy.InternalEnergyStorage internalEnergyOf(BlockEntity be) {
		try {
			java.lang.reflect.Field f = localEnergyField;
			if (f == null) {
				f = com.mrh0.createaddition.energy.AbstractElectricBlockEntity.class.getDeclaredField("localEnergy");
				f.setAccessible(true);
				localEnergyField = f;
			}
			Object v = f.get(be);
			return v instanceof com.mrh0.createaddition.energy.InternalEnergyStorage s ? s : null;
		} catch (Throwable ignored) {
			return null;
		}
	}

	/**
	 * CC&amp;A charging 类配方的电量需求（FE）；未安装 CC&amp;A / 非该类配方返回 0。
	 */
	public static int energyRequiredOf(Recipe<?> recipe) {
		if (recipe == null || !isLoaded())
			return 0;
		try {
			if (recipe instanceof ChargingRecipe charging)
				return Math.max(0, charging.getEnergy());
		} catch (Throwable ignored) {
			// 类缺失等异常：按无电量需求处理
		}
		return 0;
	}

	/**
	 * 携带电量的变体波可执行的"额外配方类型"（CC&amp;A charging = 特斯拉线圈放电加工）。
	 * 仅在 CC&amp;A 安装且波确已携带 FE（&gt;0）时给出；否则空表——避免未带电的波
	 * 把普通物品误当"充电对象"逐条尝试 charging 配方。
	 */
	public static List<IRecipeTypeInfo> extraEnergyRecipeTypes(boolean payloadHasEnergy) {
		if (!payloadHasEnergy || !isLoaded())
			return List.of();
		try {
			return List.of(ChargingRecipe.TYPE_INFO);
		} catch (Throwable ignored) {
			return List.of();
		}
	}

	/**
	 * <b>CC&amp;A 全部充电配方里最大的那条的耗电量（FE）</b>——变体波的"电量载荷上限"就用它
	 * （用户 2026-09 口径：取电量的最大值改为 CC&amp;A 充电时消耗电量的所有配方的最大值，
	 * 而不是把四周储能抽干）。
	 *
	 * <p>未安装 CC&amp;A / 没有此类配方 / 读取异常 → 返回 0（调用方据此回退到"不设上限"）。</p>
	 */
	public static int maxChargingEnergy(Level level) {
		if (level == null || !isLoaded())
			return 0;
		try {
			int max = 0;
			var type = com.mrh0.createaddition.index.CARecipes.CHARGING_TYPE.get();
			for (net.minecraft.world.item.crafting.RecipeHolder<ChargingRecipe> holder : level.getRecipeManager()
				.getAllRecipesFor(type)) {
				ChargingRecipe charging = holder.value();
				if (charging != null)
					max = Math.max(max, Math.max(0, charging.getEnergy()));
			}
			return max;
		} catch (Throwable ignored) {
			return 0;
		}
	}

	/**
	 * "雷击落地统一加工也会顺带执行"的配方类型 id（CC&amp;A charging）——波引雷时据此类 id
	 * 把这些类型从自己的类型门里摘掉，保证同一件物品不会被"雷"和"波"各加工一遍。
	 * 未安装 CC&amp;A 时返回空集。
	 *
	 * <p><b>口径（用户 2026-09 明确，勿混淆）</b>：CC&amp;A 的充电（{@code createaddition:charging}）
	 * 与本模组的雷电加工（{@code createoreexpansion:lightning} / {@code lightning_block}）是<b>两码事</b>——
	 * 它们是两套独立机制、两个不同配方类型。这里之所以要"摘掉"，仅仅因为
	 * {@code LightningEventHandler} 在落点<b>顺带兼容执行</b> CC&amp;A 的充电配方，
	 * 会和同样带这个类型的波抢同一件物品。</p>
	 *
	 * <p><b>替代关系</b>：本模组的充电加工（应力充能器 / 星辉波变器，类型
	 * {@code createoreexpansion:charging}）<b>可以执行 CC&amp;A 的全部充电配方</b>
	 * （本类 {@link #extraEnergyRecipeTypes} 把 CC&amp;A charging 类型补进带电波；
	 * {@code ChargingRecipeAssemblyMixin} 让它能作为序列加工步骤），所以玩家不需要特斯拉线圈也能做这些加工。</p>
	 */
	public static java.util.Set<ResourceLocation> strikeHandledTypeIds() {
		if (!isLoaded())
			return java.util.Set.of();
		try {
			return java.util.Set.of(ChargingRecipe.TYPE_INFO.getId());
		} catch (Throwable ignored) {
			return java.util.Set.of();
		}
	}
}
