package com.hjmmd_8.createoreexpansion.content.charger.wave;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;

/**
 * 能量波系统的<b>诊断日志出口</b>——全系统<b>唯一</b>一处日志前缀与开关定义。
 *
 * <p><b>为什么要有这个类</b>：波的行为横跨实体（{@code StellarWaveEntity}）、命中解析
 * （{@code WaveHitResolver}）、机器（{@code TransmuterMode}）与穿波转换
 * （{@code StellarWaveTransmuterPass}）四处，各方都要写同一本"轨迹流水账"。以前前缀字面量
 * 与开关常量写在实体里、只有实体自己能写日志，于是"波在变器那里到底被当成什么波处理"这类
 * 跨类事件在日志里完全不可见（玩家反馈"改了波型，第一发还是老样子"时只能靠猜）。
 * 现在前缀与开关各只有一处，谁都能写，排查时看一个开关即可。</p>
 *
 * <p><b>两个通道的量级口径</b>（这是分两个开关的全部理由）：</p>
 * <ul>
 *   <li>{@link #trace}「{@value #TRACE_TAG}」= <b>事件流</b>：每次真实发生的动作最多一行
 *       （加工成功 / 穿波转换 / 场点燃 / 注液 / 引雷…）。量级与"发生了多少事"成正比，
 *       <b>与配方库规模无关</b>，所以默认开启；</li>
 *   <li>{@link #debug}「{@value #DEBUG_TAG}」= <b>逐条淘汰</b>：每条配方为什么被挡
 *       （类型未携带 / 流体不足 / 环境不满足…）。量级与配方库规模成正比，会刷屏，默认关闭。</li>
 * </ul>
 *
 * <p>两者都是 {@code static final boolean} 常量：关掉时 {@code javac} 会把整块调用消除，
 * 运行时零开销；改动开关需重新编译（这正是"排查完把它改回去"的用法）。</p>
 */
public final class WaveDiag {

	/** 轨迹通道前缀（事件流；按主题取名，见类注释）。 */
	public static final String TRACE_TAG = "[变体波轨迹] ";

	/** 详细诊断通道前缀（逐条淘汰原因）。 */
	public static final String DEBUG_TAG = "[变体波加工] ";

	/** 轨迹通道开关（默认开：每次真实动作最多一行，量级可控）。 */
	public static final boolean TRACE = true;

	/** 详细诊断通道开关（默认关：量级与配方库规模成正比）。 */
	public static final boolean DEBUG = false;

	/** 工具类，不允许实例化。 */
	private WaveDiag() {}

	/**
	 * 写一行<b>轨迹</b>日志（事件流；默认开启）。
	 *
	 * @param msg  消息模板（{@code {}} 占位，同 slf4j）
	 * @param args 模板参数（<b>数量必须与占位符一致</b>，否则日志只打出模板本身）
	 */
	public static void trace(String msg, Object... args) {
		if (TRACE)
			CreateOreExpansion.LOGGER.info(TRACE_TAG + msg, args);
	}

	/**
	 * 写一行<b>逐条淘汰</b>诊断日志（默认关闭；量与配方库规模成正比）。
	 *
	 * @param msg  消息模板（{@code {}} 占位）
	 * @param args 模板参数（数量须与占位符一致）
	 */
	public static void debug(String msg, Object... args) {
		if (DEBUG)
			CreateOreExpansion.LOGGER.info(DEBUG_TAG + msg, args);
	}
}
