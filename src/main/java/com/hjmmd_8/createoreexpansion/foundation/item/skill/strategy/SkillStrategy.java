package com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy;

import com.hjmmd_8.createoreexpansion.client.tool.StrategyRenderer;
import com.hjmmd_8.createoreexpansion.foundation.util.params.IParams;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Set;

public interface SkillStrategy<T> {
    /**
     * 计算需要处理的 Object
     * @param skill 技能
     * @param params 需要处理的上下文
     * @return 需要处理的 Object 集合（不包括中心方块）
     */
    Set<T> calculate(DataSkill skill, IParams params);

    /**
     * 是否需要渲染
     * @param skill 技能
     * @param world 世界
     * @param params 需要处理的上下文
     * @return 是否需要渲染
     */
    default boolean shouldRender(DataSkill skill, ClientLevel world, IParams params) {
        return true;
    }

    StrategyRenderer getRenderer();
}
