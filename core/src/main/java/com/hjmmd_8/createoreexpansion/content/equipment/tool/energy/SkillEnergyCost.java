package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

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
	 * <b>技艺提升 / 记忆回溯的单侧封顶 = 2</b>（2026-10-06 批 15 收敛）。
	 *
	 * <p>两件附魔的合法等级只有 0/1/2（{@code data/createoreexpansion/enchantment/skill_boost.json}
	 * 的 {@code max_level} = 2），所以这个 {@code min(.., 2)} 本身是<b>零数值变化</b>的健壮性补丁：
	 * 它防的是"存档 / 命令塞进来的越级附魔"把加减量放大。批 15 之前它散在
	 * {@code SkillEnergyCost} 与 {@code ArmorSkillRuntime}（两条方法）里共 3 个文件 6 行，现在只有这一处。</p>
	 */
	public static final int MAX_ENCHANT_BOOST = 2;

	/**
	 * <b>减耗附魔的折扣下限 = 0.5</b>（5 级及以上一律五折）—— 与折扣斜率的唯一出处。
	 *
	 * <p>批 15 之前 {@code 0.5} 字面量出现在 {@code SkillEnergyCost#compute} 与
	 * {@code ArmorEnergy#DISCOUNT_FLOOR} 两处（后者的常量现在直接引用本字段）。</p>
	 */
	public static final double DISCOUNT_FLOOR = 0.5;

	/** <b>减耗附魔每级 10% 的折扣斜率</b>（{@code 1 - 0.1L}；L 达到 5 时被 {@link #DISCOUNT_FLOOR} 压住）。 */
	public static final double DISCOUNT_PER_LEVEL = 0.1;

	/**
	 * 减耗附魔等级 → 本次消耗的<b>折扣倍数</b>（1.0 = 无折扣）：{@code max(0.5, 1 - 0.1L)}。
	 *
	 * <p>口径未变，只是把原来写在两个类里的同一段算术收成一处（批 15）。</p>
	 *
	 * @param level 减耗附魔等级（&lt;= 0 时返回 1.0）
	 */
	public static double discountMultiplier(int level) {
		if (level <= 0) {
			return 1.0D;
		}
		return Math.max(DISCOUNT_FLOOR, 1.0D - DISCOUNT_PER_LEVEL * level);
	}

	/**
	 * <b>技能等级的唯一钳位算术</b>：把任意整数等级夹进 {@code [1, maxLevel]}。
	 *
	 * <p>批 15 收敛：这条表达式此前在 5 个文件里各写一遍（{@code SkillEnergyCost}、
	 * {@code SkillLevelTables#pick3Clamped}、{@code ArmorSkillRuntime} 两处、
	 * {@code StarShockRuntime}、{@code AllSkills#configForLevel}）——现在都调这里。
	 * 语义逐字等价（{@code maxLevel < 1} 时同样回落到 1）。</p>
	 *
	 * @param level    待夹取等级
	 * @param maxLevel 该技能/该表的最高等级
	 * @return {@code [1, max(1, maxLevel)]} 内的等级
	 */
	public static int clamp(int level, int maxLevel) {
		return Math.max(1, Math.min(level, Math.max(1, maxLevel)));
	}

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
		int boost = Math.min(ToolEnchantments.skillBoostLevel(stack), MAX_ENCHANT_BOOST);
		int regression = Math.min(ToolEnchantments.skillRegressionLevel(stack), MAX_ENCHANT_BOOST);
		return clamp(Math.max(1, baseLevel) + boost - regression, maxLevel);
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
			double multiplier = discountMultiplier(enchant);
			cost = Math.max(1, (int) Math.round(cost * multiplier));
		}
		return cost;
	}
}
