package com.hjmmd_8.createoreexpansion.foundation.item.skill;

public interface SkillCostModifier {
    SkillCostModifier DEFAULT = (skill, cost) -> cost;

    static SkillCostModifier ratio(float ratio) {
        return (skill, cost) -> (int) (cost * ratio);
    }

    static SkillCostModifier fixed(int cost) {
        return (skill, originalCost) -> cost;
    }

    int modify(ItemSkill skill, int cost);
}
