package com.hjmmd_8.createoreexpansion.client.tool.renderer;

import com.hjmmd_8.createoreexpansion.client.tool.SkillRendererConfig;
import com.hjmmd_8.createoreexpansion.client.tool.StrategyRenderer;
import com.hjmmd_8.createoreexpansion.foundation.util.params.IParams;
import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public class EmptyRenderer implements StrategyRenderer {
    @Override
    public void render(SkillRendererConfig config, ClientLevel world, Camera camera, PoseStack poseStack,
                       SuperRenderTypeBuffer buffer, IParams params) {}

    @Override
    public RenderLevelStageEvent.Stage getStage() {
        return RenderLevelStageEvent.Stage.AFTER_SKY;
    }
}
