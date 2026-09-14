package com.hjmmd_8.createoreexpansion.content.wave.gauge;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.entity.StellarWaveEntity;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveType;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTypes;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 波情读数：一只能量波<b>五要素</b>取值的打包体，并负责"取值 → 文案"。
 *
 * <p>五要素（成组显示，顺序固定）：波速（格/秒）→ 波级（只显示希腊字母）→ 波载荷 → 波型 →
 * <b>剩余寿命</b>（秒；用户 2026-09-14 新增的第五项）。五要素之后按<b>波型</b>追加一项：
 * 普通波不追加；全能波追加"可加工配方种类：N"；攻击波追加"攻击伤害：X"。</p>
 *
 * <p><b>寿命口径</b>：取 {@code AbstractChargerWaveEntity#getRemainingLifetime()}（= 寿命上限
 * {@code MAX_LIFETIME_TICKS} − 已存活 tick 数）换算成秒，与 Jade 的"剩余寿命"行<b>同一个取值点</b>，
 * 两处不可能对不上。注意它是"<b>时间</b>寿命"：波还会因为飞满 {@code MAX_TRAVEL_DISTANCE} 而提前消散，
 * 那个距离上限不折算进来（"寿命"就是时间，两套消散条件分开报）。</p>
 *
 * <p><b>职责边界</b>：本类不知道自己是被谁查的、也不去找波——找波 / 显示 / 冷却都在
 * {@link WaveQueryGaugeItem} 里；本类只回答"这只波的波情长什么样"。
 * 取值的唯一来源是波实体自己的公开访问器（波速 {@code getWaveSpeed}、
 * 波级 {@code getWaveLevel}、波型 {@code getWaveType}、寿命 {@code getRemainingLifetime}）与
 * {@link WaveLevels}（希腊字母与伤害的唯一实现点），不复制任何数值表。</p>
 *
 * <p><b>每次调用都现场取值</b>（不缓存）：查询仪在扫描动画期间每 tick 重建一次读数、
 * Jade 每帧重建一次，于是"动态"是天然的——寿命会一秒一秒往下掉，波速会随调节器与能量场变。</p>
 *
 * @param speed                 波速（格/秒，含波速调节器修正）
 * @param level                 波级（1~5），显示时只给希腊字母 α/β/γ/ε/ω
 * @param payload               波载荷读数（见 {@link WavePayloadReadout}）
 * @param type                  波型（显示名走 {@link WaveType#displayName()}，本类不对波型 id 写 switch）
 * @param processableRecipeTypes 全能波"能加工几种配方"的取值（<b>类型数</b>口径，见下）
 * @param remainingLifetime     剩余寿命（秒；见类注释的寿命口径）
 */
public record WaveReadout(double speed, int level, WavePayloadReadout payload, WaveType type,
	int processableRecipeTypes, double remainingLifetime) {

	/** 本物品全部词条前缀（中英词条见 data/lang 的两个 LangProvider）。 */
	private static final String LANG = "createoreexpansion.wave_gauge.";

	/** 取一只能量波的波情读数（只读，不改动波的状态）。 */
	public static WaveReadout of(AbstractChargerWaveEntity wave) {
		return new WaveReadout(wave.getWaveSpeed(), wave.getWaveLevel(), WavePayloadReadout.of(wave),
			wave.getWaveType(), processableRecipeTypes(wave), wave.getRemainingLifetime() / 20.0d);
	}

	/**
	 * 全能波"能加工几种配方" = 它携带的<b>应执行配方类型集合</b>的大小
	 * （{@link StellarWaveEntity#getActiveRecipeTypes()}，已去重的类型数，<b>不是</b>配方条目数）。
	 *
	 * <p>全能在本模组由变体波承载（波型只能由变体波自己转换），故非变体波给 0；
	 * 取不到也不抛异常，读数永远可用。</p>
	 */
	private static int processableRecipeTypes(AbstractChargerWaveEntity wave) {
		return wave instanceof StellarWaveEntity stellar ? stellar.getActiveRecipeTypes()
			.size() : 0;
	}

	/**
	 * 动作栏单行读数：五要素成组 + 按波型追加。
	 *
	 * <p>格式与连接符全在词条里（{@code readout} 管五要素，{@code join} 管追加项的连接），
	 * 便于中英各自调整语序与标点。</p>
	 */
	public Component line() {
		MutableComponent line = Component.translatable(LANG + "readout", speedText(), levelText(), payload.summary(),
			type.displayName(), lifetimeText());
		Component appendix = appendix();
		if (appendix != null)
			line.append(Component.translatable(LANG + "join"))
				.append(appendix);
		return line;
	}

	/** 波速文案：保留 1 位小数（格/秒）。 */
	private String speedText() {
		return String.format(Locale.ROOT, "%.1f", speed);
	}

	/** 剩余寿命文案：保留 1 位小数（秒）；取值见类注释的寿命口径。 */
	private String lifetimeText() {
		return String.format(Locale.ROOT, "%.1f", Math.max(0.0d, remainingLifetime));
	}

	/** 波级文案：只给希腊字母（查 {@link WaveLevels#displayName}，并配该等级的指示色）。 */
	private Component levelText() {
		return WaveLevels.displayName(level)
			.withStyle(style -> style.withColor(WaveLevels.indicatorColor(level)));
	}

	/**
	 * 按波型追加的读数：普通波 {@code null}（不追加任何东西）；
	 * 全能波给可加工配方种类；攻击波给攻击伤害。判型按波型 id（扩展模组注册的新波型自然不命中）。
	 */
	@Nullable
	private Component appendix() {
		if (is(WaveTypes.OMNI))
			return Component.translatable(LANG + "tail_omni", processableRecipeTypes);
		if (is(WaveTypes.ATTACK))
			return Component.translatable(LANG + "tail_attack", damageText());
		return null;
	}

	/** 本波型是否就是给定波型（按 id 比，不用对象同一性，扩展模组同 id 注册也认）。 */
	private boolean is(WaveType target) {
		return type != null && type.id() != null && type.id()
			.equals(target.id());
	}

	/** 攻击伤害文案：整数伤害不带小数尾巴（4，而不是 4.0）；伤害表将来出现小数也能显示。 */
	private String damageText() {
		float damage = WaveLevels.damage(level);
		return damage == Math.round(damage) ? String.valueOf(Math.round(damage))
			: String.format(Locale.ROOT, "%.1f", damage);
	}
}
