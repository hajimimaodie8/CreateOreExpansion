package com.hjmmd_8.createoreexpansion.content.skill.config;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.AutoSkillConfig;
import net.minecraft.util.RandomSource;

import java.util.List;

public class ReapConfig extends AutoSkillConfig {

    public static final int DEFAULT_RANGE_3X3 = 1;
    public static final int DEFAULT_RANGE_3X5 = 2;
    public static final int DEFAULT_RANGE_5X5 = 2;
    
    public static final int DEFAULT_MAX_BLOCKS_3X3 = 9;
    public static final int DEFAULT_MAX_BLOCKS_3X5 = 15;
    public static final int DEFAULT_MAX_BLOCKS_5X5 = 25;
    
    public static final float JADE_MATURE_CHANCE = 0.15F;
    public static final float TOPAZ_MATURE_CHANCE = 0.30F;
    public static final float SAPPHIRE_MATURE_CHANCE = 0.50F;

    public int energyCost;
    public boolean shouldHarvest;
    public int searchRange;
    public int maxBlocks;
    public int minStage;
    public int maxStage;
    public float matureChance;

    public ReapConfig(int energyCost, boolean shouldHarvest, int searchRange, 
                      int maxBlocks, int minStage, int maxStage, float matureChance) {
        this.energyCost = energyCost;
        this.shouldHarvest = shouldHarvest;
        this.searchRange = searchRange;
        this.maxBlocks = maxBlocks;
        this.minStage = minStage;
        this.maxStage = maxStage;
        this.matureChance = matureChance;
    }

    @Override
    protected List<FieldMapping> mappings() {
        return List.of(
                ofInt("Cost", () -> energyCost, value -> energyCost = value),
                ofBool("ShouldHarvest", () -> shouldHarvest, value -> shouldHarvest = value),
                ofInt("SearchRange", () -> searchRange, value -> searchRange = value),
                ofInt("MaxBlocks", () -> maxBlocks, v -> maxBlocks = v),
                ofInt("MinStage", () -> minStage, value -> minStage = value),
                ofInt("MaxStage", () -> maxStage, value -> maxStage = value),
                ofFloat("MatureChance", () -> matureChance, value -> matureChance = value)
        );
    }

    public int rollStage(RandomSource source) {
        return source.nextIntBetweenInclusive(minStage, maxStage);
    }
}
