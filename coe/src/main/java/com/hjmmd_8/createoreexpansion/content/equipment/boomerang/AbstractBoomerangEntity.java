package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import com.hjmmd_8.createoreexpansion.content.charger.entity.OrbitAnchor;

import java.util.HashSet;
import java.util.Set;

import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// 2026-10-03/04 行为零变化拆分（COE 层整改 第 1 批，两轮）：本文件只保留"实体本身"——
// 身份（类型 + 档位）、状态字段、同步数据、公开 API、以及必须由实体类声明的覆写。
// 职责各自住在同包：飞行 + 飞行数值 BoomerangFlight／每 tick 编排 BoomerangTick／
// 命中判定 BoomerangImpact／穿刺额度与掉头 BoomerangPierce／破坏方块 BoomerangMining／
// 开箱取物 BoomerangContainerLoot／环绕波 BoomerangOrbitWaves／吸附 BoomerangPickup／
// 收尾结算 BoomerangTails／主人看护 BoomerangOwnerWatch／存档 BoomerangEntitySaveData。
// 搬运一律逐字：只改 `this.x` → `host.x`、给被搬走的方法加宿主参数、必要时收窄可见性。
/**
 * <b>回旋镖实体基类</b>（四把镖共用：投掷 → 去程 → 回程 → 捡物 → 挖方块）。
 *
 * <h2>一、为什么是 {@link Projectile}，不是我们的波实体</h2>
 * <p>波实体（{@code AbstractChargerWaveEntity}）语义是"能量波"——由五要素定义、与波口/载荷/加工绑定；
 * 镖是<b>投射物</b>（属于某个玩家、能被拾回、掉落归属走原版）。所以<b>直接继承 {@link Projectile}</b>：
 * 白拿 owner 的标准持久化、{@code leftOwner} 的射出保护、{@code lerpRotation} 的朝向平滑。</p>
 *
 * <h2>二、照抄 Quark 的四段，并修掉它的三个 bug</h2>
 * <ol>
 *   <li><b>去程</b>：实体射线 + 方块射线取近 ⇒ 命中方块就挖掉并立刻转回程；交替循环上限
 *       {@link #MAX_IMPACT_LOOPS}。见 {@link BoomerangImpact}。</li>
 *   <li><b>位移</b>：手写 {@code setPos(pos + 速度)}，<b>不用</b> {@code move()}/{@code lerpMotion}。见 {@link BoomerangFlight}。</li>
 *   <li><b>回程</b>：{@code noPhysics = true} + 朝主人头顶转向；抵达阈值
 *       {@link BoomerangFlight#RETURN_ARRIVE_SQR}（读法与偏差理由写在那一处）。</li>
 *   <li><b>挖方块</b>：临时换主手 → 复刻原版挖掘进度 → 原版 {@code destroyBlock}（<b>唯一破坏入口</b>：
 *       权限/时运/掉落归属全交给原版）→ {@code finally} 还原。见 {@link BoomerangMining}。</li>
 * </ol>
 * <p><b>三个 Quark bug</b>（需求 8）：a. 两个存档钩子<b>必须调 {@code super}</b>（owner 的标准持久化在
 * {@code Projectile} 里）且没有另存一份 owner，见 {@link BoomerangEntitySaveData}；b. 回程有<b>自己的寿命</b>
 * {@link #MAX_RETURN_TICKS}（去程另有 {@link #MAX_OUTBOUND_TICKS}），距离是主判据、时间是兜底，
 * 见 {@link BoomerangTick#tick(AbstractBoomerangEntity)}；c. <b>属性修饰符一处都不加</b>
 * （挖掘走 {@code destroyBlock}、伤害走 {@code hurt}，全程不碰 {@code AttributeMap}）。</p>
 *
 * <h2>三、同步与存档（需求 11）</h2>
 * <p>{@link #DATA_STACK} 渲染与交还共用（客户端必须拿得到）；{@link #DATA_RETURNING} 让客户端 tick 也走
 * 回程分支（否则只靠每 {@code updateInterval} tick 一次的位置包，回程一顿一顿）；花瓣三件套
 * {@link #DATA_PETAL} / {@link #DATA_PETAL_ORIGIN} / {@link #DATA_PETAL_ANGLE} 让两端各算同一条曲线。
 * NBT 键与读写口径全在 {@link BoomerangEntitySaveData}；{@code entitiesHit} <b>只在内存</b>
 * （只用来防止同一次飞行里重复打同一只怪）。</p>
 *
 * <h2>四、构造期陷阱（AGENTS.md 第 4 条）</h2>
 * <p>{@code Entity} 的构造器会调 {@code defineSynchedData} / {@code setPos} 等可覆写方法，而字段初始化器
 * 在<b>之后</b>才跑。因此 {@link #defineSynchedData} 与 {@link #tier()} 都不许碰对象字段：前者只用静态成员
 * 与参数，后者由子类返回枚举常量。本类<b>不覆写</b>
 * {@code setPos}/{@code getBoundingBox}/{@code defineSynchedData} 以外的构造期方法。</p>
 */

