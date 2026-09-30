package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.common.registry.coe.AllSkills;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnchantments;
import net.minecraft.world.item.ItemStack;

/**
 * 技能<b>有效等级</b>的唯一口径（技能换核后只保留这一件事）。
 *
 * <p><b>2026-09-30 技能换核第 5 阶段</b>：本类原先还带着旧框架的能量口径与扣能编排
 * （{@code compute(ItemStack, ItemSkill)} / {@code tryConsume(...)} / {@code skillLevel(...)}），
 * 它们只被旧执行入口（{@code SkillsComponent#releaseSkills/releaseSkillAt} 与旧技能实现）
 * 调用；那些入口已整体删除，这三个方法经全仓检索确认<b>零调用者</b>，一并移除。
 * 能量消耗与冷却现在统一由新内核（Skiller）从技能配置读取，
 * 见 {@code integration/skiller/skill/CoeSkillSupport}。</p>
 *
 * <p>留下 {@link #effectiveLevel(ItemStack, DataSkill)} 是因为它同时服务于
 * 技能 tooltip 的等级显示与「按等级取配置」两条仍在的路径。</p>
 */
public final class SkillEnergySpend {

    private SkillEnergySpend() {
    }

    /**
     * 技能有效等级 = min(基础等级 + 技艺提升 - 技艺回溯, 技能满级)，且不低于 1。
     * 显示与消耗统一以此为准。技艺提升/技艺回溯 3 级及以上提升量/削减量一律按 2 计，
     * 两个附魔可共存（净效果 = 提升量 - 削减量）。
     *
     * <p>口径未变；换核后从 {@link DataSkill#id} 取注册 id（不再经技能实例反查）。</p>
     */
    public static int effectiveLevel(ItemStack stack, DataSkill data) {
        int base = data.nbt != null ? data.nbt.getInt("Level") : 1;
        AllSkills.RegisteredDataSkill registered = AllSkills.getData(
                data.id != null ? data.id : AllSkills.getId(data.skill));
        int maxLevel = registered != null ? registered.maxLevel() : 5;
        int boost = Math.min(ToolEnchantments.skillBoostLevel(stack), 2);
        int regression = Math.min(ToolEnchantments.skillRegressionLevel(stack), 2);
        int level = Math.max(1, base) + boost - regression;
        return Math.max(1, Math.min(level, maxLevel));
    }
}
