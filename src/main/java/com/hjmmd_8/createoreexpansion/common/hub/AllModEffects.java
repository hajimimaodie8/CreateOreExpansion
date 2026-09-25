package com.hjmmd_8.createoreexpansion.common.hub;

import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationEffects;
import com.hjmmd_8.createoreexpansion.content.transmuting.effect.TransmutationDisorderEffect;

import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * <b>同名聚合别名</b>（P3k，照 P3c 的模式；P3s 复核后确认落点正确、未改）：真正的声明住在它所属的层里——
 * {@link TransmutationEffects}（{@code common/registry/transmutation/}，TRANS 层）。
 *
 * <p><b>为什么保留这个类</b>：{@code transmutation_disorder} 的注册触发一直是
 * {@code CreateOreExpansion} 构造器里那句 {@code AllModEffects.register(modEventBus)}，
 * 根侧 SHARED 的 {@code JadeTopazBowEventHandler}（黄玉弓的基础效果）也读它。
 * 保留同名别名后这些调用点<b>一行未改</b>——类名、字段名、注册 id、注册时机全部不变。</p>
 *
 * <p><b>P3t：COE 的消费者已经改走 core 契约</b>。P3k 时 COE 的 {@code MedallionEffectHandler}
 * 直接 import 本类读 {@code TRANSMUTATION_DISORDER}——那是<b>禁止方向</b>
 * （{@code COE -> TRANS}），也是 {@code layer-closure} 报表里 COE 最后的两个 blocker 之一。
 * 现在它改读 core 的窄契约 {@code common.transmutation.TransmutationLink}
 * （由 {@code TransmutationFluids} 注入，身份比较语义一字未变）；TRANS 的
 * {@code AllModPotions} / {@code AllTransmutingType} 读的是本层声明 {@link TransmutationEffects}。
 * 剩下读本别名的是根侧两个文件（{@code CreateOreExpansion} 与
 * {@code JadeTopazBowEventHandler}），字段作为入口 API <b>原样保留</b>。</p>
 *
 * <p><b>P3s 复核结论</b>：本类不属于"把实现塞进骗人包名"那一类——它自己不持有任何声明，
 * 只有一行转发，所以留在 {@code common/hub/} 这个「集成层」名字下是诚实的
 * （同包兄弟 {@code AllFluids} 在本轮也被改成同样的纯转发形状）。</p>
 *
 * <p><b>P3l：本类已从 {@code common/} 顶层挪进 {@code common/hub/}</b>。原因是它无条件转发到
 * TRANS，而 {@code common} 顶层包要整体搬进 core 库——留在那里就是一个 {@code CORE → TRANS}
 * 的引用（探针实测：core 里 5 个「找不到符号」，全是 TRANS 的类/包）。它正是 AGENTS
 * 「破环的设计规则」里说的「聚合入口」，与 {@code AllRecipeTypes} / {@code AllCreativeModeTabs} /
 * {@code AllFluids} / {@code EnergyWaveStudyTab} 同处 {@code common/hub/}：
 * 层可以 import hub，但 hub 永远不许进 core。</p>
 */
public final class AllModEffects {

	/** 嬗乱（转发 TRANS 层声明；注册 id、类型参数、字段名与拆分前逐字相同）。 */
	public static final DeferredHolder<MobEffect, TransmutationDisorderEffect> TRANSMUTATION_DISORDER =
		TransmutationEffects.TRANSMUTATION_DISORDER;

	private AllModEffects() {
	}

	/** 注册触发：委托 TRANS 层（调用点、调用时机、事件总线全部与拆分前一致）。 */
	public static void register(IEventBus modEventBus) {
		TransmutationEffects.register(modEventBus);
	}

}
