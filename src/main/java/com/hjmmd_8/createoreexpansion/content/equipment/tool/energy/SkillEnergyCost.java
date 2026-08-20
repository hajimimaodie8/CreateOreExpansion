package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.common.AllSkills;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;
import net.minecraft.world.item.ItemStack;

/**
 * 技能能量消耗规则。
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
	 * 技能有效等级 = min(基础等级 + 技艺提升 - 技艺回溯, 技能满级)，且不低于 1。
	 * 显示与消耗统一以此为准。技艺提升/技艺回溯 3 级及以上提升量/削减量一律按 2 计，
	 * 两个附魔可共存（净效果 = 提升量 - 削减量）。
	 */
	public static int effectiveLevel(ItemStack stack, DataSkill data) {
		int base = data.nbt != null ? data.nbt.getInt("Level") : 1;
		AllSkills.RegisteredDataSkill registered = AllSkills.getData(AllSkills.getId(data.skill));
		int maxLevel = registered != null ? registered.maxLevel() : 5;
		int boost = Math.min(ToolEnchantments.skillBoostLevel(stack), 2);
		int regression = Math.min(ToolEnchantments.skillRegressionLevel(stack), 2);
		int level = Math.max(1, base) + boost - regression;
		return Math.max(1, Math.min(level, maxLevel));
	}

	/**
	 * 计算一次技能释放的实际能量消耗。
	 *
	 * @param stack 手持的工具
	 * @param skill 将要释放的技能
	 * @return 实际消耗（&lt;= 0 表示无需能量）
	 */
	public static int compute(ItemStack stack, ItemSkill skill) {
		int base = skill.getCost();
		if (base <= 0) {
			return 0;
		}
		int level = skillLevel(stack, skill);
		int cost = base * Math.max(1, level);
		int enchant = ToolEnchantments.reduceConsumptionLevel(stack);
		if (enchant > 0) {
			double multiplier = enchant >= 5 ? 0.5 : 1.0 - 0.1 * enchant;
			cost = Math.max(1, (int) Math.round(cost * multiplier));
		}
		CreateOreExpansion.LOGGER.info("[COE-ENCH] computeCost skill={} level={} base={} reduceEnchant={} cost={}",
			AllSkills.getId(skill), level, base, enchant, cost);
		return cost;
	}

	/**
	 * 读取工具上指定技能的当前有效等级（含技能提升附魔）。
	 *
	 * @return 技能等级，找不到时为 1
	 */
	private static int skillLevel(ItemStack stack, ItemSkill skill) {
		SkillsComponent component = stack.get(AllDataComponents.SKILLS);
		if (component != null) {
			for (DataSkill data : component.getAllData()) {
				if (data.skill == skill) {
					return effectiveLevel(stack, data);
				}
			}
		}
		return 1;
	}
}
