package com.hjmmd_8.createoreexpansion.content.wave.regulation;

import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveRegulatorBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveRegulatorBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * 能量调级器（Energy Wave Regulator）的核心判定逻辑：面板通道 + 应力波级调制。
 *
 * <p>方向铁律（务必记住，切勿颠倒）：
 * <ul>
 *   <li>FACING = 齿轮轴方向（齿轮绕 FACING 轴旋转）</li>
 *   <li>顶面板（RECEIVER_TOP）朝向 FACING；底面板（RECEIVER_BOTTOM）朝向 FACING 对面</li>
 *   <li>齿轮的 4 个侧面（垂直于 FACING）没有面板，能量波撞上 = 撞墙消失</li>
 *   <li>波必须沿 FACING 轴方向（面板端）进入/穿出；任何明显的垂直分量都视为撞上齿轮端</li>
 * </ul>
 *
 * <p>面板开关行为（通道判定，先于调制，无应力/有应力都适用）：
 * <pre>
 * 入口面板关闭（单开口的关闭侧 / 两侧关闭）→ 如撞正常方块，波消失
 * 两侧开口 → 通道，波可穿过（调制见下）
 * 仅入口开口 → 波进入后原路遣返
 * </pre>
 *
 * <p>应力波级调制（仅接入应力、齿轮转动时生效；不接入应力则等级永不改变）：
 * 从能量波前进方向看齿轮：
 * <ul>
 *   <li>正转速（speed &gt; 0）= 从 +FACING 看逆时针旋转（JOML 右手定则）</li>
 *   <li>波沿 +FACING 前进时，波前方视线为 -FACING，看到的旋转是镜像：顺时针</li>
 *   <li>增强（等级+1） ⟺ 从波前方看齿轮顺时针 ⟺ movement·FACING 与 speed 同号（dot * speed &gt; 0）</li>
 *   <li>减弱（等级-1） ⟺ 从波前方看齿轮逆时针 ⟺ movement·FACING 与 speed 异号</li>
 * </ul>
 *
 * <p>行为总表（升降级只在接入应力且转速达标时发生，门槛 = FAST，默认 100 RPM）：
 * <pre>
 * 无应力/转速不足 双开口  → 正常通道，等级不变，穿出
 * 无应力/转速不足 单开口  → 等级不变，原路遣返
 * 有应力 顺基准 双开口    → 增强：穿过，延迟 1/(2v) 秒后等级+1
 * 有应力 逆基准 双开口    → 减弱：穿过，立即等级-1
 * 有应力 单开口          → 进入后等级-1，原路遣返
 * 伽马波（3级）顺基准     → 升级无路可升：3 级伽马爆炸，波湮灭
 * 1 级波逆基准 / 1 级波单开口遣返 → 降级无路可降：1 级小范围爆炸，波湮灭
 * 入口面板关闭 / 齿轮端进入 → 撞墙，波消失
 * </pre>
 */
public final class EnergyWaveRegulation {

	/** 判定结果。 */
	public enum Result {
		/** 无应力双开口：等级不变，直接穿过。 */
		PASS_UNCHANGED,
		/** 无应力单开口：等级不变，原路遣返（调用方反转 movement）。 */
		BOUNCE,
		/** 顺基准双开口：穿过，延迟升级（调用方累计飞行距离 0.5 格后等级+1）。 */
		PASS_BOOST_LATER,
		/** 逆基准双开口：穿过，立即降级（等级-1，调用方处理）。 */
		PASS_DOWNGRADE,
		/** 有应力单开口：进入后降级（等级-1），原路遣返（调用方反转 movement）。 */
		BOUNCE_DOWNGRADE,
		/** 消失：齿轮端、入口面板关闭。 */
		VANISH,
		/** 伽马波（3 级）顺基准：升级无路可升 → 触发 3 级伽马爆炸后湮灭（调用方处理）。 */
		VANISH_GAMMA_BOOM,
		/** 1 级波逆基准：降级无路可降 → 触发 1 级小范围爆炸后湮灭（调用方处理）。 */
		VANISH_LOW_BOOM;
	}

