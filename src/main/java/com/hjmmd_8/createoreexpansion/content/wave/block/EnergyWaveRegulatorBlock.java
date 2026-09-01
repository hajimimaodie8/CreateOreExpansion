package com.hjmmd_8.createoreexpansion.content.wave.block;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * 能量调级器：六向应力机器（可水平/竖直放置，齿轮轴沿 FACING 方向）。
 *
 * <p>全部公共行为（面板属性、扳手分区点击、朝向、转速门槛、齿轮啮合、光照规则）
 * 继承自 {@link AbstractWaveGateBlock}；本类仅绑定方块实体。</p>
 *
 * <p><b>能量接收面（齿轮对侧两面）</b>：顶面（朝 FACING）与底面（朝 FACING 对面）
 * 各有一个能量接收面板，初始为 {@code close}（{@code wave_receiver_close} 纹理）；
 * 用扳手右键对应面切换 {@code open}/{@code close}（{@link #RECEIVER_TOP} /
 * {@link #RECEIVER_BOTTOM}），两面状态独立并存入方块实体 NBT
 * （退出存档重进仍保持）。</p>
 *
 * <p><b>应力波级调制</b>：接入应力且转速达标（≥ FAST，默认 100 RPM）时，
 * 按齿轮旋转方向对穿过/遣返的波进行等级调制（升级/降级），判定见
 * {@code EnergyWaveRegulation}。</p>
 */
public class EnergyWaveRegulatorBlock extends AbstractWaveGateBlock<EnergyWaveRegulatorBlockEntity> {

	public EnergyWaveRegulatorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Class<EnergyWaveRegulatorBlockEntity> getBlockEntityClass() {
		return EnergyWaveRegulatorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends EnergyWaveRegulatorBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ENERGY_WAVE_REGULATOR.get();
	}
}
