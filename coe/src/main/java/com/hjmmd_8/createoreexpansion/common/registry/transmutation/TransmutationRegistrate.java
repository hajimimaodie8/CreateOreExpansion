package com.hjmmd_8.createoreexpansion.common.registry.transmutation;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeCreativeTabs;
import com.hjmmd_8.createoreexpansion.common.registry.LayerRegistrate;
import com.hjmmd_8.createoreexpansion.common.registry.RegistrateTooltips;
import com.simibubi.create.foundation.data.CreateRegistrate;

/**
 * <b>TRANS（机械嬗化学，mod id {@code transmutation}）自己的 Registrate —— W6-b2 起没有任何条目</b>。
 *
 * <p>原先它持有本层唯一的两个物品（{@link TransmutationItems#TRANSMUTE_MECHANISM} /
 * {@link TransmutationItems#INCOMPLETE_TRANSMUTE_MECHANISM}）。W6-b2 把嬗化内容整块搬进
 * {@code :coe}，那两个物品改挂 {@code CoeRegistrate.REGISTRATE}（理由见
 * {@link TransmutationItems} 的类注释：datagen 的层名 {@code "transmutation"} 会让它们的模型
 * 落进 {@code transmutation.jar}，正是 {@code c:buckets} 缺陷的镜像）。</p>
 *
 * <p><b>为什么本类仍然保留</b>：{@code transmutation} 这个 mod id 按用户裁定继续存在
 * （{@code TransmutationMod} 是空壳），"以后往第三层加新配方/新条目"时就挂到这个实例上；
 * 它的总线接线仍在 {@code CreateOreExpansion} 构造器里
 * （{@code TransmutationRegistrate.REGISTRATE.registerEventListeners(modEventBus)}），
 * 所以将来往这里加入口时不会出现"条目建好了却没人注册"的静默失效。</p>
 *
 * <p><b>命名空间仍是 {@code createoreexpansion}</b>（{@link CoeCore#REGISTRY_NAMESPACE}），
 * 注册 id 与拆分前逐字一致。</p>
 *
 * <p>默认创造页 = 基础页（矿物拓展）——与拆分前"所有条目都进 base_tab"一致；
 * 本层物品本来就都在那一页（2026-09-30 起该页是本模组唯一的创造页）。</p>
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
