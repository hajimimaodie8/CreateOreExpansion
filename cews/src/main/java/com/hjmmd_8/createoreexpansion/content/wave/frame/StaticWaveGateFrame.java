package com.hjmmd_8.createoreexpansion.content.wave.frame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * 静态方块坐标系解析器（实现深度 A：轻量 FACING 推导）。
 *
 * <p>适用于固定在世界中的波闸机器（能量调级器 / 波速调节器等）：blockstate 六向 FACING
 * 旋转，机器本体不移动、不旋转。本地坐标由 FACING 直接推导：</p>
 * <ul>
 *   <li>本地 Z = FACING 法向量（面板轴）；</li>
 *   <li>FACING 水平时：本地 X = FACING 顺时针旋转 90°（水平垂直轴），本地 Y = UP；</li>
 *   <li>FACING 竖直时：本地 X = NORTH，本地 Y = EAST（水平面内正交轴）。</li>
 * </ul>
 *
 * <p>原点 = 方块中心 {@link Vec3#atCenterOf(BlockPos)}。</p>
 */
public class StaticWaveGateFrame implements WaveGateFrame {

	private final Vec3 origin;
	private final Vec3 basisX;
	private final Vec3 basisY;
	private final Vec3 basisZ;

	public StaticWaveGateFrame(BlockPos pos, Direction facing) {
		this.origin = Vec3.atCenterOf(pos);
		this.basisZ = Vec3.atLowerCornerOf(facing.getNormal());
		if (facing.getAxis().isHorizontal()) {
			// 竖直面板（面朝水平方向）：面内水平轴 = FACING 顺时针 90°，面内竖直轴 = UP
			this.basisX = Vec3.atLowerCornerOf(facing.getClockWise()
				.getNormal());
			this.basisY = Vec3.atLowerCornerOf(Direction.UP.getNormal());
		} else {
			// 水平面板（FACING 为 UP/DOWN）：面内两个水平正交轴
			this.basisX = Vec3.atLowerCornerOf(Direction.NORTH.getNormal());
			this.basisY = Vec3.atLowerCornerOf(Direction.EAST.getNormal());
		}
	}

	@Override
	public Vec3 panelAxis() {
		return basisZ;
	}

	@Override
	public Vec3 origin() {
		return origin;
	}

	@Override
	public Vec3 basisX() {
		return basisX;
	}

	@Override
	public Vec3 basisY() {
		return basisY;
	}

	@Override
	public Vec3 basisZ() {
		return basisZ;
	}
}
