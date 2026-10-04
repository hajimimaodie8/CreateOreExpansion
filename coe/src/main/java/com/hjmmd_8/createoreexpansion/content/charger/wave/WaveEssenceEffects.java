package com.hjmmd_8.createoreexpansion.content.charger.wave;

import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationEffects;
import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.energyfield.charge.LightningEssenceHitCharge;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTrailStyle;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

/**
 * <b>魔素打中玩家时施加的效果</b>（2026-10-03 需求 coe-ess 批 6；作者第 9 条逐条要求）。
 *
 * <p><b>作者原话与本类的逐条对应</b>（"不同的魔素打中玩家最好要造成相应的 buff 效果"）：</p>
 * <ul>
 *   <li>(a) <b>雷</b>：<b>玩家那一支刻意留空</b>——作者明说"先不要进行任何效果设置，因为之后我要加一个
 *       带正电和带负电的 buff"，故 {@code case LIGHTNING} 里只有一行 {@code TODO} 注释 + 直接返回，
 *       <b>连日志都不打</b>（它没有任何效果可报）。⚠ <b>生物那一支现在有内容了</b>（作者 2026-10-04
 *       统一标准：「雷魔素攻击生物之后，会随机给生物附着一种电荷，附着时间为 5 秒，附着的那个 buff
 *       等级从 1~3 随机」）⇒ 生物那一支的 {@code case LIGHTNING} 施放随机电荷，实现住在
 *       {@code content.energyfield.charge.LightningEssenceHitCharge}（"怎么施加"归电荷层，
 *       "哪一支魔素做这件事"留在本层）。这条<b>刻意的双口径</b>是作者认可的中转状态：
 *       <b>玩家</b>被雷魔素波命中仍走旧路径（本 case 空白 ⇒ 不上电荷），<b>生物</b>按本标准上电荷；</li>
 *   <li>(b) <b>水</b>：窒息音效（原版溺水受伤声） + 气泡/水花粒子 + <b>额外伤害</b>；</li>
 *   <li>(c) <b>火</b>：灼烧（原版点燃）；</li>
 *   <li>(d) <b>冰</b>："细雪冰冻"——原版 {@code setTicksFrozen}（冰凉覆盖层 + 细雪的减速）
 *       + 缓慢；<b>约 3 秒才缓过来</b>（算式见 {@link #ICE_FROZEN_TICKS}）；</li>
 *   <li>(e) <b>风</b>：一次旋风——向上推力（把玩家打飞上去） + 阵风粒子 + 旋风音效；</li>
 *   <li>(f) <b>地</b>：类似仙人掌的"刺扎"——额外伤害 + 刺扎音效 + 仙人掌碎屑粒子；</li>
 *   <li>(g) <b>异</b>：嬗乱——<b>复用本模组既有的</b> {@code createoreexpansion:transmutation_disorder}
 *       （{@link TransmutationEffects#TRANSMUTATION_DISORDER}），<b>不另造第二个嬗乱</b>；</li>
 *   <li>(h) <b>毒</b>：原版中毒。</li>
 * </ul>
 *
 * <p><b>两支入口，互不重叠</b>（2026-10-04 弓技能批 3 起）：</p>
 * <ul>
 *   <li>{@link #applyOnPlayerHit(AbstractChargerWaveEntity, Player)} —— <b>波打中玩家</b>
 *       （2026-10-03 需求 coe-ess 批 6 的那一支，调用点在波实体
 *       {@code if (target instanceof Player wearer)} 分支里；签名与八支正文一个字未动）；</li>
 *   <li>{@link #applyEssenceOnCreatureHit(Entity, LivingEntity, WaveTrailStyle)} —— <b>魔素打中生物</b>
 *       （本批新增，与玩家那一支一一对应的八支；两个调用者：① "元矢自生"打出的魔法箭命中生物，
 *       ② 波实体命中生物那一处的 {@link #applyOnWaveCreatureHit}）。</li>
 * </ul>
 *
 * <p><b>生物那一支为什么八支齐备、而波那一处只走雷</b>：作者 2026-10-04 的统一标准只写了雷
 * （「雷魔素攻击生物之后…」），另外七支对生物<b>没有</b>裁定 ⇒
 * {@link #applyOnWaveCreatureHit} 只放 {@code LIGHTNING} 过闸，波打中生物时的水/火/冰/风/地/毒/异
 * 仍与改造前<b>逐字相同</b>（什么都不做）；而"元矢自生"的作者裁定是
 * 「等效于一枚带魔素的攻击波打中该生物」+「魔素随机取 8 种之一」⇒ 它走完整的八支。</p>
 *
 * <p><b>只对玩家生效</b>（作者原话"打中<b>玩家</b>"）：原玩家那一条调用点在
 * {@link AbstractChargerWaveEntity} 命中处理的 {@code if (target instanceof Player wearer)} 分支里，
 * 该方法的签名也只收 {@link Player} ——<b>它自己仍然只服务玩家</b>。</p>
 *
 * <p><b>免疫走既有通道、不另写一套</b>：三种 MobEffect（缓慢 / 中毒 / 嬗乱）一律经原版
 * {@link net.minecraft.world.entity.LivingEntity#addEffect(MobEffectInstance)} 施加 ⇒ NeoForge 的
 * {@code MobEffectEvent.Applicable} 照常触发 ⇒ 本模组既有的效果免疫判据自动生效
 * （例如星辉石凝能佩免疫嬗乱：{@code MedallionEffectHandler#onEffectApplicable}），
 * 本类<b>不写任何</b> "谁免疫什么" 的判据。⚠ 点燃 / 细雪冻结 / 上抛 / 额外伤害<b>不是 MobEffect</b>
 * （原版没有对应的 Applicable 事件），故它们不受该通道管——这是原版的边界，不是本类绕过了它。</p>
 *
 * <p><b>与"默认魔素 = 火"的关系</b>：只有<b>真的被显式赋了魔素</b>的波才施加效果
 * （{@code getEssence() == null} ⇒ 第一句返回）。"默认火"是<b>观感</b>上的继承
 * （见 {@link AbstractChargerWaveEntity#trailStyle()}：没设魔素就继承波型风格，而攻击波的波型风格
 * 与火魔素是同一份档案）；本条需求写的是"不同的<b>魔素</b>打中玩家"，所以机器波 / 变器攻击波 /
 * 一切没赋魔素的波<b>逐字不变</b>（不会突然开始点燃玩家）。</p>
 *
 * @since 1.0.0
 */
