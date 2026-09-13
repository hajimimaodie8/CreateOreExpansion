package com.hjmmd_8.createoreexpansion.content.charger.entity;

import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.AbstractWaveGateBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.AbstractWaveGateBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.OctaEnergyWaveDifferencerBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.SixFaceDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SableBridges;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 能量波在物理结构（Sable sub-level）上的碰撞协调。
 *
 * <p>波实体本身始终在主世界（位置/渲染/粒子用世界坐标）；本类把波中心/方向换算到
 * 结构本地坐标系（经 {@link SubLevelBridge} 位姿矩阵），遍历波覆盖的结构方块，
 * 命中机器时执行与主世界相同的判定（复用 {@link WaveMachineActions}），
 * 产出位置/方向再转回世界。坐标换算细节见 {@link #handle()}。</p>
 *
 * <p>波闸的 4×4 入口中心判定在本地坐标系天然正确：BE 的 pos/FACING 是本地，波中心
 * 换算成本地后，{@code StaticWaveGateFrame} 用本地 FACING 与本地坐标判定面内偏移。</p>
 */
public class WaveSubLevelCollisions {

	private final AbstractChargerWaveEntity wave;
	private final WaveMachineActions actions;

	public WaveSubLevelCollisions(AbstractChargerWaveEntity wave) {
		this.wave = wave;
		this.actions = new WaveMachineActions(wave);
	}

	/**
	 * 主世界无命中时的结构补充判定入口：波中心落在某结构 plot 内才处理。
	 *
	 * @return true = 本 tick 已在结构上处理完毕
	 */
	public boolean tryHandle() {
		SubLevelBridge bridge = SableBridges.get();
		if (bridge == null)
			return false;
		SubLevelBridge.Hit hit = bridge.query(wave.level(), wave.getBoundingBox()
			.getCenter());
		if (hit == null)
			return false;
		handle(bridge, hit);
		return true;
	}

	/**
	 * 以结构本地坐标系遍历波覆盖的方块并执行机器判定。
	 *
	 * <p><b>坐标约定</b>：本方法内 {@code wave.getMovement()} 临时切换为结构本地方向参与判定
	 * （判定函数读写 movement），判定结束后经位姿矩阵转回世界方向；推出方块、分裂子波等
	 * 产出位置同样转回世界。</p>
	 */
	private void handle(SubLevelBridge bridge, SubLevelBridge.Hit hit) {
		Vec3 localCenter = bridge.toLocal(hit, wave.getBoundingBox()
			.getCenter());
		Vec3 localMove = bridge.toLocalDir(hit, wave.getMovement());
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

			// 判定前：把波方向临时切换为结构本地方向（判定函数读写 movement）
			wave.setMovement(localMove);
			try {
				// 波前中心（结构本地坐标，与 pos 同坐标系——4×4 入口判定依赖）
				Vec3 localWavePos = bridge.toLocal(hit, wave.getBoundingBox()
					.getCenter());
				// 伽马能量加工（≥3 级）：伽马/伊普西龙/欧米伽波命中强化避雷针各 +1，波消散
				if (bridge.getBlockEntity(hit, pos) instanceof ReinforcedLightningRodBlockEntity rod) {
					if (wave.getWaveLevel() >= 3)
						rod.onGammaWaveHit();
					ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
					wave.discard();
					return;
				}
				// 能量波闸（调级器/波速，翡翠/蓝宝石）：面板通道 + 应力调制
				if (state.getBlock() instanceof AbstractWaveGateBlock waveGate
					&& bridge.getBlockEntity(hit, pos) instanceof AbstractWaveGateBlockEntity gateBe) {
					boolean isRegulator = waveGate.modulatesWaveLevel();
					boolean vanish = isRegulator
						? actions.handleRegulator(gateBe, pos, localWavePos)
						: actions.handleWaveSpeedRegulator(gateBe, pos, localWavePos);
					if (vanish) {
						ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
						wave.discard();
						return;
					}
					wave.setPos(bridge.toWorld(hit, Vec3.atCenterOf(pos)
						.add(wave.getMovement().scale(1.0d))));
					return;
				}
				// 能量波差器：反弹/拐弯/分裂（分裂子波在结构本地出生，经 frame 转回世界后加入主世界）
				if (state.getBlock() instanceof EnergyWaveDisperserBlock) {
					boolean vanish = actions.handleDisperser(state, pos, localWavePos, hit);
					if (vanish) {
						ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
						wave.discard();
						return;
					}
					if (!wave.isAlive())
						return; // 分裂：母波静默消散
					wave.setPos(bridge.toWorld(hit, Vec3.atCenterOf(pos)
						.add(wave.getMovement().scale(1.0d))));
					return;
				}
				// 六面能量波差器
				if (state.getBlock() instanceof SixFaceDisperserBlock) {
					boolean vanish = actions.handleSixFaceDisperser(state, pos, localWavePos, hit);
					if (vanish) {
						ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
						wave.discard();
						return;
					}
					if (!wave.isAlive())
						return;
					wave.setPos(bridge.toWorld(hit, Vec3.atCenterOf(pos)
						.add(wave.getMovement().scale(1.0d))));
					return;
				}
				// 八面能量波差器（8 口：4 正交 + 4 斜；姿态换算在 actions 内完成）
				if (state.getBlock() instanceof OctaEnergyWaveDifferencerBlock) {
					boolean vanish = actions.handleOctaDisperser(state, pos, localWavePos, hit);
					if (vanish) {
						ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
						wave.discard();
						return;
					}
					if (!wave.isAlive())
						return; // 分裂：母波静默消散
					wave.setPos(bridge.toWorld(hit, Vec3.atCenterOf(pos)
						.add(wave.getMovement().scale(1.0d))));
					return;
				}
				// 结构上的其它方块（含能量感应灯/置物台——本地无 Capability 查询入口）→ 视为撞墙
				hitSolid = true;
			} finally {
				// 判定结果（反弹/穿出方向）从本地方向转回世界方向
				wave.setMovement(bridge.toWorldDir(hit, wave.getMovement()));
			}
		}
		if (hitSolid) {
			ChargerWaveFx.burst(wave.level(), wave.position(), wave.getRenderColor());
			wave.discard();
		}
	}
}
