package com.hjmmd_8.createoreexpansion.content.grinding.effect;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * 角磨轮特殊效果（按轮子物品 id 注册；拓展模组可通过
 * {@link GrindingWheelEffects#register} 注册自己的轮子效果）。
 *
 * <p>扩展点：{@link #getTimeMultiplier()} 调整加工耗时、
 * {@link #onProcessCompleted} 在产物产出后追加/提升结果、
 * {@link #getDescription()} 提供效果描述（物品 tooltip / 护目镜提示显示）。</p>
 */
public interface GrindingWheelEffect {

	/** 无效果（基准行为） */
	GrindingWheelEffect NONE = new GrindingWheelEffect() {};

	/** 加工耗时倍率（1 = 标准；小于 1 表示更快） */
	default float getTimeMultiplier() {
		return 1f;
	}

	/** 加工完成、结果已产出后调用：可追加额外产物或提升数量 */
	default void onProcessCompleted(ItemStack input, List<ItemStack> results, RandomSource random) {
	}

	/** 效果描述（用于物品 tooltip 与护目镜提示）；无效果返回空 */
	default Component getDescription() {
		return Component.empty();
	}
}
