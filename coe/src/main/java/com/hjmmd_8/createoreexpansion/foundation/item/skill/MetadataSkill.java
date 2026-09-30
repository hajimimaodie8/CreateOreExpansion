package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import net.minecraft.resources.ResourceLocation;

/**
 * 元数据技能壳 —— 只承载「技能 id + 类型」，不含任何执行或数值逻辑。
 *
 * <p><b>为什么需要它</b>：技能执行已整体迁到 Skiller 新内核
 * （{@code integration/skiller/skill/*ItemSkill}），旧自研实现类（{@code FellingSkill} 等）
 * 已全部删除。注册表（{@code AllSkills}）仍需为每条技能提供一个 {@link ItemSkill} 实例，
 * 用来回答「这条技能属于哪个触发族」与「它的翻译键是什么」——本类就是那个最小载体。</p>
 *
 * <p><b>数值从哪来</b>：能量消耗与冷却时间<b>由新内核从技能配置读取</b>
 * （{@code c -> c.energyCost} / {@code c -> c.cooldownSeconds}，
 * 见 {@code CoeSkillSupport}）。本类<b>不</b>持有这两个值——
 * 旧接口的 {@code getCost()}/{@code getCooldownSeconds()} 已在换核第 5 阶段退役。</p>
 */
public final class MetadataSkill implements ItemSkill {

    private final ResourceLocation id;
    private final SkillType type;

    public MetadataSkill(ResourceLocation id, SkillType type) {
        this.id = id;
        this.type = type;
    }

    /** 技能注册 id（{@code createoreexpansion:xxx}）。 */
    public ResourceLocation id() {
        return id;
    }

    @Override
    public SkillType getType() {
        return type;
    }

    /**
     * 翻译键口径与旧 {@link ItemSkill#getTranslateKey()} 逐字一致
     * （{@code skill.<namespace>.<path>}，并把 {@code great_}/{@code grand_} 前缀归一）。
     */
    @Override
    public String getTranslateKey() {
        String path = id.getPath();
        if (path.startsWith("great_")) {
            path = path.substring("great_".length());
        } else if (path.startsWith("grand_")) {
            path = path.substring("grand_".length());
        }
        return "skill." + id.getNamespace() + "." + path;
    }

    @Override
    public String toString() {
        return "MetadataSkill[" + id + "]";
    }
}
