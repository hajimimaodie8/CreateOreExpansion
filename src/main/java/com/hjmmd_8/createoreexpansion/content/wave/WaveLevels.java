package com.hjmmd_8.createoreexpansion.content.wave;

/**
 * 能量波等级元数据集中表（1~5 级）。
 *
 * <p>等级谱系（蓝宝石科技线扩展后）：</p>
 * <ul>
 *   <li><b>1 级 = 低充能（LOW）</b>：基础速度 2 格/秒（翡翠低充能同款）；</li>
 *   <li><b>2 级 = 高充能（HIGH）</b>：4 格/秒；</li>
 *   <li><b>3 级 = 伽马（GAMMA）</b>：6 格/秒；</li>
 *   <li><b>4 级 = 伊普西龙（EPSILON，ε）</b>：7 格/秒，紫粉色（蓝宝石线专属，翡翠线封顶 3 级）；</li>
 *   <li><b>5 级 = 欧米伽（OMEGA，Ω）</b>：8 格/秒，主体红色、拖尾/粒子金色（仅蓝宝石 256 RPM 充能器产出）。</li>
 * </ul>
 *
 * <p><b>4/5 级速度上限提升至 12 格/秒</b>（波速调节器可加速到的上限），
 * 1~3 级保持原上限 10 格/秒 —— 本表统一提供 {@link #maxSpeed(int)}。</p>
 *
 * <p>所有机器/波实体/渲染/Jade/JEI 均查本表，避免等级数值散落各处 switch。</p>
 */
public final class WaveLevels {

	/** 1 级：低充能 */
	public static final int LOW = 1;
	/** 2 级：高充能 */
	public static final int HIGH = 2;
	/** 3 级：伽马 */
	public static final int GAMMA = 3;
	/** 4 级：伊普西龙（ε，蓝宝石专属） */
	public static final int EPSILON = 4;
	/** 5 级：欧米伽（Ω，仅蓝宝石 256RPM 充能器） */
	public static final int OMEGA = 5;

	/** 当前最高等级 */
	public static final int MAX_LEVEL = OMEGA;

	/** 翡翠（Jade）科技线封顶等级：3（伽马）——4/5 级为蓝宝石专属。 */
	public static final int JADE_MAX = GAMMA;
	/** 蓝宝石（Sapphire）科技线封顶等级：5（欧米伽）。 */
	public static final int SAPPHIRE_MAX = OMEGA;

	private WaveLevels() {
	}

	/**
	 * 等级基础飞行速度（格/秒）。
	 * <ul>
	 *   <li>1 = 2（低）；2 = 4（高）；3 = 6（伽马）；</li>
	 *   <li>4 = 7（伊普西龙）；5 = 8（欧米伽）。</li>
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
	 * 1=4、2=6、3=8、4=10（伊普西龙）、5=12（欧米伽）。
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
}
