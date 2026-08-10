package com.hjmmd_8.createoreexpansion.foundation.item.skill.config;

import com.hjmmd_8.createoreexpansion.content.skill.AbstractStrategySkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.SkillStrategy;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

public interface SkillConfig<S extends ItemSkill, T extends SkillStrategy<?>> extends Consumer<CompoundTag> {
    void read(DataSkill data);
    void loadSkill(S skill);
    void loadStrategy(T strategy);

    default void load(DataSkill data, S skill, T strategy) {
        read(data);
        if (strategy != null) {
            loadStrategy(strategy);
        }
        loadSkill(skill);
    }

    @SuppressWarnings("unchecked")
    default void load(DataSkill data) {
        read(data);

        // 只对AbstractStrategySkill类型的技能加载strategy
        if (data.skill instanceof AbstractStrategySkill<?, ?, ?> strategySkill) {
            T strategy = (T) strategySkill.getStrategy();
            if (strategy != null) {
                loadStrategy(strategy);
            }
        }

        loadSkill((S) data.skill);
    }
}
