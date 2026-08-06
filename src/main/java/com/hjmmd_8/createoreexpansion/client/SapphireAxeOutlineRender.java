package com.hjmmd_8.createoreexpansion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import com.hjmmd_8.createoreexpansion.common.AllMyItems;

import java.util.*;

/**
 * 蓝宝石斧 - 连锁砍树预选框渲染
 * 蓝色线框，BFS 遍历原木+树叶
 */
public class SapphireAxeOutlineRender {

    private static final int RENDER_LIMIT = 150;
    private static final float R = 0.0F, G = 0.5F, B = 1.0F, A = 0.5F;

    public static void render(ClientLevel world, Camera camera, DeltaTracker v, PoseStack poseStack,
                              MultiBufferSource consumers, GameRenderer gameRenderer, Matrix4f matrix4f,
                              LightTexture lightTexture, LevelRenderer levelRenderer) {
        if (world == null) return;

        var player = Minecraft.getInstance().player;
        if (player == null || !player.isShiftKeyDown()) return;

        ItemStack held = player.getMainHandItem();
        if (!held.is(AllMyItems.SAPPHIRE_AXE.get())) return;

        var hitResult = Minecraft.getInstance().hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        var blockPos = blockHit.getBlockPos();
        var blockState = world.getBlockState(blockPos);

        if (!blockState.is(BlockTags.LOGS)) return;

        // BFS 遍历相连的原木和树叶
        Set<BlockPos> treeBlocks = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(blockPos);
        treeBlocks.add(blockPos);

        while (!queue.isEmpty() && treeBlocks.size() < RENDER_LIMIT) {
            BlockPos current = queue.poll();

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (treeBlocks.contains(neighbor)) continue;

                        BlockState neighborState = world.getBlockState(neighbor);
                        if (neighborState.is(BlockTags.LOGS) || neighborState.is(BlockTags.LEAVES)) {
                            treeBlocks.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        poseStack.pushPose();
        poseStack.translate(-camera.getPosition().x(), -camera.getPosition().y(), -camera.getPosition().z());

        for (BlockPos pos : treeBlocks) {
            if (pos.equals(blockPos)) continue;

            BlockState state = world.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) continue;

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
