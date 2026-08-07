package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.common.AllStrategies;
import com.hjmmd_8.createoreexpansion.content.skill.AbstractStrategySkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemStackSkillHelper;
import com.hjmmd_8.createoreexpansion.foundation.util.AreaStrategy;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Set;

public class SkillsStrategyRenderer {
    public static SkillsStrategyRenderer INSTANCE = new SkillsStrategyRenderer();

    private Player player;

    private SkillsStrategyRenderer() {}

    public void render(ClientLevel world, Camera camera, PoseStack poseStack) {
        if (world == null) return;
        if (player == null) player = Minecraft.getInstance().player;

        if (player == null) return;
        if (!AllKeys.SKILL_RELEASE.isPressed()) return;

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !ItemStackSkillHelper.hasSkill(stack)) return;

        // 检查是否看向方块
        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos center = blockHit.getBlockPos();
        BlockState centerState = world.getBlockState(center);

        // 空气方块不渲染
        if (centerState.isAir()) return;

        @SuppressWarnings("rawtypes")
        List<AbstractStrategySkill> skills = ItemStackSkillHelper.getSkills(stack).stream()
                .filter(s -> s instanceof AbstractStrategySkill)
                .map(s -> (AbstractStrategySkill) s).toList();

        // 渲染
        poseStack.pushPose();
        poseStack.translate(
                -camera.getPosition().x(),
                -camera.getPosition().y(),
                -camera.getPosition().z()
        );

        for (AbstractStrategySkill<?> skill : skills) {
            AllStrategies.RENDERERS.get(skill).render(
                    world, camera, poseStack, center, centerState, blockHit, player);
        }

        poseStack.popPose();
    }
}
