package com.hjmmd_8.createoreexpansion.client.tool;

import com.google.common.collect.Maps;
import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.content.skill.AbstractStrategySkill;
import com.hjmmd_8.createoreexpansion.foundation.FrameParams;
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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SkillsStrategyRenderer {
    public static SkillsStrategyRenderer INSTANCE = new SkillsStrategyRenderer();
    public static ParamsPool<FrameParams> pool = new ParamsPool<>(50, FrameParams::new);

    private FrameParams lastParams;
    private boolean isParamsReturned;
    private Player player;

    private ClientLevel world;
    private Camera camera;
    private PoseStack poseStack;
    private SuperRenderTypeBuffer buffer;

    private final Map<RenderLevelStageEvent.Stage, List<Runnable>> renderers = Maps.newHashMap();

    private SkillsStrategyRenderer() {}

    public void schedule(ClientLevel world, Camera camera, PoseStack poseStack, SuperRenderTypeBuffer buffer) {
        this.world = world;
        this.camera = camera;
        this.poseStack = poseStack;
        this.buffer = buffer;

        renderers.clear();
        if (!isParamsReturned) {
            pool.returnParams(lastParams);
            isParamsReturned = true;
        }

        if (world == null) return;
        if (player == null) player = Minecraft.getInstance().player;

        if (player == null) return;
        // 任一技能键（键一/键二/键三）按下即显示预览框
        boolean key1 = AllKeys.SKILL_RELEASE.isPressed();
        boolean key2 = AllKeys.SKILL_RELEASE_2.isPressed();
        boolean key3 = AllKeys.SKILL_RELEASE_3.isPressed();
        if (!key1 && !key2 && !key3) return;

        ItemStack stack = player.getMainHandItem();
        SkillItemStack skillStack = SkillItemStack.of(stack);
        if (stack.isEmpty() || !skillStack.hasSkill()) return;

        List<DataSkill> skills = skillStack.getSkillsHolder().getAllData();

        lastParams = pool.borrow()
                .putChild("BlockParams", () -> this.getBlockParams(world))
                .putChild("EntityParams", this::getEntityParams)
                .put("Player", player)
                ;
        isParamsReturned = false;

        for (DataSkill data : skills) {
            if (!(data.skill instanceof AbstractStrategySkill<?, ?> strategySkill)) continue;

            // 多技能按槽位渲染：只显示当前按下的技能键对应槽位的技能框。
            // 槽位索引 = 同类型技能中的位置（与释放端 getDataSkills(type).index 一致）
            int slot = slotOf(data, skills);
            boolean selected = (slot == 0 && key1) || (slot == 1 && key2) || (slot == 2 && key3);
            if (!selected) continue;

            // 颜色来源：从技能 NBT 的 OutlineColor 读取（AllItems 注册时写入）
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

            StrategyRenderer renderer = strategySkill.strategy().getRenderer();
            SkillRendererConfig finalConfig = config;
            renderers.computeIfAbsent(renderer.getStage(), key -> new ArrayList<>())
                    .add(() -> renderer.render(
                            finalConfig, this.world, this.camera, this.poseStack, this.buffer, lastParams));
        }
    }

    /**
     * 计算技能在同类型技能列表中的槽位索引（0 起，与释放端 getDataSkills(type) 顺序一致）。
     * 跨类型技能各自从 0 计数，保证键一/键二/键三与槽位对应。
     */
    private static int slotOf(DataSkill data, List<DataSkill> skills) {
        int slot = 0;
        for (DataSkill other : skills) {
            if (other == data) return slot;
            if (other.skill.getType() == data.skill.getType()) slot++;
        }
        return 0;
    }

    public void render(RenderLevelStageEvent.Stage stage) {
        List<Runnable> list = renderers.get(stage);
        if (list == null || list.isEmpty()) return;

        poseStack.pushPose();
        Vec3 camPos = camera.getPosition();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        for (Runnable r : list) {
            r.run();
        }

        poseStack.popPose();
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

    private FrameParams getEntityParams() {
        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) return pool.borrow();

        EntityHitResult entityHit = (EntityHitResult) hit;

        return pool.borrow()
                .put("EntityHitResult", entityHit)
                .put("Entity", entityHit.getEntity());
    }
}
