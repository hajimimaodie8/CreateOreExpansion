package com.hjmmd_8.createoreexpansion.content.equipment.tool.handler;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllItems;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public class JadeSwordSkillHandler {

	private static final float DAMAGE_MULTIPLIER = 1.5F;

	@SubscribeEvent
	public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
		if (event.getEntity().level().isClientSide)
			return;
		if (!(event.getSource().getDirectEntity() instanceof Player player))
			return;
		if (!player.isShiftKeyDown())
			return;

		ItemStack sword = player.getMainHandItem();
		if (!sword.is(AllItems.JADE_SWORD.get()))
			return;
		if (player.getCooldowns().isOnCooldown(sword.getItem()))
			return;

		trigger(player, event.getEntity());
		event.setAmount(event.getAmount() * DAMAGE_MULTIPLIER);
		player.getCooldowns().addCooldown(sword.getItem(), 3 * 20);
	}

	private static void trigger(Player player, LivingEntity target) {
		if (target.level().getRandom().nextFloat() >= 0.75F)
			return;
		ItemStack held = target.getMainHandItem();
		if (held.isEmpty())
			return;
		target.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		target.spawnAtLocation(held);
	}

}