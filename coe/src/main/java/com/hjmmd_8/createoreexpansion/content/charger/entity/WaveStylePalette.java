package com.hjmmd_8.createoreexpansion.content.charger.entity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx.Accent;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx.AccentParticle;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx.StyleProfile;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTrailStyle;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx.LIGHTNING_FLASH_CHANCE;
import static com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx.LIGHTNING_FLASH_RATIO;
import static com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx.ORBIT_FLAG_END_ROD;
import static com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx.ORBIT_FLAG_SOUL_FIRE;
import static com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx.ORBIT_FLAG_SPEED;
import static com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx.ORBIT_FLAG_SPREAD;
import static com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx.ORBIT_SOUL_SPEED;
import static com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx.ORBIT_SOUL_SPREAD;
import static com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx.ORBIT_TRAIL_COUNT;
import static com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx.ORBIT_TRAIL_SCALE;
import static com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx.ORBIT_TRAIL_SPEED;
import static com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx.ORBIT_TRAIL_SPREAD;

/**
 * <b>风格调色板与"风格 → 粒子构成"映射表的构造</b>（2026-10-06 行为零变化拆分，从
 * {@link ChargerWaveFx} 的调色板常量、颜色变换纯函数、{@code FIRE_PROFILE} 与
 * {@code buildProfiles()} <b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>每一种风格／魔素长什么样</b>——一对端点色的取色算法
 * （{@link #edgeTint}）、机械与伤害两套合金/加热变换（{@link #metalTint} / {@link #heatTint}）、
 * 以及把这些打包成 {@link ChargerWaveFx#PROFILES} 的那张表（{@link #buildProfiles()}）。</p>
 *
 * <p><b>为什么档案类型仍住 {@link ChargerWaveFx}</b>：{@link StyleProfile} / {@link Accent} /
 * {@link AccentParticle} 是<b>公开</b>嵌套类型（{@code ChargerWaveFx.StyleProfile} 是既有
 * 跨包契约），搬走会改它们的二进制名 ⇒ 本类只搬"数据与纯函数"，不搬类型。</p>
 *
 * <p>搬运口径：方法体与常量初始化式逐字相同，差异只有三类 —— {@code private} → 包级私有
 * （{@link #buildProfiles()} 是唯一对外入口）、方法引用被搬动逼出的限定符
 * （{@code ChargerWaveFx::heatTint} → {@code WaveStylePalette::heatTint} 等）、
 * 以及 {@link ChargerWaveFx} 里那几个<b>公开</b>常量改用静态导入（{@code LIGHTNING_FLASH_*} /
 * {@code ORBIT_*} 仍是 {@link ChargerWaveFx} 的声明，一个字未动）。<b>端点色、比例、概率、
 * 覆盖值、档案表内容与 {@code Collections.unmodifiableMap} 的外壳全部逐字未动。</b>
 * 私有方法名一律沿用原样（{@code buildProfiles} / {@code metalTint} …），
 * 于是每条搬走的函数体都能与 HEAD 逐令牌比中。</p>
 *
 * <p>⚠ 声明序红线照旧：{@link #FIRE_PROFILE} 必须排在 {@link #buildProfiles()} <b>之前</b>——
 * 与原来"必须声明在 {@code PROFILES} 之前"是同一条（{@code PROFILES} 的静态初始化器就是
 * {@link #buildProfiles()}）。</p>
 */
final class WaveStylePalette {

	private WaveStylePalette() {
		throw new AssertionError("This class should not be instantiated");
	}

	// ==================== 风格调色板（纯色常量，供颜色变换函数取用） ====================

