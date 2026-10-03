package com.hjmmd_8.createoreexpansion.content.charger.entity;


import com.hjmmd_8.createoreexpansion.common.CoeCore;
import java.util.List;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveDiag;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveEssenceEffects;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveHitResolver;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveMachineHandler;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveMachineHandlers;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveContraptionCollisions;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveSubLevelCollisions;
import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipe;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorEnergy;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveType;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTypes;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTrailStyle;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveMachineIntegrationPoints;

import net.createmod.catnip.levelWrappers.SchematicLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.Nullable;

/**
 * 应力充能器能量波实体抽象基类：不渲染模型（视觉靠粒子）。
 *
 * <p><b>职责划分</b>（同包 4 个类协同，按碰撞环境分文件）：</p>
 * <ul>
 *   <li>本类 = 实体骨架：字段、tick 主循环（飞行/寿命/粒子/生物/掉落物/波波碰撞）、
 *       主世界方块遍历分发、NBT、访问器；</li>
 *   <li>机器方块（调级器 / 波速调节器 / 三种差波器 / 星辉波变器）的命中判定 = <b>第二层</b>
 *       的处理器，经 {@link WaveMachineHandler} 契约与 {@link WaveMachineHandlers} 登记表回调
 *       （W6-a：原先住第一层的 {@code WaveMachineActions} 已并入那些处理器）；</li>
 *   <li>{@link WaveSubLevelCollisions} = Sable 物理结构（sub-level）碰撞协调；</li>
 *   <li>{@link WaveContraptionCollisions} = Create contraption 碰撞协调。</li>
 * </ul>
 *
 * <p>通用行为（模板方法）：服务端飞行、命中判定（生物伤害/掉落物加工/置物台加工/撞墙消散）、
 * 粒子拖尾与命中绽放、加工完成音效；客户端在消散时补充球面均匀扩散绽放。</p>
 *
 * <p>能量波颜色统一按等级（1~5：α=黄、β=绿、γ=蓝、ε=紫粉、ω=玫红+金拖尾），
 * 与机型无关——翡翠/蓝宝石等所有应力充能器发射的都是同一种能量波（{@link ChargerWaveEntity}），
 * 粒子统一用原版染色粒子（DustParticleOptions），无需注册任何自定义粒子类型。</p>
 *
 * <p>等级 1~5 见 {@link WaveLevels}：速度 2/4/6/7/8 格/秒、伤害 4/6/8/10/12。</p>
 *
 * <p><b>2026-10-02 新增：两个「通用、可选、默认关闭」的波要素</b>（作者裁定：「能量波是由好几个
 * 要素定义的」⇒ 给既有波<b>加要素</b>符合口径，另造实体才违反）：<b>环绕波要素</b>
 * （anchorWaveUuid / orbitRadius / orbitAngularSpeed / orbitPhase，见 {@link #applyOrbitElement()}）
 * 与<b>命中附加效果要素</b>（hitEffect / hitEffectDuration / hitEffectAmplifier，见
 * {@link #applyHitEffect(LivingEntity)}）。两者<b>不设时</b>字段就是 {@code null} / {@code 0}、
 * 分支第一句直接返回 ⇒ 机器波 / 变器波 / 一切既有波的行为与改造前逐字相同；
 * 并且<b>不新增实体类型、贴图、模型、渲染器</b>（关卡 {@code no-invented-wave-entity} 与
 * {@code wave-renderer-coverage} 守着这一条）。</p>
 */
