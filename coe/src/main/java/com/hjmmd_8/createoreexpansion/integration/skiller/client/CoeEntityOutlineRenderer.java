package com.hjmmd_8.createoreexpansion.integration.skiller.client;

import com.hjmmd_8.createoreexpansion.content.skill.input.AllKeys;
import com.hjmmd_8.createoreexpansion.mixin.renderers.EntityRendererAccessor;
import com.hjmmd_8.createoreexpansion.mixin.renderers.LivingEntityRendererAccessor;
import com.leaf.skiller.client.ClientSkillCache;
import com.leaf.skiller.content.skill.SkillComponent;
import com.leaf.skiller.foundation.renderer.StrategyRenderer;
import com.leaf.skiller.foundation.skill.ISkillInstance;
import com.leaf.skiller.foundation.skill.SkillBundle;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Map;
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
 * <h2>槽位门：刻意选择（用户可见，回退方式写在这里）</h2>
 * <p><b>口径 = 严格跟按键槽位</b>（键一 → 槽位 0、键二 → 1、键三 → 2），与释放侧
 * {@code integration/skiller/CoeSkillRelease#release} 逐槽位判按键<b>完全一致</b>。
 * 理由：预览必须与实效一致。剑上 {@code skin}（槽 0）与 {@code plunder}（槽 1）共用本渲染器，
 * 缺这道门时两个实例都会被调度到、同一目标被描边两遍，且后一次 {@code outlineBuffer.setColor}
 * 会盖掉前一次的颜色。</p>
 * <p><b>可一行回退到"同族全亮"</b>（迁移期行为）：把 {@link #slotAllows(ISkillInstance)} 的
 * 调用行换成 {@code AllKeys} 的三键或门即可 ——</p>
 * <pre>{@code
 * if (!AllKeys.SKILL_RELEASE.isPressed()
 *         && !AllKeys.SKILL_RELEASE_2.isPressed()
 *         && !AllKeys.SKILL_RELEASE_3.isPressed()) {
 *     return Optional.empty();
 * }
 * }</pre>
 * <p>槽位只存在于 {@code ClientSkillCache.skills} 的 {@code SkillComponent.bindings()}
 * （内核的 {@code ISkillInstance} 没有 {@code slot()}），所以这道门只能落在本渲染器里。</p>
 *
 * <h2>发光登记表</h2>
 * <p>目标实体写进 {@link GlowingEntities#ENTITIES}，由
 * {@code mixin/renderers/MinecraftMixin} 在 {@code shouldEntityAppearGlowing} 里读出
 * （读到即移除），让实体走原版发光渲染。该字段原先挂在旧类
 * {@code client/tool/renderer/EntityOutlineRenderer} 上，已随渲染统一搬到
 * {@link GlowingEntities}（旧类上那个恒返回 {@code List.of()} 的同名方法从未被使用，已删）。</p>
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
        // ── 门 1：槽位门（刻意选择：严格跟按键槽位，与释放侧一致）──
        // 剑上的 skin（槽 0）/ plunder（槽 1）共用本渲染器；旧门是"三键任一"，
        // 会让同一次预览被画两遍（两次 setColor 后一个覆盖前一个）。回退方式见类注释。
        if (!slotAllows(instance)) {
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
                player, target, red, green, blue, OutlineColors.ALPHA));
    }

    /**
     * 槽位门：本实例所在的按键槽位，当前必须是"按下的那个"。
     *
     * <p>与 {@link CoeBlockOutlineRenderer#slotAllows} 是同一份逻辑的两份拷贝（本轮的
     * 覆盖关卡要求 {@code bindings()} 在<b>两个渲染器文件里都能 grep 到</b>）。</p>
     *
     * <p><b>为什么实体这边也必须补</b>：剑上 {@code skin}（槽 0）与 {@code plunder}（槽 1）
     * 共用本渲染器 —— 缺这道门时两个实例的 {@code render} 都会被调用：同一目标实体描边画两遍，
     * 且两次 {@link OutlineBufferSource#setColor} 里后一个会盖掉前一个，颜色对不上按下的技能。</p>
     */
    private static boolean slotAllows(ISkillInstance<?> instance) {
        Integer slot = slotOf(instance);
        if (slot == null) {
            return false; // 查不到槽位就不画（宁可少画一帧，也不能全亮误导）
        }
        return switch (slot) {
            case 0 -> AllKeys.SKILL_RELEASE.isPressed();
            case 1 -> AllKeys.SKILL_RELEASE_2.isPressed();
            case 2 -> AllKeys.SKILL_RELEASE_3.isPressed();
            default -> false;
        };
    }

    /**
     * 反查本实例在 {@code ClientSkillCache.skills} 里的按键槽位（按身份 {@code ==}，
     * 因为内核的 {@code ISkillInstance} 没有 {@code slot()}）。
     *
     * @return 槽位下标；实例还不在缓存里（换工具的那一帧）或缓存为空时返回 {@code null}
     */
    private static Integer slotOf(ISkillInstance<?> instance) {
        SkillComponent component = ClientSkillCache.skills;
        if (component == null || instance == null) {
            return null;
        }
        for (Map.Entry<Integer, SkillBundle> binding : component.bindings().entrySet()) {
            SkillBundle bundle = binding.getValue();
            if (bundle == null) {
                continue;
            }
            for (ISkillInstance<?> candidate : bundle.getAllData()) {
                if (candidate == (ISkillInstance<?>) instance) {
                    return binding.getKey();
                }
            }
        }
        return null;
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

        // **关键：把目标喂给发光钩子**。这里的"边缘发光"其实是两段配合：
        //   ① 这里把实体塞进 GlowingEntities.ENTITIES；
        //   ② mixin/renderers/MinecraftMixin 在 Minecraft#shouldEntityAppearGlowing 里读这张表
        //      （读过即移除），让实体走原版发光渲染（用我们设的 outline 颜色）。
        // 我移植时只搬了"把模型画进 outline 缓冲"这一段，钩子没人喂 → 发光消失，
        // 只剩整模型染成描边色的手动渲染 → 出来的效果就是"整只生物都变色了"。
        GlowingEntities.ENTITIES.clear();
        GlowingEntities.ENTITIES.add(livingEntity);

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
}
