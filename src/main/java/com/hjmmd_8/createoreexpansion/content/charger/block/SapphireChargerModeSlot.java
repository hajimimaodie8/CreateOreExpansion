package com.hjmmd_8.createoreexpansion.content.charger.block;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 蓝宝石应力充能器模式槽（仿黄铜隧道/智能溜槽的 ValueBox 交互）。
 *
 * <p>定位经验（用户 2026-09 实测收敛）：本工程模板坐标里 <b>y 数值减小 = 视觉向下</b>、
 * 数值增大 = 向上；横向由两相对面镜像处理。竖直（东西两面带 6×6 标记）已调准；
 * 水平按 FACING 分南北/东西各加小量。全部常量在文件顶部，可自行微调。</p>
 */
public class SapphireChargerModeSlot extends ValueBoxTransform.Sided {

	// ============ 模板基准（SOUTH 模板，像素 0..16） ============
	/** 横向基准：面板相对被点击面中心的偏移（像素；两相对面镜像取反）。 */
	private static final float PANEL_LATERAL = 2f;
	/** 纵向基准（模板 y 像素；<b>数值减小 = 向下</b>）。 */
	private static final float PANEL_Y = 7.5f;
	/** 深度：贴面深度（14.5 = 贴合表面内侧，勿动）。 */
	private static final float PANEL_DEPTH = 14.5f;

	// ============ 竖直放置（东西两面带 6×6 标记，已调准） ============
	/** 竖直机带标记的两面所在轴（Axis.X = 东/西）。 */
	private static final Axis VERT_PANEL_AXIS = Axis.X;
	/** 竖直纵向：相对基准 PANEL_Y 向下 1.5px（已由玩家确认正常）。 */
	private static final float VERT_Y = PANEL_Y - 1.5f;
	/** 竖直横向（玩家确认正常后保留；方向反了取负）。 */
	private static final float VERT_LATERAL = PANEL_LATERAL - 2f;

	// ============ 水平放置（南北/东西） ============
	/** 水平统一：相对基准偏移量 0.5px（原 −0.5 反号 → +0.5）。 */
	private static final float HORIZ_DOWN = 0.5f;
	/** 南北机（口朝南/北）额外：远离轴 4px（原符号已反 → 两面对调）。方向再反就取负。 */
	private static final float NS_AWAY = - 4f;

	@Override
	public Vec3 getLocalOffset(net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos,
		BlockState state) {
		Direction side = getSide();
		Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
		float horizontalAngle = AngleHelper.horizontalAngle(side);
		boolean mirroredSide = side == Direction.WEST || side == Direction.NORTH;
		float xOff;
		float yOff;
		if (facing.getAxis() == Axis.Y) {
			// 竖直：东西两面
			xOff = VERT_LATERAL; // 两面同号（玩家已验证方向）
			yOff = VERT_Y;
		} else if (facing.getAxis() == Axis.Z) {
			// 南北机：远离轴 4px（玩家实测：上一版反了 → 符号翻回）+ 统一偏移 0.5
			xOff = mirroredSide ? -(PANEL_LATERAL + NS_AWAY) : (PANEL_LATERAL + NS_AWAY);
			yOff = PANEL_Y + HORIZ_DOWN;
		} else {
			// 东西机：仅统一偏移 0.5
			xOff = mirroredSide ? -PANEL_LATERAL : PANEL_LATERAL;
			yOff = PANEL_Y + HORIZ_DOWN;
		}
		Vec3 southLocation = VecHelper.voxelSpace(8f + xOff, yOff, PANEL_DEPTH);
		return VecHelper.rotateCentered(southLocation, horizontalAngle, Axis.Y);
	}

	@Override
	protected boolean isSideActive(BlockState state, Direction direction) {
		if (!direction.getAxis()
			.isHorizontal())
			return false;
		Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
		if (facing.getAxis() == Axis.Y)
			// 竖直：只保留带 6×6 标记的两个相对侧面
			return direction.getAxis() == VERT_PANEL_AXIS;
		// 水平：只左右两侧（与 FACING 垂直的水平向）
		return direction.getAxis() != facing.getAxis();
	}

	@Override
	protected Vec3 getSouthLocation() {
		return Vec3.ZERO;
	}

}
