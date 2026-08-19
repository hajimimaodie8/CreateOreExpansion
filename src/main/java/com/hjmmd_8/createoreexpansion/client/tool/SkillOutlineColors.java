package com.hjmmd_8.createoreexpansion.client.tool;

/**
 * 技能发光轮廓颜色 —— 全模组统一的颜色常量与自定义工厂。
 *
 * 渲染端颜色从技能 NBT 的 OutlineColor 读取（AllItems 注册时写入），
 * 此处仅提供预定义颜色常量与自定义工厂，供 {@code AllItems.skillColor(...)} 使用。
 */
public final class SkillOutlineColors {

    private SkillOutlineColors() {
    }

    /** 颜色值（RGB 浮点，0~1） */
    public record SkillColor(float r, float g, float b) {
    }

    // ========== 颜色自定义工厂 ==========

    /**
     * 用 0~255 整数 RGB 自定义颜色。
     * <pre>
     * SkillOutlineColors.of(31, 168, 92)   // 等价于翡翠绿
     * </pre>
     */
    public static SkillColor of(int r, int g, int b) {
        return new SkillColor(r / 255F, g / 255F, b / 255F);
    }

    /**
     * 用十六进制整数自定义颜色（0xRRGGBB）。
     * <pre>
     * SkillOutlineColors.fromHex(0x1FA85C) // 翡翠绿
     * </pre>
     */
    public static SkillColor fromHex(int rgb) {
        return of((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    /**
     * 用十六进制字符串自定义颜色（支持 "RRGGBB" / "#RRGGBB"）。
     *
     * @throws IllegalArgumentException 字符串不是合法 6 位十六进制颜色时抛出
     */
    public static SkillColor fromHex(String hex) {
        String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
        if (cleaned.length() != 6) {
            throw new IllegalArgumentException("Invalid hex color: " + hex);
        }
        try {
            return fromHex(Integer.parseInt(cleaned, 16));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex color: " + hex, e);
        }
    }

    // ========== 预定义颜色（矿物主题） ==========

    /** 翡翠绿 */
    public static final SkillColor JADE_GREEN = new SkillColor(0.0F, 0.85F, 0.3F);
    /** 黄玉金 */
    public static final SkillColor TOPAZ_GOLD = new SkillColor(1.0F, 0.85F, 0.0F);
    /** 蓝宝石蓝 */
    public static final SkillColor SAPPHIRE_BLUE = new SkillColor(0.0F, 0.5F, 1.0F);
    /** 星辉石粉 */
    public static final SkillColor STELLARSTONE_PINK = new SkillColor(0.875F, 0.596F, 0.655F);
    /** 雷鸣紫 */
    public static final SkillColor THUNDER_PURPLE = new SkillColor(0.58F, 0.08F, 0.83F);
    /** 默认白色 */
    public static final SkillColor DEFAULT_WHITE = new SkillColor(1.0F, 1.0F, 1.0F);

    /** 轮廓透明度（全模组统一） */
    public static final float ALPHA = 0.5F;
}
