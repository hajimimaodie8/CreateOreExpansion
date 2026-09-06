package com.hjmmd_8.createoreexpansion.content.wave.block;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * 蓝宝石波速调节器：六向应力机器（齿轮轴沿 FACING），蓝宝石科技线专属。
 *
 * <p>与翡翠波速调节器同构，公共行为继承 {@link AbstractWaveGateBlock}；机型差异：
 * <ul>
 *   <li><b>64 RPM 起调制</b>（翡翠为 FAST 100 RPM）；</li>
 *   <li><b>64~256 RPM 平均分 6 档</b>，速度修正 0.5 / 1 / 1.5 / 2 / 2.5 / 3 格/秒
 *       （翡翠仅 4 档 0.5~2），见 {@link SapphireSpeedRegulatorBlockEntity}。</li>
 * </ul>
 * 本类仅绑定方块实体。
 */
public class SapphireSpeedRegulatorBlock extends AbstractWaveGateBlock<SapphireSpeedRegulatorBlockEntity> {

	public SapphireSpeedRegulatorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Class<SapphireSpeedRegulatorBlockEntity> getBlockEntityClass() {
		return SapphireSpeedRegulatorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SapphireSpeedRegulatorBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.SAPPHIRE_SPEED_REGULATOR.get();
	}
}
