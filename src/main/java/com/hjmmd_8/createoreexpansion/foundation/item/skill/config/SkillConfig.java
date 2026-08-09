package com.hjmmd_8.createoreexpansion.foundation.item.skill.config;

import com.hjmmd_8.createoreexpansion.content.skill.AbstractStrategySkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.AreaStrategy;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

public interface SkillConfig<S extends ItemSkill, T extends AreaStrategy> extends Consumer<CompoundTag> {
    void read(DataSkill data);
    void loadSkill(S skill);
    void loadStrategy(T strategy);

    default void load(DataSkill data, S skill, T strategy) {
        read(data);
        loadStrategy(strategy);
        loadSkill(skill);
    }

    @SuppressWarnings("unchecked")
    default void load(DataSkill data) {
        read(data);
        loadStrategy((T) ((AbstractStrategySkill<?, ?>) data.skill).strategy());
        loadSkill((S) data.skill);
    }
}
