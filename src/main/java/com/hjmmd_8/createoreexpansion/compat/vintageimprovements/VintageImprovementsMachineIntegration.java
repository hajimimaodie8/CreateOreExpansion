package com.hjmmd_8.createoreexpansion.compat.vintageimprovements;

import java.util.List;

import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.StellarWaveMachineCatalog;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.StellarWaveMachineRegistry;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.fml.ModList;

/**
 * Create: Vintage Improvements（modid {@code vintageimprovements}，卷簧机/辊压机/杠杆锤/
 * 离心机/真空室/车床/激光等）与星辉波变器的联动注册。
 *
 * <p>把该 mod 的<b>加工机主块</b>登记为变器加工机并挂各自配方类型
 * （均 Create ProcessingRecipe，枚举见 {@code VintageRecipes}）。结构块/装饰块不登记
 * （变器只收 KineticBlockEntity 计应力，结构块天然被过滤）。</p>
 *
 * <p><b>真空室（压缩机）双模式</b>：机器 {@code mode} 决定当前做加压还是抽真空——
 * 以 {@link com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.MachineStateSelector}
 * 挂在真空室档案上，变器扫描时按机器<b>实时 mode 状态</b>选 PRESSURIZING 或
 * VACUUMIZING 一套（mode 为包私有字段，反射读取；映射假设 mode=true=加压，
 * 若与实际相反仅需翻转此处一行）。</p>
 *
 * <p><b>杠杆锤（helve hammer）工作模式</b>：由<b>锤正下方的方块</b>决定（铁砧/自定义锤击方块 =
 * 锤击，锻造台 = 锻造，都没有 = 整机不加工），同样以状态选择器按实时状态裁剪类型——
 * 依据与判据见 {@link #helveHammerTypes}。</p>
 *
 * <p><b>统一目录</b>：注册语句通过 {@link StellarWaveMachineCatalog#defer} 追加进
 * 集中目录（ModList 守卫内调用，未安装该 mod 时本类不加载任何 Vintage 类）。</p>
 */
public final class VintageImprovementsMachineIntegration {

	private static boolean attempted;
	/** 真空室 mode 字段（包私有 boolean）。 */
	private static java.lang.reflect.Field vacuumModeField;

	private VintageImprovementsMachineIntegration() {
	}

	/** 注册 Vintage 加工机主块、配方类型与状态选择器（幂等；未安装 mod 时静默返回）。 */
	public static void ensureRegistered() {
		if (attempted)
			return;
		attempted = true;
		if (ModList.get() == null || !ModList.get().isLoaded("vintageimprovements"))
			return;
		try {
			// 静态引用放在 try 内：Vintage 类只在 ensureRegistered 被执行时才会被加载。
			StellarWaveMachineCatalog.defer(VintageImprovementsMachineIntegration::registerMachines);
		} catch (Throwable ignored) {
			// 类缺失/注册时序异常：静默跳过（波照常工作，仅少识别机器）
		}
	}

	/** 实际登记语句（目录追加执行时运行；此时已确认 Vintage 已加载）。 */
	private static void registerMachines() {
		StellarWaveMachineRegistry.register(
			com.negodya1.vintageimprovements.VintageBlocks.SPRING_COILING_MACHINE.get())
			.addTypes(com.negodya1.vintageimprovements.VintageRecipes.COILING)
			.register();
		StellarWaveMachineRegistry.register(
			com.negodya1.vintageimprovements.VintageBlocks.CURVING_PRESS.get())
			.addTypes(com.negodya1.vintageimprovements.VintageRecipes.CURVING)
			.register();
		StellarWaveMachineRegistry.register(
			com.negodya1.vintageimprovements.VintageBlocks.BELT_GRINDER.get())
			.addTypes(com.negodya1.vintageimprovements.VintageRecipes.POLISHING)
			.register();
		StellarWaveMachineRegistry.register(
			com.negodya1.vintageimprovements.VintageBlocks.CENTRIFUGE.get())
			.addTypes(com.negodya1.vintageimprovements.VintageRecipes.CENTRIFUGATION)
			.register();
		StellarWaveMachineRegistry.register(
			com.negodya1.vintageimprovements.VintageBlocks.VIBRATING_TABLE.get())
			.addTypes(com.negodya1.vintageimprovements.VintageRecipes.VIBRATING,
				com.negodya1.vintageimprovements.VintageRecipes.LEAVES_VIBRATING)
			.register();
		// 真空室：静态挂加压/抽真空两套，选择器按实时 mode 二选一
		StellarWaveMachineRegistry.register(
			com.negodya1.vintageimprovements.VintageBlocks.VACUUM_CHAMBER.get())
			.addTypes(com.negodya1.vintageimprovements.VintageRecipes.PRESSURIZING,
				com.negodya1.vintageimprovements.VintageRecipes.VACUUMIZING)
			.withSelector(VintageImprovementsMachineIntegration::vacuumChamberTypes)
			.register();
		StellarWaveMachineRegistry.register(
			com.negodya1.vintageimprovements.VintageBlocks.HELVE.get())
			.addTypes(com.negodya1.vintageimprovements.VintageRecipes.HAMMERING,
				com.negodya1.vintageimprovements.VintageRecipes.AUTO_SMITHING,
				com.negodya1.vintageimprovements.VintageRecipes.AUTO_UPGRADE)
			.withSelector(VintageImprovementsMachineIntegration::helveHammerTypes)
			.register();
		StellarWaveMachineRegistry.register(
			com.negodya1.vintageimprovements.VintageBlocks.HELVE_KINETIC.get())
			.addTypes(com.negodya1.vintageimprovements.VintageRecipes.HAMMERING,
				com.negodya1.vintageimprovements.VintageRecipes.AUTO_SMITHING,
				com.negodya1.vintageimprovements.VintageRecipes.AUTO_UPGRADE)
			.withSelector(VintageImprovementsMachineIntegration::helveHammerTypes)
			.register();
		StellarWaveMachineRegistry.register(
			com.negodya1.vintageimprovements.VintageBlocks.LATHE_ROTATING.get())
			.addTypes(com.negodya1.vintageimprovements.VintageRecipes.TURNING)
			.register();
		StellarWaveMachineRegistry.register(
			com.negodya1.vintageimprovements.VintageBlocks.LATHE_MOVING.get())
			.addTypes(com.negodya1.vintageimprovements.VintageRecipes.TURNING)
			.register();
		StellarWaveMachineRegistry.register(
			com.negodya1.vintageimprovements.VintageBlocks.LASER.get())
			.addTypes(com.negodya1.vintageimprovements.VintageRecipes.LASER_CUTTING)
			.register();
	}

