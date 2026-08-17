package com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy;

import com.google.common.collect.Sets;
import com.hjmmd_8.createoreexpansion.client.tool.AllStrategyRenderers;
import com.hjmmd_8.createoreexpansion.client.tool.StrategyRenderer;
import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import net.minecraft.world.entity.Entity;

import java.util.Set;

/**
 * 实体策略 —— 标记技能作用于实体（渲染器为实体轮廓）。
 * 实体计算由渲染器直接基于准星目标完成，此处返回空集合。
 */
public class EntityStrategy implements SkillStrategy<Entity> {

    @Override
    public Set<Entity> calculate(DataSkill skill, IParams params) {
        return Sets.newHashSet();
    }

    @Override
    public StrategyRenderer getRenderer() {
        return AllStrategyRenderers.Renderers.ENTITY;
    }
}
