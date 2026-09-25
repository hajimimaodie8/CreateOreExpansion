package com.hjmmd_8.createoreexpansion.common.registry;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.data.lang.Translatable;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * <b>单个"本模组创造模式标签页"的载体</b>（P3c：{@code AllCreativeModeTabs} 按层拆分，住 SHARED 层）。
 *
 * <p><b>它从哪来</b>：原先是枚举 {@code common/AllCreativeModeTabs}：COE 的
 * {@code base_tab}（矿物拓展）与 CEWS 的 {@code energy_wave_study}（能量波阵学）两个枚举项
 * 住在同一个枚举里。P3c 把两个页各自搬进自己层的类
 * （{@code common/registry/coe/CoeCreativeTabs}、{@code .../cews/CewsCreativeTabs}），
 * 本类就是它们共用的那个载体与注册表。</p>
 *
 * <p><b>为什么 DeferredRegister 只留一份（SHARED）</b>：与原 {@code AllCreativeModeTabs.TABS}
 * 完全一样——同名同命名空间（{@link CoeCore#REGISTRY_NAMESPACE}）的一张创造页注册表。
 * 三层（这里是两层）各自建一张会让 {@code RegisterEvent} 多一条监听、也会改变
 * "先注册哪个页"这件事；这里保持"一张表 + 按层顺序登记"的写法，注册顺序可与拆分前逐字一致。</p>
 *
 * <p><b>{@link #tabKey(String)} 为什么是 static 工具而不是 {@code key()}</b>：{@code withTabsBefore}
 * 需要的是一个 {@link ResourceKey}，而 {@code ResourceKey} 必须在 <b>页的 holder 还不存在</b>时就能算出来
 * （COE 的页要排在 CEWS 的页前面，构建时那两个 holder 都还没被注册）。所以走"由 id 直接造 key"这条
 * 与拆分前相同的路径。</p>
 */
public final class LayerCreativeTab {

    /** 创造页注册表（命名空间 = createoreexpansion，与拆分前同一张表）。 */
    private static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CoeCore.REGISTRY_NAMESPACE);

    /** 由 id 构造标签页的 {@link ResourceKey}：<b>不依赖 holder</b>（登记期 holder 还没有）。 */
    public static ResourceKey<CreativeModeTab> tabKey(String id) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, id));
    }

    private final String id;
    private final String titleTranslationKey;
    private final ResourceKey<CreativeModeTab> before;
    private final Supplier<ItemStack> icon;

    /** 与拆分前同名的公开字段（`Translatable` 形态的标题键视图）。 */
    public final Translatable translatable;

    @Nullable
    private DeferredHolder<CreativeModeTab, CreativeModeTab> holder;

    private LayerCreativeTab(String id, String titleTranslationKey,
                             ResourceKey<CreativeModeTab> before, Supplier<ItemStack> icon) {
        this.id = id;
        this.titleTranslationKey = titleTranslationKey;
        this.before = before;
        this.icon = icon;
        this.translatable = () -> titleTranslationKey;
    }

    /** 带显式标题键的声明（拆分前 {@code AllCreativeModeTabs(String, String, ResourceKey, Supplier)}）。 */
    public static LayerCreativeTab of(String id, String titleTranslationKey,
                                      ResourceKey<CreativeModeTab> before, Supplier<ItemStack> icon) {
        return new LayerCreativeTab(id, titleTranslationKey, before, icon);
    }

    /** 标题键按 {@code itemGroup.<命名空间>.<id>} 推出的声明（拆分前的另一个构造器）。 */
    public static LayerCreativeTab of(String id, ResourceKey<CreativeModeTab> before, Supplier<ItemStack> icon) {
        return new LayerCreativeTab(id, "itemGroup." + CoeCore.REGISTRY_NAMESPACE + "." + id, before, icon);
    }

    /**
     * 把若干页按<b>给定顺序</b>登记进注册表（等价于拆分前遍历枚举 {@code values()} 的那段循环）。
     *
     * <p>顺序就是调用方传进来的列表顺序，因此"哪个页先注册"这件事完全由协调入口决定，
     * 与拆分前逐字一致。</p>
     */
    public static void registerAll(List<LayerCreativeTab> tabs) {
        for (LayerCreativeTab tab : tabs) {
            tab.holder = TABS.register(tab.id,
                () -> CreativeModeTab.builder()
                    .title(Component.translatable(tab.titleTranslationKey))
                    .withTabsBefore(tab.before)
                    .icon(tab.icon)
                    .build());
        }
    }

    /** 向事件总线注册注册表（等价于拆分前 {@code AllCreativeModeTabs.register}）。 */
    public static void registerOn(IEventBus bus) {
        TABS.register(bus);
    }

    /** 页 id（{@code base_tab} / {@code energy_wave_study}）。 */
    public String id() {
        return id;
    }

    /** 页的 {@link ResourceKey}（须先 {@code registerAll} 过）。 */
    public ResourceKey<CreativeModeTab> key() {
        return holder.getKey();
    }
}
