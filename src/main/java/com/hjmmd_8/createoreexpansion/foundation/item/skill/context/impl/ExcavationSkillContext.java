package com.hjmmd_8.createoreexpansion.foundation.item.skill.context.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface ExcavationSkillContext {
    Level level();
    BlockPos pos();
    ItemStack tool();
    LivingEntity entity();
}
