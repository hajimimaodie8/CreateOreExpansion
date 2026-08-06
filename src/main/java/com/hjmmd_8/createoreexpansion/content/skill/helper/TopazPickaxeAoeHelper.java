package com.hjmmd_8.createoreexpansion.content.skill.helper;

import com.hjmmd_8.createoreexpansion.tool.ToolEnergy;
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
 * 黄玉镐 — 3x3 范围挖掘入口
 *
 * <p>技能逻辑：按住 Shift 时，以玩家点击的方块为中心，向前方 3x3 平面范围破坏方块。</p>
 *
 * <hr>
 * <h3>新建镐子技能（如蓝玉镐）需修改的参数：</h3>
 * <table border="1">
 *   <tr><th>修改项</th><th>位置</th><th>说明</th></tr>
 *   <tr><td>RADIUS</td><td>类常量</td><td>AOE 半径，3=3x3，5=5x5</td></tr>
 *   <tr><td>DEPTH</td><td>类常量</td><td>AOE 纵深，1=单层，3=三层</td></tr>
 *   <tr><td>AllItems.xxx</td><td>causeAoe() 内</td><td>改成对应物品注册名</td></tr>
 *   <tr><td>类名</td><td>文件重命名</td><td>改为 XxxPickaxeAoeHelper</td></tr>
 * </table>
 */
public class TopazPickaxeAoeHelper {

    /** AOE 半径（3 = 3x3 平面） */
    private static final int RADIUS = 3;
    /** AOE 纵深（1 = 单层） */
    private static final int DEPTH = 1;

    /**
     * Mixin 入口。由 TopazPickaxeAoeMixin 在 destroyBlock() 时调用。
     * @param level      世界
     * @param pos        玩家直接点击的方块位置
     * @param state      方块状态
     * @param pickaxe    当前手持工具
     * @param livingEntity 挖掘者
     */
    public static void causeAoe(Level level, BlockPos pos, BlockState state,
                                ItemStack pickaxe, LivingEntity livingEntity) {
        if (!(livingEntity instanceof ServerPlayer player)) return;
        if (level.isClientSide) return;

        // 生存模式：跳过硬度为 0 的方块；创造模式允许破坏任何方块
        if (!player.isCreative() && state.getDestroySpeed(level, pos) == 0.0F) return;
        if (!player.isShiftKeyDown()) return;

        HitResult pick = player.pick(20D, 0.0F, false);
        if (!(pick instanceof BlockHitResult blockHitResult)) return;

        if (ToolEnergy.hasEnergy(pickaxe) && !ToolEnergy.consumeForSkill(player, pickaxe, ToolEnergy.PICKAXE_COST))
            return;
                BlockBreaker.breakBlocks(blockHitResult, pos, pickaxe, level, player, RADIUS, DEPTH, BlockTags.MINEABLE_WITH_PICKAXE);
    }
}
