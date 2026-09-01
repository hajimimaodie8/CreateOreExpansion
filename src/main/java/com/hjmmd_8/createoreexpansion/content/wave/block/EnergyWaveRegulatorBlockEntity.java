package com.hjmmd_8.createoreexpansion.content.wave.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 能量调级器方块实体：面板状态管理与齿轮啮合传播全部继承
 * {@link AbstractWaveGateBlockEntity}（SimpleKineticBlockEntity），
 * 本类仅提供自身的 {@code BlockEntityType} 构造器。
 *
 * <p>应力接入/转速计算/网络同步、面板 NBT 冗余备份均复用基类；
 * 齿轮随应力旋转由 {@code EnergyWaveRegulatorRenderer} 渲染。</p>
 */
public class EnergyWaveRegulatorBlockEntity extends AbstractWaveGateBlockEntity {

	public EnergyWaveRegulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}
}
