package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.content.skill.config.PlantConfig;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.content.skill.context.UseOnBlockContext;
import com.hjmmd_8.createoreexpansion.content.skill.strategy.PlantStrategy;
import com.hjmmd_8.createoreexpansion.foundation.util.params.FrameParams;
import com.hjmmd_8.createoreexpansion.foundation.util.params.ParamsPool;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ConfigSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.UseItemContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class PlantSkill extends AbstractStrategySkill<BlockPos, PlantStrategy>
    implements ConfigSkill<UseItemContext<?>, PlantConfig> {

    private static final ThreadLocal<Boolean> IS_PLANTING = ThreadLocal.withInitial(() -> false);

    private DataSkill data;
    private PlantConfig config;
    private int energyCost;

    public PlantSkill(PlantStrategy strategy) {
        super(strategy);
    }

    @Override
    public void load(PlantConfig config, DataSkill data) {
        this.data = data;
        this.config = config;
        this.energyCost = config.energyCost;
    }

    @Override
    public Class<PlantConfig> getConfigType() {
        return PlantConfig.class;
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
        if (!centerState.is(Blocks.FARMLAND)) return false;

        // 检查玩家是否持有种子（通过检查主手物品是否能在耕地上使用）
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (heldItem.isEmpty()) return false;

        // 简单检查：耕地上方是否为空气（至少有一个可种植位置）
        BlockPos abovePos = center.above();
        return level.getBlockState(abovePos).isAir();
    }

    @Override
    public void release(UseItemContext<?> context) {
        if (IS_PLANTING.get()) return;
        
        if (!(context instanceof UseOnBlockContext blockContext)) return;

        BlockPos center = blockContext.event().getPos();
        Player player = blockContext.event().getPlayer();
        if (player == null) return;

        Level level = player.level();
        if (level.isClientSide()) return;

        BlockState centerState = level.getBlockState(center);
        if (!centerState.is(Blocks.FARMLAND)) return;

        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (heldItem.isEmpty()) return;

        // 检查能量
        ItemStack hoe = player.getMainHandItem();
        if (energyCost != 0 && ToolEnergy.hasEnergy(hoe) && !ToolEnergy.consumeForSkill(hoe, this)) {
            ToolEnergy.sendLowEnergy(player, hoe);
            return;
        }

        IS_PLANTING.set(true);
        try {
        FrameParams params = ParamsPool.DEFAULT_POOL.borrow()
                .put("Center", center)
                .put("CenterState", centerState)
                .put("Player", player);

        Set<BlockPos> positions = calculate(data, params);
        int plantedCount = 0;

        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.FARMLAND)) continue;

            BlockPos abovePos = pos.above();
            BlockState aboveState = level.getBlockState(abovePos);
            
            if (!aboveState.isAir()) continue;

            if (tryPlantSeed(level, pos, abovePos, heldItem, player)) {
                plantedCount++;
                
                if (heldItem.isEmpty()) break;
            }
        }

        ParamsPool.DEFAULT_POOL.returnParams(params);
        } finally {
            IS_PLANTING.set(false);
        }
    }

    private boolean tryPlantSeed(Level level, BlockPos farmlandPos, BlockPos plantPos, 
                                  ItemStack seedItem, Player player) {
        Vec3 hitVec = new Vec3(plantPos.getX() + 0.5, plantPos.getY(), plantPos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, 
                net.minecraft.core.Direction.UP, farmlandPos, false);
        
        UseOnContext useContext = new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult);
        
        InteractionResult result = seedItem.useOn(useContext);
        
        return result == InteractionResult.SUCCESS || result == InteractionResult.CONSUME;
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
