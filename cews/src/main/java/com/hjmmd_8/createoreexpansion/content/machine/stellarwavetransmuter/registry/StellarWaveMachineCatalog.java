package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry;
import com.hjmmd_8.createoreexpansion.common.registry.WaveRecipeCapabilities;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

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
 *   机械压机  create:mechanical_press      → pressing + compacting（盆上压实同机）+ sequenced_assembly
 *   机械搅拌器 create:mechanical_mixer     → mixing
 *   机械锯    create:mechanical_saw        → cutting + sequenced_assembly
 *   石磨      create:millstone             → milling
 *   粉碎轮    create:crushing_wheel        → crushing
 *   鼓风机    create:encased_fan           → splashing / haunting / createoreexpansion:transmuting
 *   机械手    create:deployer              → deploying / item_application + sequenced_assembly
 *   注液器    create:spout                 → filling + sequenced_assembly
 *   物品排放器 create:item_drain            → emptying
 *   （2026-09-14 补记：压机/锯/机械手/注液器这四台同时也是装配线的常见步骤机，故一并提供
 *     sequenced_assembly——此前这张表漏写了它，而本表自称"波能携带哪些能力的全部来源"。）
 *
 * 【本模组】{@link #registerOwnMachines()}
 *   动力角磨床 createoreexpansion:power_angle_grinder
 *              → grinding（1 级轮）/ +crushing+milling（2 级轮）/ +dismantling（3 级轮）
 *              状态选择器 {@link #angleGrinderTypes}：按安装的角磨轮等级 + 本机转速裁剪
 *   应力充能器（翡翠/蓝宝石/星辉石）→ createoreexpansion:charging
 *
 * 【兼容模组】（各 compat 在 ModList 守卫内经 {@code defer} 追加）
 *   Vintage     卷簧机→coiling、冲压机→curving、砂带打磨机→polishing、
 *               离心机→centrifugation（**结构门槛：盆数 ≥ 4 才给该类型**，状态选择器见
 *               {@code compat.vintageimprovements.VintageImprovementsMachineIntegration#centrifugeTypes}）、
 *               振动台→vibrating+leaves_vibrating、
 *               压缩机/真空室→pressurizing|vacuumizing（按 mode 选择器）、
 *               杠杆锤（两种）→hammering|auto_smithing|auto_upgrade（按锤下方块选择器）、
 *               车床（两种）→turning、激光→laser_cutting
 *   Optical     聚光器 → focusing
 *   CC&amp;A       轧机   → rolling（登记在
 *               {@code compat.createaddition.CreateAdditionTransmuterSupport#registerMachinesIntoCatalog}）
 *
 * 【不由本目录管】
 *   闪电加工 LIGHTNING / LIGHTNING_BLOCK：走"避雷针释放机会"专用路径，不入全库池
 *   非 ProcessingRecipe 族的执行方式（例：拆解）：见 {@code content/charger/family/WaveRecipeFamilies}
 *                     —— 该文件是"波能执行哪些配方族"的唯一清单；本目录只管"机器 → 配方类型"。
 *   能量场控制器 / 波闸 / 非加工机：不产出可携配方类型
 * </pre>
 *
 * <h2>P3i：本目录如何拿到"本模组的配方类型"</h2>
 *
 * <p>本文件属 <b>CEWS</b>。改前它经聚合入口 {@code common/AllRecipeTypes} 读三层常量，
 * 机械换向后会变成一条真实的 <b>CEWS → TRANS 硬依赖</b>。现在改为<b>只问登记表</b>：
 * {@code common/registry/WaveRecipeCapabilities} 持有"可被波加工的配方类型"集合，
 * 三层各自在自己的 {@code XxxRecipeTypes} 类初始化里登记自己那一份
 * （COE 4 项 / CEWS 1 项 / TRANS 1 项，见各层静态块），本目录用 {@link #waveType(String)}
 * 按 id 取。<b>本文件因此不再 import {@code AllRecipeTypes}，也不 import 任何别层的配方类型。</b></p>
 *
 * <p>顺序由登记表里的<b>显式排序键</b>（{@code LayerOrder}）决定，与"谁先被类初始化"无关；
 * 而目录最终呈现的列表顺序仍由本文件的机器登记先后决定（与改前逐项相同）。</p>
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
			// 先把"可被波加工的配方类型"登记表唤醒（各层在自己的类初始化里登记自己那一份），
			// 再登记机器——下面的 registerCreateNatives/registerOwnMachines 全部经 waveType(id)
			// 从表里取档案，不再引用任何一层的配方类型常量。见 WaveRecipeCapabilities。
			WaveRecipeCapabilities.ensureInitialized();
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
			waveType("transmuting"));
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
			com.hjmmd_8.createoreexpansion.common.registry.coe.CoeMachines.POWER_ANGLE_GRINDER.get())
			.addTypes(waveType("grinding"),
				com.simibubi.create.AllRecipeTypes.CRUSHING, com.simibubi.create.AllRecipeTypes.MILLING,
				waveType("dismantling"))
			.withSelector(StellarWaveMachineCatalog::angleGrinderTypes)
			.register();
		// 应力充能器（翡翠/蓝宝石/星辉石）：提供 createoreexpansion:charging 能力
		// （充电配方同样是 ProcessingRecipe 族，会进全库池；类型门打开后必须有机器提供该类型，
		//  否则"变体波给工具充能"这类老玩法会因没有携带者而被挡掉）
		registerMachine(
			com.hjmmd_8.createoreexpansion.common.registry.coe.charger.CoeChargerBlocks.JADE_STRESS_CHARGER.get(),
			waveType("charging"));
		registerMachine(
			com.hjmmd_8.createoreexpansion.common.registry.coe.charger.CoeChargerBlocks.SAPPHIRE_STRESS_CHARGER.get(),
			waveType("charging"));
		registerMachine(
			com.hjmmd_8.createoreexpansion.common.registry.coe.charger.CoeChargerBlocks.STELLARSTONE_STRESS_CHARGER.get(),
			waveType("charging"));
	}

	/**
	 * 从<b>波加工能力登记表</b>里按配方类型 id 取档案（{@link WaveRecipeCapabilities}，P3i）。
	 *
	 * <p><b>为什么目录不直接引用三层的常量</b>：本文件属 CEWS。改前它经聚合入口
	 * {@code common/AllRecipeTypes} 读 TRANS 的 {@code TRANSMUTING} / COE 的
	 * {@code GRINDING}、{@code DISMANTLING} / CEWS 的 {@code CHARGING}——机械换向后这就是
	 * 一条真实的 <b>CEWS → TRANS 硬依赖</b>。改成登记表之后，本文件既不 import
	 * {@code AllRecipeTypes}，也不 import 任何别层的配方类型，只问"表里有没有"。</p>
	 *
	 * <p><b>找不到时返回 null（不抛）</b>：某一层不在（例：只装了 CEWS）时它的登记项根本不会进表，
	 * 这里必须"少一项"而不是"崩一次"；{@code null} 会被 {@code addTypes} / {@code registerMachine}
	 * 静默跳过。注意<b>整表为空</b>时（表未初始化）也会走到这条路径，所以这里同样不能抛。</p>
	 *
	 * @param id 配方类型 id（如 {@code transmuting}、{@code grinding}、{@code charging}）
	 */
	@Nullable
	private static IRecipeTypeInfo waveType(String id) {
		for (IRecipeTypeInfo type : WaveRecipeCapabilities.all())
			if (type != null && type.getId() != null
				&& id.equals(type.getId().getPath()))
				return type;
		return null;
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
		net.minecraft.world.level.block.entity.BlockEntity machine,
		java.util.List<com.simibubi.create.foundation.recipe.IRecipeTypeInfo> base) {
		if (!(machine instanceof com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlockEntity grinder))
			return base;
		com.hjmmd_8.createoreexpansion.content.grinding.item.GrindingWheelTier tier = grinder.getWheelTier();
		if (tier == null)
			return java.util.List.of(); // 没装角磨轮：这台机器此刻什么都做不了
		// 转速不足 = 这台机器此刻不加工（与 PowerAngleGrinderBlockEntity 自身判据完全一致：
		// `Math.abs(getSpeed()) < tier.getMinRpm()` 时不找任何配方）。变器"读取周围机器"同样
		// 只读"此刻真能用"的能力：角磨床停转/转速不够时，波不会凭空带上打磨能力。
		if (Math.abs(grinder.getSpeed()) < tier.getMinRpm())
			return java.util.List.of();
		return com.hjmmd_8.createoreexpansion.content.grinding.recipe.GrinderRecipeTypes
			.getFor(tier.level);
	}

	/** 单台机器快捷登记（目录统一写法：{@code registerMachine(block, types...)}；null 类型忽略）。 */
	private static void registerMachine(net.minecraft.world.level.block.Block block,
		com.simibubi.create.foundation.recipe.IRecipeTypeInfo... types) {
		StellarWaveMachineRegistry.register(block)
			.addTypes(types)
			.register();
	}
}
