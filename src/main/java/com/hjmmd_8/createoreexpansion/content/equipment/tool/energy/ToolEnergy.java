package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillCostProxy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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

	public static boolean isFailure(ItemStack stack) {
		return !(hasEnergy(stack) && getEnergy(stack) > getMaxEnergy(stack) / 5);
	}

	public static boolean canUseSkill(ItemStack stack, SkillCostProxy proxy) {
		return hasEnergy(stack) && getEnergy(stack) > proxy.getCost();
	}

	public static boolean canUseSkill(Player player, ItemStack stack, SkillCostProxy proxy) {
		if (ToolEnergy.hasEnergy(stack) && !ToolEnergy.canUseSkill(stack, proxy)) {
			ToolEnergy.sendLowEnergy(player);
			return false;
		}
		return true;
	}

	public static boolean canUseSkill(ItemStack stack, ItemSkill skill) {
		return hasEnergy(stack) && getEnergy(stack) > skill.getCost();
	}

	public static boolean canUseSkill(Player player, ItemStack stack, ItemSkill skill) {
		if (ToolEnergy.hasEnergy(stack) && !ToolEnergy.canUseSkill(stack, skill)) {
			ToolEnergy.sendLowEnergy(player);
			return false;
		}
		return true;
	}

	public static boolean consumeForSkill(Player player, ItemStack stack, SkillCostProxy proxy) {
		if (!canUseSkill(player, stack, proxy)) {
			return false;
		}
		setEnergy(stack, getEnergy(stack) - proxy.getCost());
		return true;
	}

	public static boolean consumeForSkill(Player player, ItemStack stack, ItemSkill skill) {
		if (!canUseSkill(player, stack, skill)) {
			return false;
		}
		setEnergy(stack, getEnergy(stack) - skill.getCost());
		return true;
	}

	public static void sendLowEnergy(Player player) {
		player.displayClientMessage(Component.literal("由于能量值过低，无法释放技能！").withStyle(ChatFormatting.WHITE), true);
	}

	public static void sendRemainingEnergy(Player player, ItemStack stack) {
		int energy = getEnergy(stack);
		int max = getMaxEnergy(stack);
		if (energy <= 0 || max <= 0)
			return;
		player.displayClientMessage(Component.literal("剩余能量：" + energy + " / " + max)
				.withStyle(ChatFormatting.WHITE), true);
	}
}