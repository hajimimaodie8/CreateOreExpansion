package com.hjmmd_8.createoreexpansion.content.skill.handler;

import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.content.skill.context.RightClickItemContext;
import com.hjmmd_8.createoreexpansion.content.skill.context.UseOnBlockContext;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.UseItemContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

@EventBusSubscriber
public class UseItemHandler {
    @SubscribeEvent
    public static void onUseOnBlock(UseItemOnBlockEvent event) {
        if (event.getPlayer() == null) return;
        releaseSkills(event.getPlayer(), event.getItemStack(), new UseOnBlockContext(event));
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        releaseSkills(event.getEntity(), event.getItemStack(), new RightClickItemContext(event));
    }

    private static void releaseSkills(Player player, ItemStack stack, UseItemContext<?> context) {
        if (player.level().isClientSide()) return;
        SkillItemStack skillStack = SkillItemStack.of(stack);
        if (!skillStack.hasSkill(SkillType.USE_SKILL)) return;
        ToolEnergy.sendEnergyMessage(player, stack,
                skillStack.getSkillsHolder().releaseSkills(
                        skillStack, SkillType.USE_SKILL, context));
    }
}
