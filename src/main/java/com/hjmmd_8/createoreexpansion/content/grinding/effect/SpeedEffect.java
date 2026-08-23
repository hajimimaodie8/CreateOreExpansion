package com.hjmmd_8.createoreexpansion.content.grinding.effect;

import net.minecraft.network.chat.Component;

/**
 * 加工耗时倍率效果：调整单个物品的加工时间（< 1 更快，> 1 更慢）。
 *
 * <p>描述文案用通用参数化翻译 key（{@code createoreexpansion.wheel_effect.speed}，
 * 模板含 {@code %s} 减少百分比占位符，自动由倍率换算），改倍率只改构造参数、无需动翻译。</p>
 */
public class SpeedEffect extends GrindingWheelEffect {

	/** 耗时倍率（1 = 标准；0.85 = 快 15%） */
	private final float multiplier;

	public SpeedEffect(float multiplier) {
		this.multiplier = multiplier;
	}

	@Override
	public float getTimeMultiplier() {
		return multiplier;
	}

	@Override
	public Component getDescription() {
		return Component.translatable("createoreexpansion.wheel_effect.speed",
			(int) Math.round((1f - multiplier) * 100));
	}
}
