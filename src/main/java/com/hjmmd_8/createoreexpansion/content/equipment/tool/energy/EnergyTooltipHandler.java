package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.data.COELangProvider;
import com.hjmmd_8.createoreexpansion.foundation.util.BarTooltipRender;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.awt.*;

public class EnergyTooltipHandler {
    private static final int BAR_SLOTS = 20;

    private static final String ENERGY_TRANSLATE_KEY = "item.createoreexpansion.tool.energy";

    public static final COELangProvider.Translatable ENERGY_TRANSLATABLE
            = () -> ENERGY_TRANSLATE_KEY;

    public static void addEnergyTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (!ToolEnergy.hasEnergy(stack))
            return;

        int energy = ToolEnergy.getEnergy(stack);
        int max = ToolEnergy.getMaxEnergy(stack);

        event.getToolTip().add(1, Component.translatable(ENERGY_TRANSLATE_KEY)
                .append(":")
                .withStyle(ChatFormatting.GRAY));

        Color fillColor = ToolEnergy.isFailure(stack)
                ? ToolEnergyColorConfig.DEFAULT.light
                : ToolEnergyColorConfig.DEFAULT.dark;

        Integer color = ToolEnergy.isFailure(stack)
                ? stack.get(AllDataComponents.ENERGY_COLOR_DARK)
                : stack.get(AllDataComponents.ENERGY_COLOR);

        if (color != null) {
            fillColor = new Color(color % 0xFFFFFF);
        }

        event.getToolTip().add(2, BarTooltipRender.energy(energy, max, BAR_SLOTS, fillColor));
    }
}
