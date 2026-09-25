package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsBlocks;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlocks;
import com.hjmmd_8.createoreexpansion.content.charger.block.JadeStressChargerBlock;
import com.hjmmd_8.createoreexpansion.content.charger.block.SapphireStressChargerBlock;
import com.hjmmd_8.createoreexpansion.content.charger.block.StellarstoneStressChargerBlock;
import com.hjmmd_8.createoreexpansion.content.crystal.CrystalBuddingBlock;
import com.hjmmd_8.createoreexpansion.content.crystal.CrystalClusterBlock;
import com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlock;
import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlock;
import com.hjmmd_8.createoreexpansion.content.machine.energyfieldcontroller.EnergyFieldControllerBlock;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveTransmuterBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveRegulatorBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.OctaEnergyWaveDifferencerBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.SapphireSpeedRegulatorBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.SapphireWaveRegulatorBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.SixFaceDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.StellarstoneSpeedRegulatorBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.StellarstoneWaveRegulatorBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.WaveSpeedRegulatorBlock;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.Block;

/**
 * 方块注册的<b>兼容外观（facade）</b>——P1「三层分区」改造后的落点。
 *
 * <p>真正的注册声明已按未来模块边界搬进各自的持有类：
 * 矿物拓展（COE）见 {@link CoeBlocks}、能量波阵学（CEWS）见 {@link CewsBlocks}。
 * 本类只保留<b>同名同类型</b>的别名，因此仓库里所有 {@code AllBlocks.XXX} 引用点
 * 一个字都不用改；注册 id / 贴图路径 / 数值 / 链式调用与拆分前完全一致。</p>
 *
 * <p><b>注册顺序</b>：拆分前三类之间是「方块 → 方块实体 → 物品」的触发顺序
 * （{@code CreateOreExpansion} 依次调用三个 {@code register()}）。这里用静态块
 * 显式按固定顺序把各层持有类拉起来——读取字段本身就是该层的类初始化，
 * 而 Registrate 的注册顺序 = 字段初始化顺序，因此顺序不受别名声明顺序影响。</p>
 *
 * <p>归属清单权威出处：{@code markdown_output/CEWS 能量波阵学模块（拆分方案与思索）.md} §3.2。</p>
 */
public final class AllBlocks {

	static {
		// 方块层：先 COE 再 CEWS（参数求值 = 触发对应持有类的类初始化）
		triggerLayerInit(CoeBlocks.JADE_ORE, CewsBlocks.JADE_CASING);
	}

	/** 只为触发各层持有类的类初始化而存在；真正的动作是调用处读取各层首字段。 */
	private static void triggerLayerInit(Object... firstEntries) {
	}

	// ==================== 矿物拓展（COE） ====================

