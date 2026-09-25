package com.hjmmd_8.createoreexpansion.common.hub;

import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationEffects;
import com.hjmmd_8.createoreexpansion.content.transmuting.effect.TransmutationDisorderEffect;

import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * <b>同名聚合别名</b>（P3k，照 P3c 的模式）：真正的声明住在它所属的层里——
 * {@link TransmutationEffects}（{@code common/registry/transmutation/}，TRANS 层）。
 *
 * <p><b>为什么保留这个类</b>：{@code transmutation_disorder} 被 COE
 * （{@code MedallionEffectHandler}、{@code JadeTopazBowEventHandler}）与
 * TRANS（{@code AllTransmutingType}、{@code TransmutationEventHandler}）共用，
 * 四个调用点写的都是 {@code AllModEffects.TRANSMUTATION_DISORDER}。保留一个同名别名，
 * 这些调用点只需改 import 一行——类名、字段名、注册 id、注册时机全部不变。</p>
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
