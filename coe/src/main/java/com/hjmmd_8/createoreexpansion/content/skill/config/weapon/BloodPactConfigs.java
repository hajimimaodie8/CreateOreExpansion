package com.hjmmd_8.createoreexpansion.content.skill.config.weapon;

import com.hjmmd_8.createoreexpansion.content.skill.SkillLevelTables;

/**
 * 血契置换（剑类：命中时把双方的<b>当前血量百分比</b>互换）技能 —— 统一配置类。
 *
 * <p>所有数值都在本文件统一定义，{@code AllSkills} / {@code CoeItems} 只负责引用、不改数值
 * ——与 {@link SkinConfigs} / {@link PlunderConfigs} 同形（coe-pact 批 1）。</p>
 *
 * <h2>等级的作用（作者 2026-10-03 未给曲线，按需求 §六 #4 推断；2026-10-06 批 14 修正实扣）</h2>
 * <ul>
 *     <li><b>消耗</b> = <b>{@code 100 × 等级}</b>（照同族既有值：剥取 / 夺取每一级都是 100）
 *         —— <b>批 14 的写法</b>：本表五个等级行写的都是<b>同一个「一级消耗」</b>
 *         {@link #ENERGY_COST_PER_LEVEL}（= 100），"乘等级"由既有算术
 *         {@code CoeSkillSupport#cost} ← {@code SkillEnergyCost#compute}（{@code baseCost × 有效等级}）
 *         完成。⚠ 批 1~13 那五个行写的是 <b>100 / 200 / 300 / 400 / 500</b>，而实扣又乘一次有效等级
 *         ⇒ 实际是 {@code 100 × 等级²} = <b>100 / 400 / 900 / 1600 / 2500</b>（二次乘，不是口径）。
 *         作者批 14 裁定"实扣 = 100 × 等级"⇒ 逐级实扣 <b>100 / 200 / 300 / 400 / 500</b>；</li>
 *     <li><b>冷却</b> = 恒 {@link #COOLDOWN_TICKS} tick = 30 秒（作者 2026-10-03 定）；
 *         等级只影响消耗，<b>不影响冷却</b>；</li>
 *     <li><b>交换比例与等级无关</b> ⇒ <b>比例不进等级表</b>：交换是「双方互换血量百分比」的
 *         恒等式（需求 §3.1 / §六 #4：玩家新血 = 上限_p × 对方百分比、对方新血 = 上限_t × 玩家百分比），
 *         按等级给比例加成会破坏作者的验收例子（20/20 ⇄ 30/40 ⇒ 15/20 与 40/40）
 *         ⇒ 本类与 {@link BloodPactConfig} 都<b>不含比例字段</b>。</li>
 * </ul>
 *
 * <h2>冷却载体 = <b>B（新建"按技能记"）</b> —— 作者 2026-10-03 裁定，coe-pact 批 2 已接</h2>
 * <p>{@link BloodPactConfig#cooldownTicks} 的唯一读取方是
 * {@code integration/skiller/skill/BloodPactItemSkill}，载体是
 * {@code content/equipment/tool/energy/PerSkillCooldown}（玩家持久数据
 * {@code createoreexpansion:skill_cd_<技能 id>}，键 = 技能 id）。<b>不是</b>按物品记的
 * {@code ToolSkillCooldown}：后者的判定（{@code CoeSkillSupport#onCooldown}）是整把物品一个键，
 * 拿它接 30 秒会让同一把剑上的剥取 / 夺取一起冻结 —— 作者明确否掉了这种"连坐"。</p>
 * <p>老存档：冷却键不存在 ⇒ 读到 0 ⇒ <b>未冷却</b>（最自然的缺省，无需迁移）。</p>
 * <p>本仓技能配置里的冷却字段历来记<b>秒</b>（{@code SkinConfig#cooldownSeconds} /
 * {@code PlunderConfig#cooldownSeconds}，{@code CoeSkillSupport.cooldownTicks} 负责 ×20）。
 * 这里按作者原话记 <b>tick</b>（{@link #COOLDOWN_TICKS} = 600），秒数由
 * {@link BloodPactConfig#cooldownSeconds()} 现算 —— 两种口径都只有一个真源。</p>
 */
