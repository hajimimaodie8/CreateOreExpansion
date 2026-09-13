package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry;

import java.util.List;

import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 加工机"实时状态 → 配方类型"选择器（注册在 {@link StellarWaveMachineRegistry} 的单台
 * 机器档案上）。
 *
 * <p>部分加工机<b>按自身运行状态决定当前可用配方类型</b>（例：Vintage 真空室
 * mode=加压/抽真空各对应 PRESSURIZING / VACUUMIZING 一套）。变器扫描时按机器当时的
 * 状态把"当前应执行类型"快照进波；选择器仅作展示/模式口径——实际配方执行已全库检索。</p>
 *
 * <p><b>参数类型是 {@link BlockEntity} 而不是 {@code KineticBlockEntity}</b>
 * （2026-09 修正）：加工机不全是动能机——Create 的<b>注液器 Spout</b>（{@code filling}）与
 * <b>物品排放器 Item Drain</b>（{@code emptying}）都是 {@code SmartBlockEntity}。
 * 旧签名只收动能机，变器扫描时直接把它们跳过 → 波拿不到 {@code filling}/{@code emptying} 类型
 * → 类型门把注液/排放配方挡掉（用户实测："水抽到了，但只进附近储罐，不给铁桶注液"）。
 * 需要动能状态的实现自己 {@code instanceof} 具体类再取速度即可。</p>
 */
@FunctionalInterface
public interface MachineStateSelector {

	/**
	 * @param machine 扫描到的加工机方块实体（可能是非动能机）
	 * @param base    静态档案默认类型（{@link StellarWaveMachineRegistry#typesFor}）
	 * @return 该机器当前应执行的类型列表（null/异常 = 沿用 base）
	 */
	List<IRecipeTypeInfo> typesFor(BlockEntity machine, List<IRecipeTypeInfo> base);
}
