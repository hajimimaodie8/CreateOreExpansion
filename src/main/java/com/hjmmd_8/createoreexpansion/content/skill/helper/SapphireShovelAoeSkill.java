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
 * 蓝宝石铲 - 6x6 范围铲土入口（同镐子模式）
 *
 * <p>按住 Shift 时，以玩家点击的方块为中心，在 6x6 平面范围内铲除
 * 只允许挖掘 {@link BlockTags#MINEABLE_WITH_SHOVEL} 标签中的方块。
 * 视角方向由光线追踪结果决定，和镐子逻辑一致。</p>
 *
 * <hr>
 * <h3>新工具适配参数</h3>
 * <ul>
 *   <li>RADIUS — AOE 半径（6 = 6x6 区域）</li>
 *   <li>DEPTH — AOE 纵深（1 = 单层）</li>
 * </ul>
 */
public class SapphireShovelAoeSkill implements ItemSkill {

    private static final int RADIUS = 6;
    private static final int DEPTH = 1;

    public static void causeAoe(Level level, BlockPos pos, BlockState state,
                                ItemStack shovel, LivingEntity livingEntity) {
        if (!(livingEntity instanceof ServerPlayer player)) return;
        if (level.isClientSide) return;

        if (!player.isCreative() && state.getDestroySpeed(level, pos) == 0.0F) return;
        if (!player.isShiftKeyDown()) return;

        HitResult pick = player.pick(20D, 0.0F, false);
        if (!(pick instanceof BlockHitResult blockHitResult)) return;

        if (ToolEnergy.hasEnergy(shovel) && !ToolEnergy.consumeForSkill(player, shovel, ToolEnergy.SHOVEL_COST))
            return;
                BlockBreaker.breakBlocks(blockHitResult, pos, shovel, level, player, RADIUS, DEPTH, BlockTags.MINEABLE_WITH_SHOVEL);
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
