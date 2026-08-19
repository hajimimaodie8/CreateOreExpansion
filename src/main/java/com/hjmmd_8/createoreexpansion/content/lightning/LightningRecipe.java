package com.hjmmd_8.createoreexpansion.content.lightning;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

/**
 * 闪电加工（物品）配方。输入为多槽 LightningInput：
 * 单输入配方（1 种材料）与多输入配方（多种材料组合，如雷鸣合金碎片）共用此匹配逻辑。
 */
public class LightningRecipe extends StandardProcessingRecipe<LightningInput> {

    public LightningRecipe(ProcessingRecipeParams params) {
        super(AllRecipeTypes.LIGHTNING, params);
    }

    @Override
    public boolean matches(LightningInput inv, Level worldIn) {
        if (inv.isEmpty())
            return false;
        // 贪心匹配：每个 ingredient 需在输入池中找到可消耗物品（模拟消耗，不修改原输入）
        List<ItemStack> pool = new ArrayList<>();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty())
                pool.add(stack.copy());
        }
        for (Ingredient ingredient : ingredients) {
            boolean found = false;
            for (ItemStack stack : pool) {
                if (ingredient.test(stack)) {
                    stack.shrink(1);
                    found = true;
                    break;
                }
            }
            if (!found)
                return false;
        }
        return true;
    }

    @Override
    protected int getMaxInputCount() {
        return 9;
    }

    @Override
    protected int getMaxOutputCount() {
        return 12;
    }

}
