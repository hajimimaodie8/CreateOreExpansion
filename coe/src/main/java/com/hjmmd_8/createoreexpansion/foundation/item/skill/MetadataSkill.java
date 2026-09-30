package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import net.minecraft.resources.ResourceLocation;

/**
 * 元数据技能壳 —— 只承载「技能 id + 类型 + 展示用数值」，不含任何执行逻辑。
 *
 * <p><b>为什么需要它</b>：技能执行已整体迁到 Skiller 新内核
 * （{@code integration/skiller/skill/*ItemSkill}），旧自研实现类（{@code FellingSkill} 等）
 * 只剩「物品技能数据组件的载体」这一个用途。为了在不改动
 * {@code createoreexpansion:skills} 数据组件格式（老存档红线）的前提下删掉那些实现类，
 * 注册表改为用本类做占位。</p>
 *
 * <p><b>数值口径</b>：能量消耗与冷却时间<b>由新内核从技能配置读取</b>
 * （见新内核的 {@code c -> c.energyCost} / {@code c -> c.cooldownSeconds} 与
 * {@code CoeSkillSupport}），{@link #getCost()} / {@link #getCooldownSeconds()}
 * 只保留给仍在的旧接口签名，不参与生效路径；默认值取旧实现口径（100 / 0=用全局冷却表）。</p>
 */
public final class MetadataSkill implements ItemSkill {

    /** 兜底能量消耗（与 {@link ItemSkill#getCost()} 的默认值一致）。 */
    public static final int DEFAULT_COST = 100;

    private final ResourceLocation id;
    private final SkillType type;
    private int cost = DEFAULT_COST;
    private int cooldownSeconds = 0;

    public MetadataSkill(ResourceLocation id, SkillType type) {
        this.id = id;
        this.type = type;
    }

    /** 技能注册 id（{@code createoreexpansion:xxx}）。 */
    public ResourceLocation id() {
        return id;
    }

    /** 注册期可选写入真实数值（配置里能取到消耗/冷却时）。 */
    public void applyMetadata(int cost, int cooldownSeconds) {
        this.cost = cost;
        this.cooldownSeconds = cooldownSeconds;
    }

    @Override
    public SkillType getType() {
        return type;
    }

    @Override
    public int getCost() {
        return cost;
    }

    @Override
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    /**
     * 执行入口已废弃：技能执行一律走新内核。
     *
     * <p>保留空实现只为不破坏 {@link ItemSkill} 的接口签名；旧调用点（若还有）
     * 会被 {@code SkillMigrationGate} 拦住，不会走到这里。</p>
     */
    @Override
    public void release(Object context, DataSkill data) {
        // 有意为空 —— 执行权已移交新内核（Skiller）。
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
