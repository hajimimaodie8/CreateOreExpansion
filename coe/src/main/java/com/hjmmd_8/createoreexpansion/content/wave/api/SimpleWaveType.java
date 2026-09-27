package com.hjmmd_8.createoreexpansion.content.wave.api;

import net.minecraft.resources.ResourceLocation;

/**
 * {@link WaveType} 的<b>数据式实现</b>：只描述能力与风格，不带行为。
 *
 * <p>本模组三个内置波型与绝大多数扩展模组的需求都能用它一行注册完成；
 * 需要"波型自身带行为"（例如命中时额外施法）时，才另写一个实现类。</p>
 */
public record SimpleWaveType(ResourceLocation id, boolean allowsChargingProcessing, boolean allowsRemoteProcessing,
	boolean dealsDamage, WaveTrailStyle trailStyle) implements WaveType {
}
