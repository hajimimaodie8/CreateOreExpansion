package com.hjmmd_8.createoreexpansion.skill;

import com.hjmmd_8.createoreexpansion.common.i18n.Translatable;
import com.hjmmd_8.createoreexpansion.skill.context.ExcavationSkillContext;
import com.hjmmd_8.createoreexpansion.skill.context.HitSkillContext;
import com.hjmmd_8.createoreexpansion.skill.context.UseItemContext;

import java.util.Locale;
import com.hjmmd_8.createoreexpansion.common.CoeCore;

/**
 * 技能类型 —— 决定技能由哪种上下文触发。
 *
 * <p>类型名称的翻译统一由语言 Provider 管理，此处只提供翻译键。</p>
 */
public enum SkillType {
    // 挖掘技能
    EXCAVATION_SKILL(ExcavationSkillContext.class),
    // 击中技能
    HIT_SKILL(HitSkillContext.class),

    USE_SKILL(UseItemContext.class),
    ;

    public final Class<?> contextClass;
    private final String translateKey;
    public final Translatable translatable;

    SkillType(Class<?> contextClass) {
        this.contextClass = contextClass;
        translateKey = "skillType." + CoeCore.REGISTRY_NAMESPACE + "." + this.name().toLowerCase(Locale.ROOT);
        translatable = () -> translateKey;
    }

    public boolean isInstance(Object context) {
        return contextClass.isInstance(context);
    }
}
