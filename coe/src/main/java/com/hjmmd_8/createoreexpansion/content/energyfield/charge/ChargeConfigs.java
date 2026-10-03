package com.hjmmd_8.createoreexpansion.content.energyfield.charge;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;

/**
 * 电荷系统（着正电 / 着负电）—— <b>全部数值与表现常量的唯一真源</b>（coe-charge 批 1）。
 *
 * <p>口径：本文件之外<b>任何地方</b>都不许再写字面量（时长 / 等级 / 扣血 / 爆炸 / 残留 /
 * 限频 / 配色 / 粒子类型都从这里引用）。出处逐条写在各常量上方，形如「需求 §3.3」
 * （开工需求 {@code 20261003-1750_coe-charge_charge-debuffs.md}）、「作者 2026-10-03 定」
 * （作者裁定 / 理解确认里转述的裁定）、「执行会话 2026-10-03 定」（文档留给执行会话定的值）。</p>
 *
 * <p><b>批 1 只落常量、不接调用点</b>：本类此刻的读取方只有
 * {@link ChargedPositiveEffect} / {@link ChargedNegativeEffect}（配色与常驻粒子）与
 * {@code common.registry.coe.CoeEffects}（注册）。爆炸 / 残留 / 四条途径 / 生物受场那几组
 * 常量是给批 4~8 备好的同一份真源，<b>此刻没有读取方</b>——各条注释里写明它属于哪一批。</p>
 *
 * <p><b>为什么不建自定义 {@code DamageType}</b>：作者 2026-10-03 裁定「不注册自定义 DamageType」
 * （需求 §六 #4 的推断被否决）⇒ 本类<b>刻意不含任何伤害类型 id 常量</b>，批 3 接扣血时
 * 从原版 / NeoForge 现成的伤害类型里选一个，不在本模组新建注册项。</p>
 */
public final class ChargeConfigs {

	private ChargeConfigs() {
	}

	// ==================================================================================
	// 一、等级与时长（需求 §3.1 的「等级范围 1..N」+ §六 #1 的推断值表）
	// ==================================================================================

	/** 等级下限：1（需求 §3.1「等级范围 {@code 1..N}」⇒ 1 级就是最小的一档）。 */
	public static final int MIN_LEVEL = 1;

	/**
	 * 等级上限：<b>5</b>。
	 *
	 * <p>需求 §六 #1（作者没给死、文档推断）：与工具技能上限一致（本仓 {@code AllSkills}
	 * 的 {@code maxLevel} 默认就是 5）。</p>
	 */
	public static final int MAX_LEVEL = 5;

	/**
	 * 每一级的时长（tick）：<b>200</b> tick = 10 秒。
	 *
	 * <p>需求 §六 #1：{@code 时长 = 200 tick × 等级} ⇒ Lv1 10 秒、Lv5 50 秒。
	 * 用 {@link #durationTicks(int)} 取，别在调用点乘。</p>
	 */
	public static final int DURATION_TICKS_PER_LEVEL = 200;

	/**
	 * 等级 → 效果 amplifier（0 基）。
	 *
	 * <p>需求 §3.3：原版中毒的节奏是 {@code 25 >> amplifier}，而作者要「等级 1 时与中毒一致」
	 * ⇒ {@code amplifier = 等级 − 1}（等级 1 ⇒ amplifier 0 ⇒ 25 tick 一次，与中毒 Lv1 相同）。
	 * 这也正是 {@code MobEffectInstance} 的口径（它存 0 基的 amplifier）。</p>
	 */
	public static int amplifierFor(int level) {
		return clampLevel(level) - 1;
	}

	/** 等级 → 施加时的时长（tick）= {@link #DURATION_TICKS_PER_LEVEL} × 等级（需求 §六 #1）。 */
	public static int durationTicks(int level) {
		return DURATION_TICKS_PER_LEVEL * clampLevel(level);
	}

	/** 把任意等级夹进 {@code [MIN_LEVEL, MAX_LEVEL]}（四条途径各自的等级来源都过这一道）。 */
	public static int clampLevel(int level) {
		return Mth.clamp(level, MIN_LEVEL, MAX_LEVEL);
	}

