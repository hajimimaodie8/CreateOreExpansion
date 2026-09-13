package com.hjmmd_8.createoreexpansion.util;

import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * <b>半径立方体遍历的唯一实现</b>（2026-09 抽象整理：消灭"每个扫描各抄一遍 for 循环"）。
 *
 * <p>本模组的扫描 / 取料 / 余料入库一共出现 7 处结构完全相同的循环：</p>
 * <pre>
 * for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r,-r,-r), center.offset(r,r,r))) {
 *     if (pos.equals(center)) continue;               // 不打自己的主意
 *     if (!level.isLoaded(pos)) continue;             // 未加载区块：会触发同步加载
 *     if (skip != null &amp;&amp; skip.test(pos)) continue;   // 调用方自定义排除
 *     ...各自的事情...
 * }
 * </pre>
 * <p>前三条守卫是<b>每条都必须有</b>的口径，抄在七个地方就迟早漏一个
 * （历史上"变器贴区块边界把周围未加载区块同步加载"正是这么来的）。
 * 现在统一走本类，包含/排除/顺序语义只有一处。</p>
 */
public final class RadiusScan {

	private RadiusScan() {
	}

	/**
	 * 遍历以 {@code center} 为心、半径 {@code radius}（≥1）的立方体：跳过 {@code center} 自身、
	 * 未加载区块、以及 {@code skip} 判为 true 的位置，对其余位置调用 {@code action}。
	 *
	 * @param skip   额外排除（可 null）；中心格与未加载区块已由本方法排除
	 * @param action 对每个合格位置执行（位置是<b>可变游标</b>，需保存请自行 {@code immutable()}）
	 */
	public static void forEachInRadius(Level level, BlockPos center, int radius, Predicate<BlockPos> skip,
		Consumer<BlockPos> action) {
		if (level == null || center == null || action == null)
			return;
		int r = Math.max(1, radius);
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			if (pos.equals(center))
				continue;
			if (!level.isLoaded(pos))
				continue; // 未加载区块：读方块状态/能力会触发同步加载
			if (skip != null && skip.test(pos))
				continue;
			action.accept(pos);
		}
	}

	/**
	 * 同 {@link #forEachInRadius}，但只统计满足 {@code filter} 的位置数（不收集位置）。
	 * 用于"台数"这类只关心数量的读数，避免为计数而建列表。
	 */
	public static int countInRadius(Level level, BlockPos center, int radius, Predicate<BlockPos> skip,
		Predicate<BlockPos> filter) {
		int[] count = { 0 };
		forEachInRadius(level, center, radius, skip, pos -> {
			if (filter == null || filter.test(pos))
				count[0]++;
		});
		return count[0];
	}
}
