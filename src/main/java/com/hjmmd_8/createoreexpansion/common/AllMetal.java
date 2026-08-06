package com.hjmmd_8.createoreexpansion.common;

import net.createmod.catnip.lang.Lang;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public enum AllMetal {
    TOPAZ,
    SAPPHIRE,
    JADE;

    public final String name;
    public final TagKey<Item> rawOres;
    public final TagKey<Item> crushedRawOres;
    public final TagKey<Item> ingots;
    public final TagKey<Item> nuggets;
    public final TagKey<Item> sheets;
    public final TagKey<Item> rods;
    public final TagKey<Item> wires;
    public final TagKey<Item> itemOres;
    public final TagKey<Item> itemStorageRawBlocks;
    public final TagKey<Item> itemStorageBlocks;

    public final TagKey<Block> blockOres;
    public final TagKey<Block> storageRawBlocks;
    public final TagKey<Block> storageBlocks;

    AllMetal() {
        name = Lang.asId(name());

        rawOres = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "raw_materials/" + name));
        crushedRawOres = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "crushed_raw_materials/" + name));
        ingots = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/" + name));
        nuggets = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "nuggets/" + name));
        sheets = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "sheets/" + name));
        rods = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "rods/" + name));
        wires = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "wires/" + name));
        itemOres = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores/" + name));
        itemStorageRawBlocks = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/raw_" + name));
        itemStorageBlocks = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/" + name));


        blockOres = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores/" + name));
        storageRawBlocks = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/raw_" + name));
        storageBlocks = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/" + name));
    }

    public static void register() {}
}
