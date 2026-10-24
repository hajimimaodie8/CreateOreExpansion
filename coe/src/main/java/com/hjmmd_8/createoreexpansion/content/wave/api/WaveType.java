package com.hjmmd_8.createoreexpansion.content.wave.api;

import java.util.function.UnaryOperator;

import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveFx;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * <b>波型</b>：一个能量波"是什么波"的完整定义（波情四属性之一）。
 *
 * <p>本接口是<b>对扩展模组开放的稳定面</b>：实现它（或直接用 {@link SimpleWaveType}）并在
 * {@link WaveTypes#register} 注册，就得到一个可被本模组全部机制识别的新波型——
 * 例如"雷波"（只伤害、不加工）、"冰波"（会充能加工且不伤害）之类，<b>无需修改本模组源码</b>。</p>
 *
 * <p>本接口只描述<b>能力开关</b>与<b>表现风格</b>，不持有任何世界状态；波实体侧的读取面
 * （另外三个波情要素）见 {@code AbstractChargerWaveEntity#getWaveType()}、
 * {@code #getWaveLevel()}、{@code #getWaveSpeed()}，波载荷见
 * {@code StellarWaveEntity#getPayloadItems/Fluid/Energy}（只有变体波带载荷）。</p>
 *
 * <p><b>中立性约束</b>（本仓库 hard rule）：实现方与调用方都只允许使用中立类型
 * （{@code ResourceLocation} / 原版 / NeoForge / Create 基础类型），
 * <b>不得</b> import 任何可选联动模组（CC&amp;A / Vintage / Jade / JEI / Optical / Sable）的类。</p>
 */
public interface WaveType {

	/** 唯一 id（形如 {@code createoreexpansion:normal}）。 */
	ResourceLocation id();

	/**
	 * 是否具备"充能加工"能力（{@code createoreexpansion:charging} 配方）。
	 *
	 * <p>普通波的看家本领；全能波本身没有，但可在特定条件下（变器读取半径内有星辉石应力充能器）
	 * <b>临时</b>获得——那种放行由机制侧单独判断，不改这里返回的"本性"。</p>
	 */
	boolean allowsChargingProcessing();

	/** 是否具备"读取机器 → 远程加工"能力（扫描携带 / 候选门槛 / 链式）。 */
	boolean allowsRemoteProcessing();

	/** 是否对生物造成伤害（伤害值仍由波级决定，见 {@code WaveLevels#damage}）。 */
	boolean dealsDamage();

	/** 拖尾与绽放的视觉风格。 */
	WaveTrailStyle trailStyle();

	/**
	 * <b>波型自身的颜色</b>（2026-10-24 批 4 新增的语义，D2 的第二优先）。
	 *
	 * <p>作者 D2："没有魔素 ⇒ 用波的类型所对应的颜色"。改造前波型<b>没有颜色</b>——它只有一个
	 * {@link #trailStyle()} 指针，颜色是由"等级基色 × 该风格的变换"在实体 tick 里算出来的。
	 * 现在波型自己回答颜色，取色规则收口在 {@link WaveColorRule#colorOf}。</p>
	 *
	 * <p><b>默认实现 = 由 {@link #trailStyle()} 推出的那个变换</b>（{@code ChargerWaveFx#styleColor}）——
	 * 于是本模组的三个内置波型与<b>今天已经注册过的第三方波型</b>观感逐值不变；想给波型一个
	 * 与风格无关的颜色，就在注册项里传一个显式变换（例如
	 * {@code new SimpleWaveType(id, false, false, true, WaveTrailStyle.DAMAGE, WaveColorRule.fixedRgb(0x9B5CFF))}）。</p>
	 *
	 * <p><b>入参是等级基色</b>：内置的三个变换都拿它当明度输入（所以等级仍可读）；返回
	 * {@code null} 视为"这个波型没给颜色"，回落等级基色（见 {@link #colorOf(Vec3)}）。</p>
	 */
	default UnaryOperator<Vec3> color() {
		return base -> ChargerWaveFx.styleColor(trailStyle(), base);
	}

	/** 本波型的颜色（{@code null} 安全；"没给颜色"⇒ 原样返回基色）。 */
	default Vec3 colorOf(Vec3 base) {
		Vec3 safe = base == null ? Vec3.ZERO : base;
		UnaryOperator<Vec3> tint = color();
		return tint == null ? safe : tint.apply(safe);
	}

	/**
	 * <b>波型显示名</b>（Jade"波型"行等一切对外读数；波情四属性之一的取名字口）。
	 *
	 * <p>默认按 {@link #id()} 的 path 查词条 {@code createoreexpansion.wave_type.<path>}
	 * （内置三个：{@code normal}=普通波、{@code omni}=全能波、{@code attack}=攻击波），
	 * <b>缺词条时回退显示 path 本身</b>——扩展模组注册的新波型即使没补词条也不会显示成裸键名，
	 * 与 {@code RecipeTypeNames} 的"已知类型查表、未知回退 path"同一口径。</p>
	 *
	 * <p>显示点一律调本方法（<b>不许在任何界面代码里对波型 id 写 switch</b>）；
	 * 需要更特殊的呈现（带颜色/图标/复合组件）时覆写本方法，或注册时自带实现类。</p>
	 */
	default Component displayName() {
		ResourceLocation id = id();
		if (id == null)
			return Component.empty();
		return Component.translatableWithFallback("createoreexpansion.wave_type." + id.getPath(), id.getPath());
	}
}
