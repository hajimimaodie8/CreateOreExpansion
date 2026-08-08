package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.common.AllRenderTypes;
import com.hjmmd_8.createoreexpansion.content.skill.AbstractStrategySkill;
import com.hjmmd_8.createoreexpansion.foundation.util.AreaStrategy;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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

    public void render(ClientLevel world, Camera camera, PoseStack poseStack, SuperRenderTypeBuffer buffer,
                       BlockPos center, BlockState centerState, BlockHitResult blockHit, Player player) {
        AreaStrategy strategy = skill.getStrategy();
        if (!strategy.shouldRender(skill, world, center, centerState, player)) return;

        Set<BlockPos> positions = strategy.calculatePositions(skill, center, blockHit, player);
        positions.add(center);

        float r = config.r(), g = config.g(), b = config.b(), a = config.a();

        // 第一层：不透明层（受深度测试影响）
        VertexConsumer solid = buffer.getBuffer(RenderType.LINES);
        OutlineRenderer.renderOutline(world, positions, poseStack, solid, r, g, b, a);
        buffer.draw(RenderType.LINES);

        // 第二层：半透明穿透层（禁用深度写入和测试）
        VertexConsumer transparent = buffer.getBuffer(AllRenderTypes.LINES_TRANSPARENT);
        OutlineRenderer.renderOutline(world, positions, poseStack, transparent, r, g, b, a * 0.3f);
        buffer.draw(AllRenderTypes.LINES_TRANSPARENT);
    }
}