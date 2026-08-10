package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.common.AllStrategies;
import com.hjmmd_8.createoreexpansion.content.skill.AbstractStrategySkill;
import com.hjmmd_8.createoreexpansion.foundation.FrameParams;
import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.hjmmd_8.createoreexpansion.foundation.ParamsPool;
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

import java.util.List;

public class SkillsStrategyRenderer {
    public static SkillsStrategyRenderer INSTANCE = new SkillsStrategyRenderer();
    public static ParamsPool<FrameParams> pool = new ParamsPool<>(50, FrameParams::new);

    private Player player;

    private SkillsStrategyRenderer() {}

    public void render(ClientLevel world, Camera camera, PoseStack poseStack, SuperRenderTypeBuffer buffer) {
        if (world == null) return;
        if (player == null) player = Minecraft.getInstance().player;

        if (player == null) return;
        if (!AllKeys.SKILL_RELEASE.isPressed()) return;

        ItemStack stack = player.getMainHandItem();
        SkillItemStack skillStack = SkillItemStack.of(stack);
        if (stack.isEmpty() || !skillStack.hasSkill()) return;

        List<DataSkill> skills = skillStack.getSkillsHolder().getAllData();

        // 渲染
        poseStack.pushPose();
        // 使用负的摄像机位置进行平移，将世界坐标转换为渲染坐标
        Vec3 camPos = camera.getPosition();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        IParams params = pool.borrow()
                .putChild("BlockParams", () -> this.getBlockParams(world))
                .put("Player", player)
                ;

        for (DataSkill data : skills) {
            if (!(data.skill instanceof AbstractStrategySkill<?, ?, ?>)) continue;

            // 加载config到skill和strategy（如果存在）
            if (data.config != null) {
                data.config.load(data);
            }

            SkillRendererConfig config = SkillRendererConfig.defaultConfig(data);

            if (data.nbt != null && data.nbt.contains("OutlineColor")) {
                CompoundTag tag = data.nbt.getCompound("OutlineColor");

                float r, g, b;
                r = tag.getFloat("r");
                g = tag.getFloat("g");
                b = tag.getFloat("b");

                config = new SkillRendererConfig(
                        data,
                        r == (int) r && r > 0 ? 1.0f : r - (int) r,
                        g == (int) g && g > 0 ? 1.0f : g - (int) g,
                        b == (int) b && b > 0 ? 1.0f : b - (int) b,
                        SkillRendererConfig.ALPHA
                );
            }

            AllStrategies.Renderers.BLOCK.render(
                    config, world, camera, poseStack, buffer, params);
        }

        poseStack.popPose();

        pool.returnParams(params);
    }

    private FrameParams getBlockParams(ClientLevel world) {
        // 检查是否看向方块
        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return pool.borrow();

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos center = blockHit.getBlockPos();
        BlockState centerState = world.getBlockState(center);

        // 空气方块不渲染
        if (centerState.isAir()) return pool.borrow();

        return pool.borrow()
                .put("Center", center)
                .put("CenterState", centerState)
                .put("BlockHitResult", blockHit);
    }
}
