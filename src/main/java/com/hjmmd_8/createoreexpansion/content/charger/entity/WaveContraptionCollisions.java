package com.hjmmd_8.createoreexpansion.content.charger.entity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hjmmd_8.createoreexpansion.content.wave.block.AbstractWaveGateBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.OctaEnergyWaveDifferencerBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.SixFaceDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.EnergyWaveDispersal;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.EnergyWaveRegulation;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.OctaEnergyWaveDispersal;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.SixFaceDispersal;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.WaveSpeedRegulation;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 能量波在 Create 动态结构（contraption：动力轴承/矿车装配站装配）上的碰撞协调。
 *
 * <p><b>机制</b>：contraption 方块留在主世界（数据在 {@link Contraption} 内），
 * 波中心经 {@code entity.toLocalVector} 换算到结构本地坐标，本地查方块状态后
 * 复用波闸/差波器的<b>纯 blockstate 判定</b>（contraption 上无 BE 实例/转速 →
 * 波闸仅通道不调制）。判定结果方向经 {@code entity.applyRotation} 转回世界。</p>
 *
 * <p><b>坐标系</b>：本类内 {@code wave.getMovement()} 临时切换为 contraption 本地方向参与判定，
 * 判定结束（finally）经 {@code applyRotation} 转回世界方向；分裂子波在 contraption 本地
 * 出生（位置/方向），经 {@code toGlobalVector/applyRotation} 转回世界坐标（随 contraption 旋转）。</p>
 */
public class WaveContraptionCollisions {

	private final AbstractChargerWaveEntity wave;

	public WaveContraptionCollisions(AbstractChargerWaveEntity wave) {
		this.wave = wave;
	}

