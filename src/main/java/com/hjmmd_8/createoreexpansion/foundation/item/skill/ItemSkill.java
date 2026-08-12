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
     * @param data 技能data
     * @throws ClassCastException 技能上下文类型错误
     */
    void release(Object context, DataSkill data);

    /**
     * 检查技能是否可以释放（在消耗能量前调用）
     * @param context 技能上下文
     * @param data 技能data
     * @return true=可以释放，false=条件不满足
     */
    default boolean canRelease(Object context, DataSkill data) {
        return true; // 默认总是可以释放
    }

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
