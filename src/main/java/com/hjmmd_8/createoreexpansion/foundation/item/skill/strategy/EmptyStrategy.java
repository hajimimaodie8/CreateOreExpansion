package com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy;

import com.hjmmd_8.createoreexpansion.client.tool.StrategyRenderer;
import com.hjmmd_8.createoreexpansion.common.AllStrategies;
import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;

import java.util.Set;

public class EmptyStrategy implements SkillStrategy<EmptyStrategy.EmptyValue> {
    @Override
    public Set<EmptyValue> calculate(DataSkill skill, IParams params) {
        return Set.of();
    }

    @Override
    public StrategyRenderer getRenderer() {
        return AllStrategies.Renderers.EMPTY;
    }

    public static class EmptyValue {}
}
