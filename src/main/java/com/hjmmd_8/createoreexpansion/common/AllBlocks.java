package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.charger.block.JadeCreateChargerBlock;
import com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlock;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;

import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile.ExistingModelFile;
import net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile;
import net.neoforged.neoforge.common.Tags;

/**
 * 方块注册（Registrate）。
 */
public final class AllBlocks {

	public static final BlockEntry<Block> JADE_ORE = CreateOreExpansion.REGISTRATE
		.block("jade_ore", Block::new)
		.initialProperties(() -> Blocks.DIAMOND_ORE)
		.tag(BlockTags.MINEABLE_WITH_PICKAXE)
		.tag(BlockTags.NEEDS_IRON_TOOL)
		.tag(AllMetalTags.JADE.blockOres)
		.loot((lt, block) -> {
			RegistryLookup<Enchantment> ench = lt.getRegistries()
				.lookupOrThrow(Registries.ENCHANTMENT);
			lt.add(block, lt.createSilkTouchDispatchTable(block,
				lt.applyExplosionDecay(block,
					LootItem.lootTableItem(AllItems.RAW_JADE.get())
						.apply(ApplyBonusCount.addOreBonusCount(ench.getOrThrow(Enchantments.FORTUNE))))));
		})
		.item()
		.tag(AllMetalTags.JADE.itemOres)
		.build()
		.register();

	public static final BlockEntry<Block> DEEPSLATE_JADE_ORE = CreateOreExpansion.REGISTRATE
		.block("deepslate_jade_ore", Block::new)
		.initialProperties(() -> Blocks.DEEPSLATE_DIAMOND_ORE)
		.tag(BlockTags.MINEABLE_WITH_PICKAXE)
		.tag(BlockTags.NEEDS_IRON_TOOL)
		.tag(AllMetalTags.JADE.blockOres)
		.loot((lt, block) -> {
			RegistryLookup<Enchantment> ench = lt.getRegistries()
				.lookupOrThrow(Registries.ENCHANTMENT);
			lt.add(block, lt.createSilkTouchDispatchTable(block,
				lt.applyExplosionDecay(block,
					LootItem.lootTableItem(AllItems.RAW_JADE.get())
						.apply(ApplyBonusCount.addOreBonusCount(ench.getOrThrow(Enchantments.FORTUNE))))));
		})
		.item()
		.tag(AllMetalTags.JADE.itemOres)
		.build()
		.register();

