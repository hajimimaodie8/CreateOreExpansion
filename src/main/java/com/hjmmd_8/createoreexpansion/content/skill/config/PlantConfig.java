package com.hjmmd_8.createoreexpansion.content.skill.config;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.AutoSkillConfig;

import java.util.List;

public class PlantConfig extends AutoSkillConfig {

    public static final int DEFAULT_RANGE_3X3 = 1;
    public static final int DEFAULT_RANGE_3X5 = 2;
    public static final int DEFAULT_RANGE_5X5 = 2;
    
    public static final int DEFAULT_MAX_BLOCKS_3X3 = 9;
    public static final int DEFAULT_MAX_BLOCKS_3X5 = 15;
    public static final int DEFAULT_MAX_BLOCKS_5X5 = 25;

    public int energyCost;
    public int searchRange;
    public int maxBlocks;

    public PlantConfig(int energyCost, int searchRange, int maxBlocks) {
        this.energyCost = energyCost;
        this.searchRange = searchRange;
        this.maxBlocks = maxBlocks;
    }

    @Override
    protected List<FieldMapping> mappings() {
        return List.of(
                ofInt("Cost", () -> energyCost, value -> energyCost = value),
                ofInt("SearchRange", () -> searchRange, value -> searchRange = value),
                ofInt("MaxBlocks", () -> maxBlocks, v -> maxBlocks = v)
        );
    }
}