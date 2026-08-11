package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.content.skill.config.ReapConfig;
import com.hjmmd_8.createoreexpansion.content.skill.context.UseOnBlockContext;
import com.hjmmd_8.createoreexpansion.content.skill.strategy.ReapStrategy;
import com.hjmmd_8.createoreexpansion.foundation.util.params.FrameParams;
import com.hjmmd_8.createoreexpansion.foundation.util.params.ParamsPool;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ConfigSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.UseItemContext;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

public class ReapSkill extends AbstractStrategySkill<BlockPos, ReapStrategy>
    implements ConfigSkill<UseItemContext<?>, ReapConfig> {

    DataSkill data;
    ReapConfig config;

    public ReapSkill(ReapStrategy strategy) {
        super(strategy);
    }

    @Override
    public void load(ReapConfig config, DataSkill data) {
        this.data = data;
        this.config = config;
    }

    @Override
    public Class<ReapConfig> getConfigType() {
        return ReapConfig.class;
    }

    @Override
    public void release(UseItemContext<?> context) {
        if (context instanceof UseOnBlockContext blockContext) {
            BlockPos center = blockContext.event().getPos();
            Player player = blockContext.event().getPlayer();
            if (player == null) return;
            Level level = player.level();
            if (level.isClientSide()) return;
            BlockState centerState = level.getBlockState(center);
            RandomSource source = level.getRandom();

            FrameParams params = ParamsPool.DEFAULT_POOL.borrow()
                    .put("Center", center)
                    .put("CenterState", centerState)
                    .put("Player", player);

            Set<BlockPos> positions = calculate(data, params);

            for (BlockPos pos : positions) {
                BlockState state = level.getBlockState(pos);
                int stage = config.rollStage(source);
                int nowStage = Math.min(7,
                        state.getValue(CropBlock.AGE) + stage);

                BlockState newState = state.setValue(CropBlock.AGE, nowStage);
                level.setBlock(pos, newState, 3);
            }

            ParamsPool.DEFAULT_POOL.returnParams(params);
        }
    }

    @Override
    public SkillType getType() {
        return SkillType.USE_SKILL;
    }
}
