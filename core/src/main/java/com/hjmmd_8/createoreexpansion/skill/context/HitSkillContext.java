package com.hjmmd_8.createoreexpansion.skill.context;

import net.minecraft.world.entity.LivingEntity;

public interface HitSkillContext extends PlayerContext {
    LivingEntity target();
}
