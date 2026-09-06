package com.hjmmd_8.createoreexpansion.content.wave.block;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.SimpleBlockMovingInteraction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;

/**
 * 能量波差器在 Create 动态结构（contraption）上的"开盖"交互（四面/六面/八面共用）。
 *
 * <p>Create 的 contraption 交互接口不提供"点击的面"（{@code ContraptionInteractionPacket} 的
 * face 只用于命中检测，不传给 behaviour）。四面/六面的 open_* 是整面级开关，用<b>玩家视角</b>
 * 推断目标面即可；八面需要面内细分（中心 6px=正交口、两侧 5px=斜口、顶/底 45° 8 扇区），
 * 因此八面分支用<b>玩家射线在 contraption 本地对目标方块做 slab 求交</b>还原精确命中面/点，
 * 复用与主世界 {@code onWrenched} 完全一致的 {@code resolveTarget} 判定。</p>
 *
 * <p>contraption 上的 blockstate 是<b>装配时</b>捕获的状态（属性为装配时的方向语义），
 * 玩家视角/射线是世界方向——两者在 contraption 旋转后不一致，因此射线先经
 * {@link AbstractContraptionEntity#reverseRotation}/{@code toLocalVector} 逆变换回
 * 装配本地（= blockstate 属性基准），判定与产出均在装配本地进行。</p>
 */
public class DisperserMovingInteraction extends SimpleBlockMovingInteraction {

	@Override
	protected BlockState handle(Player player, Contraption contraption, BlockPos localPos, BlockState state) {
		// 解析目标开口属性（机壳面/无开口面返回 null → 不切换）
		BooleanProperty target = null;
		if (state.getBlock() instanceof SixFaceDisperserBlock) {
			// 六向：6 面独立开关（本地方向直接对应属性）
			Direction worldFace = faceFromLook(player);
			Direction localFace = toLocalFace(contraption, worldFace);
			target = SixFaceDisperserBlock.propertyFor(localFace);
		} else if (state.getBlock() instanceof EnergyWaveDisperserBlock) {
			// 四面：open_* 是模型侧面属性，本地方向先映射到模型面（机壳顶/底 → null 不切换）
			Direction worldFace = faceFromLook(player);
			Direction localFace = toLocalFace(contraption, worldFace);
			Direction modelSide = EnergyWaveDisperserBlock.modelFaceOf(
				state.getValue(EnergyWaveDisperserBlock.FACING), localFace);
			if (modelSide != null)
				target = EnergyWaveDisperserBlock.propertyFor(modelSide);
		} else if (state.getBlock() instanceof OctaEnergyWaveDifferencerBlock) {
			// 八面：精确命中区域 → 与主世界扳手同一套判定（中心6px/两侧5px/顶底8扇区）
			Hit hit = rayLocal(player, contraption, localPos);
			if (hit != null) {
				Direction.Axis poseAxis = state.getValue(OctaEnergyWaveDifferencerBlock.AXIS);
				Direction clicked = OctaEnergyWaveDifferencerBlock.toLocalDir(poseAxis, hit.face);
				// 命中点相对方块中心（装配本地），换算到机器本地
				Vec3 rel = OctaEnergyWaveDifferencerBlock.toLocalVec(poseAxis,
					hit.point.subtract(Vec3.atCenterOf(localPos)));
				target = OctaEnergyWaveDifferencerBlock.resolveTarget(clicked, rel);
			}
		}
		if (target == null)
			return state;

		BlockState newState = state.setValue(target, !state.getValue(target));
		// 扳手音效（同主世界 onWrenched 的 WRENCH_ROTATE）。playSound 经 MC 的
		// "排除玩家"机制：客户端预测播一次、服务端执行不重放（Create 杠杆同款，无双响）。
		boolean open = newState.getValue(target);
		playSound(player, AllSoundEvents.WRENCH_ROTATE.getMainEvent(), open ? 0.8f : 0.5f);
		return newState;
	}

	// ===== 四面/六面：玩家视角 → 目标面（整面级开关近似） =====

	/** 玩家视角 → 玩家"看到"的机器表面（世界方向，同主世界 clickedFace 语义）。 */
	private static Direction faceFromLook(Player player) {
		float pitch = player.getXRot();
		if (pitch > 45f)
			return Direction.UP; // 低头俯视 → 看到顶面
		if (pitch < -45f)
			return Direction.DOWN; // 抬头仰视 → 看到底面
		// 水平：看到的是朝向玩家的那面 = 面朝方向的反向
		//（玩家面朝北 → 看到机器朝南的 SOUTH 面）
		return player.getDirection()
			.getOpposite();
	}

