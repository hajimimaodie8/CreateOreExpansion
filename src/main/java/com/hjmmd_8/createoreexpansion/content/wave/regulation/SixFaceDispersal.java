package com.hjmmd_8.createoreexpansion.content.wave.regulation;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.content.wave.block.SixFaceDisperserBlock;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 六面能量波差器（Six-Face Energy Wave Disperser）的纯判定逻辑（无状态、无副作用）。
 *
 * <p>规则（按 6 面开口总数，开口属性即世界方向，无 FACING 旋转）：</p>
 * <ul>
 *   <li><b>入口</b>：波沿某面法线进入（{@code movement} 反方向即入口世界方向）；
 *       入口开口关闭 → 撞墙消失（同四面差波器）；</li>
 *   <li><b>1 个开口</b>：原路遣返（反弹），等级不变；</li>
 *   <li><b>2 个开口</b>：从入口进、从另一个开口出（拐弯），等级不变；</li>
 *   <li><b>3 或 4 个开口</b>：其余每个开口各发一个<b>降一级</b>的波（均摊分裂）；
 *       1 级波降级无路可降而湮灭；</li>
 *   <li><b>5 或 6 个开口（新增）</b>：其余每个开口各发一个<b>降二级</b>的波；
 *       1/2 级波降二级无路可降而湮灭。</li>
 * </ul>
 */
public final class SixFaceDispersal {

	/** 判定结果（语义同 {@link EnergyWaveDispersal.Result}）。 */
	public enum Result {
		/** 撞墙消失：入口关闭 / 波级不足分裂。 */
		VANISH,
		/** 单开口：原路遣返（反弹），等级不变。 */
		BOUNCE,
		/** 双开口：从入口进、从另一开口出（拐弯），等级不变。 */
		TURN,
		/** ≥3 开口：其余每个开口发射子波（3-4 开口降一级、5-6 开口降二级），母波静默消散。 */
		SPLIT;
	}

	private SixFaceDispersal() {
	}

	/**
	 * 对命中六面差器的能量波执行一次判定。
	 *
	 * @param state     方块状态（6 个 open_* 属性）
	 * @param movement  波的飞行方向（单位向量，运动方向）
	 * @param waveLevel 波的当前等级（1=低，2=高，3=伽马）
	 * @return 判定结果；TURN/SPLIT 的出口面列表见 {@link #exitsOf(BlockState, Vec3)}
	 */
	public static Result handle(BlockState state, Vec3 movement, int waveLevel) {
		Direction entry = entryOf(movement);
		if (!SixFaceDisperserBlock.isOpen(state, entry))
			return Result.VANISH; // 入口开口关闭 → 撞墙消失

		int openCount = countOpen(state);
		if (openCount <= 1)
			return Result.BOUNCE;
		if (openCount == 2)
			return Result.TURN;
		// 3-4 开口：降一级；5-6 开口：降二级
		int decrement = openCount >= 5 ? 2 : 1;
		return waveLevel <= decrement ? Result.VANISH : Result.SPLIT;
	}

	/**
	 * 非入口的其余开口（世界方向列表）：
	 * <ul>
	 *   <li>{@link Result#TURN}：唯一出口（拐弯目标）；</li>
	 *   <li>{@link Result#SPLIT}：所有发射子波的出口（均摊分裂）。</li>
	 * </ul>
	 *
	 * @return 非入口开口的世界方向列表
	 */
	public static List<Direction> exitsOf(BlockState state, Vec3 movement) {
		Direction entry = entryOf(movement);
		List<Direction> others = new ArrayList<>(5);
		for (Direction side : Direction.values()) {
			if (SixFaceDisperserBlock.isOpen(state, side) && side != entry)
				others.add(side);
		}
		return others;
	}

	/** 分裂降级量：3-4 开口降一级、5-6 开口降二级。 */
	public static int decrementOf(BlockState state) {
		return countOpen(state) >= 5 ? 2 : 1;
	}

	/** 波的入口世界方向：运动方向反推（同四面差波器，无 FACING 映射）。 */
	private static Direction entryOf(Vec3 movement) {
		return Direction.getNearest(movement.x, movement.y, movement.z)
			.getOpposite();
	}

	/** 统计 6 面开口总数。 */
	private static int countOpen(BlockState state) {
		int count = 0;
		for (Direction side : Direction.values()) {
			if (SixFaceDisperserBlock.isOpen(state, side))
				count++;
		}
		return count;
	}
}
