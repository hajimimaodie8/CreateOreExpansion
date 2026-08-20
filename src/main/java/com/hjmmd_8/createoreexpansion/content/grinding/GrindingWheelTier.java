package com.hjmmd_8.createoreexpansion.content.grinding;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 角磨轮等级：决定最低工作转速与加工耗时（随转速线性插值）。
 *
 * <p>一级：铁角磨轮（最低 64 RPM，64 时 10 秒）；二级：钻石/翡翠/黄玉角磨轮（最低 96 RPM，96 时 5 秒）；
 * 三级：蓝宝石/星辉石角磨轮（最低 128 RPM，128 时 3 秒）。转速升至 256 时耗时线性降至 1 秒。</p>
 */
public enum GrindingWheelTier {

	TIER_1(1, 64, 10f),
	TIER_2(2, 96, 5f),
	TIER_3(3, 128, 3f);

	public final int level;
	public final int minRpm;
	public final float baseTimeSeconds;

	GrindingWheelTier(int level, int minRpm, float baseTimeSeconds) {
		this.level = level;
		this.minRpm = minRpm;
		this.baseTimeSeconds = baseTimeSeconds;
	}

	/** 从角磨轮物品 id 解析等级；非角磨轮返回 null */
	public static GrindingWheelTier from(ResourceLocation wheelId) {
		String path = wheelId.getPath();
		if (path.startsWith("iron_"))
			return TIER_1;
		if (path.startsWith("diamond_") || path.startsWith("jade_") || path.startsWith("topaz_"))
			return TIER_2;
		if (path.startsWith("sapphire_") || path.startsWith("stellarstone_"))
			return TIER_3;
		return null;
	}

	/** 加工耗时（秒）：minRpm → baseTime，256 RPM → 1 秒，线性插值；rpm 超出范围时夹取 */
	public float getProcessingTime(float rpm) {
		float clamped = Mth.clamp(rpm, minRpm, 256);
		float t = (256 - clamped) / (256 - minRpm);
		return 1 + t * (baseTimeSeconds - 1);
	}
}
