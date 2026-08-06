package com.hjmmd_8.createoreexpansion.foundation;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.ToolEnergy;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public class EnergyHudHandler {

	@SubscribeEvent
	public static void onBlockBreak(BlockEvent.BreakEvent event) {
		if (event.getPlayer() instanceof Player player) {
			ItemStack stack = player.getMainHandItem();
			if (ToolEnergy.hasEnergy(stack))
				ToolEnergy.sendEnergyActionBar(player, stack);
		}
	}

}