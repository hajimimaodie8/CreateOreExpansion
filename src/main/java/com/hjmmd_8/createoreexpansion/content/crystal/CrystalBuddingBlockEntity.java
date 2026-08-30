package com.hjmmd_8.createoreexpansion.content.crystal;

import com.hjmmd_8.createoreexpansion.common.AllBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 水晶芽床生长进度方块实体：确定性生长。
 *
 * <p>每 {@code growIntervalTicks()}（由 AllConfig 配置决定，默认 60 秒）保证尝试一次
 * 生成新芽（不受随机刻运气影响），与芽床的随机刻自然生长互补；生长进度随方块实体存盘。</p>
 */
public class CrystalBuddingBlockEntity extends BlockEntity {

	private int progress;

	public CrystalBuddingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, CrystalBuddingBlockEntity be) {
		if (level.isClientSide)
			return;
		if (state.getBlock() instanceof CrystalBuddingBlock budding) {
			if (++be.progress >= budding.growIntervalTicks()) {
				be.progress = 0;
				budding.tryGrowBud(level, pos, Direction.getRandom(level.getRandom()));
			}
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.putInt("GrowProgress", progress);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		progress = tag.getInt("GrowProgress");
	}
}
