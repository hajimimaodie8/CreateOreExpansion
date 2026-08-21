package com.hjmmd_8.createoreexpansion.client;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.client.renderer.EmptyEntityRenderer;
import com.hjmmd_8.createoreexpansion.common.AllEntityTypes;
import com.hjmmd_8.createoreexpansion.common.AllItems;
import com.hjmmd_8.createoreexpansion.common.AllPartialModels;

import net.minecraft.client.renderer.entity.EntityRenderers;
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
        // 早期触发 PartialModel 类加载，确保 Flywheel 模型烘焙前收集到部件模型
        AllPartialModels.init();

        // 充能器能量波：空渲染器（视觉靠粒子）
        EntityRenderers.register(AllEntityTypes.JADE_CHARGER_WAVE.get(), EmptyEntityRenderer::new);

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