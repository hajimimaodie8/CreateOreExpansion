package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.common.AllItems;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillCostProxy;
import com.simibubi.create.content.equipment.goggles.GogglesItem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

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

	public static void sendEnergyActionBar(Player player, ItemStack stack) {
		if (!hasEnergy(stack) || !GogglesItem.isWearingGoggles(player))
			return;
		int energy = getEnergy(stack);
		int max = getMaxEnergy(stack);
		player.displayClientMessage(buildColoredText(stack, "当前能量值 " + energy + "/" + max), true);
	}

	private static Component buildColoredText(ItemStack stack, String text) {
		MutableComponent component = Component.empty();
		if (stack.getItem() instanceof JadeTopazBowItem) {
			for (int i = 0; i < text.length(); i++) {
				float t = text.length() <= 1 ? 0 : i / (float) (text.length() - 1);
				component.append(Component.literal(String.valueOf(text.charAt(i)))
					.withStyle(Style.EMPTY.withColor(lerp(0xFFFF00, 0x00FF00, t))));
			}
		} else {
			component.append(Component.literal(text).withStyle(Style.EMPTY.withColor(getToolColor(stack))));
		}
		return component;
	}

	public static int getToolColor(ItemStack stack) {
		if (stack.is(AllItems.TOPAZ_SWORD.get()) || stack.is(AllItems.TOPAZ_PICKAXE.get())
			|| stack.is(AllItems.TOPAZ_AXE.get()) || stack.is(AllItems.TOPAZ_SHOVEL.get()))
			return 0xFFFF55;
		if (stack.is(AllItems.SAPPHIRE_SWORD.get()) || stack.is(AllItems.SAPPHIRE_PICKAXE.get())
			|| stack.is(AllItems.SAPPHIRE_AXE.get()) || stack.is(AllItems.SAPPHIRE_SHOVEL.get()))
			return 0x55AAFF;
		return 0xFFFFFF;
	}

	public static int getDarkColor(ItemStack stack) {
		if (stack.is(AllItems.TOPAZ_SWORD.get()) || stack.is(AllItems.TOPAZ_PICKAXE.get())
			|| stack.is(AllItems.TOPAZ_AXE.get()) || stack.is(AllItems.TOPAZ_SHOVEL.get()))
			return 0x8A6D00;
		if (stack.is(AllItems.SAPPHIRE_SWORD.get()) || stack.is(AllItems.SAPPHIRE_PICKAXE.get())
			|| stack.is(AllItems.SAPPHIRE_AXE.get()) || stack.is(AllItems.SAPPHIRE_SHOVEL.get()))
			return 0x1F4F8A;
		return 0x808080;
	}

	public static int lerp(int from, int to, float t) {
		int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return (0xFF << 24) | (r << 16) | (g << 8) | b;
	}

}