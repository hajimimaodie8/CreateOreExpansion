package com.hjmmd_8.createoreexpansion.content.energyfield;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeEffects;
import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * <b>电荷（着正电 / 着负电）的对外公开静态门面</b>（coe-charge 批 4；需求 §3.6 的形状 +
 * §5.1 #8 的落点 + §六 #8 的方法集）。
 *
 * <p><b>它是给谁用的</b>：本模组之外的<b>雷相关模组</b>——作者原话「方便其他与雷有关的模组
 * 获得 buff」（铁魔法那类雷系法术书、冒险模组、BGE 的结雷金剑……）。那些模组<b>不需要</b>
 * 引用本仓的 effect 类、注册器或数值表：它们只引用本类 + {@link ChargePolarity}
 * （一个两常量的枚举，无注册项、无数值）。BGE 那条「结雷金剑击中 ⇒ 随机染电」就是典型使用者
 * （需求 §3.2 #2 明确要求本仓<b>不硬编码</b> BGE 的类，只能走这个 API）。</p>
 *
 * <p><b>为什么是「简单静态方法」而不是 {@code WaveMachineIntegrationPoints} 那套
 * {@code install(Sink)} 登记机制</b>（批 4 的形态判据，两者解决的不是同一个问题）：</p>
 * <ul>
 *   <li>{@code WaveMachineIntegrationPoints} 的存在理由是<b>「对端模块可能根本没装」</b>——
 *       波引擎在 {@code :coe}（第一层）、加工机联动在 {@code :cews}（第二层），单装第一层时
 *       第二层的类<b>在 classpath 上不存在</b>。一个 {@code L1 → L2} 的编译期引用既是分层红线、
 *       又会在运行期 {@code NoClassDefFoundError}；所以必须把「读别人的类」换成「谁有谁登记」，
 *       并给每个方法配一个「未装时」的安全返回值（{@code List.of()} / {@code 0} / {@code false}）。</li>
 *   <li>电荷<b>没有这个「对端」</b>：两个 {@code MobEffect} 是本模组自己的注册项
 *       （{@code CoeEffects} 在 {@code :coe}），它们<b>永远在场</b>，不存在「没装就降级」的第二种
 *       环境 ⇒ 装一个 {@code Sink} 只是凭空多一层间接、多一个可能忘记登记的静默失败点。
 *       「未装则返回安全值」在这里的对应物是<b>空实体的安全值</b>：{@code null} 实体 /
 *       身上没有这两种效果 ⇒ {@code null} / {@code 0} / {@code false}（见各方法 javadoc），
 *       而不是「模块没装」。</li>
 * </ul>
 *
 * <p><b>数值零字面量</b>：本文件<b>一个数字字符都不写</b>（连日志文本里的编号都不写，
 * 免得「数字只能来自数值真源」这条判据要靠例外才成立）：等级上下限、等级 ↔ amplifier 的换算、
 * 等级夹取一律走 {@link ChargeConfigs}（需求 §5.1 #5 的唯一真源），关卡里有负向断言守着。
 * 连「无电荷 ⇒ 等级多少」这个哨兵值也是从表里推的（见 {@link #levelOf}）。</p>
 *
 * <p><b>★ 本批的中间态：异极 ⇒ 中和爆炸「还没有落地」（那是批 5）</b>（需求 §3.5）：
 * {@link #apply} 检测到「实体身上是相反极性」时会调用 {@link #neutralize}，而那个方法
 * <b>本批是刻意留空的钩子</b>：它只记一行日志、<b>不改任何状态</b>，并带着
 * {@code TODO 批 5}。⇒ 今天的可观测行为是：异极施加 = <b>什么都不发生 + 一行日志</b>
 * （既不是「换极」、也不是「爆炸」）。<b>刻意不静默</b>的理由见 {@link #neutralize} 的 javadoc
 * （批 3 的教训：静默分支无从判断它有没有被走到）。批 5 落地时把这个钩子换成真正的爆炸，
 * 并把关卡里「钩子是空的」那条断言一起改掉。</p>
 *
 * <p><b>Holder 身份：为什么本类不需要批 2 那个 {@code is(ResourceKey)} 兜底</b>：
 * {@code ChargeEffectRemovalHandler} 需要它，是因为它在 {@code MobEffectEvent.Remove} 里拿
 * <b>两个 {@code Holder} 变量做 {@code ==}</b>，而命令路径给出的注册表 {@code Holder.Reference}
 * 与我们自己的 {@code DeferredHolder} 壳<b>不是同一个对象</b>。本类走的是
 * {@link LivingEntity#hasEffect(Holder)} / {@link LivingEntity#getEffect(Holder)}——它们查的是
 * {@code activeEffects}（一个 {@code HashMap<Holder<MobEffect>, MobEffectInstance>}），
 * 走的是 {@code hashCode}/{@code equals} 而<b>不是</b>引用相等：NeoForge 给
 * {@code Holder.Reference} 补的 {@code equals} 与 {@code DeferredHolder#equals} 互为镜像
 * （都要求对方 {@code kind() == REFERENCE}、且 {@code getKey()} 是同一个
 * {@code ResourceKey} 实例，而 {@code ResourceKey.create} 走内部缓存 ⇒ 同一个 id 必是同一个 key），
 * 两边的 {@code hashCode} 也都是 {@code key.hashCode()}。⇒ 无论实例是「我们 {@code addEffect}
 * 时塞进去的 {@code DeferredHolder}」还是「{@code /effect give} 解析出来的
 * {@code Holder.Reference}」，本类的查询都命中。</p>
 *
 * <p><b>注册时序</b>：{@code MobEffectInstance} 的构造器会调 {@code holder.value()}
 * （取 {@code MobEffect#fillEffectCures}）⇒ 本类<b>只能在 {@code RegisterEvent} 之后调用</b>
 * （正常玩法路径都在那之后；注册完成前构造实例会抛 {@code NullPointerException}）。
 * 只查询（{@link #polarityOf} / {@link #levelOf} / {@link #hasCharge} 那一路）是不需要
 * {@code value()} 的。</p>
 *
 * <p><b>不碰的东西</b>（需求 §四）：不重写 {@code ChargePolarity} / {@code EnergyField} /
 * {@code FieldedEntity}，不改中毒，不加注册项，<b>不 import 任何可选模组类</b>
 * （{@code createaddition} / {@code curios} / {@code jade} / {@code jei} 一个都不许出现 ——
 * 这是内容包红线，关卡里有负向断言）。</p>
 */
public final class ChargeApi {

	private ChargeApi() {
	}

	// ==================================================================================
	// 一、施加
	// ==================================================================================

	/**
	 * <b>给一个生物施加（或续上）某种极性的电荷</b> —— 本门面的主方法（需求 §3.6 的
	 * {@code apply(entity, polarity, level, ticks)}）。
	 *
	 * <p>三条分支，逐条对应需求 §3.6 的「施加语义」：</p>
	 * <ol>
	 *   <li><b>身上没有电荷</b> ⇒ 直接加：等级 = {@code ChargeConfigs.clampLevel(level)}、
	 *       时长 = {@code ticks}。返回值就是原版 {@code addEffect} 的返回值 —— 被<b>免疫</b>拦下
	 *       （{@code canBeAffected} 为 false）时它返回 false 且什么都没加（这正是需求 §3.2 那句
	 *       「不能用『给实体加 effect』当判据的唯一来源」的落点：本方法的 false 就是「没加上」）。</li>
	 *   <li><b>已经是同一极</b> ⇒ <b>取等级较高者 + 时长取较长者</b>，<b>不刷新缩短</b>：
	 *       {@code mergedLevel = max(现有等级, 本次等级)}、
	 *       {@code mergedTicks = max(现有剩余时长, ticks)}，再用这一个合成实例走原版
	 *       {@code addEffect}（原版的 {@code MobEffectInstance#update} 在「等级变高」时会连带
	 *       采用对方的时长、还可能把旧的塞进 {@code hiddenEffect}；本方法先把两个维度各自取最大，
	 *       所以交给它的实例<b>单调不减</b>，那两个曲折分支都不会被走到）。
	 *       ⚠ 本支<b>恒返回 true</b>：实体本来就在这一极上，「沿用」也算成功——不会因为
	 *       「等级和时长都没变强」就报失败。</li>
	 *   <li><b>身上是相反极</b> ⇒ ★ <b>中和，不是替换</b>（需求 §3.6 逐字）。
	 *       ⚠ <b>中和爆炸是批 5</b> ⇒ 本批只把这一支交给留空的 {@link #neutralize} 钩子
	 *       （记一行日志、不改状态），本方法返回 false（这次调用<b>没有</b>让实体带上
	 *       {@code polarity} 这一极）。<b>刻意不静默</b>，理由见该钩子。</li>
	 * </ol>
	 *
	 * @param entity   目标生物（{@code null} ⇒ 返回 false，不抛）
	 * @param polarity 要施加的极性（{@code null} ⇒ 返回 false，不抛）
	 * @param level    等级（会被 {@link ChargeConfigs#clampLevel} 夹进
	 *                 {@code [MIN_LEVEL, MAX_LEVEL]}）；amplifier 由
	 *                 {@link ChargeConfigs#amplifierFor} 换算，本类不写这个换算
	 * @param ticks    本次要求的时长（tick）；同极时只增不减
	 * @return 这次调用之后，实体身上是否带着 {@code polarity} 这一极
	 */
	public static boolean apply(LivingEntity entity, ChargePolarity polarity, int level, int ticks) {
		if (entity == null || polarity == null) {
			return false;
		}
		int incomingLevel = ChargeConfigs.clampLevel(level);
		ChargePolarity current = polarityOf(entity);
		if (current == null) {
			return entity.addEffect(instance(polarity, incomingLevel, ticks));
		}
		if (current != polarity) {
			// 异极 ⇒ 中和爆炸（需求 §3.5）。★ 批 5 落地：本批只有下面那个留空的钩子。
			neutralize(entity, current, levelOf(entity), polarity, incomingLevel);
			return false;
		}
		// 同极 ⇒ 取等级较高者 + 时长取较长者（不刷新缩短）。
		MobEffectInstance existing = chargedInstance(entity);
		int mergedLevel = incomingLevel;
		if (levelOf(entity) > mergedLevel) {
			mergedLevel = levelOf(entity);
		}
		int mergedTicks = ticks;
		if (existing != null && existing.getDuration() > mergedTicks) {
			mergedTicks = existing.getDuration();
		}
		entity.addEffect(instance(polarity, mergedLevel, mergedTicks));
		return true;
	}

	/**
	 * <b>随机极性</b>地施加一次电荷（需求 §3.6 的 {@code applyRandom(entity, level, ticks)}）——
	 * 给「被雷电击中」「靠近通有足够电量的特斯拉线圈」那两条途径用（需求 §3.2 #1 / #3，
	 * 它们的极性都是随机的）。
	 *
	 * <p><b>随机源口径</b>（需求 §3.2 落地要点：「用命中/实体自己的 {@code RandomSource}，
	 * 别 {@code new Random()}」）：取 {@code entity.getRandom()}——实体自己的那个
	 * {@code RandomSource}，因此同一次世界种子下可复现、也与其它随机判定共享同一个流。
	 * 本方法只负责「掷一次硬币」，随后的合并/中和语义全部委托 {@link #apply}（唯一一处口径）。</p>
	 *
	 * @param entity 目标生物（{@code null} ⇒ 返回 false）
	 * @param level  等级（同 {@link #apply}）
	 * @param ticks  时长（同 {@link #apply}）
	 * @return 同 {@link #apply}（带上的是「这次随机到的那一极」）
	 */
	public static boolean applyRandom(LivingEntity entity, int level, int ticks) {
		if (entity == null) {
			return false;
		}
		return apply(entity,
			entity.getRandom().nextBoolean() ? ChargePolarity.POSITIVE : ChargePolarity.NEGATIVE,
			level, ticks);
	}

	// ==================================================================================
	// 二、查询
	// ==================================================================================

	/**
	 * 实体身上的电荷极性；<b>没有电荷返回 {@code null}</b>（需求 §3.6 的
	 * {@code polarityOf(entity)}）。
	 *
	 * <p>只认本模组的这两个效果（{@link CoeEffects#CHARGED_POSITIVE} /
	 * {@link CoeEffects#CHARGED_NEGATIVE}）—— 别的 {@code MobEffect} 一律不参与判定。</p>
	 *
	 * <p>一个实体<b>理论上不可能同时带两种</b>（需求 §3.5：「一个实体自身不可能同时带两种
	 * ⇒ 必然是『两个实体相遇』」）；万一真的同时存在（例如两个来源同 tick 抢着施加），
	 * 本方法<b>先看正电</b>——这个次序与 {@link #chargedInstance} 共用（后者由本方法派生），
	 * 所以「报告哪一极」与「读哪个实例」永远一致。</p>
	 *
	 * @param entity 目标生物（{@code null} ⇒ 返回 {@code null}）
	 * @return {@link ChargePolarity} 或 {@code null}
	 */
	public static ChargePolarity polarityOf(LivingEntity entity) {
		if (entity == null) {
			return null;
		}
		if (entity.hasEffect(CoeEffects.CHARGED_POSITIVE)) {
			return ChargePolarity.POSITIVE;
		}
		if (entity.hasEffect(CoeEffects.CHARGED_NEGATIVE)) {
			return ChargePolarity.NEGATIVE;
		}
		return null;
	}

	/**
	 * 实体身上的电荷等级；<b>没有电荷返回「无」</b>（需求 §3.6 的 {@code levelOf(entity)}）。
	 *
	 * <p>等级 = {@code amplifier + MIN_LEVEL} 的<b>逆变换</b>，但本文件不写那个算术：它把
	 * {@code MIN_LEVEL..MAX_LEVEL} 逐个过一遍 {@link ChargeConfigs#amplifierFor}（数值真源里
	 * 那唯一一处等级 ↔ amplifier 换算）取逆（见 {@link #levelForAmplifier}）。
	 * ⇒ 登记进来的 amplifier 一定是表里的值，取逆必然命中。</p>
	 *
	 * <p><b>「无电荷」这个哨兵值也从表里推</b>：{@code ChargeConfigs.amplifierFor(MIN_LEVEL)}
	 * ——它就是「最低等级对应的 amplifier」= <b>0</b>（与 {@code ArmorSet.effectiveSet} 的
	 * 「0 = 未激活」同口径：等级从 {@code MIN_LEVEL} 起算，0 不可能是一个真等级）。
	 * 之所以不直接写 {@code 0}：本文件守「零数字字面量」这条判据（连这个哨兵都不破例），
	 * 而 {@code MIN_LEVEL = 1} 由关卡同时钉在两侧（{@code ChargeConfigs} 的常量声明 +
	 * 本节的判据），所以推导出的 0 不会悄悄变成别的数。</p>
	 *
	 * @param entity 目标生物（{@code null} ⇒ 返回「无电荷」）
	 * @return 电荷等级；无电荷时为 0
	 */
	public static int levelOf(LivingEntity entity) {
		MobEffectInstance instance = chargedInstance(entity);
		if (instance == null) {
			return ChargeConfigs.amplifierFor(ChargeConfigs.MIN_LEVEL);
		}
		return levelForAmplifier(instance.getAmplifier());
	}

	/**
	 * 实体身上是否有电荷（任意一极，需求 §3.6 的 {@code hasCharge(entity)}）——
	 * 就是 {@code polarityOf(entity) != null}，不另立第三套判据。
	 *
	 * @param entity 目标生物（{@code null} ⇒ 返回 false）
	 */
	public static boolean hasCharge(LivingEntity entity) {
		return polarityOf(entity) != null;
	}

	// ==================================================================================
	// 三、内部：钩子与换算
	// ==================================================================================

	/**
	 * ★ <b>异极相遇的落地钩子 —— 批 5 才实现，本批刻意留空</b>（需求 §3.5 电荷中和爆炸）。
	 *
	 * <p><b>本批它做什么</b>：记一行日志，<b>什么都不改</b>（不扣血、不留残留、不动两边的效果）。
	 * 也就是说：今天「异极施加」的可观测结果 = 一行日志 + 实体保持原样（既没换极、也没爆炸）。
	 * 这是<b>有意的中间态</b>，不是漏写——批 4 只做「对外 API 面」，爆炸是批 5。</p>
	 *
	 * <p><b>为什么留一行日志而不是干脆空着</b>：空分支 = 无从判断它有没有被走到
	 * （批 3 的同一课：扣血那条 {@code return true}/{@code false} 的差别也是「无报错、无日志」的
	 * 静默失败）。异极这条路在四条获得途径落地前几乎不会被触发，一旦触发就必须留下痕迹，
	 * 否则实机验收时「异极到底走没走到钩子」只能靠猜。</p>
	 *
	 * <p><b>批 5 要在这里补什么</b>（把参数摆全就是为了那时候不用改调用点）：
	 * 爆炸等级 {@code L = min(两极等级)}、中心（同一实体触发的这一形态就是实体自身；
	 * 需求 §3.5 的「两实体接触」形态是另一个触发点、由批 5 另接触发检测）、
	 * Chebyshev 半径的<b>立方体</b>范围、范围内每个生物吃 {@code L × 4} 的<b>不破坏地形</b>范围伤害、
	 * 范围内留残留（批 6）—— 数值全部从 {@link ChargeConfigs#neutralizeChebyshevRadius} /
	 * {@link ChargeConfigs#neutralizeSideLength} / {@link ChargeConfigs#neutralizeDamage} 取，
	 * 本文件依旧不写数字。</p>
	 *
	 * @param entity       被施加的对象（异极相遇的当事人）
	 * @param current      它此刻身上的极性
	 * @param currentLevel 该极的等级（{@link #levelOf}）
	 * @param incoming     这次要施加的、相反的极性
	 * @param incomingLevel 这次的等级（已夹取）
	 */
	private static void neutralize(LivingEntity entity, ChargePolarity current, int currentLevel,
			ChargePolarity incoming, int incomingLevel) {
		// TODO 批 5（电荷中和爆炸，需求 §3.5）：L = min(currentLevel, incomingLevel)；
		//   立方体范围（Chebyshev 半径 L + 1，边长 2(L + 1) + 1）、范围内每个生物吃 L × 4、
		//   绝不破坏地形、范围内留残留（批 6）；两实体接触那一形态另接触发检测。
		//   ⚠ 落地时同时改掉关卡里「这个钩子是空的」那条断言（check-armor-sets.ps1 电荷 API 节）。
		CoeCore.LOGGER.info("[电荷中和] 异极相遇（中和爆炸落地前只记录、不改状态）：{} 现为 {} Lv{}，本次施加 {} Lv{}",
			entity, current, currentLevel, incoming, incomingLevel);
	}

	/** 该极性对应的注册项（两个 {@code DeferredHolder} 都在 {@code :coe}，永远在场）。 */
	private static Holder<MobEffect> effectFor(ChargePolarity polarity) {
		return polarity == ChargePolarity.POSITIVE ? CoeEffects.CHARGED_POSITIVE : CoeEffects.CHARGED_NEGATIVE;
	}

	/** 一个「该极性 + 该等级 + 该时长」的效果实例（amplifier 走数值真源，本类不换算）。 */
	private static MobEffectInstance instance(ChargePolarity polarity, int level, int ticks) {
		return new MobEffectInstance(effectFor(polarity), ticks, ChargeConfigs.amplifierFor(level));
	}

	/** 实体身上那个电荷实例（按 {@link #polarityOf} 报出来的那一极读，两者次序永远一致）。 */
	private static MobEffectInstance chargedInstance(LivingEntity entity) {
		ChargePolarity polarity = polarityOf(entity);
		if (polarity == null) {
			return null;
		}
		return entity.getEffect(effectFor(polarity));
	}

	/**
	 * amplifier → 等级（{@link ChargeConfigs#amplifierFor} 的逆）。
	 *
	 * <p>不写 {@code amplifier + 1}：把 {@code MIN_LEVEL..MAX_LEVEL} 逐个过一遍真源里的那张换算表，
	 * 命中的就是它（因此 {@code MIN_LEVEL} 哪天变了，本方法自动跟着变，不需要有人记得回来改）。
	 * 表外的 amplifier 只可能来自本模组之外手工构造的实例，兜底用 {@code clampLevel} 夹一下。</p>
	 */
	private static int levelForAmplifier(int amplifier) {
		for (int level = ChargeConfigs.MIN_LEVEL; level <= ChargeConfigs.MAX_LEVEL; level++) {
			if (ChargeConfigs.amplifierFor(level) == amplifier) {
				return level;
			}
		}
		return ChargeConfigs.clampLevel(amplifier + ChargeConfigs.MIN_LEVEL);
	}
}
