package com.hjmmd_8.createoreexpansion.mixin.renderers;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor<T extends LivingEntity, M extends EntityModel<T>> {

    @Invoker("getWhiteOverlayProgress")
    float invokeGetWhiteOverlayProgress(T livingEntity, float partialTicks);

    @Invoker("isBodyVisible")
    boolean invokeIsBodyVisible(T livingEntity);
}
