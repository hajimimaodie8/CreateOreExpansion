package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.neoforged.neoforge.common.Tags;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.foundation.data.BuilderTransformers;

import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

public final class AllMyBlocks {
    public static final BlockEntry<Block> JADE_ORE = CreateOreExpansion.REGISTRATE
            .block("jade_ore", Block::new)
            .initialProperties(() -> Blocks.DIAMOND_ORE)
            // 让方块的最佳挖掘工具是镐子
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            // 挖掘等级        需要石质工具
            // 二者加起来就是方块的最佳挖掘工具是镐子，且需要石质工具
            // 就是需要石质以上的镐子挖掘
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(AllMetal.JADE.blockOres)
            // 注册掉落物，这里比较复杂（因为Minecraft本身的对于矿物的掉落物就是很复杂的，比如处理时运和精准采集等，概率掉落等）
            .loot((lt, block) -> {
                        HolderLookup.RegistryLookup<Enchantment> enchantmentRegistryLookup = lt.getRegistries().lookupOrThrow(Registries.ENCHANTMENT);
                        lt.add(
                                block,
                                lt.createSilkTouchDispatchTable(
                                        block,
                                        lt.applyExplosionDecay(
                                                block,
                                                LootItem.lootTableItem(AllItems.RAW_JADE.get())
                                                    .apply(ApplyBonusCount.addOreBonusCount(
                                                            enchantmentRegistryLookup.getOrThrow(Enchantments.FORTUNE)))
                                        )
                                )
                        );
                    })
            .item()
            .tag(AllMetal.JADE.itemOres)
            .build()
            .register();

    public static final BlockEntry<Block> DEEPSLATE_JADE_ORE = CreateOreExpansion.REGISTRATE
            .block("deepslate_jade_ore", Block::new)
            .initialProperties(() -> Blocks.DEEPSLATE_DIAMOND_ORE)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(AllMetal.JADE.blockOres)
            // 同理，也需要为深层矿石注册战利品，战利品由数据包定义，所以需要runData生成数据
            .loot((lt, block) -> {
                        HolderLookup.RegistryLookup<Enchantment> enchantmentRegistryLookup = lt.getRegistries().lookupOrThrow(Registries.ENCHANTMENT);
                        lt.add(
                                block,
                                lt.createSilkTouchDispatchTable(
                                        block,
                                        lt.applyExplosionDecay(
                                                block,
                                                LootItem.lootTableItem(AllItems.RAW_JADE.get())
                                                        .apply(ApplyBonusCount.addOreBonusCount(
                                                                enchantmentRegistryLookup.getOrThrow(Enchantments.FORTUNE)))
                                        )
                                )
                        );
                    })
            .item()
            .tag(AllMetal.JADE.itemOres)
            .build()
            .register();
    // 这里通过调用方法让Java加载这个类，触发类加载，这样Java才会加载字段
    // 这样注册方块才能真正注册，否则方块不会被注册

