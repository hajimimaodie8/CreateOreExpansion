package com.hjmmd_8.createoreexpansion.common.registry.cews;

import java.util.List;

import com.hjmmd_8.createoreexpansion.compat.jei.category.StellarWaveTransmuterCategory;

/**
 * <b>CEWS（能量波阵学）自己的 JEI 内容</b>（P3c：从 {@code compat/jei/CreateOreExpansionJEI} 拆出）。
 *
 * <p>本层目前只有一张<b>无真实配方的"工作原理"说明分类</b>：星辉波变器流程卡
 * （{@link StellarWaveTransmuterCategory}，单张展示卡 + 变器方块 catalyst，无逐机器动画）。
 * 它与"有配方的类别"分开存放，是因为 JEI 的生命周期回调不同
 * （见协调入口的三段 {@code register*} 方法）。</p>
 *
 * <p><b>调用方</b>：{@link com.hjmmd_8.createoreexpansion.compat.jei.CreateOreExpansionJEI}
 * （SHARED 层的 JEI 协调入口，也是唯一的 {@code @JeiPlugin}）。它按拆分前
 * {@code loadCategories()} 里的顺序收集各层内容，所以 JEI 侧栏里的分类顺序一字不变。</p>
 */
public final class CewsJeiCategories {

    /** 本层的"工作原理"说明分类，按拆分前的展示顺序（当前只有一张）。 */
    public static List<StellarWaveTransmuterCategory> flowCategories() {
        return List.of(new StellarWaveTransmuterCategory());
    }

    private CewsJeiCategories() {}
}
