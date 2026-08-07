package com.hjmmd_8.createoreexpansion.content.equipment.tool.handler;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllItems;

import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.common.AllSkills;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public class SapphireSwordSkillHandler {

	private static final float DAMAGE_MULTIPLIER = 1.5F;

	@SubscribeEvent
	public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		if (event.getEntity().level().isClientSide)
			return;
		if (!(event.getSource().getDirectEntity() instanceof Player player))
			return;
		if (!AllKeys.SKILL_RELEASE.isPressed()) return;

		ItemStack sword = player.getMainHandItem();
		if (!sword.is(AllItems.SAPPHIRE_SWORD.get()))
			return;
		if (player.getCooldowns().isOnCooldown(sword.getItem()))
			return;
		if (!ToolEnergy.consumeForSkill(player, sword, AllSkills.SAPPHIRE_AXE_AOE.costProxy()))
			return;

		trigger(player, event.getEntity());
		event.setAmount(event.getAmount() * DAMAGE_MULTIPLIER);
		player.getCooldowns().addCooldown(sword.getItem(), 8 * 20);
	}

	private static void trigger(Player player, LivingEntity target) {
		EquipmentSlot[] slots = { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
		for (EquipmentSlot slot : slots) {
			ItemStack stack = target.getItemBySlot(slot);
			if (stack.isEmpty())
				continue;
			target.setItemSlot(slot, ItemStack.EMPTY);
			if (!player.getInventory().add(stack))
				target.spawnAtLocation(stack);
		}
		player.heal(4.0F);
		target.hurt(player.damageSources().magic(), 4.0F);
	}

}