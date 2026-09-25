package com.hjmmd_8.createoreexpansion.util;

import java.util.Locale;

/**
 * <b>转速分档表的唯一实现</b>（2026-09 抽象整理：消灭"每台机器各写一份内联三元 + 魔法数字"）。
 *
 * <p>存在意义：本模组有若干处"把转速切成几档、每档对应一个值"的口径，历史写法是把阈值与档值
 * 直接内联在三元表达式里（{@code speed >= 128 ? 3 : 2} 这种）。那样写的三个后果：</p>
 * <ul>
 *   <li>阈值与档值散在判定点，<b>读数层看不见表</b>，只能把数字再抄一遍（抄歪了面板与判定就分家）；</li>
 *   <li>"第几档""该档的下限/上限""该档对应的值"三个问题没有共同答案，谁问谁重算；</li>
 *   <li>调整区间（例如把 256 改成 320、三档改四档）要顺着调用点一个个改，漏一处就是暗错。</li>
 * </ul>
 * <p>本类把这三件事收成<b>一个对象</b>：<b>判定与显示共用同一张表</b>，表变了文案自动跟着变。</p>
 *
 * <p><b>口径（务必先读，历史上这里被误读过一次）</b>：</p>
 * <ul>
 *   <li><b>按绝对值判定</b>：{@link #indexAt(float)} 内部取 {@code Math.abs(rpm)}——反转的传动轴
 *       与正向同档，与全仓既有的 {@code Math.abs(getSpeed())} 口径一致；</li>
 *   <li><b>域从 0 RPM 起</b>：转速是绝对值域，所以第一档（下标 {@code 0}）的下限必须 {@code ≥ 0}；
 *       <b>低于第一档下限的转速一律落在第一档</b>（向下夹紧），超过最后一档下限的落在最后一档
 *       （向上夹紧）——分档表对转速全域有定义，调用方永远不需要自己夹；</li>
 *   <li><b>档位序号 0 基</b>：第一档 = 下标 {@code 0}；玩家看到的"第 N 档"由显示层 {@code +1}
 *       （唯一加工点，见 {@code TransmuterGoggles}）；</li>
 *   <li><b>下限含、上限不含</b>：第 {@code i} 档 = {@code [lowerRpm(i), lowerRpm(i+1))}，
 *       最后一档 = {@code [lowerRpm(count-1), topRpm]}（{@link #topRpm()} 可为
 *       {@link Float#POSITIVE_INFINITY}，表示最后一档无上界）。</li>
 * </ul>
 *
 * <p><b>非法表立刻抛异常</b>：下限不严格升序、数组长度不等、长度为 0、档值为负、上限不大于下限
 * 都在构造时抛 {@link IllegalArgumentException}。<b>刻意不静默纠正</b>——分档表算错的表现是
 * "机器行为与面板读数各自都对不上"，那种 bug 极难从现象反推，宁可在构造时就炸掉。</p>
 *
 * <p><b>中立性</b>：只用 JDK（{@link Math} 与 {@link String#format} 等），不 import 任何模组类，
 * 可被 {@code content} 包自由使用（可选联动的隔离约定见 AGENTS.md）。</p>
 */
public final class SpeedBands {

	/** 每档的<b>下限转速</b>（RPM，含），严格升序。 */
	private final float[] lowerRpm;

	/** 每档对应的值（长度与 {@link #lowerRpm} 相同）。 */
	private final int[] values;

	/** 最后一档的<b>上限转速</b>（RPM，含）；{@link Float#POSITIVE_INFINITY} = 无上界。 */
	private final float topRpm;

