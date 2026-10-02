package com.hjmmd_8.createoreexpansion.content.skill.config;

/**
 * <b>星芒嬗震</b>（星界套 · 槽位 3，基准等级 3 —— 见 {@code ArmorSkillLevels}）的分级数值 ——
 * 与 {@link FallGuardConfigs} / {@link LastStandConfigs} / {@link FieldChargeConfigs} 同形，
 * 是这条技能<b>唯一</b>的数值真源（冷却 / 蓄力上限 / 主波伤害 / 主波数量 / 环绕概率 / 耗能
 * 全部只在这里写一遍）。
 *
 * <h2>数值出处（用户 2026-10-02 星界轮需求 §3.3，逐字照抄）</h2>
 * <ul>
 *   <li><b>冷却</b>（{@code cooldownSeconds}）：Lv1 <b>10</b> / Lv2 <b>7</b> / Lv3 <b>4</b> 秒
 *       —— 需求 §3.3(a) 表格。作者原话曾写"1 级到 5 级依次 10/7/4"，但只给三个数，
 *       且伤害 8/10/11、发射数 1/2/3 也都是三档 ⇒ 已与作者确认<b>本技能 3 级</b>
 *       （"5 级"是笔误），与全模组"装备技能 3 级封顶"一致。</li>
 *   <li><b>长按蓄力上限</b>（{@code chargeSeconds}）：Lv1 <b>3</b> / Lv2 <b>2</b> / Lv3 <b>1</b> 秒
 *       —— 等级越高，"满效果"达成越快。</li>
 *   <li><b>主波伤害</b>（{@code mainDamage}）：Lv1 <b>8</b> / Lv2 <b>10</b> / Lv3 <b>11</b> 点。
 *       <b>环绕波伤害 = 主波伤害的一半</b>（⇒ 4 / 5 / 5.5；用户口径"所有附加能量波的伤害均为
 *       原来能量波的一半"），由 {@link #orbitDamage(int)} 给出（浮点，不做整数截断）。</li>
 *   <li><b>主波数量上限</b>（{@code maxMainWaves}）：Lv1 <b>1</b> / Lv2 <b>2</b> / Lv3 <b>3</b> 枚。
 *       Lv1 长按<b>永不分叉</b>（主波恒 1 枚），长按只抬环绕概率。</li>
 *   <li><b>环绕触发概率上限</b>（{@code orbitChanceCap}）：Lv1 <b>0.50</b> / Lv2 <b>0.75</b> /
 *       Lv3 <b>0.85</b>。实际概率 = {@code t × 上限}（线性，{@code t} 见下）。</li>
 *   <li><b>点按能量</b>（{@code tapCost}）：三档都 <b>400</b> 点（需求 §3.3(e)）。</li>
 *   <li><b>长按耗能</b>（{@code holdCostPerSecond}）：三档都 <b>100</b> 点/秒
 *       —— 与蓄力/冷却同一时间基（用户 2026-10-02 裁定第 15 条："按 tick 折算"）。</li>
 * </ul>
 *
 * <h2>蓄力曲线（需求 §六 推断值 #1 / #2，作者已裁定按推荐值执行）</h2>
 * <pre>
 *   t = min(按住秒数 / chargeSeconds, 1.0)
 *   主波数量  ：t 达到 **1/3、2/3、1.0** 时依次放出第 2、第 3 枚（离散分叉点）
 *   环绕概率  ：**t × 该级上限**（线性）
 *   点按（t ≈ 0）⇒ 1 枚主波、环绕概率 ≈ 0
 * </pre>
 * <p>分叉点与"1 枚"的关系见 {@link #mainWaveCount(int, Config)}：点按恒 ≥ 1 枚
 * （作者原话"点按也是可以的"），长按再按 t 加枚，且不超过该级上限。</p>
 *
 * @since 1.0.0
 */
public final class StarShockConfigs {

    private StarShockConfigs() {
        throw new AssertionError("This class should not be instantiated");
    }

    /** 三档（装备技能 3 级封顶，与其它四条装备技能同口径）。 */
    public static final int MAX_LEVEL = 3;

    /**
     * 分叉点（{@code t} 的下标常量）：第 {@code i} 个分叉点 = {@code FORK_POINTS[i]}。
     *
     * <p>用户裁定第 1 条：{@code t} 达 1/3、2/3、1.0 时依次放出第 2、第 3 枚主波。
     * 放成数组而不是三处字面量：改曲线只改这一行（"作者一句话能改"的落点）。</p>
     */
    public static final double[] FORK_POINTS = { 1.0D / 3.0D, 2.0D / 3.0D, 1.0D };

    /**
     * 单级星芒嬗震定义。
     *
     * @param cooldownSeconds    释放后冷却（秒；点按与长按共用）
     * @param chargeSeconds      长按蓄力上限（秒；{@code t} 的分母）
     * @param mainDamage         主波命中伤害（点）
     * @param maxMainWaves       主波数量上限（Lv1 恒 1 枚 ⇒ 长按永不分叉）
     * @param orbitChanceCap     环绕触发概率上限（0~1；实际 = t × 它）
     * @param tapCost            点按一次的装备能量消耗（点）
     * @param holdCostPerSecond  长按期间的装备能量消耗（点/秒）
     */
    public record Config(int cooldownSeconds, int chargeSeconds, float mainDamage, int maxMainWaves,
                         double orbitChanceCap, int tapCost, int holdCostPerSecond) {

        /** 蓄力上限的 tick 数（{@code chargeSeconds × 20}）。 */
        public int chargeTicks() {
            return Math.max(1, chargeSeconds * 20);
        }
    }

