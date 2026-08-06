package com.hjmmd_8.createoreexpansion.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * AOE 范围挖掘 — 客户端预选框渲染注册
 *
 * <p>在 {@link RenderLevelStageEvent.Stage#AFTER_CUTOUT_BLOCKS} 阶段，
 * 调用三个工具的 outline render 绘制金黄色范围线框。</p>
 *
 * <hr>
 * <h3>新建工具需在此文件添加一行调用：</h3>
 * <pre>
 * SapphirePickaxeOutlineRender.render(
 *     instance.level, event.getCamera(), event.getPartialTick(), event.getPoseStack(),
 *     instance.renderBuffers().bufferSource(), instance.gameRenderer,
 *     event.getProjectionMatrix(), instance.gameRenderer.lightTexture(), instance.levelRenderer
 * );
 * </pre>
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onWorldRenderLast(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) return;

        Minecraft instance = Minecraft.getInstance();

        TopazPickaxeOutlineRender.render(
                instance.level, event.getCamera(), event.getPartialTick(), event.getPoseStack(),
                instance.renderBuffers().bufferSource(), instance.gameRenderer,
                event.getProjectionMatrix(), instance.gameRenderer.lightTexture(), instance.levelRenderer
        );

        TopazShovelOutlineRender.render(
                instance.level, event.getCamera(), event.getPartialTick(), event.getPoseStack(),
                instance.renderBuffers().bufferSource(), instance.gameRenderer,
                event.getProjectionMatrix(), instance.gameRenderer.lightTexture(), instance.levelRenderer
        );

        
        JadePickaxeOutlineRender.render(
                instance.level, event.getCamera(), event.getPartialTick(), event.getPoseStack(),
                instance.renderBuffers().bufferSource(), instance.gameRenderer,
                event.getProjectionMatrix(), instance.gameRenderer.lightTexture(), instance.levelRenderer
        );

        JadeShovelOutlineRender.render(
                instance.level, event.getCamera(), event.getPartialTick(), event.getPoseStack(),
                instance.renderBuffers().bufferSource(), instance.gameRenderer,
                event.getProjectionMatrix(), instance.gameRenderer.lightTexture(), instance.levelRenderer
        );

        JadeAxeOutlineRender.render(
                instance.level, event.getCamera(), event.getPartialTick(), event.getPoseStack(),
                instance.renderBuffers().bufferSource(), instance.gameRenderer,
                event.getProjectionMatrix(), instance.gameRenderer.lightTexture(), instance.levelRenderer
        );
        
        SapphirePickaxeOutlineRender.render(
                instance.level, event.getCamera(), event.getPartialTick(), event.getPoseStack(),
                instance.renderBuffers().bufferSource(), instance.gameRenderer,
                event.getProjectionMatrix(), instance.gameRenderer.lightTexture(), instance.levelRenderer
        );

        SapphireShovelOutlineRender.render(
                instance.level, event.getCamera(), event.getPartialTick(), event.getPoseStack(),
                instance.renderBuffers().bufferSource(), instance.gameRenderer,
                event.getProjectionMatrix(), instance.gameRenderer.lightTexture(), instance.levelRenderer
        );

        SapphireAxeOutlineRender.render(
                instance.level, event.getCamera(), event.getPartialTick(), event.getPoseStack(),
                instance.renderBuffers().bufferSource(), instance.gameRenderer,
                event.getProjectionMatrix(), instance.gameRenderer.lightTexture(), instance.levelRenderer
        );
        TopazAxeOutlineRender.render(
                instance.level, event.getCamera(), event.getPartialTick(), event.getPoseStack(),
                instance.renderBuffers().bufferSource(), instance.gameRenderer,
                event.getProjectionMatrix(), instance.gameRenderer.lightTexture(), instance.levelRenderer
        );
    }
}