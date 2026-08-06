package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.common.AllSkills;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ItemStack 技能辅助类
 * 由于 ItemStack 是 final 类，无法通过 mixin 添加接口实现，因此使用辅助类
 */
public final class ItemStackSkillHelper {

    /**
     * 检查物品是否有指定类型的技能
     */
    public static boolean hasSkill(ItemStack itemStack, SkillType type) {
        Map<SkillType, List<ItemSkill>> skills = loadSkills(itemStack);
        return skills.containsKey(type);
    }

    public static boolean hasSkill(ItemStack itemStack, ItemSkill skill) {
        Map<SkillType, List<ItemSkill>> skills = loadSkills(itemStack);
        SkillType type = skill.getType();
        return skills.containsKey(type) && skills.get(type).contains(skill);
    }

    public static List<ItemSkill> getSkills(ItemStack stack, SkillType type) {
        return loadSkills(stack).get(type);
    }

    /**
     * 释放物品的指定类型技能
     */
    public static void releaseSkills(ItemStack itemStack, SkillType type, Object context) {
        Map<SkillType, List<ItemSkill>> skills = loadSkills(itemStack);
        if (!skills.containsKey(type)) {
            return;
        }

        skills.get(type).forEach(skill -> {
            if (!skill.getType().cast(context))
                throw new ClassCastException("Skill context type mismatch");
            skill.release(context);
        });
    }

    /**
     * 从物品的 DataComponent 加载技能
     */
    private static Map<SkillType, List<ItemSkill>> loadSkills(ItemStack itemStack) {
        Map<SkillType, List<ItemSkill>> result = new HashMap<>();

        List<String> skillIds = itemStack.getComponents().get(AllDataComponents.SKILLS);
        if (skillIds == null) {
            return result;
        }

        for (String skillId : skillIds) {
            ItemSkill skill = AllSkills.get(ResourceLocation.tryParse(skillId));
            if (skill != null) {
                SkillType skillType = skill.getType();
                if (!result.containsKey(skillType)) {
                    result.put(skillType, new java.util.ArrayList<>());
                }
                result.get(skillType).add(skill);
            }
        }

        return result;
    }
}