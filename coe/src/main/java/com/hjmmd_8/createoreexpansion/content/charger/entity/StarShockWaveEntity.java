package com.hjmmd_8.createoreexpansion.content.charger.entity;

import com.hjmmd_8.createoreexpansion.common.registry.coe.charger.AllEntityTypes;
import com.hjmmd_8.createoreexpansion.content.skill.config.StarShockConfigs;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTypes;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

/**
 * <b>星芒嬗震的能量波</b>（星界套 · 槽位 3，用户 2026-10-02 星界轮需求 §3.3）。
 *
 * <p>它是 {@link ChargerWaveEntity} 的子类，专门给"星界套星芒嬗震"发射的攻击波用，
 * 与充能器/差波器发射的机器波<b>分成两个实体类型</b>（{@code star_shock_wave} vs {@code charger_wave}）：
 * 这样"技能专属行为"（自定义伤害、环绕波、命中嬗乱、发射批次）只写在这一个类里，
 * 机器波的字段与流程<b>一个字节都不动</b>（改共享基类时机器波是最容易被连带改坏的）。</p>
 *
 * <h2>它比基类多出来的四件事</h2>
 * <ol>
 *   <li><b>伤害来自技能表</b>：基类按波等级查 {@code WaveLevels.damage}（α 4 / β 6 / γ 8 …
 *       那是"机器波的伤害"）。本类覆写 {@link #getDamage()}，改查
 *       {@code StarShockConfigs}（Lv1 8 / Lv2 10 / Lv3 11），<b>不动全局表</b>。</li>
 *   <li><b>发射批次</b>：同一次技能发射的主波 + 分叉波 + 环绕波共用一个批次号（基类的
 *       {@code FIRING_BATCH} 同步字段），两波同批 ⇒ 互不爆炸、互不湮灭。</li>
 *   <li><b>环绕波</b>：带父波 UUID 的波是"环绕波"——它每 tick 把自己摆在
 *       "主波位置 + 半径 × (u·cosθ + v·sinθ)" 上（u ⊥ d、v ⊥ d，d = 主波运动方向），
 *       θ = 初始角 + 每 tick 2π/20（1 圈/秒）。主波消散时它一起消散。</li>
 *   <li><b>命中附加嬗乱</b>：60 tick / amplifier 0（本仓"命中附加嬗乱"的既有惯例）。</li>
 * </ol>
 *
 * <h2>为什么不需要自己的 {@code createChildWave}</h2>
 * <p>本类只被技能发射，不参与差波器的均摊分发；{@code createChildWave} 仍按同类型造子波
 * （万一将来有机器要分裂它，也不会突然变成一个没有技能行为的普通波）。</p>
 *
 * <h2>★ 客户端渲染能否正确的关键</h2>
 * <p>环绕波的位置每 tick 由主波位置决定，而位置<b>不能</b>靠普通字段同步（字段不同步）。
 * 因此父波 UUID、半径、初始角全部走 {@link SynchedEntityData}，<b>两侧用同一个算式各自算</b>
 * （{@link #orbitPos(Vec3, Vec3, double, double)}）—— 服务端算出来是权威判定位置，
 * 客户端算出来是渲染位置，两边不会漂。这条只能进游戏实测，静态关卡看不到。</p>
 *
 * @since 1.0.0
 */
public class StarShockWaveEntity extends ChargerWaveEntity {

	/**
	 * <b>环绕半径</b>（格）：需求 §3.3(g) 推断值 #3 取 <b>0.80</b> ——
	 * 与 {@code ArmorSkillFx#RING_RADIUS}（临域充力那两圈粒子）<b>同值</b>，观感一致。
	 */
	public static final double ORBIT_RADIUS = 0.80D;

	/**
	 * <b>环绕角速度</b>（弧度/tick）：需求 §3.3(g) 推断值 #4 取 <b>1 圈/秒</b>
	 * ⇒ 每 tick {@code 2π/20}。
	 */
	public static final double ORBIT_RADIANS_PER_TICK = Math.PI * 2.0D / 20.0D;

