package com.hjmmd_8.createoreexpansion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import com.hjmmd_8.createoreexpansion.common.AllItems;

import java.util.HashSet;
import java.util.Set;

public class JadePickaxeOutlineRender {

    private static final float R = 0.0F, G = 0.85F, B = 0.3F, A = 0.5F;

    public static void render(ClientLevel world, Camera camera, DeltaTracker v, PoseStack poseStack,
                              MultiBufferSource consumers, GameRenderer gameRenderer, Matrix4f matrix4f,
                              LightTexture lightTexture, LevelRenderer levelRenderer) {
        if (world == null) return;

        var player = Minecraft.getInstance().player;
        if (player == null || !player.isShiftKeyDown()) return;

        ItemStack held = player.getMainHandItem();
        if (!held.is(AllItems.JADE_PICKAXE.get())) return;

        var hit = Minecraft.getInstance().hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos center = ((BlockHitResult) hit).getBlockPos();
        if (world.getBlockState(center).isAir()) return;

        // 1x3 行：中心 + 左右
        float yaw = player.getYRot();
        int dirIndex = (int) Math.floor((((yaw % 360) + 360) % 360) / 90.0 + 0.5) % 4;
        Direction[] dirs = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
        Direction facing = dirs[dirIndex];
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();

        Set<BlockPos> positions = new HashSet<>();
        positions.add(center);
        positions.add(center.relative(left));
        positions.add(center.relative(right));

        poseStack.pushPose();
        poseStack.translate(-camera.getPosition().x(), -camera.getPosition().y(), -camera.getPosition().z());

        for (BlockPos pos : positions) {
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
