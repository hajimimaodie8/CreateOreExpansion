package com.hjmmd_8.createoreexpansion.content.grinding.effect;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * 每种产物数量 +n 效果：加工完成后对结果列表每个物品数量增加 n（不超过堆叠上限）。
 *
 * <p>描述文案用通用参数化翻译 key（{@code createoreexpansion.wheel_effect.quantity}，
 * 模板含 {@code %s} 数量占位符），改数量只改构造参数、无需动翻译。</p>
 */
public class QuantityEffect extends GrindingWheelEffect {

	/** 每种产物增加的数量 */
	private final int amount;

	public QuantityEffect(int amount) {
		this.amount = amount;
	}

	@Override
	public void onProcessCompleted(ItemStack input, List<ItemStack> results, RandomSource random) {
		for (ItemStack stack : results) {
			stack.grow(amount);
			if (stack.getCount() > stack.getMaxStackSize())
				stack.setCount(stack.getMaxStackSize());
		}
	}

	@Override
	public Component getDescription() {
		return Component.translatable("createoreexpansion.wheel_effect.quantity", amount);
	}
}
