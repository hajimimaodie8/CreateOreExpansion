package com.hjmmd_8.createoreexpansion.common.structure;

/**
 * Sable 桥接注册表 —— 持有当前激活的 {@link SubLevelBridge} 实现。
 *
 * <p>无 Sable 环境：{@link #get()} 返回 null，调用方判空跳过（波正常按主世界逻辑运行）；<br>
 * 有 Sable 环境：主类反射加载
 * {@link com.hjmmd_8.createoreexpansion.compat.sable.SableSubLevelBridge} 后调用 {@link #set} 注册。</p>
 *
 * <p><b>为什么住 {@code common/structure}（P3e 搬迁）</b>：本类与 {@link SubLevelBridge} 讲的是
 * <b>Aeronautics / Sable 的物理结构</b>（局部坐标、位姿矩阵、方向变换），与"波"无关，
 * 只是历史上随手放在了 {@code content/wave/bridge/}。按路径那属于 CEWS 层，于是 COE 的技能 AOE
 * 策略（{@code integration.skiller.strategy.CoeAreaAoeStrategy}）一 import 它就凭空多出一条
 * 禁止方向 COE→CEWS。整包挪到 SHARED 后分层判定才诚实。</p>
 *
 * <p>刻意<b>不</b>放进 {@code compat/sable/}：那边的契约是"引用可选模组类型、只在装了 Sable 时
 * 由 {@code Class.forName} 加载"，而本类与 {@link SubLevelBridge} 零可选模组类型、任何时候都被
 * CEWS 内容与 COE 技能直接 import（AGENTS 的红线是"content 包不得 import 可选模组类"，
 * 把常加载类型塞进 {@code compat/sable} 会让这条红线无法机械核验）。可选模组的真正实现
 * （{@code SableSubLevelBridge} / {@code SablePose} / {@code SableSubLevelAccess}）仍住 {@code compat/sable/}。</p>
 */
public final class SableBridges {

	private static volatile SubLevelBridge instance;

	private SableBridges() {
	}

	/** 注册桥接实现（仅 Sable 存在时调用）。 */
	public static void set(SubLevelBridge bridge) {
		instance = bridge;
	}

	/** 当前桥接实现；null = Sable 未安装（调用方必须判空）。 */
	public static SubLevelBridge get() {
		return instance;
	}
}
