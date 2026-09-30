package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.common.i18n.Translatable;
import net.minecraft.resources.ResourceLocation;

/**
 * 物品技能条目的<b>只读元数据契约</b>（技能换核后的形态）。
 *
 * <p><b>2026-09-30 技能换核第 5 阶段</b>：本接口原先还带着执行与数值成员
 * ——{@code release(Object, DataSkill)}、{@code canRelease(Object, DataSkill)}、
 * {@code getCost()}、{@code getCooldownSeconds()}。它们的存在理由是"技能实例自己会跑"，
 * 而执行早已迁到 Skiller 新内核（{@code integration/skiller/skill/*ItemSkill}），
 * 旧实现类也全部删除；这四经全仓检索确认<b>零调用者</b>，故一并退役。</p>
 *
 * <p>现在这个接口只回答两个问题：<b>这条技能是什么类型</b>（决定由哪个触发族释放）
 * 与 <b>它的翻译键是什么</b>（tooltip 显示）。实现体只有
 * {@link MetadataSkill}（纯元数据壳）与 {@link DataSkill} 的承载关系。</p>
 */
public interface ItemSkill extends Translatable {

    /** 技能类型 —— 决定由哪个触发族（挖掘 / 受击 / 使用物品）释放。 */
    SkillType getType();

    /**
     * tooltip 用的翻译键，口径为 {@code skill.<namespace>.<path>}，
     * 并把 {@code great_}/{@code grand_} 等级前缀归一（等级由 tooltip 用罗马数字显示）。
     */
    @Override
    default String getTranslateKey() {
        ResourceLocation id = com.hjmmd_8.createoreexpansion.common.registry.coe.AllSkills.getId(this);
        if (id == null) {
            return "skill.createoreexpansion.unknown";
        }
        String path = id.getPath();
        if (path.startsWith("great_")) {
            path = path.substring("great_".length());
        } else if (path.startsWith("grand_")) {
            path = path.substring("grand_".length());
        }
        return "skill." + id.getNamespace() + "." + path;
    }
}
