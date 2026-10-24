package com.hjmmd_8.createoreexpansion.content.wave.api;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * <b>波等级门面</b>：等级 → 符号 / 速度 / 速度上限 / 伤害 / 指示灯色 / 金色拖尾 / 充能量的
 * <b>唯一查询入口</b>；数据真源已搬到 {@link WaveLevelRegistry}（每条 = 一个 {@link WaveLevelSpec}）。
 *
 * <p><b>本类为什么只剩方法体一行</b>：抽取前这里是 6 张并行表（{@link #glyph} 的符号数组、
 * {@link #baseSpeed} / {@link #damage} / {@link #indicatorColor} 三个 switch、{@link #maxSpeed}
 * 的三元阈值、以及实体里写死的 {@code isOmega() >= 5}），加第 6 级要改 8 处。现在全部委托给
 * 注册表；<b>公开签名一个字没改</b>（既有 87 处调用零改动），第三方加等级只需
 * {@code WaveLevelRegistry.register(6, spec)} 一行。</p>
 *
 * <p><b>等级谱系</b>（内置 5 条，括号内为对外显示的希腊字母符号）：</p>
 * <ul>
 *   <li><b>1 级 = α（阿尔法，LOW）</b>：基础速度 2 格/秒（翡翠线最低档）；</li>
 *   <li><b>2 级 = β（贝塔，HIGH）</b>：4 格/秒；</li>
 *   <li><b>3 级 = γ（伽马，GAMMA）</b>：6 格/秒；</li>
 *   <li><b>4 级 = ε（伊普西隆，EPSILON）</b>：7 格/秒，紫粉色（蓝宝石线专属，翡翠线封顶 3 级）；</li>
 *   <li><b>5 级 = ω（欧米伽，OMEGA）</b>：8 格/秒，主体红色、拖尾/粒子金色（仅蓝宝石 256 RPM 充能器产出）。</li>
 * </ul>
 *
 * <p><b>4/5 级速度上限提升至 12 格/秒</b>（波速调节器可加速到的上限），1~3 级保持原上限
 * 10 格/秒 —— 各自写在对应等级的注册项里。</p>
 *
 * <p><b>越界容错（改造前逐值保留）</b>：未注册的等级绝不抛异常。旧表对"低档"与"阈值"两种形态的
 * 默认值不同，本门面按方法分别保留：</p>
 * <ul>
 *   <li>{@link #baseSpeed} / {@link #damage} / {@link #indicatorColor} / {@link #glyph} /
 *       {@link #chargeEnergy}：未注册 ⇒ 取<b>最低等级</b>（α）的值（= 旧 {@code default ->} 分支）；</li>
 *   <li>{@link #maxSpeed} / {@link #isGoldTrailLevel}：未注册 ⇒ 取<b>不高于它的最高已注册等级</b>
 *       的值，没有更低等级时取最低等级（= 旧 {@code level >= EPSILON ? 12 : 10} 与
 *       {@code waveLevel >= 5} 的逐值行为）。</li>
 * </ul>
 *
 * <p><b>⚠ 颜色规则（作者 2026-10-24 定，D2；本批只记录、不改实现）</b>：最终颜色 = <b>魔素优先</b>
 * ——<b>有魔素 ⇒ 用魔素对应的颜色</b>；<b>没魔素 ⇒ 用波型对应的颜色</b>。这与
 * "颜色只由等级决定"的现状不同，因此本批<b>不动任何配色</b>：{@link #indicatorColor(int)} 仍是
 * 等级 → 指示色（充能器指示灯 / 护目镜能量条 / JEI 徽章共用），实体侧
 * {@code AbstractChargerWaveEntity#getWaveColorForLevel} 仍是等级 → 粒子色。规则落地留待后续批次
 * （B4 魔素注册表）。</p>
 *
 * <p><b>等级 → 符号</b>：对外一律只显示希腊字母（{@link #glyph(int)} / {@link #displayName(int)}），
 * 不显示任何非符号的等级文字（旧称与中文名一律不用）——符号现在来自注册项，映射实现仍只有一处，
 * 任何显示点都不得再写 switch。</p>
 *
 * <p><b>⚠ 本模组的两条科技线封顶不是"等级宇宙的上限"</b>：{@link #JADE_MAX} / {@link #SAPPHIRE_MAX}
 * 是<b>本模组充能器</b>能发出的最高等级（3 / 5），第三方注册的更高等级由第三方自己的机器负责发射。
 * 它们刻意保持 {@code final}，不随注册表变动。</p>
 *
 * <p><b>与粒子/渲染色是两个家族</b>：能量波的渲染色/粒子色是另一套更饱和的 RGB 0-1 配色
 * （{@code AbstractChargerWaveEntity#getWaveColorForLevel}）：1 级 (1,1,0)、2 级 (0,1,0)、
 * 3 级 (0,0.5,1)、4 级 (1,0.35,0.85)、5 级 (1,0.25,0.45)——与本表色相口径相同
 * （α 黄 / β 绿 / γ 蓝 / ε 紫粉 / ω 玫红）但数值只有 5 级重合；合并会改变粒子观感，
 * 故刻意保留为两套（本表 = 指示灯 / 能量条 / 徽章；实体那套 = 粒子）。</p>
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

	/**
	 * <b>当前最高已注册等级</b>（内置 = {@link #OMEGA} = 5）。
	 *
	 * <p>⚠ <b>这是全仓唯一一个非 final 的等级常量</b>：数据真源在 {@link WaveLevelRegistry}，
	 * 本字段是它维护的<b>兼容镜像</b>（注册第 6 级后自动变成 6）——正因为它是字段而不是编译期常量，
	 * 既有 30 处 {@code WaveLevels.MAX_LEVEL} 的读法才能一个字不改地跟着注册表走。
	 * <b>唯一写入者是 {@link WaveLevelRegistry#register}</b>（经 {@link #refreshMaxLevel}）；
	 * 此外任何地方都不许赋值。新代码请优先用 {@link #maxLevel()}。</p>
	 */
	public static int MAX_LEVEL = OMEGA;

	/** 翡翠（Jade）科技线封顶等级：3 级（γ）——4/5 级为蓝宝石专属。 */
	public static final int JADE_MAX = GAMMA;
	/** 蓝宝石（Sapphire）科技线封顶等级：5 级（ω）。 */
	public static final int SAPPHIRE_MAX = OMEGA;

	private WaveLevels() {
	}

	/** 当前最高已注册等级（{@link #MAX_LEVEL} 的推荐读法；语义与字段完全相同）。 */
	public static int maxLevel() {
		return WaveLevelRegistry.maxLevel();
	}

	/**
	 * 注册表在每次注册后回写兼容镜像（包内唯一写入点，见 {@link #MAX_LEVEL}）。
	 * <b>不是给外部调用的</b>。
	 */
	static void refreshMaxLevel(int maxLevel) {
		MAX_LEVEL = Math.max(OMEGA, maxLevel);
	}

	/**
	 * 等级基础飞行速度（格/秒）：1（α）= 2；2（β）= 4；3（γ）= 6；4（ε）= 7；5（ω）= 8。
	 * <p>未注册等级回落最低等级的值（改造前的 {@code default -> 2}）。</p>
	 */
	public static double baseSpeed(int level) {
		return specOrLowest(level).baseSpeed();
	}

	/**
	 * 该等级允许的最大飞行速度（格/秒，波速调节器加速上限）。
	 * <p>4/5 级上限 12，1~3 级 10；未注册等级取"不高于它的最高已注册等级"的上限
	 * （没有更低等级时取最低等级）——与改造前 {@code level >= EPSILON ? 12 : 10} 逐值一致。</p>
	 */
	public static double maxSpeed(int level) {
		WaveLevelSpec spec = WaveLevelRegistry.spec(level);
		if (spec != null)
			return spec.maxSpeed();
		WaveLevelSpec atMost = WaveLevelRegistry.highestAtMost(level);
		return (atMost != null ? atMost : specOrLowest(level)).maxSpeed();
	}

	/**
	 * 命中生物伤害（半颗心 × 2 = 满颗心计）：
	 * 1（α）=4、2（β）=6、3（γ）=8、4（ε）=10、5（ω）=12。
	 * <p>未注册等级回落最低等级的值（改造前的 {@code default -> 4f}）。</p>
	 */
	public static float damage(int level) {
		return specOrLowest(level).damage();
	}

	/**
	 * <b>等级 → 指示色（ARGB；本模组唯一一份"标准 5 档配色"读数点）</b>：
	 * 1（α）= 黄 {@code 0xFFFF55}、2（β）= 绿 {@code 0x55FF55}、3（γ）= 蓝 {@code 0x5555FF}、
	 * 4（ε）= 紫粉 {@code 0xFF66E0}、5（ω）= 玫红 {@code 0xFF4073}（数值在注册项里）。
	 *
	 * <p>供充能器指示灯、护目镜能量条填充色、JEI 等级徽章共用——这三类点位的颜色值逐档完全一致，
	 * 故只留本方法一处实现。<b>任何新增的指示色点位都必须调本方法</b>，不得再抄一张 switch。</p>
	 *
	 * <p><b>容错</b>：未注册（&lt;1 或已越界，例如 0 = 未接入应力、老存档坏数据）回落为 α 黄
	 * {@code 0xFFFF55}——与 JEI 分类改造前的默认值一致；需要"越界显灰"的点位
	 * （如充能器的指示灯）自行用 {@link #isValid(int)} 判定后另取灰。</p>
	 */
	public static int indicatorColor(int level) {
		return specOrLowest(level).indicatorColor();
	}

	/**
	 * 该等级的波是否叠 ω 的金色拖尾点缀（<b>装饰色</b>，不是波的渲染色）。
	 * <p>内置仅 5 级（ω）为 {@code true}；未注册等级取"不高于它的最高已注册等级"的标记
	 * （没有更低等级时 {@code false}）——与改造前写死的 {@code waveLevel >= 5} 逐值一致。</p>
	 */
	public static boolean isGoldTrailLevel(int level) {
		WaveLevelSpec spec = WaveLevelRegistry.spec(level);
		if (spec != null)
			return spec.goldTrail();
		WaveLevelSpec atMost = WaveLevelRegistry.highestAtMost(level);
		return atMost != null && atMost.goldTrail();
	}

	/**
	 * 该等级的波给物品 / 穿戴护甲充的点数：1（α）=100、2（β）=500、3（γ）=1000、4（ε）=1500、
	 * 5（ω）=2000（数值在注册项里；抽取前住在 {@code ChargingRecipe.energyForLevel}，现已并入本表）。
	 * <p>未注册等级回落最低等级的值（改造前的 {@code default -> 100}）。</p>
	 */
	public static int chargeEnergy(int level) {
		return specOrLowest(level).chargeEnergy();
	}

	/** 等级是否合法（= 该等级已注册；内置 1~5）。 */
	public static boolean isValid(int level) {
		return WaveLevelRegistry.isRegistered(level);
	}

	/**
	 * <b>等级 → 希腊字母符号（本模组唯一实现点）</b>：1=α、2=β、3=γ、4=ε、5=ω（符号在注册项里）。
	 *
	 * <p>对外显示波级（Jade 波情行、护目镜档位行、充能器手动等级槽、JEI 等级徽章）一律只给符号，
	 * 不附"（阿尔法）"之类的文字。任何新增显示点都必须调本方法，<b>不得再写等级 switch</b>。</p>
	 *
	 * <p><b>容错</b>：未注册（&lt;1 或越界，例如老存档/坏数据/扩展模组写坏等级）回退最低等级符号 α，
	 * 绝不抛异常——读数永远可用。</p>
	 */
	public static String glyph(int level) {
		return specOrLowest(level).glyph();
	}

	/**
	 * 波级显示组件（{@link #glyph(int)} 的组件包装，便于直接当词条参数/行内容使用）；
	 * 越界容错口径与 {@link #glyph(int)} 完全一致。
	 */
	public static MutableComponent displayName(int level) {
		return Component.literal(glyph(level));
	}

	/** 低档回落：未注册 ⇒ 最低等级（α）的声明——内置表保证它存在。 */
	private static WaveLevelSpec specOrLowest(int level) {
		WaveLevelSpec spec = WaveLevelRegistry.spec(level);
		return spec != null ? spec : WaveLevelRegistry.spec(LOW);
	}
}
