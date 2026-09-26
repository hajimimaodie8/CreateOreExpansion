package com.hjmmd_8.createoreexpansion.content.wave.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 八面能量波差器方块实体：纯静态被动机器，无应力/无数据，
 * 仅承载指示灯渲染器（{@code OctaEnergyWaveDifferencerRenderer} 按 8 面开口在顶/底环位叠灯）。
 *
 * <p>状态全部存于 blockstate（8 个 {@code open_*} 属性 + axis），随存档/网络包自动同步客户端。</p>
 */
public class OctaEnergyWaveDifferencerBlockEntity extends BlockEntity {

	public OctaEnergyWaveDifferencerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}
}
