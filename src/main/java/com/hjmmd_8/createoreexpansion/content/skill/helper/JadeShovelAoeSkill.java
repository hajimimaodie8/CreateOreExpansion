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

public class JadeShovelAoeSkill implements ItemSkill {

    private static final int MAX_DEPTH = 6;

    public static void causeAoe(Level level, BlockPos pos, BlockState state,
                                ItemStack shovel, LivingEntity livingEntity) {
        if (!(livingEntity instanceof ServerPlayer player)) return;
        if (level.isClientSide) return;
        if (!player.isCreative() && state.getDestroySpeed(level, pos) == 0.0F) return;
        if (!player.isShiftKeyDown()) return;

        HitResult pick = player.pick(20D, 0.0F, false);
        if (!(pick instanceof BlockHitResult blockHitResult)) return;
        Direction digDir;
        // 视角朝下（pitch>45）时不挖入地，改沿玩家偏航方向水平挖
        if (livingEntity.getXRot() > 45.0) {
            float yaw = livingEntity.getYRot();
            int idx = (int) Math.floor((((yaw % 360) + 360) % 360) / 90.0 + 0.5) % 4;
            Direction[] ds = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
            digDir = ds[idx];
        } else {
            digDir = blockHitResult.getDirection().getOpposite();
        }

        Set<BlockPos> targets = new HashSet<>();
        for (int i = 1; i <= MAX_DEPTH; i++) {
            BlockPos targetPos = pos.relative(digDir, i);
            BlockState targetState = level.getBlockState(targetPos);
            if (targetState.isAir() || !targetState.getFluidState().isEmpty()) break;
            if (level.getBlockEntity(targetPos) != null) break;
            if (!targetState.is(BlockTags.MINEABLE_WITH_SHOVEL)) break;
            targets.add(targetPos);
        }

        if (!targets.isEmpty()) {
            BlockBreaker.breakPositions(targets, pos, shovel, level, player);
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
