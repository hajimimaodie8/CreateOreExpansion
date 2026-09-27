package com.hjmmd_8.createoreexpansion.content.skill.config;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.AutoSkillConfig;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;

public class FellingConfig extends AutoSkillConfig {

    public int searchRange;
    public int maxBlocks;
    public BlockPredicate predicate;
    public int energyCost;
    public float logResistance;
    public float leafResistance;
    /** 砍伐后冷却秒数（创造模式无冷却） */
    public int cooldownSeconds;

    public FellingConfig(int searchRange, int maxBlocks, BlockPredicate predicate,
                         int energyCost, float logResistance, float leafResistance, int cooldownSeconds) {
        this.searchRange = searchRange;
        this.maxBlocks = maxBlocks;
        this.predicate = predicate;
        this.energyCost = energyCost;
        this.logResistance = logResistance;
        this.leafResistance = leafResistance;
        this.cooldownSeconds = cooldownSeconds;
    }

    public FellingConfig(int searchRange, int maxBlocks, BlockPredicate predicate,
                         int energyCost, float logResistance, float leafResistance) {
        this(searchRange, maxBlocks, predicate, energyCost, logResistance, leafResistance, 3);
    }

    public FellingConfig(int searchRange, BlockPredicate predicate,
                         int energyCost, float logResistance, float leafResistance) {
        this(searchRange, searchRange * searchRange * searchRange + 1, predicate,
                energyCost, logResistance, leafResistance, 3);
    }

    @Override
    protected List<FieldMapping> mappings() {
        return List.of(
                ofInt("Range",  () -> searchRange, v -> searchRange = v),
                ofInt("MaxBlocks", () -> maxBlocks, v -> maxBlocks = v),
                ofInt("Cost",      () -> energyCost, v -> energyCost = v),
                ofInt("Cooldown",  () -> cooldownSeconds, v -> cooldownSeconds = v),
                ofStr("Predicate", () -> predicate.name(), v -> predicate = BlockPredicate.valueOf(v)),
                nested("SpeedCorrection", List.of(
                        ofFloat("Log",  () -> logResistance,  v -> logResistance = v),
                        ofFloat("Leaf", () -> leafResistance, v -> leafResistance = v)
                ))
        );
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
