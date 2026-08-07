package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class ToolEnergyTooltip implements com.simibubi.create.foundation.item.TooltipModifier {

	@Override
	public void modify(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		if (!ToolEnergy.hasEnergy(stack))
			return;

		int energy = ToolEnergy.getEnergy(stack);
		int max = ToolEnergy.getMaxEnergy(stack);
		int filled = (int) Math.ceil(energy / (double) max * ToolEnergyConfig.TOOLTIP_BAR_SLOTS);
		if (filled > ToolEnergyConfig.TOOLTIP_BAR_SLOTS)
			filled = ToolEnergyConfig.TOOLTIP_BAR_SLOTS;

		event.getToolTip().add(Component.empty());
		event.getToolTip().add(Component.translatable("item.createoreexpansion.tool.energy")
			.append(":")
			.withStyle(ChatFormatting.GRAY));
		event.getToolTip().add(buildBar(stack, energy, max, filled));
	}

	private static Component buildBar(ItemStack stack, int energy, int max, int filled) {
		MutableComponent bar = Component.empty();
		boolean isLowEnergy = energy < max * ToolEnergyConfig.SKILL_USAGE_THRESHOLD;

		if (stack.getItem() instanceof JadeTopazBowItem) {
			// 翠玉弓：黄→绿渐变
			if (isLowEnergy) {
				bar.append(Component.literal(String.valueOf(ToolEnergyConfig.TOOLTIP_BAR_CHAR).repeat(filled))
					.withStyle(ChatFormatting.WHITE));
			} else {
				for (int i = 0; i < filled; i++) {
					float t = filled == 1 ? 0 : i / (float) (filled - 1);
					bar.append(Component.literal(String.valueOf(ToolEnergyConfig.TOOLTIP_BAR_CHAR))
						.withStyle(Style.EMPTY.withColor(ToolEnergy.lerp(
							ToolEnergyConfig.BOW_COLOR_START,
							ToolEnergyConfig.BOW_COLOR_END,
							t))));
				}
			}
			bar.append(Component.literal(String.valueOf(ToolEnergyConfig.TOOLTIP_BAR_CHAR).repeat(ToolEnergyConfig.TOOLTIP_BAR_SLOTS - filled))
				.withStyle(ChatFormatting.GRAY));
			return bar;
		}

		// 普通工具
		int bright = ToolEnergy.getToolColor(stack);
		int dark = ToolEnergy.getDarkColor(stack);
		
		if (isLowEnergy) {
			// 能量<20%：全部灰白
			bar.append(Component.literal(String.valueOf(ToolEnergyConfig.TOOLTIP_BAR_CHAR).repeat(filled))
				.withStyle(ChatFormatting.WHITE));
		} else {
			// 能量>=20%：亮色
			bar.append(Component.literal(String.valueOf(ToolEnergyConfig.TOOLTIP_BAR_CHAR).repeat(filled))
				.withStyle(Style.EMPTY.withColor(bright)));
		}
		// 剩余段：暗色
		bar.append(Component.literal(String.valueOf(ToolEnergyConfig.TOOLTIP_BAR_CHAR).repeat(ToolEnergyConfig.TOOLTIP_BAR_SLOTS - filled))
			.withStyle(Style.EMPTY.withColor(isLowEnergy ? ToolEnergyConfig.LOW_ENERGY_DARK : dark)));
		
		return bar;
	}
}