	/**
	 * 构造一张分档表（唯一的构造入口，校验全在这里）。
	 *
	 * @param lowerRpm 每档下限（RPM，含）：长度 ≥ 1、每项 {@code ≥ 0}、严格升序
	 * @param values   每档的值：长度与 {@code lowerRpm} 相同、每项 {@code ≥ 0}
	 * @param topRpm   最后一档的上限（RPM，含）；{@link Float#POSITIVE_INFINITY} = 无上界
	 * @throws IllegalArgumentException 表非法（校验项见类注释，刻意不静默纠正）
	 */
	private SpeedBands(float[] lowerRpm, int[] values, float topRpm) {
		if (lowerRpm == null || values == null)
			throw new IllegalArgumentException("转速分档表：下限/档值数组不得为 null");
		if (lowerRpm.length != values.length)
			throw new IllegalArgumentException("转速分档表：下限数组与档值数组长度不等（" + lowerRpm.length
				+ " vs " + values.length + "）");
		if (lowerRpm.length == 0)
			throw new IllegalArgumentException("转速分档表：至少要有一档");
		// 第一档从 0 起：转速按绝对值判定，域是 [0, ∞)。第一档的下限允许 > 0（此时"低于第一档
		// 下限"的转速向下夹紧进第一档，例如攻击场的第一档从 128 RPM 起、"没转到 128"仍算第一档），
		// 但不允许为负——那意味着表定义了 0 RPM 以下这个不存在的区间。
		if (!(lowerRpm[0] >= 0.0F))
			throw new IllegalArgumentException("转速分档表：第一档下限必须 ≥ 0 RPM（转速域从 0 起），实际 "
				+ lowerRpm[0]);
		for (int i = 1; i < lowerRpm.length; i++)
			if (!(lowerRpm[i] > lowerRpm[i - 1]))
				throw new IllegalArgumentException("转速分档表：下限必须严格升序，第 " + (i - 1) + " 档 "
					+ lowerRpm[i - 1] + " → 第 " + i + " 档 " + lowerRpm[i] + " 不是升序");
		if (!Float.isInfinite(topRpm) && !(topRpm > lowerRpm[lowerRpm.length - 1]))
			throw new IllegalArgumentException("转速分档表：上限 " + topRpm + " 必须大于最后一档下限 "
				+ lowerRpm[lowerRpm.length - 1]);
		for (int i = 0; i < values.length; i++)
			if (values[i] < 0)
				throw new IllegalArgumentException("转速分档表：第 " + i + " 档的档值不得为负，实际 " + values[i]);
		this.lowerRpm = lowerRpm.clone();
		this.values = values.clone();
		this.topRpm = topRpm;
	}

	// ================= 工厂 =================

	/**
	 * <b>线性等分表</b>：把 {@code [minimumRpm, maximumRpm]} 等分成 {@code count} 档，
	 * 每档的值从 {@code baseValue} 起逐档 {@code +1}。
	 *
	 * <p>攻击场用的就是这一支：{@code linear(128, 256, 3, 0)} → 三个档
	 * {@code [128, 170.67) / [170.67, 213.33) / [213.33, 256]}，档值（= 场盒半径）{@code 0/1/2}。
	 * 把 256 改成 320、或把 3 改成 4，<b>只有这一句工厂调用要改</b>：档位边界、每档半径、
	 * 护目镜上"第 N 档（下限 ~ 上限 RPM，半径 M 格）"与三档对照表全部由本表派生。</p>
	 *
	 * @param minimumRpm 第一档下限（RPM）；必须 {@code ≥ 0 且 < maximumRpm}
	 * @param maximumRpm 最后一档上限（RPM，含）
	 * @param count      档数（≥ 1）
	 * @param baseValue  第一档的档值；第 {@code i} 档的档值 = {@code baseValue + i}
	 * @throws IllegalArgumentException 参数非法（见类注释的校验清单）
	 */
	public static SpeedBands linear(float minimumRpm, float maximumRpm, int count, int baseValue) {
		if (count < 1)
			throw new IllegalArgumentException("转速分档表：档数必须 ≥ 1，实际 " + count);
		if (!(maximumRpm > minimumRpm))
			throw new IllegalArgumentException("转速分档表：上限 " + maximumRpm + " 必须大于下限 " + minimumRpm);
		float width = (maximumRpm - minimumRpm) / count;
		float[] lowers = new float[count];
		int[] vals = new int[count];
		for (int i = 0; i < count; i++) {
			lowers[i] = minimumRpm + width * i;
			vals[i] = baseValue + i;
		}
		return new SpeedBands(lowers, vals, maximumRpm);
	}

	/**
	 * <b>显式表</b>（最后一档无上界）：逐档给出下限转速与档值。
	 *
	 * <p>用于"只是几个阈值"的表——例如能量场半径分档
	 * （{@code of(new float[]{0, 128}, new int[]{2, 3})} = {@code |转速| < 128 → 2}、
	 * {@code ≥ 128 → 3}）。这类表没有"最后一档的上限"这个概念，故 {@link #upperRpm(int)}
	 * 对最后一档返回 {@link Float#POSITIVE_INFINITY}。</p>
	 *
	 * @param lowerRpm 每档下限（RPM，含）：长度 ≥ 1、每项 {@code ≥ 0}、严格升序
	 * @param values   每档的值：长度与 {@code lowerRpm} 相同、每项 {@code ≥ 0}
	 * @throws IllegalArgumentException 表非法（见类注释的校验清单）
	 */
	public static SpeedBands of(float[] lowerRpm, int[] values) {
		return new SpeedBands(lowerRpm, values, Float.POSITIVE_INFINITY);
	}

	/**
	 * <b>显式表</b>（给出最后一档的上限）：语义同 {@link #of(float[], int[])}，
	 * 只是最后一档也有明确上限（{@link #upperRpm(int)} 取它）。
	 *
	 * @param lowerRpm 每档下限（RPM，含）：长度 ≥ 1、每项 {@code ≥ 0}、严格升序
	 * @param values   每档的值：长度与 {@code lowerRpm} 相同、每项 {@code ≥ 0}
	 * @param topRpm   最后一档的上限（RPM，含）；必须大于最后一档下限
	 * @throws IllegalArgumentException 表非法（见类注释的校验清单）
	 */
	public static SpeedBands of(float[] lowerRpm, int[] values, float topRpm) {
		return new SpeedBands(lowerRpm, values, topRpm);
	}

