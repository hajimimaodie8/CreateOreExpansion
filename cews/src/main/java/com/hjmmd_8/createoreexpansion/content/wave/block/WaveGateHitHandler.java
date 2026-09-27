package com.hjmmd_8.createoreexpansion.content.wave.block;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveMachineHandler;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.EnergyWaveRegulation;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.WaveSpeedRegulation;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * <b>能量波闸（调级器 / 波速调节器）的波命中处置</b>（W6-a）。
 *
 * <p>本类把原先住在第一层 {@code content/charger/wave/WaveMachineActions}（主世界）与
 * {@code WaveContraptionCollisions} / {@code WaveSubLevelCollisions}（结构场景）里的
 * "机器侧"代码<b>逐行搬到第二层</b>（W6-c 里这些机器本来就住第二层），并实现第一层的
 * {@link WaveMachineHandler} 契约。搬动只改了"谁持有 wave"（原来是字段，现在是参数）
 * 与结果返回方式（原来直接 burst/discard 或返回 true/false，现在返回
 * {@link Outcome}），<b>判定与副作用逐项不变</b>。</p>
 */
public final class WaveGateHitHandler implements WaveMachineHandler {

	/** 无状态处理器，登记一次即可。 */
	public static final WaveGateHitHandler INSTANCE = new WaveGateHitHandler();

	/**
	 * 调级器增强延迟（格）：穿过顺基准调级器后还需飞行 0.5 格
	 * 才升级（0.5 格 ÷ 波速 v = 1/(2v) 秒）。0 = 无待升级。
	 */
	private static final float BOOST_DISTANCE = 0.5f;

	private WaveGateHitHandler() {
	}

	// ================= 主世界 =================

	@Override
	public Outcome onWorldHit(AbstractChargerWaveEntity wave, BlockPos pos, BlockState state) {
		// 能量波闸（调级器/波速调节器，翡翠/蓝宝石）：面板通道 + 应力调制。
		// 用基类 Block 统一命中，按 modulatesWaveLevel() 区分走等级调制还是速度调制。
		// BE 不在位时不认领：原实现在那种情况下会落回引擎的撞墙分支，这里必须保持一致。
		if (!(state.getBlock() instanceof AbstractWaveGateBlock waveGate))
			return Outcome.NOT_MINE;
		if (!(wave.level().getBlockEntity(pos) instanceof AbstractWaveGateBlockEntity gateBe))
			return Outcome.NOT_MINE;
		boolean isRegulator = waveGate.modulatesWaveLevel();
		boolean vanish;
		if (isRegulator) {
			vanish = handleRegulator(wave, gateBe, pos, wave.getBoundingBox().getCenter());
		} else {
			vanish = handleWaveSpeedRegulator(wave, gateBe, pos, wave.getBoundingBox().getCenter());
		}
		// 原调用点：vanish → burst + discard + return；否则推出方块外 + return。
		return vanish ? Outcome.BURST : Outcome.PUSH;
	}