	public static final BlockEntry<Block> JADE_ORE = CoeBlocks.JADE_ORE;
	public static final BlockEntry<Block> DEEPSLATE_JADE_ORE = CoeBlocks.DEEPSLATE_JADE_ORE;
	public static final BlockEntry<Block> JADE_BLOCK = CoeBlocks.JADE_BLOCK;
	public static final BlockEntry<Block> RAW_JADE_BLOCK = CoeBlocks.RAW_JADE_BLOCK;
	public static final BlockEntry<Block> TOPAZ_ORE = CoeBlocks.TOPAZ_ORE;
	public static final BlockEntry<Block> DEEPSLATE_TOPAZ_ORE = CoeBlocks.DEEPSLATE_TOPAZ_ORE;
	public static final BlockEntry<Block> TOPAZ_BLOCK = CoeBlocks.TOPAZ_BLOCK;
	public static final BlockEntry<Block> RAW_TOPAZ_BLOCK = CoeBlocks.RAW_TOPAZ_BLOCK;
	public static final BlockEntry<Block> NETHER_SAPPHIRE_ORE = CoeBlocks.NETHER_SAPPHIRE_ORE;
	public static final BlockEntry<Block> SAPPHIRE_BLOCK = CoeBlocks.SAPPHIRE_BLOCK;
	public static final BlockEntry<Block> RAW_SAPPHIRE_BLOCK = CoeBlocks.RAW_SAPPHIRE_BLOCK;
	public static final BlockEntry<Block> END_STELLARSTONE_ORE = CoeBlocks.END_STELLARSTONE_ORE;
	public static final BlockEntry<Block> STELLARSTONE_BLOCK = CoeBlocks.STELLARSTONE_BLOCK;
	public static final BlockEntry<Block> RAW_STELLARSTONE_BLOCK = CoeBlocks.RAW_STELLARSTONE_BLOCK;
	public static final BlockEntry<Block> THUNDERITE_BLOCK = CoeBlocks.THUNDERITE_BLOCK;
	public static final BlockEntry<Block> RUBY_BLOCK = CoeBlocks.RUBY_BLOCK;
	public static final BlockEntry<Block> SANCTSTONE_BLOCK = CoeBlocks.SANCTSTONE_BLOCK;
	public static final BlockEntry<PowerAngleGrinderBlock> POWER_ANGLE_GRINDER = CoeBlocks.POWER_ANGLE_GRINDER;
	public static final BlockEntry<ReinforcedLightningRodBlock> REINFORCED_LIGHTNING_ROD = CoeBlocks.REINFORCED_LIGHTNING_ROD;
	public static final BlockEntry<CrystalClusterBlock> JADE_SMALL_BUD = CoeBlocks.JADE_SMALL_BUD;
	public static final BlockEntry<CrystalClusterBlock> JADE_MEDIUM_BUD = CoeBlocks.JADE_MEDIUM_BUD;
	public static final BlockEntry<CrystalClusterBlock> JADE_LARGE_BUD = CoeBlocks.JADE_LARGE_BUD;
	public static final BlockEntry<CrystalClusterBlock> JADE_CLUSTER = CoeBlocks.JADE_CLUSTER;
	public static final BlockEntry<CrystalBuddingBlock> JADE_BUDDING_BLOCK = CoeBlocks.JADE_BUDDING_BLOCK;
	public static final BlockEntry<CrystalClusterBlock> TOPAZ_SMALL_BUD = CoeBlocks.TOPAZ_SMALL_BUD;
	public static final BlockEntry<CrystalClusterBlock> TOPAZ_MEDIUM_BUD = CoeBlocks.TOPAZ_MEDIUM_BUD;
	public static final BlockEntry<CrystalClusterBlock> TOPAZ_LARGE_BUD = CoeBlocks.TOPAZ_LARGE_BUD;
	public static final BlockEntry<CrystalClusterBlock> TOPAZ_CLUSTER = CoeBlocks.TOPAZ_CLUSTER;
	public static final BlockEntry<CrystalBuddingBlock> TOPAZ_BUDDING_BLOCK = CoeBlocks.TOPAZ_BUDDING_BLOCK;
	public static final BlockEntry<CrystalClusterBlock> SAPPHIRE_SMALL_BUD = CoeBlocks.SAPPHIRE_SMALL_BUD;
	public static final BlockEntry<CrystalClusterBlock> SAPPHIRE_MEDIUM_BUD = CoeBlocks.SAPPHIRE_MEDIUM_BUD;
	public static final BlockEntry<CrystalClusterBlock> SAPPHIRE_LARGE_BUD = CoeBlocks.SAPPHIRE_LARGE_BUD;
	public static final BlockEntry<CrystalClusterBlock> SAPPHIRE_CLUSTER = CoeBlocks.SAPPHIRE_CLUSTER;
	public static final BlockEntry<CrystalBuddingBlock> SAPPHIRE_BUDDING_BLOCK = CoeBlocks.SAPPHIRE_BUDDING_BLOCK;
	public static final BlockEntry<CrystalClusterBlock> STELLARSTONE_SMALL_BUD = CoeBlocks.STELLARSTONE_SMALL_BUD;
	public static final BlockEntry<CrystalClusterBlock> STELLARSTONE_MEDIUM_BUD = CoeBlocks.STELLARSTONE_MEDIUM_BUD;
	public static final BlockEntry<CrystalClusterBlock> STELLARSTONE_LARGE_BUD = CoeBlocks.STELLARSTONE_LARGE_BUD;
	public static final BlockEntry<CrystalClusterBlock> STELLARSTONE_CLUSTER = CoeBlocks.STELLARSTONE_CLUSTER;
	public static final BlockEntry<CrystalBuddingBlock> STELLARSTONE_BUDDING_BLOCK = CoeBlocks.STELLARSTONE_BUDDING_BLOCK;

