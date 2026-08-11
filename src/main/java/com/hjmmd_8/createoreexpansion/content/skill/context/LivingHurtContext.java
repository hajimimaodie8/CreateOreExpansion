package com.hjmmd_8.createoreexpansion.content.skill.context;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.HitSkillContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public record LivingHurtContext(LivingIncomingDamageEvent event) implements HitSkillContext {

    @Override
    public LivingEntity target() {
        return event.getEntity();
    }

    @Override
    public Player player() {
        if (!(event.getSource().getEntity() instanceof Player player)) return null;
        return player;
    }
}
