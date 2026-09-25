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

    /**
     * <b>能量波阵学页 id</b>（{@code energy_wave_study}，P3q 从 {@code common/hub/EnergyWaveStudyTab} 下移）。
     *
     * <p><b>为什么这个字符串要住 core</b>：页的<b>声明</b>属于 CEWS
     * （{@code common/registry/cews/CewsCreativeTabs}），但 COE 的 {@code base_tab} 要用它做
     * {@code withTabsBefore} 的链（顺序 = 矿物拓展 → 能量波阵学 → Create 调色板），
     * 而 COE <b>不许</b> import CEWS（禁止方向）。一个纯字符串常量是两层唯一都需要的东西，
     * 所以它下移到共同的底层——这也是 P3q 清掉
     * {@code CoeCreativeTabs → common/hub/EnergyWaveStudyTab} 那两条边的办法。</p>
     */
    public static final String ENERGY_WAVE_STUDY_TAB_ID = "energy_wave_study";

    /**
     * 由 id 构造标签页的 {@link ResourceKey}：<b>不依赖 holder</b>（登记期 holder 还没有）。
     */
    public static ResourceKey<CreativeModeTab> tabKey(String id) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE, id));
    }

    // -----------------------------------------------------------------------
    // P3q: 层的"我要建页了"请求 → 根侧注入注册动作
    //
    // 三个层的 Registrate 必须在设 defaultCreativeTab 之前拿到自己的页 key，而"按层顺序
    // 把页登记进注册表"这件事只有根侧知道全貌（COE 的页要在 CEWS 的页之前登记，反之不行）。
    // 于是：层调用 ensureRegistered()（core 入口，幂等），根侧在 CreateOreExpansion 构造器里
    // installTabRegistrar(...) 把真正的登记动作（按层顺序遍历两层的页）注入进来。
    // 形状与 P3e 的 LayerRegistrate.installOwnerChain / P3p 的 MedallionLink 相同。
    //
    // 两个细节是刻意的：
    //   1) installTabRegistrar 会在"请求已经来过"时立刻补跑 —— FML 构造 mod 的顺序不保证
    //      COE 一定在 CEWS / TRANS 之前，晚到的注入不能把先来的请求丢掉；
    //   2) key() 不再依赖 holder（见下），所以即使登记还没发生，
    //      REGISTRATE.defaultCreativeTab(tab.key()) 也拿得到正确的 ResourceKey，不会 NPE。
    // 幂等标志在跑之前置位（与拆分前 AllCreativeModeTabs.ensureTabs() 逐字同序）。
    // -----------------------------------------------------------------------
    private static Runnable tabRegistrar;
    private static boolean tabRegistrationRequested;
    private static boolean tabRegistrationDone;

    /** 根侧注入"按层顺序登记所有页"的动作（在 CreateOreExpansion 构造器里调用一次）。 */
    public static void installTabRegistrar(Runnable registrar) {
        tabRegistrar = registrar;
        if (tabRegistrationRequested && !tabRegistrationDone) {
            runTabRegistrar();
        }
    }

    /**
     * 幂等地请求登记创造页（层的 Registrate 调用；等价于拆分前的
     * {@code AllCreativeModeTabs.ensureTabs()}，只是登记动作由根侧注入）。
     */
    public static void ensureRegistered() {
        if (tabRegistrationDone) {
            return;
        }
        tabRegistrationRequested = true;
        if (tabRegistrar == null) {
            return; // 注入还没到；installTabRegistrar 会补跑
        }
        runTabRegistrar();
    }

    private static void runTabRegistrar() {
        tabRegistrationDone = true;
        tabRegistrar.run();
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

    /**
     * 页的 {@link ResourceKey}。
     *
     * <p><b>P3q：不再依赖 holder</b>。原先返回 {@code holder.getKey()}，于是"登记必须先于读取"
     * 成了层 Registrate 的硬约束（它要拿 key 去设 {@code defaultCreativeTab}）。而 key 本就是
     * id 的纯函数 —— {@code DeferredHolder.getKey()} 返回的也正是
     * {@code ResourceKey.create(CREATIVE_MODE_TAB, id)}，与 {@link #tabKey(String)} 逐字同值。
     * 改成直接算出来之后，登记动作可以由根侧稍后注入（{@link #ensureRegistered()}），
     * 不会因为 FML 构造 mod 的顺序不同而 NPE。值不变 ⇒ 所有既有调用点行为不变。</p>
     */
    public ResourceKey<CreativeModeTab> key() {
        return tabKey(id);
    }
}
