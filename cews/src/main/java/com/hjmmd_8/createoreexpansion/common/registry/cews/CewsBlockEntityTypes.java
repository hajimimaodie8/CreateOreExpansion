package com.hjmmd_8.createoreexpansion.common.registry.cews;

import com.hjmmd_8.createoreexpansion.client.renderer.cews.CreateChargerRenderer;
import com.hjmmd_8.createoreexpansion.client.renderer.cews.FieldControllerRenderer;
import com.hjmmd_8.createoreexpansion.client.renderer.wave.EnergyWaveDisperserRenderer;
import com.hjmmd_8.createoreexpansion.client.renderer.wave.OctaEnergyWaveDifferencerRenderer;
import com.hjmmd_8.createoreexpansion.client.renderer.wave.SixFaceDisperserRenderer;
import com.hjmmd_8.createoreexpansion.client.renderer.wave.WaveGateRenderer;
import com.hjmmd_8.createoreexpansion.content.charger.block.JadeStressChargerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.charger.block.SapphireStressChargerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.charger.block.StellarstoneStressChargerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.machine.energyfieldcontroller.EnergyFieldControllerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveTransmuterBlockEntity;
import com.hjmmd_8.createoreexpansion.client.renderer.cews.StellarWaveTransmuterRenderer;
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
 * <b>CEWS（能量波阵学）</b>方块实体注册：三种应力充能器、能量场控制器、星辉波变器、
 * 能量调级器 ×3、波速调节器 ×3、波差器家族 ×3，以及能量波实体所使用的渲染器接线。<br>
 * 归属清单权威出处：{@code markdown_output/CEWS 能量波阵学模块（拆分方案与思索）.md} §3.2。
 * 本次拆分是<b>纯搬运</b>，{@code validBlocks} 引用同层 {@code CewsBlocks} 的条目（同一对象）。
 */
public final class CewsBlockEntityTypes {

	public static final BlockEntityEntry<JadeStressChargerBlockEntity> JADE_STRESS_CHARGER = CewsRegistrate.REGISTRATE
		.blockEntity("jade_stress_charger", JadeStressChargerBlockEntity::new)
		.validBlocks(CewsBlocks.JADE_STRESS_CHARGER)
		.renderer(() -> CreateChargerRenderer::new)
		.register();

	/** 蓝宝石应力充能器方块实体（双模式：普通连续发射 / 储存簇射；渲染与翡翠共用 CreateChargerRenderer） */
	public static final BlockEntityEntry<SapphireStressChargerBlockEntity> SAPPHIRE_STRESS_CHARGER =
		CewsRegistrate.REGISTRATE
			.blockEntity("sapphire_stress_charger", SapphireStressChargerBlockEntity::new)
			.validBlocks(CewsBlocks.SAPPHIRE_STRESS_CHARGER)
			.renderer(() -> CreateChargerRenderer::new)
			.register();

	/** 星辉石应力充能器方块实体（手动发射等级 + 双模式；渲染与翡翠/蓝宝石共用 CreateChargerRenderer） */
	public static final BlockEntityEntry<StellarstoneStressChargerBlockEntity> STELLARSTONE_STRESS_CHARGER =
		CewsRegistrate.REGISTRATE
			.blockEntity("stellarstone_stress_charger", StellarstoneStressChargerBlockEntity::new)
			.validBlocks(CewsBlocks.STELLARSTONE_STRESS_CHARGER)
			.renderer(() -> CreateChargerRenderer::new)
			.register();

	/** 能量场控制器方块实体（应力 → 场强档位；渲染：FACING 反面底部传动轴） */
	public static final BlockEntityEntry<EnergyFieldControllerBlockEntity> ENERGY_FIELD_CONTROLLER =
		CewsRegistrate.REGISTRATE
			.blockEntity("energy_field_controller", EnergyFieldControllerBlockEntity::new)
			.validBlocks(CewsBlocks.ENERGY_FIELD_CONTROLLER)
			.renderer(() -> FieldControllerRenderer::new)
			.register();

	/** 星辉波变器方块实体（扫描加工机 → 为穿过的波附加加工属性；渲染：FACING 反面底部传动轴） */
	public static final BlockEntityEntry<StellarWaveTransmuterBlockEntity> STELLAR_WAVE_TRANSMUTER =
		CewsRegistrate.REGISTRATE
			.blockEntity("stellar_wave_transmuter", StellarWaveTransmuterBlockEntity::new)
			.validBlocks(CewsBlocks.STELLAR_WAVE_TRANSMUTER)
			.renderer(() -> StellarWaveTransmuterRenderer::new)
			.register();

	/** 能量调级器方块实体（齿轮随应力旋转） */
	public static final BlockEntityEntry<EnergyWaveRegulatorBlockEntity> ENERGY_WAVE_REGULATOR = CewsRegistrate.REGISTRATE
		.blockEntity("energy_wave_regulator", EnergyWaveRegulatorBlockEntity::new)
		.validBlocks(CewsBlocks.ENERGY_WAVE_REGULATOR)
		.renderer(() -> WaveGateRenderer::new)
		.register();

