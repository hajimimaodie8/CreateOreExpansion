package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.content.skill.config.ReapConfig;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.content.skill.context.UseOnBlockContext;
import com.hjmmd_8.createoreexpansion.content.skill.strategy.ReapStrategy;
import com.hjmmd_8.createoreexpansion.foundation.util.params.FrameParams;
import com.hjmmd_8.createoreexpansion.foundation.util.params.ParamsPool;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ConfigSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.UseItemContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Set;

public class ReapSkill extends AbstractStrategySkill<BlockPos, ReapStrategy>
    implements ConfigSkill<UseItemContext<?>, ReapConfig> {

    private DataSkill data;
    private ReapConfig config;
    private int energyCost;

    public ReapSkill(ReapStrategy strategy) {
        super(strategy);
    }

    @Override
    public void load(ReapConfig config, DataSkill data) {
        this.data = data;
        this.config = config;
        this.energyCost = config.energyCost;
    }

    @Override
    public Class<ReapConfig> getConfigType() {
        return ReapConfig.class;
    }

    @Override
    public boolean canRelease(Object context, DataSkill data) {
        if (!(context instanceof UseOnBlockContext blockContext)) return false;

        BlockPos center = blockContext.event().getPos();
        Player player = blockContext.event().getPlayer();
        if (player == null) return false;

        Level level = player.level();
        if (level.isClientSide()) return false;

        BlockState centerState = level.getBlockState(center);
        if (!(centerState.getBlock() instanceof CropBlock crop)) return false;

        // 仅在作物成熟时才允许释放技能
        return crop.isMaxAge(centerState);
    }

    @Override
    public void release(UseItemContext<?> context) {
        if (!(context instanceof UseOnBlockContext blockContext)) return;

        BlockPos center = blockContext.event().getPos();
        Player player = blockContext.event().getPlayer();
        if (player == null) return;

        Level level = player.level();
        if (level.isClientSide()) return;

        BlockState centerState = level.getBlockState(center);
        if (!(centerState.getBlock() instanceof CropBlock)) return;

        RandomSource random = level.getRandom();

        // 检查能量
        ItemStack hoe = player.getMainHandItem();
        if (energyCost != 0 && ToolEnergy.hasEnergy(hoe) && !ToolEnergy.consumeForSkill(hoe, this)) {
            ToolEnergy.sendLowEnergy(player, hoe);
            return;
        }

        FrameParams params = ParamsPool.DEFAULT_POOL.borrow()
                .put("Center", center)
                .put("CenterState", centerState)
                .put("Player", player);

        Set<BlockPos> positions = calculate(data, params);

        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof CropBlock crop)) continue;

            if (config.shouldHarvest && crop.isMaxAge(state)) {
                // 收割成熟作物
                harvestAndReplant(level, pos, state, crop, player, random);
            }
        }

        ParamsPool.DEFAULT_POOL.returnParams(params);
    }

    private void harvestAndReplant(Level level, BlockPos pos, BlockState state, 
                                    CropBlock crop, Player player, RandomSource random) {
        // 获取掉落物
        List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, pos, null, player, player.getMainHandItem());
        
        // 生成掉落物
        for (ItemStack drop : drops) {
            ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
        }

        // 重新种植（设为0阶段）
        BlockState newState = state.setValue(CropBlock.AGE, 0);
        
        // 根据配置概率催熟
        if (random.nextFloat() < config.matureChance) {
            int stage = config.rollStage(random);
            int finalStage = Math.min(crop.getMaxAge(), stage);
            newState = state.setValue(CropBlock.AGE, finalStage);
        }
        
        level.setBlock(pos, newState, 3);
    }

    @Override
    public int getCost() {
        return energyCost;
    }

    @Override
    public SkillType getType() {
        return SkillType.USE_SKILL;
    }
}
