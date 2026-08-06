package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.impl.ExcavationSkillContext;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.impl.HitSkillContext;

public enum SkillType {
    // 挖掘技能
    EXCAVATION_SKILL(ExcavationSkillContext.class),
    // 击中技能
    HIT_SKILL(HitSkillContext.class);

    public final Class<?> contextClass;

    SkillType(Class<?> contextClass) {
        this.contextClass = contextClass;
    }

    public boolean cast(Object context) {
        return contextClass.isInstance(context);
    }
}
