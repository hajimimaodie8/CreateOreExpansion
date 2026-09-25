package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.client.renderer.CreateChargerRenderer;
import com.hjmmd_8.createoreexpansion.client.renderer.FieldControllerRenderer;
import com.hjmmd_8.createoreexpansion.client.renderer.GrinderRenderer;
import com.hjmmd_8.createoreexpansion.client.renderer.wave.EnergyWaveDisperserRenderer;
import com.hjmmd_8.createoreexpansion.client.renderer.wave.OctaEnergyWaveDifferencerRenderer;
import com.hjmmd_8.createoreexpansion.client.renderer.wave.SixFaceDisperserRenderer;
import com.hjmmd_8.createoreexpansion.client.renderer.wave.WaveGateRenderer;
import com.hjmmd_8.createoreexpansion.content.charger.block.JadeStressChargerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.charger.block.SapphireStressChargerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.charger.block.StellarstoneStressChargerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.crystal.CrystalBuddingBlockEntity;
import com.hjmmd_8.createoreexpansion.content.machine.energyfieldcontroller.EnergyFieldControllerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveTransmuterBlockEntity;
import com.hjmmd_8.createoreexpansion.client.renderer.StellarWaveTransmuterRenderer;
import com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlockEntity;
import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveRegulatorBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.OctaEnergyWaveDifferencerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.SapphireSpeedRegulatorBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.SapphireWaveRegulatorBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.StellarstoneSpeedRegulatorBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.StellarstoneWaveRegulatorBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.SixFaceDisperserBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.WaveSpeedRegulatorBlockEntity;
import com.hjmmd_8.createoreexpansion.common.*;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

/**
 * <b>COE（矿物拓展）</b>方块实体注册：动力角磨床、强化避雷针、水晶芽床（四种宝石共用）。<br>
 * 注册顺序（方块 → 方块实体 → 物品）见 {@code AllBlockEntityTypes} 的说明；本次拆分是<b>纯搬运</b>。
 */
public final class CoeBlockEntityTypes {

	public static final BlockEntityEntry<PowerAngleGrinderBlockEntity> POWER_ANGLE_GRINDER = CreateOreExpansion.REGISTRATE
		.blockEntity("power_angle_grinder", PowerAngleGrinderBlockEntity::new)
		.validBlocks(AllBlocks.POWER_ANGLE_GRINDER)
		.renderer(() -> GrinderRenderer::new)
		.register();

	// ===== 能量感应灯方块实体：与方块一同暂时下架（待重做模型后恢复） =====

	/** 强化避雷针方块实体（γ 充能状态；渲染用原版避雷针模型，无需自定义渲染器） */
	public static final BlockEntityEntry<ReinforcedLightningRodBlockEntity> REINFORCED_LIGHTNING_ROD = CreateOreExpansion.REGISTRATE
		.blockEntity("reinforced_lightning_rod", ReinforcedLightningRodBlockEntity::new)
		.validBlocks(AllBlocks.REINFORCED_LIGHTNING_ROD)
		.register();

	/** 水晶芽床生长进度方块实体（四种宝石芽床共用） */
	public static final BlockEntityEntry<CrystalBuddingBlockEntity> CRYSTAL_BUDDING = CreateOreExpansion.REGISTRATE
		.blockEntity("crystal_budding", CrystalBuddingBlockEntity::new)
		.validBlocks(AllBlocks.JADE_BUDDING_BLOCK, AllBlocks.TOPAZ_BUDDING_BLOCK,
			AllBlocks.SAPPHIRE_BUDDING_BLOCK, AllBlocks.STELLARSTONE_BUDDING_BLOCK)
		.register();

	private CoeBlockEntityTypes() {
	}
}
