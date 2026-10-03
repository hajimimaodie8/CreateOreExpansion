package com.hjmmd_8.createoreexpansion.content.wave.block;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveMachineHandler;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SableBridges;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.EnergyWaveDispersal;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.OctaEnergyWaveDispersal;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.SixFaceDispersal;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * <b>差波器家族（四面 / 六面 / 八面）的波命中处置</b>（W6-a）。
 *
 * <p>机器侧代码的来源与搬运原则同 {@link WaveGateHitHandler}：主世界那三条分支原住第一层的
 * {@code WaveMachineActions}，contraption 那三条原住 {@code WaveContraptionCollisions}，
 * 物理结构那三条原住 {@code WaveSubLevelCollisions}。<b>判定判定与副作用逐项不变</b>，
 * 只是把"谁持有 wave"从字段改成参数、把 burst/discard/推出方块外按原样收进本类
 * （结构场景需要位姿矩阵转换，只能在本类做）。</p>
 */
public final class DisperserHitHandler implements WaveMachineHandler {

	/** 无状态处理器，登记一次即可。 */
	public static final DisperserHitHandler INSTANCE = new DisperserHitHandler();

	private DisperserHitHandler() {
	}

	// ================= 主世界 =================

	@Override
	public Outcome onWorldHit(AbstractChargerWaveEntity wave, BlockPos pos, BlockState state) {
		// 命中顺序与原 WaveHitResolver 的 if 链一致：四面 → 六面 → 八面。
		if (state.getBlock() instanceof EnergyWaveDisperserBlock) {
			boolean vanish = handleDisperser(wave, state, pos, wave.getBoundingBox()
				.getCenter(), null);
			if (vanish)
				return Outcome.BURST;
			// 分裂：母波已在执行器内静默 discard（无撞墙特效），直接结束本 tick
			if (!wave.isAlive())
				return Outcome.CONSUMED;
			return Outcome.PUSH;
		}
		if (state.getBlock() instanceof SixFaceDisperserBlock) {
			boolean vanish = handleSixFaceDisperser(wave, state, pos, wave.getBoundingBox()
				.getCenter(), null);
			if (vanish)
				return Outcome.BURST;
			if (!wave.isAlive())
				return Outcome.CONSUMED; // 分裂：母波静默消散
			return Outcome.PUSH;
		}
		if (state.getBlock() instanceof OctaEnergyWaveDifferencerBlock) {
			boolean vanish = handleOctaDisperser(wave, state, pos, wave.getBoundingBox()
				.getCenter(), null);
			if (vanish)
				return Outcome.BURST;
			if (!wave.isAlive())
				return Outcome.CONSUMED; // 分裂：母波静默消散
			return Outcome.PUSH;
		}
		return Outcome.NOT_MINE;
	}

