package com.hjmmd_8.createoreexpansion.foundation.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface ItemSkill {
    void causeAoe(Level level, BlockPos pos, BlockState state, ItemStack pickaxe, LivingEntity livingEntity);
}
