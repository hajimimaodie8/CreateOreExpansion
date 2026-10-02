package com.hjmmd_8.createoreexpansion.content.equipment.armor;

import net.minecraft.resources.ResourceLocation;

/**
 * <b>逐技能基准等级表</b>（用户 2026-10-01 规格 §8 第 1 层的唯一真源）。
 *
 * <h2>用户口径（原文）</h2>
 * <p>「整套装备各个技能等级的基准计算公式是：原来的技能等级，加上或减去因为附魔而改变的最大参数。
 * …此时，取所有基准加减值的最大值，然后把它赋予到所有的技能上。」</p>
 * <p>用户给的例子：蓝宝石套各技能基准 = 绝境守护 1 / 临域充力 2；某一件护甲附魔「技能提升 1」
 * ⇒ 最大值为 {@code max(1+1, 2+1)}…也就是玩家看到的"整体都往上挪了一级"。</p>
 *
 * <h2>落成实现</h2>
 * <ol>
 *   <li><b>每个技能有自己的一行基准</b>（同一套里各技能基准可以不同，见 {@link #baseLevelOf}：
 *       有显式覆盖用覆盖，否则回落到 {@link ArmorSet#baseLevel()}）；</li>
 *   <li>对<b>某个技能</b>，逐件算 {@code 该技能基准 + 技艺提升 − 记忆回溯}，
 *       取 {@link ArmorSet#armorSlots() 四件}里的<b>最大值</b>，再钳到
 *       {@code 1..FallGuardConfigs.MAX_LEVEL}（见 {@code ArmorSkillRuntime#effectiveLevel}）；</li>
 *   <li>等级<b>逐技能</b>求值、逐条陈列 —— 用户 2026-10-01 第二轮明确否掉"整体的 LV1"那种写法：
 *       「把套装里每个技能各自的等级都列出来」。（规格 §0.2）</li>
 * </ol>
 *
 * <h2>本轮范围（规格 §8 第 1 层）—— 只登记等级，不实现行为</h2>
 * <p>宝石套的三条技能本表<b>逐条显式登记基准等级</b>（用户 2026-10-01 更正后的编排）：
 * 槽位 1 = 绝境守护 {@code last_stand} 基准 <b>1</b>、槽位 2 = 蓄能疾骋 {@code charge_dash}
 * 基准 <b>2</b>（<b>从翠玉套移植</b>：同一个技能 id、同一套数值，翠玉套那份保持不动）、
 * 槽位 3 = 临域充力 {@code field_charge} 基准 <b>1</b>。
 * 三条都<b>不留白</b>：不要把临域充力交给"回落套基准 2" —— 宝石套的套基准恰好是 2，
 * 靠回落会把它的基准悄悄算成 2，与用户口径不符（id 常量见
 * {@code ArmorSkillRuntime#LAST_STAND_ID} / {@code CHARGE_DASH_ID} / {@code FIELD_CHARGE_ID}）。</p>
 * <p>翠玉套现有两条（{@code fall_guard} / {@code charge_dash}）没有覆盖行 ⇒ 回落到
 * {@code ArmorSet.JADE_TOPAZ} 的基准 1，与今天<b>逐字一致</b>，本轮不改变翠玉套的任何表现
 * （蓄能疾骋在翠玉套上仍是基准 1 + 回落；宝石套那一档是显式的 2）。</p>
 *
 * <h2>星界套的三条（用户 2026-10-02 星界轮）</h2>
 * <p>槽位 1 = 衡元择势 {@code balance_choice} 基准 <b>3</b>（= 套基准，<b>不必</b>显式登记）、
 * 槽位 2 = 临域充力 {@code field_charge} 基准 <b>2</b>、槽位 3 = 星芒嬗震 {@code star_shock}
 * 基准 <b>3</b>。</p>
 * <p><b>为什么只有 {@code field_charge} 必须显式</b>：星界套的 {@link ArmorSet#baseLevel()}
 * 是 <b>3</b>，而临域充力在星界套上的基准是 <b>2</b>（同一个技能 id 的高等级形态，
 * 见需求 §3.1）——靠"回落到套基准"会被<b>静默算成 3 级</b>：编译通过、只有进游戏按住键看
 * HUD 的罗马数字才暴露。另两条恰好等于套基准 3，回落与显式登记<b>同值</b>。</p>
 *
 * @since 1.0.0
 */