	/**
	 * 处理一次八面能量波差器判定（8 口：4 正交 + 4 斜）。
	 *
	 * <p><b>几何/规则</b>：见 {@link OctaEnergyWaveDispersal}。波从本地正交方向轴向进入，
	 * 按波前在入口面内的横向位置决定入口正交口或相邻斜口；1 开口反弹、2 开口转向、
	 * 3-4/5-6/7-8 开口分裂降 1/2/3 级；出口为斜口时子波沿 45° 对角方向飞行。</p>
	 *
	 * @param state   机器方块状态（AXIS + 8 open_* 属性）
	 * @param pos     机器方块位置（与 wavePos 同坐标系：主世界=世界坐标，结构=本地坐标）
	 * @param wavePos 波前中心（与 pos 同坐标系）
	 * @param frame   波所在 sub-level（结构本地坐标系，分裂子波转回世界用）；null = 主世界
	 * @return true = 波应撞墙湮灭；false = 波继续（反弹/转向，调用方负责推出方块外；
	 *         分裂时本波已被静默 discard，调用方检测 isAlive()==false 直接结束）
	 */
	private static boolean handleOctaDisperser(AbstractChargerWaveEntity wave, BlockState state, BlockPos pos,
		Vec3 wavePos, SubLevelBridge.Hit frame) {
		Direction.Axis axis = state.getValue(OctaEnergyWaveDifferencerBlock.AXIS);
		// 世界（或结构本地）→ 机器本地（站姿语义）：判定纯函数只认识本地方向/偏移
		Vec3 localMove = OctaEnergyWaveDifferencerBlock.toLocalVec(axis, wave.getMovement());
		Vec3 localRel = OctaEnergyWaveDifferencerBlock.toLocalVec(axis, wavePos.subtract(Vec3.atCenterOf(pos)));

		OctaEnergyWaveDispersal.Result result = OctaEnergyWaveDispersal.handle(state, localMove, localRel, wave.getWaveLevel());
		switch (result) {
			case VANISH -> {
				return true; // 机壳 / 入口口关闭 / 波级不足分裂
			}
			case BOUNCE -> {
				// 单开口：原路遣返（反弹），等级不变（世界方向直接取反）
				wave.setMovement(wave.getMovement().scale(-1));
				return false;
			}
			case TURN -> {
				// 双开口：从入口进、从另一开口出（转向），等级不变
				List<Vec3> outs = OctaEnergyWaveDispersal.otherOpenDirs(state, localMove, localRel);
				if (outs.isEmpty())
					return true;
				// 出口方向（本地）→ 世界
				wave.setMovement(OctaEnergyWaveDifferencerBlock.toWorldVec(axis, outs.get(0)));
				return false;
			}
			case SPLIT -> {
				// ≥3 开口：其余每个开口均摊发射降级子波（出口斜口 = 45° 对角）
				int childLevel = wave.getWaveLevel() - OctaEnergyWaveDispersal.decrementOf(state);
				Vec3 center = Vec3.atCenterOf(pos);
				// 先取出口列表：载荷要按"子波总数"分份，故需要 index/total（见 createChildWave 注释）
				List<Vec3> octaOuts = OctaEnergyWaveDispersal.otherOpenDirs(state, localMove, localRel);
				for (int outIndex = 0; outIndex < octaOuts.size(); outIndex++) {
					Vec3 localOut = octaOuts.get(outIndex);
					// 出口方向：本地 → 世界（axis=Y 不变）
					Vec3 worldOut = OctaEnergyWaveDifferencerBlock.toWorldVec(axis, localOut);
					// 在差器方块中心沿出口方向外推 1 格出生，确保碰撞盒离开差器
					AbstractChargerWaveEntity child = wave.createChildWave(center.add(worldOut), worldOut, childLevel,
						outIndex, octaOuts.size());
					if (child != null) {
						if (frame != null) {
							// 结构本地出生 → 转回世界坐标/方向（波必须出生在真实世界）；
							// spawnPos 同步修正，否则距离上限检查会把子波当"飞太远"立即消散
							SubLevelBridge b = SableBridges.get();
							if (b != null) {
								child.setPos(b.toWorld(frame, child.position()));
								child.setSpawnPos(b.toWorld(frame, child.getSpawnPos()));
								child.setMovement(b.toWorldDir(frame, child.getMovement()));
							}
						}
						// 子波继承母波的速度修正（波速调节器叠加值贯穿分裂传播链）
						child.addSpeedOffset(wave.getSpeedOffset());
						wave.level()
							.addFreshEntity(child);
					}
				}
				// 母波静默消失（能量已均摊分发到子波；不触发撞墙湮灭特效，
				// 调用方见 isAlive()==false 直接结束）
				wave.discard();
				return false;
			}
		}
		return false;
	}