public final class BloodPactConfigs {

    private BloodPactConfigs() {
    }

    /**
     * 冷却时长（tick）：<b>600 tick = 30 秒</b>（作者 2026-10-03 定），Lv1~Lv5 恒为此值。
     *
     * <p>载体 = 按技能记的 {@code PerSkillCooldown}（coe-pact 批 2 已接）—— 见类注释。</p>
     */
    public static final int COOLDOWN_TICKS = 600;

    /**
     * <b>一级消耗</b>（作者 2026-10-06 批 14 裁定）：<b>100</b>。
     *
     * <p>实扣 = {@code 一级消耗 × 有效等级} = <b>100 / 200 / 300 / 400 / 500</b>（Lv1~Lv5）。
     * "乘等级"由既有算术 {@code CoeSkillSupport#cost(stack, baseCost, effectiveLevel)}
     * ← {@code SkillEnergyCost#compute} 完成，唯一调用点 =
     * {@code integration/skiller/skill/BloodPactItemSkill#consumeResource}。</p>
     *
     * <p>⚠ <b>为什么五个等级行写的是同一个数</b>：成本函数已经乘过一次有效等级，
     * 等级行再写"100 × 等级"就是<b>二次乘</b>（批 1~13 的 100/200/300/400/500 实际扣的是
     * 100/400/900/1600/2500）。形状与三条弓专属技能的
     * {@code *Configs#ENERGY_COST_PER_LEVEL}（一级消耗 150）逐字同形：
     * <b>表里只放一级消耗，等级由调用点乘</b>。</p>
     */
    public static final int ENERGY_COST_PER_LEVEL = 100;

    /**
     * 单级血契置换技能定义。
     *
     * @param energyCost    <b>一级消耗</b>（= {@link #ENERGY_COST_PER_LEVEL} = 100，照同族既有值）；
     *                      实扣 = 本值 × 有效等级（100 / 200 / 300 / 400 / 500）
     * @param cooldownTicks 技能冷却 tick（1~5 级恒为 {@link #COOLDOWN_TICKS} = 600 tick = 30 秒；
     *                      等级只影响消耗、不影响冷却，需求 §六 #4）
     */
    public record Level(int energyCost, int cooldownTicks) {
    }

    // ========== 五个等级（两把剑的基准等级绑定 = 2026-10-06 COE 批 13 已补：蓝宝石剑 Lv1 / 星辉石剑 Lv2） ==========
    // ⚠ 批 14：五个等级行的 energyCost 都是同一个「一级消耗」（见 ENERGY_COST_PER_LEVEL 的说明）——
    // 逐级写 100/200/.../500 会被 cost(..) 再乘一次有效等级（100/400/900/1600/2500）。

    /** Lv1 —— 一级消耗 100（实扣 100 × 1 = 100）；冷却 600 tick（30 秒） */
    public static final Level LEVEL_1 = new Level(ENERGY_COST_PER_LEVEL, COOLDOWN_TICKS);

    /** Lv2 —— 一级消耗 100（实扣 100 × 2 = 200）；冷却 600 tick（30 秒） */
    public static final Level LEVEL_2 = new Level(ENERGY_COST_PER_LEVEL, COOLDOWN_TICKS);

    /** Lv3 —— 一级消耗 100（实扣 100 × 3 = 300）；冷却 600 tick（30 秒） */
    public static final Level LEVEL_3 = new Level(ENERGY_COST_PER_LEVEL, COOLDOWN_TICKS);

    /** Lv4 —— 一级消耗 100（实扣 100 × 4 = 400）；冷却 600 tick（30 秒） */
    public static final Level LEVEL_4 = new Level(ENERGY_COST_PER_LEVEL, COOLDOWN_TICKS);

    /** Lv5 —— 一级消耗 100（实扣 100 × 5 = 500）；冷却 600 tick（30 秒） */
    public static final Level LEVEL_5 = new Level(ENERGY_COST_PER_LEVEL, COOLDOWN_TICKS);

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
        return SkillLevelTables.pick5(level, LEVEL_1, LEVEL_2, LEVEL_3, LEVEL_4, LEVEL_5);
    }
}
