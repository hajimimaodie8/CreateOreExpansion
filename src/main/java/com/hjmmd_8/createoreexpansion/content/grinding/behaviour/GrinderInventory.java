package com.hjmmd_8.createoreexpansion.content.grinding.behaviour;

import java.util.function.Consumer;

import com.simibubi.create.content.processing.recipe.ProcessingInventory;

import net.minecraft.world.item.ItemStack;

/**
 * 角磨床加工库存（仿磨坊输出语义）：
 * 槽 0 为输入区（不可被输出漏斗抽取），槽 1+ 为成品区；
 * 仅加工完成后（remainingTime 归零且已出成品）才允许抽取成品。
 */
public class GrinderInventory extends ProcessingInventory {

	public GrinderInventory(Consumer<ItemStack> callback) {
		super(callback);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		// 槽 0 是输入区，不可被输出漏斗抽取
		if (slot == 0)
			return ItemStack.EMPTY;
		// 仅加工完成后才允许抽取成品
		if (remainingTime != 0 || !appliedRecipe)
			return ItemStack.EMPTY;
		// 父类 ProcessingInventory.extractItem 恒返回 EMPTY（Create 禁用抽取），
		// 这里按 ItemStackHandler 语义自行实现
		if (amount == 0)
			return ItemStack.EMPTY;
		validateSlotIndex(slot);
		ItemStack existing = getStackInSlot(slot);
		if (existing.isEmpty())
			return ItemStack.EMPTY;
		int toExtract = Math.min(amount, existing.getMaxStackSize());
		if (existing.getCount() <= toExtract) {
			if (!simulate) {
				setStackInSlot(slot, ItemStack.EMPTY);
				return existing;
			}
			return existing.copy();
		}
		ItemStack copy = existing.copy();
		copy.setCount(toExtract);
		if (!simulate) {
			existing.shrink(toExtract);
			setStackInSlot(slot, existing);
		}
		return copy;
	}
}
