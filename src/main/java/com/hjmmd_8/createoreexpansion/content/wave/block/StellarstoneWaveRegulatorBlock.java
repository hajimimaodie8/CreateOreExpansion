package com.hjmmd_8.createoreexpansion.content.wave.block;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * 星辉石能量调级器：六向应力机器（齿轮轴沿 FACING），星辉石科技线专属。
 *
 * <p>与蓝宝石能量调级器同构，公共行为继承 {@link AbstractWaveGateBlock}；机型差异：
 * <ul>
 *   <li><b>32 RPM 起调制</b>（蓝宝石为 64）——{@link StellarstoneWaveRegulatorBlockEntity}
 *       覆写 {@code getModulationSpeedThreshold()} = 32；</li>
 *   <li><b>最大可把波提升至 5 级（欧米伽）</b>；</li>
 *   <li><b>单次提升级数随转速</b>：32~128 RPM 每次 +1、129~256 RPM 每次 +2
 *       （蓝宝石恒为每次 +1），见 {@link StellarstoneWaveRegulatorBlockEntity#getBoostStepForSpeed()}。</li>
 * </ul>
 * 本类仅绑定方块实体。
 */
public class StellarstoneWaveRegulatorBlock extends AbstractWaveGateBlock<StellarstoneWaveRegulatorBlockEntity> {

	public StellarstoneWaveRegulatorBlock(Properties properties) {
		super(properties);
	}

	/** 星辉石能量调级器：对波做等级调制。 */
	@Override
	public boolean modulatesWaveLevel() {
		return true;
	}

	@Override
	public Class<StellarstoneWaveRegulatorBlockEntity> getBlockEntityClass() {
		return StellarstoneWaveRegulatorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends StellarstoneWaveRegulatorBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.STELLARSTONE_WAVE_REGULATOR.get();
	}
}
