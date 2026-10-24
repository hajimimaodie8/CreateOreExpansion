package com.hjmmd_8.createoreexpansion.content.wave.api;

/**
 * <b>一个波等级的完整数值声明（不可变）</b>：等级号不在本类里——等级号是
 * {@link WaveLevelRegistry#register(int, WaveLevelSpec)} 的第一个参数，注册表负责把
 * "等级号 ↔ 本声明" 配成一对，因此同一份声明可以被不同等级复用、也可以被整体替换（本批不做替换）。
 *
 * <p><b>7 个字段就是"加一档等级"以前必须改的 7 张表</b>（抽取前它们分别是
 * {@code WaveLevels} 里的 5 个 switch/数组 + {@code ChargingRecipe} 里那张外来表）：</p>
 * <table border="1">
 *   <caption>字段含义</caption>
 *   <tr><th>字段</th><th>含义</th><th>抽取前的旧位置</th></tr>
 *   <tr><td>{@link #glyph()}</td><td>对外显示的符号（内置 α/β/γ/ε/ω；不建议多字符）</td>
 *       <td>{@code WaveLevels.GLYPHS} 数组</td></tr>
 *   <tr><td>{@link #baseSpeed()}</td><td>基础飞行速度（格/秒）</td>
 *       <td>{@code WaveLevels.baseSpeed} 的 switch</td></tr>
 *   <tr><td>{@link #maxSpeed()}</td><td>该等级允许的最大速度（格/秒，波速调节器加速上限）</td>
 *       <td>{@code WaveLevels.maxSpeed} 的三元阈值 {@code >= EPSILON ? 12 : 10}</td></tr>
 *   <tr><td>{@link #damage()}</td><td>命中生物伤害（半颗心 × 2 = 满颗心计）</td>
 *       <td>{@code WaveLevels.damage} 的 switch</td></tr>
 *   <tr><td>{@link #indicatorColor()}</td><td>指示灯 / 能量条 / JEI 等级徽章共用的 ARGB</td>
 *       <td>{@code WaveLevels.indicatorColor} 的 switch</td></tr>
 *   <tr><td>{@link #goldTrail()}</td><td>是否叠 ω 的金色拖尾点缀（纯装饰色，<b>不是</b>波的渲染色）</td>
 *       <td>{@code AbstractChargerWaveEntity#isOmega()} 里写死的 {@code >= 5}</td></tr>
 *   <tr><td>{@link #chargeEnergy()}</td><td>该等级的波给物品 / 穿戴护甲充的点数</td>
 *       <td>{@code ChargingRecipe.energyForLevel} 的 switch（100/500/1000/1500/2000）</td></tr>
 * </table>
 *
 * <p><b>第三方注册第 6 级（一行）</b>：</p>
 * <pre>{@code
 * WaveLevelRegistry.register(6, WaveLevelSpec.builder("ζ")
 *     .baseSpeed(9.0d)           // 格/秒
 *     .maxSpeed(14.0d)           // 波速上限（>= baseSpeed）
 *     .damage(14.0f)             // 命中生物伤害
 *     .indicatorColor(0xFFB020)  // 指示灯 / 能量条 / JEI 徽章
 *     .goldTrail(false)          // 可选，缺省 false
 *     .chargeEnergy(2500)        // 可选，缺省 100（= α 档）
 *     .build());
 * }</pre>
 *
 * <p><b>校验口径</b>：构造期就把明显写错的声明挡掉（{@link IllegalArgumentException}）——
 * 符号不能为空、速度必须为正且有限、{@code maxSpeed >= baseSpeed}、伤害不能为负、
 * 充能点数不能为负。理由：这些值全部直接进玩法/存档读数，静默接受一个坏值只会变成
 * "某个等级莫名其妙飞不动 / 打不死人"，比注册当场报错难查得多。</p>
 *
 * <p><b>越界等级的回落由 {@link WaveLevels} 门面负责</b>（本类不认识"越界"这个概念）：
 * 查询未注册等级时，门面按改造前的旧口径回落（低档回落 / 阈值回落），逐值等价性写在
 * 每个门面方法的 javadoc 上。</p>
 */
public record WaveLevelSpec(String glyph, double baseSpeed, double maxSpeed, float damage,
	int indicatorColor, boolean goldTrail, int chargeEnergy) {

	public WaveLevelSpec {
		if (glyph == null || glyph.isBlank())
			throw new IllegalArgumentException("波等级符号（glyph）不能为空");
		if (!Double.isFinite(baseSpeed) || baseSpeed <= 0.0d)
			throw new IllegalArgumentException("波等级基础速度必须为正且有限（收到 " + baseSpeed + "）");
		if (!Double.isFinite(maxSpeed) || maxSpeed < baseSpeed)
			throw new IllegalArgumentException("波等级速度上限必须有限且 >= 基础速度（收到 maxSpeed=" + maxSpeed
				+ "，baseSpeed=" + baseSpeed + "）");
		if (!Float.isFinite(damage) || damage < 0.0f)
			throw new IllegalArgumentException("波等级伤害必须为非负有限值（收到 " + damage + "）");
		if (chargeEnergy < 0)
			throw new IllegalArgumentException("波等级充能点数不能为负（收到 " + chargeEnergy + "）");
	}

	/** 一行注册的可读入口：{@code WaveLevelSpec.builder("ζ").baseSpeed(..)....build()}。 */
	public static Builder builder(String glyph) {
		return new Builder(glyph);
	}

	/**
	 * {@link WaveLevelSpec} 的构建器：四个"核心"字段必须显式给出（漏写就在 {@link #build()} 报错，
	 * 不给静默默认值），两个"装饰/派生"字段有缺省（{@code goldTrail = false}、{@code chargeEnergy = 100}）。
	 */
	public static final class Builder {

		private final String glyph;
		private double baseSpeed = Double.NaN;
		private double maxSpeed = Double.NaN;
		private float damage = Float.NaN;
		private int indicatorColor = Integer.MIN_VALUE;
		private boolean goldTrail = false;
		private int chargeEnergy = 100;

		private Builder(String glyph) {
			this.glyph = glyph;
		}

		public Builder baseSpeed(double value) {
			this.baseSpeed = value;
			return this;
		}

		public Builder maxSpeed(double value) {
			this.maxSpeed = value;
			return this;
		}

		public Builder damage(float value) {
			this.damage = value;
			return this;
		}

		public Builder indicatorColor(int argb) {
			this.indicatorColor = argb;
			return this;
		}

		public Builder goldTrail(boolean value) {
			this.goldTrail = value;
			return this;
		}

		public Builder chargeEnergy(int value) {
			this.chargeEnergy = value;
			return this;
		}

		public WaveLevelSpec build() {
			if (Double.isNaN(baseSpeed))
				throw new IllegalStateException("WaveLevelSpec.builder(..) 缺少 baseSpeed(..)");
			if (Double.isNaN(maxSpeed))
				throw new IllegalStateException("WaveLevelSpec.builder(..) 缺少 maxSpeed(..)");
			if (Float.isNaN(damage))
				throw new IllegalStateException("WaveLevelSpec.builder(..) 缺少 damage(..)");
			if (indicatorColor == Integer.MIN_VALUE)
				throw new IllegalStateException("WaveLevelSpec.builder(..) 缺少 indicatorColor(..)");
			return new WaveLevelSpec(glyph, baseSpeed, maxSpeed, damage, indicatorColor, goldTrail, chargeEnergy);
		}
	}
}
