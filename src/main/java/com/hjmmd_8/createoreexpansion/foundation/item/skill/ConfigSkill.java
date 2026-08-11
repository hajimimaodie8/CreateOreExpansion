package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;

public interface ConfigSkill<T, C extends SkillConfig> extends ItemSkill {
    void load(C config, DataSkill data);
    Class<C> getConfigType();
    void release(T context);

    @Override
    default void release(Object context, DataSkill data) {
        load(data.getConfig(getConfigType()), data);
        SkillType type = getType();
        if (type.cast(context)) {
            @SuppressWarnings("unchecked")
            T typedContext = (T) context;
            release(typedContext);
        }
    }
}
