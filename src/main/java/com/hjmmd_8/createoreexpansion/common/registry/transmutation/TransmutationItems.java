package com.hjmmd_8.createoreexpansion.common.registry.transmutation;

import com.hjmmd_8.createoreexpansion.common.*;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeCreativeTabs;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

/**
 * <b>TRANS（机械嬗化学）</b>物品注册：嬗化构件 / 未完成的嬗化构件（{@code transmute_mechanism}）。
 * 嬗化相关的配方类型、效果与流体不在本类（见 {@code AllTransmutingRecipe} / {@code TransmutationEffects} / {@code TransmutationFluids}）。<br>
 * 本次拆分是<b>纯搬运</b>，注册 id 与链式调用一字未改，P2a 已删除 {@code AllItems} 外观类，引用直接指向本层。
 */
public final class TransmutationItems {

    public static final ItemEntry<Item> TRANSMUTE_MECHANISM = TransmutationRegistrate.REGISTRATE
            .item("transmute_mechanism", Item::new)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> INCOMPLETE_TRANSMUTE_MECHANISM = TransmutationRegistrate.REGISTRATE
            .item("incomplete_transmute_mechanism", Item::new)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .removeTab(CoeCreativeTabs.BASE_TAB.key())
            .register();

/** 触发本层注册类的类初始化：Registrate 的注册动作就是字段初始化，因此方法体为空。 */
	public static void register() {
	}

	private TransmutationItems() {
	}
}
