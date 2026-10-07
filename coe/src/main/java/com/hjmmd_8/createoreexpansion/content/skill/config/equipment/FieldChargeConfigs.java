package com.hjmmd_8.createoreexpansion.content.skill.config.equipment;

import com.hjmmd_8.createoreexpansion.content.energyfield.charge.ChargeConfigs;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.skill.ArmorSkillRuntime;
import com.hjmmd_8.createoreexpansion.content.skill.SkillLevelTables;

/**
 * <b>临域充力</b>（宝石套 · 槽位 3，基准等级 1 —— 见 {@code ArmorSkillLevels}；用户 2026-10-01
 * 更正：原先是槽位 2 / 基准 2）的分级数值 ——
 * 与 {@link FallGuardConfigs} / {@link LastStandConfigs} 同形，是这条技能<b>唯一</b>的数值真源
 * （判定半径、应力、时长、冷却、耗能全部只在这里写一遍）。
 *
 * <h2>数值出处（用户 2026-10-01 规格，逐字照抄，未做任何"顺手优化"）</h2>
 * <ul>
 *   <li><b>检测半径</b>（{@code radius}）：Lv1 <b>2</b> / Lv2 <b>3</b> / Lv3 <b>4</b> 格
 *       —— 规格 §2.2 表格"检测半径（格，立方体）"。判定口径见规格 §2.1 第 1 条：
 *       <b>以玩家为中心、边长 = 2×radius+1 的立方体</b>（radius=2 ⇒ 5×5×5）。
 *       <b>用户 2026-10-02 加强</b>：三档判定半径各 +1 格（原 1/2/3 ⇒ 边长 3/5/7），
 *       其余四个字段（应力 / 时长 / 冷却 / 耗能）<b>逐字不动</b>。</li>
 *   <li><b>提供的应力</b>（{@code stressSu}）：Lv1 <b>8192</b> / Lv2 <b>16384</b> / Lv3 <b>32768</b> SU
 *       —— 规格 §2.2 表格 + §4.2（"容量按等级 8192/16384/32768 SU（我们自己定）"）。
 *       ⚠ <b>这是"网络口径的总应力"</b>：Create 的应力容量是<b>每 RPM</b> 的值，网络总容量
 *       = 容量 × |转速|，而我们的注入器转速固定 32 RPM ⇒ 每 RPM 容量 = {@code stressSu / 32}
 *       = 256 / 512 / 1024。口径落在 {@code StressInjectorBlockEntity#capacityPerRpm()}。</li>
 *   <li><b>可供能时长</b>（{@code durationSeconds}）：Lv1 <b>30</b> / Lv2 <b>45</b> / Lv3 <b>60</b> 秒
 *       —— 规格 §2.2 表格"可供能时长（秒）"。</li>
 *   <li><b>释放后冷却</b>（{@code cooldownSeconds}）：Lv1 <b>25</b> / Lv2 <b>20</b> / Lv3 <b>15</b> 秒
 *       —— 规格 §2.2 表格"释放后冷却（秒）"。</li>
 *   <li><b>长按耗能</b>（{@code energyPerSecond}）：Lv1 <b>100</b> / Lv2 <b>60</b> / Lv3 <b>50</b> 点/秒
 *       —— 规格 §2.2 表格"长按耗能（点/秒）"。</li>
 * </ul>
 *
 * <h2>规格 §七 的两个默认值（Q6 / Q7）</h2>
 * <ul>
 *   <li><b>Q6 = 持续附着</b>：8192/16384/32768 是"持续供给的总容量"，不是"每 N tick 附着一次"。
 *       落点：{@code FieldChargeRuntime} 每 tick 续期（曲柄 {@code inUse} 续期 + 注入器心跳 +
 *       把容量登记进曲柄所在动力网络）。</li>
 *   <li><b>Q7 = 到时限后"停止供能 + 移除注入器 + 进冷却"</b>（不是"把方块充满到上限"）。
 *       落点：{@code FieldChargeRuntime.HoldResult#TIME_UP} 那一条收尾路径。</li>
 * </ul>
 *
 * <h2>扣能口径（"点/秒"怎么落成整数）</h2>
 * <p>用户规格 §2.3："长按期间持续消耗能量（按上表点/秒）" ⇒ 本类给出
 * {@link #costAfter(int, Config)}：<b>{@code floor(heldTicks × energyPerSecond / 20)}</b>
 *（整数运算，向下取整；与 {@code ArmorSkillRuntime#holdCost} 的"比例式 + floor"同风格）。</p>
 * <p><b>它和 holdCost 的关系（可代数验证）</b>：把 {@code maxSeconds = durationSeconds}、
 * {@code totalCost = durationSeconds × energyPerSecond} 代进 holdCost
 * ⇒ {@code min(held, duration×20) × duration×eps / (duration×20) = min(held, duration×20) × eps / 20}
 * —— 与 {@link #costAfter} <b>逐值相同</b>。之所以仍然显式写一份，是为了让"点/秒"这条口径
 * 不依赖另一个公式的代数恒等（将来谁改了 holdCost 的封顶方式，这里不会跟着悄悄变），
 * 同时 HUD 的预览行仍可走 {@code ArmorSkillRuntime#holdCost}（两处逐值等价，
 * 关卡 {@code check-armor-sets.ps1} 第 23 节会断言数值与调用点）。</p>
 *
 * @since 1.0.0
 */
