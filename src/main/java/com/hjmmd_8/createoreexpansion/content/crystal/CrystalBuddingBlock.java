package com.hjmmd_8.createoreexpansion.content.crystal;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.function.Supplier;

/**
 * 可生长水晶芽床（母岩）。
 *
 * <p>继承原版 {@link BuddingAmethystBlock}，保留其生长方向判定与支撑逻辑，
 * 使 AE2 晶体催生器（对原版芽床的 {@code instanceof} 兼容加速）可直接加速本模组水晶；
 * 同时实现 {@link EntityBlock} 挂载生长进度方块实体，提供确定性生长。</p>
 *
 * <p>随机刻与方块实体双轨生长：随机刻（原版节奏）与进度制（每 {@code GROW_INTERVAL_TICKS}
 * 保证一次）互补，进度制保证即使随机刻运气差也会持续产出。</p>
 */
public class CrystalBuddingBlock extends BuddingAmethystBlock implements EntityBlock {

	private final Supplier<? extends Block> smallBud;
	/** 芽床保底生芽间隔（秒，来自 {@link CrystalGrowthConfigs}） */
	private final int buddingGrowSeconds;

	public CrystalBuddingBlock(Properties properties, Supplier<? extends Block> smallBud,
			int buddingGrowSeconds) {
		super(properties);
		this.smallBud = smallBud;
		this.buddingGrowSeconds = buddingGrowSeconds;
	}

	/** 进度制保底生芽间隔（tick） */
	public int growIntervalTicks() {
		return Math.max(1, buddingGrowSeconds) * 20;
	}

	/** 在指定方向的空位生成一颗本宝石小芽（空气或水皆可，waterlogged 保持原流体） */
	public boolean tryGrowBud(Level level, BlockPos pos, Direction direction) {
		BlockPos neighbor = pos.relative(direction);
		BlockState neighborState = level.getBlockState(neighbor);
		if (neighborState.isAir()
			|| (neighborState.is(Blocks.WATER) && neighborState.getFluidState().is(Fluids.WATER))) {
			level.setBlockAndUpdate(neighbor, smallBud.get().defaultBlockState()
				.setValue(AmethystClusterBlock.FACING, direction)
				.setValue(AmethystClusterBlock.WATERLOGGED, neighborState.getFluidState().is(Fluids.WATER)));
			return true;
		}
		return false;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		// 仿原版：随机方向尝试生成小芽（AE2 催生器加速芽床时同样走此入口）
		tryGrowBud(level, pos, Direction.getRandom(random));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CrystalBuddingBlockEntity(AllBlockEntityTypes.CRYSTAL_BUDDING.get(), pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
			BlockEntityType<T> type) {
		return level.isClientSide ? null
			: (lvl, pos, st, be) -> CrystalBuddingBlockEntity.tick(lvl, pos, st,
				(CrystalBuddingBlockEntity) be);
	}
}