	/**
	 * 遍历本世界已加载的 contraption，波盒命中的结构方块按机器类型判定。
	 *
	 * @return true = 本 tick 已在 contraption 上处理完毕
	 */
	public boolean tryHandle() {
		Map<Integer, WeakReference<AbstractContraptionEntity>> contraptions =
			ContraptionHandler.loadedContraptions.get(wave.level());
		if (contraptions == null || contraptions.isEmpty())
			return false;
		for (WeakReference<AbstractContraptionEntity> ref : contraptions.values()) {
			AbstractContraptionEntity entity = ref.get();
			if (entity == null || !entity.isAlive())
				continue;
			Contraption contraption = entity.getContraption();
			if (contraption == null)
				continue;

			// 波中心/方向 → contraption 本地坐标系
			Vec3 localCenter = entity.toLocalVector(wave.getBoundingBox()
				.getCenter(), 0);
			Vec3 localMove = entity.reverseRotation(wave.getMovement(), 0);
			double h = 0.1; // 波盒半宽
			BlockPos min = BlockPos.containing(localCenter.x - h, localCenter.y - h, localCenter.z - h);
			BlockPos max = BlockPos.containing(localCenter.x + h, localCenter.y + h, localCenter.z + h);
			for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
				BlockState state = contraption.getContraptionWorld()
					.getBlockState(pos);
				if (state.isAir())
					continue;
				// 判定前：movement 临时切换为 contraption 本地方向
				wave.setMovement(localMove);
				try {
					// Energy wave gate (regulator/speed, jade/sapphire): on contraption no BE/speed -> channel only,
					// no modulation. Match via base Block; modulatesWaveLevel() picks level vs speed channel.
					if (state.getBlock() instanceof AbstractWaveGateBlock waveGate) {
						if (waveGate.modulatesWaveLevel()) {
						EnergyWaveRegulation.Result r = EnergyWaveRegulation.get()
							.resolve(state, pos, localCenter, wave.getMovement(), wave.getWaveLevel());
						if (handleGateResult(r, entity, pos))
							return true;
						} else {
						WaveSpeedRegulation.Result r = WaveSpeedRegulation.get()
							.resolve(state, pos, localCenter, wave.getMovement(), wave.getWaveLevel());
						if (handleSpeedGateResult(r, entity, pos))
							return true;
						}
						continue;
					}
					// 四面能量波差器：反弹/拐弯/分裂。
					// 出口为模型侧面（NORTH/EAST/SOUTH/WEST），须经 worldDirOf(FACING)
					// 转成本地方向再发射（同主世界 WaveMachineActions 的映射）。
					if (state.getBlock() instanceof EnergyWaveDisperserBlock) {
						EnergyWaveDispersal.Result r = EnergyWaveDispersal.handle(state, wave.getMovement(), localCenter,
							pos, wave.getWaveLevel());
						if (r == EnergyWaveDispersal.Result.SPLIT) {
							Direction facing = state.getValue(EnergyWaveDisperserBlock.FACING);
							List<Direction> localExits = new ArrayList<>(3);
							for (Direction modelSide : EnergyWaveDispersal.exitsOf(state, wave.getMovement())) {
								Direction worldOut = EnergyWaveDisperserBlock.worldDirOf(facing, modelSide);
								if (worldOut != null)
									localExits.add(worldOut);
							}
							if (splitContraptionChildren(localExits, 1, entity, pos))
								return true;
							continue;
						}
						if (handleDisperserResult(r, entity, pos, state))
							return true;
						continue;
					}
					// 六面能量波差器：反弹/拐弯/分裂（降级量 3-4 口 1 级、5-6 口 2 级）
					if (state.getBlock() instanceof SixFaceDisperserBlock) {
						SixFaceDispersal.Result r = SixFaceDispersal.handle(state, wave.getMovement(), localCenter, pos,
							wave.getWaveLevel());
						if (r == SixFaceDispersal.Result.SPLIT) {
							if (splitContraptionChildren(SixFaceDispersal.exitsOf(state, wave.getMovement()),
								SixFaceDispersal.decrementOf(state), entity, pos))
								return true;
							continue;
						}
						if (handleSixFaceDisperserResult(r, entity, pos, state))
							return true;
						continue;
					}
					// 八面能量波差器：8 口（4 正交 + 4 斜）。wave.getMovement() 此刻= contraption 本地方向，
					// 再按 blockstate AXIS 换算到机器本地（站姿语义）判定。
					if (state.getBlock() instanceof OctaEnergyWaveDifferencerBlock) {
						Direction.Axis axis = state.getValue(OctaEnergyWaveDifferencerBlock.AXIS);
						Vec3 octaLocalMove = OctaEnergyWaveDifferencerBlock.toLocalVec(axis, wave.getMovement());
						Vec3 relCenter = OctaEnergyWaveDifferencerBlock.toLocalVec(axis,
							localCenter.subtract(Vec3.atCenterOf(pos)));
						OctaEnergyWaveDispersal.Result r = OctaEnergyWaveDispersal.handle(state, octaLocalMove, relCenter,
							wave.getWaveLevel());
						if (r == OctaEnergyWaveDispersal.Result.SPLIT) {
							int childLevel = wave.getWaveLevel() - OctaEnergyWaveDispersal.decrementOf(state);
							if (childLevel <= 0) {
								ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
								wave.discard();
								return true;
							}
							Vec3 center = Vec3.atCenterOf(pos);
							List<Vec3> octaOuts = OctaEnergyWaveDispersal.otherOpenDirs(state, octaLocalMove, relCenter);
							for (int outIndex = 0; outIndex < octaOuts.size(); outIndex++) {
								// 出口方向：机器本地 → contraption 本地（斜口 45° 向量经 applyRotation 转世界）
								Vec3 contraptionDir = OctaEnergyWaveDifferencerBlock.toWorldVec(axis,
									octaOuts.get(outIndex));
								AbstractChargerWaveEntity child = wave.createChildWave(center.add(contraptionDir),
									contraptionDir, childLevel, outIndex, octaOuts.size());
								if (child != null) {
									child.setPos(entity.toGlobalVector(child.position(), 0));
									child.setSpawnPos(entity.toGlobalVector(child.getSpawnPos(), 0));
									child.setMovement(entity.applyRotation(child.getMovement(), 0));
									child.addSpeedOffset(wave.getSpeedOffset());
									wave.level()
										.addFreshEntity(child);
								}
							}
							wave.discard();
							return true;
						}
						if (handleOctaDisperserResult(r, entity, pos, axis, octaLocalMove, relCenter, state))
							return true;
						continue;
					}
					// contraption 上的其它方块：视为撞墙
					ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
					wave.discard();
					return true;
				} finally {
					// 判定结果（反弹/穿出方向）从本地方向转回世界方向
					wave.setMovement(entity.applyRotation(wave.getMovement(), 0));
				}
			}
		}
		return false;
	}

	/** 处理调级器纯 state 判定结果（contraption 场景）；返回 true = 本 tick 已处理。 */
	private boolean handleGateResult(EnergyWaveRegulation.Result r, AbstractContraptionEntity entity, BlockPos pos) {
		switch (r) {
			case VANISH -> {
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
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
				// 调制结果（VANISH_GAMMA_BOOM/VANISH_LOW_BOOM/PASS_BOOST_LATER/PASS_DOWNGRADE/BOUNCE_DOWNGRADE）
				// 依赖转速，纯 state 判定（speed=0）不会产生；防御性按撞墙
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
				wave.discard();
				return true;
			}
		}
	}

	/** 处理波速调节器纯 state 判定结果（contraption 场景）；返回 true = 本 tick 已处理。 */
	private boolean handleSpeedGateResult(WaveSpeedRegulation.Result r, AbstractContraptionEntity entity, BlockPos pos) {
		switch (r) {
			case VANISH -> {
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
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
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
				wave.discard();
				return true;
			}
		}
	}

	/** 处理八面差波器结果（contraption 场景）；返回 true = 本 tick 已处理。 */
	private boolean handleOctaDisperserResult(OctaEnergyWaveDispersal.Result r, AbstractContraptionEntity entity,
		BlockPos pos, Direction.Axis axis, Vec3 localMove, Vec3 relCenter, BlockState state) {
		switch (r) {
			case VANISH -> {
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
				wave.discard();
				return true;
			}
			case BOUNCE -> {
				wave.setMovement(wave.getMovement().scale(-1));
				wave.setPos(entity.toGlobalVector(Vec3.atCenterOf(pos)
					.add(wave.getMovement().scale(1.0d)), 0));
				return true;
			}
			case TURN -> {
				// 双开口：从另一开口出（方向=本地转 contraption 本地，applyRotation 在调用方 finally）
				List<Vec3> outs = OctaEnergyWaveDispersal.otherOpenDirs(state, localMove, relCenter);
				if (!outs.isEmpty()) {
					Vec3 contraptionOut = OctaEnergyWaveDifferencerBlock.toWorldVec(axis, outs.get(0));
					wave.setMovement(contraptionOut);
				}
				wave.setPos(entity.toGlobalVector(Vec3.atCenterOf(pos)
					.add(wave.getMovement().scale(1.0d)), 0));
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	/**
	 * 处理四面差波器结果（contraption 场景）；返回 true = 本 tick 已处理。
	 * TURN: {@link EnergyWaveDispersal#handle} 是纯函数（从不写 movement），
	 * 出口方向必须在此由 state + exits 计算（模型面经 worldDirOf(FACING) 映射）。
	 */
	private boolean handleDisperserResult(EnergyWaveDispersal.Result r, AbstractContraptionEntity entity,
		BlockPos pos, BlockState state) {
		switch (r) {
			case VANISH -> {
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
				wave.discard();
				return true;
			}
			case BOUNCE -> {
				wave.setMovement(wave.getMovement().scale(-1));
				wave.setPos(entity.toGlobalVector(Vec3.atCenterOf(pos)
					.add(wave.getMovement().scale(1.0d)), 0));
				return true;
			}
			case TURN -> {
				// 拐弯：朝另一个开口的模型侧面方向发射（纯函数不写 movement，此处显式计算）
				Direction facing = state.getValue(EnergyWaveDisperserBlock.FACING);
				Direction modelSide = EnergyWaveDispersal.exitsOf(state, wave.getMovement())
					.get(0);
				Direction worldOut = EnergyWaveDisperserBlock.worldDirOf(facing, modelSide);
				if (worldOut != null)
					wave.setMovement(Vec3.atLowerCornerOf(worldOut.getNormal()));
				wave.setPos(entity.toGlobalVector(Vec3.atCenterOf(pos)
					.add(wave.getMovement().scale(1.0d)), 0));
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	/** 处理六面差波器结果（contraption 场景）；返回 true = 本 tick 已处理。 */
	private boolean handleSixFaceDisperserResult(SixFaceDispersal.Result r, AbstractContraptionEntity entity,
		BlockPos pos, BlockState state) {
		switch (r) {
			case VANISH -> {
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
				wave.discard();
				return true;
			}
			case BOUNCE -> {
				wave.setMovement(wave.getMovement().scale(-1));
				wave.setPos(entity.toGlobalVector(Vec3.atCenterOf(pos)
					.add(wave.getMovement().scale(1.0d)), 0));
				return true;
			}
			case TURN -> {
				// 拐弯：朝另一个开口方向发射（六面无 FACING，出口即世界方向）
				Direction worldOut = SixFaceDispersal.exitsOf(state, wave.getMovement())
					.get(0);
				wave.setMovement(Vec3.atLowerCornerOf(worldOut.getNormal()));
				wave.setPos(entity.toGlobalVector(Vec3.atCenterOf(pos)
					.add(wave.getMovement().scale(1.0d)), 0));
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	/**
	 * contraption 上差波器分裂：其余每个开口各发一个降级子波。
	 * 子波在 contraption 本地出生（位置/方向），经 {@code entity.toGlobalVector/applyRotation}
	 * 转回世界坐标（随 contraption 旋转）。
	 *
	 * @param exits     非入口的其余开口（世界方向——四面差波器已由调用方经 worldDirOf(FACING)
	 *                  映射；六面即世界方向）
	 * @param decrement 分裂降级量（四面=1；六面 3-4 口=1、5-6 口=2）
	 * @return true = 本 tick 已处理（分裂成功或波级不足撞墙）
	 */
	private boolean splitContraptionChildren(List<Direction> exits, int decrement,
		AbstractContraptionEntity entity, BlockPos pos) {
		int childLevel = wave.getWaveLevel() - decrement;
		if (childLevel <= 0) {
			// 波级不足分裂：撞墙消散
			ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
			wave.discard();
			return true;
		}
		Vec3 center = Vec3.atCenterOf(pos);
		for (int outIndex = 0; outIndex < exits.size(); outIndex++) {
			Vec3 localDir = Vec3.atLowerCornerOf(exits.get(outIndex)
				.getNormal());
			AbstractChargerWaveEntity child = wave.createChildWave(center.add(localDir), localDir, childLevel,
				outIndex, exits.size());
			if (child != null) {
				// 本地 → 世界（位置/方向，随 contraption 旋转）
				child.setPos(entity.toGlobalVector(child.position(), 0));
				child.setSpawnPos(entity.toGlobalVector(child.getSpawnPos(), 0));
				child.setMovement(entity.applyRotation(child.getMovement(), 0));
				// 子波继承母波的速度修正（波速调节器叠加值贯穿分裂传播链）
				child.addSpeedOffset(wave.getSpeedOffset());
				wave.level()
					.addFreshEntity(child);
			}
		}
		// 母波静默消散（能量已均摊分发到子波）
		wave.discard();
		return true;
	}
}
