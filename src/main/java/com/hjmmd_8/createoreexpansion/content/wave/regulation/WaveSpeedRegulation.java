package com.hjmmd_8.createoreexpansion.content.wave.regulation;

import com.hjmmd_8.createoreexpansion.content.wave.block.AbstractWaveGateBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.block.SapphireSpeedRegulatorBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 波速调节器（Wave Speed Regulator）的核心判定逻辑：面板通道 + 应力速度调制。
 *
 * <p><b>通道判定</b>（入口/出口面板开关、撞墙、单开口反弹、双开口穿过）与
 * <b>应力调制方向</b>（增强/减弱 = dot*speed 符号）全部继承自
 * {@link AbstractWaveGateRegulation}。</p>
 *
 * <p><b>调制效果（本类实现）</b>——速度调制：</p>
 * <ul>
 *   <li><b>调制度由接入转速决定</b>：{@code 100 ~ 256 RPM} 区间平均分 4 档，
 *       |speed| 落在第 1~4 档分别加速 {@code +0.5 / +1 / +1.5 / +2} 格每秒
 *       （减速对称：{@code -0.5 / -1 / -1.5 / -2}）；</li>
 *   <li>穿过 + 增强（顺基准）→ 加速；穿过 + 减弱（逆基准）→ 减速；</li>
 *   <li><b>反弹（单开口）不改速度</b>——仅调级器在反弹时降级，波速调节器反弹纯折返；</li>
 *   <li>速度修正值以<b>叠加</b>方式累积到波的 {@code speedOffset}（见波实体），
 *       多次通过可反复叠加，且分裂/拐弯后保持（子波继承同一修正值）。</li>
 * </ul>
 */
public final class WaveSpeedRegulation extends AbstractWaveGateRegulation {

	/** 判定结果（供波实体 switch）。 */
	public enum Result {
		/** 无应力双开口：速度不变，直接穿过。 */
		PASS_UNCHANGED,
		/** 单开口：速度不变，原路遣返（反弹不改速）。 */
		BOUNCE,
		/** 顺基准双开口：穿过，加速（调制度按转速分档）。 */
		PASS_SPEED_UP,
		/** 逆基准双开口：穿过，减速（调制度按转速分档）。 */
		PASS_SPEED_DOWN,
		/** 消失：齿轮端、入口面板关闭。 */
		VANISH;
	}

	/** 分档转速区间下界：翡翠 100 RPM（= FAST 门槛，转速达标才调制）；蓝宝石 64（由机型参数提供）。 */
	private static final float TIER_BASE = 100f;
	/** 分档转速区间上界：256 RPM。 */
	private static final float TIER_MAX = 256f;
	/** 档位步长：每档加速量（格/秒）。翡翠 4 档 = 0.5 / 1 / 1.5 / 2；蓝宝石 6 档 0.5~3。 */
	private static final float STEP = 0.5f;
	/** 档位数：翡翠 4 档；蓝宝石 6 档（机型参数提供）。 */
	private static final int TIER_COUNT = 4;

	private static final WaveSpeedRegulation INSTANCE = new WaveSpeedRegulation();

	private WaveSpeedRegulation() {
	}

	/** 单例入口（判定无状态）。 */
	public static WaveSpeedRegulation get() {
		return INSTANCE;
	}

	/**
	 * 对命中波速调节器的能量波执行一次判定，返回波速调节器语义的结果枚举。
	 *
	 * <p>通道/方向判定复用基类，此处把结构化结果转换为 5 值枚举。
	 * <b>反弹（BOUNCE）不改变速度</b>——无论是否调制，单开口都只是纯折返。</p>
	 *
	 * <p><b>机型参数化</b>：门槛转速与分档（翡翠 100/4 档；蓝宝石 64/6 档）由
	 * BE 机型参数（{@link AbstractWaveGateBlockEntity}）提供。</p>
	 *
	 * <p><b>机型承载上限限制</b>（翡翠 maxSupported=3）：4/5 级波（蓝宝石专属）进入
	 * 翡翠波速调节器时，因调速不改变波等级，输出必然仍为 4/5 级 → 本次操作无效、波湮灭
	 * （蓝宝石机型 maxSupported=5 不受限）。</p>
	 *
	 * @param be        波速调节器方块实体（翡翠或蓝宝石，用于取应力/转速、面板状态与机型参数）
	 * @param wavePos   波前中心（与 BE 同坐标系）：入口中心 4×4 区域判定
	 * @param movement  波的飞行方向（单位向量，与 BE 同坐标系）
	 * @param waveLevel 波的当前等级（1~5）
	 * @return 判定结果；波实体据此决定穿过 / 遣返 / 消失
	 */
	public Result resolve(AbstractWaveGateBlockEntity be, Vec3 wavePos, Vec3 movement, int waveLevel) {
		if (waveLevel > be.getMaxSupportedWaveLevel())
			return Result.VANISH; // 翡翠机承载上限 3：4/5 级波调速必然维持 4/5 → 无效湮灭
		AbstractWaveGateRegulation.Result r = handle(be.getBlockState(), be.getBlockPos(), be.getSpeed(),
			be.getModulationSpeedThreshold(), wavePos, movement);
		return toResult(r);
	}

