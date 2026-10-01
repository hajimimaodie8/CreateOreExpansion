package com.hjmmd_8.createoreexpansion.content.skill.config;

/**
 * <b>蓄能疾骋</b>（装备技能槽位 2）的分级数值 —— 与 {@link FallGuardConfigs} 同形，是这条技能唯一的数值真源。
 *
 * <h2>数值出处</h2>
 * <p>长按上限、分段时长、冷却都是<b>用户 2026-10-01 给的规格</b>：</p>
 * <ul>
 *   <li>Lv1：三段 20/40/60 秒、长按上限 10 秒、冷却 60 秒（迅捷最高 III）</li>
 *   <li>Lv2：四段 20/40/60/80 秒、长按上限 15 秒、冷却 45 秒（迅捷最高 IV）</li>
 *   <li>Lv3：五段 24/48/72/96/120 秒、长按上限 20 秒、冷却 30 秒（迅捷最高 V）</li>
 * </ul>
 *
 * <p>⚠ <b>能量总量（{@code holdTotalCost} = 300 / 250 / 200）是我先给的默认值</b>：
 * 用户当时说"耗能我再给"、之后三次未给 ⇒ 为不卡住功能先按"等级越高越省"（与虚衡坠护 250/150/100 同风格）
 * 落一版。用户确认后只改本文件这三个常量即可，其它地方不用动。</p>
 *
 * @since 1.0.0
 */
public final class ChargeDashConfigs {

    private ChargeDashConfigs() {
    }

    /** 三档（装备技能 3 级封顶；与工具的五级封顶不同）。 */
    public static final int MAX_LEVEL = 3;

    /**
     * 单级蓄能疾骋定义。
     *
     * @param holdSeconds     长按上限秒数（按满即取最后一段）
     * @param segmentSeconds  每一段给多少秒迅捷（数组长度 = 段数 = 迅捷能到的最高级别）
     * @param holdTotalCost   按满整段的能量总量（实际按比例向下取整，口径与虚衡坠护完全一致）
     * @param cooldownSeconds 冷却秒数（松手后开始计）
     */
    public record Config(int holdSeconds, int[] segmentSeconds, int holdTotalCost, int cooldownSeconds) {

        /** 段数（= 迅捷能到的最高级别）。 */
        public int segments() {
            return segmentSeconds.length;
        }
    }

    public static final Config LEVEL_1 = new Config(10, new int[] { 20, 40, 60 }, 300, 60);
    public static final Config LEVEL_2 = new Config(15, new int[] { 20, 40, 60, 80 }, 250, 45);
    public static final Config LEVEL_3 = new Config(20, new int[] { 24, 48, 72, 96, 120 }, 200, 30);

    /** 按等级取配置（与 {@code FallGuardConfigs#config(int)} 同名同形）。 */
    public static Config config(int level) {
        return switch (Math.max(1, Math.min(MAX_LEVEL, level))) {
            case 1 -> LEVEL_1;
            case 2 -> LEVEL_2;
            default -> LEVEL_3;
        };
    }

    /**
     * 长按比例落在<b>第几段</b>（1 起算；按满或超时 = 最后一段）。
     *
     * <p>用整数运算：{@code ceil(heldTicks × segments / (holdSeconds × 20))}，
     * 一按下（哪怕 1 tick）就算进入第一段 —— 与"按多久给多久"的直觉一致。</p>
     *
     * @return 1 ~ {@code config.segments()}
     */
    public static int segmentOf(int heldTicks, Config config) {
        if (config == null || config.segments() <= 0) {
            return 1;
        }
        int maxTicks = Math.max(1, config.holdSeconds() * 20);
        int clamped = Math.max(1, Math.min(heldTicks, maxTicks));
        int segments = config.segments();
        int segment = (int) (((long) clamped * segments + maxTicks - 1) / maxTicks);
        return Math.max(1, Math.min(segments, segment));
    }
}
