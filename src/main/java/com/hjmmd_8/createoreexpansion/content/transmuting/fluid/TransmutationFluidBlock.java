package com.hjmmd_8.createoreexpansion.content.transmuting.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import org.joml.Vector3f;

public class TransmutationFluidBlock extends LiquidBlock {

	public TransmutationFluidBlock(FlowingFluid fluid, Properties properties) {
		super(fluid, properties);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		super.animateTick(state, level, pos, random);
		if (random.nextInt(4) != 0)
			return;

		double x = pos.getX() + random.nextDouble();
		double y = pos.getY() + 0.9D + random.nextDouble() * 0.3D;
		double z = pos.getZ() + random.nextDouble();
		Vector3f pink = new Vector3f(1.0F, 0.45F, 0.8F);

		level.addParticle(new DustParticleOptions(pink, 1.0F), x, y, z, 0.0D, 0.06D, 0.0D);
		if (random.nextInt(3) == 0)
			level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0D, 0.04D, 0.0D);
	}

}