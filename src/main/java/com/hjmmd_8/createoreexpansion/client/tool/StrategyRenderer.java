package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;

public interface StrategyRenderer {
    void render(SkillRendererConfig config, ClientLevel world, Camera camera,
                PoseStack poseStack, SuperRenderTypeBuffer buffer, IParams params);
}
