package com.hjmmd_8.createoreexpansion.common;

import java.util.List;
import java.util.function.Supplier;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * <b>机械动力：能量波阵学</b>（Create: Energy Wave Studies，简称 <b>CEWS</b>）板块的清单与创造标签页内容。
 *
 * <p><b>这个类解决什么</b>：能量波系统（充能器 / 调级器 / 波速调节器 / 差波器 / 能量场控制器 /
 * 星辉波变器 / 强化避雷针 / 波情查询仪）原先和矿物、宝石、工具混在同一个标签页里。
 * 用户 2026-09-14 要求把这一整块<b>归到一个独立标签页</b>，并且<b>后续要整包拆成一个独立的内置 jar
 * （新模块 CEWS）</b>——于是"哪些东西属于能量波阵学"这件事需要一份<b>唯一清单</b>：
 * 就是这里的 {@link #CONTENTS}。标签页往里放它、基础标签页按它剔除、将来拆包也照它搬。</p>
 *
 * <p><b>为什么用事件而不是逐个改注册</b>：本模组所有条目都靠 Registrate 的
 * {@code defaultCreativeTab} 自动进基础标签页（见 {@code CreateOreExpansion} 静态块），
 * 要"搬家"就得在两处改近百行注册代码。改用 NeoForge 的
 * {@link BuildCreativeModeTabContentsEvent}（MOD 总线，标签页构建时触发）：
 * <b>一处清单</b>同时完成"塞进新页 + 从旧页剔除"，注册代码一行不动，
 * 也不会漏项（漏了就是清单里没写，扫一眼清单即可核对）。</p>
 *
 * <p><b>清单口径</b>（用户 2026-09-14 裁定后）：收"能量波系统的机器 + 这一线的机壳 + 波情查询仪"。
 * <b>不含</b>矿物/宝石/水晶芽/工具/技能类物品（属矿物拓展模块）；
 * <b>不含</b>强化避雷针（用户明确：它属于矿物拓展——雷击加工本身也加工矿物类配方）；
 * <b>不含</b>能量机构（{@code energy_mechanism}）这类合成件——它是材料不是机器，
 * 若后续拆包时判定该随模块走，把它加进 {@link #CONTENTS} 即可（一处一行）。</p>
 */
public final class EnergyWaveStudyTab {

	/** 标签页 id（{@code createoreexpansion:energy_wave_study}）。 */
	public static final String TAB_ID = "energy_wave_study";

	private EnergyWaveStudyTab() {}

	/**
	 * <b>CEWS 板块的完整物品清单</b>（唯一处）：能量波系统的机器 + 机壳 + 波情查询仪。
	 *
	 * <p>顺序即标签页内的展示顺序：充能器（三种）→ 能量场控制器 → 波变器 → 调级器（三种）→
	 * 波速调节器（三种）→ 差波器（三种面数）→ 机壳（现有两种）→ 波情查询仪。</p>
	 *
	 * <p><b>两条用户裁定（2026-09-14）</b>：① <b>强化避雷针留在矿物拓展</b>——雷击加工本身
	 * 也加工矿物类配方，波只是"引雷手段"之一；② <b>机壳随本模块</b>（用户要求）。</p>
	 *
	 * <p><b>关于"三种机壳"</b>：注册表里只有 <b>两种</b>机壳方块——
	 * {@code jade_casing}、{@code sapphire_casing}（都是 {@code CasingBlock}）；
	 * <b>星辉石机壳没有方块</b>，{@code stellarstone_casing} 只是机器用的材质贴图
	 * （见 {@code textures/block/stellarstone_casing.png}）。故这里只能收两种；
	 * 若确实要一个"星辉石机壳方块"，那是新内容（要注册方块 + 模型 + 战利品表 + 语言），需用户拍板。</p>
	 */
	public static final List<Supplier<ItemStack>> CONTENTS = List.of(
		// —— 应力充能器：三条矿物线各一台（翡翠 1~3 级 / 蓝宝石 1~5 级 / 星辉石 1~5 级手动档）——
		AllBlocks.JADE_STRESS_CHARGER::asStack,
		AllBlocks.SAPPHIRE_STRESS_CHARGER::asStack,
		AllBlocks.STELLARSTONE_STRESS_CHARGER::asStack,
		// —— 能量场控制器：能量场（加速/偏转/赋能）的场源 ——
		AllBlocks.ENERGY_FIELD_CONTROLLER::asStack,
		// —— 星辉波变器：把普通波转成全能波（加工波变态）/ 点燃成攻击波（攻击波变态）——
		AllBlocks.STELLAR_WAVE_TRANSMUTER::asStack,
		// —— 能量调级器：穿过即按顺/逆基准升一级或降一级（三种材质）——
		AllBlocks.ENERGY_WAVE_REGULATOR::asStack,
		AllBlocks.SAPPHIRE_WAVE_REGULATOR::asStack,
		AllBlocks.STELLARSTONE_WAVE_REGULATOR::asStack,
		// —— 波速调节器：穿过即加减波速（三种材质）——
		AllBlocks.WAVE_SPEED_REGULATOR::asStack,
		AllBlocks.SAPPHIRE_SPEED_REGULATOR::asStack,
		AllBlocks.STELLARSTONE_SPEED_REGULATOR::asStack,
		// —— 波差器家族：把波按开口分配/转向/降级/分裂（四面 / 六面 / 八面）——
		AllBlocks.ENERGY_WAVE_DISPERSER::asStack,
		AllBlocks.SIX_FACE_DISPERSER::asStack,
		AllBlocks.OCTA_ENERGY_WAVE_DIFFERENCER::asStack,
		// —— 机壳：本模块机器的外壳建材（也是 Create 机壳标签成员，可用来包轴/齿轮）——
		AllBlocks.JADE_CASING::asStack,
		AllBlocks.SAPPHIRE_CASING::asStack,
		// —— 波情查询仪：右键报最近一只波的五要素（波速/波级/波载荷/波型/剩余寿命）——
		AllItems.WAVE_QUERY_GAUGE::asStack);

	/**
	 * 标签页内容构建（MOD 总线上由 {@code CreateOreExpansion} 构造器注册）：
	 * 往 CEWS 标签页塞清单，并从基础标签页里剔除同一批物品（否则会出现"两个页都有"）。
	 */
	public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
		ResourceKey<CreativeModeTab> tab = event.getTabKey();
		boolean intoCews = AllCreativeModeTabs.ENERGY_WAVE_STUDY.key()
			.equals(tab);
		boolean outOfBase = AllCreativeModeTabs.BASE_TAB.key()
			.equals(tab);
		if (!intoCews && !outOfBase)
			return;
		for (Supplier<ItemStack> item : CONTENTS) {
			ItemStack stack = item.get();
			if (stack.isEmpty())
				continue; // 防御：条目尚未注册完（正常流程下不会发生）
			if (intoCews)
				event.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
			else
				event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		}
		// INFO 而非 DEBUG：这段只在"标签页被构建"时跑一次（每个会话 2 行），
		// 恰好是进游戏后确认"新页有内容、旧页已剔除"的唯一日志证据，排查时不必开 debug 日志。
		CreateOreExpansion.LOGGER.info("[CEWS] 能量波阵学标签页内容同步：{} 项（{}）", CONTENTS.size(),
			intoCews ? "加入新页" : "从基础页剔除");
	}
}