	// ==================================================================================
	// 二、扣血（批 3 接；照原版中毒的两行、去掉「不致死」那一道保护）
	// ==================================================================================

	/**
	 * 扣血触发间隔的右移基数：<b>25</b>。
	 *
	 * <p>需求 §3.3 / §0.3 #1：原版中毒是 {@code shouldApplyEffectTickThisTick} 里
	 * {@code int i = 25 >> amplifier; return i > 0 ? duration % i == 0 : true;}。
	 * ⇒ 间隔 Lv1 = 25 tick、Lv2 = 12、Lv3 = 6、Lv4 = 3、Lv5 = 1。</p>
	 */
	public static final int DAMAGE_INTERVAL_SHIFT = 25;

	/**
	 * 每次伤害：<b>1.0F</b>（半颗心）。
	 *
	 * <p>需求 §3.3：原版中毒 {@code applyEffectTick} 里就是 {@code entity.hurt(..., 1.0F)}。
	 * 「每秒」是作者的口语措辞，机制以中毒为准（需求 §3.3 的注解 + §六 #5）。</p>
	 */
	public static final float DAMAGE_PER_TICK = 1.0F;

	/**
	 * 本轮是否该扣血 —— <b>照原版中毒 {@code PoisonMobEffect#shouldApplyEffectTickThisTick}
	 * 逐字搬来</b>（coe-charge 批 3 在 {@code applyEffectTick} 里用）。
	 *
	 * <p>原版那两行：{@code int i = 25 >> amplifier; return i > 0 ? duration % i == 0 : true;}
	 * —— {@code i <= 0} 时（amplifier ≥ 5，本需求到不了）每 tick 都算。</p>
	 */
	public static boolean shouldApplyDamageThisTick(int duration, int amplifier) {
		int interval = DAMAGE_INTERVAL_SHIFT >> amplifier;
		return interval > 0 ? duration % interval == 0 : true;
	}

	// ==================================================================================
	// 三、电荷中和爆炸（批 5 接）
	// ==================================================================================

	/**
	 * 爆炸等级到伤害的倍率：<b>4</b> ⇒ 区域内每个生物吃 {@code L × 4} 点伤害。
	 *
	 * <p>需求 §3.5 #4（作者原话「该区域内扣除的血量」）。</p>
	 */
	public static final int NEUTRALIZE_DAMAGE_PER_LEVEL = 4;

	/**
	 * 爆炸范围的 Chebyshev 半径 = {@code L + 1}（需求 §3.5 #2，<b>作者裁定</b>）：
	 * 立方体、边长 {@code 2(L+1)+1}（L=1 ⇒ 边长 5 ⇒ 125 格）。
	 *
	 * <p>⚠ 是<b>立方体</b>（Chebyshev），不是球体 —— 需求 §5.3 陷阱 5。</p>
	 */
	public static int neutralizeChebyshevRadius(int explosionLevel) {
		return explosionLevel + 1;
	}

	/** 爆炸范围边长 = {@code 2(L+1)+1}（需求 §3.5 #2 的同一句话，取格子数用）。 */
	public static int neutralizeSideLength(int explosionLevel) {
		return 2 * neutralizeChebyshevRadius(explosionLevel) + 1;
	}

	/** 爆炸对单个生物造成的伤害 = {@code L × }{@link #NEUTRALIZE_DAMAGE_PER_LEVEL}（需求 §3.5 #4）。 */
	public static float neutralizeDamage(int explosionLevel) {
		return (float) explosionLevel * NEUTRALIZE_DAMAGE_PER_LEVEL;
	}

	/**
	 * 中和爆炸的最小间隔（tick）：<b>10</b>。
	 *
	 * <p>需求 §六 #7 把「残留间爆炸最小间隔」留给<b>执行会话定值并写注释</b>
	 * （防无限连锁）⇒ 执行会话 2026-10-03 定 <b>10 tick = 0.5 秒</b>：
	 * 比一次爆炸的粒子/伤害结算（同 tick 内）长得多，又短到不影响正常玩法节奏。</p>
	 */
	public static final int NEUTRALIZE_COOLDOWN_TICKS = 10;

