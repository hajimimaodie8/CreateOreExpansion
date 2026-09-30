package com.hjmmd_8.createoreexpansion.common.registry.coe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * <b>会在 {@code getDisplayItems()} 里补空行的创造页子类</b>（只给 {@code base_tab} 用）。
 *
 * <p><b>它解决什么</b>：本页是一个<b>空建造器</b> —— 成员是在
 * {@code BuildCreativeModeTabContentsEvent} 上由两个 Registrate（{@code CoeRegistrate} 与
 * {@code CewsRegistrate} 的 {@code defaultCreativeTab}）塞进来的（见落地文档 §4.1~§4.3；
 * 2026-09-30 起 CEWS 的物品也走这条路 —— 该层不再有独立页）。所以「在生成器里排好顺序」
 * 这一招在这里不成立；改成<b>在渲染前的最后一刻</b>把真实列表切成三区、插空行、按分区顺序重排
 * （文档 §4.5 的路径 A）。</p>
 *
 * <p><b>怎么接上</b>：{@code CreativeModeTab.Builder#withTabFactory} 是原版的公开扩展点
 * （{@code build()} 最后一步会调 {@code tabFactory.apply(this)}）。Core 的
 * {@code LayerCreativeTab#sectioned} 持有一个纯 JDK 函数字段，由
 * {@code CoeCreativeTabs.BASE_TAB} 通过 {@code CoeSectionedTab::new} 填进来
 * —— core 层因此<b>不需要 import 本类</b>（红线：core 不许依赖层专属类）。</p>
 *
 * <p><b>为什么覆写 {@code getDisplayItems()} 而不是别的</b>：创造界面就是从这里取物品列表的
 * （{@code CreativeModeInventoryScreen} 的 {@code selectTab} 路径）。</p>
 *
 * <h3>三条不能违反的纪律（文档 §5 步骤 4 的四点 + §6.1）</h3>
 * <ol>
 *     <li><b>不要在 {@code Output} 里 {@code accept(EMPTY)}</b>：NeoForge 与原版都校验
 *         {@code stack.getCount() == 1}，而 {@code EMPTY} 的 count 是 0 ⇒ 直接抛
 *         {@code IllegalArgumentException}。空行只能在这里补。</li>
 *     <li><b>必须是纯函数</b>：切页、搜索、重建都会调用它（每次调用重算一遍三区），
 *         所以这里不做任何「只该做一次」的事，也不缓存中间态。</li>
 *     <li><b>长度守卫不能省</b>：桶内物品数之和 ≠ 真实项数时（说明判定出了问题），
 *         <b>原样返回真实列表</b>（不插空行）。渲染端找不到「整行空」就不会画横幅
 *         ⇒ 结果是「少几条横幅」，而不是「横幅错位压到物品上」。</li>
 * </ol>
 */
public final class CoeSectionedTab extends CreativeModeTab {

    /**
     * 唯一构造器：签名必须与 {@code withTabFactory} 要求的
     * {@code Function<CreativeModeTab.Builder, CreativeModeTab>} 一致。
     *
     * <p>父类是 {@code protected CreativeModeTab(Builder)} —— 原样透传，由它把 builder 的字段全读出来。</p>
     *
     * <p><b>为什么是包内可见而不是 {@code private}</b>：{@code CoeCreativeTabs} 用
     * {@code CoeSectionedTab::new} 这个方法引用（同包），{@code private} 会让构造器引用
     * <b>编译不过</b>（{@code 不兼容的类型: 构造器引用无效}）。</p>
     */
    CoeSectionedTab(CreativeModeTab.Builder builder) {
        super(builder);
    }

    @Override
    public Collection<ItemStack> getDisplayItems() {
        Collection<ItemStack> real = super.getDisplayItems();

        // 真实列表里的「空」不该存在（EMPTY 进不了 accept），这里仍然滤一遍：
        // 它同时让下面的「三方相等」守卫只与真物品比较。
        List<ItemStack> items = new ArrayList<>(real.size());
        for (ItemStack stack : real) {
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }

        List<CoeCreativeSections.Bucket> buckets = CoeCreativeSections.bucketize(items);

        int bucketSum = 0;
        for (CoeCreativeSections.Bucket bucket : buckets) {
            bucketSum += bucket.items().size();
        }
        if (bucketSum != items.size()) {
            // 守卫：一件物品被数了两次或漏数 ⇒ 退化成「不分区的原样列表」。
            return real;
        }

        return CoeCreativeSections.layout(CoeCreativeSections.BASE_TAB_KEY, buckets);
    }
}
