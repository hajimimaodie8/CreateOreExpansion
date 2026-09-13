package com.hjmmd_8.createoreexpansion.content.wave.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.Vec3;

/**
 * 能量波机器的「机壳面 / 灯盘面 4 分区点击」判定（<b>全工程唯一实现</b>）。
 *
 * <p>机器正面（FACING 面——差波器叫"机壳面"、星辉波变器叫"灯盘面"）上，按点击位置沿两条对角线
 * 分成 4 个区域，每个区域对应一个<b>世界方向</b>：上 = 北（-Z）、下 = 南（+Z）、
 * 左 = 西（-X）、右 = 东（+X）（"上/下/左/右"是正对该面看过去时的方位）。</p>
 *
 * <p><b>分区口径</b>：</p>
 * <ul>
 *   <li>原点 = <b>方块中心</b>（{@link Vec3#atCenterOf(BlockPos)}），不是点击面的投影点；</li>
 *   <li>只比较相对中心的两个轴的<b>绝对值</b>：绝对值大者所在轴决定分区，
 *       再按该轴分量的正负取方向；比较用严格 {@code >}，两轴绝对值相等时归入后一个分支的轴；</li>
 *   <li>正分量 → 该轴正方向（东 / 南 / 上），负分量 → 该轴负方向（西 / 北 / 下）。
 *       分量恰好为 0 时按"非正"处理（与严格 {@code > 0} 判定一致）。</li>
 * </ul>
 *
 * <p><b>坐标平面由机壳朝向决定</b>（与模型贴面实测校准的结果一致）：朝 Y（正立/倒置）用
 * <b>x/z</b> 平面、朝 X（躺倒，机壳面朝东西）用 <b>y/z</b> 平面、朝 Z（机壳面朝南北）用
 * <b>x/y</b> 平面。</p>
 *
 * <p><b>职责边界</b>：本类只回答"点在哪个世界方向"。该方向能不能交互（例如变器只认水平 4 侧、
 * UP/DOWN 不可开）由各机器自己的开口规则判定；世界方向 ↔ 模型侧面属性的换算见
 * {@link EnergyWaveDisperserBlock#modelFaceOf(Direction, Direction)}。</p>
 *
 * <p><b>为什么独立成类</b>：扳手点差波器机壳面与空手点变器灯盘面，原本各写了一份逐字相同的
 * 分区数学；两机改为共用本实现后，分区规则的任何调整只需改这里一处。</p>
 */
public final class MachineFaceQuadrants {

	private MachineFaceQuadrants() {
	}

	/**
	 * 取点击位置所在分区的世界方向。
	 *
	 * @param facing        机器正面朝向（FACING），决定用哪个坐标平面分区
	 * @param pos           方块位置（取方块中心作为分区原点）
	 * @param clickLocation 玩家点中的精确位置（世界坐标）
	 * @return 该分区代表的世界方向（可能是 UP/DOWN——躺倒放置时机壳面内含有竖直分区的落点）
	 */
	public static Direction worldDirectionAt(Direction facing, BlockPos pos, Vec3 clickLocation) {
		Vec3 rel = clickLocation.subtract(Vec3.atCenterOf(pos));

		if (facing.getAxis() == Axis.Y) {
			// 机壳面朝上/下：用 x/z 偏移，上北（-Z）下南（+Z）左西（-X）右东（+X）
			if (Math.abs(rel.x) > Math.abs(rel.z))
				return rel.x > 0 ? Direction.EAST : Direction.WEST;
			return rel.z > 0 ? Direction.SOUTH : Direction.NORTH;
		}
		if (facing.getAxis() == Axis.X) {
			// 机壳朝东西（躺倒），分区落在 y/z 平面
			if (Math.abs(rel.y) > Math.abs(rel.z))
				return rel.y > 0 ? Direction.UP : Direction.DOWN;
			return rel.z > 0 ? Direction.SOUTH : Direction.NORTH;
		}
		// 机壳朝南北，分区落在 x/y 平面
		if (Math.abs(rel.y) > Math.abs(rel.x))
			return rel.y > 0 ? Direction.UP : Direction.DOWN;
		return rel.x > 0 ? Direction.EAST : Direction.WEST;
	}
}
