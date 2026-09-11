package com.hjmmd_8.createoreexpansion.content.charger.entity;

import java.util.List;

import com.hjmmd_8.createoreexpansion.content.wave.block.AbstractWaveGateBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.OctaEnergyWaveDifferencerBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.SixFaceDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SableBridges;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.EnergyWaveDispersal;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.EnergyWaveRegulation;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.OctaEnergyWaveDispersal;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.SixFaceDispersal;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.WaveSpeedRegulation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 能量波命中各机器的"执行器"（无实体 tick 职责，仅单次命中判定）。
 *
 * <p>主世界、Sable 结构（本地坐标系）、contraption 三种场景共用同一套机器判定，
 * 差异只在坐标系与产出位置换算——由各碰撞协调类负责转换，本类只操作
 * {@code wave} 的状态（movement/waveLevel/speedOffset/位置）并产出结果。</p>
 *
 * <p>判定逻辑（入口映射、开口统计、决策）已抽取至 regulation 纯判定类
 * （{@link EnergyWaveRegulation}/{@link WaveSpeedRegulation}/{@link EnergyWaveDispersal}/
 * {@link SixFaceDispersal}），本类仅执行结果：反弹 / 拐弯 / 均摊分裂 / 撞墙湮灭。</p>
 *
 * <p><b>包内可见性</b>：与 {@link AbstractChargerWaveEntity} 同包，可直接读写其
 * package/protected 状态与方法（movement、waveLevel、speedOffset、createChildWave 等），
 * 保持实体字段封装（跨包不可见）。</p>
 */
public class WaveMachineActions {

	/**
	 * 调级器增强延迟（格）：穿过顺基准调级器后还需飞行 0.5 格
	 * 才升级（0.5 格 ÷ 波速 v = 1/(2v) 秒）。0 = 无待升级。
	 */
	private static final float BOOST_DISTANCE = 0.5f;

	private final AbstractChargerWaveEntity wave;

