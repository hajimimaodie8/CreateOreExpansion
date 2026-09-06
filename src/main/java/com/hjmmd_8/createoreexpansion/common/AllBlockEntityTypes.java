package com.hjmmd_8.createoreexpansion.common;

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
import com.hjmmd_8.createoreexpansion.content.crystal.CrystalBuddingBlockEntity;
import com.hjmmd_8.createoreexpansion.content.machine.energyfieldcontroller.EnergyFieldControllerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlockEntity;
import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveRegulatorBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.OctaEnergyWaveDifferencerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.SapphireSpeedRegulatorBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.SapphireWaveRegulatorBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.SixFaceDisperserBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.WaveSpeedRegulatorBlockEntity;
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

	public static final BlockEntityEntry<JadeStressChargerBlockEntity> JADE_STRESS_CHARGER = CreateOreExpansion.REGISTRATE
		.blockEntity("jade_stress_charger", JadeStressChargerBlockEntity::new)
		.validBlocks(AllBlocks.JADE_STRESS_CHARGER)
		.renderer(() -> CreateChargerRenderer::new)
		.register();

	/** 蓝宝石应力充能器方块实体（双模式：普通连续发射 / 储存簇射；渲染与翡翠共用 CreateChargerRenderer） */
	public static final BlockEntityEntry<SapphireStressChargerBlockEntity> SAPPHIRE_STRESS_CHARGER =
		CreateOreExpansion.REGISTRATE
			.blockEntity("sapphire_stress_charger", SapphireStressChargerBlockEntity::new)
			.validBlocks(AllBlocks.SAPPHIRE_STRESS_CHARGER)
			.renderer(() -> CreateChargerRenderer::new)
			.register();

	/** 能量场控制器方块实体（应力 → 场强档位；渲染：FACING 反面底部传动轴） */
	public static final BlockEntityEntry<EnergyFieldControllerBlockEntity> ENERGY_FIELD_CONTROLLER =
		CreateOreExpansion.REGISTRATE
			.blockEntity("energy_field_controller", EnergyFieldControllerBlockEntity::new)
			.validBlocks(AllBlocks.ENERGY_FIELD_CONTROLLER)
			.renderer(() -> FieldControllerRenderer::new)
			.register();

	/** 能量调级器方块实体（齿轮随应力旋转） */
	public static final BlockEntityEntry<EnergyWaveRegulatorBlockEntity> ENERGY_WAVE_REGULATOR = CreateOreExpansion.REGISTRATE
		.blockEntity("energy_wave_regulator", EnergyWaveRegulatorBlockEntity::new)
		.validBlocks(AllBlocks.ENERGY_WAVE_REGULATOR)
		.renderer(() -> WaveGateRenderer::new)
		.register();

	/** 波速调节器方块实体（齿轮随应力旋转，渲染与调级器共用 WaveGateRenderer） */
	public static final BlockEntityEntry<WaveSpeedRegulatorBlockEntity> WAVE_SPEED_REGULATOR = CreateOreExpansion.REGISTRATE
		.blockEntity("wave_speed_regulator", WaveSpeedRegulatorBlockEntity::new)
		.validBlocks(AllBlocks.WAVE_SPEED_REGULATOR)
		.renderer(() -> WaveGateRenderer::new)
		.register();

	/** 蓝宝石能量调级器方块实体（渲染与翡翠调级器共用 WaveGateRenderer） */
	public static final BlockEntityEntry<SapphireWaveRegulatorBlockEntity> SAPPHIRE_WAVE_REGULATOR =
		CreateOreExpansion.REGISTRATE
			.blockEntity("sapphire_wave_regulator", SapphireWaveRegulatorBlockEntity::new)
			.validBlocks(AllBlocks.SAPPHIRE_WAVE_REGULATOR)
			.renderer(() -> WaveGateRenderer::new)
			.register();

	/** 蓝宝石波速调节器方块实体（渲染与翡翠波速调节器共用 WaveGateRenderer） */
	public static final BlockEntityEntry<SapphireSpeedRegulatorBlockEntity> SAPPHIRE_SPEED_REGULATOR =
		CreateOreExpansion.REGISTRATE
			.blockEntity("sapphire_speed_regulator", SapphireSpeedRegulatorBlockEntity::new)
			.validBlocks(AllBlocks.SAPPHIRE_SPEED_REGULATOR)
			.renderer(() -> WaveGateRenderer::new)
			.register();

	/** 能量波差器方块实体（无应力静态，承载灯盘自定义渲染：按4侧面开闭状态叠灯位） */
	public static final BlockEntityEntry<EnergyWaveDisperserBlockEntity> ENERGY_WAVE_DISPERSER = CreateOreExpansion.REGISTRATE
		.blockEntity("energy_wave_disperser", EnergyWaveDisperserBlockEntity::new)
		.validBlocks(AllBlocks.ENERGY_WAVE_DISPERSER)
		.renderer(() -> EnergyWaveDisperserRenderer::new)
		.register();

	/** 六面能量波差器方块实体（无朝向静态，承载指示灯渲染：按相邻面开闭状态在面上叠灯） */
	public static final BlockEntityEntry<SixFaceDisperserBlockEntity> SIX_FACE_DISPERSER = CreateOreExpansion.REGISTRATE
		.blockEntity("six_face_disperser", SixFaceDisperserBlockEntity::new)
		.validBlocks(AllBlocks.SIX_FACE_DISPERSER)
		.renderer(() -> SixFaceDisperserRenderer::new)
		.register();

	/** 八面能量波差器方块实体（纯静态；承载顶/底 8 向指示灯渲染：开口方向灯亮） */
	public static final BlockEntityEntry<OctaEnergyWaveDifferencerBlockEntity> OCTA_ENERGY_WAVE_DIFFERENCER =
		CreateOreExpansion.REGISTRATE
			.blockEntity("octa_energy_wave_differencer", OctaEnergyWaveDifferencerBlockEntity::new)
			.validBlocks(AllBlocks.OCTA_ENERGY_WAVE_DIFFERENCER)
			.renderer(() -> OctaEnergyWaveDifferencerRenderer::new)
			.register();

	// ===== 能量感应灯方块实体：与方块一同暂时下架（待重做模型后恢复） =====

	/** 强化避雷针方块实体（伽马充能状态；渲染用原版避雷针模型，无需自定义渲染器） */
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

	public static void register() {
	}

	private AllBlockEntityTypes() {
	}
}
