package com.hjmmd_8.createoreexpansion.foundation.util;

import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Set;

/**
 * 区域策略接口 - 计算需要处理的方块位置
 *
 * <p>通用接口，可用于：
 * <ul>
 *   <li>技能逻辑 - 计算需要破坏的方块</li>
 *   <li>渲染预览 - 显示技能预览轮廓</li>
 *   <li>其他需要计算位置的场景</li>
 * </ul>
 *
 */
@FunctionalInterface
public interface AreaStrategy {

    /**
     * 计算需要处理的方块位置
     * @param skill 技能
     * @param center 玩家点击的中心方块
     * @param hit 玩家看向的结果
     * @param player 玩家
     * @return 需要处理的方块位置集合（不包括中心方块）
     */
    Set<BlockPos> calculatePositions(DataSkill skill, BlockPos center, BlockHitResult hit, Player player);

    /**
     * 是否需要渲染
     * @param skill 技能
     * @param world 世界
     * @param pos 方块位置
     * @param state 方块状态
     * @param player 玩家
     * @return 是否需要渲染
     */
    default boolean shouldRender(DataSkill skill, ClientLevel world, BlockPos pos, BlockState state, Player player) {
        return true;
    }
}
