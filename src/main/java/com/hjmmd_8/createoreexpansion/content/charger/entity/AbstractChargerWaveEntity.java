package com.hjmmd_8.createoreexpansion.content.charger.entity;

import java.util.List;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SableBridges;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;
import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveRegulatorBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergySensingLampBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.SixFaceDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.WaveSpeedRegulatorBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.EnergyWaveDispersal;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.EnergyWaveRegulation;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.SixFaceDispersal;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.WaveSpeedRegulation;

import net.createmod.catnip.levelWrappers.SchematicLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
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
 * <p>通用行为（模板方法）：服务端飞行、命中判定（生物伤害/掉落物加工/置物台加工/撞墙消散）、
 * 粒子拖尾与命中绽放、加工完成音效；客户端在消散时补充球面均匀扩散绽放。</p>
 *
 * <p>子类（翡翠充能波/雷鸣充能波）只需覆写 {@link #getWaveColor()} 定制三档充能态颜色，
 * 粒子统一用原版染色粒子（DustParticleOptions），无需注册任何自定义粒子类型。</p>
 *
 * <p>等级：1=低充能（2 格/秒、4 伤害）、2=高充能（4 格/秒、6 伤害）、3=伽马（6 格/秒、10 伤害）。</p>
 */
public abstract class AbstractChargerWaveEntity extends Entity {

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

	/** 出生点：用于计算飞行距离上限。 */
	private Vec3 spawnPos;

	/** 已与另一波碰撞（防同 tick 双方各触发一次爆炸）。 */
	private boolean collided;

	/** 调试：sub-level 判定日志节流（每 40 tick 打一次，避免刷屏） */
	private int lastSubLevelLogTick = -999;

	/**
	 * 调级器增强延迟（格）：穿过能量调级器（顺基准）后还需飞行 0.5 格
	 * 才升级（0.5 格 ÷ 波速 v = 1/(2v) 秒）。0 = 无待升级。
	 */
	private float boostRemaining;

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

	protected AbstractChargerWaveEntity(EntityType<?> type, Level level) {
		super(type, level);
		this.processor = new ChargerWaveProcessor(level, 1);
		this.renderColor = getWaveColor();
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

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(WAVE_LEVEL, 0);
		builder.define(SPEED_OFFSET, 0f);
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

		// 移动（速度随等级）
		setPos(position().add(movement.scale(getSpeedBlocks() / 20d)));

		// 调级器增强延迟：穿过顺基准调级器后累计飞行距离，满 0.5 格（= 1/(2v) 秒）后等级+1
		if (boostRemaining > 0) {
			boostRemaining -= (float) (getSpeedBlocks() / 20d);
			if (boostRemaining <= 0) {
				boostRemaining = 0;
				setWaveLevel(waveLevel + 1);
			}
		}

		// 平滑渐变：目标色 = 当前等级色；待升级（boostRemaining>0）时提前渐变到下一等级色，
		// 使波穿出调级器后颜色即开始过渡，而非延迟结束瞬间跳变
		Vec3 targetColor = boostRemaining > 0 ? getWaveColorForLevel(waveLevel + 1) : getWaveColor();
		renderColor = renderColor.lerp(targetColor, 0.15d);
		// 距离足够近则直接贴合目标色，避免无限逼近
		if (renderColor.distanceToSqr(targetColor) < 1.0E-5d)
			renderColor = targetColor;

		// 飞行拖尾粒子（密集，沿移动方向散布）——每 tick 6 个（用户反馈 3 个隔 tick 太稀疏，恢复原密度）
		if (level() instanceof ServerLevel server) {
			Vec3 color = renderColor;
			server.sendParticles(ChargerWaveFx.waveParticle(color, 0.45f), getX(), getY(), getZ(), 6,
				movement.x * 0.12, movement.y * 0.12, movement.z * 0.12, 0.03);
		} else if (ponderScene) {
			// Ponder 场景：客户端粒子（PonderLevel.addParticle 已实现，会渲染在场景中）
			Vec3 color = renderColor;
			for (int i = 0; i < 6; i++) {
				level().addParticle(ChargerWaveFx.waveParticle(color, 0.45f), getX(), getY(), getZ(),
					movement.x * 0.12, movement.y * 0.12, movement.z * 0.12);
			}
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

		// 命中掉落物：给能量工具充能 / 普通物品按配方转化（检测范围覆盖移动路径，避免高速跳过）
		AABB sweep = hitBox.expandTowards(movement.x * getSpeedBlocks() / 20d,
			movement.y * getSpeedBlocks() / 20d, movement.z * getSpeedBlocks() / 20d);
		List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, sweep, e -> e.isAlive());
		if (!items.isEmpty()) {
			// 核心加工逻辑已抽取至 ChargerWaveProcessor（配方匹配 → 消耗输入 → 产出结果）
			if (processor.processItemEntity(items.get(0))) {
				ChargerWaveFx.burst(level(), position(), renderColor);
				discard();
			}
			return;
		}

		// 命中方块：置物台/工作台充能、避雷针、调级器，其余方块视为撞墙消散
		handleBlockCollisions();
	}

	/**
	 * 方块碰撞检测：遍历碰撞盒覆盖的方块，按优先级处理
	 * 避雷针 → 能量调级器 → 置物台/工作台（有物品槽）→ 撞墙。
	 * 命中即 {@code burst + discard}；调级器穿过/遣返则推出方块外继续飞行。
	 *
	 * <p><b>两阶段判定</b>（主世界优先，结构补充，互不劫持）：</p>
	 * <ol>
	 *   <li><b>主世界判定</b>：遍历波碰撞盒覆盖的主世界方块（机器/地形），命中即处理；</li>
	 *   <li><b>结构判定</b>：主世界无命中（波在空旷处或物理结构区域——结构方块已从主世界
	 *       搬入 Sable 虚拟子世界，主世界读不到）时，若波中心落在某结构 plot 内，
	 *       切换到结构本地坐标系判定结构上的机器。</li>
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
			// 伽马能量波（3 级）命中强化避雷针：充能进度 +1，波消散（低/高波不充能，按撞墙消散）
			if (level().getBlockEntity(pos) instanceof ReinforcedLightningRodBlockEntity rod) {
				if (waveLevel == 3)
					rod.onGammaWaveHit();
				ChargerWaveFx.burst(level(), position(), renderColor);
				discard();
				return;
			}
			// 能量调级器：面板通道 + 应力波级调制（齿轮旋转方向 + 面板开闭），判定逻辑见 EnergyWaveRegulation
			if (level().getBlockEntity(pos) instanceof EnergyWaveRegulatorBlockEntity regulator) {
				if (handleRegulator(regulator, pos, getBoundingBox().getCenter())) {
					ChargerWaveFx.burst(level(), position(), renderColor);
					discard();
					return;
				}
				// 穿过/遣返：把波从调级器方块中心推出方块外，确保碰撞盒完全离开，
				// 避免下一 tick 仍在方块内触发二次判定（二次判定会让降级波按 1 级消失、升级延迟被反复重置）
				setPos(Vec3.atCenterOf(pos).add(movement.scale(1.0d)));
				return;
			}
			// 波速调节器：面板通道 + 应力速度调制（按转速分档加速/减速，反弹不改速），见 handleWaveSpeedRegulator
			if (level().getBlockEntity(pos) instanceof WaveSpeedRegulatorBlockEntity speedRegulator) {
				if (handleWaveSpeedRegulator(speedRegulator, pos, getBoundingBox().getCenter())) {
					ChargerWaveFx.burst(level(), position(), renderColor);
					discard();
					return;
				}
				setPos(Vec3.atCenterOf(pos).add(movement.scale(1.0d)));
				return;
			}
			// 能量波差器：多开口均摊分发（1 开口遣返 / 2 开口穿过 / ≥3 开口均摊降级），见 handleDisperser
			if (state.getBlock() instanceof EnergyWaveDisperserBlock) {
				CreateOreExpansion.LOGGER.info("[DisperserDebug] tick={} wavePos={} mv={} lvl={} stateFacing={} openN={} openE={} openS={} openW={}",
					tickCount, position(), movement, waveLevel,
					state.getValue(EnergyWaveDisperserBlock.FACING),
					state.getValue(EnergyWaveDisperserBlock.NORTH),
					state.getValue(EnergyWaveDisperserBlock.EAST),
					state.getValue(EnergyWaveDisperserBlock.SOUTH),
					state.getValue(EnergyWaveDisperserBlock.WEST));
				boolean vanish = handleDisperser(state, pos, getBoundingBox().getCenter(), null);
				CreateOreExpansion.LOGGER.info("[DisperserDebug] tick={} result={} mvAfter={}",
					tickCount, vanish ? "VANISH" : "CONTINUE", movement);
				if (vanish) {
					ChargerWaveFx.burst(level(), position(), renderColor);
					discard();
					return;
				}
				// 分裂：母波已在 handleDisperser 内静默 discard（无撞墙特效），直接结束本 tick
				if (!isAlive())
					return;
				setPos(Vec3.atCenterOf(pos).add(movement.scale(1.0d)));
				return;
			}
			// 六面能量波差器：无朝向，6 面独立开关（1 遣返 / 2 穿过 / 3-4 降一级均摊 / 5-6 降二级均摊）
			if (state.getBlock() instanceof SixFaceDisperserBlock) {
				boolean vanish = handleSixFaceDisperser(state, pos, getBoundingBox().getCenter(), null);
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
			// 能量感应灯：被波击中切换为对应波级的亮态（黄/绿/蓝，光照15 + 红石信号），
			// 亮态持续一段时间后自动熄灭（计时在 BE），波消散
			if (state.getBlock() instanceof EnergySensingLampBlock) {
				EnergySensingLampBlock.onWaveHit(level(), pos, waveLevel);
				ChargerWaveFx.burst(level(), position(), renderColor);
				discard();
				return;
			}
			IItemHandler handler = level().getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (handler != null) {
				if (processor.processBlockHandler(handler, pos)) {
					ChargerWaveFx.burst(level(), position(), renderColor);
					discard();
					return;
				}
				// 有物品槽但无匹配物品：同样视为撞墙，波在此消散（不穿过置物台/工作台）
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

		// 阶段 2：结构判定（补充，不劫持主世界）——主世界无命中时，若波中心落在
		// 某物理结构（Sable sub-level）的 plot 内，切换到结构本地坐标系判定结构上的机器。
		SubLevelBridge bridge = SableBridges.get();
		if (bridge != null) {
			SubLevelBridge.Hit hit = bridge.query(level(), getBoundingBox().getCenter());
			if (hit != null) {
				// 调试日志（节流）：进入结构本地判定
				if (tickCount - lastSubLevelLogTick > 40) {
					lastSubLevelLogTick = tickCount;
					CreateOreExpansion.LOGGER.info("[Sable] 波@{} 世界中心 {} → 结构本地 {}",
						position(), getBoundingBox().getCenter(), bridge.toLocal(hit, getBoundingBox().getCenter()));
				}
				handleSubLevelBlockCollisions(bridge, hit);
			}
		}
	}

	/** 调试：描述结构上某位置的机器类型（用于定位差波器等判定失效） */
	private static String machineKindOf(SubLevelBridge bridge, SubLevelBridge.Hit hit, BlockPos pos, BlockState state) {
		if (bridge.getBlockEntity(hit, pos) instanceof ReinforcedLightningRodBlockEntity)
			return "强化避雷针";
		if (bridge.getBlockEntity(hit, pos) instanceof EnergyWaveRegulatorBlockEntity)
			return "能量调级器";
		if (bridge.getBlockEntity(hit, pos) instanceof WaveSpeedRegulatorBlockEntity)
			return "波速调节器";
		if (state.getBlock() instanceof EnergyWaveDisperserBlock)
			return "能量波差器";
		if (state.getBlock() instanceof SixFaceDisperserBlock)
			return "六面能量波差器";
		if (state.getBlock() instanceof EnergySensingLampBlock)
			return "能量感应灯";
		return "普通方块(" + state.getBlock()
			.getDescriptionId() + ")";
	}

	/**
	 * 波在物理结构（Sable sub-level）上的碰撞判定：以结构本地坐标系遍历波覆盖的方块，
	 * 命中结构上的机器（避雷针/调级器/波速调节器/差波器）时执行与主世界相同的判定。
	 *
	 * <p><b>坐标约定</b>：波实体本身始终在主世界（位置/渲染/粒子用世界坐标）；本方法内
	 * {@code movement} 临时切换为结构本地方向参与判定（判定函数读写 {@code this.movement}），
	 * 判定结束后经位姿矩阵转回世界方向；推出方块、分裂子波等产出位置同样转回世界。</p>
	 *
	 * <p>波闸的 4×4 入口中心判定在本地坐标系天然正确：BE 的 pos/FACING 是本地，波中心
	 * 换算成本地后，{@code StaticWaveGateFrame} 用本地 FACING 与本地坐标判定面内偏移。</p>
	 *
	 * @return true = 本 tick 已在结构上处理完毕（穿过/反弹/湮灭），无需再走主世界判定
	 */
	private boolean handleSubLevelBlockCollisions(SubLevelBridge bridge, SubLevelBridge.Hit hit) {
		Vec3 localCenter = bridge.toLocal(hit, getBoundingBox().getCenter());
		Vec3 localMove = bridge.toLocalDir(hit, movement);
		double h = 0.1; // 波盒半宽（sized 0.2）
		AABB localBox = new AABB(localCenter.x - h, localCenter.y - h, localCenter.z - h,
			localCenter.x + h, localCenter.y + h, localCenter.z + h);

		boolean hitSolid = false;
		for (BlockPos pos : BlockPos.betweenClosed(
			Mth.floor(localBox.minX), Mth.floor(localBox.minY), Mth.floor(localBox.minZ),
			Mth.floor(localBox.maxX), Mth.floor(localBox.maxY), Mth.floor(localBox.maxZ))) {
			BlockState state = bridge.getBlockState(hit, pos);
			if (state.isAir())
				continue;

			// 调试日志（节流）：结构上命中的非空气方块
			if (tickCount - lastSubLevelLogTick > 40) {
				lastSubLevelLogTick = tickCount;
				CreateOreExpansion.LOGGER.info("[Sable] 结构判定：本地方块 {} = {}，机器类型：{}",
					pos, state, machineKindOf(bridge, hit, pos, state));
			}

			// 判定前：把波方向临时切换为结构本地方向（判定函数读写 this.movement）
			movement = localMove;
			try {
				// 波前中心（结构本地坐标，与 pos 同坐标系——4×4 入口判定依赖）
				Vec3 localWavePos = bridge.toLocal(hit, getBoundingBox().getCenter());
				// 伽马波命中强化避雷针：充能 +1，波消散
				if (bridge.getBlockEntity(hit, pos) instanceof ReinforcedLightningRodBlockEntity rod) {
					if (waveLevel == 3)
						rod.onGammaWaveHit();
					ChargerWaveFx.burst(level(), position(), renderColor);
					discard();
					return true;
				}
				// 能量调级器：面板通道 + 等级调制
				if (bridge.getBlockEntity(hit, pos) instanceof EnergyWaveRegulatorBlockEntity regulator) {
					if (handleRegulator(regulator, pos, localWavePos)) {
						ChargerWaveFx.burst(level(), position(), renderColor);
						discard();
						return true;
					}
					setPos(bridge.toWorld(hit, Vec3.atCenterOf(pos).add(movement.scale(1.0d))));
					return true;
				}
				// 波速调节器：面板通道 + 速度调制（反弹不改速）
				if (bridge.getBlockEntity(hit, pos) instanceof WaveSpeedRegulatorBlockEntity speedRegulator) {
					if (handleWaveSpeedRegulator(speedRegulator, pos, localWavePos)) {
						ChargerWaveFx.burst(level(), position(), renderColor);
						discard();
						return true;
					}
					setPos(bridge.toWorld(hit, Vec3.atCenterOf(pos).add(movement.scale(1.0d))));
					return true;
				}
				// 能量波差器：反弹/拐弯/分裂（分裂子波在结构本地出生，经 frame 转回世界后加入主世界）
				if (state.getBlock() instanceof EnergyWaveDisperserBlock) {
					boolean vanish = handleDisperser(state, pos, localWavePos, hit);
					if (vanish) {
						ChargerWaveFx.burst(level(), position(), renderColor);
						discard();
						return true;
					}
					if (!isAlive())
						return true; // 分裂：母波静默消散
					setPos(bridge.toWorld(hit, Vec3.atCenterOf(pos).add(movement.scale(1.0d))));
					return true;
				}
				// 六面能量波差器
				if (state.getBlock() instanceof SixFaceDisperserBlock) {
					boolean vanish = handleSixFaceDisperser(state, pos, localWavePos, hit);
					if (vanish) {
						ChargerWaveFx.burst(level(), position(), renderColor);
						discard();
						return true;
					}
					if (!isAlive())
						return true;
					setPos(bridge.toWorld(hit, Vec3.atCenterOf(pos).add(movement.scale(1.0d))));
					return true;
				}
				// 结构上的其它方块（含能量感应灯/置物台——本地无 Capability 查询入口）→ 视为撞墙
				hitSolid = true;
			} finally {
				// 判定结果（反弹/穿出方向）从本地方向转回世界方向
				movement = bridge.toWorldDir(hit, movement);
			}
		}
		if (hitSolid) {
			ChargerWaveFx.burst(level(), position(), renderColor);
			discard();
		}
		return true;
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

	/**
	 * 调级器增强延迟（格）：穿过顺基准调级器后还需飞行 0.5 格
	 * 才升级（0.5 格 ÷ 波速 v = 1/(2v) 秒）。0 = 无待升级。
	 */
	private static final float BOOST_DISTANCE = 0.5f;

	/**
	 * 处理一次调级器判定。
	 *
	 * @param regulator 调级器方块实体
	 * @param pos       调级器方块位置（与 wavePos 同坐标系：主世界=世界坐标，结构=本地坐标）
	 * @param wavePos   波前中心（必须与 pos 同坐标系——结构场景传本地坐标，否则 4×4 入口判定失真）
	 * @return true = 波应在原地湮灭（调用方负责 burst + discard）；false = 波继续（已按结果
	 *         升级/降级/反转 movement，调用方负责推出方块外）
	 */
	private boolean handleRegulator(EnergyWaveRegulatorBlockEntity regulator, BlockPos pos, Vec3 wavePos) {
		EnergyWaveRegulation.Result result = EnergyWaveRegulation.get()
			.resolve(regulator, wavePos, movement, waveLevel);
		switch (result) {
			case VANISH -> {
				// 齿轮端/入口关闭：如撞墙消失，无爆炸
				return true;
			}
			case VANISH_GAMMA_BOOM -> {
				// 伽马波（3级）顺基准升级无路可升 → 3 级伽马爆炸后湮灭
				ChargerWaveFx.triggerBoom(level(), this, position(), renderColor, null, 3);
				return true;
			}
			case VANISH_LOW_BOOM -> {
				// 1 级波逆基准降级无路可降 → 1 级小范围爆炸后湮灭
				ChargerWaveFx.triggerBoom(level(), this, position(), renderColor, null, 1);
				return true;
			}
			case PASS_UNCHANGED -> {
				// 无应力双开口：等级不变，正常穿过
			}
			case BOUNCE -> {
				// 无应力单开口：等级不变，原路遣返（折 180° 原路返回）
				movement = movement.scale(-1);
			}
			case PASS_BOOST_LATER -> {
				// 顺基准双开口：穿过，延迟升级（飞行 0.5 格 = 1/(2v) 秒后等级+1）
				boostRemaining = BOOST_DISTANCE;
			}
			case PASS_DOWNGRADE -> {
				// 逆基准双开口：穿过，立即降级
				setWaveLevel(waveLevel - 1);
			}
			case BOUNCE_DOWNGRADE -> {
				// 有应力单开口：降级并原路遣返（1 级波已在判定层走 VANISH_LOW_BOOM 爆炸，这里仅 ≥2 级）
				setWaveLevel(waveLevel - 1);
				movement = movement.scale(-1);
			}
		}
		return false;
	}

	/**
	 * 处理一次波速调节器判定。
	 * <ul>
	 *   <li><b>VANISH</b>：齿轮端/入口关闭 → 撞墙湮灭；</li>
	 *   <li><b>BOUNCE</b>：单开口 → 纯折返（速度不变）；</li>
	 *   <li><b>PASS_UNCHANGED</b>：无应力双开口 → 速度不变穿过；</li>
	 *   <li><b>PASS_SPEED_UP/DOWN</b>：有应力双开口 → 按转速分档对 {@code speedOffset}
	 *       叠加加速/减速量（叠加语义，可多次累积），速度保持穿过。</li>
	 * </ul>
	 *
	 * @param speedRegulator 波速调节器方块实体
	 * @param pos            波速调节器方块位置
	 * @return true = 波应撞墙湮灭（调用方负责 burst + discard）；false = 波继续
	 */
	private boolean handleWaveSpeedRegulator(WaveSpeedRegulatorBlockEntity speedRegulator, BlockPos pos, Vec3 wavePos) {
		WaveSpeedRegulation.Result result = WaveSpeedRegulation.get()
			.resolve(speedRegulator, wavePos, movement);
		switch (result) {
			case VANISH -> {
				return true; // 齿轮端/入口关闭：撞墙湮灭
			}
			case BOUNCE -> {
				// 单开口：纯折返，速度不变
				movement = movement.scale(-1);
				return false;
			}
			case PASS_UNCHANGED -> {
				// 无应力双开口：速度不变穿过
				return false;
			}
			case PASS_SPEED_UP -> {
				// 顺基准：加速（按转速分档叠加）
				float amount = WaveSpeedRegulation.offsetForSpeed(speedRegulator.getSpeed());
				addSpeedOffset(amount);
				return false;
			}
			case PASS_SPEED_DOWN -> {
				// 逆基准：减速（按转速分档叠加）
				float amount = WaveSpeedRegulation.offsetForSpeed(speedRegulator.getSpeed());
				addSpeedOffset(-amount);
				return false;
			}
		}
		return false;
	}

	/**
	 * 处理一次能量波差器判定（多开口均摊分发）。
	 *
	 * <p>判定逻辑（入口映射、开口统计、决策）已抽取至 {@link EnergyWaveDispersal}，
	 * 此处仅执行结果：反弹 / 拐弯 / 均摊分裂 / 撞墙湮灭。</p>
	 *
	 * @param state   差器方块状态（FACING + 4 开口属性）
	 * @param pos     差器方块位置（与 wavePos 同坐标系）
	 * @param wavePos 波前中心（结构场景为本地坐标，用于 4×4 入口判定）
	 * @param frame   波所在 sub-level（结构本地坐标系，分裂子波转回世界用）；null = 主世界
	 * @return true = 波应撞墙湮灭（调用方负责 burst + discard）；false = 波继续（反弹/拐弯，
	 *         调用方负责推出方块外；若本波已被静默 discard——分裂场景——调用方检测 isAlive()==false 直接结束）
	 */
	private boolean handleDisperser(BlockState state, BlockPos pos, Vec3 wavePos, SubLevelBridge.Hit frame) {
		EnergyWaveDispersal.Result result = EnergyWaveDispersal.handle(state, movement, wavePos, pos, waveLevel);
		Direction facing = state.getValue(EnergyWaveDisperserBlock.FACING);

		switch (result) {
			case VANISH -> {
				// 机壳面 / 入口关闭 / 1 级波分裂降级无路可降 → 撞墙湮灭
				return true;
			}
			case BOUNCE -> {
				// 单开口：原路遣返（反弹），等级不变
				movement = movement.scale(-1);
				return false;
			}
			case TURN -> {
				// 双开口：从入口进、从另一开口出（拐弯），等级不变
				Direction worldOut = EnergyWaveDisperserBlock.worldDirOf(facing,
					EnergyWaveDispersal.exitsOf(state, movement).get(0));
				movement = Vec3.atLowerCornerOf(worldOut.getNormal());
				return false;
			}
			case SPLIT -> {
				// ≥3 开口：其余每个开口均摊发射降一级的波
				int childLevel = waveLevel - 1;
				Vec3 center = Vec3.atCenterOf(pos);
				for (Direction modelSide : EnergyWaveDispersal.exitsOf(state, movement)) {
					Direction worldOut = EnergyWaveDisperserBlock.worldDirOf(facing, modelSide);
					// 在差器方块中心沿出口方向外推 1 格出生，确保碰撞盒离开差器
					AbstractChargerWaveEntity child = createChildWave(
						center.add(Vec3.atLowerCornerOf(worldOut.getNormal())), worldOut, childLevel);
					if (child != null) {
						if (frame != null) {
							// 结构本地出生 → 转回世界坐标/方向（波必须出生在真实世界）；
							// spawnPos 同步修正，否则距离上限检查会把子波当"飞太远"立即消散
							SubLevelBridge b = SableBridges.get();
							if (b != null) {
								child.setPos(b.toWorld(frame, child.position()));
								child.spawnPos = b.toWorld(frame, child.spawnPos);
								child.movement = b.toWorldDir(frame, child.movement);
							}
						}
						// 子波继承母波的速度修正（波速调节器叠加值贯穿分裂传播链）
						child.addSpeedOffset(this.speedOffset);
						level().addFreshEntity(child);
					}
				}
				// 母波静默消失（能量已均摊分发到子波；不触发撞墙湮灭特效，
				// 调用方见 isAlive()==false 直接结束）
				this.discard();
				return false;
			}
		}
		return false;
	}

	/**
	 * 创建降一级的"子波"（供差器均摊分发）。子类实现为各自能量波类型的新实例。
	 *
	 * @param pos     出生位置（已外推到差器外）
	 * @param facing  发射方向（出口开口朝向）
	 * @param level   子波等级（= 原波等级 - 1）
	 * @return 新波实体；null 则放弃发射
	 */
	protected abstract AbstractChargerWaveEntity createChildWave(Vec3 pos, Direction facing, int level);

	/**
	 * 六面能量波差器处理：无朝向，入口 = 运动反方向世界面（6 面皆可开口）。
	 * 规则：入口关闭→撞墙消失；1 开口→反弹；2 开口→拐弯；3-4 开口→其余开口各发降一级子波；
	 * 5-6 开口→其余开口各发降二级子波（新增）。
	 *
	 * @param pos   差器方块位置（与 wavePos 同坐标系）
	 * @param wavePos 波前中心（结构场景为本地坐标，用于 4×4 入口判定）
	 * @param frame 波所在 sub-level（结构本地坐标系，分裂子波转回世界用）；null = 主世界
	 * @return true = 波应撞墙湮灭；false = 波继续（调用方推出方块外；分裂时本波已静默 discard）
	 */
	private boolean handleSixFaceDisperser(BlockState state, BlockPos pos, Vec3 wavePos, SubLevelBridge.Hit frame) {
		SixFaceDispersal.Result result = SixFaceDispersal.handle(state, movement, wavePos, pos, waveLevel);
		switch (result) {
			case VANISH -> {
				return true; // 入口关闭 / 波级不足分裂 → 撞墙湮灭
			}
			case BOUNCE -> {
				// 单开口：原路遣返（反弹），等级不变
				movement = movement.scale(-1);
				return false;
			}
			case TURN -> {
				// 双开口：从入口进、从另一开口出（拐弯），等级不变
				Direction worldOut = SixFaceDispersal.exitsOf(state, movement).get(0);
				movement = Vec3.atLowerCornerOf(worldOut.getNormal());
				return false;
			}
			case SPLIT -> {
				// 3-4 开口降一级、5-6 开口降二级：其余每个开口均摊发射子波
				int childLevel = waveLevel - SixFaceDispersal.decrementOf(state);
				Vec3 center = Vec3.atCenterOf(pos);
				for (Direction worldOut : SixFaceDispersal.exitsOf(state, movement)) {
					// 在差器方块中心沿出口方向外推 1 格出生，确保碰撞盒离开差器
					AbstractChargerWaveEntity child = createChildWave(
						center.add(Vec3.atLowerCornerOf(worldOut.getNormal())), worldOut, childLevel);
					if (child != null) {
						if (frame != null) {
							// 结构本地出生 → 转回世界坐标/方向（波必须出生在真实世界）；
							// spawnPos 同步修正，否则距离上限检查会把子波当"飞太远"立即消散
							SubLevelBridge b = SableBridges.get();
							if (b != null) {
								child.setPos(b.toWorld(frame, child.position()));
								child.spawnPos = b.toWorld(frame, child.spawnPos);
								child.movement = b.toWorldDir(frame, child.movement);
							}
						}
						// 子波继承母波的速度修正（波速调节器叠加值贯穿分裂传播链）
						child.addSpeedOffset(this.speedOffset);
						level().addFreshEntity(child);
					}
				}
				// 母波静默消失（能量已均摊分发到子波）
				this.discard();
				return false;
			}
		}
		return false;
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
	 * 移动速度（格/秒）：等级基础速度（低 2、高 4、伽马 6）+ 速度修正值
	 * （波速调节器叠加），夹在 {@link #MIN_SPEED} ~ {@link #MAX_SPEED} 之间。
	 * 子类可覆写基础速度（如雷鸣波更快）。
	 */
	protected double getSpeedBlocks() {
		double base = switch (waveLevel) {
			case 2 -> 4;
			case 3 -> 6;
			default -> 2;
		};
		return Math.max(MIN_SPEED, Math.min(MAX_SPEED, base + speedOffset));
	}

	/** 施加一次速度修正（叠加语义：在此值上增加 amount，可正可负）。 */
	protected void addSpeedOffset(double amount) {
		this.speedOffset += amount;
		// 同步到客户端（Jade 速度显示）
		this.entityData.set(SPEED_OFFSET, (float) this.speedOffset);
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
		double base = switch (level) {
			case 2 -> 4;
			case 3 -> 6;
			default -> 2;
		};
		return Math.max(MIN_SPEED, Math.min(MAX_SPEED, base + this.entityData.get(SPEED_OFFSET)));
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

	/** 命中伤害：低 4、高 6、伽马 10 —— 子类可覆写定制（如雷鸣波更高） */
	protected float getDamage() {
		return switch (waveLevel) {
			case 2 -> 6f;
			case 3 -> 10f;
			default -> 4f;
		};
	}

	/** 能量波颜色（RGB 0-1），随充能等级变化 —— 子类各自配色（如翡翠黄/绿/蓝、雷鸣紫系） */
	protected abstract Vec3 getWaveColor();

	/** 指定等级的波颜色（供调级器渐变提前取下一等级色用）；默认取当前等级色，子类按需覆写 */
	protected Vec3 getWaveColorForLevel(int level) {
		return getWaveColor();
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		waveLevel = tag.getInt("WaveLevel");
		movement = new Vec3(tag.getDouble("MoveX"), tag.getDouble("MoveY"), tag.getDouble("MoveZ"));
		boostRemaining = tag.getFloat("BoostRemaining");
		speedOffset = tag.getDouble("SpeedOffset");
		if (tag.contains("SpawnX"))
			spawnPos = new Vec3(tag.getDouble("SpawnX"), tag.getDouble("SpawnY"), tag.getDouble("SpawnZ"));
		// 服务端从 NBT 恢复等级后，同步核心加工逻辑的等级（决定可匹配配方上限）
		processor = new ChargerWaveProcessor(level(), waveLevel);
		// 同步数据写回（供客户端 Jade 显示）
		this.entityData.set(WAVE_LEVEL, waveLevel);
		this.entityData.set(SPEED_OFFSET, (float) speedOffset);
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("WaveLevel", waveLevel);
		tag.putDouble("MoveX", movement.x);
		tag.putDouble("MoveY", movement.y);
		tag.putDouble("MoveZ", movement.z);
		tag.putFloat("BoostRemaining", boostRemaining);
		tag.putDouble("SpeedOffset", speedOffset);
		if (spawnPos != null) {
			tag.putDouble("SpawnX", spawnPos.x);
			tag.putDouble("SpawnY", spawnPos.y);
			tag.putDouble("SpawnZ", spawnPos.z);
		}
	}
}
