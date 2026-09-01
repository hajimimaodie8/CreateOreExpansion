package com.hjmmd_8.createoreexpansion.content.wave.bridge;

/**
 * Sable 桥接注册表 —— 持有当前激活的 {@link SubLevelBridge} 实现。
 *
 * <p>无 Sable 环境：{@link #get()} 返回 null，调用方判空跳过（波正常按主世界逻辑运行）；<br>
 * 有 Sable 环境：主类反射加载 {@link SableSubLevelBridge} 后调用 {@link #set} 注册。</p>
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
