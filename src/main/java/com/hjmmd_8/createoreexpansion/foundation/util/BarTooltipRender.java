package com.hjmmd_8.createoreexpansion.foundation.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.awt.Color;

/**
 * 竖线进度条工具 — 仿 Ponder 风格
 *
 * <p>用法：<br>
 * {@link #durability(int, int, int)} — 耐久条<br>
 * {@link #progress(float, int)} — 通用进度<br>
 * {@link #energy(int, int, int)} — 能量条<br>
 * {@link #custom(float, int, Color, Color)} — 自定义颜色</p>
 */
public class BarTooltipRender {

    public static Component durability(int current, int max, int total) {
        float ratio = max > 0 ? (float) current / max : 0;
        return makeBar(ratio, total,
                ratio > 0.5 ? new Color(0x55FF55) : ratio > 0.25 ? new Color(0xFFFF55) : new Color(0xFF5555),
                new Color(0x333333));
    }

    public static Component energy(int current, int max, int total) {
        float ratio = max > 0 ? (float) current / max : 0;
        return makeBar(ratio, total,
                new Color(0x5555FF),
                new Color(0x333333));
    }

    public static Component energy(int current, int max, int total, Color fillColor) {
        float ratio = max > 0 ? (float) current / max : 0;
        return makeBar(ratio, total,
                fillColor,
                new Color(0x333333));
    }

    public static Component progress(float progress, int total) {
        return makeBar(Math.clamp(progress, 0, 1), total,
                new Color(0xAAAAAA),
                new Color(0x333333));
    }

    /**
     * 自定义颜色进度条
     *
     * @param progress  0.0 ~ 1.0
     * @param total     总格数
     * @param fillColor 已填充颜色
     * @param emptyColor 未填充颜色
     */
    public static Component custom(float progress, int total, Color fillColor, Color emptyColor) {
        return makeBar(Math.clamp(progress, 0, 1), total, fillColor, emptyColor);
    }

    // ========== 内部 ==========

    /**
     * 创建竖线进度条
     *
     * @param progress  0.0 ~ 1.0
     * @param total     总格子数
     * @param fillColor 已填充部分的颜色
     * @param emptyColor 未填充部分的颜色
     */
    private static Component makeBar(float progress, int total, Color fillColor, Color emptyColor) {
        float clamped = Math.clamp(progress, 0f, 1f);
        int filled = (int) (clamped * total);
        int fillRgb = fillColor.getRGB() & 0xFFFFFF;
        int emptyRgb = emptyColor.getRGB() & 0xFFFFFF;

        MutableComponent bar = Component.empty();

        for (int i = 0; i < total; i++) {
            MutableComponent seg = Component.literal("|");
            seg.withStyle(Style.EMPTY.withColor(i < filled ? fillRgb : emptyRgb));
            bar.append(seg);
        }

        return bar;
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static Component gradientBar(float progress, int total, Color startColor, Color endColor) {
        StringBuilder sb = new StringBuilder();
        int current = (int) (progress * total);

        for (int i = 0; i < total; i++) {
            float t = (float) i / total;
            Color c = lerpColor(startColor, endColor, t);
            sb.append("§").append(toHex(c));
            sb.append("|");
        }

        return Component.literal(sb.toString());
    }

    private static Color lerpColor(Color a, Color b, float t) {
        int r = (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue()  - a.getBlue())  * t);
        return new Color(r, g, bl);
    }
}