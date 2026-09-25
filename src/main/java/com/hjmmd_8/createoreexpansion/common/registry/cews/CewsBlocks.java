package com.hjmmd_8.createoreexpansion.common.registry.cews;

import com.hjmmd_8.createoreexpansion.content.charger.block.ChargerMovementBehaviour;
import com.hjmmd_8.createoreexpansion.content.charger.block.JadeStressChargerBlock;
import com.hjmmd_8.createoreexpansion.content.charger.block.SapphireStressChargerBlock;
import com.hjmmd_8.createoreexpansion.content.charger.block.StellarstoneStressChargerBlock;
import com.hjmmd_8.createoreexpansion.content.machine.energyfieldcontroller.EnergyFieldControllerBlock;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveTransmuterBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.DisperserMovingInteraction;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveRegulatorBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.SapphireSpeedRegulatorBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.SapphireWaveRegulatorBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.StellarstoneSpeedRegulatorBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.StellarstoneWaveRegulatorBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.OctaEnergyWaveDifferencerBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.SixFaceDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.WaveSpeedRegulatorBlock;
import com.hjmmd_8.createoreexpansion.common.*;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
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
 * <b>CEWS（能量波阵学）</b>方块注册：三种应力充能器、能量场控制器、星辉波变器、
 * 能量调级器 ×3、波速调节器 ×3、波差器家族 ×3（四面 / 六面 / 八面）与机壳 ×3
 * （翡翠 / 蓝宝石 / 星辉石；后者是 P1 复核发现的<b>第三个</b>机壳方块，方案文档 §3.2 当时只记了两种）。<br>
 * 归属清单权威出处：{@code markdown_output/CEWS 能量波阵学模块（拆分方案与思索）.md} §3.2。
 * 本次拆分是<b>纯搬运</b>，注册 id 与链式调用一字未改，P2a 已删除 {@code AllBlocks} 外观类，引用直接指向本层。
 */
public final class CewsBlocks {