	/**
	 * 处理一次四面能量波差器判定（多开口均摊分发）。
	 *
	 * @param state   差器方块状态（FACING + 4 开口属性）
	 * @param pos     差器方块位置（与 wavePos 同坐标系）
	 * @param wavePos 波前中心（结构场景为本地坐标，用于 4×4 入口判定）
	 * @param frame   波所在 sub-level（结构本地坐标系，分裂子波转回世界用）；null = 主世界
	 * @return true = 波应撞墙湮灭（调用方负责 burst + discard）；false = 波继续（反弹/拐弯，
	 *         调用方负责推出方块外；若本波已被静默 discard——分裂场景——调用方检测 isAlive()==false 直接结束）
	 */
	private static boolean handleDisperser(AbstractChargerWaveEntity wave, BlockState state, BlockPos pos,
		Vec3 wavePos, SubLevelBridge.Hit frame) {
		EnergyWaveDispersal.Result result = EnergyWaveDispersal.handle(state, wave.getMovement(), wavePos, pos, wave.getWaveLevel());
		Direction facing = state.getValue(EnergyWaveDisperserBlock.FACING);

		switch (result) {
			case VANISH -> {
				// 机壳面 / 入口关闭 / 1 级波分裂降级无路可降 → 撞墙湮灭
				return true;
			}
			case BOUNCE -> {
				// 单开口：原路遣返（反弹），等级不变
				wave.setMovement(wave.getMovement().scale(-1));
				return false;
			}
			case TURN -> {
				// 双开口：从入口进、从另一开口出（拐弯），等级不变
				Direction worldOut = EnergyWaveDisperserBlock.worldDirOf(facing,
					EnergyWaveDispersal.exitsOf(state, wave.getMovement()).get(0));
				wave.setMovement(Vec3.atLowerCornerOf(worldOut.getNormal()));
				return false;
			}
			case SPLIT -> {
				// ≥3 开口：其余每个开口均摊发射降一级的波
				int childLevel = wave.getWaveLevel() - 1;
				Vec3 center = Vec3.atCenterOf(pos);
				List<Direction> disperserOuts = EnergyWaveDispersal.exitsOf(state, wave.getMovement());
				for (int outIndex = 0; outIndex < disperserOuts.size(); outIndex++) {
					Direction worldOut = EnergyWaveDisperserBlock.worldDirOf(facing, disperserOuts.get(outIndex));
					Vec3 outDir = Vec3.atLowerCornerOf(worldOut.getNormal());
					// 在差器方块中心沿出口方向外推 1 格出生，确保碰撞盒离开差器
					AbstractChargerWaveEntity child = wave.createChildWave(center.add(outDir), outDir, childLevel,
						outIndex, disperserOuts.size());
					if (child != null) {
						if (frame != null) {
							// 结构本地出生 → 转回世界坐标/方向（波必须出生在真实世界）；
							// spawnPos 同步修正，否则距离上限检查会把子波当"飞太远"立即消散
							SubLevelBridge b = SableBridges.get();
							if (b != null) {
								child.setPos(b.toWorld(frame, child.position()));
								child.setSpawnPos(b.toWorld(frame, child.getSpawnPos()));
								child.setMovement(b.toWorldDir(frame, child.getMovement()));
							}
						}
						// 子波继承母波的速度修正（波速调节器叠加值贯穿分裂传播链）
						child.addSpeedOffset(wave.getSpeedOffset());
						wave.level()
							.addFreshEntity(child);
					}
				}
				// 母波静默消失（能量已均摊分发到子波；不触发撞墙湮灭特效，
				// 调用方见 isAlive()==false 直接结束）
				wave.discard();
				return false;
			}
		}
		return false;
	}

