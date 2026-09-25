package com.hjmmd_8.createoreexpansion.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class AllModItemTags {

	public static final TagKey<Item> TRANSMUTATION_PROTECTED =
		TagKey.create(Registries.ITEM,
			ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, "transmutation_protected"));

	/** 星辉石系列（佩 + 全部工具）：接触嬗化液/虚空自动漂浮不销毁 */
	public static final TagKey<Item> STELLARSTONE_ITEMS =
		TagKey.create(Registries.ITEM,
			ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, "stellarstone_items"));

	/** 雷鸣合金系列（佩 + 全部工具）：被闪电击中豁免并自动充满能量 */
	public static final TagKey<Item> THUNDERITE_ITEMS =
		TagKey.create(Registries.ITEM,
			ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, "thunderite_items"));

	private AllModItemTags() {
	}

}