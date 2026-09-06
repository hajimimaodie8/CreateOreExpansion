package com.hjmmd_8.createoreexpansion.compat.createaddition;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.energyfield.ChargePolarity;
import com.mrh0.createaddition.blocks.tesla_coil.TeslaCoilBlock;
import com.mrh0.createaddition.blocks.tesla_coil.TeslaCoilBlockEntity;
import com.mrh0.createaddition.config.CommonConfig;
import com.mrh0.createaddition.index.CABlocks;
import com.mrh0.createaddition.index.CASounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * 与 CC&amp;A（Create Crafts &amp; Additions）<b>特斯拉线圈</b>的联动：给路过的能量波赋电荷。
 *
 * <p>规则（用户 2026-09 定义）：线圈检测自身周围<b>半径 1 格立方体</b>内是否有一条
 * <b>不带电</b>的能量波 —— 有则给它赋一次<b>随机极性</b>的电荷；每赋一次：
 * <ul>
 *   <li>消耗线圈 {@value #ENERGY_COST_FE} FE 电量；</li>
 *   <li>播放一次<b>放电动画</b>（POWERED 通电模型保持约半秒，复用 CC&amp;A 充能时的
 *       通电表现；随后由 CC&amp;A 自身 tick 自动断电）+ 放电音效（跟随 CC&amp;A 全局声音开关）；</li>
 *   <li>进入 {@value #COOLDOWN_TICKS} tick（1 秒）冷却，期间不再赋电荷。</li>
 * </ul>
 * 波赋上电荷后即带电飞行（能量加速/偏转场开始作用），不再重复赋。</p>
 *
 * <p>CC&amp;A 为<b>可选</b>联动：未安装时本类方法第一行即返回（波照常飞行），
 * 不引用任何 CC&amp;A 常量初始化，无崩溃风险。</p>
 */
public final class TeslaCoilWaveCharger {

	/** 单次赋电荷消耗的线圈电量（FE）。 */
	public static final int ENERGY_COST_FE = 1000;

	/** 赋电荷后的线圈冷却时长（tick；20 tick = 1 秒）。 */
	public static final int COOLDOWN_TICKS = 20;

	/** 放电"通电"动画保持时长（tick；仿 CC&amp;A 皮带充能完成后的通电时长）。 */
	private static final int POWER_FLASH_TICKS = 10;

	/** 写在线圈 BE 持久数据上的键：下一次可赋电荷的 gameTime。 */
	private static final String TAG_CD_UNTIL = "co_wave_charge_cd_until";

	/** 反射缓存：CC&amp;A 受保护字段 {@code poweredTimer}（延迟初始化，CC&amp;A 未装时不触碰）。 */
	private static volatile java.lang.reflect.Field poweredTimerField;

	private TeslaCoilWaveCharger() {
	}

	private static boolean isCreateAdditionLoaded() {
		return net.neoforged.fml.ModList.get() != null
			&& net.neoforged.fml.ModList.get().isLoaded("createaddition");
	}

	/**
	 * 尝试让贴近该波的某台 CC&amp;A 特斯拉线圈为其赋一次随机电荷（服务端波 tick 调用）。
	 *
	 * <p>判定按"线圈周围半径 1 格立方体"的<b>体积相交</b>而非波中心所在格：波的碰撞盒
	 * 外扩 1 格后与线圈方块盒相交即算擦着边/进入范围 —— 贴边飞过的波也会被电到。</p>
	 *
	 * @return true = 本 tick 已被线圈赋电荷（可提前停止后续处理）
	 */
	public static boolean chargeNearbyCoil(AbstractChargerWaveEntity wave) {
		if (wave == null || wave.level().isClientSide)
			return false;
		if (wave.getChargePolarity() != null)
			return false; // 已带电：不再赋
		if (!isCreateAdditionLoaded())
			return false;
		try {
			// 1) 物理结构（Sable sub-level）：波在主世界、结构方块在 sub-level —— 枚举本世界
			//    全部结构，把波世界坐标反算成本地坐标后在本地 ±2 格内找线圈赋电荷
			//   （无需波"压到"结构实体方块，贴着结构场/空气飞过也能被电）。
			com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge bridge =
				com.hjmmd_8.createoreexpansion.content.wave.bridge.SableBridges.get();
			if (bridge != null && bridge.isActive()
				&& wave.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
				for (Object sub : bridge.subLevels(serverLevel)) {
					com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge.Hit hit =
						new com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge.Hit(sub);
					if (structureChargeNearby(wave, bridge, hit))
						return true;
				}
			}
		} catch (Throwable ignored) {
			// CC&A 或 Sable 异常：静默跳过
		}
		try {
			Level level = wave.level();
			AABB zone = wave.getBoundingBox().inflate(1.0); // 波周围 1 格 = 线圈"半径 1 立方体"探测区
			BlockPos p = wave.blockPosition();
			// 波与线圈相距最多 2 格时才有相交可能，扫描 ±2 覆盖"贴边经过"
			for (int dx = -2; dx <= 2; dx++) {
				for (int dy = -2; dy <= 2; dy++) {
					for (int dz = -2; dz <= 2; dz++) {
						BlockPos bp = p.offset(dx, dy, dz);
						BlockEntity be = level.getBlockEntity(bp);
						if (be instanceof TeslaCoilBlockEntity coil
							&& new AABB(bp).intersects(zone)
							&& tryZap(level, coil, wave))
							return true;
					}
				}
			}
		} catch (Throwable ignored) {
			// CC&amp;A 异常/缺类：静默跳过，波照常飞行
		}
		return false;
	}

	/**
	 * 结构本地找线圈并赋电荷：波命中 sub-level 时，线圈方块实体在结构本地子世界里，
	 * 主世界 getBlockEntity 查不到 —— 经 bridge 在本地 ±2 格内找 TeslaCoilBlockEntity。
	 * 扣电走反射 internalConsumeEnergy（结构方块状态不可安全改写 → 不做 POWERED 块态动画，
	 * 改在世界坐标播放电音效 + 电火花粒子；冷却写线圈 BE 持久数据）。
	 */
	private static boolean structureChargeNearby(AbstractChargerWaveEntity wave,
		com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge bridge,
		com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge.Hit hit) {
		Vec3 localPos = bridge.toLocal(hit, wave.position());
		BlockPos cell = BlockPos.containing(localPos);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				for (int dz = -2; dz <= 2; dz++) {
					BlockPos bp = cell.offset(dx, dy, dz);
					net.minecraft.world.level.block.entity.BlockEntity be = bridge.getBlockEntity(hit, bp);
					if (be instanceof TeslaCoilBlockEntity coil && tryZapStructure(wave, bridge, hit, coil))
						return true;
				}
			}
		}
		return false;
	}

	/** 结构线圈放电：冷却 → 反射扣电 → 随机电荷 → 冷却标记 → 世界放电音效/电火花。 */
	private static boolean tryZapStructure(AbstractChargerWaveEntity wave,
		com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge bridge,
		com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge.Hit hit,
		TeslaCoilBlockEntity coil) {
		Level worldLevel = wave.level();
		long now = worldLevel.getGameTime();
		if (now < coil.getPersistentData().getLong(TAG_CD_UNTIL))
			return false;
		com.mrh0.createaddition.energy.InternalEnergyStorage internal = internalEnergyOf(coil);
		if (internal == null || internal.getEnergyStored() < ENERGY_COST_FE)
			return false;
		if (internal.internalConsumeEnergy(ENERGY_COST_FE) < ENERGY_COST_FE)
			return false;

		wave.setCharge(worldLevel.random.nextBoolean() ? ChargePolarity.POSITIVE : ChargePolarity.NEGATIVE);
		coil.getPersistentData().putLong(TAG_CD_UNTIL, now + COOLDOWN_TICKS);

		// 放电表现：结构上不做块态 POWERED（Sable 本地世界状态不可安全改写），在世界坐标播音效+火花
		Vec3 coilWorld = bridge.toWorld(hit, Vec3.atCenterOf(coil.getBlockPos()));
		if (CommonConfig.AUDIO_ENABLED.get())
			worldLevel.playSound(null, coilWorld.x, coilWorld.y, coilWorld.z,
				CASounds.LOUD_ZAP.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
		if (worldLevel instanceof net.minecraft.server.level.ServerLevel server) {
			net.minecraft.core.particles.ParticleOptions dust =
				new net.minecraft.core.particles.DustParticleOptions(
					new org.joml.Vector3f(1.0F, 0.9F, 0.25F), 1.2F);
			server.sendParticles(dust, coilWorld.x, coilWorld.y + 0.5, coilWorld.z,
				10, 0.3, 0.3, 0.3, 0.01);
		}
		return true;
	}

	/** 反射取 CC&A 线圈内部能量存储（结构 BE 无法用主世界 capability 查询）。 */
	private static volatile java.lang.reflect.Field localEnergyField;

	private static com.mrh0.createaddition.energy.InternalEnergyStorage internalEnergyOf(TeslaCoilBlockEntity coil) {
		try {
			java.lang.reflect.Field f = localEnergyField;
			if (f == null) {
				f = com.mrh0.createaddition.energy.AbstractElectricBlockEntity.class.getDeclaredField("localEnergy");
				f.setAccessible(true);
				localEnergyField = f;
			}
			Object v = f.get(coil);
			return v instanceof com.mrh0.createaddition.energy.InternalEnergyStorage s ? s : null;
		} catch (Throwable ignored) {
			return null;
		}
	}

	/** 单台线圈尝试放电赋电荷：冷却 + 电量足够 → 扣电 → 随机极性 → 通电闪光 + 音效 + 起冷却。 */
	private static boolean tryZap(Level level, TeslaCoilBlockEntity coil, AbstractChargerWaveEntity wave) {
		BlockPos pos = coil.getBlockPos();
		long now = level.getGameTime();
		if (now < coil.getPersistentData().getLong(TAG_CD_UNTIL))
			return false; // 冷却中
		IEnergyStorage es = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, (Direction) null);
		if (es == null || es.getEnergyStored() < ENERGY_COST_FE)
			return false; // 电量不足

		// 扣电：特斯拉线圈是"纯输入"设备（maxExtract = 0），capability.extractEnergy 恒返回 0，
		// 必须走 CC&A 内部的 internalConsumeEnergy（CC&A 自身的伤害放电也用它）。
		if (es instanceof com.mrh0.createaddition.energy.InternalEnergyStorage internal) {
			if (internal.internalConsumeEnergy(ENERGY_COST_FE) < ENERGY_COST_FE)
				return false;
		} else if (es.extractEnergy(ENERGY_COST_FE, false) < ENERGY_COST_FE) {
			return false; // 兜底（理论上不会走这里）
		}

		wave.setCharge(level.random.nextBoolean() ? ChargePolarity.POSITIVE : ChargePolarity.NEGATIVE);
		coil.getPersistentData().putLong(TAG_CD_UNTIL, now + COOLDOWN_TICKS);

		// 放电动画：切 POWERED 模型并保持约半秒（CC&A 自身 tick 在 poweredTimer 归零后自动断电）
		TeslaCoilBlock block = (TeslaCoilBlock) CABlocks.TESLA_COIL.get();
		block.setPowered(level, pos, true);
		setPoweredTimer(coil, POWER_FLASH_TICKS);

		// 放电音效（跟随 CC&A 全局声音开关）
		if (CommonConfig.AUDIO_ENABLED.get())
			level.playSound(null, pos, CASounds.LOUD_ZAP.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
		return true;
	}

	/** 反射把 CC&amp;A 受保护字段 {@code poweredTimer} 置位：其 tick 维持通电并随后自动断电。 */
	private static void setPoweredTimer(TeslaCoilBlockEntity coil, int ticks) {
		try {
			java.lang.reflect.Field f = poweredTimerField;
			if (f == null) {
				f = TeslaCoilBlockEntity.class.getDeclaredField("poweredTimer");
				f.setAccessible(true);
				poweredTimerField = f;
			}
			f.setInt(coil, ticks);
		} catch (Throwable ignored) {
			// 反射失败：POWERED 已切 true，至少能闪一下；赋电荷与扣电不受影响
		}
	}
}