public final class ArmorSkillLevels {

    private ArmorSkillLevels() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 该套上<b>某个技能</b>的基准等级。
     *
     * <p>有该技能的显式覆盖就用覆盖，否则回落到该套自身的基准等级
     * （{@link ArmorSet#baseLevel()}，而不是 {@code wornLevel}：本表描述的是"套与技能的基准"，
     * 与玩家此刻穿没穿、穿了几件无关）。</p>
     *
     * @param set     套装；{@code null} ⇒ 0
     * @param skillId 技能 id（如 {@code createoreexpansion:last_stand}）；{@code null} ⇒ 该套基准
     * @return 基准等级（1~4）
     */
    public static int baseLevelOf(ArmorSet set, ResourceLocation skillId) {
        if (set == null) {
            return 0;
        }
        if (skillId != null && set == ArmorSet.SAPPHIRE_RUBY) {
            // 宝石套（用户 2026-10-01 更正后的编排，三项**各自显式**登记）：
            //   槽位 1 = 绝境守护 last_stand     基准 1（已实现）
            //   槽位 2 = 蓄能疾骋 charge_dash    基准 2（**从翠玉套移植**：同一 id、同一套数值）
            //   槽位 3 = 临域充力 field_charge   基准 1（用户 2026-10-01 更正：原来是槽位 2 / 基准 2）
            // ⚠ 三项都必须显式：宝石套的套基准 baseLevel() 是 2，靠回落会把临域充力算成 2。
            if (ArmorSkillRuntime.LAST_STAND_ID.equals(skillId)) {
                return 1;
            }
            if (ArmorSkillRuntime.CHARGE_DASH_ID.equals(skillId)) {
                return 2;
            }
            if (ArmorSkillRuntime.FIELD_CHARGE_ID.equals(skillId)) {
                return 1;
            }
        }
        // === SET-BRANCH-END: SAPPHIRE_RUBY ===
        // 上面那条注释是**分支边界哨兵**（不是装饰）：关卡第 21b 节按"从 `set == ArmorSet.SAPPHIRE_RUBY`
        // 起、到本哨兵止"截取宝石分支再逐条解析它的显式覆盖，否则新增的星界分支会被并进宝石分支一起数
        // （2026-10-02 实测：星界三条被计入宝石，断言 `skill-levels-gem-3` 由 3 条变成 6 条而假红）。
        // ⚠ **每一个新增的套装分支都必须在它后面补一行同形式的哨兵**，否则下一轮会出现同一个假红。
        if (skillId != null && set == ArmorSet.ASTRAL) {
            // 星界套（用户 2026-10-02 星界轮，三条各自显式登记）：
            //   槽位 1 = 衡元择势 balance_choice 基准 3（= 套基准）
            //   槽位 2 = 临域充力 field_charge  基准 2（**同一个技能 id 的高等级形态**）
            //   槽位 3 = 星芒嬗震 star_shock     基准 3（= 套基准）
            // ⚠ 只有 field_charge 是"必须显式"的那一条：星界套的套基准是 3，
            //   靠回落会把临域充力算成 3 级（编译全绿、只有看 HUD 才暴露）。
            //   另两条与套基准同值，仍然显式写出来 —— 表里不留白，
            //   "某个技能的基准"因此永远能在本方法里逐条读到，不依赖套基准的数值。
            if (ArmorSkillRuntime.BALANCE_CHOICE_ID.equals(skillId)) {
                return 3;
            }
            if (ArmorSkillRuntime.FIELD_CHARGE_ID.equals(skillId)) {
                return 2;
            }
            if (ArmorSkillRuntime.STAR_SHOCK_ID.equals(skillId)) {
                return 3;
            }
        }
        // === SET-BRANCH-END: ASTRAL ===
        return set.baseLevel();
    }
}
