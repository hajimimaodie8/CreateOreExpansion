package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.client.tool.renderer.BlockToolOutlineRenderer;
import com.hjmmd_8.createoreexpansion.client.tool.renderer.EmptyRenderer;
import com.hjmmd_8.createoreexpansion.client.tool.renderer.EntityOutlineRenderer;

/**
 * 客户端渲染器注册表 —— 各策略通过 {@code getRenderer()} 返回对应渲染器。
 *
 * <p>领域层不感知渲染，此处为渲染器实例的唯一持有处。</p>
 */
public final class AllStrategyRenderers {

    private AllStrategyRenderers() {
    }

    /** 渲染器实例 */
    public static final class Renderers {
        public static final StrategyRenderer BLOCK = new BlockToolOutlineRenderer();
        public static final StrategyRenderer ENTITY = new EntityOutlineRenderer();
        public static final StrategyRenderer EMPTY = new EmptyRenderer();
    }
}