    // 需求 §3.3(a) 表格 + §3.3(e)：冷却 10/7/4、蓄力 3/2/1、伤害 8/10/11、主波 1/2/3、
    // 环绕概率上限 50/75/85%、点按 400、长按 100 点/秒（三档同值）。
    public static final Config LEVEL_1 = new Config(10, 3, 8f, 1, 0.50D, 400, 100);
    public static final Config LEVEL_2 = new Config(7, 2, 10f, 2, 0.75D, 400, 100);
    public static final Config LEVEL_3 = new Config(4, 1, 11f, 3, 0.85D, 400, 100);

    /** 按等级取配置（与其它四条装备技能的 {@code config(int)} 同名同形）。 */
    public static Config config(int level) {
        return switch (Math.max(1, Math.min(MAX_LEVEL, level))) {
            case 1 -> LEVEL_1;
            case 2 -> LEVEL_2;
            default -> LEVEL_3;
        };
    }

    /**
     * 该等级的主波伤害（{@code float}，直接喂给 {@code LivingEntity#hurt}）。
     *
     * <p>波实体侧的唯一取值口：{@code StarShockWaveEntity#getDamage()} 覆写基类、
     * 改查这里（基类 {@code WaveLevels.damage} 是"机器波"的表，本技能不碰它）。</p>
     */
    public static float damage(int level) {
        return config(level).mainDamage();
    }

    /**
     * 环绕波伤害 = <b>主波伤害的一半</b>（需求 §3.3(d)：4 / 5 / 5.5）。
     *
     * <p>刻意保留<b>小数</b>（Lv3 = 5.5）而不是取整：用户写的就是"原来能量波的一半"，
     * 5.5 点是原版能表示的伤害值（生命值是 float）。</p>
     */
    public static float orbitDamage(int level) {
        return config(level).mainDamage() / 2.0F;
    }

    /**
     * 蓄力进度 {@code t = min(按住 tick 数 / 蓄力上限 tick, 1.0)}（需求 §3.3(b) 的曲线）。
     *
     * @param heldTicks 已按住的服务端 tick 数
     * @param config    该等级的配置
     * @return 0.0 ~ 1.0
     */
    public static double chargeProgress(int heldTicks, Config config) {
        if (config == null || heldTicks <= 0) {
            return 0.0D;
        }
        int maxTicks = config.chargeTicks();
        return Math.min(1.0D, (double) heldTicks / (double) maxTicks);
    }

    /**
     * 该等级、该蓄力进度下<b>总共该有几枚主波</b>（含点按那第一枚）。
     *
     * <pre>
     *   t &lt; 1/3            ⇒ 1 枚
     *   1/3 ≤ t &lt; 2/3      ⇒ 2 枚
     *   2/3 ≤ t &lt; 1.0      ⇒ 3 枚
     *   t ≥ 1.0            ⇒ 该级上限（Lv1 = 1、Lv2 = 2、Lv3 = 3）
     * </pre>
     *
     * <p>用 {@code ≥ FORK_POINTS[i]} 的离散判定而不是"枚数 = ceil(t × 3)"：两者在分叉点上等价，
     * 但后者会把 {@code t = 0.34} 也算成 2 枚 —— 分叉点必须<b>逐点</b>可读、可单独改。</p>
     *
     * <p>点按（{@code heldTicks = 0} ⇒ {@code t = 0}）返回 <b>1</b>：作者原话"点按也是可以的"，
     * 点按就是"发出 1 枚主波，不需要长按"。</p>
     */
    public static int mainWaveCount(int heldTicks, Config config) {
        if (config == null) {
            return 1;
        }
        int cap = Math.max(1, config.maxMainWaves());
        double t = chargeProgress(heldTicks, config);
        int count = 1;
        for (double fork : FORK_POINTS) {
            if (t >= fork && count < cap) {
                count++;
            }
        }
        return Math.min(count, cap);
    }

    /**
     * 环绕波触发概率 = {@code t × 该级上限}（需求 §六 推断值 #2，线性）。
     *
     * <p>"每枚主波各自 0~1 枚"⇒ 每枚主波都单独掷一次这个概率（见 {@code StarShockRuntime}）。</p>
     */
    public static double orbitChance(int heldTicks, Config config) {
        if (config == null) {
            return 0.0D;
        }
        double t = chargeProgress(heldTicks, config);
        return Math.max(0.0D, Math.min(1.0D, t * config.orbitChanceCap()));
    }

    /**
     * 长按这么多 tick 时应扣的装备能量（<b>按 tick 折算</b>，用户裁定第 15 条）：
     * <b>{@code floor(heldTicks × holdCostPerSecond / 20)}</b>。
     *
     * <p>与 {@link FieldChargeConfigs#costAfter(int, Config)} 逐值同形（同一套"点/秒 ⇒ 整数"口径），
     * 但<b>不封顶</b>到蓄力上限之外："星芒嬗震的长按耗能按 tick 折算"只有一句口径，
     * 蓄满之后的每 tick 仍在扣（蓄满只是"不再长威力"，不是"停止收费"）——
     * 见 {@code StarShockRuntime#hold} 的说明。</p>
     */
    public static int holdCostAfter(int heldTicks, Config config) {
        if (config == null || heldTicks <= 0 || config.holdCostPerSecond() <= 0) {
            return 0;
        }
        return (int) ((long) heldTicks * config.holdCostPerSecond() / 20L);
    }
}
