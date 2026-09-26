package com.hjmmd_8.createoreexpansion.common.registry.cews;

import com.hjmmd_8.createoreexpansion.content.wave.gauge.WaveQueryGaugeItem;
import com.hjmmd_8.createoreexpansion.common.*;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeCreativeTabs;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

/**
 * <b>CEWS（能量波阵学）</b>物品注册：能量构件 / 未完成的能量构件（{@code energy_mechanism}，
 * 按方案文档 §3.2 建议①归本层）与波情查询仪。<br>
 * 本次拆分是<b>纯搬运</b>，注册 id 与链式调用一字未改，P2a 已删除 {@code AllItems} 外观类，引用直接指向本层。
 */
public final class CewsItems {

    public static final ItemEntry<Item> ENERGY_MECHANISM = CewsRegistrate.REGISTRATE
            .item("energy_mechanism", Item::new)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> INCOMPLETE_ENERGY_MECHANISM = CewsRegistrate.REGISTRATE
            .item("incomplete_energy_mechanism", Item::new)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .removeTab(CoeCreativeTabs.BASE_TAB.key())
            .register();

    // ========== 波情查询仪（手持静态贴图；右键查询最近能量波的波情，查询期间播动画贴图） ==========

    public static final ItemEntry<WaveQueryGaugeItem> WAVE_QUERY_GAUGE = CewsRegistrate.REGISTRATE
            .item("wave_query_gauge", WaveQueryGaugeItem::new)
            // 模型走手写 JSON（models/item/wave_query_gauge.json：默认静态 idle + overrides 挂
            // createoreexpansion:scanning 指向动画模型），故这里给空的 datagen 模型提供者，
            // 避免 datagen 另生成一份同名模型（做法与 jade_topaz_bow 一致）
            .model((ctx, provider) -> {})
            .register();

/** 触发本层注册类的类初始化：Registrate 的注册动作就是字段初始化，因此方法体为空。 */
	public static void register() {
	}

	private CewsItems() {
	}
}
