package com.hjmmd_8.createoreexpansion.content.wave.api;

/**
 * 波型对应的<b>拖尾／绽放视觉风格</b>。
 *
 * <p>与 {@link WaveType} 分开，是因为"能力"是玩法语义、会随平衡调整，而"风格"只是表现；
 * 扩展模组想复用既有风格就直接挑一个，想自定义则等本枚举扩展（当前三种已覆盖本模组全部波型）。</p>
 */
public enum WaveTrailStyle {

	/** 原生风格：波色随波级/电荷变化（普通波）。 */
	NORMAL,
	/** 机械感：金属灰/黄铜色调 + 电火花类粒子，凸显"读机器加工"的本质（全能波）。 */
	MECHANICAL,
	/** 伤害感：红/橙色调 + 暴击类粒子（攻击波）。 */
	DAMAGE
}
