package com.hjmmd_8.createoreexpansion.content.wave.api;

import java.util.function.UnaryOperator;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * <b>一种魔素（essence）的完整声明 —— id 化、可注册（2026-10-24 波系统重构批 4）</b>。
 *
 * <p>改造前"魔素"只有 {@link WaveTrailStyle} 这个枚举：颜色档案是 {@code EnumMap<枚举, 档案>}、
 * 命中效果是 {@code switch (枚举)}，第三方<b>造不出第 12 个值</b>，也就什么都加不了。
 * 现在每个魔素是一条 {@link WaveEssence} 记录，住在 {@link WaveEssenceRegistry} 里，
 * 一行注册即可新增：</p>
 *
 * <pre>{@code
 * // 第三方（在自己的 @Mod 构造器里，RegisterEvent 之前）：
 * WaveEssenceRegistry.register(WaveEssence.of("yourmod:thunder")
 *     .color(0x9B5CFF)                  // 颜色（= 该魔素的颜色，D2 的"有魔素 ⇒ 魔素色"）
 *     .trailCount(8).trailScale(0.55f)  // 可选：拖尾颗数 / 尺度（不写 = 沿用波型档案的基线）
 *     .behavior(new MyThunderHit())     // 可选：命中行为（不写 = 只有颜色/粒子）
 *     .build());
 * }</pre>
 *
 * <h2>id 规则（老 NBT 逐字兼容，方案 §3.5 R-4）</h2>
 * <ul>
 *   <li><b>内置 11 个</b>：id <b>逐字等于枚举名</b>（{@code "FIRE"} / {@code "WATER"} …，
 *       含前三个波型风格 {@code NORMAL} / {@code MECHANICAL} / {@code DAMAGE}）。于是存档里
 *       {@code Essence:"FIRE"} 一个字节都不用改，读回来还是同一个魔素。</li>
 *   <li><b>第三方</b>：id 必须写成 {@code namespace:path}（含冒号）。不含冒号的名字一律按
 *       <b>内置枚举名</b>解析（这就是老存档的解析口径），所以第三方不能占用无冒号的名字 ——
 *       注册时直接拒绝，而不是让它永远读不回来。</li>
 *   <li><b>认不出的 id</b> ⇒ {@code null}（= 没有魔素，回落波型色），与改造前"未知名 → null"
 *       逐字一致。</li>
 * </ul>
 *
 * <h2>颜色（作者 D2 的读取端）</h2>
 * <p>{@link #colorOf(Vec3)} 是"这个魔素的颜色"的<b>唯一读取口</b>：注册时给了颜色就用它；
 * 没给颜色但挂在某个内置枚举值上，就用该枚举值在 {@code ChargerWaveFx} 调色板里的变换
 * （内置 11 个都是这样登记的 ⇒ 观感与批 3 逐字节相同）。</p>
 *
 * <p><b>为什么颜色是"变换"而不是"一个固定值"</b>：内置魔素的现有配色是"由波色亮度在两端点色之间
 * 取色"（水 = 深蓝→亮蓝、冰 = 蓝→淡蓝……），等级因此仍可读。第三方想要纯平色，就传
 * {@code .color(0xRRGGBB)}（= 忽略入参的常量变换），两条路都不需要第二套取色代码。</p>
 *
 * <p><b>与"波型色"的优先序</b>：{@link WaveColorRule#colorOf(WaveEssence, WaveType, Vec3)}
 * ——<b>有魔素 ⇒ 魔素色；没魔素 ⇒ 波型色；两者都没有 ⇒ 等级色</b>。本类不知道波型，也不知道等级。</p>
 */
public final class WaveEssence {

	/** 内置魔素的 id：逐字等于枚举名（见类注释的 id 规则）。 */
	private final String id;
	/** 内置魔素挂着的枚举值；第三方为 {@code null}。 */
	private final WaveTrailStyle builtin;
	/** 颜色变换；{@code null} = 没给颜色（回落等级色）。 */
	private final UnaryOperator<Vec3> color;
	/** 拖尾颗数覆盖；{@code null} = 用调用方基线（再由风格档案覆盖）。 */
	private final Integer trailCount;
	/** 拖尾尺度覆盖；{@code null} = 同上。 */
	private final Float trailScale;
	/** 命中行为；{@code null} = 只有表现（内置 11 个全部为 {@code null}）。 */
	private final WaveEssenceBehavior behavior;

	private WaveEssence(String id, WaveTrailStyle builtin, UnaryOperator<Vec3> color, Integer trailCount,
		Float trailScale, WaveEssenceBehavior behavior) {
		this.id = id;
		this.builtin = builtin;
		this.color = color;
		this.trailCount = trailCount;
		this.trailScale = trailScale;
		this.behavior = behavior;
	}

	/**
	 * <b>内置魔素的登记形态</b>（只给 {@link WaveEssenceRegistry} 用）：id 必须逐字等于枚举名。
	 *
	 * @param id    枚举名（{@code "FIRE"} …）
	 * @param style 该魔素挂着的枚举值
	 * @param color 该魔素的颜色变换（调用 {@code ChargerWaveFx.styleColor(style, base)}；
	 *              以 lambda 形式传入，登记时<b>不</b>触发粒子表初始化）
	 */
	static WaveEssence builtin(String id, WaveTrailStyle style, UnaryOperator<Vec3> color) {
		return new WaveEssence(id, style, color, null, null, null);
	}

	/** 第三方注册入口：{@code WaveEssence.of("yourmod:thunder")...build()}。 */
	public static Builder of(String id) {
		return new Builder(id);
	}

	/** 唯一 id（内置 = 枚举名；第三方 = {@code namespace:path}）。 */
	public String id() {
		return id;
	}

	/** 内置魔素挂着的枚举值；第三方返回 {@code null}（= 它没有枚举值，这正是 id 化的意义）。 */
	public WaveTrailStyle trailStyle() {
		return builtin;
	}

	/** 是否给了颜色（{@code false} = 最终回落等级色）。 */
	public boolean hasColor() {
		return color != null;
	}

	/**
	 * <b>本魔素的颜色</b>：给了颜色变换就用它，否则原样返回基底（等级色）。
	 *
	 * @param base 波的等级基色（RGB 0-1；{@code null} 视为黑）
	 */
	public Vec3 colorOf(Vec3 base) {
		Vec3 safe = base == null ? Vec3.ZERO : base;
		return color == null ? safe : color.apply(safe);
	}

	/** 拖尾颗数覆盖（{@code null} = 不覆盖）。 */
	public Integer trailCountOverride() {
		return trailCount;
	}

	/** 拖尾尺度覆盖（{@code null} = 不覆盖）。 */
	public Float trailScaleOverride() {
		return trailScale;
	}

	/** 命中行为（{@code null} = 没有自带行为，命中走既有 {@code WaveEssenceEffects} 表）。 */
	public WaveEssenceBehavior behavior() {
		return behavior;
	}

	/**
	 * 显示名：内置 ⇒ 既有魔素词条（{@link WaveTrailStyle#displayName()}，中英各一份、缺词条回退枚举名）；
	 * 第三方 ⇒ 自己的 id（扩展模组要本地化就覆写自己的呈现，本模组不替它造词条键）。
	 */
	public Component displayName() {
		return builtin != null ? builtin.displayName() : Component.literal(id);
	}

	@Override
	public String toString() {
		return "WaveEssence[" + id + "]";
	}

	/**
	 * 第三方注册的构建器。<b>颜色可选</b>（不写 = 回落等级色）；
	 * 颗数 / 尺度 / 行为都可选（不写 = 沿用既有表现与既有命中表）。
	 */
	public static final class Builder {

		private final String id;
		private UnaryOperator<Vec3> color;
		private Integer trailCount;
		private Float trailScale;
		private WaveEssenceBehavior behavior;

		private Builder(String id) {
			this.id = id;
		}

		/** 固定颜色（{@code 0xRRGGBB}）：不管波级多亮都是这个色 —— 第三方最常用的一行。 */
		public Builder color(int rgb) {
			this.color = WaveColorRule.fixedRgb(rgb);
			return this;
		}

		/** 固定颜色（RGB 0-1）。 */
		public Builder color(Vec3 fixed) {
			this.color = WaveColorRule.fixed(fixed);
			return this;
		}

		/** 颜色变换（入参 = 波的等级基色；返回该魔素的颜色）。 */
		public Builder color(UnaryOperator<Vec3> tint) {
			if (tint == null)
				throw new IllegalArgumentException("魔素 " + id + " 的颜色变换不能为 null（不想要颜色就别调 color(..)）");
			this.color = tint;
			return this;
		}

		/** 拖尾颗数覆盖（&gt; 0）。 */
		public Builder trailCount(int count) {
			if (count <= 0)
				throw new IllegalArgumentException("魔素 " + id + " 的拖尾颗数必须 > 0（收到 " + count + "）");
			this.trailCount = count;
			return this;
		}

		/** 拖尾尺度覆盖（&gt; 0）。 */
		public Builder trailScale(float scale) {
			if (!Float.isFinite(scale) || scale <= 0.0f)
				throw new IllegalArgumentException("魔素 " + id + " 的拖尾尺度必须为正且有限（收到 " + scale + "）");
			this.trailScale = scale;
			return this;
		}

		/** 命中行为（服务端命中链里调用；不写 = 只有表现）。 */
		public Builder behavior(WaveEssenceBehavior hitBehavior) {
			if (hitBehavior == null)
				throw new IllegalArgumentException("魔素 " + id + " 的行为不能为 null（不想要行为就别调 behavior(..)）");
			this.behavior = hitBehavior;
			return this;
		}

		public WaveEssence build() {
			return new WaveEssence(id, null, color, trailCount, trailScale, behavior);
		}
	}
}
