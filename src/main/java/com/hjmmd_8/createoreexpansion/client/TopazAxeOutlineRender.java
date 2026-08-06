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
 * 黄玉斧 — 连锁砍树预选框渲染
 *
 * <p>按住 Shift 时，对视线内的原木执行 BFS（26 方向），
 * 将所有相连的原木和树叶用金黄色线框标出。</p>
 *
 * <hr>
 * <h3>新建斧子技能需修改的参数：</h3>
 * <ul>
 *   <li>RENDER_LIMIT — 预览最大方块数（防卡顿）</li>
 *   <li>AllItems.xxx — 改成对应物品注册名</li>
 *   <li>颜色可分别设置原木和树叶</li>
 * </ul>
 */
public class TopazAxeOutlineRender {

    /** 渲染预览的最大方块数（避免卡顿） */
    private static final int RENDER_LIMIT = 150;

    public static void render(ClientLevel world, Camera camera, DeltaTracker v, PoseStack poseStack,
                              MultiBufferSource consumers, GameRenderer gameRenderer, Matrix4f matrix4f,
                              LightTexture lightTexture, LevelRenderer levelRenderer) {
        if (world == null) return;

        var player = Minecraft.getInstance().player;
        if (player == null || !player.isShiftKeyDown()) return;

        ItemStack held = player.getMainHandItem();
        if (!held.is(AllMyItems.TOPAZ_AXE.get())) return;

        var hit = Minecraft.getInstance().hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos center = blockHit.getBlockPos();
        BlockState centerState = world.getBlockState(center);
        if (!centerState.is(BlockTags.LOGS)) return;

        // BFS 26 方向遍历相连的原木和树叶
        Set<BlockPos> treeBlocks = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(center);
        treeBlocks.add(center);

        while (!queue.isEmpty() && treeBlocks.size() < RENDER_LIMIT) {
            BlockPos current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos nb = current.offset(dx, dy, dz);
                        if (treeBlocks.contains(nb)) continue;
                        BlockState ns = world.getBlockState(nb);
                        if (ns.is(BlockTags.LOGS) || ns.is(BlockTags.LEAVES)) {
                            treeBlocks.add(nb);
                            queue.add(nb);
                        }
                    }
                }
            }
        }

        poseStack.pushPose();
        poseStack.translate(-camera.getPosition().x(), -camera.getPosition().y(), -camera.getPosition().z());

        for (BlockPos pos : treeBlocks) {
            if (pos.equals(center)) continue;
            BlockState state = world.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) continue;

            VoxelShape shape = state.getVisualShape(world, pos, CollisionContext.empty());

            // 原木用亮金黄，树叶用暗金黄
            float r, g, b;
            if (state.is(BlockTags.LOGS)) {
                r = 1.0F; g = 0.85F; b = 0.0F;
            } else {
                r = 1.0F; g = 0.7F; b = 0.0F;
            }

            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
            LevelRenderer.renderVoxelShape(
                poseStack, consumers.getBuffer(RenderType.lines()),
                shape, 0, 0, 0, r, g, b, 0.5F, true
            );
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
