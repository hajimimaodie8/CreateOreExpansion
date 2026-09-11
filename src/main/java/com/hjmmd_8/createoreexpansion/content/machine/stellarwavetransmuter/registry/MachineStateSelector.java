package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry;

import java.util.List;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

/**
 * 加工机"实时状态 → 配方类型"选择器（注册在 {@link StellarWaveMachineRegistry} 的单台
 * 机器档案上）。
 *
 * <p>部分加工机<b>按自身运行状态决定当前可用配方类型</b>（例：Vintage 真空室
 * mode=加压/抽真空各对应 PRESSURIZING / VACUUMIZING 一套）。变器扫描时按机器当时的
 * 状态把"当前应执行类型"快照进波；选择器仅作展示/模式口径——实际配方执行已全库检索。</p>
 */
@FunctionalInterface
public interface MachineStateSelector {

	/**
	 * @param machine 扫描到的动能机器
	 * @param base    静态档案默认类型（{@link StellarWaveMachineRegistry#typesFor}）
	 * @return 该机器当前应执行的类型列表（null/异常 = 沿用 base）
	 */
	List<IRecipeTypeInfo> typesFor(KineticBlockEntity machine, List<IRecipeTypeInfo> base);
}
