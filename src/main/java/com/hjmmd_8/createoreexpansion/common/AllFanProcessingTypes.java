package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.transmuting.AllTransmutingType;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

public class AllFanProcessingTypes {
	public static final AllTransmutingType TRANSMUTING = register("transmuting", new AllTransmutingType());

	private static <T extends FanProcessingType> T register(String name, T type) {
		return Registry.register(CreateBuiltInRegistries.FAN_PROCESSING_TYPE,
			ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, name), type);
	}

	public static void init() {
	}

}
