package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.client.renderer.CreateChargerRenderer;
import com.hjmmd_8.createoreexpansion.client.renderer.GrinderRenderer;
import com.hjmmd_8.createoreexpansion.client.renderer.wave.EnergyWaveDisperserRenderer;
import com.hjmmd_8.createoreexpansion.client.renderer.wave.SixFaceDisperserRenderer;
import com.hjmmd_8.createoreexpansion.client.renderer.wave.WaveGateRenderer;
import com.hjmmd_8.createoreexpansion.content.charger.block.JadeCreateChargerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.crystal.CrystalBuddingBlockEntity;
import com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlockEntity;
import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergySensingLampBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveRegulatorBlockEntity;
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

	public static final BlockEntityEntry<JadeCreateChargerBlockEntity> JADE_CREATE_CHARGER = CreateOreExpansion.REGISTRATE
		.blockEntity("jade_create_charger", JadeCreateChargerBlockEntity::new)
		.validBlocks(AllBlocks.JADE_CREATE_CHARGER)
		.renderer(() -> CreateChargerRenderer::new)
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

	/** 能量感应灯计时方块实体（仅服务端计时：亮态到点熄灭；无渲染器，外观走 blockstate 变体模型） */
	public static final BlockEntityEntry<EnergySensingLampBlockEntity> ENERGY_SENSING_LAMP = CreateOreExpansion.REGISTRATE
		.blockEntity("energy_sensing_lamp", EnergySensingLampBlockEntity::new)
		.validBlocks(AllBlocks.ENERGY_SENSING_LAMP)
		.register();

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
