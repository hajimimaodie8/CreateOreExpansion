package com.hjmmd_8.createoreexpansion.common.energy;

import net.minecraft.world.item.ItemStack;

/**
 * 技能能量消耗规则（<b>与技能类型无关的通用算术</b>）。
 *
 * <p><b>P3p</b>：本类已搬进共享库（core）。原来那两个按旧技能框架类型算的重载
 * （{@code effectiveLevel(ItemStack, DataSkill)} / {@code compute(ItemStack, ItemSkill)}）
 * 要读技能注册表与 {@code SKILLS} 组件，属于层内类型，已整体搬到层里的
 * {@code skill.SkillEnergySpend}（P3r 后旧技能框架住 skill 包），
 * 方法体逐字未改。这里只剩这两个只吃 {@code int} 的重载——新内核（Skiller）走的就是它们。</p>
 *
 * <p>{@code 消耗 = 一级消耗(技能注册值) × 当前技能等级}，再乘上减耗附魔的折扣
 * （1 级 90%、2 级 80%、3 级 70%、4 级 60%、5 级及以上 50%）。</p>
 *
 * <p>技能等级取「有效等级」：{@code min(基础等级 + 技能提升附魔, 技能满级)}——
 * 显示与消耗统一以此为准，技能提升附魔不写入物品 NBT（移除附魔即恢复原等级）。</p>
 */
public final class SkillEnergyCost {

	private SkillEnergyCost() {}

	/**
	 * 技能有效等级（新内核版重载）= min(基础等级 + 技艺提升 - 技艺回溯, 满级)，且不低于 1。
	 * 技艺提升/技艺回溯 3 级及以上提升量/削减量一律按 2 计，两个附魔可共存（净效果 = 提升量 - 削减量）。
	 * 显示与消耗统一以此为准。
	 *
	 * @param stack     手持工具（读技艺提升/技艺回溯附魔）
	 * @param baseLevel 物品上该技能的基础等级
	 * @param maxLevel  该技能的满级
	 */
	public static int effectiveLevel(ItemStack stack, int baseLevel, int maxLevel) {
		int boost = Math.min(ToolEnchantments.skillBoostLevel(stack), 2);
		int regression = Math.min(ToolEnchantments.skillRegressionLevel(stack), 2);
		int level = Math.max(1, baseLevel) + boost - regression;
		return Math.max(1, Math.min(level, Math.max(1, maxLevel)));
	}

	/**
	 * 计算一次技能释放的实际能量消耗（新内核版重载）。
	 *
	 * <p>口径与旧框架的 {@code SkillEnergySpend#compute(ItemStack, ItemSkill)} 完全一致：
	 * {@code 消耗 = 一级消耗(注册值) × 有效等级}，再乘减耗附魔折扣
	 * （1 级 90%、2 级 80%、3 级 70%、4 级 60%、5 级及以上 50%）。
	 * 区别只是不再依赖旧的 {@code ItemSkill}（旧实现由调用方从配置里取一级消耗、
	 * 用 {@link #effectiveLevel(ItemStack, int, int)} 取有效等级）。</p>
	 *
	 * @param stack          手持的工具
	 * @param baseCost       一级消耗（技能注册值 / 该等级配置里的 Cost）
	 * @param effectiveLevel 有效等级（含技艺提升/回溯附魔）
	 * @return 实际消耗（&lt;= 0 表示无需能量）
	 */
	public static int compute(ItemStack stack, int baseCost, int effectiveLevel) {
		if (baseCost <= 0) {
			return 0;
		}
		int cost = baseCost * Math.max(1, effectiveLevel);
		int enchant = ToolEnchantments.reduceConsumptionLevel(stack);
		if (enchant > 0) {
			double multiplier = enchant >= 5 ? 0.5 : 1.0 - 0.1 * enchant;
			cost = Math.max(1, (int) Math.round(cost * multiplier));
		}
		return cost;
	}
}
