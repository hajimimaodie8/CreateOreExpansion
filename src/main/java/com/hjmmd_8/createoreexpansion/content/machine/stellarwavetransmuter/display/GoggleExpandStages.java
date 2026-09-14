package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.display;

import net.minecraft.core.BlockPos;

/**
 * 护目镜面板的<b>分档展开状态</b>（"瞄准只给机器名 → 按住 Shift 给概要 → 再按住一次给全部读数"）。
 *
 * <p><b>用户口径（2026-09-14 实测反馈）</b>：变器的护目镜面板默认就是一整坨（模式 / 半径 / 加热 /
 * 设备台数 / 加工机 / 转速 / 应力 / 载荷 / 配方类型清单…），玩家用空手点侧面调波口时，
 * <b>那个框正好盖住他要看的那一面</b>。要求改成三档：</p>
 * <ol>
 *   <li><b>瞄准（没按 Shift）</b>：只显示"星辉波变器"一行——框小到不会挡住任何东西；</li>
 *   <li><b>按住 Shift</b>：<b>概要</b>（模式 / 半径 / 加工机数 / 转速 / 应力）；</li>
 *   <li><b>松开后再按住一次 Shift</b>（仍瞄着同一台）：<b>全部读数</b>（加热 / 设备台数 / 载荷 /
 *       配方类型清单 / 最近波…）。</li>
 * </ol>
 *
 * <p><b>为什么是"按住才显示"而不是"按一下锁定"</b>：用户第一句是"只有按住 Shift 蹲下时才会出现"，
 * 所以松手即回到第 0 档（只剩名字）；但"再次按住"要能直接给全量，故<b>按了几次</b>这个计数
 * 在"同一台机器 + 手没离开"期间保留（{@link #visibleStage} 的 {@code presses}）。
 * 于是三档的体感是：松手 → 收起；连按两次 → 全量。</p>
 *
 * <p><b>状态<b>不该</b>放在方块实体里</b>：它纯粹是"某玩家此刻的面板展开档位"——不是世界状态，
 * 不落盘、不同步、多人下各看各的。故用一个客户端侧的小状态机：每帧由面板调用一次
 * {@link #visibleStage}，内部只记"目标方块 / 上帧是否按住 / 已按次数 / 上次调用时刻"。</p>
 *
 * <p><b>为什么只依赖 {@link BlockPos} 与 {@code System.currentTimeMillis}</b>：本类被放在 content 层
 * （display 子包）而不是 client 层——那样 content 就不必反向依赖 client。它也不引用任何客户端类型，
 * 在专用服务器上只是永不调用的死状态；调用点（Create 的护目镜浮层）本来就只在客户端跑。</p>
 *
 * <p><b>重置条件（缺一不可）</b>：① 目标方块变了（换看别的机器）；② 距上次调用超过
 * {@link #RESET_GAP_MS}（说明玩家已经移开视线——浮层停止调用本类，于是"瞄回去"应当从第 0 档重新开始）。
 * 只做 ① 的话，"看 A → 移开 → 再看 A"会保留上次的全量展开，与"瞄准只给名字"的口径矛盾。</p>
 */
public final class GoggleExpandStages {

	/** 第 0 档：只显示机器名（瞄准即此档）。 */
	public static final int NAME_ONLY = 0;

	/** 第 1 档：概要（按住 Shift 一次）。 */
	public static final int SUMMARY = 1;

	/** 第 2 档：全部读数（再按住 Shift 一次）。 */
	public static final int FULL = 2;

	/**
	 * 视线离开该方块多久算"重新开始"（毫秒）。
	 *
	 * <p>护目镜浮层只在"瞄着带护目镜信息的方块"那几帧调用本类，移开视线就不再调用；
	 * 取 500 ms（≈10 tick）——比"手抖一下移开又移回"长，比"玩家真的换目标再换回来"短。</p>
	 */
	private static final long RESET_GAP_MS = 500L;

	/** 当前目标方块（null = 还没瞄过任何方块）。 */
	private static BlockPos target;

	/** 上一帧是否按着 Shift（上升沿检测：只有"新按下"才算一次展开）。 */
	private static boolean lastSneak;

	/** 在"同一台机器 + 手没离开"期间累计的按住次数（1 = 概要，≥2 = 全量）。 */
	private static int presses;

	/** 上次被调用的时刻（毫秒；用于判"视线是否已离开"）。 */
	private static long lastCallMs;

	private GoggleExpandStages() {
	}

	/**
	 * 本帧应当显示到第几档（{@link #NAME_ONLY} / {@link #SUMMARY} / {@link #FULL}）。
	 *
	 * @param pos      当前瞄着的方块位置（护目镜浮层给的就是它）
	 * @param sneaking 玩家此刻是否按住 Shift（Create 传入的 {@code player.isShiftKeyDown()}）
	 * @return 可见档位：没按住 Shift 恒为 {@link #NAME_ONLY}（松手即收起）
	 */
	public static int visibleStage(BlockPos pos, boolean sneaking) {
		long now = System.currentTimeMillis();
		boolean sameTarget = pos != null && pos.equals(target) && now - lastCallMs <= RESET_GAP_MS;
		if (!sameTarget) {
			target = pos == null ? null : pos.immutable();
			// 换目标时若正按着 Shift：算作"本机的第 1 次按住"——否则玩家得先抬手再按才有概要，
			// 而"一边按着 Shift 一边把准心移过去"是最常见的操作顺序。
			presses = sneaking ? 1 : 0;
			lastSneak = sneaking;
			lastCallMs = now;
			return sneaking ? SUMMARY : NAME_ONLY;
		}
		if (sneaking && !lastSneak)
			presses = Math.min(FULL, presses + 1); // 新的按下 = 展开下一档（封顶全量）
		lastSneak = sneaking;
		lastCallMs = now;
		return sneaking ? Math.max(SUMMARY, presses) : NAME_ONLY;
	}
}