public final class WaveEssenceEffects {

	// ==================================================================================
	// 数值真源（全仓唯一一处）：【作者定】= 作者第 9 条写死的；【我定】= 作者没写死、由执行会话
	// 定的默认值 —— 两类都写成具名常量，作者改数值只动这一块，别处不许抄数字
	// （关卡 wave-essence-hit-values 既钉这些声明式，也钉"调用点必须传常量名、不许传字面量"）。
	// ==================================================================================

	/** 水：额外伤害（点）。【我定 2 点：作者原文只写"并给玩家造成额外伤害"】 */
	public static final float WATER_EXTRA_DAMAGE = 2.0F;

	/**
	 * 火：点燃秒数。【我定 5 秒 = {@code floor(5.0F * 20) = 100} tick：作者只写"造成灼烧效果"】
	 *
	 * <p>走原版 {@link net.minecraft.world.entity.Entity#igniteForSeconds(float)} ⇒ 点燃状态、
	 * 火焰粒子、每 20 tick 1 点火焰伤害全部是原版既有行为，本类不自己算火伤。</p>
	 */
	public static final float FIRE_IGNITE_SECONDS = 5.0F;

	/**
	 * 冰：细雪冻结的 {@code ticksFrozen} 值。【我定】<b>这就是"约 3 秒才缓过来"的全部算式</b>：
	 *
	 * <pre>
	 *   原版解冻速度（{@code LivingEntity#aiStep}）：不在细雪里时每 tick <b>−2</b>
	 *   ⇒ 缓过来所需 tick = ICE_FROZEN_TICKS ÷ 2 = 120 ÷ 2 = <b>60 tick = 3.00 秒</b>
	 *   原版"完全冻住"阈值（{@code Entity#getTicksRequiredToFreeze()}）= <b>140</b>
	 *   ⇒ 120 &lt; 140：冰霜覆盖层按 120/140 ≈ <b>86%</b> 渲染（一眼看得出被冻住），
	 *     但 {@code isFullyFrozen()} 恒为 false ⇒ <b>不会</b>每 40 tick 掉 1 点冻伤
	 *     —— 作者要的是"细雪冰冻的效果 + 3 秒缓过来"，不是白送一份冻伤。
	 *   改这一个数就同时改"冻得多深"与"多久缓过来"（÷2 的关系是原版写死的）。
	 * </pre>
	 */
	public static final int ICE_FROZEN_TICKS = 120;

