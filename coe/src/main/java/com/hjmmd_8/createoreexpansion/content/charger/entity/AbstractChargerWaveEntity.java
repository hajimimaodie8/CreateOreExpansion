package com.hjmmd_8.createoreexpansion.content.charger.entity;


import com.hjmmd_8.createoreexpansion.common.CoeCore;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveDiag;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveEssenceEffects;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveHitResolver;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveContraptionCollisions;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveSubLevelCollisions;
import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipe;
import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.energy.ArmorEnergy;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveType;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTypes;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTrailStyle;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveEssence;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveEssenceRegistry;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveColorRule;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveSpeed;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveMachineIntegrationPoints;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveAttrKey;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveAttrs;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveState;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveStateNbt;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveStateSync;

import net.createmod.catnip.levelWrappers.SchematicLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
 * <p><b>本类的形状（2026-10-06 行为零变化拆分）</b>：本类保留<b>公开/受保护形状与全部公开实现</b>
 * ——{@code tick()} 的每 tick 阶段序列与命中链、{@code remove()} 的三条收尾、NBT 两侧、
 * {@code FieldedEntity} 六个覆写、四十余条访问器（{@code WaveAccess} 门面的取值面）一个字未动；
 * 按职责域拆出的同包类只做搬运（宿主仍是这些状态的<b>唯一持有者</b>，只把被读写的字段由
 * {@code private} 放宽到包级私有）：</p>
 * <ul>
 *   <li>{@link WaveOrbitElement} —— 环绕波要素（位置改写 + 出生/心跳诊断 + 环面几何粒子）；</li>
 *   <li>{@link WaveCollision} —— 波波碰撞（同批豁免 → 范围爆炸 → 相互湮灭）。</li>
 * </ul>
 * <p><b>刻意没搬的两处</b>（"按职责域拆、不按行数硬拆"）：命中附加效果要素（{@code applyHitEffect}
 * 只有 6 行实现，它的 3 个字段与那道闸门仍与宿主共处一地）与能量场接口（{@code FieldedEntity}
 * 的六个覆写就是本类对外的运行期契约，搬出去只会多六条转发壳）。</p>
 * <p>构造期一个字未动：两个构造器只碰参数与原始类型字段，没有调用任何被搬出去的 helper
 * （见 {@code setPos} 覆写的构造期约束说明）。</p>
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
 * （anchorWaveUuid / orbitRadius / orbitAngularSpeed / orbitPhase，见 {@link WaveOrbitElement#tick}）
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

	/**
	 * 最大飞行距离（格）：超过即消散，与寿命上限互为兜底。
	 *
	 * <p>它现在只是<b>缺省值</b>（{@link #maxTravelDistance} 的初值）：2026-10-24 批 6 起每枚波可以
	 * 有自己的行程上限（{@link #setMaxTravelDistance}），不设时逐字等于本常量 ⇒ 一切既有波的
	 * 消散时机一个 tick 都不变。</p>
	 */
	protected static final double MAX_TRAVEL_DISTANCE = 64.0;

	/**
	 * <b>本波自己的最大飞行距离（格）</b>（2026-10-24 批 6）：默认 = {@link #MAX_TRAVEL_DISTANCE}。
	 *
	 * <p>缺省值的语义就是旧行为：{@code tick()} 里那一句判定读它，字段初值是 64.0 ⇒
	 * "没设"与改造前逐字相同。刻意是原始类型（不依赖构造期初始化顺序，判定只在 tick 里跑）。</p>
	 */
	private double maxTravelDistance = MAX_TRAVEL_DISTANCE;

	/**
	 * <b>本波的全部属性值</b>（2026-10-24 波系统重构批 3，方案 §3.2 的"单一真源"）。
	 *
	 * <p>等级 / 方向 / 速度修正 / 电荷 / 出生点 / 波型 / 魔素 / 主人 / 环绕四件套 / 命中效果三件套 /
	 * 重力，<b>全部</b>住在这里；本类只保留位置、寿命计时、tick 阶段、命中链以及必要的运行态缓存。
	 * NBT 不再有手写键名，只有 {@link com.hjmmd_8.createoreexpansion.content.wave.api.WaveStateNbt}
	 * 对 {@code WaveAttrs} 表的遍历。</p>
	 */
	private final WaveState state = new WaveState();

	/** 7 个同步视图的句柄（accessor 的<b>声明</b>仍在下面，顺序/序列化器/缺省值一个字未动）。 */
	private final WaveStateSync.Views syncedViews = new WaveStateSync.Views(WAVE_LEVEL, SPEED_OFFSET, CHARGE,
		WAVE_TYPE, FIRING_BATCH, ESSENCE, OWNER);

	/** 已与另一波碰撞（防同 tick 双方各触发一次爆炸）。 */
	boolean collided;

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
	 *
	 * <p>2026-10-24 批 3：字段本体搬进 {@link WaveState}（键 {@code BoostRemaining}），本类只留
	 * 读写口 {@link #getBoostRemaining()} / {@link #setBoostRemaining(float)} —— 读档/存档由
	 * {@code WaveAttrs} 表统一驱动。</p>
	 */

	/**
	 * 调级器增强步数（本次延迟升级一次提升几级）：翡翠/蓝宝石恒为 1；
	 * 星辉石调级器按授予时的本机转速可为 2（单次 +2，等级仍封顶于
	 * {@link WaveLevels#MAX_LEVEL}）。由机器侧的调级器处置器（W6-a 起住第二层，
	 * 经 {@link WaveMachineHandler} 契约回调）在授予延迟升级时从调级器 BE 机型参数
	 * （{@code getBoostStepForSpeed()}）写入。
	 *
	 * <p>2026-10-24 批 3：字段本体搬进 {@link WaveState}（键 {@code BoostStep}），本类只留
	 * {@link #getBoostStep()} / {@link #setBoostStep(int)}；缺键读作 1、读侧夹在
	 * {@code 1..maxLevel()} 的旧口径写在 {@code WaveAttrs} 的编解码器里。</p>
	 */

	/**
	 * 当前渲染颜色（RGB 0-1）：每 tick 向目标颜色插值靠近，实现升降级/遣返时的平滑渐变。
	 * 目标色：正常 = 当前等级色；待升级（boostRemaining &gt; 0）时提前变为下一等级色，
	 * 使波穿出调级器后颜色即开始过渡，而非延迟结束瞬间跳变。
	 */
	Vec3 renderColor;

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
		this.state.set(WaveAttrs.LEVEL, waveLevel);
		this.processor = new ChargerWaveProcessor(level, waveLevel);
		this.setMovement(movementDir.normalize());
		this.renderColor = getWaveColor();
		this.setSpawnPos(pos);
		setPos(pos);
		// 服务端/客户端实例都写入同步数据（Jade 等客户端读取需要）。7 个视图由 WaveStateSync 一处推送，
		// 值没变时 SynchedEntityData#set 是空操作 ⇒ 与改造前"只推等级与速度修正"可观察行为相同。
		pushSyncedViews();
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

	/**
	 * <b>主人</b>（= 发射者）的同步 key（2026-10-03 需求 {@code coe-boom2} §3.2，
	 * "不伤发射者"的<b>数据层</b>）：{@code Optional<UUID>}，{@code Optional.empty()} = <b>无主人</b>。
	 *
	 * <p>用原版就有的 {@code EntityDataSerializers.OPTIONAL_UUID}：空值有<b>专门的表示</b>
	 * （既不是 null 也不是空串），与 {@link #ESSENCE} 拿字符串表"未设"是同一个口径 ——
	 * 读不出来就是"没有主人"，不会抛异常。老存档 / 机器波 / 一切未赋主人的波读到的都是
	 * {@code Optional.empty()}。</p>
	 */
	private static final net.minecraft.network.syncher.EntityDataAccessor<Optional<UUID>> OWNER =
		SynchedEntityData.defineId(AbstractChargerWaveEntity.class, net.minecraft.network.syncher.EntityDataSerializers.OPTIONAL_UUID);

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(WAVE_LEVEL, 0);
		builder.define(SPEED_OFFSET, 0f);
		builder.define(CHARGE, 0);
		builder.define(WAVE_TYPE, "");
		builder.define(FIRING_BATCH, 0);
		builder.define(ESSENCE, "");
		builder.define(OWNER, Optional.empty());
	}

	/**
	 * 本实体种类的 NBT 键范围（基类 = {@link WaveAttrKey.Scope#BASE}；{@code charger_wave} /
	 * {@code stellar_wave} 各自追加自己的键）。{@link WaveStateNbt} 按它筛选表里的行。
	 */
	protected WaveAttrKey.Scope waveScope() {
		return WaveAttrKey.Scope.BASE;
	}

	/**
	 * 本波的<b>属性值对象</b>（子类读写自己那一层的键用；它仍是唯一持有者 —— 子类
	 * <b>不许</b>再声明属性字段）。同包助手类读属性一律走既有 getter，不要直接碰它。
	 */
	protected final WaveState waveState() {
		return this.state;
	}

	/** 把 {@link #state} 里的 7 个值推送到同步视图（<b>唯一</b>推送口，见 {@link WaveStateSync}）。 */
	private void pushSyncedViews() {
		WaveStateSync.push(state, this.entityData, syncedViews);
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
		// 2026-10-24 批 3：批次数也住在 WaveState（NBT 键 FiringBatch 由表读写），所以这里必须
		// 先写状态再推同步视图 —— 只写 entityData 会让"运行期设过批次"的波存档时写回 0。
		this.state.set(WaveAttrs.FIRING_BATCH, batch);
		pushSyncedViews();
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
	 * （见 {@link WaveOrbitElement#tick}）；锚点不存在/已消散时本波<b>立刻 discard</b>，
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
	 *
	 * <p><b>2026-10-24 批 3</b>：字段本体搬进 {@link WaveState}（键 {@code OrbitAnchor}，
	 * 环绕四件套的组长）；本类只留 {@link #getOrbitAnchorUuid()} / {@link #setOrbitAnchor}。</p>
	 */

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

	/** 环绕波要素：角速度（<b>弧度/tick</b>，1 圈/秒 = 2π/20）——键 {@code OrbitAngularSpeed}。 */

	/**
	 * 环绕波要素：初始相位（弧度）。{@code 0} = 出生时位于基向量 u 的正方向一侧（默认）。
	 * 刻意用 {@code tickCount} 当时间基（θ = 相位 + 角速度 × 已存活 tick）⇒ 不需要额外字段，
	 * 也不受读档/重载影响。</p>
	 *
	 * <p>2026-10-24 批 3：字段搬进 {@link WaveState}（键 {@code OrbitPhase}）。</p>
	 */

	/**
	 * 本 tick 的<b>当前相位</b>（弧度；只有环绕波有意义）——粒子侧拿它把"环面留痕桩"铺在
	 * 波此刻所在的那一圈上，使"它绕到哪儿了"一眼可读。
	 *
	 * <p>刻意不在粒子侧重算 {@code 相位 + 角速度 × tickCount}：那会让"位置公式"与"粒子公式"
	 * 变成两处真源（改一处忘一处就漂移）。这里只有一个写入点（{@link WaveOrbitElement#tick}）。</p>
	 */
	double orbitCurrentPhase;

	/**
	 * 本 tick 的<b>父波位置</b>（只有环绕波有意义；父波取不到时为 {@code null}）。
	 *
	 * <p>用途有二：粒子侧要拿它算圆周点（位置已改写，父波位置本身没存下来）；
	 * 以及心跳日志的"距父波距离"——这个距离恒等于 {@link WaveAttrs#ORBIT_RADIUS}，正是"半径 0.8 的环绕
	 * 真的在绕"的机器可判证据。</p>
	 */
	@Nullable
	Vec3 orbitAnchorPos;

	/**
	 * 本 tick 用来铺环的<b>环平面法向</b>（只有环绕波有意义）= {@link WaveOrbitElement#tick} 里
	 * <b>实际使用</b>的那个方向（父波的运动方向，取不到才回落到自身方向）。
	 *
	 * <p>为什么不能由粒子侧直接读 {@code movement}：位置改写用的是<b>父波</b>的方向，而能量场可以把
	 * 父波的方向逐 tick 掰弯（{@code setFieldVelocity}）——两者一旦不同，粒子圈的法向就与真实轨道
	 * 错开（画出一个跟轨道不平行的环）。这里让位置改写把"我这一 tick 到底按哪个法向铺的环"记下来，
	 * 粒子侧只读这个值 ⇒ 圈与轨道必然共面。</p>
	 */
	@Nullable
	Vec3 orbitRingNormal;

	/**
	 * <b>命中附加效果要素</b>：命中生物时追加施加的药水效果（{@code null} = <b>不做事</b>，
	 * 即默认关闭）。
	 *
	 * <p>施加走原版 {@link net.minecraft.world.entity.LivingEntity#addEffect} ⇒
	 * NeoForge 的 {@code MobEffectEvent.Applicable} 照常触发，于是既有的一切"效果免疫"
	 * 拦截点（例如星辉石凝能佩免疫嬗乱：{@code MedallionEffectHandler#onEffectApplicable}）
	 * <b>自动生效</b>，本类不写任何一套自己的免疫判据。</p>
	 *
	 * <p>2026-10-24 批 3：三件套搬进 {@link WaveState}（键 {@code HitEffect} 为组长，
	 * {@code HitEffectDuration} / {@code HitEffectAmplifier} 随它成组）。</p>
	 */

	// ==================================================================================
	// ★ 2026-10-05 弓技能批 4（宝石弓「量波置换」）：第三个"通用、可选、默认关闭"的波要素
	//    —— <b>重力要素</b>（作者原话："射出的箭替换为具有同样重力效果、但在水中能够沿直线飞行的
	//    随机魔素攻击能量波"）。
	//
	// ⚠ 2026-10-05 批 10（同日，作者裁定）：<b>弓那一侧的重力被撤回</b>，主波改走直线 ⇒
	//    今天全仓没有任何调用点。要素<b>保留</b>（理由见下面 setGravity 的 javadoc），
	//    于是"机器波 / 一切既有波逐字不变"这条性质继续由同一个默认值保证。
	//
	// 为什么它是"给既有波加一个要素"而不是新实体/新飞行模式：与环绕波要素、命中附加效果要素
	// 同一口径 —— 作者 2026-10-02 的硬口径是"能量波是由好几个要素定义的，不要再凭空造一个新的
	// 能量波"。本要素<b>不设时字段就是 0</b>，每 tick 的位移分支第一句直接返回同一个 Vec3
	// ⇒ <b>机器波 / 变器波 / 差波器子波 / 星芒嬗震的主波与环绕波 / 回旋镖的环绕波，飞行与改造前
	// 逐字相同</b>（关卡 bow4-gravity-gated 守着这条负向）。
	//
	// ⚠ 它<b>只</b>改变"本波自己走出来的那一跳"（tick 里那一句 setPos 的入参），
	// 不动 movement 主方向、不动 getSpeedBlocks()、不动速度修正、不碰能量场修正：
	// 于是"环绕波的环平面法向"（= 锚点的 movement = 发射方向）与一切既有读数都不变。
	// ==================================================================================

	/**
	 * <b>重力要素</b>：本波每 tick 额外叠加的<b>向下加速度</b>（格/秒²）。
	 * {@code <= 0} = <b>未设</b>（字段默认值，即"关闭"）⇒ 本要素一个字节都不生效。
	 *
	 * <p>单位与口径：{@code 格/秒²}；每 tick 的下落速度增量 = {@code 它 / 20}（格/秒），
	 * 每 tick 的下落位移 = 当前下落速度 {@code / 20}（格）。两者都用 {@code ChargeConfigs.TICKS_PER_SECOND}
	 * 这个"一秒 = 多少 tick"的唯一真源，不在本类写 20。</p>
	 *
	 * <p><b>数值本身不住在这里</b>：本类只提供"要素"（开关 + 施加），强度由调用方传
	 * （批 4 时是宝石弓的 {@code BowWaveShiftConfigs.GRAVITY_BLOCKS_PER_SECOND_SQUARED} = 1.35）
	 * —— 与"波级/波速由波情五要素的持有者填、实体只负责解释"是同一个分工。</p>
	 *
	 * <p>⚠ <b>批 10（2026-10-05，作者裁定）起全仓没有任何 {@code .setGravity(} 调用点</b>：
	 * 弓那一侧的重力被撤回，主波改走直线。要素本身连同下面两个访问器<b>刻意保留</b> ——
	 * 默认 0 ⇒ {@link #applyGravityElement} 第一句原样返回同一个 {@code step}，一切既有波
	 * 逐字不变；而"共享波基类的公开/受保护形状只许增长"是既有判定（{@code :cews} 也编译它）。
	 * 将来要把抛物线加回来，改的是发射点一行，不必再动实体与存档形状。</p>
	 *
	 * <p>刻意是原始类型（默认 0.0）：{@link #setPos} 会被父类 {@code Entity} 的构造器调用，
	 * 那一刻字段初始化器还没执行 —— 本字段只被 {@link #tick()} 读，构造期不可达，
	 * <p>2026-10-24 批 3：字段搬进 {@link WaveState}（键 {@code GravityAcceleration}；写侧仍是
	 * "&gt; 0 才写"，读侧仍是"无条件读"，见 {@code WaveAttrs}）。</p>
	 */

	/**
	 * 重力要素的<b>运行态</b>：已攒下的下落速度（格/秒，恒 ≥ 0；向下）。
	 *
	 * <p>每 tick {@code += 加速度 / 20}；位移里按 {@code -它 / 20} 追加到下跳上。</p>
	 *
	 * <p><b>水中走直线</b>（作者原话"在水中能够沿直线飞行"）：波处于水中时本值被<b>清零</b>
	 * 且不再累加 ⇒ 该 tick 的位移回到"沿 movement 的匀速直线"，
	 * 与一切既有波在水中逐字相同（关卡 bow4-water-straight 守着这条：
	 * 水中分支里既不能有加速度累加、也不能有下落位移）。</p>
	 *
	 * <p>刻意<b>不进 NBT</b>：它是"这一次飞行的过程量"，不是配置；波实体类型是 {@code noSave()}
	 * 本来也不落盘。读档后从 0 重新开始累积，与改造前"没有这个要素"同形。</p>
	 */
	private double gravityFallSpeed;

	/**
	 * 设置<b>重力要素</b>（可选；不调用 = 本波不受重力，飞行与一切既有波逐字相同）。
	 *
	 * @param accelerationBlocksPerSecondSquared 向下加速度（格/秒²；{@code <= 0} 视为不做事）
	 */
	public void setGravity(double accelerationBlocksPerSecondSquared) {
		this.state.set(WaveAttrs.GRAVITY, accelerationBlocksPerSecondSquared);
	}

	/** 是否设了重力要素（诊断/关卡用；{@code false} = 飞行与改造前逐字相同）。 */
	public boolean hasGravity() {
		return this.state.get(WaveAttrs.GRAVITY) > 0.0D;
	}

	/**
	 * 设置<b>环绕波要素</b>（一次设全；调用方还必须继承父波批次号，见 {@link WaveAttrs#ORBIT_ANCHOR}）。
	 *
	 * @param anchorWaveUuid  父波 UUID（非 null）
	 * @param radius          环绕半径（格）
	 * @param angularSpeedRad 角速度（弧度/tick；1 圈/秒 = 2π/20）
	 * @param phaseRad        初始相位（弧度）
	 */
	public void setOrbitAnchor(UUID anchorWaveUuid, double radius, double angularSpeedRad, double phaseRad) {
		this.state.set(WaveAttrs.ORBIT_ANCHOR, anchorWaveUuid);
		this.state.set(WaveAttrs.ORBIT_RADIUS, radius);
		this.state.set(WaveAttrs.ORBIT_ANGULAR_SPEED, angularSpeedRad);
		this.state.set(WaveAttrs.ORBIT_PHASE, phaseRad);
	}

	/** 父波 UUID（{@code null} = 本波不环绕 ⇒ 要素关闭）。 */
	@Nullable
	public UUID getOrbitAnchorUuid() {
		return this.state.get(WaveAttrs.ORBIT_ANCHOR);
	}

	/** 是否设了环绕波要素（诊断/关卡用；{@code false} = 行为与改造前逐字相同）。 */
	public boolean isOrbiting() {
		return this.state.get(WaveAttrs.ORBIT_ANCHOR) != null;
	}

	/** 环绕半径（格）。 */
	public double getOrbitRadius() {
		return this.state.get(WaveAttrs.ORBIT_RADIUS);
	}

	/** 环绕角速度（弧度/tick）。 */
	public double getOrbitAngularSpeed() {
		return this.state.get(WaveAttrs.ORBIT_ANGULAR_SPEED);
	}

	/** 环绕初始相位（弧度）。 */
	public double getOrbitPhase() {
		return this.state.get(WaveAttrs.ORBIT_PHASE);
	}

	/**
	 * 设置<b>命中附加效果要素</b>（可选；不调用 = 命中只走既有链）。
	 *
	 * @param effect        要施加的效果（Holder；一般传 {@code DeferredHolder}）
	 * @param durationTicks 持续 tick 数（{@code <= 0} 视为不做事）
	 * @param amplifier     效果等级（0 = I 级）
	 */
	public void setHitEffect(Holder<MobEffect> effect, int durationTicks, int amplifier) {
		this.state.set(WaveAttrs.HIT_EFFECT, effect);
		this.state.set(WaveAttrs.HIT_EFFECT_DURATION, durationTicks);
		this.state.set(WaveAttrs.HIT_EFFECT_AMPLIFIER, amplifier);
	}

	/** 命中附加效果（{@code null} = 要素关闭）。 */
	@Nullable
	public Holder<MobEffect> getHitEffect() {
		return this.state.get(WaveAttrs.HIT_EFFECT);
	}

	/** 命中附加效果持续 tick 数（0 = 要素关闭）。 */
	public int getHitEffectDuration() {
		return this.state.get(WaveAttrs.HIT_EFFECT_DURATION);
	}

	/** 命中附加效果等级（amplifier）。 */
	public int getHitEffectAmplifier() {
		return this.state.get(WaveAttrs.HIT_EFFECT_AMPLIFIER);
	}

	/**
	 * 波当前所属<b>波型</b>：服务端读 {@link WaveState}（方案 §3.2 的唯一持有者），
	 * 客户端按同步 id 现查注册表（拖尾/绽放特效在客户端播，必须能拿到）。
	 *
	 * <p>服务端与同步值同源——只有 {@link #trySetWaveType(WaveType)} 会改动它们，且两者一起写，
	 * 故不存在"服务端与客户端看到不同波型"的窗口（构造与读档同理）。</p>
	 */
	public WaveType getWaveType() {
		if (level() != null && level().isClientSide)
			return WaveTypes.byIdString(this.entityData.get(WAVE_TYPE));
		return this.state.get(WaveAttrs.WAVE_TYPE);
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
		this.state.set(WaveAttrs.WAVE_TYPE, target);
		pushSyncedViews();
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

	// 2026-10-24 批 4：魔素 id 化。状态里存的是 WaveEssence（注册项），内置 11 个的 id 逐字等于
	// 枚举名 ⇒ 存档键 Essence 与同步 STRING 的字节一个都没变；getEssence() 仍是枚举形态
	// （既有 156 处调用零改动），essenceSpec() 才拿得到"完整注册项"（第三方魔素没有枚举值）。

	/**
	 * 本波的<b>魔素注册项</b>（{@code null} = 未设）—— 魔素的<b>完整</b>读取口：id、颜色、可选拖尾档案、
	 * 可选命中行为全在这里。
	 *
	 * <p>服务端读 {@link WaveState}，客户端按同步字符串现查 {@link WaveEssenceRegistry}
	 * （与 {@link #getWaveType()} 同一形状：两者都由 {@link #trySetEssence} 一起写，
	 * 故不存在"服务端与客户端看到不同魔素"的窗口）。</p>
	 *
	 * <p><b>为什么还要 getEssence()</b>：第三方魔素没有枚举值，{@code getEssence()} 对它只能回
	 * {@code null}；所有"魔素是哪一种"的判断（显示行、命中八支表、环绕池）仍走枚举，第三方魔素的
	 * 表现/行为走本方法 + {@link WaveColorRule} + 注册项自带的行为钩子。</p>
	 */
	@Nullable
	public WaveEssence essenceSpec() {
		if (level() != null && level().isClientSide)
			return WaveEssenceRegistry.byIdString(this.entityData.get(ESSENCE));
		return this.state.get(WaveAttrs.ESSENCE);
	}

	/**
	 * 本波的<b>魔素</b>（枚举形态；{@code null} = 未设，或这枚波带的是第三方魔素）。
	 * 客户端读同步值、服务端读 {@link WaveState}（见 {@link #essenceSpec()}）。
	 */
	@Nullable
	public WaveTrailStyle getEssence() {
		WaveEssence essence = essenceSpec();
		return essence == null ? null : essence.trailStyle();
	}

	/**
	 * 设置<b>魔素</b>（枚举形态 = 内置 11 个之一，<b>可随时改</b>——无"一生一次"约束）。
	 *
	 * <p><b>非攻击波被显式设置时忽略</b>（作者裁定：魔素是攻击波专有；§3.2 的"忽略 + 不设"）：
	 * 不写字段、不报错，返回 {@code false}。判据取波型自报的 {@link WaveType#dealsDamage()}
	 * （攻击态 = 会伤害的波），<b>不</b>在这里写死某一个内置波型 —— 扩展模组注册的
	 * "会伤害的波型"因此自动获得魔素能力。</p>
	 *
	 * <p>{@code null} = 清除魔素（回落波型风格/波型色；任何波型都允许清除）。</p>
	 *
	 * <p><b>2026-10-24 批 4</b>：本方法现在只是"枚举 → 注册项"的转发（{@link WaveEssenceRegistry#builtinFor}），
	 * 与 {@link #trySetEssence(WaveEssence)} 共用<b>同一份</b>写入逻辑。</p>
	 *
	 * @return 是否真的写进去了
	 */
	public boolean trySetEssence(@Nullable WaveTrailStyle target) {
		return trySetEssence(target == null ? null : WaveEssenceRegistry.builtinFor(target));
	}

	/**
	 * 设置<b>魔素</b>（注册项形态 —— <b>第三方魔素走这里</b>）：{@code wave.trySetEssence(myEssence)}。
	 *
	 * <p>语义与枚举重载完全相同（可随时改 / 非攻击波忽略 / {@code null} 清除）。
	 * ⚠ 字面量 {@code trySetEssence(null)} 会因为两个重载而歧义（编译期就报错，不会静默）；
	 * 要清除请传一个 {@code (WaveTrailStyle) null}。</p>
	 *
	 * @return 是否真的写进去了
	 */
	public boolean trySetEssence(@Nullable WaveEssence target) {
		if (target == null) {
			this.state.set(WaveAttrs.ESSENCE, null);
			pushSyncedViews();
			return true;
		}
		if (!getWaveType().dealsDamage())
			return false;
		this.state.set(WaveAttrs.ESSENCE, target);
		pushSyncedViews();
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
		// 2026-10-24 批 4：魔素注册项自带的"拖尾档案"可覆盖调用方基线（内置 11 个都不覆盖 ⇒ 走到最后一行）。
		WaveEssence essence = essenceSpec();
		Float override = essence == null ? null : essence.trailScaleOverride();
		if (override != null)
			return override;
		return isOrbiting() ? ChargerWaveFx.ORBIT_TRAIL_SCALE : 0.45f;
	}

	/** 本波拖尾主粒子的<b>调用方基线颗数</b>（见 {@link #trailScale()} 的说明）。 */
	private int trailCount() {
		// 同上：魔素注册项可覆盖基线颗数；内置 11 个都不覆盖 ⇒ 走到最后一行。
		WaveEssence essence = essenceSpec();
		Integer override = essence == null ? null : essence.trailCountOverride();
		if (override != null)
			return override;
		return isOrbiting() ? ChargerWaveFx.ORBIT_TRAIL_COUNT : 6;
	}

	// ==================================================================================
	// 主人（2026-10-03 需求 coe-boom2 批 1）：<b>可缺省的"发射者" UUID</b>
	//
	// 本批只建<b>数据层</b>（字段 + 同步 + NBT 两侧可缺省 + 唯一容错解析入口 + 判据 isOwner）：
	// <b>命中谓词一个字不改</b>（那是批 2，等作者裁定）。
	// 缺省语义 = <b>无主人 = 不排除任何人</b> ⇒ 机器波 / 变器波 / 一切既有波在"没人 setOwner"时
	// 行为与改造前<b>逐字相同</b>，老存档（没有该键）也不会崩。
	// ⚠ 唯一用途 = "不伤发射者"；<b>不许</b>拿它去做归属 / 阵营 / 只打敌人（作者 2026-10-03 明确禁止）。
	// ==================================================================================

	// 2026-10-24 批 3：字段搬进 WaveState（键 Owner）；NBT 读写与"有没有设过"由表驱动。

	/**
	 * <b>本波是否已经给主人充过能</b>（2026-10-03 批 2，我加的一道"一次飞行只充一次"闸门）。
	 *
	 * <p><b>为什么需要它</b>：主人被排除在命中列表之外 ⇒ 波不再"撞上就消散"，而是<b>穿过去继续飞</b>；
	 * 而波每 tick 只走 0.025~0.6 格、命中盒是 1.0 格见方 ⇒ 同一枚波会<b>连续好几 tick</b> 罩住主人。
	 * 若不记账，主人一趟就吃到 3~4 份充能，与旧口径（撞一次 ⇒ 充一次 ⇒ 消散）不等 ——
	 * 那是把"打你自己来充能"悄悄放大成"站进波里刷能量"。</p>
	 *
	 * <p>默认 {@code false}（原始类型字段，不写初值）；<b>刻意不进同步、不进 NBT</b>：
	 * 充能是服务端行为，而波实体类型是 {@code noSave()}（不进区块存档）⇒ 一次飞行一份状态就够。</p>
	 */
	private boolean ownerCharged;

	/**
	 * <b>唯一容错解析入口</b>：把<b>同步值</b> {@code Optional<UUID>} 读成
	 * {@code @Nullable UUID} —— 空同步值（{@code Optional.empty()}）⇒ {@code null}。
	 *
	 * <p><b>为什么这里刻意不解析字符串、全类零处 {@code UUID.fromString}</b>：
	 * {@code UUID.fromString} 是"字符串 ⇒ UUID"的<b>裸解析</b>，遇到空串 / 被改过的值就抛
	 * {@code IllegalArgumentException}；而这条路径跑在实体的 <b>tick 与读档</b>里，一抛就是崩。
	 * 本字段的两种原始表示本来就都是原版自带的容错读法：同步值用
	 * {@code OPTIONAL_UUID}（空值有专门表示），存档用 {@link CompoundTag#hasUUID}。</p>
	 */
	@Nullable
	private static UUID ownerOf(@Nullable Optional<UUID> synced) {
		return synced == null || synced.isEmpty() ? null : synced.get();
	}

	/**
	 * 本波的<b>主人 UUID</b>（{@code null} = 无主人）。服务端读 {@link WaveState}，客户端读
	 * <b>同步值</b>（与 {@link #getWaveType()} / {@link #getEssence()} 同一形状）。
	 */
	@Nullable
	public UUID getOwnerUuid() {
		if (level() != null && level().isClientSide) {
			return ownerOf(this.entityData.get(OWNER));
		}
		return this.state.get(WaveAttrs.OWNER);
	}

	/**
	 * <b>唯一的主人判据</b>：比 UUID（{@code e.getUUID()}）。
	 *
	 * <p>刻意<b>不做</b> {@code server.getEntity(uuid)} 查询：那个查询在"跨维度 / 已卸载 /
	 * 尚未落地"时返回 null，会让"是不是发射者"凭空变成 false；而发射者本人此刻就在命中判定的
	 * 现场，直接拿它的 UUID 比是唯一不会漂移的判据。</p>
	 *
	 * <p><b>本批（批 1）还没有任何调用方</b>：命中谓词一个字没改（见 §3.2 的批 2）。
	 * 缺省（无主人）⇒ 恒 {@code false} ⇒ 不排除任何人。</p>
	 */
	public boolean isOwner(@Nullable Entity e) {
		UUID owner = getOwnerUuid();
		return owner != null && e != null && owner.equals(e.getUUID());
	}

	/**
	 * 记下本波的<b>主人</b>（= 发射者）；{@code null} = 清除（回到"无主人 = 不排除任何人"）。
	 *
	 * <p><b>全仓只有三处会调它</b>：星芒嬗震的主波与环绕波、回旋镖的环绕波。其余一切波的生成点
	 * （三台充能器 / 列车充能器 / 差波器子波 / 两个变体波生成点）<b>一律不调</b> ⇒ 它们的字段保持
	 * {@code null}，命中行为与改造前逐字相同（关卡 {@code wave-owner-assignments} 把"恰好 3 处"钉死）。</p>
	 */
	public void setOwner(@Nullable Entity owner) {
		this.state.set(WaveAttrs.OWNER, owner == null ? null : owner.getUUID());
		pushSyncedViews();
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
	 * <b>环绕波锚点契约（{@link OrbitAnchor}）的"取运动方向"实现 —— 波这一侧</b>
	 * （2026-10-02 批 4；作者裁定 D9 = A）。
	 *
	 * <p>实现体<b>只有一行</b>，而且就是改造前 {@code WaveOrbitElement#tick} 里那一行
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
	 * <p><b>为什么抽成静态方法而不是把三元表达式写进 {@link WaveOrbitElement#tick}</b>：
	 * 关卡要能钉住"镖与波各实现一次"，而不是钉住某一个调用点的写法。</p>
	 */
	public static Vec3 orbitDirectionOf(Entity anchor) {
		if (anchor instanceof OrbitAnchor orbitAnchor) {
			return orbitAnchor.orbitDirection();
		}
		return anchor.getDeltaMovement();
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
	 * <p><b>为什么抽成一处</b>：位置改写（{@link WaveOrbitElement#tick}）与粒子侧
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
	 * <b>命中附加效果要素</b>（可选、默认关闭，见 {@link WaveAttrs#HIT_EFFECT}）：<b>既有命中链之后</b>
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
		Holder<MobEffect> hitEffect = getHitEffect();
		int hitEffectDuration = getHitEffectDuration();
		if (hitEffect == null || hitEffectDuration <= 0) {
			return;
		}
		target.addEffect(new MobEffectInstance(hitEffect, hitEffectDuration, getHitEffectAmplifier()));
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
				|| (getSpawnPos() != null
					&& position().distanceToSqr(getSpawnPos()) > maxTravelDistance() * maxTravelDistance()))) {
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
		Vec3 nominal = getMovement().scale(getSpeedBlocks()); // 格/秒（当前名义速度向量）
		Vec3 step = nominal.scale(1.0 / 20.0);
		Vec3 corrected = com.hjmmd_8.createoreexpansion.content.energyfield.EnergyFields.applyFields(level(), this);
		if (corrected != null && corrected != nominal && !corrected.equals(nominal)) {
			// 场修正生效：按修正后的速度向量移动，并把主方向/速率对齐到真实速度
			setFieldVelocity(corrected);
			step = corrected.scale(1.0 / 20.0);
		}
		// ★ 重力要素（可选、默认关闭；弓技能批 4「量波置换」）：把本 tick 的位移追加一个向下分量。
		// ⚠ 刻意放在"能量场修正之后、自走位移之前"：重力是<b>本波自己的位移</b>的一部分，
		// 必须与正常自走一起落在 selfPropelled 窗口里（否则会被 setPos 覆写当成"外力搬运"，
		// 让攻击场的"真的穿过场盒"判定把受重力的波整段跳过）。
		// 未设要素（字段默认 0）⇒ 本方法第一句就返回同一个 Vec3，位移与改造前逐字相同。
		step = applyGravityElement(step);
		// 这一段是唯一的"自走"位移；selfPropelled 之外的一切 setPos 都记为外力搬运（见 setPos 覆写）。
		// 环绕波要素（可选、默认关闭）也在这段里改写位置：它同样是"波自己走出来的位移"
		// （环上相邻两点只差 0.25 格），故刻意留在 selfPropelled 窗口内，不被当成瞬移断点——
		// 否则攻击场的"真的穿过场盒"判定会永远跳过环绕波。位置改写必须发生在移动<b>之后</b>：
		// 本 tick 的最终位置就是圆周点，命中/粒子/诊断全部按它算。
		selfPropelled = true;
		setPos(position().add(step));
		boolean anchorAlive = WaveOrbitElement.applyOrbitElement(this);
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
			boolean inside = getChargePolarity() != null
				&& com.hjmmd_8.createoreexpansion.content.energyfield.EnergyFields.isInAnyField(level(), position());
			if (inside != wasInsideField) {
				wasInsideField = inside;
				CoeCore.LOGGER.info("[能量场] 波#{} {}场  pos={}  电荷={}  场内场数={}",
					getId(), inside ? "进入" : "离开", position(), getChargePolarity(),
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
		if (getChargePolarity() == null && level() instanceof net.minecraft.server.level.ServerLevel) {
			try {
				WaveMachineIntegrationPoints.chargeNearbyCoil(this);
			} catch (Throwable ignored) {
				// 联动异常：波照常飞行
			}
		}

		// 调级器增强延迟：穿过顺基准调级器后累计飞行距离，满 0.5 格（= 1/(2v) 秒）后等级 +boostStep
		// （翡翠/蓝宝石恒 +1；星辉石按授予时的转速可为 +2）。提升等级封顶于 WaveLevels.MAX_LEVEL（5），
		// 星辉石 +2 从 4 级升到 6 时实际停在 5（ω），不会超上限。
		float boostRemaining = getBoostRemaining();
		if (boostRemaining > 0) {
			boostRemaining -= (float) (getSpeedBlocks() / 20d);
			if (boostRemaining <= 0) {
				boostRemaining = 0;
				setWaveLevel(Math.min(getWaveLevel() + getBoostStep(), WaveLevels.MAX_LEVEL));
				setBoostStep(1);
			}
			setBoostRemaining(boostRemaining);
		}

		// 平滑渐变：目标色 = 当前等级基色；待升级（boostRemaining>0）时提前渐变到提升后的等级基色
		// （按 boostStep 预览，封顶 MAX_LEVEL），使波穿出调级器后颜色即开始过渡，而非延迟结束瞬间跳变。
		// 2026-09-14：目标色再过一遍**本波波型的拖尾风格变换**（ChargerWaveFx.styleColor）——
		// 于是"波体本身 / 拖尾 / 绽放 / 爆炸"四处用同一套颜色。
		// 2026-10-24 批 4（作者 D2，裁定"B"）：这一步改成**唯一的颜色规则**
		// WaveColorRule#colorOf ——
		//   ① 有魔素 ⇒ 魔素色（WaveEssence 登记的颜色变换）；
		//   ② 没魔素 ⇒ 波型色（WaveType#color，内置三个 = 原回退风格的变换）；
		//   ③ 两者都没给 ⇒ 等级基色。
		// 内置魔素/波型的颜色变换都拿等级基色当**明度输入** ⇒ 观感与改造前逐值相同
		// （"纯平色"读法已被作者明确否决，理由 = 等级读不出来；见 WaveColorRule 类注释）。
		Vec3 levelColor = getBoostRemaining() > 0
			? getWaveColorForLevel(Math.min(getWaveLevel() + getBoostStep(), WaveLevels.MAX_LEVEL))
			: getWaveColor();
		Vec3 targetColor = WaveColorRule.colorOf(essenceSpec(), getWaveType(), levelColor);
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
		//      见下面的 isOrbiting() 分支与 WaveOrbitElement.emitGeometry。
		//
		// 为什么<b>只</b>跳过/叠加粒子而不提前 return：后面的命中判定/方块碰撞/波波碰撞/收尾分支
		// 对环绕波仍然有意义（它照样会撞上生物、撞上父波、寿命到点），提前 return 等于顺手
		// 把它的命中与收尾一起关掉——那是改机制，不是改观感（关卡
		// wave-orbit-particles-exclusive 专门守着这条负向断言）。
		// ==================================================================================
		// 2026-10-24 批 3：本 tick 的飞行方向从 WaveState 取一次（此后的粒子与扫掠盒都用同一份；
		// 旧代码读字段，字段在上一段能量场修正后已经定格，两者逐位相同）。
		Vec3 movement = getMovement();
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
			WaveOrbitElement.emitOrbitGeometry(this, orbitServer);
		}

		// Ponder 场景：无真实方块/实体碰撞，飞一段距离后自动消散（remove 时客户端球面绽放）
		if (ponderScene) {
			if (tickCount > 40)
				discard();
			return;
		}

		// 命中生物/掉落物：单次实体查询（复用同一 AABB，避免两次 getEntitiesOfClass 遍历开销）。
		// 波波碰撞优先：两个能量波相遇 → 相互湮灭，触发范围爆炸（见 WaveCollision.resolve）。
		AABB hitBox = getBoundingBox().inflate(0.4);
		List<AbstractChargerWaveEntity> waves = level().getEntitiesOfClass(AbstractChargerWaveEntity.class,
			hitBox, w -> w != this && w.isAlive() && !w.collided);
		if (!waves.isEmpty()) {
			WaveCollision.handleWaveCollision(this, waves.get(0));
			return;
		}

		// 命中生物：<b>所有波型都碰撞消散</b>（用户 2026-10-01："能量波碰到实体后应该立刻消失，
		// 而不是穿过去，这是非常不合常理的"）。伤害仍然<b>只有攻击态</b>造成 ——
		// 普通态专事加工、变体态走远程加工，它们只是"撞上就消失"，不兼职武器。
		// ★ 2026-10-03（coe-boom2 批 2，作者裁定 <b>B 方案</b>）：主人从<b>命中列表</b>里排除
		// （{@code !isOwner(e)}）⇒ 波从主人身上穿过去、照飞：主人不吃命中伤害、不上魔素命中效果、
		// 不触发 burst、波也不 discard（"波照飞、不消散、不受伤害、不上魔素效果"）。
		// 缺省（无主人 = 一切机器波 / 变器波）时 {@link #isOwner} 恒 false ⇒ 本行与改造前
		// <b>逐字等价</b>。<b>反例</b>：把"无主人"当成"排除所有人"（isOwner 在 owner==null 时返回 true）
		// 会让机器波穿过所有人 —— 那是灾难级，关卡 wave-owner-hit 专门守着这一条。
		List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, hitBox, e -> e.isAlive() && !isOwner(e));
		// ★ <b>只给主人充能</b>（B 方案的核心，2026-10-03）：主人既然已被排除在上面那个列表之外，
		// 就不会再走下面那条原路径（那一条只服务"列表里的玩家"）⇒ 全类给主人充能<b>恰好这一处</b>。
		// 要保住的既有口径 = 2026-10-01"让能量波去打你自己来充能"：入口与参数照原路径<b>逐字抄</b>
		// （{@code ArmorEnergy.chargeWorn} + {@code ChargingRecipe.energyForLevel(this.waveLevel)}），
		// <b>不新造充能量、不新造半径</b>——判据就是同一个 hitBox（与命中判定同一处口径）。
		// ⛔ 这一段<b>只有充能</b>：不造成伤害、不施加魔素命中效果、不触发绽放、不销毁波（波照飞）。
		// 两道闸门：① 无主人（机器波）⇒ getOwnerUuid() 为 null ⇒ 连查询都不发，行为逐字不变；
		// ② ownerCharged ⇒ 一次飞行只充一次（波速 0.025~0.6 格/tick、命中盒 1.0 格见方，同一枚波
		// 会连续好几 tick 罩住主人；不设这道闸门就是把"撞一次充一次"变成"过一趟充三四次"）。
		if (getOwnerUuid() != null && !ownerCharged) {
			List<Player> ownersInBox = level().getEntitiesOfClass(Player.class, hitBox,
				p -> p.isAlive() && isOwner(p));
			if (!ownersInBox.isEmpty()) {
				ownerCharged = true;
				ArmorEnergy.chargeWorn(ownersInBox.get(0), ChargingRecipe.energyForLevel(getWaveLevel()));
			}
		}
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
				ArmorEnergy.chargeWorn(wearer, ChargingRecipe.energyForLevel(getWaveLevel()));
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
			// ★ coe-charge 批 5（需求 §3.2 表格第 4 行）：**被带正电/负电的能量波击中 ⇒ 继承波的极性**
			// （等级 = 波级；时长 = 数值真源的 durationTicks(波级)，即 200 tick × 波级，绝不在这里写乘法）。
			// 刻意放在上面那条"目标就是玩家"的分支<b>之外</b> ⇒ <b>玩家与生物同时成立</b>（与魔素命中效果
			// 那条"只对玩家"相反，需求 §3.2 #4 没有玩家限定）。
			// 未带电的波（机器波 / 变器波 / 一切既有波）getChargePolarity() 返回 null ⇒ 第一句就跳过，
			// 它们的命中行为与改造前<b>逐字相同</b>。
			// 施加一律走 ChargeApi（四条获得途径共用同一个施加面）：同极合并、异极中和都由门面负责，
			// 本类不构造 MobEffectInstance、也不认那两个 effect 类（免疫照旧走 addEffect 的既有通道）。
			// ⚠ 既有命中链的<b>相对次序</b>一个字未动：伤害 ⇒ 护甲充能 ⇒ applyHitEffect ⇒ ★本段 ⇒ burst ⇒ discard。
			com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity waveCharge = getChargePolarity();
			if (waveCharge != null) {
				com.hjmmd_8.createoreexpansion.content.energyfield.ChargeApi.apply(target, waveCharge,
					getWaveLevel(),
					com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs.durationTicks(getWaveLevel()));
			}
			// ★ 2026-10-04（弓技能批 3「元矢自生」，作者当日下达的<b>统一标准</b>）：
			// 「雷魔素攻击生物之后，会随机给生物附着一种电荷，附着时间为 5 秒，附着的那个 buff
			// 等级从 1~3 随机」⇒ 这条标准<b>不只服务元矢自生</b>：凡"带雷魔素的攻击波命中生物"
			// 都算，所以施加点就落在这一处"波命中生物"的路径上（与上面那条"目标就是玩家"的
			// 魔素分支<b>并列而不重叠</b>），元矢自生只是"借用同一条路径"。
			// 它刻意放在玩家分支<b>之外</b> ⇒ 生物才吃得到；也刻意**只**放雷过闸：另外七种魔素
			// 对生物尚无裁定，未带雷魔素的波打中生物时这句什么都不做（与改造前逐字相同）。
			// ⚠ 玩家那一侧的口径没变：雷魔素打中玩家仍走旧路径、不上电荷（作者已知并接受这条
			//   中转期的不一致，见 WaveEssenceEffects 类注释 (a)）。
			// 施加面仍是"魔素层 ⇒ 电荷层 ChargeApi"两级（本类不认电荷、不构造效果实例、不写数字）。
			WaveEssenceEffects.applyOnWaveCreatureHit(this, target);
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
		// 环绕波要素未设 ⇒ 与改造前逐字相同（第一道闸；见 WaveOrbitElement.tick 的同形守卫）。
		UUID orbitAnchorUuid = getOrbitAnchorUuid();
		if (orbitAnchorUuid == null || pos == null) {
			return;
		}
		if (!(level() instanceof ServerLevel server)) {
			return;
		}
		Entity anchor = server.getEntity(orbitAnchorUuid);
		if (anchor instanceof OrbitAnchor orbitAnchor) {
			orbitAnchor.orbitMineBlock(pos);
		}
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
				ChargerWaveFx.burstOrbitSpawn(orbitServer, position(), renderColor, null, null, getOrbitRadius(),
					orbitCurrentPhase);
			} else {
				ChargerWaveFx.burst(orbitServer, position(), trailStyle(), renderColor);
			}
			// 收尾诊断行（与出生/心跳同一条通道，见 WaveOrbitElement.logDiag）：作者只靠日志判
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
				why, getOrbitAnchorUuid(), getFiringBatch(), WaveLevels.glyph(getWaveLevel()),
				WaveOrbitElement.fmt2(getOrbitRadius()), tickCount);
		}
		super.remove(reason);
	}

	// 注意：这里**没有**速度上限常量——上限由等级速度表给（WaveLevels.maxSpeed：1~3 级 10、4/5 级 12）。
	// 2026-09-14 删除了一处遗留的 `MAX_SPEED = 10.0d`：它全仓无人使用，而取值与速度表矛盾（表里 4/5 级是 12），
	// 留着只会让读者以为加速被夹在 10。
	// 2026-10-24 批 2：速度下限（原 `MIN_SPEED = 0.5d`）与整段夹取也搬进了唯一实现 WaveSpeed（见该类注释）。

	/**
	 * 移动速度（格/秒）：等级基础速度（查 {@link WaveLevels#baseSpeed}，α 2 / β 4 /
	 * γ 6 / ε 7 / ω 8）+ 速度修正值（波速调节器叠加），
	 * 夹在 {@link WaveSpeed#MIN_SPEED} ~ {@link WaveLevels#maxSpeed}（4/5 级 12，1~3 级 10）之间。
	 *
	 * <p><b>唯一实现</b>：本方法只把本波的两个值交给 {@link WaveSpeed#perSecond}。改造前同一段
	 * 夹取算式在本类里写了两遍（本方法与 {@link #getWaveSpeed()}），两处对同一枚波会差一个
	 * float 舍入；现在算术只有那一份，任何"波速"点位都不得再抄第二份。</p>
	 */
	protected double getSpeedBlocks() {
		return WaveSpeed.perSecond(getWaveLevel(), getSpeedOffset());
	}

	/**
	 * <b>重力要素</b>（可选、默认关闭，见 {@link WaveAttrs#GRAVITY}）：把本 tick 的位移
	 * 追加一个向下的速度分量，并把下落速度按秒²加速度累积。
	 *
	 * <p>三条口径，逐条对应关卡：</p>
	 * <ol>
	 *   <li><b>要素未设 ⇒ 原样返回</b>（第一句）：{@code step} 是同一个对象、同一份数值
	 *       ⇒ 机器波 / 变器波 / 一切既有波的飞行与改造前<b>逐字相同</b>（正向与负向都在
	 *       bow4-gravity-gated 里）；</li>
	 *   <li><b>水中走直线</b>：{@link net.minecraft.world.entity.Entity#isInWater()}（= 原版
	 *       {@code wasTouchingWater}，在本 tick 的 {@code super.tick()} 里刚刷新）为真时
	 *       <b>不累加、并把下落速度清零</b> ⇒ 位移恒为 {@code movement × 速度 / 20}，
	 *       即"沿发射方向的匀速直线"。这里刻意<b>不是</b>"在水中把重力翻倍/减半"，
	 *       而是"水中这个要素整个不作用"（作者原话："在水中能够沿直线飞行"）；</li>
	 *   <li><b>下落分量只进位移、不进 movement</b>：主方向仍是发射方向 ⇒
	 *       {@link #orbitDirection()}（环绕波的环平面法向）、粒子散布方向、掉落物扫掠盒
	 *       与能量场读数全部不受重力影响（"别改既有要素的读数"）。</li>
	 * </ol>
	 *
	 * <p>下游消费者（{@code ChargerWaveFx} 的拖尾/绽放、{@code WaveHitResolver} 的命中链）
	 * 一个字都不用改：它们只看位置与 movement，而位置由本方法逐 tick 影响。</p>
	 */
	private Vec3 applyGravityElement(Vec3 step) {
		if (!hasGravity()) {
			// 要素未设（= 一切既有波）：原样返回同一个 Vec3 —— 不是"加个零向量"，
			// 是这一整段在数值上与不存在等价（关卡 bow4-gravity-gated 的负向断言读这一句）。
			return step;
		}
		if (isInWater()) {
			// 水中走直线：不再累加下落速度，并把它清零 ⇒ 本 tick 位移 = 沿 movement 的匀速直线。
			gravityFallSpeed = 0.0D;
			return step;
		}
		gravityFallSpeed += gravityAcceleration() * ChargeConfigs.perTickFactor();
		return step.add(0.0D, -gravityFallSpeed * ChargeConfigs.perTickFactor(), 0.0D);
	}

	/**
	 * <b>重力要素的当前值</b>（键 {@code GravityAcceleration}；本类唯一的读取点）。
	 *
	 * <p>2026-10-24 批 6：由 {@code private} 放宽为公开访问器 —— 演示面/诊断面与 {@code WaveAccess}
	 * 门面都要能"读回我设了多少"，而此前这个要素只有写入口（{@link #setGravity}）与布尔判据
	 * （{@link #hasGravity}）。默认 {@code 0.0D} = 未设，与改造前逐字相同。</p>
	 *
	 * <p>⚠ <b>刻意不叫 {@code getGravity()}</b>：原版 {@link Entity#getGravity()} 是 {@code final}
	 * 的（1.21.1 的实体重力属性读法），同名会直接编译失败。本方法读的是<b>本要素</b>的加速度，
	 * 与那个原版字段无关。</p>
	 */
	public double gravityAcceleration() {
		return this.state.get(WaveAttrs.GRAVITY);
	}

	/** 施加一次速度修正（叠加语义：在此值上增加 amount，可正可负）。 */
	public void addSpeedOffset(double amount) {
		this.state.set(WaveAttrs.SPEED_OFFSET, getSpeedOffset() + amount);
		// 同步到客户端（Jade 速度显示）
		pushSyncedViews();
	}

	// ========== 能量场（FieldedEntity） ==========

	/** 赋予/清除电荷（能量着电器调用；null 清除）。写入同步数据（Jade 客户端显示用）。 */
	public void setCharge(com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity charge) {
		this.state.set(WaveAttrs.CHARGE, charge);
		pushSyncedViews();
	}

	/** 当前电荷（可能为 null = 不带电）。优先读同步数据（客户端实例）；服务端兜底用状态。 */
	@Override
	public com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity getChargePolarity() {
		int code = this.entityData.get(CHARGE);
		if (code == 1)
			return com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity.POSITIVE;
		if (code == 2)
			return com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity.NEGATIVE;
		return this.state.get(WaveAttrs.CHARGE); // 服务端尚未广播前兜底
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
		return getMovement().scale(getSpeedBlocks());
	}

	/**
	 * 写入修正后的速度向量（格/秒）：主方向跟随真实速度方向，速率变化并入 speedOffset（可被场加速/减速）。
	 *
	 * <p>速率差走唯一写入口 {@link #addSpeedOffset(double)}：它在同一个值上做加法，同时刷新客户端
	 * 显示用的同步值 —— 否则 Jade 的波速行会停在场修正前的旧值（服务端现在是权威字段，
	 * 客户端只有同步值，两者不能各说各话）。位移本身与改造前逐值相同。</p>
	 */
	@Override
	public void setFieldVelocity(Vec3 velocity) {
		double len = velocity.length();
		if (len < 1.0E-4) {
			setMovement(Vec3.ZERO);
			return;
		}
		Vec3 old = getMovement().scale(getSpeedBlocks());
		double oldLen = old.length();
		// 速率差并入叠加修正（加速场正向累积、负电荷/反向减速）
		if (Double.isFinite(len - oldLen) && Math.abs(len - oldLen) > 1.0E-6)
			addSpeedOffset(len - oldLen);
		setMovement(velocity.scale(1.0 / len));
	}

	// ========== 公开只读访问（供 Jade 等外部显示） ==========
	// 注意：Jade 在客户端运行，读到的是客户端实体实例——普通字段（waveLevel/speedOffset）
	// 不会同步，等级与速度修正必须从 SynchedEntityData（WAVE_LEVEL/SPEED_OFFSET）读取。
	// 唯一例外是服务端：它同时持有权威 double 字段，故速度修正按 side 分流（speedOffsetForRead）。

	/** 波等级（1=α、2=β、3=γ）。 */
	public int getWaveLevel() {
		return this.entityData.get(WAVE_LEVEL);
	}

	/**
	 * 实际运行速度（格/秒，含波速调节器/能量场修正）。
	 *
	 * <p><b>与 {@link #getSpeedBlocks()} 是同一个算术</b>（{@link WaveSpeed#perSecond}），
	 * 差别只在"速度修正从哪种介质读"：服务端读权威 {@code double} 字段，客户端只能读同步来的
	 * {@code float}（{@link #SPEED_OFFSET}，{@code double} 没有序列化器）。改造前这里另写了一整遍
	 * 夹取、且恒读 float，于是同一枚波在服务端会与位移用的速度差一个 float 舍入
	 * （客户端本来也只有 float，读到的值与改造前逐位相同）。</p>
	 */
	public double getWaveSpeed() {
		return WaveSpeed.perSecond(getWaveLevel(), speedOffsetForRead());
	}

	/**
	 * 速度修正的读法：服务端读权威字段（{@code double}，与位移/命中扫掠盒同一个值），
	 * 客户端读同步值（{@code double} 没有序列化器，只能同步 {@code float}）。
	 *
	 * <p>这是全类唯一允许"直接读 {@link #SPEED_OFFSET} 求速度"的地方；其余一切速度算术都走
	 * {@link WaveSpeed#perSecond}（关卡 {@code wave-speed-single-source} 守着）。</p>
	 */
	private double speedOffsetForRead() {
		return level().isClientSide ? this.entityData.get(SPEED_OFFSET) : getSpeedOffset();
	}

	/** 剩余寿命（tick）：距自动消散还剩多少 tick（负数/0 = 即将消散）。 */
	public int getRemainingLifetime() {
		return MAX_LIFETIME_TICKS - tickCount;
	}

	/**
	 * <b>改写剩余寿命（tick）</b>（2026-10-24 批 6：寿命从此可读<b>也可设</b>）。
	 *
	 * <p>写的是本类自己的时间基 {@code tickCount}（原版 {@code Entity} 字段，实体唯一的"活了多久"）：
	 * {@code tickCount = MAX_LIFETIME_TICKS - ticks}。于是 {@link #getRemainingLifetime()} 与 tick 里
	 * 那条 {@code tickCount > MAX_LIFETIME_TICKS} 的消散判定<b>同时</b>看到新值 —— 没有第二个计时器、
	 * 也没有第二处判定（这正是"一处真源"的要求）。</p>
	 *
	 * <p><b>不设时逐字不变</b>：本方法没有任何调用点，既有波的 {@code tickCount} 仍由原版 tick 递增。
	 * 传入值夹到 {@code [0, MAX_LIFETIME_TICKS]}：{@code 0} = 本 tick 之后再飞一 tick 就消散，
	 * 负值不会让 {@link #getRemainingLifetime()} 读出"比上限还长"。</p>
	 *
	 * <p>⚠ 这是<b>时间</b>寿命；行程上限（{@link #setMaxTravelDistance}）是另一条独立的消散条件，
	 * 本方法不会放宽它。</p>
	 *
	 * @param ticks 新的剩余寿命（tick；夹到 0..{@link #MAX_LIFETIME_TICKS}）
	 */
	public void setRemainingLifetime(int ticks) {
		this.tickCount = MAX_LIFETIME_TICKS - net.minecraft.util.Mth.clamp(ticks, 0, MAX_LIFETIME_TICKS);
	}

	/**
	 * <b>等级基色</b>（RGB 0-1，Jade / 查询仪显示用）。
	 *
	 * <p>⚠ 这是"等级基色"，<b>不是</b>这枚波最终看到的颜色（最终颜色 = {@link WaveColorRule#colorOf}：
	 * 有魔素 ⇒ 魔素色、没魔素 ⇒ 波型色）。客户端实例的 {@code renderColor} 字段不同步，所以显示层
	 * 在客户端只能取到这个静态基底 —— 这也是 Jade 的"魔素"行单独用
	 * {@code ChargerWaveFx.styleColorRgb(essence)} 上色的原因（两行各说各自的事实）。</p>
	 */
	public Vec3 getWaveRenderColor() {
		return getWaveColorForLevel(getWaveLevel());
	}

	/**
	 * 设置能量波等级并同步核心加工逻辑（等级决定可匹配配方上限、伤害、波速）。
	 * 调级器升级/降级、以及增强延迟到期升级时调用。
	 */
	public void setWaveLevel(int level) {
		this.state.set(WaveAttrs.LEVEL, level);
		this.processor = new ChargerWaveProcessor(level(), level);
		// 同步到客户端（Jade 等级显示）
		pushSyncedViews();
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
		return new Vec3(this.state.get(WaveAttrs.MOVE_X), this.state.get(WaveAttrs.MOVE_Y),
			this.state.get(WaveAttrs.MOVE_Z));
	}

	/** 覆写主运动方向（只改方向，不动速度修正）。 */
	public void setMovement(Vec3 movement) {
		if (movement == null) {
			this.state.clear(WaveAttrs.MOVE_X);
			this.state.clear(WaveAttrs.MOVE_Y);
			this.state.clear(WaveAttrs.MOVE_Z);
			return;
		}
		this.state.set(WaveAttrs.MOVE_X, movement.x);
		this.state.set(WaveAttrs.MOVE_Y, movement.y);
		this.state.set(WaveAttrs.MOVE_Z, movement.z);
	}

	/** 出生点（最大行程判定用；读档前为 null）。 */
	public Vec3 getSpawnPos() {
		if (!this.state.isSet(WaveAttrs.SPAWN_X))
			return null;
		return new Vec3(this.state.get(WaveAttrs.SPAWN_X), this.state.get(WaveAttrs.SPAWN_Y),
			this.state.get(WaveAttrs.SPAWN_Z));
	}

	/** 设置出生点（子波按 contraption / sub-level 坐标换算后回写）。 */
	public void setSpawnPos(Vec3 spawnPos) {
		if (spawnPos == null) {
			this.state.clear(WaveAttrs.SPAWN_X);
			this.state.clear(WaveAttrs.SPAWN_Y);
			this.state.clear(WaveAttrs.SPAWN_Z);
			return;
		}
		this.state.set(WaveAttrs.SPAWN_X, spawnPos.x);
		this.state.set(WaveAttrs.SPAWN_Y, spawnPos.y);
		this.state.set(WaveAttrs.SPAWN_Z, spawnPos.z);
	}

	/**
	 * <b>本波的行程上限（格）</b>（2026-10-24 批 6）：默认 = {@link #MAX_TRAVEL_DISTANCE} = 64.0。
	 *
	 * <p>读它的是 {@code tick()} 里那一句判定（越过即消散）。"读"此前<b>完全拿不到</b>
	 * （常量是 {@code protected static final}，门面也没有转发），第三方无法回答"这枚波还能飞多远"。</p>
	 */
	public double maxTravelDistance() {
		return this.maxTravelDistance;
	}

	/**
	 * <b>已飞行距离（格）</b>：当前位置到出生点的直线距离；出生点未知（读档前 / 没设）⇒ {@code 0}。
	 *
	 * <p>它是"行程"的读数面（与 {@link #maxTravelDistance()} 配对）：第三方不必自己拼
	 * {@code position().distanceTo(getSpawnPos())}，也不会漏掉"出生点为 null"那一支。</p>
	 */
	public double travelledDistance() {
		Vec3 spawnPos = getSpawnPos();
		return spawnPos == null ? 0.0D : position().distanceTo(spawnPos);
	}

	/**
	 * <b>改写本波的行程上限（格）</b>（2026-10-24 批 6：行程从此可设）。
	 *
	 * <p>缺省值就是 {@link #MAX_TRAVEL_DISTANCE}；<b>不调用</b> ⇒ 判定与改造前逐字相同（同一个数、
	 * 同一个比较、同一个 tick）。刻意接受任意正值：上限可以<b>放宽</b>也可以<b>收紧</b>，
	 * 是否合理由调用方负责（与重力要素"强度由调用方传"同一分工）。</p>
	 *
	 * <p>⚠ 非正值一律<b>不做事</b>（保持当前上限）：{@code 0} 的语义会是"出生即消散"，
	 * 与"没设"无法区分，容易把一次笔误变成"这枚波根本飞不出去"。</p>
	 *
	 * @param blocks 新的行程上限（格；{@code <= 0} 视为不做事）
	 */
	public void setMaxTravelDistance(double blocks) {
		if (!Double.isFinite(blocks) || blocks <= 0.0D)
			return;
		this.maxTravelDistance = blocks;
	}

	/**
	 * <b>读一个"表外" NBT 键</b>（第三方属性袋；2026-10-24 批 6）。
	 *
	 * <p>批 3 的未知键保留袋（{@code WaveState#unknown()}）保证第三方写在波 NBT 上的键
	 * <b>读一遍再写一遍不会被抹掉</b>；但袋子的入口此前只在包内，第三方既写不进也读不出。
	 * 本方法是那一对入口里的"读"。</p>
	 *
	 * @param key NBT 键名（本模组 31 个已声明键不属于这里）
	 * @return 原样保存的标签；没有 / 键名为 {@code null} ⇒ {@code null}
	 */
	@Nullable
	public Tag extraTag(String key) {
		return key == null ? null : this.state.unknown()
			.get(key);
	}

	/**
	 * <b>写一个"表外" NBT 键</b>（第三方属性袋；2026-10-24 批 6）。
	 *
	 * <p>写进 {@code WaveState#unknown()}，由 {@link WaveStateNbt} 在存档时"只补不覆盖"地带出去
	 * （父类字段名不会被顶掉）。<b>已声明的 31 个键拒绝</b>：那些键的值真源是 {@link WaveAttrs} 表，
	 * 从侧门写会被下次读写按表覆盖 —— 静默失效比拒绝更糟。</p>
	 *
	 * @param key   NBT 键名（{@code null} / 空串 / 已声明键 ⇒ 返回 {@code false}）
	 * @param value 标签（{@code null} ⇒ 返回 {@code false}；要删请用 {@link #removeExtraTag}）
	 * @return 是否真的写进去了
	 */
	public boolean putExtraTag(String key, Tag value) {
		if (key == null || key.isEmpty() || value == null || WaveAttrs.isDeclared(key))
			return false;
		this.state.unknown()
			.put(key, value);
		return true;
	}

	/**
	 * <b>删一个"表外" NBT 键</b>（第三方属性袋；2026-10-24 批 6）。
	 *
	 * @param key NBT 键名（{@code null} ⇒ {@code false}）
	 * @return 是否真的删掉了（原来就没有 ⇒ {@code false}）
	 */
	public boolean removeExtraTag(String key) {
		return key != null && this.state.unknown()
			.remove(key) != null;
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
		return this.state.get(WaveAttrs.SPEED_OFFSET);
	}

	/** 加速场剩余作用距离（格；>0 表示本 tick 处于加速场中）。 */
	public float getBoostRemaining() {
		return this.state.get(WaveAttrs.BOOST_REMAINING);
	}

	/** 覆写加速场剩余作用距离（波闸/能量场写入）。 */
	public void setBoostRemaining(float boostRemaining) {
		this.state.set(WaveAttrs.BOOST_REMAINING, boostRemaining);
	}

	/** 加速场档位（每 tick 提速的等级）。 */
	public int getBoostStep() {
		return this.state.get(WaveAttrs.BOOST_STEP);
	}

	/** 覆写加速场档位。 */
	public void setBoostStep(int boostStep) {
		this.state.set(WaveAttrs.BOOST_STEP, boostStep);
	}
	/** 命中伤害：α 4、β 6、γ 8、ε 10、ω 12 —— 查 {@link WaveLevels#damage}。 */
	protected float getDamage() {
		return WaveLevels.damage(getWaveLevel());
	}

	/**
	 * <b>本波当前会造成的命中伤害（只读公开面）</b> —— 就是命中链真正用的那个值。
	 *
	 * <p>2026-10-24 批 6：门面此前<b>没有</b>任何伤害读数（{@code getDamage()} 是 {@code protected}），
	 * 第三方只能自己按波级查表；而 {@code charger_wave} 还有"自定义伤害"覆盖，自己查表会算错。
	 * 本方法只是转发 {@link #getDamage()}：**基类仍然是"纯等级表"那一行**（关卡
	 * {@code wave-essence-values} / {@code star-orbit-element} 都钉着它），子类覆盖（
	 * {@code ChargerWaveEntity#getDamage}）自然被看见。</p>
	 *
	 * <p>⚠ <b>它只回答"会打多少"，不提供覆写入口</b>：非 {@code charger_wave} 的伤害覆盖需要改基类
	 * 那条被钉住的纯表算术 = 玩法面变更，本批按"纯加法"红线<b>不做</b>（见批 6 报告）。</p>
	 */
	public float damageValue() {
		return getDamage();
	}

	/**
	 * <b>等级基色</b>（RGB 0-1）：1 α=黄、2 β=绿、3 γ=蓝、4 ε=紫粉、5 ω=玫红 —— 数值住在
	 * 等级注册项里（{@link WaveLevels#particleColor(int)}）。
	 *
	 * <p>⚠ 它<b>不是</b>最终颜色：最终颜色 = {@link WaveColorRule#colorOf}（有魔素 ⇒ 魔素色；
	 * 没魔素 ⇒ 波型色；都没给 ⇒ 本方法）。</p>
	 */
	protected Vec3 getWaveColor() {
		return getWaveColorForLevel(getWaveLevel());
	}

	/**
	 * 指定等级的<b>等级基色</b>（供调级器渐变提前取下一等级色用）。
	 *
	 * <p>2026-10-24 批 4：原来这 5 臂 {@code switch (level)} 已搬进
	 * {@link WaveLevelSpec#particleColor()}（字面量逐位不变），本方法只剩转发 —— 于是"加一档等级"
	 * 仍然只是等级注册表里的一行，而不是在这里再补一个 {@code case}。</p>
	 */
	protected Vec3 getWaveColorForLevel(int level) {
		return WaveLevels.particleColor(level);
	}

	/**
	 * 是否 ω（5 级）波：粒子拖尾用金色。
	 *
	 * <p>2026-10-24 波等级注册化（批 1）：这里原来写死 {@code waveLevel >= 5}——那是"等级表之外的第 3
	 * 个写死阈值"（加第 6 级会自动吃金色尾迹，而且没人能改）。现在标记由该等级的注册项声明
	 * （{@code WaveLevelSpec#goldTrail}，内置仅 5 级为 true），越界等级的回落口径与改造前逐值一致
	 * （见 {@link WaveLevels#isGoldTrailLevel(int)}）。本方法签名不变（:cews 依赖它）。</p>
	 */
	public boolean isOmega() {
		return WaveLevels.isGoldTrailLevel(getWaveLevel());
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
	 * 创建降一级的"子波"（供差器均摊分发）。
	 *
	 * <p><b>2026-10-24 批 5（作者裁定 D5：「只要是分裂出来的子波，只改变等级，并加上一个颜色渐变的
	 * 过渡，其他什么都不改」）："造子波"只有这一处实现</b> —— 本方法是<b>模板方法</b>，三步固定顺序：</p>
	 * <ol>
	 *   <li>{@link #newChildWave} —— 子类只回答"用哪种构造器造一个空壳"（唯一的类型差异）；</li>
	 *   <li>{@link #inheritForChild} —— <b>整份拷贝父波的 {@link WaveState}</b>（31 键，唯一表），
	 *       再重写"这枚子波自己的"四个分裂参数（等级 / 方向 / 出生点 / 起始渲染色）；</li>
	 *   <li>{@link #distributePayloadToChild} —— 变体波把<b>物质</b>（载荷 / 链式次数）按份均摊，
	 *       普通波空实现。</li>
	 * </ol>
	 *
	 * <p>⚠ <b>以前这里是 {@code abstract}</b>：两个子类各写一遍（普通波只继承等级、变体波再抄 5 个
	 * 能力键 + 载荷均摊），而 5 处差器分支又在调用点手抄 spawnPos / movement / speedOffset ——
	 * 那正是方案 §1.4-A 的"隐形第四真源"：<b>新属性默认不会传给子波，而且没有任何报错</b>。
	 * 现在"加一个属性"只要在 {@link WaveAttrs} 加一行，子波自动继承，调用点一个字都不用加。</p>
	 *
	 * @param pos   出生位置（已外推到差器外；结构 / contraption 场景由调用方<b>先换算到世界坐标</b>）
	 * @param dir   发射方向（出口开口朝向，单位向量，可为任意方向）
	 * @param level 子波等级（= 原波等级 - 降级量）
	 * @param index 本子波在<b>本次分裂</b>中的序号（0 起）
	 * @param total 本次分裂的子波总数（≥1）
	 * @return 新波实体；null 则放弃发射
	 */
	public final AbstractChargerWaveEntity createChildWave(Vec3 pos, Vec3 dir, int level, int index, int total) {
		AbstractChargerWaveEntity child = newChildWave(pos, dir, level);
		if (child == null)
			return null;
		inheritForChild(child, pos, dir, level);
		distributePayloadToChild(child, index, total);
		return child;
	}

	/**
	 * 子类的<b>唯一差异</b>：造一个空壳子波（等级 / 位置 / 方向由构造器先写进同步数据，
	 * 随后 {@link #inheritForChild} 会把父波整份属性覆盖上去）。
	 */
	protected abstract AbstractChargerWaveEntity newChildWave(Vec3 pos, Vec3 dir, int level);

	/**
	 * <b>分裂继承的唯一出处</b>（作者 D5：子波 = 父波的完整拷贝，只改等级 + 颜色渐变过渡）。
	 *
	 * <p>本体只有一句 {@link WaveState#copyFrom}：31 个属性键整份覆盖（未知键也深拷贝）。
	 * 所以这里<b>不许出现任何逐属性的写入</b> —— 不写 {@code WaveAttrs.XXX}、不写
	 * {@code setOwner / setFiringBatch / trySetType / trySetEssence / setCustomDamage /
	 * setHitEffect / setCharge / setOrbitAnchor / setGravity / addSpeedOffset}：
	 * 加一个属性只要在 {@link WaveAttrs} 加一行，子波自动继承；任何"在这里补一个 setter"的写法
	 * 都会把方案 §1.4-A 的第四真源重新长出来（关卡 {@code wave-child-inherit} 判红）。</p>
	 *
	 * <p>拷贝之后只重写四个"这枚子波自己的"值：</p>
	 * <ul>
	 *   <li><b>等级</b> —— 分裂逻辑给的（差器按开口数降 1 / 2 / 3 级）；</li>
	 *   <li><b>方向 / 出生点</b> —— 出口几何给的（子波从哪个口出、从哪一点出生）；</li>
	 *   <li><b>起始渲染色</b> —— = 父波<b>当前</b>色。之后每 tick 的
	 *       {@code renderColor.lerp(target, 0.15)} 自动把它渐变到子波自己的目标色
	 *       （等级基色 / 魔素色 / 波型色，由 {@link WaveColorRule} 决定）—— 观感上就是
	 *       "出生那一刻还是父波的颜色，约一秒内过渡成自己的"。</li>
	 * </ul>
	 */
	protected final void inheritForChild(AbstractChargerWaveEntity child, Vec3 pos, Vec3 dir, int level) {
		child.state.copyFrom(this.state);
		child.setWaveLevel(level);
		child.setMovement(dir.normalize());
		child.setSpawnPos(pos);
		child.setRenderColor(this.renderColor);
	}

	/**
	 * <b>穿波转换的唯一继承实现</b>（2026-10-24 批 6）：变器把一枚旧波<b>换成一枚新波</b>
	 * （{@code charger_wave} → 变体波），新波要承接"表现/飞行连续性"的那三项。
	 *
	 * <p><b>为什么不是 {@link #inheritForChild}</b>：那是<b>分裂</b>（一枚父波 → 多枚子波，
	 * 作者 D5：整份拷贝、只改等级与出生色）。穿波是<b>旧波换新波</b>：新波不是子波，它带着变器
	 * 刚扫描到的载荷/配方类型/加热/转速/读取半径，而且**旧波被 discard**。把 D5 的"全拷贝"套到
	 * 这条路径会改玩法（新波会开始带主人 / 批次号 / 波型 / 魔素 / 重力 / 命中效果 / 环绕），
	 * 所以它是<b>另一套模板</b>，而不是分裂模板的一个开关。</p>
	 *
	 * <h2>改前 vs 改后（逐项）</h2>
	 * <p>改造前这三项由 {@code :cews} 的 {@code StellarWaveTransmuterPass} 手抄三行；现在只有这一处。
	 * 继承集合<b>逐项相同</b>：</p>
	 * <ul>
	 *   <li><b>速度修正</b>（{@code addSpeedOffset}，叠加语义 ⇒ 与旧波同一个绝对速度）；</li>
	 *   <li><b>电荷极性</b>（{@code setCharge}，能量场作用在穿波前后连续）；</li>
	 *   <li><b>当前渲染色</b>（{@code setRenderColor}，观感连续）。</li>
	 * </ul>
	 * <p><b>刻意不继承</b>（= 改造前的实际行为，一个字节都没变）：等级与方向由新波的构造器给
	 * （{@code new StellarWaveEntity(level, exit, movement, waveLevel)}）；<b>波型 / 魔素 / 主人 /
	 * 批次号 / 环绕四件套 / 命中效果三件套 / 重力 / 寿命 / 行程 / 自定义伤害</b>都不带。
	 * 其中"主人 / 批次号"最要紧：带上主人 ⇒ 穿出来的波不再伤发射者；带上批次号 ⇒ 同批波互相不湮灭，
	 * 两者都是<b>玩法变化</b>，必须由作者单独裁定（批 6 报告已单列，等实机与裁定）。</p>
	 *
	 * <p>⚠ 本方法<b>不许</b>被改造出一条"顺手多继承一点"的路：新增一项就是改玩法，改之前先问。</p>
	 *
	 * @param replacement 变器新造的那枚变体波（{@code null} ⇒ 不做事）
	 */
	public final void inheritForTransmute(AbstractChargerWaveEntity replacement) {
		if (replacement == null)
			return;
		replacement.addSpeedOffset(getSpeedOffset());
		replacement.setCharge(getChargePolarity());
		replacement.setRenderColor(getRenderColor());
	}

	/**
	 * 分裂时的<b>物质</b>处置（默认空实现：普通能量波不带载荷）。
	 *
	 * <p>变体波覆写它，把载荷（物品 / 流体 / 电量）与链式次数<b>按份均摊</b>给第 {@code index} 个
	 * 子波（算术全在 {@code WaveChildSplit}）。口径不变："能力"整份继承、"物质"按份均摊。</p>
	 */
	protected void distributePayloadToChild(AbstractChargerWaveEntity child, int index, int total) {
	}

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
		// 2026-10-24 批 3：117 行手写键名 → 一次表遍历（WaveAttrs 31 键 + WaveStateNbt）。
		// 读侧语义逐字保留：缺键回落、成组守卫、四个"旧代码本来就夹取"的键都写在表的声明里；
		// WaveLevel 刻意不夹取（方案 R-3：夹取会改玩法）。
		WaveStateNbt.read(this.state, tag, waveScope());
		// 服务端从 NBT 恢复等级后，同步核心加工逻辑的等级（决定可匹配配方上限）
		this.processor = new ChargerWaveProcessor(level(), getWaveLevel());
		// 7 个同步视图（Jade 等客户端读取）由唯一推送口写回。
		pushSyncedViews();
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		// 2026-10-24 批 3：写不写由 WaveAttrs 的 Persist / 成组关系决定，与改造前逐键相同；
		// 表外键由 WaveStateNbt 原样补回（未知键保留袋，方案 R-5）。
		WaveStateNbt.write(this.state, tag, waveScope());
	}
}