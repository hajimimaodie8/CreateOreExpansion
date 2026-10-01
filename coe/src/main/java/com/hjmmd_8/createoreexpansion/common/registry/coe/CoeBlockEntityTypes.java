package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.client.renderer.GrinderRenderer;
import com.hjmmd_8.createoreexpansion.content.crystal.CrystalBuddingBlockEntity;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.field.StressInjectorBlockEntity;
import com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlockEntity;
import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlockEntity;
import com.hjmmd_8.createoreexpansion.common.*;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

/**
 * <b>COE（矿物拓展）</b>方块实体注册：动力角磨床、强化避雷针、水晶芽床（四种宝石共用）。<br>
 * 注册触发顺序（方块 → 方块实体 → 物品）由 {@code CreateOreExpansion} 按层显式调用；本次拆分是<b>纯搬运</b>。
 */
public final class CoeBlockEntityTypes {

	public static final BlockEntityEntry<PowerAngleGrinderBlockEntity> POWER_ANGLE_GRINDER = CoeRegistrate.REGISTRATE
		.blockEntity("power_angle_grinder", PowerAngleGrinderBlockEntity::new)
		.validBlocks(CoeBlocks.POWER_ANGLE_GRINDER)
		.renderer(() -> GrinderRenderer::new)
		.register();

	// ===== 能量感应灯方块实体：与方块一同暂时下架（待重做模型后恢复） =====

	/**
	 * <b>应力注入器方块实体</b>（临域充力临时放置的隐藏方块，规格 §4.2 方案 A）。
	 *
	 * <p>刻意<b>不</b>注册渲染器：它的方块状态是原版玻璃模型（{@code minecraft:block/glass}），
	 * 不需要方块实体渲染；Flywheel 侧没有 visualizer 时
	 * {@code VisualizationHelper#queueUpdate} 会走 {@code BlockEntityStorage#willAccept}
	 * 直接 return（已核 flywheel 1.0.6 源码）⇒ 不会 NPE、也不会被跳过渲染。</p>
	 */
	public static final BlockEntityEntry<StressInjectorBlockEntity> STRESS_INJECTOR = CoeRegistrate.REGISTRATE
		.blockEntity("stress_injector", StressInjectorBlockEntity::new)
		.validBlocks(CoeBlocks.STRESS_INJECTOR)
		.register();

	/** 强化避雷针方块实体（γ 充能状态；渲染用原版避雷针模型，无需自定义渲染器） */
	public static final BlockEntityEntry<ReinforcedLightningRodBlockEntity> REINFORCED_LIGHTNING_ROD = CoeRegistrate.REGISTRATE
		.blockEntity("reinforced_lightning_rod", ReinforcedLightningRodBlockEntity::new)
		.validBlocks(CoeBlocks.REINFORCED_LIGHTNING_ROD)
		.register();

	/** 水晶芽床生长进度方块实体（四种宝石芽床共用） */
	public static final BlockEntityEntry<CrystalBuddingBlockEntity> CRYSTAL_BUDDING = CoeRegistrate.REGISTRATE
		.blockEntity("crystal_budding", CrystalBuddingBlockEntity::new)
		.validBlocks(CoeBlocks.JADE_BUDDING_BLOCK, CoeBlocks.TOPAZ_BUDDING_BLOCK,
			CoeBlocks.SAPPHIRE_BUDDING_BLOCK, CoeBlocks.STELLARSTONE_BUDDING_BLOCK)
		.register();

/** 触发本层注册类的类初始化：Registrate 的注册动作就是字段初始化，因此方法体为空。 */
	public static void register() {
	}

	private CoeBlockEntityTypes() {
	}
}
