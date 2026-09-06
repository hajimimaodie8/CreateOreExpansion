package com.hjmmd_8.createoreexpansion.content.wave.regulation;

import com.hjmmd_8.createoreexpansion.content.wave.WaveLevels;
import com.hjmmd_8.createoreexpansion.content.wave.block.AbstractWaveGateBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.SapphireWaveRegulatorBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
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
	 * <p>通道/方向判定复用基类 {@link #handle(BlockState, BlockPos, float, Vec3, Vec3)}，
	 * 此处仅把结构化结果（channel + modulate + boost）转换为调级器的 8 值枚举，
	 * 保持旧版行为完全一致。</p>
	 *
	 * <p><b>机型参数化</b>：门槛转速（翡翠 FAST 100 / 蓝宝石 64）与最大可升等级
	 * （翡翠 3 / 蓝宝石 5）由 BE 机型参数（{@link AbstractWaveGateBlockEntity}）提供。</p>
	 *
	 * @param be        调级器方块实体（翡翠或蓝宝石，用于取应力/转速、面板状态与机型参数）
	 * @param wavePos   波前中心（与 BE 同坐标系）：入口中心 4×4 区域判定
	 * @param movement  波的飞行方向（单位向量，与 BE 同坐标系）
	 * @param waveLevel 波的当前等级（1=低，2=高，3=伽马，4=伊普西龙，5=欧米伽）
	 * @return 判定结果；波实体据此决定穿过 / 遣返 / 消失
	 */
	public Result resolve(AbstractWaveGateBlockEntity be, Vec3 wavePos, Vec3 movement, int waveLevel) {
		AbstractWaveGateRegulation.Result r = handle(be.getBlockState(), be.getBlockPos(), be.getSpeed(),
			be.getModulationSpeedThreshold(), wavePos, movement);
		return toResult(r, waveLevel, be.getMaxSupportedWaveLevel());
	}

	/**
	 * 纯方块状态判定（Create 动态结构 contraption / 无 BE 实例场景）：
	 * 无应力/转速 → 仅通道判定，不调制。机型承载上限由方块类型判断
	 * （蓝宝石 5 / 翡翠 3，4/5 级波限制仅翡翠机型有）。
	 *
	 * @param state     波闸方块状态（面板开闭/朝向）
	 * @param pos       波闸方块位置（与 wavePos 同坐标系）
	 * @param wavePos   波前中心（与 pos 同坐标系）
	 * @param movement  波的飞行方向（单位向量，与 state 同坐标系）
	 * @param waveLevel 波的当前等级
	 */
	public Result resolve(BlockState state, BlockPos pos, Vec3 wavePos, Vec3 movement, int waveLevel) {
		AbstractWaveGateRegulation.Result r = handle(state, pos, 0f, wavePos, movement);
		return toResult(r, waveLevel, maxSupportedOf(state));
	}

	/** 由方块类型判断机型承载上限：蓝宝石 5，其余（翡翠）3。 */
	private static int maxSupportedOf(BlockState state) {
		return state.getBlock() instanceof SapphireWaveRegulatorBlock
			? WaveLevels.SAPPHIRE_MAX
			: WaveLevels.JADE_MAX;
	}

	/**
	 * 结构化结果 → 调级器 8 值枚举（含等级边缘爆炸语义与机型承载上限限制）。
	 *
	 * <p><b>机型承载上限限制</b>（翡翠 maxSupported=3）：输入波为 4/5 级（蓝宝石专属）
	 * 时，翡翠调级器<b>不允许输出/维持 4/5 级波</b>——只有"降级到 ≤3"的操作有效
	 * （4→3 可以；5→4 后仍 &gt;3，也不行）；其它（原样穿过/反弹/升级）一律操作无效 → 波湮灭。
	 * 蓝宝石机型（maxSupported=5）不受此限制。判定入口关闭等 VANISH 情形照旧湮灭。</p>
	 */
	private Result toResult(AbstractWaveGateRegulation.Result r, int waveLevel, int maxLevel) {
		// 机型承载上限预检：波级超出机型上限（翡翠机遇 4/5 级波）
		if (waveLevel > maxLevel) {
			// 仅降级方向且降级后 ≤ 上限才有效（4→3；5→4 仍超上限 → 无效）
			boolean downToLimit = r.modulate() && !r.boost() && waveLevel - 1 <= maxLevel;
			if (!downToLimit)
				return Result.VANISH; // 4/5 级波在翡翠机上：非降级操作一律无效湮灭
			// 放行降级：按通道动作分发
			return r.channel() == AbstractWaveGateRegulation.Channel.PASS
				? Result.PASS_DOWNGRADE
				: Result.BOUNCE_DOWNGRADE;
		}
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
					// 顺基准：等级+1。达到机型上限（翡翠 3 伽马；蓝宝石 5 欧米伽）→ 过载爆炸湮灭。
					return waveLevel >= maxLevel ? Result.VANISH_GAMMA_BOOM : Result.PASS_BOOST_LATER;
				}
				// 逆基准：等级-1。1 级波降级无路可降 → 低级爆炸。
				return waveLevel <= 1 ? Result.VANISH_LOW_BOOM : Result.PASS_DOWNGRADE;
			}
		}
		return Result.VANISH;
	}
}
