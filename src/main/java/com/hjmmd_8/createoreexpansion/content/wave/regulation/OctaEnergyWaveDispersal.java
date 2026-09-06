package com.hjmmd_8.createoreexpansion.content.wave.regulation;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.content.wave.block.OctaCorner;
import com.hjmmd_8.createoreexpansion.content.wave.block.OctaEnergyWaveDifferencerBlock;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 八面能量波差器（Octa Energy Wave Differencer）的纯判定逻辑（无状态、无副作用）。
 *
 * <p><b>几何</b>：机器 8 个口 = 4 个正交口（本地 N/E/S/W，模型大方块侧面中心 6×16 区，
 * 贴 small 纹理）+ 4 个斜口（本地 NE/NW/SE/SW，45° 斜板，两侧 5×16 区，贴 big 纹理）。
 * 顶/底面（本地 U/D）是机壳，不是波入口。</p>
 *
 * <p><b>入口</b>：波沿正交方向轴向飞行命中机器。入口正交面 = 运动反方向的本地正交面；
 * 再按波前相对机器中心在该面内的<b>横向偏移</b>细分，偏移决定入口到底走哪个口：
 * 中心 6px(|u|≤3/16) = 该<b>正交口</b>；两侧各 5px = 相邻<b>斜口</b>
 * （横向正侧 → 该面 rightCorner，负侧 → leftCorner；与扳手交互一致）。
 * 入口口与开口开闭独立：波落在中心区则正交口须开，落在斜口区则斜口须开，
 * 否则撞墙消失（即使另一个口开着也不能从该区进入）。</p>
 *
 * <p><b>规则</b>（按 8 口开口总数）：
 * <ul>
 *   <li>入口口关闭 → 撞墙消失（VANISH）；</li>
 *   <li><b>1 开口</b>：原路遣返（反弹），等级不变；</li>
 *   <li><b>2 开口</b>：从入口进、从另一开口出（转向），等级不变；</li>
 *   <li><b>3~4 开口</b>：其余每个开口各发一个<b>降一级</b>子波（均摊分裂）；</li>
 *   <li><b>5~6 开口</b>：其余每个开口各发一个<b>降二级</b>子波；</li>
 *   <li><b>7~8 开口</b>：其余每个开口各发一个<b>降三级</b>子波；</li>
 *   <li>降级后等级 &le; 0 → 母波湮灭（VANISH）。</li>
 * </ul>
 * 出口为斜口时子波沿 45° 对角方向飞行（{@link #dirOf(OctaCorner)}）。</p>
 *
 * <p><b>坐标系</b>：全部输入输出为<b>机器本地（站姿语义）</b>——开口属性=本地，
 * 斜口方向=本地对角；AXIS 姿态的 world↔local 换算由调用方（{@code WaveMachineActions}/
 * 各碰撞协调类）负责，本类不感知姿态。</p>
 */
public final class OctaEnergyWaveDispersal {

	/** 正交面中心 6px 半宽（3/16 格）：|u| 在此内 = 该正交口，超出 = 相邻斜口。 */
	private static final double ORTHO_HALF = 3.0 / 16.0;

	/** 判定结果（语义同 {@link EnergyWaveDispersal.Result}）。 */
	public enum Result {
		/** 撞墙消失：入口口关闭 / 顶底机壳 / 波级不足分裂。 */
		VANISH,
		/** 单开口：原路遣返（反弹），等级不变。 */
		BOUNCE,
		/** 双开口：从入口进、从另一开口出（转向），等级不变。 */
		TURN,
		/** ≥3 开口：其余每个开口发射降级子波（3-4 降 1、5-6 降 2、7-8 降 3），母波静默消散。 */
		SPLIT;
	}

	/** 入口口：正交口或斜口之一（由波在入口面内的横向偏移决定）。 */
	private static final class Entry {
		final Direction face;   // 入口正交面（波撞上的面）
		final OctaCorner corner; // 入口斜口；null = 入口为正交口
		final boolean ortho;    // true=中心区→正交口；false=斜口区→斜口

		Entry(Direction face, OctaCorner corner, boolean ortho) {
			this.face = face;
			this.corner = corner;
			this.ortho = ortho;
		}
	}

	private OctaEnergyWaveDispersal() {
	}

	/**
	 * 对命中八面差波器的能量波执行一次判定（本地坐标系）。
	 *
	 * @param state     机器方块状态（AXIS + 8 个 open_* 属性；开口属性=本地）
	 * @param movement  波的飞行方向（本地坐标，单位向量，运动方向）
	 * @param relCenter 波前中心相对机器方块中心的偏移（本地坐标）
	 * @param waveLevel 波的当前等级（1~5）
	 * @return 判定结果；出口集合见 {@link #otherOpenDirs(BlockState, Vec3, Vec3)}（含剔除入口口）
	 */
	public static Result handle(BlockState state, Vec3 movement, Vec3 relCenter, int waveLevel) {
		Direction entryFace = entryFaceOf(movement);
		if (entryFace == null)
			return Result.VANISH; // 顶/底机壳：非波入口
		Entry entry = entryAt(entryFace, relCenter);
		if (entry.ortho) {
			if (!OctaEnergyWaveDifferencerBlock.isOpen(state, entryFace))
				return Result.VANISH; // 中心区入口，正交口关闭
		} else {
			if (!OctaEnergyWaveDifferencerBlock.isOpen(state, entry.corner))
				return Result.VANISH; // 斜口区入口，斜口关闭
		}
		int openCount = countOpen(state);
		if (openCount <= 1)
			return Result.BOUNCE;
		if (openCount == 2)
			return Result.TURN;
		int decrement = decrementOf(openCount);
		return waveLevel <= decrement ? Result.VANISH : Result.SPLIT;
	}

	/** 入口正交面（运动反方向的本地正交面）；顶/底机壳 → null。 */
	public static Direction entryFaceOf(Vec3 movement) {
		Direction d = Direction.getNearest(movement.x, movement.y, movement.z);
		if (d.getAxis() == Direction.Axis.Y)
			return null; // 顶/底机壳
		return d.getOpposite();
	}

	/** 波落在入口面内哪个口：按横向偏移分中心区(正交口)/两侧区(斜口)。 */
	private static Entry entryAt(Direction entryFace, Vec3 relCenter) {
		double u = relCenter.dot(Vec3.atLowerCornerOf(rightOf(entryFace)
			.getNormal()));
		if (Math.abs(u) <= ORTHO_HALF)
			return new Entry(entryFace, null, true);
		return new Entry(entryFace, u > 0 ? rightCorner(entryFace) : leftCorner(entryFace), false);
	}

	/**
	 * 除入口外的其余开口方向（本地单位向量，含斜口 45°）。
	 * <ul>
	 *   <li>TURN：唯一出口（另一个开口）；</li>
	 *   <li>SPLIT：所有发射子波的出口。</li>
	 * </ul>
	 */
	public static List<Vec3> otherOpenDirs(BlockState state, Vec3 movement, Vec3 relCenter) {
		List<Vec3> outs = new ArrayList<>(7);
		Direction entryFace = entryFaceOf(movement);
		if (entryFace == null)
			return outs;
		Entry entry = entryAt(entryFace, relCenter);

		for (Direction side : new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
			if (OctaEnergyWaveDifferencerBlock.isOpen(state, side) && !(entry.ortho && side == entryFace))
				outs.add(dirOf(side));
		}
		for (OctaCorner c : OctaCorner.values()) {
			if (OctaEnergyWaveDifferencerBlock.isOpen(state, c) && !(!entry.ortho && c == entry.corner))
				outs.add(dirOf(c));
		}
		return outs;
	}

	/** 分裂降级量：3-4 口降 1、5-6 口降 2、7-8 口降 3。 */
	public static int decrementOf(BlockState state) {
		return decrementOf(countOpen(state));
	}

	private static int decrementOf(int openCount) {
		return openCount >= 7 ? 3 : openCount >= 5 ? 2 : 1;
	}

	/** 8 口开口总数。 */
	private static int countOpen(BlockState state) {
		int count = 0;
		for (Direction side : new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST })
			if (OctaEnergyWaveDifferencerBlock.isOpen(state, side))
				count++;
		for (OctaCorner c : OctaCorner.values())
			if (OctaEnergyWaveDifferencerBlock.isOpen(state, c))
				count++;
		return count;
	}

	/** 口的出射方向（本地单位向量）：正交口轴向、斜口 45° 对角。 */
	public static Vec3 dirOf(Direction side) {
		return Vec3.atLowerCornerOf(side.getNormal());
	}

	public static Vec3 dirOf(OctaCorner corner) {
		Vec3 a = Vec3.atLowerCornerOf(corner.dirA()
			.getNormal());
		Vec3 b = Vec3.atLowerCornerOf(corner.dirB()
			.getNormal());
		return a.add(b)
			.normalize();
	}

	// ===== 相邻斜口表（与 OctaEnergyWaveDifferencerBlock 扳手一致，单源防错） =====

	/** 正交面视角的"右方向"（面内横向正方向）。 */
	public static Direction rightOf(Direction face) {
		return switch (face) {
			case NORTH -> Direction.EAST;
			case EAST -> Direction.SOUTH;
			case SOUTH -> Direction.WEST;
			case WEST -> Direction.NORTH;
			default -> throw new IllegalArgumentException("八面差波器仅水平正交面: " + face);
		};
	}

	/** 正交面右侧相邻斜口。 */
	public static OctaCorner rightCorner(Direction face) {
		return switch (face) {
			case NORTH -> OctaCorner.NORTH_EAST;
			case EAST -> OctaCorner.SOUTH_EAST;
			case SOUTH -> OctaCorner.SOUTH_WEST;
			case WEST -> OctaCorner.NORTH_WEST;
			default -> throw new IllegalArgumentException();
		};
	}

	/** 正交面左侧相邻斜口。 */
	public static OctaCorner leftCorner(Direction face) {
		return switch (face) {
			case NORTH -> OctaCorner.NORTH_WEST;
			case EAST -> OctaCorner.NORTH_EAST;
			case SOUTH -> OctaCorner.SOUTH_EAST;
			case WEST -> OctaCorner.SOUTH_WEST;
			default -> throw new IllegalArgumentException();
		};
	}
}
