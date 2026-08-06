package com.hjmmd_8.createoreexpansion.content.transmuting.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public final class TransmutationFluid {

	private TransmutationFluid() {
	}

	public static class Source extends BaseFlowingFluid.Source {

		public Source(Properties properties) {
			super(properties);
		}

		@Override
		public int getTickDelay(LevelReader level) {
			return level.dimensionType().ultraWarm() ? 10 : 30;
		}

		@Override
		public int getDropOff(LevelReader level) {
			return 2;
		}

		@Override
		public int getSlopeFindDistance(LevelReader level) {
			return 3;
		}

		@Override
		public int getSpreadDelay(Level level, BlockPos pos, FluidState state, FluidState newState) {
			int delay = this.getTickDelay(level);
			if (!state.isEmpty() && !newState.isEmpty() && !state.getValue(FlowingFluid.FALLING)
				&& !newState.getValue(FlowingFluid.FALLING)
				&& newState.getHeight(level, pos) > state.getHeight(level, pos)
				&& level.getRandom().nextInt(4) == 0) {
				delay *= 4;
			}
			return delay;
		}

		@Override
		protected float getExplosionResistance() {
			return 100.0F;
		}

	}

	public static class Flowing extends BaseFlowingFluid.Flowing {

		public Flowing(Properties properties) {
			super(properties);
		}

		@Override
		public int getTickDelay(LevelReader level) {
			return level.dimensionType().ultraWarm() ? 10 : 30;
		}

		@Override
		public int getDropOff(LevelReader level) {
			return 2;
		}

		@Override
		public int getSlopeFindDistance(LevelReader level) {
			return 3;
		}

		@Override
		public int getSpreadDelay(Level level, BlockPos pos, FluidState state, FluidState newState) {
			int delay = this.getTickDelay(level);
			if (!state.isEmpty() && !newState.isEmpty() && !state.getValue(FlowingFluid.FALLING)
				&& !newState.getValue(FlowingFluid.FALLING)
				&& newState.getHeight(level, pos) > state.getHeight(level, pos)
				&& level.getRandom().nextInt(4) == 0) {
				delay *= 4;
			}
			return delay;
		}

		@Override
		protected float getExplosionResistance() {
			return 100.0F;
		}

	}

}