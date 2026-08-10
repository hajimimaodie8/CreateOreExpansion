package com.hjmmd_8.createoreexpansion.content.skill.config;

import com.hjmmd_8.createoreexpansion.content.skill.AreaAoeSkill;
import com.hjmmd_8.createoreexpansion.content.skill.strategy.AreaAoeStrategy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.AutoSkillConfig;
import com.hjmmd_8.createoreexpansion.foundation.util.DualDirection;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class AreaAoeConfig extends AutoSkillConfig<AreaAoeSkill, AreaAoeStrategy> {

    public int energyCost;
    public int width;
    public int height;
    public int depth;
    public DualDirection.From directionSource;
    public TagKey<Block> mineableTag;

    public AreaAoeConfig(int energyCost, TagKey<Block> mineableTag,
                         int width, int height, int depth, DualDirection.From directionSource) {
        this.energyCost = energyCost;
        this.mineableTag = mineableTag;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.directionSource = directionSource;
    }

    public AreaAoeConfig(int energyCost, TagKey<Block> mineableTag,
                         int width, int height, int depth) {
        this(energyCost, mineableTag, width, height, depth, DualDirection.From.BLOCK_FACE);
    }

    public AreaAoeConfig(int energyCost, int width, int height, int depth) {
        this(energyCost, BlockTags.MINEABLE_WITH_PICKAXE, width, height, depth, DualDirection.From.BLOCK_FACE);
    }

    public AreaAoeConfig(int energyCost, int size, int depth) {
        this(energyCost, BlockTags.MINEABLE_WITH_PICKAXE, size, size, depth, DualDirection.From.BLOCK_FACE);
    }

    @Override
    public void loadSkill(AreaAoeSkill skill) {
        skill.load(this);
    }

    @Override
    public void loadStrategy(AreaAoeStrategy strategy) {
        strategy.load(this);
    }

    @Override
    protected List<FieldMapping> mappings() {
        return List.of(
                of("Cost",   () -> energyCost, v -> energyCost = v),
                of("Width",  () -> width,      v -> width = v),
                of("Height", () -> height,     v -> height = v),
                of("Depth",  () -> depth,      v -> depth = v),
                ofStr("DirectionSource", () -> directionSource.name(),
                        v -> directionSource = DualDirection.From.valueOf(v)),
                ofStr("MineableTag",     () -> mineableTag.location().toString(),
                        v -> mineableTag = TagKey.create(Registries.BLOCK, ResourceLocation.parse(v)))
        );
    }
}