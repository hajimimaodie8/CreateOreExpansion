package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.common.AllSkills;
import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.AbstractSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.SkillStrategy;

import java.util.Set;

/**
 * 策略技能抽象基类 - 定义技能与Strategy的强类型依赖关系
 *
 * <p>每个具体技能类都应该继承此类并指定它使用的Strategy类型。
 * 这样可以确保类型安全，防止配置错误。</p>
 *
 * @param <S> 此技能使用的Strategy类型（必须继承AreaStrategy）
 */
public abstract class AbstractStrategySkill<P, S extends SkillStrategy<P>, C extends SkillConfig<?, S>> extends AbstractSkill {

    private final S strategy;

    /**
     * 创建策略技能
     * @param strategy 此技能使用的策略实例（可以为null）
     */
    protected AbstractStrategySkill(S strategy) {
        super();
        this.strategy = strategy; // 允许null，支持没有strategy的技能
    }

    /**
     * 获取此技能使用的策略
     * @return 策略实例，可能为null
     */
    public final S strategy() {
        return strategy;
    }

    /**
     * 获取策略类型（用于反射等场景）
     * @return 策略的Class对象，如果strategy为null则返回null
     */
    @SuppressWarnings("unchecked")
    public final Class<S> getStrategyType() {
        if (strategy == null) return null;
        return (Class<S>) strategy.getClass();
    }

    public final Set<P> calculate(DataSkill data, IParams params) {
        if (strategy == null) {
            return java.util.Collections.emptySet();
        }
        return strategy.calculate(data, params);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "strategy=" + (strategy != null ? strategy : "null") +
                ", cost=" + getCost() +
                ", id=" + AllSkills.getId(this) +
                '}';
    }

    public void load(C config) {

    }
}
