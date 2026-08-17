package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.foundation.util.BarTooltipRender;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.awt.*;

public class EnergyTooltipHandler {
    private static final int BAR_SLOTS = 20;

    private static final String ENERGY_TRANSLATE_KEY = "item.createoreexpansion.tool.energy";

    /**
     * 能量条紧跟技能区插入（index 由技能区返回），保证能量条显示在技能下方、其它信息上方。
     *
     * @param startIndex 技能区结束后的下一个可用 index
     * @return 插入完成后的下一个可用 index
     */
    public static int addEnergyTooltip(ItemTooltipEvent event, int startIndex) {
        ItemStack stack = event.getItemStack();

        if (!ToolEnergy.hasEnergy(stack))
            return startIndex;

        int energy = ToolEnergy.getEnergy(stack);
        int max = ToolEnergy.getMaxEnergy(stack);

        int index = startIndex;

        event.getToolTip().add(index++, Component.translatable(ENERGY_TRANSLATE_KEY)
                .append(":")
                .withStyle(ChatFormatting.GRAY));

        // 能量为0时显示暗色，否则显示亮色
        boolean isEmpty = energy == 0;
        Color fillColor = isEmpty
                ? ToolEnergyColorConfig.DEFAULT.light
                : ToolEnergyColorConfig.DEFAULT.dark;

        Integer color = isEmpty
                ? stack.get(AllDataComponents.ENERGY_COLOR_DARK)
                : stack.get(AllDataComponents.ENERGY_COLOR);

        if (color != null) {
            fillColor = new Color(color % 0xFFFFFF);
        }

        event.getToolTip().add(index, BarTooltipRender.energy(energy, max, BAR_SLOTS, fillColor));
        return index + 1;
    }
}
