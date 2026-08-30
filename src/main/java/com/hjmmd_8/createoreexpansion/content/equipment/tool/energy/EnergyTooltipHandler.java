package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.foundation.util.BarTooltipRender;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.awt.*;
import java.util.List;

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

        // 翠玉之弓（传说武器）：能量条从左（绿）到右（黄）渐变，其余工具保持单色
        if (stack.getItem() instanceof com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem) {
            event.getToolTip().add(index, BarTooltipRender.energyGradient(
                    energy, max, BAR_SLOTS, new Color(0x55FF55), new Color(0xFFFF55)));
            return index + 1;
        }

        event.getToolTip().add(index, BarTooltipRender.energy(energy, max, BAR_SLOTS, fillColor));
        return index + 1;
    }

    /**
     * 绑定信息行：位于能量区之后（能量标题行、能量条、再下一行）。
     * 工具显示绑定的凝能佩（单枚）；凝能佩显示绑定的全部工具（一枚佩可绑多工具）。
     * 颜色取 BIND_COLOR（默认翡翠绿）。
     */
    public static void addBoundTooltip(ItemTooltipEvent event, int index) {
        ItemStack stack = event.getItemStack();

        ResourceLocation boundMedallion = stack.get(AllDataComponents.BOUND_MEDALLION);
        if (boundMedallion != null) {
            // 工具侧：绑定单枚佩
            Item item = BuiltInRegistries.ITEM.get(boundMedallion);
            if (item == null || item == Items.AIR)
                return;
            int color = item.getDefaultInstance().getOrDefault(AllDataComponents.BIND_COLOR, 0x55FF55);
            event.getToolTip().add(index, Component.literal("绑定：")
                    .withStyle(style -> style.withColor(color))
                    .append(Component.translatable(item.getDescriptionId())
                            .withStyle(style -> style.withColor(color))));
            return;
        }

        List<ResourceLocation> boundTools = stack.get(AllDataComponents.BOUND_TOOL);
        if (boundTools == null || boundTools.isEmpty())
            return;
        // 佩侧：绑定多个工具；「绑定：」用佩的 BIND_COLOR，工具名用各工具自身的能量色
        int bindColor = stack.getOrDefault(AllDataComponents.BIND_COLOR, 0x55FF55);
        MutableComponent line = Component.literal("绑定：").withStyle(style -> style.withColor(bindColor));
        boolean first = true;
        for (ResourceLocation id : boundTools) {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item == null || item == Items.AIR)
                continue;
            if (!first)
                line.append("、");
            int toolColor = item.getDefaultInstance().getOrDefault(AllDataComponents.ENERGY_COLOR, bindColor);
            line.append(Component.translatable(item.getDescriptionId()).withStyle(style -> style.withColor(toolColor)));
            first = false;
        }
        event.getToolTip().add(index, line);
    }
}
