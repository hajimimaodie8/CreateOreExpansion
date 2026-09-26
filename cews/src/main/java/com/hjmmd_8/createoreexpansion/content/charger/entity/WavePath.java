package com.hjmmd_8.createoreexpansion.content.charger.entity;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 一只能量波的<b>最近路径折线</b>（旧 → 新，最多 {@link #POINT_COUNT} 个落点，逐段带"是否断点"标记）。
 *
 * <p><b>解决什么问题</b>：攻击场（{@code TransmuterMode.ATTACK}）要判断"这只波到底有没有从场盒里
 * 穿过"，而场是<b>周期性观测</b>的（节拍见 {@code TransmuterMode#fieldIntervalTicks()}）。只拿
 * "上一 tick 位置 → 当前位置"一条线段的话，两次观测之间的 {@code 节拍 − 1} tick 完全没有被任何
 * 线段覆盖：波只要在那段空档里<b>擦过场盒的棱/角</b>（盒内弦长远短于棱长、停留不足 1 tick），
 * 就一次都不会被点燃。本类把最近若干 tick 的实际落点留成折线，观测者一次就能拿到自上次观测以来的
 * <b>全部位移</b>——覆盖不再取决于"节拍与波速的赛跑"。</p>
 *
 * <p><b>状态放在波身上（信息专家原则）</b>："波从哪儿飞到哪儿"是波自己的事，不该让每台变器各存一份
 * 波位置（那是 N×M 的交叉状态，还要处理波消散/换维度时的清理）。波每 tick 压一个点（O(1)，无分配），
 * 观测者只问一句 {@link #crosses}——攻击场那边依旧只是"一次实体查询 + 若干次线段判定"，
 * <b>没有每 tick 方块遍历</b>。</p>
 *
 * <p><b>外力搬运（瞬移）必须断开——而且是"立刻"断</b>：机器把波挪位置时（撞机器后推出方块外、
 * 原路遣返、结构坐标换算等），那一跳不是"飞过去的"；若当成位移线段，攻击场会把整段瞬移路径上的
 * 场盒都误算成"穿过"。这里有两处要堵，缺一不可：</p>
 * <ol>
 *   <li><b>已经压入的那一段</b>：下一次 {@link #push} 把新落点标记为断点，于是
 *       "瞬移前落点 → 瞬移后落点"这一段被跳过（瞬移<b>之前</b>的合法历史照旧保留——
 *       把整条折线清掉会连带丢掉"瞬移前刚穿过场"的记录，那是一次真穿越）；</li>
 *   <li><b>还没压入的"尾点 → 实时位置"那一段</b>：瞬移可能发生在一次观测之后、下一次压点之前，
 *       此时观测者看到的"最新落点 → 实时位置"仍然是那一跳。所以这一跳必须<b>当场</b>生效：
 *       波实体在 {@code setPos} 里置一个标志并把它作为 {@link #crosses} 的 {@code liveBroken}
 *       参数传进来，这一段随即被跳过，而不是等到下一次压点。
 *       （2026-09 验证脚本实测：只做第 1 条时，瞬移误判样本一个都没减少。）</li>
 * </ol>
 *
 * <p><b>为什么不给本类加 {@code markLiveBreak()} setter</b>：那个写法踩过一个真实的坑——波实体在
 * {@code setPos} 里调用它，而 {@code setPos} 会被<b>父类 {@code Entity} 的构造器</b>调用
 * （{@code Entity.<init>} 内部 {@code setPos(0,0,0)}），那一刻字段初始化器还没跑、本对象还是 null，
 * 于是开炮即 NPE（{@code crash-2026-09-14_09.47.56-server.txt}）。现在瞬移标志是波实体自己的
 * <b>原始类型字段</b>（构造期赋值天然安全），本类只被"构造完成之后"的代码触碰。</p>
 *
 * <p><b>出生同理</b>：波出生那一刻由构造函数 {@code setPos} 触发同一个标记，第一段因此从出生点开始，
 * 折线起点不会是实体默认坐标 (0,0,0)（2026-09 前的旧实现读 {@code xo/yo/zo}，对刚出生的波就是原点，
 * 于是"从原点飞到当前位置"那条假线段会横跨半个世界）。</p>
 *
 * <p><b>与观测节拍的耦合（唯一一处，两个方向都写了注释）</b>：折线跨度
 * {@link #MAX_OBSERVATION_GAP_TICKS} tick 必须 ≥ 任何观测者的观测间隔，所以该常量同时是
 * "观测间隔"的硬上限——攻击场的节拍按它夹一次（{@code TransmuterMode.fieldIntervalTicks()} 里的
 * {@code Math.min}）。公式算出的节拍将来被放宽（例如波速表大改）也不会漏：节拍永远不超过折线跨度。</p>
 *
 * <p><b>折线是近似</b>：相邻落点之间按直线连接，而带电波在偏转场里走的是弧线，故真实轨迹与折线之间
 * 有极小的弦差（每 tick 偏转量级远小于场盒尺寸）。对本判定的语义（"有没有进过这个盒子"）而言这个
 * 弦差可以忽略；要更精确就得每 tick 记曲率，收益不成比例。</p>
 *
 * <p><b>实证（蒙特卡洛脚本，2026-09）</b>：用真实 {@code WavePath} + 原版 {@code AABB} 跑 3 万条随机
 * 弹道（速度 0.05~1.0 格/tick、持续弯折、5% 概率瞬移），以"模拟飞行的真实轨迹"为真值：</p>
 * <ul>
 *   <li>与旧判定（只看"上一 tick → 当前"）逐观测点对比：<b>旧为真而新为假 = 0</b>（严格不回归），
 *       旧为假而新为真 = 2006 次（都是擦棱/擦角落在观测空档里，正是本类要补的漏）；</li>
 *   <li>节拍扫描（对<b>两种观测时机</b>各测一遍）：观测恰好落在采样之后（此时"实时段"是零长度，
 *       覆盖最短）→ 安全上限 <b>4</b>；观测落在两次采样之间（实时段有效，多覆盖一段）→ 安全上限 5。
 *       取最坏的那个，故 {@link #MAX_OBSERVATION_GAP_TICKS} = 4——它是<b>实测出来的紧上限</b>，
 *       不是估的；</li>
 *   <li>瞬移误判样本：只做"下次压点时断开"时一个都没减少（76 → 76），补上
 *       "实时段当场断开"（本类 {@code liveBroken} 参数）后才归零——本文件开头那两条缺一不可。</li>
 * </ul>
 */
public final class WavePath {

	/**
	 * 落点数（含最新一点）：5 点 = 4 段位移 = <b>4 tick 跨度</b>。
	 *
	 * <p>取值依据：攻击场的观测间隔现表为 2 tick（{@code TransmuterMode.shortestSafeFieldInterval}），
	 * 留一倍余量；同时 5 个引用（{@code Vec3}）的内存开销对"同时存在的波数量"量级可忽略。</p>
	 */
	public static final int POINT_COUNT = 5;

	/**
	 * 观测者观测间隔（tick）的硬上限 = 折线跨度 {@code POINT_COUNT − 1}。
	 *
	 * <p>攻击场的节拍按此值夹紧（{@code Math.min}）：<b>观测间隔不得超过折线跨度</b>，
	 * 否则两次观测之间会重新出现"没被任何线段覆盖"的空档，本类的意义就被抹掉了。</p>
	 */
	public static final int MAX_OBSERVATION_GAP_TICKS = POINT_COUNT - 1;

	/** 折线落点（下标 0 = 最旧）。 */
	private final Vec3[] points = new Vec3[POINT_COUNT];

	/** 逐段断点标记：{@code brokenBefore[i] = true} 表示"点 i−1 → 点 i"这一段不是飞行位移，判定时跳过。 */
	private final boolean[] brokenBefore = new boolean[POINT_COUNT];

	/** 当前有效落点数（0 ~ {@link #POINT_COUNT}）。 */
	private int size;

	/**
	 * 压入一个落点（<b>只由波实体每 tick 调用</b>：包内可见，外部只能查询，改不了轨迹）。
	 *
	 * @param point       本 tick 结束时的实际位置
	 * @param breakBefore 与该点之前的落点之间<b>不是飞行位移</b>（机器瞬移 / 出生）→ 这一段判定时跳过
	 */
	void push(Vec3 point, boolean breakBefore) {
		if (point == null)
			return;
		if (size == POINT_COUNT) {
			System.arraycopy(points, 1, points, 0, POINT_COUNT - 1);
			System.arraycopy(brokenBefore, 1, brokenBefore, 0, POINT_COUNT - 1);
			size--;
		}
		brokenBefore[size] = breakBefore;
		points[size++] = point;
	}

	/**
	 * <b>折线是否与给定盒相交</b>：任一段<b>有效</b>位移线段穿过盒子，或任一落点落在盒内即算。
	 *
	 * <p>{@code live} = 波形<b>当前实时位置</b>，作为折线最后一点参与判定：观测发生在 tick 中间时，
	 * 本 tick 已经走出的位移同样要算进去（否则又会出现"最后一个 tick 没人管"的小空档）。
	 * {@code liveBroken} = 自上次采样以来发生过外力搬运 → 最后这一段（尾点 → 实时位置）只做
	 * "点是否在盒内"的判定，不按线段算。</p>
	 *
	 * <p><b>为什么把"实时段是否断"做成参数，而不是本类的一个 setter</b>：波实体要在
	 * {@code setPos} 里上报瞬移，而 {@code setPos} 会被<b>父类构造器</b>调用（见波实体里的说明），
	 * 那一刻本对象还不存在。改由调用方在查询时把标志传进来，本类就只被"构造完成之后"的代码触碰。</p>
	 *
	 * <p>本方法只回答几何问题，不判断波型、不改任何状态——"是不是普通波、要不要点燃"由调用方决定。</p>
	 */
	public boolean crosses(AABB box, Vec3 live, boolean liveBroken) {
		if (box == null)
			return false;
		Vec3 previous = null;
		for (int i = 0; i < size; i++) {
			Vec3 point = points[i];
			if (box.contains(point))
				return true;
			if (previous != null && !brokenBefore[i] && segmentCrosses(box, previous, point))
				return true;
			previous = point;
		}
		if (live == null)
			return false;
		if (box.contains(live))
			return true;
		return previous != null && !liveBroken && segmentCrosses(box, previous, live);
	}

	/**
	 * 一条位移线段是否与盒相交。
	 *
	 * <p><b>为什么用 {@link AABB#clip(Vec3, Vec3)}</b>：它是原版的线段裁剪工具（返回入面交点，
	 * 无交点返回空），语义正是"这条线段与盒有没有相交"；而 {@link AABB#intersects(Vec3, Vec3)} 只是
	 * "线段包围盒与盒粗略重叠"，斜擦而过、甚至只在拐角附近路过的线段都会被判成相交——那正是本判定
	 * 要排除的误判，故不能用。</p>
	 *
	 * <p><b>{@code contains} 那一支不可省</b>：{@code clip} 内部的点裁剪只接受 {@code 0 < t < 1} 的
	 * 入面交点，起点已在盒内时返回空；少了这一支，"上一落点就已在盒内"的波会全部漏判
	 * ——那恰恰是半径小、波速慢时最常见的情形。</p>
	 */
	private static boolean segmentCrosses(AABB box, Vec3 from, Vec3 to) {
		return box.contains(from) || box.clip(from, to)
			.isPresent();
	}
}
