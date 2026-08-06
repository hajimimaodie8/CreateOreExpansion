package com.hjmmd_8.createoreexpansion.foundation.item.skill.context.impl;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface PlayerContext {
    Player player();

    default ItemStack getMainHand() {
        return player().getMainHandItem();
    }

    default ItemStack getOffHand() {
        return player().getOffhandItem();
    }
}
