package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.client.renderer.CreateChargerRenderer;
import com.hjmmd_8.createoreexpansion.client.renderer.GrinderRenderer;
import com.hjmmd_8.createoreexpansion.content.charger.block.JadeCreateChargerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlockEntity;
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

	public static void register() {
	}

	private AllBlockEntityTypes() {
	}
}
