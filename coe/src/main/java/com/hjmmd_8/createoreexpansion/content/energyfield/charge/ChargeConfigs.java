package com.hjmmd_8.createoreexpansion.content.energyfield.charge;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;

import org.joml.Vector3f;

/**
 * 电荷系统（着正电 / 着负电）—— <b>全部数值与表现常量的唯一真源</b>（coe-charge 批 1）。
 *
 * <p>口径：本文件之外<b>任何地方</b>都不许再写字面量（时长 / 等级 / 扣血 / 爆炸 / 残留 /
 * 限频 / 配色 / 粒子类型都从这里引用）。出处逐条写在各常量上方，形如「需求 §3.3」
 * （开工需求 {@code 20261003-1750_coe-charge_charge-debuffs.md}）、「作者 2026-10-03 定」
 * （作者裁定 / 理解确认里转述的裁定）、「执行会话 2026-10-03 定」（文档留给执行会话定的值）。</p>
 *
 * <p><b>批 1 只落常量、不接调用点</b>（此后逐批接上）：本类此刻的读取方是
 * {@link ChargedPositiveEffect} / {@link ChargedNegativeEffect}（配色、常驻粒子、扣血节奏）、
 * {@code common.registry.coe.CoeEffects}（注册）、{@code ChargeApi}（等级换算 / 爆炸 / 中和冷却）
 * 与 {@code ChargeResidues} + {@code ChargeResidueData}（批 7 的残留：寿命 / 等级 / 载体名 /
 * 稀疏粒子）。<b>只剩「生物受场」那一组（批 8）还没有读取方</b>——各条注释里写明它属于哪一批。</p>
 *
 * <p><b>自定义 {@code DamageType}：作者先否决、同日后改判为「按需求原文建」</b>
 * （2026-10-03，批 6 执行期间）。需求 §六 #4 原文就建议新建一个（便于区分与免疫），
 * 作者先裁定「不注册自定义 DamageType」⇒ 批 1~3 走的是 {@code damageSources().magic()}；
 * 随后改判 ⇒ 现在本表持有 {@link #CHARGE_DAMAGE_TYPE}（{@code createoreexpansion:charge}），
 * 两个效果的每次扣血与中和爆炸的范围伤害<b>全部</b>改走它，而且<b>只有这一个</b>
 * 自定义伤害类型（数据包侧见该常量的 javadoc；旧那条「不得出现自定义伤害类型」的负向
 * 断言已按这条裁定<b>反向改口径</b>，见关卡 {@code charge-one-custom-damage-type}）。</p>
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
	 * 每一级的时长（tick）：<b>100</b> tick = 5 秒。
	 *
	 * <p><b>作者 2026-10-03 裁定（批 6 执行期间改判）</b>：一级 5 秒、二级 10 秒、三级 15 秒、
	 * 四级 20 秒、五级 25 秒 ⇒ {@code 时长 = 100 tick × 等级}。
	 * ⚠ 需求文档 §六 #1 原本推的是 {@code 200 tick × 等级}（10/20/30/40/50 秒），
	 * <b>已被这条裁定覆盖</b>。改这一个数就同时改五档（公式化的意义正在于此），
	 * 引用它的四处（三条获得途径 + 对外 API 的示例）一处都不用动。</p>
	 *
	 * <p>用 {@link #durationTicks(int)} 取，别在调用点乘。</p>
	 */
	public static final int DURATION_TICKS_PER_LEVEL = 100;

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
	// 一之二、自定义伤害类型（批 6 按作者裁定新增；需求 §六 #4 的原文方案）
	// ==================================================================================

	/**
	 * <b>电荷伤害的自定义伤害类型 id：{@code createoreexpansion:charge}</b>
	 * （作者 2026-10-03 裁定 —— 批 6 执行期间改判，覆盖了此前「不注册自定义 DamageType」的裁定）。
	 *
	 * <p>需求 §六 #4 原文就是「新建一个自定义伤害类型（例 {@code createoreexpansion:charge}）」
	 * （理由：便于区分与免疫）。作者先说「不注册」，随后改判<b>按需求原文办</b> ⇒
	 * 现在它回来了，而且<b>只有这一个</b>：两个效果的每次扣血（{@code applyEffectTick}）
	 * 与中和爆炸的范围伤害<b>全部</b>走本键，见两处调用点。</p>
	 *
	 * <p><b>数据包侧</b>：{@code coe/src/main/resources/data/createoreexpansion/damage_type/charge.json}
	 * （{@code message_id = "charge"} + {@code scaling = "when_caused_by_living_non_player"} +
	 * {@code exhaustion = 0.0}，与中毒/magic 同一档口径）。伤害类型是<b>数据包注册表</b>，
	 * 没有代码注册这一步，所以本表只持有这个 {@code ResourceKey}；死亡消息键
	 * {@code death.attack.charge} 与 {@code death.attack.charge.player} 写在两个语言 provider 里
	 * （英语 {@code EnglishLangProvider} / 中文 {@code ChineseLangProvider}），
	 * 由 {@code runData} 铺到全部语言拷贝。</p>
	 *
	 * <p>⚠ 本类因此<b>必须</b> import {@code DamageType} / {@code ResourceKey} /
	 * {@code Registries}：旧口径那条「电荷文件不得出现这些名字」的负向断言已按本裁定
	 * <b>反向改口径</b>（现在是「必须恰好有这一个、且只能有这一个」）。</p>
	 */
	public static final ResourceKey<DamageType> CHARGE_DAMAGE_TYPE =
		ResourceKey.create(Registries.DAMAGE_TYPE, CoeCore.modLoc("charge"));

	// ==================================================================================
	// 二、扣血（批 3 已接；照原版中毒的两行、去掉「不致死」那一道保护）
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
	 *
	 * <p>伤害<b>种类</b>不在这里，它不是一个「数」：两个效果取的是
	 * {@code entity.damageSources().magic()}（与既有嬗乱效果同口径，见
	 * {@code ChargedPositiveEffect#applyEffectTick}）。⇒ 致死时的死亡消息是原版的
	 * 「被魔法杀死」那一类，而不是「中毒」；作者若要专属死亡消息，改的是那一处调用，
	 * 不是本表，也仍然<b>不新增自定义 {@code DamageType}</b>。</p>
	 */
	public static final float DAMAGE_PER_TICK = 1.0F;

	/**
	 * 本轮是否该扣血 —— <b>照原版中毒 {@code PoisonMobEffect#shouldApplyEffectTickThisTick}
	 * 逐字搬来</b>（coe-charge 批 3 已在两个效果的 {@code applyEffectTick} 里接上）。
	 *
	 * <p>原版那两行：{@code int i = 25 >> amplifier; return i > 0 ? duration % i == 0 : true;}
	 * —— {@code i <= 0} 时（amplifier ≥ 5，本需求到不了）每 tick 都算。</p>
	 */
	public static boolean shouldApplyDamageThisTick(int duration, int amplifier) {
		int interval = DAMAGE_INTERVAL_SHIFT >> amplifier;
		return interval > 0 ? duration % interval == 0 : true;
	}

	// ==================================================================================
	// 三、电荷中和爆炸（批 6 已接；等级 / 范围 / 伤害 / 冷却 / 中心与扣血口径）
	// ==================================================================================

	/**
	 * 爆炸等级到伤害的倍率：<b>2</b> ⇒ 区域内每个生物吃 {@code L × 2} 点伤害
	 * （Lv1 = 2 点、Lv5 = 10 点）。
	 *
	 * <p>需求 §3.5 #4（作者原话「该区域内扣除的血量」）。
	 * <b>作者 2026-10-03 裁定（批 6 执行期间改判）：这里从 4 调到 2</b>
	 * —— Lv5 的范围内伤害从 20 点降到 10 点。改这一个数就同时改五档。</p>
	 */
	public static final int NEUTRALIZE_DAMAGE_PER_LEVEL = 2;

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

	/**
	 * 中和爆炸中心的<b>中点插值系数</b>：<b>0.5</b> ⇒ 爆炸中心 = 两实体位置的中点（需求 §六 #6）。
	 *
	 * <p>为什么不写在中和的实现里：那个文件的判据是「<b>一个数字字符都不许出现</b>」
	 * （关卡 {@code charge-api-no-literals} 逐字符守着 {@code ChargeApi}），连 {@code 0.5}
	 * 也不行 ⇒ 数值只能住在本表、按名引用。只有一方参与中和时（异极由外部施加）中心退化成
	 * 该方自身的位置，不经过这个系数。</p>
	 */
	public static final double NEUTRALIZE_MIDPOINT_WEIGHT = 0.5D;

	/**
	 * 中和爆炸扣血前<b>写回目标 {@code invulnerableTime} 的值：0</b>。
	 *
	 * <p><b>为什么非清不可</b>（不清就是静默无效）：原版 {@code LivingEntity#hurt} 在
	 * {@code invulnerableTime > 10} 时只结算「比上一击更大的那一部分」——{@code amount <= lastHurt}
	 * 直接 {@code return false}。而中和的触发点常常就落在<b>同一 tick 刚刚结算过的另一刀</b>上
	 * （被雷击 / 被带电波击中 / 被雷鸣合金武器命中，三条都是「先扣血、再染电」），那一刀已经把帧拉满
	 * ⇒ 不清的话 {@code L × 4} 会<b>整段被吃掉、一点血都不掉</b>，而且没有任何报错。
	 * 清帧后立刻 {@code hurt} ⇒ 冷却窗口由本次爆炸重新开启（保护长度不变），
	 * 总伤害 = 原来那一下 + {@code L × 4}。仓内同一处口径的先例：
	 * {@code WaveEssenceEffects#hurtThroughIFrames}（水/地魔素的额外伤害就是这么做才生效的）。</p>
	 */
	public static final int NEUTRALIZE_INVULNERABLE_TIME = 0;

	// ==================================================================================
	// 四、电荷残留（批 7 已接：寿命 / 等级在下面，载体名与稀疏粒子在「四之二」）
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

	/**
	 * ★ <b>残留的持久化载体名（{@code SavedData} 的 id）</b>：
	 * {@code createoreexpansion_charge_residues}。
	 *
	 * <p><b>为什么这个字符串住在本表</b>：它是一把<b>存档键</b>（与配置键 / 语言键 / 数据包路径
	 * 同一类红线 —— 改掉就等于把老存档里的残留整片丢掉），所以与数值一样<b>只许有一处声明</b>，
	 * 调用点按名引用（关卡钉着「声明 + 恰好一处使用」）。</p>
	 *
	 * <p><b>老存档（根本没有这个文件）的行为 = 无残留</b>：维度数据存储拿这张表时，
	 * 文件不存在就<b>直接调工厂的构造器</b>（得到一张空表），{@code load} 只在文件存在时才被调用；
	 * 而 {@code load} 读的是一个 {@code getList(..)}（缺键返回空列表，不抛）
	 * ⇒ 老存档进游戏后残留数恒为 0，<b>不报错、不需要任何迁移代码</b>。
	 * 关卡里有断言同时钉住这两半（键名 + 缺失即空表）。</p>
	 *
	 * <p><b>⚠ 零新注册</b>：{@code SavedData} 是<b>逐维度的存档附件</b>，不占任何
	 * {@code Registries} 条目、不需要 {@code DeferredRegister}、不进数据包
	 * —— 本批因此<b>一个注册项都没有新增</b>（关卡有负向断言）。</p>
	 */
	public static final String RESIDUE_DATA_NAME = "createoreexpansion_charge_residues";

	/**
	 * 残留稀疏粒子的发射间隔（tick）：<b>10</b>（= 半秒一次）。
	 *
	 * <p>需求 §3.5 #7：残留「同样用爆炸式粒子，但<b>更稀疏</b>」⇒ 爆炸是「一次性一大把」，
	 * 残留是「存活期内连续、每次一小把」。间隔取 10 tick：{@link #residueLifetimeTicks(int)}
	 * 的 114~136 tick 寿命里，一条残留发 11~13 次。</p>
	 */
	public static final int RESIDUE_PARTICLE_INTERVAL_TICKS = 10;

	/**
	 * 残留每次发射的<b>每种颜色</b>粒子颗数基数：<b>3</b>。
	 *
	 * <p>对照爆炸的 {@link #NEUTRALIZE_PARTICLE_BASE} = 30（两色各发一份）⇒ 每次发射的
	 * 密度只有爆炸的十分之一，「稀疏」由此成立。</p>
	 */
	public static final int RESIDUE_PARTICLE_BASE = 3;

	/** 残留每次发射的<b>每级增量</b>：<b>1</b>（爆炸是 {@link #NEUTRALIZE_PARTICLE_PER_LEVEL} = 20）。 */
	public static final int RESIDUE_PARTICLE_PER_LEVEL = 1;

	/**
	 * 一条残留在<b>一次发射</b>里的主粒子颗数（<b>每种颜色</b>）
	 * = {@link #RESIDUE_PARTICLE_BASE} + 等级 × {@link #RESIDUE_PARTICLE_PER_LEVEL}。
	 *
	 * <p>⚠ 残留<b>不发 {@code FLASH}</b>：那个粒子的口径是 {@link #NEUTRALIZE_FLASH_COUNT}
	 * 「一次中和恰好一颗」（需求 §3.8 原话「⚠ 单次，别每 tick 刷」）—— 残留每 10 tick 发一次，
	 * 跟着刷 FLASH 就是把那条口径反过来做。</p>
	 */
	public static int residueParticleCount(int explosionLevel) {
		return RESIDUE_PARTICLE_BASE + explosionLevel * RESIDUE_PARTICLE_PER_LEVEL;
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
	 * 爆炸「+ 一次 {@code FLASH}，⚠ 单次，别每 tick 刷」）。批 6 的中和爆炸用
	 * （颗数 {@link #NEUTRALIZE_FLASH_COUNT}）；批 7 的残留表现若沿用同一套，也取这里。
	 */
	public static final ParticleOptions PARTICLE_FLASH = ParticleTypes.FLASH;

	// ----------------------------------------------------------------------------------
	// 中和爆炸的一次性表现（批 6 接）—— 上面三条是常驻/共用粒子，这一块只服务爆炸那一下
	// ----------------------------------------------------------------------------------

	/**
	 * 中和爆炸的<b>正电色粒子</b>：{@code minecraft:dust}，颜色 = {@link #POSITIVE_COLOR}
	 * （玫红，取作者图标主色）。
	 *
	 * <p>需求 §3.8：中和爆炸是「爆炸式<b>两色混合</b>（正电色 + 负电色）+ 一次 {@code FLASH}」。
	 * 两个颜色常量本来只有 {@code int} 形态（HUD 图标底色用），而粒子要
	 * {@code ParticleOptions} ⇒ 在这里（数值真源）做一次 {@code 0xRRGGBB → 三分量 0~1}
	 * 的换算，调用点仍然只按名取粒子。</p>
	 */
	public static final ParticleOptions PARTICLE_NEUTRALIZE_POSITIVE = dustOf(POSITIVE_COLOR);

	/** 中和爆炸的<b>负电色粒子</b>：同 {@link #PARTICLE_NEUTRALIZE_POSITIVE}，颜色 = {@link #NEGATIVE_COLOR}（蓝）。 */
	public static final ParticleOptions PARTICLE_NEUTRALIZE_NEGATIVE = dustOf(NEGATIVE_COLOR);

	/**
	 * 中和爆炸主粒子的<b>颗数基数</b>：<b>30</b>（每种颜色各发这么多 + 等级增量）。
	 *
	 * <p>需求 §3.8 只写「爆炸式两色混合」，没给密度 ⇒ 执行会话 2026-10-03 定：
	 * 与既有波爆炸（{@code ChargerWaveFx#boomParticleCount} = {@code 30 + 等级 × 25}，
	 * 1 级 55 颗）同一量级，但中和是<b>两色各发一份</b>、还要留出 FLASH，故增量取 20。
	 * 逐级：Lv1 两色各 50（合计 100）、Lv5 各 130（合计 260）。</p>
	 */
	public static final int NEUTRALIZE_PARTICLE_BASE = 30;

	/** 中和爆炸主粒子的<b>每级增量</b>：<b>20</b>（算式见 {@link #neutralizeParticleCount(int)}）。 */
	public static final int NEUTRALIZE_PARTICLE_PER_LEVEL = 20;

	/** 中和爆炸主粒子的散布半径（各轴，格）。【我定：爆炸是立体的，比绽放（0.5）散、比整波碰撞（1.2）略大】 */
	public static final double NEUTRALIZE_PARTICLE_SPREAD = 1.2D;

	/** 中和爆炸主粒子的初速系数。【我定：与既有波爆炸同值（0.15），密度更高时观感一致】 */
	public static final double NEUTRALIZE_PARTICLE_SPEED = 0.15D;

	/**
	 * 中和爆炸的 {@code FLASH} 颗数：<b>1</b>（<b>恰好一次</b>）。
	 *
	 * <p>需求 §3.8 的「⚠ 单次，别每 tick 刷」：这个常量钉住「一次中和 = 一颗闪光」，
	 * 免得将来有人把爆炸密度的算式顺手套到闪光上（{@code FLASH} 是强闪光粒子，多刷会糊屏）。</p>
	 */
	public static final int NEUTRALIZE_FLASH_COUNT = 1;

	/** 中和爆炸主粒子颗数（<b>每种颜色</b>）= {@link #NEUTRALIZE_PARTICLE_BASE} + 等级 × {@link #NEUTRALIZE_PARTICLE_PER_LEVEL}。 */
	public static int neutralizeParticleCount(int explosionLevel) {
		return NEUTRALIZE_PARTICLE_BASE + explosionLevel * NEUTRALIZE_PARTICLE_PER_LEVEL;
	}

	/**
	 * 把 {@code 0xRRGGBB} 拆成 {@code minecraft:dust} 要的三分量（0~1）并打包成粒子选项
	 * （尺度取原版默认 1.0，不随等级变——大小是观感常量，不该跟着爆炸等级漂）。
	 */
	private static ParticleOptions dustOf(int rgb) {
		return new DustParticleOptions(new Vector3f(
			((rgb >> 16) & 0xFF) / 255.0F,
			((rgb >> 8) & 0xFF) / 255.0F,
			(rgb & 0xFF) / 255.0F), 1.0F);
	}
}
