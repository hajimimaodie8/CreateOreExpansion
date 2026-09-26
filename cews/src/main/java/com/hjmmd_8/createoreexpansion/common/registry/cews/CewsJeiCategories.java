package com.hjmmd_8.createoreexpansion.common.registry.cews;

import java.util.List;

import com.hjmmd_8.createoreexpansion.compat.jei.cews.StellarWaveTransmuterCategory;

/**
 * <b>CEWS（能量波阵学）自己的 JEI 内容</b>（P3c：从 {@code compat/jei/CreateOreExpansionJEI} 拆出）。
 *
 * <p>本层目前只有一张<b>无真实配方的"工作原理"说明分类</b>：星辉波变器流程卡
 * （{@link StellarWaveTransmuterCategory}，单张展示卡 + 变器方块 catalyst，无逐机器动画）。
 * 它与"有配方的类别"分开存放，是因为 JEI 的生命周期回调不同
 * （见协调入口的三段 {@code register*} 方法）。</p>
 *
 * <p><b>调用方</b>（P7a 更正）：同模块的 {@code compat.jei.cews.CewsJeiPlugin}（{@code :cews} 自己的
 * {@code @JeiPlugin}）。原来写的是"集成层的 {@code CreateOreExpansionJEI} 按拆分前的逐条顺序收集，
 * 所以 JEI 侧栏里的分类顺序一字不变"——<b>那个机制描述是错的</b>：JEI 侧栏顺序由
 * {@code config/jei/recipe-category-sort-order.ini} 对<b>配方类型 UID</b> 排序决定
 * （{@code RecipeManagerInternal} 构造时统一排序），与插件、与收集顺序都无关。
 * 保持顺序要做的唯一一件事就是"别改类别 UID、别少注册类别"，本轮两者都没做。</p>
 */
public final class CewsJeiCategories {

    /** 本层的"工作原理"说明分类，按拆分前的展示顺序（当前只有一张）。 */
    public static List<StellarWaveTransmuterCategory> flowCategories() {
        return List.of(new StellarWaveTransmuterCategory());
    }

    private CewsJeiCategories() {}
}
