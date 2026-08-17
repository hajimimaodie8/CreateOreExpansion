package com.hjmmd_8.createoreexpansion.content.lightning;

import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public class LightningBlockRecipe extends StandardProcessingRecipe<SingleRecipeInput> {

    public LightningBlockRecipe(ProcessingRecipeParams params) {
        super(AllRecipeTypes.LIGHTNING_BLOCK, params);
    }

    @Override
    public boolean matches(SingleRecipeInput inv, Level worldIn) {
        if (inv.isEmpty())
            return false;
        return ingredients.get(0).test(inv.getItem(0));
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
