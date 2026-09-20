package com.hjmmd_8.createoreexpansion.integration.skiller.client;

import com.hjmmd_8.createoreexpansion.client.tool.SkillRendererConfig;
import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.mixin.renderers.EntityRendererAccessor;
import com.hjmmd_8.createoreexpansion.mixin.renderers.LivingEntityRendererAccessor;
import com.leaf.skiller.foundation.renderer.StrategyRenderer;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.StrategySkill;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;
import java.util.Optional;

/**
 * 生物描边预览（新内核版）——把旧 {@code client/tool/renderer/EntityOutlineRenderer} 搬过来。
 *
 * <p>目标实体取法与原实现一致：看 {@code Minecraft#hitResult}，是 {@link EntityHitResult}
 * 才描边（看方块/看空就不画）。描边本体（皮肤贴图 outline + 模型渲染 + 换色）逐行照抄，
 * 连"睡眠姿态偏移""按比例缩放""-1.501 的模型原点"这些细节都没改。</p>
 *
 * <p><b>相机位移</b>：旧实现由调度器统一做（push → translate(-camPos) → 画 → pop），
 * 这里同样由本渲染器自己做一遍——上次就是漏了它，导致框被画到两倍距离外。</p>
 *
 * @since 1.0.0
 */
public class CoeEntityOutlineRenderer implements StrategyRenderer<EntityOutlineRenderContext> {

    @Override
    public Optional<EntityOutlineRenderContext> getContext(Minecraft mc, ClientLevel level, Player player,
                                                            ISkillInstance<EntityOutlineRenderContext> instance) {
        if (mc == null || level == null || player == null || instance == null) {
            return Optional.empty();
        }
        // 与方块预览同一道门：不按技能键不显示
        if (!AllKeys.SKILL_RELEASE.isPressed()
                && !AllKeys.SKILL_RELEASE_2.isPressed()
                && !AllKeys.SKILL_RELEASE_3.isPressed()) {
            return Optional.empty();
        }
        // 目标实体 = 准星命中（原实现就是读 Minecraft#hitResult，而不是自己做射线）
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) {
            return Optional.empty();
        }
        if (!(((EntityHitResult) hit).getEntity() instanceof LivingEntity target)) {
            return Optional.empty();
        }
        // 技能本体必须是策略技能，且该策略允许收集（与释放侧判定同源）
        if (!(instance.skill().getSkill() instanceof StrategySkill<?, ?> strategySkill)
                || strategySkill.strategy() == null) {
            return Optional.empty();
        }
        CompoundTag color = instance.data() == null ? null : instance.data().getCompound("OutlineColor");
        float red = color != null && color.contains("r") ? color.getFloat("r") : 1.0F;
        float green = color != null && color.contains("g") ? color.getFloat("g") : 1.0F;
        float blue = color != null && color.contains("b") ? color.getFloat("b") : 1.0F;
        return Optional.of(new EntityOutlineRenderContext(
                player, target, red, green, blue, SkillRendererConfig.ALPHA));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void render(EntityOutlineRenderContext context, ISkillInstance<EntityOutlineRenderContext> instance,
                       Minecraft mc, PoseStack poseStack, Camera camera, MultiBufferSource buffer) {
        if (context == null || camera == null) {
            return;
        }
        LivingEntity livingEntity = context.target();
        if (livingEntity == null) {
            return;
        }
        Player player = context.getPlayer();

        // 相机位移（旧调度器统一做的这一步）
        Vec3 camPos = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        OutlineBufferSource outlineBuffer = mc.renderBuffers().outlineBufferSource();
        outlineBuffer.setColor(
                (int) (context.red() * 255),
                (int) (context.green() * 255),
                (int) (context.blue() * 255),
                (int) (context.alpha() * 255));

        EntityRenderer<? super LivingEntity> renderer = mc.getEntityRenderDispatcher().getRenderer(livingEntity);
        if (!(renderer instanceof LivingEntityRenderer<?, ?> livingRenderer)) {
            poseStack.popPose();
            return;
        }
        LivingEntityRendererAccessor<LivingEntity, EntityModel<LivingEntity>> accessor =
                (LivingEntityRendererAccessor<LivingEntity, EntityModel<LivingEntity>>) livingRenderer;

        float partialTicks = mc.getTimer().getGameTimeDeltaPartialTick(true);

        poseStack.pushPose();
        poseStack.translate(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());

        float f = Mth.rotLerp(partialTicks, livingEntity.yBodyRotO, livingEntity.yBodyRot);
        float f1 = Mth.rotLerp(partialTicks, livingEntity.yHeadRotO, livingEntity.yHeadRot);
        float f2 = Mth.wrapDegrees(f1 - f);
        if (livingEntity.hasPose(Pose.SLEEPING)) {
            Direction direction = livingEntity.getBedOrientation();
            if (direction != null) {
                float f3 = livingEntity.getEyeHeight(Pose.STANDING) - 0.1F;
                poseStack.translate((float) (-direction.getStepX()) * f3, 0.0F, (float) (-direction.getStepZ()) * f3);
            }
        }

        float scale = livingEntity.getScale();
        poseStack.scale(scale, scale, scale);
        float bob = accessor.invokeGetBob(livingEntity, partialTicks);
        accessor.invokeSetupRotations(livingEntity, poseStack, bob, f, partialTicks, scale);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        accessor.invokeScale(livingEntity, poseStack, partialTicks);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        ResourceLocation location = ((EntityRendererAccessor<LivingEntity>) renderer)
                .invokeGetTextureLocation(livingEntity);
        EntityModel<LivingEntity> model = (EntityModel<LivingEntity>) livingRenderer.getModel();

        boolean bodyInvisible = !accessor.invokeIsBodyVisible(livingEntity)
                && !livingEntity.isInvisibleTo(player);
        RenderType type = RenderType.outline(location);

        VertexConsumer consumer = outlineBuffer.getBuffer(type);
        consumer.setColor(context.red(), context.green(), context.blue(), context.alpha());
        int overlay = LivingEntityRenderer.getOverlayCoords(
                livingEntity, accessor.invokeGetWhiteOverlayProgress(livingEntity, partialTicks));
        model.renderToBuffer(poseStack, consumer, 0xF000F0, overlay, bodyInvisible ? 654311423 : -1);

        poseStack.popPose();
        poseStack.popPose();
        outlineBuffer.endOutlineBatch();
    }

    @Override
    public RenderLevelStageEvent.Stage getStage() {
        return RenderLevelStageEvent.Stage.AFTER_ENTITIES;
    }

    /** 供将来需要"多个目标一起描边"时使用（旧实现用同一份静态列表喂给 MinecraftMixin 的发光判定）。 */
    public static List<Entity> glowingEntities() {
        return List.of();
    }
}
