package com.hjmmd_8.createoreexpansion.content.transmuting;

import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.world.level.Level;

import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

public class AllTransmutingRecipe extends StandardProcessingRecipe<RecipeWrapper> {

    public AllTransmutingRecipe(ProcessingRecipeParams params) {
        super(AllRecipeTypes.TRANSMUTING, params);
    }

    @Override
    public boolean matches(RecipeWrapper inv, Level worldIn) {
        if (inv.isEmpty())
            return false;
        return ingredients.get(0)
            .test(inv.getItem(0));
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 12;
    }

}
