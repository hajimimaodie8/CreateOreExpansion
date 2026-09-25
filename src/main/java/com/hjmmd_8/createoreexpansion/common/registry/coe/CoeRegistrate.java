package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.hub.AllCreativeModeTabs;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.LayerRegistrate;
import com.hjmmd_8.createoreexpansion.common.registry.RegistrateTooltips;
import com.simibubi.create.foundation.data.CreateRegistrate;

/**
 * <b>COE（矿物拓展，mod id {@code createoreexpansion}）自己的 Registrate</b>。
 *
 * <p>拆分前全模组共用一个 {@code CreateOreExpansion.REGISTRATE}；P3b 起每一层各持一个实例，
 * 本层（{@code common/registry/coe/**}、{@code AllFluids}、{@code SeriesTraits}）的所有
 * {@code .item(...) / .block(...) / .blockEntity(...)} 都走这里。</p>
 *
 * <p><b>命名空间</b>：{@link LayerRegistrate} 的构造参数恒为
 * {@link CoeCore#REGISTRY_NAMESPACE}（{@code createoreexpansion}）——所以注册 id 一个都没变。
 * 注意它<b>不是</b>"COE 的 mod id"这件事的复述：CEWS / TRANS 两个 Registrate 用的是同一个命名空间。</p>
 *
 * <p><b>实例化顺序</b>：本类的静态块必须在 {@link CoeBlocks} / {@link CoeItems} 的字段初始化
 * <b>之前</b>跑完（因为 {@code defaultCreativeTab} / tooltip 工厂都是"构建条目时读取"的实例状态）。
 * Java 的类初始化保证：任何对 {@code CoeRegistrate.REGISTRATE} 的首次访问都会先完整初始化本类，
 * 而 {@link CoeBlocks} / {@link CoeItems} 的第一个字段就要读它——顺序天然成立。</p>
 *
 * <p><b>datagen</b>：本实例的数据提供器由 {@code data/CreateOreExpansionDatagen} 统一创建
 * （原因见 {@link LayerRegistrate} 类注释）。</p>
 */
public final class CoeRegistrate {

    /** 本层唯一的 Registrate 实例（命名空间 = {@code createoreexpansion}）。 */
    public static final CreateRegistrate REGISTRATE =
        LayerRegistrate.create(CoeCore.REGISTRY_NAMESPACE, true);

    static {
        // 创造标签页的 holder 必须先就位，defaultCreativeTab 才能拿到 ResourceKey。
        // ensureTabs() 幂等，CEWS 那边也会调（谁先初始化都成立）。
        AllCreativeModeTabs.ensureTabs();
        // 通用两级提示（描述行 + 动能统计）。充能器专用提示归 CEWS，本层不接。
        RegistrateTooltips.install(REGISTRATE, false);
        // COE 的默认创造页 = 基础页（矿物拓展）——本层自己的常量，不再绕 SHARED 聚合入口
        REGISTRATE.defaultCreativeTab(CoeCreativeTabs.BASE_TAB.key());
    }

    private CoeRegistrate() {}
}
