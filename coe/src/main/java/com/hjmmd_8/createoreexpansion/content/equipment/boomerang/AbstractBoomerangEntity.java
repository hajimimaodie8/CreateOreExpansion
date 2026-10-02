package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * <b>回旋镖实体基类</b>（四把镖共用：投掷 → 去程 → 回程 → 捡物 → 挖方块）。
 *
 * <h2>一、为什么是 {@link Projectile}，不是我们的波实体</h2>
 * <p>波实体（{@code AbstractChargerWaveEntity}）{@code extends Entity}，语义是"能量波"——由五要素定义、
 * 与波口/载荷/加工绑定。镖是<b>投射物</b>：它属于某个玩家、能被拾回、掉落归属要走原版。
 * 所以这里<b>直接继承 {@link Projectile}</b>（于是白拿三件事：owner 的标准持久化、
 * {@code leftOwner} 的射出保护、{@code lerpRotation} 的朝向平滑）。</p>
 *
 * <h2>二、照抄 Quark Pickarang/Flamerang 的四段，并修掉它的三个 bug</h2>
 * <ol>
 *   <li><b>去程</b>：{@link #checkImpact()} 先射线查实体（{@link ProjectileUtil#getEntityHitResult}，
 *       AABB 用 {@code getBoundingBox().expandTowards(速度).inflate(1)}），再查方块
 *       （{@code level().clip(COLLIDER, Fluid.NONE)}）；命中方块 ⇒ 尝试挖掉 + 立刻转回程。
 *       交替循环上限 {@link #MAX_IMPACT_LOOPS}（超了写日志，不崩、不死循环）。
 *       <b>另外每 tick 判一次"飞太远"</b>：离主人超过该档的收回距离
 *       （{@link BoomerangTier#returnDistance()}）就<b>立刻掉头</b> —— 作者 2026-10-02 报的
 *       "扔远了会自动消失"就是缺这一条，执行处 {@link #outboundRangeExceeded}。</li>
 *   <li><b>位移</b>：手写 {@code setPos(pos + 速度)}，<b>不用</b> {@code move()}/{@code lerpMotion}；
 *       阻力陆地 {@value #AIR_DRAG} / 水中 {@value #WATER_DRAG}；朝向由速度反算并 lerp 平滑
 *       （{@code Projectile#updateRotation}，原版的 atan2 + lerp，不重写一遍）。</li>
 *   <li><b>回程</b>：{@code noPhysics = true} + 朝 {@code owner.position() + (0,1,0)} 归一化转向，
 *       速度 {@value #RETURN_SPEED}（+ 效率加成，本模组恒 0）；抵达判定见
 *       {@link #RETURN_ARRIVE_SQR} 的注释。</li>
 *   <li><b>挖方块</b>：把镖<b>临时塞进</b> {@code player.getInventory().selected} +
 *       {@code setItemInHand(MAIN_HAND)} → 复刻原版挖掘进度 → 与该档的 maxHardness 比 →
 *       {@code player.gameMode.destroyBlock(pos)}（<b>唯一破坏入口</b>：权限/时运/掉落归属全交给原版）
 *       → {@code finally} 还原。掉落就是世界里普通的 {@link ItemEntity}，镖<b>不存</b>内部库存。</li>
 * </ol>
 *
 * <p><b>修掉的三个 Quark bug</b>（需求 8）：</p>
 * <ul>
 *   <li><b>a. 存档</b>：{@link #readAdditionalSaveData}/{@link #addAdditionalSaveData} <b>必须调
 *       {@code super}</b> —— owner 的标准持久化就在 {@code Projectile} 里
 *       （{@code Owner} UUID + {@code LeftOwner}）。少这一行，区块重载后 owner 解析不到，
 *       镖在第一个 tick 就走"主人没了"的兜底掉在地上。<b>并且没有自己另存一份 owner</b>。</li>
 *   <li><b>b. 回程超时</b>：Quark 把超时写在<b>去程</b>分支里，玩家持续远离时它永远追不上、
 *       永不消散还穿墙。这里回程有<b>自己的寿命</b> {@link #MAX_RETURN_TICKS}（外加去程上限
 *       {@link #MAX_OUTBOUND_TICKS}，防止"打不到任何方块"时永远飞下去）。超时后
 *       <b>就地落地</b>（{@code spawnAtLocation}）再消散 —— 宁可掉在远处，也不让物品蒸发。
 *       <b>与"距离判据"的分工（作者 2026-10-02）</b>：去程正常结束靠<b>距离</b>
 *       （离主人超过该档收回距离 ⇒ 掉头，玩家可预期），时间上限只是<b>兜底</b>。</li>
 *   <li><b>c. 属性修饰符不泄漏</b>：Quark 在命中时两次 {@code addTransientAttributeModifiers}。
 *       这里<b>一处都没有</b>（挖掘走原版 {@code destroyBlock}，伤害走 {@code hurt}，
 *       全程不碰 {@code AttributeMap}）。</li>
 * </ul>
 *
 * <h2>三、同步与存档（需求 11）</h2>
 * <ul>
 *   <li>{@link #DATA_STACK}（{@code ITEM_STACK}）——渲染器要画它，客户端必须拿得到；
 *       同时它也是"交还给玩家"的那一份（能量已在投掷/挖掘时扣掉）。</li>
 *   <li>{@link #DATA_RETURNING}（{@code BOOLEAN}）——客户端 tick 也要走回程分支（否则回程只能
 *       靠每 {@code updateInterval} tick 一次的位置包，看起来一顿一顿）。</li>
 *   <li>NBT：{@code liveTime} / {@code returnTicks} / {@code hitCount} / {@code slot} / 投掷原点
 *       （{@code ThrowOriginX/Y/Z}，见 {@link #recordThrowOrigin}）/ 镖本身。
 *       {@code entitiesHit} <b>只在内存</b>（它只用来防止同一次飞行里重复打同一只怪）。</li>
 * </ul>
 *
 * <h2>四、构造期陷阱（AGENTS.md 第 4 条）</h2>
 * <p>{@code Entity} 的构造器会调 {@code defineSynchedData} / {@code setPos} 等可覆写方法，
 * 而字段初始化器在<b>之后</b>才跑。因此 {@link #defineSynchedData} 与 {@link #tier()} 都不许碰
 * 对象字段：{@code defineSynchedData} 只用静态成员与参数，{@link #tier()} 由子类返回枚举常量。
 * 本类<b>不覆写</b> {@code setPos}/{@code getBoundingBox}/{@code defineSynchedData} 以外的构造期方法。</p>
 */
public abstract class AbstractBoomerangEntity extends Projectile {

	/** 渲染与交还都用的那一份镖（同步数据；写入只有 {@link #setItemStack} 一处）。 */
	private static final EntityDataAccessor<ItemStack> DATA_STACK =
		SynchedEntityData.defineId(AbstractBoomerangEntity.class, EntityDataSerializers.ITEM_STACK);

	/** 是否处于回程段（同步数据：客户端 tick 也读它）。 */
	private static final EntityDataAccessor<Boolean> DATA_RETURNING =
		SynchedEntityData.defineId(AbstractBoomerangEntity.class, EntityDataSerializers.BOOLEAN);

	/** 去程单 tick 内"实体 ⇄ 方块"交替判定的循环上限（需求 2；超了写日志，不崩）。 */
	public static final int MAX_IMPACT_LOOPS = 100;
	/**
	 * 去程寿命上限（tick）：<b>兜底判据</b>。
	 *
	 * <p>去程的正常结束是<b>距离</b>：离主人超过该档的收回距离（{@link BoomerangTier#returnDistance()}，
	 * 5/10/15/20 格）就立刻掉头，见 {@link #outboundRangeExceeded}。这一条时间上限只负责
	 * "距离判据万一失效"（例如主人始终贴身跟着、镖贴身绕圈那种极端）时也<b>绝不永远飞下去</b>。</p>
	 */
	public static final int MAX_OUTBOUND_TICKS = 200;
	/** 回程寿命上限（tick）：Quark bug b 的修复处，超了就落地消散。 */
	public static final int MAX_RETURN_TICKS = 300;
	/** 回程速度。 */
	public static final double RETURN_SPEED = 0.7D;
	/** 回程速度的效率加成系数（Quark 用 Efficiency 附魔等级；本模组第一批没有附魔通道 ⇒ 恒 0）。 */
	public static final double RETURN_SPEED_PER_EFFICIENCY = 0.325D;
	/**
	 * 抵达判定阈值 —— <b>「与主人的距离²」</b>（3.25 = 1.8²）。
	 *
	 * <p>需求原文写的是 {@code motion.lengthSqr() < 3.25}。逐字照抄会坏：回程速度恒为
	 * {@value #RETURN_SPEED} ⇒ {@code |motion|² = 0.49 < 3.25} 恒成立，镖会在<b>第一个回程 tick
	 * 就判定"已到达"</b>（捡不到路上的东西、也回不到玩家手上）。因此按同一组常数的几何含义读作
	 * "离主人还剩多远"（平方比较，省一次开方）。</p>
	 */
	public static final double RETURN_ARRIVE_SQR = 3.25D;
	/** 抵达判定的效率加成系数（同上，恒 0）。 */
	public static final double RETURN_ARRIVE_SQR_PER_EFFICIENCY = 0.25D;
	/** 本模组第一批的效率加成恒 0（没有附魔通道；留着是为了让公式与 Quark 逐字对应）。 */
	public static final double RETURN_EFFICIENCY = 0.0D;
	/** 陆地阻力。 */
	public static final double AIR_DRAG = 0.99D;
	/** 水中阻力。 */
	public static final double WATER_DRAG = 0.8D;
	/** 回程捡物的扫描半径（以自身碰撞盒外扩）。 */
	public static final double PICKUP_RADIUS = 2.0D;
	/** 捡到的掉落物上船后的拾取延迟（tick）——防止它刚贴上就被路过的玩家顺手吸走。 */
	public static final int PICKUP_DELAY = 5;
	/** 乘客的骑乘位再下移这么多（需求 4）。 */
	public static final double PASSENGER_OFFSET_Y = 0.4D;

	/** 存活 tick（去程 + 回程；进 NBT）。 */
	private int liveTime;
	/** 回程段已飞 tick（进 NBT —— 超时判定要能跨存档继续数）。 */
	private int returnTicks;
	/** 本次飞行命中生物的只数（进 NBT；第二批技能的"穿刺"要用）。 */
	private int hitCount;
	/** 投掷时记下的背包槽位：回程交还优先还回这一格（进 NBT）。 */
	private int slot;
	/**
	 * 投掷原点（进 NBT —— 区块重载后不丢）。
	 *
	 * <p>当下<b>只作为备用基准</b>：收回距离按"与主人的距离"算（作者 2026-10-02 裁定）。
	 * 想改成按原点算，只改 {@link #outboundRangeExceeded} 里那一行。</p>
	 */
	private double originX;
	private double originY;
	private double originZ;
	/** 投掷原点是否已记录（服务端第一条去程 tick 置位，重载时由 NBT 键的存在与否恢复）。 */
	private boolean originRecorded;
	/** 本次飞行已经打过的实体 id（<b>只在内存</b>，防止同一只怪被同一把镖反复打）。 */
	private final Set<Integer> entitiesHit = new HashSet<>();

	protected AbstractBoomerangEntity(EntityType<? extends AbstractBoomerangEntity> type, Level level) {
		super(type, level);
	}

	/** 这一把镖的数值档（子类只提供它；构造期绝不被调用）。 */
	protected abstract BoomerangTier tier();

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_STACK, ItemStack.EMPTY);
		builder.define(DATA_RETURNING, false);
	}

	// ================= 同步数据读写 =================

	/** 渲染与交还用的镖（永远不是 EMPTY 之外的引用共享：读方要改先 {@code copy()}）。 */
	public ItemStack getItemStack() {
		return this.entityData.get(DATA_STACK);
	}

	public void setItemStack(ItemStack stack) {
		this.entityData.set(DATA_STACK, stack.copy());
	}

	public boolean isReturning() {
		return this.entityData.get(DATA_RETURNING);
	}

	/**
	 * 切换去程/回程（唯一入口）。
	 *
	 * <p>切到回程时同时做两件事：{@code noPhysics = true}（穿墙回手——<b>但必须有回程寿命，
	 * 见 {@link #MAX_RETURN_TICKS}，否则就是 Quark 那个"永远穿墙追不上"的 bug</b>）
	 * 与回程计时归零。</p>
	 */
	public void setReturning(boolean returning) {
		if (returning && !this.entityData.get(DATA_RETURNING)) {
			this.noPhysics = true;
			this.returnTicks = 0;
		}
		this.entityData.set(DATA_RETURNING, returning);
	}

	/** 投掷时记下的背包槽位（回程交还优先还回这里）。 */
	public int getSlot() {
		return slot;
	}

	public void setSlot(int slot) {
		this.slot = slot;
	}

	public int getHitCount() {
		return hitCount;
	}

	// ================= tick 骨架 =================

	@Override
	public void tick() {
		// super 一定要调：Projectile 在这里补 gameEvent(射出了) 与 leftOwner（射出保护），
		// Entity 在这里补 baseTick（火焰/传送门/上一 tick 的朝向 xRotO/yRotO —— lerpRotation 靠它）。
		super.tick();
		this.liveTime++;

		Entity owner = getOwner();
		if (!level().isClientSide && (owner == null || !owner.isAlive())) {
			ownerGone();
			return;
		}

		if (this.entityData.get(DATA_RETURNING)) {
			this.noPhysics = true; // 客户端也置上：它只驱动 isInWall 之类的原版分支，与我们的手写位移无关
			if (!level().isClientSide) {
				this.returnTicks++;
				if (this.returnTicks > MAX_RETURN_TICKS) {
					returnTimedOut();
					return;
				}
			}
			tickReturning(owner);
		} else {
			if (!level().isClientSide && !this.originRecorded) {
				recordThrowOrigin(); // 投掷原点只在服务端记一次，进 NBT（客户端用不到它）
			}
			if (!level().isClientSide && outboundRangeExceeded(owner)) {
				// 主判据（作者 2026-10-02 报的"扔远了会自动消失"）：飞过本档收回距离 ⇒ 立刻掉头。
				// 不是消失、也不是掉在地上 —— 交给既有回程段（RETURNING）把镖送回主人手里。
				setReturning(true);
			} else if (!level().isClientSide && this.liveTime > MAX_OUTBOUND_TICKS) {
				// 兜底（分工见 MAX_OUTBOUND_TICKS 的注释）：距离判据万一失效，也不许永远飞下去。
				setReturning(true);
			} else if (tickOutbound()) {
				return; // 本 tick 刚命中方块并转入回程：不再前进，免得钻进墙里
			}
		}

		if (!level().isClientSide && this.entityData.get(DATA_RETURNING)) {
			pickUpItems();
		}
	}

	/** 去程：命中判定 + 位移。返回 true 表示"本 tick 已转入回程，别再前进"。 */
	private boolean tickOutbound() {
		if (!level().isClientSide && checkImpact()) {
			return true;
		}
		Vec3 motion = getDeltaMovement();
		double drag = isInWater() ? WATER_DRAG : AIR_DRAG;
		setDeltaMovement(motion.scale(drag));
		setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
		updateRotation();
		return false;
	}

	/**
	 * 记下投掷原点（<b>只调一次</b>：服务端第一条去程 tick）。
	 *
	 * <p>取"第一条去程 tick 的位置"而不是"投掷那一瞬间的那一点"：实体的出生点<b>就是</b>投掷点
	 * （{@code BoomerangItem#use} 里的 {@code setPos(player.getX(), player.getEyeY() - 0.1, player.getZ())}），
	 * 出生到第一次 tick 之间没有任何位移，两者等价 —— 这样<b>不必改物品类</b>，
	 * 也让"重载后不丢"只靠 NBT 这一处。</p>
	 */
	private void recordThrowOrigin() {
		this.originX = getX();
		this.originY = getY();
		this.originZ = getZ();
		this.originRecorded = true;
	}

	/**
	 * 去程"飞太远就掉头"的判据（<b>唯一判据处</b>；作者 2026-10-02 报的"扔远了会自动消失"）。
	 *
	 * <p><b>基准 = 与主人的距离</b>（作者裁定）：主人往后退，镖更早回头 —— 这是"收回距离"的
	 * 直觉读法。基准点取 {@code owner.position() + (0,1,0)}，与回程的目标点（{@link #tickReturning}）
	 * <b>是同一点</b>，于是"距离² &lt; 3.25 ⇒ 已到家"与"距离 &gt; 该档收回距离 ⇒ 掉头"共用同一参照物。</p>
	 *
	 * <p>阈值一律来自 {@link BoomerangTier#returnDistance()}（5/10/15/20 格）——<b>这里不许出现
	 * 距离字面量</b>。{@code owner} 非空由 {@link #tick()} 顶部的兜底保证（主人没了/死了先走
	 * {@code ownerGone()}：落地 + 消散，<b>不</b>走这里）。</p>
	 *
	 * <p><b>若要改成按投掷原点判</b>（原点已在 {@link #recordThrowOrigin} 记下并进 NBT）：
	 * 只改下面那一行 {@code distSqr}，换成 {@code position().distanceToSqr(originX, originY, originZ)} 即可。</p>
	 */
	private boolean outboundRangeExceeded(Entity owner) {
		int limit = tier().returnDistance();
		// ⇩ 基准行（要改成按投掷原点判，只改这一行）
		double distSqr = position().distanceToSqr(owner.position().add(0.0D, 1.0D, 0.0D));
		return distSqr > (double) limit * limit;
	}

	/** 回程：朝主人头顶归一化转向 + 位移；抵达就交还。 */
	private void tickReturning(Entity owner) {
		if (owner == null) {
			return; // 客户端可能暂时解析不到主人；服务端的 null 已在 tick() 里走兜底
		}
		Vec3 target = owner.position().add(0.0D, 1.0D, 0.0D);
		Vec3 delta = target.subtract(position());
		if (delta.lengthSqr() < RETURN_ARRIVE_SQR + RETURN_EFFICIENCY * RETURN_ARRIVE_SQR_PER_EFFICIENCY) {
			if (!level().isClientSide) {
				collect(owner);
			}
			return;
		}
		Vec3 step = delta.normalize().scale(RETURN_SPEED + RETURN_EFFICIENCY * RETURN_SPEED_PER_EFFICIENCY);
		setDeltaMovement(step);
		setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
		updateRotation();
	}

	// ================= 去程命中判定 =================

	/**
	 * 去程命中判定（需求 2）：实体射线 + 方块射线，<b>取更近的那一个</b>。
	 *
	 * <p>⚠ 与需求原文的差异（我独创的一处）：原文是"先查实体，没实体再查方块"。那样会
	 * <b>隔着墙打到墙后的生物</b>（Quark 的镖本来不伤害生物，所以它无所谓；我们加了伤害就有关了）。
	 * 两条射线都做、按距离取近的，才既打得到生物又不穿墙。</p>
	 *
	 * <p>循环（"交替"）：打完一只生物后从命中点继续往前查，一 tick 内可以连续穿刺多只，
	 * 上限 {@link #MAX_IMPACT_LOOPS}；同一只生物靠 {@code entitiesHit} 去重。</p>
	 *
	 * @return true = 本 tick 命中了方块并已转入回程
	 */
	private boolean checkImpact() {
		Vec3 motion = getDeltaMovement();
		if (motion.lengthSqr() < 1.0E-7D) {
			return false;
		}
		Vec3 start = position();
		for (int loop = 0; loop < MAX_IMPACT_LOOPS; loop++) {
			Vec3 end = start.add(motion);
			AABB box = getBoundingBox().expandTowards(motion).inflate(1.0D);
			EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
				level(), this, start, end, box, this::canHitEntity);
			BlockHitResult blockHit = level().clip(
				new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

			boolean hasBlock = blockHit.getType() != HitResult.Type.MISS;
			boolean blockIsCloser = hasBlock && (entityHit == null
				|| start.distanceToSqr(blockHit.getLocation()) <= start.distanceToSqr(entityHit.getLocation()));

			if (blockIsCloser) {
				onHitBlock(blockHit.getBlockPos());
				return true;
			}
			if (entityHit == null) {
				return false;
			}
			if (!onHitEntity(entityHit.getEntity())) {
				return false; // 已经打过 ⇒ 本 tick 收手（否则同一个命中点会无限重来）
			}
			start = entityHit.getLocation();
		}
		// 需求 2：超上限"打日志别崩"（不抛、不递归）
		CoeCore.LOGGER.warn("[回旋镖] 单 tick 命中判定超过 {} 次，结束本次去程判定：{}", MAX_IMPACT_LOOPS, this);
		return false;
	}

	/** 命中生物：只伤一次、记个数。返回 false 表示"这只已经打过了"。 */
	private boolean onHitEntity(Entity target) {
		if (target == getOwner() || !entitiesHit.add(target.getId())) {
			return false;
		}
		// 伤害源：跟能量波同一处口径（indirectMagic(this, null)），**不引 mixin、也不冒充玩家攻击**
		// （需求 9：要"玩家攻击"语义的话必须先问作者）。
		target.hurt(damageSources().indirectMagic(this, null), tier().damage());
		hitCount++;
		return true;
	}

	/** 命中方块：能挖就挖，然后不管挖没挖动都转回程（镖撞墙 = 回来）。 */
	private void onHitBlock(BlockPos pos) {
		mineBlock(pos);
		setReturning(true);
	}

	// ================= 挖方块（照搬 Quark 最精妙的那一段） =================

	/**
	 * 尝试挖掉 {@code pos} 上的方块。
	 *
	 * <p>流程（顺序不能改）：</p>
	 * <ol>
	 *   <li>门槛：硬度 ≥ 0（基岩之类 -1 直接不动）、硬度 ≤ 该档 maxHardness、不在该档的
	 *       {@code INCORRECT_FOR_*_TOOL} 标签里（= 挖掘等级，与 {@code AllTiers} 同一判据）；</li>
	 *   <li>复刻原版挖掘进度 {@code digSpeed / (hardness * i)}，{@code i = 30/100}
	 *       ——{@code i} 由 {@code player.hasCorrectToolForDrops(state)} 决定，而它读的是
	 *       <b>主手</b>那一格，所以下面必须先把镖塞进手里；</li>
	 *   <li>临时把镖塞进 {@code inventory.selected} + {@code setItemInHand(MAIN_HAND)}，
	 *       {@code player.gameMode.destroyBlock(pos)}；</li>
	 *   <li>{@code finally} 还原那一格（<b>无论如何</b>都要还原，异常也不能把玩家的物品换掉）。</li>
	 * </ol>
	 *
	 * <p>能量：挖成功才扣 {@link ToolEnergy}（先 {@code canAfford} 预检，成功后再 {@code consume}
	 * ——预检与扣减都在 {@code ToolEnergy} 里，本类不自己读写组件）。</p>
	 */
	private void mineBlock(BlockPos pos) {
		if (!(getOwner() instanceof ServerPlayer player)) {
			return;
		}
		BlockState state = level().getBlockState(pos);
		if (state.isAir()) {
			return;
		}
		float hardness = state.getDestroySpeed(level(), pos);
		if (hardness < 0.0F) {
			return;
		}
		BoomerangTier tier = tier();
		if (hardness > tier.maxHardness()) {
			return;
		}
		if (state.is(tier.incorrectBlocks())) {
			return;
		}
		// 原版挖掘进度：一 tick 内进度 ≥ 1 才算挖开（i=30 正确工具 / i=100 用错工具）。
		// 这里用**位置敏感**的 hasCorrectToolForDrops —— 它就是 NeoForge 的 doPlayerHarvestCheck
		// （EventHooks.doPlayerHarvestCheck：先取原版单参判定的值，再过 PlayerEvent.HarvestCheck，
		// 让别的模组有机会否决）。单参那个重载在 NeoForge 里是 @Deprecated。
		int i = player.hasCorrectToolForDrops(state, level(), pos) ? 30 : 100;
		if (tier.digSpeed() / (hardness * i) < 1.0F) {
			return;
		}
		ItemStack stack = getItemStack();
		if (stack.isEmpty() || !ToolEnergy.canAfford(player, stack, tier.mineCost())) {
			return;
		}

		Inventory inventory = player.getInventory();
		int hotbar = inventory.selected;
		ItemStack saved = inventory.getItem(hotbar);
		inventory.setItem(hotbar, stack);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		boolean destroyed;
		try {
			// 唯一破坏入口：权限 / 时运 / 掉落归属 / 统计全交给原版（我们只负责"能不能挖"这一关）
			destroyed = player.gameMode.destroyBlock(pos);
		} finally {
			inventory.setItem(hotbar, saved);
			player.setItemInHand(InteractionHand.MAIN_HAND, saved);
		}
		if (destroyed) {
			// 耐久 → 能量：挖掉一个方块扣一次（扣在镖自己身上，回程交还的就是扣过的那一份）
			ToolEnergy.consume(player, stack, tier.mineCost());
			// consume 是就地改组件（改的是同步数据里那个对象），这里再写一次让 SynchedEntityData
			// 明确标脏 —— 客户端那份跟着更新，别只靠"交还时背包同步"这一条兜底。
			setItemStack(stack);
		}
	}

	// ================= 回程捡物 =================

	/**
	 * 回程每 tick 扫一次膨胀 {@value #PICKUP_RADIUS} 格：掉落物与经验球上船
	 * （{@code startRiding(this)} + 掉落物设拾取延迟 {@value #PICKUP_DELAY}）。
	 */
	private void pickUpItems() {
		AABB area = getBoundingBox().inflate(PICKUP_RADIUS);
		List<Entity> found = level().getEntitiesOfClass(Entity.class, area, e -> canCarry(e) && !e.isPassenger());
		for (Entity entity : found) {
			entity.startRiding(this);
			if (entity instanceof ItemEntity item) {
				item.setPickUpDelay(PICKUP_DELAY);
			}
		}
	}

	/** 只让掉落物与经验球上船（需求 4）。 */
	private static boolean canCarry(Entity entity) {
		return entity instanceof ItemEntity || entity instanceof ExperienceOrb;
	}

	/** 放行这两类乘客（默认实现只放行 {@code canRide} 的生命体）。 */
	@Override
	public boolean canAddPassenger(Entity passenger) {
		return canCarry(passenger);
	}

	/** 乘客骑乘位整体下移 {@value #PASSENGER_OFFSET_Y}（需求 4）。 */
	@Override
	public Vec3 getPassengerRidingPosition(Entity passenger) {
		return super.getPassengerRidingPosition(passenger).subtract(0.0D, PASSENGER_OFFSET_Y, 0.0D);
	}

	// ================= 交还与兜底 =================

	/**
	 * 抵达主人：乘客逐个 {@code playerTouch(player)}，镖本身走三步交还（需求 5）——
	 * 原槽空就放回原槽，否则 {@code inventory.add}，再不行 {@code player.drop}。
	 */
	private void collect(Entity owner) {
		if (owner instanceof Player player) {
			for (Entity passenger : new ArrayList<>(getPassengers())) {
				passenger.stopRiding();
				if (passenger instanceof ItemEntity item) {
					// 刚上船时设了拾取延迟；这里是我们主动交付，先把延迟清掉，
					// 否则 playerTouch 会因为延迟而什么都不做、东西就跟着镖一起消失了。
					item.setPickUpDelay(0);
				}
				passenger.playerTouch(player);
			}
			ItemStack stack = getItemStack().copy();
			if (!stack.isEmpty()) {
				giveToPlayer(player, stack);
			}
		} else {
			// 理论上到不了（只有玩家能投掷）；真到了也别把东西吞掉
			dropPassengers();
			ItemStack stack = getItemStack().copy();
			if (!stack.isEmpty()) {
				spawnAtLocation(stack, 0.0F);
			}
		}
		discard();
	}

	/** 交还三步（需求 5）：原槽 → 背包 → 掉在地上。 */
	private void giveToPlayer(Player player, ItemStack stack) {
		Inventory inventory = player.getInventory();
		// "原槽" = 投掷时记下的那一格（主手 = 当时的快捷栏格；副手 = 40 号副手格），
		// 不是"此刻选中的格子"——飞行途中玩家换格子是常事，换过就还错地方了。
		int slot = this.slot;
		if (slot >= 0 && slot < inventory.getContainerSize() && inventory.getItem(slot).isEmpty()) {
			inventory.setItem(slot, stack);
		} else if (!inventory.add(stack)) {
			player.drop(stack, false);
		}
	}

	/**
	 * owner 失效兜底（需求 6）：主人没了/死了 ⇒ 先把自己从墙里拔出来，再把镖丢在地上，然后消散。
	 * 宁可掉在墙上，也不让物品随实体一起消失。
	 */
	private void ownerGone() {
		while (isInWall()) {
			setPos(getX(), getY() + 1.0D, getZ());
		}
		dropPassengers();
		ItemStack stack = getItemStack().copy();
		if (!stack.isEmpty()) {
			spawnAtLocation(stack, 0.0F);
		}
		discard();
	}

	/**
	 * 回程超时的收尾（Quark bug b 的修复落点）：就地落地 + 消散。
	 *
	 * <p>不写 {@code discard()} 之外的花样：玩家跑得比镖快时，"追不上"的正确结果是
	 * <b>东西还在世界上</b>（原地掉落），而不是永远穿墙追、永不消散。</p>
	 */
	private void returnTimedOut() {
		CoeCore.LOGGER.debug("[回旋镖] 回程超时（{} tick）就地落地：{}", MAX_RETURN_TICKS, this);
		dropPassengers();
		ItemStack stack = getItemStack().copy();
		if (!stack.isEmpty()) {
			spawnAtLocation(stack, 0.0F);
		}
		discard();
	}

	/** 把还在船上的掉落物放回世界（别让镖一消散，乘客跟着一起蒸发）。 */
	private void dropPassengers() {
		for (Entity passenger : new ArrayList<>(getPassengers())) {
			passenger.stopRiding();
			if (passenger instanceof ItemEntity item && !item.getItem().isEmpty()) {
				spawnAtLocation(item.getItem(), 0.0F);
				item.discard();
			}
		}
	}

	// ================= 存档（bug a：必须调 super） =================

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		// ⛔ 这一行是 Quark bug a 的修复：owner（Owner UUID / LeftOwner）的标准持久化就在 super 里。
		super.readAdditionalSaveData(tag);
		this.liveTime = tag.getInt("LiveTime");
		this.returnTicks = tag.getInt("ReturnTicks");
		this.hitCount = tag.getInt("HitCount");
		this.slot = tag.getInt("Slot");
		// 投掷原点（可选键：键在 ⇒ 已记录。重载后不许重记，否则基准会被挪到重载点）
		if (tag.contains("ThrowOriginX")) {
			this.originX = tag.getDouble("ThrowOriginX");
			this.originY = tag.getDouble("ThrowOriginY");
			this.originZ = tag.getDouble("ThrowOriginZ");
			this.originRecorded = true;
		}
		// 镖本身（含扣过的能量）也要能跨区块重载；老存档没有该键时保持 EMPTY 的兜底形状。
		if (tag.contains("BoomerangStack")) {
			setItemStack(ItemStack.parseOptional(registryAccess(), tag.getCompound("BoomerangStack")));
		}
		// 回程段是同步值，但它同时驱动 noPhysics：重载后要把本侧的状态补齐
		if (this.entityData.get(DATA_RETURNING)) {
			this.noPhysics = true;
		}
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag); // 同上：owner 由 Projectile 存
		tag.putInt("LiveTime", this.liveTime);
		tag.putInt("ReturnTicks", this.returnTicks);
		tag.putInt("HitCount", this.hitCount);
		tag.putInt("Slot", this.slot);
		// 投掷原点：记过才写（没记过就不写键，读回来仍是"未记录"）
		if (this.originRecorded) {
			tag.putDouble("ThrowOriginX", this.originX);
			tag.putDouble("ThrowOriginY", this.originY);
			tag.putDouble("ThrowOriginZ", this.originZ);
		}
		ItemStack stack = getItemStack();
		if (!stack.isEmpty()) {
			tag.put("BoomerangStack", stack.save(registryAccess()));
		}
	}
}
