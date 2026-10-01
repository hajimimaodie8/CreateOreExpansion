package com.hjmmd_8.createoreexpansion.content.skill.config;

/**
 * <b>绝境守护</b>（宝石套 · 槽位 1，基准等级 1）的分级数值 —— 与 {@link FallGuardConfigs}
 * 同形，是这条技能<b>唯一</b>的数值真源（被动触发与主动分段都读这里）。
 *
 * <h2>数值出处（用户 2026-10-01 规格，逐字照抄，未做任何"顺手优化"）</h2>
 * <ul>
 *   <li><b>被动概率</b>（{@code procChance}）：Lv1 <b>50%</b> / Lv2 <b>60%</b> / Lv3 <b>70%</b>
 *       —— 规格 §一 1.1。</li>
 *   <li><b>长按上限</b>（{@code holdSeconds}）：Lv1 <b>15</b> / Lv2 <b>10</b> / Lv3 <b>5</b> 秒
 *       —— 规格 §6.1 Q1 定稿，用户原话"长按上限不就是从 15 一级到 10 二级到 5 三级吗"。
 *       ⚠ 与蓄能疾骋"等级越高按得越久"<b>相反</b>：绝境守护等级越高 = 越快拿到高段 buff。</li>
 *   <li><b>能量总量</b>（{@code holdTotalCost}）：Lv1 <b>500</b> / Lv2 <b>400</b> / Lv3 <b>300</b>
 *       —— 规格 §一 1.2 表格"按满整段的能量总量"；实际扣除按与虚衡坠护同一个比例式
 *       （{@code ArmorSkillRuntime#holdCost}，<b>向下取整</b>）。</li>
 *   <li><b>段时长</b>（{@code segmentSeconds}）：Lv1 <b>20/40/60</b>（规格 §一 1.2 原文）、
 *       Lv2 <b>20/40/60/80</b>、Lv3 <b>24/48/72/96/120</b>。</li>
 *   <li><b>内置冷却</b>（{@code cooldownSeconds}）：三档都 <b>30</b> 秒。</li>
 *   <li><b>继承的摔落豁免概率</b>（{@code passiveFallChance}）：沿用虚衡坠护同级那一档
 *       （规格 §1.1"继承虚衡坠护的摔落豁免"）⇒ <b>直接读 {@link FallGuardConfigs}</b>，
 *       不复制数值（改那边这里跟着变，单一数据源）。</li>
 * </ul>
 *
 * <p>⚠ <b>Lv2/Lv3 的段时长与三档冷却都是我给的默认值</b>（规格 §7 的 Q3 / Q4 表：
 * Q3 冷却默认 <b>30 秒</b>、Q4 段时长默认 Lv2 = 20/40/60/80、Lv3 = 24/48/72/96/120）。
 * 用户确认后<b>只改本文件</b>这几个常量即可，其它地方不用动。</p>
 *
 * <h2>不死图腾 buff 的分级口径（规格 §7 Q5 默认值）</h2>
 * <p>"三类都按段位 +1 级"：段位 1..N ⇒ 三个效果的 amplifier 各 <b>+0..+（N-1）</b>，
 * 也就是在<b>原版不死图腾</b>给的那一组（吸收 amplifier 1、再生 amplifier 1、
 * 抗火 amplifier 0）之上逐段抬升。落点在 {@code LastStandHandler#applyTotemEffects}。</p>
 *
 * @since 1.0.0
 */
public final class LastStandConfigs {

    private LastStandConfigs() {
        throw new AssertionError("This class should not be instantiated");
    }

    /** 三档（装备技能 3 级封顶，用户 2026-10-01 明确"和工具的五级封顶不一样"）。 */
    public static final int MAX_LEVEL = 3;

    /**
     * 单级绝境守护定义。
     *
     * @param holdSeconds        长按上限秒数（超过即视为按满；等级越高越短）
     * @param holdTotalCost      按满整段的能量总量（实际按比例向下取整，口径同虚衡坠护）
     * @param segmentSeconds     每一段的图腾 buff 秒数（数组长度 = 段数 = 图腾增益封顶级）
     * @param procChance         被动触发概率（0~1，规格 §一 1.1 的 50/60/70%）
     * @param cooldownSeconds    内置冷却秒数（被动触发后 / 主动松手后都按它起冷）
     * @param passiveFallChance  继承的摔落豁免被动概率（与虚衡坠护同级同值）
     */
    public record Config(int holdSeconds, int holdTotalCost, int[] segmentSeconds, double procChance,
                         int cooldownSeconds, double passiveFallChance) {

        /** 段数（= 不死图腾增益能到的最高级别）。 */
        public int segments() {
            return segmentSeconds.length;
        }
    }

    // 被动概率 Lv1 50% / Lv2 60% / Lv3 70%；长按上限 15/10/5 秒；能量总量 500/400/300；
    // 段时长 Lv1 20/40/60（规格原文）、Lv2 20/40/60/80、Lv3 24/48/72/96/120（§7 Q4 默认值）；
    // 冷却一律 30 秒（§7 Q3 默认值）；摔落豁免概率沿用虚衡坠护同级那一档。
    public static final Config LEVEL_1 = new Config(15, 500, new int[] { 20, 40, 60 }, 0.50D, 30,
        FallGuardConfigs.LEVEL_1.passiveChance());
    public static final Config LEVEL_2 = new Config(10, 400, new int[] { 20, 40, 60, 80 }, 0.60D, 30,
        FallGuardConfigs.LEVEL_2.passiveChance());
    public static final Config LEVEL_3 = new Config(5, 300, new int[] { 24, 48, 72, 96, 120 }, 0.70D, 30,
        FallGuardConfigs.LEVEL_3.passiveChance());

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
     * <p>与 {@code ChargeDashConfigs#segmentOf} 逐字同一语义（整数运算，避免浮点误差）：
     * {@code ceil(heldTicks × segments / (holdSeconds × 20))} —— 一按下（哪怕 1 tick）
     * 就算进入第一段。</p>
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
