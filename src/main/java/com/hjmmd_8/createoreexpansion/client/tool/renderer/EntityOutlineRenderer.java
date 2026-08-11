package com.hjmmd_8.createoreexpansion.client.tool.renderer;

import com.google.common.collect.Lists;
import com.hjmmd_8.createoreexpansion.client.tool.SkillRendererConfig;
import com.hjmmd_8.createoreexpansion.client.tool.StrategyRenderer;
import com.hjmmd_8.createoreexpansion.foundation.util.params.IParams;
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
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;
import java.util.Set;

public class EntityOutlineRenderer implements StrategyRenderer {
    public static List<Entity> glowingEntities = Lists.newArrayList();

    @SuppressWarnings("unchecked")
    @Override
    public void render(SkillRendererConfig config, ClientLevel world, Camera camera, PoseStack poseStack,
                       SuperRenderTypeBuffer buffer, IParams params) {
        glowingEntities.clear();
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

        OutlineBufferSource outlineBuffer = mc.renderBuffers().outlineBufferSource();
        outlineBuffer.setColor(
                (int)(config.r() * 255),
                (int)(config.g() * 255),
                (int)(config.b() * 255),
                (int)(config.a() * 255)
        );

        for (Entity e : entities) {
            if (!(e instanceof LivingEntity livingEntity)) continue;

            EntityRenderer<? super LivingEntity> renderer = mc.getEntityRenderDispatcher()
                    .getRenderer(livingEntity);

            if (!(renderer instanceof LivingEntityRenderer<?, ?> livingRenderer)) continue;
            var accessor = (LivingEntityRendererAccessor<LivingEntity, EntityModel<LivingEntity>>) livingRenderer;

            float partialTicks = mc.getTimer().getGameTimeDeltaPartialTick(true);

            poseStack.pushPose();
            poseStack.translate(
                    livingEntity.getX(),
                    livingEntity.getY(),
                    livingEntity.getZ()
            );

            float f = Mth.rotLerp(partialTicks, livingEntity.yBodyRotO, livingEntity.yBodyRot);
            float f1 = Mth.rotLerp(partialTicks, livingEntity.yHeadRotO, livingEntity.yHeadRot);
            float f2 = f1 - f;

            f2 = Mth.wrapDegrees(f2);
            if (livingEntity.hasPose(Pose.SLEEPING)) {
                Direction direction = livingEntity.getBedOrientation();
                if (direction != null) {
                    float f3 = livingEntity.getEyeHeight(Pose.STANDING) - 0.1F;
                    poseStack.translate((float)(-direction.getStepX()) * f3, 0.0F, (float)(-direction.getStepZ()) * f3);
                }
            }

            float f8 = livingEntity.getScale();
            poseStack.scale(f8, f8, f8);
            float f9 = accessor.invokeGetBob(livingEntity, partialTicks);
            accessor.invokeSetupRotations(livingEntity, poseStack, f9, f, partialTicks, f8);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            accessor.invokeScale(livingEntity, poseStack, partialTicks);
            poseStack.translate(0.0F, -1.501F, 0.0F);

            ResourceLocation location = ((EntityRendererAccessor<LivingEntity>) renderer)
                    .invokeGetTextureLocation(livingEntity);
            EntityModel<LivingEntity> model = (EntityModel<LivingEntity>) livingRenderer.getModel();

            boolean flag = !accessor.invokeIsBodyVisible(livingEntity) && !livingEntity.isInvisibleTo(player);
            RenderType type = RenderType.outline(location);
            glowingEntities.add(e);

            VertexConsumer consumer = outlineBuffer.getBuffer(type);
            consumer.setColor(config.r(), config.g(), config.b(), config.a());
            int i = LivingEntityRenderer.getOverlayCoords(
                    livingEntity, accessor.invokeGetWhiteOverlayProgress(livingEntity, partialTicks));
            model.renderToBuffer(poseStack, consumer, 0xF000F0, i, flag ? 654311423 : -1);

            poseStack.popPose();
        }
        outlineBuffer.endOutlineBatch();
    }

    @Override
    public RenderLevelStageEvent.Stage getStage() {
        return RenderLevelStageEvent.Stage.AFTER_ENTITIES;
    }
}
