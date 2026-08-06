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
 * 黄玉镐 — 3x3 范围挖掘预选框渲染
 *
 * <p>按住 Shift 时，在 3x3 范围内所有方块上绘制金黄色线框。</p>
 *
 * <hr>
 * <h3>新建镐子技能需修改的参数：</h3>
 * <ul>
 *   <li>RADIUS / DEPTH — 与 Helper 中的值保持一致</li>
 *   <li>AllItems.xxx — 改成对应物品注册名</li>
 *   <li>R, G, B — 线框颜色（1.0, 0.85, 0.0 = 金黄色）</li>
 *   <li>A — 透明度（0.5）</li>
 * </ul>
 */
public class TopazPickaxeOutlineRender {

    private static final int RADIUS = 3;
    private static final int DEPTH = 1;
    private static final float R = 1.0F, G = 0.85F, B = 0.0F, A = 0.5F;

    public static void render(ClientLevel world, Camera camera, DeltaTracker v, PoseStack poseStack,
                              MultiBufferSource consumers, GameRenderer gameRenderer, Matrix4f matrix4f,
                              LightTexture lightTexture, LevelRenderer levelRenderer) {
        if (world == null) return;

        var player = Minecraft.getInstance().player;
        if (player == null || !player.isShiftKeyDown()) return;

        ItemStack held = player.getMainHandItem();
        if (!held.is(AllItems.TOPAZ_PICKAXE.get())) return;

        var hit = Minecraft.getInstance().hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos center = ((BlockHitResult) hit).getBlockPos();
        BlockState centerState = world.getBlockState(center);
        if (centerState.isAir()) return;

        var dir = ((BlockHitResult) hit).getDirection();
        var bb = AreaUtil.getAreaOfEffect(center, dir, RADIUS, DEPTH);

        poseStack.pushPose();
        poseStack.translate(-camera.getPosition().x(), -camera.getPosition().y(), -camera.getPosition().z());

        Iterator<BlockPos> it = BlockPos.betweenClosedStream(bb).iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            if (pos.equals(center)) continue;

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
