package com.hjmmd_8.createoreexpansion.content.skill;

/**
 * <b>技能等级 → 档位定义</b>的查表工具（各 {@code *Configs} 的等级表统一走这里）。
 *
 * <p>各 {@code *Configs} 类自己持有 {@code LEVEL_1..LEVEL_N} 常量（数值真源仍在那里，不动），
 * 但"按整数等级取哪一档"这段 <b>switch 逐字相同</b>的样板曾经散在 14 处方法里
 * （伐树 / 锄地 / 开岩 / 引渠 / 血契 / 弓咒 / 缴械 / 掠夺 / 剥皮 / 蓄能疾骋 / 虚衡坠护 /
 * 临域充力 / 绝境守护 / 星芒嬗震）—— 越界等级的兜底口径、夹取写法一旦要改就得改 14 遍。
 * 本类把那两段样板收成两处：</p>
 *
 * <ul>
 *   <li>{@link #pick5(int, Object, Object, Object, Object, Object)} —— 五档，<b>不夹取</b>：
 *       {@code 1..4} 精确取，其余（0 / 负数 / ≥5）一律回落<b>第 5 档</b>。
 *       （与改造前各 {@code level(int)} 的 {@code default -> LEVEL_5} 逐字等价。）</li>
 *   <li>{@link #pick3Clamped(int, int, Object, Object, Object)} —— 三档，<b>先夹取</b>：
 *       {@code max(1, min(maxLevel, level))} 之后再取，{@code 1 / 2} 精确、其余第 3 档。
 *       （与改造前各 {@code config(int)} 的 {@code Math.max(1, Math.min(MAX_LEVEL, level))} 逐字等价。）</li>
 * </ul>
 *
 * <p>⚠ 两段语义<b>刻意不同</b>，不要合并：五档那支之所以不夹取，是因为调用方传的就是
 * {@code AllSkills} 里的技能等级，越界即"配置没登记"这种异常，回落最高档比回落最低档
 * 更接近原意图（原写法如此，本类只搬运不改口径）。</p>
 *
 * @since 1.0.0
 */
public final class SkillLevelTables {

    private SkillLevelTables() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 五档查表（<b>不夹取</b>）：{@code 1..4} 精确取，其余一律第 5 档。
     *
     * @param level  技能等级（越界回落第 5 档）
     * @param level1 一档定义
     * @param level2 二档定义
     * @param level3 三档定义
     * @param level4 四档定义
     * @param level5 五档定义（同时是越界兜底）
     * @return 命中的那一档
     */
    public static <T> T pick5(int level, T level1, T level2, T level3, T level4, T level5) {
        return switch (level) {
            case 1 -> level1;
            case 2 -> level2;
            case 3 -> level3;
            case 4 -> level4;
            default -> level5;
        };
    }

    /**
     * 三档查表（<b>先把等级夹取到 {@code [1, maxLevel]}</b>）：{@code 1 / 2} 精确取，其余第 3 档。
     *
     * @param level    技能等级（&lt;= 0 或超界会被夹取）
     * @param maxLevel 该技能表的最高等级（装备技能恒为 3）
     * @param level1   一档定义
     * @param level2   二档定义
     * @param level3   三档定义
     * @return 命中的那一档
     */
    public static <T> T pick3Clamped(int level, int maxLevel, T level1, T level2, T level3) {
        return switch (Math.max(1, Math.min(maxLevel, level))) {
            case 1 -> level1;
            case 2 -> level2;
            default -> level3;
        };
    }
}
