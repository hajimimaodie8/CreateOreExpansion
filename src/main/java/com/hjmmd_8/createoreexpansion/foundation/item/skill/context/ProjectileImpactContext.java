package com.hjmmd_8.createoreexpansion.foundation.item.skill.context;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.impl.HitSkillContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public record ProjectileImpactContext(Player player, LivingEntity target) implements HitSkillContext { }
