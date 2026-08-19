package com.hjmmd_8.createoreexpansion.content.lightning;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * 闪电加工的多物品输入（RecipeInput）。
 * 既支持单物品（单个掉落物/置物台），也支持多材料组合（多个掉落物/工作盆）。
 */
public record LightningInput(List<ItemStack> items) implements RecipeInput {

    public static LightningInput of(ItemStack single) {
        return new LightningInput(List.of(single));
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }
}
