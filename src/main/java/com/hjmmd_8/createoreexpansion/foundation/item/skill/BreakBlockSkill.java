package com.hjmmd_8.createoreexpansion.foundation.item.skill;

import com.hjmmd_8.createoreexpansion.content.skill.AbstractStrategySkill;
import com.hjmmd_8.createoreexpansion.foundation.util.AreaStrategy;
import com.hjmmd_8.createoreexpansion.foundation.util.BlockBreaker;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public abstract class BreakBlockSkill<T extends AreaStrategy, C> extends AbstractStrategySkill<T> implements TypedItemSkill<C>{

    public final TagKey<Block> mineableTag;

    protected BreakBlockSkill(T strategy, TagKey<Block> mineableTag) {
        super(strategy);
        this.mineableTag = mineableTag;
    }

    protected void breakBlocks(Set<BlockPos> positions, BlockPos center,
                          ItemStack tool, Level level, LivingEntity entity,
                          @Nullable TagKey<Block> mineableTag) {
        BlockBreaker.breakPositions(positions, center, tool, level, entity, mineableTag);
    }
}
