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
 */
public final class SkillEnergyCost {

	private SkillEnergyCost() {}

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
	 * 读取工具上指定技能的当前等级（技能注册写入的 NBT {@code Level} 键）。
	 *
	 * @return 技能等级，找不到时为 1
	 */
	private static int skillLevel(ItemStack stack, ItemSkill skill) {
		SkillsComponent component = stack.get(AllDataComponents.SKILLS);
		if (component != null) {
			for (DataSkill data : component.getAllData()) {
				if (data.skill == skill && data.nbt != null) {
					int level = data.nbt.getInt("Level");
					if (level > 0) {
						return level;
					}
				}
			}
		}
		return 1;
	}
}
