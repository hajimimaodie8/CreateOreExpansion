package com.hjmmd_8.createoreexpansion.skill.attribute;

public interface SkillAttributeModifier<V> {
    void modify(ModifiableAttribute<V> attribute);
}
