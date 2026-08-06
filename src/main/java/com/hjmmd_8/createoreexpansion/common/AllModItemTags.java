package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class AllModItemTags {

	public static final TagKey<Item> TRANSMUTATION_PROTECTED =
		TagKey.create(Registries.ITEM,
			ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "transmutation_protected"));

	private AllModItemTags() {
	}

}