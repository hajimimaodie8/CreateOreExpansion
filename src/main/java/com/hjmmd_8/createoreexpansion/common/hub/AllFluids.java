package com.hjmmd_8.createoreexpansion.common.hub;

import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationFluids;
import com.hjmmd_8.createoreexpansion.content.transmuting.fluid.TransmutationFluid;
import com.tterrag.registrate.util.entry.FluidEntry;

/**
 * <b>同名聚合别名</b>（P3s，照 P3c / P3p / P3k 的既有模式）：真正的声明住在它所属的层里——
 * {@link TransmutationFluids}（{@code common/registry/transmutation/}，TRANS 层）。
 *
 * <p><b>为什么保留这个类</b>：{@code transmutation_fluid} 被 COE
 * （{@code MedallionClientHandler}、{@code MedallionEffectHandler}）与 TRANS
 * （{@code TransmutationEventHandler}）共用，JEI 的 {@code TransmutingCategory} 也用它做催化剂。
 * 四个调用点写的都是 {@code AllFluids.TRANSMUTATION_FLUID}，保留同名别名后
 * <b>这些调用点一行未改</b>——类名、字段名、泛型实参、注册 id、注册时机全部不变。</p>
 *
 * <p><b>本轮实际改了什么</b>：① 那段 Registrate 链式声明整体搬到
 * {@link TransmutationFluids}（实现类 {@code TransmutationFluid} /
 * {@code TransmutationFluidBlock} 现在同属 TRANS，构造逻辑跟着实现走）；
 * ② 删掉一行<b>死的</b> {@code import ...CreateOreExpansion}——它在中文档里被提及、
 * 代码里零使用，却在依赖报表里让"层 → {@code @Mod} 入口"多出一条假边。</p>
 *
 * <p><b>为什么别名类必须留在 {@code common/hub/}</b>：它无条件转发到 TRANS
 * （{@code common.registry.transmutation}），而 {@code common} 顶层包要整体搬进 core 库
 * ——留在那里就是一个 {@code CORE → TRANS} 引用。hub 是集成层：层可以 import hub，
 * 但 hub 永远不许进 core（同 {@code AllRecipeTypes} / {@code AllCreativeModeTabs}）。</p>
 */
public final class AllFluids {

	/** 嬗变液（转发 TRANS 层声明；注册 id、桶、贴图、标签与拆分前逐字相同）。 */
	public static final FluidEntry<TransmutationFluid.Flowing> TRANSMUTATION_FLUID =
		TransmutationFluids.TRANSMUTATION_FLUID;

	private AllFluids() {
	}

	/** 注册触发：委托 TRANS 层（调用点、调用时机、事件总线全部与拆分前一致）。 */
	public static void register() {
		TransmutationFluids.register();
	}

}
