package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.*;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.field.StressInjectorBlock;
import com.hjmmd_8.createoreexpansion.content.grinding.block.GrinderCoverPlaceholderBlock;
import com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlock;
import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlock;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.data.BuilderTransformers;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile.ExistingModelFile;
import net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile;
import net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder;

/**
 * <b>COE（矿物拓展）</b>的<b>机壳与机器类方块</b>注册：三种机壳（翡翠 / 蓝宝石 / 星辉石）、
 * 动力角磨床、应力注入器、强化避雷针。
 *
 * <p>与 {@link CoeBlocks} 的分工是<b>职责域</b>（作者 2026-10-04 指定）：{@code CoeBlocks} 只留
 * <b>简单方块</b>（矿物 / 材料块 / 晶体与芽床 —— 没有 BE、也没有机器行为的方块），
 * 本类收<b>机壳</b>与<b>机器类方块</b>。判据与逐件归属见
 * {@code markdown_output/COE 层功能清点与变更史.md} 的批 2 记录。</p>
 *
 * <p><b>为什么机壳算"机器"而不是"简单方块"</b>：三种机壳是 {@code CasingBlock}（无 BE、无机器行为），
 * 单看"有没有 BE"它们与简单方块同类；但作者的拆法把 {@code *_CASING} 明确点拨到本类，
 * 且它们与本类其余三件在职责上同族（都是"机械构件"：机壳可包轴/齿轮，属机械玩法）。
 * 因此本类的判据是<b>职责域</b>，而"有没有 BE / 有没有机器行为"用来判<b>晶体与芽床</b>那一支
 * （见 {@link CoeBlocks} 的类注释）。</p>
 *
 * <p><b>注册顺序契约</b>：本类的字段顺序 = 拆分前它们在 {@code CoeBlocks} 里的原顺序
 * （机壳3 → 角磨床 → 应力注入器 → 强化避雷针）。类初始化由 {@code CoeBlocks} 的一个
 * {@code static {}} 块在<b>原机壳槽位</b>触发，因此与 {@code CoeBlocks} 合并后的登记顺序
 * 与拆分前逐条一致；{@code CreateOreExpansion} 里紧随 {@code CoeBlocks.register()} 的
 * 显式 {@code CoeMachines.register()} 是本仓「注册触发顺序显式写死」的口径落点（运行时为幂等空操作）。</p>
 *
 * <p><b>搬运纪律</b>：本类全部内容是从 {@code CoeBlocks} <b>逐字搬来</b>的（批 2 之前的最近一次
 * 结构性改动见下面的 W9 注记），注册 id / 贴图路径 / 数值 / 链式调用一字未改。</p>
 */
public final class CoeMachines {

	// ========== 机壳（Casing）：翡翠 / 蓝宝石 / 星辉石 ==========
	// W9（用户裁定 2026-09-28）：「三个机壳归属于 COE，而不归属于 CEWS」。
	// 这三条原先住在 common/registry/cews/CewsBlocks（W6-c 的 U3 按"对应机壳"把它们留在了 L2），
	// 现整条搬回 COE 层；本批（批 2）再从 CoeBlocks 搬进本类，位置 = **拆分前 JADE_CASING
	// 在原 AllBlocks 里的槽位**（紧接 SANCTSTONE_BLOCK，见 9678b5ce 的 AllBlocks.java:344），
	// 三种机壳按 翡翠 → 蓝宝石 → 星辉石 成组，与拆分前 create:casing 标签里的出现顺序一致。
	// 为什么落在 COE 层的 registrate 里而不是 CEWS：机壳的 CTM sprite shift（AllSpriteShifts#*_CASING）
	// 住 core，第一层不能 import 第二层，机壳登记在本层时方向天然合法；且一个 Java 包不能同时属于
	// 两个 mod 文件（JPMS），新建包只会多一个风险点。
	// ⚠ 唯一可观察的副作用：这三条由 CoeRegistrate 注册 ⇒ Registrate 会把它们按注册顺序收进
	// 本模组唯一的创造页（CoeCreativeTabs.BASE_TAB），再由 CoeCreativeSections 的判据归到「机械」分区。

