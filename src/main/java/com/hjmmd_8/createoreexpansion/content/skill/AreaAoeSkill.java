package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.impl.ExcavationSkillContext;
import com.hjmmd_8.createoreexpansion.foundation.util.BlockBreaker;
import com.hjmmd_8.createoreexpansion.foundation.util.DualDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AreaAoeSkill implements ItemSkill {

    private final int energyCost;
    private final int width;
    private final int height;
    private final int depth;
    private final TagKey<Block> mineableTag;

    public AreaAoeSkill(int width, int height, int depth, int energyCost, TagKey<Block> mineableTag) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.energyCost = energyCost;
        this.mineableTag = mineableTag;
    }

    public AreaAoeSkill(int width, int height, int depth, int energyCost) {
        this(width, height, depth, energyCost, BlockTags.MINEABLE_WITH_PICKAXE);
    }

    public void causeAoe(Level level, BlockPos pos, BlockState state,
                         ItemStack pickaxe, LivingEntity livingEntity) {
        if (!(livingEntity instanceof ServerPlayer player)) return;
        if (level.isClientSide) return;

        // 生存模式跳过硬度为 0 的方块；创造模式允许破坏任何方块
        if (!player.isCreative() && state.getDestroySpeed(level, pos) == 0.0F) return;
        if (!player.isShiftKeyDown()) return;

        HitResult pick = player.pick(20D, 0.0F, false);
        if (!(pick instanceof BlockHitResult blockHitResult)) return;

        DualDirection direction = DualDirection.of(player, blockHitResult);

        if (energyCost != 0 && ToolEnergy.hasEnergy(pickaxe) && !ToolEnergy.consumeForSkill(player, pickaxe, this))
            return;
        BlockBreaker.breakBlocks(direction, pos, pickaxe, level, player, width, height, depth, mineableTag);
    }

    @Override
    public void release(Object context) {
        if (context instanceof ExcavationSkillContext ctx) {
            causeAoe(ctx.level(), ctx.pos(), ctx.level().getBlockState(ctx.pos()), ctx.tool(), ctx.entity());
        }
    }

    @Override
    public SkillType getType() {
        return SkillType.EXCAVATION_SKILL;
    }

    @Override
    public int getCost() {
        return energyCost;
    }
}