	// ==================================================================================
	// 四、电荷残留（批 6 接）
	// ==================================================================================

	/**
	 * 残留寿命的常数项（秒）：<b>5.0</b>。
	 *
	 * <p>需求 §3.5 #6（<b>作者裁定按公式</b>）：存活时间 = {@code 5 + ln(L+1)} 秒
	 * ⇒ tick = {@code round((5 + ln(L+1)) × 20)}：L=1 ⇒ 114 tick、L=5 ⇒ 136 tick。
	 * 用 {@link #residueLifetimeTicks(int)} 取，别在调用点算。</p>
	 */
	public static final double RESIDUE_BASE_SECONDS = 5.0D;

	/** 一秒的 tick 数：<b>20</b>（原版）。公式里「秒 → tick」的换算用。 */
	public static final int TICKS_PER_SECOND = 20;

	/** 残留寿命（tick）= {@code round((5 + ln(L+1)) × 20)}（需求 §3.5 #6，作者裁定按公式）。 */
	public static int residueLifetimeTicks(int explosionLevel) {
		return (int) Math.round((RESIDUE_BASE_SECONDS + Math.log(explosionLevel + 1)) * TICKS_PER_SECOND);
	}

	/**
	 * 接触残留下的电荷等级 = {@code max(1, L − 1)}。
	 *
	 * <p>需求 §3.5 #9（作者原话：「Buff 等级等于爆炸等级减 1；若爆炸等级为 1，Buff 默认为 1」）。
	 * ⚠ {@code max} 那一下不能省：L=1 时必须给 1，不是 0（需求 §5.2 验收专门有一条）。</p>
	 */
	public static int residueInflictedLevel(int explosionLevel) {
		return Math.max(MIN_LEVEL, explosionLevel - 1);
	}

	// ==================================================================================
	// 五、四条获得途径（批 4 接；极性来源见需求 §3.2）
	// ==================================================================================

	/** 被雷电击中给的等级：<b>1</b>（需求 §3.2 #1 / §六 #3：三条随机极性途径里雷电取 1）。 */
	public static final int LIGHTNING_LEVEL = 1;

	/** 靠近特斯拉线圈给的等级：<b>1</b>（需求 §3.2 #3 / §六 #3）。 */
	public static final int TESLA_COIL_LEVEL = 1;

	/**
	 * 特斯拉线圈「通有足够电量」的门槛：<b>20 000 FE</b>。
	 *
	 * <p>需求 §3.2 #3（作者原话「大于等于 20kFE」）⇒ 恰好等于 20 000 时<b>算达标</b>（≥）。</p>
	 */
	public static final int TESLA_COIL_MIN_ENERGY_FE = 20_000;

	/**
	 * 特斯拉线圈判定间隔：<b>40</b> tick（= 2 秒）。
	 *
	 * <p>需求 §3.2 #3 的落地要点：「必须限频（例：每 40 tick 判定一次）」+ §5.3 陷阱 7
	 * （站在线圈旁不能每 tick 施加）。</p>
	 */
	public static final int TESLA_COIL_CHECK_INTERVAL_TICKS = 40;

	/** 特斯拉线圈的判定距离：<b>4.0</b> 格（需求 §3.2 #3 的「距离阈值 4 格」）。 */
	public static final double TESLA_COIL_RANGE = 4.0D;

	// ==================================================================================
	// 六、带电生物受能量场作用（批 8 接；需求 §3.7 / §六 #9）
	// ==================================================================================

	/**
	 * 生物受场的强度缩放：<b>0.05</b>（= 波那侧的 1.0 的 1/20）。
	 *
	 * <p>需求 §六 #9（「比波小」）⇒ 作者 2026-10-03 定 <b>0.05</b>。
	 * 唯一的受力真源仍是 {@code EnergyField#apply(vel, polarity, strengthScale)}
	 * （需求 §5.3 陷阱 10：别另写一套公式）。</p>
	 */
	public static final double LIVING_FIELD_STRENGTH_SCALE = 0.05D;

