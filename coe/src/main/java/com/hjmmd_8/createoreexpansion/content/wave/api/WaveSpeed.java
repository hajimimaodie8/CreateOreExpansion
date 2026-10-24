package com.hjmmd_8.createoreexpansion.content.wave.api;

/**
 * <b>能量波速度算术的唯一实现</b>：等级基础速度 + 速度修正，再夹在"全局下限 ~ 该等级上限"之间。
 *
 * <p><b>为什么要有它</b>：改造前同一段夹取算式在 {@code AbstractChargerWaveEntity} 里写了两遍
 * ——{@code getSpeedBlocks()}（读服务端 {@code double} 字段，供位移 / 扫掠盒 / 加速场计时）
 * 与 {@code getWaveSpeed()}（读同步来的 {@code float} 副本，供 Jade / 查询仪）——
 * 两处对<b>同一枚波</b>算出的速度会差一个 float 舍入。本类把算术收成一份；两个调用点只保留
 * "值从哪种介质读"的差别：服务端读权威 {@code double} 字段、客户端只有 {@code float} 同步值。
 * 服务端两处从此逐位相同（详见 {@code AbstractChargerWaveEntity#speedOffsetForRead}）。</p>
 *
 * <p><b>数值口径（逐字等于改造前，改这里就是改玩法）</b>：</p>
 * <ul>
 *   <li>基础速度 = {@link WaveLevels#baseSpeed(int)}（α 2 / β 4 / γ 6 / ε 7 / ω 8 格/秒）；</li>
 *   <li>速度修正 = 叠加值（波速调节器 / 加速场），语义见实体 {@code addSpeedOffset(double)}；</li>
 *   <li>上限 = {@link WaveLevels#maxSpeed(int)}（1~3 级 10、4/5 级 12；未注册等级按
 *       {@link WaveLevels} 的回落口径）；</li>
 *   <li>下限 = {@link #MIN_SPEED} = 0.5 格/秒（<b>全局一个值</b>，不是每个等级一项）。</li>
 * </ul>
 *
 * <p><b>夹取顺序与运算符逐字保留</b>：先 {@code Math.min(上限, 基础 + 修正)}、再
 * {@code Math.max(下限, ·)}。不要改成 {@code Mth.clamp}：两者对 {@code NaN} 的行为不同
 * （{@code Math.min/max} 传播 {@code NaN}，{@code Mth.clamp} 会把它压成边界），而
 * {@code speedOffset} 是外部叠加值，改造前就是"{@code NaN} 照样传播"。</p>
 *
 * <p><b>谁在读</b>：实体位移（{@code getSpeedBlocks()}）、能量场速度向量
 * （{@code fieldVelocity} / {@code setFieldVelocity}）、Jade 波速行与查询仪读数
 * （{@code getWaveSpeed()} → {@link WaveAccess#speedPerSecond}）。<b>任何新增的"波速"点位
 * 都必须调本方法</b>，不得再抄一份 {@code Math.max(MIN_SPEED, Math.min(...))} ——
 * 关卡 {@code wave-speed-single-source} 守着这一条。</p>
 */
public final class WaveSpeed {

	/**
	 * 速度下限（格/秒）：减速（负的 {@code speedOffset}）不能把波压到比这更慢。
	 *
	 * <p>改造前是 {@code AbstractChargerWaveEntity#MIN_SPEED}（{@code protected static final}，
	 * 子类也改不了）；本批移到本类，字形与数值都没变 —— 它仍是<b>全局</b>下限，不是每级一项。
	 * 把它放在这里而不是注册表里：全仓 5 个等级共用一个下限，抄进 5 行注册项只会让"改下限"
	 * 从 1 处变成 5 处。</p>
	 */
	public static final double MIN_SPEED = 0.5d;

	private WaveSpeed() {
	}

	/**
	 * 实际速度（格/秒）= {@code clamp(等级基础速度 + 速度修正, MIN_SPEED, 该等级上限)}。
	 *
	 * @param level       波等级（见 {@link WaveLevels}；未注册等级按门面的回落口径）
	 * @param speedOffset 速度修正叠加值（格/秒，可正可负；0 = 无修正）
	 */
	public static double perSecond(int level, double speedOffset) {
		return Math.max(MIN_SPEED, Math.min(WaveLevels.maxSpeed(level), WaveLevels.baseSpeed(level) + speedOffset));
	}
}
