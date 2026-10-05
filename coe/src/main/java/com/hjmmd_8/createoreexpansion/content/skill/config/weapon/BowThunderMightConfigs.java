package com.hjmmd_8.createoreexpansion.content.skill.config.weapon;

import com.hjmmd_8.createoreexpansion.content.equipment.item.BowTier;
import com.hjmmd_8.createoreexpansion.content.skill.SkillLevelTables;

/**
 * <b>雷鸣弓专属主动技能「雷鸣神力」（作者 2026-10-05 弓技能批 5）的数值真源</b> ——
 * 与 {@link BowWaveShiftConfigs} / {@link BowCurseConfigs} / {@link BowDisarmConfigs} 同形，
 * 是这条技能<b>唯一</b>写数字的地方：等级表（范围边长 / 真雷概率）、落点射程、区域容差、
 * 扫掠外扩量全部只在这里写一遍，发射点（{@code BowThunderMightLauncher}）与物品侧
 * （{@code JadeTopazBowItem}）<b>一个数字都不写</b>。
 *
 * <h2>作者原话（逐字）</h2>
 * <blockquote>
 * 雷鸣弓「雷鸣神力」：按住技能键打出的那一发，以命中点为中心、边长 2×2 / 3×3 / 4×4 的水平方形内，
 * 生物随机获得一种电荷；并把 20% / 40% / 60% 的概率真的劈一道雷。
 * </blockquote>
 *
 * <h2>它<b>不是</b>「发波」（本批与宝石弓那条的分水岭）</h2>
 * <p>{@link BowWaveShiftConfigs} 那条（宝石弓「量波置换」/ 星界弓「星元波置」）发的是既有
 * {@code ChargerWaveEntity}；本技能<b>一枚波都不发</b>，它只做两件事：</p>
 * <ol>
 *   <li><b>给落点附近范围内的生物挂随机电荷</b> —— 施加面<b>唯一</b>：
 *       {@code LightningEssenceHitCharge#applyOnHit}（它内部调 {@code ChargeApi#applyRandom}）。
 *       口径照作者 2026-10-04 下的<b>统一标准</b>（"雷魔素攻击生物之后，会随机给生物附着一种电荷，
 *       附着时间为 5 秒，附着的那个 buff 等级从 1~3 随机"）⇒ <b>极性随机 / 等级 1~3 随机 /
 *       时长固定 5 秒</b>，三个值全部按名取自 {@code ChargeConfigs}
 *       （{@code LIGHTNING_ESSENCE_CHARGE_TICKS} / {@code randomLightningEssenceLevel(..)}），
 *       本表<b>不复制</b>它们、也不另写第二套等级或时长公式；</li>
 *   <li><b>概率劈一道原版闪电</b> —— {@code EntityType.LIGHTNING_BOLT}（作者明确选了
 *       <b>原版闪电</b>：会点燃、会破坏地形），落点 = 命中点，<b>绝不取施放者自己的坐标</b>，
 *       而且只在技能那一发判定<b>一次</b>（没有 tick 循环、不看天气 ⇒ 雷雨天也不会无限刷）。</li>
 * </ol>
 *
 * <h2>★ 等级取哪一套（与 {@link BowWaveShiftConfigs} 逐字同口径）</h2>
 * <p>发射等级 = <b>该弓的档位起始等级</b> {@link BowTier#baseSkillLevel()}（走
 * {@code JadeTopazBowItem#fireThunderMightInsteadOfArrow} 传
 * {@code this.tier.baseSkillLevel()}），<b>不是</b>玩家的附魔加成等级
 * （{@code effectiveSkillLevel}）—— 理由与「元矢自生」/「量波置换」那条逐字相同：作者给的是
 * 「该弓的档位」这个按弓固定的量，附魔不该移动它。</p>
 * <p>⚠ 今天只有雷鸣弓这一档（{@link #appliesTo}），而 {@code BowTier#baseSkillLevel()} 的雷鸣行是
 * <b>3</b> ⇒ 实机上<b>恒走 Lv3 那一行</b>（4×4 / 60%）。Lv1 / Lv2 两行不是死代码：它们是这张表的
 * 完整口径（作者原话给了三档），并且随"该弓档位起始等级"这个读数走——将来若把雷鸣档的起始等级
 * 调低、或再挂一把新弓到这张表上，三行会各自生效，调用点一个字都不用改。</p>
 *
 * <h2>「n×n」的读法（本批唯一一处需要写清楚的几何口径）</h2>
 * <p>作者写的是「以命中点为中心、边长 n 的<b>水平方形</b>」⇒ 本批按<b>方形而不是立方体</b>实现：</p>
 * <ul>
 *   <li><b>水平两个轴</b>：{@code [x - n/2, x + n/2]} × {@code [z - n/2, z + n/2]}
 *       （边长恰好 n 格，{@link #areaSideFor(int)} 的 {@code n} 就是边长本身）；</li>
 *   <li><b>垂直轴</b>：只给一格容差（{@link #AREA_VERTICAL_HALF_EXTENT} = 1.0 格）——
 *       纯平面（零厚度）在几何上<b>永远匹配不到站在落点上的生物</b>（AABB 相交判据在每一轴上
 *       都要求严格重叠：零厚度盒与"脚正好踩在落点上"的生物包围盒在 y 轴上不相交）。
 *       一格容差是"水平方形"这句话的最保守落地：不会把三层楼上的生物卷进来，
 *       也不至于因为方块表面与生物包围盒差零点几格而漏掉站在落点上的那只。</li>
 * </ul>
 * <p>⚠ <b>垂直容差是「我独创、作者没写死」的一个数</b>（作者只给了"水平方形"）：要改成
 * 零厚度、或者改成 n 格高的立方体，只改本表两个方法/常量（本类是唯一取值点）。</p>
 *
 * <h2>耗能与冷却</h2>
 * <p>沿用批 4 已落地的口径：<b>无箭那一发</b>已经由 {@code prepareProjectiles} 的"无箭补给"付过
 * {@code JadeTopazBowItem.NO_ARROW_COST} 点（那条路一字未动）；<b>有箭那一发</b>箭本身就是代价
 * （{@code draw(..)} 已经把它从物品栏收走）。耐久照原版同一笔账在物品侧扣。冷却<b>无</b>
 * （与另外两条弓技能同形：不是正式技能条目，内核侧没有它的耗能与冷却）。</p>
 *
 * <h2>⛔ 零注册、零语言键、零新 id</h2>
 * <p>与批 4 逐字同形：本技能<b>不是</b>正式技能条目 —— 不加 {@code AllSkills} 条目、不进内核白名单、
 * 不写语言键、不跑 {@code runData}；它挂在本把弓自己那两条既有技能的键位上
 * （{@code CoeSkillRelease#anyHeldItemSkillKeyPressed}，服务端权威读数）。</p>
 *
 * @since 1.0.0
 */
