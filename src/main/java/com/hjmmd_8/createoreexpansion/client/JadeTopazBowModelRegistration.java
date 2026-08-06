package com.hjmmd_8.createoreexpansion.client;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllItems;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID, value = Dist.CLIENT)
public class JadeTopazBowModelRegistration {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(AllItems.JADE_TOPAZ_BOW.get(),
                ResourceLocation.withDefaultNamespace("pull"),
                (stack, level, entity, seed) -> {
                    if (entity == null)
                        return 0.0F;
                    return entity.getUseItem() != stack ? 0.0F
                        : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 25.0F;
                });

            ItemProperties.register(AllItems.JADE_TOPAZ_BOW.get(),
                ResourceLocation.withDefaultNamespace("pulling"),
                (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
        });
    }

}