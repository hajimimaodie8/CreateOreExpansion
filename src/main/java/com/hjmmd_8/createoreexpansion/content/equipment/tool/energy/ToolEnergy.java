package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.awt.Color;

public final class ToolEnergy {
	private ToolEnergy() {}

	/**
	 * 获取物品的最大能量值
	 * @param stack 待处理的物品
	 * @return 最大能量值，物品无最大能量值时返回 -1
	 */
	public static int getMaxEnergy(ItemStack stack) {
		Integer mx = stack.getComponents().get(AllDataComponents.MAX_ENERGY);
		if (mx != null)
			return mx;
		return -1;
	}

	/**
	 * 判断物品是否具有能量值
	 * @param stack 待处理的物品
	 * @return 是否具有能量值
	 */
	public static boolean hasEnergy(ItemStack stack) {
		int max = getMaxEnergy(stack);
		return max != -1;
	}

	/**
	 * 获取物品的能量值
	 * @param stack 待处理的物品
	 * @return 能量值，物品无能量值时返回 -1
	 */
	public static int getEnergy(ItemStack stack) {
		Integer energy = stack.getComponents().get(AllDataComponents.ENERGY);
		return energy != null ? energy : -1;
	}

	public static void setEnergy(ItemStack stack, int energy) {
		int max = getMaxEnergy(stack);
		if (max <= 0)
			return;
		int value = Math.max(0, Math.min(max, energy));
		stack.set(AllDataComponents.ENERGY, value);
	}

	public static boolean canUseSkill(ItemStack stack, ItemSkill skill) {
		int cost = skill.getCost();
		// cost为0时不需要能量组件
		if (cost == 0) return true;
		// cost>0时需要能量组件和足够能量
		return hasEnergy(stack) && getEnergy(stack) >= cost;
	}

	public static boolean canUseSkill(ItemStack stack, DataSkill data) {
		int cost = data.cost;
		// cost为0时不需要能量组件
		if (cost == 0) return true;
		// cost>0时需要能量组件和足够能量
		return hasEnergy(stack) && getEnergy(stack) >= cost;
	}

	public static boolean consumeForSkill(ItemStack stack, ItemSkill skill) {
		int cost = skill.getCost();
		// cost为0时不需要消耗
		if (cost == 0) return true;

		if (!canUseSkill(stack, skill)) {
			return false;
		}
		setEnergy(stack, getEnergy(stack) - cost);
		return true;
	}

	public static boolean consumeForSkill(ItemStack stack, DataSkill data) {
		int cost = data.cost;
		// cost为0时不需要消耗
		if (cost == 0) return true;

		if (!canUseSkill(stack, data)) {
			return false;
		}
		setEnergy(stack, getEnergy(stack) - cost);
		return true;
	}

	public static void sendLowEnergy(Player player, ItemStack stack) {
		int colorRGB = getEnergyColor(stack);
		player.displayClientMessage(
			Component.literal("由于能量值过低，无法释放技能！")
				.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorRGB))), 
			true);
	}

	public static void sendRemainingEnergy(Player player, ItemStack stack) {
		int energy = getEnergy(stack);
		int max = getMaxEnergy(stack);
		int colorRGB = getEnergyColor(stack);
		player.displayClientMessage(
			Component.literal("剩余能量：" + energy + " / " + max)
				.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorRGB))), 
			true);
	}

	public static void sendEnergyMessage(Player player, ItemStack stack, boolean flag) {
		if (!flag) sendLowEnergy(player, stack);
		else sendRemainingEnergy(player, stack);
	}

	/**
	 * 获取工具能量条颜色（RGB）
	 */
	private static int getEnergyColor(ItemStack stack) {
		Integer colorValue = stack.get(AllDataComponents.ENERGY_COLOR);
		return colorValue != null ? (colorValue & 0xFFFFFF) : 0xFFFFFF;
	}
}
