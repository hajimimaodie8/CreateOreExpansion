package com.hjmmd_8.createoreexpansion.client.tool;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;

import java.util.Set;

/**
 * 工具范围渲染器 - 统一的渲染逻辑
 *
 * <p>使用策略模式支持不同的渲染类型：
 * <ul>
 *   <li>AOE 范围渲染（镐/铲）</li>
 *   <li>方向性渲染（翡翠镐）</li>
 *   <li>线性挖掘（铲子）</li>
 *   <li>树砍伐（斧子）</li>
 * </ul>
 */
public class ToolOutlineRenderer {
    private final RendererConfig config;
    private final RenderStrategy strategy;
    private final boolean requireLogs;

    /**
     * 创建渲染器
     * @param config 渲染配置（物品、颜色）
     * @param strategy 位置计算策略
     */
    public ToolOutlineRenderer(RendererConfig config, RenderStrategy strategy) {
        this(config, strategy, false);
    }

    /**
     * 创建渲染器（斧子专用）
     * @param config 渲染配置
     * @param strategy 位置计算策略
     * @param requireLogs 是否要求目标是原木
     */
    public ToolOutlineRenderer(RendererConfig config, RenderStrategy strategy, boolean requireLogs) {
        this.config = config;
        this.strategy = strategy;
        this.requireLogs = requireLogs;
    }

    /**
     * 渲染范围线框
     */
    public void render(ClientLevel world, Camera camera, DeltaTracker v, PoseStack poseStack,
                       MultiBufferSource consumers, GameRenderer gameRenderer, Matrix4f matrix4f,
                       LightTexture lightTexture, LevelRenderer levelRenderer) {
        if (world == null) return;

        Player player = Minecraft.getInstance().player;
        if (player == null || !player.isShiftKeyDown()) return;

        // 检查手持物品
        if (!player.getMainHandItem().is(config.item())) return;

        // 检查是否看向方块
        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos center = blockHit.getBlockPos();
        BlockState centerState = world.getBlockState(center);

        // 空气方块不渲染
        if (centerState.isAir()) return;

        // 斧子检查是否是原木
        if (requireLogs && !centerState.is(BlockTags.LOGS)) return;

        // 计算需要渲染的位置
        Set<BlockPos> positions = strategy.calculatePositions(center, blockHit, player);

        // 将中心方块也加入渲染集合
        positions.add(center);

        if (positions.isEmpty()) return;

        // 开始渲染
        poseStack.pushPose();
        poseStack.translate(-camera.getPosition().x(), -camera.getPosition().y(), -camera.getPosition().z());

        // 使用优化的轮廓渲染器 - 只渲染真正的外轮廓
        OutlineRenderer.renderOutline(
            world, positions, poseStack,
            consumers.getBuffer(RenderType.lines()),
            config.r(), config.g(), config.b(), config.a()
        );

        poseStack.popPose();
    }
}
