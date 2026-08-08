package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.data.COELangProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// Enum 类 —— 枚举类，用于创建创造物品栏
public enum AllCreativeModeTabs {
    // 具体枚举项
    @SuppressWarnings("Convert2MethodRef")
    BASE_TAB("base_tab", "itemGroup.createoreexpansion",
            com.simibubi.create.AllCreativeModeTabs.PALETTES_CREATIVE_TAB.getKey(), () -> AllItems.JADE_INGOT.asStack());

    // 演出注册器
    private static final DeferredRegister<CreativeModeTab> TABS
            = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateOreExpansion.MOD_ID);


    // 公开变量
    public DeferredHolder<CreativeModeTab, CreativeModeTab> holder;

    public final String id;
    private final String titleTranslationKey;
    public final COELangProvider.Translatable translatable;
    public final ResourceKey<CreativeModeTab> before;
    public final Supplier<ItemStack> icon;

    AllCreativeModeTabs(String id, String title,
                               ResourceKey<CreativeModeTab> before, Supplier<ItemStack> icon) {
        this.id = id;
        this.titleTranslationKey = title;
        this.before = before;
        this.icon = icon;
        this.translatable = () -> titleTranslationKey;
    }

    AllCreativeModeTabs(String id, ResourceKey<CreativeModeTab> before, Supplier<ItemStack> icon) {
        this(id, "itemGroup." + CreateOreExpansion.MOD_ID + "." + id, before, icon);
    }

    // 注册物品栏，不向事件总线注册注册器
    public static void registerTabs() {
        for (AllCreativeModeTabs tab : values()) {
            tab.holder = TABS.register(tab.id,
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable(tab.titleTranslationKey))
                            .withTabsBefore(tab.before)
                            .icon(tab.icon)
                            .build());
        }
    }

    // 向事件总线注册注册器
    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    public ResourceKey<CreativeModeTab> key() {
        return holder.getKey();
    }
}
