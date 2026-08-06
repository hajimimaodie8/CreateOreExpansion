package com.hjmmd_8.createoreexpansion.content.skill.helper;

import com.hjmmd_8.createoreexpansion.content.equipment.tool.ToolEnergy;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillType;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.context.impl.ExcavationSkillContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.tags.BlockTags;
import com.hjmmd_8.createoreexpansion.foundation.util.BlockBreaker;

/**
 * 蓝宝石镐 - 5x5 范围挖掘入口
 *
 * <p>按住 Shift 时，以玩家点击的方块为中心，向前方 5x5 平面范围破坏方块。</p>
 *
 * <hr>
 * <h3>新工具适配参数</h3>
 * <ul>
 *   <li>RADIUS — AOE 半径（5 = 5x5）</li>
 *   <li>DEPTH — AOE 纵深（1 = 单层）</li>
 * </ul>
 */
public class SapphirePickaxeAoeSkill implements ItemSkill {

    private static final int RADIUS = 5;
    private static final int DEPTH = 1;

    public static void causeAoe(Level level, BlockPos pos, BlockState state,
                                ItemStack pickaxe, LivingEntity livingEntity) {
        if (!(livingEntity instanceof ServerPlayer player)) return;
        if (level.isClientSide) return;

        // 生存模式跳过硬度为 0 的方块；创造模式允许破坏任何方块
        if (!player.isCreative() && state.getDestroySpeed(level, pos) == 0.0F) return;
        if (!player.isShiftKeyDown()) return;

        HitResult pick = player.pick(20D, 0.0F, false);
        if (!(pick instanceof BlockHitResult blockHitResult)) return;

        if (ToolEnergy.hasEnergy(pickaxe) && !ToolEnergy.consumeForSkill(player, pickaxe, ToolEnergy.PICKAXE_COST))
            return;
                BlockBreaker.breakBlocks(blockHitResult, pos, pickaxe, level, player, RADIUS, DEPTH, BlockTags.MINEABLE_WITH_PICKAXE);
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