public abstract class AbstractBoomerangEntity extends Projectile implements OrbitAnchor {

	/** 渲染与交还都用的那一份镖（同步数据；写入只有 {@link #setItemStack} 一处）。 */
	static final EntityDataAccessor<ItemStack> DATA_STACK =
		SynchedEntityData.defineId(AbstractBoomerangEntity.class, EntityDataSerializers.ITEM_STACK);

	/** 是否处于回程段（同步数据：客户端 tick 也读它）。 */
	static final EntityDataAccessor<Boolean> DATA_RETURNING =
		SynchedEntityData.defineId(AbstractBoomerangEntity.class, EntityDataSerializers.BOOLEAN);

	/** <b>本次飞行是不是长按（花瓣曲线）</b>（同步数据；客户端要跟着算，见 {@link BoomerangFlight#tickPetal(AbstractBoomerangEntity)}）。 */
	static final EntityDataAccessor<Boolean> DATA_PETAL =
		SynchedEntityData.defineId(AbstractBoomerangEntity.class, EntityDataSerializers.BOOLEAN);

	/** 花瓣曲线的<b>锚点 P</b>（同步数据；出手那一刻镖的出生点）。抄服务端的值，两端才画出同一条曲线。 */
	static final EntityDataAccessor<Vector3f> DATA_PETAL_ORIGIN =
		SynchedEntityData.defineId(AbstractBoomerangEntity.class, EntityDataSerializers.VECTOR3);

	/** 花瓣曲线的<b>基准角 ψ</b>（同步数据；弧度）。存"数学角"而不是玩家 yaw，换算口径见 {@link BoomerangCurveConfigs}。 */
	static final EntityDataAccessor<Float> DATA_PETAL_ANGLE =
		SynchedEntityData.defineId(AbstractBoomerangEntity.class, EntityDataSerializers.FLOAT);

	/** 去程单 tick 内"实体 ⇄ 方块"交替判定的循环上限（需求 2；超了写日志，不崩）。 */
	public static final int MAX_IMPACT_LOOPS = 100;

	/**
	 * 去程寿命上限（tick）：<b>兜底判据</b>。
	 *
	 * <p>去程的正常结束是<b>距离</b>（{@link BoomerangFlight#outboundRangeExceeded(AbstractBoomerangEntity, Entity)}）；
	 * 这一条只负责"距离判据万一失效"（主人始终贴身跟着那种极端）时也<b>绝不永远飞下去</b>。</p>
	 */
	public static final int MAX_OUTBOUND_TICKS = 200;

	/** 回程寿命上限（tick）：Quark bug b 的修复处，超了就交还玩家（见 {@link BoomerangTails#returnTimedOut(AbstractBoomerangEntity)}）。 */
	public static final int MAX_RETURN_TICKS = 300;

	/** 吸附扫描半径（以自身碰撞盒外扩）—— 去程与回程共用（批 6 起不再只是回程捡物）。 */
	public static final double PICKUP_RADIUS = 2.0D;

	/** 捡到的掉落物上船后的拾取延迟（tick）——防止它刚贴上就被路过的玩家顺手吸走。 */
	public static final int PICKUP_DELAY = 5;

	/** 乘客的骑乘位再下移这么多（需求 4）。 */
	public static final double PASSENGER_OFFSET_Y = 0.4D;