	/**
	 * 处理一次六面能量波差器判定（无朝向，入口 = 运动反方向世界面，6 面皆可开口）。
	 * 规则：入口关闭→撞墙消失；1 开口→反弹；2 开口→拐弯；3-4 开口→其余开口各发降一级子波；
	 * 5-6 开口→其余开口各发降二级子波。
	 *
	 * @param state   差器方块状态（6 个 open_* 属性）
	 * @param pos     差器方块位置（与 wavePos 同坐标系）
	 * @param wavePos 波前中心（结构场景为本地坐标，用于 4×4 入口判定）
	 * @param frame   波所在 sub-level（结构本地坐标系，分裂子波转回世界用）；null = 主世界
	 * @return true = 波应撞墙湮灭；false = 波继续（调用方推出方块外；分裂时本波已静默 discard）
	 */
	private static boolean handleSixFaceDisperser(AbstractChargerWaveEntity wave, BlockState state, BlockPos pos,
		Vec3 wavePos, SubLevelBridge.Hit frame) {
		SixFaceDispersal.Result result = SixFaceDispersal.handle(state, wave.getMovement(), wavePos, pos, wave.getWaveLevel());
		switch (result) {
			case VANISH -> {
				return true; // 入口关闭 / 波级不足分裂 → 撞墙湮灭
			}
			case BOUNCE -> {
				// 单开口：原路遣返（反弹），等级不变
				wave.setMovement(wave.getMovement().scale(-1));
				return false;
			}
			case TURN -> {
				// 双开口：从入口进、从另一开口出（拐弯），等级不变
				Direction worldOut = SixFaceDispersal.exitsOf(state, wave.getMovement()).get(0);
				wave.setMovement(Vec3.atLowerCornerOf(worldOut.getNormal()));
				return false;
			}
			case SPLIT -> {
				// 3-4 开口降一级、5-6 开口降二级：其余每个开口均摊发射子波
				int childLevel = wave.getWaveLevel() - SixFaceDispersal.decrementOf(state);
				Vec3 center = Vec3.atCenterOf(pos);
				List<Direction> sixOuts = SixFaceDispersal.exitsOf(state, wave.getMovement());
				for (int outIndex = 0; outIndex < sixOuts.size(); outIndex++) {
					Vec3 outDir = Vec3.atLowerCornerOf(sixOuts.get(outIndex)
						.getNormal());
					// 在差器方块中心沿出口方向外推 1 格出生，确保碰撞盒离开差器
					AbstractChargerWaveEntity child = wave.createChildWave(center.add(outDir), outDir, childLevel,
						outIndex, sixOuts.size());
					if (child != null) {
						if (frame != null) {
							// 结构本地出生 → 转回世界坐标/方向（波必须出生在真实世界）；
							// spawnPos 同步修正，否则距离上限检查会把子波当"飞太远"立即消散
							SubLevelBridge b = SableBridges.get();
							if (b != null) {
								child.setPos(b.toWorld(frame, child.position()));
								child.setSpawnPos(b.toWorld(frame, child.getSpawnPos()));
								child.setMovement(b.toWorldDir(frame, child.getMovement()));
							}
						}
						// 子波继承母波的速度修正（波速调节器叠加值贯穿分裂传播链）
						child.addSpeedOffset(wave.getSpeedOffset());
						wave.level()
							.addFreshEntity(child);
					}
				}
				// 母波静默消失（能量已均摊分发到子波）
				wave.discard();
				return false;
			}
		}
		return false;
	}

	// ================= Create contraption（结构本地坐标系） =================

