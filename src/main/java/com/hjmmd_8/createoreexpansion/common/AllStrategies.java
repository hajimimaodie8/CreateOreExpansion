package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.client.tool.renderer.BlockToolOutlineRenderer;
import com.hjmmd_8.createoreexpansion.client.tool.SkillRendererConfig;
import com.hjmmd_8.createoreexpansion.client.tool.StrategyRenderer;
import com.hjmmd_8.createoreexpansion.client.tool.renderer.EmptyRenderer;
import com.hjmmd_8.createoreexpansion.client.tool.renderer.EntityOutlineRenderer;
import com.hjmmd_8.createoreexpansion.foundation.util.params.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.SkillStrategy;
import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.HashMap;
import java.util.Map;

public class AllStrategies {
    public static final Map<ItemSkill, SkillStrategy<?>> STRATEGIES = new HashMap<>();
    public static final Map<SkillStrategy<?>, StrategyRenderer> RENDERERS = new HashMap<>();

    public enum Renderers implements StrategyRenderer {
        EMPTY(new EmptyRenderer()),
        BLOCK(new BlockToolOutlineRenderer()),
        ENTITY(new EntityOutlineRenderer()),
        ;

        private final StrategyRenderer renderer;
        Renderers(StrategyRenderer instance) {
            renderer = instance;
        }

        @Override
        public void render(SkillRendererConfig config, ClientLevel world, Camera camera, PoseStack poseStack, SuperRenderTypeBuffer buffer, IParams params) {
            renderer.render(config, world, camera, poseStack, buffer, params);
        }

        @Override
        public RenderLevelStageEvent.Stage getStage() {
            return renderer.getStage();
        }
    }
}