	/**
	 * 钢青色：机械风格的暗端。
	 *
	 * <p>2026-09-14 由原来的中性钢灰 {@code (0.62,0.60,0.56)} 改成<b>明显偏冷的钢青</b>——
	 * 玩家反馈"加工波的色调看不出来，只能靠爆开特效判断"：中性灰近于"褪色"，与各级波色拉不开差距；
	 * 偏青的钢色则是任何一级波色都不会出现的色相，一眼能认出这是加工波。</p>
	 */
	private static final Vec3 METAL_STEEL = new Vec3(0.52, 0.70, 0.82);
	/** 黄铜色：机械风格的亮端（暖、偏黄的机加工感）。 */
	private static final Vec3 METAL_BRASS = new Vec3(0.86, 0.62, 0.24);
	/** 深血色：伤害风格的暗端。 */
	private static final Vec3 HEAT_DEEP_RED = new Vec3(0.85, 0.15, 0.12);
	/** 炽橙色：伤害风格的亮端。 */
	private static final Vec3 HEAT_ORANGE = new Vec3(1.0, 0.55, 0.10);

	// ==================== 八种魔素（2026-10-03 需求 coe-ess）的调色板 ====================
	//
	// 每种魔素一对端点色（暗端 / 亮端）——<b>两端都留在该魔素的色相里</b>（不像 heatTint 那样
	// 亮端直接给"炽橙"）：由波的渲染色亮度在两端之间取色，再按 ESSENCE_BLEND 向波色插值。
	// 于是波级仍然可读（亮波取亮端、暗波取暗端；插值后还留着 15% 波色），
	// **而魔素的种类在任何波级上都一眼可辨**——正是作者 2026-10-03 的口径
	// "魔素颜色要与种类匹配（水=蓝、冰=淡蓝…）"。
	//
	// 为什么是 0.85 而不是 heatTint 的 0.7：波色里有 α 的纯黄 (1,1,0) 这种极亮基色，留三成会把
	// "水=蓝"拉成浅黄绿（0.7 时实算 α 波上的水 = (0.79,0.91,0.68)）；0.85 时 α 波上的水 =
	// (0.39,0.67,0.81) 的青蓝，仍留着 15% 波色（逐级实算表见汇报）。
	//
	// **这里是颜色的唯一真源**：后续批次的显示层 / 命中效果层要取色，一律走
	// ChargerWaveFx.styleColor(style, baseColor)（要固定色板就用 styleColor(style, Vec3(1,1,1))
	// = 该魔素的亮端色），**别再另开一份颜色表**（两处各写一份 = 漂移源）。

	/** 魔素端点色向该魔素主色的插值系数（见上面那段：为什么不是 heatTint 的 0.7）。 */
	private static final double ESSENCE_BLEND = 0.85D;

	/** 水魔素端点色：深（0.03,0.20,0.62 深蓝）→ 浅（0.32,0.66,1.00 亮蓝）。 */
	private static final Vec3 WATER_DEEP = new Vec3(0.03, 0.20, 0.62);
	private static final Vec3 WATER_PALE = new Vec3(0.32, 0.66, 1.00);
	/** 地魔素端点色：深（0.24,0.15,0.08 湿土）→ 浅（0.62,0.47,0.26 干土）。 */
	private static final Vec3 EARTH_DEEP = new Vec3(0.24, 0.15, 0.08);
	private static final Vec3 EARTH_PALE = new Vec3(0.62, 0.47, 0.26);
	/** 风魔素端点色：深（0.32,0.66,0.64 青）→ 浅（0.72,0.94,0.92 青白）。 */
	private static final Vec3 WIND_DEEP = new Vec3(0.32, 0.66, 0.64);
	private static final Vec3 WIND_PALE = new Vec3(0.72, 0.94, 0.92);
	/** 冰魔素端点色：深（0.34,0.64,0.94）→ 浅（0.62,0.88,1.00 淡蓝）。 */
	private static final Vec3 ICE_DEEP = new Vec3(0.34, 0.64, 0.94);
	private static final Vec3 ICE_PALE = new Vec3(0.62, 0.88, 1.00);
	/** 雷魔素端点色：深（0.40,0.34,1.00 电紫蓝）→ 浅（0.80,0.76,1.00 淡紫白）。 */
	private static final Vec3 LIGHTNING_DEEP = new Vec3(0.40, 0.34, 1.00);
	private static final Vec3 LIGHTNING_PALE = new Vec3(0.80, 0.76, 1.00);
	/** 毒魔素端点色：深（0.24,0.38,0.06 墨绿）→ 浅（0.72,0.94,0.20 黄绿）。 */
	private static final Vec3 POISON_DEEP = new Vec3(0.24, 0.38, 0.06);
	private static final Vec3 POISON_PALE = new Vec3(0.72, 0.94, 0.20);

