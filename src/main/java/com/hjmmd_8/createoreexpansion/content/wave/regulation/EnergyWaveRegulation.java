package com.hjmmd_8.createoreexpansion.content.wave.regulation;

import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveRegulatorBlockEntity;

import net.minecraft.world.phys.Vec3;

/**
 * 能量调级器（Energy Wave Regulator）的核心判定逻辑：面板通道 + 应力波级调制。
 *
 * <p><b>通道判定</b>（入口/出口面板开关、撞墙、单开口反弹、双开口穿过）与
 * <b>应力调制方向</b>（增强/减弱 = dot*speed 符号）全部继承自
 * {@link AbstractWaveGateRegulation}。</p>
 *
 * <p><b>调制效果（本类实现）</b>——等级调制：</p>
 * <ul>
 *   <li>穿过 + 增强（顺基准）→ 升级：延迟 1/(2v) 秒后等级+1；伽马波（3级）升级无路可升 → 伽马爆炸湮灭；</li>
 *   <li>穿过 + 减弱（逆基准）→ 立即降级；1 级波降级无路可降 → 低级爆炸湮灭；</li>
 *   <li>反弹 + 调制 → 降级并原路遣返（1 级波走低级爆炸）。</li>
 * </ul>
 *
 * <p>行为总表（升降级只在接入应力且转速达标时发生，门槛 = FAST，默认 100 RPM）：
 * <pre>
 * 无应力/转速不足 双开口  → 正常通道，等级不变，穿出（PASS_UNCHANGED）
 * 无应力/转速不足 单开口  → 等级不变，原路遣返（BOUNCE）
 * 有应力 顺基准 双开口    → 增强：穿过，延迟 1/(2v) 秒后等级+1（PASS_BOOST_LATER）
 * 有应力 逆基准 双开口    → 减弱：穿过，立即等级-1（PASS_DOWNGRADE）
 * 有应力 单开口          → 进入后等级-1，原路遣返（BOUNCE_DOWNGRADE）
 * 伽马波（3级）顺基准     → 升级无路可升：3 级伽马爆炸，波湮灭（VANISH_GAMMA_BOOM）
 * 1 级波逆基准 / 1 级波单开口遣返 → 降级无路可降：1 级小范围爆炸，波湮灭（VANISH_LOW_BOOM）
 * 入口面板关闭 / 齿轮端进入 → 撞墙，波消失（VANISH）
 * </pre>
 */
public final class EnergyWaveRegulation extends AbstractWaveGateRegulation {

	/** 判定结果（语义保持与旧版一致，供波实体 switch）。 */
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

	private static final EnergyWaveRegulation INSTANCE = new EnergyWaveRegulation();

	private EnergyWaveRegulation() {
	}

	/** 单例入口（判定无状态）。 */
	public static EnergyWaveRegulation get() {
		return INSTANCE;
	}

	/**
	 * 对命中调级器的能量波执行一次判定，返回调级器语义的结果枚举。
	 *
	 * <p>通道/方向判定复用基类 {@link #handle(AbstractWaveGateBlockEntity, Vec3, Vec3)}，
	 * 此处仅把结构化结果（channel + modulate + boost）转换为调级器的 8 值枚举，
	 * 保持旧版行为完全一致。</p>
	 *
	 * @param be        调级器方块实体（用于取应力/转速与面板状态）
	 * @param wavePos   波前中心（世界坐标，波实体碰撞盒中心）：入口中心 4×4 区域判定
	 * @param movement  波的飞行方向（单位向量）
	 * @param waveLevel 波的当前等级（1=低，2=高，3=伽马）
	 * @return 判定结果；波实体据此决定穿过 / 遣返 / 消失
	 */
	public Result resolve(EnergyWaveRegulatorBlockEntity be, Vec3 wavePos, Vec3 movement, int waveLevel) {
		AbstractWaveGateRegulation.Result r = handle(be, wavePos, movement);
		switch (r.channel()) {
			case VANISH -> {
				return Result.VANISH;
			}
			case BOUNCE -> {
				if (!r.modulate())
					return Result.BOUNCE;
				// 有应力单开口：降级并遣返（1 级波降级无路可降 → 低级爆炸）
				return waveLevel <= 1 ? Result.VANISH_LOW_BOOM : Result.BOUNCE_DOWNGRADE;
			}
			case PASS -> {
				if (!r.modulate())
					return Result.PASS_UNCHANGED;
				if (r.boost()) {
					// 顺基准：等级+1。伽马波（3 级）升级无路可升 → 伽马爆炸。
					return waveLevel >= 3 ? Result.VANISH_GAMMA_BOOM : Result.PASS_BOOST_LATER;
				}
				// 逆基准：等级-1。1 级波降级无路可降 → 低级爆炸。
				return waveLevel <= 1 ? Result.VANISH_LOW_BOOM : Result.PASS_DOWNGRADE;
			}
		}
		return Result.VANISH;
	}
}
