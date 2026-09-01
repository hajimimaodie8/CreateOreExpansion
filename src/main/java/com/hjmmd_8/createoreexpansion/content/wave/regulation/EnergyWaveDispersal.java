package com.hjmmd_8.createoreexpansion.content.wave.regulation;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 能量波差器（Energy Wave Disperser）的纯判定逻辑（无状态、无副作用）。
 *
 * <p>规则（按 4 侧面开口总数，开口属性为模型坐标系）：</p>
 * <ul>
 *   <li><b>入口</b>：波沿某侧面法线进入。波的 {@code movement} 是<b>运动方向</b>（指向飞行前方），
 *       波进入差器时接触的面是<b>运动反方向</b>那一侧（如向北飞则从南面进入）；</li>
 *   <li>入口方向为机壳面（顶/底）或入口开口关闭 → 撞墙消失；</li>
 *   <li><b>1 个开口</b>：原路遣返（反弹），等级不变；</li>
 *   <li><b>2 个开口</b>：从入口进、从另一个开口出（拐弯），等级不变；</li>
 *   <li><b>3 或 4 个开口</b>：其余每个开口各发一个<b>降一级</b>的波（均摊分裂）；
 *       1 级波降级无路可降而湮灭。</li>
 * </ul>
 */
public final class EnergyWaveDispersal {

	/** 判定结果。 */
	public enum Result {
		/** 撞墙消失：机壳面 / 入口关闭 / 1 级波分裂（调用方 burst + discard）。 */
		VANISH,
		/** 单开口：原路遣返（反弹），等级不变（调用方反转 movement）。 */
		BOUNCE,
		/** 双开口：从入口进、从另一开口出（拐弯），等级不变（调用方改用出口方向飞行）。 */
		TURN,
		/** ≥3 开口：其余每个开口发射降一级子波，母波静默消散（调用方负责发射与消散）。 */
		SPLIT;
	}

	private EnergyWaveDispersal() {
	}

	/**
	 * 对命中差器的能量波执行一次判定。
	 *
	 * @param state     差器方块状态（FACING + 4 开口属性）
	 * @param movement  波的飞行方向（单位向量，运动方向；与 state 同坐标系）
	 * @param wavePos   波前中心（与 state 同坐标系：主世界=世界坐标，结构=本地坐标，
	 *                  用于 4×4 入口中心判定）
	 * @param pos       差器方块位置（与 state 同坐标系）
	 * @param waveLevel 波的当前等级（1=低，2=高，3=伽马）
	 * @return 判定结果；TURN/SPLIT 的出口模型面列表见 {@link #exitsOf(BlockState, Vec3)}
	 */
	public static Result handle(BlockState state, Vec3 movement, Vec3 wavePos, BlockPos pos, int waveLevel) {
		Direction entry = entryOf(state, movement);
		if (entry == null)
			return Result.VANISH; // 撞机壳面（模型顶/底无开口）
		if (!EnergyWaveDisperserBlock.isOpen(state, entry))
			return Result.VANISH; // 入口开口关闭
		// 4×4 入口中心判定：波前中心相对差波器中心在入口面内的偏移超限（斜射/偏移射擦边）
		// → 如撞正常方块，波消失（与波闸的 ENTRY_CENTER_HALF 一致）
		if (!inEntryCenter(movement, wavePos, pos))
			return Result.VANISH;

		int openCount = countOpen(state);
		if (openCount <= 1)
			return Result.BOUNCE;
		if (openCount == 2)
			return Result.TURN;
		return waveLevel <= 1 ? Result.VANISH : Result.SPLIT;
	}

	/**
	 * 4×4 入口中心判定：波前中心相对差波器方块中心，在入口面（运动反方向侧面）内的
	 * 面内偏移（|轴1|、|轴2|）任一超过 {@link AbstractWaveGateRegulation#ENTRY_CENTER_HALF}
	 * → 视为斜射/偏移射擦边，不触发入口（撞墙消失）。
	 */
	private static boolean inEntryCenter(Vec3 movement, Vec3 wavePos, BlockPos pos) {
		Direction entry = Direction.getNearest(movement.x, movement.y, movement.z)
			.getOpposite();
		Vec3 center = Vec3.atCenterOf(pos);
		Vec3 rel = wavePos.subtract(center);
		Vec3 a1;
		Vec3 a2;
		if (entry.getAxis().isHorizontal()) {
			a1 = Vec3.atLowerCornerOf(entry.getClockWise()
				.getNormal());
			a2 = Vec3.atLowerCornerOf(Direction.UP.getNormal());
		} else {
			a1 = Vec3.atLowerCornerOf(Direction.NORTH.getNormal());
			a2 = Vec3.atLowerCornerOf(Direction.EAST.getNormal());
		}
		double half = AbstractWaveGateRegulation.ENTRY_CENTER_HALF;
		return Math.abs(rel.dot(a1)) <= half && Math.abs(rel.dot(a2)) <= half;
	}

	/**
	 * 非入口的其余开口（模型面列表）：
	 * <ul>
	 *   <li>{@link Result#TURN}：唯一出口（拐弯目标）；</li>
	 *   <li>{@link Result#SPLIT}：所有发射子波的出口（均摊分裂）。</li>
	 * </ul>
	 *
	 * @param state    差器方块状态
	 * @param movement 波的飞行方向（用于反推入口）
	 * @return 非入口开口的模型面列表
	 */
	public static List<Direction> exitsOf(BlockState state, Vec3 movement) {
		Direction entry = entryOf(state, movement);
		if (entry == null)
			return List.of();

		List<Direction> others = new ArrayList<>(4);
		for (Direction side : new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
			if (EnergyWaveDisperserBlock.isOpen(state, side) && side != entry)
				others.add(side);
		}
		return others;
	}

	/**
	 * 波的入口模型面：运动方向反推世界入口方向，再映射到模型面。
	 *
	 * @return 模型侧面（NORTH/EAST/SOUTH/WEST）；撞机壳面（顶/底）时返回 null
	 */
	private static Direction entryOf(BlockState state, Vec3 movement) {
		Direction facing = state.getValue(EnergyWaveDisperserBlock.FACING);
		Direction entryWorld = Direction.getNearest(movement.x, movement.y, movement.z)
			.getOpposite();
		return EnergyWaveDisperserBlock.modelFaceOf(facing, entryWorld);
	}

	/** 统计 4 个侧面开口总数。 */
	private static int countOpen(BlockState state) {
		int count = 0;
		for (Direction side : new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
			if (EnergyWaveDisperserBlock.isOpen(state, side))
				count++;
		}
		return count;
	}
}
