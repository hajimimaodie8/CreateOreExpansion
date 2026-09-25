package com.hjmmd_8.createoreexpansion.common.hub;

import com.hjmmd_8.createoreexpansion.common.registry.LayerCreativeTab;
import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsCreativeTabs;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeCreativeTabs;

import net.neoforged.bus.api.IEventBus;

/**
 * <b>创造模式标签页的协调入口</b>（P3c：四个"多层枢纽文件"按层拆分后保留的同名入口，住 SHARED 层）。
 *
 * <p><b>拆分前</b>：本类是一个枚举，COE 的 {@code base_tab} 与 CEWS 的
 * {@code energy_wave_study} 两个枚举项、以及那张 {@code DeferredRegister<CreativeModeTab>}
 * 混在一处。三个层的 Registrate（{@code CoeRegistrate} / {@code CewsRegistrate} /
 * {@code TransmutationRegistrate}）与两个物品类都靠 {@code AllCreativeModeTabs.BASE_TAB.key()}
 * 这类引用拿 {@code ResourceKey}，所以入口的<b>类名与两个常量名必须原样保留</b>。</p>
 *
 * <p><b>拆分后</b>：两个页的<b>声明</b>搬进各自层的类
 * （{@link CoeCreativeTabs#BASE_TAB} / {@link CewsCreativeTabs#ENERGY_WAVE_STUDY}），
 * 本类只负责"按拆分前的顺序登记"：</p>
 * <ul>
 *   <li>两个字段的文本顺序 = 拆分前枚举常量的声明顺序（BASE_TAB → ENERGY_WAVE_STUDY），
 *       于是两层类被初始化的顺序也就是拆分前的顺序；</li>
 *   <li>{@link #registerTabs()} 按同一顺序把页登记进注册表
 *       （原实现是遍历 {@code values()}，现在显式按层调用）；</li>
 *   <li>{@link #ensureTabs()} 的幂等语义、{@link #register(IEventBus)} 的接线时机都不变。</li>
 * </ul>
 *
 * <p><b>标签页顺序</b>（用 {@code withTabsBefore} 链起来，必须无环）：
 * {@code base_tab}（矿物拓展）→ {@code energy_wave_study}（能量波阵学）→ Create 的调色板。
 * 也就是"本体的矿物线在前，能量波阵学（CEWS）作为独立板块紧跟其后"——
 * 链的两端分别声明在 {@link CoeCreativeTabs} 与 {@link CewsCreativeTabs} 里。</p>
 */
public final class AllCreativeModeTabs {

    // ================= 两个创造页：声明在各层，这里只定顺序 =================
    //
    // 顺序 = 拆分前枚举常量顺序（BASE_TAB(COE) → ENERGY_WAVE_STUDY(CEWS)），逐字相同。
    // 恰好也就是本工程的层约定顺序 COE → CEWS。

    /** 矿物拓展页（COE 层：{@link CoeCreativeTabs#BASE_TAB}）。 */
    public static final LayerCreativeTab BASE_TAB = CoeCreativeTabs.BASE_TAB;

    /**
     * <b>机械动力：能量波阵学</b>（Create: Energy Wave Studies，简称 <b>CEWS</b>）页
     * （CEWS 层：{@link CewsCreativeTabs#ENERGY_WAVE_STUDY}）。
     *
     * <p>能量波系统的机器 + 三种机壳（现有两种）+ 波情查询仪归到这里，作为一个独立板块
     * （用户 2026-09-14 要求）。图标用<b>翡翠应力充能器</b>——它是整条能量波线的起点（波由充能器发出），
     * 比"波变器"更能代表这一板块（用户指定）。</p>
     *
     * <p>后续要把它整包拆成一个独立的内置 jar（新模块 CEWS），届时"哪些内容属于这个模块"就以
     * {@code common/registry/cews/EnergyWaveStudyTab#CONTENTS} 那一份清单为准——所以清单只有一处，
     * 标签页内容与未来的拆包依据共用它。那个类在 P3s 已从 {@code common/hub/} 搬进 CEWS 自己的
     * 注册包（它 17 项全是 CEWS 内容），本入口因此不再 import 它。</p>
     */
    public static final LayerCreativeTab ENERGY_WAVE_STUDY = CewsCreativeTabs.ENERGY_WAVE_STUDY;

    // ================= 注册动作（顺序 = 拆分前逐字相同） =================

    /** 标签页是否已经建好（{@link #ensureTabs()} 的幂等标志）。 */
    private static boolean tabsRegistered;

    /**
     * 幂等地把两个页登记进注册表（填好各自的 holder）。
     *
     * <p>COE / CEWS / TRANS 三个 Registrate 都要在设 {@code defaultCreativeTab} 之前拿到
     * {@link #BASE_TAB}/{@link #ENERGY_WAVE_STUDY} 的 {@link net.minecraft.resources.ResourceKey}，
     * 而"谁先被类初始化"取决于四个 {@code @Mod} 构造器的顺序——所以这里做成幂等，
     * 谁先来谁负责建，后续调用直接返回（与拆分前一致）。</p>
     */
    public static void ensureTabs() {
        if (tabsRegistered) {
            return;
        }
        tabsRegistered = true;
        registerTabs();
    }

    /**
     * 登记两个页，<b>顺序与拆分前逐字相同</b>：先 COE 的 {@code base_tab}，再 CEWS 的
     * {@code energy_wave_study}（原实现是遍历枚举 {@code values()}，枚举顺序就是这个顺序）。
     *
     * <p>不向事件总线注册注册器——那件事在 {@link #register(IEventBus)} 里。</p>
     */
    public static void registerTabs() {
        // 层顺序 = 拆分前的枚举顺序：COE → CEWS
        LayerCreativeTab.registerAll(CoeCreativeTabs.tabs());
        LayerCreativeTab.registerAll(CewsCreativeTabs.tabs());
    }

    /** 向事件总线注册注册表（原 {@code AllCreativeModeTabs.register}，调用点未变）。 */
    public static void register(IEventBus bus) {
        LayerCreativeTab.registerOn(bus);
    }

    private AllCreativeModeTabs() {}
}
