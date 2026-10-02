package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

/**
 * <b>回旋镖技能的数值真源</b>（穿刺；环绕是批 4）—— 与 {@code FellingConfigs} / {@code SkinConfigs}
 * 同形：本类只写数字与纯算术，<b>不进任何 Minecraft 类型</b>，每一个魔数在这里只出现一次。
 *
 * <p>需求依据：开工需求 2026-10-02 §3.5（穿刺）、§3.7（技能等级与消耗）、§六 推断值 #6（id 与语言键）。
 * 消费方只有两处，各自读它的一段：</p>
 * <ul>
 *   <li>{@link #pierceEnergyCost(int)} —— 由 {@code BoomerangItem#throwCost} 读
 *       （<b>唯一的能量消耗出口</b>，需求 §3.7 的 ⚠：判合计与扣合计必须在同一处）；</li>
 *   <li>{@link #pierceMobQuota(int)} / {@link #pierceBlockQuota(int)} —— 由
 *       {@code AbstractBoomerangEntity} 在命中判定里读（投掷时一次性算好、记在实体上）。</li>
 * </ul>
 *
 * <h2>一、穿透额度（需求 §3.5：作者原话"至多可穿透 3×技能等级 个生物，以及 5×技能等级 个方块"）</h2>
 * <table border="1">
 *   <caption>等级 → 额度（Lv1..5）</caption>
 *   <tr><th>等级</th><th>生物额度 = 3L</th><th>方块额度 = 5L</th><th>穿刺附加消耗 = 20L</th></tr>
 *   <tr><td>1</td><td>3</td><td>5</td><td>20</td></tr>
 *   <tr><td>2</td><td>6</td><td>10</td><td>40</td></tr>
 *   <tr><td>3</td><td>9</td><td>15</td><td>60</td></tr>
 *   <tr><td>4</td><td>12</td><td>20</td><td>80</td></tr>
 *   <tr><td>5</td><td>15</td><td>25</td><td>100</td></tr>
 * </table>
 *
 * <h2>二、等级从哪里来</h2>
 * <p><b>基准等级</b>由档位给（{@link BoomerangTier#baseSkillLevel()}：翠玉 1 / 宝石 2 / 星界 3 / 雷鸣 3，
 * 需求 §3.1 给死 —— ⚠ 与护甲的"雷鸣 = 4"<b>不同</b>，别照抄 {@code ArmorSkillLevels}）；
 * <b>有效等级</b> = {@code SkillEnergyCost.effectiveLevel(stack, 基准, }{@link #MAX_SKILL_LEVEL}{@code )}
 * —— 技艺提升 / 技艺回溯各按 ±（3 级及以上按 2 计），然后钳到 {@code 1..}{@value #MAX_SKILL_LEVEL}。
 * 读取点只有 {@code BoomerangItem#effectiveSkillLevel(ItemStack, int)} 一处（物品与实体共用）。</p>
 * <p>⚠ 附带事实（批 3 报告里写明、代码不处理）：正提升量上限是 <b>+2</b>，所以
 * <b>翠玉实际最高 3 级、宝石最高 4 级</b>，只有星界/雷鸣（基准 3）能真正吃到 5 级。
 * 这是"基准等级 + 附魔 ±2"这条既有公式的必然结果，不是钳位写错。</p>
 *
 * <h2>三、为什么消耗是"每级 20"而不是一张逐级表</h2>
 * <p>需求 §3.7 给的就是一个乘法：{@code 20 × 等级}。写成 {@link #pierceEnergyCost(int)}
 * 而不是四个字面量，是为了让"改一个数就改所有等级"成立，也让关卡能把 {@code 20/40/60/80/100}
 * 逐值钉住（它算的就是这一行）。</p>
 *
 * @since 1.0.0
 */
public final class BoomerangSkillConfigs {

    private BoomerangSkillConfigs() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 回旋镖技能的等级上限（需求 §3.7："上限 5，钳到 1..5"）。
     *
     * <p>它是 {@code AllSkills.PIERCE} 的 {@code maxLevel}（注册处引用本常量，
     * 不在两处各写一个 5），也是 {@code SkillEnergyCost.effectiveLevel} 的钳位参数。</p>
     */
    public static final int MAX_SKILL_LEVEL = 5;

    /** 每级可穿透的<b>生物</b>数（需求 §3.5 的 3）。 */
    public static final int PIERCE_MOBS_PER_LEVEL = 3;

    /** 每级可穿透的<b>方块</b>数（需求 §3.5 的 5）。 */
    public static final int PIERCE_BLOCKS_PER_LEVEL = 5;

    /** 每级追加的<b>穿刺能量消耗</b>（需求 §3.7 的 20）。 */
    public static final int PIERCE_ENERGY_PER_LEVEL = 20;

    /** 把任意等级钳进 {@code 1..}{@link #MAX_SKILL_LEVEL}（额度与消耗共用同一条钳位）。 */
    public static int clampLevel(int level) {
        return Math.min(Math.max(1, level), MAX_SKILL_LEVEL);
    }

    /**
     * 本次投掷的<b>生物穿透额度</b> = {@code 3 × 等级}（Lv1..5 ⇒ 3 / 6 / 9 / 12 / 15）。
     *
     * @param level 有效技能等级（调用方一律先过 {@link #clampLevel(int)} 或
     *              {@code SkillEnergyCost.effectiveLevel}）
     */
    public static int pierceMobQuota(int level) {
        return PIERCE_MOBS_PER_LEVEL * clampLevel(level);
    }

    /** 本次投掷的<b>方块穿透额度</b> = {@code 5 × 等级}（Lv1..5 ⇒ 5 / 10 / 15 / 20 / 25）。 */
    public static int pierceBlockQuota(int level) {
        return PIERCE_BLOCKS_PER_LEVEL * clampLevel(level);
    }

    /**
     * 穿刺的<b>额外</b>投掷消耗 = {@code 20 × 等级}（Lv1..5 ⇒ 20 / 40 / 60 / 80 / 100）。
     *
     * <p>它是"追加在模式消耗上"的那一项（需求 §3.7）：模式消耗仍由
     * {@link BoomerangTier#throwCost(boolean)} 给，两者相加的唯一一处是
     * {@code BoomerangItem#throwCost(ItemStack, Player, boolean)}。</p>
     */
    public static int pierceEnergyCost(int level) {
        return PIERCE_ENERGY_PER_LEVEL * clampLevel(level);
    }
}