	// ================= 查询 =================

	/** 档数（{@code ≥ 1}）。 */
	public int count() {
		return values.length;
	}

	/**
	 * <b>转速落在第几档</b>（本类唯一一处分档判定）——按 {@code Math.abs(rpm)} 判，
	 * 低于第一档下限夹到 {@code 0}、高于最后一档下限夹到 {@code count() - 1}。
	 *
	 * @param rpm 转速（RPM，<b>可能为负</b>；本方法内部取绝对值）
	 * @return 档位序号 {@code 0 ~ count()-1}（0 基；0 = 第一档）
	 */
	public int indexAt(float rpm) {
		float abs = Math.abs(rpm);
		for (int i = values.length - 1; i >= 1; i--)
			if (abs >= lowerRpm[i])
				return i;
		return 0;
	}

	/**
	 * <b>转速对应的值</b>：{@code values[indexAt(rpm)]}。
	 *
	 * @param rpm 转速（RPM，可能为负）
	 * @return 该档的值（档值非负，见构造校验）
	 */
	public int valueAt(float rpm) {
		return values[indexAt(rpm)];
	}

	/**
	 * <b>第 index 档的值</b>——显示层按下标逐档列举（例如三档对照表）时的入口，
	 * 与 {@link #valueAt(float)} 读的是同一张表。
	 *
	 * @param index 档位序号（0 基）
	 * @return 该档的值
	 * @throws IndexOutOfBoundsException 下标越界
	 */
	public int valueOfIndex(int index) {
		checkIndex(index);
		return values[index];
	}

	/**
	 * <b>第 index 档的下限转速</b>（RPM，含）。
	 *
	 * @param index 档位序号（0 基）
	 * @return 下限（RPM）
	 * @throws IndexOutOfBoundsException 下标越界
	 */
	public float lowerRpm(int index) {
		checkIndex(index);
		return lowerRpm[index];
	}

	/**
	 * <b>第 index 档的上限转速</b>（RPM，<b>不含</b>）——等于下一档的下限；
	 * 最后一档取 {@link #topRpm()}（{@link Float#POSITIVE_INFINITY} = 无上界）。
	 *
	 * <p>本方法刻意返回"不含"的上限语义：区间文案写成 {@code 下限 ~ 上限} 时，相邻两档共享的
	 * 那个分界值只出现一次、不会同时被两档占用，玩家照着读数换档不会有歧义。</p>
	 *
	 * @param index 档位序号（0 基）
	 * @return 上限（RPM）
	 * @throws IndexOutOfBoundsException 下标越界
	 */
	public float upperRpm(int index) {
		checkIndex(index);
		return index + 1 < lowerRpm.length ? lowerRpm[index + 1] : topRpm;
	}

	/** 最后一档的上限（RPM，含）；{@link Float#POSITIVE_INFINITY} = 无上界。 */
	public float topRpm() {
		return topRpm;
	}

	// ================= 文本 =================

	/**
	 * <b>RPM 的显示文本</b>——本模组转速文案的<b>唯一格式化实现</b>（护目镜 / 分档区间文案都读它）。
	 *
	 * <p>规则：保留一位小数，<b>整数不显示多余的 {@code .0}</b>——{@code 128 → "128"}、
	 * {@code 170.666 → "170.7"}、{@code 213.333 → "213.3"}、{@code 256 → "256"}。
	 * 分档边界通常是 {@code (max-min)/档数} 算出来的循环小数，一位小数既能区分相邻档，
	 * 又不会把面板挤成 {@code 170.66666666666666}。整数表（如 128/256）仍旧显示得干净。</p>
	 *
	 * <p>{@link Locale#ROOT} 固定小数点：某些语言环境用逗号做小数分隔符，那会让 RPM 区间
	 * 文案与该 locale 的列表分隔符撞在一起（无法区分"170,7 ~ 213,3"里的逗号）。</p>
	 *
	 * @param rpm 转速（RPM）
	 * @return 显示文本（整数无小数位，否则一位小数）
	 */
	public static String formatRpm(float rpm) {
		if (!Float.isFinite(rpm))
			return rpm > 0 ? "∞" : "-∞";
		// 先按一位小数四舍五入，再让整数值退化成整数文本（%.1f 无法直接省略 .0）
		String text = String.format(Locale.ROOT, "%.1f", rpm);
		return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
	}

	private void checkIndex(int index) {
		if (index < 0 || index >= values.length)
			throw new IndexOutOfBoundsException("转速分档表：档位下标越界 " + index + "（档数 " + values.length + "）");
	}
}
