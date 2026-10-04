package com.hjmmd_8.createoreexpansion.content.skill.config.equipment;

/**
 * <b>虚衡坠护</b>（翠玉套 · 槽位 1）技能 —— 统一配置类（概率 / 长按上限 / 耗能 / 冷却的集中修改点）。
 *
 * <p>与 {@code FellingConfigs} 等同一套写法：{@code AllSkills} 只引用本类，不在别处写数值。</p>
 *
 * <h2>用户 2026-10-01 定稿的各级数值（原文口径，未做任何"顺手优化"）</h2>
 * <ul>
 *   <li><b>触发概率</b>：Lv1 50% / Lv2 60% / Lv3 70%（被动摔落豁免）。</li>
 *   <li><b>长按上限</b>：Lv1 3 秒 / Lv2 4 秒 / Lv3 5 秒。</li>
 *   <li><b>能量消耗</b>：整段长按的总量 Lv1 250 / Lv2 150 / Lv3 100（<b>等级越高越省</b>）。
 *       实际扣除按用户给的比例式：<b>{@code floor(按住秒数 / 上限秒数 × 总量)}</b>，
 *       提前松手只扣按比例的那部分，<b>一律向下取整</b>（见
 *       {@code ArmorSkillRuntime#holdCost}）。</li>
 *   <li><b>冷却</b>：Lv1 15 秒 / Lv2 10 秒 / Lv3 7 秒。</li>
 * </ul>
 *
 * <p>主动（长按）期间的豁免率恒为 100%（预告："按住技能键则 100% 可豁免摔落伤害"），
 * 三档都一样 —— 等级影响的是"能按多久 / 花多少 / 冷却多久"，不是豁免率。</p>
 *
 * @since 1.0.0
 */
public final class FallGuardConfigs {

    private FallGuardConfigs() {
    }

    /** 三档（装备技能 3 级封顶，用户 2026-10-01 明确"和工具的五级封顶不一样"）。 */
    public static final int MAX_LEVEL = 3;

    /**
     * 单级虚衡坠护定义。
     *
     * @param passiveChance    被动豁免概率（0~1）
     * @param holdSeconds      长按上限秒数（超过即视为按满）
     * @param holdTotalCost    按满整段的能量总量（实际按比例向下取整）
     * @param cooldownSeconds  冷却秒数（松手后开始计）
     */
    public record Config(double passiveChance, int holdSeconds, int holdTotalCost, int cooldownSeconds) {
    }

    public static final Config LEVEL_1 = new Config(0.50D, 3, 250, 15);
    public static final Config LEVEL_2 = new Config(0.60D, 4, 150, 10);
    public static final Config LEVEL_3 = new Config(0.70D, 5, 100, 7);

    /**
     * 按等级取配置（与 {@code FellingConfigs#config(int)} 同名同形，供
     * {@code AllSkills} 的 {@code configsByLevel} 直接引用）。
     *
     * @param level 技能等级（&lt;= 0 或超界时钳到 1~{@link #MAX_LEVEL}）
     */
    public static Config config(int level) {
        return switch (Math.max(1, Math.min(MAX_LEVEL, level))) {
            case 1 -> LEVEL_1;
            case 2 -> LEVEL_2;
            default -> LEVEL_3;
        };
    }
}