	/** 翡翠机壳（{@code createoreexpansion:jade_casing}，Create 机壳标签成员，可包轴/齿轮）。 */
	public static final BlockEntry<CasingBlock> JADE_CASING = CoeRegistrate.REGISTRATE
		.block("jade_casing", CasingBlock::new)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_GREEN))
		.transform(BuilderTransformers.casing(() -> AllSpriteShifts.JADE_CASING))
		.register();

	/** 蓝宝石机壳（{@code createoreexpansion:sapphire_casing}）。 */
	public static final BlockEntry<CasingBlock> SAPPHIRE_CASING = CoeRegistrate.REGISTRATE
		.block("sapphire_casing", CasingBlock::new)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_BLUE))
		.transform(BuilderTransformers.casing(() -> AllSpriteShifts.SAPPHIRE_CASING))
		.register();

	/** 星辉石机壳（{@code createoreexpansion:stellarstone_casing}）：带星辉石系列特性。 */
	public static final BlockEntry<CasingBlock> STELLARSTONE_CASING = CoeRegistrate.REGISTRATE
		.block("stellarstone_casing", CasingBlock::new)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_PINK))
		.transform(BuilderTransformers.casing(() -> AllSpriteShifts.STELLARSTONE_CASING))
		// 星辉石系列特性（掉落物不落虚空 / 岩浆与嬗化液中不销毁并发光…）：登记进系列方块标签。
		// 本方块的物品由上面 casing 变换器注册，拿不到 ItemBuilder 去挂物品标签，故走方块标签这一支
		// （运行时判定三支都认：物品标签 ∪ 方块标签 ∪ 注册名约定，见 SeriesTraits#isStellarstone）。
		.transform(SeriesTraits.addStellarstoneTraits())
		.register();

	// ========== 动力角磨床（power_angle_grinder） ==========
	@SuppressWarnings("removal") // addLayer 过时但无替代，用于 cutoutMipped 渲染层
	public static final BlockEntry<PowerAngleGrinderBlock> POWER_ANGLE_GRINDER = CoeRegistrate.REGISTRATE
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

	// ===== 角磨床盖侧占位方块（grinder_cover_placeholder）—— ⚠ **不可获取的隐藏方块** =====
	// 作者 2026-10-07 原话（COE 批 22 的判据）：「咱就不能在它开盖的时候，把盖身的那个部分判定成
	// 一个方块吗？……这样还用考虑什么活塞推动流体流动的问题？……你把这个改成"活塞不能推动这个方块"」。
	// 它就是"开盖时盖那一格真的有一个方块"这件事的载体：放置 / 活塞 / 流体三件事一次解决，
	// 因此批 21 那条"放置事件取消"监听（GrinderCoverPlacementGuard）当场变成死代码并已删除。
	//
	// 与上面所有方块的四条关键区别（**缺任何一条这个修法就退化**，逐条都有负向断言守着）：
	//   ① **没有 .item()** ⇒ 没有 BlockItem：不进背包、不进创造页任何分区、不进 JEI、搜不到
	//      （Registrate 只把**物品**填进创造页：AbstractRegistrate#item(...) 里才读
	//      defaultCreativeModeTab）；且**不调 .lang()** ⇒ 零新增语言键（八份 lang 拷贝一字不动）；
	//   ② **.noLootTable()** ⇒ Block#getLootTable() = BuiltInLootTables.EMPTY ⇒ Registrate 的
	//      战利品回调被跳过、BlockLootSubProvider 也跳过它 ⇒ **一个战利品表文件都不生成**，
	//      被挖/被炸不掉任何东西；
	//   ③ 方块状态指向**原版** minecraft:block/air（模型文件内容就是空的 {}，零贴图零引用）
	//      ⇒ **不新增贴图、不新增模型文件**（美术红线"一张图都不要改、也不要自己画"零触碰）；
	//      方块本身由 GrinderCoverPlaceholderBlock#getRenderShape = RenderShape.INVISIBLE
	//      完全跳过渲染，getShape 返回空形状 ⇒ 连选中描边都没有、射线也打不到它；
	//   ④ **.forceSolidOn() 是"挡住流体"的唯一关键**（见 GrinderCoverPlaceholderBlock 的类注释：
	//      FlowingFluid#canHoldFluid 只看 !state.blocksMotion()，而 blocksMotion() ← isSolid()
	//      ← calculateSolid() 在 collisionShape 为空时返回 false——noCollission + 空形状正好
	//      落进那一支）。原版悬挂告示牌就是 forceSolidOn().noCollission() 这一对。
	//      **不调 replaceable()** ⇒ 默认 false ⇒ 那一格不接受任何放置（这是"放置"那一半的全部）。
	//      **pushReaction(BLOCK)** ⇒ 活塞推不动也拉不动。**instabreak 刻意不给 -1**，
	//      否则 PushReaction 那条会失去可验证性（isPushable 会先在 destroySpeed == -1 处返回）。
	// 命名空间仍是 createoreexpansion（红线）：注册走 CoeRegistrate；id 取内部风格。
	public static final BlockEntry<GrinderCoverPlaceholderBlock> GRINDER_COVER_PLACEHOLDER = CoeRegistrate.REGISTRATE
		.block("grinder_cover_placeholder", GrinderCoverPlaceholderBlock::new)
		.properties(p -> p.mapColor(MapColor.NONE)
			.noCollission()
			.noOcclusion()
			.noLootTable()
			// ⚠ 这一条不能删：没有它，流体闸门 canHoldFluid 走 !blocksMotion() 会放行（见类注释）
			.forceSolidOn()
			.pushReaction(PushReaction.BLOCK)
			.instabreak()
			// ⚠ 方法名是 sound(SoundType)，不是 soundType(..)（1.21.1：Properties#sound）
			.sound(SoundType.EMPTY))
		// 无状态方块（没有 OPEN / FACING 之类的属性）⇒ 一条 "" 变体即可，指向原版空气模型。
		.blockstate((ctx, prov) -> prov.simpleBlock(ctx.get(), new UncheckedModelFile("minecraft:block/air")))
		// 内部名字（英文侧由这里钉死，中文侧在 ChineseLangProvider 里补一条同名键 —— 中英键集必须对齐）。
		// 与 STRESS_INJECTOR 同款；这个方块没有物品形态、又是"看不见 + 瞄不到"，所以这个名字
		// 实际永远不会出现在任何界面上（创造页 / JEI / 搜索 / 悬浮提示都到不了它）。
		.lang("Grinder Cover Placeholder (internal)")
		.register();

	// ===== 能量感应灯（EnergySensingLamp）：暂时下架 —— 待作者重做模型后恢复注册。
	// 贴图/模型文件保留在 resources（assets/.../energy_wave_machine/jade_energy_sensing_lamp_*.json + png）。

	// ========== 应力注入器（stress_injector）—— ⚠ **不可获取的内部方块** ==========
	// 用户 2026-10-01 规格 §4.2（方案 A）：临域充力发动时由技能**临时放置**在曲柄旁边、
	// 收尾时移除；容量按等级 8192/16384/32768 SU（我们自己的表，不借 Create 给曲柄的容量）。
	// 用户原话："可千万千万不要真的把它注册成一个能够被获取的方块。这个玩意儿应该是非常隐藏的那种。"
	// ⇒ 与上面所有方块的三处关键区别（**缺任何一条玩家就能拿到它**）：
	//   ① **没有 .item()** ⇒ 没有 BlockItem：不进背包、不进创造页（Registrate 只把**物品**
	//      填进页：AbstractRegistrate#item(...) 里才读 defaultCreativeModeTab）、不进 JEI / 创造页搜索；
	//   ② **.properties(p -> p.noLootTable())** ⇒ Block#getLootTable() = BuiltInLootTables.EMPTY
	//      ⇒ Registrate 的战利品回调被跳过、BlockLootSubProvider 也跳过它 ⇒ **一个战利品表文件都不生成**，
	//      被挖/被炸也不掉任何东西；
	//   ③ 方块状态指向**原版**的 minecraft:block/air（其模型就是一个空的 "{}"，零贴图）——
	//      不新增贴图、不新增模型文件，也不引用任何 png（美术红线"一张图都不要改、也不要自己画"
	//      因此零触碰）；方块本身由 StressInjectorBlock#getRenderShape = RenderShape.INVISIBLE
	//      完全跳过渲染（用户 2026-10-02："我想把这个方块的材质设置成全透明"⇒ 做到"看不到任何东西"），
	//      并且 getShape 返回空形状 ⇒ 连选中描边都没有、射线也打不到它。
	// 命名空间仍是 createoreexpansion（红线），id 取内部风格。完整说明见
	// content/equipment/armor/field/StressInjectorBlock 的类注释。
	public static final BlockEntry<StressInjectorBlock> STRESS_INJECTOR = CoeRegistrate.REGISTRATE
		.block("stress_injector", StressInjectorBlock::new)
		.properties(p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE)
			.noCollission()
			.noOcclusion()
			.noLootTable())
		// 每个 FACING 各一条变体，一律指向**原版空气模型**（minecraft:block/air 的内容就是空的 "{}"，
		// 零贴图、零引用）：方块带属性时**不要**用空 key 的"全状态"写法，逐值列出来最稳
		// （本仓 reinforced_lightning_rod / 水晶都是这个写法）。这条 blockstate 只是"有定义"，
		// 因为 RenderShape.INVISIBLE 根本不进渲染管线 ⇒ 玩家一个像素都看不到。
		.blockstate((ctx, prov) -> {
			VariantBlockStateBuilder vb = prov.getVariantBuilder(ctx.get());
			for (Direction dir : Direction.values()) {
				vb.partialState()
					.with(StressInjectorBlock.FACING, dir)
					.modelForState()
					.modelFile(new UncheckedModelFile("minecraft:block/air"))
					.addModel();
			}
		})
		// 内部名字（英文侧由这里钉死，中文侧在 ChineseLangProvider 里补一条同名键 —— 中英键集必须对齐）。
		.lang("Stress Injector (internal)")
		.register();

	/** 强化避雷针：继承原版 LightningRodBlock（全部原版行为保留），叠加 γ 级能量波充能；
	 * 加入原版 lightning_rods tag（三叉戟引雷、铁砧工艺等交互正常作用）。 */
	public static final BlockEntry<ReinforcedLightningRodBlock> REINFORCED_LIGHTNING_ROD = CoeRegistrate.REGISTRATE
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

/** 触发本层注册类的类初始化：Registrate 的注册动作就是字段初始化，因此方法体为空。 */
	public static void register() {
	}

	private CoeMachines() {
	}
}
