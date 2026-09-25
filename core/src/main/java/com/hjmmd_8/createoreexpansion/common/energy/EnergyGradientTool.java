package com.hjmmd_8.createoreexpansion.common.energy;

/**
 * <b>「能量条/能量行做渐变呈现」的工具标记契约</b>（P3p：契约进 core，实现留层里）。
 *
 * <p>能量门面 {@link ToolEnergy} 与能量 tooltip {@code EnergyTooltipHandler} 都要对
 * <b>翠玉之弓</b>（传说武器）单独走"黄→绿逐字符/逐格渐变"，其余工具保持单色。
 * 而「翠玉之弓」是矿物拓展层的物品类，库不能 import 它——于是把这个显示契约提成
 * 一个<b>零方法标记接口</b>：谁实现它，谁的能量文案就走渐变。</p>
 *
 * <p>判别处本来写的是 {@code instanceof JadeTopazBowItem}（还带一处完整 FQN 字面量），
 * 现在换成 {@code instanceof EnergyGradientTool}：判定结果对现有物品逐个相同
 * （目前只有 {@code JadeTopazBowItem} 实现它）。</p>
 */
public interface EnergyGradientTool {
}