	/** 毒魔素的点缀色：药水系黄绿（{@code ENTITY_EFFECT} 的带参构造取它）。 */
	private static final Vec3 POISON_ACCENT_COLOR = new Vec3(0.62, 0.86, 0.20);

	/**
	 * <b>伤害风格（攻击波）叠加的粒子类型</b>——集中一处定义，改一行即可整体换风格。
	 *
	 * <ul>
	 *   <li>现取 {@link ParticleTypes#FLAME}（火焰）：攻击波本体与拖尾已是红橙调，尾焰与之同色相，
	 *       读作"打击／灼烧"；</li>
	 *   <li>若日后想换成冰霜，只需把下面这一行改成 {@code ParticleTypes.SNOWFLAKE}
	 *       （冷调，与红橙主色反差最强）；</li>
	 * </ul>
	 *
	 * <p><b>2026-09 用户规格变更</b>：这个位置原先挂的是 {@code ParticleTypes.DAMAGE_INDICATOR}
	 * （原版 {@code damage} 贴图，形状就是一个<b>暗红色心形</b>）——用户实测反馈"攻击波尾部粒子是
	 * 红心，不对"，要求改成火焰/冰霜，故换成火焰。随此变更，本类原先"伤害风格刻意不用火焰
	 * （怕被误解成点燃机器）"的口径作废。</p>
	 */
	private static final ParticleOptions DAMAGE_ACCENT_PARTICLE = ParticleTypes.FLAME;

	/**
	 * <b>攻击波默认魔素「火」的档案 = 伤害风格的同一份实例</b>（2026-10-03 需求 coe-ess）。
	 *
	 * <p>为什么共用实例而不是在 {@link #buildProfiles()} 里再写一遍同样的字面量：
	 * 作者口径"火魔素与现状一模一样"⇒ 两份字面量只要能各自漂移，早晚会漂移；
	 * 这里是同一对象，<b>结构上不可能不一致</b>。关卡
	 * {@code wave-essence-fire-is-damage} 守着这条（且它在故意改坏时变红）。</p>
	 *
	 * <p>⚠ <b>必须声明在 {@link ChargerWaveFx#PROFILES} 之前</b>：{@code PROFILES} 的静态初始化器会调用
	 * {@link #buildProfiles()}，若本字段排在它后面，读到的是 {@code null}
	 * ⇒ DAMAGE/FIRE 双双落空并静默回落 NORMAL（不报错、观感全变）。</p>
	 */
	private static final StyleProfile FIRE_PROFILE = new StyleProfile(WaveStylePalette::heatTint, List.of(
		// 暴击星芒：快而散，数量占主粒子的 1/4 —— 打击瞬间的"脆"反馈
		Accent.of(ParticleTypes.CRIT, 0.25, 1.1, 1.5),
		// 尾焰：慢而聚，数量占 1/8 —— 原为"DAMAGE_INDICATOR（暗红心形）"，
		// 用户 2026-09 实测要求改成火焰，类型集中在 DAMAGE_ACCENT_PARTICLE 一处
		Accent.of(DAMAGE_ACCENT_PARTICLE, 0.125, 1.0, 0.8)),
		null, null, null, null);

