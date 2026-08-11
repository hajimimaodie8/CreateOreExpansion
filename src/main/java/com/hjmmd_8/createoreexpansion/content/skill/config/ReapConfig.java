package com.hjmmd_8.createoreexpansion.content.skill.config;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.AutoSkillConfig;
import net.minecraft.util.RandomSource;

import java.util.List;

public class ReapConfig extends AutoSkillConfig {

    public boolean shouldHarvest;
    public int searchRange;
    public int maxBlocks;
    public int minStage;
    public int maxStage;
    public int energyCost;

    public ReapConfig(int energyCost, boolean shouldHarvest, int searchRange, int maxBlocks, int minStage, int maxStage) {
        this.energyCost = energyCost;
        this.searchRange = searchRange;
        this.maxBlocks = maxBlocks;
        this.minStage = minStage;
        this.maxStage = maxStage;
        this.shouldHarvest = shouldHarvest;
    }

    @Override
    protected List<FieldMapping> mappings() {
        return List.of(
                ofBool("ShouldHarvest", () -> shouldHarvest, value -> shouldHarvest = value),
                ofInt("SearchRange", () -> searchRange, value -> searchRange = value),
                ofInt("MaxBlocks", () -> maxBlocks, v -> maxBlocks = v),
                ofInt("MinStage", () -> minStage, value -> minStage = value),
                ofInt("MaxStage", () -> maxStage, value -> maxStage = value),
                ofInt("Cost", () -> energyCost, value -> energyCost = value)
        );
    }

    public int rollStage(RandomSource source) {
        return source.nextIntBetweenInclusive(minStage, maxStage);
    }
}