	/**
	 * 处理一次调级器判定。
	 *
	 * @param regulator 调级器方块实体
	 * @param pos       调级器方块位置（与 wavePos 同坐标系：主世界=世界坐标，结构=本地坐标）
	 * @param wavePos   波前中心（必须与 pos 同坐标系——结构场景传本地坐标，否则 4×4 入口判定失真）
	 * @return true = 波应在原地湮灭（调用方负责 burst + discard）；false = 波继续（已按结果
	 *         升级/降级/反转 movement，调用方负责推出方块外）
	 */
	private boolean handleRegulator(AbstractChargerWaveEntity wave, AbstractWaveGateBlockEntity regulator,
		BlockPos pos, Vec3 wavePos) {
		EnergyWaveRegulation.Result result = EnergyWaveRegulation.get()
			.resolve(regulator, wavePos, wave.getMovement(), wave.getWaveLevel());
		switch (result) {
			case VANISH -> {
				// 齿轮端/入口关闭：如撞墙消失，无爆炸
				return true;
			}
			case VANISH_OVERLOAD_BOOM -> {
				// 顺基准且已达机型承载上限（翡翠 3 = γ / 蓝宝石·星辉石 5 = ω）→ 过载爆炸后湮灭。
				// 爆炸规模按波自身等级放大（用户 2026-09-14 定稿）：翡翠机 3 级（105 颗），
				// 蓝宝石/星辉石机 5 级（155 颗）——与"波波碰撞取较低等级"同一套取值口径。
				ChargerWaveFx.triggerBoom(wave.level(), wave, wave.position(), wave.getWaveType().trailStyle(),
					wave.getRenderColor(), null, boomLevel(wave));
				return true;
			}
			case VANISH_FLOOR_BOOM -> {
				// 1 级（α）波逆基准 / 单开口遣返：降级无路可降 → 爆炸后湮灭（等级恒为 1，即 α 级规模）
				ChargerWaveFx.triggerBoom(wave.level(), wave, wave.position(), wave.getWaveType().trailStyle(),
					wave.getRenderColor(), null, boomLevel(wave));
				return true;
			}
			case PASS_UNCHANGED -> {
				// 无应力双开口：等级不变，正常穿过
			}
			case BOUNCE -> {
				// 无应力单开口：等级不变，原路遣返（折 180° 原路返回）
				wave.setMovement(wave.getMovement().scale(-1));
			}
			case PASS_BOOST_LATER -> {
				// 顺基准双开口：穿过，延迟升级（飞行 0.5 格 = 1/(2v) 秒后等级提升）。
				// 提升级数按机型参数：翡翠/蓝宝石恒 1；星辉石按本机转速 1 或 2（波侧封顶 MAX_LEVEL=5）
				wave.setBoostRemaining(BOOST_DISTANCE);
				wave.setBoostStep(regulator.getBoostStepForSpeed());
			}
			case PASS_DOWNGRADE -> {
				// 逆基准双开口：穿过，立即降级
				wave.setWaveLevel(wave.getWaveLevel() - 1);
			}
			case BOUNCE_DOWNGRADE -> {
				// 有应力单开口：降级并原路遣返（1 级波已在判定层走 VANISH_FLOOR_BOOM 爆炸，这里仅 ≥2 级）
				wave.setWaveLevel(wave.getWaveLevel() - 1);
				wave.setMovement(wave.getMovement().scale(-1));
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
	 * @param wavePos        波前中心（须与 pos 同坐标系）
	 * @return true = 波应撞墙湮灭（调用方负责 burst + discard）；false = 波继续
	 */
	private boolean handleWaveSpeedRegulator(AbstractChargerWaveEntity wave,
		AbstractWaveGateBlockEntity speedRegulator, BlockPos pos, Vec3 wavePos) {
		WaveSpeedRegulation.Result result = WaveSpeedRegulation.get()
			.resolve(speedRegulator, wavePos, wave.getMovement(), wave.getWaveLevel());
		switch (result) {
			case VANISH -> {
				return true; // 齿轮端/入口关闭：撞墙湮灭
			}
			case BOUNCE -> {
				// 单开口：纯折返，速度不变
				wave.setMovement(wave.getMovement().scale(-1));
				return false;
			}
			case PASS_UNCHANGED -> {
				// 无应力双开口：速度不变穿过
				return false;
			}
			case PASS_SPEED_UP -> {
				// 顺基准：加速（按机型分档叠加：翡翠 4 档 / 蓝宝石 6 档）
				float amount = speedAmountFor(speedRegulator);
				wave.addSpeedOffset(amount);
				return false;
			}
			case PASS_SPEED_DOWN -> {
				// 逆基准：减速（按机型分档叠加）
				float amount = speedAmountFor(speedRegulator);
				wave.addSpeedOffset(-amount);
				return false;
			}
		}
		return false;
	}

	/** 按波速调节器机型分档计算本次调速量（格/秒）：档位参数取自 BE（翡翠 4 档 0.5 步进 / 蓝宝石 6 档）。 */
	private static float speedAmountFor(AbstractWaveGateBlockEntity speedRegulator) {
		return WaveSpeedRegulation.offsetForSpeed(speedRegulator.getSpeed(),
			speedRegulator.getSpeedTierBase(), speedRegulator.getSpeedTierMax(),
			speedRegulator.getSpeedTierCount(), speedRegulator.getSpeedTierStep());
	}

	/**
	 * <b>爆炸等级取值（唯一实现）</b>：一律用<b>波自身等级</b>，并夹到波速/伤害表的合法档位
	 * （{@link WaveLevels#LOW}~{@link WaveLevels#MAX_LEVEL}）——爆炸粒子量按等级线性放大
	 * （见 {@code ChargerWaveFx#triggerBoom} 的 {@code 30 + boomLevel × 25}），越界值不得直接传下去。
	 */
	private static int boomLevel(AbstractChargerWaveEntity wave) {
		return Mth.clamp(wave.getWaveLevel(), WaveLevels.LOW, WaveLevels.MAX_LEVEL);
	}

	// ================= Create contraption（结构本地坐标系） =================

	@Override
	public Outcome onContraptionHit(AbstractChargerWaveEntity wave, BlockPos pos, BlockState state,
		AbstractContraptionEntity entity, Vec3 localCenter) {
		// 能量波闸（调级器/波速，翡翠/蓝宝石）：contraption 上无 BE/转速 → 仅通道，不调制。
		if (!(state.getBlock() instanceof AbstractWaveGateBlock waveGate))
			return Outcome.NOT_MINE;
		if (waveGate.modulatesWaveLevel()) {
			EnergyWaveRegulation.Result r = EnergyWaveRegulation.get()
				.resolve(state, pos, localCenter, wave.getMovement(), wave.getWaveLevel());
			return handleGateResult(wave, r, entity, pos) ? Outcome.CONSUMED : Outcome.SKIP;
		} else {
			WaveSpeedRegulation.Result r = WaveSpeedRegulation.get()
				.resolve(state, pos, localCenter, wave.getMovement(), wave.getWaveLevel());
			return handleSpeedGateResult(wave, r, entity, pos) ? Outcome.CONSUMED : Outcome.SKIP;
		}
	}

	/** 处理调级器纯 state 判定结果（contraption 场景）；返回 true = 本 tick 已处理。 */
	private static boolean handleGateResult(AbstractChargerWaveEntity wave, EnergyWaveRegulation.Result r,
		AbstractContraptionEntity entity, BlockPos pos) {
		switch (r) {
			case VANISH -> {
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.getWaveType().trailStyle(), wave.getRenderColor());
				wave.discard();
				return true;
			}
			case BOUNCE -> {
				wave.setMovement(wave.getMovement().scale(-1));
				wave.setPos(entity.toGlobalVector(Vec3.atCenterOf(pos)
					.add(wave.getMovement().scale(1.0d)), 0));
				return true;
			}
			case PASS_UNCHANGED -> {
				wave.setPos(entity.toGlobalVector(Vec3.atCenterOf(pos)
					.add(wave.getMovement().scale(1.0d)), 0));
				return true;
			}
			default -> {
				// 调制结果（VANISH_OVERLOAD_BOOM/VANISH_FLOOR_BOOM/PASS_BOOST_LATER/PASS_DOWNGRADE/BOUNCE_DOWNGRADE）
				// 依赖转速，纯 state 判定（speed=0）不会产生；防御性按撞墙
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.getWaveType().trailStyle(), wave.getRenderColor());
				wave.discard();
				return true;
			}
		}
	}

	/** 处理波速调节器纯 state 判定结果（contraption 场景）；返回 true = 本 tick 已处理。 */
	private static boolean handleSpeedGateResult(AbstractChargerWaveEntity wave, WaveSpeedRegulation.Result r,
		AbstractContraptionEntity entity, BlockPos pos) {
		switch (r) {
			case VANISH -> {
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.getWaveType().trailStyle(), wave.getRenderColor());
				wave.discard();
				return true;
			}
			case BOUNCE -> {
				wave.setMovement(wave.getMovement().scale(-1));
				wave.setPos(entity.toGlobalVector(Vec3.atCenterOf(pos)
					.add(wave.getMovement().scale(1.0d)), 0));
				return true;
			}
			case PASS_UNCHANGED -> {
				wave.setPos(entity.toGlobalVector(Vec3.atCenterOf(pos)
					.add(wave.getMovement().scale(1.0d)), 0));
				return true;
			}
			default -> {
				// PASS_SPEED_UP/DOWN 依赖转速，纯 state 判定不会产生；防御性按撞墙
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.getWaveType().trailStyle(), wave.getRenderColor());
				wave.discard();
				return true;
			}
		}
	}

	// ================= Sable 物理结构（结构本地坐标系） =================

	@Override
	public Outcome onSubLevelHit(AbstractChargerWaveEntity wave, BlockPos pos, BlockState state,
		SubLevelBridge bridge, SubLevelBridge.Hit hit) {
		// 能量波闸（调级器/波速，翡翠/蓝宝石）：面板通道 + 应力调制
		if (!(state.getBlock() instanceof AbstractWaveGateBlock waveGate))
			return Outcome.NOT_MINE;
		if (!(bridge.getBlockEntity(hit, pos) instanceof AbstractWaveGateBlockEntity gateBe))
			return Outcome.NOT_MINE;
		Vec3 localWavePos = bridge.toLocal(hit, wave.getBoundingBox()
			.getCenter());
		boolean isRegulator = waveGate.modulatesWaveLevel();
		boolean vanish = isRegulator
			? handleRegulator(wave, gateBe, pos, localWavePos)
			: handleWaveSpeedRegulator(wave, gateBe, pos, localWavePos);
		if (vanish) {
			ChargerWaveFx.burst(wave.level(), wave.position(), wave.getWaveType().trailStyle(), wave.getRenderColor());
			wave.discard();
			return Outcome.CONSUMED;
		}
		wave.setPos(bridge.toWorld(hit, Vec3.atCenterOf(pos)
			.add(wave.getMovement().scale(1.0d))));
		return Outcome.CONSUMED;
	}
}
