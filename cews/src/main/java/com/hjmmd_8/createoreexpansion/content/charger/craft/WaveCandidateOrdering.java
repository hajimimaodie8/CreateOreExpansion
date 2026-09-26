package com.hjmmd_8.createoreexpansion.content.charger.craft;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveMachineIntegrations;

import net.minecraft.resources.ResourceLocation;

/**
 * 变体波的<b>候选排序策略</b>：同一输入同时匹配多条配方时，决定先试哪一条。
 *
 * <p><b>纯策略、零状态</b>：本类不持有任何字段，全部入参显式传入——LRU 记忆（"该输入上次成功的
 * 配方"）由调用方保管并作为 {@code rememberedId} 传入，转速档由调用方按波携带的变器转速算出后
 * 传入。这样排序规则可以脱离实体单独推理与验证。</p>
 *
 * <p>顺序规则：{@code preferred}（本次方块命中的批锁）→ 该输入种类最近一次成功的配方
 * （调用方的有界 LRU）→ <b>转速档匹配者</b> → <b>专用度最高者</b>。</p>
 */
public final class WaveCandidateOrdering {

	private WaveCandidateOrdering() {
	}

	/**
	 * 从候选集中挑选本次要执行的候选（= {@link #order} 的首位）。
	 *
	 * @param rememberedId 该输入种类最近一次成功的配方 id（可 null）
	 * @param waveSpeedMode 波携带的变器转速档（见 {@code StellarWaveMachineIntegrations}）
	 */
	public static Candidate pick(List<Candidate> candidates, Candidate preferred, ResourceLocation rememberedId,
		int waveSpeedMode) {
		List<Candidate> ordered = order(candidates, preferred, rememberedId, waveSpeedMode);
		return ordered.isEmpty() ? null : ordered.get(0);
	}

	/**
	 * 候选排序：把本次要执行的那条排在最前，其余按"<b>转速档匹配 → 专用度</b>"降序
	 * （见 {@link #compareCandidates} / {@link #compareSpecificity}）。
	 *
	 * <p><b>任何情况下都不再随机抽取</b>。原实现无锁时用 {@code random.nextInt} 随机挑一条，
	 * 而"同一输入同时匹配多条配方"恰恰是最常见的场景：铜锭既匹配"压成铜板"（1 输入）
	 * 又匹配"铜+锌→黄铜"（2 输入，需加热）；下界合金锭既匹配"辊成下界合金板"（1 输入）
	 * 又匹配"下界合金锭+钻石工具→下界合金工具"（2 输入）。随机抽取让后者大概率抽不到，
	 * 玩家看到的就是"放齐了料却搓不出黄铜／升级不生效"（用户 2026-09 实测反馈的两例）。</p>
	 */
	public static List<Candidate> order(List<Candidate> candidates, Candidate preferred, ResourceLocation rememberedId,
		int waveSpeedMode) {
		List<Candidate> rest = new ArrayList<>(candidates);
		List<Candidate> ordered = new ArrayList<>(candidates.size());
		if (preferred != null) {
			Candidate locked = byId(rest, preferred.id);
			if (locked != null) {
				ordered.add(locked);
				rest.remove(locked);
			}
		}
		if (ordered.isEmpty()) {
			Candidate locked = byId(rest, rememberedId);
			if (locked != null) {
				ordered.add(locked);
				rest.remove(locked);
			}
		}
		rest.sort((a, b) -> compareCandidates(a, b, waveSpeedMode));
		ordered.addAll(rest);
		return ordered;
	}

	/**
	 * 候选比较（排序用，越小越优先）：<b>① 转速档匹配优先</b> → ② 专用度（{@link #compareSpecificity}）。
	 *
	 * <p>① 与 Vintage 自身口径一致：抛光配方 {@code speed_limits} 匹配当前转速档的进优选桶，
	 * 一条都不匹配时才用兜底桶（{@code GrinderBlockEntity} 的桶选择逻辑，去混淆字节码核对）。
	 * 这里用的是波携带的<b>变器转速</b>算出的档位；无转速要求的配方一律与"不匹配"同档位
	 * （Vintage 里它们同样落在兜底桶），于是不影响其它任何排序结论。</p>
	 */
	public static int compareCandidates(Candidate a, Candidate b, int waveSpeedMode) {
		int bySpeedBand = Integer.compare(speedBandRank(a, waveSpeedMode), speedBandRank(b, waveSpeedMode));
		if (bySpeedBand != 0)
			return bySpeedBand;
		return compareSpecificity(a, b);
	}

	/** 候选的转速档排序档位：0 = 与波转速档匹配（优先）；1 = 无要求或档位不匹配（兜底）。 */
	public static int speedBandRank(Candidate candidate, int waveSpeedMode) {
		int required;
		try {
			required = StellarWaveMachineIntegrations.recipeSpeedMode(candidate.recipe);
		} catch (Throwable ignored) {
			return 1; // 读取异常：按兜底处理，绝不让联动异常影响排序
		}
		if (required <= 0)
			return 1;
		return required == waveSpeedMode ? 0 : 1;
	}

	/**
	 * 候选"专用度"比较（越小越优先）：
	 * <ol>
	 *   <li><b>物品输入数多者优先</b>——2 输入的搅拌/升级压过 1 输入的压制/辊压；</li>
	 *   <li>其次流体输入数多者优先；</li>
	 *   <li>最后按配方数据包 id 字典序——保证"同种输入 → 同一种产物"，不随 random 抖动。</li>
	 * </ol>
	 * 若玩家确实要走"不那么专用"的那条（例如只想把铜锭压成铜板），用工作盆的配方过滤器
	 * 把目标产物列进去即可：过滤器闸门在候选集生成之后、排序之前生效，语义不变。
	 */
	public static int compareSpecificity(Candidate a, Candidate b) {
		int byItems = Integer.compare(b.recipe.getIngredients()
			.size(), a.recipe.getIngredients()
				.size());
		if (byItems != 0)
			return byItems;
		int byFluids = Integer.compare(b.fluids.size(), a.fluids.size());
		if (byFluids != 0)
			return byFluids;
		String idA = a.id == null ? "" : a.id.toString();
		String idB = b.id == null ? "" : b.id.toString();
		return idA.compareTo(idB);
	}

	/** 按配方数据包 id 在候选集中找同一候选（找不到 = 该配方当前已不可执行 → null）。 */
	public static Candidate byId(List<Candidate> candidates, ResourceLocation id) {
		if (id == null)
			return null;
		for (Candidate c : candidates)
			if (id.equals(c.id))
				return c;
		return null;
	}
}
