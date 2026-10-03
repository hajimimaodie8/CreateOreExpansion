package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargedNegativeEffect;
import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargedPositiveEffect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * <b>COE（矿物拓展）层的 {@code MobEffect} 声明</b>（coe-charge 批 1）。
 *
 * <p>注册形状照 TRANS 层的 {@code TransmutationEffects}（同一个 {@code DeferredRegister} +
 * {@code DeferredHolder} 配方）：命名空间恒 {@link CoeCore#REGISTRY_NAMESPACE}
 * （{@code createoreexpansion}，存档红线 —— 不是 mod id {@code coe_integration}），
 * 注册 id 因此是 {@code createoreexpansion:charged_positive} /
 * {@code createoreexpansion:charged_negative}（需求 §六 #2 逐字）。</p>
 *
 * <p><b>为什么不塞进 {@code TransmutationEffects}</b>：那个类声明的是<b>嬗化线</b>的效果
 * （它的类注释明确反对「层自己的东西住在聚合入口里」）。电荷属于能量场/闪电线，是 COE 层的东西
 * ⇒ 按「内容在哪一层，就由那一层的注册器声明」另开一个。一个模组对同一个注册表开两个
 * {@code DeferredRegister} 是 NeoForge 的正常用法（两份 id 不冲突即可）。</p>
 *
 * <p><b>唯一注册触发点</b>：{@code CreateOreExpansion} 构造器里那一行
 * {@code CoeEffects.register(modEventBus)}（W6-b2 起嬗化效果也走同一处）。重复挂总线会在
 * {@code RegisterEvent} 上重复注册，所以本类的 {@link #register(IEventBus)} 全仓只许被调用一次。</p>
 *
 * <p>datagen 归属：MobEffect 不经 datagen（没有生成文件），只有语言键（根的两个 provider）与
 * 图标贴图（{@code assets/createoreexpansion/textures/mob_effect/<注册名>.png}，
 * 由 {@code MobEffect} 的注册 id 直接决定路径，无需任何模型/blockstate）。</p>
 */
public final class CoeEffects {

	/** COE 层的 MOB_EFFECT 延迟注册器（命名空间恒 {@link CoeCore#REGISTRY_NAMESPACE}）。 */
	public static final DeferredRegister<MobEffect> EFFECTS =
		DeferredRegister.create(Registries.MOB_EFFECT, CoeCore.REGISTRY_NAMESPACE);

	/** 着正电（{@code charged_positive}）：不可牛奶解除、能扣死的负面效果。 */
	public static final DeferredHolder<MobEffect, ChargedPositiveEffect> CHARGED_POSITIVE =
		EFFECTS.register("charged_positive", ChargedPositiveEffect::new);

	/** 着负电（{@code charged_negative}）：与着正电互补的另一极，两者相遇触发中和爆炸。 */
	public static final DeferredHolder<MobEffect, ChargedNegativeEffect> CHARGED_NEGATIVE =
		EFFECTS.register("charged_negative", ChargedNegativeEffect::new);

	private CoeEffects() {
	}

	/** 注册触发（{@code CreateOreExpansion} 构造器里唯一一次调用）。 */
	public static void register(IEventBus modEventBus) {
		EFFECTS.register(modEventBus);
	}

}
