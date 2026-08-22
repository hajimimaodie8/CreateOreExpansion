package com.hjmmd_8.createoreexpansion.content.grinding.behaviour;

import java.util.function.Consumer;

import com.simibubi.create.content.processing.recipe.ProcessingInventory;

import net.minecraft.world.item.ItemStack;

/**
 * 角磨床加工库存（仿磨坊输出语义）：
 * 槽 0 为输入区（不可被输出漏斗抽取），槽 1+ 为成品区（随时可抽取）；
 * 加工完成的成品由 insertToOutput 直接存入成品区（绕过 isItemValid 的槽 0 限制），
 * 加工完成后立即重置继续，不阻塞后续输入。
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
		// 成品区（槽 1+）随时可抽取：加工中也能抽走旧成品，新成品由 insertToOutput 继续存入
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

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		// 槽 0 输入区：槽 0 空即可插入（成品区槽 1+ 有成品不影响新输入）；
		// 槽 1+ 成品区：拒绝外部插入（成品由 insertToOutput 用 setStackInSlot 直接存入，防污染输出区）
		return slot == 0 ? getStackInSlot(0)
			.isEmpty() : false;
	}
}
