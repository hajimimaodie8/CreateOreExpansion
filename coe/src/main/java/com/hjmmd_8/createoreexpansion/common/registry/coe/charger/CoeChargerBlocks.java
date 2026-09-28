package com.hjmmd_8.createoreexpansion.common.registry.coe.charger;

import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRegistrate;
import com.hjmmd_8.createoreexpansion.content.charger.block.ChargerBlockSlots;
import com.hjmmd_8.createoreexpansion.content.charger.block.ChargerMovementBehaviour;
import com.hjmmd_8.createoreexpansion.content.charger.block.JadeStressChargerBlock;
import com.hjmmd_8.createoreexpansion.content.charger.block.SapphireStressChargerBlock;
import com.hjmmd_8.createoreexpansion.content.charger.block.StellarstoneStressChargerBlock;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile.ExistingModelFile;
import net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile;
import net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder;

/**
 * <b>三台应力充能器的方块登记（第一层侧）</b>（W6-c）。
 *
 * <h2>一、它为什么存在</h2>
 * <p>这三条登记原先住在 {@code common/registry/cews/CewsBlocks}（第二层的类），但充能器本身
 * <b>属于第一层</b>：波引擎（{@code content/charger/**}）里 {@code ChargingRecipe} /
 * {@code ChargingJEI} / JEI 动画全都直接读它们，单装 {@code coe.jar} 时这三台机器必须存在。
 * 一个 Java 包不能同时属于两个 mod 文件（JPMS {@code ResolutionException}），
 * 所以 {@code CewsBlocks} 被按层拆成两半：这一半随 {@code :coe} 走，
 * 机器那 11 条（能量场控制器 / 星辉波变器 / 调级器 ×3 / 波速调节器 ×3 / 差波器 ×3）留在
 * {@code CewsBlocks}。</p>
 *
 * <p><b>纯搬运</b>：三条登记的链式调用、注册 id、datagen 变体生成、物品模型父级与
 * {@code onRegister} 副作用（应力影响值 + Create 动态结构适配）与拆分前<b>逐字相同</b>；
 * 唯一改动是 Registrate 实例从 {@code CewsRegistrate.REGISTRATE} 换成
 * {@link CoeRegistrate#REGISTRATE}（两个实例的命名空间与实例配置本就相同，
 * 注册 id 与默认创造页随 {@code ChargerKineticTooltip#withChargers} / 创造页清单另行保持）。</p>
 *
 * <h2>二、静态块为什么在类尾</h2>
 * <p>W6-a 建的 {@link ChargerBlockSlots} 是"第一层自己的槽位表"：三台充能器的
 * {@code IBE#getBlockEntityType()} 与 JEI 动画/催化剂只认它。注入方就是登记这三条的类，
 * 方向恒为「登记方 → 第一层表」。用 {@code Supplier} 而不是直接 {@code .get()}：
 * 类初始化期 {@code DeferredHolder} 尚未绑定，取值必须延迟到运行期真正被读的那一刻。</p>
 */
public final class CoeChargerBlocks {

	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<JadeStressChargerBlock> JADE_STRESS_CHARGER = CoeRegistrate.REGISTRATE
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
	public static final BlockEntry<SapphireStressChargerBlock> SAPPHIRE_STRESS_CHARGER = CoeRegistrate.REGISTRATE
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
		CoeRegistrate.REGISTRATE
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

	/**
	 * 把 3 台充能器的<b>方块</b>槽位与默认方块状态注入第一层表（见类注释"二"）。
	 *
	 * <p><b>W6-c 起方向变成同层</b>：W6-a 那版注释写的是「L2 → L1」，现在注入方与读取方都在
	 * {@code :coe}，所以这个静态块不再跨层；它保留的唯一理由是"表是读取方唯一认的东西"这条
	 * 结构（{@code ChargingRecipe#addRequiredMachines} / {@code ChargingJEI} / JEI 动画）。</p>
	 */
	static {
		ChargerBlockSlots.installJadeBlock(() -> JADE_STRESS_CHARGER.get());
		ChargerBlockSlots.installSapphireBlock(() -> SAPPHIRE_STRESS_CHARGER.get());
		ChargerBlockSlots.installStellarstoneBlock(() -> STELLARSTONE_STRESS_CHARGER.get());
		ChargerBlockSlots.installJadeDefaultState(() -> JADE_STRESS_CHARGER.getDefaultState());
		ChargerBlockSlots.installSapphireDefaultState(() -> SAPPHIRE_STRESS_CHARGER.getDefaultState());
	}

	/** 触发本层注册类的类初始化：Registrate 的注册动作就是字段初始化，因此方法体为空。 */
	public static void register() {
	}

	private CoeChargerBlocks() {
	}
}
