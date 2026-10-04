package com.hjmmd_8.createoreexpansion.content.equipment.item;

import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTrailStyle;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * <b>弓被动技能「元矢自生」的数值与抽签真源</b>（作者 2026-10-04 弓技能批 3）。
 *
 * <p>「元矢自生」= <b>无箭射击</b>（本模组四把弓共用的那条"没箭时耗能造一支魔法箭"的路，
 * 见 {@link JadeTopazBowItem#NO_ARROW_COST} / {@code prepareProjectiles}）时，按<b>该弓的档位
 * 起始等级</b>掷一次骰子：中签就给这一发魔法箭附上<b>八种魔素里随机的一种</b>，命中生物时
 * 等效于"一枚带魔素的攻击波打中该生物"（效果本体住在
 * {@code content.charger.wave.WaveEssenceEffects#applyEssenceOnCreatureHit}）。</p>
 *
 * <h2>作者裁定与逐条落地</h2>
 * <ul>
 *   <li><b>被动、不占键位</b>：弓只有键一/键二两个技能槽（那两条是继承来的
 *       {@code bow_curse} / {@code bow_disarm}）⇒ 本技能<b>没有技能条目、没有键位、没有冷却</b>，
 *       它是"无箭射击"这条路上的一个纯概率增强，所以它也不进 {@code AllSkills}
 *       （红线：那两条弓技能的注册条目一个字不许动）；</li>
 *   <li><b>触发时机 = 无箭射击</b>：唯一抽取点在 {@code JadeTopazBowItem#prepareProjectiles} 的
 *       "无箭且能量够"那条分支里 ⇒ 有箭的普通射击<b>永远不抽</b>；无箭补给（耗
 *       {@link JadeTopazBowItem#NO_ARROW_COST} 点能量、造一支 {@code INTANGIBLE_PROJECTILE}
 *       的箭）本身<b>逐字保留</b>，本类只在补给之后多掷一次骰子；</li>
 *   <li><b>数值 = Lv1/Lv2/Lv3 ⇒ 10% / 20% / 30%</b>（{@link #CHANCE_BY_LEVEL}，作者给死）；</li>
 *   <li><b>等级取法</b>：{@code BowTier#baseSkillLevel()}（翠玉 1 / 宝石 2 / 星界 3 / 雷鸣 3）
 *       —— 与批 2 的技能起始等级<b>同一处真源</b>；上限就是本表长度 <b>3</b>
 *       （{@link #chanceForLevel(int)} 内部夹取）⇒ 调用点<b>一个等级/概率字面量都不写</b>；</li>
 *   <li><b>魔素随机取 8 种之一</b>：{@link #ESSENCE_POOL} —— {@link WaveTrailStyle} 的前三个值
 *       （{@code NORMAL} / {@code MECHANICAL} / {@code DAMAGE}）是<b>波型风格</b>、不是魔素，
 *       刻意不进池子（与波实体那条"环绕波魔素池"同一判据：魔素 = 枚举的后八个值）；</li>
 *   <li><b>不扣能、无冷却</b>：中签与否都不再扣第二笔能量、也不写任何冷却账
 *       （本类没有冷却相关的任何一行）。</li>
 * </ul>
 *
 * <h2>⚠ 「玩家技艺提升会不会影响它」——<b>不会</b>（刻意）</h2>
 * <p>本技能的等级取自 {@code BowTier#baseSkillLevel()}（<b>该弓的档位起始等级</b>，一个注册期就
 * 固定下来的值），<b>不是</b> {@link JadeTopazBowItem#effectiveSkillLevel(net.minecraft.world.item.ItemStack)}
 * —— 后者会读物品附魔（技艺提升 / 技艺回溯）得到"这一把弓上技能的有效等级"。两者刻意分开：</p>
 * <ul>
 *   <li>作者给的口径是「Lv1/2/3 概率 10%/20%/30%」+「取该弓的档位起始等级」，两个都是<b>按弓</b>
 *       的量 ⇒ 四把弓各自一个固定概率（10% / 20% / 30% / 30%），与玩家身上有什么附魔无关；</li>
 *   <li>于是本被动<b>不可能</b>因为玩家给弓打了技艺提升而把概率顶到 Lv4/Lv5（那两档在作者的表里
 *       根本不存在），也不会与"技能那一对"的等级读数纠缠在一起 —— 技能的有效等级照旧只走
 *       {@code effectiveSkillLevel}，一个字未动。</li>
 * </ul>
 *
 * <p><b>为什么单独一个类</b>：这一组数与抽签方式（概率表 / 魔素池 / 容错解析）在仓里只有这一处，
 * 弓物品那边只读不算（{@code JadeTopazBowItem} 里没有 10/20/30、也没有八种魔素的名字）。</p>
 *
 * @since 1.0.0
 */
public final class BowMetaArrowTrait {

	/**
	 * <b>等级 → 中签概率</b>：Lv1 = 10%、Lv2 = 20%、Lv3 = 30%（<b>【作者定】</b>2026-10-04）。
	 *
	 * <p>数组下标 = 等级 − 1，<b>表长 3 就是"上限 3"本身</b>
	 * （{@link #chanceForLevel(int)} 用它夹取）：多写一行就等于凭空多出 Lv4 的概率档，
	 * 而作者只给了三级。</p>
	 */
	private static final float[] CHANCE_BY_LEVEL = { 0.10F, 0.20F, 0.30F };

	/**
	 * <b>随机魔素的池子 = 八种魔素</b>（【作者定】："魔素随机取 8 种之一"）。
	 *
	 * <p>顺序照 {@link WaveTrailStyle} 的声明序（水/火/地/风/冰/雷/毒/异）——
	 * 不是玩法语义，只是让"池子里有哪些"一眼能与枚举对得上。前三个值
	 * （{@code NORMAL} / {@code MECHANICAL} / {@code DAMAGE}）是<b>波型风格</b>，不进池子：
	 * 它们被当魔素传来时，魔素层那一支会按"没有魔素效果"处理（什么都不做）。</p>
	 */
	private static final WaveTrailStyle[] ESSENCE_POOL = {
		WaveTrailStyle.WATER,
		WaveTrailStyle.FIRE,
		WaveTrailStyle.EARTH,
		WaveTrailStyle.WIND,
		WaveTrailStyle.ICE,
		WaveTrailStyle.LIGHTNING,
		WaveTrailStyle.POISON,
		WaveTrailStyle.ARCANE
	};

	private BowMetaArrowTrait() {
	}

	/**
	 * 该等级的中签概率（{@link #CHANCE_BY_LEVEL}）；等级被夹进 {@code [1, 表长]}。
	 *
	 * <p>调用点传的是 {@code BowTier#baseSkillLevel()}（今天恒 1/2/3/3）⇒ 夹取只是兜底：
	 * 万一将来某个档位的起始等级超过了表长，这里返回的是最高一档的概率（30%），
	 * 而不是数组越界崩掉。</p>
	 */
	public static float chanceForLevel(int level) {
		return CHANCE_BY_LEVEL[Mth.clamp(level, 1, CHANCE_BY_LEVEL.length) - 1];
	}

	/**
	 * 这一次无箭射击是否中签（唯一一处掷骰子）。
	 *
	 * <p>随机源由调用点传（{@code player.getRandom()}）：与仓里其它概率效果同一口径 ——
	 * 用实体自己的 {@code RandomSource}，不 {@code new Random()}。</p>
	 */
	public static boolean procs(int level, RandomSource random) {
		return random.nextFloat() < chanceForLevel(level);
	}

	/** 从 {@link #ESSENCE_POOL} 里等概率抽一种魔素（唯一一处抽签）。 */
	public static WaveTrailStyle randomEssence(RandomSource random) {
		return ESSENCE_POOL[random.nextInt(ESSENCE_POOL.length)];
	}

	/**
	 * 按<b>枚举名</b>查魔素，认不出来就是 {@code null}（命中处理器读箭上那个字符串时用）。
	 *
	 * <p><b>为什么不用 {@link WaveTrailStyle#valueOf}</b>：箭的 {@code persistentData} 是存档里
	 * 跟着实体走的一块 NBT、也是客户端可以塞字符串的地方 —— {@code valueOf} 遇到空串/未知名字会抛
	 * {@code IllegalArgumentException}，而这条路径跑在命中处理里（一抛就是崩）。
	 * 仓里同一件事的先例是波实体的 {@code essenceByName}（"未登记值静默回落"）：认不出来 ⇒
	 * {@code null} ⇒ 命中处理器什么都不做（不崩、不改玩法）。</p>
	 */
	@Nullable
	public static WaveTrailStyle essenceByName(@Nullable String name) {
		if (name == null || name.isEmpty()) {
			return null;
		}
		for (WaveTrailStyle style : ESSENCE_POOL) {
			if (style.name()
				.equals(name)) {
				return style;
			}
		}
		return null;
	}
}
