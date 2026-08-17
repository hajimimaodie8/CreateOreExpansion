package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;

/**
 * 工具渲染配置 —— 承载单个技能的一次渲染所需参数。
 *
 * <p>颜色来自技能 NBT 的 OutlineColor（AllItems 注册时写入），
 * {@link #defaultConfig} 提供默认白色兜底。</p>
 */
public record SkillRendererConfig(DataSkill skill, float r, float g, float b, float a) {

    public static SkillRendererConfig defaultConfig(DataSkill skill) {
        return new SkillRendererConfig(skill, DEFAULT[0], DEFAULT[1], DEFAULT[2], ALPHA);
    }

    /** 默认白色兜底 */
    private static final float[] DEFAULT = {1.0F, 1.0F, 1.0F};

    public static final float ALPHA = 0.5F;
}
