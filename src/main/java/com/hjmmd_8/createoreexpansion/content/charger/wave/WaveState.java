package com.hjmmd_8.createoreexpansion.content.charger.wave;

import java.util.Locale;

/**
 * 能量波的<b>状态</b>（2026-09 用户口径：三种状态，且<b>一生只能改变一次</b>）。
 *
 * <ul>
 *   <li>{@link #NORMAL} <b>普通态</b>：本模组原生能量波，"废物"——<b>只会充能加工</b>
 *       （{@code createoreexpansion:charging} 配方），此外什么都没有：不伤害生物、
 *       没有机器读取与远程加工、没有链式/引雷/载荷；</li>
 *   <li>{@link #OMNI} <b>变体态／全能态</b>：经变器"加工波变态"穿波转换而来
 *       （即原"变体波" {@code StellarWaveEntity}）。<b>没有充能加工</b>，但有
 *       "读取机器 → 携带能力 → 远程加工"这一整套（扫描携带 / 候选门槛 / 链式 / 引雷 / 载荷），
 *       保留原色调，拖尾走机械感特效；同样不伤害生物；</li>
 *   <li>{@link #ATTACK} <b>攻击态／攻击波</b>：被变器"攻击波变态"的场点燃而来，
 *       <b>纯攻击</b>——不加工任何配方（充能与远程加工都没有），只对生物造成伤害，
 *       伤害值沿用原普通能量波对生物造成的伤害（{@code WaveLevels.damage(waveLevel)}），
 *       拖尾走红色伤害感特效。</li>
 * </ul>
 *
 * <p><b>只变一次的规则</b>：{@link #NORMAL} 之外的状态视为"已定型"（{@link #isLocked()}），
 * 任何转换或场都不再改动它——于是"攻击波飞过加工波变态变器不会变全能态"、
 * "全能态飞过攻击场也不会获得伤害"这两条由状态机本身保证，不必在各处写特判。</p>
 *
 * <p><b>不动的东西</b>：波与波湮灭（{@code triggerBoom}）的表现与它对生物/实体的伤害，
 * 与状态无关，保持原样。</p>
 */
public enum WaveState {

	/** 普通态：只会充能加工。 */
	NORMAL,
	/** 变体态／全能态：远程读取机器并加工，无充能加工、无伤害。 */
	OMNI,
	/** 攻击态：纯攻击，只伤害生物。 */
	ATTACK;

	/** 是否已定型（只变一次：非普通态即已改变过，之后不再接受任何状态改动）。 */
	public boolean isLocked() {
		return this != NORMAL;
	}

	/** 是否对生物造成伤害（仅攻击态；伤害值沿用原普通能量波的公式）。 */
	public boolean dealsDamage() {
		return this == ATTACK;
	}

	/** 是否处理"充能波"（{@code createoreexpansion:charging}）配方——只有普通态会。 */
	public boolean allowsChargingRecipes() {
		return this == NORMAL;
	}

	/** 是否具备"读取机器 → 远程加工"能力——只有变体态有。 */
	public boolean allowsRemoteProcessing() {
		return this == OMNI;
	}

	/** 存档/网络用的稳定 id（与枚举名解耦，避免以后加状态时错位）。 */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** 按 id 还原（未知/缺失一律回 {@link #NORMAL}，老存档不会因缺字段出错）。 */
	public static WaveState byId(String id) {
		if (id != null)
			for (WaveState s : values())
				if (s.id()
					.equals(id))
					return s;
		return NORMAL;
	}

	/** 按序数还原（同步数据用；越界回 {@link #NORMAL}）。 */
	public static WaveState byOrdinal(int ordinal) {
		WaveState[] all = values();
		return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NORMAL;
	}
}
