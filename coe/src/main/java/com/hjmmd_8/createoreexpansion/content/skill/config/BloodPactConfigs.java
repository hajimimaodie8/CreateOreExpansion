package com.hjmmd_8.createoreexpansion.content.skill.config;

/**
 * 血契置换（剑类：命中时把双方的<b>当前血量百分比</b>互换）技能 —— 统一配置类。
 *
 * <p>所有数值都在本文件统一定义，{@code AllSkills} / {@code CoeItems} 只负责引用、不改数值
 * ——与 {@link SkinConfigs} / {@link PlunderConfigs} 同形（coe-pact 批 1）。</p>
 *
 * <h2>等级的作用（作者 2026-10-03 未给曲线，按需求 §六 #4 推断 —— 可一句话改）</h2>
 * <ul>
 *     <li><b>消耗</b> = {@code 100 × 等级}（照同族既有值：剥取 / 夺取每一级都是 100）；</li>
 *     <li><b>冷却</b> = 恒 {@link #COOLDOWN_TICKS} tick = 30 秒（作者 2026-10-03 定）；
 *         等级只影响消耗，<b>不影响冷却</b>；</li>
 *     <li><b>交换比例与等级无关</b> ⇒ <b>比例不进等级表</b>：交换是「双方互换血量百分比」的
 *         恒等式（需求 §3.1 / §六 #4：玩家新血 = 上限_p × 对方百分比、对方新血 = 上限_t × 玩家百分比），
 *         按等级给比例加成会破坏作者的验收例子（20/20 ⇄ 30/40 ⇒ 15/20 与 40/40）
 *         ⇒ 本类与 {@link BloodPactConfig} 都<b>不含比例字段</b>。</li>
 * </ul>
 *
 * <h2>⚠ 冷却载体待作者裁定（A / B / C），批 2 才接</h2>
 * <p>D2 未裁定 ⇒ 本批（coe-pact 批 1）<b>只把冷却值放进配置</b>、<b>不接任何载体</b>：
 * {@link BloodPactConfig#cooldownTicks} 目前<b>没有任何读取方</b>。三个候选载体：</p>
 * <ul>
 *     <li><b>A</b> 沿用现有的「按物品记」（{@code ToolSkillCooldown} / 原版 {@code ItemCooldowns}，
 *         剥取 / 夺取走的就是这条）；</li>
 *     <li><b>B</b> 新建「按技能记」；</li>
 *     <li><b>C</b> 三个都改。</li>
 * </ul>
 * <p>本仓技能配置里的冷却字段历来记<b>秒</b>（{@code SkinConfig#cooldownSeconds} /
 * {@code PlunderConfig#cooldownSeconds}，{@code CoeSkillSupport.cooldownTicks} 负责 ×20）。
 * 这里按作者原话记 <b>tick</b>（{@link #COOLDOWN_TICKS} = 600），秒数由
 * {@link BloodPactConfig#cooldownSeconds()} 现算 —— 两种口径都只有一个真源，批 2 不必各自换算。</p>
 */
public final class BloodPactConfigs {

    private BloodPactConfigs() {
    }

    /**
     * 冷却时长（tick）：<b>600 tick = 30 秒</b>（作者 2026-10-03 定），Lv1~Lv5 恒为此值。
     *
     * <p>⚠ 载体待作者裁定，批 2 接 —— 见类注释。</p>
     */
    public static final int COOLDOWN_TICKS = 600;

    /**
     * 单级血契置换技能定义。
     *
     * @param energyCost    单次技能能量消耗（= {@code 100 × 等级}，照同族既有值）
     * @param cooldownTicks 技能冷却 tick（1~5 级恒为 {@link #COOLDOWN_TICKS} = 600 tick = 30 秒；
     *                      <b>载体待裁定</b>，批 1 不接）
     */
    public record Level(int energyCost, int cooldownTicks) {
    }

    // ========== 五个等级（两把剑的基准等级绑定 = coe-pact 批 3，本批不绑） ==========

    /** Lv1 —— 消耗 100 × 1 = 100；冷却 600 tick（30 秒） */
    public static final Level LEVEL_1 = new Level(100, COOLDOWN_TICKS);

    /** Lv2 —— 消耗 100 × 2 = 200；冷却 600 tick（30 秒） */
    public static final Level LEVEL_2 = new Level(200, COOLDOWN_TICKS);

    /** Lv3 —— 消耗 100 × 3 = 300；冷却 600 tick（30 秒） */
    public static final Level LEVEL_3 = new Level(300, COOLDOWN_TICKS);

    /** Lv4 —— 消耗 100 × 4 = 400；冷却 600 tick（30 秒） */
    public static final Level LEVEL_4 = new Level(400, COOLDOWN_TICKS);

    /** Lv5 —— 消耗 100 × 5 = 500；冷却 600 tick（30 秒） */
    public static final Level LEVEL_5 = new Level(500, COOLDOWN_TICKS);

    /**
     * 由等级定义构造 {@link BloodPactConfig}，供 {@code AllSkills} 注册血契置换技能使用。
     * 数值来源统一为本类，避免散落各处。
     */
    public static BloodPactConfig config(Level level) {
        return new BloodPactConfig(level.energyCost(), level.cooldownTicks());
    }

    // ========== 按等级取配置（一技能多等级） ==========

    /** 血契置换按等级取配置（Lv1~5） */
    public static Level level(int level) {
        return switch (level) {
            case 1 -> LEVEL_1;
            case 2 -> LEVEL_2;
            case 3 -> LEVEL_3;
            case 4 -> LEVEL_4;
            default -> LEVEL_5;
        };
    }
}
