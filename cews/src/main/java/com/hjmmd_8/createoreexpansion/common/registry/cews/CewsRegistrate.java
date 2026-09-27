package com.hjmmd_8.createoreexpansion.common.registry.cews;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.LayerRegistrate;
import com.hjmmd_8.createoreexpansion.common.registry.RegistrateTooltips;
import com.simibubi.create.foundation.data.CreateRegistrate;

/**
 * <b>CEWS（能量波阵学，mod id {@code cews}）自己的 Registrate</b>。
 *
 * <p>本层（{@code common/registry/cews/**}）的<b>机器</b>方块 / 方块实体 / 物品全部走这里：
 * {@link CewsBlocks}（14 处）、{@link CewsBlockEntityTypes}（11 处）、{@link CewsItems}（3 处）。
 * 三台应力充能器不在其中（W6-c，见下）。</p>
 *
 * <p><b>命名空间仍是 {@code createoreexpansion}</b>（{@link CoeCore#REGISTRY_NAMESPACE}），
 * 与 CEWS 自己的 mod id 无关——这就是"拆模块不改注册 id"的关键。</p>
 *
 * <p><b>W6-c：充能器提示改挂第一层</b>。{@code ChargerKineticTooltip} 只对
 * {@code JadeStressChargerBlock} / {@code SapphireStressChargerBlock} 生效，而这两台机器
 * 现在与它们的登记一起住第一层（{@code common.registry.coe.charger.CoeChargerBlocks}），
 * 所以那段 modifier 由 {@code CoeRegistrate} 的静态块串联。本层仍装"通用两级"，
 * 机器物品的提示一字未变。</p>
 */
public final class CewsRegistrate {

    /** 本层唯一的 Registrate 实例（命名空间 = {@code createoreexpansion}）。 */
    public static final CreateRegistrate REGISTRATE =
        LayerRegistrate.create(CoeCore.REGISTRY_NAMESPACE, false);

    static {
        // P7a：这里不再需要"请求登记创造页"——登记动作已由 CewsMod 构造器里的
        // LayerCreativeTab.registerAll(CewsCreativeTabs.tabs()) 直接完成。
        // W6-c：共享库的通用两级（描述行 + 动能统计）仍旧装；但"充能器专用那一段"
        // （ChargerKineticTooltip#withChargers）已随充能器搬到第一层 —— 它必须装在
        // CoeRegistrate 上，否则那三台机器的物品悬停会丢掉自定义应力区间行。
        RegistrateTooltips.install(REGISTRATE);
        // CEWS 的默认创造页 = 能量波阵学页（本层自己的常量，不再绕 SHARED 聚合入口）
        REGISTRATE.defaultCreativeTab(CewsCreativeTabs.ENERGY_WAVE_STUDY.key());
    }

    private CewsRegistrate() {}
}
