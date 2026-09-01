package com.hjmmd_8.createoreexpansion.content.wave.regulation;

import com.hjmmd_8.createoreexpansion.content.wave.block.WaveSpeedRegulatorBlockEntity;

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

	/** 分档转速区间下界：100 RPM（= FAST 门槛，转速达标才调制）。 */
	private static final float TIER_BASE = 100f;
	/** 分档转速区间上界：256 RPM。 */
	private static final float TIER_MAX = 256f;
	/** 档位步长：每档加速量（格/秒），第 1~4 档 = 0.5 / 1 / 1.5 / 2。 */
	private static final float STEP = 0.5f;

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
	 * @param be        波速调节器方块实体（用于取应力/转速与面板状态）
	 * @param wavePos   波前中心（世界坐标，波实体碰撞盒中心）：入口中心 4×4 区域判定
	 * @param movement  波的飞行方向（单位向量）
	 * @return 判定结果；波实体据此决定穿过 / 遣返 / 消失
	 */
	public Result resolve(WaveSpeedRegulatorBlockEntity be, Vec3 wavePos, Vec3 movement) {
		AbstractWaveGateRegulation.Result r = handle(be, wavePos, movement);
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
	 * <p>100~256 RPM 平均分 4 档：第 1 档 +0.5、第 2 档 +1、第 3 档 +1.5、第 4 档 +2
	 * （上限 256 封顶；低于 100 不调制由基类判定保证，此处仅防除零）。</p>
	 *
	 * @param speed 当前转速（取绝对值参与分档）
	 * @return 档位对应的修正量（正 = 加速）
	 */
	public static float offsetForSpeed(float speed) {
		return (tierForSpeed(speed) + 1) * STEP;
	}

	/**
	 * 当前转速对应的变速档位索引（0~3）。
	 * <p>100~256 RPM 平均分 4 档（每档 39 RPM）：
	 * 档 0（I）= 100~138、档 1（II）= 139~177、档 2（III）= 178~216、档 3（IV）= 217~256。
	 * 低于 100 按 0 处理（不达标不调制，仅供显示参考）；高于 256 封顶按 3。</p>
	 *
	 * @param speed 当前转速（取绝对值参与分档）
	 * @return 档位索引 0~3
	 */
	public static int tierForSpeed(float speed) {
		float abs = Math.abs(speed);
		float tier = (abs - TIER_BASE) / (TIER_MAX - TIER_BASE) * 4f; // 0~4
		return Math.max(0, Math.min(3, (int) Math.floor(tier)));
	}

	/** 档位罗马数字（I/II/III/IV），供护目镜提示显示。 */
	public static String romanForTier(int tier) {
		return switch (tier) {
			case 0 -> "I";
			case 1 -> "II";
			case 2 -> "III";
			default -> "IV";
		};
	}
}
