package com.hjmmd_8.createoreexpansion.content.wave;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 能量波等级元数据集中表（1~5 级）。
 *
 * <p>等级谱系（蓝宝石科技线扩展后，括号内为对外显示的希腊字母符号）：</p>
 * <ul>
 *   <li><b>1 级 = α（阿尔法，LOW）</b>：基础速度 2 格/秒（翡翠线最低档）；</li>
 *   <li><b>2 级 = β（贝塔，HIGH）</b>：4 格/秒；</li>
 *   <li><b>3 级 = γ（伽马，GAMMA）</b>：6 格/秒；</li>
 *   <li><b>4 级 = ε（伊普西隆，EPSILON）</b>：7 格/秒，紫粉色（蓝宝石线专属，翡翠线封顶 3 级）；</li>
 *   <li><b>5 级 = ω（欧米伽，OMEGA）</b>：8 格/秒，主体红色、拖尾/粒子金色（仅蓝宝石 256 RPM 充能器产出）。</li>
 * </ul>
 *
 * <p><b>4/5 级速度上限提升至 12 格/秒</b>（波速调节器可加速到的上限），
 * 1~3 级保持原上限 10 格/秒 —— 本表统一提供 {@link #maxSpeed(int)}。</p>
 *
 * <p>所有机器/波实体/渲染/Jade/JEI 均查本表，避免等级数值散落各处 switch。</p>
 *
 * <p><b>等级 → 符号</b>：对外一律只显示希腊字母（{@link #glyph(int)} / {@link #displayName(int)}），
 * 不显示"低/高/伽马/伊普西龙/欧米伽"这类文字——映射实现仅此一处，任何显示点都不得再写 switch。</p>
 */
public final class WaveLevels {

	/** 1 级：α（阿尔法） */
	public static final int LOW = 1;
	/** 2 级：β（贝塔） */
	public static final int HIGH = 2;
	/** 3 级：γ（伽马） */
	public static final int GAMMA = 3;
	/** 4 级：ε（伊普西隆，蓝宝石专属） */
	public static final int EPSILON = 4;
	/** 5 级：ω（欧米伽，仅蓝宝石 256RPM 充能器） */
	public static final int OMEGA = 5;

	/** 当前最高等级 */
	public static final int MAX_LEVEL = OMEGA;

	/**
	 * 各级符号表（下标 = 等级 − {@link #LOW}）：α / β / γ / ε / ω。
	 * <b>等级 → 符号的唯一数据源</b>，见 {@link #glyph(int)}。
	 */
	private static final String[] GLYPHS = { "α", "β", "γ", "ε", "ω" };

	/** 翡翠（Jade）科技线封顶等级：3 级（γ）——4/5 级为蓝宝石专属。 */
	public static final int JADE_MAX = GAMMA;
	/** 蓝宝石（Sapphire）科技线封顶等级：5 级（ω）。 */
	public static final int SAPPHIRE_MAX = OMEGA;

	private WaveLevels() {
	}

	/**
	 * 等级基础飞行速度（格/秒）。
	 * <ul>
	 *   <li>1（α）= 2；2（β）= 4；3（γ）= 6；</li>
	 *   <li>4（ε）= 7；5（ω）= 8。</li>
	 * </ul>
	 */
	public static double baseSpeed(int level) {
		return switch (level) {
			case HIGH -> 4;
			case GAMMA -> 6;
			case EPSILON -> 7;
			case OMEGA -> 8;
			default -> 2;
		};
	}

	/**
	 * 该等级允许的最大飞行速度（格/秒，波速调节器加速上限）。
	 * 4/5 级上限 12，1~3 级保持 10。
	 */
	public static double maxSpeed(int level) {
		return level >= EPSILON ? 12.0d : 10.0d;
	}

	/**
	 * 命中生物伤害（半颗心 × 2 = 满颗心计）：
	 * 1（α）=4、2（β）=6、3（γ）=8、4（ε）=10、5（ω）=12。
	 * <p>数值为暂定方案，可随平衡调整。</p>
	 */
	public static float damage(int level) {
		return switch (level) {
			case HIGH -> 6f;
			case GAMMA -> 8f;
			case EPSILON -> 10f;
			case OMEGA -> 12f;
			default -> 4f;
		};
	}

	/** 等级是否合法（1~5）。 */
	public static boolean isValid(int level) {
		return level >= LOW && level <= MAX_LEVEL;
	}

	/**
	 * <b>等级 → 希腊字母符号（本模组唯一实现点）</b>：1=α、2=β、3=γ、4=ε、5=ω。
	 *
	 * <p>对外显示波级（Jade 波情行、护目镜档位行、充能器手动等级槽、JEI 等级徽章）一律只给符号，
	 * 不附"（阿尔法）"之类的文字。任何新增显示点都必须调本方法，<b>不得再写等级 switch</b>。</p>
	 *
	 * <p><b>容错</b>：越界（&lt;1 或 &gt;5，例如老存档/坏数据/扩展模组写坏等级）回退 1 级符号 α，
	 * 绝不抛异常——读数永远可用。</p>
	 */
	public static String glyph(int level) {
		return GLYPHS[isValid(level) ? level - LOW : 0];
	}

	/**
	 * 波级显示组件（{@link #glyph(int)} 的组件包装，便于直接当词条参数/行内容使用）；
	 * 越界容错口径与 {@link #glyph(int)} 完全一致。
	 */
	public static MutableComponent displayName(int level) {
		return Component.literal(glyph(level));
	}
}
