package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * 工具附魔领域：读取工具上的模组附魔等级，并提供附魔效果计算。
 *
 * <p>目前管理两个附魔：</p>
 * <ul>
 *     <li>{@link #REDUCE_CONSUMPTION} 减耗——技能能量消耗折扣（1~5 级 90%~50%，超过 5 级按 50%）；</li>
 *     <li>{@link #SWIFT_START} 迅启——技能冷却缩减（1~4 级 90%~50%，超过 4 级按 50%）。</li>
 * </ul>
 */
public final class ToolEnchantments {

	/** 减耗附魔的注册键（data-driven，见 data/createoreexpansion/enchantment/reduce_consumption.json） */
	public static final ResourceKey<Enchantment> REDUCE_CONSUMPTION = ResourceKey.create(
		Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("createoreexpansion", "reduce_consumption"));

	/** 迅启附魔的注册键（data-driven，见 data/createoreexpansion/enchantment/swift_start.json） */
	public static final ResourceKey<Enchantment> SWIFT_START = ResourceKey.create(
		Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("createoreexpansion", "swift_start"));

	/** 技能提升附魔的注册键（data-driven，见 data/createoreexpansion/enchantment/skill_boost.json） */
	public static final ResourceKey<Enchantment> SKILL_BOOST = ResourceKey.create(
		Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("createoreexpansion", "skill_boost"));

	/** 技艺回溯（诅咒）附魔的注册键（data-driven，见 data/createoreexpansion/enchantment/skill_regression.json） */
	public static final ResourceKey<Enchantment> SKILL_REGRESSION = ResourceKey.create(
		Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("createoreexpansion", "skill_regression"));

	private ToolEnchantments() {}

	/** 工具上减耗附魔的等级（0 = 未附魔） */
	public static int reduceConsumptionLevel(ItemStack stack) {
		return level(stack, REDUCE_CONSUMPTION);
	}

	/** 工具上迅启附魔的等级（0 = 未附魔） */
	public static int swiftStartLevel(ItemStack stack) {
		return level(stack, SWIFT_START);
	}

	/** 工具上技能提升附魔的等级（0/1/2，技能等级提升量） */
	public static int skillBoostLevel(ItemStack stack) {
		return level(stack, SKILL_BOOST);
	}

	/** 工具上技艺回溯（诅咒）附魔的等级（0/1/2，技能等级削减量） */
	public static int skillRegressionLevel(ItemStack stack) {
		return level(stack, SKILL_REGRESSION);
	}

	/**
	 * 按迅启附魔等级缩减技能冷却时长。
	 *
	 * <p>1 级 90%、2 级 80%、3 级 70%、4 级及以上 50%（向上取整，至少 1 tick）。</p>
	 *
	 * @param stack 手持的工具
	 * @param ticks 原始冷却时长（tick）
	 * @return 缩减后的冷却时长
	 */
	public static int reduceCooldown(ItemStack stack, int ticks) {
		if (ticks <= 0) {
			return ticks;
		}
		int level = swiftStartLevel(stack);
		if (level <= 0) {
			return ticks;
		}
		double multiplier = level >= 4 ? 0.5 : 1.0 - 0.1 * level;
		return Math.max(1, (int) Math.ceil(ticks * multiplier));
	}

	/** 通用：读取工具上指定附魔的等级（工具自身的 ENCHANTMENTS 组件） */
	private static int level(ItemStack stack, ResourceKey<Enchantment> key) {
		ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		if (enchantments.isEmpty()) {
			return 0;
		}
		for (Holder<Enchantment> holder : enchantments.keySet()) {
			if (holder.is(key)) {
				return enchantments.getLevel(holder);
			}
		}
		return 0;
	}
}
