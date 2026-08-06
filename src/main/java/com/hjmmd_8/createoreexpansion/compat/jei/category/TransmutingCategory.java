package com.hjmmd_8.createoreexpansion.compat.jei.category;

import com.hjmmd_8.createoreexpansion.common.AllMyFluids;
import com.hjmmd_8.createoreexpansion.content.transmuting.AllTransmutingRecipe;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;

public class TransmutingCategory extends ProcessingViaFanCategory.MultiOutput<AllTransmutingRecipe> {

	public TransmutingCategory(Info<AllTransmutingRecipe> info) {
		super(info);
	}

	@Override
	protected void renderAttachedBlock(GuiGraphics graphics) {
		GuiGameElement.of(AllMyFluids.TRANSMUTATION_FLUID.get().getSource())
			.scale(SCALE)
			.atLocal(0, 0, 2)
			.lighting(AnimatedKinetics.DEFAULT_LIGHTING)
			.render(graphics);
	}

}
