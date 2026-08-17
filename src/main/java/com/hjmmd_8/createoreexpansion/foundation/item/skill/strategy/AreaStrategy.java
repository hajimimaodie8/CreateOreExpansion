package com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy;

import com.hjmmd_8.createoreexpansion.client.tool.AllStrategyRenderers;
import com.hjmmd_8.createoreexpansion.client.tool.StrategyRenderer;
import net.minecraft.core.BlockPos;

/**
 * 区域策略接口 - 计算需要处理的方块位置。
 *
 * <p>通用接口，可用于技能破坏逻辑与渲染预览，渲染器统一为方块轮廓。</p>
 */
@FunctionalInterface
public interface AreaStrategy extends SkillStrategy<BlockPos> {

    @Override
    default StrategyRenderer getRenderer() {
        return AllStrategyRenderers.Renderers.BLOCK;
    }
}
