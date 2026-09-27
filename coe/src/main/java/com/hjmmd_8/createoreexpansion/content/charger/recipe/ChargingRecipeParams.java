package com.hjmmd_8.createoreexpansion.content.charger.recipe;

import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 充电配方参数（ProcessingRecipe 参数子类，Create 6 标准）。
 *
 * <p>基础字段（ingredients/results/…）在 {@link ProcessingRecipeParams}，
 * 本类只叠加配方自带的 <b>level</b>（1~5 充能等级）。照 Create
 * {@code ItemApplicationRecipeParams} 的写法：codec 用父级 codec + 额外字段，
 * 网络编解码覆写 encode/decode 并调 super。</p>
 */
public class ChargingRecipeParams extends ProcessingRecipeParams {

	public static MapCodec<ChargingRecipeParams> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		codec(ChargingRecipeParams::new).forGetter(Function.identity()),
		Codec.INT.optionalFieldOf("level", 1).forGetter(ChargingRecipeParams::level)
	).apply(instance, (params, level) -> {
		params.level = Math.max(1, level);
		return params;
	}));

	public static StreamCodec<RegistryFriendlyByteBuf, ChargingRecipeParams> STREAM_CODEC =
		streamCodec(ChargingRecipeParams::new);

	protected int level = 1;

	protected final int level() {
		return level;
	}

	@Override
	protected void encode(RegistryFriendlyByteBuf buffer) {
		super.encode(buffer);
		ByteBufCodecs.INT.encode(buffer, level);
	}

	@Override
	protected void decode(RegistryFriendlyByteBuf buffer) {
		super.decode(buffer);
		level = Math.max(1, ByteBufCodecs.INT.decode(buffer));
	}
}
