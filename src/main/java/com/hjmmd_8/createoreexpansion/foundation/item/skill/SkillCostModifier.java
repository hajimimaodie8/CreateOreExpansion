package com.hjmmd_8.createoreexpansion.foundation.item.skill;

public interface SkillCostModifier {
    SkillCostModifier DEFAULT = (skill, cost) -> cost;

    static SkillCostModifier ratio(float ratio) {
        return (skill, cost) -> (int) (cost * ratio);
    }

    static SkillCostModifier fixed(int cost) {
        return (skill, originalCost) -> cost;
    }

    static SkillCostModifier merge(SkillCostModifier a, SkillCostModifier b) {
        return (skill, cost) -> b.modify(skill, a.modify(skill, cost));
    }

    int modify(ItemSkill skill, int cost);

    default SkillCostModifier merge(SkillCostModifier other) {
        return (skill, cost) -> other.modify(skill, modify(skill, cost));
    }
}
