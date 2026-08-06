package com.hjmmd_8.createoreexpansion.foundation.item.skill.attribute;

import java.util.List;
import java.util.Map;

public interface SkillAttributeModifierHolder {
    Map<ModifiableAttributeType<?, ?>, List<SkillAttributeModifier<?>>> modifiers();

    @SuppressWarnings({"unchecked", "rawtypes"})
    default <C, V> ModifiableAttribute<V> modifier(ModifiableAttributeType<C, V> type, ModifiableAttribute<V> attribute) {
        modifiers().get(type)
                .forEach(modifier -> ((SkillAttributeModifier) modifier).modify(attribute));
        return attribute;
    }
}
