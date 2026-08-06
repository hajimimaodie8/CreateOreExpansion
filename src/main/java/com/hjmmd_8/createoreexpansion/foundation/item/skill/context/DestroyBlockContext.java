package com.hjmmd_8.createoreexpansion.foundation.item.skill.context;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.impl.ExcavationSkillContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public record DestroyBlockContext(Level level, BlockPos pos, ItemStack tool,
                                  LivingEntity entity) implements ExcavationSkillContext { }