public final class BowThunderMightConfigs {

	private BowThunderMightConfigs() {
		throw new AssertionError("This class should not be instantiated");
	}

	/** 三档（装备/武器技能 3 级封顶，与其余各条同口径；表长 3 就是"上限 3"本身）。 */
	public static final int MAX_LEVEL = 3;

	/**
	 * 单级「雷鸣神力」定义。
	 *
	 * @param areaSideBlocks 范围内那个<b>水平方形</b>的边长（格）：Lv1 = 2 / Lv2 = 3 / Lv3 = 4（作者给死）
	 * @param realBoltChance 真的劈一道<b>原版闪电</b>的概率（0~1）：Lv1 = 0.20 / Lv2 = 0.40 / Lv3 = 0.60（作者给死）
	 */
	public record Level(int areaSideBlocks, float realBoltChance) {
	}

	// ========== 三个等级（作者 2026-10-05 给死：2×2/20% · 3×3/40% · 4×4/60%） ==========

	/** Lv1 —— 水平方形边长 2 格、真雷 20%。 */
	public static final Level LEVEL_1 = new Level(2, 0.20F);

	/** Lv2 —— 水平方形边长 3 格、真雷 40%。 */
	public static final Level LEVEL_2 = new Level(3, 0.40F);

	/** Lv3 —— 水平方形边长 4 格、真雷 60%（<b>雷鸣弓走的就是这一档</b>：它的档位起始等级 = 3）。 */
	public static final Level LEVEL_3 = new Level(4, 0.60F);

