package com.hjmmd_8.createoreexpansion.foundation.item.skill.strategy;

import net.minecraft.core.BlockPos;

/**
 * 区域策略接口 - 计算需要处理的方块位置。
 *
 * <p>通用接口，可用于技能破坏逻辑与渲染预览。</p>
 *
 * <p><b>W2 渲染统一</b>：原先这里覆写 {@code getRenderer()} 返回旧
 * {@code AllStrategyRenderers.Renderers.BLOCK}；旧渲染栈删除后该覆写随之删除
 * （{@link SkillStrategy} 也已不再声明这个渲染 API）。方块预览现在由新路径的
 * {@code CoeBlockOutlineRenderer} 承担。本接口仍是 {@code @FunctionalInterface}：
 * 唯一的抽象方法是从 {@link SkillStrategy} 继承的 {@code calculate}。</p>
 */
@FunctionalInterface
public interface AreaStrategy extends SkillStrategy<BlockPos> {
}
