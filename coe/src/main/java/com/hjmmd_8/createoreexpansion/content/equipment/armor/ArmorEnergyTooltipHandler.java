package com.hjmmd_8.createoreexpansion.content.equipment.armor;

import java.awt.Color;
import java.util.List;

import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergyColorConfig;
import com.hjmmd_8.createoreexpansion.util.BarTooltipRender;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * <b>护甲能量 tooltip</b>：给"能储能的护甲"显示能量条（样式与工具那条一致）。
 *
 * <p>为什么单独一份而不是复用 {@code EnergyTooltipHandler}：那一份住在 <b>core</b>（共享层），
 * 而"护甲容量"这件事只有 {@code :coe} 知道（{@link ArmorEnergy} 要读 {@link ArmorSet} 与
 * {@link ArmorEnchantments}）。core 不许反向引用层内代码，所以护甲这边在 coe 侧接一条 ——
 * 但<b>画法与语言键刻意与工具完全一致</b>（同一个 {@code item.createoreexpansion.tool.energy}
 * 与 {@link BarTooltipRender}），玩家看到的是同一种条。</p>
 *
 * <p>它挂在 {@code ClientEvents#onItemTooltip} 里、<b>工具能量区之后</b>（一件物品同时是工具又是
 * 护甲的情况不存在，所以顺序只影响空行数量）。</p>
 *
 * <h2>显示什么</h2>
 * <ul>
 *   <li>本件能量条：{@code 当前 / 本件容量}。容量是<b>现算</b>的（带散构聚能 = 2500）；
 *       所以一件 250 的翠玉头盔在附上散构聚能后，条会自动变成 2500 的刻度。</li>
 *   <li>成套时补一行套装合计：{@code 套装合计 3250 / 4000} —— 因为技能的扣能口径是
 *       <b>四件平摊</b>，玩家真正需要知道的是合计。</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class ArmorEnergyTooltipHandler {

    /** 与工具能量区<b>同一个键</b>（玩家看到的标题一致）。 */
    private static final String ENERGY_TRANSLATE_KEY = "item.createoreexpansion.tool.energy";

    /** 套装合计行。 */
    private static final String SET_TOTAL_TRANSLATE_KEY = "createoreexpansion.tooltip.armor_energy_total";

    /** 与工具能量条同一格数（见 {@code EnergyTooltipHandler#BAR_SLOTS}）。 */
    private static final int BAR_SLOTS = 20;

    private ArmorEnergyTooltipHandler() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 若该物品是可储能护甲，往后追加能量区。
     *
     * @param startIndex 现有内容之后的下一个可用 index
     * @return 插入完成后的下一个可用 index（不适用时原样返回）
     */
    public static int addArmorEnergyTooltip(ItemTooltipEvent event, int startIndex) {
        ItemStack stack = event.getItemStack();
        int max = ArmorEnergy.maxOf(stack);
        if (max <= 0) {
            return startIndex;
        }
        int energy = Math.min(ArmorEnergy.getEnergy(stack), max);
        List<Component> tip = event.getToolTip();
        int index = startIndex;

        if (index > 1) {
            tip.add(index++, CommonComponents.EMPTY);
        }
        tip.add(index++, Component.translatable(ENERGY_TRANSLATE_KEY).append(":").withStyle(ChatFormatting.GRAY));

        Color fillColor = energy == 0
            ? ToolEnergyColorConfig.DEFAULT.light
            : ToolEnergyColorConfig.DEFAULT.dark;
        tip.add(index++, BarTooltipRender.energy(energy, max, BAR_SLOTS, fillColor));

        // 套装合计：只在玩家身上穿着成套（或散构聚能补齐）时才有意义 —— 它是技能扣能的"池子"
        if (event.getEntity() != null) {
            ArmorSet effective = ArmorSet.effectiveSet(event.getEntity());
            if (effective != null && ArmorSet.of(stack) != null) {
                tip.add(index++, Component.translatable(SET_TOTAL_TRANSLATE_KEY,
                    ArmorEnergy.totalEnergy(event.getEntity()),
                    ArmorEnergy.totalMax(event.getEntity())).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return index;
    }
}
