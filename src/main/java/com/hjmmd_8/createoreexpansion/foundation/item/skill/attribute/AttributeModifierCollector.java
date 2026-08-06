package com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute;

import java.util.List;
import java.util.Map;

public class AttributeModifierCollector implements SkillAttributeModifierHolder {
    private final Map<ModifiableAttributeType<?, ?>, List<SkillAttributeModifier<?>>> modifiers = new java.util.HashMap<>();
    public int count = 0;

    public AttributeModifierCollector collect(SkillAttributeModifierHolder holder) {
        modifiers.putAll(holder.modifiers());
        count++;
        return this;
    }

    @Override
    public Map<ModifiableAttributeType<?, ?>, List<SkillAttributeModifier<?>>> modifiers() {
        return modifiers;
    }
}
