package com.hjmmd_8.createoreexpansion.common.registry.cews;

import com.hjmmd_8.createoreexpansion.common.AllCreativeModeTabs;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.LayerRegistrate;
import com.hjmmd_8.createoreexpansion.common.registry.RegistrateTooltips;
import com.simibubi.create.foundation.data.CreateRegistrate;

/**
 * <b>CEWS（能量波阵学，mod id {@code cews}）自己的 Registrate</b>。
 *
 * <p>本层（{@code common/registry/cews/**}）的方块 / 方块实体 / 物品全部走这里：
 * {@link CewsBlocks}（17 处）、{@link CewsBlockEntityTypes}（14 处）、{@link CewsItems}（3 处）。</p>
 *
 * <p><b>命名空间仍是 {@code createoreexpansion}</b>（{@link CoeCore#REGISTRY_NAMESPACE}），
 * 与 CEWS 自己的 mod id 无关——这就是"拆模块不改注册 id"的关键。</p>
 *
 * <p><b>充能器提示归本层</b>：{@code client/ChargerKineticTooltip} 只对
 * {@code JadeStressChargerBlock} / {@code SapphireStressChargerBlock} 生效，这两种机器是本层的
 * （{@link CewsBlocks#JADE_STRESS_CHARGER} / {@link CewsBlocks#SAPPHIRE_STRESS_CHARGER}），
 * 所以 {@code chargers = true}。COE / TRANS 的物品对该 modifier 恒返回 {@code null}，
 * 两头写法等价，这里按"机器归谁"来挂。</p>
 */
public final class CewsRegistrate {

    /** 本层唯一的 Registrate 实例（命名空间 = {@code createoreexpansion}）。 */
    public static final CreateRegistrate REGISTRATE =
        LayerRegistrate.create(CoeCore.REGISTRY_NAMESPACE, false);

    static {
        AllCreativeModeTabs.ensureTabs();
        RegistrateTooltips.install(REGISTRATE, true);
        // CEWS 的默认创造页 = 能量波阵学页
        REGISTRATE.defaultCreativeTab(AllCreativeModeTabs.ENERGY_WAVE_STUDY.key());
    }

    private CewsRegistrate() {}
}
