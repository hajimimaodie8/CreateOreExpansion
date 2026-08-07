package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.common.AllItems;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillCostProxy;
import com.simibubi.create.content.equipment.goggles.GogglesItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ToolEnergy {
	private ToolEnergy() {}

	public static int getMaxEnergy(ItemStack stack) {
		Integer stored = stack.get(AllDataComponents.MAX_ENERGY);
		if (stored != null)
			return stored;
		
		Item item = stack.getItem();
		int maxEnergy = getMaxEnergyByItemType(item);
		
		if (maxEnergy > 0) {
			stack.set(AllDataComponents.MAX_ENERGY, maxEnergy);
			stack.set(AllDataComponents.ENERGY, maxEnergy);
		}
		
		return maxEnergy;
	}

	private static int getMaxEnergyByItemType(Item item) {
		if (item == AllItems.JADE_SWORD.get() || item == AllItems.JADE_PICKAXE.get()
			|| item == AllItems.JADE_AXE.get() || item == AllItems.JADE_SHOVEL.get()) {
			return ToolEnergyConfig.JADE_MAX_ENERGY;
		}
		if (item == AllItems.TOPAZ_SWORD.get() || item == AllItems.TOPAZ_PICKAXE.get()
			|| item == AllItems.TOPAZ_AXE.get() || item == AllItems.TOPAZ_SHOVEL.get()) {
			return ToolEnergyConfig.TOPAZ_MAX_ENERGY;
		}
		if (item == AllItems.SAPPHIRE_SWORD.get() || item == AllItems.SAPPHIRE_PICKAXE.get()
			|| item == AllItems.SAPPHIRE_AXE.get() || item == AllItems.SAPPHIRE_SHOVEL.get()) {
			return ToolEnergyConfig.SAPPHIRE_MAX_ENERGY;
		}
		if (item == AllItems.JADE_TOPAZ_BOW.get()) {
			return ToolEnergyConfig.JADE_TOPAZ_BOW_MAX_ENERGY;
		}
		
		return 0;
	}

	public static boolean hasEnergy(ItemStack stack) {
		return getMaxEnergy(stack) > 0;
	}

	public static int getEnergy(ItemStack stack) {
		Integer energy = stack.get(AllDataComponents.ENERGY);
		if (energy != null)
			return energy;
		
		int max = getMaxEnergy(stack);
		return max > 0 ? max : 0;
	}

	public static void setEnergy(ItemStack stack, int energy) {
		int max = getMaxEnergy(stack);
		if (max <= 0)
			return;
		int value = Math.max(0, Math.min(max, energy));
		stack.set(AllDataComponents.ENERGY, value);
	}

	private static boolean hasEnoughEnergy(ItemStack stack, int cost) {
		if (!hasEnergy(stack))
			return false;
		int current = getEnergy(stack);
		int max = getMaxEnergy(stack);
		return current >= cost && current >= max * ToolEnergyConfig.SKILL_USAGE_THRESHOLD;
	}

	public static boolean canUseSkill(ItemStack stack, SkillCostProxy proxy) {
		return hasEnoughEnergy(stack, proxy.getCost());
	}

	public static boolean canUseSkill(Player player, ItemStack stack, SkillCostProxy proxy) {
		if (hasEnergy(stack) && !canUseSkill(stack, proxy)) {
			sendLowEnergyWarning(player, stack);
			return false;
		}
		return true;
	}

	public static boolean canUseSkill(ItemStack stack, ItemSkill skill) {
		return hasEnoughEnergy(stack, skill.getCost());
	}

	public static boolean canUseSkill(Player player, ItemStack stack, ItemSkill skill) {
		if (hasEnergy(stack) && !canUseSkill(stack, skill)) {
			sendLowEnergyWarning(player, stack);
			return false;
		}
		return true;
	}

	public static boolean consumeForSkill(Player player, ItemStack stack, SkillCostProxy proxy) {
		if (!canUseSkill(player, stack, proxy)) {
			return false;
		}
		setEnergy(stack, getEnergy(stack) - proxy.getCost());
		sendEnergyActionBar(player, stack);
		return true;
	}

	public static boolean consumeForSkill(Player player, ItemStack stack, ItemSkill skill) {
		if (!canUseSkill(player, stack, skill)) {
			return false;
		}
		setEnergy(stack, getEnergy(stack) - skill.getCost());
		sendEnergyActionBar(player, stack);
		return true;
	}

	private static void sendLowEnergyWarning(Player player, ItemStack stack) {
		if (!GogglesItem.isWearingGoggles(player))
			return;
		player.displayClientMessage(
			Component.literal("由于能量值过低无法使用技能")
				.withStyle(ChatFormatting.RED), 
			true
		);
	}

	public static void sendEnergyActionBar(Player player, ItemStack stack) {
		if (!hasEnergy(stack) || !GogglesItem.isWearingGoggles(player))
			return;
		int energy = getEnergy(stack);
		int max = getMaxEnergy(stack);
		
		MutableComponent message = buildColoredText(stack, "当前能量值：" + energy + "/" + max);
		
		if (energy < max * ToolEnergyConfig.SKILL_USAGE_THRESHOLD) {
			message.append(Component.literal(" ")
				.append(Component.literal("由于能量值过低无法使用技能")
					.withStyle(ChatFormatting.RED)));
		}
		
		player.displayClientMessage(message, true);
	}

	private static MutableComponent buildColoredText(ItemStack stack, String text) {
		MutableComponent component = Component.empty();
		if (stack.getItem() instanceof JadeTopazBowItem) {
			for (int i = 0; i < text.length(); i++) {
				float t = text.length() <= 1 ? 0 : i / (float) (text.length() - 1);
				component.append(Component.literal(String.valueOf(text.charAt(i)))
					.withStyle(Style.EMPTY.withColor(lerp(
						ToolEnergyConfig.BOW_COLOR_START, 
						ToolEnergyConfig.BOW_COLOR_END, 
						t))));
			}
		} else {
			component.append(Component.literal(text).withStyle(Style.EMPTY.withColor(getToolColor(stack))));
		}
		return component;
	}

	public static int getToolColor(ItemStack stack) {
		if (stack.is(AllItems.JADE_SWORD.get()) || stack.is(AllItems.JADE_PICKAXE.get())
			|| stack.is(AllItems.JADE_AXE.get()) || stack.is(AllItems.JADE_SHOVEL.get()))
			return ToolEnergyConfig.JADE_COLOR_BRIGHT;
		if (stack.is(AllItems.TOPAZ_SWORD.get()) || stack.is(AllItems.TOPAZ_PICKAXE.get())
			|| stack.is(AllItems.TOPAZ_AXE.get()) || stack.is(AllItems.TOPAZ_SHOVEL.get()))
			return ToolEnergyConfig.TOPAZ_COLOR_BRIGHT;
		if (stack.is(AllItems.SAPPHIRE_SWORD.get()) || stack.is(AllItems.SAPPHIRE_PICKAXE.get())
			|| stack.is(AllItems.SAPPHIRE_AXE.get()) || stack.is(AllItems.SAPPHIRE_SHOVEL.get()))
			return ToolEnergyConfig.SAPPHIRE_COLOR_BRIGHT;
		return ToolEnergyConfig.LOW_ENERGY_COLOR;
	}

	public static int getDarkColor(ItemStack stack) {
		if (stack.is(AllItems.JADE_SWORD.get()) || stack.is(AllItems.JADE_PICKAXE.get())
			|| stack.is(AllItems.JADE_AXE.get()) || stack.is(AllItems.JADE_SHOVEL.get()))
			return ToolEnergyConfig.JADE_COLOR_DARK;
		if (stack.is(AllItems.TOPAZ_SWORD.get()) || stack.is(AllItems.TOPAZ_PICKAXE.get())
			|| stack.is(AllItems.TOPAZ_AXE.get()) || stack.is(AllItems.TOPAZ_SHOVEL.get()))
			return ToolEnergyConfig.TOPAZ_COLOR_DARK;
		if (stack.is(AllItems.SAPPHIRE_SWORD.get()) || stack.is(AllItems.SAPPHIRE_PICKAXE.get())
			|| stack.is(AllItems.SAPPHIRE_AXE.get()) || stack.is(AllItems.SAPPHIRE_SHOVEL.get()))
			return ToolEnergyConfig.SAPPHIRE_COLOR_DARK;
		return ToolEnergyConfig.LOW_ENERGY_DARK;
	}

	public static int lerp(int from, int to, float t) {
		int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return (0xFF << 24) | (r << 16) | (g << 8) | b;
	}
}