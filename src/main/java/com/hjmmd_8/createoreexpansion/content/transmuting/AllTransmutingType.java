package com.hjmmd_8.createoreexpansion.content.transmuting;

import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.hjmmd_8.createoreexpansion.common.AllTags;
import java.util.List;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import com.hjmmd_8.createoreexpansion.common.AllModEffects;

import net.createmod.catnip.theme.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class AllTransmutingType implements FanProcessingType {

	@Override
	public boolean isValidAt(Level level, BlockPos pos) {
		FluidState fluidState = level.getFluidState(pos);
		return AllTags.AllFluidTags.FAN_PROCESSING_CATALYSTS_TRANSMUTING.matches(fluidState);
	}

	@Override
	public int getPriority() {
		return 500;
	}

	@Override
	public boolean canProcess(ItemStack stack, Level level) {
		return AllRecipeTypes.TRANSMUTING.find(new SingleRecipeInput(stack), level)
			.isPresent();
	}

	@Override
	@Nullable
	public List<ItemStack> process(ItemStack stack, Level level) {
		return AllRecipeTypes.TRANSMUTING.find(new SingleRecipeInput(stack), level)
			.map(RecipeHolder::value)
			.map(r -> RecipeApplier.applyRecipeOn(level, stack, r, true))
			.orElse(null);
	}

	@Override
	public void spawnProcessingParticles(Level level, Vec3 pos) {
		if (level.random.nextInt(8) != 0)
			return;
		Vector3f color = new Color(0xFF69B4).asVectorF();
		level.addParticle(new DustParticleOptions(color, 1), pos.x + (level.random.nextFloat() - .5f) * .5f,
			pos.y + .5f, pos.z + (level.random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
		level.addParticle(new DustParticleOptions(color, 1.5f), pos.x + (level.random.nextFloat() - .5f) * .5f, pos.y + .5f,
			pos.z + (level.random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
		level.addParticle(ParticleTypes.END_ROD, pos.x + (level.random.nextFloat() - .5f) * .5f, pos.y + .5f,
			pos.z + (level.random.nextFloat() - .5f) * .5f, 0, 1 / 8f, 0);
	}

	@Override
	public void morphAirFlow(AirFlowParticleAccess particleAccess, RandomSource random) {
		particleAccess.setColor(Color.mixColors(0xFF69B4, 0xFFB6C1, random.nextFloat()));
		particleAccess.setAlpha(1f);
		if (random.nextFloat() < 1 / 32f)
			particleAccess.spawnExtraParticle(new DustParticleOptions(new Color(0xFF69B4).asVectorF(), 1.5f), .125f);
		if (random.nextFloat() < 1 / 16f)
			particleAccess.spawnExtraParticle(new DustParticleOptions(new Color(0xFFB6C1).asVectorF(), 1f), .125f);
		if (random.nextFloat() < 1 / 32f)
			particleAccess.spawnExtraParticle(ParticleTypes.END_ROD, .125f);
	}

	@Override
	public void affectEntity(Entity entity, Level level) {
		if (level.isClientSide)
			return;
		if (entity instanceof Player player && !player.isSpectator())
			player.addEffect(new MobEffectInstance(AllModEffects.TRANSMUTATION_DISORDER, 60, 0));
	}

}
