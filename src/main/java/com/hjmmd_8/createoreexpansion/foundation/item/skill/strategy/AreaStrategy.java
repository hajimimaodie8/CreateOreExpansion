package com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy;

import com.hjmmd_8.createoreexpansion.client.tool.StrategyRenderer;
import com.hjmmd_8.createoreexpansion.common.AllStrategies;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
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
public interface AreaStrategy extends SkillStrategy<BlockPos> {

    @Override
    default StrategyRenderer getRenderer() {
        return AllStrategies.Renderers.BLOCK;
    }
}
