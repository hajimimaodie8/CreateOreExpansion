package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;

/**
 * 工具渲染配置
 */
public record RendererConfig(DataSkill skill, float r, float g, float b, float a) {

    public static RendererConfig fromArray(DataSkill skill, float[] config) {
        return new RendererConfig(skill, config[0], config[1], config[2], ALPHA);
    }

    public static RendererConfig defaultConfig(DataSkill skill) {
        return fromArray(skill, DEFAULT);
    }

    // 预定义颜色
    public static final float[] JADE_GREEN = {0.0F, 0.85F, 0.3F};
    public static final float[] SAPPHIRE_BLUE = {0.0F, 0.5F, 1.0F};
    public static final float[] TOPAZ_GOLD = {1.0F, 0.85F, 0.0F};
    public static final float[] DEFAULT = {1.0F, 1.0F, 1.0F};
    public static final float ALPHA = 0.5F;
}
