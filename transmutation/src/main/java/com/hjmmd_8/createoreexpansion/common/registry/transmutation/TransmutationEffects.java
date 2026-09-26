package com.hjmmd_8.createoreexpansion.common.registry.transmutation;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.transmuting.effect.TransmutationDisorderEffect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * <b>TRANS（机械嬗化学）层的 {@code MobEffect} 声明</b>（P3k：从 {@code common/AllModEffects} 搬来）。
 *
 * <p><b>为什么搬</b>：{@code transmutation_disorder} 的实体效果类
 * {@link TransmutationDisorderEffect} 本来就住在 TRANS 层
 * （{@code content/transmuting/effect/}），而声明却挂在 {@code common} 顶层的聚合入口里——
 * 这正是 AGENTS 说的「全模组共用的登记表住在某一层里」的镜像问题：层自己的东西住在聚合入口里。
 * 按 P3c 的既有模式，声明回到本层（那个别名 {@code common/AllModEffects} 已在 P7a 删除）。</p>
 *
 * <p><b>注册顺序</b>：{@code DeferredRegister} 由本层自己的 {@code @Mod} 入口
 * （{@code TransmutationMod} 构造器）挂总线。P7a 之前它由集成层的别名转发、
 * 触发点在 {@code CreateOreExpansion} 构造器里——那条路在"只装 transmutation.jar"时不存在
 * （嬗乱效果根本不会注册），现在归本层自己。</p>
 *
 * <p><b>datagen 归属没变</b>：MobEffect 不经 datagen，注册命名空间仍是
 * {@link CoeCore#REGISTRY_NAMESPACE}（{@code createoreexpansion}），注册 id
 * {@code createoreexpansion:transmutation_disorder} 与语言键一字未改。</p>
 */
public final class TransmutationEffects {

	/** 本层（以及整个模组）唯一的 MOB_EFFECT 延迟注册器。 */
	public static final DeferredRegister<MobEffect> EFFECTS =
		DeferredRegister.create(Registries.MOB_EFFECT, CoeCore.REGISTRY_NAMESPACE);

	/** 嬗乱（transmutation_disorder）：接触嬗化液/被闪电击中/被雷霆合金工具命中等路径施加。 */
	public static final DeferredHolder<MobEffect, TransmutationDisorderEffect> TRANSMUTATION_DISORDER =
		EFFECTS.register("transmutation_disorder", TransmutationDisorderEffect::new);

	private TransmutationEffects() {
	}

	/** 注册触发（P7a 起由 {@code TransmutationMod} 构造器直接调用）。 */
	public static void register(IEventBus modEventBus) {
		EFFECTS.register(modEventBus);
	}

}
