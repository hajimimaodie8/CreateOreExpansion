package com.hjmmd_8.createoreexpansion.content.grinding;

import com.hjmmd_8.createoreexpansion.common.AllTags;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * 角磨轮等级：决定最低工作转速与加工耗时（随转速线性插值）。
 *
 * <p>等级参数在此集中定义为常量：一级最低 64 RPM（64 时 10 秒）、二级最低 96 RPM（96 时 5 秒）、
 * 三级最低 128 RPM（128 时 3 秒）；转速升至 256 时耗时线性降至 1 秒。</p>
 *
 * <p>等级判定基于等级 tag（{@code createoreexpansion:grinding_wheels/tier_N}）而非物品 id——
 * 拓展模组只需把自己的角磨轮加入对应等级 tag 即可获得该等级，无需改本模组代码。</p>
 */
public enum GrindingWheelTier {

	/** 一级角磨轮：最低转速 64，64 RPM 时加工耗时 10 秒 */
	TIER_1(1, 64, 10f),
	/** 二级角磨轮：最低转速 96，96 RPM 时加工耗时 5 秒 */
	TIER_2(2, 96, 5f),
	/** 三级角磨轮：最低转速 128，128 RPM 时加工耗时 3 秒 */
	TIER_3(3, 128, 3f);

	/** 等级编号（1/2/3） */
	public final int level;
	/** 最低工作转速（RPM），低于此转速不加工 */
	public final int minRpm;
	/** 最低转速下单个物品的加工耗时（秒） */
	public final float baseTimeSeconds;

	GrindingWheelTier(int level, int minRpm, float baseTimeSeconds) {
		this.level = level;
		this.minRpm = minRpm;
		this.baseTimeSeconds = baseTimeSeconds;
	}

	/** 从角磨轮物品解析等级：按等级 tag 判定；非角磨轮返回 null */
	public static GrindingWheelTier from(ItemStack stack) {
		if (stack.is(AllTags.AllItemTags.GRINDING_WHEELS_TIER_3.tag))
			return TIER_3;
		if (stack.is(AllTags.AllItemTags.GRINDING_WHEELS_TIER_2.tag))
			return TIER_2;
		if (stack.is(AllTags.AllItemTags.GRINDING_WHEELS_TIER_1.tag))
			return TIER_1;
		return null;
	}

	/** 加工耗时（秒）：minRpm → baseTime，256 RPM → 1 秒，线性插值；rpm 超出范围时夹取 */
	public float getProcessingTime(float rpm) {
		float clamped = Mth.clamp(rpm, minRpm, 256);
		float t = (256 - clamped) / (256 - minRpm);
		return 1 + t * (baseTimeSeconds - 1);
	}
}
