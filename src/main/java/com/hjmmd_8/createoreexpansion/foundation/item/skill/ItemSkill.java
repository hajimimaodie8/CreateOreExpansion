package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.common.AllSkills;
import com.hjmmd_8.createoreexpansion.common.AllStrategies;
import com.hjmmd_8.createoreexpansion.data.lang.Translatable;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.AreaStrategy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.SkillStrategy;
import net.minecraft.resources.ResourceLocation;

public interface ItemSkill extends Translatable {

    /**
     * 释放技能
     * @param context 技能上下文
     * @param data 技能 data
     * @throws ClassCastException 技能上下文类型错误
     */
    void release(Object context, DataSkill data);

    SkillType getType();

    default int getCost() {
        return 100;
    }

    @Override
    default String getTranslateKey() {
        ResourceLocation id = AllSkills.getId(this);
        return "skill." + id.getNamespace() + "." + id.getPath();
    }

    default ResourceLocation getId() {
        return AllSkills.getId(this);
    }

    default SkillStrategy<?> getStrategy() {
        return AllStrategies.STRATEGIES.get(this);
    }
}