	/**
	 * 世界方向 → contraption 本地方向（装配时方向）。
	 * <p>交互时实体必已装配（{@code contraption.entity} 非空）；防御性兜底：实体缺失时退回世界方向。</p>
	 */
	private static Direction toLocalFace(Contraption contraption, Direction worldFace) {
		AbstractContraptionEntity entity = contraption.entity;
		if (entity == null)
			return worldFace;
		Vec3 local = entity.reverseRotation(
			new Vec3(worldFace.getStepX(), worldFace.getStepY(), worldFace.getStepZ()), 1.0f);
		return Direction.getNearest(local.x, local.y, local.z);
	}

	// ===== 八面：contraption 本地精确射线求交 =====

	/** 命中结果：命中面（装配本地 = blockstate 属性基准）与精确命中点（装配本地坐标）。 */
	private record Hit(Direction face, Vec3 point) {
	}

	/**
	 * 玩家射线与目标方块（contraption 本地单位盒 [localPos, localPos+1)³）求交（slab 法）。
	 * 眼位/方向先经 contraption 位姿逆变换到本地，结果面/点均为<b>装配本地</b>坐标。
	 *
	 * @return null = 射线未穿过该方块（防御性；Create 已保证 localPos 命中，正常不会发生）
	 */
	private static Hit rayLocal(Player player, Contraption contraption, BlockPos localPos) {
		AbstractContraptionEntity entity = contraption.entity;
		Vec3 origin = player.getEyePosition(1.0f);
		Vec3 dir = player.getLookAngle();
		if (entity != null) {
			// 世界 → contraption 本地（装配坐标系）
			origin = entity.toLocalVector(origin, 1.0f);
			dir = entity.reverseRotation(dir, 1.0f)
				.normalize();
		}
		return slab(origin, dir, localPos);
	}

	/** slab 法求射线与单位方块盒交点；返回进入面（本地）与命中点（本地坐标）。 */
	private static Hit slab(Vec3 origin, Vec3 dir, BlockPos pos) {
		double minX = pos.getX(), minY = pos.getY(), minZ = pos.getZ();
		double maxX = minX + 1, maxY = minY + 1, maxZ = minZ + 1;
		double tmin = Double.NEGATIVE_INFINITY, tmax = Double.POSITIVE_INFINITY;
		Direction face = null;

		// X 轴
		if (Math.abs(dir.x) < 1e-6) {
			if (origin.x < minX || origin.x > maxX)
				return null;
		} else {
			double t1 = (minX - origin.x) / dir.x, t2 = (maxX - origin.x) / dir.x;
			Direction fNear = dir.x > 0 ? Direction.WEST : Direction.EAST;
			if (t1 > t2) {
				double tmp = t1;
				t1 = t2;
				t2 = tmp;
				fNear = fNear.getOpposite();
			}
			if (t1 > tmin) {
				tmin = t1;
				face = fNear;
			}
			tmax = Math.min(tmax, t2);
		}
		// Y 轴
		if (Math.abs(dir.y) < 1e-6) {
			if (origin.y < minY || origin.y > maxY)
				return null;
		} else {
			double t1 = (minY - origin.y) / dir.y, t2 = (maxY - origin.y) / dir.y;
			Direction fNear = dir.y > 0 ? Direction.DOWN : Direction.UP;
			if (t1 > t2) {
				double tmp = t1;
				t1 = t2;
				t2 = tmp;
				fNear = fNear.getOpposite();
			}
			if (t1 > tmin) {
				tmin = t1;
				face = fNear;
			}
			tmax = Math.min(tmax, t2);
		}
		// Z 轴
		if (Math.abs(dir.z) < 1e-6) {
			if (origin.z < minZ || origin.z > maxZ)
				return null;
		} else {
			double t1 = (minZ - origin.z) / dir.z, t2 = (maxZ - origin.z) / dir.z;
			Direction fNear = dir.z > 0 ? Direction.NORTH : Direction.SOUTH;
			if (t1 > t2) {
				double tmp = t1;
				t1 = t2;
				t2 = tmp;
				fNear = fNear.getOpposite();
			}
			if (t1 > tmin) {
				tmin = t1;
				face = fNear;
			}
			tmax = Math.min(tmax, t2);
		}

		if (tmax < tmin || face == null)
			return null;
		return new Hit(face, origin.add(dir.scale(tmin)));
	}

	/** 注册到 Create 的 contraption 交互注册表（供 AllBlocks onRegister 调用）。 */
	public static MovingInteractionBehaviour instance() {
		return new DisperserMovingInteraction();
	}
}
