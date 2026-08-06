package com.hjmmd_8.createoreexpansion.tool;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ToolSkillCooldown {

	private ToolSkillCooldown() {
	}

	public static boolean isReady(Player player, ItemStack stack) {
		return !stack.isEmpty() && !player.getCooldowns().isOnCooldown(stack.getItem());
	}

	public static void start(Player player, ItemStack stack, int seconds) {
		if (!stack.isEmpty())
			player.getCooldowns().addCooldown(stack.getItem(), seconds * 20);
	}

}