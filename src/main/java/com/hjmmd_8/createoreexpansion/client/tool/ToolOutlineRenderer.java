package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.content.skill.AbstractStrategySkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemStackSkillHelper;
import com.hjmmd_8.createoreexpansion.foundation.util.AreaStrategy;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;

import java.util.Set;

/**
 * 工具范围渲染器 - 统一的渲染逻辑
 *
 * <p>现在渲染器直接使用策略技能实例，通过 {@link AbstractStrategySkill#getStrategy()}
 * 获取策略，确保渲染预览与实际技能行为完全一致。</p>
 *
 * <p>支持的渲染类型：
 * <ul>
 *   <li>AOE 范围渲染（镐/铲）</li>
 *   <li>方向性渲染（翡翠镐）</li>
 *   <li>线性挖掘（铲子）</li>
 *   <li>树砍伐（斧子）</li>
 * </ul>
 */
public class ToolOutlineRenderer {
    private final RendererConfig config;
    private final AbstractStrategySkill<?> skill;

    /**
     * 创建渲染器（斧子专用）
     * @param config 渲染配置
     * @param skill 策略技能实例
     */
    public ToolOutlineRenderer(RendererConfig config, AbstractStrategySkill<?> skill) {
        this.config = config;
        this.skill = skill;
    }

    /**
     * 渲染范围线框
     */
    public void render(ClientLevel world, Camera camera, PoseStack poseStack, MultiBufferSource consumers,
                       BlockPos center, BlockState centerState, BlockHitResult blockHit, Player player) {
        AreaStrategy strategy = skill.getStrategy();
        if (!strategy.shouldRender(world, center, centerState, player)) return;
        Set<BlockPos> positions = strategy.calculatePositions(center, blockHit, player);
        positions.add(center);

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