	/**
	 * 构造风格档案表（此处即"风格 → 粒子"的全部知识）。
	 *
	 * <ul>
	 *   <li><b>NORMAL</b>：颜色恒等变换、无点缀——纯粹是改造前的原版染色尘埃，
	 *       保证旧重载（无风格参数）行为逐字不变。</li>
	 *   <li><b>MECHANICAL</b>（机械感，用于全能波"读机器加工"）：颜色向<b>冷调钢青</b>收敛
	 *       （见 {@link #metalTint}）；点缀 {@code ELECTRIC_SPARK} 表达"电学/机加工的火花"，
	 *       少量 {@code SMOKE} 表达"机器运转的粉尘排气"。<b>刻意不用火焰/岩浆类</b>，
	 *       避免被误解成"点燃机器"。</li>
	 *   <li><b>DAMAGE</b>（伤害感，用于攻击波）：见 {@link #FIRE_PROFILE}
	 *       （颜色向深红/橙收敛，见 {@link #heatTint}；点缀 {@code CRIT} 暴击星芒 +
	 *       {@link #DAMAGE_ACCENT_PARTICLE} 尾焰）。</li>
	 *   <li><b>WATER / FIRE / EARTH / WIND / ICE / LIGHTNING / POISON / ARCANE</b>
	 *       （2026-10-03 需求 coe-ess 的八种魔素）：每种一行，颜色变换各有端点色
	 *       （见调色板常量），点缀按需求 §3.1 右列逐条落；其中
	 *       <ul>
	 *         <li>{@code FIRE} = {@link #FIRE_PROFILE}（同一实例，攻击波默认魔素 ⇒ 观感零变化）；</li>
	 *         <li>其余七种的数量/尺度覆盖一律 {@code null}（= 用调用方基线 6/0.45 或 12/0.62），
	 *             <b>只有 ARCANE 例外</b>：它要等于环绕波那一套，故数量/尺度/散布/速度四项
	 *             全部<b>引用环绕波那组常量</b>（{@link ChargerWaveFx#ORBIT_TRAIL_COUNT} /
	 *             {@link ChargerWaveFx#ORBIT_TRAIL_SCALE} / {@link ChargerWaveFx#ORBIT_TRAIL_SPREAD} /
	 *             {@link ChargerWaveFx#ORBIT_TRAIL_SPEED}），两条点缀同样引用
	 *             {@link ChargerWaveFx#ORBIT_FLAG_END_ROD} / {@link ChargerWaveFx#ORBIT_FLAG_SOUL_FIRE} /
	 *             {@link ChargerWaveFx#ORBIT_FLAG_SPREAD} / {@link ChargerWaveFx#ORBIT_SOUL_SPREAD}——
	 *             <b>不复制字面量</b>，
	 *             于是"异"与环绕波的结构性一致（改常量两处同时改）。</li>
	 *         <li><b>环面留痕桩不进 ARCANE</b>：它是"这枚波在环绕"这个几何事实专属，主波没有环平面。</li>
	 *       </ul>
	 *   </li>
	 * </ul>
	 */
	static Map<WaveTrailStyle, StyleProfile> buildProfiles() {
		EnumMap<WaveTrailStyle, StyleProfile> profiles = new EnumMap<>(WaveTrailStyle.class);
		profiles.put(WaveTrailStyle.NORMAL,
			new StyleProfile(UnaryOperator.identity(), List.of(), null, null, null, null));
		profiles.put(WaveTrailStyle.MECHANICAL,
			new StyleProfile(WaveStylePalette::metalTint, List.of(
				// 电火花：快、散、数量占主粒子的一半（2026-09-14 由 1/4 提到 1/2 —— 机械感的"电"是最显眼的标识）
				Accent.of(ParticleTypes.ELECTRIC_SPARK, 0.5, 1.15, 1.6),
				// 烟雾：慢、略散、数量占 1/6 —— 机器排气的"重"感（非火焰）
				Accent.of(ParticleTypes.SMOKE, 0.1666, 1.35, 0.5)),
				null, null, null, null));
		// 伤害风格 = 火魔素（同一实例；见 FIRE_PROFILE 的说明与声明序红线）
		profiles.put(WaveTrailStyle.DAMAGE, FIRE_PROFILE);
		profiles.put(WaveTrailStyle.FIRE, FIRE_PROFILE);

		// ---- 其余七种魔素（2026-10-03 需求 coe-ess）------------------------------------
		// 每条点缀的"比例 / 散布倍率 / 速度倍率 / 概率"都写在行内；比例在生效基数 6 上必须
		// ≥ 1/12 才不为 0 颗，故"少量/偶发"一律用 rare(概率) 表达，不用更小的比例。
		profiles.put(WaveTrailStyle.WATER,
			new StyleProfile(WaveStylePalette::waterTint, List.of(
				// 气泡：慢、略散 —— 水的"体"
				Accent.of(ParticleTypes.BUBBLE, 0.25, 1.20, 0.60),
				// 气泡破裂：比气泡快一点，读作"浮到水面炸开"
				Accent.of(ParticleTypes.BUBBLE_POP, 0.1666, 1.10, 0.90),
				// 水花：快而散 —— 溅起来的那一下
				Accent.of(ParticleTypes.SPLASH, 0.125, 1.35, 1.40),
				// 落水：偶发的一颗"水滴"（1/8 概率）
				Accent.of(ParticleTypes.FALLING_WATER, 0.1666, 1.00, 0.40).rare(0.125D)),
				null, null, null, null));
		profiles.put(WaveTrailStyle.EARTH,
			new StyleProfile(WaveStylePalette::earthTint, List.of(
				// 三种泥土各带概率：六条若全必发会在基数 6 上接近主粒子量（成"粒子雨"），
				// 故按合计每 tick 约 1.95 颗铺开（见类注释的魔素预算）
				Accent.of(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
					0.1666, 0.90, 0.70).rare(0.50D),
				Accent.of(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COARSE_DIRT.defaultBlockState()),
					0.1666, 0.95, 0.75).rare(0.40D),
				Accent.of(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()),
					0.1666, 0.90, 0.70).rare(0.30D),
				// 草：草方块碎屑
				Accent.of(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GRASS_BLOCK.defaultBlockState()),
					0.1666, 0.85, 0.60).rare(0.35D),
				// 孢子花粉：轻、慢、飘（"草"的另一种读法）
				Accent.of(ParticleTypes.SPORE_BLOSSOM_AIR, 0.1666, 1.25, 0.30).rare(0.25D),
				// 堆肥桶粒子：土腥味的最后一层
				Accent.of(ParticleTypes.COMPOSTER, 0.1666, 1.00, 0.50).rare(0.15D)),
				null, null, null, null));
		profiles.put(WaveTrailStyle.WIND,
			new StyleProfile(WaveStylePalette::windTint, List.of(
				// 阵风：⚠ 1.21.1 的 SMALL_GUST 是 SimpleParticleType（无颜色/尺寸参数），
				// 只能无参使用（见 AccentParticle 的说明）；"尺寸跟随波的尺度"落在主粒子上
				Accent.of(ParticleTypes.SMALL_GUST, 0.25, 1.40, 1.20)),
				null, null, null, null));
		profiles.put(WaveTrailStyle.ICE,
			new StyleProfile(WaveStylePalette::iceTint, List.of(
				// 细雪：主点缀
				Accent.of(ParticleTypes.SNOWFLAKE, 0.25, 1.10, 0.70),
				// 雪块碎屑
				Accent.of(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState()),
					0.125, 1.00, 0.80),
				// 雪球碎屑：偶发
				Accent.of(ParticleTypes.ITEM_SNOWBALL, 0.1666, 1.30, 1.30).rare(0.35D)),
				null, null, null, null));
		profiles.put(WaveTrailStyle.LIGHTNING,
			new StyleProfile(WaveStylePalette::lightningTint, List.of(
				// 电火花：快、散 —— 雷的主体点缀
				Accent.of(ParticleTypes.ELECTRIC_SPARK, 0.25, 1.15, 1.60),
				// 强闪光：低概率 + 只挂主波（完整公式与常量见 LIGHTNING_FLASH_RATIO / LIGHTNING_FLASH_CHANCE）；
				// 散布 0.6 倍、速度 0 ⇒ 一颗停在波位置上的闪光，不跟着飞出去
				Accent.of(ParticleTypes.FLASH, LIGHTNING_FLASH_RATIO, 0.60, 0.0D)
					.rare(LIGHTNING_FLASH_CHANCE).mainOnly()),
				null, null, null, null));
		profiles.put(WaveTrailStyle.POISON,
			new StyleProfile(WaveStylePalette::poisonTint, List.of(
				// 药水粒子：带参构造，颜色取药水系黄绿（见 POISON_ACCENT_COLOR）
				Accent.of(WaveStylePalette::poisonEffectParticle, 0.25, 1.05, 0.50),
				// 黏液：更"黏"的观感，偶发（需求里写作"可选"）
				Accent.of(ParticleTypes.ITEM_SLIME, 0.1666, 0.90, 0.35).rare(0.30D)),
				null, null, null, null));
		profiles.put(WaveTrailStyle.ARCANE,
			new StyleProfile(UnaryOperator.identity(), List.of(
				// 与环绕波原来那套常量逐项对齐（全部引用那组常量，不复制字面量）：
				//   END_ROD 每 tick 4 颗、散布 0.10、速度 0.02
				new Accent(AccentParticle.of(ParticleTypes.END_ROD),
					(double) ORBIT_FLAG_END_ROD / ORBIT_TRAIL_COUNT, 1.0D, ORBIT_FLAG_SPEED / ORBIT_TRAIL_SPEED,
					1.0D, false, new Vec3(ORBIT_FLAG_SPREAD, ORBIT_FLAG_SPREAD, ORBIT_FLAG_SPREAD)),
				//   青焰每 tick 2 颗、速度 0.005；散布是**各向同性** 0.02（环绕波那处是按环平面法向
				//   逐轴缩放 motion×0.02，静态档案表达不了方向 —— 见汇报里的"逐项对照"）
				new Accent(AccentParticle.of(ParticleTypes.SOUL_FIRE_FLAME),
					(double) ORBIT_FLAG_SOUL_FIRE / ORBIT_TRAIL_COUNT, 1.0D, ORBIT_SOUL_SPEED / ORBIT_TRAIL_SPEED,
					1.0D, false, new Vec3(ORBIT_SOUL_SPREAD, ORBIT_SOUL_SPREAD, ORBIT_SOUL_SPREAD))),
				// 数量 12 / 尺度 0.62 / 散布 0.12 / 速度 0.01 —— 与环绕波逐字节同一组常量
				ORBIT_TRAIL_COUNT, ORBIT_TRAIL_SCALE,
				new Vec3(ORBIT_TRAIL_SPREAD, ORBIT_TRAIL_SPREAD, ORBIT_TRAIL_SPREAD), ORBIT_TRAIL_SPEED));
		return Collections.unmodifiableMap(profiles);
	}

	/**
	 * 机械风格的颜色变换：把波的渲染色"合金化"。
	 *
	 * <p>两步纯数学，无分支：</p>
	 * <ol>
	 *   <li><b>去饱和</b>：按亮度把基色向自身灰度插值 <b>80%</b>——金属的反射光很弱饱和，
	 *       这一步让任何波色（黄/绿/蓝/紫/玫红）先褪成"金属灰"的底色；</li>
	 *   <li><b>合金化</b>：再向"钢青→黄铜"的合金色插值 <b>0.88</b>。合金色由基色亮度决定
	 *       （暗 → {@link #METAL_STEEL} 钢青；亮 → {@link #METAL_BRASS} 黄铜），
	 *       于是"暗色波偏冷钢、亮色波偏暖铜"，仍能看出是哪一级波，但整体是机械金属调。</li>
	 * </ol>
	 *
	 * <p><b>2026-09-14 调强</b>：原先"去饱和 55% + 合金 65%"，合成后仍留着约三成原色相，
	 * 在高速飞行的短拖尾上几乎看不出来（玩家实测反馈）。现在合成后原色相只剩约 2%~3%
	 * （{@code 0.2 × 0.12 ≈ 0.024}）——**颜色本身**就是加工波的标识，不必再等它爆开。</p>
	 */
	private static Vec3 metalTint(Vec3 base) {
		double lum = Mth.clamp(luminance(base), 0.0, 1.0);
		Vec3 desaturated = base.lerp(new Vec3(lum, lum, lum), 0.80);
		Vec3 alloy = METAL_STEEL.lerp(METAL_BRASS, lum);
		return desaturated.lerp(alloy, 0.88);
	}

	/**
	 * 伤害风格的颜色变换：把波的渲染色"加热"。同样无分支两步：
	 * <ol>
	 *   <li>由基色亮度在"深红(0.85,0.15,0.12) → 炽橙(1.0,0.55,0.1)"之间取热度色；</li>
	 *   <li>基色向热度色插值 70%——保留 30% 原波色，所以仍能看出这是哪一级波，
	 *       但整体压进红橙区间，读作"伤害"而非"充能"。</li>
	 * </ol>
	 */
	private static Vec3 heatTint(Vec3 base) {
		double lum = Mth.clamp(luminance(base), 0.0, 1.0);
		Vec3 heat = HEAT_DEEP_RED.lerp(HEAT_ORANGE, lum);
		return base.lerp(heat, 0.7);
	}

	// ==================== 八种魔素（2026-10-03 需求 coe-ess）的颜色变换 ====================
	//
	// 全部是同一个纯函数 edgeTint 的实例：由基色亮度在"暗端 → 亮端"之间取该魔素的主色，
	// 再按 ESSENCE_BLEND 向波色插值（= 只变换、不覆盖，仍能看出波级）。
	// 加一种魔素 = 加一对端点色常量 + 一行方法引用，不必新写算法。

	/**
	 * 魔素的通用颜色变换：<b>由基色亮度在端点色之间取主色，再向基色插值 {@link #ESSENCE_BLEND}</b>。
	 *
	 * @param base 波的渲染色（纯函数，不改入参）
	 * @param dark 该魔素的暗端色（波色暗 ⇒ 取它）
	 * @param pale 该魔素的亮端色（波色亮 ⇒ 取它）
	 */
	private static Vec3 edgeTint(Vec3 base, Vec3 dark, Vec3 pale) {
		double lum = Mth.clamp(luminance(base), 0.0, 1.0);
		return base.lerp(dark.lerp(pale, lum), ESSENCE_BLEND);
	}

	/** 水魔素：蓝（深蓝 → 亮蓝）。 */
	private static Vec3 waterTint(Vec3 base) {
		return edgeTint(base, WATER_DEEP, WATER_PALE);
	}

	/** 地魔素：泥土棕（壤土 → 干土）。 */
	private static Vec3 earthTint(Vec3 base) {
		return edgeTint(base, EARTH_DEEP, EARTH_PALE);
	}

	/** 风魔素：青白（青 → 青白）。 */
	private static Vec3 windTint(Vec3 base) {
		return edgeTint(base, WIND_DEEP, WIND_PALE);
	}

	/** 冰魔素：淡蓝（蓝 → 淡蓝）。 */
	private static Vec3 iceTint(Vec3 base) {
		return edgeTint(base, ICE_DEEP, ICE_PALE);
	}

	/** 雷魔素：电紫蓝 → 淡紫白。 */
	private static Vec3 lightningTint(Vec3 base) {
		return edgeTint(base, LIGHTNING_DEEP, LIGHTNING_PALE);
	}

	/** 毒魔素：墨绿 → 黄绿。 */
	private static Vec3 poisonTint(Vec3 base) {
		return edgeTint(base, POISON_DEEP, POISON_PALE);
	}

	/**
	 * 毒魔素的点缀粒子：{@code ENTITY_EFFECT}（药水粒子）按<b>渲染色</b>带参构造，
	 * 颜色向 {@link #POISON_ACCENT_COLOR}（药水系黄绿）插值 0.7 —— 仍留着三成波色，
	 * 于是"毒"与"这是哪一级波"同时可读。
	 */
	private static ParticleOptions poisonEffectParticle(Vec3 color, float scale) {
		Vec3 c = (color == null ? Vec3.ZERO : color).lerp(POISON_ACCENT_COLOR, 0.7D);
		return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,
			(float) c.x, (float) c.y, (float) c.z);
	}

	/** 感知亮度（Rec.601 权重）：颜色变换用它决定"暗端/亮端"的插值权重。 */
	private static double luminance(Vec3 color) {
		return 0.299 * color.x + 0.587 * color.y + 0.114 * color.z;
	}
}
