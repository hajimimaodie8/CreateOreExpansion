package com.hjmmd_8.createoreexpansion.common.energy;

import java.awt.Color;
import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * <b>「能量条/能量行做渐变呈现」的工具契约</b>（P3p：契约进 core，实现留层里）。
 *
 * <p>能量门面 {@link com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy}
 * 与能量 tooltip {@code EnergyTooltipHandler} 都要对<b>翠玉之弓</b>（传说武器）单独走
 * "黄→绿逐字符/逐格渐变"，其余工具保持单色。而「翠玉之弓」是矿物拓展层的物品类，
 * 库不能 import 它——于是把这个显示契约提成一个接口：谁实现它，谁的能量文案就走渐变。</p>
 *
 * <h2>色标从哪来（2026-10-02 修「四把回旋镖能量条配色与档位不匹配」）</h2>
 * <p>本接口原先是个<b>零方法标记</b>：两个消费方（{@code EnergyTooltipHandler} 的条、
 * {@code ToolEnergy} 的文案行）各自把色标<b>写死</b>成翠玉之弓那条
 * {@code 0x55FF55 -> 0xFFFF55}。后果是凡是实现本接口的物品都拿到<b>同一条</b>黄绿渐变——
 * 回旋镖四把因此全部显示同一种颜色（翠玉那把"看着对"，只是因为它的档位色恰好等于那条
 * 渐变的起点，而不是因为取色真的分档）。</p>
 *
 * <p>现在色标由<b>物品自己</b>回答（{@link #energyGradientStops(ItemStack)}）：</p>
 * <ul>
 *   <li>回旋镖四把覆写它，返回<b>自己档位</b>的
 *       {@code ToolEnergyColorConfig} 两端色（JADE / SAPPHIRE / STELLARSTONE / THUNDERITE，
 *       见 {@code BoomerangTier#color()}）——四把各不相同；</li>
 *   <li>翠玉之弓<b>不覆写</b>，默认返回空表 ⇒ 消费方回落到本契约的历史默认
 *       （翠玉之弓那条绿→黄，逐字不变；护甲翠玉套的配色也仍以它为源）；</li>
 *   <li>以后再加"按材质变色的渐变条"，只需覆写这一个方法，不必再动任何一个消费方。</li>
 * </ul>
 */
public interface EnergyGradientTool {

    /**
     * 这条能量条的渐变色标（<b>左 → 右</b>）。
     *
     * @param stack 正在渲染提示的物品（挂在物品实例上，将来可按组件/NBT 变化）
     * @return 2 个及以上的色标；<b>空表 = 未覆写</b>，消费方回落到本契约的历史默认
     *         （翠玉之弓那条 {@code 0x55FF55 -> 0xFFFF55}，字面量留在两个消费方各自那一处，
     *         因此 {@code tools/check-armor-sets.ps1} §14 对弓那条配色的断言仍然有效）
     */
    default List<Color> energyGradientStops(ItemStack stack) {
        return List.of();
    }
}
