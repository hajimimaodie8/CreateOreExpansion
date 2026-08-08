package com.hjmmd_8.createoreexpansion.client;

import com.hjmmd_8.createoreexpansion.client.tool.SkillsStrategyRenderer;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.EnergyTooltipHandler;
import com.hjmmd_8.createoreexpansion.content.skill.tooltip.SkillsTooltipHandler;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

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
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft instance = Minecraft.getInstance();

        // 统一调用所有渲染器
        SkillsStrategyRenderer.INSTANCE.render(
                instance.level,
                event.getCamera(),
                event.getPoseStack(),
                DefaultSuperRenderTypeBuffer.getInstance()
        );
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        EnergyTooltipHandler.addEnergyTooltip(event);
        SkillsTooltipHandler.addSkillsTooltip(event);
    }
}
