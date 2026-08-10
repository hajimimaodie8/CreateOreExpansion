package com.hjmmd_8.createoreexpansion.content.skill.config;

import com.hjmmd_8.createoreexpansion.content.skill.SkinSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.AutoSkillConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy.SkillStrategy;

import java.util.List;

public class SkinConfig extends AutoSkillConfig<SkinSkill, SkillStrategy<?>> {

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

    @Override
    public void loadSkill(SkinSkill skill) {
        skill.load(this);
    }

    @Override
    public void loadStrategy(SkillStrategy<?> strategy) {}
}
