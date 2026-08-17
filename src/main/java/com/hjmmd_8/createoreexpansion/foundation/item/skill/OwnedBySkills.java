package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface OwnedBySkills {

    Map<SkillType, List<ItemSkill>> skills();

    default List<ItemSkill> getSkills(SkillType type) {
        return skills().get(type);
    }

    default List<ItemSkill> getAllSkills() {
        if (skills().isEmpty()) {
            return Collections.emptyList();
        }
        List<ItemSkill> all = new ArrayList<>();
        for (List<ItemSkill> skillList : skills().values()) {
            all.addAll(skillList);
        }
        return Collections.unmodifiableList(all);
    }

    default boolean isEmpty() {
        return skills().isEmpty();
    }

    default boolean hasSkill(SkillType type) {
        return skills().containsKey(type);
    }

    default boolean hasSkill(ItemSkill skill) {
        return skills().containsKey(skill.getType()) && skills().get(skill.getType()).contains(skill);
    }

    /**
     * 释放技能
     * @param skillStack 技能 ItemStack
     * @param type 技能类型
     * @param context 技能上下文
     * @return true=至少有一个技能被释放（能量预检查通过）
     */
    boolean releaseSkills(SkillItemStack skillStack, SkillType type, Object context);
}