	/** 波速调节器方块实体（齿轮随应力旋转，渲染与调级器共用 WaveGateRenderer） */
	public static final BlockEntityEntry<WaveSpeedRegulatorBlockEntity> WAVE_SPEED_REGULATOR = CewsRegistrate.REGISTRATE
		.blockEntity("wave_speed_regulator", WaveSpeedRegulatorBlockEntity::new)
		.validBlocks(CewsBlocks.WAVE_SPEED_REGULATOR)
		.renderer(() -> WaveGateRenderer::new)
		.register();

	/** 蓝宝石能量调级器方块实体（渲染与翡翠调级器共用 WaveGateRenderer） */
	public static final BlockEntityEntry<SapphireWaveRegulatorBlockEntity> SAPPHIRE_WAVE_REGULATOR =
		CewsRegistrate.REGISTRATE
			.blockEntity("sapphire_wave_regulator", SapphireWaveRegulatorBlockEntity::new)
			.validBlocks(CewsBlocks.SAPPHIRE_WAVE_REGULATOR)
			.renderer(() -> WaveGateRenderer::new)
			.register();

	/** 蓝宝石波速调节器方块实体（渲染与翡翠波速调节器共用 WaveGateRenderer） */
	public static final BlockEntityEntry<SapphireSpeedRegulatorBlockEntity> SAPPHIRE_SPEED_REGULATOR =
		CewsRegistrate.REGISTRATE
			.blockEntity("sapphire_speed_regulator", SapphireSpeedRegulatorBlockEntity::new)
			.validBlocks(CewsBlocks.SAPPHIRE_SPEED_REGULATOR)
			.renderer(() -> WaveGateRenderer::new)
			.register();

	/** 星辉石能量调级器方块实体（渲染与蓝宝石/翡翠调级器共用 WaveGateRenderer） */
	public static final BlockEntityEntry<StellarstoneWaveRegulatorBlockEntity> STELLARSTONE_WAVE_REGULATOR =
		CewsRegistrate.REGISTRATE
			.blockEntity("stellarstone_wave_regulator", StellarstoneWaveRegulatorBlockEntity::new)
			.validBlocks(CewsBlocks.STELLARSTONE_WAVE_REGULATOR)
			.renderer(() -> WaveGateRenderer::new)
			.register();

	/** 星辉石波速调节器方块实体（渲染与翡翠/蓝宝石波速调节器共用 WaveGateRenderer） */
	public static final BlockEntityEntry<StellarstoneSpeedRegulatorBlockEntity> STELLARSTONE_SPEED_REGULATOR =
		CewsRegistrate.REGISTRATE
			.blockEntity("stellarstone_speed_regulator", StellarstoneSpeedRegulatorBlockEntity::new)
			.validBlocks(CewsBlocks.STELLARSTONE_SPEED_REGULATOR)
			.renderer(() -> WaveGateRenderer::new)
			.register();

	/** 能量波差器方块实体（无应力静态，承载灯盘自定义渲染：按4侧面开闭状态叠灯位） */
	public static final BlockEntityEntry<EnergyWaveDisperserBlockEntity> ENERGY_WAVE_DISPERSER = CewsRegistrate.REGISTRATE
		.blockEntity("energy_wave_disperser", EnergyWaveDisperserBlockEntity::new)
		.validBlocks(CewsBlocks.ENERGY_WAVE_DISPERSER)
		.renderer(() -> EnergyWaveDisperserRenderer::new)
		.register();

	/** 六面能量波差器方块实体（无朝向静态，承载指示灯渲染：按相邻面开闭状态在面上叠灯） */
	public static final BlockEntityEntry<SixFaceDisperserBlockEntity> SIX_FACE_DISPERSER = CewsRegistrate.REGISTRATE
		.blockEntity("six_face_disperser", SixFaceDisperserBlockEntity::new)
		.validBlocks(CewsBlocks.SIX_FACE_DISPERSER)
		.renderer(() -> SixFaceDisperserRenderer::new)
		.register();

	/** 八面能量波差器方块实体（纯静态；承载顶/底 8 向指示灯渲染：开口方向灯亮） */
	public static final BlockEntityEntry<OctaEnergyWaveDifferencerBlockEntity> OCTA_ENERGY_WAVE_DIFFERENCER =
		CewsRegistrate.REGISTRATE
			.blockEntity("octa_energy_wave_differencer", OctaEnergyWaveDifferencerBlockEntity::new)
			.validBlocks(CewsBlocks.OCTA_ENERGY_WAVE_DIFFERENCER)
			.renderer(() -> OctaEnergyWaveDifferencerRenderer::new)
			.register();

/** 触发本层注册类的类初始化：Registrate 的注册动作就是字段初始化，因此方法体为空。 */
	public static void register() {
	}

	private CewsBlockEntityTypes() {
	}
}
