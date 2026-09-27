package com.hjmmd_8.createoreexpansion.content.charger.wave;

import java.lang.ref.WeakReference;
import java.util.Map;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 能量波在 Create 动态结构（contraption：动力轴承/矿车装配站装配）上的碰撞协调。
 *
 * <p><b>机制</b>：contraption 方块留在主世界（数据在 {@link Contraption} 内），
 * 波中心经 {@code entity.toLocalVector} 换算到结构本地坐标，本地查方块状态后交给**机器处理器**
 * （第二层的 {@code WaveMachineHandler}，见 {@link WaveMachineHandlers.dispatchContraption}——
 * contraption 上无 BE 实例/转速 → 波闸仅通道不调制）。判定结果方向经 {@code entity.applyRotation} 转回世界。</p>
 *
 * <p><b>W6-a</b>：机器分支（波闸 / 三种差波器）原先直接写在本类里（那是 {@code L1 → L2} 禁止边），
 * 现已搬进第二层的处理器；本类只保留"结构遍历 + 坐标换算 + 其它方块撞墙"这三件事。</p>
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
					// 机器方块（波闸 / 三种差波器）：由第二层的处理器认领并处置
					// （判定/换算/特效全在处理器内，本类只按结果决定去留）。
					WaveMachineHandler.Outcome outcome =
						WaveMachineHandlers.dispatchContraption(wave, pos, state, entity, localCenter);
					if (outcome == WaveMachineHandler.Outcome.SKIP)
						continue; // 认领了但本次不处理：下一个方块
					if (outcome == WaveMachineHandler.Outcome.NOT_MINE) {
						// contraption 上的其它方块：视为撞墙
						ChargerWaveFx.burst(wave.level(), wave.position(), wave.getWaveType().trailStyle(), wave.getRenderColor());
						wave.discard();
					}
					return true;
				} finally {
					// 判定结果（反弹/穿出方向）从本地方向转回世界方向
					wave.setMovement(entity.applyRotation(wave.getMovement(), 0));
				}
			}
		}
		return false;
	}
}
