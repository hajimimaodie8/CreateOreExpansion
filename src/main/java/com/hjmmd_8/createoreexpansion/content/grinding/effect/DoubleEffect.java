package com.hjmmd_8.createoreexpansion.content.grinding.effect;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * 概率全部产物翻倍效果：加工完成后按概率将每种产物各复制一份追加到结果列表。
 *
 * <p>描述文案用通用参数化翻译 key（{@code createoreexpansion.wheel_effect.double}，
 * 模板含 {@code %s} 概率占位符），改概率只改构造参数、无需动翻译。</p>
 */
public class DoubleEffect extends GrindingWheelEffect {

	/** 触发概率（0~1） */
	private final float chance;

	public DoubleEffect(float chance) {
		this.chance = chance;
	}

	@Override
	public void onProcessCompleted(ItemStack input, List<ItemStack> results, RandomSource random) {
		if (results.isEmpty() || random.nextFloat() >= chance)
			return;
		int size = results.size();
		for (int i = 0; i < size; i++)
			results.add(results.get(i)
				.copy());
	}

	@Override
	public Component getDescription() {
		return Component.translatable("createoreexpansion.wheel_effect.double",
			(int) Math.round(chance * 100));
	}
}
