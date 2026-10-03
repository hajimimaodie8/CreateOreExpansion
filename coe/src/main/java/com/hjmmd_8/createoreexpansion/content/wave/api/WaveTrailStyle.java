package com.hjmmd_8.createoreexpansion.content.wave.api;

/**
 * 波型对应的<b>拖尾／绽放视觉风格</b>。
 *
 * <p>与 {@link WaveType} 分开，是因为"能力"是玩法语义、会随平衡调整，而"风格"只是表现；
 * 扩展模组想复用既有风格就直接挑一个，想自定义则等本枚举扩展。</p>
 *
 * <p><b>2026-10-03（需求 coe-ess）：攻击波的「魔素」就是这个枚举的后八个值</b>——
 * {@link #WATER}／{@link #FIRE}／{@link #EARTH}／{@link #WIND}／{@link #ICE}／
 * {@link #LIGHTNING}／{@link #POISON}／{@link #ARCANE}（水/火/地/风/冰/雷/毒/异）。
 * 魔素是<b>纯视觉</b>属性（不碰伤害/抗性/相克，一律由波级决定），每个值在
 * {@code ChargerWaveFx.buildProfiles()} 里各有一份档案；其中 {@link #FIRE} 与
 * {@link #DAMAGE} 是<b>同一份档案实例</b>（火 = 现状锚点：默认产生的攻击波都是有火魔素的，
 * 所以"火"必须逐字节等于改动前的攻击波观感）。</p>
 *
 * <p>⚠ <b>前三个值的声明序不许动</b>（{@code NORMAL} / {@code MECHANICAL} / {@code DAMAGE}）：
 * 新增只能往 {@link #DAMAGE} 之后追加。</p>
 */
public enum WaveTrailStyle {

	/** 原生风格：波色随波级/电荷变化（普通波）。 */
	NORMAL,
	/** 机械感：金属灰/黄铜色调 + 电火花类粒子，凸显"读机器加工"的本质（全能波）。 */
	MECHANICAL,
	/** 伤害感：红/橙色调 + 暴击类粒子（攻击波）。 */
	DAMAGE,
	/** 魔素·水：蓝白调 + 气泡 / 气泡破裂 / 水花类粒子。 */
	WATER,
	/**
	 * 魔素·火：<b>与 {@link #DAMAGE} 共用同一份档案实例</b>（不是复制一份字面量——
	 * 复制就是漂移源）。默认魔素即火 ⇒ 既有攻击波观感零变化。
	 */
	FIRE,
	/** 魔素·地：泥土棕调 + 泥土 / 草方块类粒子。 */
	EARTH,
	/** 魔素·风：青白调 + 阵风（{@code SMALL_GUST}）粒子。 */
	WIND,
	/** 魔素·冰：淡蓝调 + 细雪 / 雪块 / 雪球类粒子。 */
	ICE,
	/** 魔素·雷：紫白调 + 电火花，另配<b>低概率</b>强闪光（{@code FLASH}，只挂主波）。 */
	LIGHTNING,
	/** 魔素·毒：黄绿调 + 药水（{@code ENTITY_EFFECT}）粒子。 */
	POISON,
	/**
	 * 魔素·异：星界套<b>伴随波（环绕波）原来那一套</b>粒子——12 颗 / 尺度 0.62 +
	 * {@code END_ROD} + 青焰 {@code SOUL_FIRE_FLAME}。
	 *
	 * <p>⚠ <b>不含环面留痕桩</b>：那是"这枚波在环绕"这个几何事实专属（见
	 * {@code ChargerWaveFx.sendOrbitMarks} 与 {@code burstOrbitSpawn}），主波没有环平面，
	 * 塞进风格档案里要么崩、要么画出一圈没有意义的桩点。</p>
	 */
	ARCANE
}
