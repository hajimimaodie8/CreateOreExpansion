package com.hjmmd_8.createoreexpansion.content.skill.config;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.AutoSkillConfig;

import java.util.List;

public class SkinConfig extends AutoSkillConfig {

    /** 本次技能是否掉落的概率（0~1） */
    public float dropChance;
    public int energyCost;
    /** 技能冷却秒数（创造模式无冷却） */
    public int cooldownSeconds;
    /** 掉落数量分布权重（掉 0 个 / 1 个 / 2 个 / 3 个，总和随意，按比例取） */
    public int drop0Weight;
    public int drop1Weight;
    public int drop2Weight;
    public int drop3Weight;

    public SkinConfig(float dropChance, int energyCost, int cooldownSeconds,
                      int drop0Weight, int drop1Weight, int drop2Weight, int drop3Weight) {
        this.dropChance = dropChance;
        this.energyCost = energyCost;
        this.cooldownSeconds = cooldownSeconds;
        this.drop0Weight = drop0Weight;
        this.drop1Weight = drop1Weight;
        this.drop2Weight = drop2Weight;
        this.drop3Weight = drop3Weight;
    }

    /** 兼容旧用法：固定掉 1 个，默认冷却 1 秒 */
    public SkinConfig(float dropChance, int energyCost) {
        this(dropChance, energyCost, 1, 0, 100, 0, 0);
    }

    @Override
    protected List<FieldMapping> mappings() {
        return List.of(
                ofFloat("DropChance", () -> dropChance, value -> dropChance = value),
                ofInt("Cost", () -> energyCost, value -> energyCost = value),
                ofInt("Cooldown", () -> cooldownSeconds, value -> cooldownSeconds = value),
                ofInt("Drop0", () -> drop0Weight, value -> drop0Weight = value),
                ofInt("Drop1", () -> drop1Weight, value -> drop1Weight = value),
                ofInt("Drop2", () -> drop2Weight, value -> drop2Weight = value),
                ofInt("Drop3", () -> drop3Weight, value -> drop3Weight = value)
        );
    }
}
