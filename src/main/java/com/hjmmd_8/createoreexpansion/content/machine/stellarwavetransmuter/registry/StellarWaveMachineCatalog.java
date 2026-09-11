package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry;

import java.util.ArrayList;
import java.util.List;

/**
 * 星辉波变器"加工机目录"：本仓库全部联动机器的<b>集中声明处</b>。
 *
 * <p><b>设计意图（用户 2026-09 定义）</b>：所有能被变器调用的机器统一在这里登记，
 * 不再散落在各 compat 包各自写 {@code register(...)}。目录只是"注册语句集合"——
 * 内部全部走同一入口 {@link StellarWaveMachineRegistry#register} 链式登记，加一台机器
 * = 在本类里加一行。</p>
 *
 * <p><b>延迟加载隔离</b>：第三方 mod（Vintage/Optical/CC&amp;A 等）的机器必须等
 * {@code ModList.isLoaded} 确认后才可触碰其方块/类型类——目录不直接引用第三方常量，
 * 而是让各 compat 集成在守卫内把自己的机器<b>追加到</b>本目录（同一链式入口）。
 * Create 原生与本 mod 机器可直接静态登记。</p>
 *
 * <h2>已登记机器一览（机器 → 配方类型 → 登记处）</h2>
 *
 * <p><b>这张表就是"波能携带哪些能力"的全部来源</b>：变器扫描半径内的机器 → 配方类型快照 → 随波携带
 * （{@code StellarWaveEntity#allowedTypeIds}）；波只执行携带到的类型（配置
 * {@code wave.requireCarriedType}，默认开）。想让新机器/新模组可被读取，就在这里加一行。</p>
 *
 * <pre>
 * 【Create 原生】{@link #registerCreateNatives()}
 *   机械压机  create:mechanical_press      → pressing + compacting（盆上压实同机）
 *   机械搅拌器 create:mechanical_mixer     → mixing
 *   机械锯    create:mechanical_saw        → cutting
 *   石磨      create:millstone             → milling
 *   粉碎轮    create:crushing_wheel        → crushing
 *   鼓风机    create:encased_fan           → splashing / haunting / createoreexpansion:transmuting
 *   机械手    create:deployer              → deploying / item_application
 *   注液器    create:spout                 → filling
 *   物品排放器 create:item_drain            → emptying
 *
 * 【本模组】{@link #registerOwnMachines()}
 *   动力角磨床 createoreexpansion:power_angle_grinder
 *              → grinding（1 级轮）/ +crushing+milling（2 级轮）/ +dismantling（3 级轮）
 *              状态选择器 {@link #angleGrinderTypes}：按安装的角磨轮等级 + 本机转速裁剪
 *   应力充能器（翡翠/蓝宝石/星辉石）→ createoreexpansion:charging
 *
 * 【兼容模组】（各 compat 在 ModList 守卫内经 {@code defer} 追加）
 *   Vintage     卷簧机→coiling、冲压机→curving、砂带打磨机→polishing、
 *               离心机→centrifugation、振动台→vibrating+leaves_vibrating、
 *               压缩机/真空室→pressurizing|vacuumizing（按 mode 选择器）、
 *               杠杆锤（两种）→hammering|auto_smithing|auto_upgrade（按锤下方块选择器）、
 *               车床（两种）→turning、激光→laser_cutting
 *   Optical     聚光器 → focusing
 *   CC&amp;A       轧机   → rolling
 *
 * 【不由本目录管】
 *   闪电加工 LIGHTNING / LIGHTNING_BLOCK：走"避雷针释放机会"专用路径，不入全库池
 *   非 ProcessingRecipe 族的执行方式（例：拆解）：见 {@code content/charger/family/WaveRecipeFamilies}
 *                     —— 该文件是"波能执行哪些配方族"的唯一清单；本目录只管"机器 → 配方类型"。
 *   能量场控制器 / 波闸 / 非加工机：不产出可携配方类型
 * </pre>
 */
public final class StellarWaveMachineCatalog {

	/** 追加登记用的可变列表（各 compat 在 ModList 守卫后追加自己的机器）。 */
	private static final List<Runnable> DEFERRED = new ArrayList<>();

	private StellarWaveMachineCatalog() {
	}

	/** 各 compat 集成把自己 mod 的机器追加进目录（ModList 守卫后调用；幂等由调用方保证）。 */
	public static void defer(Runnable registration) {
		if (registration != null)
			DEFERRED.add(registration);
	}

