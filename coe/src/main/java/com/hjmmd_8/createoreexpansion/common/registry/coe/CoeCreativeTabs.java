package com.hjmmd_8.createoreexpansion.common.registry.coe;

import java.util.List;

import com.hjmmd_8.createoreexpansion.common.registry.LayerCreativeTab;

/**
 * <b>COE（矿物拓展）自己的创造模式标签页</b>（P3c：从 {@code common/AllCreativeModeTabs} 拆出）。
 *
 * <p>本层只有一个页：{@code base_tab}（"矿物拓展"，图标 = 翡翠锭）。它<b>排在最前</b>——
 * {@code withTabsBefore} 指向 CEWS 的页（{@code energy_wave_study}），于是整条顺序是
 * <b>矿物拓展 → 能量波阵学 → Create 调色板</b>（与拆分前的枚举顺序逐字一致）。</p>
 *
 * <p><b>为什么这里能引用 CEWS 的页而不违反分层方向</b>：本类读的是
 * {@link LayerCreativeTab#ENERGY_WAVE_STUDY_TAB_ID}（core 的 {@code static final String} 常量，
 * P3q 从 {@code common/hub/EnergyWaveStudyTab} 下移；那个类本身 P3s 又搬到了
 * {@code common/registry/cews/}），<b>没有</b> import 任何 CEWS 层的类，
 * 所以不存在 COE → CEWS 的编译期依赖。</p>
 *
 * <p><b>本类不做注册动作</b>：创造页注册表住在 {@link LayerCreativeTab}（core），
 * 由根侧注入的登记动作按层顺序登记——这是"注册顺序不变"的前提。</p>
 */
public final class CoeCreativeTabs {

    /** 矿物拓展页（{@code createoreexpansion:base_tab}，标题键 {@code itemGroup.createoreexpansion}）。 */
    @SuppressWarnings("Convert2MethodRef")
    public static final LayerCreativeTab BASE_TAB = LayerCreativeTab.of(
        "base_tab",
        "itemGroup.createoreexpansion",
        LayerCreativeTab.tabKey(LayerCreativeTab.ENERGY_WAVE_STUDY_TAB_ID),
        () -> CoeItems.JADE_INGOT.asStack())
        // 本页是「一页 + 三条分区横幅（矿物 / 机械 / 装备）」：用会补空行的子类构建。
        // 只有本页声明工厂 —— CEWS 的 energy_wave_study 页必须保持原样（它靠自己那份
        // remove/accept 清单钉顺序），core 的 LayerCreativeTab 因此按页区分，不会误伤。
        .sectioned(CoeSectionedTab::new);

    /** 本层页的声明顺序（由协调入口读取，顺序 = 拆分前的枚举顺序）。 */
    private static final List<LayerCreativeTab> TABS = List.of(BASE_TAB);

    /** 本层全部页，按声明顺序。 */
    public static List<LayerCreativeTab> tabs() {
        return TABS;
    }

    private CoeCreativeTabs() {}
}
