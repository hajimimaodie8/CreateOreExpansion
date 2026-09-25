package com.hjmmd_8.createoreexpansion.content.grinding.effect;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * 角磨轮特殊效果抽象基类（面向对象：抽象 + 多态）。
 *
 * <p>每个效果类封装自己的状态与行为（封装），继承本类覆写对应方法（继承），
 * 注册表按物品 id 持有 {@link GrindingWheelEffect} 引用、运行时多态调用（多态）。</p>
 *
 * <p><b>自定义扩展</b>：</p>
 * <ul>
 *     <li>复用内置效果：{@code new BonusEffect(0.5f)}、{@code new SpeedEffect(0.8f)}、
 *         {@code new QuantityEffect(2)}、{@code new DoubleEffect(0.25f)} —— 数值即参数，
 *         描述文案自动用通用参数化翻译 key 填充（改数值无需动翻译）；</li>
 *     <li>全新效果：继承本类，覆写 {@link #getTimeMultiplier()}（调耗时）、
 *         {@link #onProcessCompleted}（追加/提升产物）、{@link #getDescription()}（描述文案）；</li>
 *     <li>注册：{@link GrindingWheelEffects#register} 按轮子物品 id 挂接。</li>
 * </ul>
 */
public abstract class GrindingWheelEffect {

	/** 无效果（基准行为） */
	public static final GrindingWheelEffect NONE = new GrindingWheelEffect() {
	};

	/** 加工耗时倍率（1 = 标准；小于 1 表示更快） */
	public float getTimeMultiplier() {
		return 1f;
	}

	/** 加工完成、结果已产出后调用：可追加额外产物或提升数量 */
	public void onProcessCompleted(ItemStack input, List<ItemStack> results, RandomSource random) {
	}

	/** 效果描述（用于物品 tooltip 与护目镜提示）；无效果返回空 */
	public Component getDescription() {
		return Component.empty();
	}
}