	/** 父波 UUID 的两半（{@code null} = 本波不是环绕波，就是一枚普通主波/分叉波）。 */
	private static final EntityDataAccessor<Integer> ORBIT_PARENT_MOST =
		SynchedEntityData.defineId(StarShockWaveEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ORBIT_PARENT_LEAST =
		SynchedEntityData.defineId(StarShockWaveEntity.class, EntityDataSerializers.INT);

	/** 环绕波的初始相位角（弧度）——"这颗环绕波从主波的哪一侧开始转"由发射那一刻定死。 */
	private static final EntityDataAccessor<Float> ORBIT_START_ANGLE =
		SynchedEntityData.defineId(StarShockWaveEntity.class, EntityDataSerializers.FLOAT);

	/** 环绕半径（同步值：将来若要按等级/概率调半径，只需在发射点写一次，两侧跟着变）。 */
	private static final EntityDataAccessor<Float> ORBIT_RADIUS_DATA =
		SynchedEntityData.defineId(StarShockWaveEntity.class, EntityDataSerializers.FLOAT);

	/** 命中附加的嬗乱时长（tick）——本仓"命中附加嬗乱"的既有惯例 60（3 秒）。 */
	private static final int DISORDER_TICKS = 60;

	/** 命中附加的嬗乱等级（amplifier）——同样沿用惯例 0（= 显示为 I 级）。 */
	private static final int DISORDER_AMPLIFIER = 0;

	public StarShockWaveEntity(EntityType<?> type, Level level) {
		super(type, level);
	}

	/**
	 * 主波 / 分叉波构造（不带环绕信息）。
	 *
	 * @param level      世界
	 * @param pos        出生位置
	 * @param movementDir 飞行方向（准心方向）
	 * @param waveLevel  波等级（本模组技能里 = 星芒嬗震的技能等级 1~3；伤害另走技能表，见 {@link #getDamage()}）
	 */
	public StarShockWaveEntity(Level level, Vec3 pos, Vec3 movementDir, int waveLevel) {
		super(AllEntityTypes.STAR_SHOCK_WAVE.get(), level, pos, movementDir, waveLevel);
		// 技能波恒为"攻击"波型（需求 §3.3(c)：波型/属性 = 攻击态）。
		// trySetWaveType 只允许从 NORMAL 变一次，构造期正好是 NORMAL ⇒ 这里一定成功。
		trySetWaveType(WaveTypes.ATTACK);
	}

	/**
	 * 环绕波构造（带父波与相位）。
	 *
	 * @param level      世界
	 * @param pos        出生位置（调用方已按圆周点算好，避免出生瞬间跳一格）
	 * @param movementDir 主波的运动方向（本波自己并不沿它飞，位置由 {@link #afterMove()} 改写）
	 * @param waveLevel  主波的波等级（伤害与主波同级）
	 * @param parentUuid 主波 UUID（服务端实例的 {@link net.minecraft.world.entity.Entity#getUUID()}）
	 * @param startAngle 初始相位（弧度）
	 */
	public StarShockWaveEntity(Level level, Vec3 pos, Vec3 movementDir, int waveLevel,
							   java.util.UUID parentUuid, double startAngle) {
		super(AllEntityTypes.STAR_SHOCK_WAVE.get(), level, pos, movementDir, waveLevel);
		trySetWaveType(WaveTypes.ATTACK);
		setOrbitParent(parentUuid);
		this.entityData.set(ORBIT_START_ANGLE, (float) startAngle);
		this.entityData.set(ORBIT_RADIUS_DATA, (float) ORBIT_RADIUS);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ORBIT_PARENT_MOST, 0);
		builder.define(ORBIT_PARENT_LEAST, 0);
		builder.define(ORBIT_START_ANGLE, 0f);
		builder.define(ORBIT_RADIUS_DATA, (float) ORBIT_RADIUS);
	}

	// ================== 伤害（技能表，不动全局表） ==================

