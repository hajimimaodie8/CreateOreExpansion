package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.entity.OrbitAnchor;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 *
 * <h2>五、批 2（2026-10-02）：两种飞行模式 · 花瓣曲线 · 耐久只累计、回程一次结算</h2>
 *
 * <p><b>a. 点按 = 逐字沿用现状（直线）</b>。去程仍是"实体射线 + 方块射线取近者 ⇒ 命中方块即
 * {@link #setReturning}、命中生物只伤不回头（靠 {@code entitiesHit} 去重）"，主判据仍是
 * {@link #outboundRangeExceeded}。<b>这一支一个字都没改</b>（需求 §3.2 / §5.3 陷阱 #6）。</p>
 *
 * <p><b>b. 长按 = 花瓣曲线</b>（需求 §3.4；数学与常数全在 {@link BoomerangCurveConfigs}）。
 * 分支在 {@link #tickOutbound()} 的第一行：{@code isPetalFlight() ⇒ tickPetal()}。
 * 曲线用<b>弧长参数化</b>推进（{@code Δs = v(s)/L_total}，每 tick 一次），
 * <b>s 走到 1 才 {@code setReturning(true)}</b>（"必须飞完一瓣才能返回"）；
 * 花瓣段<b>不受</b>距离判据约束（否则主人一挪步就会把花瓣从中间掐断），只剩
 * {@link #MAX_OUTBOUND_TICKS} 兜底。
 * <br>⚠ <b>批 3 修正 2</b>：花瓣段<b>也要</b>走命中判定（{@link #tickPetal} 调
 * {@link #checkImpact()} 这一处，与点按共用），所以长按同样能伤生物、挖方块、吃穿刺额度；
 * 但"必须飞完一瓣才能返回"是硬优先级 ⇒ 花瓣段<b>永不</b>因命中而提前掉头
 * （{@link #onHitBlock} / {@link #onHitEntity} 里的 {@code isPetalFlight()} 分支）。</p>
 *
 * <p>曲线的锚点是<b>出手那一刻的位置</b>（{@link #startPetalFlight} 由物品传入，
 * 走同步数据 {@link #DATA_PETAL_ORIGIN} 所以客户端也能自己算出同一条曲线），
 * 基准角 ψ 是出手那一刻玩家水平朝向的数学角 ⇒ 与需求 §3.4.3 的
 * {@code x = P.x + r(φ)·cos(ψ+φ)} 逐字同形。⚠ 需求把 {@code P} 写成"玩家位置"这个常量：
 * 出手之后玩家再走动，花瓣<b>不会</b>跟着平移（想改成跟随主人只需在 {@link #tickPetal}
 * 里把锚点换成主人的当前位置，一行）。</p>
 *
 * <p><b>c. 耐久：飞行期间只累计、不写回</b>（需求 §3.8，本类最容易做错的一处）。
 * 损耗累计在 {@link #flightWear} 上：投掷那次由物品给（点按 −2 / 长按 −5，
 * {@link BoomerangTier#throwWear(boolean)}），此后每命中一个生物 / 每挖掉一个方块各
 * {@code +}{@link BoomerangTier#WEAR_PER_HIT}。<b>整段飞行里一次都不碰物品的 {@code DAMAGE}</b>
 * （逐次写回会把耐久提前打到 0，正是 §3.8 禁止的"当场归零"）。</p>
 *
 * <p><b>d. 结算只有一处</b>（裁定 D14）：{@link #collect}（交还）/
 * {@link #returnTimedOut}（回程超时）/ {@link #ownerGone}（主人失效）三条尾路径
 * <b>全部只调</b> {@link #finishFlight(boolean)}，由它调 {@link #settleWear(ItemStack)}：
 * 累计 &lt; 剩余 ⇒ 扣一次写回（活下来的镖恒有 ≥ 1 点耐久，绝不留下 0 耐久物品）；
 * 累计 ≥ 剩余 ⇒ <b>爆掉</b>（播放 {@code ITEM_BREAK} + <b>不 {@code spawnAtLocation}</b>，
 * 物品就此消失）。<b>爆掉时乘客/并入物照样先交给玩家</b>（需求 §六 推断值 #5，
 * "不给会白丢一次挖掘收益"），拿不到玩家时照旧落地、绝不销毁。冷却不受爆掉影响：
 * 冷却是在投掷那一刻就上好的（{@code BoomerangItem#releaseUsing}）。</p>
 *
 * <h2>六、批 3（2026-10-02）：穿刺技能（需求 §3.5 / §3.6 的"镖本身"那一半）</h2>
 * <p>四把镖都带 {@code createoreexpansion:pierce}（基准等级 1/2/3/3、钳到 5），
 * 不需要开关、点按与长按都生效。额度<b>每次投掷各一份</b>：实体是每次投掷新建的，
 * 账记在 {@link #pierceMobsLeft} / {@link #pierceBlocksLeft} 上，由
 * {@link #ensurePierceQuota()} 从镖的栈现读等级算一次（{@link BoomerangSkillConfigs}
 * 的 {@code 3L} / {@code 5L}）。三条规则：</p>
 * <ol>
 *   <li><b>额度用完即掉头</b>（需求 §3.5 的原话，= 回到"碰到就回"的行为）：生物额度用完的那一次
 *       命中、方块额度用完的那一次挖掘，都会 {@code setReturning(true)}；</li>
 *   <li><b>"飞完一瓣"优先</b>（需求 §3.2 + 批 3 裁定）：花瓣段只吃额度、<b>绝不</b>提前掉头
 *       ——两个 {@code isPetalFlight()} 分支就是这条优先级的落点，关卡 §29n-2 钉着它；</li>
 *   <li><b>穿透破坏照样扣耐久</b>（需求 §六 推断值 #4）：命中生物 / 挖掉方块各
 *       −{@link BoomerangTier#WEAR_PER_HIT}，走的仍是批 2 的"只累计、回程一次结算"。</li>
 * </ol>
 * <p>⚠ <b>撞上挖不动的方块不掉额度但会掉头</b>（点按段）：{@link #mineBlock} 的三关
 * （硬度 / 挖掘等级 / 原版进度）没过 ⇒ 不消耗额度、也不穿墙（批 1/2 的"撞墙即回"照旧）。
 * 花瓣段则穿过（曲线是固定路径，与批 2 的花瓣段行为一致）。</p>
 *
 * <h2>七、批 4（2026-10-02）：环绕技能（需求 §3.6 / §3.7）</h2>
 * <p>投掷时挂上 <b>L 枚环绕波</b>（L = 有效技能等级，1..5），锚点就是<b>这枚镖</b>：
 * 镖实现 {@link OrbitAnchor}（契约由作者裁定 D9 = A 放宽："锚点不必是波"），
 * 于是既有的环绕波要素（半径 / 角速度 / 相位 / 垂面几何 / "锚点没了就收尾"）
 * <b>一个字都不用改</b>就服务了回旋镖。要点：</p>
 * <ul>
 *   <li><b>数量 = L</b>（{@code BoomerangSkillConfigs#orbitCount}）；
 *       <b>半径 1.5</b>、<b>角速度 1 圈/秒</b>、基相位 0、第 i 枚相位 {@code 2π·i/L}
 *       （均匀铺满一圈，否则 L 枚会重合成一枚）；</li>
 *   <li><b>平面 = 镖运动方向的垂面</b>（{@link #orbitDirection()} = {@code getDeltaMovement()}，
 *       既有几何负责把它变成两个基向量）；</li>
 *   <li><b>同批豁免</b>：L 枚共用一个<b>负数</b>批次号（见 {@link #nextOrbitBatch()}）——
 *       镖不是波、没有批次号，所以这里自造一个来源；负数是为了与星芒嬗震的<b>正数</b>序列
 *       永不相等（否则两组批次可能撞号 ⇒ 两枚本该湮灭的波互相豁免）。机器波仍是批次 0
 *       （= "不属于任何批次"）⇒ 长期口径不变；</li>
 *   <li><b>伤害 2×L</b>：走 {@code ChargerWaveEntity} 的"自定义伤害"要素（波形仍是既有实体、
 *       仍是 ATTACK 波型），<b>撞生物 ⇒ 该枚消失</b>（既有波的命中语义，不需要新代码）；</li>
 *   <li><b>撞方块 ⇒ 挖掉 + 该枚消失</b>：{@link #orbitMineBlock(BlockPos)} 直接复用
 *       {@link #mineBlock}（同一条 {@code maxHardness} / {@code miningLevel} / 原版进度判定，
 *       同样 −1 耐久）；掉落物与经验由原版生成在世界里，靠镖既有的回程吸附
 *       （{@link #pickUpItems()}）带走、{@link #finishFlight(boolean)} 交给玩家（零新机制）；</li>
 *   <li><b>收尾</b>：镖 {@code discard()} 后环绕波下一 tick 自己收尾（既有
 *       "锚点没了即 discard"语义）；</li>
 *   <li><b>消耗 15×L</b>：与模式消耗、穿刺 20×L 相加在 {@code BoomerangItem#throwCost} 同一处。</li>
 * </ul>
 */
public abstract class AbstractBoomerangEntity extends Projectile implements OrbitAnchor {

	/** 渲染与交还都用的那一份镖（同步数据；写入只有 {@link #setItemStack} 一处）。 */
	private static final EntityDataAccessor<ItemStack> DATA_STACK =
		SynchedEntityData.defineId(AbstractBoomerangEntity.class, EntityDataSerializers.ITEM_STACK);

	/** 是否处于回程段（同步数据：客户端 tick 也读它）。 */
	private static final EntityDataAccessor<Boolean> DATA_RETURNING =
		SynchedEntityData.defineId(AbstractBoomerangEntity.class, EntityDataSerializers.BOOLEAN);

	/**
	 * <b>本次飞行是不是长按（花瓣曲线）</b>（同步数据）。
	 *
	 * <p>客户端也必须知道：去程的位移在两端各自算（{@link #tickPetal}），而位置包每
	 * {@code updateInterval} 才来一次（实体类型上的 10 tick）——只靠位置包会让花瓣一顿一顿。
	 * 所以曲线本身走"两端用同一组常数现算"，由本标志 + {@link #DATA_PETAL_ORIGIN} +
	 * {@link #DATA_PETAL_ANGLE} 三个同步值一起决定它长什么样。</p>
	 */
	private static final EntityDataAccessor<Boolean> DATA_PETAL =
		SynchedEntityData.defineId(AbstractBoomerangEntity.class, EntityDataSerializers.BOOLEAN);

	/**
	 * 花瓣曲线的<b>锚点 P</b>（同步数据；出手那一刻镖的出生点 = 玩家眼睛下方 0.1 格）。
	 *
	 * <p>用 {@code VECTOR3} 而不是"让客户端自己记出生点"：客户端的那一份位置是位置包给的，
	 * 与出生点之间可能有若干 tick 的误差；锚点直接抄服务端的值，两端才会画出同一条曲线。</p>
	 */
	private static final EntityDataAccessor<Vector3f> DATA_PETAL_ORIGIN =
		SynchedEntityData.defineId(AbstractBoomerangEntity.class, EntityDataSerializers.VECTOR3);

	/**
	 * 花瓣曲线的<b>基准角 ψ</b>（同步数据；弧度，= {@code atan2(出手时朝向.z, 出手时朝向.x)}）。
	 *
	 * <p>存"数学角"而不是玩家 yaw：需求 §3.4.3 的公式是 {@code r·cos(ψ+φ)} / {@code r·sin(ψ+φ)}
	 * （x/z 平面上的数学极角），而 MC 的 yaw 是以 +Z 为 0、绕 -Y 转的另一套约定——
	 * 在这里换算一次，实体里就不会再出现"那到底是哪个角"的歧义。</p>
	 */
	private static final EntityDataAccessor<Float> DATA_PETAL_ANGLE =
		SynchedEntityData.defineId(AbstractBoomerangEntity.class, EntityDataSerializers.FLOAT);

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

	/**
	 * <b>主人"被传送走了"的判据阈值</b>（格²；作者 2026-10-02 第三次裁定第 4 条）。
	 *
	 * <p>判据 = <b>主人一 tick 内的位置跳变</b>超过 {@code 16} 格（{@code 16² = 256}）：
	 * 玩家任何正常移动都在 4 格/tick 以内（自由落体终端速度 3.92 格/tick 就是上限，
	 * 冲刺 0.28 / 鞘翅+烟花约 3.5）⇒ 16 格留了 4 倍余量；而传送（{@code /tp}、传送门换维度、
	 * 死亡重生、末影珍珠 ≥ 20 格）必然远超它。</p>
	 *
	 * <p>为什么不用"与主人的距离绝对值"：主人正常跑位/飞行也能在 300 tick 的回程寿命里
	 * 拉开上百格（回程只有 0.7 格/tick），那样会把"正常拉开距离"误判成传送；
	 * 而"一 tick 跳变"是传送的<b>充分</b>特征（没有正常移动能做到）。</p>
	 */
	public static final double OWNER_TELEPORT_JUMP_SQR = 16.0D * 16.0D;

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
	/**
	 * <b>本次飞行累计的耐久损耗</b>（进 NBT；需求 §3.8 的"只累计、不写回"就落在这个字段上）。
	 *
	 * <p>投掷那一次由 {@code BoomerangItem} 给（点按 −2 / 长按 −5），此后每命中一个生物 / 每挖掉
	 * 一个方块各 +{@link BoomerangTier#WEAR_PER_HIT}。<b>整段飞行不碰物品的 {@code DAMAGE}</b>——
	 * 逐次写回会把耐久提前打到 0，那正是需求 §3.8 禁止的"当场归零"；
	 * 唯一一次写回在 {@link #settleWear(ItemStack)}。</p>
	 *
	 * <p>进 NBT 的理由与 {@link #returnTicks} 同一条：跨区块重载不许把账抹掉，
	 * 否则"飞出去一趟把耐久欠账躲掉"就成了可行策略。</p>
	 */
	private int flightWear;
	/**
	 * 花瓣曲线的<b>归一化弧长进度 s</b>（0 = 刚出手、1 = 走完一瓣；只在内存 + NBT）。
	 *
	 * <p>不进同步数据：两端从 0 开始、每 tick 各推进一次（推进公式只依赖
	 * {@link BoomerangCurveConfigs} 的常数与 R），所以它天然同步；
	 * 进 NBT 则是为了区块重载后不从头再飞一瓣。</p>
	 */
	private double petalProgress;

	// ── 主人瞬移检测（作者 2026-10-02 第三次裁定第 4 条；只在服务端维护，不进 NBT） ──
	// 为什么要它：主人被 /tp 或传送门扔到极远处时，镖既追不上也"回不到手里"，只能
	// 飞满 MAX_RETURN_TICKS 再收尾。这里改为**一 tick 内主人位置跳变超过阈值就当场收尾**，
	// 并把镖交还玩家（见 teleportRecover / finishFlight(false)）。上一 tick 的位置只用于
	// 算跳变，重载后从"当前点"重新起算（一 tick 的判据不需要跨存档）。
	/** 主人上一 tick 的 X（服务端）。 */
	private double lastOwnerX;
	/** 主人上一 tick 的 Y（服务端）。 */
	private double lastOwnerY;
	/** 主人上一 tick 的 Z（服务端）。 */
	private double lastOwnerZ;
	/** 上一 tick 的主人位置是否已记录（第一 tick 只记不比，免得把出生点当成"跳变"）。 */
	private boolean lastOwnerTracked;

	/**
	 * <b>本次投掷还剩多少"生物穿透额度"</b>（需求 §3.5；批 3）。{@code -1} = 尚未初始化。
	 *
	 * <p>额度 = {@code 3 × 有效技能等级}，由 {@link #ensurePierceQuota()} 从
	 * {@link #getItemStack()} 现读一次（同时也把方块额度算出来）。<b>它是"每次投掷一份"的账</b>：
	 * 实体是每次投掷新建的对象 ⇒ 天然"每次投掷独立重置"，绝不会跨投掷累计
	 * （那就是把额度记在物品上了，需求明确排除）。</p>
	 */
	private int pierceMobsLeft = -1;
	/** 还剩多少"方块穿透额度"（{@code 5 × 有效技能等级}）；{@code -1} = 尚未初始化。见 {@link #pierceMobsLeft}。 */
	private int pierceBlocksLeft = -1;

	// ================= 技能携带标记（作者 2026-10-02 第二次裁定） =================
	//
	// 裁定原文（要点）：穿刺与环绕**不是主动释放的技能**，而是"**按住技能键时，该次投掷自带
	// 的效果**"——与装备技能同一种东西（按住才生效的被动效果），**没有任何"释放"动作**：
	//   · 按住技能键 + 右键投掷 ⇒ 这一发自带该效果；
	//   · 不按技能键 + 右键投掷 ⇒ 这一发什么都不带（也不扣 20L / 15L）；
	//   · 投掷本身永远是右键；技能键只是"这次投掷带不带这个效果"的开关。
	// 因此**不走** CoeSkillRelease / 内核释放路径、**没有**冷却、没有主动触发：这里只有
	// 两个"这次投掷带不带"的布尔值，由 BoomerangItem#releaseUsing 在投掷那一刻读技能键写一次。
	//
	// ⚠ 这与批 3/批 4 的旧口径相反（旧："不需要开关、只要投掷就生效"）——旧口径已被作者
	// 2026-10-02 的第二次裁定**推翻**，本类与 BoomerangItem 的注释都把它写下来了（本仓规则：
	// 口径被推翻要写明，不许静默删除）。

	/**
	 * <b>本次投掷是否携带穿刺技能</b>（= 投掷那一刻按住了键一；见类注释与
	 * {@code BoomerangItem#skillKeyHeld}）。
	 *
	 * <p>它只决定一件事：{@link #ensurePierceQuota()} 里"技能来源"那一份要不要算。
	 * 它<b>不</b>是本实体自己读的键（那样每次命中都读一次，与"投掷那一刻决定"的自相矛盾），
	 * 也不是内建额度（长按自带的那 1 只 / 1 个与它无关，见
	 * {@code BoomerangSkillConfigs#BUILTIN_PIERCE_MOBS_ON_HOLD}）。</p>
	 */
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

	/**
	 * <b>初始化本次投掷的穿透额度</b>（只在第一次需要时算一次）。
	 *
	 * <p>等级走 {@link BoomerangItem#effectiveSkillLevel(ItemStack, int)}（与物品侧<b>同一个</b>
	 * 读取点）——基准等级取本档 {@link BoomerangTier#baseSkillLevel()}，叠技艺提升 / 技艺回溯后
	 * 钳到 5。额度本身来自 {@link BoomerangSkillConfigs}（{@code 3L} 与 {@code 5L}）。</p>
	 *
	 * <p>为什么<b>惰性</b>初始化而不是在投掷时写：实体在被投掷的那一刻已经把镖的栈同步进来了
	 * （{@code setItemStack} 在 {@code addFreshEntity} 之前），但"这一趟到底有没有用到穿透"
	 * 只有第一次命中才知道；惰性初始化让"没打过任何东西的一趟"完全不碰这套账，
	 * 也让跨区块重载（{@code -1} 与已用的剩余额度都进 NBT）与之一致。</p>
	 */
	private void ensurePierceQuota() {
		if (this.pierceMobsLeft >= 0) {
			return;
		}
		// 额度只有一个来源：**穿刺技能**（投掷那一刻按住键一，作者 2026-10-02 第二次裁定）。
		// 「长按自带 1 / 1」那个暂定值已被第三次裁定废除（花瓣段不吃任何额度，见
		// BoomerangSkillConfigs 里那一段留档）⇒ 没携带技能时额度恒 0，判定自然退化成
		// "碰到即回"（点按段）。
		if (this.pierceSkillCarried) {
			int level = BoomerangItem.effectiveSkillLevel(getItemStack(), tier().baseSkillLevel());
			this.pierceMobsLeft = BoomerangSkillConfigs.pierceMobQuota(level);
			this.pierceBlocksLeft = BoomerangSkillConfigs.pierceBlockQuota(level);
		} else {
			this.pierceMobsLeft = 0;
			this.pierceBlocksLeft = 0;
		}
	}

	/** 本次投掷剩余的<b>生物</b>穿透额度（初始化后；只读给关卡/调试用）。 */
	public int getPierceMobsLeft() {
		ensurePierceQuota();
		return this.pierceMobsLeft;
	}

	/** 本次投掷剩余的<b>方块</b>穿透额度（初始化后；只读给关卡/调试用）。 */
	public int getPierceBlocksLeft() {
		ensurePierceQuota();
		return this.pierceBlocksLeft;
	}

	// ================= 技能携带标记（作者 2026-10-02 第二次裁定） =================

	/**
	 * <b>写入"这一发带不带那两个效果"</b>（投掷那一刻<b>唯一</b>一处写入口，由
	 * {@code BoomerangItem#releaseUsing} 调用）。
	 *
	 * <p>为什么在实体上而不是物品上：与穿刺额度同理 —— <b>一次投掷一个实体</b>，
	 * 记在实体上天然"每次投掷各一份"，绝不会跨投掷累计（记在栈上就会）。</p>
	 *
	 * @param pierceSkill 投掷那一刻是否按住键一（穿刺）
	 * @param orbitSkill  投掷那一刻是否按住键二（环绕）
	 */
	public void setCarriedSkills(boolean pierceSkill, boolean orbitSkill) {
		this.pierceSkillCarried = pierceSkill;
		this.orbitSkillCarried = orbitSkill;
	}

	/** 本次投掷是否携带穿刺技能（决定 {@link #ensurePierceQuota()} 里"技能来源"那一份）。 */
	public boolean isPierceSkillCarried() {
		return this.pierceSkillCarried;
	}

	/** 本次投掷是否携带环绕技能（生成环绕波的那一处读它）。 */
	public boolean isOrbitSkillCarried() {
		return this.orbitSkillCarried;
	}

	// ================= 环绕技能（2026-10-02 批 4；需求 §3.6 / §3.7） =================

	/**
	 * <b>环绕波批次号计数器</b>（服务端权威、进程内自减 ⇒ 分配出来的批次号<b>恒为负数</b>）。
	 *
	 * <p><b>为什么自造一个来源</b>：豁免判据 {@code sameFiringBatch} 比较的是"两枚波是不是
	 * 同一次发射"，而<b>镖不是波、根本没有批次号</b>（唯一既有的分配器
	 * {@code StarShockRuntime#nextBatch()} 是星界套的私有实现，本项目口径也不让回旋镖依赖它）。
	 * 所以这里造一个只用给"这一次投掷的 L 枚环绕波"的号，让它们<b>彼此不湮灭</b>。</p>
	 *
	 * <p><b>为什么是负数（与"批次 0"和星芒嬗震都不撞号）</b>：{@code sameFiringBatch} 要求
	 * 双方都非 0 且相等，而<b>机器波恒为 0</b>（"不属于任何批次"）⇒ 负数天然不等于 0；
	 * 星芒嬗震的序列是<b>正数</b>（{@code BATCH_SEQUENCE++}，从 1 起）⇒
	 * 两个来源的值域不相交，永远不可能出现"一次星界技能发射与一次回旋镖投掷撞号"，
	 * 也就不会出现"两枚本应互相湮灭的波互相豁免"（长期口径：任意两波相交即爆炸湮灭）。</p>
	 *
	 * <p>位宽 32 位、只在同一次投掷内部比较 ⇒ 与既有批次号同一种"进程内短标识"口径。</p>
	 */
	private static int ORBIT_BATCH_SEQUENCE = 0;

	/**
	 * 分配一个<b>环绕波批次号</b>（{@link #ORBIT_BATCH_SEQUENCE}；<b>恒 &lt; 0</b> ⇒ 恒非 0）。
	 *
	 * <p>回绕兜底：{@code int} 减到 {@code Integer.MIN_VALUE} 再减会变成正数
	 * （= 可能与星芒嬗震的序列撞号），所以到 0 就绕回 {@code -1}。正常游戏里不可能发生，
	 * 但"绕回正数"这条路径正是必须堵掉的（它与"批次号 0"是同一类静默失效）。</p>
	 */
	private static synchronized int nextOrbitBatch() {
		ORBIT_BATCH_SEQUENCE--;
		if (ORBIT_BATCH_SEQUENCE >= 0) {
			ORBIT_BATCH_SEQUENCE = -1;
		}
		return ORBIT_BATCH_SEQUENCE;
	}

	/**
	 * <b>环绕波锚点契约（{@link OrbitAnchor}）的"取运动方向"实现 —— 镖这一侧</b>
	 * （作者裁定 D9 = A）：镖的手写位移就写在原版速度字段上
	 * （点按的 {@code shootFromRotation} / 花瓣段的 {@code setDeltaMovement(target - position)}，
	 * 见 {@link #tickOutbound()} / {@link #tickPetal()}），所以"镖的运动方向"就是
	 * {@code getDeltaMovement()}。环绕波的环平面<b>垂直于它</b> ⇒ 与需求 §3.6 第 2 条
	 * "环绕运动轨迹 = 回旋镖当前运动轨迹的垂面"逐字同形。</p>
	 */
	@Override
	public Vec3 orbitDirection() {
		return getDeltaMovement();
	}

	/**
	 * <b>环绕波撞到普通方块时的回调</b>（{@link OrbitAnchor#orbitMineBlock(BlockPos)} 的镖侧实现）：
	 * 直接复用 {@link #mineBlock(BlockPos)} —— 同一条判定链（硬度 ≥ 0、≤ 本档
	 * {@code maxHardness}、不在本档 {@code INCORRECT_FOR_*_TOOL}、原版挖掘进度
	 * {@code digSpeed/(hardness*i)} 达标），同一条唯一破坏入口
	 * （{@code player.gameMode.destroyBlock}），以及同一条耐久记账
	 * （挖掉 ⇒ {@code addFlightWear(WEAR_PER_HIT)}，<b>挖不动不计</b>）。</p>
	 *
	 * <p><b>挖不动 ⇒ 什么都不做</b>（返回 {@code false}），环绕波照旧按"撞墙"消散 ——
	 * 需求 §3.6 只写了"碰方块 ⇒ 挖掘并使其消失"，没写"挖不动怎么办"；
	 * 这里取最小偏差的读法：<b>不挖、但该枚照样消失</b>（撞上方块就是碰撞），
	 * 与既有波的撞墙语义一致。见报告 §⑥。</p>
	 */
	@Override
	public boolean orbitMineBlock(BlockPos pos) {
		return mineBlock(pos);
	}

	/**
	 * ★ <b>投掷时挂上环绕技能：生成 L 枚环绕波</b>（需求 §3.6；批 4 的唯一生成处）。
	 *
	 * <p>由 {@code BoomerangItem#releaseUsing} 在镖<b>已入世界之后</b>调用一次
	 * （点按与长按<b>都</b>调 —— 需求 §六 推断值 #9：技能不需要开关、两种模式都生效）。</p>
	 *
	 * <p>每一枚都是<b>既有波实体</b>（{@code ChargerWaveEntity}，与充能器/星芒嬗震同一种），
	 * 只设三个要素：</p>
	 * <ol>
	 *   <li><b>波型 = ATTACK</b>（{@code trySetWaveType}）：既有命中链里"造成伤害"那一支的
	 *       门槛就是 {@code getWaveType().dealsDamage()}，不设它环绕波就只是个观光粒子；</li>
	 *   <li><b>自定义伤害 = 2 × 等级</b>（{@code ChargerWaveEntity#setCustomDamage}）——
	 *       正是作者裁定 D9(A) 的"伤害可覆写"，<b>不</b>另写一套波级表；</li>
	 *   <li><b>环绕要素</b>（{@code setOrbitAnchor}）：锚点 = 这枚镖的 UUID、半径 1.5、
	 *       角速度 1 圈/秒、相位 {@code 基相位 + 2π·i/L}（均匀铺满一圈）。</li>
	 * </ol>
	 * <p>外加<b>同一个批次号</b>（同一次投掷的 L 枚彼此不湮灭；见 {@link #nextOrbitBatch()}）。</p>
	 *
	 * <p>⚠ <b>不新增实体类型 / 贴图 / 模型 / 渲染器</b>：用的是既有 {@code charger_wave}，
	 * 视觉仍走既有的环绕波粒子（关卡守着这一条）。</p>
	 *
	 * @param stack     投掷的那一把镖（读取有效技能等级：基准等级取本档 + 技艺提升/回溯，钳 1..5）
	 * @param flightDir 出手方向（点按 = 镖刚拿到的速度向量；长按 = 投掷那一刻的准心方向，
	 *                  因为花瓣段第一 tick 还没有位移）——它同时是环绕波的出生朝向，
	 *                  而每 tick 的环平面法向仍从锚点<b>实时</b>取（镖转弯时环跟着转）
	 */
	public void spawnOrbitWaves(ItemStack stack, Vec3 flightDir) {
		// ★ 第一道门：**这次投掷必须携带环绕技能**（投掷那一刻按住了键二；作者 2026-10-02
		// 第二次裁定）。不按技能键 ⇒ 一枚都不生成。判据是投掷时写在实体上的标记，
		// **不是**这里现读按键（"投掷那一刻读一次"是唯一口径，见 setCarriedSkills）。
		if (!this.orbitSkillCarried) {
			return;
		}
		if (!(level() instanceof ServerLevel server)) {
			return; // 实体由服务端生成（两端各造一枚就成双份）
		}
		int level = BoomerangItem.effectiveSkillLevel(stack, tier().baseSkillLevel());
		int count = BoomerangSkillConfigs.orbitCount(level);
		float damage = BoomerangSkillConfigs.orbitDamage(level);
		int batch = nextOrbitBatch();
		Vec3 dir = flightDir == null ? Vec3.ZERO : flightDir;
		for (int i = 0; i < count; i++) {
			// 既有波实体、既有构造（与充能器/星芒嬗震同一个）：
			// 出生点 = 镖此刻的位置（第一 tick 就会被环绕要素改写到环上）。
			ChargerWaveEntity orbit = new ChargerWaveEntity(level(), position(), dir,
				BoomerangSkillConfigs.ORBIT_WAVE_LEVEL);
			orbit.setFiringBatch(batch); // 同一次投掷的 L 枚共用一个（负数）批次号
			orbit.trySetWaveType(WaveTypes.ATTACK); // 要素 4：伤害那一支的门槛
			orbit.setCustomDamage(damage); // 2 × 等级（既有实体的可选自定义伤害）
			orbit.setOrbitAnchor(getUUID(), BoomerangSkillConfigs.ORBIT_RADIUS,
				BoomerangSkillConfigs.ORBIT_ANGULAR_SPEED,
				BoomerangSkillConfigs.ORBIT_PHASE + Math.PI * 2.0D * (double) i / (double) count);
			server.addFreshEntity(orbit);
		}
	}

	/**
	 * <b>命中之后"要不要掉头"的唯一判定</b>（生物与方块两条命中路径共用这一处）。
	 *
	 * <p>三条判据，顺序固定 —— <b>"飞完一瓣"永远排第一</b>（需求 §3.2 + 批 3 裁定：
	 * 花瓣段不许因为命中而提前返回，额度用完也不行）：</p>
	 * <ol>
	 *   <li>{@code isPetalFlight()} ⇒ 恒 <b>不掉头</b>（花瓣段唯一允许的掉头是"走完 s=1"）；</li>
	 *   <li>这次命中<b>本来就不允许穿过</b>（{@code mayPierceThrough = false}：撞上挖不动的方块）
	 *       ⇒ 照旧掉头（批 1/2 的"撞墙即回"）；</li>
	 *   <li>额度还没用完（{@code quotaLeft > 0}）⇒ 穿过去继续飞；<b>用完 ⇒ 掉头</b>
	 *       （需求 §3.5："额度用完即掉头（= 回到点按那种碰到就回的行为）"）。</li>
	 * </ol>
	 *
	 * @param quotaLeft        该类额度在本次命中<b>扣减之后</b>的余量
	 * @param mayPierceThrough {@code false} = 这次命中不允许穿过（挖不动的方块）
	 * @return {@code true} = 已转入回程（调用方不得再前进）
	 */
	private boolean turnAroundIfNotPiercing(int quotaLeft, boolean mayPierceThrough) {
		if (isPetalFlight()) {
			// ★ 花瓣段（作者 2026-10-02 第三次裁定）：**近程无限制** —— 沿花瓣轨迹上所有方块都破坏、
			// 所有生物都伤害，**不吃任何额度**（"长按自带 1/1"那个暂定值当场作废）。
			// 只有**飞远了**（超出本档"能力范围"）才回到上限规则：那时遇到**超出本档能力**的方块
			// （挖不动 ⇒ mayPierceThrough == false）就「既不破坏也不伤害，直接返回」。
			// 近程遇到挖不动的方块 ⇒ **穿过去继续飞完这一瓣**（"无限制"那一句的最小读法；
			// 需求没写死这一种情形，见报告 §⑥）。
			if (mayPierceThrough || withinCapabilityRange()) {
				return false;
			}
		} else {
			// 点按段（逐字沿用批 3 口径）：撞不动（墙）或额度用完 ⇒ 掉头；否则穿过去继续飞。
			boolean quotaSpent = quotaLeft <= 0;
			if (mayPierceThrough && !quotaSpent) {
				return false;
			}
		}
		// ⚠ 全实体只剩这一个 setReturning(true) 在这条规则里（另外三处分别在距离判据、
		// 去程时间上限、飞完一瓣）——关卡 §29n-4 把"恰好 4 处"钉死，别再复制一份。
		setReturning(true);
		return true;
	}

	/**
	 * <b>花瓣段专用的"能力范围"判据</b>（作者 2026-10-02 第三次裁定第 3 条）：离主人
	 * <b>不超过本档"能力范围"</b>（{@link BoomerangTier#capabilityRange()}）⇒ 近程，破坏与伤害无限制。
	 *
	 * <p>⚠ <b>阈值是待作者确认的暂定值</b>：现在 {@code capabilityRange() == returnDistance()}
	 * （5/10/15/20 格）。它是<b>唯一一处</b>取值点 —— 作者回话后只改
	 * {@code BoomerangTier#capabilityRange()} 那一行，本方法一个字不用动。</p>
	 *
	 * <p>参照点与 {@link #outboundRangeExceeded} <b>同一个</b>（{@code owner.position() + (0,1,0)}）：
	 * 两条距离判据（点按的"飞太远就掉头"与花瓣的"飞远了回到上限"）必须用同一把尺子，
	 * 否则"多远算远"会有两个答案。</p>
	 */
	private boolean withinCapabilityRange() {
		Entity owner = getOwner();
		if (owner == null) {
			return false; // 拿不到主人 ⇒ 按"超出"处理（保守：不再无限制破坏）
		}
		int limit = tier().capabilityRange();
		return position().distanceToSqr(owner.position().add(0.0D, 1.0D, 0.0D)) <= (double) limit * limit;
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

	// ================= 两种模式：花瓣飞行 + 耐久累计（2026-10-02 批 2） =================

	/**
	 * <b>把本次飞行标记成长按（花瓣曲线）</b>并记下曲线锚点与基准角 —— <b>唯一入口</b>，
	 * 由 {@code BoomerangItem#releaseUsing} 在长按投掷时调用（服务端）。
	 *
	 * @param origin    曲线锚点 {@code P}（= 镖的出生点：玩家眼睛下方 0.1 格）
	 * @param baseAngle 基准角 ψ（弧度）—— 出手那一刻水平朝向的<b>数学角</b>
	 *                  {@code atan2(朝向.z, 朝向.x)}，与需求 §3.4.3 的极坐标公式同源
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

	/** 本次飞行累计的耐久损耗（需求 §3.8 的账；只在 {@link #settleWear(ItemStack)} 一次写回）。 */
	public int getFlightWear() {
		return flightWear;
	}

	/**
	 * 往本次飞行的耐久账上记一笔（投掷 −2/−5、每命中一个生物 / 每挖掉一个方块各 −1）。
	 *
	 * <p><b>只记账</b>：不碰物品、不判爆。判爆与写回都在 {@link #settleWear(ItemStack)}。
	 * 非正数直接忽略（调用点不必自己判）。</p>
	 */
	public void addFlightWear(int amount) {
		if (amount > 0) {
			this.flightWear += amount;
		}
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

		// ★ 主人瞬移兜底（作者 2026-10-02 第三次裁定第 4 条）：/tp、传送门换维度、死亡重生…
		// 一旦发现（换维度，或一 tick 跳变超过 OWNER_TELEPORT_JUMP_SQR）就**当场收尾**：
		// 清除实体 + 把镖交还玩家（finishFlight(false)，绝不掉在地上、也不继续飞）。
		if (!level().isClientSide && owner != null && ownerTeleported(owner)) {
			teleportRecover();
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
				return; // 本 tick 不再前进（命中方块且没挖穿 / 已转入回程）：免得钻进墙里
			}
		}

		if (!level().isClientSide && this.entityData.get(DATA_RETURNING)) {
			pickUpItems();
		}
	}

	/**
	 * 去程：命中判定 + 位移（<b>两端各跑一次</b>）。返回 true 表示"本 tick 已转入回程，别再前进"。
	 *
	 * <p><b>第一行就是模式分岔</b>（2026-10-02 批 2）：长按（花瓣曲线）走 {@link #tickPetal()}，
	 * 点按逐字沿用下面这一支（射线命中 + 阻力位移）。</p>
	 *
	 * <p>⚠ <b>批 3 修正</b>：命中判定<b>两种模式共用</b> {@link #checkImpact()} 这一处入口
	 * （花瓣段由 {@link #tickPetal()} 调它，不再"穿过生物/方块无效果"）——"掉头"与"穿过去"
	 * 的分歧只在 {@link #onHitBlock} / {@link #onHitEntity} 内部，按
	 * {@code isPetalFlight()} 与穿刺额度决定（见那两个方法）。这里点按那一支一个字没动。</p>
	 */
	private boolean tickOutbound() {
		if (isPetalFlight()) {
			return tickPetal();
		}
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
	 * <b>长按的花瓣曲线飞行</b>（需求 §3.4；数学与常数全在 {@link BoomerangCurveConfigs}）。
	 *
	 * <p>每 tick 四步，顺序固定：</p>
	 * <ol>
	 *   <li><b>命中判定</b>（<b>服务端</b>）：调<b>与点按同一处</b>的 {@link #checkImpact()}
	 *       ——花瓣段也要能伤生物、挖方块、吃穿刺额度（2026-10-02 批 3 修正 2；
	 *       批 2 这里什么都不做，等于"镖穿过生物/方块无效果"）。
	 *       ⚠ <b>掉头不归它管</b>：{@link #onHitBlock} / {@link #onHitEntity} 内部看到
	 *       {@code isPetalFlight()} 就不会掉头（"必须飞完一瓣才能返回"优先于"额度用完"），
	 *       所以这里的返回值被<b>刻意忽略</b>——花瓣段不会因为命中而提前结束；</li>
	 *   <li><b>推进弧长</b> {@code s += v(s)/L_total}（{@code Δt = 1 tick}；v(s) 关于 s=0.5 对称）；</li>
	 *   <li>{@code s ≥ 1} ⇒ <b>飞完一瓣</b>，服务端 {@link #setReturning(boolean)} 转回程，本 tick 不再前进
	 *       （"必须飞完一瓣才能返回"；客户端不写同步值，等服务端那一份推回来）；</li>
	 *   <li>否则把 {@code s} 反查成 φ（{@link BoomerangCurveConfigs#phiAt}），按
	 *       {@code P + r(φ)·(cos(ψ+φ), 0, sin(ψ+φ))} 求新位置，位移写进 {@code deltaMovement}
	 *       并 {@code setPos}（{@code updateRotation} 靠这个位移反算朝向）。</li>
	 * </ol>
	 *
	 * <p>锚点用同步数据里的 {@link #DATA_PETAL_ORIGIN}（<b>不是</b> {@link #originX}）：
	 * 前者出手时就同步给了客户端，两端才画得出同一条曲线；后者是服务端第一条 tick 记的、
	 * 给"距离判据改基准"留的后路（见 {@link #outboundRangeExceeded}）。</p>
	 *
	 * <p>命中判定必须在<b>位移之前</b>：它用的是"上一 tick 的位移向量"
	 * （点按那一支的 {@code motion} 也来自 {@code getDeltaMovement()}，同一口径）。
	 * 出手第一 tick 还没有位移（长按不走 {@code shootFromRotation}）⇒ {@code checkImpact} 里的
	 * "位移太小直接返回"会把它挡掉，这是预期行为。</p>
	 */
	private boolean tickPetal() {
		if (!level().isClientSide) {
			// 与点按共用同一处命中入口。返回值（= 是否已转入回程）在这里被刻意忽略：
			// 花瓣段唯一允许的掉头是"飞完一瓣"（下面那一支），额度用完不掉头。
			checkImpact();
		}
		BoomerangCurveConfigs.Petal petal = BoomerangCurveConfigs.petal(tier().returnDistance());
		double next = BoomerangCurveConfigs.stepProgress(this.petalProgress, petal);
		if (next >= 1.0D) {
			this.petalProgress = 1.0D;
			if (!level().isClientSide) {
				setReturning(true); // 一瓣走完 ⇒ 回程（不是消失、也不是掉地上）
			}
			return true;
		}
		this.petalProgress = next;
		Vector3f origin = this.entityData.get(DATA_PETAL_ORIGIN);
		double baseAngle = this.entityData.get(DATA_PETAL_ANGLE);
		Vec3 target = new Vec3(
			origin.x() + BoomerangCurveConfigs.offsetX(next, petal.radius(), baseAngle),
			origin.y(), // 不抬升：整瓣在同一水平面内（需求 §六 推断值 #3）
			origin.z() + BoomerangCurveConfigs.offsetZ(next, petal.radius(), baseAngle));
		setDeltaMovement(target.subtract(position()));
		setPos(target.x, target.y, target.z);
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
	 *
	 * <p><b>长按（花瓣）不参与这条判据</b>（2026-10-02 批 2）：花瓣的最远点离锚点恰好 R，
	 * 而锚点是"出手那一刻的主人"——主人但凡挪一步，这条判据就会在花瓣飞到一半时判超距、
	 * 把"必须飞完一瓣"当场掐断。所以长按直接返回 {@code false}，交给
	 * {@link #MAX_OUTBOUND_TICKS} 兜底（一瓣 ≈ 70 tick，远在 200 以内）。</p>
	 */
	private boolean outboundRangeExceeded(Entity owner) {
		if (isPetalFlight()) {
			return false;
		}
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
	 * <p><b>批 3：这是两种飞行模式共用的唯一命中入口</b>（点按的 {@link #tickOutbound} 与
	 * 花瓣段的 {@link #tickPetal} 都调它）——"穿过还是掉头""吃不吃额度""花瓣段要不要提前返回"
	 * 这些分歧全部落在 {@link #onHitBlock} 与 {@link #onHitEntity} 内部，
	 * 这里<b>没有第二份射线/命中代码</b>（关卡 §29n-4 钉着这一点）。</p>
	 *
	 * @return true = 本 tick 别再前进（命中了方块，或某次命中把镖掉头了）
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
				// 命中方块 ⇒ 由 onHitBlock 决定"穿过去（挖掉了且还有额度）"还是"停下（额度用完/挖不动）"。
				return onHitBlock(blockHit.getBlockPos());
			}
			if (entityHit == null) {
				return false;
			}
			if (!onHitEntity(entityHit.getEntity())) {
				return false; // 已经打过 ⇒ 本 tick 收手（否则同一个命中点会无限重来）
			}
			if (isReturning()) {
				// 这一只把生物额度用完了 ⇒ 掉头（点按段；花瓣段不会置位，见 onHitEntity）
				return true;
			}
			start = entityHit.getLocation();
		}
		// 需求 2：超上限"打日志别崩"（不抛、不递归）
		CoeCore.LOGGER.warn("[回旋镖] 单 tick 命中判定超过 {} 次，结束本次去程判定：{}", MAX_IMPACT_LOOPS, this);
		return false;
	}

	/**
	 * 命中生物：只伤一次、记个数、吃一份穿刺额度。返回 false 表示"这只已经打过了"。
	 *
	 * <p>耐久（2026-10-02 批 2：需求 §3.8）：每命中一个生物<b>额外记一笔 −1</b>
	 * （{@link BoomerangTier#WEAR_PER_HIT}）。<b>只记账</b>——见 {@link #flightWear}：
	 * 飞行期间一次都不写回物品，回到玩家手里才由 {@link #settleWear(ItemStack)} 一次结算。</p>
	 *
	 * <p><b>穿刺额度（批 3，需求 §3.5）</b>：每命中一只生物消耗一份<b>生物额度</b>
	 * （{@code 3 × 等级}，见 {@link #ensurePierceQuota()}）。额度用完 ⇒ <b>掉头</b>
	 * （需求："额度用完即掉头"）——但<b>花瓣段除外</b>：{@code isPetalFlight()} 时只消耗额度、
	 * 绝不掉头（"必须飞完一瓣才能返回"优先）。两条判据都只在下面这一段里。</p>
	 */
	private boolean onHitEntity(Entity target) {
		if (target == getOwner() || !entitiesHit.add(target.getId())) {
			return false;
		}
		// 伤害源：跟能量波同一处口径（indirectMagic(this, null)），**不引 mixin、也不冒充玩家攻击**
		// （需求 9：要"玩家攻击"语义的话必须先问作者）。
		target.hurt(damageSources().indirectMagic(this, null), tier().damage());
		hitCount++;
		addFlightWear(BoomerangTier.WEAR_PER_HIT);
		// 需求 §六 推断值 #4：穿透命中同样扣耐久（上面那行已扣）。
		// ⚠ 这里照旧**无条件**记账（不在命中路径里判花瓣/点按 —— 那条分歧只属于
		// turnAroundIfNotPiercing 一处，关卡 §29n-4 钉着这条）。花瓣段不读这份额度，
		// 所以"记了但没用"是零影响（花瓣段的判据是能力范围，见那个 helper）。
		ensurePierceQuota();
		this.pierceMobsLeft = Math.max(0, this.pierceMobsLeft - 1);
		// 额度用完即掉头（生物永远允许"穿过"⇒ mayPierceThrough = true）；
		// 花瓣段那一支由 turnAroundIfNotPiercing 内部挡掉（近程无限制）。
		turnAroundIfNotPiercing(this.pierceMobsLeft, true);
		return true;
	}

	/**
	 * 命中方块：能挖就挖（{@link #mineBlock}），然后按模式与额度决定"穿过去还是掉头"
	 * （2026-10-02 批 3：需求 §3.5）。
	 *
	 * <p>本方法只做两件事：<b>尝试挖</b>（{@code destroyed = mineBlock(pos)}）与
	 * <b>扣额度</b>；"穿过去还是掉头"整条判据交给 {@link #turnAroundIfNotPiercing(int, boolean)}
	 * 那一处（生物那条路径用的是同一个方法）——于是"花瓣段优先"这条规则<b>在代码里只有一处</b>。</p>
	 *
	 * <p>行为对照（三种情形）：</p>
	 * <ol>
	 *   <li><b>花瓣段</b>：挖掉了就吃一份额度，然后<b>一律不掉头</b>（"必须飞完一瓣才能返回"
	 *       优先于"额度用完"）；挖不动的方块也穿过——花瓣曲线是固定路径，与批 2 的花瓣段行为一致；</li>
	 *   <li><b>点按 + 挖不动</b>：{@code mayPierceThrough = false} ⇒ 撞墙，照旧掉头（批 1/2 的行为）；</li>
	 *   <li><b>点按 + 挖掉了</b>：额度没用完就<b>穿过去继续飞</b>，用完则<b>掉头</b>。</li>
	 * </ol>
	 *
	 * <p>额度只在<b>真的挖掉</b>时消耗：挖不动的方块不消耗额度（否则"额度被挖不穿的墙吃掉"
	 * 会让玩家少穿透几个能挖的方块）。</p>
	 *
	 * @return {@code true} = 已转入回程（调用方不得再前进）；挖穿且还有额度时是 {@code false}
	 */
	private boolean onHitBlock(BlockPos pos) {
		boolean destroyed = mineBlock(pos);
		// 照旧**无条件**记账（与 onHitEntity 同形，不在命中路径里判模式：那条分歧只在
		// turnAroundIfNotPiercing 一处）。花瓣段不读这份额度 ⇒ 记了也不影响花瓣行为。
		ensurePierceQuota();
		if (destroyed) {
			this.pierceBlocksLeft = Math.max(0, this.pierceBlocksLeft - 1);
		}
		// 唯一一处"要不要掉头"：点按段挖不动 ⇒ 掉头 / 额度用完 ⇒ 掉头 / 挖掉了且还有额度 ⇒ 穿过去；
		// 花瓣段近程一律不掉头（无限制），飞远了遇挖不动的方块才掉头（见上面那个 helper）。
		return turnAroundIfNotPiercing(this.pierceBlocksLeft, destroyed);
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
	 * <p><b>代价（2026-10-02 批 2 改口径）</b>：挖掉一个方块<b>不再扣能量</b>，改记一笔
	 * <b>−1 耐久</b>（{@link BoomerangTier#WEAR_PER_HIT}）。
	 * ⚠ 旧的 {@code BoomerangTier#mineCost()}（5/10/15/20 点）已被作者推翻并删除（需求 §3.8 + §3.9），
	 * 本方法里那一行 {@code ToolEnergy.canAfford/consume} 也随之删掉——能量只花在投掷那一处。
	 * 与命中生物一样，这里<b>只记账</b>（{@link #flightWear}），不写回物品。</p>
	 *
	 * @return true = 真的挖掉了（{@code destroyBlock} 答应）；false = 任一门槛没过或没挖动。
	 *         <b>批 3 起有返回值</b>：{@link #onHitBlock} 靠它决定"吃不吃穿刺额度、
	 *         穿过去还是掉头"——挖不动的方块不消耗额度，也仍然把镖拦下来（点按段）。
	 */
	private boolean mineBlock(BlockPos pos) {
		if (!(getOwner() instanceof ServerPlayer player)) {
			return false;
		}
		BlockState state = level().getBlockState(pos);
		if (state.isAir()) {
			return false;
		}
		float hardness = state.getDestroySpeed(level(), pos);
		if (hardness < 0.0F) {
			return false;
		}
		BoomerangTier tier = tier();
		if (hardness > tier.maxHardness()) {
			return false;
		}
		if (state.is(tier.incorrectBlocks())) {
			return false;
		}
		// 原版挖掘进度：一 tick 内进度 ≥ 1 才算挖开（i=30 正确工具 / i=100 用错工具）。
		// 这里用**位置敏感**的 hasCorrectToolForDrops —— 它就是 NeoForge 的 doPlayerHarvestCheck
		// （EventHooks.doPlayerHarvestCheck：先取原版单参判定的值，再过 PlayerEvent.HarvestCheck，
		// 让别的模组有机会否决）。单参那个重载在 NeoForge 里是 @Deprecated。
		int i = player.hasCorrectToolForDrops(state, level(), pos) ? 30 : 100;
		if (tier.digSpeed() / (hardness * i) < 1.0F) {
			return false;
		}
		ItemStack stack = getItemStack();
		if (stack.isEmpty()) {
			return false; // 没有镖就没有"临时塞进手里"这一步（能量已不再参与挖掘判定）
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
			// 需求 §3.8：挖掉一个方块 ⇒ 额外 −1 耐久。**只记账、不写回**（见 flightWear 的注释）：
			// 这里绝不能调 BoomerangItem#addWear —— 那正是"耐久被提前打到 0"的那条错路。
			addFlightWear(BoomerangTier.WEAR_PER_HIT);
		}
		return destroyed;
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

	// ================= 交还与兜底：三条尾路径 = 同一处结算（裁定 D14） =================

	/**
	 * 抵达主人 ⇒ 走 {@link #finishFlight(boolean)}（{@code landInWorld = false}：镖交回手里）。
	 *
	 * <p>为什么本方法只留一行：需求 §3.8 的批量结算必须<b>三条尾路径共用同一处</b>，
	 * 否则"跑得比镖快"（回程超时）与"主人没了"（ownerGone）就会绕开结算，玩家可以靠跑位
	 * 逃避爆掉。语义（乘客 playerTouch 吸收、镖走原槽→背包→掉落三步）仍与批 1 逐字一致。</p>
	 */
	private void collect(Entity owner) {
		finishFlight(false);
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
	 * owner 失效兜底（需求 6）：主人没了/死了 ⇒ 先把自己从墙里拔出来，再走
	 * {@link #finishFlight(boolean)}（{@code landInWorld = true}：镖落在世界上）。
	 * 宁可掉在墙上，也不让物品随实体一起消失（<b>除非耐久结算判它爆掉</b>）。
	 */
	private void ownerGone() {
		while (isInWall()) {
			setPos(getX(), getY() + 1.0D, getZ());
		}
		finishFlight(true);
	}

	/**
	 * 回程超时的收尾（Quark bug b 的修复落点）：就地落地 + 消散。
	 *
	 * <p>不写 {@code discard()} 之外的花样：玩家跑得比镖快时，"追不上"的正确结果是
	 * <b>东西还在世界上</b>（原地掉落），而不是永远穿墙追、永不消散。</p>
	 *
	 * <p>⚠ 但<b>耐久结算照样要走</b>（2026-10-02 批 2 / 裁定 D14）：它就是那条"玩家跑得比镖快"
	 * 的路径，绕开它等于给了一条免费躲避爆掉的捷径。</p>
	 */
	private void returnTimedOut() {
		// ⚠ 2026-10-02 第三次裁定改了这条尾路径的落点：作者要求"**归还给玩家本身**"。
		// 旧口径（批 1/批 2：就地落地 spawnAtLocation）已作废 —— 现在与"抵达主人"走同一支
		// finishFlight(false)：镖进原槽 → 背包 → 实在放不下才掉在玩家脚下（giveToPlayer 三步），
		// 乘客一并交给玩家。耐久结算照旧（绕开它就是免费躲避爆掉的捷径）。
		CoeCore.LOGGER.debug("[回旋镖] 回程超时（{} tick）⇒ 直接交还玩家：{}", MAX_RETURN_TICKS, this);
		finishFlight(false);
	}

	/**
	 * <b>主人被传送走了的兜底收尾</b>（作者 2026-10-02 第三次裁定第 4 条）。
	 *
	 * <p>判据见 {@link #ownerTeleported(Entity)}。收尾走 {@link #finishFlight(boolean)}
	 * 的 {@code landInWorld = false} 那一支：<b>乘客交给玩家、镖交给玩家</b>（原槽 → 背包 →
	 * 掉在玩家脚下），然后 {@code discard()}。<b>不掉在地上、不继续飞</b>——这正是作者原话
	 * 「自动清除该实体，并将回旋镖归还给玩家本身」。</p>
	 */
	private void teleportRecover() {
		CoeCore.LOGGER.debug("[回旋镖] 主人被传送（换维度或一 tick 跳变超 {} 格）⇒ 清除实体并交还玩家：{}",
			Math.sqrt(OWNER_TELEPORT_JUMP_SQR), this);
		finishFlight(false);
	}

	/**
	 * <b>主人这一 tick 是不是被传送了</b>（判据与理由见 {@link #OWNER_TELEPORT_JUMP_SQR}）。
	 *
	 * <p>两种情况算传送：① 主人<b>换了维度</b>（{@code owner.level() != level()}，
	 * 传送门 / 指令 / 重生都会命中）；② 主人一 tick 内的位置跳变超过阈值。</p>
	 *
	 * <p>本方法<b>顺带推进</b>上一 tick 位置（每次调用都记录当前点）⇒ 每个服务端 tick 必须
	 * 恰好调用一次；调用点在 {@link #tick()} 顶部，owner 兜底之后。</p>
	 */
	private boolean ownerTeleported(Entity owner) {
		if (owner.level() != level()) {
			return true;
		}
		double dx = owner.getX() - this.lastOwnerX;
		double dy = owner.getY() - this.lastOwnerY;
		double dz = owner.getZ() - this.lastOwnerZ;
		boolean tracked = this.lastOwnerTracked;
		this.lastOwnerX = owner.getX();
		this.lastOwnerY = owner.getY();
		this.lastOwnerZ = owner.getZ();
		this.lastOwnerTracked = true;
		return tracked && dx * dx + dy * dy + dz * dz > OWNER_TELEPORT_JUMP_SQR;
	}

	/**
	 * ★ <b>三条收尾路径的唯一汇合处</b>（{@link #collect} / {@link #returnTimedOut} / {@link #ownerGone}）
	 * —— 耐久在这里<b>一次</b>结算，乘客与镖本身在这里按同一条口径交付。
	 *
	 * <p>顺序固定：<b>先结算耐久</b>（判爆与写回都在 {@link #settleWear(ItemStack)}），
	 * <b>再交付乘客</b>，最后交付镖本身。</p>
	 *
	 * @param landInWorld {@code true} = 镖落在世界上（超时 / 主人没了）；{@code false} = 交还到手里
	 */
	private void finishFlight(boolean landInWorld) {
		ItemStack stack = getItemStack().copy();
		boolean exploded = settleWear(stack);
		Player player = getOwner() instanceof Player owner ? owner : null;
		if (player != null && (exploded || !landInWorld)) {
			// 交还路径：乘客交给玩家（原有语义：playerTouch 吸收）。
			// 爆掉：**并入物也必须先交给玩家**（需求 §3.8 第 4 条 + §六 推断值 #5：
			// "返回时被玩家吸收"，爆掉就不给等于白丢一次挖掘收益）。
			handPassengersToPlayer(player);
		} else {
			// 落地路径（或拿不到玩家）：乘客照旧落地，绝不销毁。
			dropPassengers();
		}
		if (!exploded && !stack.isEmpty()) {
			if (player == null || landInWorld) {
				spawnAtLocation(stack, 0.0F);
			} else {
				giveToPlayer(player, stack);
			}
		}
		// 爆掉时 stack 被丢弃在这里（**不** spawnAtLocation）：物品就此消失，见 settleWear。
		discard();
	}

	/**
	 * ★ <b>本次飞行的耐久结算 —— 全程唯一一次写回</b>（需求 §3.8；裁定 D14）。
	 *
	 * <p>公式：{@code remaining = 耐久上限 − DAMAGE}（= {@code BoomerangItem#getDurability}），
	 * 与累计损耗 {@link #flightWear} 相比：</p>
	 * <ul>
	 *   <li>{@code 累计 < 剩余} ⇒ {@link BoomerangItem#addWear(ItemStack, int)} 写回一次
	 *       ⇒ 结果恒 <b>≥ 1</b>（不留 0 耐久物品）；</li>
	 *   <li>{@code 累计 >= 剩余} ⇒ <b>爆掉</b>：播放 {@code ITEM_BREAK} + 返回 {@code true}。
	 *       调用方负责让物品消失（<b>不</b> {@code spawnAtLocation}）。
	 *       <br>⚠ 判据取 {@code >=} 而不是需求字面的 {@code >}：需求同一段里还钉着
	 *       "不要留下一个耐久为 0 的物品"，而 {@code 累计 == 剩余} 恰好会造出那个 0。
	 *       两条要求在这里只能保一条 ⇒ 保"绝不留 0 耐久"（活着的镖恒有 ≥ 1 点），
	 *       偏差只有"恰好扣完"这一个点，见报告 §⑥ 与待作者确认清单。</li>
	 * </ul>
	 *
	 * @return {@code true} = 镖因耐久不足爆掉（物品必须消失）
	 */
	private boolean settleWear(ItemStack stack) {
		if (this.flightWear <= 0 || stack.isEmpty() || !(stack.getItem() instanceof BoomerangItem boomerang)) {
			return false; // 没磨损 / 不是本模组的镖（老存档兜底）：原样交还
		}
		int remaining = boomerang.getDurability(stack);
		if (this.flightWear >= remaining) {
			level().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
			CoeCore.LOGGER.debug("[回旋镖] 耐久不足爆掉：累计损耗 {} ≥ 剩余 {}：{}", this.flightWear, remaining, this);
			return true;
		}
		boomerang.addWear(stack, this.flightWear); // ← 唯一一处写回（结果 ≥ 1）
		return false;
	}

	/** 把船上的乘客逐个交给玩家（{@code playerTouch} 吸收；掉落物先清掉拾取延迟，否则它什么都不做）。 */
	private void handPassengersToPlayer(Player player) {
		for (Entity passenger : new ArrayList<>(getPassengers())) {
			passenger.stopRiding();
			if (passenger instanceof ItemEntity item) {
				// 刚上船时设了拾取延迟；这里是我们主动交付，先把延迟清掉，
				// 否则 playerTouch 会因为延迟而什么都不做、东西就跟着镖一起消失了。
				item.setPickUpDelay(0);
			}
			passenger.playerTouch(player);
		}
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
		// 本次飞行的耐久账（批 2）：不读回来 = 重载一次就能把欠的耐久一笔勾销。
		this.flightWear = tag.getInt("FlightWear");
		// 穿刺额度（批 3）：-1 表示"还没初始化"（那时不写键，读回来仍是 -1，第一次命中再算）；
		// 已用掉一部分的额度必须读回来，否则"飞出去半趟、卸载区块"就能白刷一份额度。
		this.pierceMobsLeft = tag.contains("PierceMobsLeft") ? tag.getInt("PierceMobsLeft") : -1;
		this.pierceBlocksLeft = tag.contains("PierceBlocksLeft") ? tag.getInt("PierceBlocksLeft") : -1;
		// 技能携带标记（同前）：没有该键 = 老存档/没带 ⇒ false（与"不按技能键"同义）
		this.pierceSkillCarried = tag.getBoolean("PierceSkillCarried");
		this.orbitSkillCarried = tag.getBoolean("OrbitSkillCarried");
		// 花瓣曲线状态（批 2）：模式 + 锚点 + 基准角 + 进度（写侧见 addAdditionalSaveData）。
		if (tag.getBoolean("PetalFlight")) {
			this.entityData.set(DATA_PETAL, true);
			this.entityData.set(DATA_PETAL_ORIGIN, new Vector3f(
				(float) tag.getDouble("PetalOriginX"),
				(float) tag.getDouble("PetalOriginY"),
				(float) tag.getDouble("PetalOriginZ")));
			this.entityData.set(DATA_PETAL_ANGLE, (float) tag.getDouble("PetalAngle"));
			this.petalProgress = tag.getDouble("PetalProgress");
		}
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
		// 本次飞行的耐久账（2026-10-02 批 2）：跨区块重载不许把欠账抹掉，否则
		// "飞出去一趟正好让区块卸载"就成了躲避爆掉的捷径。
		tag.putInt("FlightWear", this.flightWear);
		// 穿刺额度（2026-10-02 批 3）：记过才写（-1 = 还没初始化就不写键，
		// 读回来仍是 -1 ⇒ 第一次命中时按当时的等级现算，与"从没打过东西"完全等价）。
		if (this.pierceMobsLeft >= 0) {
			tag.putInt("PierceMobsLeft", this.pierceMobsLeft);
			tag.putInt("PierceBlocksLeft", this.pierceBlocksLeft);
		}
		// 技能携带标记（作者 2026-10-02 第二次裁定）：投掷那一刻的按键结果，重载后必须还在，
		// 否则"区块卸载再回来"会把这一发带的效果（以及环绕波的生成资格）抹掉。
		tag.putBoolean("PierceSkillCarried", this.pierceSkillCarried);
		tag.putBoolean("OrbitSkillCarried", this.orbitSkillCarried);
		// 花瓣曲线状态（2026-10-02 批 2）：模式 + 锚点 + 基准角 + 进度。
		// ⚠ 这三项平时走同步数据（两端要一起算曲线），但同步数据不进存档 ⇒ 重载一次就会
		// 退化成"点按直线"（进度归零 = 从头再飞一瓣），所以必须各自落一份 NBT。
		if (isPetalFlight()) {
			Vector3f petalOrigin = this.entityData.get(DATA_PETAL_ORIGIN);
			tag.putBoolean("PetalFlight", true);
			tag.putDouble("PetalOriginX", petalOrigin.x());
			tag.putDouble("PetalOriginY", petalOrigin.y());
			tag.putDouble("PetalOriginZ", petalOrigin.z());
			tag.putDouble("PetalAngle", this.entityData.get(DATA_PETAL_ANGLE));
			tag.putDouble("PetalProgress", this.petalProgress);
		}
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
