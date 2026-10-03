package com.hjmmd_8.createoreexpansion.content.charger.wave;

import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationEffects;
import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

/**
 * <b>魔素打中玩家时施加的效果</b>（2026-10-03 需求 coe-ess 批 6；作者第 9 条逐条要求）。
 *
 * <p><b>作者原话与本类的逐条对应</b>（"不同的魔素打中玩家最好要造成相应的 buff 效果"）：</p>
 * <ul>
 *   <li>(a) <b>雷</b>：<b>刻意留空</b>——作者明说"先不要进行任何效果设置，因为之后我要加一个
 *       带正电和带负电的 buff"，故 {@code case LIGHTNING} 里只有一行 {@code TODO} 注释 + 直接返回，
 *       <b>连日志都不打</b>（它没有任何效果可报）；</li>
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
 * <p><b>只对玩家生效</b>（作者原话"打中<b>玩家</b>"）：调用点在
 * {@link AbstractChargerWaveEntity} 命中处理的 {@code if (target instanceof Player wearer)} 分支里，
 * 本类的方法签名也只收 {@link Player} ——<b>生物不吃这些效果</b>（生物照旧只吃波级伤害）。</p>
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
				// TODO 正电/负电 buff（作者后续加）：本批**刻意什么都不做** —— 不留空方法、
				// 不发粒子/音效/效果、也不打日志（没有发生的事不该出现在事件流里）。
				return;
			}
			default -> {
				// NORMAL / MECHANICAL / DAMAGE 是**波型风格**、不是魔素：被当魔素显式传进来时
				// 按"没有魔素效果"处理（不崩、不报错、什么都不做）。
				return;
			}
		}
		WaveDiag.trace(
			"魔素命中玩家：{} 级波携带 {} 魔素打中 {}，已施加该魔素的玩家效果（只对玩家生效，生物不吃这些效果）",
			wave.getWaveLevel(), essence.name(), player.getName()
				.getString());
	}

	/**
	 * <b>额外伤害</b>（水 2 点 / 地 1 点）——<b>必须先清掉本次波的伤害刚开启的无敌帧</b>。
	 *
	 * <p>为什么非清不可（不清就是静默无效）：原版 {@code LivingEntity#hurt} 在
	 * {@code invulnerableTime > 10} 时走"只结算比上一击更大的部分"那条路 ——
	 * {@code amount <= lastHurt} 直接 {@code return false}。而波自己的伤害就在<b>本 tick 刚刚</b>
	 * 结算完（{@code getDamage()} = 4/6/8/10/12，且把 {@code invulnerableTime} 置为 20），
	 * 于是"额外 2 点"会整段被吃掉、玩家一点都不多掉血，而且没有任何报错。
	 * 清帧后立刻 {@code hurt} ⇒ 这一 tick 的无敌帧由<b>本次额外伤害</b>重新开启（20 tick 的保护窗口
	 * 长度不变），总伤害 = 波级伤害 + 额外伤害。</p>
	 *
	 * <p>伤害走 {@code indirectMagic}（与波自身那一击同一个伤害类型口径）⇒ 照常过护甲 / 附魔 /
	 * 吸收 / 创造模式的 {@code abilities.invulnerable}（创造玩家照样不掉血）。</p>
	 */
	private static void hurtThroughIFrames(AbstractChargerWaveEntity wave, Player player, float extraDamage) {
		player.invulnerableTime = 0;
		player.hurt(player.damageSources()
			.indirectMagic(wave, null), extraDamage);
	}

	/** 命中处的一声原版音效（音量/音高/音源分类都是具名常量）。 */
	private static void playAtHit(ServerLevel server, Player player, SoundEvent sound) {
		server.playSound(null, player.getX(), player.getY(), player.getZ(), sound, HIT_SOUND_SOURCE,
			HIT_SOUND_VOLUME, HIT_SOUND_PITCH);
	}

	/** 命中处的一簇原版粒子（位置取玩家胸口高度；颗数/散布/速度都是具名常量）。 */
	private static void puffAtHit(ServerLevel server, Player player, ParticleOptions particle) {
		server.sendParticles(particle, player.getX(), player.getY() + HIT_PARTICLE_HEIGHT, player.getZ(),
			HIT_PARTICLE_COUNT, HIT_PARTICLE_SPREAD, HIT_PARTICLE_SPREAD, HIT_PARTICLE_SPREAD,
			HIT_PARTICLE_SPEED);
	}
}
