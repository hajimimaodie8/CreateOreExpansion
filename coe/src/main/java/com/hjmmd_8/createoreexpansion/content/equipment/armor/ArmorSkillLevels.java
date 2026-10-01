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
 * <p>宝石套的两条技能（绝境守护 {@code last_stand} 基准 <b>1</b> /
 * 临域充力 {@code field_charge} 基准 <b>2</b>）本表<b>只登记基准等级</b>：
 * 它们的配置类、被动/主动效果、应力注入在<b>第 2 / 3 层</b>落地，
 * 因此目前<b>既不在</b> {@code ArmorSkillProvider#skillIdsOf} <b>也不在</b>内核注册表 / {@code AllSkills} 里
 * （id 常量见 {@code ArmorSkillRuntime#LAST_STAND_ID} / {@code FIELD_CHARGE_ID}）。</p>
 * <p>翠玉套现有两条（{@code fall_guard} / {@code charge_dash}）没有覆盖行 ⇒ 回落到
 * {@code ArmorSet.JADE} 的基准 1，与今天<b>逐字一致</b>，本轮不改变翠玉套的任何表现。</p>
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
        if (skillId != null && set == ArmorSet.GEM) {
            // 规格 §一 / §二：绝境守护（槽位 1）基准 1、临域充力（槽位 2）基准 2。
            if (ArmorSkillRuntime.LAST_STAND_ID.equals(skillId)) {
                return 1;
            }
            if (ArmorSkillRuntime.FIELD_CHARGE_ID.equals(skillId)) {
                return 2;
            }
        }
        return set.baseLevel();
    }
}
