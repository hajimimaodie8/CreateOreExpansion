package com.hjmmd_8.createoreexpansion.content.charger.entity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlockEntity;
import com.hjmmd_8.createoreexpansion.content.wave.WaveLevels;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTrailStyle;

import net.minecraft.core.BlockPos;
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
 * <p><b>粒子预算</b>：点缀粒子按"主粒子数量 × 比例"派生（见 {@link Accent} 与
 * {@link #accentCount(int, Accent) 数量算法}）——<b>点缀颗数 = Math.round(基数 × 比例)</b>，
 * 每个风格挂两种点缀，比例各为 1/4 与 1/8（同风格两比例之和恒为 0.375），
 * 故单次发射的总粒子量最多为主粒子基数的 1.375 倍。<b>逐档实算</b>
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
 */
public final class ChargerWaveFx {

	// ==================== 风格调色板（纯色常量，供颜色变换函数取用） ====================

	/** 钢灰色：机械风格的暗端（冷、去饱和的金属感）。 */
	private static final Vec3 METAL_STEEL = new Vec3(0.62, 0.60, 0.56);
	/** 黄铜色：机械风格的亮端（暖、偏黄的机加工感）。 */
	private static final Vec3 METAL_BRASS = new Vec3(0.72, 0.55, 0.28);
	/** 深血色：伤害风格的暗端。 */
	private static final Vec3 HEAT_DEEP_RED = new Vec3(0.85, 0.15, 0.12);
	/** 炽橙色：伤害风格的亮端。 */
	private static final Vec3 HEAT_ORANGE = new Vec3(1.0, 0.55, 0.10);

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
	 * 一种风格的全部视觉参数（不可变数据对象）。
	 *
	 * <p>把"风格"表达成数据而不是方法里的分支，是为了让新增/调整风格只改
	 * {@link #buildProfiles()} 里的一行，而不是在三处发送粒子方法里各加一段 if。</p>
	 *
	 * @param tint    主粒子颜色变换：输入波的渲染色（RGB 0-1），输出该风格的粒子色。
	 *                必须是<b>纯函数</b>——同一输入永远给同一输出，且不得改动入参（{@code Vec3} 本身不可变）。
	 * @param accents 叠加在主粒子之上的点缀粒子构成；空列表 = 该风格不加点缀（如 NORMAL）。
	 */
	public record StyleProfile(UnaryOperator<Vec3> tint, List<Accent> accents) {
	}

	/**
	 * 点缀粒子档案：在风格主粒子之外额外发送的一类原版粒子。
	 *
	 * <p>数量按比例派生而非写死，这样同一个档案在"拖尾（6 颗）/绽放（30 颗）/
	 * 爆炸（1~5 级 55/80/105/130/155 颗）"三种量级下都能自动等比缩放——
	 * 实算方式与各档合计见类注释的"粒子预算"一节。</p>
	 *
	 * @param particle     原版粒子类型（无参构造的 {@code SimpleParticleType}，客户端无需注册）
	 * @param ratio        相对主粒子数量的比例（0.25 = 每 4 颗主粒子配 1 颗点缀）
	 * @param spreadScale  相对主粒子散布半径的倍率（&gt;1 更散，&lt;1 更聚）
	 * @param speedScale   相对主粒子初速的倍率（火花要快、烟雾要慢）
	 */
	public record Accent(ParticleOptions particle, double ratio, double spreadScale, double speedScale) {
	}

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
	 *   <li><b>MECHANICAL</b>（机械感，用于全能波"读机器加工"）：颜色向钢灰/黄铜收敛
	 *       （见 {@link #metalTint}）；点缀 {@code ELECTRIC_SPARK} 表达"电学/机加工的火花"，
	 *       少量 {@code SMOKE} 表达"机器运转的粉尘排气"。<b>刻意不用火焰/岩浆类</b>，
	 *       避免被误解成"点燃机器"。</li>
	 *   <li><b>DAMAGE</b>（伤害感，用于攻击波）：颜色向深红/橙收敛（见 {@link #heatTint}）；
	 *       点缀 {@code CRIT} 表达"暴击星芒"，{@code DAMAGE_INDICATOR} 表达"挨了一下"的伤害反馈。
	 *       <b>同样不用火焰</b>，与"点燃/着火"玩法语义彻底隔开。</li>
	 * </ul>
	 */
	private static Map<WaveTrailStyle, StyleProfile> buildProfiles() {
		EnumMap<WaveTrailStyle, StyleProfile> profiles = new EnumMap<>(WaveTrailStyle.class);
		profiles.put(WaveTrailStyle.NORMAL,
			new StyleProfile(UnaryOperator.identity(), List.of()));
		profiles.put(WaveTrailStyle.MECHANICAL,
			new StyleProfile(ChargerWaveFx::metalTint, List.of(
				// 电火花：快、散、数量占主粒子的 1/4 —— 机械运转的"电"感
				new Accent(ParticleTypes.ELECTRIC_SPARK, 0.25, 1.15, 1.6),
				// 烟雾：慢、略散、数量占 1/8 —— 机器排气的"重"感（非火焰）
				new Accent(ParticleTypes.SMOKE, 0.125, 1.35, 0.5))));
		profiles.put(WaveTrailStyle.DAMAGE,
			new StyleProfile(ChargerWaveFx::heatTint, List.of(
				// 暴击星芒：快而散，数量占主粒子的 1/4 —— 打击瞬间的"脆"反馈
				new Accent(ParticleTypes.CRIT, 0.25, 1.1, 1.5),
				// 伤害指示：慢而聚，数量占 1/8 —— 像弹出的伤害数字碎屑
				new Accent(ParticleTypes.DAMAGE_INDICATOR, 0.125, 1.0, 0.8))));
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
	 *   <li><b>去饱和</b>：按亮度把基色向自身灰度插值 55%——金属的反射光很弱饱和，
	 *       这一步让任何波色（黄/绿/蓝/紫/玫红）先褪成"金属灰"的底色；</li>
	 *   <li><b>合金化</b>：再向"钢灰→黄铜"的合金色插值 65%。合金色由基色亮度决定
	 *       （暗 → 0.62,0.60,0.56 钢灰；亮 → 0.72,0.55,0.28 黄铜），
	 *       于是"深色波偏冷钢、亮色波偏暖铜"，一眼能分辨仍是哪一级波，但整体是机械金属调。</li>
	 * </ol>
	 */
	private static Vec3 metalTint(Vec3 base) {
		double lum = Mth.clamp(luminance(base), 0.0, 1.0);
		Vec3 desaturated = base.lerp(new Vec3(lum, lum, lum), 0.55);
		Vec3 alloy = METAL_STEEL.lerp(METAL_BRASS, lum);
		return desaturated.lerp(alloy, 0.65);
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

	/** 感知亮度（Rec.601 权重）：颜色变换用它决定"暗端/亮端"的插值权重。 */
	private static double luminance(Vec3 color) {
		return 0.299 * color.x + 0.587 * color.y + 0.114 * color.z;
	}

	/** 点缀粒子数量 = 主粒子量 × 比例（四舍五入，封顶 {@link #MAX_ACCENT_PER_EMIT}）。 */
	private static int accentCount(int baseCount, Accent accent) {
		if (baseCount <= 0)
			return 0;
		return Math.min(MAX_ACCENT_PER_EMIT, (int) Math.round(baseCount * accent.ratio()));
	}

	// ==================== 粒子构造（风格感知） ====================

	/**
	 * 风格化的能量波拖尾粒子：原版染色粒子（{@link DustParticleOptions}），颜色经该风格的颜色变换。
	 *
	 * <p>只返回"主粒子"——点缀粒子（电火花/暴击）不是染色粒子，无法塞进同一个
	 * {@link ParticleOptions}，故由 {@link #sendTrail} / {@link #addTrailParticles} 一并发射。</p>
	 *
	 * @param style     波型风格（null / 未登记 → NORMAL）
	 * @param baseColor 波的渲染色（RGB 0-1）
	 * @param scale     粒子尺寸（原版染色粒子的 scale）
	 */
	public static ParticleOptions waveParticle(WaveTrailStyle style, Vec3 baseColor, float scale) {
		return particleFor(profile(style), baseColor, scale);
	}

	/** 由已查好的档案构粒子（内部用：一次发射内只查一次表，避免逐颗重复查表）。 */
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
		StyleProfile profile = profile(style);
		server.sendParticles(particleFor(profile, baseColor, scale), pos.x, pos.y, pos.z, count,
			spread.x, spread.y, spread.z, speed);
		for (Accent accent : profile.accents()) {
			int extras = accentCount(count, accent);
			if (extras <= 0)
				continue;
			server.sendParticles(accent.particle(), pos.x, pos.y, pos.z, extras,
				spread.x * accent.spreadScale(), spread.y * accent.spreadScale(),
				spread.z * accent.spreadScale(), speed * accent.speedScale());
		}
	}

	/**
	 * 客户端（Ponder 思索者场景等）拖尾：逐粒子 {@code addParticle}，主粒子 + 风格点缀。
	 *
	 * <p>与 {@link #sendTrail} 的区别只在发送通道（本方法不经过网络包），故参数与粒子构成一一对应；
	 * 点缀粒子在此额外叠加一点随机抖动，让火花/碎屑看起来是"崩出去"的而不是沿波轴平移。</p>
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
		StyleProfile profile = profile(style);
		ParticleOptions particle = particleFor(profile, baseColor, scale);
		for (int i = 0; i < count; i++)
			level.addParticle(particle, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
		RandomSource random = level.random;
		for (Accent accent : profile.accents()) {
			int extras = accentCount(count, accent);
			double speedScale = accent.speedScale();
			for (int i = 0; i < extras; i++) {
				level.addParticle(accent.particle(), pos.x, pos.y, pos.z,
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
	 * 伤害风格炸开的是暴击星芒与伤害指示（读作"挨了一击"）。音效保持原有的紫水晶共鸣，不改玩法。</p>
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
		server.sendParticles(particleFor(profile, baseColor, 0.6f), pos.x, pos.y, pos.z, BURST_COUNT,
			0.5, 0.5, 0.5, 0.3);
		for (Accent accent : profile.accents()) {
			int extras = accentCount(BURST_COUNT, accent);
			if (extras <= 0)
				continue;
			server.sendParticles(accent.particle(), pos.x, pos.y, pos.z, extras,
				0.5 * accent.spreadScale(), 0.5 * accent.spreadScale(), 0.5 * accent.spreadScale(),
				0.3 * accent.speedScale());
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
	 * 视觉上像"球面炸开 + 溅射"。</p>
	 *
	 * @param level     客户端世界
	 * @param pos       绽放中心
	 * @param style     风格
	 * @param baseColor 波的渲染色（RGB 0-1）
	 */
	public static void burstParticles(Level level, Vec3 pos, WaveTrailStyle style, Vec3 baseColor) {
		StyleProfile profile = profile(style);
		ParticleOptions particle = particleFor(profile, baseColor, 0.5f);
		RandomSource random = level.random;
		for (int i = 0; i < BURST_COUNT; i++) {
			Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian())
				.normalize();
			level.addParticle(particle, pos.x, pos.y, pos.z,
				dir.x * 0.35, dir.y * 0.35, dir.z * 0.35);
		}
		for (Accent accent : profile.accents()) {
			int extras = accentCount(BURST_COUNT, accent);
			double speed = 0.35 * accent.speedScale();
			for (int i = 0; i < extras; i++) {
				Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian())
					.normalize();
				level.addParticle(accent.particle(), pos.x, pos.y, pos.z,
					dir.x * speed, dir.y * speed, dir.z * speed);
			}
		}
	}

	/**
	 * 触发一次范围能量爆炸（不破坏地形）：
	 * <ul>
	 *   <li><b>爆炸半径 = 爆炸等级（格）</b>：水平正方形范围半径（1→3×3、2→5×5、3→7×7、
	 *       4→9×9、5→11×11）——4 级伊普西龙波爆炸 4 格、5 级欧米伽波爆炸 5 格；</li>
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
			int count = 30 + boomLevel * 25; // 55 / 80 / 105 / 130 / 155 个（1~5 级）
			server.sendParticles(particleFor(profile, color, 0.7f), center.x, center.y, center.z, count,
				1.2, 1.2, 1.2, 0.15);
			if (color2 != null) {
				// 混合第二色，增强视觉层次（同为该风格着色）
				server.sendParticles(particleFor(profile, color2, 0.5f), center.x, center.y, center.z, count / 2,
					1.0, 1.0, 1.0, 0.12);
			}
			// 风格点缀：数量随爆炸密度等比放大（1 级 55 → 14+7=21 颗，5 级 155 → 39+19=58 颗）
			for (Accent accent : profile.accents()) {
				int extras = accentCount(count, accent);
				if (extras <= 0)
					continue;
				server.sendParticles(accent.particle(), center.x, center.y, center.z, extras,
					1.2 * accent.spreadScale(), 1.2 * accent.spreadScale(), 1.2 * accent.spreadScale(),
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

		// 范围内生物：受到该等级波对应的撞击伤害（低 4 / 高 6 / 伽马 8 / 伊普西龙 10 / 欧米伽 12）
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
				// 伽马能量加工（等级 ≥3）：范围内强化避雷针获得 1 次 γ 充能
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

	/** 按等级取命中伤害（低 4 / 高 6 / 伽马 8 / 伊普西龙 10 / 欧米伽 12）。 */
	static float damageForLevel(int level) {
		return WaveLevels.damage(level);
	}
}
