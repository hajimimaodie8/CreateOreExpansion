package com.hjmmd_8.createoreexpansion.common.registry.cews;

import com.hjmmd_8.createoreexpansion.common.registry.LayerRecipeType;
import com.hjmmd_8.createoreexpansion.common.registry.WaveRecipeCapabilities;
import com.hjmmd_8.createoreexpansion.common.registry.WaveRecipeCapabilities.LayerOrder;
import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipe;

/**
 * <b>CEWS（能量波阵学）自己的配方类型</b>（P3c：从 {@code common/AllRecipeTypes} 拆出）。
 *
 * <p>本层目前只有一个配方类型：充能（{@code createoreexpansion:charging}）——
 * 翡翠/蓝宝石/星辉石应力充能器的能量波击中物品时按 {@code level} 字段匹配。
 * 注册 id、序列化器、配方类型与拆分前逐字一致。</p>
 *
 * <p><b>本类不做注册动作</b>：两张注册表住在 {@link LayerRecipeType}（core），
 * 由 {@code LayerBootstrap.ensureAttached(modEventBus)} 统一挂到事件总线——六个条目共用同一对注册表
 * （且"恰挂一次"），这是"注册顺序不变"的前提。</p>
 */
public final class CewsRecipeTypes {

    /** 充能加工：{@code createoreexpansion:charging}（等级是配方自带的 {@code level} 字段）。 */
    public static final LayerRecipeType CHARGING =
        LayerRecipeType.serializer("CHARGING", () -> new ChargingRecipe.Serializer<>(ChargingRecipe::new));

    /**
     * <b>本层的波加工能力登记</b>（P3i）：把自己这一份配方类型交给
     * {@link WaveRecipeCapabilities}。
     *
     * <p>三层的登记互不知情：本类只 import 登记表（SHARED），不 import 也不引用
     * COE / TRANS 的任何配方类型——这正是"机械换向后 CEWS 不再硬依赖 TRANS"的那条性质。</p>
     */
    static {
        WaveRecipeCapabilities.addOrdered(LayerOrder.CEWS).add(CHARGING);
    }

    private CewsRecipeTypes() {}
}
