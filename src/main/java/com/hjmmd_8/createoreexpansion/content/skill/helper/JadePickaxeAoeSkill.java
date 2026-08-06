package com.hjmmd_8.createoreexpansion.content.skill.helper;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.impl.ExcavationSkillContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import com.hjmmd_8.createoreexpansion.foundation.util.BlockBreaker;

import java.util.HashSet;
import java.util.Set;

public class JadePickaxeAoeSkill implements ItemSkill {

    public static void causeAoe(Level level, BlockPos pos, BlockState state,
                                ItemStack pickaxe, LivingEntity livingEntity) {
        if (!(livingEntity instanceof ServerPlayer player)) return;
        if (level.isClientSide) return;
        if (!player.isCreative() && state.getDestroySpeed(level, pos) == 0.0F) return;
        if (!player.isShiftKeyDown()) return;

        HitResult pick = player.pick(20D, 0.0F, false);
        if (!(pick instanceof BlockHitResult)) return;

        // 将玩家偏航角映射为朝向方向：0=South, 90=West, 180=North, -90=East
        float yaw = player.getYRot();
        int dirIndex = (int) Math.floor((((yaw % 360) + 360) % 360) / 90.0 + 0.5) % 4;
        Direction[] dirs = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
        Direction facing = dirs[dirIndex];
        Direction left = facing.getCounterClockWise();  // 玩家左手侧
        Direction right = facing.getClockWise();         // 玩家右手侧

        Set<BlockPos> targets = new HashSet<>();
        if (level.getBlockState(pos.relative(left)).is(BlockTags.MINEABLE_WITH_PICKAXE)) targets.add(pos.relative(left));
            if (level.getBlockState(pos.relative(right)).is(BlockTags.MINEABLE_WITH_PICKAXE)) targets.add(pos.relative(right));

        if (!targets.isEmpty()) {
            BlockBreaker.breakPositions(targets, pos, pickaxe, level, player);
        }
    }

    @Override
    public void release(Object context) {
        if (!SkillType.EXCAVATION_SKILL.cast(context))
            throw new ClassCastException("Expected ExcavationSkillContext");
        if (context instanceof ExcavationSkillContext ctx) {
            causeAoe(ctx.level(), ctx.pos(), ctx.level().getBlockState(ctx.pos()), ctx.tool(), ctx.entity());
        }
    }

    @Override
    public SkillType getType() {
        return SkillType.EXCAVATION_SKILL;
    }
}
