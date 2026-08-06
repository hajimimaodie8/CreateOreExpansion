package com.hjmmd_8.createoreexpansion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import com.hjmmd_8.createoreexpansion.common.AllItems;
import com.hjmmd_8.createoreexpansion.foundation.util.AreaUtil;

import java.util.Iterator;

/**
 * 蓝宝石镐 - 5x5 范围挖掘预选框渲染
 * 蓝色线框，alpha=0.5
 */
public class SapphirePickaxeOutlineRender {

    private static final int RADIUS = 5;
    private static final int DEPTH = 1;
    private static final float R = 0.0F, G = 0.5F, B = 1.0F, A = 0.5F;

    public static void render(ClientLevel world, Camera camera, DeltaTracker v, PoseStack poseStack,
                              MultiBufferSource consumers, GameRenderer gameRenderer, Matrix4f matrix4f,
                              LightTexture lightTexture, LevelRenderer levelRenderer) {
        if (world == null) return;

        var player = Minecraft.getInstance().player;
        if (player == null || !player.isShiftKeyDown()) return;

        ItemStack held = player.getMainHandItem();
        if (!held.is(AllItems.SAPPHIRE_PICKAXE.get())) return;

        var blockHitResult = Minecraft.getInstance().hitResult;
        if (blockHitResult == null || blockHitResult.getType() != HitResult.Type.BLOCK) return;

        var blockPos = ((BlockHitResult) blockHitResult).getBlockPos();
        var direction = ((BlockHitResult) blockHitResult).getDirection();

        if (world.getBlockState(blockPos).isAir()) return;

        var bb = AreaUtil.getAreaOfEffect(blockPos, direction, RADIUS, DEPTH);

        poseStack.pushPose();
        poseStack.translate(-camera.getPosition().x(), -camera.getPosition().y(), -camera.getPosition().z());

        Iterator<BlockPos> it = BlockPos.betweenClosedStream(bb).iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            if (pos.equals(blockPos)) continue;

            BlockState state = world.getBlockState(pos);
            FluidState fluid = state.getFluidState();
            if (state.isAir() || !fluid.isEmpty()) continue;

            VoxelShape shape = state.getVisualShape(world, pos, CollisionContext.empty());

            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
            LevelRenderer.renderVoxelShape(
                poseStack, consumers.getBuffer(RenderType.lines()),
                shape, 0, 0, 0, R, G, B, A, true
            );
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