	/**
	 * 限幅一：<b>单 tick 速度增量上限</b> = <b>0.25</b> 格/秒。
	 *
	 * <p>需求 §六 #9 要求「必须限幅」（具体值执行会话定）⇒ 执行会话 2026-10-03 定：
	 * 调试命令的场强上限是 100（{@code EnergyFieldDebugCommands} 的
	 * {@code doubleArg(0.1, 100)}），一块场给出的单 tick 增量就是
	 * {@code 100 × 0.05 / 20 = 0.25} ⇒ <b>单块场永远打不满这一条</b>，它只截
	 * 「多块场叠加」的情形（同一个点可以同时被若干块场覆盖）。</p>
	 */
	public static final double LIVING_FIELD_MAX_DELTA_PER_TICK = 0.25D;

	/**
	 * 限幅二：<b>受场后的速度上限</b> = <b>8.0</b> 格/秒（= 0.4 格/tick）。
	 *
	 * <p>需求 §六 #9（「否则偏转场可能把玩家甩出世界/穿墙」）⇒ 执行会话 2026-10-03 定：
	 * 0.4 格/tick <b>小于玩家的碰撞箱宽度 0.6 格</b> ⇒ 单 tick 位移不可能整块穿过一个方块，
	 * 这是「不会穿墙」的量化依据；相对地，玩家步行约 0.216 格/tick、疾跑约 0.28 格/tick，
	 * 8 格/秒 已经明显能推动/偏转，但仍远低于坠落终速，不会把人甩出世界。</p>
	 */
	public static final double LIVING_FIELD_MAX_SPEED = 8.0D;

	// ==================================================================================
	// 七、表现：配色与粒子（批 1 就接；需求 §3.8 / §0.2）
	// ==================================================================================

	/**
	 * 着正电的配色：<b>{@code RGB(199,67,112)}</b>（玫红）。
	 *
	 * <p>需求 §0.2 的作者素材像素实测主色（{@code 带正电.png}）+ §3.8 的「正电 = 玫红系」。
	 * 传给 {@code MobEffect} 构造器（HUD 图标底色 / 药水色），也供批 3 的彩色粒子用。</p>
	 */
	public static final int POSITIVE_COLOR = 0xC74370;

	/**
	 * 着负电的配色：<b>{@code RGB(75,103,253)}</b>（蓝）。
	 *
	 * <p>需求 §0.2 的作者素材像素实测主色（{@code 带负电.png}）+ §3.8 的「负电 = 蓝系」。</p>
	 */
	public static final int NEGATIVE_COLOR = 0x4B67FD;

	/**
	 * 常驻粒子（主）：{@code minecraft:electric_spark}。
	 *
	 * <p>需求 §3.8：作者要「类似于电荷的效果，不是那种药水的效果」⇒ 常驻粒子<b>不用</b>
	 * 原版 {@code entity_effect}（药水/滞留粒子），改用 {@code ParticleTypes.ELECTRIC_SPARK}。
	 * 本模组<b>不注册自定义粒子类型</b>（§3.8 末段 + §四 范围边界）。</p>
	 */
	public static final ParticleOptions PARTICLE_MAIN = ParticleTypes.ELECTRIC_SPARK;

	/**
	 * 常驻粒子（点缀）：{@code minecraft:end_rod}（需求 §3.8「{@code ELECTRIC_SPARK} 为主 +
	 * 少量 {@code END_ROD}」）。批 3 起与 {@link #PARTICLE_MAIN} 按稀疏节奏混发。
	 */
	public static final ParticleOptions PARTICLE_ACCENT = ParticleTypes.END_ROD;

	/**
	 * 中和爆炸 / 残留的一次性闪光粒子：{@code minecraft:flash}（需求 §3.8：
	 * 爆炸「+ 一次 {@code FLASH}，⚠ 单次，别每 tick 刷」）。批 5 / 批 6 用。
	 */
	public static final ParticleOptions PARTICLE_FLASH = ParticleTypes.FLASH;
}
