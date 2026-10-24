package com.hjmmd_8.createoreexpansion.content.wave.api;

import java.util.function.UnaryOperator;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * {@link WaveType} 的<b>数据式实现</b>：只描述能力、风格与颜色，不带行为。
 *
 * <p>本模组三个内置波型与绝大多数扩展模组的需求都能用它一行注册完成；
 * 需要"波型自身带行为"（例如命中时额外施法）时，才另写一个实现类。</p>
 *
 * <p><b>2026-10-24 批 4 新增第 6 个参数（可选）</b>：{@code colorOverride} = 这个波型自己的颜色变换。
 * 不传（旧的 5 参构造器）⇒ 用 {@link WaveType#color()} 的默认实现（= 由 {@code trailStyle()} 推出的
 * 变换），所以<b>既有调用一个字不用改、观感逐值不变</b>；传了 ⇒ D2 的"没魔素 ⇒ 波型色"就用它
 * （例如 {@code WaveColorRule.fixedRgb(0x9B5CFF)} 就是纯平色）。</p>
 */
public record SimpleWaveType(ResourceLocation id, boolean allowsChargingProcessing, boolean allowsRemoteProcessing,
	boolean dealsDamage, WaveTrailStyle trailStyle, UnaryOperator<Vec3> colorOverride) implements WaveType {

	/** 兼容旧形状的构造器：不指定颜色 ⇒ 由拖尾风格推出（= 改造前的颜色）。 */
	public SimpleWaveType(ResourceLocation id, boolean allowsChargingProcessing, boolean allowsRemoteProcessing,
		boolean dealsDamage, WaveTrailStyle trailStyle) {
		this(id, allowsChargingProcessing, allowsRemoteProcessing, dealsDamage, trailStyle, null);
	}

	@Override
	public UnaryOperator<Vec3> color() {
		return colorOverride != null ? colorOverride : WaveType.super.color();
	}
}
