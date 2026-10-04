package com.hjmmd_8.createoreexpansion.content.charger.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * <b>变体波分裂时的"物质均摊"算术</b>（2026-10-06 行为零变化拆分，从
 * {@link StellarWaveEntity} 的 {@code splitShare} / {@code splitItems} 两件<b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>差波器把一个母波分成 n 份时，第 i 份怎么算</b>——
 * 纯算术、无世界引用、无实体状态（所以它是静态的，也是唯一一处"按份均摊"的实现）。</p>
 *
 * <p>搬运口径：方法体逐字相同，差异只有 {@code private} → 包级私有。
 * <b>"余数优先给靠前的子波"与"摊平成逐件位置后按位置取模"两条规则、以及
 * {@code Math.max/min} 的边界夹取一个字未动</b>；调用点仍是
 * {@link StellarWaveEntity#createChildWave}（整份继承"能力"、按份均摊"物质"的语义不变）。</p>
 */
final class WaveChildSplit {

	private WaveChildSplit() {
		throw new AssertionError("This class should not be instantiated");
	}

	/** n 等份中的第 i 份（余数优先给靠前的子波）：{@code splitShare(5,1,2) == 2}、{@code splitShare(5,0,2) == 3}。 */
	static int splitShare(int amount, int index, int total) {
		if (amount <= 0)
			return 0;
		if (total <= 1)
			return amount;
		int base = amount / total;
		return base + (index < amount % total ? 1 : 0);
	}

	/**
	 * 载荷物品按"<b>每个子波拿到尽量不同的种类、数量尽量均匀</b>"发放（2026-09 用户口径）。
	 *
	 * <p><b>做法：把载荷摊平成"每件一个位置"的序列，子波 i 取所有 {@code 位置 % 子波数 == i}</b>
	 * （同一物料的若干件在序列里连续，于是被轮转着分给不同子波）。四条性质：</p>
	 * <ol>
	 *   <li><b>绝不凭空制造 / 丢失</b>：每件物品只落进一个子波，各子波之和恒等于母波原量；</li>
	 *   <li><b>种类最大化分散</b>：相邻物品总是分给不同子波，所以"每种各 1 件"时各子波拿到的是
	 *       <b>互不相同</b>的种类。旧实现按"每种数量等分 + 余数给靠前的子波"，会让<b>第 0 个子波
	 *       独吞全部种类、其余子波空手</b>（每种 count=1 时余数全落在 index 0）；</li>
	 *   <li><b>数量最均匀</b>：任一子波的件数与平均值的差不超过 1 件；</li>
	 *   <li><b>确定性</b>：每个子波各自调用都能独立算出自己那一份，无需在子波间共享状态
	 *       （分裂是"逐个开口调用 createChildWave"，没有统一的分发时机）。</li>
	 * </ol>
	 *
	 * <p>同一物料件数大于子波数时（例如 A×5、2 个子波）无法做到"种类互不相同"，此时退化为
	 * "该物料在各子波间尽量均匀"（3 / 2），仍有界且守恒。</p>
	 */
	static List<ItemStack> splitItems(List<ItemStack> items, int index, int total) {
		int n = Math.max(1, total);
		int me = Math.max(0, Math.min(index, n - 1));
		List<ItemStack> out = new ArrayList<>(items.size());
		int position = 0; // 摊平后的位置游标：第 p 件对应"第 p 个位置"
		for (ItemStack stack : items) {
			if (stack == null || stack.isEmpty())
				continue;
			int count = stack.getCount();
			int take = 0;
			for (int p = 0; p < count; p++)
				if ((position + p) % n == me)
					take++;
			position += count;
			if (take > 0)
				out.add(stack.copyWithCount(take));
		}
		return out;
	}
}
