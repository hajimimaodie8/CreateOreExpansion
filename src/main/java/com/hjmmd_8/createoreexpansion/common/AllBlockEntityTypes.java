package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.client.renderer.CreateChargerRenderer;
import com.hjmmd_8.createoreexpansion.client.renderer.GrinderRenderer;
import com.hjmmd_8.createoreexpansion.content.charger.block.JadeCreateChargerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlockEntity;
import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

/**
 * 方块实体注册。
 */
public final class AllBlockEntityTypes {

	public static final BlockEntityEntry<PowerAngleGrinderBlockEntity> POWER_ANGLE_GRINDER = CreateOreExpansion.REGISTRATE
		.blockEntity("power_angle_grinder", PowerAngleGrinderBlockEntity::new)
		.validBlocks(AllBlocks.POWER_ANGLE_GRINDER)
		.renderer(() -> GrinderRenderer::new)
		.register();

	public static final BlockEntityEntry<JadeCreateChargerBlockEntity> JADE_CREATE_CHARGER = CreateOreExpansion.REGISTRATE
		.blockEntity("jade_create_charger", JadeCreateChargerBlockEntity::new)
		.validBlocks(AllBlocks.JADE_CREATE_CHARGER)
		.renderer(() -> CreateChargerRenderer::new)
		.register();

	/** 强化避雷针方块实体（伽马充能状态；渲染用原版避雷针模型，无需自定义渲染器） */
	public static final BlockEntityEntry<ReinforcedLightningRodBlockEntity> REINFORCED_LIGHTNING_ROD = CreateOreExpansion.REGISTRATE
		.blockEntity("reinforced_lightning_rod", ReinforcedLightningRodBlockEntity::new)
		.validBlocks(AllBlocks.REINFORCED_LIGHTNING_ROD)
		.register();

	public static void register() {
	}

	private AllBlockEntityTypes() {
	}
}
