package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.transmuting.effect.TransmutationDisorderEffect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AllModEffects {

	public static final DeferredRegister<MobEffect> EFFECTS =
		DeferredRegister.create(Registries.MOB_EFFECT, CreateOreExpansion.MOD_ID);

	public static final DeferredHolder<MobEffect, TransmutationDisorderEffect> TRANSMUTATION_DISORDER =
		EFFECTS.register("transmutation_disorder", TransmutationDisorderEffect::new);

	private AllModEffects() {
	}

	public static void register(IEventBus modEventBus) {
		EFFECTS.register(modEventBus);
	}

}