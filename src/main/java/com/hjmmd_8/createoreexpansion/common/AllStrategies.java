package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.client.tool.BlockToolOutlineRenderer;
import com.hjmmd_8.createoreexpansion.client.tool.SkillRendererConfig;
import com.hjmmd_8.createoreexpansion.client.tool.StrategyRenderer;
import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.hjmmd_8.createoreexpansion.foundation.Params;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.AreaStrategy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.SkillStrategy;
import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.HashMap;
import java.util.Map;

public class AllStrategies {
    public static final Map<ItemSkill, SkillStrategy<?>> STRATEGIES = new HashMap<>();
    public static final Map<SkillStrategy<?>, StrategyRenderer> RENDERERS = new HashMap<>();

    public enum Renderers implements StrategyRenderer {
        BLOCK(new BlockToolOutlineRenderer());

        private final StrategyRenderer renderer;
        Renderers(StrategyRenderer instance) {
            renderer = instance;
        }

        @Override
        public void render(SkillRendererConfig config, ClientLevel world, Camera camera, PoseStack poseStack, SuperRenderTypeBuffer buffer, IParams params) {
            renderer.render(config, world, camera, poseStack, buffer, params);
        }
    }
}
