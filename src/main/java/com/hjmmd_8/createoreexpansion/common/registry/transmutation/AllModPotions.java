package com.hjmmd_8.createoreexpansion.common.registry.transmutation;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 嬗乱药水的注册处（P3l：从 {@code common/} 顶层搬进 TRANS 层）。
 *
 * <p><b>为什么它不能留在 {@code common} 顶层</b>：{@code common} 顶层包整体搬进了 core 库，
 * 而本类的三条药水全部由 {@code transmutation_disorder}（TRANS 层的 {@code MobEffect}）构成 ⇒
 * 留在那里就是一个 {@code CORE → TRANS} 的引用，而 core 编译期根本看不见 TRANS。</p>
 *
 * <p><b>为什么引用自己层的 {@code TransmutationEffects} 而不是 hub 里的别名</b>：
 * AGENTS「破环的设计规则」——层只引用自己层的常量，聚合入口只许被别的层/入口单向引用。
 * 二者指向<b>同一个</b> {@code DeferredHolder}，注册 id {@code createoreexpansion:transmutation_disorder}
 * 与注册时机一字未变。</p>
 */
public final class AllModPotions {

	public static final DeferredRegister<Potion> POTIONS =
		DeferredRegister.create(Registries.POTION, CoeCore.REGISTRY_NAMESPACE);

	// 酿造材料暂定，之后替换成正式材料即可
	public static final Item BREWING_INGREDIENT = Items.AMETHYST_SHARD;

	public static final DeferredHolder<Potion, Potion> TRANSMUTATION =
		POTIONS.register("transmutation_disorder",
			() -> new Potion("transmutation_disorder",
				new MobEffectInstance(TransmutationEffects.TRANSMUTATION_DISORDER, 20 * 180, 0)));

	public static final DeferredHolder<Potion, Potion> LONG_TRANSMUTATION =
		POTIONS.register("long_transmutation_disorder",
			() -> new Potion("long_transmutation_disorder",
				new MobEffectInstance(TransmutationEffects.TRANSMUTATION_DISORDER, 20 * 480, 0)));

	public static final DeferredHolder<Potion, Potion> STRONG_TRANSMUTATION =
		POTIONS.register("strong_transmutation_disorder",
			() -> new Potion("strong_transmutation_disorder",
				new MobEffectInstance(TransmutationEffects.TRANSMUTATION_DISORDER, 20 * 90, 1)));

	private AllModPotions() {
	}

	public static void register(IEventBus modEventBus) {
		POTIONS.register(modEventBus);
	}

	public static void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
		PotionBrewing.Builder builder = event.getBuilder();
		builder.addMix(Potions.AWKWARD, BREWING_INGREDIENT, TRANSMUTATION);
		builder.addMix(TRANSMUTATION, Items.GLOWSTONE_DUST, STRONG_TRANSMUTATION);
		builder.addMix(TRANSMUTATION, Items.REDSTONE, LONG_TRANSMUTATION);
	}

}