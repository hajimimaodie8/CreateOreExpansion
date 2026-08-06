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
import com.hjmmd_8.createoreexpansion.common.AllMyItems;

/**
 * 榛勭帀閾?鈥?涓€鍒楁寲鎺橀閫夋娓叉煋
 *
 * <p>鎸変綇 Shift 鏃讹紝鍦ㄦ寲鎺樻柟鍚戜笂涓€鍒楁柟鍧楃粯鍒堕噾榛勮壊绾挎锛屾渶澶?{@code MAX_DEPTH} 鏍笺€?/p>
 *
 * <hr>
 * <h3>鏂板缓閾插瓙鎶€鑳介渶淇敼鐨勫弬鏁帮細</h3>
 * <ul>
 *   <li>MAX_DEPTH 鈥?涓?Helper 涓殑鍊间繚鎸佷竴鑷?/li>
 *   <li>AllItems.xxx 鈥?鏀规垚瀵瑰簲鐗╁搧娉ㄥ唽鍚?/li>
 *   <li>R, G, B, A 鈥?绾挎棰滆壊鍜岄€忔槑搴?/li>
 * </ul>
 */
public class TopazShovelOutlineRender {

    private static final int MAX_DEPTH = 8;
    private static final float R = 1.0F, G = 0.85F, B = 0.0F, A = 0.5F;

    public static void render(ClientLevel world, Camera camera, DeltaTracker v, PoseStack poseStack,
                              MultiBufferSource consumers, GameRenderer gameRenderer, Matrix4f matrix4f,
                              LightTexture lightTexture, LevelRenderer levelRenderer) {
        if (world == null) return;

        var player = Minecraft.getInstance().player;
        if (player == null || !player.isShiftKeyDown()) return;

        ItemStack held = player.getMainHandItem();
        if (!held.is(AllMyItems.TOPAZ_SHOVEL.get())) return;

        var hit = Minecraft.getInstance().hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos center = ((BlockHitResult) hit).getBlockPos();
        BlockState centerState = world.getBlockState(center);
        if (centerState.isAir()) return;

                Direction digDir;
        if (player.getXRot() > 45.0F) {
            float y = player.getYRot();
            int idx = (int)Math.floor((((y%360)+360)%360)/90.0+0.5)%4;
            digDir = (new Direction[]{Direction.SOUTH,Direction.WEST,Direction.NORTH,Direction.EAST})[idx];
        } else {
            digDir = ((BlockHitResult) hit).getDirection().getOpposite();
        }

        poseStack.pushPose();
        poseStack.translate(-camera.getPosition().x(), -camera.getPosition().y(), -camera.getPosition().z());

        for (int i = 1; i <= MAX_DEPTH; i++) {
            BlockPos pos = center.relative(digDir, i);
            BlockState state = world.getBlockState(pos);
            FluidState fluid = state.getFluidState();
            if (state.isAir() || !fluid.isEmpty()) break;

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
