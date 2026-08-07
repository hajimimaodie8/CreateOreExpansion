package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.common.AllItems;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 工具渲染配置
 */
public record RendererConfig(ItemSkill skill, float r, float g, float b, float a) {

    // 预定义颜色
    public static final float[] JADE_GREEN = {0.0F, 0.85F, 0.3F};
    public static final float[] SAPPHIRE_BLUE = {0.0F, 0.5F, 1.0F};
    public static final float[] TOPAZ_GOLD = {1.0F, 0.85F, 0.0F};
    public static final float ALPHA = 0.5F;
}