	// ==================== 能量波阵学（CEWS） ====================

	public static final BlockEntry<CasingBlock> JADE_CASING = CewsBlocks.JADE_CASING;
	public static final BlockEntry<JadeStressChargerBlock> JADE_STRESS_CHARGER = CewsBlocks.JADE_STRESS_CHARGER;
	public static final BlockEntry<SapphireStressChargerBlock> SAPPHIRE_STRESS_CHARGER = CewsBlocks.SAPPHIRE_STRESS_CHARGER;
	public static final BlockEntry<StellarstoneStressChargerBlock> STELLARSTONE_STRESS_CHARGER = CewsBlocks.STELLARSTONE_STRESS_CHARGER;
	public static final BlockEntry<EnergyFieldControllerBlock> ENERGY_FIELD_CONTROLLER = CewsBlocks.ENERGY_FIELD_CONTROLLER;
	public static final BlockEntry<StellarWaveTransmuterBlock> STELLAR_WAVE_TRANSMUTER = CewsBlocks.STELLAR_WAVE_TRANSMUTER;
	public static final BlockEntry<SapphireWaveRegulatorBlock> SAPPHIRE_WAVE_REGULATOR = CewsBlocks.SAPPHIRE_WAVE_REGULATOR;
	public static final BlockEntry<EnergyWaveRegulatorBlock> ENERGY_WAVE_REGULATOR = CewsBlocks.ENERGY_WAVE_REGULATOR;
	public static final BlockEntry<WaveSpeedRegulatorBlock> WAVE_SPEED_REGULATOR = CewsBlocks.WAVE_SPEED_REGULATOR;
	public static final BlockEntry<SapphireSpeedRegulatorBlock> SAPPHIRE_SPEED_REGULATOR = CewsBlocks.SAPPHIRE_SPEED_REGULATOR;
	public static final BlockEntry<StellarstoneWaveRegulatorBlock> STELLARSTONE_WAVE_REGULATOR = CewsBlocks.STELLARSTONE_WAVE_REGULATOR;
	public static final BlockEntry<StellarstoneSpeedRegulatorBlock> STELLARSTONE_SPEED_REGULATOR = CewsBlocks.STELLARSTONE_SPEED_REGULATOR;
	public static final BlockEntry<EnergyWaveDisperserBlock> ENERGY_WAVE_DISPERSER = CewsBlocks.ENERGY_WAVE_DISPERSER;
	public static final BlockEntry<SixFaceDisperserBlock> SIX_FACE_DISPERSER = CewsBlocks.SIX_FACE_DISPERSER;
	public static final BlockEntry<OctaEnergyWaveDifferencerBlock> OCTA_ENERGY_WAVE_DIFFERENCER = CewsBlocks.OCTA_ENERGY_WAVE_DIFFERENCER;
	public static final BlockEntry<CasingBlock> SAPPHIRE_CASING = CewsBlocks.SAPPHIRE_CASING;
	public static final BlockEntry<CasingBlock> STELLARSTONE_CASING = CewsBlocks.STELLARSTONE_CASING;

	/** 保留旧调用点：Registrate 的注册由字段初始化完成，本方法本身不做事。 */
	public static void register() {
	}

	private AllBlocks() {
	}
}
