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
 * <p><b>定位口径（2026-09 重算，取代原先按"南北机/东西机"分类的手调符号）</b>：
 * 模型（{@code *_stress_charger_*.json}）里两个 6×6 标记面板在模型空间位于
 * {@code x=1..2} 与 {@code x=14..15}、{@code y=3..9}、{@code z=5..11}，即标记中心相对面中心
 * <b>横向偏 2px</b>（竖直放置时纵向偏 −1.5px 由 {@link #VERT_Y} 处理）。所以取值框该落在哪，
 * 完全由"<b>FACING 的正方向</b> + <b>看向的侧面</b>"决定，推导结果如下（8 种组合全部落在标记中心）：</p>
 * <pre>
 * 横向偏移(px) = lateralMark × (侧面 ∈ {SOUTH, WEST} ? +1 : −1)
 * lateralMark  = (FACING ∈ {SOUTH, EAST}) ? −2 : +2      // 模型旋转把标记甩向哪一侧
 *
 * FACING=NORTH：EAST −2 / WEST +2      FACING=SOUTH：EAST +2 / WEST −2
 * FACING=EAST ：NORTH +2 / SOUTH −2    FACING=WEST ：NORTH −2 / SOUTH +2
 * </pre>
 *
 * <p><b>旧实现为什么偏</b>：它只用"侧面"决定符号（{@code side==WEST||side==NORTH} → 取负），
 * 没把 FACING 的正方向算进去——于是 <b>FACING=SOUTH 与 FACING=EAST 的机器上，取值框会被镜像到
 * 标记的另一侧（共差 4px）</b>，而 FACING=NORTH / WEST 恰好是对的（当年的手调就是在
 * 这两种朝向下收敛的）。现在所有朝向/侧面都由同一条公式给出，不再需要逐朝向试错常量。</p>
 */
public class SapphireChargerModeSlot extends ValueBoxTransform.Sided {

	// ============ 模板基准（SOUTH 模板，像素 0..16） ============
	/** 横向基准：标记中心相对面中心的偏移量（像素；<b>符号由朝向与侧面共同决定</b>，见类注释）。 */
	private static final float PANEL_LATERAL = 2f;
	/** 纵向基准（模板 y 像素；<b>数值减小 = 向下</b>）。 */
	private static final float PANEL_Y = 7.5f;
	/** 深度：贴面深度（14.5 = 贴合表面内侧，勿动）。 */
	private static final float PANEL_DEPTH = 14.5f;

	// ============ 竖直放置（FACING 轴 Y：标记面板在东西两面） ============
	/** 竖直机带标记的两面所在轴（Axis.X = 东/西）。 */
	private static final Axis VERT_PANEL_AXIS = Axis.X;
	/** 竖直纵向：标记中心 y=6px（模型 y=3..9）→ 相对基准向下 1.5px。 */
	private static final float VERT_Y = PANEL_Y - 1.5f;
	/** 竖直横向：标记在面中心，横向偏移 0。 */
	private static final float VERT_LATERAL = 0f;

	// ============ 水平放置（南北/东西） ============
	/** 水平统一：标记中心 y=8px（模型 z=5..11 映射到世界 y）→ 相对基准向下 0.5px。 */
	private static final float HORIZ_DOWN = 0.5f;

	@Override
	public Vec3 getLocalOffset(net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos,
		BlockState state) {
		Direction side = getSide();
		Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
		float horizontalAngle = AngleHelper.horizontalAngle(side);
		float xOff;
		float yOff;
		if (facing.getAxis() == Axis.Y) {
			// 竖直：东西两面，标记居中
			xOff = VERT_LATERAL;
			yOff = VERT_Y;
		} else {
			// 水平：标记横向偏 2px，偏向哪一侧由 FACING 的正方向决定（南/东 → 标记偏 −2）
			float lateralMark = (facing == Direction.SOUTH || facing == Direction.EAST)
				? -PANEL_LATERAL
				: PANEL_LATERAL;
			// SOUTH 模板横向在旋转到各侧面时的符号：南/西同号，北/东反号
			xOff = (side == Direction.SOUTH || side == Direction.WEST) ? lateralMark : -lateralMark;
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
