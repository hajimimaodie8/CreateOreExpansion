package com.hjmmd_8.createoreexpansion.common.registry.cews;

import java.util.List;

import com.hjmmd_8.createoreexpansion.common.hub.EnergyWaveStudyTab;
import com.hjmmd_8.createoreexpansion.common.registry.LayerCreativeTab;

/**
 * <b>CEWS（能量波阵学）自己的创造模式标签页</b>（P3c：从 {@code common/AllCreativeModeTabs} 拆出）。
 *
 * <p>本层只有一个页：{@code energy_wave_study}（"机械动力：能量波阵学"，图标 = 翡翠应力充能器）。
 * 它排在 {@link com.simibubi.create.AllCreativeModeTabs#PALETTES_CREATIVE_TAB}（Create 调色板）
 * 之前，即整条顺序 <b>矿物拓展 → 能量波阵学 → Create 调色板</b>。</p>
 *
 * <p>"哪些物品属于这个页"的唯一清单位于 {@link EnergyWaveStudyTab#CONTENTS}
 * （SHARED 层）——本类只管页本身。页 id 取 {@link EnergyWaveStudyTab#TAB_ID}，
 * 于是"页 id"与"内容清单"共用同一个常量来源。</p>
 *
 * <p><b>本类不做注册动作</b>：创造页注册表住在 {@link LayerCreativeTab}（SHARED），
 * 由协调入口 {@code common/AllCreativeModeTabs} 按层顺序登记——这是"注册顺序不变"的前提。</p>
 */
public final class CewsCreativeTabs {

    /** 能量波阵学页（{@code createoreexpansion:energy_wave_study}）。 */
    public static final LayerCreativeTab ENERGY_WAVE_STUDY = LayerCreativeTab.of(
        EnergyWaveStudyTab.TAB_ID,
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
