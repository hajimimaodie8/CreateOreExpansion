package com.hjmmd_8.createoreexpansion.content.equipment.tool;

import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.common.AllItems;
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

	public static final String ENERGY_TAG = "energy";
	public static final int BOW_MAX = 100;
	public static final int TOPAZ_MAX = 600;
	public static final int SAPPHIRE_MAX = 1100;
	public static final int SWORD_COST = 100;
	public static final int PICKAXE_COST = 10;
	public static final int SHOVEL_COST = 10;
	public static final int AXE_COST = 100;

	private ToolEnergy() {
	}

	public static int getMaxEnergy(ItemStack stack) {
		if (stack.getItem() instanceof JadeTopazBowItem)
			return BOW_MAX;
		if (stack.is(AllItems.TOPAZ_SWORD.get()) || stack.is(AllItems.TOPAZ_PICKAXE.get())
			|| stack.is(AllItems.TOPAZ_AXE.get()) || stack.is(AllItems.TOPAZ_SHOVEL.get()))
			return TOPAZ_MAX;
		if (stack.is(AllItems.SAPPHIRE_SWORD.get()) || stack.is(AllItems.SAPPHIRE_PICKAXE.get())
			|| stack.is(AllItems.SAPPHIRE_AXE.get()) || stack.is(AllItems.SAPPHIRE_SHOVEL.get()))
			return SAPPHIRE_MAX;
		return 0;
	}

	public static boolean hasEnergy(ItemStack stack) {
		return getMaxEnergy(stack) > 0;
	}

	public static int getEnergy(ItemStack stack) {
		int max = getMaxEnergy(stack);
		if (max <= 0)
			return 0;
		CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		int energy = tag.contains(ENERGY_TAG) ? tag.getInt(ENERGY_TAG) : max;
		return Math.max(0, Math.min(max, energy));
	}

	public static void setEnergy(ItemStack stack, int energy) {
		int max = getMaxEnergy(stack);
		if (max <= 0)
			return;
		int value = Math.max(0, Math.min(max, energy));
		stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
			data -> data.update(tag -> tag.putInt(ENERGY_TAG, value)));
	}

	public static boolean canUseSkill(Player player, ItemStack stack) {
		return hasEnergy(stack) && getEnergy(stack) > getMaxEnergy(stack) / 5;
	}

	public static boolean consumeForSkill(Player player, ItemStack stack, int cost) {
		if (!canUseSkill(player, stack)) {
			sendLowEnergy(player);
			return false;
		}
		setEnergy(stack, getEnergy(stack) - cost);
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