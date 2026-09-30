package com.hjmmd_8.createoreexpansion.common.registry;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.i18n.Translatable;

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
 * 两层各自建一张会让 {@code RegisterEvent} 多一条监听；一张表由谁挂总线（P7a 起 =
 * {@link LayerBootstrap#ensureAttached}，恰一次）与"谁先登记页"是两件事，后者只影响
 * {@code BuiltInRegistries.CREATIVE_MODE_TAB} 的枚举顺序。</p>
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
    // P7a：登记动作<b>按层自持</b>——原来的"根侧注入 registrar"机制已删除
    //
    // 旧形态（P3q）：层调用 ensureRegistered() 发请求，根侧
    // installTabRegistrar(...) 把"按层顺序遍历所有页"的动作注入进来。那套机制的前提是
    // **根工程在场**；而 P7a 的终局是根工程降级为 dev-only、发布形态只有三个模块 jar，
    // 所以登记动作必须由每一层自己发起：
    //     :coe  → LayerCreativeTab.registerAll(CoeCreativeTabs.tabs())
    //     :cews → LayerCreativeTab.registerAll(CewsCreativeTabs.tabs())
    //
    // 谁先登记不影响玩家可见顺序：创造页的显示顺序是 withTabsBefore 图上的**拓扑排序**
    // （CreativeModeTabRegistry.recalculateItemCreativeModeTabs → TopologicalSort），
    // 登记顺序只影响 BuiltInRegistries.CREATIVE_MODE_TAB 的枚举顺序（不进存档、不参与网络同步）。
    // 每层的 tabs() 只含自己那一页，因此跨层不会撞 id
    // （同 id 二次登记会抛 IllegalStateException("Duplicate registration …")）。
    //
    // key() 本来就不依赖 holder（见下），所以 Registrate 设 defaultCreativeTab 时不需要
    // "页已经登记过"这个前提——这也是本机制能被删掉的前提。
    // -----------------------------------------------------------------------
    private final String id;
    private final String titleTranslationKey;
    private final ResourceKey<CreativeModeTab> before;
    private final Supplier<ItemStack> icon;

    /**
     * 可选的「页实现」工厂（{@code CreativeModeTab.Builder -> CreativeModeTab}），默认 {@code null}。
     *
     * <p><b>为什么是 {@link java.util.function.Function} 而不是某个具体类</b>：需要换实现的是
     * {@code :coe} 的 {@code base_tab}（它要一个会在 {@code getDisplayItems()} 里补空行的子类，
     * 见 {@code docs/共享经验-盔甲与材料集/08-创造页创造分区横幅…}），而本类住 core（SHARED 层）
     * —— <b>core 不许 import 层专属类</b>。所以这里只持有一个纯 JDK 函数，由 {@code :coe}
     * 通过 {@link #sectioned} 填进来（与 {@code CoeCore} 既有的注入手法一致）。</p>
     *
     * <p><b>为什么是「每页一个字段」而不是「core 里一个全局静态」</b>：每层都调一次全局注入
     * 会互相覆盖（最后调的那层说了算），而 CEWS 的 {@code energy_wave_study} 页
     * <b>必须保持原样</b>（它靠 {@code EnergyWaveStudyTab} 的 remove/accept 钉顺序）。</p>
     */
    @Nullable
    private java.util.function.Function<CreativeModeTab.Builder, CreativeModeTab> tabFactory;

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
     * 声明本页用<b>自定义的 {@code CreativeModeTab} 子类</b>来构建（默认不用）。
     *
     * <p>调用方给的是 {@code CreativeModeTab.Builder -> CreativeModeTab} 的构造器引用
     * （例如 {@code CoeSectionedTab::new}），它会被转交给原版的
     * {@code CreativeModeTab.Builder#withTabFactory}，由 {@code build()} 在最后一步调用
     * （NeoForge 21.1.228 / 1.21.1：{@code CreativeModeTab$Builder} 有字段
     * {@code tabFactory} 与公开方法 {@code withTabFactory}，本轮已用 {@code javap} 核实）。</p>
     *
     * <p><b>返回 {@code this} 以便链式声明</b>：{@code LayerCreativeTab.of(...).sectioned(CoeSectionedTab::new)}。</p>
     */
    public LayerCreativeTab sectioned(
            java.util.function.Function<CreativeModeTab.Builder, CreativeModeTab> factory) {
        this.tabFactory = factory;
        return this;
    }

    /**
     * 把若干页按<b>给定顺序</b>登记进注册表（等价于拆分前遍历枚举 {@code values()} 的那段循环）。
     *
     * <p><b>P7a：调用方 = 每个层自己的 {@code @Mod} 构造器</b>（见上面那段注释），
     * 一页只登记一次；同一个 id 登记两次会抛
     * {@code IllegalStateException("Duplicate registration …")}。</p>
     *
     * <p><b>分区换实现只走 {@link #sectioned}</b>：没声明工厂的页（CEWS 那一页）走原来那条
     * {@code CreativeModeTab.builder()...build()} 路径，行为与分区特性引入前逐字一致。</p>
     */
    public static void registerAll(List<LayerCreativeTab> tabs) {
        for (LayerCreativeTab tab : tabs) {
            tab.holder = TABS.register(tab.id, () -> {
                CreativeModeTab.Builder builder = CreativeModeTab.builder()
                    .title(Component.translatable(tab.titleTranslationKey))
                    .withTabsBefore(tab.before)
                    .icon(tab.icon);
                if (tab.tabFactory != null) {
                    builder.withTabFactory(tab.tabFactory);
                }
                return builder.build();
            });
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
     * <p><b>P3q 起不再依赖 holder</b>。原先返回 {@code holder.getKey()}，于是"登记必须先于读取"
     * 成了层 Registrate 的硬约束（它要拿 key 去设 {@code defaultCreativeTab}）。而 key 本就是
     * id 的纯函数 —— {@code DeferredHolder.getKey()} 返回的也正是
     * {@code ResourceKey.create(CREATIVE_MODE_TAB, id)}，与 {@link #tabKey(String)} 逐字同值。
     * P7a 删掉"根侧注入登记动作"那套机制之后，这条性质是本机制能成立的前提：
     * 页的登记可以晚于 Registrate 设 {@code defaultCreativeTab}，不会 NPE，值也不变。</p>
     */
    public ResourceKey<CreativeModeTab> key() {
        return tabKey(id);
    }
}
