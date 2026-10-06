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
 * <h2>★ 等级取哪一套（批 12 改口径，与「星元波置」同批同改；<b>批 14 改读数</b>）</h2>
 * <p>发射等级 = <b>该弓槽 2 那条技能的「有效」等级</b>
 * {@link BowTier#thirdSkillEffectiveLevel(net.minecraft.world.item.ItemStack)}（走
 * {@code JadeTopazBowItem#fireThunderMightInsteadOfArrow} 传
 * {@code this.tier.thirdSkillEffectiveLevel(weapon)}）—— 基准是槽 2 的绑定等级
 * {@link BowTier#thirdSkillLevel()}（作者批 12 的表：雷鸣神力 <b>①</b>），再按既有口径
 * 叠加技艺提升 / 技艺回溯，按 {@link BowTier#maxSkillLevel()}（雷鸣 = 3）钳位。</p>
 * <p>⚠ <b>批 12~13 读的是注册期常量 {@code thirdSkillLevel()} = ①</b> ⇒ 实机上<b>恒走 Lv1 那一行</b>
 * （2×2 / 20%），而费用却随有效等级涨到 450 —— 作者批 14 的"这两个玩意儿只能一级？他们不是
 * 封顶三级吗"点的就是这个。现在三行全部可达：Lv1/2/3 ⇒ <b>2×2 / 3×3 / 4×4</b> 与
 * <b>20% / 40% / 60%</b>，且与费用同源。</p>
 * <p>⚠ <b>批 5~11 读的是 {@link BowTier#baseSkillLevel()}（雷鸣 = 3）</b>，那是"元矢自生"那个被动的
 * 档位起始等级，与技能等级无关 —— 它在批 12 被换掉，批 14 之后仍然<b>不是</b>本技能的等级来源。</p>
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
 * <h2>耗能与冷却（<b>2026-10-06 批 13 补齐</b>；批 5~12 这两项都为「无」）</h2>
 * <p>作者原话：「<b>基础的能量都是一次释放技能，消耗 150 乘以技能等级点</b>」「<b>只有弓，弓的话，
 * 冷却是3秒、4秒、5秒</b>」⇒ 本条技能补上 {@link #ENERGY_COST_PER_LEVEL}（一级消耗 150，
 * 实际 = 150 × 有效等级）与 {@link #cooldownSecondsFor(int)}（3 / 4 / 5 秒，宿主 = 按技能记的
 * {@code PerSkillCooldown}，判定在 {@code BowExclusiveShotItemSkill} 里两处同判）。</p>
 * <p>⚠ 「有箭那一发箭本身就是代价」（{@code draw(..)} 已把它从物品栏收走）、「无箭那一发由
 * {@code prepareProjectiles} 另付 {@code JadeTopazBowItem.NO_ARROW_COST}」与"耐久照原版同一笔账"
 * 这三条既有口径<b>一个字未改</b>：新增的是技能自己那一次释放的能量与冷却，两笔账互不替代。</p>
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

	/**
	 * <b>一级消耗</b>（作者 2026-10-06 批 13 给死）：「基础的能量都是一次释放技能，消耗
	 * <b>150 乘以技能等级</b>点」⇒ 本条技能每释放一次扣 {@code 150 × 有效等级} 点工具能量
	 * （Lv1 = 150 / Lv2 = 300 / Lv3 = 450）。
	 *
	 * <p>这里写的是<b>一级消耗</b>（一个数），"乘等级"由既有算术
	 * {@code CoeSkillSupport.cost(..) ← SkillEnergyCost.compute(..)} 完成；唯一调用点 =
	 * {@code BowExclusiveShotItemSkill#consumeResource}（按名读本常量）。</p>
	 */
	public static final int ENERGY_COST_PER_LEVEL = 150;

	// ========== 三个等级（作者 2026-10-05 给死：2×2/20% · 3×3/40% · 4×4/60%） ==========
	// ⚠ 批 13 加的那一列（冷却 3/4/5 秒）**不进下面三行**：边长与真雷概率被关卡
	// bow5-thunder-tables 逐字钉住 ⇒ 为加一列而改那处钉法，会把"作者既有三行的数值"从
	// "逐字不变"降级成"改过一遍"。冷却单独走下面那个按等级的读数（同一张表、同一个夹取）。

	/** Lv1 —— 水平方形边长 2 格、真雷 20%。 */
	public static final Level LEVEL_1 = new Level(2, 0.20F);

	/** Lv2 —— 水平方形边长 3 格、真雷 40%。 */
	public static final Level LEVEL_2 = new Level(3, 0.40F);

	/** Lv3 —— 水平方形边长 4 格、真雷 60%。 */
	public static final Level LEVEL_3 = new Level(4, 0.60F);

	/** 按等级取配置（与其余各条 {@code *Configs} 同名同形；越界先夹到 [1, 3]）。 */
	public static Level level(int level) {
		return SkillLevelTables.pick3Clamped(level, MAX_LEVEL, LEVEL_1, LEVEL_2, LEVEL_3);
	}

	/**
	 * <b>本技能对哪一档弓生效</b>（作者 2026-10-05："雷鸣弓专属技能"）。
	 *
	 * <p>刻意写成<b>穷尽 switch（无 default）</b>：将来给枚举加一档弓，这里会<b>编译不过</b>，
	 * 而不是静默地让新弓"什么也不发生"。今天只有雷鸣弓这一档为 {@code true}
	 * ⇒ 翠玉 / 宝石 / 星界三把弓的这条闸门恒不通过。</p>
	 *
	 * <p>⚠ 与 {@link BowWaveShiftConfigs#appliesTo(BowTier)} 是<b>两张独立的表、两个独立的判据</b>：
	 * 批 12 之后<b>雷鸣在两处都为 true</b> —— 但那是<b>两条不同的技能槽</b>（槽 1 = 量波置换走那张表、
	 * 槽 2 = 本表这条雷鸣神力），弓侧三道闸门的第二道判据是"这一发的标记是不是本技能写的"
	 * （{@code BowExclusiveShotItemSkill#matches}）⇒ 同一次射击最多只被其中一条接管。
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
	 * 该技能等级的<b>冷却秒数</b>（3 / 4 / 5；作者 2026-10-06 批 13 给死）。
	 *
	 * <p>三条弓专属技能（量波置换 / 星元波置 / 雷鸣神力）<b>同一张表</b>；越界先夹到 [1, 3]
	 * —— 走 {@link SkillLevelTables#pick3Clamped(int, int, Object, Object, Object)} 这个既有形状
	 * （表长 = 上限本身，与 {@link #level(int)} 同一个夹取），本类<b>不</b>手写
	 * {@code Math.max/min}、也不另立第二张等级表。</p>
	 *
	 * <p>宿主 = 按技能记的 {@code PerSkillCooldown}（雷鸣弓的
	 * {@code BowTier#perSkillCooldown()} = true），判定位置 = {@code BowExclusiveShotItemSkill} 的
	 * {@code consumeResource} 与 {@code release} <b>两处同判</b>（既有红线：只判一处会出现
	 * "冷却中照扣能量"或"扣了却不执行"）。</p>
	 */
	public static int cooldownSecondsFor(int level) {
		return SkillLevelTables.pick3Clamped(level, MAX_LEVEL, 3, 4, 5);
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
