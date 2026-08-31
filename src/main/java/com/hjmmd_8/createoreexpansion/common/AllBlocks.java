package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.charger.block.JadeCreateChargerBlock;
import com.hjmmd_8.createoreexpansion.content.crystal.CrystalBuddingBlock;
import com.hjmmd_8.createoreexpansion.content.crystal.CrystalClusterBlock;
import com.hjmmd_8.createoreexpansion.content.crystal.CrystalGrowthConfigs;
import com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlock;
import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveRegulatorBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.SixFaceDisperserBlock;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile.ExistingModelFile;
import net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile;
import net.neoforged.neoforge.common.Tags;

import java.util.function.Supplier;

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

	public static final BlockEntry<Block> RUBY_BLOCK = CreateOreExpansion.REGISTRATE
		.block("ruby_block", Block::new)
		.initialProperties(() -> Blocks.DIAMOND_BLOCK)
		.properties(p -> p.mapColor(MapColor.COLOR_RED)
			.requiresCorrectToolForDrops())
		.transform(TagGen.pickaxeOnly())
		.tag(BlockTags.NEEDS_IRON_TOOL)
		.tag(Tags.Blocks.STORAGE_BLOCKS)
		.tag(BlockTags.BEACON_BASE_BLOCKS)
		.tag(AllMetalTags.RUBY.storageBlocks)
		.item()
		.tag(AllMetalTags.RUBY.itemStorageBlocks)
		.build()
		.register();

	public static final BlockEntry<Block> SANCTSTONE_BLOCK = CreateOreExpansion.REGISTRATE
		.block("sanctstone_block", Block::new)
		.initialProperties(() -> Blocks.DIAMOND_BLOCK)
		.properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY)
			.requiresCorrectToolForDrops())
		.transform(TagGen.pickaxeOnly())
		.tag(BlockTags.NEEDS_IRON_TOOL)
		.tag(Tags.Blocks.STORAGE_BLOCKS)
		.tag(BlockTags.BEACON_BASE_BLOCKS)
		.tag(AllMetalTags.SANCTSTONE.storageBlocks)
		.item()
		.tag(AllMetalTags.SANCTSTONE.itemStorageBlocks)
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

	/** 能量调级器：六向应力机器（齿轮轴沿 FACING），承接翡翠应力充能器能量波调级。 */
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<EnergyWaveRegulatorBlock> ENERGY_WAVE_REGULATOR = CreateOreExpansion.REGISTRATE
		.block("energy_wave_regulator", EnergyWaveRegulatorBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_GREEN))
		.properties(p -> p.noOcclusion())
		.properties(p -> p.isRedstoneConductor((state, level, pos) -> false))
		.addLayer(() -> () -> RenderType.cutoutMipped())
		.transform(TagGen.axeOrPickaxe())
		.blockstate((ctx, prov) -> {
			// 机座模型按顶/底能量接收面板 open/close 选 4 个变体（_00=关/关、_01=关/开、_10=开/关、_11=开/开）；
			// 齿轮由方块实体渲染器动态添加。六向 FACING 旋转：
			// 模型默认正面为 +Y——竖直放置（UP）用原样，DOWN 转 180，水平先绕 X 躺下再绕 Y 转向
			ExistingModelFile[] models = new ExistingModelFile[4];
			for (int i = 0; i < 4; i++)
				models[i] = prov.models()
					.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
						"block/energy_wave_machine/energy_wave_regulator_" + i));
			VariantBlockStateBuilder vb = prov.getVariantBuilder(ctx.get());
			for (Direction dir : Direction.values()) {
				int xRot = dir == Direction.UP ? 0
					: dir == Direction.DOWN ? 180
						: dir.getAxis()
							.isHorizontal() ? 270 : 0;
				int yRot = dir.getAxis()
					.isHorizontal() ? (int) dir.toYRot() : 0;
				for (int top = 0; top < 2; top++) {
					for (int bottom = 0; bottom < 2; bottom++) {
						vb.partialState()
							.with(DirectionalKineticBlock.FACING, dir)
							.with(EnergyWaveRegulatorBlock.RECEIVER_TOP, top == 1)
							.with(EnergyWaveRegulatorBlock.RECEIVER_BOTTOM, bottom == 1)
							.modelForState()
							.modelFile(models[top * 2 + bottom])
							.rotationX(xRot)
							.rotationY(yRot)
							.addModel();
					}
				}
			}
		})
		.onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> 4.0))
		.item()
		.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("energy_wave_regulator"))
			.parent(new UncheckedModelFile("createoreexpansion:block/energy_wave_machine/energy_wave_regulator_item")))
		.build()
		.register();

	/** 能量波差器：无应力被动机器，模型上下翡翠机壳、四面能量接收面关闭材质；
	 * 六向 FACING 旋转（三种朝向与调级器一致），4 侧面开口可独立开关，无需方块实体/渲染器。 */
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<EnergyWaveDisperserBlock> ENERGY_WAVE_DISPERSER = CreateOreExpansion.REGISTRATE
		.block("energy_wave_disperser", EnergyWaveDisperserBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_GREEN))
		.properties(p -> p.noOcclusion())
		.properties(p -> p.isRedstoneConductor((state, level, pos) -> false))
		.addLayer(() -> () -> RenderType.cutoutMipped())
		.transform(TagGen.axeOrPickaxe())
		.blockstate((ctx, prov) -> {
			// 4 侧面开口独立开关 → 16 个变体模型（索引 = north*1 + east*2 + south*4 + west*8）；
			// 变体负责 4 侧面的 wave_receiver_close/open 纹理切换；灯盘（up/down）纹理统一
			// 为 disperser_lamp_0（全灭底纹），亮灯位由方块实体渲染器按状态叠加 light.png。
			// 六向 FACING 旋转（与调级器同款）：模型默认正面为 +Y——UP 原样、DOWN 转 180、水平躺倒再转向
			ExistingModelFile[] models = new ExistingModelFile[16];
			for (int i = 0; i < 16; i++)
				models[i] = prov.models()
					.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
						"block/energy_wave_machine/energy_wave_disperser_" + i));
			VariantBlockStateBuilder vb = prov.getVariantBuilder(ctx.get());
			for (Direction dir : Direction.values()) {
				int xRot = dir == Direction.UP ? 0
					: dir == Direction.DOWN ? 180
						: dir.getAxis()
							.isHorizontal() ? 270 : 0;
				int yRot = dir.getAxis()
					.isHorizontal() ? (int) dir.toYRot() : 0;
				for (int n = 0; n < 2; n++) {
					for (int e = 0; e < 2; e++) {
						for (int s = 0; s < 2; s++) {
							for (int w = 0; w < 2; w++) {
								vb.partialState()
									.with(EnergyWaveDisperserBlock.FACING, dir)
									.with(EnergyWaveDisperserBlock.NORTH, n == 1)
									.with(EnergyWaveDisperserBlock.EAST, e == 1)
									.with(EnergyWaveDisperserBlock.SOUTH, s == 1)
									.with(EnergyWaveDisperserBlock.WEST, w == 1)
									.modelForState()
									.modelFile(models[n * 1 + e * 2 + s * 4 + w * 8])
									.rotationX(xRot)
									.rotationY(yRot)
									.addModel();
							}
						}
					}
				}
			}
		})
		.item()
		.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("energy_wave_disperser"))
			.parent(new UncheckedModelFile("createoreexpansion:block/energy_wave_machine/energy_wave_disperser_item")))
		.build()
		.register();

	/** 六面能量波差器：无朝向固定机器，6 面全部为能量接收面板（wave_receiver close/open）独立开关；
	 * 5/6 面开口时波分裂为降二级子波（判定见 SixFaceDispersal）。 */
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<SixFaceDisperserBlock> SIX_FACE_DISPERSER = CreateOreExpansion.REGISTRATE
		.block("six_face_disperser", SixFaceDisperserBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_GREEN))
		.properties(p -> p.noOcclusion())
		.properties(p -> p.isRedstoneConductor((state, level, pos) -> false))
		.addLayer(() -> () -> RenderType.cutoutMipped())
		.transform(TagGen.axeOrPickaxe())
		.blockstate((ctx, prov) -> {
			// 6 面开口独立开关 → 64 个变体模型（索引 = up*1 + down*2 + north*4 + east*8 + south*16 + west*32）；
			// 每个变体 6 面直接 close/open 纹理（同四面差波器方案，整面切换，无需渲染器）
			ExistingModelFile[] models = new ExistingModelFile[64];
			for (int i = 0; i < 64; i++)
				models[i] = prov.models()
					.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
						"block/energy_wave_machine/six_face_disperser_" + i));
			VariantBlockStateBuilder vb = prov.getVariantBuilder(ctx.get());
			for (int u = 0; u < 2; u++) {
				for (int d = 0; d < 2; d++) {
					for (int n = 0; n < 2; n++) {
						for (int e = 0; e < 2; e++) {
							for (int s = 0; s < 2; s++) {
								for (int w = 0; w < 2; w++) {
									vb.partialState()
										.with(SixFaceDisperserBlock.UP, u == 1)
										.with(SixFaceDisperserBlock.DOWN, d == 1)
										.with(SixFaceDisperserBlock.NORTH, n == 1)
										.with(SixFaceDisperserBlock.EAST, e == 1)
										.with(SixFaceDisperserBlock.SOUTH, s == 1)
										.with(SixFaceDisperserBlock.WEST, w == 1)
										.modelForState()
										.modelFile(models[u * 1 + d * 2 + n * 4 + e * 8 + s * 16 + w * 32])
										.addModel();
								}
							}
						}
					}
				}
			}
		})
		.item()
		.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("six_face_disperser"))
			.parent(new UncheckedModelFile("createoreexpansion:block/energy_wave_machine/six_face_disperser")))
		.build()
		.register();

	/** 强化避雷针：继承原版 LightningRodBlock（全部原版行为保留），叠加伽马能量波充能；
	 * 加入原版 lightning_rods tag（三叉戟引雷、铁砧工艺等交互正常作用）。 */
	public static final BlockEntry<ReinforcedLightningRodBlock> REINFORCED_LIGHTNING_ROD = CreateOreExpansion.REGISTRATE
		.block("reinforced_lightning_rod", ReinforcedLightningRodBlock::new)
		.initialProperties(SharedProperties::copperMetal)
		.properties(p -> p.requiresCorrectToolForDrops())
		// 原版 lightning_rods tag（BlockTags 无此常量，用 create 显式创建）
		.tag(BlockTags.create(ResourceLocation.withDefaultNamespace("lightning_rods")))
		.blockstate((ctx, prov) -> {
			// 仿原版避雷针 blockstate：facing 六向 + powered 两态，模型普通/强化各一
			VariantBlockStateBuilder vb = prov.getVariantBuilder(ctx.get());
			for (Direction dir : Direction.values()) {
				for (boolean powered : new boolean[] { false, true }) {
					int xRot = dir == Direction.DOWN ? 180 : dir == Direction.UP ? 0 : 90;
					int yRot = switch (dir) {
						case NORTH -> 0;
						case SOUTH -> 180;
						case WEST -> 270;
						case EAST -> 90;
						default -> 0;
					};
					String name = powered ? "reinforced_lightning_rod_powered" : "reinforced_lightning_rod";
					vb.partialState()
						.with(BlockStateProperties.FACING, dir)
						.with(BlockStateProperties.POWERED, powered)
						.modelForState()
						.modelFile(prov.models()
							.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
								"block/reinforced_lightning_rod/" + name)))
						.rotationX(xRot)
						.rotationY(yRot)
						.addModel();
				}
			}
		})
		.item()
		.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("reinforced_lightning_rod"))
			.parent(new UncheckedModelFile("createoreexpansion:block/reinforced_lightning_rod/reinforced_lightning_rod")))
		.build()
		.register();

	// ========== 可生长水晶（翡翠/黄玉/蓝宝石/星辉石）——继承原版紫水晶机制，AE2 催生器可加速 ==========
	// 每种水晶 5 个方块：小芽 → 中芽 → 大芽 → 簇（随机刻逐级生长），芽床（随机刻 + 方块实体进度双轨生芽）。
	// 生长速度由 CrystalGrowthConfigs（模组内统一修改点）按宝石独立自定义。
	// 掉落：中芽掉小块 ×1、大芽掉小块 ×2、成熟簇掉大块 ×1；时运额外产物有概率（仿钻石矿），
	// 中/大芽额外为小块、成熟簇额外只能是小块；小芽无掉落；芽床普通破坏掉落自身。

	/** 翡翠水晶小芽（仿原版小紫水晶芽尺寸） */
	public static final BlockEntry<CrystalClusterBlock> JADE_SMALL_BUD = crystalBud("jade_small_bud", 3, 4,
		() -> AllBlocks.JADE_MEDIUM_BUD.get(), null, 0, null, CrystalGrowthConfigs.JADE_BUD_STAGE_SECONDS);
	/** 翡翠水晶中芽（挖掘掉落小块翡翠 ×1，时运额外小块有概率） */
	public static final BlockEntry<CrystalClusterBlock> JADE_MEDIUM_BUD = crystalBud("jade_medium_bud", 4, 3,
		() -> AllBlocks.JADE_LARGE_BUD.get(), () -> AllItems.JADE_SMALL_SHARD.get(), 1, null, CrystalGrowthConfigs.JADE_BUD_STAGE_SECONDS);
	/** 翡翠水晶大芽（挖掘掉落小块翡翠 ×2，时运额外小块有概率） */
	public static final BlockEntry<CrystalClusterBlock> JADE_LARGE_BUD = crystalBud("jade_large_bud", 5, 2,
		() -> AllBlocks.JADE_CLUSTER.get(), () -> AllItems.JADE_SMALL_SHARD.get(), 2, null, CrystalGrowthConfigs.JADE_BUD_STAGE_SECONDS);
	/** 翡翠水晶簇（成熟，挖掘掉落大块翡翠 ×1，时运额外只能是小块翡翠） */
	public static final BlockEntry<CrystalClusterBlock> JADE_CLUSTER = crystalBud("jade_cluster", 7, 3,
		null, () -> AllItems.JADE_BIG_SHARD.get(), 1, () -> AllItems.JADE_SMALL_SHARD.get(), CrystalGrowthConfigs.JADE_BUD_STAGE_SECONDS);
	/** 翡翠水晶芽床（母岩，挖掉只掉粗翡翠，不可搬迁） */
	public static final BlockEntry<CrystalBuddingBlock> JADE_BUDDING_BLOCK = crystalBudding("jade_budding_block",
		() -> AllBlocks.JADE_SMALL_BUD.get(), MapColor.TERRACOTTA_GREEN, CrystalGrowthConfigs.JADE_BUDDING_SECONDS,
		() -> AllItems.RAW_JADE.get());

	/** 黄玉水晶小芽 */
	public static final BlockEntry<CrystalClusterBlock> TOPAZ_SMALL_BUD = crystalBud("topaz_small_bud", 3, 4,
		() -> AllBlocks.TOPAZ_MEDIUM_BUD.get(), null, 0, null, CrystalGrowthConfigs.TOPAZ_BUD_STAGE_SECONDS);
	/** 黄玉水晶中芽（挖掘掉落小块黄玉 ×1，时运额外小块有概率） */
	public static final BlockEntry<CrystalClusterBlock> TOPAZ_MEDIUM_BUD = crystalBud("topaz_medium_bud", 4, 3,
		() -> AllBlocks.TOPAZ_LARGE_BUD.get(), () -> AllItems.TOPAZ_SMALL_SHARD.get(), 1, null, CrystalGrowthConfigs.TOPAZ_BUD_STAGE_SECONDS);
	/** 黄玉水晶大芽（挖掘掉落小块黄玉 ×2，时运额外小块有概率） */
	public static final BlockEntry<CrystalClusterBlock> TOPAZ_LARGE_BUD = crystalBud("topaz_large_bud", 5, 2,
		() -> AllBlocks.TOPAZ_CLUSTER.get(), () -> AllItems.TOPAZ_SMALL_SHARD.get(), 2, null, CrystalGrowthConfigs.TOPAZ_BUD_STAGE_SECONDS);
	/** 黄玉水晶簇（成熟，挖掘掉落大块黄玉 ×1，时运额外只能是小块黄玉） */
	public static final BlockEntry<CrystalClusterBlock> TOPAZ_CLUSTER = crystalBud("topaz_cluster", 7, 3,
		null, () -> AllItems.TOPAZ_BIG_SHARD.get(), 1, () -> AllItems.TOPAZ_SMALL_SHARD.get(), CrystalGrowthConfigs.TOPAZ_BUD_STAGE_SECONDS);
	/** 黄玉水晶芽床（母岩，挖掉只掉粗黄玉，不可搬迁） */
	public static final BlockEntry<CrystalBuddingBlock> TOPAZ_BUDDING_BLOCK = crystalBudding("topaz_budding_block",
		() -> AllBlocks.TOPAZ_SMALL_BUD.get(), MapColor.TERRACOTTA_ORANGE, CrystalGrowthConfigs.TOPAZ_BUDDING_SECONDS,
		() -> AllItems.RAW_TOPAZ.get());

	/** 蓝宝石水晶小芽 */
	public static final BlockEntry<CrystalClusterBlock> SAPPHIRE_SMALL_BUD = crystalBud("sapphire_small_bud", 3, 4,
		() -> AllBlocks.SAPPHIRE_MEDIUM_BUD.get(), null, 0, null, CrystalGrowthConfigs.SAPPHIRE_BUD_STAGE_SECONDS);
	/** 蓝宝石水晶中芽（挖掘掉落小块蓝宝石 ×1，时运额外小块有概率） */
	public static final BlockEntry<CrystalClusterBlock> SAPPHIRE_MEDIUM_BUD = crystalBud("sapphire_medium_bud", 4, 3,
		() -> AllBlocks.SAPPHIRE_LARGE_BUD.get(), () -> AllItems.SAPPHIRE_SMALL_SHARD.get(), 1, null, CrystalGrowthConfigs.SAPPHIRE_BUD_STAGE_SECONDS);
	/** 蓝宝石水晶大芽（挖掘掉落小块蓝宝石 ×2，时运额外小块有概率） */
	public static final BlockEntry<CrystalClusterBlock> SAPPHIRE_LARGE_BUD = crystalBud("sapphire_large_bud", 5, 2,
		() -> AllBlocks.SAPPHIRE_CLUSTER.get(), () -> AllItems.SAPPHIRE_SMALL_SHARD.get(), 2, null, CrystalGrowthConfigs.SAPPHIRE_BUD_STAGE_SECONDS);
	/** 蓝宝石水晶簇（成熟，挖掘掉落大块蓝宝石 ×1，时运额外只能是小块蓝宝石） */
	public static final BlockEntry<CrystalClusterBlock> SAPPHIRE_CLUSTER = crystalBud("sapphire_cluster", 7, 3,
		null, () -> AllItems.SAPPHIRE_BIG_SHARD.get(), 1, () -> AllItems.SAPPHIRE_SMALL_SHARD.get(), CrystalGrowthConfigs.SAPPHIRE_BUD_STAGE_SECONDS);
	/** 蓝宝石水晶芽床（母岩，挖掉只掉粗蓝宝石，不可搬迁） */
	public static final BlockEntry<CrystalBuddingBlock> SAPPHIRE_BUDDING_BLOCK = crystalBudding("sapphire_budding_block",
		() -> AllBlocks.SAPPHIRE_SMALL_BUD.get(), MapColor.TERRACOTTA_BLUE, CrystalGrowthConfigs.SAPPHIRE_BUDDING_SECONDS,
		() -> AllItems.RAW_SAPPHIRE.get());

	/** 星辉石水晶小芽 */
	public static final BlockEntry<CrystalClusterBlock> STELLARSTONE_SMALL_BUD = crystalBud("stellarstone_small_bud", 3, 4,
		() -> AllBlocks.STELLARSTONE_MEDIUM_BUD.get(), null, 0, null, CrystalGrowthConfigs.STELLARSTONE_BUD_STAGE_SECONDS);
	/** 星辉石水晶中芽（挖掘掉落小块星辉石 ×1，时运额外小块有概率） */
	public static final BlockEntry<CrystalClusterBlock> STELLARSTONE_MEDIUM_BUD = crystalBud("stellarstone_medium_bud", 4, 3,
		() -> AllBlocks.STELLARSTONE_LARGE_BUD.get(), () -> AllItems.STELLARSTONE_SMALL_SHARD.get(), 1, null, CrystalGrowthConfigs.STELLARSTONE_BUD_STAGE_SECONDS);
	/** 星辉石水晶大芽（挖掘掉落小块星辉石 ×2，时运额外小块有概率） */
	public static final BlockEntry<CrystalClusterBlock> STELLARSTONE_LARGE_BUD = crystalBud("stellarstone_large_bud", 5, 2,
		() -> AllBlocks.STELLARSTONE_CLUSTER.get(), () -> AllItems.STELLARSTONE_SMALL_SHARD.get(), 2, null, CrystalGrowthConfigs.STELLARSTONE_BUD_STAGE_SECONDS);
	/** 星辉石水晶簇（成熟，挖掘掉落大块星辉石 ×1，时运额外只能是小块星辉石） */
	public static final BlockEntry<CrystalClusterBlock> STELLARSTONE_CLUSTER = crystalBud("stellarstone_cluster", 7, 3,
		null, () -> AllItems.STELLARSTONE_BIG_SHARD.get(), 1, () -> AllItems.STELLARSTONE_SMALL_SHARD.get(), CrystalGrowthConfigs.STELLARSTONE_BUD_STAGE_SECONDS);
	/** 星辉石水晶芽床（母岩，挖掉只掉粗星辉石，不可搬迁） */
	public static final BlockEntry<CrystalBuddingBlock> STELLARSTONE_BUDDING_BLOCK = crystalBudding("stellarstone_budding_block",
		() -> AllBlocks.STELLARSTONE_SMALL_BUD.get(), MapColor.COLOR_PURPLE, CrystalGrowthConfigs.STELLARSTONE_BUDDING_SECONDS,
		() -> AllItems.RAW_STELLARSTONE.get());

	/**
	 * 注册水晶芽/簇方块（小/中/大芽可生长到下一阶段；簇由 nextStage=null 标记）。
	 *
	 * <p>继承原版 {@link AmethystClusterBlock}（碰撞箱/支撑/含水全保留），
	 * 每次随机刻按 CrystalGrowthConfigs 配置的「每阶段秒数」换算推进概率；加入原版
	 * c:buds / c:clusters tag，兼容 AE2 催生器对原版芽簇的加速判定。</p>
	 *
	 * <p>掉落规则（dropItem/dropCount/bonusItem 决定）：中芽掉小块宝石 ×1、大芽掉小块宝石 ×2、
	 * 成熟簇掉大块宝石 ×1；时运额外产物<b>有概率</b>掉落（仿原版钻石矿 ore_drops 公式，
	 * 每级时运随机 0~等级 个，非必加）——中/大芽额外仍为小块（bonusItem=null 时同 dropItem），
	 * 成熟簇额外只能是小块宝石（bonusItem=小块，大块不随时运增加）。小芽无掉落。
	 * 仿原版：只有最终簇注册物品（创造栏/JEI 可见）。</p>
	 *
	 * @param name      方块名
	 * @param height    模型高度（px）
	 * @param xz        模型 xz 偏移（px）
	 * @param nextStage 下一阶段方块（null = 最终簇）
	 * @param dropItem  挖掘掉落物（null = 无掉落）
	 * @param dropCount 基础掉落数量
	 * @param bonusItem 时运额外掉落物（null = 与 dropItem 相同）
	 * @param stageGrowSeconds 该宝石芽每阶段平均生长秒数（CrystalGrowthConfigs）
	 */
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutout 渲染层
	private static BlockEntry<CrystalClusterBlock> crystalBud(String name, int height, int xz,
			Supplier<Block> nextStage, Supplier<? extends Item> dropItem, int dropCount,
			Supplier<? extends Item> bonusItem, float stageGrowSeconds) {
		var builder = CreateOreExpansion.REGISTRATE
				.block(name, p -> new CrystalClusterBlock(height, xz, p, nextStage, stageGrowSeconds))
				.initialProperties(() -> Blocks.SMALL_AMETHYST_BUD)
				.properties(p -> p.randomTicks())
				.tag(BlockTags.MINEABLE_WITH_PICKAXE)
				.tag(nextStage == null ? Tags.Blocks.CLUSTERS : Tags.Blocks.BUDS)
				.addLayer(() -> () -> RenderType.cutout())
				.blockstate((ctx, prov) -> {
					VariantBlockStateBuilder vb = prov.getVariantBuilder(ctx.get());
					ModelFile model = prov.models()
							.cross(ctx.getName(), prov.modLoc("block/" + ctx.getName()));
					for (Direction dir : Direction.values()) {
						int x = dir == Direction.DOWN ? 180 : dir.getAxis()
								.isHorizontal() ? 90 : 0;
						int y = switch (dir) {
							case NORTH -> 0;
							case SOUTH -> 180;
							case WEST -> 270;
							case EAST -> 90;
							default -> 0;
						};
						vb.partialState()
								.with(AmethystClusterBlock.FACING, dir)
								.modelForState()
								.modelFile(model)
								.rotationX(x)
								.rotationY(y)
								.addModel();
					}
				})
				.loot((lt, block) -> {
					if (dropItem != null) {
						RegistryLookup<Enchantment> ench = lt.getRegistries()
							.lookupOrThrow(Registries.ENCHANTMENT);
						LootTable.Builder table = LootTable.lootTable();
						if (bonusItem != null) {
							// 成熟簇：大块固定 1 个（不随时运增加）+ 小块作为时运额外（概率掉落）
							table.withPool(LootPool.lootPool()
								.setRolls(ConstantValue.exactly(1))
								.add(lt.applyExplosionDecay(block,
									LootItem.lootTableItem(dropItem.get())
										.apply(SetItemCountFunction.setCount(ConstantValue.exactly(dropCount))))));
							table.withPool(LootPool.lootPool()
								.setRolls(ConstantValue.exactly(1))
								.add(lt.applyExplosionDecay(block,
									LootItem.lootTableItem(bonusItem.get())
										.apply(ApplyBonusCount.addOreBonusCount(ench.getOrThrow(Enchantments.FORTUNE))))));
						} else {
							// 中/大芽：dropCount 个碎片 + 时运概率额外（同一物品，仿钻石矿 ore_drops）
							table.withPool(LootPool.lootPool()
								.setRolls(ConstantValue.exactly(1))
								.add(lt.applyExplosionDecay(block,
									LootItem.lootTableItem(dropItem.get())
										.apply(SetItemCountFunction.setCount(ConstantValue.exactly(dropCount)))
										.apply(ApplyBonusCount.addOreBonusCount(ench.getOrThrow(Enchantments.FORTUNE))))));
						}
						lt.add(block, table);
					} else {
						lt.dropOther(block, Items.AIR); // 小芽：无掉落
					}
				});
		// 只有簇注册物品（仿原版：芽无物品，创造栏/JEI 不可见）
		if (nextStage == null) {
			builder.item()
					// 仿原版：物品形态用 item/generated（一面显示），复用方块贴图
					.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder(ctx.getName()))
							.parent(new UncheckedModelFile("minecraft:item/generated"))
							.texture("layer0", prov.modLoc("block/" + ctx.getName())))
					.build();
		}
		return builder.register();
	}

	/**
	 * 注册水晶芽床（母岩）。
	 *
	 * <p>继承原版（AE2 催生器兼容）+ 生长进度方块实体（保底生芽间隔见 CrystalGrowthConfigs）；
	 * 芽床<b>不可搬迁</b>：挖掉后只掉落 1 个对应宝石粗矿（不返还芽床本身），
	 * 加入 c:budding_blocks tag。</p>
	 */
	private static BlockEntry<CrystalBuddingBlock> crystalBudding(String name, Supplier<Block> smallBud,
			MapColor color, int buddingGrowSeconds, Supplier<? extends Item> rawOre) {
		return CreateOreExpansion.REGISTRATE
			.block(name, p -> new CrystalBuddingBlock(p, smallBud, buddingGrowSeconds))
			.initialProperties(() -> Blocks.BUDDING_AMETHYST)
			.properties(p -> p.mapColor(color))
			.tag(BlockTags.MINEABLE_WITH_PICKAXE)
			.tag(Tags.Blocks.BUDDING_BLOCKS)
			.loot((lt, block) -> lt.dropOther(block, rawOre.get()))
			.item()
			.build()
			.register();
	}

	public static void register() {
	}

	private AllBlocks() {
	}
}
