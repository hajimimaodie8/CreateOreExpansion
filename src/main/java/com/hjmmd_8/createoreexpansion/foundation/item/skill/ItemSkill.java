package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.common.AllSkills;
import com.hjmmd_8.createoreexpansion.data.COELangProvider;
import net.minecraft.resources.ResourceLocation;

public interface ItemSkill extends COELangProvider.Translatable {

    /**
     * 释放技能
     * @param context 技能上下文
     * @throws ClassCastException 技能上下文类型错误
     */
    void release(Object context);

    SkillType getType();

    default int getCost() {
        return 100;
    }

    default SkillCostProxy costProxy() {
        return new SkillCostProxy(this);
    }

    @Override
    default String getTranslateKey() {
        ResourceLocation id = AllSkills.getId(this);
        return "skill." + id.getNamespace() + "." + id.getPath();
    }
}