	public static final BlockEntry<Block> JADE_BLOCK = CreateOreExpansion.REGISTRATE
		.block("jade_block", Block::new)
		.initialProperties(() -> Blocks.DIAMOND_BLOCK)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_GREEN)
			.requiresCorrectToolForDrops())
		.transform(TagGen.pickaxeOnly())
		.tag(BlockTags.NEEDS_IRON_TOOL)
		.tag(Tags.Blocks.STORAGE_BLOCKS)
		.tag(BlockTags.BEACON_BASE_BLOCKS)
		.tag(AllMetalTags.JADE.storageBlocks)
		.item()
		.tag(AllMetalTags.JADE.itemStorageBlocks)
		.build()
		.register();

	public static final BlockEntry<Block> RAW_JADE_BLOCK = CreateOreExpansion.REGISTRATE
		.block("raw_jade_block", Block::new)
		.initialProperties(() -> Blocks.RAW_GOLD_BLOCK)
		.properties(p -> p.mapColor(MapColor.GLOW_LICHEN)
			.requiresCorrectToolForDrops())
		.transform(TagGen.pickaxeOnly())
		.tag(Tags.Blocks.STORAGE_BLOCKS)
		.tag(BlockTags.NEEDS_IRON_TOOL)
		.tag(AllMetalTags.JADE.storageRawBlocks)
		.item()
		.tag(AllMetalTags.JADE.itemStorageRawBlocks)
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
		.tag(AllMetalTags.TOPAZ.blockOres)
		.loot((lt, block) -> {
			RegistryLookup<Enchantment> ench = lt.getRegistries()
				.lookupOrThrow(Registries.ENCHANTMENT);
			lt.add(block, lt.createSilkTouchDispatchTable(block,
				lt.applyExplosionDecay(block,
					LootItem.lootTableItem(AllItems.RAW_TOPAZ.get())
						.apply(ApplyBonusCount.addOreBonusCount(ench.getOrThrow(Enchantments.FORTUNE))))));
		})
		.item()
		.tag(AllMetalTags.TOPAZ.itemOres)
		.build()
		.register();

	public static final BlockEntry<Block> DEEPSLATE_TOPAZ_ORE = CreateOreExpansion.REGISTRATE
		.block("deepslate_topaz_ore", Block::new)
		.initialProperties(() -> Blocks.DEEPSLATE_DIAMOND_ORE)
		.tag(BlockTags.MINEABLE_WITH_PICKAXE)
		.tag(BlockTags.NEEDS_DIAMOND_TOOL)
		.tag(AllMetalTags.TOPAZ.blockOres)
		.loot((lt, block) -> {
			RegistryLookup<Enchantment> ench = lt.getRegistries()
				.lookupOrThrow(Registries.ENCHANTMENT);
			lt.add(block, lt.createSilkTouchDispatchTable(block,
				lt.applyExplosionDecay(block,
					LootItem.lootTableItem(AllItems.RAW_TOPAZ.get())
						.apply(ApplyBonusCount.addOreBonusCount(ench.getOrThrow(Enchantments.FORTUNE))))));
		})
		.item()
		.tag(AllMetalTags.TOPAZ.itemOres)
		.build()
		.register();

	public static final BlockEntry<Block> TOPAZ_BLOCK = CreateOreExpansion.REGISTRATE
		.block("topaz_block", Block::new)
		.initialProperties(() -> Blocks.DIAMOND_BLOCK)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_ORANGE)
			.requiresCorrectToolForDrops())
		.transform(TagGen.pickaxeOnly())
		.tag(BlockTags.NEEDS_IRON_TOOL)
		.tag(Tags.Blocks.STORAGE_BLOCKS)
		.tag(BlockTags.BEACON_BASE_BLOCKS)
		.tag(AllMetalTags.TOPAZ.storageBlocks)
		.item()
		.tag(AllMetalTags.TOPAZ.itemStorageBlocks)
		.build()
		.register();

	public static final BlockEntry<Block> RAW_TOPAZ_BLOCK = CreateOreExpansion.REGISTRATE
		.block("raw_topaz_block", Block::new)
		.initialProperties(() -> Blocks.RAW_GOLD_BLOCK)
		.properties(p -> p.mapColor(MapColor.GLOW_LICHEN)
			.requiresCorrectToolForDrops())
		.transform(TagGen.pickaxeOnly())
		.tag(Tags.Blocks.STORAGE_BLOCKS)
		.tag(BlockTags.NEEDS_IRON_TOOL)
		.tag(AllMetalTags.TOPAZ.storageRawBlocks)
		.item()
		.tag(AllMetalTags.TOPAZ.itemStorageRawBlocks)
		.build()
		.register();

	public static final BlockEntry<Block> NETHER_SAPPHIRE_ORE = CreateOreExpansion.REGISTRATE
		.block("nether_sapphire_ore", Block::new)
		.initialProperties(() -> Blocks.ANCIENT_DEBRIS)
		.tag(BlockTags.MINEABLE_WITH_PICKAXE)
		.tag(BlockTags.NEEDS_DIAMOND_TOOL)
		.tag(AllMetalTags.SAPPHIRE.blockOres)
		.loot((lt, block) -> {
			RegistryLookup<Enchantment> ench = lt.getRegistries()
				.lookupOrThrow(Registries.ENCHANTMENT);
			lt.add(block, lt.createSilkTouchDispatchTable(block,
				lt.applyExplosionDecay(block,
					LootItem.lootTableItem(AllItems.RAW_SAPPHIRE.get())
						.apply(ApplyBonusCount.addOreBonusCount(ench.getOrThrow(Enchantments.FORTUNE))))));
		})
		.item()
		.tag(AllMetalTags.SAPPHIRE.itemOres)
		.build()
		.register();

	public static final BlockEntry<Block> SAPPHIRE_BLOCK = CreateOreExpansion.REGISTRATE
		.block("sapphire_block", Block::new)
		.initialProperties(() -> Blocks.DIAMOND_BLOCK)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_BLUE)
			.requiresCorrectToolForDrops())
		.transform(TagGen.pickaxeOnly())
		.tag(BlockTags.NEEDS_IRON_TOOL)
		.tag(Tags.Blocks.STORAGE_BLOCKS)
		.tag(BlockTags.BEACON_BASE_BLOCKS)
		.tag(AllMetalTags.SAPPHIRE.storageBlocks)
		.item()
		.tag(AllMetalTags.SAPPHIRE.itemStorageBlocks)
		.build()
		.register();

	public static final BlockEntry<Block> RAW_SAPPHIRE_BLOCK = CreateOreExpansion.REGISTRATE
		.block("raw_sapphire_block", Block::new)
		.initialProperties(() -> Blocks.RAW_GOLD_BLOCK)
		.properties(p -> p.mapColor(MapColor.GLOW_LICHEN)
			.requiresCorrectToolForDrops())
		.transform(TagGen.pickaxeOnly())
		.tag(Tags.Blocks.STORAGE_BLOCKS)
		.tag(BlockTags.NEEDS_IRON_TOOL)
		.tag(AllMetalTags.SAPPHIRE.storageRawBlocks)
		.item()
		.tag(AllMetalTags.SAPPHIRE.itemStorageRawBlocks)
		.build()
		.register();

	public static final BlockEntry<Block> END_STELLARSTONE_ORE = CreateOreExpansion.REGISTRATE
		.block("end_stellarstone_ore", Block::new)
		.initialProperties(() -> Blocks.ANCIENT_DEBRIS)
		.tag(BlockTags.MINEABLE_WITH_PICKAXE)
		.tag(BlockTags.NEEDS_DIAMOND_TOOL)
		.tag(AllMetalTags.STELLARSTONE.blockOres)
		.loot((lt, block) -> {
			RegistryLookup<Enchantment> ench = lt.getRegistries()
				.lookupOrThrow(Registries.ENCHANTMENT);
			lt.add(block, lt.createSilkTouchDispatchTable(block,
				lt.applyExplosionDecay(block,
					LootItem.lootTableItem(AllItems.RAW_STELLARSTONE.get())
						.apply(ApplyBonusCount.addOreBonusCount(ench.getOrThrow(Enchantments.FORTUNE))))));
		})
		.item()
		.tag(AllMetalTags.STELLARSTONE.itemOres)
		.build()
		.register();

	public static final BlockEntry<Block> STELLARSTONE_BLOCK = CreateOreExpansion.REGISTRATE
		.block("stellarstone_block", Block::new)
		.initialProperties(() -> Blocks.DIAMOND_BLOCK)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_BLUE)
			.requiresCorrectToolForDrops())
		.transform(TagGen.pickaxeOnly())
		.tag(BlockTags.NEEDS_IRON_TOOL)
		.tag(Tags.Blocks.STORAGE_BLOCKS)
		.tag(BlockTags.BEACON_BASE_BLOCKS)
		.tag(AllMetalTags.STELLARSTONE.storageBlocks)
		.item()
		.tag(AllMetalTags.STELLARSTONE.itemStorageBlocks)
		.build()
		.register();

	public static final BlockEntry<Block> RAW_STELLARSTONE_BLOCK = CreateOreExpansion.REGISTRATE
		.block("raw_stellarstone_block", Block::new)
		.initialProperties(() -> Blocks.RAW_GOLD_BLOCK)
		.properties(p -> p.mapColor(MapColor.GLOW_LICHEN)
			.requiresCorrectToolForDrops())
		.transform(TagGen.pickaxeOnly())
		.tag(Tags.Blocks.STORAGE_BLOCKS)
		.tag(BlockTags.NEEDS_IRON_TOOL)
		.tag(AllMetalTags.STELLARSTONE.storageRawBlocks)
		.item()
		.tag(AllMetalTags.STELLARSTONE.itemStorageRawBlocks)
		.build()
		.register();

	public static final BlockEntry<Block> THUNDERITE_BLOCK = CreateOreExpansion.REGISTRATE
		.block("thunderite_block", Block::new)
		.initialProperties(() -> Blocks.DIAMOND_BLOCK)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_BLUE)
			.requiresCorrectToolForDrops())
		.transform(TagGen.pickaxeOnly())
		.tag(BlockTags.NEEDS_IRON_TOOL)
		.tag(Tags.Blocks.STORAGE_BLOCKS)
		.tag(BlockTags.BEACON_BASE_BLOCKS)
		.tag(AllMetalTags.THUNDERITE.storageBlocks)
		.item()
		.tag(AllMetalTags.THUNDERITE.itemStorageBlocks)
		.build()
		.register();

	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<PowerAngleGrinderBlock> POWER_ANGLE_GRINDER = CreateOreExpansion.REGISTRATE
		.block("power_angle_grinder", PowerAngleGrinderBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.PODZOL))
		.properties(p -> p.noOcclusion())
		.properties(p -> p.isRedstoneConductor((state, level, pos) -> false))
		.addLayer(() -> () -> RenderType.cutoutMipped())
		.transform(TagGen.axeOrPickaxe())
		.blockstate((ctx, prov) -> {
			ExistingModelFile closed = prov.models()
				.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
					"block/power_angle_grinder/power_angle_grinde"));
			ExistingModelFile open = prov.models()
				.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
					"block/power_angle_grinder/power_angle_grinder_rotated"));
			VariantBlockStateBuilder vb = prov.getVariantBuilder(ctx.get());
			for (Direction dir : Plane.HORIZONTAL) {
				int y = (int) dir.toYRot();
				vb.partialState()
					.with(HorizontalKineticBlock.HORIZONTAL_FACING, dir)
					.with(PowerAngleGrinderBlock.OPEN, false)
					.modelForState()
					.modelFile(closed)
					.rotationY(y)
					.addModel();
				vb.partialState()
					.with(HorizontalKineticBlock.HORIZONTAL_FACING, dir)
					.with(PowerAngleGrinderBlock.OPEN, true)
					.modelForState()
					.modelFile(open)
					.rotationY(y)
					.addModel();
			}
		})
		.onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> 8.0))
		.item()
		.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("power_angle_grinder"))
			.parent(new UncheckedModelFile("createoreexpansion:block/power_angle_grinder/power_angle_grinder_item")))
		.build()
		.register();

	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<JadeCreateChargerBlock> JADE_CREATE_CHARGER = CreateOreExpansion.REGISTRATE
		.block("jade_create_charger", JadeCreateChargerBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_GREEN))
		.properties(p -> p.noOcclusion())
		.properties(p -> p.isRedstoneConductor((state, level, pos) -> false))
		.addLayer(() -> () -> RenderType.cutoutMipped())
		.transform(TagGen.axeOrPickaxe())
		.blockstate((ctx, prov) -> {
			ExistingModelFile[] models = new ExistingModelFile[4];
			for (int i = 0; i < 4; i++)
				models[i] = prov.models()
					.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
						"block/jade_create_charger/jade_create_charger_" + i));
			VariantBlockStateBuilder vb = prov.getVariantBuilder(ctx.get());
			for (Direction dir : Direction.values()) {
				// 模型默认正面为 +Y（发射头朝上）：竖直放置（UP）用原样，DOWN 转 180，
				// 水平放置先绕 X 躺下（+Y→+Z）再绕 Y 转向 facing
				int xRot = dir == Direction.UP ? 0
					: dir == Direction.DOWN ? 180
						: dir.getAxis()
							.isHorizontal() ? 270 : 0;
				int yRot = dir.getAxis()
					.isHorizontal() ? (int) dir.toYRot() : 0;
				for (int mode = 0; mode < 4; mode++) {
					vb.partialState()
						.with(DirectionalKineticBlock.FACING, dir)
						.with(JadeCreateChargerBlock.MODE, mode)
						.modelForState()
						.modelFile(models[mode])
						.rotationX(xRot)
						.rotationY(yRot)
						.addModel();
				}
			}
		})
		.onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> 4.0))
		.item()
		.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("jade_create_charger"))
			.parent(new UncheckedModelFile("createoreexpansion:block/jade_create_charger/jade_create_charger_item")))
		.build()
		.register();

	public static void register() {
	}

	private AllBlocks() {
	}
}
