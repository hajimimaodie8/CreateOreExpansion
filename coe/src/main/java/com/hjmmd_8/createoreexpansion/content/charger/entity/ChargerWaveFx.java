package com.hjmmd_8.createoreexpansion.content.charger.entity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTrailStyle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import org.joml.Vector3f;

/**
 * 能量波视觉效果工具 —— 命中绽放/范围爆炸/粒子构造，与波实体状态解耦（参数化）。
 *
 * <p>从 {@link AbstractChargerWaveEntity} 拆出（该实体曾同时承担实体状态/碰撞判定/
 * 机器交互/视觉效果的职责）；本类只负责"波产生的声光效果"，无实体字段依赖。</p>
 *
 * <p><b>风格化（{@link WaveTrailStyle}）</b>：本类的粒子构造一律先经
 * {@link #profile(WaveTrailStyle) 风格档案}，由<b>唯一一处映射表</b>
 * {@link #PROFILES} 决定"这一风格的主粒子颜色怎么变、要不要叠什么点缀粒子"。
 * 所有公开方法都有风格参数版本，并保留无风格参数的旧重载（默认 {@link WaveTrailStyle#NORMAL}，
 * 行为与改造前逐字一致），供既有调用点继续使用。</p>
 *
 * <p><b>粒子预算</b>：点缀粒子按"<b>生效颗数</b> × 比例"派生（见 {@link Accent} 与
 * {@link #accentCount(int, Accent) 数量算法}）——<b>点缀颗数 = Math.round(生效颗数 × 比例)</b>，
 * 生效颗数 = 该档案的数量覆盖（{@link StyleProfile#countOverride()}；null 时 = 调用方基数）。
 * 每条点缀还可带<b>出现概率</b>（{@link Accent#chance()}）：概率 <b>只决定这一簇发不发</b>，
 * 发就按上面的颗数发——于是"偶发的一两颗"不必再靠"更小的比例"表达（比例 &lt; 1/12 在基数 6 上
 * 会被四舍五入成 0 颗，见 {@link #LIGHTNING_FLASH_CHANCE}）。旧的三个风格各挂两种必发点缀、
 * 比例 1/4 与 1/8（两比例之和恒为 0.375），故单次发射的总粒子量最多为主粒子基数的 1.375 倍。
 * <b>逐档实算</b>
 * （基数 + round(基数 × 1/4) + round(基数 × 1/8) = 合计）：</p>
 * <ul>
 *   <li><b>拖尾</b>（基数 6，见 {@code AbstractChargerWaveEntity} 每 tick 6 颗）：
 *       6 + round(1.5)=2 + round(0.75)=1 = <b>9</b>；</li>
 *   <li><b>绽放</b>（基数 {@link #BURST_COUNT} = 30）：30 + round(7.5)=8 + round(3.75)=4 = <b>42</b>；</li>
 *   <li><b>爆炸</b>（基数 {@code 30 + boomLevel × 25}，见 {@link #triggerBoom}）：
 *       1 级 55 + round(13.75)=14 + round(6.875)=7 = <b>76</b>；
 *       2 级 80 + round(20)=20 + round(10)=10 = <b>110</b>；
 *       3 级 105 + round(26.25)=26 + round(13.125)=13 = <b>144</b>；
 *       4 级 130 + round(32.5)=33 + round(16.25)=16 = <b>179</b>；
 *       5 级 155 + round(38.75)=39 + round(19.375)=19 = <b>213</b>。</li>
 * </ul>
 * <p>以上合计<b>不含</b>爆炸的"可选第二色"那一次发送——它另发 {@code 基数 / 2} 颗主粒子
 * （整数除法：55→27、80→40、105→52、130→65、155→77），且不带任何点缀。
 * 各档点缀颗数上限为 39（5 级爆炸），始终低于 {@link #MAX_ACCENT_PER_EMIT}，
 * 故"总量不超过现状 2 倍"的红线在 1~5 级内恒满足。
 * {@link WaveTrailStyle#NORMAL} 不挂任何点缀，逐字节等于改造前。</p>
 *
 * <p><b>2026-10-03（需求 coe-ess）：八种魔素的点缀预算</b>。魔素挂 1~6 条点缀，多数带出现概率，
 * 故按<b>比例 × 概率之和</b>计（拖尾生效基数 6）：水 <b>0.5624</b> / 火 0.375（= 伤害风格）/
 * 地 0.3249 / 风 0.25 / 冰 0.4333 / 雷 0.2552 / 毒 0.30 / 异 0.5（异另带数量覆盖 12，
 * 与调用方基数无关）。最大 0.5624（水）⇒ 单次发射的总量不超过主粒子的 <b>1.5625 倍</b>，
 * 仍在本类"不超过现状 2 倍"的红线内；{@link #MAX_ACCENT_PER_EMIT} 依旧不会被触发
 * （魔素侧最大的 1/4 比例在 5 级爆炸上给 39 颗，与旧风格同为最大值）。</p>
 */
