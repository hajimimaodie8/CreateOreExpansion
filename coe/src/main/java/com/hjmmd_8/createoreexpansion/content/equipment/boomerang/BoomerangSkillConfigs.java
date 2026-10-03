package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

/**
 * <b>回旋镖技能的数值真源</b>（穿刺 + 环绕）—— 与 {@code FellingConfigs} / {@code SkinConfigs}
 * 同形：本类只写数字与纯算术，<b>不进任何 Minecraft 类型</b>，每一个魔数在这里只出现一次。
 *
 * <p>需求依据：开工需求 2026-10-02 §3.5（穿刺）、§3.6（环绕）、§3.7（技能等级与消耗）、
 * §六 推断值 #6（id 与语言键）、§六 推断值 #7（环绕参数沿用既有）。消费方各自读它的一段：</p>
 * <ul>
 *   <li>{@link #pierceEnergyCost(int)} / {@link #orbitEnergyCost(int)} —— 由
 *       {@code BoomerangItem#throwCost} 读（<b>唯一的能量消耗出口</b>，需求 §3.7 的 ⚠：
 *       判合计与扣合计必须在同一处）；</li>
 *   <li>{@link #pierceMobQuota(int)} / {@link #pierceBlockQuota(int)} —— 由
 *       {@code AbstractBoomerangEntity} 在命中判定里读（投掷时一次性算好、记在实体上）；</li>
 *   <li>{@link #orbitCount(int)} / {@link #orbitDamage(int)} / {@link #ORBIT_RADIUS} /
 *       {@link #ORBIT_ANGULAR_SPEED} / {@link #ORBIT_PHASE} —— 由
 *       {@code AbstractBoomerangEntity#spawnOrbitWaves} 读（投掷时按等级生成 L 枚环绕波）。</li>
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

    // ==================================================================================
    // ⛔ 已废除（作者 2026-10-02 第三次裁定）：「长按自带 1 生物 + 1 方块」这个暂定值
    // ==================================================================================
    // 第二次裁定曾把它当成"长按自带的那一部分穿刺"（暂定 1 只生物 / 1 个方块，与技能额度相加）。
    // 第三次裁定的原话是：长按（花瓣）沿轨迹走一圈时「**会破坏它途径中的所有方块，并对途径中的
    // 所有生物造成伤害，破坏方块和造成生物伤害没有任何限制**」，并明确「**以本条为准，别再按额度
    // 限制长按**」⇒ 那个暂定值当场作废：
    //   · 花瓣段**不消耗任何额度**（所有方块都破坏、所有生物都伤害，只受能力范围与本档挖掘门槛约束）；
    //   · 因此本类**不再**持有 BUILTIN_PIERCE_MOBS_ON_HOLD / BUILTIN_PIERCE_BLOCKS_ON_HOLD
    //     （旧值 1 / 1 写在这里，按本仓"口径被推翻要写明、不许静默删除"的规矩留档）；
    //   · 穿刺额度（3L / 5L）现在只服务**点按**那一支，且只在按住键一投掷时才有；
    //   · 花瓣段的边界改由"能力范围"决定（见 BoomerangTier#capabilityRange()）。

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

    // ==================================================================================
    // 环绕技能（需求 §3.6）—— 2026-10-02 批 4
    // ==================================================================================

    /**
     * 每级追加的<b>环绕能量消耗</b>（需求 §3.7 的 15）。
     *
     * <p>与穿刺同样：它是"额外"那一项，追加在模式消耗上；两者相加的<b>唯一一处</b>是
     * {@code BoomerangItem#throwCost(ItemStack, Player, boolean)}（需求 §3.7 陷阱 #7）。</p>
     */
    public static final int ORBIT_ENERGY_PER_LEVEL = 15;

    /**
     * 每级环绕波的<b>单枚伤害</b>（需求 §3.6 第 3 条：{@code 2 × 技能等级}）。
     *
     * <p>这个数不走既有波级表（波级表是 4/6/8/10/12），而是作为
     * {@code ChargerWaveEntity} 的"自定义伤害"要素写进环绕波（作者裁定 D9 = A：
     * 环绕波<b>必须</b>复用既有实体，所以伤害改成可覆写，默认仍是波级表）。</p>
     */
    public static final int ORBIT_DAMAGE_PER_LEVEL = 2;

    /**
     * <b>环绕半径</b>（格；需求 §3.6 第 2 条给死 <b>1.5</b>）。
     *
     * <p>与星芒嬗震的 0.80 是两个不同的数（那是星界轮需求 §3.3(b) 的值，本类不引用它）。</p>
     */
    public static final double ORBIT_RADIUS = 1.5D;

    /** <b>环绕角速度</b>：需求 §3.6 第 2 条"其他环绕参数和那个差不多" ⇒ 沿用既有的 <b>1 圈/秒</b>。 */
    public static final double ORBIT_TURNS_PER_SECOND = 1.0D;

    /**
     * <b>环绕角速度（弧度/tick）</b> = 2π × 圈/秒 ÷ 20；1 圈/秒 时 = <b>2π/20 ≈ 0.3142</b>。
     *
     * <p>与 {@code StarShockRuntime} 的同名常量<b>同一个算式、同一个值</b>
     * （需求 §六 推断值 #7："沿用现有 {@code applyOrbitElement()} 的值"）——
     * 刻意<b>不</b>跨类引用 StarShockRuntime 的私有常量（那是星界套的类，
     * 回旋镖不该依赖它），而是让两处算式逐字相同、并由关卡各自钉住这个值。</p>
     */
    public static final double ORBIT_ANGULAR_SPEED = 2.0D * Math.PI * ORBIT_TURNS_PER_SECOND / 20.0D;

    /**
     * <b>环绕初始相位</b>（弧度；需求 §3.6 第 2 条给死 <b>0</b>）。
     *
     * <p>它是<b>基相位</b>：第 {@code i} 枚环绕波的实际初始相位是
     * {@code ORBIT_PHASE + 2π·i/数量} —— 相位全等会让 L 枚小波<b>完全重合成一枚</b>
     * （"数量 = 等级"就白写了），而均匀分配是"绕成一圈"的唯一读法；
     * 第 0 枚的相位恰好就是 {@value #ORBIT_PHASE}（= 需求给的那个 0）。
     * 见报告 §⑥（需求没写死、由我定的部分）。</p>
     */
    public static final double ORBIT_PHASE = 0.0D;

    /**
     * 环绕波自己的<b>波级</b>（α = 1，见 {@code WaveLevels.LOW}）。
     *
     * <p>波级对环绕波<b>只剩观感意义</b>（颜色 / 粒子 / 拖尾风格）：伤害由自定义伤害要素给
     * （{@link #orbitDamage(int)}），位置由环绕要素每 tick 改写（速度表用不上）。
     * 取 α 是与星芒嬗震的环绕波（1~2 级）同一个量级，也是"粒子/波形对标那套"的一部分。</p>
     */
    public static final int ORBIT_WAVE_LEVEL = 1;

    /**
     * 本次投掷生成的<b>环绕波数量</b> = 技能等级（需求 §3.6 第 1 条：Lv1..5 ⇒ 1~5 枚）。
     *
     * <p>与额度/消耗共用同一条 {@link #clampLevel(int)} 钳位。</p>
     */
    public static int orbitCount(int level) {
        return clampLevel(level);
    }

    /** 环绕波的<b>单枚伤害</b> = {@code 2 × 等级}（Lv1..5 ⇒ 2 / 4 / 6 / 8 / 10）。 */
    public static int orbitDamage(int level) {
        return ORBIT_DAMAGE_PER_LEVEL * clampLevel(level);
    }

    /**
     * 环绕的<b>额外</b>投掷消耗 = {@code 15 × 等级}（Lv1..5 ⇒ 15 / 30 / 45 / 60 / 75）。
     *
     * <p>它是"追加在模式消耗上"的那一项（需求 §3.7）：模式消耗仍由
     * {@code BoomerangTier#throwCost(boolean)} 给，穿刺与环绕两项相加的唯一一处是
     * {@code BoomerangItem#throwCost(ItemStack, Player, boolean)}。</p>
     */
    public static int orbitEnergyCost(int level) {
        return ORBIT_ENERGY_PER_LEVEL * clampLevel(level);
    }
}
