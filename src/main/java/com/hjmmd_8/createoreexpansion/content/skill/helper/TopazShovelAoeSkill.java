package com.hjmmd_8.createoreexpansion.content.skill.helper;

import com.hjmmd_8.createoreexpansion.content.equipment.tool.ToolEnergy;
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

/**
 * 黄玉铲 — 一列挖掘入口
 *
 * <p>技能逻辑：按住 Shift 时，从点击的方块面向外挖掘一列方块（最多 8 格），
 * 且只能挖掘 {@link BlockTags#MINEABLE_WITH_SHOVEL} 标签中的方块（泥土、沙子等）。
 * 碰见石头等非铲子方块时立即停止整列。</p>
 *
 * <hr>
 * <h3>新建铲子技能需修改的参数：</h3>
 * <table>
 *   <tr><th>修改项</th><th>说明</th></tr>
 *   <tr><td>MAX_DEPTH</td><td>最大挖掘深度（格数）</td></tr>
 *   <tr><td>AllItems.xxx</td><td>改成对应物品注册名</td></tr>
 *   <tr><td>MINEABLE_WITH_SHOVEL</td><td>可改为其他标签（如 MINEABLE_WITH_PICKAXE）</td></tr>
 * </table>
 */
public class TopazShovelAoeSkill implements ItemSkill {

    /** 最大挖掘深度（格数） */
    private static final int MAX_DEPTH = 8;

    /**
     * Mixin 入口。由 TopazShovelAoeMixin 在 destroyBlock() 时调用。
     * @param level      世界
     * @param pos        玩家直接点击的方块位置
     * @param state      方块状态
     * @param shovel     当前手持工具
     * @param livingEntity 挖掘者
     */
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

        // 计算一列方块位置（遇非铲子方块即停止）
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
            if (ToolEnergy.hasEnergy(shovel) && !ToolEnergy.consumeForSkill(player, shovel, ToolEnergy.SHOVEL_COST))
                return;
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
