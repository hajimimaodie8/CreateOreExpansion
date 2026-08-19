package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.common.AllModifiableAttributes;
import com.hjmmd_8.createoreexpansion.content.skill.attribute.BreakBlockSpeedModifiableAttribute;
import com.hjmmd_8.createoreexpansion.content.skill.config.FellingConfig;
import com.hjmmd_8.createoreexpansion.content.skill.config.FellingConfigs;
import com.hjmmd_8.createoreexpansion.content.skill.strategy.FellingStrategy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.util.BlockSearch;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 砍伐技能 - 斧头连锁砍树。
 *
 * 破坏位置由 {@link FellingStrategy} 计算，与渲染预览共用同一实现；
 * 砍伐同一棵树时挖掘速度会按树木规模衰减（见 {@link #load} 中注册的修饰器）。
 * 冷却时长与速度衰减因子均来自配置（见 {@link FellingConfigs}）。
 */
public class FellingSkill extends AoeExcavationSkill<FellingConfig, FellingStrategy> {

    /** 速度衰减下限：满上限大树时砍伐速度约等于钻石镐挖黑曜石（≈0.037 倍） */
    private static final float MIN_SPEED_MULTIPLIER = 0.02F;

    private int energyCost;
    private float logResistance;
    private float leafResistance;
    private FellingConfig.BlockPredicate predicate;
    private int searchRange;
    private int maxBlocks;
    private int cooldownSeconds;

    public FellingSkill(FellingStrategy strategy) {
        super(strategy);
    }

    @Override
    protected boolean isTargetBlock(BlockState state) {
        return state.is(BlockTags.LOGS);
    }

    @Override
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    @Override
    public void load(FellingConfig config, DataSkill data) {
        this.energyCost = config.energyCost;
        this.logResistance = config.logResistance;
        this.leafResistance = config.leafResistance;
        this.predicate = config.predicate;
        this.searchRange = config.searchRange;
        this.maxBlocks = config.maxBlocks;
        this.cooldownSeconds = config.cooldownSeconds;
        this.data = data;

        clearModifier();
        addModifier(AllModifiableAttributes.BREAK_BLOCK_SPEED, attribute -> {
            if (!(attribute instanceof BreakBlockSpeedModifiableAttribute speedAttribute)) return;

            Level level = speedAttribute.getLevel();
            BlockPos pos = speedAttribute.getPos();

            // 统计树木规模，树越大砍伐越慢
            BlockSearch.TreeStats tree = BlockSearch.countTree(level, pos, maxBlocks, searchRange,
                    predicate == FellingConfig.BlockPredicate.IS_TREE);

            int treeSize = tree.total();
            if (treeSize == 0) return;
            float multiplier = Math.max(MIN_SPEED_MULTIPLIER, 1f / (1f + tree.logs() * logResistance +
                    ((predicate == FellingConfig.BlockPredicate.IS_TREE)
                            ? tree.leaves() * leafResistance
                            : 0)));
            speedAttribute.setValue(speedAttribute.getValue() * multiplier);
        });
    }

    @Override
    public SkillType getType() {
        return SkillType.EXCAVATION_SKILL;
    }

    @Override
    public int getCost() {
        return energyCost;
    }

    @Override
    public Class<FellingConfig> getConfigType() {
        return FellingConfig.class;
    }
}
