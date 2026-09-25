package com.hjmmd_8.createoreexpansion.common.hub;

import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationFluids;
import com.hjmmd_8.createoreexpansion.content.transmuting.fluid.TransmutationFluid;
import com.tterrag.registrate.util.entry.FluidEntry;

/**
 * <b>同名聚合别名</b>（P3s，照 P3c / P3p / P3k 的既有模式）：真正的声明住在它所属的层里——
 * {@link TransmutationFluids}（{@code common/registry/transmutation/}，TRANS 层）。
 *
 * <p><b>为什么保留这个类</b>：{@code transmutation_fluid} 的注册触发一直是
 * {@code CreateOreExpansion} 构造器里那句 {@code AllFluids.register()}（位置与顺序一字未动）。
 * 保留同名别名后那个调用点<b>一行未改</b>——类名、方法名、注册 id、注册时机全部不变。</p>
 *
 * <p><b>P3t：层内消费者已经全部改走归属层，hub 只剩根侧一个调用点</b>。P3s 时还有四个
 * 调用点写 {@code AllFluids.TRANSMUTATION_FLUID}，现在都不再走这里：TRANS 的
 * {@code TransmutationEventHandler} 与 {@code TransmutingCategory} 改读本层声明
 * {@link TransmutationFluids}（层 → 集成层那条边正是 {@code layer-closure} 报表里的 blocker）；
 * COE 的两个凝能佩处理器（{@code MedallionClientHandler} / {@code MedallionEffectHandler}）
 * 因为 {@code COE -> TRANS} 是禁止方向，改读 core 的窄契约
 * {@code common.transmutation.TransmutationLink}（由 {@code TransmutationFluids} 注入）。
 * 于是本类只剩 {@code CreateOreExpansion} 的 {@code register()} 一个调用者，
 * 字段 {@link #TRANSMUTATION_FLUID} 作为入口 API <b>原样保留</b>（值、泛型实参、语义不变）。</p>
 *
 * <p><b>为什么别名类必须留在 {@code common/hub/}</b>：它无条件转发到 TRANS
 * （{@code common.registry.transmutation}），而 {@code common} 顶层包已经整体搬进 core 库
 * ——留在那里就是一个 {@code CORE → TRANS} 引用。hub 是集成层：根侧可以 import hub，
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
