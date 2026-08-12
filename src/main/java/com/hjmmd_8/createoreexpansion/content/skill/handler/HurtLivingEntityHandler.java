package com.hjmmd_8.createoreexpansion.content.skill.handler;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.content.skill.context.LivingHurtContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public class HurtLivingEntityHandler {
	@SubscribeEvent
	public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		if (event.getEntity().level().isClientSide)
			return;
		if (!(event.getSource().getDirectEntity() instanceof Player player))
			return;
		if (!AllKeys.SKILL_RELEASE.isPressed()) return;

		ItemStack sword = player.getMainHandItem();
		SkillItemStack skillStack = SkillItemStack.of(sword);
		if (!skillStack.hasSkill(SkillType.HIT_SKILL))
			return;
		if (player.getCooldowns().isOnCooldown(sword.getItem()))
			return;

		trigger(skillStack, event, sword, player);
		player.getCooldowns().addCooldown(sword.getItem(), 20);
	}

	private static void trigger(SkillItemStack skillStack, LivingIncomingDamageEvent event,
								ItemStack stack, Player player) {
		LivingHurtContext context = new LivingHurtContext(event);
		ToolEnergy.sendEnergyMessage(player, stack,
				skillStack.getSkillsHolder().releaseSkills(
						skillStack, SkillType.HIT_SKILL, context));
	}
}