	/** 真空室状态选择器：非真空室原样返回；真空室按 mode 只给当前模式那一套配方类型。 */
	private static List<IRecipeTypeInfo> vacuumChamberTypes(net.minecraft.world.level.block.entity.BlockEntity machine,
		List<IRecipeTypeInfo> base) {
		if (!(machine instanceof com.negodya1.vintageimprovements.content.kinetics.vacuum_chamber.VacuumChamberBlockEntity chamber))
			return base;
		boolean pressurizing = readVacuumMode(chamber);
		return List.of(pressurizing
			? com.negodya1.vintageimprovements.VintageRecipes.PRESSURIZING
			: com.negodya1.vintageimprovements.VintageRecipes.VACUUMIZING);
	}

	/**
	 * <b>杠杆锤状态选择器</b>：杠杆锤的工作模式由<b>锤正下方的方块</b>决定，模式不同能做整套配方都不同。
	 *
	 * <p><b>依据（去混淆字节码逐一核对 {@code HelveBlockEntity#updateAnvilState}）</b>：</p>
	 * <ul>
	 *   <li>下方方块 {@code instanceof AnvilBlock}，或其物品在 {@code vintageimprovements:anvils}
	 *       tag 中 → {@code changeMode(1)} = <b>锤击</b>；</li>
	 *   <li>下方方块物品在 {@code vintageimprovements:custom_hammering_blocks} tag 中（整合包自定义
	 *       锤击方块）→ 同样 {@code changeMode(1)} = <b>锤击</b>；</li>
	 *   <li>下方方块是 {@code minecraft:smithing_table} → {@code changeMode(2)} = <b>锻造</b>；</li>
	 *   <li>其它（含空气）→ {@code changeMode(0)} → {@code canProcess()} 为 false：<b>整机不加工</b>。</li>
	 * </ul>
	 *
	 * <p>判定按<b>方块 id 前缀</b>而不是 {@code instanceof HelveBlockEntity}：动能杠杆锤的方块实体
	 * 直接继承 Create {@code KineticBlockEntity}（与 {@code HelveBlockEntity} 不是同一条继承链），
	 * 而变器只扫描 KineticBlockEntity，按 id 判定对两种杠杆锤都成立。</p>
	 *
	 * <p>读不到世界/异常时返回 {@code base}（保守沿用静态全集），不让联动异常影响整体扫描。</p>
	 */
	private static List<IRecipeTypeInfo> helveHammerTypes(net.minecraft.world.level.block.entity.BlockEntity machine,
		List<IRecipeTypeInfo> base) {
		try {
			if (machine.getLevel() == null)
				return base;
			ResourceLocation id = BuiltInRegistries.BLOCK.getKey(machine.getBlockState()
				.getBlock());
			if (!id.toString()
				.startsWith("vintageimprovements:helve_hammer"))
				return base; // 不是杠杆锤：原样（选择器只挂在杠杆锤档案上，这里只是双保险）
			Block below = machine.getLevel()
				.getBlockState(machine.getBlockPos()
					.below())
				.getBlock();
			// 与该 mod 自身判据同口径：AnvilBlock 或自家 tag（物品 tag → 探针物品栈比对）
			ItemStack probe = below.asItem()
				.getDefaultInstance();
			if (below instanceof AnvilBlock
				|| probe.is(com.negodya1.vintageimprovements.content.kinetics.helve_hammer.HelveBlockEntity.anvilTag)
				|| probe.is(com.negodya1.vintageimprovements.content.kinetics.helve_hammer.HelveBlockEntity.customAnvilTag))
				return List.of(com.negodya1.vintageimprovements.VintageRecipes.HAMMERING);
			if (below == Blocks.SMITHING_TABLE)
				return List.of(com.negodya1.vintageimprovements.VintageRecipes.AUTO_SMITHING,
					com.negodya1.vintageimprovements.VintageRecipes.AUTO_UPGRADE);
			return List.of(); // 锤下没有铁砧/锻造台：这台机器此刻不工作
		} catch (Throwable ignored) {
			return base;
		}
	}

	/** 反射读取真空室 mode（boolean）；失败回退默认加压。 */
	private static boolean readVacuumMode(
		com.negodya1.vintageimprovements.content.kinetics.vacuum_chamber.VacuumChamberBlockEntity chamber) {
		try {
			java.lang.reflect.Field f = vacuumModeField;
			if (f == null) {
				f = com.negodya1.vintageimprovements.content.kinetics.vacuum_chamber.VacuumChamberBlockEntity.class
					.getDeclaredField("mode");
				f.setAccessible(true);
				vacuumModeField = f;
			}
			return Boolean.TRUE.equals(f.get(chamber));
		} catch (Throwable ignored) {
			return true;
		}
	}
}
