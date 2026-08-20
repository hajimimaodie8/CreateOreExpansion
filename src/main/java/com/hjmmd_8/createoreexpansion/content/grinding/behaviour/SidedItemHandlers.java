package com.hjmmd_8.createoreexpansion.content.grinding.behaviour;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 按访问方向限制的侧面物品处理器（配合能力注册使用）：
 * 顶部漏斗只能输入（context=UP）、底面漏斗只能输出（context=DOWN），
 * 左右两侧与无方向访问由机器自身直接暴露库存。
 */
public final class SidedItemHandlers {

	private SidedItemHandlers() {}

	/** 只能输入（顶部漏斗）：抽取一律返回空 */
	public static IItemHandler inputOnly(IItemHandler delegate) {
		return new InputOnlyHandler(delegate);
	}

	/** 只能输出（底面漏斗）：插入一律拒绝 */
	public static IItemHandler outputOnly(IItemHandler delegate) {
		return new OutputOnlyHandler(delegate);
	}

	private static class InputOnlyHandler implements IItemHandler {

		private final IItemHandler delegate;

		InputOnlyHandler(IItemHandler delegate) {
			this.delegate = delegate;
		}

		@Override
		public int getSlots() {
			return delegate.getSlots();
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return delegate.getStackInSlot(slot);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return delegate.insertItem(slot, stack, simulate);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return delegate.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return delegate.isItemValid(slot, stack);
		}
	}

	private static class OutputOnlyHandler implements IItemHandler {

		private final IItemHandler delegate;

		OutputOnlyHandler(IItemHandler delegate) {
			this.delegate = delegate;
		}

		@Override
		public int getSlots() {
			return delegate.getSlots();
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return delegate.getStackInSlot(slot);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return delegate.extractItem(slot, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return delegate.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return false;
		}
	}
}
