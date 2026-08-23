package com.hjmmd_8.createoreexpansion.content.grinding.effect;

import java.util.HashMap;
import java.util.Map;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;

import net.minecraft.resources.ResourceLocation;

/**
 * 角磨轮效果注册表：按轮子物品 id 关联特殊效果。
 *
 * <p>面向对象：注册表持有 {@link GrindingWheelEffect} 抽象引用，运行时多态调用
 * （耗时倍率 / 产物追加 / 描述文案）。内置效果为独立类
 * （{@link BonusEffect}、{@link SpeedEffect}、{@link QuantityEffect}、{@link DoubleEffect}），
 * 均可在构造时配置参数并复用；全新效果继承 {@link GrindingWheelEffect} 即可。</p>
 *
 * <p>开闭原则：内置轮子效果在此集中注册；拓展模组调用 {@link #register} 即可为
 * 任意角磨轮（含第三方物品）挂接自己的效果，无需改动本模组代码。</p>
 *
 * <p>内置效果：铁=基准；金=25% 额外一份；黄铜=耗时 -25%；锌=耗时 -15%；
 * 翡翠=15% 双倍；钻石=耗时 -15%；黄玉=每种产物 +1；蓝宝石=35% 额外一份；
 * 星辉石=耗时 -30%（星辉加速）；下界合金=每种产物 +2。</p>
 */
public final class GrindingWheelEffects {

	private static final Map<ResourceLocation, GrindingWheelEffect> EFFECTS = new HashMap<>();

	static {
		// 一级
		register("iron_grinding_wheel", GrindingWheelEffect.NONE);
		register("gold_grinding_wheel", new BonusEffect(0.25f));
		register("brass_grinding_wheel", new SpeedEffect(0.75f));
		register("zinc_grinding_wheel", new SpeedEffect(0.85f));
		// 二级
		register("jade_grinding_wheel", new DoubleEffect(0.15f));
		register("diamond_grinding_wheel", new SpeedEffect(0.85f));
		register("topaz_grinding_wheel", new QuantityEffect(1));
		// 三级
		register("sapphire_grinding_wheel", new BonusEffect(0.35f));
		register("stellarstone_grinding_wheel", new SpeedEffect(0.7f));
		register("netherite_grinding_wheel", new QuantityEffect(2));
	}

	private GrindingWheelEffects() {
	}

	/** 注册角磨轮效果（拓展模组用本模组命名空间） */
	public static void register(String wheelId, GrindingWheelEffect effect) {
		register(CreateOreExpansion.modLoc(wheelId), effect);
	}

	/** 注册角磨轮效果（任意命名空间） */
	public static void register(ResourceLocation wheelId, GrindingWheelEffect effect) {
		EFFECTS.put(wheelId, effect);
	}

	/** 查询轮子效果；未注册返回无效果 */
	public static GrindingWheelEffect get(ResourceLocation wheelId) {
		return EFFECTS.getOrDefault(wheelId, GrindingWheelEffect.NONE);
	}
}