	/** <b>"飞行方向退化成一个点"的判据</b>（{@code |d|²} 下限；批 4 十字挖掘的零向量兜底）。值与 {@link BoomerangImpact#checkImpact(AbstractBoomerangEntity)} 第一行的字面量逐字相同；再判一次是为了 {@link BoomerangMining#mineCross(AbstractBoomerangEntity, BlockPos)} 这个独立入口不拿零向量去定垂面轴（{@code normalize()} 会出 NaN）。 */
	static final double DEGENERATE_DIRECTION_SQR = 1.0E-7D;

	/** <b>主人"被传送走了"的判据阈值</b>（格²；= 一 tick 跳变 16 格）。取值理由随判据一起在 {@link BoomerangOwnerWatch}。 */
	public static final double OWNER_TELEPORT_JUMP_SQR = 16.0D * 16.0D;

	/** 存活 tick（去程 + 回程；进 NBT）。 */
	int liveTime;
	/** 回程段已飞 tick（进 NBT —— 超时判定要能跨存档继续数）。 */
	int returnTicks;
	/** 本次飞行命中生物的只数（进 NBT；第二批技能的"穿刺"要用）。 */
	int hitCount;
	/** 投掷时记下的背包槽位：回程交还优先还回这一格（进 NBT）。 */
	int slot;
	/** 投掷原点（进 NBT —— 区块重载后不丢）；当下<b>只作备用基准</b>，改基准只改 {@link BoomerangFlight#outboundRangeExceeded(AbstractBoomerangEntity, Entity)} 那一行。 */
	double originX;
	double originY;
	double originZ;
	/** 投掷原点是否已记录（服务端第一条去程 tick 置位，重载时由 NBT 键的存在与否恢复）。 */
	boolean originRecorded;
	/** 本次飞行已经打过的实体 id（<b>只在内存</b>，防止同一只怪被同一把镖反复打）。 */
	final Set<Integer> entitiesHit = new HashSet<>();
	/** <b>本次飞行累计的耐久损耗</b>（进 NBT；投掷 −2/−5、每命中一只生物 / 挖掉一个方块各 +{@link BoomerangTier#WEAR_PER_HIT}）。整段飞行<b>不碰物品的 {@code DAMAGE}</b>，唯一一次写回与判爆在 {@link BoomerangTails#settleWear(AbstractBoomerangEntity, ItemStack)}。 */
	int flightWear;
	/** 花瓣曲线的<b>归一化弧长进度 s</b>（0 = 刚出手、1 = 走完一瓣；只在内存 + NBT）。不进同步数据：两端从 0 开始、每 tick 各推进一次，天然同步。 */
	double petalProgress;

	// ── 主人瞬移检测（作者 2026-10-02 第三次裁定第 4 条；只在服务端维护，不进 NBT） ──
	// 判据、阈值理由与收尾都在 BoomerangOwnerWatch；重载后从"当前点"重新起算
	// （一 tick 的判据不需要跨存档），所以这里只有"上一 tick 的位置"这四个字段。
	/** 主人上一 tick 的位置（服务端）。 */
	double lastOwnerX;
	double lastOwnerY;
	double lastOwnerZ;
	/** 上一 tick 的主人位置是否已记录（第一 tick 只记不比，免得把出生点当成"跳变"）。 */
	boolean lastOwnerTracked;

	/** <b>本次投掷还剩多少"生物穿透额度"</b>（需求 §3.5；批 3）。{@code -1} = 尚未初始化。额度 = {@code 3 × 有效技能等级}，由 {@link BoomerangPierce#ensurePierceQuota(AbstractBoomerangEntity)} 现读一次；实体每次投掷新建 ⇒ 天然"每次投掷各一份"。 */
	private int pierceMobsLeft = -1;

	/** 还剩多少"方块穿透额度"（{@code 5 × 有效技能等级}）；{@code -1} = 尚未初始化。见 {@link #pierceMobsLeft}。 */
	private int pierceBlocksLeft = -1;

