package com.hjmmd_8.createoreexpansion.content.wave.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 六面能量波差器方块实体：纯静态被动机器，无应力/无数据，仅承载指示灯渲染器
 * （{@code SixFaceDisperserRenderer} 按各面相邻面的开闭状态在面上叠加指示灯）。
 *
 * <p>状态全部存于 blockstate（6 个 {@code open_*} 属性），随存档/网络包自动同步客户端。</p>
 */
public class SixFaceDisperserBlockEntity extends BlockEntity {

	public SixFaceDisperserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}
}