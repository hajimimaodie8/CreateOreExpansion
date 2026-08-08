package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.common.AllStrategies;
import com.hjmmd_8.createoreexpansion.content.skill.AbstractStrategySkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.List;

public class SkillsStrategyRenderer {
    public static SkillsStrategyRenderer INSTANCE = new SkillsStrategyRenderer();

    private Player player;
    private static final ToolOutlineRenderer toolOutlineRenderer = new ToolOutlineRenderer();

    private SkillsStrategyRenderer() {}

    public void render(ClientLevel world, Camera camera, PoseStack poseStack, SuperRenderTypeBuffer buffer) {
        if (world == null) return;
        if (player == null) player = Minecraft.getInstance().player;

        if (player == null) return;
        if (!AllKeys.SKILL_RELEASE.isPressed()) return;

        ItemStack stack = player.getMainHandItem();
        SkillItemStack skillStack = SkillItemStack.of(stack);
        if (stack.isEmpty() || !skillStack.hasSkill()) return;

        // 检查是否看向方块
        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos center = blockHit.getBlockPos();
        BlockState centerState = world.getBlockState(center);

        // 空气方块不渲染
        if (centerState.isAir()) return;

        List<DataSkill> skills = skillStack.getSkillsHolder().getAllData();

        // 渲染
        poseStack.pushPose();
        // 使用负的摄像机位置进行平移，将世界坐标转换为渲染坐标
        Vec3 camPos = camera.getPosition();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        for (DataSkill data : skills) {
            if (!(data.skill instanceof AbstractStrategySkill<?>)) return;

            RendererConfig config = RendererConfig.defaultConfig(data);


            if (data.nbt != null) {
                if (data.nbt.contains("OutlineColor")) {
                    CompoundTag tag = data.nbt.getCompound("OutlineColor");

                    config = new RendererConfig(
                            data,
                            tag.getFloat("r"),
                            tag.getFloat("g"),
                            tag.getFloat("b"),
                            RendererConfig.ALPHA
                    );
                }
            }
            toolOutlineRenderer.render(
                    config, world, camera, poseStack, buffer,
                    center, centerState, blockHit, player
            );
        }

        poseStack.popPose();
    }
}