	// ================= 技能携带标记（作者 2026-10-02 第二次裁定） =================
	//
	// 裁定原文（要点）：穿刺与环绕**不是主动释放的技能**，而是"**按住技能键时该次投掷自带
	// 的效果**"，**没有任何"释放"动作**：按住键一/二 + 右键投掷 ⇒ 这一发自带该效果；
	// 不按 ⇒ 什么都不带（也不扣 20L / 15L）；投掷本身永远是右键，技能键只是"带不带"的开关。
	// 因此**不走** CoeSkillRelease / 内核释放路径、**没有**冷却、没有主动触发：这里只有两个
	// 布尔值，由 BoomerangItem#releaseUsing 在投掷那一刻读技能键写一次。
	// ⚠ 这与批 3/批 4 的旧口径相反（旧："不需要开关、只要投掷就生效"），旧口径已被作者
	// 2026-10-02 的第二次裁定**推翻**（本仓规则：口径被推翻要写明，不许静默删除）。

	/** <b>本次投掷是否携带穿刺技能</b>（= 投掷那一刻按住了键一）。只决定 {@link BoomerangPierce#ensurePierceQuota(AbstractBoomerangEntity)} 里"技能来源"那一份要不要算；<b>不</b>是命中时现读的键，也不是内建额度。 */
	private boolean pierceSkillCarried;

	/** <b>本次投掷是否携带环绕技能</b>（= 投掷那一刻按住了键二；只决定投掷时生不生成环绕波）。 */
	private boolean orbitSkillCarried;

	protected AbstractBoomerangEntity(EntityType<? extends AbstractBoomerangEntity> type, Level level) {
		super(type, level);
	}

