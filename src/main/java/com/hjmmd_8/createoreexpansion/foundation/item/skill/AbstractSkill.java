package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.ModifiableAttributeType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.SkillAttributeModifier;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute.SkillAttributeModifierHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class AbstractSkill implements ItemSkill, SkillAttributeModifierHolder {

    protected final Map<ModifiableAttributeType<?, ?>, List<SkillAttributeModifier<?>>> modifiers;

    public AbstractSkill() {
        modifiers = new java.util.HashMap<>();
    }

    @Override
    public Map<ModifiableAttributeType<?, ?>, List<SkillAttributeModifier<?>>> modifiers() {
        return modifiers;
    }

    protected <C, V> AbstractSkill addModifier(ModifiableAttributeType<C, V> type, SkillAttributeModifier<V> modifier) {
        modifiers.computeIfAbsent(type, k -> new ArrayList<>()).add(modifier);
        return this;
    }

    protected <C, V> AbstractSkill addModifiers(ModifiableAttributeType<C, V> type, SkillAttributeModifier<V>... modifiers) {
        this.modifiers.computeIfAbsent(type, k -> new ArrayList<>())
                .addAll(Arrays.stream(modifiers).toList());
        return this;
    }
}
