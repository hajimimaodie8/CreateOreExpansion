package com.hjmmd_8.createoreexpansion.client;

import com.hjmmd_8.createoreexpansion.client.tool.SkillsStrategyRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * AOE 范围挖掘 — 客户端预选框渲染注册
 *
 * <p>在 {@link RenderLevelStageEvent.Stage#AFTER_CUTOUT_BLOCKS} 阶段，
 * 调用所有工具的 outline render 绘制范围线框。</p>
 *
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onWorldRenderLast(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) return;

        Minecraft instance = Minecraft.getInstance();

        // 统一调用所有渲染器
        SkillsStrategyRenderer.INSTANCE.render(
                instance.level,
                instance.getEntityRenderDispatcher().camera,
                event.getPoseStack()
        );
    }
}