	/** 冰：缓慢持续 tick。【我定 60 = 3.00 秒：与上面"缓过来"的窗口<b>同一个 3 秒</b>（同时结束）】 */
	public static final int ICE_SLOW_TICKS = 60;

	/** 冰：缓慢等级（amplifier，0 = 缓慢 I）。【我定】 */
	public static final int ICE_SLOW_AMPLIFIER = 0;

	/**
	 * 风：向上推力（格/tick 的<b>速度增量</b>）。【我定】
	 *
	 * <p>走原版 {@link net.minecraft.world.entity.Entity#push(double, double, double)}：它是
	 * {@code deltaMovement += (x,y,z)} 且把 {@code hasImpulse} 置位 ⇒ 服务端会把新速度同步给客户端
	 * （{@code ServerEntity#sendChanges} 的 {@code hasImpulse} 那条），所以玩家真的会被打飞上去，
	 * 而不是只有服务端自己动了。</p>
	 */
	public static final double WIND_LAUNCH_UP = 0.75D;

	/**
	 * 风：推力<b>水平分量</b>（格/tick）。【我定 0 = 只往上打飞（作者口径"可以把玩家打飞上去"）】
	 *
	 * <p>留着这个具名 0 是为了"一行改成带横向分量的旋风"：改成 0.3 就是斜着甩出去。
	 * 水平惯性（玩家当时跑动的速度）不受影响——{@code push} 是<b>叠加</b>而不是覆盖。</p>
	 */
	public static final double WIND_LAUNCH_SIDE = 0.0D;

	/** 地：额外伤害（点）。【我定 1 点：作者原文只写"类似于仙人掌的刺扎效果"】 */
	public static final float EARTH_EXTRA_DAMAGE = 1.0F;

	/**
	 * 毒：中毒持续 tick。【我定 8.0 秒（160 tick）】
	 *
	 * <p>原版中毒（amplifier 0）每 {@code 25 >> amplifier = 25} tick 结算 1 点魔法伤害，
	 * 且<b>血 ≤ 1 点时不再结算</b>（毒不致死）⇒ 160 tick 最多 7 点、不会杀人。
	 * 与火（5 秒 5 点）同一量级。</p>
	 */
	public static final int POISON_DURATION_TICKS = 160;

	/** 毒：中毒等级（amplifier，0 = 中毒 I）。【我定】 */
	public static final int POISON_AMPLIFIER = 0;

	/**
	 * 异：嬗乱持续 tick。【我定 60 = 3.00 秒：与星芒嬗震技能侧
	 * {@code StarShockRuntime#HIT_DISORDER_TICKS} 的既有时长口径一致（同一个效果、同一个 3 秒）】
	 */
	public static final int ARCANE_DISORDER_TICKS = 60;

	/** 异：嬗乱等级（amplifier，0 = I 级）。【我定：与技能侧 {@code HIT_DISORDER_AMPLIFIER} 一致】 */
	public static final int ARCANE_DISORDER_AMPLIFIER = 0;

	/** 命中粒子：颗数（八种魔素共用一档）。【我定】 */
	public static final int HIT_PARTICLE_COUNT = 12;

	/** 命中粒子：散布半径（各轴，格）。【我定】 */
	public static final double HIT_PARTICLE_SPREAD = 0.4D;

	/** 命中粒子：初速系数。【我定】 */
	public static final double HIT_PARTICLE_SPEED = 0.05D;

	/** 命中粒子：发射高度偏移（格）——取玩家胸口附近（玩家高 1.8）。【我定】 */
	public static final double HIT_PARTICLE_HEIGHT = 1.0D;

