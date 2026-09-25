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
 *       {@link Float#POSITIVE_INFINITY}，表示最后一档无上界）。判定就用这个半开口径
 *       （{@link #indexAt(float)}）；<b>显示</b>要的是互不重叠的闭区间，读
 *       {@link #upperInclusiveRpm(int)}（非末档 = 下一档下限 − 1）或对末档改用"≥下限"的写法。</li>
 *   <li><b>下限一律是整数 RPM</b>（用户 2026-09 口径："转速调成整数 RPM，不要用小数"）：
 *       {@link #linear(float, float, int, int)} / {@link #equalSteps(float, float, int, int)}
 *       两个工厂都会把算出来的下限量化成整数；{@link #of(float[], int[])} 由调用方直接给整数表。
 *       <b>量化后的那一张表就是判定与显示共用的表</b>——不存在"显示 171 而 170.5 已进第二档"
 *       这类错位，因为两者读的是同一个 {@code lowerRpm}。</li>
 * </ul>
 *
 * <p><b>非法表立刻抛异常</b>：下限不严格升序、数组长度不等、长度为 0、档值为负、上限不大于下限，
 * 以及<b>工厂量化成整数下限后档位不再严格升序</b>（档距不足 1 RPM）都在构造时抛
 * {@link IllegalArgumentException}。<b>刻意不静默纠正</b>——分档表算错的表现是
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
	 * <p><b>下限量化为整数</b>（用户 2026-09 口径：档位边界不出现小数）：算出的
	 * {@code minimumRpm + width × i} 一律<b>向上取整</b>（{@link Math#ceil(double)}）。
	 * 攻击场用的就是这一支：{@code linear(128, 256, 3, 0)} → 三个档
	 * {@code [128, 171) / [171, 214) / [214, 256]}，档值（= 场盒半径）{@code 0/1/2}；
	 * 显示成闭区间就是 {@code 128~170 / 171~213 / ≥214}（{@link #upperInclusiveRpm(int)} 给上界）。</p>
	 *
	 * <p><b>为什么必须向上取整（而不是四舍五入/向下取整）</b>：量化只该改"显示与判定的精度"，
	 * 不该改玩法。对整数转速 {@code s} 与任意实数边界 {@code b} 有
	 * {@code s ≥ ceil(b) ⟺ s ≥ b}——所以向上取整之后，<b>每一个整数转速落在哪一档都与量化前逐点相同</b>
	 * （反例：四舍五入把 {@code 213.33 → 213}，于是 213 RPM 从第二档被挪进第三档，攻击场半径
	 * 1→2 直接变了玩法；向下取整同理会在另一侧挪档）。非整数转速（齿轮比带小数时才会出现）
	 * 的归档仍可能相差不到 1 RPM 的一小段，这是"整数 RPM"口径的必然结果。</p>
	 *
	 * <p>把 256 改成 320、或把 3 改成 4，<b>只有这一句工厂调用要改</b>：档位边界、每档半径、
	 * 护目镜上"第 N 档（下限 ~ 上限 RPM，半径 M 格）"全部由本表派生。</p>
	 *
	 * @param minimumRpm 第一档下限（RPM）；必须 {@code ≥ 0 且 < maximumRpm}
	 * @param maximumRpm 最后一档上限（RPM，含）
	 * @param count      档数（≥ 1）
	 * @param baseValue  第一档的档值；第 {@code i} 档的档值 = {@code baseValue + i}
	 * @throws IllegalArgumentException 参数非法，或量化后档位不再严格升序（见类注释的校验清单）
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
			// 量化（唯一加工点）：判定与显示读的都是这份整数下限，所以两边不可能错位。
			// 用 ceil 而非 round/floor——只有 ceil 能保证整数转速的归档与量化前逐点一致（理由见 javadoc）。
			lowers[i] = (float) Math.ceil(minimumRpm + width * i);
			vals[i] = baseValue + i;
		}
		checkQuantizedAscending(lowers, "linear(" + minimumRpm + ", " + maximumRpm + ", " + count + ")");
		return new SpeedBands(lowers, vals, maximumRpm);
	}

	/**
	 * <b>整数等分表</b>（下限向上取整，末档自 {@code maximumRpm} 起、无上界）：
	 * 第 {@code i} 档下限 = {@code minimumRpm + ceil((maximumRpm − minimumRpm) × i / (count − 1))}，
	 * 于是首档自 {@code minimumRpm} 起、末档下限恰为 {@code maximumRpm}（{@link #topRpm()} = +∞）。
	 *
	 * <p>等于"把 {@code [minimumRpm, maximumRpm)} 等分成 {@code count − 1} 段，再在其上追加一档
	 * {@code ≥ maximumRpm}"。应力充能器用的就是这一支：翡翠
	 * {@code equalSteps(1, 256, 3, 1)} → {@code 1 / 129 / 256}（α 1~128、β 129~255、γ ≥256），
	 * 蓝宝石 {@code equalSteps(1, 256, 5, 1)} → {@code 1 / 65 / 129 / 193 / 256}。</p>
	 *
	 * <p><b>为什么是向上取整而不是四舍五入</b>：充能器的旧实现是
	 * {@code speed <= max/2 → 1}、{@code floor((speed−1)/(max−1)×4)+1} 这类实数分界，
	 * 整数转速下等价于"分界值向上取整"。取整方向一改（例如四舍五入），
	 * 整数转速的档位就会跟着变——那是<b>玩法侧</b>行为（充能档位决定 blockstate {@code MODE}），
	 * 本次重构不允许。向上取整则保证整数转速逐个不变（有逐点对照的验证程序为证）。</p>
	 *
	 * @param minimumRpm 第一档下限（RPM）；必须 {@code ≥ 0 且 < maximumRpm}（充能器传 1）
	 * @param maximumRpm 末档下限（RPM，含）；它以上的转速都落在末档
	 * @param count      档数（≥ 2）
	 * @param baseValue  第一档的档值；第 {@code i} 档的档值 = {@code baseValue + i}
	 * @throws IllegalArgumentException 参数非法，或量化后档位不再严格升序（见类注释的校验清单）
	 */
	public static SpeedBands equalSteps(float minimumRpm, float maximumRpm, int count, int baseValue) {
		if (count < 2)
			throw new IllegalArgumentException("转速分档表：整数等分表的档数必须 ≥ 2（末档自上限起），实际 " + count);
		if (!(maximumRpm > minimumRpm))
			throw new IllegalArgumentException("转速分档表：上限 " + maximumRpm + " 必须大于下限 " + minimumRpm);
		float[] lowers = new float[count];
		int[] vals = new int[count];
		for (int i = 0; i < count; i++) {
			double offset = (maximumRpm - minimumRpm) * (double) i / (count - 1);
			lowers[i] = minimumRpm + (float) Math.ceil(offset);
			vals[i] = baseValue + i;
		}
		checkQuantizedAscending(lowers, "equalSteps(" + minimumRpm + ", " + maximumRpm + ", " + count + ")");
		return new SpeedBands(lowers, vals, Float.POSITIVE_INFINITY);
	}

	/**
	 * <b>工厂量化后的自检</b>：下限一旦被取整，档距不足 1 RPM 的表就可能出现重复下限
	 * （例如 {@code linear(0, 3, 4, 0)} 量化出 {@code 0/1/2/2}）。这里提前抛出带"档距不足"
	 * 提示的异常——否则只会得到构造函数那句"下限必须严格升序"，看不出是量化造成的。
	 */
	private static void checkQuantizedAscending(float[] lowers, String table) {
		for (int i = 1; i < lowers.length; i++)
			if (!(lowers[i] > lowers[i - 1]))
				throw new IllegalArgumentException("转速分档表：" + table + " 量化成整数下限后第 " + (i - 1)
					+ " 档 " + lowers[i - 1] + " 与第 " + i + " 档 " + lowers[i]
					+ " 不再严格升序（档距不足 1 RPM：请拉开上下限或减少档数）");
	}

	/**
	 * <b>显式表</b>（最后一档无上界）：逐档给出下限转速与档值。
	 *
	 * <p>用于"只是几个阈值"的表——例如能量场半径分档
	 * （{@code of(new float[]{0, 128}, new int[]{2, 3})} = {@code |转速| < 128 → 2}、
	 * {@code ≥ 128 → 3}）。这类表没有"最后一档的上限"这个概念，故 {@link #upperRpm(int)}
	 * 对最后一档返回 {@link Float#POSITIVE_INFINITY}。</p>
	 *
	 * <p><b>本工厂不做量化</b>：下限由调用方直接给出（口径见类注释——工厂负责算，本工厂负责收），
	 * 调用方应当给整数 RPM（全仓现有调用点给的都是整数）。</p>
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

	/**
	 * <b>第 index 档的"闭区间"上界</b>（RPM，<b>含</b>）——<b>显示层</b>把区间写成不重叠的
	 * {@code 下限 ~ 上界} 时读它：值 = 下一档下限 {@code − 1}（本表的档距是整数 1 RPM，
	 * 所以"下一档的第一个转速减一"就是本档的最后一个转速）。
	 *
	 * <p>与判定口径的关系：判定是半开的（{@link #indexAt(float)} 用
	 * {@link #lowerRpm(int)} 比较），闭区间只是把同一个数换一种说法——
	 * 例如 {@code lowers = {128, 171, 213}} 时第 0 档判 {@code [128, 171)}、显示 {@code 128~170}，
	 * 两档之间不重不漏。<b>末档没有这样的上界</b>（{@link #upperRpm(int)} 可能为
	 * {@link Float#POSITIVE_INFINITY}），显示层对末档改用"≥下限"的写法，故本方法对末档
	 * 原样返回 {@link #topRpm()}（调用方不必读它）。</p>
	 *
	 * @param index 档位序号（0 基）
	 * @return 闭区间上界（RPM）；末档 = {@link #topRpm()}（可能为 +∞）
	 * @throws IndexOutOfBoundsException 下标越界
	 */
	public float upperInclusiveRpm(int index) {
		checkIndex(index);
		return index + 1 < lowerRpm.length ? lowerRpm[index + 1] - 1.0F : topRpm;
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
	 * {@code 170.7 → "170.7"}、{@code 256 → "256"}。档位边界经 {@link #linear} /
	 * {@link #equalSteps} 量化后都是整数，所以区间文案里不会再出现小数位；保留一位小数这条
	 * 规则只服务"非档位"的转速读数（例如某处的实时转速）。{@link Locale#ROOT} 固定小数点：
	 * 某些语言环境用逗号做小数分隔符，那会让 RPM 区间文案与该 locale 的列表分隔符撞在一起。</p>
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
