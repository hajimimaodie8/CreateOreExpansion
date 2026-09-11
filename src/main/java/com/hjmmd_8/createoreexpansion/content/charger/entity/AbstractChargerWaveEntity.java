package com.hjmmd_8.createoreexpansion.content.charger.entity;

import java.util.List;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.WaveLevels;
import com.hjmmd_8.createoreexpansion.content.wave.block.AbstractWaveGateBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.AbstractWaveGateBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.OctaEnergyWaveDifferencerBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.SixFaceDisperserBlock;

import net.createmod.catnip.levelWrappers.SchematicLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 应力充能器能量波实体抽象基类：不渲染模型（视觉靠粒子）。
 *
 * <p><b>职责划分</b>（同包 4 个类协同，按碰撞环境分文件）：</p>
 * <ul>
 *   <li>本类 = 实体骨架：字段、tick 主循环（飞行/寿命/粒子/生物/掉落物/波波碰撞）、
 *       主世界方块遍历分发、NBT、访问器；</li>
 *   <li>{@link WaveMachineActions} = 主世界/结构共用的机器执行器（调级器/波速调节器/
 *       四面差波器/六面差波器的单次命中判定）；</li>
 *   <li>{@link WaveSubLevelCollisions} = Sable 物理结构（sub-level）碰撞协调；</li>
 *   <li>{@link WaveContraptionCollisions} = Create contraption 碰撞协调。</li>
 * </ul>
 *
 * <p>通用行为（模板方法）：服务端飞行、命中判定（生物伤害/掉落物加工/置物台加工/撞墙消散）、
 * 粒子拖尾与命中绽放、加工完成音效；客户端在消散时补充球面均匀扩散绽放。</p>
 *
 * <p>能量波颜色统一按等级（1~5：低=黄、高=绿、伽马=蓝、伊普西龙=紫粉、欧米伽=玫红+金拖尾），
 * 与机型无关——翡翠/蓝宝石等所有应力充能器发射的都是同一种能量波（{@link ChargerWaveEntity}），
 * 粒子统一用原版染色粒子（DustParticleOptions），无需注册任何自定义粒子类型。</p>
 *
 * <p>等级 1~5 见 {@link WaveLevels}：速度 2/4/6/7/8 格/秒、伤害 4/6/10/14/18。</p>
 */
