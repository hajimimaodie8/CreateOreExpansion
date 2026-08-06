package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.common.AllItems;

/**
 * 工具轮廓渲染器注册表
 *
 * <p>集中管理所有工具的渲染器配置</p>
 */
public class ToolOutlineRenderers {

    // 翡翠工具 - 绿色
    public static final ToolOutlineRenderer JADE_PICKAXE = new ToolOutlineRenderer(
        RendererConfig.jade(AllItems.JADE_PICKAXE),
        new DirectionalRenderStrategy()
    );

    public static final ToolOutlineRenderer JADE_SHOVEL = new ToolOutlineRenderer(
        RendererConfig.jade(AllItems.JADE_SHOVEL),
        new LinearRenderStrategy(6)
    );

    public static final ToolOutlineRenderer JADE_AXE = new ToolOutlineRenderer(
        RendererConfig.jade(AllItems.JADE_AXE),
        new TreeChoppingRenderStrategy(8, false, 150),
        true  // 需要原木
    );

    // 蓝宝石工具 - 蓝色
    public static final ToolOutlineRenderer SAPPHIRE_PICKAXE = new ToolOutlineRenderer(
        RendererConfig.sapphire(AllItems.SAPPHIRE_PICKAXE),
        new AreaRenderStrategy(5, 1)
    );

    public static final ToolOutlineRenderer SAPPHIRE_SHOVEL = new ToolOutlineRenderer(
        RendererConfig.sapphire(AllItems.SAPPHIRE_SHOVEL),
        new AreaRenderStrategy(7, 1)
    );

    public static final ToolOutlineRenderer SAPPHIRE_AXE = new ToolOutlineRenderer(
        RendererConfig.sapphire(AllItems.SAPPHIRE_AXE),
        new TreeChoppingRenderStrategy(12, true, Integer.MAX_VALUE),
        true  // 需要原木
    );

    // 黄玉工具 - 金色
    public static final ToolOutlineRenderer TOPAZ_PICKAXE = new ToolOutlineRenderer(
        RendererConfig.topaz(AllItems.TOPAZ_PICKAXE),
        new AreaRenderStrategy(3, 1)
    );

    public static final ToolOutlineRenderer TOPAZ_SHOVEL = new ToolOutlineRenderer(
        RendererConfig.topaz(AllItems.TOPAZ_SHOVEL),
        new LinearRenderStrategy(8)
    );

    public static final ToolOutlineRenderer TOPAZ_AXE = new ToolOutlineRenderer(
        RendererConfig.topaz(AllItems.TOPAZ_AXE),
        new TreeChoppingRenderStrategy(8, true, Integer.MAX_VALUE),
        true  // 需要原木
    );

    // 所有渲染器数组，用于统一调用
    public static final ToolOutlineRenderer[] ALL_RENDERERS = {
        JADE_PICKAXE, JADE_SHOVEL, JADE_AXE,
        SAPPHIRE_PICKAXE, SAPPHIRE_SHOVEL, SAPPHIRE_AXE,
        TOPAZ_PICKAXE, TOPAZ_SHOVEL, TOPAZ_AXE
    };
}