	private EnergyWaveRegulation() {
	}

	/**
	 * 对命中调级器的能量波执行一次判定。
	 *
	 * @param be        调级器方块实体（用于取应力/转速与面板状态）
	 * @param movement  波的飞行方向（单位向量）
	 * @param waveLevel 波的当前等级（1=低，2=高，3=伽马）
	 * @return 判定结果；波实体据此决定穿过 / 遣返 / 消失
	 */
	public static Result handle(EnergyWaveRegulatorBlockEntity be, Vec3 movement, int waveLevel) {
		var state = be.getBlockState();
		Direction facing = state.getValue(EnergyWaveRegulatorBlock.FACING);
		boolean topOpen = state.getValue(EnergyWaveRegulatorBlock.RECEIVER_TOP);
		boolean bottomOpen = state.getValue(EnergyWaveRegulatorBlock.RECEIVER_BOTTOM);

		Vec3 facingVec = Vec3.atLowerCornerOf(facing.getNormal());
		double dot = movement.dot(facingVec);

		// 1. 进入端必须是面板端：波必须沿 FACING 轴方向飞行。
		//    任何明显的垂直分量都意味着波撞上了齿轮侧面 → 撞墙消失。
		if (Math.abs(dot) < 0.9d) {
			return Result.VANISH;
		}
		// dot > 0：波沿 +FACING 前进 → 从 x 小处来，先接触 -FACING 侧的面 = 底面板
		//           （底面板朝 FACING 对面）
		// dot < 0：波沿 -FACING 前进 → 先接触 +FACING 侧的面 = 顶面板（朝 FACING）
		boolean fromTop = dot < 0d;

		// 2. 入口面板：关闭则如撞正常方块，波消失（覆盖单开口的关闭侧、两侧关闭）。
		boolean entryOpen = fromTop ? topOpen : bottomOpen;
		if (!entryOpen) {
			return Result.VANISH;
		}

		boolean exitOpen = fromTop ? bottomOpen : topOpen;

		// 3. 双开口：正常通道 / 应力调制。
		if (exitOpen) {
			float speed = be.getSpeed();
			if (!isFastEnough(speed)) {
				// 未接入应力或转速不足（< FAST，默认 100 RPM）：
				// 机器只是普通能量波通道，等级不变（护目镜会显示转速不足提示）。
				return Result.PASS_UNCHANGED;
			}
			// 从波前方看齿轮：
			//   顺时针（增强） ⟺ movement·FACING 与 speed 同号（dot * speed > 0）
			boolean boost = dot * speed > 0d;
			if (boost) {
				// 顺基准：等级+1。伽马波（3 级）升级无路可升 → 3 级伽马爆炸后湮灭。
				return waveLevel >= 3 ? Result.VANISH_GAMMA_BOOM : Result.PASS_BOOST_LATER;
			}
			// 逆基准：等级-1。1 级波降级无路可降 → 1 级小范围爆炸后湮灭。
			return waveLevel <= 1 ? Result.VANISH_LOW_BOOM : Result.PASS_DOWNGRADE;
		}

		// 4. 单开口：
		//    无应力或转速不足（< FAST）：纯通道行为，等级不变，原路遣返；
		//    有应力且达标：进入后等级-1（最低 1 级），原路遣返。
		//    1 级波降级无路可降 → 1 级小范围爆炸后湮灭。
		float speed = be.getSpeed();
		if (!isFastEnough(speed)) {
			return Result.BOUNCE;
		}
		return waveLevel <= 1 ? Result.VANISH_LOW_BOOM : Result.BOUNCE_DOWNGRADE;
	}

	/**
	 * 转速是否达标：|speed| ≥ FAST 门槛（默认 100 RPM，跟随 Create 配置 fastSpeed）。
	 * 与 {@code EnergyWaveRegulatorBlock#getMinimumRequiredSpeedLevel()} 保持一致。
	 */
	private static boolean isFastEnough(float speed) {
		float required = IRotate.SpeedLevel.FAST.getSpeedValue();
		return Math.abs(speed) >= required;
	}
}
