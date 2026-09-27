package com.hjmmd_8.createoreexpansion.common.registry.transmutation;

import com.hjmmd_8.createoreexpansion.common.*;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeCreativeTabs;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRegistrate;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

/**
 * <b>嬗化线的物品注册</b>：嬗化构件 / 未完成的嬗化构件（{@code transmute_mechanism}）。
 * 嬗化相关的配方类型、效果与流体不在本类（见 {@code AllTransmutingRecipe} / {@code TransmutationEffects} / {@code TransmutationFluids}）。<br>
 * 注册 id 与链式调用一字未改（P2a 已删除 {@code AllItems} 外观类，引用直接指向本层）。
 *
 * <p><b>W6-b2：这两个物品从 {@code TransmutationRegistrate} 改挂 {@link CoeRegistrate#REGISTRATE}</b>
 * （用户裁定：嬗化内容整块进第一层，注册触发搬进 {@code CreateOreExpansion}）。
 * 为什么必须换 Registrate —— 这是"发布形态单装矩阵"的判据，不是风格问题：</p>
 * <ul>
 *   <li>datagen 的「注册 id → 层」表是按 <b>Registrate 实例</b>建的
 *       （{@code CreateOreExpansionDatagen#collectTagLayers} 与
 *       {@code LayerLangSplitter.Layer}），而 {@code TransmutationRegistrate} 被登记成层名
 *       {@code "transmutation"} ⇒ 它的条目模型/语言会写进 {@code transmutation/src/generated}
 *       ⇒ 只装 {@code coe.jar} 时两个物品<b>没有模型</b>（{@code c:buckets} 缺陷的镜像）。</li>
 *   <li>换成 {@code CoeRegistrate} 后，"条目在哪一层"这个问题自动得到正确答案（{@code coe}），
 *       不需要把 {@code TransmutationRegistrate} 的 provider 指到 {@code coe} 的输出根
 *       ——后者会让两个 Registrate 的 provider 共用一个模块目录，而
 *       {@code LayerDataProvider#purgeStale} 会把"不是自己产出的"文件删掉。</li>
 *   <li>两个 Registrate 的实例配置本来就逐字相同（都是
 *       {@code RegistrateTooltips.install(...)} + {@code defaultCreativeTab(BASE_TAB)}），
 *       所以换实例不改变默认创造页、tooltip 工厂、注册 id、模型路径或语言键。</li>
 * </ul>
 * <p>{@code TransmutationRegistrate} 本身仍然保留（空壳模块的"以后加新东西"落点），
 * 它的总线接线也仍在 {@code CreateOreExpansion} 里。</p>
 */
public final class TransmutationItems {

    public static final ItemEntry<Item> TRANSMUTE_MECHANISM = CoeRegistrate.REGISTRATE
            .item("transmute_mechanism", Item::new)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> INCOMPLETE_TRANSMUTE_MECHANISM = CoeRegistrate.REGISTRATE
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
