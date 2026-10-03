package com.hjmmd_8.createoreexpansion.content.energyfield;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeEffects;
import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;
import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeEffectRemovalHandler;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

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
 * <p><b>★ 中和爆炸（批 6 已落地；需求 §3.5 / §3.6）</b>：{@link #apply} 检测到「实体身上是
 * 相反极性」时会调用 {@link #neutralize}，后者与<b>两实体接触检测</b>（同包
 * {@code ChargeContactHandler} 每 tick 调 {@link #checkContact}）汇进同一个
 * {@code detonate(..)}：等级 {@code L = min(两极等级)}；中心 = 两实体位置<b>中点</b>
 * （只有一方时退化成该方位置，需求 §六 #6）；Chebyshev 半径 {@code L + 1} 的<b>立方体</b>内
 * <b>每个</b>生物吃 {@code L × 2}（走自定义伤害类型 {@link ChargeConfigs#CHARGE_DAMAGE_TYPE}、
 * 先清无敌帧、创造玩家除外）；
 * <b>绝不破坏地形</b>；参与中和的每一方<b>两种</b>电荷效果都被移除；并按
 * {@link ChargeConfigs#NEUTRALIZE_COOLDOWN_TICKS} 双方各记一笔账
 * （防同一对贴身时每 tick 反复爆——需求没写、但几何上必然发生的洞）。
 * 残留仍是批 7：本类只在中和点留了具名钩子 {@link #leaveResidue}
 * （带 {@code TODO 批 7}，并记一行日志 —— <b>刻意不静默</b>，批 3 的同一课）。</p>
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
	 *       这一支交给 {@link #neutralize} —— 批 6 起它就是<b>真正的爆炸</b>
	 *       （扣血 / 移除双方的两种效果 / 记账 / 粒子，逐条见那里的 javadoc），
	 *       本方法返回 false：这次调用<b>没有</b>让实体带上 {@code polarity} 这一极
	 *       （中和之后它两种电都不带）。</li>
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
			// 异极 ⇒ 中和爆炸（需求 §3.5 / §3.6，批 6 已落地）：交给 neutralize，本方法返回 false。
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
	// 三、内部：中和爆炸、接触检测与换算
	// ==================================================================================

	/**
	 * 「下一次可以中和的时刻」写在实体持久数据上的键（{@link #stampNeutralization} 写、
	 * {@link #neutralizationOnCooldown} 读）。
	 *
	 * <p>带命名空间前缀（{@code coe_}）的理由与既有波桥冷却键 {@code co_wave_charge_cd_until}
	 * 相同：实体的持久数据是所有模组共用的一块 NBT，裸名会撞车。用「到期时刻」而不是
	 * 「上次中和 tick」的理由见 {@link #stampNeutralization}。</p>
	 */
	private static final String NEUTRALIZE_UNTIL_TAG = "coe_charge_neutralize_until";

	/**
	 * ★ <b>异极相遇 ⇒ 电荷中和爆炸：『异极由外部施加』这一形态的唯一入口</b>
	 * （coe-charge 批 6；需求 §3.5 / §3.6）。
	 *
	 * <p><b>两条触发形态</b>：{@link #apply} 的第三条分支走这里；需求 §3.5 的
	 * 「两个带电实体<b>接触</b>（hitbox 相交）」走 {@link #checkContact}。
	 * 两条最终都汇进 {@link #detonate}（唯一实现），所以「等级 / 中心 / 范围 / 伤害 /
	 * 移除 / 记账 / 粒子」只有一份代码。</p>
	 *
	 * <p><b>为什么第二方是 {@code null}</b>：调用点（{@code apply}）手上只有当事人一个实体
	 * —— 相反的极性是<b>外部来源</b>（雷击 / 特斯拉线圈 / 带电波 / 雷鸣合金武器技能）施加的，
	 * 没有实体可传 ⇒ 中和中心退化成当事人自身的位置。需求 §六 #6 的「两实体位置中点」
	 * 属于接触那一形态，由 {@link #centerOf} 在同一处实现。</p>
	 *
	 * <p><b>五个参数一个都没白给</b>：两个等级取 {@code min} 就是爆炸等级 {@code L}
	 * （需求 §3.5 #1）；两个极性进事件流日志（谁是正电、谁是负电，实机验收只能靠日志认）。</p>
	 *
	 * <p><b>方法名与签名刻意不动</b>：关卡 §38 钉着调用点的逐字形状
	 * （{@code neutralize(entity, current, levelOf(entity), polarity, incomingLevel);}）
	 * 与「这个钩子存在」；本批只换它的<b>内部</b>——五个公开签名与那一条调用形状都不变。</p>
	 *
	 * @param entity        被施加的对象（异极相遇的当事人）
	 * @param current       它此刻身上的极性
	 * @param currentLevel  该极的等级（{@link #levelOf}）
	 * @param incoming      这次要施加的、相反的极性
	 * @param incomingLevel 这次的等级（已夹取）
	 */
	private static void neutralize(LivingEntity entity, ChargePolarity current, int currentLevel,
			ChargePolarity incoming, int incomingLevel) {
		detonate(entity, null, current, currentLevel, incoming, incomingLevel);
	}

	/**
	 * <b>中和爆炸的唯一实现</b>（两条触发形态都汇到这里）—— 顺序是刻意的，逐条对应需求：
	 * <ol>
	 *   <li><b>等级</b> {@code L = min(两极等级)}（需求 §3.5 #1）；</li>
	 *   <li><b>中心</b> = 两方位置<b>中点</b>（只有一方时 = 该方自身位置；需求 §六 #6）；</li>
	 *   <li><b>先移除双方的两种效果</b>（需求 §3.5「中和 = 都消失」）—— 放在扣血之前：
	 *       爆炸落定之后没有任何一方还带着电，否则几何上必然出现「下一 tick 又中和」；</li>
	 *   <li><b>记账</b>：双方各写一笔「下一次可中和的时刻」（{@link #stampNeutralization}）；</li>
	 *   <li><b>范围伤害</b>：立方体内每个生物 {@code L × 2}（{@link #hurtInCube}）；</li>
	 *   <li><b>粒子</b>：两色混合 + 恰好一次 {@code FLASH}（{@link #sendBurst}）；</li>
	 *   <li><b>残留钩子</b>：批 7（{@link #leaveResidue}，刻意不静默）。</li>
	 * </ol>
	 *
	 * @param first          参与中和的第一方（{@code apply} 那条 = 持现极性的当事人；接触那条任一方）
	 * @param second         第二方；{@code null} = 异极由外部施加，本次只有一方
	 * @param firstPolarity  第一方此刻的极性（进日志）
	 * @param firstLevel     第一方的等级
	 * @param secondPolarity 第二方（或外部施加）的极性（进日志）
	 * @param secondLevel    第二方（或外部施加）的等级
	 */
	private static void detonate(LivingEntity first, LivingEntity second, ChargePolarity firstPolarity,
			int firstLevel, ChargePolarity secondPolarity, int secondLevel) {
		int level = Math.min(firstLevel, secondLevel);
		Center center = centerOf(first, second);
		removeCharges(first);
		if (second != null) {
			removeCharges(second);
		}
		stampNeutralization(first);
		if (second != null) {
			stampNeutralization(second);
		}
		// 伤害来源：走电荷自己的自定义伤害类型（ChargeConfigs.CHARGE_DAMAGE_TYPE，
		// 作者 2026-10-03 批 6 期间改判：按需求 §六 #4 原文新建），攻击者取第二方（接触形态）；
		// 只有一方时归当事人 —— 调用点没有别的实体可指名。不传 null 是因为 null 会让
		// 死亡消息取不到攻击者（会落到 death.attack.charge.player 那条兜底上）。
		hurtInCube(first.level(), second == null ? first : second, center, level);
		sendBurst(first.level(), center, level);
		leaveResidue(center, level);
		CoeCore.LOGGER.info(
			"[电荷中和] {}（{} Lv{}）与 {}（{} Lv{}）中和：中心 {}、爆炸等级 Lv{}、立方体边长 {} 格、范围内每个生物扣 {} 点、不破坏地形",
			first.getName().getString(), firstPolarity, firstLevel,
			second == null ? "外部施加的第二极" : second.getName().getString(), secondPolarity, secondLevel,
			center, level, ChargeConfigs.neutralizeSideLength(level), ChargeConfigs.neutralizeDamage(level));
	}

	/**
	 * <b>两实体接触检测</b>（需求 §3.5 的触发条件：一个着正电的实体与一个着负电的实体
	 * <b>碰撞箱相交</b>）—— 由同包的 {@code ChargeContactHandler} 每 tick 对每个生物调一次。
	 *
	 * <p><b>为什么必须有这个触发点</b>：另外几条途径都是「某件事发生 ⇒ 调 {@link #apply}」，
	 * 而「两个带电生物贴在一起」<b>不产生任何事件</b> ⇒ 只在 {@code apply} 里落地中和的话，
	 * 作者要的那一幕（正电生物撞上负电生物 ⇒ 爆炸）永远不会发生，而且没有任何报错。</p>
	 *
	 * <p><b>三道最便宜的闸，顺序即开销顺序</b>：① 不带电（绝大多数实体）⇒ 立刻返回；
	 * ② 自己还在中和冷却里 ⇒ 返回（双方都会记账，见 {@link #stampNeutralization}）；
	 * ③ 才去扫碰撞箱找相反极性的那一方，找不到就什么都不做。</p>
	 *
	 * <p><b>包级私有、不是对外 API</b>：它是给同包驱动用的内部入口——五个公开方法之外的
	 * 第六个<b>公开</b>成员一个都不加（§38 的 surface 是「恰好这五个」的对外承诺）。</p>
	 *
	 * @param entity 本 tick 走到检查的那个生物（可能不带电）
	 */
	static void checkContact(LivingEntity entity) {
		ChargePolarity current = polarityOf(entity);
		if (current == null || neutralizationOnCooldown(entity)) {
			return;
		}
		ChargePolarity wanted = oppositeOf(current);
		LivingEntity counterpart = findContactingCounterpart(entity, wanted);
		if (counterpart == null) {
			return;
		}
		detonate(entity, counterpart, current, levelOf(entity), wanted, levelOf(counterpart));
	}

	/**
	 * 与 {@code entity} 的<b>碰撞箱相交</b>（需求 §3.5 的「接触」= hitbox 相交）、
	 * 且身上恰是 {@code wanted} 这一极的生物；取扫到的<b>第一个</b>，没有则 {@code null}。
	 *
	 * <p>只取一个：同一个 tick 里先被扫到的那一对先中和，双方效果随即都被移除 ⇒
	 * 紧接着处理另一方时它已经不带电（{@link #checkContact} 的第一道闸直接返回），
	 * 不会出现「一对多」的连环爆炸。</p>
	 */
	private static LivingEntity findContactingCounterpart(LivingEntity entity, ChargePolarity wanted) {
		for (LivingEntity candidate : entity.level().getEntitiesOfClass(LivingEntity.class,
				entity.getBoundingBox(), LivingEntity::isAlive)) {
			if (candidate != entity && polarityOf(candidate) == wanted) {
				return candidate;
			}
		}
		return null;
	}

	/** 相反极性（{@code ChargePolarity} 只提供 {@code sign()}；本仓取反的唯一一处）。 */
	private static ChargePolarity oppositeOf(ChargePolarity polarity) {
		return polarity == ChargePolarity.POSITIVE ? ChargePolarity.NEGATIVE : ChargePolarity.POSITIVE;
	}

	/** 中和中心：两方都在 ⇒ 位置<b>中点</b>（需求 §六 #6）；只有一方 ⇒ 该方自身的位置。 */
	private static Center centerOf(LivingEntity first, LivingEntity second) {
		var firstPos = first.position();
		if (second == null) {
			return new Center(firstPos.x, firstPos.y, firstPos.z);
		}
		var secondPos = second.position();
		double weight = ChargeConfigs.NEUTRALIZE_MIDPOINT_WEIGHT;
		return new Center(
			firstPos.x + (secondPos.x - firstPos.x) * weight,
			firstPos.y + (secondPos.y - firstPos.y) * weight,
			firstPos.z + (secondPos.z - firstPos.z) * weight);
	}

	/**
	 * <b>中和中心</b>（世界坐标三元组）—— 一个只在本类内部流转的小记录。
	 *
	 * <p><b>为什么不用 {@code Vec3}</b>：本文件守「<b>零数字字符</b>」这条判据
	 * （关卡 {@code charge-api-no-literals} 逐字符扫过整份源码），而那个类型名自带一个数字
	 * ⇒ 中心改成用三个 {@code double} 携带；读的位置一律 {@code .x()} / {@code .y()} / {@code .z()}。
	 * 这是本文件唯一一处「为了判据而选的形状」，理由记在这里免得后人"顺手改回 Vec3"。</p>
	 */
	private record Center(double x, double y, double z) {
	}

	/**
	 * 把一方身上<b>两种</b>电荷效果都去掉（中和 = 都消失，需求 §3.5）—— 逐个调，
	 * 不写「清空全部效果」（那会顺手把嬗乱、缓慢之类别人的效果一起抹掉）。
	 *
	 * <p>⚠ <b>必须走 {@link ChargeEffectRemovalHandler#removeForNeutralization}</b>，
	 * 不能直接 {@code entity.removeEffect(..)}：批 2 那道挡牛奶的闸门拦的正是这两个效果的
	 * {@code MobEffectEvent.Remove}，而 {@code removeEffect} 的<b>第一句</b>就是 post 那个事件
	 * ⇒ 直接调会返回 false、<b>效果一个都去不掉，而且什么都不报</b>
	 * （逐句理由与实测出处见那个方法）。</p>
	 */
	private static void removeCharges(LivingEntity entity) {
		ChargeEffectRemovalHandler.removeForNeutralization(entity, CoeEffects.CHARGED_POSITIVE);
		ChargeEffectRemovalHandler.removeForNeutralization(entity, CoeEffects.CHARGED_NEGATIVE);
	}

	/**
	 * <b>记账：把「下一次可以中和的时刻」写在实体自己的持久数据上</b>
	 * （= 本次 {@code level.getGameTime()} + {@link ChargeConfigs#NEUTRALIZE_COOLDOWN_TICKS}）。
	 *
	 * <p><b>为什么需要这道闸</b>（需求没写、但几何上必然发生的洞）：中和的触发点之一是
	 * 「碰撞箱相交」，而两个实体贴在一起可以持续任意多 tick。今天「爆炸之后双方效果都被移除」
	 * 已经挡住绝大多数重复，但只要两边在冷却窗口内又被重新染电（同 tick 的两次施加、
	 * 将来批 7 的残留、或者站在通电线圈旁），同一对就会<b>每 tick 再爆一次</b>——
	 * 那就是可感知的掉帧与「爆炸风暴」（需求 §5.2「防连锁」那条专门要实测）。
	 * 双方各记一笔 ⇒ 任意一方在窗口内就不再中和。</p>
	 *
	 * <p><b>为什么记「到期时刻」而不是「上次中和 tick」</b>：缺失的 NBT 键读出来是
	 * {@code 0}，而「没记过账」必须落在「不在冷却中」那一侧。比较 {@code now < until}
	 * （与既有波桥 {@code co_wave_charge_cd_until} 同一形状）天然满足：新实体 until = 0
	 * ⇒ 立刻可中和。反过来写成「上次中和 tick」再算差值的话，刚开服的那几个 tick
	 * （gameTime 还小于冷却长度）会被误判成「在冷却里」——一个只在新世界头半秒出现、
	 * 且完全没有日志的洞。</p>
	 */
	private static void stampNeutralization(LivingEntity entity) {
		entity.getPersistentData().putLong(NEUTRALIZE_UNTIL_TAG,
			entity.level().getGameTime() + ChargeConfigs.NEUTRALIZE_COOLDOWN_TICKS);
	}

	/** 该实体是否还在中和冷却里（{@link #stampNeutralization} 写、这里读，同一个键）。 */
	private static boolean neutralizationOnCooldown(LivingEntity entity) {
		return entity.level().getGameTime() < entity.getPersistentData().getLong(NEUTRALIZE_UNTIL_TAG);
	}

	/**
	 * <b>范围伤害</b>（需求 §3.5 #2/#4/#5/#10）：以 {@code center} 为中心、Chebyshev 半径
	 * {@code L + 1} 的<b>立方体</b>内<b>每个</b>生物扣 {@code L × 2}。
	 *
	 * <ul>
	 *   <li><b>立方体不是球</b>（需求 §5.3 陷阱 5）：判据就是
	 *       {@code new AABB(center, center).inflate(ChargeConfigs.neutralizeChebyshevRadius(L))}
	 *       —— 一个以中心为心的整块盒，与「Chebyshev ≤ L+1 的所有格」同一形状；
	 *       半径按名取自数值真源（本文件一个数字都不写）；</li>
	 *   <li><b>两个当事方自己就在盒子里</b>（需求 §3.5 #10）：接触的一对相距不到一格、
	 *       半径至少两格 ⇒ 双方都吃这一下，不必为「当事人」另补一次伤害；</li>
	 *   <li><b>伤害类型</b>：{@link ChargeConfigs#CHARGE_DAMAGE_TYPE}
	 *       （{@code createoreexpansion:charge}）—— 作者 2026-10-03 改判后，
	 *       两个效果的扣血与这里的范围伤害<b>共用同一个</b>自定义类型；</li>
	 *   <li><b>不破坏地形</b>（需求 §3.5 #5）：整条路只有 {@code hurt}，
	 *       没有 {@code Level#explode}、没有任何方块操作（关卡有全仓负向断言守着）；</li>
	 *   <li><b>创造玩家除外</b>：与既有 {@code ChargerWaveFx#triggerBoom} /
	 *       {@code AbstractChargerWaveEntity} 同一形状——只跳过扣血，画面上照旧看得见爆炸；</li>
	 *   <li><b>先清无敌帧</b>：见 {@link ChargeConfigs#NEUTRALIZE_INVULNERABLE_TIME}
	 *       （不清就是「这一下被刚挨的那一刀整段吃掉、一点血都不掉」，而且不报错）。</li>
	 * </ul>
	 *
	 * @param world  两方所在世界（伤害来源也从它取）
	 * @param source 伤害的攻击者（有第二方 = 第二方；只有一方 = 当事人自己）
	 */
	private static void hurtInCube(Level world, LivingEntity source, Center center, int level) {
		double radius = ChargeConfigs.neutralizeChebyshevRadius(level);
		AABB cube = new AABB(center.x() - radius, center.y() - radius, center.z() - radius,
			center.x() + radius, center.y() + radius, center.z() + radius);
		float damage = ChargeConfigs.neutralizeDamage(level);
		for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, cube, LivingEntity::isAlive)) {
			if (target instanceof Player player && player.isCreative()) {
				continue;
			}
			target.invulnerableTime = ChargeConfigs.NEUTRALIZE_INVULNERABLE_TIME;
			target.hurt(world.damageSources().source(ChargeConfigs.CHARGE_DAMAGE_TYPE, source), damage);
		}
	}

	/**
	 * <b>中和爆炸的粒子：两色混合 + 恰好一次 {@code FLASH}</b>（需求 §3.8）。
	 *
	 * <p>三种粒子全部是原版（{@code minecraft:dust} 两份 + {@code minecraft:flash} 一次），
	 * 颜色、颗数、散布、速度都按名取自数值真源。本方法<b>一次中和只调一次</b>：
	 * 「别每 tick 刷」由触发点保证——中和本身是单次事件，冷却闸门管的是
	 * 「同一个实体别在窗口内再中和」，不靠这里节流。颗数随爆炸等级增长
	 * （{@link ChargeConfigs#neutralizeParticleCount}），免得 5 级的大立方体比 1 级还稀。</p>
	 */
	private static void sendBurst(Level world, Center center, int level) {
		if (!(world instanceof ServerLevel server)) {
			return; // 粒子是服务端权威（与本模组其它表现层同一口径）
		}
		int count = ChargeConfigs.neutralizeParticleCount(level);
		double spread = ChargeConfigs.NEUTRALIZE_PARTICLE_SPREAD;
		double speed = ChargeConfigs.NEUTRALIZE_PARTICLE_SPEED;
		server.sendParticles(ChargeConfigs.PARTICLE_NEUTRALIZE_POSITIVE,
			center.x(), center.y(), center.z(), count, spread, spread, spread, speed);
		server.sendParticles(ChargeConfigs.PARTICLE_NEUTRALIZE_NEGATIVE,
			center.x(), center.y(), center.z(), count, spread, spread, spread, speed);
		server.sendParticles(ChargeConfigs.PARTICLE_FLASH,
			center.x(), center.y(), center.z(), ChargeConfigs.NEUTRALIZE_FLASH_COUNT, spread, spread, spread, speed);
	}

	/**
	 * ★ <b>中和点留电荷残留 —— 批 7 的唯一落地钩子，本批刻意留空</b>（需求 §3.5 #6~#9）。
	 *
	 * <p><b>本批它做什么</b>：记一行日志，<b>不生成任何残留</b>。也就是说：今天中和之后
	 * 那块地方是干净的——没有残留载体、没有残留粒子、没有「接触残留随机染电」。</p>
	 *
	 * <p><b>为什么留一行日志而不是干脆空着</b>：空分支 = 无从判断它有没有被走到
	 * （批 3 的同一课：静默分支与「没被走到」从外部完全不可区分）。中和点正是批 7 的落点，
	 * 实机验收时要能一眼看出「这条钩子走到了、只是还没实现」。</p>
	 *
	 * <p><b>批 7 要在这里补什么</b>（参数摆全就是为了那时候不用改调用点）：在 {@code center}
	 * 生成残留载体、存活 {@link ChargeConfigs#residueLifetimeTicks(int)} tick、稀疏粒子、
	 * 生物接触残留 ⇒ 随机染电且等级 = {@link ChargeConfigs#residueInflictedLevel(int)}，
	 * 并处理「同一实体对同一块残留只染一次 + 残留间爆炸最小间隔」的防连锁规则
	 * （需求 §3.5 的「必须处理的一个设计洞」）。</p>
	 *
	 * @param center 中和点（{@link #detonate} 算出的爆炸中心）
	 * @param level  爆炸等级 {@code L}（残留的寿命与给的等级都从它派生）
	 */
	private static void leaveResidue(Center center, int level) {
		// TODO 批 7（电荷残留，需求 §3.5 #6~#9）：清单见方法 javadoc；本批只留这个具名钩子，
		//   并在这里记一行日志（刻意不静默）。
		//   ⚠ 落地时同时改掉关卡里「残留仍是钩子」那条断言（check-armor-sets.ps1 中和爆炸节）。
		CoeCore.LOGGER.info("[电荷中和] 中和点 {}（爆炸等级 Lv{}）已记账：电荷残留属于后续批次，本批不生成残留、不放残留粒子",
			center, level);
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