public final class ChargerWaveFx {

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
	 * 雷魔素：{@code FLASH} 点缀的<b>每次发射颗数系数</b>。
	 *
	 * <p>取 1/6 而不是"更小的比例"：{@link #accentCount(int, Accent)} 是
	 * {@code round(基数 × 比例)}，主波拖尾基数 6 ⇒ <b>比例 &lt; 1/12 时结果恒为 0</b>
	 * （一颗都不发、且不报错）。1/6 在基数 6 上恰好 {@code round(1.0) = 1} 颗。</p>
	 */
	public static final double LIGHTNING_FLASH_RATIO = 1.0D / 6.0D;

	/**
	 * 雷魔素：{@code FLASH} 的<b>每次发射出现概率</b>（<i>作者 2026-10-03 交由执行会话定</i>）。
	 *
	 * <p><b>完整公式</b>：{@code 期望颗数/发 = round(生效基数 × }{@link #LIGHTNING_FLASH_RATIO}{@code )
	 * × }{@link #LIGHTNING_FLASH_CHANCE}{@code }。</p>
	 *
	 * <p><b>目标观感 = 偶发、不刺眼</b>：主波拖尾每 tick 发一次，本概率下平均每 32 tick
	 * （约 1.6 秒）才闪 1 颗；且"概率只决定这一簇发不发、发就只发 1 颗"——绝不会出现每 tick 多颗
	 * 强闪光糊屏（{@code FLASH} 是强闪光粒子）。该簇另标了 {@code mainWaveOnly}：
	 * 只挂主波、环绕波不发（作者建议"低概率 + 只挂主波"）。</p>
	 */
	public static final double LIGHTNING_FLASH_CHANCE = 1.0D / 32.0D;

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

	/** 命中绽放的主粒子基数（改造前即为 30，保持不变）。 */
	private static final int BURST_COUNT = 30;
	/**
	 * 单个点缀档案的颗数上限（每次发送各自封顶）：防止未来等级/比例改动让点缀失控。
	 * <p>现有 1~5 级的最大值出现在 5 级爆炸（基数 155）的 1/4 点缀上——39 颗，
	 * 距本上限尚有 9 颗余量，故当前它不会真正生效。</p>
	 */
	private static final int MAX_ACCENT_PER_EMIT = 48;

	private ChargerWaveFx() {
	}

	// ==================== 风格档案：风格 → （颜色变换 + 点缀粒子） ====================

	/**
	 * 点缀粒子的<b>带参构造器</b>：按（该风格变换后的渲染色, 本次发射的生效尺度）现场构造粒子选项。
	 *
	 * <p>为什么必须能带参：原版粒子并非都是无参的 {@code SimpleParticleType}——
	 * 例：{@code ENTITY_EFFECT} 是 {@code ParticleType<ColorParticleOption>}（颜色是构造参数）、
	 * {@code BLOCK} 是 {@code ParticleType<BlockParticleOption>}（方块状态是构造参数）。
	 * 无参粒子用 {@link #of(ParticleOptions)} 包一层即可（每次返回同一个实例，零开销）。</p>
	 *
	 * <p>⚠ <b>2026-10-03 实地核对 1.21.1</b>：{@code SMALL_GUST} 在本版本是
	 * <b>{@code SimpleParticleType}</b>（<b>没有</b>颜色/尺寸参数，见 {@code ParticleTypes}），
	 * 需求里写的"带颜色 + 尺寸参数"在 1.21.1 不成立 ⇒ 风魔素只能无参使用它，
	 * "尺寸跟随波的尺度"因此落在<b>主粒子</b>上（染色尘埃的 {@code scale} 本来就是调用方基线
	 * 0.45 / 0.62）。本接口的 {@code scale} 入参保留：一旦某版本给出带尺寸参数的风粒子，
	 * 只改风那一行即可。</p>
	 */
	@FunctionalInterface
	public interface AccentParticle {

		/** 构造这一簇的粒子选项（{@code color} 已过风格的颜色变换）。 */
		ParticleOptions create(Vec3 color, float scale);

		/** 无参粒子（{@code SimpleParticleType}、以及构造参数与颜色/尺度无关的粒子）。 */
		static AccentParticle of(ParticleOptions particle) {
			return (color, scale) -> particle;
		}
	}

	/**
	 * 一种风格的全部视觉参数（不可变数据对象）。
	 *
	 * <p>把"风格"表达成数据而不是方法里的分支，是为了让新增/调整风格只改
	 * {@link #buildProfiles()} 里的一行，而不是在几处发送粒子方法里各加一段 if。</p>
	 *
	 * @param tint           主粒子颜色变换：输入波的渲染色（RGB 0-1），输出该风格的粒子色。
	 *                       必须是<b>纯函数</b>——同一输入永远给同一输出，且不得改动入参
	 *                       （{@code Vec3} 本身不可变）。
	 * @param accents        叠加在主粒子之上的点缀粒子构成；空列表 = 该风格不加点缀（如 NORMAL）。
	 * @param countOverride  主粒子<b>数量覆盖</b>；{@code null} = 用调用方基线（拖尾 6 / 绽放 30）。
	 *                       只作用于"波自己的观感"那几条路径（拖尾 / 绽放）：爆炸的密度是爆炸等级的
	 *                       算式（{@link #boomParticleCount(int)}，日志与实发共用同一算式），
	 *                       <b>不受</b>本覆盖影响。
	 * @param scaleOverride  主粒子<b>尺度覆盖</b>；{@code null} = 用调用方基线（拖尾 0.45 / 绽放 0.6）。
	 * @param spreadOverride 主粒子<b>散布覆盖</b>（各轴半径的绝对值）；{@code null} = 用调用方基线。
	 *                       存在的理由：主波链路的散布/速度是<b>调用方按运动方向现算</b>的，
	 *                       而"异"要逐字节等于环绕波那一套（其散布/速度是常量）⇒ 必须能钉死。
	 * @param speedOverride  主粒子<b>速度覆盖</b>；{@code null} = 用调用方基线。
	 */
	public record StyleProfile(UnaryOperator<Vec3> tint, List<Accent> accents,
		Integer countOverride, Float scaleOverride, Vec3 spreadOverride, Double speedOverride) {

		/** 生效颗数：档案覆盖优先，{@code null} = 调用方基数。 */
		public int effectiveCount(int callerCount) {
			return countOverride != null ? countOverride : callerCount;
		}

		/** 生效尺度：档案覆盖优先，{@code null} = 调用方基线。 */
		public float effectiveScale(float callerScale) {
			return scaleOverride != null ? scaleOverride : callerScale;
		}

		/** 生效散布：档案覆盖优先，{@code null} = 调用方基线。 */
		public Vec3 effectiveSpread(Vec3 callerSpread) {
			return spreadOverride != null ? spreadOverride : callerSpread;
		}

		/** 生效速度：档案覆盖优先，{@code null} = 调用方基线。 */
		public double effectiveSpeed(double callerSpeed) {
			return speedOverride != null ? speedOverride : callerSpeed;
		}
	}

	/**
	 * 点缀粒子档案：在风格主粒子之外额外发送的一类原版粒子。
	 *
	 * <p>数量按比例派生而非写死，这样同一个档案在"拖尾（6 颗）/绽放（30 颗）/
	 * 爆炸（1~5 级 55/80/105/130/155 颗）"三种量级下都能自动等比缩放——
	 * 实算方式与各档合计见类注释的"粒子预算"一节。唯一钉死颗数的是"异"（数量覆盖 12）；
	 * 它的两条点缀用"绝对散布"（见 {@link #spread(Vec3)}）对齐环绕波的常量。</p>
	 *
	 * @param particle       粒子构造器（见 {@link AccentParticle}；无参粒子用
	 *                       {@link AccentParticle#of(ParticleOptions)} 包一层）
	 * @param ratio          相对<b>生效颗数</b>的比例（0.25 = 每 4 颗主粒子配 1 颗点缀）；
	 *                       ⚠ 生效颗数 6 时比例 &lt; 1/12 会被四舍五入成 0 颗（不报错、不显示）
	 * @param spreadScale    相对主粒子散布半径的倍率（&gt;1 更散、&lt;1 更聚）；
	 *                       {@link #spread(Vec3)} 给了绝对散布时本项被忽略
	 * @param speedScale     相对主粒子初速的倍率（火花要快、烟雾要慢）
	 * @param chance         每次发射时"这一簇发不发"的概率（1.0 = 必发）。
	 *                       <b>节流就用它</b>：想要"偶发的一两颗"不能靠调小 {@code ratio}
	 *                       （会变成一颗都不发），而应"比例 ≥ 1/12 保证每发 1 颗 + 本概率决定发不发"
	 * @param mainWaveOnly   {@code true} = 只挂主波：环绕波即使同风格也不发这一簇
	 *                       （作者 2026-10-03 对雷闪光的建议"低概率 + 只挂主波"就落在这里）
	 * @param spreadOverride 本簇的<b>绝对</b>散布（各轴半径）；{@code null} = 按
	 *                       {@code 生效散布 × spreadScale} 缩放。用于把某一簇钉到既有常量上
	 *                       （"异"的 END_ROD / 青焰对齐 {@code sendOrbitTrail}）
	 */
	public record Accent(AccentParticle particle, double ratio, double spreadScale, double speedScale,
		double chance, boolean mainWaveOnly, Vec3 spreadOverride) {

		/** 常规点缀：必发、散布/速度按本次发射基线等比缩放、主波与环绕波都发、无绝对散布覆盖。 */
		public static Accent of(ParticleOptions particle, double ratio, double spreadScale, double speedScale) {
			return new Accent(AccentParticle.of(particle), ratio, spreadScale, speedScale, 1.0D, false, null);
		}

		/** 带参点缀：粒子由（渲染色, 生效尺度）现场构造（如 {@code ENTITY_EFFECT} 吃药水黄绿）。 */
		public static Accent of(AccentParticle particle, double ratio, double spreadScale, double speedScale) {
			return new Accent(particle, ratio, spreadScale, speedScale, 1.0D, false, null);
		}

		/**
		 * 偶发：每次发射以 {@code chance} 概率发出这一簇（概率只决定发不发，颗数照
		 * {@code round(生效颗数 × ratio)} 发）。
		 */
		public Accent rare(double chance) {
			return new Accent(particle, ratio, spreadScale, speedScale, chance, mainWaveOnly, spreadOverride);
		}

		/** 只挂主波（环绕波不发这一簇）。 */
		public Accent mainOnly() {
			return new Accent(particle, ratio, spreadScale, speedScale, chance, true, spreadOverride);
		}

		/** 绝对散布（各轴半径的绝对值，忽略 {@code spreadScale} 与调用方散布）。 */
		public Accent spread(Vec3 absolute) {
			return new Accent(particle, ratio, spreadScale, speedScale, chance, mainWaveOnly, absolute);
		}
	}

	/**
	 * <b>攻击波默认魔素「火」的档案 = 伤害风格的同一份实例</b>（2026-10-03 需求 coe-ess）。
	 *
	 * <p>为什么共用实例而不是在 {@code buildProfiles()} 里再写一遍同样的字面量：
	 * 作者口径"火魔素与现状一模一样"⇒ 两份字面量只要能各自漂移，早晚会漂移；
	 * 这里是同一对象，<b>结构上不可能不一致</b>。关卡
	 * {@code wave-essence-fire-is-damage} 守着这条（且它在故意改坏时变红）。</p>
	 *
	 * <p>⚠ <b>必须声明在 {@link #PROFILES} 之前</b>：{@code PROFILES} 的静态初始化器会调用
	 * {@code buildProfiles()}，若本字段排在它后面，读到的是 {@code null}
	 * ⇒ DAMAGE/FIRE 双双落空并静默回落 NORMAL（不报错、观感全变）。</p>
	 */
	private static final StyleProfile FIRE_PROFILE = new StyleProfile(ChargerWaveFx::heatTint, List.of(
		// 暴击星芒：快而散，数量占主粒子的 1/4 —— 打击瞬间的"脆"反馈
		Accent.of(ParticleTypes.CRIT, 0.25, 1.1, 1.5),
		// 尾焰：慢而聚，数量占 1/8 —— 原为"DAMAGE_INDICATOR（暗红心形）"，
		// 用户 2026-09 实测要求改成火焰，类型集中在 DAMAGE_ACCENT_PARTICLE 一处
		Accent.of(DAMAGE_ACCENT_PARTICLE, 0.125, 1.0, 0.8)),
		null, null, null, null);

	/**
	 * <b>唯一的"风格 → 粒子构成"映射表</b>（只读 {@link EnumMap}，按枚举查表 O(1)）。
	 *
	 * <p>新增一种风格：先由用户确认扩展 {@link WaveTrailStyle} 枚举，然后<b>只在本方法里加一行</b>
	 * {@code profiles.put(...)}——颜色算法可复用下面的纯函数，也可新写一个纯函数。
	 * 未登记的枚举值会由 {@link #profile(WaveTrailStyle)} 回落为 NORMAL（不崩、不改玩法）。</p>
	 */
	private static final Map<WaveTrailStyle, StyleProfile> PROFILES = buildProfiles();

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
	 *             全部<b>引用环绕波那组常量</b>（{@link #ORBIT_TRAIL_COUNT} /
	 *             {@link #ORBIT_TRAIL_SCALE} / {@link #ORBIT_TRAIL_SPREAD} /
	 *             {@link #ORBIT_TRAIL_SPEED}），两条点缀同样引用
	 *             {@link #ORBIT_FLAG_END_ROD} / {@link #ORBIT_FLAG_SOUL_FIRE} /
	 *             {@link #ORBIT_FLAG_SPREAD} / {@link #ORBIT_SOUL_SPREAD}——<b>不复制字面量</b>，
	 *             于是"异"与环绕波的结构性一致（改常量两处同时改）。</li>
	 *         <li><b>环面留痕桩不进 ARCANE</b>：它是"这枚波在环绕"这个几何事实专属，主波没有环平面。</li>
	 *       </ul>
	 *   </li>
	 * </ul>
	 */
	private static Map<WaveTrailStyle, StyleProfile> buildProfiles() {
		EnumMap<WaveTrailStyle, StyleProfile> profiles = new EnumMap<>(WaveTrailStyle.class);
		profiles.put(WaveTrailStyle.NORMAL,
			new StyleProfile(UnaryOperator.identity(), List.of(), null, null, null, null));
		profiles.put(WaveTrailStyle.MECHANICAL,
			new StyleProfile(ChargerWaveFx::metalTint, List.of(
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
			new StyleProfile(ChargerWaveFx::waterTint, List.of(
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
			new StyleProfile(ChargerWaveFx::earthTint, List.of(
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
			new StyleProfile(ChargerWaveFx::windTint, List.of(
				// 阵风：⚠ 1.21.1 的 SMALL_GUST 是 SimpleParticleType（无颜色/尺寸参数），
				// 只能无参使用（见 AccentParticle 的说明）；"尺寸跟随波的尺度"落在主粒子上
				Accent.of(ParticleTypes.SMALL_GUST, 0.25, 1.40, 1.20)),
				null, null, null, null));
		profiles.put(WaveTrailStyle.ICE,
			new StyleProfile(ChargerWaveFx::iceTint, List.of(
				// 细雪：主点缀
				Accent.of(ParticleTypes.SNOWFLAKE, 0.25, 1.10, 0.70),
				// 雪块碎屑
				Accent.of(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState()),
					0.125, 1.00, 0.80),
				// 雪球碎屑：偶发
				Accent.of(ParticleTypes.ITEM_SNOWBALL, 0.1666, 1.30, 1.30).rare(0.35D)),
				null, null, null, null));
		profiles.put(WaveTrailStyle.LIGHTNING,
			new StyleProfile(ChargerWaveFx::lightningTint, List.of(
				// 电火花：快、散 —— 雷的主体点缀
				Accent.of(ParticleTypes.ELECTRIC_SPARK, 0.25, 1.15, 1.60),
				// 强闪光：低概率 + 只挂主波（完整公式与常量见 LIGHTNING_FLASH_RATIO / LIGHTNING_FLASH_CHANCE）；
				// 散布 0.6 倍、速度 0 ⇒ 一颗停在波位置上的闪光，不跟着飞出去
				Accent.of(ParticleTypes.FLASH, LIGHTNING_FLASH_RATIO, 0.60, 0.0D)
					.rare(LIGHTNING_FLASH_CHANCE).mainOnly()),
				null, null, null, null));
		profiles.put(WaveTrailStyle.POISON,
			new StyleProfile(ChargerWaveFx::poisonTint, List.of(
				// 药水粒子：带参构造，颜色取药水系黄绿（见 POISON_ACCENT_COLOR）
				Accent.of(ChargerWaveFx::poisonEffectParticle, 0.25, 1.05, 0.50),
				// 黏液：更"黏"的观感，偶发（需求里写作"可选"）
				Accent.of(ParticleTypes.ITEM_SLIME, 0.1666, 0.90, 0.35).rare(0.30D)),
				null, null, null, null));
		profiles.put(WaveTrailStyle.ARCANE,
			new StyleProfile(UnaryOperator.identity(), List.of(
				// 与 sendOrbitTrail 逐项对齐（全部引用那组常量，不复制字面量）：
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
	 * 取某种风格的档案（映射表的唯一读取口）。
	 *
	 * @param style 波型暴露的风格；{@code null} 或未登记的值一律回落 {@link WaveTrailStyle#NORMAL}
	 */
	public static StyleProfile profile(WaveTrailStyle style) {
		StyleProfile profile = style == null ? null : PROFILES.get(style);
		return profile != null ? profile : PROFILES.get(WaveTrailStyle.NORMAL);
	}

	/**
	 * 该风格<b>是否在映射表里登记过</b>（{@link #PROFILES} 的成员判定）。
	 *
	 * <p><b>为什么魔素解析需要它</b>（2026-10-03 需求 coe-ess 批 3）：口径是
	 * "有魔素<b>且已登记</b>就按魔素，否则继承波型风格"。{@link #profile(WaveTrailStyle)} 对
	 * 未登记的值会<b>静默回落 NORMAL</b>（不崩、不改玩法）—— 若不先问这一句，
	 * "设了一个没登记的魔素"会表现为"波突然变成裸染色尘埃"，而不是"回落波型风格"。
	 * 这里只回答"表里有没有"，不做任何回落。</p>
	 */
	public static boolean isRegistered(WaveTrailStyle style) {
		return style != null && PROFILES.containsKey(style);
	}

	/**
	 * 按风格变换粒子颜色（纯函数入口，供需要自行取色的调用方使用）。
	 *
	 * @param style     风格
	 * @param baseColor 波的渲染色（RGB 0-1）
	 * @return 该风格下的粒子色；NORMAL 原样返回入参
	 */
	public static Vec3 styleColor(WaveTrailStyle style, Vec3 baseColor) {
		return profile(style).tint()
			.apply(baseColor == null ? Vec3.ZERO : baseColor);
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

	/** 点缀粒子数量 = 生效颗数 × 比例（四舍五入，封顶 {@link #MAX_ACCENT_PER_EMIT}）。 */
	private static int accentCount(int baseCount, Accent accent) {
		if (baseCount <= 0)
			return 0;
		return Math.min(MAX_ACCENT_PER_EMIT, (int) Math.round(baseCount * accent.ratio()));
	}

	/**
	 * 这一簇本 tick 要不要发（{@link Accent#chance() 概率}节流）。
	 *
	 * <p>概率 <b>只决定"发不发"</b>，颗数仍由 {@link #accentCount(int, Accent)} 定——
	 * 于是"偶发的一两颗"不会被四舍五入成 0 颗（那正是雷闪光要解决的问题）。</p>
	 */
	private static boolean accentRolls(RandomSource random, Accent accent) {
		return accent.chance() >= 1.0D || random.nextDouble() < accent.chance();
	}

	/**
	 * 这一簇是否允许出现在本次发射上（{@link Accent#mainWaveOnly()} = 只挂主波）。
	 *
	 * <p><b>2026-10-03 需求 coe-ess 批 3 起 {@code orbiter} 真的会被传 {@code true}</b>：
	 * 批 1+2 只在档案里声明了这个旗标（调用点一律 {@code false}），批 3 把环绕波并进
	 * <b>同一条风格链路</b>（主波与环绕波都走 {@link #sendTrail} / {@link #addTrailParticles}），
	 * 于是"这枚波在环绕"这件事必须随调用一并带进来 —— 否则雷魔素的强闪光
	 * （{@code FLASH}，作者要求"低概率 + 只挂主波"）会跟着环绕波一起刷。</p>
	 *
	 * @param orbiter 本次发射是否来自<b>环绕波</b>（{@code AbstractChargerWaveEntity#isOrbiting()}）
	 */
	private static boolean accentAllowed(Accent accent, boolean orbiter) {
		return !accent.mainWaveOnly() || !orbiter;
	}

	/**
	 * 一簇点缀的<b>散布</b>：给了绝对散布就用绝对的（{@link Accent#spread(Vec3)}，
	 * "异"用它钉到环绕波的常量上），否则按本次发射的生效散布等比缩放。
	 */
	private static Vec3 resolveAccentSpread(Accent accent, Vec3 effectiveSpread) {
		return accent.spreadOverride() != null ? accent.spreadOverride()
			: effectiveSpread.scale(accent.spreadScale());
	}

	// ==================== 粒子构造（风格感知） ====================

	/**
	 * 风格化的能量波拖尾粒子：原版染色粒子（{@link DustParticleOptions}），颜色经该风格的颜色变换。
	 *
	 * <p>只返回"主粒子"——点缀粒子（电火花/暴击/气泡）不是染色粒子，无法塞进同一个
	 * {@link ParticleOptions}，故由 {@link #sendTrail} / {@link #addTrailParticles} 一并发射。</p>
	 *
	 * @param style     波型风格（null / 未登记 → NORMAL）
	 * @param baseColor 波的渲染色（RGB 0-1）
	 * @param scale     粒子尺寸（原版染色粒子的 scale；档案有尺度覆盖时以覆盖为准）
	 */
	public static ParticleOptions waveParticle(WaveTrailStyle style, Vec3 baseColor, float scale) {
		StyleProfile profile = profile(style);
		return particleFor(profile, baseColor, profile.effectiveScale(scale));
	}

	/**
	 * 由已查好的档案构粒子（内部用：一次发射内只查一次表，避免逐颗重复查表）。
	 *
	 * <p>尺度由调用方先过 {@link StyleProfile#effectiveScale(float)}——本方法只管"染色 + 构造"。</p>
	 */
	private static ParticleOptions particleFor(StyleProfile profile, Vec3 baseColor, float scale) {
		Vec3 color = profile.tint()
			.apply(baseColor == null ? Vec3.ZERO : baseColor);
		return new DustParticleOptions(
			new Vector3f((float) color.x, (float) color.y, (float) color.z), scale);
	}

	/** 能量波粒子数据（向后兼容重载，行为等同改造前）：NORMAL 风格的染色粒子。 */
	static ParticleOptions waveParticle(Vec3 color, float scale) {
		return waveParticle(WaveTrailStyle.NORMAL, color, scale);
	}

	/**
	 * 服务端拖尾：按风格发射一批主粒子，并追加该风格的点缀粒子。
	 *
	 * <p>调用方把原本的 {@code server.sendParticles(ChargerWaveFx.waveParticle(color, scale), ...)}
	 * 换成这一次调用即可同时拿到"主粒子 + 点缀"，无需自己遍历风格。</p>
	 *
	 * <p>数量/尺度/散布/速度都可以被该风格的档案<b>覆盖</b>（见 {@link StyleProfile}）：
	 * 现有三个风格与七种魔素都不覆盖（逐字节等于调用方基线），只有"异"覆盖成环绕波那一套。</p>
	 *
	 * @param server    服务端世界
	 * @param pos       发射位置（波当前位置）
	 * @param style     风格
	 * @param baseColor 波的渲染色
	 * @param scale     主粒子尺寸
	 * @param count     主粒子数量
	 * @param spread    主粒子散布半径（各轴）
	 * @param speed     主粒子速度系数
	 */
	public static void sendTrail(ServerLevel server, Vec3 pos, WaveTrailStyle style, Vec3 baseColor,
		float scale, int count, Vec3 spread, double speed) {
		sendTrail(server, pos, style, baseColor, scale, count, spread, speed, false);
	}

	/**
	 * 服务端拖尾（带 {@code orbiter} 旗标版）：本波的粒子构成与 {@link #sendTrail(ServerLevel, Vec3,
	 * WaveTrailStyle, Vec3, float, int, Vec3, double)} 逐字相同，只多回答"这是不是环绕波"——
	 * 它只影响 {@link Accent#mainWaveOnly()} 的点缀（见 {@link #accentAllowed}），
	 * <b>不影响用哪个风格档案</b>（档案由 style 决定）。
	 *
	 * @param orbiter {@code true} = 本次发射来自环绕波（主波与环绕波走同一条路）
	 */
	public static void sendTrail(ServerLevel server, Vec3 pos, WaveTrailStyle style, Vec3 baseColor,
		float scale, int count, Vec3 spread, double speed, boolean orbiter) {
		StyleProfile profile = profile(style);
		int effectiveCount = profile.effectiveCount(count);
		float effectiveScale = profile.effectiveScale(scale);
		Vec3 effectiveSpread = profile.effectiveSpread(spread);
		double effectiveSpeed = profile.effectiveSpeed(speed);
		server.sendParticles(particleFor(profile, baseColor, effectiveScale), pos.x, pos.y, pos.z,
			effectiveCount, effectiveSpread.x, effectiveSpread.y, effectiveSpread.z, effectiveSpeed);
		RandomSource random = server.random;
		for (Accent accent : profile.accents()) {
			if (!accentAllowed(accent, orbiter))
				continue;
			int extras = accentCount(effectiveCount, accent);
			if (extras <= 0 || !accentRolls(random, accent))
				continue;
			Vec3 accentSpread = resolveAccentSpread(accent, effectiveSpread);
			server.sendParticles(accent.particle().create(baseColor, effectiveScale), pos.x, pos.y, pos.z,
				extras, accentSpread.x, accentSpread.y, accentSpread.z,
				effectiveSpeed * accent.speedScale());
		}
	}

	/**
	 * 客户端（Ponder 思索者场景等）拖尾：逐粒子 {@code addParticle}，主粒子 + 风格点缀。
	 *
	 * <p>与 {@link #sendTrail} 的区别只在发送通道（本方法不经过网络包），故参数与粒子构成一一对应；
	 * 点缀粒子在此额外叠加一点随机抖动，让火花/碎屑看起来是"崩出去"的而不是沿波轴平移。
	 * （本方法<b>从来不用"散布"</b>——它的初速由 {@code velocity} 给，故档案的
	 * {@code spreadOverride} 在客户端这条路上不生效，只有数量/尺度/概率/取色生效。）</p>
	 *
	 * @param level     客户端世界（PonderLevel 等的 addParticle 已实现）
	 * @param pos       发射位置
	 * @param style     风格
	 * @param baseColor 波的渲染色
	 * @param scale     主粒子尺寸
	 * @param count     主粒子数量
	 * @param velocity  主粒子初速度（对应 {@code addParticle} 的 vx/vy/vz）
	 */
	public static void addTrailParticles(Level level, Vec3 pos, WaveTrailStyle style, Vec3 baseColor,
		float scale, int count, Vec3 velocity) {
		addTrailParticles(level, pos, style, baseColor, scale, count, velocity, false);
	}

	/**
	 * 客户端（Ponder 思索者场景等）拖尾（带 {@code orbiter} 旗标版）：粒子构成与
	 * {@link #addTrailParticles(Level, Vec3, WaveTrailStyle, Vec3, float, int, Vec3)} 逐字相同，
	 * 只多回答"这是不是环绕波"（只影响 {@link Accent#mainWaveOnly()} 的点缀）。
	 *
	 * <p><b>为什么两条路都要带这个旗标</b>：需求 coe-ess 的陷阱清单第 8 条 ——
	 * Ponder 场景走的是本方法（客户端 {@code addParticle}），只改服务端那条等于"雷闪光只挂主波"
	 * 在 Ponder 里失效。</p>
	 */
	public static void addTrailParticles(Level level, Vec3 pos, WaveTrailStyle style, Vec3 baseColor,
		float scale, int count, Vec3 velocity, boolean orbiter) {
		StyleProfile profile = profile(style);
		int effectiveCount = profile.effectiveCount(count);
		float effectiveScale = profile.effectiveScale(scale);
		ParticleOptions particle = particleFor(profile, baseColor, effectiveScale);
		for (int i = 0; i < effectiveCount; i++)
			level.addParticle(particle, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
		RandomSource random = level.random;
		for (Accent accent : profile.accents()) {
			if (!accentAllowed(accent, orbiter))
				continue;
			int extras = accentCount(effectiveCount, accent);
			if (extras <= 0 || !accentRolls(random, accent))
				continue;
			ParticleOptions accentParticle = accent.particle().create(baseColor, effectiveScale);
			double speedScale = accent.speedScale();
			for (int i = 0; i < extras; i++) {
				level.addParticle(accentParticle, pos.x, pos.y, pos.z,
					(velocity.x + random.nextGaussian() * 0.03) * speedScale,
					(velocity.y + random.nextGaussian() * 0.03) * speedScale,
					(velocity.z + random.nextGaussian() * 0.03) * speedScale);
			}
		}
	}

	// ==================== 绽放 / 爆炸 ====================

	/**
	 * 服务端命中绽放：对应颜色向外扩散的染色粒子（大散布 + 速度，近似球面扩散）
	 * + 加工完成音效（紫水晶共鸣）。
	 *
	 * @param level 波所在世界（服务端）
	 * @param pos   绽放中心（世界坐标）
	 * @param color 粒子颜色（RGB 0-1）
	 */
	public static void burst(Level level, Vec3 pos, Vec3 color) {
		burst(level, pos, WaveTrailStyle.NORMAL, color);
	}

	/**
	 * 服务端命中绽放（风格化）：主粒子颜色按风格变换，并按风格追加点缀粒子。
	 *
	 * <p>风格差异体现在"这次命中代表什么"：机械风格炸开的是电火花与排气烟（读作"机器被加工"），
	 * 伤害风格炸开的是暴击星芒与尾焰（读作"挨了一击"）。音效保持原有的紫水晶共鸣，不改玩法。</p>
	 *
	 * @param level     波所在世界（服务端；非服务端静默返回）
	 * @param pos       绽放中心（世界坐标）
	 * @param style     风格
	 * @param baseColor 波的渲染色（RGB 0-1）
	 */
	public static void burst(Level level, Vec3 pos, WaveTrailStyle style, Vec3 baseColor) {
		if (!(level instanceof ServerLevel server))
			return;
		StyleProfile profile = profile(style);
		int effectiveCount = profile.effectiveCount(BURST_COUNT);
		float effectiveScale = profile.effectiveScale(0.6f);
		Vec3 spread = new Vec3(0.5, 0.5, 0.5);
		server.sendParticles(particleFor(profile, baseColor, effectiveScale), pos.x, pos.y, pos.z,
			effectiveCount, spread.x, spread.y, spread.z, 0.3);
		RandomSource random = server.random;
		for (Accent accent : profile.accents()) {
			if (!accentAllowed(accent, false))
				continue;
			int extras = accentCount(effectiveCount, accent);
			if (extras <= 0 || !accentRolls(random, accent))
				continue;
			Vec3 accentSpread = resolveAccentSpread(accent, spread);
			server.sendParticles(accent.particle().create(baseColor, effectiveScale), pos.x, pos.y, pos.z,
				extras, accentSpread.x, accentSpread.y, accentSpread.z, 0.3 * accent.speedScale());
		}
		server.playSound(null, pos.x, pos.y, pos.z, SoundEvents.AMETHYST_BLOCK_RESONATE,
			SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	/** 客户端球面均匀扩散绽放（对应颜色，实体消散时补充）。 */
	static void burstParticles(Level level, Vec3 pos, Vec3 color) {
		burstParticles(level, pos, WaveTrailStyle.NORMAL, color);
	}

	/**
	 * 客户端球面均匀扩散绽放（风格化）：主粒子颜色按风格变换，并按风格追加点缀粒子。
	 *
	 * <p>主粒子仍是"高斯方向归一化后 ×0.35"的球面扩散（与无风格重载逐字相同）；
	 * 点缀粒子沿用同一方向再乘各自的速度倍率，故火花/碎屑与主粒子同向但更快或更慢，
	 * 视觉上像"球面炸开 + 溅射"。（与 {@link #addTrailParticles} 同理：本方法<b>从来不用"散布"</b>，
	 * 方向由逐个粒子的速度向量给，故档案的 {@code spreadOverride} 在这条客户端路上不生效——
	 * 生效的是数量/尺度/概率/取色。）</p>
	 *
	 * @param level     客户端世界
	 * @param pos       绽放中心
	 * @param style     风格
	 * @param baseColor 波的渲染色（RGB 0-1）
	 */
	public static void burstParticles(Level level, Vec3 pos, WaveTrailStyle style, Vec3 baseColor) {
		StyleProfile profile = profile(style);
		int effectiveCount = profile.effectiveCount(BURST_COUNT);
		float effectiveScale = profile.effectiveScale(0.5f);
		ParticleOptions particle = particleFor(profile, baseColor, effectiveScale);
		RandomSource random = level.random;
		for (int i = 0; i < effectiveCount; i++) {
			Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian())
				.normalize();
			level.addParticle(particle, pos.x, pos.y, pos.z,
				dir.x * 0.35, dir.y * 0.35, dir.z * 0.35);
		}
		for (Accent accent : profile.accents()) {
			if (!accentAllowed(accent, false))
				continue;
			int extras = accentCount(effectiveCount, accent);
			if (extras <= 0 || !accentRolls(random, accent))
				continue;
			ParticleOptions accentParticle = accent.particle().create(baseColor, effectiveScale);
			double speed = 0.35 * accent.speedScale();
			for (int i = 0; i < extras; i++) {
				Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian())
					.normalize();
				level.addParticle(accentParticle, pos.x, pos.y, pos.z,
					dir.x * speed, dir.y * speed, dir.z * speed);
			}
		}
	}

	/**
	 * 触发一次范围能量爆炸（不破坏地形）：
	 * <ul>
	 *   <li><b>爆炸半径 = 爆炸等级（格）</b>：水平正方形范围半径（1→3×3、2→5×5、3→7×7、
	 *       4→9×9、5→11×11）——4 级 ε 波爆炸 4 格、5 级 ω 波爆炸 5 格；</li>
	 *   <li>区域内生物受该等级撞击伤害（查 {@link net.minecraft.world.damagesource.DamageSource} 前
	 *       见 {@link #damageForLevel}：4/6/8/10/12）；</li>
	 *   <li>区域内掉落物 / 置物台物品按该等级直接充能加工；</li>
	 *   <li><b>≥3 级爆炸</b>额外给范围内强化避雷针 +1 γ 充能；</li>
	 *   <li>密集粒子扩散（比撞墙 30 个更密）+ 爆炸音效。</li>
	 * </ul>
	 *
	 * @param level     波所在世界
	 * @param source    伤害源（能量波实体，作为 indirectMagic 攻击者）
	 * @param center    爆炸中心（世界坐标）
	 * @param color     主粒子颜色（可选第二色混合，null 则单色）
	 * @param color2    第二粒子颜色（null = 单色）
	 * @param boomLevel 爆炸等级（1~5；两波碰撞取较低等级）
	 */
	public static void triggerBoom(Level level, Entity source, Vec3 center, Vec3 color, Vec3 color2, int boomLevel) {
		triggerBoom(level, source, center, WaveTrailStyle.NORMAL, color, color2, boomLevel);
	}

	/**
	 * 爆炸主粒子数（<b>唯一算式</b>：{@link #triggerBoom} 与轨迹日志共用，避免"日志里报的数"与
	 * "真实发出的数"两处各写一遍而分叉）。
	 *
	 * @param boomLevel 爆炸等级（1~5）
	 * @return 主粒子数量（现表 55 / 80 / 105 / 130 / 155）
	 */
	public static int boomParticleCount(int boomLevel) {
		return 30 + boomLevel * 25;
	}

	/**
	 * 触发一次范围能量爆炸（风格化）：玩法与上面的无风格重载完全一致，
	 * 仅把两次粒子发送（主色 + 可选第二色）与点缀粒子按风格着色/补粒子。
	 *
	 * <p>爆炸取"两波碰撞"的语义：第二色是对方波的颜色，两者用<b>同一风格</b>变换，
	 * 于是"机械波撞机械波"炸出的是钢灰/黄铜火花雨，"攻击波撞攻击波"炸出的是红橙星芒雨。</p>
	 *
	 * @param level     波所在世界
	 * @param source    伤害源（能量波实体，作为 indirectMagic 攻击者）
	 * @param center    爆炸中心（世界坐标）
	 * @param style     风格（NORMAL = 改造前行为）
	 * @param color     主粒子颜色（波的渲染色）
	 * @param color2    第二粒子颜色（对方波的渲染色，null = 单色）
	 * @param boomLevel 爆炸等级（1~5）
	 */
	public static void triggerBoom(Level level, Entity source, Vec3 center, WaveTrailStyle style,
		Vec3 color, Vec3 color2, int boomLevel) {
		if (level instanceof ServerLevel server) {
			StyleProfile profile = profile(style);
			// 密集球面扩散粒子（等级越高越密）
			int count = boomParticleCount(boomLevel); // 55 / 80 / 105 / 130 / 155 个（1~5 级）
			// ⚠ 爆炸的密度**不受档案的数量/尺度覆盖**：它是爆炸等级的算式（boomParticleCount，
			// 轨迹日志与真实发送共用同一算式），改了会让日志里的数与实发数分叉。
			server.sendParticles(particleFor(profile, color, 0.7f), center.x, center.y, center.z, count,
				1.2, 1.2, 1.2, 0.15);
			if (color2 != null) {
				// 混合第二色，增强视觉层次（同为该风格着色）
				server.sendParticles(particleFor(profile, color2, 0.5f), center.x, center.y, center.z, count / 2,
					1.0, 1.0, 1.0, 0.12);
			}
			// 风格点缀：数量随爆炸密度等比放大（1 级 55 → 14+7=21 颗，5 级 155 → 39+19=58 颗）
			RandomSource random = server.random;
			Vec3 spread = new Vec3(1.2, 1.2, 1.2);
			for (Accent accent : profile.accents()) {
				if (!accentAllowed(accent, false))
					continue;
				int extras = accentCount(count, accent);
				if (extras <= 0 || !accentRolls(random, accent))
					continue;
				Vec3 accentSpread = resolveAccentSpread(accent, spread);
				server.sendParticles(accent.particle().create(color, 0.7f), center.x, center.y, center.z,
					extras, accentSpread.x, accentSpread.y, accentSpread.z,
					0.15 * accent.speedScale());
			}
			// 能量冲击音效（不破坏地形，仅声光效果）
			server.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE,
				SoundSource.BLOCKS, 1.0F, 1.0F);
		}

		// 水平正方形范围（半径 = 爆炸等级），按等级处理生物伤害与物品加工
		double r = boomLevel;
		AABB area = new AABB(center.x - r, center.y - 0.5, center.z - r,
			center.x + r, center.y + 0.5, center.z + r);
		ChargerWaveProcessor boomProcessor = new ChargerWaveProcessor(level, boomLevel);

		// 范围内生物：受到该等级波对应的撞击伤害（α 4 / β 6 / γ 8 / ε 10 / ω 12）
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive())) {
			if (!(target instanceof Player player) || !player.isCreative()) {
				target.hurt(level.damageSources()
					.indirectMagic(source, null), damageForLevel(boomLevel));
			}
		}

		// 范围内掉落物：按爆炸等级直接加工
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, e -> e.isAlive())) {
			boomProcessor.processItemEntity(item);
		}

		// 范围内方块（置物台/工作台等有物品槽者）：按爆炸等级直接加工
		int minX = Mth.floor(center.x - r);
		int maxX = Mth.floor(center.x + r);
		int minZ = Mth.floor(center.z - r);
		int maxZ = Mth.floor(center.z + r);
		int y = Mth.floor(center.y);
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				BlockPos pos = new BlockPos(x, y, z);
				// γ 级能量加工（等级 ≥3）：范围内强化避雷针获得 1 次 γ 充能
				if (boomLevel >= 3
					&& level.getBlockEntity(pos) instanceof ReinforcedLightningRodBlockEntity rod) {
					rod.onGammaWaveHit();
				}
				IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
				if (handler != null)
					boomProcessor.processBlockHandler(handler, pos);
			}
		}
	}

	/** 按等级取命中伤害（α 4 / β 6 / γ 8 / ε 10 / ω 12）。 */
	static float damageForLevel(int level) {
		return WaveLevels.damage(level);
	}

	// ==================================================================================
	// 环绕波粒子（2026-10-02 作者裁定："即使有环绕波生成，过于不明显也是 bug"；
	// 2026-10-03 需求 coe-ess 批 3 按【风格层 / 几何专属层】重新切分）
	//
	// 环绕波的实体类型、渲染器、波型都与主波逐字相同（红线：不新增实体类型/贴图/模型），
	// 视觉 100% 靠粒子（EmptyEntityRenderer）。它在 2026-10-03 之前是"另一整套粒子"
	// （`if (isOrbiting()) sendOrbitTrail(...) else sendTrail(...)` 的二选一），
	// 现在按需求 §3.4 切成两层：
	//   A · 风格层（由魔素决定，主波与环绕波走同一条路 = 同一个 {@link #sendTrail} /
	//       {@link #addTrailParticles} 调用）：主体尘埃 = 调用方基线（主波 6 颗 / 0.45，
	//       环绕波 12 颗 / 0.62）+ 该魔素档案的点缀。环绕波原先那套
	//       "12 颗 / 0.62 + END_ROD + 青焰"已<b>整份搬进</b> {@link WaveTrailStyle#ARCANE}
	//       的档案（"异"魔素 = 作者说的"伴随波现在的效果"）；档案引用本处常量，不复制字面量。
	//   B · 几何专属层（只属于"这枚波在环绕"这个几何事实，与魔素无关，也不进任何风格档案）：
	//       环面留痕桩（{@link #sendOrbitMarks}）+ 出生整圈标记（{@link #burstOrbitSpawn}）。
	//       为什么不进档案：主波没有环平面，把桩点塞进"异"档案要么崩、要么在平飞路径上
	//       画出一圈没有意义的点（需求 §3.4 的陷阱 3）。
	//
	// 三个时刻各来一簇（作者要求：出生 / 命中 / 父波消散一起收尾）：
	//   出生 = {@link #burstOrbitSpawn}（几何层）、命中与消散 = 既有的 {@link #burst}
	//   （吃的是 {@code AbstractChargerWaveEntity#trailStyle()}，即该波自己生效的魔素），
	//   三处都走本类，不新增任何发送通道。
	// ==================================================================================

	/** 环绕波主体尘埃：每 tick 颗数（主波拖尾是 6）——环绕波走风格层时的<b>调用方基线</b>。 */
	public static final int ORBIT_TRAIL_COUNT = 12;

	/** 环绕波主体尘埃：粒子尺度（主波拖尾是 0.45f）——环绕波走风格层时的<b>调用方基线</b>。 */
	public static final float ORBIT_TRAIL_SCALE = 0.62f;

	/**
	 * 环绕波主体尘埃：散布半径（各轴）——"异"魔素的档案<b>引用本常量</b>
	 * （主波那条链路的散布是调用方按运动方向现算的，故必须能钉死才能逐字节对齐）。
	 */
	public static final double ORBIT_TRAIL_SPREAD = 0.12;

	/** 环绕波主体尘埃：速度系数（"异"魔素的档案引用本常量）。 */
	public static final double ORBIT_TRAIL_SPEED = 0.01;

	/** "异"魔素的标志粒子 END_ROD：每 tick 颗数（白亮"信标"，与主波的染色尘埃完全两样）。 */
	public static final int ORBIT_FLAG_END_ROD = 4;

	/** "异"魔素的标志粒子 SOUL_FIRE_FLAME：每 tick 颗数（青焰，任何波级的冷暖反差都足够大）。 */
	public static final int ORBIT_FLAG_SOUL_FIRE = 2;

	/** "异"魔素的标志粒子 END_ROD：散布半径（各轴）。 */
	public static final double ORBIT_FLAG_SPREAD = 0.10;

	/** "异"魔素的标志粒子 END_ROD：速度系数。 */
	public static final double ORBIT_FLAG_SPEED = 0.02;

	/** "异"魔素的标志粒子 SOUL_FIRE_FLAME：散布半径（各向同性；见 {@link WaveTrailStyle#ARCANE}）。 */
	public static final double ORBIT_SOUL_SPREAD = 0.02;

	/** "异"魔素的标志粒子 SOUL_FIRE_FLAME：速度系数。 */
	public static final double ORBIT_SOUL_SPEED = 0.005;

	/**
	 * 环面留痕：每几 tick 补一圈静态桩点（越小圈越"实"）。
	 *
	 * <p>取 3 而不是更大值：原版染色尘埃的寿命是写死的一小段（本仓既有构造只有
	 * {@code DustParticleOptions(Vector3f, float)} 两个参数，没有寿命入参），所以"圈"是由
	 * <b>残留桩点</b>拼出来的——间隔一大就只剩几个孤点，间隔 3 tick 时同一时刻约有两圈桩点在
	 * 场上，圆环是连续的。代价是常数颗（{@value #ORBIT_MARK_COUNT} 颗 / 3 tick）。</p>
	 */
	public static final int ORBIT_MARK_INTERVAL_TICKS = 3;

	/** 环面留痕：一圈几颗桩点（沿环平面均分角度）。 */
	public static final int ORBIT_MARK_COUNT = 8;

	/** 环面留痕桩之色：电青（刻意不用任何波级色——它标记的是"轨道"这个几何事实）。 */
	private static final Vec3 ORBIT_MARK_COLOR = new Vec3(0.35, 0.95, 1.0);

	/** 环面留痕桩的粒子尺度（比尘埃略小：桩点是辅助，不抢主体）。 */
	private static final float ORBIT_MARK_SCALE = 0.35f;

	/** 出生簇：主尘埃颗数（比绽放 30 颗小，只在出生点炸一小团）。 */
	private static final int ORBIT_SPAWN_COUNT = 24;

	/**
	 * <b>几何专属层的「环面留痕桩」</b>（服务端）：一圈静止桩点，勾出"这枚波在绕圈"这件事本身。
	 *
	 * <p><b>它为什么单独一个方法</b>（2026-10-03 需求 coe-ess §3.4 的 A/B 分层）：本方法
	 * <b>只画几何</b>——主体尘埃与标志粒子（END_ROD / 青焰）已整份搬进
	 * {@link WaveTrailStyle#ARCANE} 的档案，走风格层（主波与环绕波同一条路）。
	 * 留在本方法里的只有"铺在环平面上的一圈桩点"：它依赖环平面基向量与半径，
	 * 而这两样只对"在环绕"这件事有意义 ⇒ 它是几何专属，与魔素无关，也不进任何风格档案。
	 * 主波永远不调本方法（它没有环平面），所以主波用"异"魔素时不会画出无意义的桩点。</p>
	 *
	 * @param server  服务端世界
	 * @param center  环绕波当前位置（= 圆周点）
	 * @param u       环平面基向量之一（见 {@code AbstractChargerWaveEntity#orbitPlaneAxes()}）
	 * @param v       环平面基向量之二（u × v = 环平面法向，右手系）
	 * @param radius  环绕半径（格；用实体自身的要素值，不在这里写常量）
	 * @param phase   当前相位（弧度）——桩点绕它对称铺开，于是每 tick 都能看出波转到哪儿了
	 * @param ticking 是否到了补"环面留痕桩"的那一 tick（节流由调用方按
	 *                {@link #ORBIT_MARK_INTERVAL_TICKS} 决定，本方法只负责画）
	 */
	public static void sendOrbitMarks(ServerLevel server, Vec3 center, Vec3 u, Vec3 v,
		double radius, double phase, boolean ticking) {
		if (!ticking || u == null || v == null)
			return;
		DustParticleOptions mark = new DustParticleOptions(
			new Vector3f((float) ORBIT_MARK_COLOR.x, (float) ORBIT_MARK_COLOR.y, (float) ORBIT_MARK_COLOR.z),
			ORBIT_MARK_SCALE);
		for (int i = 0; i < ORBIT_MARK_COUNT; i++) {
			double theta = phase + (Math.PI * 2.0D * i) / ORBIT_MARK_COUNT;
			Vec3 offset = u.scale(radius * Math.cos(theta)).add(v.scale(radius * Math.sin(theta)));
			Vec3 at = center.add(offset);
			server.sendParticles(mark, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
	}

	/**
	 * <b>环绕波出生簇</b>：出生点炸一小团（主色尘埃 + 一点 END_ROD + 一圈环面桩）。
	 *
	 * <p>出生点是父波位置（半径 0 处），所以这一簇既是"我来了"的提示，
	 * 也顺手把整圈轨道标出来 —— 玩家发射后立刻能看到主波周围多了一个环。</p>
	 */
	public static void burstOrbitSpawn(ServerLevel server, Vec3 center, Vec3 color, Vec3 u, Vec3 v,
		double radius, double phase) {
		Vec3 base = color == null ? Vec3.ZERO : color;
		server.sendParticles(new DustParticleOptions(
				new Vector3f((float) base.x, (float) base.y, (float) base.z), ORBIT_TRAIL_SCALE),
			center.x, center.y, center.z, ORBIT_SPAWN_COUNT, 0.25, 0.25, 0.25, 0.05);
		server.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, ORBIT_FLAG_END_ROD,
			0.20, 0.20, 0.20, 0.03);
		if (u == null || v == null)
			return;
		DustParticleOptions mark = new DustParticleOptions(
			new Vector3f((float) ORBIT_MARK_COLOR.x, (float) ORBIT_MARK_COLOR.y, (float) ORBIT_MARK_COLOR.z),
			ORBIT_MARK_SCALE);
		for (int i = 0; i < ORBIT_MARK_COUNT; i++) {
			double theta = phase + (Math.PI * 2.0D * i) / ORBIT_MARK_COUNT;
			Vec3 at = center.add(u.scale(radius * Math.cos(theta)))
				.add(v.scale(radius * Math.sin(theta)));
			server.sendParticles(mark, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
	}
}