public final class FieldChargeConfigs {

    private FieldChargeConfigs() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 三档（装备技能 3 级封顶，与 {@link LastStandConfigs#MAX_LEVEL} 同口径）。
     *
     * <p>批 15 收敛：唯一字面量在 {@link ArmorSkillRuntime#EQUIPMENT_SKILL_MAX_LEVEL}
     * （真实等级的钳位处），本常量是它的<b>别名</b>，值逐位不变（仍是 3）。</p>
     */
    public static final int MAX_LEVEL = ArmorSkillRuntime.EQUIPMENT_SKILL_MAX_LEVEL;

    /**
     * 单级临域充力定义（规格 §2.2 表格的五行，逐行一个字段）。
     *
     * @param radius           检测半径（格，**立方体**半边；边长 = 2×radius+1）
     * @param stressSu         提供的应力总量（SU；网络口径，注入器按 32 RPM 折算成每 RPM 容量）
     * @param durationSeconds  可供能时长上限（秒）
     * @param cooldownSeconds  释放后冷却（秒）
     * @param energyPerSecond  长按耗能（点/秒）
     */
    public record Config(int radius, int stressSu, int durationSeconds, int cooldownSeconds,
                         int energyPerSecond) {

        /** 按满整段的总耗能（{@code durationSeconds × energyPerSecond}）—— 给 HUD 预览行的"总量"用。 */
        public int totalCost() {
            return Math.max(0, durationSeconds) * Math.max(0, energyPerSecond);
        }

        /** 按满整段的总 tick 数（时长上限）。 */
        public int durationTicks() {
            return Math.max(0, durationSeconds) * ChargeConfigs.TICKS_PER_SECOND;
        }

        /** 判定立方体的边长（2×radius+1）。 */
        public int boxSize() {
            return 2 * Math.max(0, radius) + 1;
        }
    }

    // 规格 §2.2 表格逐行照抄 —— 半径按用户 2026-10-02「临域充力加强」改为 2/3/4（边长 5/7/9）；
    // 其余四列不变：应力 8192/16384/32768、时长 30/45/60、冷却 25/20/15、耗能 100/60/50。
    public static final Config LEVEL_1 = new Config(2, 8192, 30, 25, 100);
    public static final Config LEVEL_2 = new Config(3, 16384, 45, 20, 60);
    public static final Config LEVEL_3 = new Config(4, 32768, 60, 15, 50);

    /** 按等级取配置（与 {@code LastStandConfigs#config(int)} 同名同形）。 */
    public static Config config(int level) {
        return SkillLevelTables.pick3Clamped(level, MAX_LEVEL, LEVEL_1, LEVEL_2, LEVEL_3);
    }

    /**
     * 按住 {@code heldTicks} 时<b>应当已经付出</b>的能量：
     * <b>{@code floor(heldTicks × energyPerSecond / 20)}</b>（点/秒按 tick 线性累计、向下取整）。
     *
     * <p>到达时长上限后不再增长（{@code heldTicks} 被 clamp 到 {@code durationSeconds × 20}）——
     * 口径与 {@code ArmorSkillRuntime#holdCost} 的封顶一致。</p>
     *
     * @param heldTicks 已按住的服务端 tick 数（&lt;= 0 ⇒ 0）
     * @param config    该等级的配置；{@code null} ⇒ 0
     */
    public static int costAfter(int heldTicks, Config config) {
        if (config == null || heldTicks <= 0 || config.energyPerSecond() <= 0) {
            return 0;
        }
        int maxTicks = config.durationTicks();
        int ticks = maxTicks <= 0 ? heldTicks : Math.min(heldTicks, maxTicks);
        // long 乘法：heldTicks（最长 60×20=1200）与 eps（最长 100）都小，但按纪律一律防溢出。
        return (int) ((long) ticks * config.energyPerSecond() / ChargeConfigs.TICKS_PER_SECOND);
    }

    /**
     * 从 {@code heldTicks} 到 {@code heldTicks + 1} 这一步新增的耗能（0 或 1~5 点）。
     *
     * <p>用途：临域充力是<b>边按边扣</b>的（与其它装备技能"松手一次性按比例扣"不同），
     * 所以"能量见底"的判据不是"累计应付 ≥ 可用"（那会在能量剩一半时就误停），
     * 而是<b>"下一步还扣得起吗"</b>。见 {@code ArmorSkillRuntime#isExhausted}。</p>
     */
    public static int stepCost(int heldTicks, Config config) {
        if (config == null) {
            return 0;
        }
        return Math.max(0, costAfter(heldTicks + 1, config) - costAfter(heldTicks, config));
    }
}
