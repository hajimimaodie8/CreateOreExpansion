package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.content.skill.config.AreaAoeConfig;
import com.hjmmd_8.createoreexpansion.content.skill.strategy.AreaAoeStrategy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * 范围AOE技能 - 镐子/铲子的范围挖掘（开岩、引渠、平场）。
 *
 * <p>破坏位置由 {@link AreaAoeStrategy} 计算，与渲染预览共用同一实现。</p>
 */
public class AreaAoeSkill extends AoeExcavationSkill<AreaAoeConfig, AreaAoeStrategy> {

    private int energyCost;
    private TagKey<Block> mineableTag;

    public AreaAoeSkill(AreaAoeStrategy strategy) {
        super(strategy);
        this.energyCost = 0;
        this.mineableTag = BlockTags.MINEABLE_WITH_PICKAXE;
    }

    @Override
    protected TagKey<Block> getMineableTag() {
        return mineableTag;
    }

    @Override
    public void load(AreaAoeConfig config, DataSkill data) {
        this.energyCost = config.energyCost;
        this.mineableTag = config.mineableTag;
        this.data = data;
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
    public Class<AreaAoeConfig> getConfigType() {
        return AreaAoeConfig.class;
    }
}