	/**
	 * 纯方块状态判定（Create 动态结构 contraption / 无 BE 实例场景）：
	 * 无应力/转速 → 仅通道判定，不调制。机型承载上限由方块类型判断
	 * （蓝宝石 5 / 翡翠 3）。
	 *
	 * @param state     波闸方块状态（面板开闭/朝向）
	 * @param pos       波闸方块位置（与 wavePos 同坐标系）
	 * @param wavePos   波前中心（与 pos 同坐标系）
	 * @param movement  波的飞行方向（单位向量，与 state 同坐标系）
	 * @param waveLevel 波的当前等级
	 */
	public Result resolve(BlockState state, BlockPos pos, Vec3 wavePos, Vec3 movement, int waveLevel) {
		int maxSupported = state.getBlock() instanceof SapphireSpeedRegulatorBlock
			? com.hjmmd_8.createoreexpansion.content.wave.WaveLevels.SAPPHIRE_MAX
			: com.hjmmd_8.createoreexpansion.content.wave.WaveLevels.JADE_MAX;
		if (waveLevel > maxSupported)
			return Result.VANISH; // 翡翠机承载上限 3：4/5 级波调速必然维持 4/5 → 无效湮灭
		AbstractWaveGateRegulation.Result r = handle(state, pos, 0f, wavePos, movement);
		return toResult(r);
	}

	/** 结构化结果 → 波速调节器 5 值枚举。 */
	private Result toResult(AbstractWaveGateRegulation.Result r) {
		switch (r.channel()) {
			case VANISH -> {
				return Result.VANISH;
			}
			case BOUNCE -> {
				// 反弹不改速度（区别于调级器的反弹降级）
				return Result.BOUNCE;
			}
			case PASS -> {
				if (!r.modulate())
					return Result.PASS_UNCHANGED;
				// 波速调节器的加减速方向与调级器的增强/减弱相反：
				// 基类 boost 为"调级器语义"（dot*speed>0 = 顺基准 = 等级增强），
				// 实测校准：波速调节器在基类判定"减弱"方向时加速、反之减速，故取反。
				return !r.boost() ? Result.PASS_SPEED_UP : Result.PASS_SPEED_DOWN;
			}
		}
		return Result.VANISH;
	}

	/**
	 * 按接入转速计算本次调制应施加的速度修正量（格/秒）。
	 * <p>翡翠：100~256 RPM 平均分 4 档：第 1 档 +0.5、第 2 档 +1、第 3 档 +1.5、第 4 档 +2
	 * （上限 256 封顶；低于 100 不调制由基类判定保证，此处仅防除零）。</p>
	 *
	 * @param speed 当前转速（取绝对值参与分档）
	 * @return 档位对应的修正量（正 = 加速）
	 */
	public static float offsetForSpeed(float speed) {
		return offsetForSpeed(speed, TIER_BASE, TIER_MAX, TIER_COUNT, STEP);
	}

	/**
	 * 按接入转速计算本次调制应施加的速度修正量（格/秒），档位参数由机型决定。
	 *
	 * @param speed   当前转速（绝对值）
	 * @param base    分档转速区间下界（翡翠 100、蓝宝石 64）
	 * @param max     分档转速区间上界（256）
	 * @param tierCount 档位数（翡翠 4、蓝宝石 6）
	 * @param step    单档修正步长（0.5）
	 * @return 档位对应的修正量（正 = 加速）
	 */
	public static float offsetForSpeed(float speed, float base, float max, int tierCount, float step) {
		return (tierForSpeed(speed, base, max, tierCount) + 1) * step;
	}

	/**
	 * 当前转速对应的变速档位索引（0~3，翡翠；蓝宝石机型 0~5）。
	 *
	 * @param speed 当前转速（取绝对值参与分档）
	 * @return 档位索引
	 */
	public static int tierForSpeed(float speed) {
		return tierForSpeed(speed, TIER_BASE, TIER_MAX, TIER_COUNT);
	}

	/**
	 * 当前转速对应的变速档位索引，档位参数由机型决定。
	 * <p>区间 [base, max] 平均分 tierCount 档；低于 base 按 0（不达标不调制），高于 max 封顶末档。</p>
	 *
	 * @param speed     当前转速（取绝对值参与分档）
	 * @param base      分档转速区间下界
	 * @param max       分档转速区间上界
	 * @param tierCount 档位数
	 * @return 档位索引 0 ~ tierCount-1
	 */
	public static int tierForSpeed(float speed, float base, float max, int tierCount) {
		float abs = Math.abs(speed);
		float tier = (abs - base) / (max - base) * tierCount; // 0~tierCount
		return Math.max(0, Math.min(tierCount - 1, (int) Math.floor(tier)));
	}

	/** 档位罗马数字（I/II/III/IV，翡翠 4 档；蓝宝石 6 档扩展至 VI），供护目镜提示显示。 */
	public static String romanForTier(int tier) {
		return switch (tier) {
			case 0 -> "I";
			case 1 -> "II";
			case 2 -> "III";
			case 3 -> "IV";
			case 4 -> "V";
			default -> "VI";
		};
	}
}
