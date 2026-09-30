package com.hjmmd_8.createoreexpansion.common.registry.coe;

import java.util.List;

import com.hjmmd_8.createoreexpansion.common.registry.LayerCreativeTab;

/**
 * <b>本模组唯一的创造模式标签页</b>：{@code base_tab}（"矿物拓展"，图标 = 翡翠锭）。
 *
 * <p><b>2026-09-30（用户裁定）：全模组只保留这一个页。</b>原来 CEWS 的
 * {@code energy_wave_study} 页与其搬运逻辑 {@code EnergyWaveStudyTab} 已删除，
 * CEWS 层的 12 台波机器改由 {@code CewsRegistrate#defaultCreativeTab} 直接进本页，
 * 落在<b>「机械」分区</b>（判定见 {@link CoeCreativeSections}）。</p>
 *
 * <p><b>排序锚点</b>：本页接手了原 CEWS 页的位置 —— {@code withTabsBefore} 指向
 * Create 的调色板页（{@code AllCreativeModeTabs.PALETTES_CREATIVE_TAB}），
 * 所以玩家可见顺序仍是<b>矿物拓展 → Create 调色板</b>，与删除前一致。
 * 注意<b>不能</b>拿原版 {@code HOTBAR/SEARCH/OP_BLOCKS/INVENTORY} 当锚点
 * （会把原版页拉进排序图，见协作文档 §6.5）。</p>
 *
 * <p><b>本类不做注册动作</b>：创造页注册表住在 {@link LayerCreativeTab}（core），
 * 由每一层自己的 {@code @Mod} 构造器调用 {@code registerAll} 登记。</p>
 */
public final class CoeCreativeTabs {

    /** 矿物拓展页（{@code createoreexpansion:base_tab}，标题键 {@code itemGroup.createoreexpansion}）。 */
    @SuppressWarnings("Convert2MethodRef")
    public static final LayerCreativeTab BASE_TAB = LayerCreativeTab.of(
        "base_tab",
        "itemGroup.createoreexpansion",
        com.simibubi.create.AllCreativeModeTabs.PALETTES_CREATIVE_TAB.getKey(),
        () -> CoeItems.JADE_INGOT.asStack())
        // 本页是「一页 + 三条分区横幅（矿物 / 机械 / 装备）」：用会补空行的子类构建。
        // 现在全模组只有这一个页，所以"按页区分工厂"这件事失去了对象，
        // 但 core 的 LayerCreativeTab 仍保留按页注入的能力（将来若再添页就不会误伤）。
        .sectioned(CoeSectionedTab::new);

    /** 本层页的声明顺序（由协调入口读取，顺序 = 拆分前的枚举顺序）。 */
    private static final List<LayerCreativeTab> TABS = List.of(BASE_TAB);

    /** 本层全部页，按声明顺序。 */
    public static List<LayerCreativeTab> tabs() {
        return TABS;
    }

    private CoeCreativeTabs() {}
}
