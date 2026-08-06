package com.hjmmd_8.createoreexpansion.foundation.item.skill;

public class SkillCostProxy {
    public final ItemSkill skill;
    public SkillCostModifier modifier = SkillCostModifier.DEFAULT;

    public SkillCostProxy(ItemSkill skill) {
        this.skill = skill;
    }

    public SkillCostProxy modifier(SkillCostModifier modifier) {
        this.modifier = modifier;
        return this;
    }

    public int getCost() {
        return modifier.modify(skill, skill.getCost());
    }
}
