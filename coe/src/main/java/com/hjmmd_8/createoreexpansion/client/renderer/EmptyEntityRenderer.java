package com.hjmmd_8.createoreexpansion.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * 空渲染器：不渲染任何模型（用于纯粒子视觉的实体，如充能器能量波）。
 */
public class EmptyEntityRenderer<T extends Entity> extends EntityRenderer<T> {

	public EmptyEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(T entity) {
		return null;
	}

	@Override
	public void render(T entity, float yaw, float partialTicks, PoseStack poseStack,
					   MultiBufferSource buffer, int light) {
	}
}
