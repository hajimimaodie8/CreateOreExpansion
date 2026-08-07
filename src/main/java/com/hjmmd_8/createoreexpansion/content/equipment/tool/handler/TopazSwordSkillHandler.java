package com.hjmmd_8.createoreexpansion.content.equipment.tool.handler;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllItems;

import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillCostModifier;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillCostProxy;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public class TopazSwordSkillHandler {

	private static final float DAMAGE_MULTIPLIER = 1.5F;

	@SubscribeEvent
	public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		if (event.getEntity().level().isClientSide)
			return;
		if (!(event.getSource().getDirectEntity() instanceof Player player))
			return;
		if (!AllKeys.SKILL_RELEASE.isPressed()) return;

		ItemStack sword = player.getMainHandItem();
		if (!sword.is(AllItems.TOPAZ_SWORD.get()))
			return;
		if (player.getCooldowns().isOnCooldown(sword.getItem()))
			return;
		if (!ToolEnergy.consumeForSkill(player, sword, new SkillCostProxy(null).modifier(SkillCostModifier.fixed(100))))
			return;

		trigger(player, event.getEntity());
		event.setAmount(event.getAmount() * DAMAGE_MULTIPLIER);
		player.getCooldowns().addCooldown(sword.getItem(), 5 * 20);
	}

	private static void trigger(Player player, LivingEntity target) {
		ItemStack mainHand = target.getMainHandItem();
		if (!mainHand.isEmpty()) {
			target.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			transferOrDrop(player, target, mainHand);
			return;
		}

		EquipmentSlot[] armorSlots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
		for (EquipmentSlot slot : armorSlots) {
			ItemStack stack = target.getItemBySlot(slot);
			if (!stack.isEmpty()) {
				target.setItemSlot(slot, ItemStack.EMPTY);
				transferOrDrop(player, target, stack);
				return;
			}
		}

		ItemStack offhand = target.getItemBySlot(EquipmentSlot.OFFHAND);
		if (!offhand.isEmpty()) {
			target.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
			transferOrDrop(player, target, offhand);
		}
	}

	private static void transferOrDrop(Player player, LivingEntity target, ItemStack stack) {
		if (player.level().getRandom().nextFloat() < 0.5F && player.getInventory().add(stack))
			return;
		if (!stack.isEmpty())
			target.spawnAtLocation(stack);
	}

}