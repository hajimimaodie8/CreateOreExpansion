package com.hjmmd_8.createoreexpansion.client.tool;

import com.hjmmd_8.createoreexpansion.common.AllItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 工具渲染配置
 */
public class RendererConfig {
    private final DeferredHolder<Item, ?> item;
    private final float r, g, b, a;

    public RendererConfig(DeferredHolder<Item, ?> item, float r, float g, float b, float a) {
        this.item = item;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public DeferredHolder<Item, ?> item() { return item; }
    public float r() { return r; }
    public float g() { return g; }
    public float b() { return b; }
    public float a() { return a; }

    // 预定义颜色
    public static final float[] JADE_GREEN = {0.0F, 0.85F, 0.3F};
    public static final float[] SAPPHIRE_BLUE = {0.0F, 0.5F, 1.0F};
    public static final float[] TOPAZ_GOLD = {1.0F, 0.85F, 0.0F};
    public static final float ALPHA = 0.5F;

    // 快速创建方法
    public static RendererConfig jade(DeferredHolder<Item, ?> item) {
        return new RendererConfig(item, JADE_GREEN[0], JADE_GREEN[1], JADE_GREEN[2], ALPHA);
    }

    public static RendererConfig sapphire(DeferredHolder<Item, ?> item) {
        return new RendererConfig(item, SAPPHIRE_BLUE[0], SAPPHIRE_BLUE[1], SAPPHIRE_BLUE[2], ALPHA);
    }

    public static RendererConfig topaz(DeferredHolder<Item, ?> item) {
        return new RendererConfig(item, TOPAZ_GOLD[0], TOPAZ_GOLD[1], TOPAZ_GOLD[2], ALPHA);
    }
}
