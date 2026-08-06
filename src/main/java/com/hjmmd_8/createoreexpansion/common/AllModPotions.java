package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;

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

public final class AllModPotions {

	public static final DeferredRegister<Potion> POTIONS =
		DeferredRegister.create(Registries.POTION, CreateOreExpansion.MOD_ID);

	// 酿造材料暂定，之后替换成正式材料即可
	public static final Item BREWING_INGREDIENT = Items.AMETHYST_SHARD;

	public static final DeferredHolder<Potion, Potion> TRANSMUTATION =
		POTIONS.register("transmutation_disorder",
			() -> new Potion("transmutation_disorder",
				new MobEffectInstance(AllModEffects.TRANSMUTATION_DISORDER, 20 * 180, 0)));

	public static final DeferredHolder<Potion, Potion> LONG_TRANSMUTATION =
		POTIONS.register("long_transmutation_disorder",
			() -> new Potion("long_transmutation_disorder",
				new MobEffectInstance(AllModEffects.TRANSMUTATION_DISORDER, 20 * 480, 0)));

	public static final DeferredHolder<Potion, Potion> STRONG_TRANSMUTATION =
		POTIONS.register("strong_transmutation_disorder",
			() -> new Potion("strong_transmutation_disorder",
				new MobEffectInstance(AllModEffects.TRANSMUTATION_DISORDER, 20 * 90, 1)));

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