	/** 这一把镖的数值档（子类只提供它；构造期绝不被调用）。 */
	protected abstract BoomerangTier tier();

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_STACK, ItemStack.EMPTY);
		builder.define(DATA_RETURNING, false);
		builder.define(DATA_PETAL, false);
		builder.define(DATA_PETAL_ORIGIN, new Vector3f());
		builder.define(DATA_PETAL_ANGLE, 0.0F);
	}

	// ================= 穿刺技能（2026-10-02 批 3；需求 §3.5） =================

	/** 本次投掷剩余的<b>生物</b>穿透额度（初始化后；只读给关卡/调试用）。 */
	public int getPierceMobsLeft() {
		BoomerangPierce.ensurePierceQuota(this);
		return this.pierceMobsLeft;
	}

	/** 本次投掷剩余的<b>方块</b>穿透额度（初始化后；只读给关卡/调试用）。 */
	public int getPierceBlocksLeft() {
		BoomerangPierce.ensurePierceQuota(this);
		return this.pierceBlocksLeft;
	}

	// ================= 穿刺额度的包级读写点（2026-10-03 拆分 glue） =================
	//
	// 这两个额度字段**仍然 private**（关卡 §29n-2 用字面量 'private int pierceMobsLeft = -1' 钉着
	// "额度是实体状态、每次投掷各一份、不许挪到物品上"）。搬出去的 BoomerangPierce / BoomerangImpact
	// 需要读写它们，于是这里放四个一行包级访问器——**没有**把字段改成 public，也没有把额度搬到别处。

	int pierceMobsLeft() {
		return this.pierceMobsLeft;
	}

	void setPierceMobsLeft(int value) {
		this.pierceMobsLeft = value;
	}

	int pierceBlocksLeft() {
		return this.pierceBlocksLeft;
	}

	void setPierceBlocksLeft(int value) {
		this.pierceBlocksLeft = value;
	}

	/** 技能携带标记的包级写点（同上：字段仍 private；投掷路径唯一的写入点仍是 setCarriedSkills）。 */
	void setPierceSkillCarried(boolean carried) {
		this.pierceSkillCarried = carried;
	}

	void setOrbitSkillCarried(boolean carried) {
		this.orbitSkillCarried = carried;
	}

	// ================= 技能携带标记（作者 2026-10-02 第二次裁定） =================

	/**
	 * <b>写入"这一发带不带那两个效果"</b>（投掷那一刻<b>唯一</b>一处写入口，由
	 * {@code BoomerangItem#releaseUsing} 调用）。记在实体上而不是物品上：一次投掷一个实体，
	 * 天然"每次投掷各一份"（记在栈上就会跨投掷累计）。
	 *
	 * @param pierceSkill 投掷那一刻是否按住键一（穿刺）
	 * @param orbitSkill  投掷那一刻是否按住键二（环绕）
	 */
	public void setCarriedSkills(boolean pierceSkill, boolean orbitSkill) {
		this.pierceSkillCarried = pierceSkill;
		this.orbitSkillCarried = orbitSkill;
	}

	/** 本次投掷是否携带穿刺技能（决定 {@link BoomerangPierce#ensurePierceQuota(AbstractBoomerangEntity)} 里"技能来源"那一份）。 */
	public boolean isPierceSkillCarried() {
		return this.pierceSkillCarried;
	}

	/** 本次投掷是否携带环绕技能（生成环绕波的那一处读它）。 */
	public boolean isOrbitSkillCarried() {
		return this.orbitSkillCarried;
	}

	// ================= 环绕技能（2026-10-02 批 4；需求 §3.6 / §3.7） =================

	/**
	 * <b>环绕波锚点契约（{@link OrbitAnchor}）的"取运动方向"实现 —— 镖这一侧</b>
	 * （作者裁定 D9 = A）：镖的位移就写在原版速度字段上，所以"镖的运动方向"就是
	 * {@code getDeltaMovement()}；环平面<b>垂直于它</b> ⇒ 与需求 §3.6 第 2 条逐字同形。
	 */
	@Override
	public Vec3 orbitDirection() {
		return getDeltaMovement();
	}

	/**
	 * <b>环绕波撞到普通方块时的回调</b>（{@link OrbitAnchor#orbitMineBlock(BlockPos)} 的镖侧实现）：
	 * 直接复用 {@link BoomerangMining#mineBlock(AbstractBoomerangEntity, BlockPos)}（同一条判定链、
	 * 唯一破坏入口与耐久记账）。
	 *
	 * <p><b>挖不动 ⇒ 什么都不做</b>（返回 {@code false}），该枚照旧按"撞墙"消散 —— 需求 §3.6 没写
	 * "挖不动怎么办"，这里取最小偏差的读法：<b>不挖、但该枚照样消失</b>。见报告 §⑥。</p>
	 */
	@Override
	public boolean orbitMineBlock(BlockPos pos) {
		return BoomerangMining.mineBlock(this, pos);
	}

	/**
	 * ★ <b>投掷时挂上环绕技能：生成 L 枚环绕波</b>（需求 §3.6；批 4 的唯一生成处）。
	 *
	 * <p>实现整体在 {@link BoomerangOrbitWaves#spawnOrbitWaves(AbstractBoomerangEntity, ItemStack, Vec3)}；
	 * 本方法保留原公开签名（投掷侧唯一入口），只做一行转发。</p>
	 *
	 * @param stack     投掷的那一把镖（等级 = 本档基准 + 技艺提升/回溯，钳 1..5）
	 * @param flightDir 出手方向；同时是环绕波的出生朝向（每 tick 的环平面法向仍从锚点<b>实时</b>取）
	 */
	public void spawnOrbitWaves(ItemStack stack, Vec3 flightDir) {
		BoomerangOrbitWaves.spawnOrbitWaves(this, stack, flightDir);
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
	 * 切换去程/回程（唯一入口）。切到回程时同时做两件事：{@code noPhysics = true}（穿墙回手——<b>但必须有
	 * 回程寿命 {@link #MAX_RETURN_TICKS}，否则就是 Quark 那个"永远穿墙追不上"的 bug</b>）与回程计时归零。
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

	// ================= 两种模式：花瓣飞行 + 耐久累计（2026-10-02 批 2） =================

	/**
	 * <b>把本次飞行标记成长按（花瓣曲线）</b>并记下曲线锚点与基准角 —— <b>唯一入口</b>，
	 * 由 {@code BoomerangItem#releaseUsing} 在长按投掷时调用（服务端）。曲线本身（弧长参数化推进）
	 * 在 {@link BoomerangFlight#tickPetal(AbstractBoomerangEntity)}；这里只写同步数据与进度归零。
	 *
	 * @param origin    曲线锚点 {@code P}（= 镖的出生点：玩家眼睛下方 0.1 格）
	 * @param baseAngle 基准角 ψ（弧度）= 出手那一刻水平朝向的<b>数学角</b> {@code atan2(朝向.z, 朝向.x)}
	 */
	public void startPetalFlight(Vec3 origin, double baseAngle) {
		this.entityData.set(DATA_PETAL, true);
		this.entityData.set(DATA_PETAL_ORIGIN, new Vector3f((float) origin.x, (float) origin.y, (float) origin.z));
		this.entityData.set(DATA_PETAL_ANGLE, (float) baseAngle);
		this.petalProgress = 0.0D;
	}

	/** 本次飞行是不是长按（花瓣曲线）。点按（直线）恒 {@code false}。 */
	public boolean isPetalFlight() {
		return this.entityData.get(DATA_PETAL);
	}

	/** 本次飞行累计的耐久损耗（需求 §3.8 的账；只在 {@link BoomerangTails#settleWear(AbstractBoomerangEntity, ItemStack)} 一次写回）。 */
	public int getFlightWear() {
		return flightWear;
	}

	/** 往本次飞行的耐久账上记一笔。<b>只记账</b>：不碰物品、不判爆（那两件都在 {@link BoomerangTails#settleWear(AbstractBoomerangEntity, ItemStack)}）；非正数直接忽略。 */
	public void addFlightWear(int amount) {
		if (amount > 0) {
			this.flightWear += amount;
		}
	}

	// ================= tick 骨架 =================

	/**
	 * <b>每 tick 的唯一入口</b>：{@code super.tick()} 必须留在这里 —— {@code Projectile} 在那里补
	 * {@code gameEvent(射出了)} 与 {@code leftOwner}（射出保护），{@code Entity} 在那里补 {@code baseTick}。
	 * 其后的<b>阶段序列</b>（主人兜底 → 主人瞬移 → 去程/回程 → 唯一吸附点）逐字搬进了
	 * {@link BoomerangTick#tick(AbstractBoomerangEntity)}，顺序与每一步的理由都在那里。
	 */
	@Override
	public void tick() {
		super.tick();
		BoomerangTick.tick(this);
	}

	/**
	 * <b>转向微桥</b>（2026-10-03 拆分必需，一行）：{@code Projectile#updateRotation()} 是<b>别的包</b>里的
	 * {@code protected}，同包 helper 调不到它（JLS 6.6.2）；helper 改用本方法 —— 它<b>只</b>转发原方法。
	 */
	void rotateToMotion() {
		updateRotation();
	}

	/** 放行这两类乘客（默认实现只放行 {@code canRide} 的生命体）。 */
	@Override
	public boolean canAddPassenger(Entity passenger) {
		return BoomerangPickup.canCarry(passenger);
	}

	/**
	 * ★ <b>本镖不打自己的乘客</b>（批 6 一并补上的必要闸门）：乘客的骑乘位就在镖身上，而
	 * {@code ProjectileUtil} 的候选只排除载具自己、不排除乘客 ⇒ 不去掉它，镖下一个 tick 就会把自己的
	 * 战利品当敌人打（{@code ItemEntity} 只有 5 点血，而伤害是 6~12 点）。
	 *
	 * <p>为什么批 6 起它才"必须"、以及完整的几何推理，见 {@link BoomerangPickup} 的类注释第八节。</p>
	 */
	@Override
	protected boolean canHitEntity(Entity target) {
		return target.getVehicle() != this && super.canHitEntity(target);
	}

	/** 乘客骑乘位整体下移 {@value #PASSENGER_OFFSET_Y}（需求 4）。 */
	@Override
	public Vec3 getPassengerRidingPosition(Entity passenger) {
		return super.getPassengerRidingPosition(passenger).subtract(0.0D, PASSENGER_OFFSET_Y, 0.0D);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		// ⛔ 这一行是 Quark bug a 的修复：owner（Owner UUID / LeftOwner）的标准持久化就在 super 里，
		// 且必须早于下面的读入（键与口径见 BoomerangEntitySaveData#read）。
		super.readAdditionalSaveData(tag);
		BoomerangEntitySaveData.read(this, tag);
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag); // 同上：owner 由 Projectile 存
		BoomerangEntitySaveData.write(this, tag);
	}
}
