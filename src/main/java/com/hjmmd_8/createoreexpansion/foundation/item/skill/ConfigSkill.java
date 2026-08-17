package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;

/**
 * 配置型技能 - 技能参数由 {@link SkillConfig} 提供。
 *
 * <p>所有配置型技能在注册时都会通过
 * {@link #loadConfig(ItemSkill, SkillConfig, DataSkill)} 完成一次初始化，
 * 因此 {@link ItemSkill#getCost()} 从注册起就返回真实消耗值。</p>
 *
 * @param <T> 技能上下文类型
 * @param <C> 技能配置类型
 */
public interface ConfigSkill<T, C extends SkillConfig> extends ItemSkill {
    void load(C config, DataSkill data);

    Class<C> getConfigType();

    void release(T context);

    @Override
    default void release(Object context, DataSkill data) {
        C config = data.getConfig(getConfigType());
        if (config == null) {
            // 缺少配置（例如未经注册直接构造的技能），无法执行
            return;
        }
        load(config, data);
        SkillType type = getType();
        if (type.isInstance(context)) {
            @SuppressWarnings("unchecked")
            T typedContext = (T) context;
            release(typedContext);
        }
    }

    /**
     * 将配置载入技能实例（若该技能实现了 ConfigSkill）。
     *
     * <p>在 {@link com.hjmmd_8.createoreexpansion.common.AllSkills.SkillBuilder#register()}
     * 中调用，保证技能实例的字段（如能量消耗）在注册后即为真实值。</p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void loadConfig(ItemSkill skill, SkillConfig config, DataSkill data) {
        if (skill instanceof ConfigSkill configSkill) {
            configSkill.load(config, data);
        }
    }
}
