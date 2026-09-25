package com.hjmmd_8.createoreexpansion.common;

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
 * （{@code MedallionEffectHandler}、{@code JadeTopazBowEventHandler}）、TRANS
 * （{@code AllTransmutingType}、{@code TransmutationEventHandler}）与
 * {@code common/AllModPotions} 三处共用，四个文件的调用点写的是
 * {@code AllModEffects.TRANSMUTATION_DISORDER}。把声明搬进 TRANS 之后，这个<b>同名、同包</b>
 * 的别名让所有调用点（含 {@code CreateOreExpansion} 构造器里的注册触发）
 * <b>一行都不用改</b>——类名、FQN、字段名、注册时机全部不变。</p>
 *
 * <p><b>注意</b>：本类别无选择地转发到 TRANS，因此它<b>不能</b>随 {@code common} 顶层包
 * 一起搬进 core 库（那会构成 {@code CORE → TRANS} 的禁止方向，且 :core:compileJava 根本
 * 看不见 TRANS 的类）。它属于 AGENTS「破环的设计规则」里说的「聚合入口」——
 * 将来若要搬，应当先挪进 {@code common/hub/}（与 {@code AllRecipeTypes} /
 * {@code AllCreativeModeTabs} / {@code AllFluids} 同处），而不是 core。</p>
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
