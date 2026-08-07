package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.common.AllRenderTypes;
import com.hjmmd_8.createoreexpansion.content.skill.AbstractStrategySkill;
import com.hjmmd_8.createoreexpansion.foundation.util.AreaStrategy;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Set;

/**
 * 工具范围渲染器 — FTB Ultimine 风格
 *
 * <p>双层渲染：第一层不透明（可被方块遮挡），第二层半透明（穿透方块始终可见）。</p>
 */
public class ToolOutlineRenderer {

    private final RendererConfig config;
    private final AbstractStrategySkill<?> skill;

    public ToolOutlineRenderer(RendererConfig config, AbstractStrategySkill<?> skill) {
        this.config = config;
        this.skill = skill;
    }

    public void render(ClientLevel world, Camera camera, PoseStack poseStack,
                       BlockPos center, BlockState centerState, BlockHitResult blockHit, Player player) {
        AreaStrategy strategy = skill.getStrategy();
        if (!strategy.shouldRender(world, center, centerState, player)) return;

        Minecraft mc = Minecraft.getInstance();

        Set<BlockPos> positions = strategy.calculatePositions(center, blockHit, player);
        positions.add(center);

        float r = config.r(), g = config.g(), b = config.b(), a = config.a();

        // TODO ：这里有渲染bug，实际渲染的框会跟着摄像头走

        // 第一层
        poseStack.pushPose();
        VertexConsumer solid = mc.renderBuffers().bufferSource().getBuffer(RenderType.LINES);
        OutlineRenderer.renderOutline(world, positions, poseStack, solid, r, g, b, a);
        mc.renderBuffers().bufferSource().endBatch(RenderType.LINES);
        poseStack.popPose();


//        // 第二层
        poseStack.pushPose();
        VertexConsumer transparent = mc.renderBuffers().bufferSource().getBuffer(AllRenderTypes.LINES_TRANSPARENT);
        OutlineRenderer.renderOutline(world, positions, poseStack, transparent, r, g, b, a * 0.3f);
        mc.renderBuffers().bufferSource().endBatch(AllRenderTypes.LINES_TRANSPARENT);
        poseStack.popPose();

    }
}