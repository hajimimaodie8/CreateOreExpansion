package com.hjmmd_8.createoreexpansion.content.skill.config;

import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;

/**
 * <b>星芒嬗震</b>（星界套 · 槽位 3，基准等级 1 —— 见 {@code ArmorSkillLevels}）的分级数值 ——
 * 与 {@link FallGuardConfigs} / {@link LastStandConfigs} / {@link FieldChargeConfigs} 同形，
 * 是这条技能<b>唯一</b>的数值真源（冷却 / 蓄力上限 / 主波伤害 / 主波数量 / 环绕概率 / 耗能
 * 全部只在这里写一遍）。
 *
 * <h2>⛔ 波<b>不</b>由本表定义：本表只说"发几枚、多快、花多少"，波本身走既有五要素</h2>
 * <p>作者 2026-10-02 硬口径：「能量波是由好几个要素定义的…<b>你不要再凭空造出一个新的能量波哈，
 * 不要造出一个攻击波哈</b>」⇒ 这条技能发出的波就是既有的
 * {@code content/charger/entity/ChargerWaveEntity}（实体类型 {@code createoreexpansion:charger_wave}，
 * 与三台应力充能器/差波器<b>同一个类型、同一个渲染器</b>），它的一切由既有的<b>波情五要素</b>决定
 * （口径见 AGENTS.md「波情'五要素'的唯一取值点」）：</p>
 * <table border="1">
 *   <caption>五要素的既有取值点，以及本技能各填什么</caption>
 *   <tr><th>要素</th><th>既有取值点</th><th>星芒嬗震</th></tr>
 *   <tr><td>波速</td><td>{@code AbstractChargerWaveEntity#getSpeedBlocks()} =
 *       {@code WaveLevels#baseSpeed(波级)} + 速度修正（夹在 {@code WaveLevels#maxSpeed}）</td>
 *       <td>不单独设 —— 由波级带出（γ/ε/ω = 6/7/8 格/秒），修正值 0</td></tr>
 *   <tr><td>波级</td><td>{@code AbstractChargerWaveEntity#getWaveLevel()}（构造参数，1~5 = α/β/γ/ε/ω）</td>
 *       <td><b>{@link #waveLevelFor(int)}</b>：技能 1/2/3 级 ⇒ <b>γ / ε / ω</b>（理由见该方法）</td></tr>
 *   <tr><td>波载荷</td><td>{@code StellarWaveEntity} 的 payload（只有星辉波变器的变体波带载荷）</td>
 *       <td><b>空载</b>：普通能量波（{@code ChargerWaveEntity}）本来就不带载荷，本技能也不给</td></tr>
 *   <tr><td>波型</td><td>{@code AbstractChargerWaveEntity#trySetWaveType}（一生只能从普通波变一次）</td>
 *       <td><b>{@code WaveTypes.ATTACK}</b>（攻击态 —— 就是"变器攻击波变态"引燃出来的那一个波型）</td></tr>
 *   <tr><td>剩余寿命</td><td>{@code AbstractChargerWaveEntity#getRemainingLifetime()}（200 tick 上限 − 已存活）</td>
 *       <td>不设 —— 沿用既有的 <b>200 tick（10 秒）</b>上限</td></tr>
 * </table>
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
 *       ⚠ <b>它不再直接是伤害值，而是"挑波级的依据"</b>：波的实际伤害由波级查
 *       {@link WaveLevels#damage(int)} 得来（{@link #waveLevelFor(int)} 就是按这条对齐的）——
 *       既有系统的伤害**只能**由波级决定（{@code WaveType#dealsDamage()} 的注释写明
 *       "伤害值仍由波级决定，见 WaveLevels#damage"）。</li>
 *   <li><b>主波数量上限</b>（{@code maxMainWaves}）：Lv1 <b>1</b> / Lv2 <b>2</b> / Lv3 <b>3</b> 枚。
 *       Lv1 长按<b>永不分叉</b>（主波恒 1 枚）。</li>
 *   <li><b>环绕触发概率上限</b>（{@code orbitChanceCap}）：Lv1 <b>0.50</b> / Lv2 <b>0.75</b> /
 *       Lv3 <b>0.85</b>。实际概率 = {@code t × 它}（{@link #orbitChance(int, Config)}），
 *       <b>点按（t = 0）恒为 0</b>；承载方式是既有波实体上的<b>环绕波要素</b>
 *       （{@code setOrbitAnchor}，不新增实体类型），环绕波级见 {@link #orbitWaveLevelFor(int)}。</li>
 *   <li><b>点按能量</b>（{@code tapCost}）：三档都 <b>400</b> 点（需求 §3.3(e)）。</li>
 *   <li><b>长按耗能</b>（{@code holdCostPerSecond}）：三档都 <b>100</b> 点/秒
 *       —— 与蓄力/冷却同一时间基（用户 2026-10-02 裁定第 15 条："按 tick 折算"）。</li>
 * </ul>
 *
 * <h2>蓄力曲线（需求 §六 推断值 #1 / #2，作者已裁定按推荐值执行）</h2>
 * <pre>
 *   t = min(按住秒数 / chargeSeconds, 1.0)
 *   主波数量  ：t 达到 **1/3、2/3、1.0** 时依次放出第 2、第 3 枚（离散分叉点）
 *   环绕概率  ：**t × 该级上限**（线性；点按 t = 0 ⇒ 0）—— 承载在既有波的环绕波要素上
 *   点按（t ≈ 0）⇒ 1 枚主波
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
     * @param mainDamage         需求里写的主波伤害（点）——<b>不是直接伤害值</b>，而是挑波级的依据
     *                           （见 {@link #waveLevelFor(int)}）
     * @param maxMainWaves       主波数量上限（Lv1 恒 1 枚 ⇒ 长按永不分叉）
     * @param orbitChanceCap     环绕触发概率上限（0~1；实际 = t × 它；见 {@link #orbitChance(int, Config)}）
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
     * <b>技能等级 → 波级</b>（本技能"按技能等级取波级"的唯一映射点；2026-10-02 星界轮第二版重定）。
     *
     * <p>作者把星芒嬗震的基准等级改成 <b>1</b> 之后，原来"技能等级直接当波级"的写法（⇒ α/β/γ）
     * 就必须重定：需求 §3.3(d) 给的主波伤害是 <b>8 / 10 / 11</b>，而既有系统里伤害**只能**由
     * 波级决定 —— {@link WaveLevels#damage(int)} 只有 4 / 6 / 8 / 10 / 12 五个取值。于是映射
     * 就按"<b>取伤害不小于需求值的最低波级</b>"来定：</p>
     *
     * <table border="1">
     *   <caption>映射表（以既有系统允许的取值为准）</caption>
     *   <tr><th>技能等级</th><th>需求伤害</th><th>波级</th><th>符号</th><th>实际伤害</th>
     *       <th>波速</th><th>差</th></tr>
     *   <tr><td>1（基准）</td><td>8</td><td>3</td><td>γ</td><td>8</td><td>6 格/秒</td><td>0</td></tr>
     *   <tr><td>2</td><td>10</td><td>4</td><td>ε</td><td>10</td><td>7 格/秒</td><td>0</td></tr>
     *   <tr><td>3</td><td>11</td><td>5</td><td>ω</td><td>12</td><td>8 格/秒</td><td>+1</td></tr>
     * </table>
     *
     * <p><b>Lv3 的 +1 是这条映射唯一的偏差</b>（需求 11、既有表只有 10 与 12）：取 12 而不是 10，
     * 因为"取伤害不小于需求值的最低档"是单调、可一句话说明的规则；取 10 会低于需求。
     * 若作者要"宁低不高"，把下面的比较改成 {@code <=} 即可（3 级会落到 ε = 10）。</p>
     *
     * <p><b>越界容错</b>：{@code level} 先经 {@link #config(int)} 钳到 1~{@link #MAX_LEVEL}；
     * 需求伤害高于 ω 的 12 时返回 {@link WaveLevels#MAX_LEVEL}（封顶）。</p>
     */
    public static int waveLevelFor(int level) {
        float wanted = config(level).mainDamage();
        int chosen = WaveLevels.LOW;
        for (int lv = WaveLevels.LOW; lv <= WaveLevels.MAX_LEVEL; lv++) {
            chosen = lv;
            if (WaveLevels.damage(lv) >= wanted) {
                break;
            }
        }
        return chosen;
    }

    /**
     * 需求里写的那一档主波伤害（8 / 10 / 11）。
     *
     * <p>⚠ 它<b>不是</b>这条技能实际打出的伤害：实际伤害由 {@link #waveLevelFor(int)} 选出的
     * 波级查 {@link WaveLevels#damage(int)} 得来（8 / 10 / 12）。本方法保留为"需求值的读取口"，
     * 供 {@link #waveLevelFor(int)} 与文档/关卡核对。</p>
     */
    public static float damage(int level) {
        return config(level).mainDamage();
    }

    /**
     * 环绕波伤害 = <b>主波伤害的一半</b>（需求 §3.3(d)：4 / 5 / 5.5）。
     *
     * <p>⚠ <b>它不是"要打出的伤害"，而是"挑环绕波级的依据"</b>——既有系统里伤害只能由波级决定
     * （见 {@link #waveLevelFor(int)} 的同一口径），所以真正落地的是
     * {@link #orbitWaveLevelFor(int)} 选出的波级查 {@link WaveLevels#damage(int)} 得到的值。</p>
     */
    public static float orbitDamage(int level) {
        return config(level).mainDamage() / 2.0F;
    }

    /**
     * <b>环绕波的波级</b>（需求 §3.3(d)"环绕波伤害 = 主波一半"落在既有系统上的取值点）。
     *
     * <p>规则与 {@link #waveLevelFor(int)} <b>逐字同形</b>：<b>取伤害不小于
     * {@link #orbitDamage(int)} 的最低波级</b>（"取不到一半就宁高不宁低"）。实际取值：</p>
     * <table border="1">
     *   <caption>环绕波级（现算，关卡 star-orbit-damage-map 用同一规则复算）</caption>
     *   <tr><th>技能等级</th><th>需求主波伤害</th><th>一半</th><th>环绕波级</th><th>符号</th>
     *       <th>实际伤害</th></tr>
     *   <tr><td>1</td><td>8</td><td>4</td><td>1</td><td>α</td><td>4（恰好一半）</td></tr>
     *   <tr><td>2</td><td>10</td><td>5</td><td>2</td><td>β</td><td>6（≥ 一半的最低档）</td></tr>
     *   <tr><td>3</td><td>11</td><td>5.5</td><td>2</td><td>β</td><td>6（≥ 一半的最低档）</td></tr>
     * </table>
     * <p>按"主波<b>实际</b>伤害（8/10/12）的一半"算结果完全一样（4 / 5 / 6）——
     * 两种口径在这张表上不分叉，故取与需求表同源的 {@link #orbitDamage(int)}。</p>
     */
    public static int orbitWaveLevelFor(int level) {
        float wanted = orbitDamage(level);
        int chosen = WaveLevels.LOW;
        for (int lv = WaveLevels.LOW; lv <= WaveLevels.MAX_LEVEL; lv++) {
            chosen = lv;
            if (WaveLevels.damage(lv) >= wanted) {
                break;
            }
        }
        return chosen;
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
     * <p><b>每枚主波各自滚一次</b>（需求 §3.3(b)"每枚主波各自 0~1 枚"），
     * 滚骰点在 {@code StarShockRuntime#fireMainWave}，每枚主波只滚一次。</p>
     *
     * <p><b>点按恒不生成环绕波</b>：点按那一 tick 传 {@code heldTicks = 0} ⇒
     * {@link #chargeProgress(int, Config)} 返回 {@code 0.0} ⇒ 概率 {@code 0.0}
     * （不是"很小"，是恒 0）。关卡 {@code star-orbit-tap-zero} 守着这条公式形状。</p>
     *
     * <p><b>承载方式（2026-10-02 裁定）</b>：环绕波不是新实体，而是<b>既有</b>
     * {@code ChargerWaveEntity} 上那个"通用、可选、默认关闭"的<b>环绕波要素</b>
     * （{@code AbstractChargerWaveEntity#setOrbitAnchor}：父波 UUID + 半径 + 角速度 + 相位）；
     * 父波消散时环绕波靠"取不到父波即 discard"一起收尾。</p>
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
