package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsBlockEntityTypes;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlockEntityTypes;
import com.hjmmd_8.createoreexpansion.content.charger.block.JadeStressChargerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.charger.block.SapphireStressChargerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.charger.block.StellarstoneStressChargerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.crystal.CrystalBuddingBlockEntity;
import com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlockEntity;
import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlockEntity;
import com.hjmmd_8.createoreexpansion.content.machine.energyfieldcontroller.EnergyFieldControllerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveTransmuterBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveRegulatorBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.OctaEnergyWaveDifferencerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.SapphireSpeedRegulatorBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.SapphireWaveRegulatorBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.SixFaceDisperserBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.StellarstoneSpeedRegulatorBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.StellarstoneWaveRegulatorBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.WaveSpeedRegulatorBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

/**
 * 方块实体注册的<b>兼容外观（facade）</b>——P1「三层分区」改造后的落点。
 *
 * <p>真正的注册声明已按未来模块边界搬进各自的持有类：
 * 矿物拓展（COE）见 {@link CoeBlockEntityTypes}、能量波阵学（CEWS）见 {@link CewsBlockEntityTypes}。
 * 本类只保留<b>同名同类型</b>的别名，因此仓库里所有 {@code AllBlockEntityTypes.XXX} 引用点
 * 一个字都不用改；注册 id 与链式调用与拆分前完全一致
 * （{@code validBlocks} 引用的方块仍走 {@code AllBlocks} 别名，指向同一个 {@code BlockEntry}）。</p>
 *
 * <p><b>注册顺序</b>：拆分前三类之间是「方块 → 方块实体 → 物品」的触发顺序
 * （{@code CreateOreExpansion} 依次调用三个 {@code register()}）。这里用静态块
 * 显式按固定顺序把各层持有类拉起来——读取字段本身就是该层的类初始化。</p>
 *
 * <p>归属清单权威出处：{@code markdown_output/CEWS 能量波阵学模块（拆分方案与思索）.md} §3.2。</p>
 */
public final class AllBlockEntityTypes {

	static {
		// 方块实体层：COE → CEWS（参数求值 = 触发对应持有类的类初始化）
		triggerLayerInit(CoeBlockEntityTypes.POWER_ANGLE_GRINDER, CewsBlockEntityTypes.JADE_STRESS_CHARGER);
	}

	/** 只为触发各层持有类的类初始化而存在；真正的动作是调用处读取各层首字段。 */
	private static void triggerLayerInit(Object... firstEntries) {
	}

	// ==================== 矿物拓展（COE） ====================

	public static final BlockEntityEntry<PowerAngleGrinderBlockEntity> POWER_ANGLE_GRINDER = CoeBlockEntityTypes.POWER_ANGLE_GRINDER;
	public static final BlockEntityEntry<ReinforcedLightningRodBlockEntity> REINFORCED_LIGHTNING_ROD = CoeBlockEntityTypes.REINFORCED_LIGHTNING_ROD;
	public static final BlockEntityEntry<CrystalBuddingBlockEntity> CRYSTAL_BUDDING = CoeBlockEntityTypes.CRYSTAL_BUDDING;

	// ==================== 能量波阵学（CEWS） ====================

	public static final BlockEntityEntry<JadeStressChargerBlockEntity> JADE_STRESS_CHARGER = CewsBlockEntityTypes.JADE_STRESS_CHARGER;
	public static final BlockEntityEntry<SapphireStressChargerBlockEntity> SAPPHIRE_STRESS_CHARGER = CewsBlockEntityTypes.SAPPHIRE_STRESS_CHARGER;
	public static final BlockEntityEntry<StellarstoneStressChargerBlockEntity> STELLARSTONE_STRESS_CHARGER = CewsBlockEntityTypes.STELLARSTONE_STRESS_CHARGER;
	public static final BlockEntityEntry<EnergyFieldControllerBlockEntity> ENERGY_FIELD_CONTROLLER = CewsBlockEntityTypes.ENERGY_FIELD_CONTROLLER;
	public static final BlockEntityEntry<StellarWaveTransmuterBlockEntity> STELLAR_WAVE_TRANSMUTER = CewsBlockEntityTypes.STELLAR_WAVE_TRANSMUTER;
	public static final BlockEntityEntry<EnergyWaveRegulatorBlockEntity> ENERGY_WAVE_REGULATOR = CewsBlockEntityTypes.ENERGY_WAVE_REGULATOR;
	public static final BlockEntityEntry<WaveSpeedRegulatorBlockEntity> WAVE_SPEED_REGULATOR = CewsBlockEntityTypes.WAVE_SPEED_REGULATOR;
	public static final BlockEntityEntry<SapphireWaveRegulatorBlockEntity> SAPPHIRE_WAVE_REGULATOR = CewsBlockEntityTypes.SAPPHIRE_WAVE_REGULATOR;
	public static final BlockEntityEntry<SapphireSpeedRegulatorBlockEntity> SAPPHIRE_SPEED_REGULATOR = CewsBlockEntityTypes.SAPPHIRE_SPEED_REGULATOR;
	public static final BlockEntityEntry<StellarstoneWaveRegulatorBlockEntity> STELLARSTONE_WAVE_REGULATOR = CewsBlockEntityTypes.STELLARSTONE_WAVE_REGULATOR;
	public static final BlockEntityEntry<StellarstoneSpeedRegulatorBlockEntity> STELLARSTONE_SPEED_REGULATOR = CewsBlockEntityTypes.STELLARSTONE_SPEED_REGULATOR;
	public static final BlockEntityEntry<EnergyWaveDisperserBlockEntity> ENERGY_WAVE_DISPERSER = CewsBlockEntityTypes.ENERGY_WAVE_DISPERSER;
	public static final BlockEntityEntry<SixFaceDisperserBlockEntity> SIX_FACE_DISPERSER = CewsBlockEntityTypes.SIX_FACE_DISPERSER;
	public static final BlockEntityEntry<OctaEnergyWaveDifferencerBlockEntity> OCTA_ENERGY_WAVE_DIFFERENCER = CewsBlockEntityTypes.OCTA_ENERGY_WAVE_DIFFERENCER;

	/** 保留旧调用点：Registrate 的注册由字段初始化完成，本方法本身不做事。 */
	public static void register() {
	}

	private AllBlockEntityTypes() {
	}
}