	/** 执行目录登记：先登记 Create 原生与本模组自家机器，再执行已追加的 compat 注册（可多次调用，
	 *  compat 的 defer 在唤醒守卫后补进；执行过的追加项出队，重复调用不重复注册）。 */
	public static void init() {
		if (!nativesRegistered) {
			nativesRegistered = true;
			registerCreateNatives();
			registerOwnMachines();
		}
		for (int i = 0; i < DEFERRED.size(); i++) {
			Runnable r = DEFERRED.get(i);
			try {
				r.run();
			} catch (Throwable ignored) {
				// 单个联动注册异常：不影响其它机器
			}
		}
		DEFERRED.clear();
	}

	private static boolean nativesRegistered;

	/** Create 原生处理机（集中声明：压机/锯/石磨/粉碎轮/鼓风机/机械手/搅拌器/注液器/物品排放器）。 */
	private static void registerCreateNatives() {
		// 动力压机：传送带上压片；压在**工作盆**上则是"压实"（Create 同机两态）；
		// 序列装配（sequenced_assembly）：装配线常见的步骤机之一（如"冲压"步）→ 一并提供该类型
		registerMachine(
			com.simibubi.create.AllBlocks.MECHANICAL_PRESS.get(),
			com.simibubi.create.AllRecipeTypes.PRESSING,
			com.simibubi.create.AllRecipeTypes.COMPACTING,
			com.simibubi.create.AllRecipeTypes.SEQUENCED_ASSEMBLY);
		registerMachine(
			com.simibubi.create.AllBlocks.MECHANICAL_MIXER.get(),
			com.simibubi.create.AllRecipeTypes.MIXING);
		registerMachine(
			com.simibubi.create.AllBlocks.MECHANICAL_SAW.get(),
			com.simibubi.create.AllRecipeTypes.CUTTING,
			com.simibubi.create.AllRecipeTypes.SEQUENCED_ASSEMBLY);
		registerMachine(
			com.simibubi.create.AllBlocks.MILLSTONE.get(),
			com.simibubi.create.AllRecipeTypes.MILLING);
		registerMachine(
			com.simibubi.create.AllBlocks.CRUSHING_WHEEL.get(),
			com.simibubi.create.AllRecipeTypes.CRUSHING);
		// 鼓风机：身后介质决定加工类型（环境判定见变体波 §4）；介质为嬗变液时走本模组的 transmuting
		registerMachine(
			com.simibubi.create.AllBlocks.ENCASED_FAN.get(),
			com.simibubi.create.AllRecipeTypes.SPLASHING, com.simibubi.create.AllRecipeTypes.HAUNTING,
			com.hjmmd_8.createoreexpansion.common.AllRecipeTypes.TRANSMUTING);
		// 机械手：装配线的核心步骤机 → deploying/item_application ＋ 序列装配 sequenced_assembly
		registerMachine(
			com.simibubi.create.AllBlocks.DEPLOYER.get(),
			com.simibubi.create.AllRecipeTypes.DEPLOYING, com.simibubi.create.AllRecipeTypes.ITEM_APPLICATION,
			com.simibubi.create.AllRecipeTypes.SEQUENCED_ASSEMBLY);
		// 注液器 / 物品排放器：注液 filling / 排空 emptying（流体驱动，配方同样是 ProcessingRecipe 族）
		registerMachine(
			com.simibubi.create.AllBlocks.SPOUT.get(),
			com.simibubi.create.AllRecipeTypes.FILLING,
			com.simibubi.create.AllRecipeTypes.SEQUENCED_ASSEMBLY);
		registerMachine(
			com.simibubi.create.AllBlocks.ITEM_DRAIN.get(),
			com.simibubi.create.AllRecipeTypes.EMPTYING);
	}

