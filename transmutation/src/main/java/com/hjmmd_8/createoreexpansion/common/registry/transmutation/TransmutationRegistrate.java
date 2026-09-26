package com.hjmmd_8.createoreexpansion.common.registry.transmutation;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeCreativeTabs;
import com.hjmmd_8.createoreexpansion.common.registry.LayerRegistrate;
import com.hjmmd_8.createoreexpansion.common.registry.RegistrateTooltips;
import com.simibubi.create.foundation.data.CreateRegistrate;

/**
 * <b>TRANS（机械嬗化学，mod id {@code transmutation}）自己的 Registrate</b>。
 *
 * <p>本层目前只有两个物品（{@link TransmutationItems#TRANSMUTE_MECHANISM} /
 * {@link TransmutationItems#INCOMPLETE_TRANSMUTE_MECHANISM}），但注册器要独立——
 * 否则 TRANS 模块永远无法脱离 COE 单独编译。</p>
 *
 * <p><b>命名空间仍是 {@code createoreexpansion}</b>（{@link CoeCore#REGISTRY_NAMESPACE}），
 * 注册 id 与拆分前逐字一致。</p>
 *
 * <p>默认创造页 = 基础页（矿物拓展）——与拆分前"所有条目都进 base_tab"一致；
 * 嬗变相关物品没有被 {@code EnergyWaveStudyTab.CONTENTS} 收进 CEWS 页。</p>
 */
public final class TransmutationRegistrate {

    /** 本层唯一的 Registrate 实例（命名空间 = {@code createoreexpansion}）。 */
    public static final CreateRegistrate REGISTRATE =
        LayerRegistrate.create(CoeCore.REGISTRY_NAMESPACE, false);

    static {
        // P7a：不再需要"请求登记创造页"——本层自己没有页，而它借用的 COE 基础页由 :coe 的
        // @Mod 构造器登记（LayerCreativeTab.registerAll(CoeCreativeTabs.tabs())）。
        // defaultCreativeTab 只要 ResourceKey（由 id 算出、不依赖 holder），顺序无所谓。
        RegistrateTooltips.install(REGISTRATE);
        // TRANS -> COE 是允许方向（不是环）：TRANS 的默认创造页就是 COE 的基础页
        REGISTRATE.defaultCreativeTab(CoeCreativeTabs.BASE_TAB.key());
    }

    private TransmutationRegistrate() {}
}
