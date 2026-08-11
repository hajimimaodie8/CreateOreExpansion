package com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy;

import com.google.common.collect.Sets;
import com.hjmmd_8.createoreexpansion.client.tool.StrategyRenderer;
import com.hjmmd_8.createoreexpansion.common.AllStrategies;
import com.hjmmd_8.createoreexpansion.foundation.util.params.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import net.minecraft.world.entity.Entity;

import java.util.Set;

public class EntityStrategy implements SkillStrategy<Entity> {
    @Override
    public Set<Entity> calculate(DataSkill skill, IParams params) {
        return Sets.newHashSet();
    }

    @Override
    public StrategyRenderer getRenderer() {
        return AllStrategies.Renderers.ENTITY;
    }
}
