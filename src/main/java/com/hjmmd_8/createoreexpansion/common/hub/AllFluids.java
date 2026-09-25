package com.hjmmd_8.createoreexpansion.common.hub;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.transmuting.fluid.TransmutationFluidBlock;
import com.hjmmd_8.createoreexpansion.content.transmuting.fluid.TransmutationFluid;
import com.tterrag.registrate.util.entry.FluidEntry;

import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.Tags;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRegistrate;

public class AllFluids {

	public static final FluidEntry<TransmutationFluid.Flowing> TRANSMUTATION_FLUID =
		CoeRegistrate.REGISTRATE.fluid("transmutation_fluid",
				ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, "block/transmutation_fluid_still"),
				ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, "block/transmutation_fluid_flowing"),
				CreateRegistrate::defaultFluidType,
				TransmutationFluid.Flowing::new)
			.lang("Transmutation Fluid")
			.properties(b -> b.viscosity(6000).density(3000).temperature(1300).lightLevel(8))
			.fluidProperties(p -> p.levelDecreasePerBlock(2).tickRate(10).slopeFindDistance(3).explosionResistance(100f))
			.source(TransmutationFluid.Source::new)
			.block(TransmutationFluidBlock::new)
			.build()
			.bucket()
			.tag(Tags.Items.BUCKETS)
			.build()
			.register();

	public static void register() {}
}