	public static final BlockEntry<CasingBlock> JADE_CASING = CewsRegistrate.REGISTRATE
			.block("jade_casing", CasingBlock::new)
			.properties(p -> p.mapColor(MapColor.TERRACOTTA_GREEN))
			.transform(BuilderTransformers.casing(() -> AllSpriteShifts.JADE_CASING))
			.register();

	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<JadeStressChargerBlock> JADE_STRESS_CHARGER = CewsRegistrate.REGISTRATE
		.block("jade_stress_charger", JadeStressChargerBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_GREEN))
		.properties(p -> p.noOcclusion())
		.properties(p -> p.isRedstoneConductor((state, level, pos) -> false))
		.addLayer(() -> () -> RenderType.cutoutMipped())
		.transform(TagGen.axeOrPickaxe())
		.blockstate((ctx, prov) -> {
			// MODE 属性现为 0~5（共享基类，为蓝宝石 4/5 级预留）；翡翠只用 0~3，
			// 4/5 补位复用 _3 模型（翡翠实际不会进入，纯满足 blockstate 全覆盖）
			ExistingModelFile[] models = new ExistingModelFile[4];
			for (int i = 0; i < 4; i++)
				models[i] = prov.models()
					.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
						"block/jade_stress_charger/jade_stress_charger_" + i));
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
				for (int mode = 0; mode <= 5; mode++) {
					vb.partialState()
						.with(DirectionalKineticBlock.FACING, dir)
						.with(JadeStressChargerBlock.MODE, mode)
						.modelForState()
						.modelFile(models[Math.min(mode, 3)])
						.rotationX(xRot)
						.rotationY(yRot)
						.addModel();
				}
			}
		})
		.onRegister(block -> {
			BlockStressValues.IMPACTS.register(block, () -> 4.0);
			// Create 动态结构（动力轴承/矿车装配站）适配：充能器随结构自行工作（像钻头/动力锯）
			MovementBehaviour.REGISTRY.register(block, new ChargerMovementBehaviour());
		})
		.item()
		.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("jade_stress_charger"))
			.parent(new UncheckedModelFile("createoreexpansion:block/jade_stress_charger/jade_stress_charger_item")))
		.build()
		.register();

	/** 蓝宝石应力充能器：蓝宝石科技线专属充能器（可蓄至 4/5 级、双模式）。 */
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<SapphireStressChargerBlock> SAPPHIRE_STRESS_CHARGER = CewsRegistrate.REGISTRATE
		.block("sapphire_stress_charger", SapphireStressChargerBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_BLUE))
		.properties(p -> p.noOcclusion())
		.properties(p -> p.isRedstoneConductor((state, level, pos) -> false))
		.addLayer(() -> () -> RenderType.cutoutMipped())
		.transform(TagGen.axeOrPickaxe())
		.blockstate((ctx, prov) -> {
			// 0~5 挡模型齐全：_4/_5 当前复制 _3 外观占位（后续替换文件即可独立成 4/5 级外观）
			ExistingModelFile[] models = new ExistingModelFile[6];
			for (int i = 0; i < 6; i++)
				models[i] = prov.models()
					.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
						"block/sapphire_stress_charger/sapphire_stress_charger_" + i));
			VariantBlockStateBuilder vb = prov.getVariantBuilder(ctx.get());
			for (Direction dir : Direction.values()) {
				// 与翡翠充能器同款旋转映射：UP 原样、DOWN 180、水平绕 X 躺下再绕 Y 转向
				int xRot = dir == Direction.UP ? 0
					: dir == Direction.DOWN ? 180
						: dir.getAxis()
							.isHorizontal() ? 270 : 0;
				int yRot = dir.getAxis()
					.isHorizontal() ? (int) dir.toYRot() : 0;
				// MODE 0~5 全覆盖：每个挡位指向各自模型（_4/_5 现为 _3 外观占位，文件独立可后续替换）
				for (int mode = 0; mode <= 5; mode++) {
					vb.partialState()
						.with(DirectionalKineticBlock.FACING, dir)
						.with(SapphireStressChargerBlock.MODE, mode)
						.modelForState()
						.modelFile(models[mode])
						.rotationX(xRot)
						.rotationY(yRot)
						.addModel();
				}
			}
		})
		.onRegister(block -> {
			BlockStressValues.IMPACTS.register(block, () -> 4.0);
			// Create 动态结构适配：蓝宝石充能器随结构自行工作（同翡翠）
			MovementBehaviour.REGISTRY.register(block, new ChargerMovementBehaviour());
		})
		.item()
		.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("sapphire_stress_charger"))
			.parent(new UncheckedModelFile("createoreexpansion:block/sapphire_stress_charger/sapphire_stress_charger_item")))
		.build()
		.register();

	/** 星辉石应力充能器：蓝宝石充能器的复制变体——发射等级手动固定（两侧槽：模式 + 手动等级）。 */
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<StellarstoneStressChargerBlock> STELLARSTONE_STRESS_CHARGER =
		CewsRegistrate.REGISTRATE
			.block("stellarstone_stress_charger", StellarstoneStressChargerBlock::new)
			.initialProperties(SharedProperties::stone)
			.properties(p -> p.mapColor(MapColor.COLOR_PURPLE))
			.properties(p -> p.noOcclusion())
			.properties(p -> p.isRedstoneConductor((state, level, pos) -> false))
			.addLayer(() -> () -> RenderType.cutoutMipped())
			.transform(TagGen.axeOrPickaxe())
			.blockstate((ctx, prov) -> {
				// MODE 0~5 全覆盖：与蓝宝石同构（_4/_5 现为独立占位文件，可后续替换独立外观）
				ExistingModelFile[] models = new ExistingModelFile[6];
				for (int i = 0; i < 6; i++)
					models[i] = prov.models()
						.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
							"block/stellarstone_stress_charger/stellarstone_stress_charger_" + i));
				VariantBlockStateBuilder vb = prov.getVariantBuilder(ctx.get());
				for (Direction dir : Direction.values()) {
					// 与翡翠/蓝宝石充能器同款旋转映射：UP 原样、DOWN 180、水平绕 X 躺下再绕 Y 转向
					int xRot = dir == Direction.UP ? 0
						: dir == Direction.DOWN ? 180
							: dir.getAxis()
								.isHorizontal() ? 270 : 0;
					int yRot = dir.getAxis()
						.isHorizontal() ? (int) dir.toYRot() : 0;
					for (int mode = 0; mode <= 5; mode++) {
						vb.partialState()
							.with(DirectionalKineticBlock.FACING, dir)
							.with(StellarstoneStressChargerBlock.MODE, mode)
							.modelForState()
							.modelFile(models[mode])
							.rotationX(xRot)
							.rotationY(yRot)
							.addModel();
					}
				}
			})
			.onRegister(block -> {
				BlockStressValues.IMPACTS.register(block, () -> 4.0);
				// Create 动态结构适配：星辉石充能器随结构自行工作（同翡翠/蓝宝石）
				MovementBehaviour.REGISTRY.register(block, new ChargerMovementBehaviour());
			})
			.item()
			.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("stellarstone_stress_charger"))
				.parent(new UncheckedModelFile(
					"createoreexpansion:block/stellarstone_stress_charger/stellarstone_stress_charger_item")))
			.build()
			.register();

	/** 能量场控制器（Energy Field Controller）：六向应力机器，应力输入 → 场强档位（配对/极性/机壳扩展规则见 BE 注释）。 */
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<EnergyFieldControllerBlock> ENERGY_FIELD_CONTROLLER = CewsRegistrate.REGISTRATE
		.block("energy_field_controller", EnergyFieldControllerBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_BLUE))
		.properties(p -> p.noOcclusion())
		.properties(p -> p.isRedstoneConductor((state, level, pos) -> false))
		.addLayer(() -> () -> RenderType.cutoutMipped())
		.transform(TagGen.axeOrPickaxe())
		.blockstate((ctx, prov) -> {
			// 模型默认"口"（接收面板）在 +Y 顶面、轴在底面（与充能器同构）：
			// UP 原样、DOWN 180、水平先绕 X 躺下（+Y→+Z）再绕 Y 转向 facing
			// OPEN=false 用 close 模型、OPEN=true 用 open 变体（口面贴 energy_field_controller_receiver_open）
			ExistingModelFile closed = prov.models()
				.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
					"block/energy_field_controller/sapphire_field_controller"));
			ExistingModelFile open = prov.models()
				.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
					"block/energy_field_controller/sapphire_field_controller_open"));
			VariantBlockStateBuilder vb = prov.getVariantBuilder(ctx.get());
			for (Direction dir : Direction.values()) {
				int xRot = dir == Direction.UP ? 0
					: dir == Direction.DOWN ? 180
						: dir.getAxis()
							.isHorizontal() ? 270 : 0;
				int yRot = dir.getAxis()
					.isHorizontal() ? (int) dir.toYRot() : 0;
				for (boolean isOpen : new boolean[] { false, true }) {
					vb.partialState()
						.with(DirectionalKineticBlock.FACING, dir)
						.with(EnergyFieldControllerBlock.OPEN, isOpen)
						.modelForState()
						.modelFile(isOpen ? open : closed)
						.rotationX(xRot)
						.rotationY(yRot)
						.addModel();
				}
			}
		})
		.onRegister(block -> {
			BlockStressValues.IMPACTS.register(block, () -> 4.0);
		})
		.item()
		.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("energy_field_controller"))
			.parent(new UncheckedModelFile("createoreexpansion:block/energy_field_controller/sapphire_field_controller_item")))
		.build()
		.register();

	/** 星辉波变器：六向应力机器（单轴口，FACING 反面接入），扫描加工机并为穿过的能量波附加加工属性。
	 * <p>模型 = 差波器侧面几何（星辉石能量接收面 close/open 整面切换）+ 顶面灯盘
	 * （{@code stellarstone_disperser_lamp}）+ 场控底面底座（轴口面 {@code stellarstone_gearbox}）。
	 * 4 个水平侧面独立开关 → 16 个变体模型 × 6 个 FACING = 96 个 variant。</p> */
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<StellarWaveTransmuterBlock> STELLAR_WAVE_TRANSMUTER = CewsRegistrate.REGISTRATE
		.block("stellar_wave_transmuter", StellarWaveTransmuterBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_PURPLE))
		.properties(p -> p.noOcclusion())
		.properties(p -> p.isRedstoneConductor((state, level, pos) -> false))
		.addLayer(() -> () -> RenderType.cutoutMipped())
		.transform(TagGen.axeOrPickaxe())
		.blockstate((ctx, prov) -> {
			// 16 个变体模型（4 个水平侧面的 open 组合），六向 FACING 旋转：
			// 模型默认灯盘朝 +Y——竖直放置（UP）原样、DOWN 转 180，水平先绕 X 躺下（+Y→+Z）再绕 Y 转向 facing
			ExistingModelFile[] models = new ExistingModelFile[16];
			for (int i = 0; i < 16; i++)
				models[i] = prov.models()
					.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
						"block/energy_wave_machine/stellarstone_wave_transmuter_" + i));
			VariantBlockStateBuilder vb = prov.getVariantBuilder(ctx.get());
			for (Direction dir : Direction.values()) {
				int xRot = dir == Direction.UP ? 0
					: dir == Direction.DOWN ? 180
						: dir.getAxis()
							.isHorizontal() ? 270 : 0;
				int yRot = dir.getAxis()
					.isHorizontal() ? (int) dir.toYRot() : 0;
				for (int index = 0; index < 16; index++) {
					vb.partialState()
						.with(DirectionalKineticBlock.FACING, dir)
						.with(StellarWaveTransmuterBlock.NORTH, (index & 8) != 0)
						.with(StellarWaveTransmuterBlock.SOUTH, (index & 4) != 0)
						.with(StellarWaveTransmuterBlock.WEST, (index & 2) != 0)
						.with(StellarWaveTransmuterBlock.EAST, (index & 1) != 0)
						.modelForState()
						.modelFile(models[index])
						.rotationX(xRot)
						.rotationY(yRot)
						.addModel();
				}
			}
		})
		.onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> 4.0))
		.item()
		.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("stellar_wave_transmuter"))
			.parent(new UncheckedModelFile(
				"createoreexpansion:block/energy_wave_machine/stellarstone_wave_transmuter_item")))
		.build()
		.register();

	/** 蓝宝石能量调级器：蓝宝石科技线专属（64 RPM 起调制、最大可把波提升至 5 级 ω）。 */
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<SapphireWaveRegulatorBlock> SAPPHIRE_WAVE_REGULATOR = CewsRegistrate.REGISTRATE
		.block("sapphire_wave_regulator", SapphireWaveRegulatorBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_BLUE))
		.properties(p -> p.noOcclusion())
		.properties(p -> p.isRedstoneConductor((state, level, pos) -> false))
		.addLayer(() -> () -> RenderType.cutoutMipped())
		.transform(TagGen.axeOrPickaxe())
		.blockstate((ctx, prov) -> {
			// 与翡翠调级器同款：机座模型按顶/底面板 open/close 选 4 变体，六向 FACING 旋转
			ExistingModelFile[] models = new ExistingModelFile[4];
			for (int i = 0; i < 4; i++)
				models[i] = prov.models()
					.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
						"block/energy_wave_machine/sapphire_wave_regulator_" + i));
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
							.with(SapphireWaveRegulatorBlock.RECEIVER_TOP, top == 1)
							.with(SapphireWaveRegulatorBlock.RECEIVER_BOTTOM, bottom == 1)
							.modelForState()
							.modelFile(models[top * 2 + bottom])
							.rotationX(xRot)
							.rotationY(yRot)
							.addModel();
					}
				}
			}
		})
		// 蓝宝石能量调级器：应力消耗固定为 8x RPM（同翡翠调级器；不随波级变化）
		.onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> 8.0))
		.item()
		.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("sapphire_wave_regulator"))
			.parent(new UncheckedModelFile("createoreexpansion:block/energy_wave_machine/sapphire_wave_regulator_item")))
		.build()
		.register();

	/** 能量调级器：六向应力机器（齿轮轴沿 FACING），承接翡翠应力充能器能量波调级。
	 * 与蓝宝石调级器共享 {@code AbstractWaveGateBlock} 基类（机型差异：转速门槛/最大升等级）。 */
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<EnergyWaveRegulatorBlock> ENERGY_WAVE_REGULATOR = CewsRegistrate.REGISTRATE
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
						"block/energy_wave_machine/jade_energy_wave_regulator_" + i));
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
		// 能量调级器：应力消耗固定为 8x RPM（不随波级变化；速度调节器仍为 4x）
		.onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> 8.0))
		.item()
		.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("energy_wave_regulator"))
			.parent(new UncheckedModelFile("createoreexpansion:block/energy_wave_machine/jade_energy_wave_regulator_item")))
		.build()
		.register();

	/** 波速调节器：与能量调级器同构（模型/旋转/面板交互一致，仅侧面贴图暂为复制占位），
	 * 应力速度调制：按转速分档加速/减速（100~256 RPM 分 4 档 ±0.5/1/1.5/2 格/秒），反弹不改速。 */
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<WaveSpeedRegulatorBlock> WAVE_SPEED_REGULATOR = CewsRegistrate.REGISTRATE
		.block("wave_speed_regulator", WaveSpeedRegulatorBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_GREEN))
		.properties(p -> p.noOcclusion())
		.properties(p -> p.isRedstoneConductor((state, level, pos) -> false))
		.addLayer(() -> () -> RenderType.cutoutMipped())
		.transform(TagGen.axeOrPickaxe())
		.blockstate((ctx, prov) -> {
			// 与调级器同款：机座模型按顶/底面板 open/close 选 4 变体，六向 FACING 旋转
			ExistingModelFile[] models = new ExistingModelFile[4];
			for (int i = 0; i < 4; i++)
				models[i] = prov.models()
					.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
						"block/energy_wave_machine/jade_wave_speed_regulator_" + i));
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
							.with(WaveSpeedRegulatorBlock.RECEIVER_TOP, top == 1)
							.with(WaveSpeedRegulatorBlock.RECEIVER_BOTTOM, bottom == 1)
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
		.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("wave_speed_regulator"))
			.parent(new UncheckedModelFile("createoreexpansion:block/energy_wave_machine/jade_wave_speed_regulator_item")))
		.build()
		.register();

	/** 蓝宝石波速调节器：蓝宝石科技线专属（64 RPM 起调制、64~256 RPM 分 6 档 ±0.5~3 格/秒）。 */
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<SapphireSpeedRegulatorBlock> SAPPHIRE_SPEED_REGULATOR = CewsRegistrate.REGISTRATE
		.block("sapphire_speed_regulator", SapphireSpeedRegulatorBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_BLUE))
		.properties(p -> p.noOcclusion())
		.properties(p -> p.isRedstoneConductor((state, level, pos) -> false))
		.addLayer(() -> () -> RenderType.cutoutMipped())
		.transform(TagGen.axeOrPickaxe())
		.blockstate((ctx, prov) -> {
			// 与翡翠波速调节器同款：机座模型按顶/底面板 open/close 选 4 变体，六向 FACING 旋转
			ExistingModelFile[] models = new ExistingModelFile[4];
			for (int i = 0; i < 4; i++)
				models[i] = prov.models()
					.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
						"block/energy_wave_machine/sapphire_speed_regulator_" + i));
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
							.with(SapphireSpeedRegulatorBlock.RECEIVER_TOP, top == 1)
							.with(SapphireSpeedRegulatorBlock.RECEIVER_BOTTOM, bottom == 1)
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
		.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("sapphire_speed_regulator"))
			.parent(new UncheckedModelFile("createoreexpansion:block/energy_wave_machine/sapphire_speed_regulator_item")))
		.build()
		.register();

	/** 星辉石能量调级器：星辉石科技线专属（32 RPM 起调制、单次提升级数随转速 +1/+2、最大升至 5 级 ω）。 */
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<StellarstoneWaveRegulatorBlock> STELLARSTONE_WAVE_REGULATOR =
		CewsRegistrate.REGISTRATE
			.block("stellarstone_wave_regulator", StellarstoneWaveRegulatorBlock::new)
			.initialProperties(SharedProperties::stone)
			.properties(p -> p.mapColor(MapColor.COLOR_PURPLE))
			.properties(p -> p.noOcclusion())
			.properties(p -> p.isRedstoneConductor((state, level, pos) -> false))
			.addLayer(() -> () -> RenderType.cutoutMipped())
			.transform(TagGen.axeOrPickaxe())
			.blockstate((ctx, prov) -> {
				// 与蓝宝石调级器同款：机座模型按顶/底面板 open/close 选 4 变体，六向 FACING 旋转
				ExistingModelFile[] models = new ExistingModelFile[4];
				for (int i = 0; i < 4; i++)
					models[i] = prov.models()
						.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
							"block/energy_wave_machine/stellarstone_wave_regulator_" + i));
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
								.with(StellarstoneWaveRegulatorBlock.RECEIVER_TOP, top == 1)
								.with(StellarstoneWaveRegulatorBlock.RECEIVER_BOTTOM, bottom == 1)
								.modelForState()
								.modelFile(models[top * 2 + bottom])
								.rotationX(xRot)
								.rotationY(yRot)
								.addModel();
						}
					}
				}
			})
			// 星辉石能量调级器：应力消耗固定为 8x RPM（同蓝宝石/翡翠调级器；不随波级变化）
			.onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> 8.0))
			.item()
			.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("stellarstone_wave_regulator"))
				.parent(new UncheckedModelFile("createoreexpansion:block/energy_wave_machine/stellarstone_wave_regulator_item")))
			.build()
			.register();

	/** 星辉石波速调节器：星辉石科技线专属（32 RPM 起调制、32~256 RPM 分 8 档 ±0.5~4 格/秒）。 */
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<StellarstoneSpeedRegulatorBlock> STELLARSTONE_SPEED_REGULATOR =
		CewsRegistrate.REGISTRATE
			.block("stellarstone_speed_regulator", StellarstoneSpeedRegulatorBlock::new)
			.initialProperties(SharedProperties::stone)
			.properties(p -> p.mapColor(MapColor.COLOR_PURPLE))
			.properties(p -> p.noOcclusion())
			.properties(p -> p.isRedstoneConductor((state, level, pos) -> false))
			.addLayer(() -> () -> RenderType.cutoutMipped())
			.transform(TagGen.axeOrPickaxe())
			.blockstate((ctx, prov) -> {
				// 与蓝宝石波速调节器同款：机座模型按顶/底面板 open/close 选 4 变体，六向 FACING 旋转
				ExistingModelFile[] models = new ExistingModelFile[4];
				for (int i = 0; i < 4; i++)
					models[i] = prov.models()
						.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
							"block/energy_wave_machine/stellarstone_speed_regulator_" + i));
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
								.with(StellarstoneSpeedRegulatorBlock.RECEIVER_TOP, top == 1)
								.with(StellarstoneSpeedRegulatorBlock.RECEIVER_BOTTOM, bottom == 1)
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
			.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("stellarstone_speed_regulator"))
				.parent(new UncheckedModelFile("createoreexpansion:block/energy_wave_machine/stellarstone_speed_regulator_item")))
			.build()
			.register();

	/** 能量波差器：无应力被动机器，模型上下翡翠机壳、四面能量接收面关闭材质；
	 * 六向 FACING 旋转（三种朝向与调级器一致），4 侧面开口可独立开关，无需方块实体/渲染器。 */
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<EnergyWaveDisperserBlock> ENERGY_WAVE_DISPERSER = CewsRegistrate.REGISTRATE
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
						"block/energy_wave_machine/jade_energy_wave_disperser_" + i));
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
		.onRegister(MovingInteractionBehaviour.interactionBehaviour(DisperserMovingInteraction.instance()))
		.item()
		.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("energy_wave_disperser"))
			.parent(new UncheckedModelFile("createoreexpansion:block/energy_wave_machine/jade_energy_wave_disperser_item")))
		.build()
		.register();

	/** 六面能量波差器：无朝向固定机器，6 面全部为能量接收面板（wave_receiver close/open）独立开关；
	 * 5/6 面开口时波分裂为降二级子波（判定见 SixFaceDispersal）。 */
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<SixFaceDisperserBlock> SIX_FACE_DISPERSER = CewsRegistrate.REGISTRATE
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
						"block/energy_wave_machine/jade_six_face_disperser_" + i));
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
		.onRegister(MovingInteractionBehaviour.interactionBehaviour(DisperserMovingInteraction.instance()))
		.item()
		.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("six_face_disperser"))
			.parent(new UncheckedModelFile("createoreexpansion:block/energy_wave_machine/jade_six_face_disperser")))
		.build()
		.register();

	/** 八面能量波差器：无朝向固定机器，8 个接收方向（4 正交面 + 4 斜向）独立开关；
	 * 开口数规则：1=反弹、2=通道、3~4=分裂降 1、5~6=降 2、7~8=降 3。
	 * 模型/贴图 = octa_energy_wave_differencer（sapphire 系）。 */
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<OctaEnergyWaveDifferencerBlock> OCTA_ENERGY_WAVE_DIFFERENCER = CewsRegistrate.REGISTRATE
		.block("octa_energy_wave_differencer", OctaEnergyWaveDifferencerBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_BLUE))
		.properties(p -> p.noOcclusion())
		.properties(p -> p.isRedstoneConductor((state, level, pos) -> false))
		.addLayer(() -> () -> RenderType.cutoutMipped())
		.transform(TagGen.axeOrPickaxe())
		.blockstate((ctx, prov) -> {
			// 8 面 open 组合 → 256 变体模型（薄 parent 覆盖：base 定义 8 个方向纹理键，
			// 变体只覆盖 open 方向为 open 贴图）。变体索引位序见 gen_octa_variants.js：
			// bit0=n, bit1=e, bit2=s, bit3=w, bit4=ne, bit5=nw, bit6=se, bit7=sw。
			// 注意：紫黑根因曾是灯 PartialModel 未注册（已修），变体模型本身有效。
			ExistingModelFile[] models = new ExistingModelFile[256];
			for (int i = 0; i < 256; i++)
				models[i] = prov.models()
					.getExistingFile(ResourceLocation.fromNamespaceAndPath("createoreexpansion",
						"block/octa_energy_wave_differencer/octa_energy_wave_differencer_" + i));
			VariantBlockStateBuilder vb = prov.getVariantBuilder(ctx.get());
			for (Direction.Axis axis : new Direction.Axis[] { Direction.Axis.Y, Direction.Axis.X, Direction.Axis.Z }) {
				// 姿态旋转（待目测校准）：axis=Y 原样；axis=X 应让顶/底朝东西(点东西向墙)，
				// axis=Z 应让顶/底朝南北(点南北向墙)。先给组合 x90+y90 / x90，若方向反只调 y 的 90/270。
				int xRot = axis == Direction.Axis.Y ? 0 : 90;
				int yRot = axis == Direction.Axis.X ? 90 : 0;
				for (int n = 0; n < 2; n++)
					for (int e = 0; e < 2; e++)
						for (int s = 0; s < 2; s++)
							for (int w = 0; w < 2; w++)
								for (int ne = 0; ne < 2; ne++)
									for (int nw = 0; nw < 2; nw++)
										for (int se = 0; se < 2; se++)
											for (int sw = 0; sw < 2; sw++)
												vb.partialState()
													.with(OctaEnergyWaveDifferencerBlock.AXIS, axis)
													.with(OctaEnergyWaveDifferencerBlock.NORTH, n == 1)
													.with(OctaEnergyWaveDifferencerBlock.EAST, e == 1)
													.with(OctaEnergyWaveDifferencerBlock.SOUTH, s == 1)
													.with(OctaEnergyWaveDifferencerBlock.WEST, w == 1)
													.with(OctaEnergyWaveDifferencerBlock.NORTH_EAST, ne == 1)
													.with(OctaEnergyWaveDifferencerBlock.NORTH_WEST, nw == 1)
													.with(OctaEnergyWaveDifferencerBlock.SOUTH_EAST, se == 1)
													.with(OctaEnergyWaveDifferencerBlock.SOUTH_WEST, sw == 1)
													.modelForState()
													.modelFile(models[n * 1 + e * 2 + s * 4 + w * 8 + ne * 16 + nw * 32 + se * 64 + sw * 128])
													.rotationX(xRot)
													.rotationY(yRot)
													.addModel();
			}
		})
		// 航空学/contraption 兼容：结构装配后可右键开合（与四面/六面差波器同款交互，
		// 本机支持命中细分：中心 6px/两侧 5px/顶底 8 扇区，见 DisperserMovingInteraction）
		.onRegister(MovingInteractionBehaviour.interactionBehaviour(DisperserMovingInteraction.instance()))
		.item()
		.model((ctx, prov) -> ((ItemModelBuilder) prov.getBuilder("octa_energy_wave_differencer"))
			.parent(new UncheckedModelFile("createoreexpansion:block/octa_energy_wave_differencer/octa_energy_wave_differencer")))
		.build()
		.register();

	public static final BlockEntry<CasingBlock> SAPPHIRE_CASING = CewsRegistrate.REGISTRATE
		.block("sapphire_casing", CasingBlock::new)
			.properties(p -> p.mapColor(MapColor.TERRACOTTA_BLUE))
			.transform(BuilderTransformers.casing(() -> AllSpriteShifts.SAPPHIRE_CASING))
			.register();

	public static final BlockEntry<CasingBlock> STELLARSTONE_CASING = CewsRegistrate.REGISTRATE
		.block("stellarstone_casing", CasingBlock::new)
			.properties(p -> p.mapColor(MapColor.TERRACOTTA_PINK))
			.transform(BuilderTransformers.casing(() -> AllSpriteShifts.STELLARSTONE_CASING))
			// 星辉石系列特性（掉落物不落虚空 / 岩浆与嬗化液中不销毁并发光…）：登记进系列方块标签。
			// 本方块的物品由上面 casing 变换器注册，拿不到 ItemBuilder 去挂物品标签，故走方块标签这一支
			// （运行时判定三支都认：物品标签 ∪ 方块标签 ∪ 注册名约定，见 SeriesTraits#isStellarstone）。
			.transform(SeriesTraits.addStellarstoneTraits())
			.register();

/** 触发本层注册类的类初始化：Registrate 的注册动作就是字段初始化，因此方法体为空。 */
	public static void register() {
	}

	private CewsBlocks() {
	}
}
