package com.hjmmd_8.createoreexpansion.content.wave.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 能量波差器方块实体：纯静态被动机器，无应力/无数据，仅承载自定义渲染器
 * （{@code EnergyWaveDisperserRenderer} 按 4 侧面开闭状态在顶/底灯盘叠加灯位）。
 *
 * <p>状态全部存于 blockstate（{@code open_north}/{@code open_east}/{@code open_south}/{@code open_west}），
 * 方块状态随存档/网络包自动同步客户端，渲染器直接读 blockstate 即可。</p>
 */
public class EnergyWaveDisperserBlockEntity extends BlockEntity {

	public EnergyWaveDisperserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}
}