    public static final BlockEntry<Block> JADE_BLOCK = CreateOreExpansion.REGISTRATE
            .block("jade_block", Block::new)
            .initialProperties(() -> Blocks.DIAMOND_BLOCK)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_GREEN)
                    .requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .tag(AllMetal.JADE.storageBlocks)
            .item()
            .tag(AllMetal.JADE.itemStorageBlocks)
            .build()
            .register();

    public static final BlockEntry<Block> RAW_JADE_BLOCK = CreateOreExpansion.REGISTRATE
            .block("raw_jade_block", Block::new)
            .initialProperties(() -> Blocks.RAW_GOLD_BLOCK)
            .properties(p -> p.mapColor(MapColor.GLOW_LICHEN)
                    .requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(AllMetal.JADE.storageRawBlocks)
            .item()
            .tag(AllMetal.JADE.itemStorageRawBlocks)
            .build()
            .register();

    public static final BlockEntry<CasingBlock> JADE_CASING = CreateOreExpansion.REGISTRATE
            .block("jade_casing", CasingBlock::new)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_GREEN))
            .transform(BuilderTransformers.casing(() -> AllSpriteShifts.JADE_CASING))
            .register();

    public static final BlockEntry<Block> TOPAZ_ORE = CreateOreExpansion.REGISTRATE
            .block("topaz_ore", Block::new)
            .initialProperties(() -> Blocks.DIAMOND_ORE)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .tag(BlockTags.NEEDS_DIAMOND_TOOL)
            .tag(AllMetal.TOPAZ.blockOres)
            .loot((lt, block) -> {
                HolderLookup.RegistryLookup<Enchantment> enchantmentRegistryLookup = lt.getRegistries().lookupOrThrow(Registries.ENCHANTMENT);
                lt.add(
                        block,
                        lt.createSilkTouchDispatchTable(
                                block,
                                lt.applyExplosionDecay(
                                        block,
                                        LootItem.lootTableItem(AllItems.RAW_TOPAZ.get())
                                                .apply(ApplyBonusCount.addOreBonusCount(
                                                        enchantmentRegistryLookup.getOrThrow(Enchantments.FORTUNE)))
                                )
                        )
                );
            })
            .item()
            .tag(AllMetal.TOPAZ.itemOres)
            .build()
            .register();

    public static final BlockEntry<Block> DEEPSLATE_TOPAZ_ORE = CreateOreExpansion.REGISTRATE
            .block("deepslate_topaz_ore", Block::new)
            .initialProperties(() -> Blocks.DEEPSLATE_DIAMOND_ORE)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .tag(BlockTags.NEEDS_DIAMOND_TOOL)
            .tag(AllMetal.TOPAZ.blockOres)
            // 同理，也需要为深层矿石注册战利品，战利品由数据包定义，所以需要runData生成数据
            .loot((lt, block) -> {
                HolderLookup.RegistryLookup<Enchantment> enchantmentRegistryLookup = lt.getRegistries().lookupOrThrow(Registries.ENCHANTMENT);
                lt.add(
                        block,
                        lt.createSilkTouchDispatchTable(
                                block,
                                lt.applyExplosionDecay(
                                        block,
                                        LootItem.lootTableItem(AllItems.RAW_TOPAZ.get())
                                                .apply(ApplyBonusCount.addOreBonusCount(
                                                        enchantmentRegistryLookup.getOrThrow(Enchantments.FORTUNE)))
                                )
                        )
                );
            })
            .item()
            .tag(AllMetal.TOPAZ.itemOres)
            .build()
            .register();
    // 这里通过调用方法让Java加载这个类，触发类加载，这样Java才会加载字段
    // 这样注册方块才能真正注册，否则方块不会被注册

    public static final BlockEntry<Block> TOPAZ_BLOCK = CreateOreExpansion.REGISTRATE
            .block("topaz_block", Block::new)
            .initialProperties(() -> Blocks.DIAMOND_BLOCK)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_ORANGE)
                    .requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .tag(AllMetal.TOPAZ.storageBlocks)
            .item()
            .tag(AllMetal.TOPAZ.itemStorageBlocks)
            .build()
            .register();

    public static final BlockEntry<Block> RAW_TOPAZ_BLOCK = CreateOreExpansion.REGISTRATE
            .block("raw_topaz_block", Block::new)
            .initialProperties(() -> Blocks.RAW_GOLD_BLOCK)
            .properties(p -> p.mapColor(MapColor.GLOW_LICHEN)
                    .requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(AllMetal.TOPAZ.storageRawBlocks)
            .item()
            .tag(AllMetal.TOPAZ.itemStorageRawBlocks)
            .build()
            .register();

    public static final BlockEntry<Block> NETHER_SAPPHIRE_ORE = CreateOreExpansion.REGISTRATE
            .block("nether_sapphire_ore", Block::new)
            .initialProperties(() -> Blocks.ANCIENT_DEBRIS)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .tag(BlockTags.NEEDS_DIAMOND_TOOL)
            .tag(AllMetal.SAPPHIRE.blockOres)
            .loot((lt, block) -> {
                HolderLookup.RegistryLookup<Enchantment> enchantmentRegistryLookup = lt.getRegistries().lookupOrThrow(Registries.ENCHANTMENT);
                lt.add(
                        block,
                        lt.createSilkTouchDispatchTable(
                                block,
                                lt.applyExplosionDecay(
                                        block,
                                        LootItem.lootTableItem(AllItems.RAW_SAPPHIRE.get())
                                                .apply(ApplyBonusCount.addOreBonusCount(
                                                        enchantmentRegistryLookup.getOrThrow(Enchantments.FORTUNE)))
                                )
                        )
                );
            })
            .item()
            .tag(AllMetal.SAPPHIRE.itemOres)
            .build()
            .register();

    public static final BlockEntry<Block> SAPPHIRE_BLOCK = CreateOreExpansion.REGISTRATE
            .block("sapphire_block", Block::new)
            .initialProperties(() -> Blocks.DIAMOND_BLOCK)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_BLUE)
                    .requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.BEACON_BASE_BLOCKS)
            .tag(AllMetal.SAPPHIRE.storageBlocks)
            .item()
            .tag(AllMetal.SAPPHIRE.itemStorageBlocks)
            .build()
            .register();

    public static final BlockEntry<Block> RAW_SAPPHIRE_BLOCK = CreateOreExpansion.REGISTRATE
            .block("raw_sapphire_block", Block::new)
            .initialProperties(() -> Blocks.RAW_GOLD_BLOCK)
            .properties(p -> p.mapColor(MapColor.GLOW_LICHEN)
                    .requiresCorrectToolForDrops())
            .transform(pickaxeOnly())
            .tag(Tags.Blocks.STORAGE_BLOCKS)
            .tag(BlockTags.NEEDS_IRON_TOOL)
            .tag(AllMetal.SAPPHIRE.storageRawBlocks)
            .item()
            .tag(AllMetal.SAPPHIRE.itemStorageRawBlocks)
            .build()
            .register();
    public static void register() {}
}
