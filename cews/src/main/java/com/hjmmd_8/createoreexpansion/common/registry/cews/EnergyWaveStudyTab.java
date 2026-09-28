package com.hjmmd_8.createoreexpansion.common.registry.cews;

import java.util.List;
import java.util.function.Supplier;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeCreativeTabs;
import com.hjmmd_8.createoreexpansion.common.registry.coe.charger.CoeChargerBlocks;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * <b>机械动力：能量波阵学</b>（Create: Energy Wave Studies，简称 <b>CEWS</b>）板块的清单与创造标签页内容。
 *
 * <p><b>P3s：本类从 {@code common/hub/} 搬进 CEWS 自己的注册包</b>。理由有两条：
 * ① 它的 17 项清单里每一件都是 CEWS 的东西（{@code CewsBlocks} / {@code CewsItems}），
 * 一条 COE / TRANS 的成分都没有——它本来就是"CEWS 的清单"，挂在集成层是历史遗留；
 * ② 它是 CEWS 层文件通向集成层的唯一一条边（{@code CewsMod → common.hub.EnergyWaveStudyTab}），
 * 搬进本包后 CEWS 的 {@code LAYER-NO} 上下文里不再出现这个 blocker。落点是本包
 * （{@code common.registry.cews}）而不是新建包：同包的 {@code CewsBlocks}/{@code CewsItems}
 * 正是清单要读的两个类，且"注册 + 该注册的清单"同住一个层包是诚实的。</p>
 *
 * <p><b>调用点零改动</b>：{@code CewsMod} 里那句 {@code modEventBus.addListener(...)} 的
 * 方法引用只跟着 import 走一行；{@code CreateOreExpansion} 构造器里的
 * {@code EnergyWaveStudyTab.register()}-式接线（如果将来加）与页 id 常量
 * {@link #TAB_ID} 的取值一字未变。</p>
 *
 * <p><b>P3t：页 key 改读「归属层」的声明，不再走集成层别名</b>。{@link #onBuildContents}
 * 需要两个 {@code ResourceKey}：本层的 {@code energy_wave_study} 与 COE 层的 {@code base_tab}
 * （用来判"塞进新页"还是"从基础页剔除"）。原先它读 {@code common/hub/AllCreativeModeTabs} 的
 * 两个转发字段，而 hub 是<b>根侧</b>的类——CEWS 真要拆成子模块时看不见它，这一条边就是
 * {@code layer-closure} 报表里 CEWS 最后的 blocker。现在改读
 * {@link CewsCreativeTabs#ENERGY_WAVE_STUDY}（同层，连 import 都不需要）与
 * {@link CoeCreativeTabs#BASE_TAB}（CEWS → COE，允许方向）；
 * {@code AllCreativeModeTabs} 里那两个字段本来就是这两者的同名转发，所以
 * {@code key()} 的取值、以及"哪些页受本方法影响"一个字都没变。层文件不再 import hub。</p>
 *
 * <p><b>这个类解决什么</b>：能量波系统（充能器 / 调级器 / 波速调节器 / 差波器 / 能量场控制器 /
 * 星辉波变器 / 强化避雷针 / 波情查询仪）原先和矿物、宝石、工具混在同一个标签页里。
 * 用户 2026-09-14 要求把这一整块<b>归到一个独立标签页</b>，并且<b>后续要整包拆成一个独立的内置 jar
 * （新模块 CEWS）</b>——于是"哪些东西属于能量波阵学"这件事需要一份<b>唯一清单</b>：
 * 就是这里的 {@link #CONTENTS}。标签页往里放它、基础标签页按它剔除、将来拆包也照它搬。</p>
 *
 * <p><b>为什么用事件而不是逐个改注册</b>：分区之前本模组所有条目都靠 Registrate 的
 * {@code defaultCreativeTab} 自动进<b>基础</b>标签页（见 {@code CreateOreExpansion} 静态块），
 * 要"搬家"就得在两处改近百行注册代码。改用 NeoForge 的
 * {@link BuildCreativeModeTabContentsEvent}（MOD 总线，标签页构建时触发）：
 * <b>一处清单</b>同时完成"塞进新页 + 从旧页剔除"，注册代码一行不动，
 * 也不会漏项（漏了就是清单里没写，扫一眼清单即可核对）。</p>
 *
 * <p><b>⚠ 分区之后口径变了（2026-09-27，P13/W4 的教训）</b>：现在<b>每层 Registrate 自持创造页</b>——
 * {@link CewsRegistrate} 的默认页是本页（{@code ENERGY_WAVE_STUDY}），
 * {@code CoeRegistrate}/{@code TransmutationRegistrate} 的默认页才是 {@code BASE_TAB}。
 * 所以本清单里的东西<b>已经由 Registrate 自动放进本页</b>：
 * "往新页放"这半边不再是新增，而是<b>重复添加</b>（会抛
 * {@code already exists in the tab's list}）。{@link #onBuildContents} 因此改成
 * <b>先摘再放</b>；同理"从基础页剔除"那半边现在摘不到东西，只剩兜底语义。
 * 此坑与发布形态/几个 mod 文件无关——dev 里同一份监听器顺序、同一个事件，照崩。</p>
 *
 * <p><b>清单口径</b>（用户 2026-09-14 裁定，W9 2026-09-28 修订）：收"能量波系统的机器 + 波情查询仪"。
 * <b>不含</b>矿物/宝石/水晶芽/工具/技能类物品（属矿物拓展模块）；
 * <b>不含</b>强化避雷针（用户明确：它属于矿物拓展——雷击加工本身也加工矿物类配方）；
 * <b>不含</b>三种机壳（W9 用户裁定「三个机壳归属于 COE，而不归属于 CEWS」⇒ 归矿物页）。</p>
 *
 * <p><b>⚠ 本清单 ≠ "CEWS 页里有什么"（2026-09-27 实测更正）</b>：{@code CewsRegistrate}
 * 自持本页（其 {@code defaultCreativeTab} 就是本页），所以<b>凡经它注册的条目都由 Registrate
 * 自动进本页</b>，与本清单无关。实测（W9 改动前）CEWS 页 <b>20 项</b> = 本清单 17 项 + 3 项不在清单里的
 * （{@code stellarstone_casing} / {@code energy_mechanism} / {@code incomplete_energy_mechanism}，
 * 按注册顺序排在本页最前）。本清单实际只决定<b>那 17 项的展示顺序</b>（见 {@link #onBuildContents}）。
 * W9 之后：{@code stellarstone_casing} 随三个机壳离开本页（去矿物页），
 * 所以本页 = 本清单 15 项 + 2 项不在清单里的（{@code energy_mechanism} /
 * {@code incomplete_energy_mechanism}）= <b>17 项</b>。
 * 那两个能量机构该不该在本页属<b>玩法面、待用户裁定</b>——既有口径是
 * "材料不是机器、留矿物页"，分区之后它们已经实际落在本页。</p>
 */
public final class EnergyWaveStudyTab {

	/** 标签页 id（{@code createoreexpansion:energy_wave_study}）。 */
	public static final String TAB_ID = "energy_wave_study";

	private EnergyWaveStudyTab() {}

	/**
	 * <b>CEWS 板块的完整物品清单</b>（唯一处）：能量波系统的机器 + 波情查询仪。
	 *
	 * <p>顺序即标签页内的展示顺序：充能器（三种）→ 能量场控制器 → 波变器 → 调级器（三种）→
	 * 波速调节器（三种）→ 差波器（三种面数）→ 波情查询仪。<b>15 项</b>。</p>
	 *
	 * <p><b>三条用户裁定</b>：① <b>强化避雷针留在矿物拓展</b>（2026-09-14）——雷击加工本身
	 * 也加工矿物类配方，波只是"引雷手段"之一；② <b>机壳原先随本模块</b>（2026-09-14）——
	 * 该条已被 W9（2026-09-28）的用户裁定<b>取代</b>：「三个机壳归属于 COE，而不归属于 CEWS」；
	 * ③ <b>三个机壳归 COE</b>（2026-09-28）——三条机壳登记搬回
	 * {@code common.registry.coe.CoeBlocks}，因此它们出现在<b>矿物页</b>，本清单不收。</p>
	 *
	 * <p><b>关于"三种机壳"（2026-09-27 更正 + W9 落地）</b>：注册表里有
	 * <b>三种</b>机壳方块，都是 {@code CasingBlock}：{@code jade_casing}、{@code sapphire_casing}
	 * 与 {@code stellarstone_casing}。原文写"<i>星辉石机壳没有方块，
	 * {@code stellarstone_casing} 只是机器用的材质贴图</i>"是把"贴图"当成了"没有方块"——
	 * 该方块<b>一直存在</b>。W9 起三者<b>全部</b>住 {@code CoeBlocks}（{@code CoeRegistrate.REGISTRATE}），
	 * 默认进基础页；本清单与它们再无关系。</p>
	 */
	public static final List<Supplier<ItemStack>> CONTENTS = List.of(
		// —— 应力充能器：三条矿物线各一台（翡翠 1~3 级 / 蓝宝石 1~5 级 / 星辉石 1~5 级手动档）——
		// W6-c：这三条登记的类是第一层的 CoeChargerBlocks（读它是 L2 → L1，允许方向）。
		CoeChargerBlocks.JADE_STRESS_CHARGER::asStack,
		CoeChargerBlocks.SAPPHIRE_STRESS_CHARGER::asStack,
		CoeChargerBlocks.STELLARSTONE_STRESS_CHARGER::asStack,
		// —— 能量场控制器：能量场（加速/偏转/赋能）的场源 ——
		CewsBlocks.ENERGY_FIELD_CONTROLLER::asStack,
		// —— 星辉波变器：把普通波转成全能波（加工波变态）/ 点燃成攻击波（攻击波变态）——
		CewsBlocks.STELLAR_WAVE_TRANSMUTER::asStack,
		// —— 能量调级器：穿过即按顺/逆基准升一级或降一级（三种材质）——
		CewsBlocks.ENERGY_WAVE_REGULATOR::asStack,
		CewsBlocks.SAPPHIRE_WAVE_REGULATOR::asStack,
		CewsBlocks.STELLARSTONE_WAVE_REGULATOR::asStack,
		// —— 波速调节器：穿过即加减波速（三种材质）——
		CewsBlocks.WAVE_SPEED_REGULATOR::asStack,
		CewsBlocks.SAPPHIRE_SPEED_REGULATOR::asStack,
		CewsBlocks.STELLARSTONE_SPEED_REGULATOR::asStack,
		// —— 波差器家族：把波按开口分配/转向/降级/分裂（四面 / 六面 / 八面）——
		CewsBlocks.ENERGY_WAVE_DISPERSER::asStack,
		CewsBlocks.SIX_FACE_DISPERSER::asStack,
		CewsBlocks.OCTA_ENERGY_WAVE_DIFFERENCER::asStack,
		// —— W9：机壳不再进本清单 ——
		// 三条机壳（jade_casing / sapphire_casing / stellarstone_casing）按用户裁定
		// （2026-09-28「三个机壳归属于 COE」）搬回第一层的 CoeBlocks，由 CoeRegistrate 注册 ⇒
		// Registrate 自动把它们放进**矿物页**（CoeCreativeTabs.BASE_TAB）。
		// 因此本清单不再收它们：收了就等于"从矿物页摘掉、塞回 CEWS 页"，与裁定相反。
		// —— 波情查询仪：右键报最近一只波的五要素（波速/波级/波载荷/波型/剩余寿命）——
		CewsItems.WAVE_QUERY_GAUGE::asStack);

	/**
	 * 标签页内容构建（MOD 总线上由 {@code CreateOreExpansion} 构造器注册）：
	 * 往 CEWS 标签页按 {@link #CONTENTS} 顺序放清单，并从基础标签页里剔除同一批物品
	 * （否则会出现"两个页都有"）。
	 *
	 * <p><b>2026-09-27（P13/W4）修：往新页那一支必须先摘再放，不能直接 {@code accept}。</b>
	 * 本类的原始写法是"塞进新页"——那是<b>分区之前</b>的口径：当时所有条目都默认进基础页，
	 * 所以 CEWS 页是空的、{@code accept} 只会新增。而 {@link CewsRegistrate} 现在自己就
	 * {@code REGISTRATE.defaultCreativeTab(CewsCreativeTabs.ENERGY_WAVE_STUDY.key())}，
	 * <b>CEWS 的方块/物品已经由 Registrate 按注册顺序自动放进本页</b>
	 * （{@code AbstractRegistrate#onBuildCreativeModeTabContents} → {@code CreativeModeTabModifier#accept}），
	 * 于是这一支变成<b>重复添加</b> → {@code accept} 断言失败：
	 * {@code IllegalArgumentException: Itemstack 1 … already exists in the tab's list}
	 * （实测崩溃：{@code crash-2026-09-27_14.00.11-client.txt}；监听器顺序 0=Registrate / 1=本模组）。</p>
	 *
	 * <p><b>为什么不直接把 {@code accept} 删掉了事</b>：Registrate 放进页里的顺序是
	 * <b>注册顺序</b>，而 {@link #CONTENTS} 的注释明写"顺序即标签页内的展示顺序"——
	 * 删掉 {@code accept} 就等于把展示顺序交还给注册顺序。改成
	 * <b>先 {@code remove} 再 {@code accept}</b> 就把顺序按 {@link #CONTENTS} 重新钉死：
	 * 摘掉那一份再追加回末尾，逐条处理完 n 项后，末尾 n 项恰好就是 {@link #CONTENTS} 的顺序
	 * （{@code InsertableLinkedOpenCustomHashSet} 的迭代顺序即最终展示顺序——
	 * {@code EventHooks#onCreativeModeTabBuildContents} 就是按 parentEntries/searchEntries 的迭代顺序
	 * 逐条 {@code output.accept} 的）。</p>
	 *
	 * <p><b>为什么 {@code remove} 是安全的（源码依据）</b>：
	 * {@code BuildCreativeModeTabContentsEvent#remove} <b>不做任何断言</b>，两个集合上都是
	 * 直接 {@code remove(...)}（只有 {@code insertAfter}/{@code insertBefore} 才断言"目标必须已存在"）。
	 * 所以"原来就在页里"和"原本不在页里"两种情形都安全，本方法因此是<b>幂等</b>的；
	 * 判等的策略是 {@code ItemStackLinkedSet.TYPE_AND_TAG}（{@code isSameItemSameComponents}），
	 * 与 {@code accept} 断言"是否已存在"用的是同一个策略，故 Registrate 放进去的那一份必然摘得掉。
	 * visibility 仍取 {@code PARENT_AND_SEARCH_TABS}（与 Registrate 放进来时用的默认值一致）。</p>
	 */
	public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
		ResourceKey<CreativeModeTab> tab = event.getTabKey();
		boolean intoCews = CewsCreativeTabs.ENERGY_WAVE_STUDY.key()
			.equals(tab);
		boolean outOfBase = CoeCreativeTabs.BASE_TAB.key()
			.equals(tab);
		if (!intoCews && !outOfBase)
			return;
		for (Supplier<ItemStack> item : CONTENTS) {
			ItemStack stack = item.get();
			if (stack.isEmpty())
				continue; // 防御：条目尚未注册完（正常流程下不会发生）
			if (intoCews) {
				// 幂等：先摘掉 Registrate 已按注册顺序放进来的一份（不存在时是空操作），
				// 再按 CONTENTS 的顺序追加回页尾——两步都走同一个 visibility。
				event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
				event.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
			} else {
				// 防御性保留：本层条目现在由 CewsRegistrate 自持页、不再进基础页，
				// 所以这一支目前摘不到任何东西（remove 对不存在的条目是空操作，不会抛）。
				// 留着的理由是"清单里的东西不许出现在基础页"这条不变量——
				// 将来若某件被挪回 CoeRegistrate/TransmutationRegistrate（或某一层短暂错配默认页），
				// 这里就是唯一兜底，且零成本、零异常。
				event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
			}
		}
		// INFO 而非 DEBUG：这段只在"标签页被构建"时跑一次（每个会话 2 行），
		// 恰好是进游戏后确认"新页有内容、旧页已剔除"的唯一日志证据，排查时不必开 debug 日志。
		CoeCore.LOGGER.info("[CEWS] 能量波阵学标签页内容同步：{} 项（{}）", CONTENTS.size(),
			intoCews ? "加入新页" : "从基础页剔除");
	}
}