public abstract class AbstractChargerWaveEntity extends Entity
	implements com.hjmmd_8.createoreexpansion.content.energyfield.FieldedEntity, OrbitAnchor {

	/**
	 * 最大存活 tick（200 tick = 10 秒）：能量波飞出充能器后即使不撞墙也会自动消散，
	 * 防止充能器持续发射导致波实体无限累积（日志曾见波 tick=683 仍在飞行），
	 * 这是"加机器后卡顿"的主要来源之一。
	 *
	 * <p>10 秒足够覆盖绝大多数用法：α 波飞 20 格、β 波 40 格、γ 波 60 格；
	 * 也给波波碰撞留出足够的相遇窗口（两波从相对充能器射出到相遇通常 &lt; 5 秒）。</p>
	 */
	protected static final int MAX_LIFETIME_TICKS = 200;

	/** 最大飞行距离（格）：超过即消散，与寿命上限互为兜底。 */
	protected static final double MAX_TRAVEL_DISTANCE = 64.0;

	private int waveLevel;
	private Vec3 movement = Vec3.ZERO;

	/**
	 * 速度修正值（格/秒，可正可负）：由波速调节器按转速分档叠加施加。
	 * <ul>
	 *   <li><b>叠加语义</b>：每次过波速调节器在此值上 +/− 档位量（多次叠加累积）；</li>
	 *   <li><b>继承</b>：差波器分裂出的子波、拐弯/反弹后的波均保持同一修正值
	 *       （实际速度 = 等级基础速度 + 此值，受上下限约束）；</li>
	 *   <li>与等级解耦：等级基础速度（2/4/6）不变，此值仅为额外叠加层。</li>
	 * </ul>
	 */
	private double speedOffset;

	/**
	 * 电荷极性（能量着电器赋予）：null = 不带电（能量场不作用）。
	 * 携带电荷的波在能量加速/偏转场中受洛伦兹式作用（见 FieldedEntity）。
	 */
	protected com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity charge;

	/** 出生点：用于计算飞行距离上限。 */
	private Vec3 spawnPos;

	/** 已与另一波碰撞（防同 tick 双方各触发一次爆炸）。 */
	private boolean collided;

	/** 上一 tick 是否处于任意能量场内（诊断日志：只记状态翻转，避免刷屏）。仅服务端使用，不存 NBT。 */
	private boolean wasInsideField;

	/**
	 * <b>最近路径折线</b>：攻击场判定"这只波有没有真的从场盒里穿过"的依据（见 {@link WavePath}）。
	 * 由本类每 tick 压入一个落点，对外只读（{@link #pathCrosses}）。
	 */
	private final WavePath wavePath = new WavePath();

	/**
	 * 本 tick 的位移是否由波<b>自己</b>走出（{@code true} 只包住 tick 里那一句自走 {@code setPos}）。
	 *
	 * <p>{@code false} 期间的任何 {@link #setPos} 都算"外力搬运"——机器把波推出方块外、原路遣返、
	 * 结构场景坐标换算等。那种一跳不是飞行位移，不能参与"穿过判定"，否则攻击场会把整段瞬移路径上的
	 * 场盒都误算成穿过（断开语义见 {@link #pathBroken}）。</p>
	 */
	private boolean selfPropelled;

	/**
	 * 自上次压入落点以来是否发生过外力搬运（瞬移）：下一次 {@link WavePath#push} 把该段标为断点，
	 * 同时 {@link #pathCrosses} 连带跳过"尾点 → 实时位置"这一段。
	 *
	 * <p><b>刻意用原始类型</b>：{@link #setPos} 会被<b>父类 {@code Entity} 的构造器</b>调用
	 * （{@code Entity.<init>} 内部就会 {@code setPos(0,0,0)}），那一刻字段初始化器还没执行——
	 * boolean 有默认值 false 所以安全；换成任何对象字段、或在这里碰 {@code wavePath}，都会 NPE
	 * （2026-09 真实崩溃 {@code crash-2026-09-14_09.47.56-server.txt}）。
	 * <b>{@link #setPos} 里只能碰原始类型——这条约束改不得。</b></p>
	 */
	private boolean pathBroken;

	/**
	 * 调级器增强延迟（格）：穿过能量调级器（顺基准）后还需飞行 0.5 格
	 * 才升级（0.5 格 ÷ 波速 v = 1/(2v) 秒）。0 = 无待升级。
	 */
	protected float boostRemaining;

	/**
	 * 调级器增强步数（本次延迟升级一次提升几级）：翡翠/蓝宝石恒为 1；
	 * 星辉石调级器按授予时的本机转速可为 2（单次 +2，等级仍封顶于
	 * {@link WaveLevels#MAX_LEVEL}）。由机器侧的调级器处置器（W6-a 起住第二层，
	 * 经 {@link WaveMachineHandler} 契约回调）在授予延迟升级时从调级器 BE 机型参数
	 * （{@code getBoostStepForSpeed()}）写入。
	 */
	protected int boostStep = 1;

	/**
	 * 当前渲染颜色（RGB 0-1）：每 tick 向目标颜色插值靠近，实现升降级/遣返时的平滑渐变。
	 * 目标色：正常 = 当前等级色；待升级（boostRemaining &gt; 0）时提前变为下一等级色，
	 * 使波穿出调级器后颜色即开始过渡，而非延迟结束瞬间跳变。
	 */
	private Vec3 renderColor;

	/**
	 * 能量波核心加工逻辑（独立抽取至 {@link ChargerWaveProcessor}）：
	 * 命中掉落物/置物台时按 charging 配方匹配 → 消耗输入 → 产出结果。
	 */
	private ChargerWaveProcessor processor;

	/** Sable 物理结构碰撞协调。 */
	private final WaveSubLevelCollisions subLevelCollisions;
	/** Create contraption 碰撞协调。 */
	private final WaveContraptionCollisions contraptionCollisions;

	protected AbstractChargerWaveEntity(EntityType<?> type, Level level) {
		super(type, level);
		this.processor = new ChargerWaveProcessor(level, 1);
		this.renderColor = getWaveColor();
		this.subLevelCollisions = new WaveSubLevelCollisions(this);
		this.contraptionCollisions = new WaveContraptionCollisions(this);
	}

	/**
	 * 主构造：以任意方向向量出生（支持 Sable 物理结构旋转后的任意朝向）。
	 *
	 * @param type        实体类型
	 * @param level       出生世界（主世界；结构场景由调用方转换好世界坐标后传入）
	 * @param pos         出生位置（世界坐标）
	 * @param movementDir 飞行方向（任意向量，无需单位化，内部 normalize）
	 * @param waveLevel   波等级（1=α，2=β，3=γ）
	 */
	protected AbstractChargerWaveEntity(EntityType<?> type, Level level, Vec3 pos, Vec3 movementDir, int waveLevel) {
		this(type, level);
		this.waveLevel = waveLevel;
		this.processor = new ChargerWaveProcessor(level, waveLevel);
		this.movement = movementDir.normalize();
		this.renderColor = getWaveColor();
		this.spawnPos = pos;
		setPos(pos);
		// 服务端/客户端实例都写入同步数据（Jade 等客户端读取需要）
		this.entityData.set(WAVE_LEVEL, waveLevel);
		this.entityData.set(SPEED_OFFSET, 0f);
	}

	/** 等级同步 key（客户端 Jade 显示用） */
	private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> WAVE_LEVEL =
		SynchedEntityData.defineId(AbstractChargerWaveEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
	/** 速度修正同步 key（客户端 Jade 显示用；double 无序列化器，用 FLOAT 精度足够） */
	private static final net.minecraft.network.syncher.EntityDataAccessor<Float> SPEED_OFFSET =
		SynchedEntityData.defineId(AbstractChargerWaveEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
	/** 电荷同步 key（客户端 Jade 显示用）：0=无 1=正 2=负 */
	private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> CHARGE =
		SynchedEntityData.defineId(AbstractChargerWaveEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

	/** 波型 id（同步给客户端：拖尾特效按波型分风格；用 id 字符串，因为扩展模组注册的波型没有序数）。 */
	private static final net.minecraft.network.syncher.EntityDataAccessor<String> WAVE_TYPE =
		SynchedEntityData.defineId(AbstractChargerWaveEntity.class, net.minecraft.network.syncher.EntityDataSerializers.STRING);

	/**
	 * <b>发射批次</b>（同步给客户端）：同一次技能发射产生的全部波（主波 + 同向并排的分叉波）
	 * 共用一个<b>非零</b>批次号，{@code 0} 表示"不属于任何批次"（充能器发射的机器波恒为 0）。
	 *
	 * <p><b>它唯一的作用是"同批豁免碰撞"</b>（用户 2026-10-02 星界轮裁定）：
	 * 一次技能发射的多枚波是<b>并排</b>出去的（偏移只有 0.35/0.22 格，而碰撞盒外扩 0.4 ⇒
	 * 出生瞬间就在彼此的命中盒里），而本仓长期口径是"任意两波相交即 triggerBoom + 相互湮灭"，
	 * 不豁免就是"点按第二枚即自爆"。豁免范围<b>刻意只有"同批次"</b>：批次号不同（别人的波、
	 * 两台机器互相打的波）照旧正常爆炸湮灭 —— 长期口径不变。</p>
	 *
	 * <p><b>为什么是 {@link SynchedEntityData} 而不是 NBT</b>（用户明确要求"照现有波字段的形状"）：
	 * 豁免判定跑在<b>服务端</b>（两波都在服务端实例上，字段直读就够），而同步字段的意义在于
	 * <b>客户端也拿得到同一次发射的编组</b>——将来客户端要按批次做特效/分组渲染时不必再改同步协议。
	 * NBT 只在存档读写时生效，救不了"服务端权威值要立刻到客户端"这条需求。</p>
	 *
	 * <p>用 {@code INT}（不是 UUID）：批次号是进程内自增的短标识，只要求"同一时刻同一维度内不重复"，
	 * 不要求全局唯一 —— 位宽 32 位、只在同一次发射内比较，代价最低。</p>
	 */
	private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> FIRING_BATCH =
		SynchedEntityData.defineId(AbstractChargerWaveEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

	/**
	 * <b>魔素</b>的同步 key（2026-10-03 需求 coe-ess 批 3）：{@link WaveTrailStyle} 的<b>枚举名</b>，
	 * 空串 / 未知名字 = 未设（⇒ 回落波型风格）。
	 *
	 * <p>照 {@link #WAVE_TYPE} 的既成形状：枚举本身没有 {@code EntityDataSerializer}，
	 * 而字符串同步对"未知值 / 老客户端 / 缺省"都能容错 —— 读不出来就是"没有魔素"，
	 * 而不是抛异常。{@code getEssence()} 的客户端分支就查这个值。</p>
	 */
	private static final net.minecraft.network.syncher.EntityDataAccessor<String> ESSENCE =
		SynchedEntityData.defineId(AbstractChargerWaveEntity.class, net.minecraft.network.syncher.EntityDataSerializers.STRING);

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(WAVE_LEVEL, 0);
		builder.define(SPEED_OFFSET, 0f);
		builder.define(CHARGE, 0);
		builder.define(WAVE_TYPE, "");
		builder.define(FIRING_BATCH, 0);
		builder.define(ESSENCE, "");
	}

	/**
	 * 这次波的发射批次号（0 = 不属于任何批次）。
	 *
	 * <p>服务端与客户端读的是同一个同步值（写入只有 {@link #setFiringBatch(int)} 一处）。</p>
	 */
	public int getFiringBatch() {
		return this.entityData.get(FIRING_BATCH);
	}

	/**
	 * 标记本波属于哪一次发射（同一次技能发射的主波/分叉波传同一个号）。
	 *
	 * <p>只允许设置一次（构造期由子类或发射方写入）；{@code 0} 表示清除批次
	 * （清除后该波回到"任何波都能与它湮灭"的默认口径）。</p>
	 */
	public void setFiringBatch(int batch) {
		this.entityData.set(FIRING_BATCH, batch);
	}

	/**
	 * <b>两波是否同属一次发射</b>（"同批豁免碰撞"的唯一判据）。
	 *
	 * <p>刻意要求<b>双方都非 0</b>：批次号 0 是"没有批次"（机器发射的波、老存档、扩展模组造的波），
	 * 若把 0 也当成一个批次，全场的机器波就会互相豁免 —— 那是把长期口径整个推翻，
	 * 而不是加一条最小例外（需求 §3.3(f) 第 2 条明确禁止）。</p>
	 */
	public static boolean sameFiringBatch(AbstractChargerWaveEntity a, AbstractChargerWaveEntity b) {
		if (a == null || b == null) {
			return false;
		}
		int left = a.getFiringBatch();
		return left != 0 && left == b.getFiringBatch();
	}

	// ==================================================================================
	// 通用可选要素（2026-10-02 作者裁定：「能量波是由好几个要素定义的」，给既有波<b>加要素</b>
	// 符合口径，另造实体才违反 ⇒ 星芒嬗震需要而既有实体没有的两件事，做成"可选、默认关闭"的
	// 要素挂在本共享实体上）
	//
	// ⛔ 这里<b>不新增实体类型、贴图、模型、渲染器</b>：机器波 / 变器波 / 一切既有波在
	// <b>不设</b>这两个要素时行为与改造前<b>逐字相同</b>——字段默认值就是"关闭"
	// （引用型 null、数值型 0），每条分支的第一句都是"要素未设 ⇒ 直接返回"。
	// ==================================================================================

	/**
	 * <b>环绕波要素</b>之一：<b>锚点实体</b> UUID（{@code null} = <b>不环绕</b>，即默认关闭）。
	 *
	 * <p>设了它以后，本波每 tick 的位置被改写成「锚点位置 + r × (u·cosθ + v·sinθ)」
	 * （见 {@link #applyOrbitElement()}）；锚点不存在/已消散时本波<b>立刻 discard</b>，
	 * 不留孤立波。要求：发射方在设本要素的同时<b>必须</b>把"同一批"的批次号一并继承
	 * （{@link #setFiringBatch(int)}）——环绕波出生点与锚点重合（相距 0），飞起来后 0.8 格半径
	 * 在斜相位（两轴分量 0.566）也落进对方命中盒（按轴判据 0.6），不同批就是"第一圈就互相湮灭"。</p>
	 *
	 * <p><b>2026-10-02 批 4（作者裁定 D9 = A）：字段名沿用了 {@code orbitAnchorUuid}，但语义已从
	 * "父波 UUID"放宽成"任意实体的 UUID"</b>——回旋镖的环绕技能（需求 §3.6）就是要拿
	 * <b>那枚镖</b>当锚点（镖是 {@code Projectile}，不是波）。旧口径"必须是
	 * {@code AbstractChargerWaveEntity}"会让这种环绕波<b>出生即死</b>，故判据放宽成
	 * {@code anchor != null && anchor.isAlive()}，运动方向改走契约
	 * {@link OrbitAnchor#orbitDirection()}（波与镖各实现一次）。既有调用方（星芒嬗震传父波）
	 * 走的仍是同一条路径、同一行取值 ⇒ 行为逐字不变。</p>
	 */
	@Nullable
	private UUID orbitAnchorUuid;

	/** 环绕波要素：环绕半径（格）；{@code 0} = 关闭（默认）。 */
	private double orbitRadius;

	/**
	 * <b>环绕波心跳日志的节流刻度</b>（每几 tick 一行位置行）：20 tick = 1 秒一行。
	 *
	 * <p>环绕波寿命上限与主波同为 {@value #MAX_LIFETIME_TICKS} tick ⇒ 单枚最多 10 行心跳，
	 * 量级与"发生了多少事"成正比（与 {@link WaveDiag} 的 trace 通道口径一致）；
	 * 取 20 而不是更小，是为了在"证明真的在绕"与"不刷屏"之间取平衡。</p>
	 */
	private static final int ORBIT_HEARTBEAT_TICKS = 20;

	/**
	 * <b>"父波已经没了"标记</b>（只有环绕波会被置位）：{@link #tick()} 里查出父波取不到/已消散后置位，
	 * 随后 {@link #remove(RemovalReason)} 据此把消散原因写成"父波消散（主波消散 ⇒ 环绕波一起收尾）"，
	 * 而不是笼统的"自身消散"。
	 *
	 * <p>为什么是"标记 + 在唯一的移除回调里记一行"而不是在查不到父波的那一刻直接记：
	 * 实体被移除的路径不止一条（父波没了、命中目标、寿命/行程上限），收尾日志散在各处就必然
	 * 出现"有的路径有日志、有的没有"；{@code remove} 是所有路径的唯一汇合点。</p>
	 */
	private boolean orbitAnchorLost;

	/** 环绕波要素：角速度（<b>弧度/tick</b>，1 圈/秒 = 2π/20）；{@code 0} = 关闭（默认）。 */
	private double orbitAngularSpeed;

	/**
	 * 环绕波要素：初始相位（弧度）。{@code 0} = 出生时位于基向量 u 的正方向一侧（默认）。
	 * 刻意用 {@code tickCount} 当时间基（θ = 相位 + 角速度 × 已存活 tick）⇒ 不需要额外字段，
	 * 也不受读档/重载影响。
	 */
	private double orbitPhase;

	/**
	 * 本 tick 的<b>当前相位</b>（弧度；只有环绕波有意义）——粒子侧拿它把"环面留痕桩"铺在
	 * 波此刻所在的那一圈上，使"它绕到哪儿了"一眼可读。
	 *
	 * <p>刻意不在粒子侧重算 {@code 相位 + 角速度 × tickCount}：那会让"位置公式"与"粒子公式"
	 * 变成两处真源（改一处忘一处就漂移）。这里只有一个写入点（{@link #applyOrbitElement()}）。</p>
	 */
	private double orbitCurrentPhase;

	/**
	 * 本 tick 的<b>父波位置</b>（只有环绕波有意义；父波取不到时为 {@code null}）。
	 *
	 * <p>用途有二：粒子侧要拿它算圆周点（位置已改写，父波位置本身没存下来）；
	 * 以及心跳日志的"距父波距离"——这个距离恒等于 {@link #orbitRadius}，正是"半径 0.8 的环绕
	 * 真的在绕"的机器可判证据。</p>
	 */
	@Nullable
	private Vec3 orbitAnchorPos;

	/**
	 * 本 tick 用来铺环的<b>环平面法向</b>（只有环绕波有意义）= {@link #applyOrbitElement()} 里
	 * <b>实际使用</b>的那个方向（父波的运动方向，取不到才回落到自身方向）。
	 *
	 * <p>为什么不能由粒子侧直接读 {@code movement}：位置改写用的是<b>父波</b>的方向，而能量场可以把
	 * 父波的方向逐 tick 掰弯（{@code setFieldVelocity}）——两者一旦不同，粒子圈的法向就与真实轨道
	 * 错开（画出一个跟轨道不平行的环）。这里让位置改写把"我这一 tick 到底按哪个法向铺的环"记下来，
	 * 粒子侧只读这个值 ⇒ 圈与轨道必然共面。</p>
	 */
	@Nullable
	private Vec3 orbitRingNormal;

	/**
	 * <b>命中附加效果要素</b>：命中生物时追加施加的药水效果（{@code null} = <b>不做事</b>，
	 * 即默认关闭）。
	 *
	 * <p>施加走原版 {@link net.minecraft.world.entity.LivingEntity#addEffect} ⇒
	 * NeoForge 的 {@code MobEffectEvent.Applicable} 照常触发，于是既有的一切"效果免疫"
	 * 拦截点（例如星辉石凝能佩免疫嬗乱：{@code MedallionEffectHandler#onEffectApplicable}）
	 * <b>自动生效</b>，本类不写任何一套自己的免疫判据。</p>
	 */
	@Nullable
	private Holder<MobEffect> hitEffect;

	/** 命中附加效果要素：持续 tick 数；{@code <= 0} = 不做事（默认 0）。 */
	private int hitEffectDuration;

	/** 命中附加效果要素：效果等级（amplifier，0 = I 级）；默认 0。 */
	private int hitEffectAmplifier;

	/**
	 * 设置<b>环绕波要素</b>（一次设全；调用方还必须继承父波批次号，见 {@link #orbitAnchorUuid}）。
	 *
	 * @param anchorWaveUuid  父波 UUID（非 null）
	 * @param radius          环绕半径（格）
	 * @param angularSpeedRad 角速度（弧度/tick；1 圈/秒 = 2π/20）
	 * @param phaseRad        初始相位（弧度）
	 */
	public void setOrbitAnchor(UUID anchorWaveUuid, double radius, double angularSpeedRad, double phaseRad) {
		this.orbitAnchorUuid = anchorWaveUuid;
		this.orbitRadius = radius;
		this.orbitAngularSpeed = angularSpeedRad;
		this.orbitPhase = phaseRad;
	}

	/** 父波 UUID（{@code null} = 本波不环绕 ⇒ 要素关闭）。 */
	@Nullable
	public UUID getOrbitAnchorUuid() {
		return orbitAnchorUuid;
	}

	/** 是否设了环绕波要素（诊断/关卡用；{@code false} = 行为与改造前逐字相同）。 */
	public boolean isOrbiting() {
		return orbitAnchorUuid != null;
	}

	/** 环绕半径（格）。 */
	public double getOrbitRadius() {
		return orbitRadius;
	}

	/** 环绕角速度（弧度/tick）。 */
	public double getOrbitAngularSpeed() {
		return orbitAngularSpeed;
	}

	/** 环绕初始相位（弧度）。 */
	public double getOrbitPhase() {
		return orbitPhase;
	}

	/**
	 * 设置<b>命中附加效果要素</b>（可选；不调用 = 命中只走既有链）。
	 *
	 * @param effect        要施加的效果（Holder；一般传 {@code DeferredHolder}）
	 * @param durationTicks 持续 tick 数（{@code <= 0} 视为不做事）
	 * @param amplifier     效果等级（0 = I 级）
	 */
	public void setHitEffect(Holder<MobEffect> effect, int durationTicks, int amplifier) {
		this.hitEffect = effect;
		this.hitEffectDuration = durationTicks;
		this.hitEffectAmplifier = amplifier;
	}

	/** 命中附加效果（{@code null} = 要素关闭）。 */
	@Nullable
	public Holder<MobEffect> getHitEffect() {
		return hitEffect;
	}

	/** 命中附加效果持续 tick 数（0 = 要素关闭）。 */
	public int getHitEffectDuration() {
		return hitEffectDuration;
	}

	/** 命中附加效果等级（amplifier）。 */
	public int getHitEffectAmplifier() {
		return hitEffectAmplifier;
	}

	/** 波型（服务端权威值；见 {@link #getWaveType()} 的客户端分支）。 */
	private WaveType waveType = WaveTypes.NORMAL;

	/**
	 * 波当前所属<b>波型</b>：服务端读本字段，客户端按同步 id 现查注册表
	 * （拖尾/绽放特效在客户端播，必须能拿到）。
	 *
	 * <p>服务端与同步值同源——只有 {@link #trySetWaveType(WaveType)} 会改动它们，且两者一起写，
	 * 故不存在"服务端与客户端看到不同波型"的窗口（构造与读档同理）。</p>
	 */
	public WaveType getWaveType() {
		if (level() != null && level().isClientSide)
			return WaveTypes.byIdString(this.entityData.get(WAVE_TYPE));
		return waveType;
	}

	/**
	 * 尝试改变波的<b>波型</b>（<b>一生只能变一次</b>）：当前已不是普通波则拒绝。
	 *
	 * <p>变器"加工波变态"穿波转换 → {@link WaveTypes#OMNI}；"攻击波变态"的场点燃 → {@link WaveTypes#ATTACK}。
	 * 普通波只能作为初值、不能作为目标（否则"变一次"的语义会被绕开）。</p>
	 *
	 * @return 是否真的改变了波型
	 */
	public boolean trySetWaveType(WaveType target) {
		if (target == null || target == WaveTypes.NORMAL || getWaveType() != WaveTypes.NORMAL)
			return false;
		this.waveType = target;
		this.entityData.set(WAVE_TYPE, target.id()
			.toString());
		return true;
	}

	// ==================================================================================
	// 魔素（2026-10-03 需求 coe-ess 批 3）：攻击波专有的<b>纯视觉</b>属性
	//
	// 口径（作者裁定，逐条落在这里）：
	//   · 默认 = <b>火</b>，但"默认"不靠字段初值——字段初值是 {@code null}（<b>未设</b>），
	//     由 {@link #trailStyle()} 的回落把它读成"继承波型风格"；而攻击波的波型风格恰好就是
	//     {@link WaveTrailStyle#DAMAGE}，且 {@link WaveTrailStyle#FIRE} 与它是同一份档案实例
	//     ⇒ "默认火"在结构上成立，且老存档（没有该字段）逐字节走回落，观感零变化。
	//   · 可缺省：NBT 只在该字段真的被设过时才写键（见 addAdditionalSaveData），
	//     读到没有该键 ⇒ 保持 null ⇒ 回落波型风格；老存档不会崩。
	//   · 允许中途改（<b>没有</b>"一生一次"约束——那是 {@link #trySetWaveType} 对波型的约束，
	//     魔素不受它管）。
	//   · <b>攻击波专有</b>：非攻击波被显式设置时<b>忽略</b>（不写字段、不报错、返回 false，见
	//     {@link #trySetEssence}）。因此"读它"这件事不需要再写第二道"是不是攻击波"的特判——
	//     非攻击波的身上不可能有非 null 的魔素。
	// ==================================================================================

	/** 魔素（服务端权威值；{@code null} = 未设 ⇒ 继承波型风格）。 */
	@Nullable
	private WaveTrailStyle essence;

	/**
	 * 本波的<b>魔素</b>（{@code null} = 未设）。服务端读本字段，客户端按同步名字现查枚举
	 * （与 {@link #getWaveType()} 同一形状：两者都由 {@link #trySetEssence} 一起写，
	 * 故不存在"服务端与客户端看到不同魔素"的窗口）。
	 */
	@Nullable
	public WaveTrailStyle getEssence() {
		if (level() != null && level().isClientSide)
			return essenceByName(this.entityData.get(ESSENCE));
		return essence;
	}

	/**
	 * 按<b>枚举名</b>查魔素（同步数据与 NBT 的容错入口，<b>认不出来就是"没有"</b>）。
	 *
	 * <p><b>为什么不用 {@code WaveTrailStyle.valueOf}</b>：同步值可能是空串、未知名字，
	 * 老存档里也可能留着被改过的字符串 —— {@code valueOf} 会抛
	 * {@code IllegalArgumentException}，而这条路径跑在实体的 tick / 读档里（一抛就是崩）。
	 * 本方法与 {@code ChargerWaveFx#profile} 的"未登记值静默回落"同一口径：
	 * 认不出来 ⇒ {@code null} ⇒ {@link #trailStyle()} 回落"继承波型风格"（不崩、不改玩法）。</p>
	 */
	@Nullable
	private static WaveTrailStyle essenceByName(String name) {
		if (name == null || name.isEmpty())
			return null;
		for (WaveTrailStyle style : WaveTrailStyle.values()) {
			if (style.name()
				.equals(name))
				return style;
		}
		return null;
	}

	/**
	 * 设置<b>魔素</b>（<b>可随时改</b>——无"一生一次"约束）。
	 *
	 * <p><b>非攻击波被显式设置时忽略</b>（作者裁定：魔素是攻击波专有；§3.2 的"忽略 + 不设"）：
	 * 不写字段、不报错，返回 {@code false}。判据取波型自报的 {@link WaveType#dealsDamage()}
	 * （攻击态 = 会伤害的波），<b>不</b>在这里写死某一个内置波型 —— 扩展模组注册的
	 * "会伤害的波型"因此自动获得魔素能力。</p>
	 *
	 * <p>{@code null} = 清除魔素（回落波型风格；任何波型都允许清除）。</p>
	 *
	 * @return 是否真的写进去了
	 */
	public boolean trySetEssence(@Nullable WaveTrailStyle target) {
		if (target == null) {
			this.essence = null;
			this.entityData.set(ESSENCE, "");
			return true;
		}
		if (!getWaveType().dealsDamage())
			return false;
		this.essence = target;
		this.entityData.set(ESSENCE, target.name());
		return true;
	}

	/**
	 * <b>本波真正生效的拖尾风格</b> —— 全仓<b>唯一</b>的风格来源（2026-10-03 需求 coe-ess 批 3，口径 1）。
	 *
	 * <p><b>公式只有这一处</b>：{@code 有魔素（且已登记）⇒ 用魔素；否则继承波型风格}。
	 * 全部 32 处风格消费点（波实体的拖尾/绽放/爆炸 · 命中解析 · 碰撞协调 · 第二层的差波器与
	 * 波门处理器）都必须调本方法，<b>不许</b>在别处再写一份
	 * {@code essence != null ? ... : ...} 的同形（关卡 {@code wave-essence-single-source} 守着这条）。</p>
	 *
	 * <p>刻意<b>不</b>写"只对攻击波读魔素"的特判（作者明确否决）：非攻击波身上不可能有魔素
	 * （设置侧就忽略了，见 {@link #trySetEssence}），再判一次只会多一处会漂移的分支。</p>
	 *
	 * <p>{@code ChargerWaveFx.isRegistered} 那一问也是必需的：未登记的枚举值会被
	 * {@code ChargerWaveFx#profile} 静默回落成 {@code NORMAL}（不崩、不改玩法）——
	 * 那与"没有魔素 ⇒ 继承波型风格"是两回事，不先问就会让"设了一个没登记的魔素"表现为
	 * "这枚攻击波突然变成裸染色尘埃"。</p>
	 */
	public WaveTrailStyle trailStyle() {
		WaveTrailStyle essence = getEssence();
		// ↓ 这一句是本方法唯一的"回落"分支：继承波型风格（= 老行为，也是默认魔素"火"的载体）。
		return essence != null && ChargerWaveFx.isRegistered(essence) ? essence : getWaveType().trailStyle();
	}

	/**
	 * 本波拖尾主粒子的<b>调用方基线尺度</b>（风格档案可覆盖，见 {@code StyleProfile#effectiveScale}）。
	 *
	 * <p>几何（{@link #isOrbiting()}）只影响<b>这一组量</b>（颗数 / 尺度）：环绕波比主波更粗更密
	 * （作者 2026-10-02："即使有环绕波生成，过于不明显也是 bug"）。它<b>不</b>参与"用哪个风格档案"
	 * 的选择 —— 那由 {@link #trailStyle()} 唯一决定（这才是需求 §3.4"粒子由魔素决定"的含义）。</p>
	 */
	private float trailScale() {
		return isOrbiting() ? ChargerWaveFx.ORBIT_TRAIL_SCALE : 0.45f;
	}

	/** 本波拖尾主粒子的<b>调用方基线颗数</b>（见 {@link #trailScale()} 的说明）。 */
	private int trailCount() {
		return isOrbiting() ? ChargerWaveFx.ORBIT_TRAIL_COUNT : 6;
	}

	/**
	 * <b>位置写入总入口</b>（覆写：区分"自己飞"与"被机器挪"）。
	 *
	 * <p>{@link net.minecraft.world.entity.Entity#setPos(Vec3)} 是 {@code final}，它内部就会调到本方法，
	 * 所以自走位移（tick 里唯一的 {@code setPos(position().add(step))}）与所有外力搬运都必然经过这里，
	 * 不必去 20 多处调用点逐个加标记（漏一处就等于漏一个误判窗口）。</p>
	 *
	 * <p>非自走写入只置一个原始类型标志（{@link #pathBroken}）：瞬移那一跳不能当作飞行位移参与攻击场的
	 * 穿过判定，而且必须"当场"生效——瞬移可能落在"一次观测之后、下一次采样之前"，等到下次采样才断就
	 * 已经晚了一步（验证脚本实测过这一点）。<b>不改变任何位置/速度行为。</b></p>
	 *
	 * <p><b>为什么这里只能碰原始类型</b>：{@link net.minecraft.world.entity.Entity} 的构造器内部会
	 * 调用 {@code setPos(0,0,0)}，而覆写方法在字段初始化器之前就已经可能被调用（对象还没构造完、
	 * 对象字段全是 null）。2026-09 曾因此在 {@code wavePath} 上抛 NPE、开炮即服务端崩溃
	 * （{@code crash-2026-09-14_09.47.56-server.txt}）。任何需要对象的动作都必须挪到
	 * {@link #tick()} 或 {@code pathCrosses} 里去。</p>
	 */
	@Override
	public void setPos(double x, double y, double z) {
		super.setPos(x, y, z);
		if (!selfPropelled)
			pathBroken = true;
	}

	/**
	 * <b>波最近一段路径是否与给定盒相交</b>——攻击场"真的穿过场盒"的唯一判定入口。
	 *
	 * <p>把几何问题转交轨迹对象（{@link WavePath#crosses}）：调用方只问"有没有穿过"，
	 * 拿不到也改不了轨迹本身。判定覆盖"最近 {@link WavePath#MAX_OBSERVATION_GAP_TICKS} tick 的
	 * 全部位移 + 当前实时位置"，因此不再依赖"观测节拍 × 波速"的赛跑，也不必读原版
	 * {@code xo/yo/zo}（那只有一 tick，且对刚出生的波是 (0,0,0)）。</p>
	 */
	public boolean pathCrosses(AABB box) {
		return wavePath.crosses(box, position(), pathBroken);
	}

	/**
	 * <b>环绕波要素</b>（可选、默认关闭，见 {@link #orbitAnchorUuid}）：把自身位置改写成
	 * <b>{@code 锚点位置 + r × (u·cosθ + v·sinθ)}</b>。
	 *
	 * <p><b>2026-10-02 批 4（作者裁定 D9 = A）</b>：本文档原先逐字写的是"父波"——
	 * 锚点判据已从 {@code instanceof AbstractChargerWaveEntity} 放宽成
	 * {@code anchor != null && anchor.isAlive()}（见 {@link OrbitAnchor}），
	 * 锚点可以是<b>任何实体</b>（回旋镖的环绕技能就是拿镖当锚点）。
	 * 下文的"父波"一律读作"锚点"；对既有调用方（星芒嬗震传的是父波）两者是同一个对象，
	 * 取值与判据<b>逐字不变</b>。</p>
	 *
	 * <h2>坐标基（环平面垂直于锚点运动方向）</h2>
	 * <p>取锚点运动方向 {@code d}（{@link OrbitAnchor#orbitDirection()}；单位向量），
	 * 再取一个与本方向不平行的参考轴
	 * {@code reference}（{@code |d.y| > 0.9} 时用 +X，否则用 +Y——避免叉乘退化），
	 * 然后</p>
	 * <pre>
	 *   u = normalize(reference × d)      // 垂直于 d 的平面基之一
	 *   v = normalize(d × u)              // 与 u、d 都垂直（u × v = d，右手系）
	 * </pre>
	 * <p>于是 {@code u}、{@code v} 张成的平面<b>垂直于运动方向</b>，圆周点落在"以锚点为圆心、
	 * 垂直于飞行方向的环"上：θ = 0 时在 {@code u} 正方向一侧，θ 增大时按 u→v 方向旋转。
	 * 时间基用 {@code tickCount}（θ = 初始相位 + 角速度 × 已存活 tick）⇒ 不额外占字段。</p>
	 *
	 * <h2>默认关闭与两个边界</h2>
	 * <ul>
	 *   <li>要素未设（{@code orbitAnchorUuid == null}）⇒ <b>第一句就返回 true</b>，位置一个字不改
	 *       ——机器波 / 变器波 / 一切既有波的行为与改造前逐字相同；</li>
	 *   <li>客户端 / Ponder 场景（不是 {@link ServerLevel}）⇒ 不动位置（客户端位置由服务端同步，
	 *       客户端 tick 本来就在移动之前 return）；</li>
	 *   <li>锚点取不到或已消散（{@code isAlive() == false}）⇒ 返回 {@code false}，
	 *       调用方<b>立刻 {@code discard()}</b> ⇒ "锚点消散 ⇒ 环绕波一起收尾"只有这一条实现，
	 *       不再叠第二层机制，也不会留下孤立波（对镖同样成立：镖 {@code discard()} 后
	 *       环绕波下一 tick 自己收尾）。</li>
	 * </ul>
	 *
	 * @return {@code false} = 锚点已不存在，调用方必须 discard 自己
	 */
	private boolean applyOrbitElement() {
		if (this.orbitAnchorUuid == null) {
			return true;
		}
		if (!(level() instanceof ServerLevel server)) {
			return true;
		}
		Entity anchor = server.getEntity(this.orbitAnchorUuid);
		// ★ 批 4（作者裁定 D9 = A）：锚点判据从"必须是另一枚波"放宽成"是个活着的实体"——
		// 回旋镖的环绕技能要拿**那枚镖**当锚点，而镖是 Projectile、不是波；旧判据会让环绕波
		// 出生即死（第一 tick 就查不到"父波"）。既有调用方（星芒嬗震）传的仍是波 ⇒ 走的还是
		// 下面同一条路径，行为逐字不变。
		if (anchor == null || !anchor.isAlive()) {
			return false;
		}
		// ★ "取运动方向"抽成契约（OrbitAnchor#orbitDirection）：波 = getMovement()（与改造前
		// 逐字同源）、镖 = getDeltaMovement()。非 OrbitAnchor 的实体回落到原版速度向量。
		Vec3 dir = orbitDirectionOf(anchor);
		if (dir.lengthSqr() < 1.0E-9D) {
			dir = movement;
		}
		if (dir.lengthSqr() < 1.0E-9D) {
			dir = new Vec3(0.0D, 0.0D, 1.0D);
		}
		dir = dir.normalize();
		Vec3[] axes = orbitPlaneAxes(dir);
		Vec3 u = axes[0];
		Vec3 v = axes[1];
		double theta = orbitPhase + orbitAngularSpeed * (double) tickCount;
		Vec3 anchorPos = anchor.position();
		Vec3 offset = u.scale(orbitRadius * Math.cos(theta)).add(v.scale(orbitRadius * Math.sin(theta)));
		setPos(anchorPos.add(offset));
		// 只记三个原始事实，供粒子/日志用（见三个字段的说明）；位置公式本身仍只有上面这一处。
		this.orbitCurrentPhase = theta;
		this.orbitAnchorPos = anchorPos;
		this.orbitRingNormal = dir;
		logOrbitDiag(anchorPos);
		return true;
	}

	/**
	 * <b>环绕波锚点契约（{@link OrbitAnchor}）的"取运动方向"实现 —— 波这一侧</b>
	 * （2026-10-02 批 4；作者裁定 D9 = A）。
	 *
	 * <p>实现体<b>只有一行</b>，而且就是改造前 {@code applyOrbitElement()} 里那一行
	 * （{@code Vec3 dir = parent.getMovement();}）—— 所以"波当锚点"的环平面法向与改造前
	 * <b>逐字相同</b>（这是"既有波行为不变"最直接的一条证据）。</p>
	 *
	 * <p>刻意<b>不</b>返回 {@code getDeltaMovement()}：波的手写位移只走 {@code setPos}，
	 * 从不写原版速度字段（那个字段对波恒为零向量）—— 换成它等于把环平面的法向换成"永远退化"，
	 * 既有环绕波会立刻歪到 +Z 平面上。</p>
	 */
	@Override
	public Vec3 orbitDirection() {
		return getMovement();
	}

	/**
	 * <b>取锚点运动方向的唯一一处</b>（环绕波几何的第二个入口，
	 * 与 {@link #orbitPlaneAxes(Vec3)} 一起构成"环平面几何只有一处实现"）：
	 * 锚点实现了 {@link OrbitAnchor} 就问它（波 / 镖各一处实现），
	 * 否则回落到原版速度向量（任何实体的通用运动方向）。
	 *
	 * <p><b>为什么抽成静态方法而不是把三元表达式写进 {@link #applyOrbitElement()}</b>：
	 * 关卡要能钉住"镖与波各实现一次"，而不是钉住某一个调用点的写法。</p>
	 */
	public static Vec3 orbitDirectionOf(Entity anchor) {
		if (anchor instanceof OrbitAnchor orbitAnchor) {
			return orbitAnchor.orbitDirection();
		}
		return anchor.getDeltaMovement();
	}

	/**
	 * 两位小数、<b>与区域设置无关</b>的格式化（日志要机器可比对：某些区域会把小数点写成逗号）。
	 */
	private static String fmt2(double value) {
		return String.format(java.util.Locale.ROOT, "%.2f", value);
	}

	/**
	 * <b>环绕波的诊断行（调试可见性；作者 2026-10-02：「现在尝试添加调试行，我来进行测试有没有环绕波生成」）</b>。
	 *
	 * <p>三条时刻线，全部走 {@link WaveDiag#trace}（AGENTS 红线：波相关日志的唯一出口），
	 * <b>不新建第二套日志前缀</b>：</p>
	 * <ol>
	 *   <li><b>出生</b>（{@code tickCount == 1}）：父波 UUID、批次、半径、角速度（弧度/tick 与圈/秒）、
	 *       初始相位 —— 一眼能判"它到底生成了没有、按什么参数绕"；</li>
	 *   <li><b>位置心跳</b>（每 20 tick 一行，节流）：当前 tick、自身坐标、父波坐标、
	 *       <b>距父波距离</b>（恒等于半径 ⇒ 这就是"r≈0.8 真的在绕"的机器可判证据）、批次；</li>
	 *   <li><b>消散</b>：见 {@link #remove(RemovalReason)}（所有移除路径的唯一汇合点）。</li>
	 * </ol>
	 *
	 * <p>节流刻度刻意与"同批次豁免碰撞"那行（{@code tickCount % 20}）取同一拍：
	 * 一次发射的环绕波与并排主波在同一 tick 打日志，读起来能对齐。</p>
	 */
	private void logOrbitDiag(Vec3 anchorPos) {
		if (tickCount == 1) {
			WaveDiag.trace(
				"环绕波出生：{} 级波（{}），父波 UUID {}，批次 {}（继承父波），半径 {} 格、角速度 {} 弧度/tick（{} 圈/秒）、起始相位 {}；位置 = 父波位置 + r×(u·cosθ + v·sinθ) 逐 tick 改写",
				waveLevel, WaveLevels.glyph(waveLevel), orbitAnchorUuid, getFiringBatch(),
				fmt2(orbitRadius), fmt2(orbitAngularSpeed),
				fmt2(orbitAngularSpeed * 20.0D / (Math.PI * 2.0D)),
				fmt2(orbitPhase));
		} else if (tickCount % ORBIT_HEARTBEAT_TICKS == 0) {
			WaveDiag.trace(
				"环绕波心跳：tick {}，自身 {}，父波 {}，距父波 {} 格（恒 = 半径 {} 格），批次 {}",
				tickCount, position(), anchorPos, fmt2(position().distanceTo(anchorPos)),
				fmt2(orbitRadius), getFiringBatch());
		}
	}

	/**
	 * <b>环平面基向量</b>（环绕波几何的<b>唯一</b>实现）：给定环平面法向 {@code d}（父波运动方向），
	 * 返回两个张成该平面的单位向量 {@code [u, v]}，满足 {@code u × v = d}（右手系）。
	 *
	 * <p>取法：{@code |d.y| > 0.9}（接近竖直飞行，用 +Y 会叉乘退化）时参考轴取 +X，否则取 +Y；</p>
	 * <pre>
	 *   u = normalize(reference × d)
	 *   v = normalize(d × u)
	 * </pre>
	 *
	 * <p><b>为什么抽成一处</b>：位置改写（{@link #applyOrbitElement()}）与粒子侧
	 * （"环面留痕桩"必须铺在同一个环平面上）都要用这两个基向量；复制成两份的那一刻起，
	 * 改了一处忘另一处就会让粒子圈与真实轨道错开。</p>
	 */
	public static Vec3[] orbitPlaneAxes(Vec3 d) {
		Vec3 dir = d == null || d.lengthSqr() < 1.0E-9D ? new Vec3(0.0D, 0.0D, 1.0D) : d.normalize();
		Vec3 reference = Math.abs(dir.y) > 0.9D
			? new Vec3(1.0D, 0.0D, 0.0D)
			: new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 u = reference.cross(dir).normalize();
		Vec3 v = dir.cross(u).normalize();
		return new Vec3[] { u, v };
	}

	/**
	 * <b>几何专属层的粒子</b>：只有"这枚波在环绕"这件事能产生的那两簇
	 * （2026-10-03 需求 coe-ess §3.4 的 B 层；只在服务端调用，唯一调用点是 {@link #tick()} 里
	 * {@code isOrbiting()} 的那条分支）。
	 *
	 * <p><b>本方法刻意只画几何</b>——环绕波的<b>风格粒子</b>（主体尘埃 + 该魔素的点缀，包括
	 * "异"那套 END_ROD / 青焰）已并入风格层，与主波走同一个
	 * {@link ChargerWaveFx#sendTrail} 调用。留在这里的两件东西都依赖环平面：
	 * <b>环面留痕桩</b>（{@link ChargerWaveFx#sendOrbitMarks}）与<b>出生整圈标记</b>
	 * （{@link ChargerWaveFx#burstOrbitSpawn}）。它们<b>不随魔素变</b>，也不进任何风格档案 ——
	 * 主波没有环平面，把桩点塞进"异"档案会在平飞路径上画出一圈没有意义的点。</p>
	 *
	 * <p>注意本方法<b>只负责粒子</b>：调用点不等价于 {@code return}，环绕波照样往下走命中判定、
	 * 方块碰撞、波波碰撞与寿命/收尾分支 —— 观感与机制在这里是分开的两件事。</p>
	 *
	 * <p>三个时刻各来一簇（作者 2026-10-02 要求："出生、命中、父波消散一起收尾"三处都要能感知
	 * 它存在过）：</p>
	 * <ol>
	 *   <li><b>出生</b>（{@code tickCount == 1}，实体刚被放进世界的第一 tick）：在父波位置炸一簇
	 *       {@link ChargerWaveFx#burstOrbitSpawn}，并把整圈轨道一次标出来 —— 玩家发射后立刻能
	 *       看到主波周围多了一个环；</li>
	 *   <li><b>每 tick</b>：每逢 {@link ChargerWaveFx#ORBIT_MARK_INTERVAL_TICKS} 补一圈环面留痕桩
	 *       （{@link ChargerWaveFx#sendOrbitMarks}）；</li>
	 *   <li><b>命中与消散</b>：命中走既有的命中链（{@code hitEffect → ChargerWaveFx.burst →
	 *       discard}，用的是环绕波自己的波级色与其 {@link #trailStyle()}）；其余任何移除路径
	 *       （父波没了、寿命与行程上限）在 {@link #remove} 里补最后一簇 —— 见那里。</li>
	 * </ol>
	 */
	private void emitOrbitGeometry(ServerLevel server) {
		Vec3 anchorPos = this.orbitAnchorPos;
		if (anchorPos == null) {
			// 本 tick 还没成功改写位置（理论上不会发生：位置改写就在本 tick 移动段里）——
			// 保守退化：这一 tick 不画任何几何粒子（宁可不画，也不画出错位的圈）。
			return;
		}
		// 环平面法向取"位置改写实际用的那个"（见 orbitRingNormal 的说明）：父波方向被能量场掰弯时，
		// 粒子圈必须跟着同一套几何，才不会画出一个与真实轨道不平行的环。
		Vec3 normal = orbitRingNormal != null ? orbitRingNormal : movement;
		Vec3[] axes = orbitPlaneAxes(normal);
		// 出生簇：实体加入世界后的第一 tick（tickCount 在 super.tick() 里 +1，故首 tick 读作 1）
		if (tickCount == 1) {
			ChargerWaveFx.burstOrbitSpawn(server, anchorPos, renderColor, axes[0], axes[1], orbitRadius,
				orbitCurrentPhase);
		}
		ChargerWaveFx.sendOrbitMarks(server, position(), axes[0], axes[1],
			orbitRadius, orbitCurrentPhase, tickCount % ChargerWaveFx.ORBIT_MARK_INTERVAL_TICKS == 0);
	}

	/**
	 * <b>命中附加效果要素</b>（可选、默认关闭，见 {@link #hitEffect}）：<b>既有命中链之后</b>
	 * 的追加动作。
	 *
	 * <p>既有链一个字不改：{@code 攻击态 ⇒ hurt(WaveLevels.damage(波级)) ⇒ 给穿戴护甲玩家充能
	 * ⇒ 绽放消散}；本方法只在"充能"与"绽放"之间补一次 {@code addEffect}，而且
	 * <b>未设要素时第一句就 return</b>（默认关闭）。</p>
	 *
	 * <p>施加走原版 {@link LivingEntity#addEffect(MobEffectInstance)} ⇒ NeoForge 的
	 * {@code MobEffectEvent.Applicable} 照常触发 ⇒ 一切既有的"效果免疫/拦截"判据自动生效
	 * （如星辉石凝能佩免疫嬗乱），本类不另写免疫逻辑。</p>
	 */
	private void applyHitEffect(LivingEntity target) {
		if (this.hitEffect == null || this.hitEffectDuration <= 0) {
			return;
		}
		target.addEffect(new MobEffectInstance(this.hitEffect, this.hitEffectDuration, this.hitEffectAmplifier));
	}

	@Override
	public void tick() {
		super.tick();

		// Ponder 思索者场景：PonderLevel（SchematicLevel 子类）isClientSide=true，但实体仍被逐帧 tick，
		// 需模拟服务端飞行 + 播客户端粒子，否则场景里波静止无拖尾（仅 clear 时闪一下绽放）
		boolean ponderScene = level() instanceof SchematicLevel;
		if (level().isClientSide && !ponderScene)
			return;

		// 寿命/距离上限：波不撞墙也会自动消散，防止实体无限累积（卡顿主因）。
		// Ponder 场景另有 40 tick 上限（见下），此处只管真实世界。
		if (!ponderScene
			&& (tickCount > MAX_LIFETIME_TICKS
				|| (spawnPos != null && position().distanceToSqr(spawnPos) > MAX_TRAVEL_DISTANCE * MAX_TRAVEL_DISTANCE))) {
			discard();
			return;
		}

		// 路径采样（攻击场"穿过判定"用，见 WavePath）：此刻的位置 = 上一 tick 结束时的落点。
		// 采样在移动之前做，本 tick 的位移由 pathCrosses 里的实时点补上；上次采样之后若发生外力搬运
		// （机器推波/遣返/坐标换算），该段会被标成断点并在判定时跳过。
		wavePath.push(position(), pathBroken);
		pathBroken = false;

		// 移动（速度随等级）。带电荷且身处能量场时，把"名义速度向量"交给场修正：
		// 加速场沿场向增减速、偏转场横向弯折路径（每 tick 微调 movement 方向/速率，呈弧线）。
		Vec3 nominal = movement.scale(getSpeedBlocks()); // 格/秒（当前名义速度向量）
		Vec3 step = nominal.scale(1.0 / 20.0);
		Vec3 corrected = com.hjmmd_8.createoreexpansion.content.energyfield.EnergyFields.applyFields(level(), this);
		if (corrected != null && corrected != nominal && !corrected.equals(nominal)) {
			// 场修正生效：按修正后的速度向量移动，并把主方向/速率对齐到真实速度
			setFieldVelocity(corrected);
			step = corrected.scale(1.0 / 20.0);
		}
		// 这一段是唯一的"自走"位移；selfPropelled 之外的一切 setPos 都记为外力搬运（见 setPos 覆写）。
		// 环绕波要素（可选、默认关闭）也在这段里改写位置：它同样是"波自己走出来的位移"
		// （环上相邻两点只差 0.25 格），故刻意留在 selfPropelled 窗口内，不被当成瞬移断点——
		// 否则攻击场的"真的穿过场盒"判定会永远跳过环绕波。位置改写必须发生在移动<b>之后</b>：
		// 本 tick 的最终位置就是圆周点，命中/粒子/诊断全部按它算。
		selfPropelled = true;
		setPos(position().add(step));
		boolean anchorAlive = applyOrbitElement();
		selfPropelled = false;
		// 父波已消散/取不到 ⇒ 自己立刻收尾（"主波消散 ⇒ 环绕波一起收尾"就靠这一条，不加第二层机制）
		if (!anchorAlive) {
			// "生成了但立刻消散"必须能自证（作者只有日志可看）：唯一原因就是上面那次父波查找失败，
			// 把父波 UUID 与本次批次写出来 ⇒ 日志里可区分"没生成 / 生成即消散 / 一直在飞"。
			// 2026-10-02 第三版：原因先记进字段，完整的"消散"行由 remove() 统一打
			// （所有移除路径都汇合在那里，避免每条 discard 前各写半行而漏掉某条路径）。
			orbitAnchorLost = true;
			discard();
			return;
		}

		// 诊断日志（仅服务端；控制器方块完成后移除）：带电波进出场状态翻转 + 场内每 10 tick 修正摘要
		if (level() instanceof net.minecraft.server.level.ServerLevel server) {
			boolean inside = charge != null
				&& com.hjmmd_8.createoreexpansion.content.energyfield.EnergyFields.isInAnyField(level(), position());
			if (inside != wasInsideField) {
				wasInsideField = inside;
				CoeCore.LOGGER.info("[能量场] 波#{} {}场  pos={}  电荷={}  场内场数={}",
					getId(), inside ? "进入" : "离开", position(), charge,
					com.hjmmd_8.createoreexpansion.content.energyfield.EnergyFields.count(level()));
			}
			if (inside && tickCount % 10 == 0) {
				double oldLen = nominal.length();
				double newLen = corrected.length();
				double dot = oldLen > 1.0E-9 && newLen > 1.0E-9
					? nominal.dot(corrected) / (oldLen * newLen)
					: 1.0;
				dot = net.minecraft.util.Mth.clamp(dot, -1.0, 1.0);
				CoeCore.LOGGER.info("[能量场] 波#{} 场内修正  pos={}  速度 {:.2f}→{:.2f} 格/秒  偏角 {:.1f}°",
					getId(), position(), oldLen, newLen, Math.toDegrees(Math.acos(dot)));
			}
		}

		// CC&A 特斯拉线圈赋电荷：波不带电且贴近线圈（半径 1 格立方体）时，线圈耗电赋一次
		// 随机极性电荷（放电闪光 + 1 秒冷却；CC&A 未安装时静默跳过）。每 tick 探测，防高速波漏检。
		if (charge == null && level() instanceof net.minecraft.server.level.ServerLevel) {
			try {
				WaveMachineIntegrationPoints.chargeNearbyCoil(this);
			} catch (Throwable ignored) {
				// 联动异常：波照常飞行
			}
		}

		// 调级器增强延迟：穿过顺基准调级器后累计飞行距离，满 0.5 格（= 1/(2v) 秒）后等级 +boostStep
		// （翡翠/蓝宝石恒 +1；星辉石按授予时的转速可为 +2）。提升等级封顶于 WaveLevels.MAX_LEVEL（5），
		// 星辉石 +2 从 4 级升到 6 时实际停在 5（ω），不会超上限。
		if (boostRemaining > 0) {
			boostRemaining -= (float) (getSpeedBlocks() / 20d);
			if (boostRemaining <= 0) {
				boostRemaining = 0;
				setWaveLevel(Math.min(waveLevel + boostStep, WaveLevels.MAX_LEVEL));
				boostStep = 1;
			}
		}

		// 平滑渐变：目标色 = 当前等级色；待升级（boostRemaining>0）时提前渐变到提升后的等级色
		// （按 boostStep 预览，封顶 MAX_LEVEL），使波穿出调级器后颜色即开始过渡，而非延迟结束瞬间跳变。
		// 2026-09-14：目标色再过一遍**本波波型的拖尾风格变换**（ChargerWaveFx.styleColor）——
		// 于是"波体本身 / 拖尾 / 绽放 / 爆炸"四处用同一套颜色，加工波一眼就是钢青-黄铜的机械色，
		// 不必等它爆开才认得出（此前只有拖尾粒子被染色，玩家实测反馈"色调看不出来"）。
		// 普通波的风格是恒等变换，故其颜色与改造前逐字不变。
		Vec3 levelColor = boostRemaining > 0
			? getWaveColorForLevel(Math.min(waveLevel + boostStep, WaveLevels.MAX_LEVEL))
			: getWaveColor();
		Vec3 targetColor = ChargerWaveFx.styleColor(trailStyle(), levelColor);
		renderColor = renderColor.lerp(targetColor, 0.15d);
		// 距离足够近则直接贴合目标色，避免无限逼近
		if (renderColor.distanceToSqr(targetColor) < 1.0E-5d)
			renderColor = targetColor;

		// 飞行粒子（密集，沿移动方向散布）：主体 = 当前渲染色（ω=玫红），颜色由本波的拖尾风格决定——
		// 服务端走 ChargerWaveFx.sendTrail、Ponder 场景走 ChargerWaveFx.addTrailParticles，
		// 此处只负责把风格（trailStyle()，唯一解析处）与原有的位置/数量/散布/速度原样传过去，
		// 风格带来的颜色变换与点缀粒子全部在 ChargerWaveFx 的风格映射表里定义。
		// ω（5 级）额外每 tick 叠 1 颗金色尾迹点缀 —— 金色是刻意叠加的装饰色（不是波的渲染色），
		// 故继续用无风格重载，保持金饰不被染色、只占少数，避免整条波看起来发黄。
		//
		// ==================================================================================
		// ★ 粒子分派 = 【风格层】按魔素 + 【几何专属层】按 isOrbiting() 额外叠加
		//   （2026-10-03 需求 coe-ess §3.4；本批把原来的"二选一"整段换掉）
		//
		// 旧形状（2026-10-02 起）是：
		//     if (level() instanceof ServerLevel orbitServer && isOrbiting()) {
		//         emitOrbitTrail(orbitServer);              ← 环绕波自己那整套
		//     } else if (level() instanceof ServerLevel server) {
		//         ChargerWaveFx.sendTrail(..., 0.45f, 6, ...)   ← 主波
		//     }
		// 它是"二选一"：环绕波<b>整段跳过</b>主波那条链路。需求 §3.4 要求改成两层叠加——
		//
		//   ① 风格层（由魔素决定，主波与环绕波走<b>同一条路</b>）：下面的 sendTrail /
		//      addTrailParticles 调用点只有一个，风格实参就是 trailStyle()（唯一解析处）。
		//      几何只经由两个<b>明确</b>输入进来：调用方基线（trailScale() / trailCount()，
		//      环绕波更粗更密）与 mainOnly 旗标（isOrbiting()，雷魔素的强闪光"只挂主波"）。
		//      两者都<b>不</b>参与"用哪个风格档案"——那才是"粒子由魔素决定"的含义。
		//      环绕波原来那套粒子内容（12/0.62 + END_ROD + 青焰）已整份搬进
		//      WaveTrailStyle.ARCANE 的档案，于是主波用"异"魔素时逐字节等于它。
		//   ② 几何专属层（只有"这枚波在环绕"才有，与魔素无关）：环面留痕桩 + 出生整圈标记，
		//      见下面的 isOrbiting() 分支与 emitOrbitGeometry。
		//
		// 为什么<b>只</b>跳过/叠加粒子而不提前 return：后面的命中判定/方块碰撞/波波碰撞/收尾分支
		// 对环绕波仍然有意义（它照样会撞上生物、撞上父波、寿命到点），提前 return 等于顺手
		// 把它的命中与收尾一起关掉——那是改机制，不是改观感（关卡
		// wave-orbit-particles-exclusive 专门守着这条负向断言）。
		// ==================================================================================
		if (level() instanceof ServerLevel server) {
			ChargerWaveFx.sendTrail(server, position(), trailStyle(), renderColor,
				trailScale(), trailCount(), movement.scale(0.12), 0.03, isOrbiting());
			if (isOmega())
				server.sendParticles(ChargerWaveFx.waveParticle(OMEGA_GOLD, 0.5f), getX(), getY(), getZ(), 1,
					movement.x * 0.3, movement.y * 0.3, movement.z * 0.3, 0.05);
		} else if (ponderScene) {
			// Ponder 场景：客户端粒子（PonderLevel.addParticle 已实现，会渲染在场景中）；
			// 与上面<b>同一条风格链路</b>（只是不走网络包），故魔素在 Ponder 里同样生效
			// （需求陷阱 8：只改服务端那条 = Ponder 里"雷闪光只挂主波"失效）。
			ChargerWaveFx.addTrailParticles(level(), position(), trailStyle(), renderColor,
				trailScale(), trailCount(), movement.scale(0.12), isOrbiting());
			if (isOmega())
				level().addParticle(ChargerWaveFx.waveParticle(OMEGA_GOLD, 0.5f), getX(), getY(), getZ(),
					movement.x * 0.3, movement.y * 0.3, movement.z * 0.3);
		}
		// ---- 几何专属层：只有"这枚波在环绕"才有资格发（环面留痕桩 + 出生整圈标记）----
		// 刻意放在风格层<b>之后</b>、且<b>不</b>写成风格层的 else ——两段是叠加关系，
		// 环绕波既吃自己魔素的风格粒子，又额外吃这一层几何粒子。
		if (isOrbiting() && level() instanceof ServerLevel orbitServer) {
			emitOrbitGeometry(orbitServer);
		}

		// Ponder 场景：无真实方块/实体碰撞，飞一段距离后自动消散（remove 时客户端球面绽放）
		if (ponderScene) {
			if (tickCount > 40)
				discard();
			return;
		}

		// 命中生物/掉落物：单次实体查询（复用同一 AABB，避免两次 getEntitiesOfClass 遍历开销）。
		// 波波碰撞优先：两个能量波相遇 → 相互湮灭，触发范围爆炸（见 handleWaveCollision）。
		AABB hitBox = getBoundingBox().inflate(0.4);
		List<AbstractChargerWaveEntity> waves = level().getEntitiesOfClass(AbstractChargerWaveEntity.class,
			hitBox, w -> w != this && w.isAlive() && !w.collided);
		if (!waves.isEmpty()) {
			handleWaveCollision(waves.get(0));
			return;
		}

		// 命中生物：<b>所有波型都碰撞消散</b>（用户 2026-10-01："能量波碰到实体后应该立刻消失，
		// 而不是穿过去，这是非常不合常理的"）。伤害仍然<b>只有攻击态</b>造成 ——
		// 普通态专事加工、变体态走远程加工，它们只是"撞上就消失"，不兼职武器。
		List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, hitBox, e -> e.isAlive());
		if (!entities.isEmpty()) {
			LivingEntity target = entities.get(0);
			if (getWaveType().dealsDamage()
				&& (!(target instanceof Player player) || !player.isCreative())) {
				target.hurt(level().damageSources()
					.indirectMagic(this, null), getDamage());
			}
			// 2026-10-01（用户定稿的充能路径②）：能量波打中<b>穿戴护甲的玩家</b> ⇒ 给穿戴中的
			// 四件护甲充能，额度与"波给物品充能"完全一致（ChargingRecipe.energyForLevel）。
			// 现在所有波型都会撞上实体，所以这条对所有波型都生效 —— 正是用户要的
			// "让能量波去打你自己来充能"。刻意放在创造模式判定之外：创造玩家也照充。
			if (target instanceof Player wearer) {
				ArmorEnergy.chargeWorn(wearer, ChargingRecipe.energyForLevel(this.waveLevel));
				// 魔素命中效果（2026-10-03 需求 coe-ess 批 6）：作者原话是"不同的魔素打中<b>玩家</b>"
				// ⇒ <b>只在这一条"目标就是玩家"的分支里</b>施加，生物那条路一次都不调
				// （生物照旧只吃波级伤害）。而且只有真的赋了魔素的波才有效果：
				// 未设魔素 ⇒ WaveEssenceEffects 第一句返回，机器波/变器攻击波逐字不变。
				// 免疫不在这里判：三种 MobEffect 走原版 addEffect ⇒ MobEffectEvent.Applicable
				// ⇒ 既有免疫判据（如凝能佩免嬗乱）自动生效（本类刻意不认那个效果名，见
				// 关卡 wave-hit-effect-chain 的负向断言）。
				WaveEssenceEffects.applyOnPlayerHit(this, wearer);
			}
			// 命中附加效果要素（可选、默认关闭）：既有链"伤害 ⇒ 护甲充能"之后<b>追加</b>一行，
			// 未设要素时整段跳过（机器波/变器波命中行为逐字不变）。见 applyHitEffect 的说明。
			applyHitEffect(target);
			ChargerWaveFx.burst(level(), position(), trailStyle(), renderColor);
			discard();
			return;
		}

		// 命中掉落物：给能量工具充能 / 普通物品按配方转化（检测范围覆盖移动路径，避免高速跳过）。
		// 模板方法 onItemHit：普通波走充电配方加工；变体波（StellarWaveEntity）覆写为链式远程加工
		AABB sweep = hitBox.expandTowards(movement.x * getSpeedBlocks() / 20d,
			movement.y * getSpeedBlocks() / 20d, movement.z * getSpeedBlocks() / 20d);
		List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, sweep, e -> e.isAlive());
		if (!items.isEmpty()) {
			onItemHit(items);
			return;
		}

		// 命中方块：置物台/工作台充能、避雷针、调级器，其余方块视为撞墙消散
		handleBlockCollisions();
	}

	/**
	 * 命中掉落物模板方法（默认=普通波行为：单个物品按 charging 配方加工，命中即绽放开消散）。
	 * 变体波（星辉波变器产物）覆写本方法执行链式远程加工并管理携带载荷。
	 */
	protected void onItemHit(List<ItemEntity> items) {
		// 加工：只有"允许充能加工"的波型才真的去处理（攻击态是纯攻击；变体态覆写本方法走远程加工，
		// 见 StellarWaveEntity）。加工失败也不影响下面的消散 —— 见下一段。
		if (getWaveType().allowsChargingProcessing()) {
			// 核心加工逻辑已抽取至 ChargerWaveProcessor（配方匹配 → 消耗输入 → 产出结果 / 护甲充能）
			processor.processItemEntity(items.get(0));
		}
		// 消散：<b>撞到掉落物一律绽放并消失</b>（用户 2026-10-01："能量波遇到不会被加工的物品
		// 会直接穿过去，这是不行的"）。
		// 旧写法只有 processItemEntity 返回 true（= 配方匹配成功）才消散，于是：
		//   · 不匹配任何 charging 配方的掉落物 ⇒ 波直接穿过（用户报的这条）；
		//   · 攻击态波 ⇒ 提前 return，连碰都不碰。
		// 现在改为"物理碰撞"语义：波是实体，撞到东西就该没。
		ChargerWaveFx.burst(level(), position(), trailStyle(), renderColor);
		discard();
	}

	/**
	 * 方块碰撞检测：遍历碰撞盒覆盖的方块，按优先级处理
	 * 避雷针 → 能量调级器 → 置物台/工作台（有物品槽）→ 撞墙。
	 * 命中即 {@code burst + discard}；调级器穿过/遣返则推出方块外继续飞行。
	 *
	 * <p><b>两阶段判定</b>（主世界优先，结构补充，互不劫持）：</p>
	 * <ol>
	 *   <li><b>主世界判定</b>：遍历波碰撞盒覆盖的主世界方块（机器/地形），命中即处理；
	 *       各机器的单次命中判定委托给登记表里的第二层处理器
	 *       （{@link WaveMachineHandlers} ← {@link WaveMachineHandler}）；</li>
	 *   <li><b>动态结构判定</b>：主世界无命中（波在空旷处或结构区域——结构方块已从主世界
	 *       搬入 Sable 虚拟子世界 / contraption 数据内，主世界读不到）时，依次尝试
	 *       contraption（{@link WaveContraptionCollisions}）与 Sable sub-level
	 *       （{@link WaveSubLevelCollisions}）。</li>
	 * </ol>
	 */
	/** 方块命中解析：分支链已迁到 {@link com.hjmmd_8.createoreexpansion.content.charger.wave.WaveHitResolver}。 */
	private void handleBlockCollisions() {
		WaveHitResolver.resolve(this, contraptionCollisions, subLevelCollisions);
	}

	/**
	 * 命中带物品槽方块（置物台/工作台/工作盆…）的钩子。
	 *
	 * <p>默认（普通波）：把槽内物品交给 {@link ChargerWaveProcessor#processBlockHandler}
	 * 按 charging 配方加工——无论是否匹配成功，波都会在此消散（命中即消耗，原行为）。</p>
	 *
	 * <p>变体波（{@code StellarWaveEntity}）覆写：用自己携带的加工机配方类型逐槽
	 * 链式远程加工（产物放回槽位/就近输出），成功则本 tick 结束不消散（链用尽才绽放）。</p>
	 *
	 * @return true = 本 tick 已被处理（调用方结束方块遍历）；false = 无可加工，
	 *         由调用方按"撞墙消散"处理
	 */
	public boolean handleItemInventoryBlock(IItemHandler handler, BlockPos pos) {
		// 充能加工是"普通态专属"：攻击态不加工（返回 false → 调用方按撞墙处理，波在方块处消散）
		if (!getWaveType().allowsChargingProcessing())
			return false;
		processor.processBlockHandler(handler, pos);
		return false; // 普通波：无论匹配与否都按撞墙消散（调用方处理特效）
	}

	/**
	 * 撞到<b>不带物品槽的普通方块</b>（地形 / 机器外壳等，即"撞墙"）时的钩子。
	 *
	 * <p>默认空实现。变体波（{@code StellarWaveEntity}）覆写它做<b>引雷</b>：
	 * 波携带避雷针引雷次数时，撞到哪里就在哪里落一道雷——这样"闪电方块转化"
	 * （{@code createoreexpansion:lightning_block}）也能被波远程执行
	 * （否则只有带物品槽的方块会走 {@link #handleItemInventoryBlock}，普通方块永远吃不到雷）。</p>
	 *
	 * <p><b>2026-10-02 批 4 新增：环绕波 + "会挖方块的锚点"</b>（作者裁定 D9 = A；
	 * 需求 §3.6 第 4 条"环绕波撞方块 ⇒ 把它挖掘并使其消失"）。本方法在既有引雷之外，
	 * 追加一次<b>可选</b>的回调：环绕波的锚点若实现了
	 * {@link OrbitAnchor#orbitMineBlock(net.minecraft.core.BlockPos)}
	 * （回旋镖就是；波的默认实现返回 {@code false}、什么都不做），就在这里把它挖掉。</p>
	 * <ul>
	 *   <li><b>未设环绕要素</b>（{@code orbitAnchorUuid == null}）⇒ <b>第一句就返回</b>，
	 *       机器波 / 变器波 / 一切既有波一个字节都不变；</li>
	 *   <li><b>环着一枚波</b>（星芒嬗震）⇒ 走 {@link OrbitAnchor} 的默认实现（不挖），
	 *       行为与改造前逐字相同；</li>
	 *   <li><b>波照旧消散</b>：本回调不改变调用方（{@code WaveHitResolver} 的"撞墙"分支）
	 *       紧随其后的 {@code burst + discard} ⇒ "环绕波撞方块 ⇒ 挖掉 + 该枚消失"。</li>
	 * </ul>
	 *
	 * <p>调用时机：波即将因撞墙而绽放消散之前，只在此处调用一次。</p>
	 */
	public void onSolidBlockHit(BlockPos pos) {
		// 环绕波要素未设 ⇒ 与改造前逐字相同（第一道闸；见 applyOrbitElement 的同形守卫）。
		if (this.orbitAnchorUuid == null || pos == null) {
			return;
		}
		if (!(level() instanceof ServerLevel server)) {
			return;
		}
		Entity anchor = server.getEntity(this.orbitAnchorUuid);
		if (anchor instanceof OrbitAnchor orbitAnchor) {
			orbitAnchor.orbitMineBlock(pos);
		}
	}

	/**
	 * 波波碰撞：两个能量波相遇时相互湮灭，在相遇点触发范围能量爆炸。
	 *
	 * <p>爆炸特性：</p>
	 * <ul>
	 *   <li><b>爆炸等级</b> = 两个波等级的较小值（min）；</li>
	 *   <li><b>爆炸范围</b> = 以碰撞点为中心、半径 = 爆炸等级的水平正方形区域
	 *       （半径 1 → 3×3，半径 2 → 5×5，半径 3 → 7×7），不破坏地形；</li>
	 *   <li><b>区域内生物</b>：受到该等级波撞击生物的等量伤害（α 4 / β 6 / γ 8）；</li>
	 *   <li><b>区域内掉落物 / 置物台物品</b>：按爆炸等级直接执行充能加工（复用
	 *       {@link ChargerWaveProcessor}，含能量工具充能与普通物品配方转化）；</li>
	 *   <li><b>粒子</b>：比撞墙绽放（30 个）更密集的爆炸扩散粒子。</li>
	 * </ul>
	 *
	 * @param other 碰撞的另一个波
	 */
	private void handleWaveCollision(AbstractChargerWaveEntity other) {
		// ================== 同批豁免（用户 2026-10-02 星界轮，需求 §3.3(f)） ==================
		// 两侧触发点都用得着这一条，理由见下面"两侧触发点"那段注释：
		//   ① 主动侧：本波在自己的 tick 里查到命中盒内还有一只波（tick() 里的 waves.get(0)）并调用本方法；
		//   ② 被动侧：本波自己的 tick 也跑了同一段查询，于是它同样会调用本方法（由 collided 标记防重）。
		// 判据放在**本方法最开头**：只要两波同批 ⇒ 双方都直接 return ——
		// 不触发 triggerBoom、不记 waveDiag、不置 collided、不 discard。于是无论"谁先跑到"，
		// 结果都是"谁都不爆"（这正是"双向"的含义：豁免不是靠某一侧的特判，而是两波共用的同一个入口）。
		// ⚠ 豁免范围**只有**同批次：批次号 0（机器波、别人的波）不参与，见 sameFiringBatch 的说明。
		if (sameFiringBatch(this, other)) {
			// 诊断日志：豁免不是"没撞上"，而是"撞上了但按同批跳过"——出事时这一行能直接区分两者。
			// 节流：并排飞的多枚波彼此一直在命中盒里，若每 tick 打一行会把事件流日志刷爆。
			if (tickCount % 20 == 0) {
				WaveDiag.trace("波波碰撞豁免（同一次发射，批次 {}）：{} 级 × {} 级 相遇但互不爆炸、互不湮灭",
					getFiringBatch(), WaveLevels.glyph(waveLevel), WaveLevels.glyph(other.waveLevel));
			}
			return;
		}
		// =====================================================================================

		int boomLevel = Math.min(waveLevel, other.waveLevel);
		// 碰撞点取两波中心中点
		Vec3 center = position().add(other.position()).scale(0.5);

		// 1. 范围爆炸：粒子 + 音效 + 区域效果
		ChargerWaveFx.triggerBoom(level(), this, center, trailStyle(), renderColor,
			other.renderColor, boomLevel);

		// 轨迹日志（事件流：一次碰撞一行）：用户口径是"任意两列波（不管波级）撞上就必须有影响"，
		// 这一行把"到底撞没撞上、按哪一级结算"写进日志——出事时能直接分辨"没撞上"与"撞上了没效果"。
		// 粒子数走 ChargerWaveFx.boomParticleCount（唯一算式），日志里的数与真实发出的数必然一致。
		WaveDiag.trace(
			"波波碰撞：{} 级 × {} 级（波型 {} × {}）→ 爆炸等级 {}：半径 {} 格范围伤害 {}、范围内掉落物/置物台按该级加工、粒子 {} 颗、不破坏地形",
			WaveLevels.glyph(waveLevel), WaveLevels.glyph(other.waveLevel), getWaveType()
				.id()
				.getPath(),
			other.getWaveType()
				.id()
				.getPath(),
			WaveLevels.glyph(boomLevel), boomLevel, (int) WaveLevels.damage(boomLevel),
			ChargerWaveFx.boomParticleCount(boomLevel));

		// 2. 两波相互湮灭（标记防对方同 tick 重复触发）
		this.collided = true;
		other.collided = true;
		other.discard();
		discard();
	}

	@Override
	public void remove(RemovalReason reason) {
		// 客户端在实体消散时补充球面均匀扩散绽放
		if (reason == RemovalReason.DISCARDED && level().isClientSide) {
			ChargerWaveFx.burstParticles(level(), position(), trailStyle(), renderColor);
		}
		// 环绕波的"收尾簇"（作者 2026-10-02 要求的三个时刻之一：出生 / 命中 / 收尾）。
		// 为什么放在这里而不是放在每一条 discard 之前：环绕波是<b>被主波牵着走</b>的，
		// 它的消散路径有多条（父波没了、命中目标、寿命与行程上限），散在各处写就必然漏；
		// remove() 是所有路径的唯一汇合点，于是"它存在过"这件事在任何路径下都留下同一簇粒子。
		if (isOrbiting() && level() instanceof ServerLevel orbitServer) {
			if (tickCount == 1) {
				// 出生即收尾（第一 tick 就查不到父波）：这是最容易被误判成"根本没生成"的形态，
				// 所以额外在收尾点炸一簇带环面留痕的出生簇，屏幕上也能看见"它冒了一下"。
				ChargerWaveFx.burstOrbitSpawn(orbitServer, position(), renderColor, null, null, orbitRadius,
					orbitCurrentPhase);
			} else {
				ChargerWaveFx.burst(orbitServer, position(), trailStyle(), renderColor);
			}
			// 收尾诊断行（与出生/心跳同一条通道，见 logOrbitDiag）：作者只靠日志判
			// "生成了没有、是不是立刻没了、为什么没的"。三种原因一眼可分：
			//   · 父波消散 ⇒ 主波消散带走了它（正常收尾，唯一实现）；
			//   · 出生即收尾 ⇒ 它生成了却在第一 tick 就没了（最容易被误判成"没生成"）；
			//   · 自身消散 ⇒ 命中目标，或撞上寿命/行程上限。
			String why = orbitAnchorLost
				? "父波消散（主波消散 ⇒ 环绕波一起收尾，唯一实现）"
				: (tickCount == 1
					? "出生即收尾（第一 tick 就取不到父波）"
					: "自身消散（命中 / 寿命 / 行程上限）");
			WaveDiag.trace("环绕波消散：{}；父波 UUID {}，批次 {}，波级 {}，半径 {} 格，已存活 {} tick",
				why, orbitAnchorUuid, getFiringBatch(), WaveLevels.glyph(waveLevel),
				fmt2(orbitRadius), tickCount);
		}
		super.remove(reason);
	}

	/** 速度下限（格/秒）：减速不能低于此值。 */
	protected static final double MIN_SPEED = 0.5d;
	// 注意：这里**没有**速度上限常量——上限由等级速度表给（WaveLevels.maxSpeed：1~3 级 10、4/5 级 12）。
	// 2026-09-14 删除了一处遗留的 `MAX_SPEED = 10.0d`：它全仓无人使用，而取值与速度表矛盾（表里 4/5 级是 12），
	// 留着只会让读者以为加速被夹在 10。见 getSpeedBlocks() 的夹取表达式。

	/**
	 * 移动速度（格/秒）：等级基础速度（查 {@link WaveLevels#baseSpeed}，α 2 / β 4 /
	 * γ 6 / ε 7 / ω 8）+ 速度修正值（波速调节器叠加），
	 * 夹在 {@link #MIN_SPEED} ~ {@link WaveLevels#maxSpeed}（4/5 级 12，1~3 级 10）之间。
	 * 子类可覆写基础速度（按等级分档）。
	 */
	protected double getSpeedBlocks() {
		double base = WaveLevels.baseSpeed(waveLevel);
		return Math.max(MIN_SPEED, Math.min(WaveLevels.maxSpeed(waveLevel), base + speedOffset));
	}

	/** 施加一次速度修正（叠加语义：在此值上增加 amount，可正可负）。 */
	public void addSpeedOffset(double amount) {
		this.speedOffset += amount;
		// 同步到客户端（Jade 速度显示）
		this.entityData.set(SPEED_OFFSET, (float) this.speedOffset);
	}

	// ========== 能量场（FieldedEntity） ==========

	/** 赋予/清除电荷（能量着电器调用；null 清除）。写入同步数据（Jade 客户端显示用）。 */
	public void setCharge(com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity charge) {
		this.charge = charge;
		this.entityData.set(CHARGE, charge == null ? 0
			: charge == com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity.POSITIVE ? 1 : 2);
	}

	/** 当前电荷（可能为 null = 不带电）。优先读同步数据（客户端实例）；服务端兜底用字段。 */
	@Override
	public com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity getChargePolarity() {
		int code = this.entityData.get(CHARGE);
		if (code == 1)
			return com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity.POSITIVE;
		if (code == 2)
			return com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity.NEGATIVE;
		return charge; // 服务端尚未广播前兜底
	}

	/** 维度 key（供能量场注册表匹配）。 */
	@Override
	public String fieldLevelKey() {
		return level().dimension()
			.location()
			.toString();
	}

	/** 当前位置（世界坐标）。 */
	@Override
	public Vec3 fieldPosition() {
		return position();
	}

	/** 当前运动速度向量（格/秒）= 名义主方向 × 速率。 */
	@Override
	public Vec3 fieldVelocity() {
		return movement.scale(getSpeedBlocks());
	}

	/** 写入修正后的速度向量（格/秒）：主方向跟随真实速度方向，速率变化并入 speedOffset（可被场加速/减速）。 */
	@Override
	public void setFieldVelocity(Vec3 velocity) {
		double len = velocity.length();
		if (len < 1.0E-4) {
			movement = Vec3.ZERO;
			return;
		}
		Vec3 old = movement.scale(getSpeedBlocks());
		double oldLen = old.length();
		// 速率差并入叠加修正（加速场正向累积、负电荷/反向减速）
		if (Double.isFinite(len - oldLen) && Math.abs(len - oldLen) > 1.0E-6)
			speedOffset += len - oldLen;
		movement = velocity.scale(1.0 / len);
	}

	// ========== 公开只读访问（供 Jade 等外部显示） ==========
	// 注意：Jade 在客户端运行，读到的是客户端实体实例——普通字段（waveLevel/speedOffset）
	// 不会同步，必须从 SynchedEntityData（WAVE_LEVEL/SPEED_OFFSET）读取。

	/** 波等级（1=α、2=β、3=γ）。 */
	public int getWaveLevel() {
		return this.entityData.get(WAVE_LEVEL);
	}

	/** 实际运行速度（格/秒，含波速调节器修正）。 */
	public double getWaveSpeed() {
		int level = getWaveLevel();
		double base = WaveLevels.baseSpeed(level);
		return Math.max(MIN_SPEED, Math.min(WaveLevels.maxSpeed(level), base + this.entityData.get(SPEED_OFFSET)));
	}

	/** 剩余寿命（tick）：距自动消散还剩多少 tick（负数/0 = 即将消散）。 */
	public int getRemainingLifetime() {
		return MAX_LIFETIME_TICKS - tickCount;
	}

	/** 当前渲染颜色（RGB 0-1，随波等级/种类）。
	 * 客户端实例的 {@code renderColor} 字段不同步，这里按等级取静态色（Jade 显示用）。 */
	public Vec3 getWaveRenderColor() {
		return getWaveColorForLevel(getWaveLevel());
	}

	/**
	 * 设置能量波等级并同步核心加工逻辑（等级决定可匹配配方上限、伤害、波速）。
	 * 调级器升级/降级、以及增强延迟到期升级时调用。
	 */
	public void setWaveLevel(int level) {
		this.waveLevel = level;
		this.processor = new ChargerWaveProcessor(level(), level);
		// 同步到客户端（Jade 等级显示）
		this.entityData.set(WAVE_LEVEL, level);
	}

	// ========== 受控访问：主运动方向 / 出生点 / 渲染色 / 速度修正 ==========
	// 这几个字段原本靠"同包 protected 直连"，供 charger/wave/ 下的助手类
	// （WaveHitResolver / WaveContraptionCollisions / WaveSubLevelCollisions）与
	// 第二层的机器处理器（经 WaveMachineHandler 契约）读写；搬包后跨包直连非法，
	// 故一律改走下面的访问器。
	//
	// 波等级：助手类继续用既有的 {@link #getWaveLevel()}（读 SynchedEntityData 的 WAVE_LEVEL）。
	// 服务端上它与上面的 {@code waveLevel} 字段恒等——构造、{@link #setWaveLevel(int)}、读档
	// 三处都是"字段与同步数据一起写"，而助手类只在服务端运行，故无需再加一个字段读取口。

	/** 主运动方向（单位向量）。速率另由 {@link #getSpeedBlocks()} 与速度修正决定。 */
	public Vec3 getMovement() {
		return movement;
	}

	/** 覆写主运动方向（只改方向，不动速度修正）。 */
	public void setMovement(Vec3 movement) {
		this.movement = movement;
	}

	/** 出生点（最大行程判定用；读档前为 null）。 */
	public Vec3 getSpawnPos() {
		return spawnPos;
	}

	/** 设置出生点（子波按 contraption / sub-level 坐标换算后回写）。 */
	public void setSpawnPos(Vec3 spawnPos) {
		this.spawnPos = spawnPos;
	}

	/**
	 * 当前渲染色（服务端实例上的<b>实时值</b>，随等级/种类并逐 tick 向目标色插值）。
	 *
	 * <p>注意与 {@link #getWaveRenderColor()} 的区别：后者按<b>等级取静态色</b>，
	 * 专供 Jade 在客户端显示（客户端实例的 {@code renderColor} 字段不同步）。</p>
	 */
	public Vec3 getRenderColor() {
		return renderColor;
	}

	/** 覆写渲染色（子波继承母波颜色用）。 */
	public void setRenderColor(Vec3 renderColor) {
		this.renderColor = renderColor;
	}

	/**
	 * 速度修正叠加值（格/秒，可正可负）。<b>写入请走 {@link #addSpeedOffset(double)}</b>——
	 * 它会同时同步客户端显示用的 {@code SPEED_OFFSET}。
	 */
	public double getSpeedOffset() {
		return speedOffset;
	}

	/** 加速场剩余作用距离（格；>0 表示本 tick 处于加速场中）。 */
	public float getBoostRemaining() {
		return boostRemaining;
	}

	/** 覆写加速场剩余作用距离（波闸/能量场写入）。 */
	public void setBoostRemaining(float boostRemaining) {
		this.boostRemaining = boostRemaining;
	}

	/** 加速场档位（每 tick 提速的等级）。 */
	public int getBoostStep() {
		return boostStep;
	}

	/** 覆写加速场档位。 */
	public void setBoostStep(int boostStep) {
		this.boostStep = boostStep;
	}
	/** 命中伤害：α 4、β 6、γ 8、ε 10、ω 12 —— 查 {@link WaveLevels#damage}。 */
	protected float getDamage() {
		return WaveLevels.damage(waveLevel);
	}

	/** 能量波颜色（RGB 0-1），随波等级统一：1 α=黄、2 β=绿、3 γ=蓝、4 ε=紫粉、5 ω=玫红 */
	protected Vec3 getWaveColor() {
		return getWaveColorForLevel(waveLevel);
	}

	/** 指定等级的波颜色（供调级器渐变提前取下一等级色用）——颜色由等级决定，与机型无关 */
	protected Vec3 getWaveColorForLevel(int level) {
		return switch (level) {
			case 2 -> new Vec3(0, 1, 0); // β：绿
			case 3 -> new Vec3(0, 0.5f, 1); // γ：蓝
			case 4 -> new Vec3(1f, 0.35f, 0.85f); // ε：紫粉
			case 5 -> new Vec3(1f, 0.25f, 0.45f); // ω：玫红（主体红，偏粉紫）
			default -> new Vec3(1, 1, 0); // α/其它：黄
		};
	}

	/** 是否 ω（5 级）波：粒子拖尾用金色。 */
	public boolean isOmega() {
		return waveLevel >= 5;
	}

	/** ω 拖尾金色（RGB 0-1）。 */
	public static final Vec3 OMEGA_GOLD = new Vec3(1f, 0.85f, 0.2f);

	/**
	 * 飞行拖尾粒子颜色（RGB 0-1）：ω（5 级）波用金色，其余等级用当前渲染色。
	 */
	protected Vec3 getTrailParticleColor() {
		return isOmega() ? OMEGA_GOLD : renderColor;
	}

	/**
	 * 创建降一级的"子波"（供差器均摊分发）。子类实现为各自能量波类型的新实例。
	 * <b>方向为任意向量</b>（斜口出口沿 45° 对角飞行，轴向出口传轴向单位向量即可）。
	 *
	 * @param pos   出生位置（已外推到差器外）
	 * @param dir   发射方向（出口开口朝向，单位向量，可为任意方向）
	 * @param level 子波等级（= 原波等级 - 降级量）
	 * @param index 本子波在<b>本次分裂</b>中的序号（0 起）
	 * @param total 本次分裂的子波总数（≥1）
	 * @return 新波实体；null 则放弃发射
	 *
	 * <p><b>为什么要传 index/total（2026-09 修复载荷复制）</b>：分裂 = "母波 discard + 每个开口一个子波"，
	 * 而母波携带的<b>载荷（物品/流体/电量）与链式次数属于物质</b>——若每个子波都整份继承，
	 * 2~3 个开口就等于把同一批物品复制 2~3 份（消散时各自归还容器，净赚 ✗）。
	 * 差器既有语义是"能量<b>均摊</b>分发到子波"，所以载荷也必须按份均摊
	 * （见 {@code StellarWaveEntity#createChildWave}）。</p>
	 */
	public abstract AbstractChargerWaveEntity createChildWave(Vec3 pos, Vec3 dir, int level, int index,
		int total);

	/**
	 * <b>分裂前的"物质已分发"钩子</b>：所有差器分裂路径在 {@code discard()} 母波之前调用一次。
	 *
	 * <p>为什么需要：母波被 discard 时会走 {@code remove(DISCARDED)} —— 变体波在那里会把剩余载荷
	 * <b>释放回容器</b>（见 {@code StellarWaveEntity#releasePayload}）。但分裂场景里母波的载荷已经
	 * <b>按份分给了子波</b>，若再释放一次就是"凭空多出一份"（2026-09 修复的载荷复制）。</p>
	 *
	 * <p>默认空实现（普通能量波不带载荷）；{@code StellarWaveEntity} 覆写为"标记载荷已处置"。</p>
	 */
	protected void onPayloadDistributedToChildren() {
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		waveLevel = tag.getInt("WaveLevel");
		movement = new Vec3(tag.getDouble("MoveX"), tag.getDouble("MoveY"), tag.getDouble("MoveZ"));
		boostRemaining = tag.getFloat("BoostRemaining");
		boostStep = Mth.clamp(tag.getInt("BoostStep"), 1, WaveLevels.MAX_LEVEL);
		speedOffset = tag.getDouble("SpeedOffset");
		if (tag.contains("SpawnX"))
			spawnPos = new Vec3(tag.getDouble("SpawnX"), tag.getDouble("SpawnY"), tag.getDouble("SpawnZ"));
		// 服务端从 NBT 恢复等级后，同步核心加工逻辑的等级（决定可匹配配方上限）
		processor = new ChargerWaveProcessor(level(), waveLevel);
		// 电荷（0=无 1=正 2=负）恢复并写回同步数据
		int chargeCode = tag.getInt("Charge");
		charge = chargeCode == 1
			? com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity.POSITIVE
			: chargeCode == 2
				? com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity.NEGATIVE
				: null;
		// 同步数据写回（供客户端 Jade 显示）
		this.entityData.set(WAVE_LEVEL, waveLevel);
		this.entityData.set(SPEED_OFFSET, (float) speedOffset);
		this.entityData.set(CHARGE, chargeCode);
		// 波状态：老存档没有该键 → 保持构造时的状态（变体波的构造/覆写会保证它是变体态）
		if (tag.contains("WaveType"))
			waveType = WaveTypes.byIdString(tag.getString("WaveType"));
		this.entityData.set(WAVE_TYPE, waveType.id().toString());
		// 魔素（攻击波专有、纯视觉；2026-10-03 需求 coe-ess 批 3）：<b>可缺省</b>——
		// 老存档没有该键 ⇒ 保持 null（= 未设）⇒ trailStyle() 回落"继承波型风格"，
		// 观感与改造前逐字相同。名字认不出（被改过 / 未来删过的值）同样当作"没有"，
		// 不抛异常（见 essenceByName）。同步值一起写回，客户端渲染立刻拿到同一个魔素。
		this.essence = tag.contains("Essence") ? essenceByName(tag.getString("Essence")) : null;
		this.entityData.set(ESSENCE, this.essence == null ? "" : this.essence.name());
		// 发射批次（0 = 无批次）：老存档没有该键 ⇒ 0，行为与改造前逐字一致（任何波都能与它湮灭）
		this.entityData.set(FIRING_BATCH, tag.getInt("FiringBatch"));
		// 通用可选要素（默认关闭）：老存档 / 未设要素的波没有这些键 ⇒ 保持字段默认值
		// （null / 0 ⇒ 两条分支都不进，行为与改造前逐字相同）。
		// ⚠ 波实体类型是 EntityType.Builder#noSave() ⇒ 实际不会被写进区块存档；这段与
		// FIRING_BATCH 一样是"形状保底"，将来若去掉 noSave 也不会静默丢要素。
		if (tag.hasUUID("OrbitAnchor")) {
			orbitAnchorUuid = tag.getUUID("OrbitAnchor");
			orbitRadius = tag.getDouble("OrbitRadius");
			orbitAngularSpeed = tag.getDouble("OrbitAngularSpeed");
			orbitPhase = tag.getDouble("OrbitPhase");
		}
		if (tag.contains("HitEffect")) {
			ResourceLocation effectId = ResourceLocation.tryParse(tag.getString("HitEffect"));
			hitEffect = effectId == null
				? null
				: BuiltInRegistries.MOB_EFFECT.getHolder(effectId).orElse(null);
			hitEffectDuration = tag.getInt("HitEffectDuration");
			hitEffectAmplifier = tag.getInt("HitEffectAmplifier");
		}
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("WaveLevel", waveLevel);
		tag.putDouble("MoveX", movement.x);
		tag.putDouble("MoveY", movement.y);
		tag.putDouble("MoveZ", movement.z);
		tag.putFloat("BoostRemaining", boostRemaining);
		tag.putInt("BoostStep", boostStep);
		tag.putDouble("SpeedOffset", speedOffset);
		tag.putString("WaveType", waveType.id().toString());
		tag.putInt("FiringBatch", getFiringBatch());
		// 魔素（攻击波专有、纯视觉；2026-10-03 需求 coe-ess 批 3）：<b>只在真的设了的时候才写键</b>
		// ⇒ 没设魔素的波（= 一切既有波），存档内容与改造前逐字相同（"默认行为一个字不变"
		// 包括 NBT 形状）；读到没有该键 ⇒ null ⇒ 回落波型风格。
		if (essence != null) {
			tag.putString("Essence", essence.name());
		}
		// 通用可选要素（默认关闭）：<b>只在真的设了的时候才写键</b> ⇒ 没设要素的波，
		// 存档内容与改造前逐字相同（"默认行为一个字不变"包括 NBT 形状）。
		if (orbitAnchorUuid != null) {
			tag.putUUID("OrbitAnchor", orbitAnchorUuid);
			tag.putDouble("OrbitRadius", orbitRadius);
			tag.putDouble("OrbitAngularSpeed", orbitAngularSpeed);
			tag.putDouble("OrbitPhase", orbitPhase);
		}
		if (hitEffect != null) {
			ResourceLocation hitEffectId = BuiltInRegistries.MOB_EFFECT.getKey(hitEffect.value());
			if (hitEffectId != null) {
				tag.putString("HitEffect", hitEffectId.toString());
				tag.putInt("HitEffectDuration", hitEffectDuration);
				tag.putInt("HitEffectAmplifier", hitEffectAmplifier);
			}
		}
		tag.putInt("Charge", charge == null ? 0
			: charge == com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity.POSITIVE ? 1 : 2);
		if (spawnPos != null) {
			tag.putDouble("SpawnX", spawnPos.x);
			tag.putDouble("SpawnY", spawnPos.y);
			tag.putDouble("SpawnZ", spawnPos.z);
		}
	}
}