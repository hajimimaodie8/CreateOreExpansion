package com.hjmmd_8.createoreexpansion.foundation.item.skill.config;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

public interface SkillConfig extends Consumer<CompoundTag> {
    void load(DataSkill data);
}
