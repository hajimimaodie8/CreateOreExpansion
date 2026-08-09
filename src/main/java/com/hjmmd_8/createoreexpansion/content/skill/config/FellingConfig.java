package com.hjmmd_8.createoreexpansion.content.skill.config;

import com.hjmmd_8.createoreexpansion.content.skill.FellingSkill;
import com.hjmmd_8.createoreexpansion.content.skill.strategy.FellingStrategy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.AutoSkillConfig;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;

public class FellingConfig extends AutoSkillConfig<FellingSkill, FellingStrategy> {

    public int searchRange;
    public int maxBlocks;
    public BlockPredicate predicate;
    public int energyCost;
    public float logResistance;
    public float leafResistance;

    public FellingConfig(int searchRange, int maxBlocks, BlockPredicate predicate,
                         int energyCost, float logResistance, float leafResistance) {
        this.searchRange = searchRange;
        this.maxBlocks = maxBlocks;
        this.predicate = predicate;
        this.energyCost = energyCost;
        this.logResistance = logResistance;
        this.leafResistance = leafResistance;
    }

    public FellingConfig(int searchRange, BlockPredicate predicate,
                         int energyCost, float logResistance, float leafResistance) {
        this.searchRange = searchRange;
        this.maxBlocks = searchRange * searchRange * searchRange + 1;
        this.predicate = predicate;
        this.energyCost = energyCost;
        this.logResistance = logResistance;
        this.leafResistance = leafResistance;
    }

    @Override
    protected List<FieldMapping> mappings() {
        return List.of(
                of("Range",  () -> searchRange,  v -> searchRange = v),
                of("MaxBlocks", () -> maxBlocks, v -> maxBlocks = v),
                of("Cost",      () -> energyCost, v -> energyCost = v),
                ofStr("Predicate", () -> predicate.name(), v -> predicate = BlockPredicate.valueOf(v)),
                nested("SpeedCorrection", List.of(
                        ofFloat("Log",  () -> logResistance,  v -> logResistance = v),
                        ofFloat("Leaf", () -> leafResistance, v -> leafResistance = v)
                ))
        );
    }

    @Override
    public void loadSkill(FellingSkill skill) {
        skill.load(this);
    }

    @Override
    public void loadStrategy(FellingStrategy strategy) {
        strategy.load(this);
    }

    public enum BlockPredicate implements Predicate<BlockState> {
        IS_LOG( state -> state.is(BlockTags.LOGS)),
        IS_TREE(state -> state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)),
        ;

        final Predicate<BlockState> predicate;
        BlockPredicate(Predicate<BlockState> predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean test(BlockState o) {
            return predicate.test(o);
        }
    }
}
