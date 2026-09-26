package com.hjmmd_8.createoreexpansion.content.energyfield;

/**
 * 电荷极性（能量着电器赋予能量波/实体）：
 * <ul>
 *   <li><b>POSITIVE（正电荷）</b>：在加速场中沿场方向受力；在偏转场中按
 *       {@code 运动 × 场} 方向偏转；</li>
 *   <li><b>NEGATIVE（负电荷）</b>：受力与正电荷相反（加速场反向、偏转场反向弯折）。</li>
 * </ul>
 */
public enum ChargePolarity {
	POSITIVE,
	NEGATIVE;

	/** 受力方向符号：正 = +1，负 = -1（加速沿场、偏转取反）。 */
	public double sign() {
		return this == POSITIVE ? 1 : -1;
	}
}