	/**
	 * <b>本模组自家加工机</b>（与 Create 原生并列的第二段集中声明）。
	 *
	 * <p><b>动力角磨床</b>：它属于"状态驱动型"机器——<b>能执行哪些配方类型随安装的角磨轮等级变化</b>
	 * （见 {@link com.hjmmd_8.createoreexpansion.content.grinding.recipe.GrinderRecipeTypes}：
	 * 1 级轮 = 打磨；2 级轮 <b>+=</b> 粉碎/研磨；3 级轮 <b>+=</b> 拆解）。注册时挂
	 * {@link MachineStateSelector} 按实时等级裁剪，变器的"携带配方类型"才读得到"换了轮就换了能力"。</p>
	 *
	 * <p><b>为什么能列「拆解」</b>：{@code DismantlingRecipe implements Recipe}，<b>不属于</b> Create
	 * {@code ProcessingRecipe} 族——它的执行方式登记在
	 * {@code content/charger/family/WaveRecipeFamilies}（"波可执行配方族"唯一清单，拆解族在那里），
	 * 所以这里可以如实把它列为该机器的能力。</p>
	 */
	private static void registerOwnMachines() {
		StellarWaveMachineRegistry.register(
			com.hjmmd_8.createoreexpansion.common.AllBlocks.POWER_ANGLE_GRINDER.get())
			.addTypes(com.hjmmd_8.createoreexpansion.common.AllRecipeTypes.GRINDING,
				com.simibubi.create.AllRecipeTypes.CRUSHING, com.simibubi.create.AllRecipeTypes.MILLING,
				com.hjmmd_8.createoreexpansion.common.AllRecipeTypes.DISMANTLING)
			.withSelector(StellarWaveMachineCatalog::angleGrinderTypes)
			.register();
		// 应力充能器（翡翠/蓝宝石/星辉石）：提供 createoreexpansion:charging 能力
		// （充电配方同样是 ProcessingRecipe 族，会进全库池；类型门打开后必须有机器提供该类型，
		//  否则"变体波给工具充能"这类老玩法会因没有携带者而被挡掉）
		registerMachine(
			com.hjmmd_8.createoreexpansion.common.AllBlocks.JADE_STRESS_CHARGER.get(),
			com.hjmmd_8.createoreexpansion.common.AllRecipeTypes.CHARGING);
		registerMachine(
			com.hjmmd_8.createoreexpansion.common.AllBlocks.SAPPHIRE_STRESS_CHARGER.get(),
			com.hjmmd_8.createoreexpansion.common.AllRecipeTypes.CHARGING);
		registerMachine(
			com.hjmmd_8.createoreexpansion.common.AllBlocks.STELLARSTONE_STRESS_CHARGER.get(),
			com.hjmmd_8.createoreexpansion.common.AllRecipeTypes.CHARGING);
	}

	/**
	 * 角磨床状态选择器：按<b>当前安装的角磨轮等级</b>给出这台机器此刻真正能执行的配方类型。
	 *
	 * <p>与角磨床自身口径一致（{@code PowerAngleGrinderBlockEntity} 也是遍历
	 * {@code GrinderRecipeTypes.getFor(tier.level)} 找配方的）；未装轮 = 整机不加工 → 空表，
	 * 这样变器面板不会凭空列出打磨能力。第三方通过 {@code GrinderRecipeTypes.register} 追加的
	 * 自有类型会自动出现在这里（开闭原则），无需改本方法。</p>
	 *
	 * <p><b>三类轮 → 能力</b>：1 级 =打磨 {@code grinding}；2 级 <b>+=</b> 粉碎/研磨
	 * （Create {@code crushing}/{@code milling}）；3 级 <b>+=</b> 拆解 {@code dismantling}
	 * （由 {@code WaveRecipeFamilies} 登记的非 ProcessingRecipe 族执行，波侧已可直接执行）。</p>
	 */
	private static java.util.List<com.simibubi.create.foundation.recipe.IRecipeTypeInfo> angleGrinderTypes(
		com.simibubi.create.content.kinetics.base.KineticBlockEntity machine,
		java.util.List<com.simibubi.create.foundation.recipe.IRecipeTypeInfo> base) {
		if (!(machine instanceof com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlockEntity grinder))
			return base;
		com.hjmmd_8.createoreexpansion.content.grinding.item.GrindingWheelTier tier = grinder.getWheelTier();
		if (tier == null)
			return java.util.List.of(); // 没装角磨轮：这台机器此刻什么都做不了
		// 转速不足 = 这台机器此刻不加工（与 PowerAngleGrinderBlockEntity 自身判据完全一致：
		// `Math.abs(getSpeed()) < tier.getMinRpm()` 时不找任何配方）。变器"读取周围机器"同样
		// 只读"此刻真能用"的能力：角磨床停转/转速不够时，波不会凭空带上打磨能力。
		if (Math.abs(machine.getSpeed()) < tier.getMinRpm())
			return java.util.List.of();
		return com.hjmmd_8.createoreexpansion.content.grinding.recipe.GrinderRecipeTypes
			.getFor(tier.level);
	}

	/** 单台机器快捷登记（目录统一写法：{@code registerMachine(block, types...)}）。 */
	private static void registerMachine(net.minecraft.world.level.block.Block block,
		com.simibubi.create.foundation.recipe.IRecipeTypeInfo... types) {
		StellarWaveMachineRegistry.register(block)
			.addTypes(types)
			.register();
	}
}