	/** 命中音效：音量。【我定】 */
	public static final float HIT_SOUND_VOLUME = 1.0F;

	/** 命中音效：音高。【我定】 */
	public static final float HIT_SOUND_PITCH = 1.0F;

	/**
	 * 命中音效：音源分类。【我定】四种音效都是"玩家自己身上挨了一下"的反馈
	 * （溺水声 / 冻伤声 / 刺扎声 / 旋风），故统一走 {@link SoundSource#PLAYERS} 这一档音量滑块。
	 */
	public static final SoundSource HIT_SOUND_SOURCE = SoundSource.PLAYERS;

	/**
	 * 地：刺扎粒子。【我定】取<b>仙人掌方块碎屑</b>——原版没有"仙人掌刺"粒子，而"仙人掌扎人"
	 * 的表现就是绿屑 + 一声刺扎音（{@link SoundEvents#PLAYER_HURT_SWEET_BERRY_BUSH}，
	 * 原版唯一一条"被带刺方块扎到"的玩家受伤声）。
	 */
	private static final ParticleOptions CACTUS_PRICK_PARTICLE =
		new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CACTUS.defaultBlockState());

	private WaveEssenceEffects() {
	}

	/**
	 * <b>魔素命中玩家的唯一施加点</b>：由 {@link AbstractChargerWaveEntity} 的命中处理在
	 * "目标就是玩家"那条分支里调用（生物那条路根本不调本方法）。
	 *
	 * <p>三道闸，顺序即优先级：</p>
	 * <ol>
	 *   <li><b>没有魔素 ⇒ 什么都不做</b>（{@code getEssence() == null}）：机器波 / 变器攻击波 /
	 *       一切未赋魔素的波与改造前逐字相同；</li>
	 *   <li><b>不是服务端 ⇒ 什么都不做</b>（粒子/音效/效果全是服务端权威；波实体的客户端 tick
	 *       本来就在命中判定之前 return，这里是第二道闸，也是 Ponder 场景的兜底）；</li>
	 *   <li><b>按魔素分派</b>（{@code switch}）：八种魔素各一条 {@code case}，
	 *       {@code LIGHTNING} 留空、{@code default}（前三种波型风格被当魔素传进来时）同样留空。</li>
	 * </ol>
	 *
	 * <p>最后一行是事件流日志（走 {@link WaveDiag}：波相关日志的唯一出口）——它落在 {@code switch}
	 * <b>之后</b>，而"什么都没做"的两条臂都提前 {@code return}，于是日志恰好等价于
	 * "这次真的施加了某一种魔素效果"（作者只能靠日志与实机验收）。</p>
	 *
	 * @param wave   打出这一击的波（取魔素、当伤害来源、取波级写日志）
	 * @param player 被击中的玩家（<b>只有玩家</b>会走到这里）
	 */
	public static void applyOnPlayerHit(AbstractChargerWaveEntity wave, Player player) {
		if (wave == null || player == null) {
			return;
		}
		WaveTrailStyle essence = wave.getEssence();
		if (essence == null) {
			return;
		}
		if (!(player.level() instanceof ServerLevel server)) {
			return;
		}
		switch (essence) {
			case WATER -> {
				// 额外伤害 + 窒息的声音 + 气泡/水花（作者第 9 条 (b)）
				hurtThroughIFrames(wave, player, WATER_EXTRA_DAMAGE);
				playAtHit(server, player, SoundEvents.PLAYER_HURT_DROWN);
				puffAtHit(server, player, ParticleTypes.SPLASH);
				puffAtHit(server, player, ParticleTypes.BUBBLE);
			}
			case FIRE -> {
				// 灼烧：原版点燃 + 火焰粒子（作者第 9 条 (c)）
				player.igniteForSeconds(FIRE_IGNITE_SECONDS);
				puffAtHit(server, player, ParticleTypes.FLAME);
			}
			case ICE -> {
				// 细雪冰冻 + 缓慢：两者都在第 3 秒同时结束（作者第 9 条 (d)，算式见 ICE_FROZEN_TICKS）
				player.setTicksFrozen(ICE_FROZEN_TICKS);
				player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
					ICE_SLOW_TICKS, ICE_SLOW_AMPLIFIER));
				playAtHit(server, player, SoundEvents.PLAYER_HURT_FREEZE);
				puffAtHit(server, player, ParticleTypes.SNOWFLAKE);
			}
			case WIND -> {
				// 旋风：向上打飞 + 阵风粒子 + 旋风音（作者第 9 条 (e)）
				player.push(WIND_LAUNCH_SIDE, WIND_LAUNCH_UP, WIND_LAUNCH_SIDE);
				playAtHit(server, player, SoundEvents.BREEZE_WHIRL);
				puffAtHit(server, player, ParticleTypes.SMALL_GUST);
			}
			case EARTH -> {
				// 刺扎：额外伤害 + 刺扎音 + 仙人掌碎屑（作者第 9 条 (f)）
				hurtThroughIFrames(wave, player, EARTH_EXTRA_DAMAGE);
				playAtHit(server, player, SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH);
				puffAtHit(server, player, CACTUS_PRICK_PARTICLE);
			}
			case POISON -> {
				// 原版中毒（作者第 9 条 (h)）：走 addEffect ⇒ MobEffectEvent.Applicable 照常触发
				player.addEffect(new MobEffectInstance(MobEffects.POISON,
					POISON_DURATION_TICKS, POISON_AMPLIFIER));
				puffAtHit(server, player, ParticleTypes.ITEM_SLIME);
			}
			case ARCANE -> {
				// 嬗乱（作者第 9 条 (g)）：<b>复用</b>既有注册 id 的同一个 DeferredHolder
				// （createoreexpansion:transmutation_disorder），不另造第二个嬗乱效果。
				// 星辉石凝能佩的免疫判据比较的正是这一个 Holder（MedallionEffectHandler
				// #onEffectApplicable），所以它在这里自动生效 —— 本类不写免疫判据。
				player.addEffect(new MobEffectInstance(TransmutationEffects.TRANSMUTATION_DISORDER,
					ARCANE_DISORDER_TICKS, ARCANE_DISORDER_AMPLIFIER));
				puffAtHit(server, player, ParticleTypes.END_ROD);
			}
			case LIGHTNING -> {
				// TODO 正电/负电 buff（作者后续加）：**雷这一支刻意什么都不做** —— 电荷的获得与查询
				// 已收口在 content.energyfield.ChargeApi（coe-charge 批 5 起「被雷击 / 雷鸣合金技能命中 /
				// 靠近通电线圈 / 被带电波击中」四条途径都调它）；这里不再自己施加，因为本臂只收 Player，
				// 而电荷要玩家与生物都能获得。本批**不改代码**：不留空方法、不发粒子/音效/效果、
				// 也不打日志（没有发生的事不该出现在事件流里）。
				return;
			}
			default -> {
				// NORMAL / MECHANICAL / DAMAGE 是**波型风格**、不是魔素：被当魔素显式传进来时
				// 按"没有魔素效果"处理（不崩、不报错、什么都不做）。
				return;
			}
		}
		WaveDiag.trace(
			"魔素命中玩家：{} 级波携带 {} 魔素打中 {}，已施加该魔素的玩家效果（玩家这一支只服务玩家；生物那一支见 applyEssenceOnCreatureHit）",
			wave.getWaveLevel(), essence.name(), player.getName()
				.getString());
	}

	/**
	 * 按魔素分派（<b>生物那一支</b>，2026-10-04 弓技能批 3 新增）——与玩家那一支一一对应的八支。
	 *
	 * <p><b>谁走这里</b>：① {@code JadeTopazBowEventHandler}（"元矢自生"打出的魔法箭带着魔素命中
	 * 生物）；② {@link #applyOnWaveCreatureHit}（带雷魔素的波命中生物，作者 2026-10-04 统一标准）。</p>
	 *
	 * <p>三道闸，与玩家那一支同形：目标不是空 ⇒ 目标<b>不是玩家</b>（玩家那一支有它自己的入口，
	 * 两支互不重叠）⇒ 服务端。魔素为 {@code null} 直接返回（机器波 / 未赋魔素的波逐字不变）。</p>
	 *
	 * <p>⚠ 八支的正文与玩家那一支<b>逐条对应</b>（同一批数值常量、同一批音效/粒子），差别只在两点：
	 * 一是作用于 {@link LivingEntity}（玩家那一支的形参是 {@link Player}），二是
	 * {@code case LIGHTNING} <b>有内容</b>（随机电荷，见类注释 (a)）。数值仍然只住本文件顶部那一块，
	 * 本方法一个新数字都不写。</p>
	 *
	 * @param source 这一击的来源实体（波打中生物时是波、箭打中生物时是箭）——水/地的额外伤害
	 *               按它的 {@code indirectMagic(source, null)} 结算，与玩家那一支同一种伤害类型
	 * @param target 被魔素打中的生物（<b>非玩家</b>；玩家请走 {@link #applyOnPlayerHit}）
	 * @param essence 这一击携带的魔素（{@code null} ⇒ 什么都不做）
	 */
	public static void applyEssenceOnCreatureHit(Entity source, LivingEntity target, WaveTrailStyle essence) {
		if (source == null || target == null || essence == null) {
			return;
		}
		if (target instanceof Player) {
			// 玩家那一支的唯一入口是 applyOnPlayerHit（形参就是 Player）：两支不许互相顶替，
			// 否则"玩家吃哪八支、生物吃哪八支"就不再是两处可分别断言的事实。
			return;
		}
		if (!(target.level() instanceof ServerLevel server)) {
			return;
		}
		switch (essence) {
			case WATER -> {
				hurtThroughIFrames(source, target, WATER_EXTRA_DAMAGE);
				playAtHit(server, target, SoundEvents.PLAYER_HURT_DROWN);
				puffAtHit(server, target, ParticleTypes.SPLASH);
				puffAtHit(server, target, ParticleTypes.BUBBLE);
			}
			case FIRE -> {
				target.igniteForSeconds(FIRE_IGNITE_SECONDS);
				puffAtHit(server, target, ParticleTypes.FLAME);
			}
			case ICE -> {
				target.setTicksFrozen(ICE_FROZEN_TICKS);
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
					ICE_SLOW_TICKS, ICE_SLOW_AMPLIFIER));
				playAtHit(server, target, SoundEvents.PLAYER_HURT_FREEZE);
				puffAtHit(server, target, ParticleTypes.SNOWFLAKE);
			}
			case WIND -> {
				target.push(WIND_LAUNCH_SIDE, WIND_LAUNCH_UP, WIND_LAUNCH_SIDE);
				playAtHit(server, target, SoundEvents.BREEZE_WHIRL);
				puffAtHit(server, target, ParticleTypes.SMALL_GUST);
			}
			case EARTH -> {
				hurtThroughIFrames(source, target, EARTH_EXTRA_DAMAGE);
				playAtHit(server, target, SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH);
				puffAtHit(server, target, CACTUS_PRICK_PARTICLE);
			}
			case POISON -> {
				target.addEffect(new MobEffectInstance(MobEffects.POISON,
					POISON_DURATION_TICKS, POISON_AMPLIFIER));
				puffAtHit(server, target, ParticleTypes.ITEM_SLIME);
			}
			case ARCANE -> {
				target.addEffect(new MobEffectInstance(TransmutationEffects.TRANSMUTATION_DISORDER,
					ARCANE_DISORDER_TICKS, ARCANE_DISORDER_AMPLIFIER));
				puffAtHit(server, target, ParticleTypes.END_ROD);
			}
			case LIGHTNING -> {
				// 作者 2026-10-04 统一标准：随机极性 / 1~3 随机等级 / 固定 5 秒。
				// "怎么施加"归电荷层（ChargeApi 的那一个类里），本层只回答"雷这一支做什么"。
				LightningEssenceHitCharge.applyOnHit(target);
			}
			default -> {
				// NORMAL / MECHANICAL / DAMAGE 是波型风格、不是魔素：与玩家那一支同样什么都不做。
				return;
			}
		}
		WaveDiag.trace(
			"魔素命中生物：{} 魔素打中 {}（来源 {}），已施加该魔素的生物效果（雷那一支按作者统一标准给随机电荷）",
			essence.name(), target.getName()
				.getString(),
			source.getName()
				.getString());
	}

	/**
	 * <b>带雷魔素的波命中生物</b>——作者 2026-10-04 统一标准的落地口
	 * （原话「雷魔素攻击生物之后，会随机给生物附着一种电荷…」；唯一调用点是波实体命中处理的
	 * "命中生物"那一处，与玩家那一支的调用点<b>并列而不重叠</b>）。
	 *
	 * <p>它只放 {@code LIGHTNING} 过闸：作者这条统一标准写的是<b>雷</b>，另外七种魔素对生物尚无裁定
	 * ⇒ 未带雷魔素的波打中生物时，本方法第一句就返回，波的行为与改造前<b>逐字相同</b>
	 * （这条闸门就是"不越权改玩法"的那道边界）。真正的八支分派委托给
	 * {@link #applyEssenceOnCreatureHit}（唯一实现，箭那一支走同一个方法 ⇒ 两条来源同一份效果）。</p>
	 *
	 * @param wave   打出这一击的波（读它的魔素）
	 * @param target 被击中的生物
	 */
	public static void applyOnWaveCreatureHit(AbstractChargerWaveEntity wave, LivingEntity target) {
		if (wave == null || target == null) {
			return;
		}
		if (wave.getEssence() != WaveTrailStyle.LIGHTNING) {
			return;
		}
		applyEssenceOnCreatureHit(wave, target, WaveTrailStyle.LIGHTNING);
	}

	/**
	 * <b>额外伤害</b>（水 2 点 / 地 1 点）——<b>必须先清掉本次命中刚开启的无敌帧</b>。
	 *
	 * <p>为什么非清不可（不清就是静默无效）：原版 {@code LivingEntity#hurt} 在
	 * {@code invulnerableTime > 10} 时走"只结算比上一击更大的部分"那条路 ——
	 * {@code amount <= lastHurt} 直接 {@code return false}。而这一击自己的伤害就在<b>本 tick 刚刚</b>
	 * 结算完（波级波是 {@code getDamage()} = 4/6/8/10/12、"元矢自生"的箭是箭自身的伤害，
	 * 两者都把 {@code invulnerableTime} 置为 20），于是"额外 2 点"会整段被吃掉、目标一点都不多掉血，
	 * 而且没有任何报错。清帧后立刻 {@code hurt} ⇒ 这一 tick 的无敌帧由<b>本次额外伤害</b>重新开启
	 * （20 tick 的保护窗口长度不变），总伤害 = 原来那一击 + 额外伤害。</p>
	 *
	 * <p>伤害走 {@code indirectMagic}（与波自身那一击同一个伤害类型口径）⇒ 照常过护甲 / 附魔 /
	 * 吸收 / 创造模式的 {@code abilities.invulnerable}（创造玩家照样不掉血）。
	 * 攻击者刻意与波那一支<b>逐字同形</b>（第三参传 {@code null}）：两支的额外伤害在这一点上
	 * 没有第二种口径。</p>
	 */
	private static void hurtThroughIFrames(Entity source, LivingEntity target, float extraDamage) {
		target.invulnerableTime = 0;
		target.hurt(target.damageSources()
			.indirectMagic(source, null), extraDamage);
	}

	/** 命中处的一声原版音效（音量/音高/音源分类都是具名常量）。 */
	private static void playAtHit(ServerLevel server, LivingEntity target, SoundEvent sound) {
		server.playSound(null, target.getX(), target.getY(), target.getZ(), sound, HIT_SOUND_SOURCE,
			HIT_SOUND_VOLUME, HIT_SOUND_PITCH);
	}

	/** 命中处的一簇原版粒子（位置取目标胸口高度；颗数/散布/速度都是具名常量）。 */
	private static void puffAtHit(ServerLevel server, LivingEntity target, ParticleOptions particle) {
		server.sendParticles(particle, target.getX(), target.getY() + HIT_PARTICLE_HEIGHT, target.getZ(),
			HIT_PARTICLE_COUNT, HIT_PARTICLE_SPREAD, HIT_PARTICLE_SPREAD, HIT_PARTICLE_SPREAD,
			HIT_PARTICLE_SPEED);
	}
}
