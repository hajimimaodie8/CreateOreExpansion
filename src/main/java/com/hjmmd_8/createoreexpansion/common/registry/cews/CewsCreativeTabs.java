package com.hjmmd_8.createoreexpansion.common.registry.cews;

import java.util.List;

import com.hjmmd_8.createoreexpansion.common.registry.LayerCreativeTab;

/**
 * <b>CEWS（能量波阵学）自己的创造模式标签页</b>（P3c：从 {@code common/AllCreativeModeTabs} 拆出）。
 *
 * <p>本层只有一个页：{@code energy_wave_study}（"机械动力：能量波阵学"，图标 = 翡翠应力充能器）。
 * 它排在 {@link com.simibubi.create.AllCreativeModeTabs#PALETTES_CREATIVE_TAB}（Create 调色板）
 * 之前，即整条顺序 <b>矿物拓展 → 能量波阵学 → Create 调色板</b>。</p>
 *
 * <p>"哪些物品属于这个页"的唯一清单住在同包的 {@code EnergyWaveStudyTab#CONTENTS}
 * （P3s 从 {@code common/hub/} 搬来，因为它 17 项全是 CEWS 的内容）——本类只管页本身。页 id 取
 * {@link LayerCreativeTab#ENERGY_WAVE_STUDY_TAB_ID}（core 常量，P3q 下移）：CEWS 与 COE
 * 的 {@code withTabsBefore} 链读的是同一个常量，页 id 只有一个来源。</p>
 *
 * <p><b>本类不做注册动作</b>：创造页注册表住在 {@link LayerCreativeTab}（core），
 * 由根侧注入的登记动作按层顺序登记——这是"注册顺序不变"的前提。</p>
 */
public final class CewsCreativeTabs {

    /** 能量波阵学页（{@code createoreexpansion:energy_wave_study}）。 */
    public static final LayerCreativeTab ENERGY_WAVE_STUDY = LayerCreativeTab.of(
        LayerCreativeTab.ENERGY_WAVE_STUDY_TAB_ID,
        com.simibubi.create.AllCreativeModeTabs.PALETTES_CREATIVE_TAB.getKey(),
        () -> CewsBlocks.JADE_STRESS_CHARGER.asStack());

    /** 本层页的声明顺序（由协调入口读取，顺序 = 拆分前的枚举顺序）。 */
    private static final List<LayerCreativeTab> TABS = List.of(ENERGY_WAVE_STUDY);

    /** 本层全部页，按声明顺序。 */
    public static List<LayerCreativeTab> tabs() {
        return TABS;
    }

    private CewsCreativeTabs() {}
}
