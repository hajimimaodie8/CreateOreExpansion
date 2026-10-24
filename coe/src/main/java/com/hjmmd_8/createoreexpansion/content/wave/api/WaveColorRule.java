package com.hjmmd_8.createoreexpansion.content.wave.api;

import java.util.function.UnaryOperator;

import net.minecraft.world.phys.Vec3;

/**
 * <b>波的颜色规则 —— D2 的唯一实现处（2026-10-24 波系统重构批 4）</b>。
 *
 * <p>作者 2026-10-24 的原话：</p>
 * <blockquote>
 * 颜色应该由模数和波的类型共同定义。获取波的颜色时优先采样模数：如果有模数，则返回模数所对应的颜色；
 * 如果没有模数，则返回波的类型所对应的颜色。
 * </blockquote>
 *
 * <p>落成代码就是本类的 {@link #colorOf(WaveEssence, WaveType, Vec3)}，<b>全仓只此一处</b>：</p>
 * <ol>
 *   <li><b>有魔素</b> ⇒ {@link WaveEssence#colorOf(Vec3)}（该魔素登记的颜色）；</li>
 *   <li><b>没魔素</b> ⇒ {@link WaveType#color()}（该波型登记的颜色）；</li>
 *   <li><b>两者都没有</b> ⇒ 等级基色（{@link WaveLevels#particleColor(int)} 给的
 *       {@code levelColor}）。</li>
 * </ol>
 *
 * <p><b>"等级色"现在的角色（本批的裁定，报告里有理由）</b>：它<b>不再是颜色决策者</b>
 * （色相由魔素 / 波型决定），只作为<b>基底</b>传给 ①② 的颜色变换、以及③的兜底。
 * 内置的 11 个魔素与 3 个波型的颜色变换都以它为明度输入 —— 所以"α 黄 / β 绿 / γ 蓝 / ε 紫粉 /
 * ω 玫红"这套等级可读性<b>逐值保留</b>（改前 = 改后）；第三方注册纯平色
 * （{@link WaveEssence.Builder#color(int)} / {@link #fixed(Vec3)}）时它会被忽略，
 * 那时同一个魔素在所有等级上就是同一个色 —— 这是注册方的选择。</p>
 *
 * <p><b>⚠ 作者裁定的读法（2026-10-24，逐字："B"）—— 不许"简化"成纯平色</b>：
 * <b>色相来自魔素 / 波型，明度仍按等级</b>。"有魔素 ⇒ 魔素色；没魔素 ⇒ 波型色；都没给 ⇒ 回落等级色"
 * 是唯一优先序；等级色降级为<b>基底 / 兜底</b>，不是颜色决策者。
 * <b>"纯平色"（读法 A：魔素/波型各一个固定色、完全忽略等级）已被作者明确否决</b>，
 * 理由 = <b>等级会读不出来</b>（α 与 ω 变成同一个色）。以后谁想"简化"成本方法的 ①/② 直接返回
 * 一个常量、把等级基色丢掉，请先回看这条裁定 —— 那不是简化，是改玩法观感。</p>
 *
 * <p><b>本类不做什么</b>：不查等级表、不认识波实体、不做 lerp（渐变仍在实体 tick 里，
 * 因为那是"这一帧往目标色靠多少"的运行态，不是颜色规则）、不碰粒子构造。</p>
 */
public final class WaveColorRule {

	private WaveColorRule() {
		throw new AssertionError("This class should not be instantiated");
	}

	/**
	 * <b>一枚波当前该是什么颜色（D2 的唯一实现）</b>：魔素优先，其次波型，最后等级色。
	 *
	 * <p>⚠ <b>判据顺序就是 D2 的优先序，不许调换</b>：关卡 {@code wave-color-rule} 直接读
	 * 本方法体的分支顺序（魔素分支必须在波型分支之前），并把"函数体里第一句判魔素"钉住。</p>
	 *
	 * @param essence    这枚波的魔素（{@code null} = 没有魔素）；见
	 *                   {@code AbstractChargerWaveEntity#essenceSpec()}
	 * @param type       这枚波的波型（{@code null} = 没有波型，按没有处理）
	 * @param levelColor 等级基色（{@code WaveLevels.particleColor(level)}；{@code null} 视为黑）
	 * @return 粒子 / 拖尾 / 绽放 / 爆炸共用的渲染色（RGB 0-1）
	 */
	public static Vec3 colorOf(WaveEssence essence, WaveType type, Vec3 levelColor) {
		Vec3 base = levelColor == null ? Vec3.ZERO : levelColor;
		// ① 有魔素 ⇒ 用魔素对应的颜色（D2 的第一优先）
		if (essence != null)
			return essence.colorOf(base);
		// ② 没有魔素 ⇒ 用波型对应的颜色（波型注册项自带；内置三个 = 原回退风格的变换）
		if (type != null)
			return type.colorOf(base);
		// ③ 魔素与波型都没给颜色 ⇒ 兜底用等级色
		return base;
	}

	/** 固定颜色的颜色变换（不管基底多亮都返回同一个色）—— 第三方注册"纯平色"用。 */
	public static UnaryOperator<Vec3> fixed(Vec3 color) {
		Vec3 fixed = color == null ? Vec3.ZERO : color;
		return base -> fixed;
	}

	/**
	 * {@code 0xRRGGBB} 形式的固定颜色（各通道 /255；与显示层的
	 * {@code ChargerWaveFx#styleColorRgb} 的打包方向相反，但那一个是"Vec3 → int"、
	 * 本方法是"int → Vec3"，两者都不是颜色表，故不构成第二份真源）。
	 */
	public static UnaryOperator<Vec3> fixedRgb(int rgb) {
		double r = ((rgb >> 16) & 0xFF) / 255.0D;
		double g = ((rgb >> 8) & 0xFF) / 255.0D;
		double b = (rgb & 0xFF) / 255.0D;
		return fixed(new Vec3(r, g, b));
	}
}
