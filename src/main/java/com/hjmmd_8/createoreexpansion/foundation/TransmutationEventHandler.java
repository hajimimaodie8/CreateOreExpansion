package com.hjmmd_8.createoreexpansion.foundation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllItems;
import com.hjmmd_8.createoreexpansion.common.AllModEffects;
import com.hjmmd_8.createoreexpansion.common.AllFluids;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.IMedallion;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public final class TransmutationEventHandler {

	private static final Map<UUID, Integer> FLUID_CONTACT_TICKS = new HashMap<>();

	private TransmutationEventHandler() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide)
			return;
		if (player.isSpectator())
			return;

		if (player.getFluidTypeHeight(AllFluids.TRANSMUTATION_FLUID.get().getFluidType()) > 0.0D) {
			// 佩戴星辉石凝能佩时免疫嬗乱（双保险：这里直接拦截 + MobEffectEvent.Applicable 兜底）
			if (IMedallion.isWearing(player, AllItems.STELLARSTONE_STRESS_MEDALLION.get()))
				return;
			int contactTicks = FLUID_CONTACT_TICKS.merge(player.getUUID(), 1, Integer::sum);
			int level = 1 + contactTicks / (15 * 20);
			player.addEffect(new MobEffectInstance(AllModEffects.TRANSMUTATION_DISORDER, 60, level - 1));
		} else {
			FLUID_CONTACT_TICKS.remove(player.getUUID());
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof Player player)
			FLUID_CONTACT_TICKS.remove(player.getUUID());
	}

}