	@Override
	public Outcome onContraptionHit(AbstractChargerWaveEntity wave, BlockPos pos, BlockState state,
		AbstractContraptionEntity entity, Vec3 localCenter) {
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
				if (splitContraptionChildren(wave, localExits, 1, entity, pos))
					return Outcome.CONSUMED;
				return Outcome.SKIP;
			}
			if (handleDisperserResult(wave, r, entity, pos, state))
				return Outcome.CONSUMED;
			return Outcome.SKIP;
		}
		// 六面能量波差器：反弹/拐弯/分裂（降级量 3-4 口 1 级、5-6 口 2 级）
		if (state.getBlock() instanceof SixFaceDisperserBlock) {
			SixFaceDispersal.Result r = SixFaceDispersal.handle(state, wave.getMovement(), localCenter, pos,
				wave.getWaveLevel());
			if (r == SixFaceDispersal.Result.SPLIT) {
				if (splitContraptionChildren(wave, SixFaceDispersal.exitsOf(state, wave.getMovement()),
					SixFaceDispersal.decrementOf(state), entity, pos))
					return Outcome.CONSUMED;
				return Outcome.SKIP;
			}
			if (handleSixFaceDisperserResult(wave, r, entity, pos, state))
				return Outcome.CONSUMED;
			return Outcome.SKIP;
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
					ChargerWaveFx.burst(wave.level(), wave.position(), wave.trailStyle(), wave.getRenderColor());
					wave.discard();
					return Outcome.CONSUMED;
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
				return Outcome.CONSUMED;
			}
			if (handleOctaDisperserResult(wave, r, entity, pos, axis, octaLocalMove, relCenter, state))
				return Outcome.CONSUMED;
			return Outcome.SKIP;
		}
		return Outcome.NOT_MINE;
	}

	/** 处理八面差波器结果（contraption 场景）；返回 true = 本 tick 已处理。 */
	private static boolean handleOctaDisperserResult(AbstractChargerWaveEntity wave,
		OctaEnergyWaveDispersal.Result r, AbstractContraptionEntity entity,
		BlockPos pos, Direction.Axis axis, Vec3 localMove, Vec3 relCenter, BlockState state) {
		switch (r) {
			case VANISH -> {
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.trailStyle(), wave.getRenderColor());
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
	private static boolean handleDisperserResult(AbstractChargerWaveEntity wave, EnergyWaveDispersal.Result r,
		AbstractContraptionEntity entity, BlockPos pos, BlockState state) {
		switch (r) {
			case VANISH -> {
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.trailStyle(), wave.getRenderColor());
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
	private static boolean handleSixFaceDisperserResult(AbstractChargerWaveEntity wave, SixFaceDispersal.Result r,
		AbstractContraptionEntity entity, BlockPos pos, BlockState state) {
		switch (r) {
			case VANISH -> {
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.trailStyle(), wave.getRenderColor());
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
	private static boolean splitContraptionChildren(AbstractChargerWaveEntity wave, List<Direction> exits,
		int decrement, AbstractContraptionEntity entity, BlockPos pos) {
		int childLevel = wave.getWaveLevel() - decrement;
		if (childLevel <= 0) {
			// 波级不足分裂：撞墙消散
			ChargerWaveFx.burst(wave.level(), wave.position(), wave.trailStyle(), wave.getRenderColor());
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

	// ================= Sable 物理结构（结构本地坐标系） =================

	@Override
	public Outcome onSubLevelHit(AbstractChargerWaveEntity wave, BlockPos pos, BlockState state,
		SubLevelBridge bridge, SubLevelBridge.Hit hit) {
		Vec3 localWavePos = bridge.toLocal(hit, wave.getBoundingBox()
			.getCenter());
		// 能量波差器：反弹/拐弯/分裂（分裂子波在结构本地出生，经 frame 转回世界后加入主世界）
		if (state.getBlock() instanceof EnergyWaveDisperserBlock) {
			boolean vanish = handleDisperser(wave, state, pos, localWavePos, hit);
			if (vanish) {
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.trailStyle(), wave.getRenderColor());
				wave.discard();
				return Outcome.CONSUMED;
			}
			if (!wave.isAlive())
				return Outcome.CONSUMED; // 分裂：母波静默消散
			wave.setPos(bridge.toWorld(hit, Vec3.atCenterOf(pos)
				.add(wave.getMovement().scale(1.0d))));
			return Outcome.CONSUMED;
		}
		// 六面能量波差器
		if (state.getBlock() instanceof SixFaceDisperserBlock) {
			boolean vanish = handleSixFaceDisperser(wave, state, pos, localWavePos, hit);
			if (vanish) {
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.trailStyle(), wave.getRenderColor());
				wave.discard();
				return Outcome.CONSUMED;
			}
			if (!wave.isAlive())
				return Outcome.CONSUMED;
			wave.setPos(bridge.toWorld(hit, Vec3.atCenterOf(pos)
				.add(wave.getMovement().scale(1.0d))));
			return Outcome.CONSUMED;
		}
		// 八面能量波差器（8 口：4 正交 + 4 斜；姿态换算在 actions 内完成）
		if (state.getBlock() instanceof OctaEnergyWaveDifferencerBlock) {
			boolean vanish = handleOctaDisperser(wave, state, pos, localWavePos, hit);
			if (vanish) {
				ChargerWaveFx.burst(wave.level(), wave.position(), wave.trailStyle(), wave.getRenderColor());
				wave.discard();
				return Outcome.CONSUMED;
			}
			if (!wave.isAlive())
				return Outcome.CONSUMED; // 分裂：母波静默消散
			wave.setPos(bridge.toWorld(hit, Vec3.atCenterOf(pos)
				.add(wave.getMovement().scale(1.0d))));
			return Outcome.CONSUMED;
		}
		return Outcome.NOT_MINE;
	}
}