	public WaveMachineActions(AbstractChargerWaveEntity wave) {
		this.wave = wave;
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
	public boolean handleRegulator(AbstractWaveGateBlockEntity regulator, BlockPos pos, Vec3 wavePos) {
		EnergyWaveRegulation.Result result = EnergyWaveRegulation.get()
			.resolve(regulator, wavePos, wave.movement, wave.waveLevel);
		switch (result) {
			case VANISH -> {
				// 齿轮端/入口关闭：如撞墙消失，无爆炸
				return true;
			}
			case VANISH_GAMMA_BOOM -> {
				// 伽马波（3级）顺基准升级无路可升 → 3 级伽马爆炸后湮灭
				ChargerWaveFx.triggerBoom(wave.level(), wave, wave.position(), wave.renderColor, null, 3);
				return true;
			}
			case VANISH_LOW_BOOM -> {
				// 1 级波逆基准降级无路可降 → 1 级小范围爆炸后湮灭
				ChargerWaveFx.triggerBoom(wave.level(), wave, wave.position(), wave.renderColor, null, 1);
				return true;
			}
			case PASS_UNCHANGED -> {
				// 无应力双开口：等级不变，正常穿过
			}
			case BOUNCE -> {
				// 无应力单开口：等级不变，原路遣返（折 180° 原路返回）
				wave.movement = wave.movement.scale(-1);
			}
			case PASS_BOOST_LATER -> {
				// 顺基准双开口：穿过，延迟升级（飞行 0.5 格 = 1/(2v) 秒后等级提升）。
				// 提升级数按机型参数：翡翠/蓝宝石恒 1；星辉石按本机转速 1 或 2（波侧封顶 MAX_LEVEL=5）
				wave.boostRemaining = BOOST_DISTANCE;
				wave.boostStep = regulator.getBoostStepForSpeed();
			}
			case PASS_DOWNGRADE -> {
				// 逆基准双开口：穿过，立即降级
				wave.setWaveLevel(wave.waveLevel - 1);
			}
			case BOUNCE_DOWNGRADE -> {
				// 有应力单开口：降级并原路遣返（1 级波已在判定层走 VANISH_LOW_BOOM 爆炸，这里仅 ≥2 级）
				wave.setWaveLevel(wave.waveLevel - 1);
				wave.movement = wave.movement.scale(-1);
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
	public boolean handleWaveSpeedRegulator(AbstractWaveGateBlockEntity speedRegulator, BlockPos pos, Vec3 wavePos) {
		WaveSpeedRegulation.Result result = WaveSpeedRegulation.get()
			.resolve(speedRegulator, wavePos, wave.movement, wave.waveLevel);
		switch (result) {
			case VANISH -> {
				return true; // 齿轮端/入口关闭：撞墙湮灭
			}
			case BOUNCE -> {
				// 单开口：纯折返，速度不变
				wave.movement = wave.movement.scale(-1);
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
	 * 处理一次八面能量波差器判定（8 口：4 正交 + 4 斜）。
	 *
	 * <p><b>几何/规则</b>：见 {@link OctaEnergyWaveDispersal}。波从本地正交方向轴向进入，
	 * 按波前在入口面内的横向位置决定入口正交口或相邻斜口；1 开口反弹、2 开口转向、
	 * 3-4/5-6/7-8 开口分裂降 1/2/3 级；出口为斜口时子波沿 45° 对角方向飞行。</p>
	 *
	 * <p><b>姿态</b>：开口属性=本地；AXIS=X/Z 躺姿时世界坐标先换算到本地（站姿语义）判定，
	 * 产出方向再换回世界。axis=Y 时本地=世界，无需换算。</p>
	 *
	 * @param state   机器方块状态（AXIS + 8 open_* 属性）
	 * @param pos     机器方块位置（与 wavePos 同坐标系：主世界=世界坐标，结构=本地坐标）
	 * @param wavePos 波前中心（与 pos 同坐标系）
	 * @param frame   波所在 sub-level（结构本地坐标系，分裂子波转回世界用）；null = 主世界
	 * @return true = 波应撞墙湮灭；false = 波继续（反弹/转向，调用方负责推出方块外；
	 *         分裂时本波已被静默 discard，调用方检测 isAlive()==false 直接结束）
	 */
	public boolean handleOctaDisperser(BlockState state, BlockPos pos, Vec3 wavePos, SubLevelBridge.Hit frame) {
		Direction.Axis axis = state.getValue(OctaEnergyWaveDifferencerBlock.AXIS);
		// 世界（或结构本地）→ 机器本地（站姿语义）：判定纯函数只认识本地方向/偏移
		Vec3 localMove = OctaEnergyWaveDifferencerBlock.toLocalVec(axis, wave.movement);
		Vec3 localRel = OctaEnergyWaveDifferencerBlock.toLocalVec(axis, wavePos.subtract(Vec3.atCenterOf(pos)));

		OctaEnergyWaveDispersal.Result result = OctaEnergyWaveDispersal.handle(state, localMove, localRel, wave.waveLevel);
		switch (result) {
			case VANISH -> {
				return true; // 机壳 / 入口口关闭 / 波级不足分裂
			}
			case BOUNCE -> {
				// 单开口：原路遣返（反弹），等级不变（世界方向直接取反）
				wave.movement = wave.movement.scale(-1);
				return false;
			}
			case TURN -> {
				// 双开口：从入口进、从另一开口出（转向），等级不变
				List<Vec3> outs = OctaEnergyWaveDispersal.otherOpenDirs(state, localMove, localRel);
				if (outs.isEmpty())
					return true;
				// 出口方向（本地）→ 世界
				wave.movement = OctaEnergyWaveDifferencerBlock.toWorldVec(axis, outs.get(0));
				return false;
			}
			case SPLIT -> {
				// ≥3 开口：其余每个开口均摊发射降级子波（出口斜口 = 45° 对角）
				int childLevel = wave.waveLevel - OctaEnergyWaveDispersal.decrementOf(state);
				Vec3 center = Vec3.atCenterOf(pos);
				for (Vec3 localOut : OctaEnergyWaveDispersal.otherOpenDirs(state, localMove, localRel)) {
					// 出口方向：本地 → 世界（axis=Y 不变）
					Vec3 worldOut = OctaEnergyWaveDifferencerBlock.toWorldVec(axis, localOut);
					// 在差器方块中心沿出口方向外推 1 格出生，确保碰撞盒离开差器
					AbstractChargerWaveEntity child = wave.createChildWave(center.add(worldOut), worldOut, childLevel);
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
						child.addSpeedOffset(wave.speedOffset);
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
	public boolean handleDisperser(BlockState state, BlockPos pos, Vec3 wavePos, SubLevelBridge.Hit frame) {
		EnergyWaveDispersal.Result result = EnergyWaveDispersal.handle(state, wave.movement, wavePos, pos, wave.waveLevel);
		Direction facing = state.getValue(EnergyWaveDisperserBlock.FACING);

		switch (result) {
			case VANISH -> {
				// 机壳面 / 入口关闭 / 1 级波分裂降级无路可降 → 撞墙湮灭
				return true;
			}
			case BOUNCE -> {
				// 单开口：原路遣返（反弹），等级不变
				wave.movement = wave.movement.scale(-1);
				return false;
			}
			case TURN -> {
				// 双开口：从入口进、从另一开口出（拐弯），等级不变
				Direction worldOut = EnergyWaveDisperserBlock.worldDirOf(facing,
					EnergyWaveDispersal.exitsOf(state, wave.movement).get(0));
				wave.movement = Vec3.atLowerCornerOf(worldOut.getNormal());
				return false;
			}
			case SPLIT -> {
				// ≥3 开口：其余每个开口均摊发射降一级的波
				int childLevel = wave.waveLevel - 1;
				Vec3 center = Vec3.atCenterOf(pos);
				for (Direction modelSide : EnergyWaveDispersal.exitsOf(state, wave.movement)) {
					Direction worldOut = EnergyWaveDisperserBlock.worldDirOf(facing, modelSide);
					Vec3 outDir = Vec3.atLowerCornerOf(worldOut.getNormal());
					// 在差器方块中心沿出口方向外推 1 格出生，确保碰撞盒离开差器
					AbstractChargerWaveEntity child = wave.createChildWave(center.add(outDir), outDir, childLevel);
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
						child.addSpeedOffset(wave.speedOffset);
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
	public boolean handleSixFaceDisperser(BlockState state, BlockPos pos, Vec3 wavePos, SubLevelBridge.Hit frame) {
		SixFaceDispersal.Result result = SixFaceDispersal.handle(state, wave.movement, wavePos, pos, wave.waveLevel);
		switch (result) {
			case VANISH -> {
				return true; // 入口关闭 / 波级不足分裂 → 撞墙湮灭
			}
			case BOUNCE -> {
				// 单开口：原路遣返（反弹），等级不变
				wave.movement = wave.movement.scale(-1);
				return false;
			}
			case TURN -> {
				// 双开口：从入口进、从另一开口出（拐弯），等级不变
				Direction worldOut = SixFaceDispersal.exitsOf(state, wave.movement).get(0);
				wave.movement = Vec3.atLowerCornerOf(worldOut.getNormal());
				return false;
			}
			case SPLIT -> {
				// 3-4 开口降一级、5-6 开口降二级：其余每个开口均摊发射子波
				int childLevel = wave.waveLevel - SixFaceDispersal.decrementOf(state);
				Vec3 center = Vec3.atCenterOf(pos);
				for (Direction worldOut : SixFaceDispersal.exitsOf(state, wave.movement)) {
					Vec3 outDir = Vec3.atLowerCornerOf(worldOut.getNormal());
					// 在差器方块中心沿出口方向外推 1 格出生，确保碰撞盒离开差器
					AbstractChargerWaveEntity child = wave.createChildWave(center.add(outDir), outDir, childLevel);
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
						child.addSpeedOffset(wave.speedOffset);
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
}
