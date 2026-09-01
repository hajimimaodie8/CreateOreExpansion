package com.hjmmd_8.createoreexpansion.content.wave.block;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * 波速调节器：六向应力机器（可水平/竖直放置，齿轮轴沿 FACING 方向）。
 *
 * <p>与能量调级器同构（模型/贴图/旋转/扳手交互完全一致，仅侧面贴图暂为复制占位），
 * 公共行为继承 {@link AbstractWaveGateBlock}；本类仅绑定方块实体。</p>
 *
 * <p><b>应力速度调制</b>：接入应力且转速达标（≥ FAST，默认 100 RPM）时，
 * 按齿轮旋转方向对穿过/遣返的波进行<b>速度</b>调制（加速/减速）——
 * 调制度按接入转速在 100~256 RPM 区间分 4 档（+0.5 / +1 / +1.5 / +2 格每秒），
 * 判定见 {@code WaveSpeedRegulation}。<b>反弹时不改变速度</b>（仅调级器反弹降级）。</p>
 */
public class WaveSpeedRegulatorBlock extends AbstractWaveGateBlock<WaveSpeedRegulatorBlockEntity> {

	public WaveSpeedRegulatorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Class<WaveSpeedRegulatorBlockEntity> getBlockEntityClass() {
		return WaveSpeedRegulatorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends WaveSpeedRegulatorBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.WAVE_SPEED_REGULATOR.get();
	}
}
