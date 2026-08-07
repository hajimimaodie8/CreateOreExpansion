package com.hjmmd_8.createoreexpansion.content.skill;

import com.hjmmd_8.createoreexpansion.common.AllKeys;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.AbstractSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.TypedItemSkill;
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

/**
 * 范围AOE技能 - 统一的挖掘技能实现
 * 支持：镐子（3x3/5x5平面）、铲子（深度挖掘/范围挖掘）
 */
public class AreaAoeSkill extends AbstractSkill implements TypedItemSkill<ExcavationSkillContext> {

    private final int energyCost;
    private final int width;
    private final int height;
    private final int depth;
    private final TagKey<Block> mineableTag;
    private final DualDirection.From from;

    public AreaAoeSkill(int width, int height, int depth, int energyCost, TagKey<Block> mineableTag, DualDirection.From from) {
        super();
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.energyCost = energyCost;
        this.mineableTag = mineableTag;
        this.from = from;
    }

    public AreaAoeSkill(int width, int height, int depth, int energyCost, TagKey<Block> mineableTag) {
        this(width, height, depth, energyCost, mineableTag, DualDirection.fromBlockFace());
    }

    public AreaAoeSkill(int width, int height, int depth, int energyCost, DualDirection.From from) {
        this(width, height, depth, energyCost, BlockTags.MINEABLE_WITH_PICKAXE, from);
    }

    public AreaAoeSkill(int width, int height, int depth, int energyCost) {
        this(width, height, depth, energyCost, BlockTags.MINEABLE_WITH_PICKAXE, DualDirection.fromBlockFace());
    }

    protected void causeAoe(Level level, BlockPos pos, BlockState state,
                         ItemStack pickaxe, LivingEntity livingEntity) {
        if (!(livingEntity instanceof ServerPlayer player)) return;
        if (level.isClientSide) return;

        // 生存模式跳过硬度为 0 的方块；创造模式允许破坏任何方块
        if (!player.isCreative() && state.getDestroySpeed(level, pos) == 0.0F) return;
        if (!AllKeys.SKILL_RELEASE.isPressed()) return;

        HitResult pick = player.pick(20D, 0.0F, false);
        if (!(pick instanceof BlockHitResult hit)) return;

        DualDirection direction = DualDirection.from(player, hit, from);

        if (energyCost != 0 && ToolEnergy.hasEnergy(pickaxe) && !ToolEnergy.consumeForSkill(player, pickaxe, this))
            return;
        BlockBreaker.breakBlocks(direction, pos, pickaxe, level, player, width, height, depth, mineableTag);
    }

    @Override
    public void releaseTyped(ExcavationSkillContext ctx) {
        causeAoe(ctx.level(), ctx.pos(), ctx.level().getBlockState(ctx.pos()), ctx.tool(), ctx.entity());
    }

    @Override
    @Deprecated
    public void release(Object context) {
        // 通过 TypedItemSkill 实现，此方法保留用于向后兼容
        TypedItemSkill.super.release(context);
    }

    @Override
    public SkillType getType() {
        return SkillType.EXCAVATION_SKILL;
    }

    @Override
    public int getCost() {
        return energyCost;
    }

    // Getters for potential modifier usage
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getDepth() { return depth; }
}
