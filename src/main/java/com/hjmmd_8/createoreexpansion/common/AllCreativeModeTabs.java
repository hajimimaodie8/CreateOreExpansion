package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.data.lang.Translatable;
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
    //
    // 标签页顺序（用 withTabsBefore 链起来，must be 无环）：
    //   base_tab（矿物拓展）→ energy_wave_study（能量波阵学）→ Create 的调色板
    // 也就是"本体的矿物线在前，能量波阵学（CEWS）作为独立板块紧跟其后"。
    @SuppressWarnings("Convert2MethodRef")
    BASE_TAB("base_tab", "itemGroup.createoreexpansion",
            tabKey(EnergyWaveStudyTab.TAB_ID), () -> AllItems.JADE_INGOT.asStack()),

    /**
     * <b>机械动力：能量波阵学</b>（Create: Energy Wave Studies，简称 <b>CEWS</b>）。
     *
     * <p>能量波系统的机器 + 三种机壳（现有两种）+ 波情查询仪归到这里，作为一个独立板块
     * （用户 2026-09-14 要求）。图标用<b>翡翠应力充能器</b>——它是整条能量波线的起点（波由充能器发出），
     * 比"波变器"更能代表这一板块（用户指定）。</p>
     *
     * <p>后续要把它整包拆成一个独立的内置 jar（新模块 CEWS），届时"哪些内容属于这个模块"就以
     * {@link EnergyWaveStudyTab#CONTENTS} 那一份清单为准——所以清单只有一处，标签页内容与
     * 未来的拆包依据共用它。</p>
     */
    ENERGY_WAVE_STUDY(EnergyWaveStudyTab.TAB_ID,
            com.simibubi.create.AllCreativeModeTabs.PALETTES_CREATIVE_TAB.getKey(),
            () -> AllBlocks.JADE_STRESS_CHARGER.asStack());

    /** 由 id 构造标签页的 {@link ResourceKey}：<b>不依赖 holder</b>（枚举构造期 holder 还没有）。 */
    private static ResourceKey<CreativeModeTab> tabKey(String id) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, id));
    }

    // 演出注册器
    private static final DeferredRegister<CreativeModeTab> TABS
            = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateOreExpansion.MOD_ID);


    // 公开变量
    public DeferredHolder<CreativeModeTab, CreativeModeTab> holder;

    public final String id;
    private final String titleTranslationKey;
    public final Translatable translatable;
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
