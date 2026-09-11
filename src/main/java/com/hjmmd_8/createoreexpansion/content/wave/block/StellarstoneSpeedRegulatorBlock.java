package com.hjmmd_8.createoreexpansion.content.wave.block;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * 星辉石波速调节器：六向应力机器（齿轮轴沿 FACING），星辉石科技线专属。
 *
 * <p>与蓝宝石波速调节器同构，公共行为继承 {@link AbstractWaveGateBlock}；机型差异：
 * <ul>
 *   <li><b>32 RPM 起调制</b>（蓝宝石为 64 RPM）；</li>
 *   <li><b>32~256 RPM 平均分 8 档</b>，速度修正 0.5 / 1 / 1.5 / 2 / 2.5 / 3 / 3.5 / 4 格/秒
 *       （蓝宝石仅 6 档 0.5~3），见 {@link StellarstoneSpeedRegulatorBlockEntity}。</li>
 * </ul>
 * 本类仅绑定方块实体。
 */
public class StellarstoneSpeedRegulatorBlock extends AbstractWaveGateBlock<StellarstoneSpeedRegulatorBlockEntity> {

	public StellarstoneSpeedRegulatorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Class<StellarstoneSpeedRegulatorBlockEntity> getBlockEntityClass() {
		return StellarstoneSpeedRegulatorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends StellarstoneSpeedRegulatorBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.STELLARSTONE_SPEED_REGULATOR.get();
	}
}