	/**
	 * 命中生物伤害 = {@code StarShockConfigs} 里该等级的主波伤害（8 / 10 / 11）。
	 *
	 * <p><b>为什么覆写而不是给基类加参数</b>：基类那个 {@code waveLevel} 同时喂给三处 ——
	 * 颜色（α 黄/β 绿/γ 蓝）、波速（2/4/6 格/秒）、以及爆炸等级。技能伤害与"波等级"不是同一个量，
	 * 覆写 {@code getDamage()} 只改伤害这一处，波速/颜色/爆炸等级照旧由等级决定（与"沿用现有
	 * 攻击态波默认值"那条口径一致）。</p>
	 */
	@Override
	protected float getDamage() {
		return StarShockConfigs.damage(super.getWaveLevel());
	}

	/**
	 * 命中生物之后：附加<b>嬗乱</b>（{@code createoreexpansion:transmutation_disorder}，
	 * 60 tick / amplifier 0 —— 需求 §3.3(d) 与本仓既有惯例逐字一致）。
	 *
	 * <p>⚠ <b>星辉石凝能佩免疫嬗乱是既有设计，不是本技能的 bug</b>（需求 §3.3(h)）：
	 * 那条豁免在 {@code MedallionEffectHandler} 里按"效果类型"拦截，本技能不绕它、
	 * 也不该绕它 —— 打这类目标时伤害照常、嬗乱不生效。</p>
	 *
	 * <p>直接传 {@code DeferredHolder}（不 {@code .get()}）：它本身就是
	 * {@code Holder<MobEffect>}，与 {@code JadeTopazBowEventHandler#addEffect} 同一形状；
	 * {@code .get()} 拿到的是具体实现类，反而塞不进 {@code Holder<MobEffect>} 形参。</p>
	 */
	@Override
	protected void onLivingEntityHit(LivingEntity target) {
		target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
			com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationEffects
				.TRANSMUTATION_DISORDER,
			DISORDER_TICKS, DISORDER_AMPLIFIER));
	}

	// ================== 环绕波 ==================

	/** 标记本波为"环绕 parentUuid 的环绕波"；{@code null} 清除（回到普通波）。 */
	private void setOrbitParent(@Nullable java.util.UUID parentUuid) {
		long most = parentUuid == null ? 0L : parentUuid.getMostSignificantBits();
		long least = parentUuid == null ? 0L : parentUuid.getLeastSignificantBits();
		if (parentUuid != null && (int) (most >>> 32) == 0 && (int) (least >>> 32) == 0) {
			// 重建出来会是全零（= 同步值的两个 int 都是 0）= 被当成"没有父波"。
			// 这种 UUID 实际不可能出现（UUID v4 随机），但把它抬成 most=1 只花一行，
			// 换掉"环绕波静默退化成普通波"这一整类歧义。
			most = 1L << 32;
		}
		this.entityData.set(ORBIT_PARENT_MOST, (int) (most >>> 32));
		this.entityData.set(ORBIT_PARENT_LEAST, (int) (least >>> 32));
	}

	/**
	 * 本波环绕的那颗主波（{@code null} = 不是环绕波）。
	 *
	 * <p>从<b>两半 int</b>重建 UUID —— 同步字段没有 UUID 序列化器，所以父波身份用
	 * "UUID 的最高 32 位 + 次高 32 位"承载（两个 {@code >>> 32} 的 int）。
	 * <b>位宽说明（我定的，可一句话改）</b>：比较只用这 64 位；同一维度内同时存在的波数量
	 * 远小于碰撞概率可忽略的量级。真要严格，改成再补两个 int（低 64 位）即可，判据口径不变。</p>
	 *
	 * <p><b>全零 UUID 的兜底</b>：同步值的两个 int 都是 0 时本方法返回 {@code null}（= 不是环绕波），
	 * 所以"重建出来恰好是全零 UUID"的主波会被当成没有父波。这种主波的环绕波会退化成普通技能波
	 * （单独飞、按寿命消散，不会崩），但为了避免任何歧义，写入侧把那一种情况抬成 {@code most = 1}。</p>
	 */
	public @Nullable java.util.UUID getOrbitParent() {
		long hi = Integer.toUnsignedLong(this.entityData.get(ORBIT_PARENT_MOST));
		long lo = Integer.toUnsignedLong(this.entityData.get(ORBIT_PARENT_LEAST));
		long most = (hi << 32);
		long least = (lo << 32);
		if (most == 0L && least == 0L) {
			return null;
		}
		return new java.util.UUID(most, least);
	}

	/** 本波是不是环绕波。 */
	public boolean isOrbiter() {
		return getOrbitParent() != null;
	}

	/**
	 * <b>环绕位置的唯一算式</b>（服务端判定与客户端渲染共用同一个方法）。
	 *
	 * <pre>
	 * d = 主波运动方向（单位向量）
	 * u = perpendicularUnit(d)          // 与 d 垂直的单位向量（退化安全）
	 * v = d × u                         // (u, v, d) 右手系
	 * p = 主波位置 + r · (u·cosθ + v·sinθ)，  θ = 初始角 + 存活 tick 数 × 2π/20
	 * </pre>
	 *
	 * <p>{@code u} 与 {@code v} 都垂直于 {@code d} ⇒ 环绕波正好落在<b>垂直于主波运动方向的平面</b>里
	 * （需求 §3.3(g)"在主波轨迹的垂直平面内绕主波做圆周运动"）。</p>
	 *
	 * @param parentPos 主波当前位置
	 * @param direction 主波运动方向（未归一化也可以，内部归一化）
	 * @param radius    环绕半径（格）
	 * @param angle     本 tick 的角度 θ（弧度）
	 */
	public static Vec3 orbitPos(Vec3 parentPos, Vec3 direction, double radius, double angle) {
		Vec3 d = direction.lengthSqr() < 1.0E-9D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
		Vec3 u = perpendicularUnit(d);
		Vec3 v = d.cross(u).normalize();
		return parentPos.add(u.scale(Math.cos(angle) * radius)).add(v.scale(Math.sin(angle) * radius));
	}

	/**
	 * <b>垂直于 {@code axis} 的单位向量</b>，退化安全：取三个分量里绝对值最小的那个世界基向量当参考，
	 * 再 {@code axis × reference} 归一化。因为 {@code |axis·ref| ≤ 1/√3}，
	 * {@code |axis × ref| ≥ √(2/3)}，任何方向都不会退化成零向量。
	 *
	 * <p>与 {@code ArmorSkillFx#perpendicularUnit} 同一套做法（那里给临域充力的粒子环用）。
	 * 刻意不跨模块复用那个私有方法：波实体在 {@code content/charger/entity}，
	 * 粒子助手在 {@code content/equipment/armor}，为一个纯几何函数拉一条依赖不划算；
	 * 两处都是 12 行的纯函数，且各自的关卡/注释都说明了退化安全的口径。</p>
	 */
	private static Vec3 perpendicularUnit(Vec3 axis) {
		double ax = Math.abs(axis.x);
		double ay = Math.abs(axis.y);
		double az = Math.abs(axis.z);
		Vec3 reference;
		if (ax <= ay && ax <= az) {
			reference = new Vec3(1.0D, 0.0D, 0.0D);
		} else if (ay <= az) {
			reference = new Vec3(0.0D, 1.0D, 0.0D);
		} else {
			reference = new Vec3(0.0D, 0.0D, 1.0D);
		}
		return axis.cross(reference).normalize();
	}

	/**
	 * 位置改写：环绕波每 tick 把自己摆到圆周点上。
	 *
	 * <p>父波找不到（已消散 / 尚未同步到客户端）⇒ 本 tick 不动位置，也不自行消散：
	 * 真正的"一起收尾"由 {@link #onRemoved} 在主波侧触发（主波消散时主动 discard 自己的环绕波），
	 * 这条只是兜底 —— 万一主波以我们没覆盖到的路径消失，环绕波仍会按自己的寿命上限自然消散，
	 * 不会留下永久实体。</p>
	 */
	@Override
	protected void afterMove() {
		if (!isOrbiter()) {
			return;
		}
		ChargerWaveEntity parent = resolveParent();
		if (parent == null) {
			return;
		}
		double angle = orbitStartAngle() + tickCount * ORBIT_RADIANS_PER_TICK;
		Vec3 target = orbitPos(parent.position(), parent.getMovement(), orbitRadius(), angle);
		setPos(target);
	}

	/** 环绕波的初始相位（弧度）。 */
	private double orbitStartAngle() {
		return this.entityData.get(ORBIT_START_ANGLE);
	}

	/** 环绕半径（格）。 */
	private double orbitRadius() {
		float r = this.entityData.get(ORBIT_RADIUS_DATA);
		return r <= 0f ? ORBIT_RADIUS : r;
	}

	/** 解析主波实体（服务端 O(1) 查 UUID，客户端退化成一次本地遍历）。 */
	private @Nullable ChargerWaveEntity resolveParent() {
		java.util.UUID parentId = getOrbitParent();
		if (parentId == null || level() == null) {
			return null;
		}
		if (level() instanceof net.minecraft.server.level.ServerLevel server) {
			// 服务端：UUID → 实体的索引查询（不要在这里遍历实体表，环绕波每 tick 都要问一次）
			net.minecraft.world.entity.Entity found = server.getEntity(parentId);
			return found instanceof ChargerWaveEntity wave && wave.isAlive() && !wave.isRemoved()
				? wave
				: null;
		}
		// 客户端：没有按 UUID 的索引，退而按"父波必然在环绕半径附近"查一次
		// （环绕半径 0.8 格，搜索盒取 2 格已远超需要）。同步到之前返回 null，
		// 那一 tick 环绕波停在原地 —— 下 tick 父波同步到了就继续转。
		for (ChargerWaveEntity wave : level().getEntitiesOfClass(ChargerWaveEntity.class,
				getBoundingBox().inflate(2.0D))) {
			if (parentId.equals(wave.getUUID()) && wave.isAlive() && !wave.isRemoved()) {
				return wave;
			}
		}
		return null;
	}

	/**
	 * 主波消散 ⇒ 连带收尾它自己的环绕波（需求 §3.3(g)："主波被湮灭 / 到寿 / 消失时，
	 * 其环绕波必须一起收尾，不许留下孤立波"）。
	 *
	 * <p>用 {@code level().getEntitiesOfClass(StarShockWaveEntity.class, bigBox)} 找"环绕我的那些"：
	 * 环绕半径只有 0.8 格，碰撞盒 0.2，所以范围取 4 格已远超需要；再按
	 * {@link #getOrbitParent()} 精确比对 UUID，不会误伤同一批次里的别的环绕波
	 * （它们环绕的是<b>别的主波</b>）。</p>
	 *
	 * <p>为什么在 {@code remove} 上做而不是逐个调用点：波有六七条消散路径（撞墙、撞生物、
	 * 撞掉落物、到寿、距离上限、被别的波湮灭、{@code /kill}），挂在 {@code remove} 上一次覆盖。</p>
	 */
	@Override
	protected void onRemoved(RemovalReason reason) {
		if (isOrbiter() || level() == null || level().isClientSide) {
			return;
		}
		java.util.UUID me = getUUID();
		net.minecraft.world.phys.AABB box = getBoundingBox().inflate(4.0D);
		for (StarShockWaveEntity child : level().getEntitiesOfClass(StarShockWaveEntity.class, box)) {
			if (child == this || child.isRemoved()) {
				continue;
			}
			if (me.equals(child.getOrbitParent())) {
				child.discard();
			}
		}
	}

	/**
	 * 差器均摊分发：造同类型的降级子波。
	 *
	 * <p>本类目前只被技能发射，不走差器路径；这里仍返回同类型，保证"万一被分裂"时
	 * 分裂出来的仍是技能波（带技能伤害与嬗乱），而不是悄悄退化成普通机器波。</p>
	 */
	@Override
	public AbstractChargerWaveEntity createChildWave(Vec3 pos, Vec3 dir, int level, int index, int total) {
		StarShockWaveEntity child = new StarShockWaveEntity(level(), pos, dir, level);
		child.setFiringBatch(getFiringBatch());
		return child;
	}
}
