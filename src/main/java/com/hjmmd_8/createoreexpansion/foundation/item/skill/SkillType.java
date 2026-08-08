package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.data.lang.COELangProvider;
import com.hjmmd_8.createoreexpansion.data.lang.Translatable;
import com.hjmmd_8.createoreexpansion.data.lang.Translator;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.impl.ExcavationSkillContext;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.impl.HitSkillContext;

import java.util.Locale;

public enum SkillType {
    // 挖掘技能
    EXCAVATION_SKILL(ExcavationSkillContext.class),
    // 击中技能
    HIT_SKILL(HitSkillContext.class);

    public final Class<?> contextClass;
    private final String translateKey;
    public final Translatable translatable;

    SkillType(Class<?> contextClass) {
        this.contextClass = contextClass;
        translateKey = "skillType." + CreateOreExpansion.MOD_ID + "." + this.name().toLowerCase(Locale.ROOT);
        translatable = () -> translateKey;
    }

    public boolean cast(Object context) {
        return contextClass.isInstance(context);
    }

    private enum SkillTypeTranslator implements Translator {
        INSTANCE;

        @Override
        public COELangProvider.Builder translate(COELangProvider.Builder builder) {
            return builder
                    .add(EXCAVATION_SKILL.translatable, "挖掘技能", "Excavation Skill")
                    .add(HIT_SKILL.translatable, "攻击技能", "Hit Skill")
                    ;
        }
    }

    public static COELangProvider.Builder translate(COELangProvider.Builder builder) {
        return builder
                .add(SkillTypeTranslator.INSTANCE);
    }
}
