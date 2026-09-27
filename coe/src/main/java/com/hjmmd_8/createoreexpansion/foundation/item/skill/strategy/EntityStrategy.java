package com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy;

import com.google.common.collect.Sets;
import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import net.minecraft.world.entity.Entity;

import java.util.Set;

/**
 * 实体策略 —— 标记技能作用于实体。
 * 实体计算由渲染器直接基于准星目标完成，此处返回空集合。
 *
 * <p><b>W2 渲染统一</b>：原先这里覆写 {@code getRenderer()} 返回旧
 * {@code AllStrategyRenderers.Renderers.ENTITY}；旧渲染栈删除后该覆写随之删除
 * （{@link SkillStrategy} 也已不再声明这个渲染 API）。实体描边预览现在由新路径的
 * {@code CoeEntityOutlineRenderer} 承担，策略只需在自己的 {@code collect} 里
 * 说明"算哪些实体"、渲染器由 {@code getRendererId()} 找到。</p>
 */
public class EntityStrategy implements SkillStrategy<Entity> {

    @Override
    public Set<Entity> calculate(DataSkill skill, IParams params) {
        return Sets.newHashSet();
    }
}