public abstract class AbstractChargerWaveEntity extends Entity
	implements com.hjmmd_8.createoreexpansion.content.energyfield.FieldedEntity {

	/**
	 * 最大存活 tick（200 tick = 10 秒）：能量波飞出充能器后即使不撞墙也会自动消散，
	 * 防止充能器持续发射导致波实体无限累积（日志曾见波 tick=683 仍在飞行），
	 * 这是"加机器后卡顿"的主要来源之一。
	 *
	 * <p>10 秒足够覆盖绝大多数用法：低波飞 20 格、高波 40 格、伽马 60 格；
	 * 也给波波碰撞留出足够的相遇窗口（两波从相对充能器射出到相遇通常 &lt; 5 秒）。</p>
	 */
	protected static final int MAX_LIFETIME_TICKS = 200;

	/** 最大飞行距离（格）：超过即消散，与寿命上限互为兜底。 */
	protected static final double MAX_TRAVEL_DISTANCE = 64.0;

	protected int waveLevel;
	protected Vec3 movement = Vec3.ZERO;

	/**
	 * 速度修正值（格/秒，可正可负）：由波速调节器按转速分档叠加施加。
	 * <ul>
	 *   <li><b>叠加语义</b>：每次过波速调节器在此值上 +/− 档位量（多次叠加累积）；</li>
	 *   <li><b>继承</b>：差波器分裂出的子波、拐弯/反弹后的波均保持同一修正值
	 *       （实际速度 = 等级基础速度 + 此值，受上下限约束）；</li>
	 *   <li>与等级解耦：等级基础速度（2/4/6）不变，此值仅为额外叠加层。</li>
	 * </ul>
	 */
	protected double speedOffset;

	/**
	 * 电荷极性（能量着电器赋予）：null = 不带电（能量场不作用）。
	 * 携带电荷的波在能量加速/偏转场中受洛伦兹式作用（见 FieldedEntity）。
	 */
	protected com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity charge;

	/** 出生点：用于计算飞行距离上限。 */
	protected Vec3 spawnPos;

	/** 已与另一波碰撞（防同 tick 双方各触发一次爆炸）。 */
	private boolean collided;

	/** 上一 tick 是否处于任意能量场内（诊断日志：只记状态翻转，避免刷屏）。仅服务端使用，不存 NBT。 */
	private boolean wasInsideField;

	/**
	 * 调级器增强延迟（格）：穿过能量调级器（顺基准）后还需飞行 0.5 格
	 * 才升级（0.5 格 ÷ 波速 v = 1/(2v) 秒）。0 = 无待升级。
	 */
	protected float boostRemaining;

	/**
	 * 调级器增强步数（本次延迟升级一次提升几级）：翡翠/蓝宝石恒为 1；
	 * 星辉石调级器按授予时的本机转速可为 2（单次 +2，等级仍封顶于
	 * {@link WaveLevels#MAX_LEVEL}）。由 {@link WaveMachineActions} 在授予
	 * 延迟升级时从调级器 BE 机型参数（{@code getBoostStepForSpeed()}）写入。
	 */
	protected int boostStep = 1;

	/**
	 * 当前渲染颜色（RGB 0-1）：每 tick 向目标颜色插值靠近，实现升降级/遣返时的平滑渐变。
	 * 目标色：正常 = 当前等级色；待升级（boostRemaining &gt; 0）时提前变为下一等级色，
	 * 使波穿出调级器后颜色即开始过渡，而非延迟结束瞬间跳变。
	 */
	protected Vec3 renderColor;

	/**
	 * 能量波核心加工逻辑（独立抽取至 {@link ChargerWaveProcessor}）：
	 * 命中掉落物/置物台时按 charging 配方匹配 → 消耗输入 → 产出结果。
	 */
	private ChargerWaveProcessor processor;

	/** 主世界/结构共用的机器执行器（调级器/波速/差波器命中判定）。 */
	private final WaveMachineActions actions;
	/** Sable 物理结构碰撞协调。 */
	private final WaveSubLevelCollisions subLevelCollisions;
	/** Create contraption 碰撞协调。 */
	private final WaveContraptionCollisions contraptionCollisions;

	protected AbstractChargerWaveEntity(EntityType<?> type, Level level) {
		super(type, level);
		this.processor = new ChargerWaveProcessor(level, 1);
		this.renderColor = getWaveColor();
		this.actions = new WaveMachineActions(this);
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
	 * @param waveLevel   波等级（1=低，2=高，3=伽马）
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

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(WAVE_LEVEL, 0);
		builder.define(SPEED_OFFSET, 0f);
		builder.define(CHARGE, 0);
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
		setPos(position().add(step));

		// 诊断日志（仅服务端；控制器方块完成后移除）：带电波进出场状态翻转 + 场内每 10 tick 修正摘要
		if (level() instanceof net.minecraft.server.level.ServerLevel server) {
			boolean inside = charge != null
				&& com.hjmmd_8.createoreexpansion.content.energyfield.EnergyFields.isInAnyField(level(), position());
			if (inside != wasInsideField) {
				wasInsideField = inside;
				CreateOreExpansion.LOGGER.info("[能量场] 波#{} {}场  pos={}  电荷={}  场内场数={}",
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
				CreateOreExpansion.LOGGER.info("[能量场] 波#{} 场内修正  pos={}  速度 {:.2f}→{:.2f} 格/秒  偏角 {:.1f}°",
					getId(), position(), oldLen, newLen, Math.toDegrees(Math.acos(dot)));
			}
		}

		// CC&A 特斯拉线圈赋电荷：波不带电且贴近线圈（半径 1 格立方体）时，线圈耗电赋一次
		// 随机极性电荷（放电闪光 + 1 秒冷却；CC&A 未安装时静默跳过）。每 tick 探测，防高速波漏检。
		if (charge == null && level() instanceof net.minecraft.server.level.ServerLevel) {
			try {
				com.hjmmd_8.createoreexpansion.compat.createaddition.TeslaCoilWaveCharger.chargeNearbyCoil(this);
			} catch (Throwable ignored) {
				// 联动异常：波照常飞行
			}
		}

		// 调级器增强延迟：穿过顺基准调级器后累计飞行距离，满 0.5 格（= 1/(2v) 秒）后等级 +boostStep
		// （翡翠/蓝宝石恒 +1；星辉石按授予时的转速可为 +2）。提升等级封顶于 WaveLevels.MAX_LEVEL（5），
		// 星辉石 +2 从 4 级升到 6 时实际停在 5（欧米伽），不会超上限。
		if (boostRemaining > 0) {
			boostRemaining -= (float) (getSpeedBlocks() / 20d);
			if (boostRemaining <= 0) {
				boostRemaining = 0;
				setWaveLevel(Math.min(waveLevel + boostStep, WaveLevels.MAX_LEVEL));
				boostStep = 1;
			}
		}

		// 平滑渐变：目标色 = 当前等级色；待升级（boostRemaining>0）时提前渐变到提升后的等级色
		// （按 boostStep 预览，封顶 MAX_LEVEL），使波穿出调级器后颜色即开始过渡，而非延迟结束瞬间跳变
		Vec3 targetColor = boostRemaining > 0
			? getWaveColorForLevel(Math.min(waveLevel + boostStep, WaveLevels.MAX_LEVEL))
			: getWaveColor();
		renderColor = renderColor.lerp(targetColor, 0.15d);
		// 距离足够近则直接贴合目标色，避免无限逼近
		if (renderColor.distanceToSqr(targetColor) < 1.0E-5d)
			renderColor = targetColor;

		// 飞行粒子（密集，沿移动方向散布）：主体 = 当前渲染色（欧米伽=玫红），
		// 欧米伽（5 级）额外每 tick 叠 1 颗金色尾迹点缀 —— 金色只占少数，避免整条波看起来发黄
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ChargerWaveFx.waveParticle(renderColor, 0.45f), getX(), getY(), getZ(), 6,
				movement.x * 0.12, movement.y * 0.12, movement.z * 0.12, 0.03);
			if (isOmega())
				server.sendParticles(ChargerWaveFx.waveParticle(OMEGA_GOLD, 0.5f), getX(), getY(), getZ(), 1,
					movement.x * 0.3, movement.y * 0.3, movement.z * 0.3, 0.05);
		} else if (ponderScene) {
			// Ponder 场景：客户端粒子（PonderLevel.addParticle 已实现，会渲染在场景中）
			for (int i = 0; i < 6; i++) {
				level().addParticle(ChargerWaveFx.waveParticle(renderColor, 0.45f), getX(), getY(), getZ(),
					movement.x * 0.12, movement.y * 0.12, movement.z * 0.12);
			}
			if (isOmega())
				level().addParticle(ChargerWaveFx.waveParticle(OMEGA_GOLD, 0.5f), getX(), getY(), getZ(),
					movement.x * 0.3, movement.y * 0.3, movement.z * 0.3);
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

		// 命中生物：造成伤害（碰撞盒改为 0.2 小盒后，检测范围适当放大补偿，避免波穿过生物不造成伤害）
		List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, hitBox, e -> e.isAlive());
		if (!entities.isEmpty()) {
			LivingEntity target = entities.get(0);
			if (!(target instanceof Player player) || !player.isCreative()) {
				target.hurt(level().damageSources()
					.indirectMagic(this, null), getDamage());
			}
			ChargerWaveFx.burst(level(), position(), renderColor);
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
		// 核心加工逻辑已抽取至 ChargerWaveProcessor（配方匹配 → 消耗输入 → 产出结果）
		if (processor.processItemEntity(items.get(0))) {
			ChargerWaveFx.burst(level(), position(), renderColor);
			discard();
		}
	}

	/**
	 * 方块碰撞检测：遍历碰撞盒覆盖的方块，按优先级处理
	 * 避雷针 → 能量调级器 → 置物台/工作台（有物品槽）→ 撞墙。
	 * 命中即 {@code burst + discard}；调级器穿过/遣返则推出方块外继续飞行。
	 *
	 * <p><b>两阶段判定</b>（主世界优先，结构补充，互不劫持）：</p>
	 * <ol>
	 *   <li><b>主世界判定</b>：遍历波碰撞盒覆盖的主世界方块（机器/地形），命中即处理；
	 *       各机器的单次命中判定委托 {@link WaveMachineActions}；</li>
	 *   <li><b>动态结构判定</b>：主世界无命中（波在空旷处或结构区域——结构方块已从主世界
	 *       搬入 Sable 虚拟子世界 / contraption 数据内，主世界读不到）时，依次尝试
	 *       contraption（{@link WaveContraptionCollisions}）与 Sable sub-level
	 *       （{@link WaveSubLevelCollisions}）。</li>
	 * </ol>
	 */
	private void handleBlockCollisions() {
		boolean hitSolid = false;
		for (BlockPos pos : BlockPos.betweenClosed(
			Mth.floor(getBoundingBox().minX), Mth.floor(getBoundingBox().minY), Mth.floor(getBoundingBox().minZ),
			Mth.floor(getBoundingBox().maxX), Mth.floor(getBoundingBox().maxY), Mth.floor(getBoundingBox().maxZ))) {
			BlockState state = level().getBlockState(pos);
			if (state.isAir())
				continue;
			// 伽马能量加工（≥3 级）：伽马/伊普西龙/欧米伽波命中强化避雷针各 +1 充能，波消散
			if (level().getBlockEntity(pos) instanceof ReinforcedLightningRodBlockEntity rod) {
				if (waveLevel >= 3)
					rod.onGammaWaveHit();
				ChargerWaveFx.burst(level(), position(), renderColor);
				discard();
				return;
			}
			// 能量波闸（调级器/波速调节器，翡翠/蓝宝石）：面板通道 + 应力调制。
			// 用基类 Block 统一命中，按 modulatesWaveLevel() 区分走等级调制还是速度调制
			if (state.getBlock() instanceof AbstractWaveGateBlock waveGate
				&& level().getBlockEntity(pos) instanceof AbstractWaveGateBlockEntity gateBe) {
				boolean isRegulator = waveGate.modulatesWaveLevel();
				boolean vanish;
				if (isRegulator) {
					vanish = actions.handleRegulator(gateBe, pos, getBoundingBox().getCenter());
				} else {
					vanish = actions.handleWaveSpeedRegulator(gateBe, pos, getBoundingBox().getCenter());
				}
				if (vanish) {
					ChargerWaveFx.burst(level(), position(), renderColor);
					discard();
					return;
				}
				// 穿过/遣返：把波从方块中心推出方块外，确保碰撞盒完全离开，
				// 避免下一 tick 仍在方块内触发二次判定（二次判定会让降级波按 1 级消失、升级延迟被反复重置）
				setPos(Vec3.atCenterOf(pos).add(movement.scale(1.0d)));
				return;
			}
			// 能量波差器：多开口均摊分发（1 开口遣返 / 2 开口穿过 / ≥3 开口均摊降级）
			if (state.getBlock() instanceof EnergyWaveDisperserBlock) {
				boolean vanish = actions.handleDisperser(state, pos, getBoundingBox().getCenter(), null);
				if (vanish) {
					ChargerWaveFx.burst(level(), position(), renderColor);
					discard();
					return;
				}
				// 分裂：母波已在执行器内静默 discard（无撞墙特效），直接结束本 tick
				if (!isAlive())
					return;
				setPos(Vec3.atCenterOf(pos).add(movement.scale(1.0d)));
				return;
			}
			// 六面能量波差器：无朝向，6 面独立开关（1 遣返 / 2 穿过 / 3-4 降一级均摊 / 5-6 降二级均摊）
			if (state.getBlock() instanceof SixFaceDisperserBlock) {
				boolean vanish = actions.handleSixFaceDisperser(state, pos, getBoundingBox().getCenter(), null);
				if (vanish) {
					ChargerWaveFx.burst(level(), position(), renderColor);
					discard();
					return;
				}
				if (!isAlive())
					return; // 分裂：母波静默消散
				setPos(Vec3.atCenterOf(pos).add(movement.scale(1.0d)));
				return;
			}
			// 八面能量波差器：8 口（4 正交 + 4 斜），1 遣返 / 2 转向 / 3-4 降 1 / 5-6 降 2 / 7-8 降 3
			if (state.getBlock() instanceof OctaEnergyWaveDifferencerBlock) {
				boolean vanish = actions.handleOctaDisperser(state, pos, getBoundingBox().getCenter(), null);
				if (vanish) {
					ChargerWaveFx.burst(level(), position(), renderColor);
					discard();
					return;
				}
				if (!isAlive())
					return; // 分裂：母波静默消散
				setPos(Vec3.atCenterOf(pos).add(movement.scale(1.0d)));
				return;
			}
			// 星辉波变器：入口侧开口则处理——对面开口→变体波携带扫描属性从对侧穿出；
			// 对面关闭→原路遣返（等级不变）；入口关闭/机壳面/竖直撞击→按撞墙消散
			if (state.getBlock() instanceof com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveTransmuterBlock) {
				StellarWaveTransmuterPass.Result pass = StellarWaveTransmuterPass.tryConvert(this, pos);
				if (pass == StellarWaveTransmuterPass.Result.HIT_WALL) {
					hitSolid = true; // 波口关闭 / 撞灯盘·轴口面：不可穿，波撞墙消散
					continue;
				}
				// CONVERTED：已转成变体波（原波静默 discard）；
				// BOUNCED：已反向并推出方块外（等级不变），继续飞行
				return;
			}
			IItemHandler handler = level().getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (handler != null) {
				// 命中带物品槽方块（置物台/工作台/工作盆…）。普通波：只按 charging 配方加工；
				// 变体波覆写本钩子，用自己携带的加工机配方类型处理槽内物品（链式远程加工）。
				if (handleItemInventoryBlock(handler, pos))
					return; // 钩子已处理（可能加工成功并继续/已消散），本 tick 结束
				// 无匹配物品：同样视为撞墙，波在此消散（不穿过置物台/工作台）
				ChargerWaveFx.burst(level(), position(), renderColor);
				discard();
				return;
			}
			hitSolid = true;
		}
		if (hitSolid) {
			ChargerWaveFx.burst(level(), position(), renderColor);
			discard();
			return;
		}

		// 阶段 2：动态结构判定（补充，不劫持主世界）——主世界无命中时：
		// a) Create contraption（动力轴承/矿车装配站装配）；b) Sable sub-level（物理结构）
		if (contraptionCollisions.tryHandle())
			return;
		subLevelCollisions.tryHandle();
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
	protected boolean handleItemInventoryBlock(IItemHandler handler, BlockPos pos) {
		processor.processBlockHandler(handler, pos);
		return false; // 普通波：无论匹配与否都按撞墙消散（调用方处理特效）
	}

	/**
	 * 波波碰撞：两个能量波相遇时相互湮灭，在相遇点触发范围能量爆炸。
	 *
	 * <p>爆炸特性：</p>
	 * <ul>
	 *   <li><b>爆炸等级</b> = 两个波等级的较小值（min）；</li>
	 *   <li><b>爆炸范围</b> = 以碰撞点为中心、半径 = 爆炸等级的水平正方形区域
	 *       （半径 1 → 3×3，半径 2 → 5×5，半径 3 → 7×7），不破坏地形；</li>
	 *   <li><b>区域内生物</b>：受到该等级波撞击生物的等量伤害（低 4 / 高 6 / 伽马 10）；</li>
	 *   <li><b>区域内掉落物 / 置物台物品</b>：按爆炸等级直接执行充能加工（复用
	 *       {@link ChargerWaveProcessor}，含能量工具充能与普通物品配方转化）；</li>
	 *   <li><b>粒子</b>：比撞墙绽放（30 个）更密集的爆炸扩散粒子。</li>
	 * </ul>
	 *
	 * @param other 碰撞的另一个波
	 */
	private void handleWaveCollision(AbstractChargerWaveEntity other) {
		int boomLevel = Math.min(waveLevel, other.waveLevel);
		// 碰撞点取两波中心中点
		Vec3 center = position().add(other.position()).scale(0.5);

		// 1. 范围爆炸：粒子 + 音效 + 区域效果
		ChargerWaveFx.triggerBoom(level(), this, center, renderColor, other.renderColor, boomLevel);

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
			ChargerWaveFx.burstParticles(level(), position(), renderColor);
		}
		super.remove(reason);
	}

	/** 速度下限（格/秒）：减速不能低于此值。 */
	protected static final double MIN_SPEED = 0.5d;
	/** 速度上限（格/秒）：加速不能超过此值。 */
	protected static final double MAX_SPEED = 10.0d;

	/**
	 * 移动速度（格/秒）：等级基础速度（查 {@link WaveLevels#baseSpeed}，低 2 / 高 4 /
	 * 伽马 6 / 伊普西龙 7 / 欧米伽 8）+ 速度修正值（波速调节器叠加），
	 * 夹在 {@link #MIN_SPEED} ~ {@link WaveLevels#maxSpeed}（4/5 级 12，1~3 级 10）之间。
	 * 子类可覆写基础速度（如雷鸣波更快）。
	 */
	protected double getSpeedBlocks() {
		double base = WaveLevels.baseSpeed(waveLevel);
		return Math.max(MIN_SPEED, Math.min(WaveLevels.maxSpeed(waveLevel), base + speedOffset));
	}

	/** 施加一次速度修正（叠加语义：在此值上增加 amount，可正可负）。 */
	protected void addSpeedOffset(double amount) {
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

	/** 波等级（1=低、2=高、3=伽马）。 */
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
	protected void setWaveLevel(int level) {
		this.waveLevel = level;
		this.processor = new ChargerWaveProcessor(level(), level);
		// 同步到客户端（Jade 等级显示）
		this.entityData.set(WAVE_LEVEL, level);
	}

	/** 命中伤害：低 4、高 6、伽马 10、伊普西龙 14、欧米伽 18 —— 查 {@link WaveLevels#damage}。 */
	protected float getDamage() {
		return WaveLevels.damage(waveLevel);
	}

	/** 能量波颜色（RGB 0-1），随波等级统一：1 低=黄、2 高=绿、3 伽马=蓝、4 伊普西龙=紫粉、5 欧米伽=玫红 */
	protected Vec3 getWaveColor() {
		return getWaveColorForLevel(waveLevel);
	}

	/** 指定等级的波颜色（供调级器渐变提前取下一等级色用）——颜色由等级决定，与机型无关 */
	protected Vec3 getWaveColorForLevel(int level) {
		return switch (level) {
			case 2 -> new Vec3(0, 1, 0); // 高：绿
			case 3 -> new Vec3(0, 0.5f, 1); // 伽马：蓝
			case 4 -> new Vec3(1f, 0.35f, 0.85f); // 伊普西龙：紫粉
			case 5 -> new Vec3(1f, 0.25f, 0.45f); // 欧米伽：玫红（主体红，偏粉紫）
			default -> new Vec3(1, 1, 0); // 低/其它：黄
		};
	}

	/** 是否欧米伽（5 级）波：粒子拖尾用金色。 */
	public boolean isOmega() {
		return waveLevel >= 5;
	}

	/** 欧米伽拖尾金色（RGB 0-1）。 */
	public static final Vec3 OMEGA_GOLD = new Vec3(1f, 0.85f, 0.2f);

	/**
	 * 飞行拖尾粒子颜色（RGB 0-1）：欧米伽（5 级）波用金色，其余等级用当前渲染色。
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
	 * @return 新波实体；null 则放弃发射
	 */
	protected abstract AbstractChargerWaveEntity createChildWave(Vec3 pos, Vec3 dir, int level);

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
		tag.putInt("Charge", charge == null ? 0
			: charge == com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity.POSITIVE ? 1 : 2);
		if (spawnPos != null) {
			tag.putDouble("SpawnX", spawnPos.x);
			tag.putDouble("SpawnY", spawnPos.y);
			tag.putDouble("SpawnZ", spawnPos.z);
		}
	}
}
