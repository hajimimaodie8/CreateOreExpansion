package com.hjmmd_8.createoreexpansion.content.skill.config;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.AutoSkillConfig;

import java.util.List;

public class SkinConfig extends AutoSkillConfig {

    public float dropChance;
    public int energyCost;

    public SkinConfig(float dropChance, int energyCost) {
        this.dropChance = dropChance;
        this.energyCost = energyCost;
    }

    @Override
    protected List<FieldMapping> mappings() {
        return List.of(
                ofFloat("DropChance", () -> dropChance, value -> dropChance = value),
                of("Cost", () -> energyCost, value -> energyCost = value)
        );
    }
}
