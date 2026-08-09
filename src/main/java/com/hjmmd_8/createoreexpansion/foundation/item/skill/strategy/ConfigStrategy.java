package com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;

public abstract class ConfigStrategy<C extends SkillConfig<? extends ItemSkill, ? extends AreaStrategy>> implements AreaStrategy {
    public abstract void load(C config);
}
