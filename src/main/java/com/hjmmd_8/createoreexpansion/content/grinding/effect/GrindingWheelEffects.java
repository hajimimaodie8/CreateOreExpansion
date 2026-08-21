package com.hjmmd_8.createoreexpansion.content.grinding.effect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * 角磨轮效果注册表：按轮子物品 id 关联特殊效果。
 *
 * <p>开闭原则：内置轮子效果在此集中注册；拓展模组调用 {@link #register} 即可为
 * 任意角磨轮（含第三方物品）挂接自己的效果，无需改动本模组代码。</p>
 *
 * <p>内置效果：铁=基准；金=25% 额外一份；黄铜=耗时 -25%；锌=每种产物 +1；
 * 翡翠=15% 双倍；钻石=耗时 -15%；黄玉=每种产物 +2；蓝宝石=35% 额外一份；
 * 星辉石=耗时 -30%（星辉加速）；下界合金=每种产物 +3。</p>
 */
public final class GrindingWheelEffects {

	private static final Map<ResourceLocation, GrindingWheelEffect> EFFECTS = new HashMap<>();

	static {
		// 一级
		register("iron_grinding_wheel", GrindingWheelEffect.NONE);
		register("gold_grinding_wheel", bonusEffect(0.25f, "createoreexpansion.wheel_effect.gold_grinding_wheel"));
		register("brass_grinding_wheel", speedEffect(0.75f, "createoreexpansion.wheel_effect.brass_grinding_wheel"));
		register("zinc_grinding_wheel", quantityEffect(1, "createoreexpansion.wheel_effect.zinc_grinding_wheel"));
		// 二级
		register("jade_grinding_wheel", doubleEffect(0.15f, "createoreexpansion.wheel_effect.jade_grinding_wheel"));
		register("diamond_grinding_wheel", speedEffect(0.85f, "createoreexpansion.wheel_effect.diamond_grinding_wheel"));
		register("topaz_grinding_wheel", quantityEffect(2, "createoreexpansion.wheel_effect.topaz_grinding_wheel"));
		// 三级
		register("sapphire_grinding_wheel", bonusEffect(0.35f, "createoreexpansion.wheel_effect.sapphire_grinding_wheel"));
		register("stellarstone_grinding_wheel", speedEffect(0.7f, "createoreexpansion.wheel_effect.stellarstone_grinding_wheel"));
		register("netherite_grinding_wheel", quantityEffect(3, "createoreexpansion.wheel_effect.netherite_grinding_wheel"));
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

	// ===== 内置效果 =====

	/** 概率额外产出一份结果（复制随机一个产物） */
	private static GrindingWheelEffect bonusEffect(float chance, String descKey) {
		return new GrindingWheelEffect() {
			@Override
			public void onProcessCompleted(ItemStack input, List<ItemStack> results, RandomSource random) {
				if (results.isEmpty() || random.nextFloat() >= chance)
					return;
				results.add(results.get(random.nextInt(results.size()))
					.copy());
			}

			@Override
			public Component getDescription() {
				return Component.translatable(descKey);
			}
		};
	}

	/** 加工耗时倍率 */
	private static GrindingWheelEffect speedEffect(float multiplier, String descKey) {
		return new GrindingWheelEffect() {
			@Override
			public float getTimeMultiplier() {
				return multiplier;
			}

			@Override
			public Component getDescription() {
				return Component.translatable(descKey);
			}
		};
	}

	/** 每种产物数量 +n（不超过堆叠上限） */
	private static GrindingWheelEffect quantityEffect(int n, String descKey) {
		return new GrindingWheelEffect() {
			@Override
			public void onProcessCompleted(ItemStack input, List<ItemStack> results, RandomSource random) {
				for (ItemStack stack : results) {
					stack.grow(n);
					if (stack.getCount() > stack.getMaxStackSize())
						stack.setCount(stack.getMaxStackSize());
				}
			}

			@Override
			public Component getDescription() {
				return Component.translatable(descKey);
			}
		};
	}

	/** 概率全部产物翻倍（每种复制一份） */
	private static GrindingWheelEffect doubleEffect(float chance, String descKey) {
		return new GrindingWheelEffect() {
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
				return Component.translatable(descKey);
			}
		};
	}
}
