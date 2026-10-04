package com.hjmmd_8.createoreexpansion.content.equipment.armor.skill;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;
import com.hjmmd_8.createoreexpansion.integration.skiller.ArmorSkillProvider;

/**
 * <b>装备技能的槽位表</b>（2026-10-05 行为零变化拆分，从
 * {@code ArmorSkillRuntime#skillId(ArmorSet, int)} 与 {@code #hasSkill(ArmorSet, int)} 逐字搬出）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>某一套装备的第 index 个装备槽位上是哪个技能</b>。
 * 技能 id 的真源仍然是 {@link ArmorSkillRuntime} 上的那 6 对公开常量
 * （{@code FALL_GUARD} / {@code CHARGE_DASH} / {@code LAST_STAND} / {@code FIELD_CHARGE} /
 * {@code BALANCE_CHOICE} / {@code STAR_SHOCK}），本类<b>只做映射、不定义 id</b> —— 所以常量
 * 一个都没有搬走（硬边界：public 形状只许新增不许删改）。</p>
 *
 * <p>搬运口径：方法体与原 {@code ArmorSkillRuntime} 里那一份<b>逐字相同</b>，差异只有两类 ——
 * {@code private} → 包级私有（跨类可见）、包里那几个技能 id 常量改写成
 * {@code ArmorSkillRuntime.<ID>} 限定名（原来的裸名只在宿主类里才解析得到）。判据、顺序、
 * 注释一个字都没有动；顺序错会静默取错配置表（编译通过、只有按住键看 HUD 才暴露），
 * 所以这里的 case 顺序与 {@code ArmorSkillProvider#SET_SKILL_IDS} 必须逐字同序。</p>
 */
final class ArmorSkillSlots {

    private ArmorSkillSlots() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 该套在第 index 个装备槽位上的技能 id。
     *
     * <p>顺序 = 槽位顺序（{@link ArmorSkillProvider#SLOT_BASE} 起），与
     * {@link ArmorSkillProvider#skillIdsOf} 的表<b>必须逐字同序</b>：那张表决定客户端轮询哪个槽位、
     * HUD 列哪几行，这里决定"按下这个槽位到底跑哪个技能"。</p>
     *
     * <p>翠玉套 {@code fall_guard / charge_dash}；宝石套（用户 2026-10-01 更正后的编排）
     * {@code last_stand / charge_dash / field_charge} —— 蓄能疾骋是<b>从翠玉套移植</b>的同一条技能
     * （同一个 id、同一套数值），因此它同时住在两套的同一段槽位空间里。
     * 星界套 {@code balance_choice / field_charge / star_shock}；雷鸣套
     * {@code balance_choice / field_charge}（<b>只有两个槽位</b>：用户 2026-10-02
     * 「雷鸣套装：1、2 通星界」，槽位 3 雷鸣威震<b>故意未登记、待做</b>）。</p>
     */
    static @Nullable String skillId(ArmorSet set, int index) {
        if (set == ArmorSet.JADE_TOPAZ) {
            return switch (index) {
                case 0 -> ArmorSkillRuntime.FALL_GUARD;
                case 1 -> ArmorSkillRuntime.CHARGE_DASH;
                default -> null;
            };
        }
        if (set == ArmorSet.SAPPHIRE_RUBY) {
            // 用户 2026-10-01 更正后的宝石套编排（三条，槽位顺序即此处 case 顺序，
            // 必须与 ArmorSkillProvider.SET_SKILL_IDS[SAPPHIRE_RUBY] 逐字同序）：
            //   槽位 1 = 绝境守护、槽位 2 = 蓄能疾骋（从翠玉套移植）、槽位 3 = 临域充力。
            return switch (index) {
                case 0 -> ArmorSkillRuntime.LAST_STAND;
                case 1 -> ArmorSkillRuntime.CHARGE_DASH;
                case 2 -> ArmorSkillRuntime.FIELD_CHARGE;
                default -> null;
            };
        }
        if (set == ArmorSet.ASTRAL) {
            // 星界套（用户 2026-10-02 星界轮，三条，槽位顺序即此处 case 顺序，
            // 必须与 ArmorSkillProvider.SET_SKILL_IDS[ASTRAL] 逐字同序）：
            //   槽位 1 = 衡元择势、槽位 2 = 临域充力（**同一个技能 id 的高等级形态**）、
            //   槽位 3 = 星芒嬗震。
            // 顺序错会**静默取错配置表**（编译通过、只有按住键看 HUD 才暴露）。
            return switch (index) {
                case 0 -> ArmorSkillRuntime.BALANCE_CHOICE;
                case 1 -> ArmorSkillRuntime.FIELD_CHARGE;
                case 2 -> ArmorSkillRuntime.STAR_SHOCK;
                default -> null;
            };
        }
        if (set == ArmorSet.THUNDER) {
            // 雷鸣套（用户 2026-10-02 雷鸣轮）：「雷鸣套装：1、2 通星界」——
            //   槽位 1 = 衡元择势、槽位 2 = 临域充力 II（**同一个技能 id 的高等级形态**）。
            // 执行体一律按 **skillId** 分派（ArmorSkillRuntime 里的 FIELD_CHARGE.equals(skill) /
            // BALANCE_CHOICE.equals(skill) 等分支），**不按套**，所以复用这两个 id 之后
            // 雷鸣套上它们**天然生效**，与星界套上行为完全一致 —— 只差基准等级 3 vs 2
            // （3 是装备技能封顶，见 EQUIPMENT_SKILL_MAX_LEVEL）。
            // ⚠ **只有两个 case，不许给 index 2 造任何东西**：槽位 3（雷鸣威震）本轮
            //   **故意不登记（待做）** ⇒ default -> null ⇒ hasSkill(THUNDER, 2) 为 false、
            //   装备段键三对雷鸣套不响应、HUD 只列两行。
            // 顺序必须与 ArmorSkillProvider.SET_SKILL_IDS[THUNDER] 逐字同序。
            return switch (index) {
                case 0 -> ArmorSkillRuntime.BALANCE_CHOICE;
                case 1 -> ArmorSkillRuntime.FIELD_CHARGE;
                default -> null;
            };
        }
        return null;
    }

    /** 该套在第 index 个装备槽位上有没有技能（雷鸣套的 1、2 通星界 ⇒ 见实现）。 */
    static boolean hasSkill(ArmorSet set, int index) {
        return skillId(set, index) != null;
    }
}
