package com.hjmmd_8.createoreexpansion.tool;

import com.hjmmd_8.createoreexpansion.item.JadeTopazBowItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class ToolEnergyTooltip implements com.simibubi.create.foundation.item.TooltipModifier {

	private static final int BAR_SLOTS = 10;

	@Override
	public void modify(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		if (!ToolEnergy.hasEnergy(stack))
			return;

		int energy = ToolEnergy.getEnergy(stack);
		int max = ToolEnergy.getMaxEnergy(stack);
		int filled = (int) Math.ceil(energy / (double) max * BAR_SLOTS);
		if (filled > BAR_SLOTS)
			filled = BAR_SLOTS;

		event.getToolTip().add(Component.empty());
		event.getToolTip().add(Component.translatable("item.createoreexpansion.tool.energy")
			.append(":")
			.withStyle(ChatFormatting.GRAY));
		event.getToolTip().add(buildBar(stack, energy, max, filled));
	}

	private static Component buildBar(ItemStack stack, int energy, int max, int filled) {
		MutableComponent bar = Component.empty();
		boolean low = energy <= max / 5;

		if (stack.getItem() instanceof JadeTopazBowItem) {
			if (low) {
				bar.append(Component.literal("│".repeat(filled)).withStyle(ChatFormatting.WHITE));
			} else {
				for (int i = 0; i < filled; i++) {
					float t = filled == 1 ? 0 : i / (float) (filled - 1);
					bar.append(Component.literal("│")
						.withStyle(Style.EMPTY.withColor(ToolEnergy.lerp(0xFFFF00, 0x00FF00, t))));
				}
			}
			bar.append(Component.literal("│".repeat(BAR_SLOTS - filled)).withStyle(ChatFormatting.GRAY));
			return bar;
		}

		int bright = ToolEnergy.getToolColor(stack);
		int dark = ToolEnergy.getDarkColor(stack);
		if (low) {
			bar.append(Component.literal("│".repeat(filled)).withStyle(ChatFormatting.WHITE));
		} else {
			bar.append(Component.literal("│".repeat(filled)).withStyle(Style.EMPTY.withColor(bright)));
		}
		bar.append(Component.literal("│".repeat(BAR_SLOTS - filled)).withStyle(Style.EMPTY.withColor(dark)));
		return bar;
	}

}