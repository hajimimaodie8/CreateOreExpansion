package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnusedReturnValue")
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

    default OwnedBySkills addSkill(ItemSkill skill) {
        if (!skills().containsKey(skill.getType())) {
            skills().put(skill.getType(), new ArrayList<>());
        }
        skills().get(skill.getType()).add(skill);
        return this;
    }

    default OwnedBySkills removeSkill(SkillType type) {
        skills().remove(type);
        return this;
    }

    default OwnedBySkills removeSkill(ItemSkill skill) {
        SkillType type = skill.getType();
        if (hasSkill(type)) {
            skills().get(type).remove(skill);
        }
        return this;
    }

    /**
     * 释放技能
     * @param skillStack 技能 ItemStack
     * @param type 技能类型
     * @param context 技能上下文
     * @throws ClassCastException 技能上下文类型不匹配
     */
    boolean releaseSkills(SkillItemStack skillStack, SkillType type, Object context);
}
