package com.hjmmd_8.createoreexpansion.content.grinding.item;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.common.AllTags;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * 角磨轮等级：决定最低工作转速与加工耗时（随转速线性插值）。
 *
 * <p>等级参数（最低转速 / 最低转速下耗时）已抽为模组配置（{@link AllConfig.Grinding}，
 * 配置文件 {@code createoreexpansion-common.toml} 的 {@code grinding} 段），
 * 整合包/玩家可自行调节；耗时插值上限跟随 Create 的 {@code maxRotationSpeed}（默认 256）。</p>
 *
 * <p>等级判定基于等级 tag（{@code createoreexpansion:grinding_wheels/tier_N}）而非物品 id——
 * 拓展模组只需把自己的角磨轮加入对应等级 tag 即可获得该等级，无需改本模组代码。</p>
 */
public enum GrindingWheelTier {

	/** 一级角磨轮（铁/金/黄铜/锌） */
	TIER_1(1),
	/** 二级角磨轮（翡翠/钻石/黄玉） */
	TIER_2(2),
	/** 三级角磨轮（蓝宝石/星辉石/下界合金） */
	TIER_3(3);

	/** 等级编号（1/2/3） */
	public final int level;

	GrindingWheelTier(int level) {
		this.level = level;
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

	/** 最低工作转速（RPM，配置可调），低于此转速不加工 */
	public int getMinRpm() {
		return switch (level) {
			case 2 -> AllConfig.tier2MinRpm;
			case 3 -> AllConfig.tier3MinRpm;
			default -> AllConfig.tier1MinRpm;
		};
	}

	/** 最低转速下单个物品的加工耗时（秒，配置可调） */
	public float getBaseTimeSeconds() {
		return switch (level) {
			case 2 -> AllConfig.tier2Time;
			case 3 -> AllConfig.tier3Time;
			default -> AllConfig.tier1Time;
		};
	}

	/** 加工耗时（秒）：minRpm → baseTime，maxRotationSpeed → 1 秒，线性插值；rpm 超出范围时夹取 */
	public float getProcessingTime(float rpm) {
		int max = AllConfigs.server().kinetics.maxRotationSpeed.get();
		int minRpm = getMinRpm();
		float base = getBaseTimeSeconds();
		float clamped = Mth.clamp(rpm, minRpm, max);
		float t = (max - clamped) / (float) (max - minRpm);
		return 1 + t * (base - 1);
	}
}
