package com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy;

import com.hjmmd_8.createoreexpansion.client.tool.StrategyRenderer;
import com.hjmmd_8.createoreexpansion.foundation.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Set;

/**
 * 技能策略 —— 负责计算技能需要处理的实体/方块集合，并提供对应渲染器。
 *
 * @param <T> 被处理的对象类型（BlockPos / Entity）
 */
public interface SkillStrategy<T> {

    /**
     * 计算需要处理的对象集合（不包括中心方块）。
     *
     * @param skill  技能数据
     * @param params 上下文参数
     */
    Set<T> calculate(DataSkill skill, IParams params);

    /**
     * 是否需要渲染预览。
     */
    default boolean shouldRender(DataSkill skill, ClientLevel world, IParams params) {
        return true;
    }

    /**
     * 策略对应的预览渲染器（方块范围框 / 实体轮廓 / 空）。
     */
    StrategyRenderer getRenderer();
}
