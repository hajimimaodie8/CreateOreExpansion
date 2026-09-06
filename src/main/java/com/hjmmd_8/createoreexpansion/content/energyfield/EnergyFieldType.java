package com.hjmmd_8.createoreexpansion.content.energyfield;

/**
 * 能量场类型（匀强场）：
 * <ul>
 *   <li><b>ACCELERATION（加速场）</b>——类比电场：沿场方向给带电实体持续施加加速度
 *       （正电荷沿场方向加速、负电荷反向），速度随在场时间累积；</li>
 *   <li><b>DEFLECTION（偏转场）</b>——类比磁场：给运动带电实体施加<b>垂直于运动方向</b>
 *       的偏转力（洛伦兹式，方向 = 运动方向 × 场方向 再按电荷极性取号），
 *       不改变速率、只弯折路径。</li>
 * </ul>
 */
public enum EnergyFieldType {
	/** 加速场：沿场方向加速/减速带电实体（不改方向，只改沿场轴速度分量）。 */
	ACCELERATION,
	/** 偏转场：横向弯折带电实体路径（不改变速率大小）。 */
	DEFLECTION,
}
