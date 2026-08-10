package com.hjmmd_8.createoreexpansion.client.tool.renderer;

import com.hjmmd_8.createoreexpansion.client.tool.SkillRendererConfig;
import com.hjmmd_8.createoreexpansion.client.tool.StrategyRenderer;
import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.EntityStrategy;
import com.hjmmd_8.createoreexpansion.mixin.renderers.EntityRendererAccessor;
import com.hjmmd_8.createoreexpansion.mixin.renderers.LivingEntityRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

public class EntityOutlineRenderer implements StrategyRenderer {
    @SuppressWarnings("unchecked")
    @Override
    public void render(SkillRendererConfig config, ClientLevel world, Camera camera, PoseStack poseStack,
                       SuperRenderTypeBuffer buffer, IParams params) {
        IParams entityTag = params.get("EntityParams", IParams.class);
        if (entityTag.isEmpty()) return;
        Entity entity = entityTag.get("Entity", Entity.class);
        Player player = params.get("Player", Player.class);
        Minecraft mc = Minecraft.getInstance();

        if (!(entity instanceof LivingEntity)) return;

        DataSkill dataSkill = config.skill();
        ItemSkill skill = dataSkill.skill;
        EntityStrategy strategy = (EntityStrategy) skill.getStrategy();
        if (!strategy.shouldRender(dataSkill, world, entityTag)) return;

        Set<Entity> entities = strategy.calculate(dataSkill, entityTag.put("World", world));
        entities.add(entity);

        for (Entity e : entities) {
            if (!(e instanceof LivingEntity livingEntity)) continue;

            EntityRenderer<? super LivingEntity> renderer = mc.getEntityRenderDispatcher()
                    .getRenderer(livingEntity);

            if (!(renderer instanceof LivingEntityRenderer<?, ?> livingRenderer)) continue;
            float partialTicks = mc.getTimer().getGameTimeDeltaPartialTick(true);

            ResourceLocation location = ((EntityRendererAccessor<LivingEntity>) renderer)
                    .invokeGetTextureLocation(livingEntity);
            var accessor = (LivingEntityRendererAccessor<LivingEntity, EntityModel<LivingEntity>>) livingRenderer;
            EntityModel<LivingEntity> model = (EntityModel<LivingEntity>) livingRenderer.getModel();

            boolean flag = !accessor.invokeIsBodyVisible(livingEntity) && !entity.isInvisibleTo(player);
            RenderType type = RenderType.outline(location);

            poseStack.pushPose();
            poseStack.translate(
                    livingEntity.getX(),
                    livingEntity.getY(),
                    livingEntity.getZ()
            );

            VertexConsumer consumer = buffer.getBuffer(type);
            consumer.setColor(config.r(), config.g(), config.b(), config.a());
            int i = LivingEntityRenderer.getOverlayCoords(
                    livingEntity, accessor.invokeGetWhiteOverlayProgress(livingEntity, partialTicks));
            model.renderToBuffer(poseStack, consumer, 0xF000F0, i, flag ? 654311423 : -1);
            buffer.draw(type);

            poseStack.popPose();
        }
    }
}