	/** 按等级取配置（与其余各条 {@code *Configs} 同名同形；越界先夹到 [1, 3]）。 */
	public static Level level(int level) {
		return SkillLevelTables.pick3Clamped(level, MAX_LEVEL, LEVEL_1, LEVEL_2, LEVEL_3);
	}

	/**
	 * <b>本技能对哪一档弓生效</b>（作者 2026-10-05："雷鸣弓专属技能"）。
	 *
	 * <p>刻意写成<b>穷尽 switch（无 default）</b>：将来给枚举加一档弓，这里会<b>编译不过</b>，
	 * 而不是静默地让新弓"什么也不发生"。今天恰好只有雷鸣弓这一档为 {@code true}
	 * ⇒ 翠玉 / 宝石 / 星界三把弓的射击路径（含它们各自的技能）<b>一个字节都不变</b>。</p>
	 *
	 * <p>⚠ 与 {@link BowWaveShiftConfigs#appliesTo(BowTier)} 是<b>两张独立的表、两个独立的判据</b>：
	 * 宝石 + 星界走那张（发波），雷鸣走这张（电荷 + 真雷），翠玉两处都是 false。
	 * 两处都是穷尽 switch ⇒ 加一档新弓时<b>两处都会编译不过</b>，不会被任何一方静默吞掉。</p>
	 */
	public static boolean appliesTo(BowTier tier) {
		if (tier == null) {
			return false;
		}
		return switch (tier) {
			case THUNDER -> true;
			case JADE_TOPAZ, SAPPHIRE_RUBY, ASTRAL -> false;
		};
	}

	/** 该技能等级的<b>水平方形边长</b>（2 / 3 / 4 格，作者给死）。 */
	public static int areaSideFor(int level) {
		return level(level).areaSideBlocks();
	}

	/** 该技能等级的<b>真雷概率</b>（0.20F / 0.40F / 0.60F，作者给死）。 */
	public static float realBoltChanceFor(int level) {
		return level(level).realBoltChance();
	}

	/**
	 * 该技能等级的<b>水平半边长</b>（格）= 边长 ÷ 2，就是那个方形的 {@code ±} 值。
	 *
	 * <p>刻意做成表里的一个读数而不是让发射点写 {@code side / 2.0D}：本批有一条口径是
	 * "发射类里<b>一个数字字符都不写</b>"（连那个 2 都算），于是"除以二"这件事也归本表。</p>
	 */
	public static double areaHalfExtentFor(int level) {
		return areaSideFor(level) / 2.0D;
	}

	/**
	 * <b>范围内那个水平方形的垂直半容差</b>（格）：<b>1.0</b>。
	 *
	 * <p>为什么不是 0、为什么不是"边长那么多"（= 立方体）—— 读法与理由全文见类注释的
	 * 「n×n 的读法」一节。这一行是那个口径的<b>唯一</b>取值点。</p>
	 */
	public static final double AREA_VERTICAL_HALF_EXTENT = 1.0D;

	/**
	 * <b>落点射程</b>（格）：命中点射线的最远长度 —— 打空（天上 / 无方块方向）时落点就取这个距离。
	 *
	 * <p>⚠ <b>这个数是本项目自定的第一版射程</b>（作者只给了"以命中点为中心"，没给数）：
	 * 口径 = <b>与波引擎的飞行预算同量级</b>（{@code AbstractChargerWaveEntity#MAX_TRAVEL_DISTANCE}
	 * = 64 格，"一条能量波飞多远"这条既有约定），不引入新的量级。太短会让"瞄准远处目标"这一发
	 * 落在半空（那一支箭已经被技能换掉了），太长没有代价（射线本来就会先撞上地形）。
	 * 要调只改这一行（本类是全仓唯一取值点）。</p>
	 */
	public static final double MAX_IMPACT_RANGE = 64.0D;

	/**
	 * <b>落点射线的扫掠外扩量</b>（格）：照抄回旋镖那条"实体射线 + 方块射线"的扫掠盒
	 * （{@code BoomerangImpact#checkImpact} 的 {@code inflate(1.0D)}）—— 射线是一条零宽度的线，
	 * 不外扩就擦不到生物的包围盒边缘。
	 */
	public static final double PICK_SWEEP_INFLATE = 1.0D;
}
