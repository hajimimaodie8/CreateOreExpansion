package com.hjmmd_8.createoreexpansion.content.grinding.effect;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * 概率额外产出一份结果：加工完成后按概率复制随机一个产物追加到结果列表。
 *
 * <p>描述文案用通用参数化翻译 key（{@code createoreexpansion.wheel_effect.bonus}，
 * 模板含 {@code %s} 概率占位符），改概率只改构造参数、无需动翻译。</p>
 */
public class BonusEffect extends GrindingWheelEffect {

	/** 触发概率（0~1） */
	private final float chance;

	public BonusEffect(float chance) {
		this.chance = chance;
	}

	@Override
	public void onProcessCompleted(ItemStack input, List<ItemStack> results, RandomSource random) {
		if (results.isEmpty() || random.nextFloat() >= chance)
			return;
		results.add(results.get(random.nextInt(results.size()))
			.copy());
	}

	@Override
	public Component getDescription() {
		return Component.translatable("createoreexpansion.wheel_effect.bonus",
			(int) Math.round(chance * 100));
	}
}
