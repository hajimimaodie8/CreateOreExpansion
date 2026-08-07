package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.foundation.util.BarTooltipRender;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.awt.*;

@EventBusSubscriber
public class EnergyTooltipHandler {
    private static final int BAR_SLOTS = 20;

    public static final int JADE_COLOR = 0xFFE5B4;
    public static final int TOPAZ_COLOR = 0xFFE5B4;
    public static final int SAPPHIRE_COLOR = 0xFFE5B4;

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Color fillColor;

        Integer color = stack.get(AllDataComponents.ENERGY_COLOR);
        if (color != null) {
            color %= 0xFFFFFF;
            fillColor = new Color(color);
        }

        if (!ToolEnergy.hasEnergy(stack))
            return;

        int energy = ToolEnergy.getEnergy(stack);
        int max = ToolEnergy.getMaxEnergy(stack);

        event.getToolTip().add(1, Component.translatable("item.createoreexpansion.tool.energy")
                .append(":")
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(2, BarTooltipRender.energy(energy, max, BAR_SLOTS));
    }
}
