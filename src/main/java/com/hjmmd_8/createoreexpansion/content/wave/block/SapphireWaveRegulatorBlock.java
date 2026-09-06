package com.hjmmd_8.createoreexpansion.content.wave.block;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * 蓝宝石能量调级器：六向应力机器（齿轮轴沿 FACING），蓝宝石科技线专属。
 *
 * <p>与翡翠能量调级器同构，公共行为继承 {@link AbstractWaveGateBlock}；机型差异：
 * <ul>
 *   <li><b>64 RPM 起调制</b>（翡翠为 FAST 100 RPM）——{@link SapphireWaveRegulatorBlockEntity}
 *       覆写 {@code getModulationSpeedThreshold()} = 64；</li>
 *   <li><b>最大可把波提升至 5 级（欧米伽）</b>——1~4 级波顺向逐级 +1 至 5
 *       （翡翠封顶 3，见 {@code getMaxBoostLevel()}）。</li>
 * </ul>
 * 本类仅绑定方块实体。
 */
public class SapphireWaveRegulatorBlock extends AbstractWaveGateBlock<SapphireWaveRegulatorBlockEntity> {

	public SapphireWaveRegulatorBlock(Properties properties) {
		super(properties);
	}

	/** 蓝宝石能量调级器：对波做等级调制。 */
	@Override
	public boolean modulatesWaveLevel() {
		return true;
	}

	@Override
	public Class<SapphireWaveRegulatorBlockEntity> getBlockEntityClass() {
		return SapphireWaveRegulatorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends SapphireWaveRegulatorBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.SAPPHIRE_WAVE_REGULATOR.get();
	}
}
