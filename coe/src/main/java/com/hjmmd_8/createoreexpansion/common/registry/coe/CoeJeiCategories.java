package com.hjmmd_8.createoreexpansion.common.registry.coe;

import java.util.List;

import com.hjmmd_8.createoreexpansion.compat.jei.coe.LightningBlockCategory;
import com.hjmmd_8.createoreexpansion.compat.jei.coe.LightningCategory;
import com.hjmmd_8.createoreexpansion.compat.jei.base.CreateRecipeCategory;
import com.hjmmd_8.createoreexpansion.content.lightning.LightningBlockRecipe;
import com.hjmmd_8.createoreexpansion.content.lightning.LightningRecipe;

import net.minecraft.world.level.block.Blocks;

/**
 * <b>COE（矿物拓展）自己的 JEI 配方类别</b>（P3c：从 {@code compat/jei/CreateOreExpansionJEI} 拆出）。
 *
 * <p>本层有两个类别：<b>雷击加工</b>（{@code lightning}）与<b>方块雷击加工</b>
 * （{@code lightning_block}），催化剂 = 原版避雷针 + 本模组的强化避雷针。</p>
 *
 * <p><b>调用方</b>（P7a 更正）：同模块的 {@code compat.jei.coe.CreateOreExpansionJEI}
 * （{@code :coe} 自己的 {@code @JeiPlugin}）。原来写的是"集成层的
 * {@code CreateOreExpansionJEI} 按拆分前的逐条顺序收集，所以 JEI 侧栏里的分类顺序一字不变"——
 * <b>那个机制描述是错的</b>：侧栏顺序由 {@code recipe-category-sort-order.ini} 对配方类型 UID
 * 排序决定，与插件、与收集顺序都无关（详见那个插件类的注释）。</p>
 *
 * <p><b>跨层引用方向</b>：本类 import 的是 core 的 {@code compat/jei/base/**} 以及本层的
 * 配方类型/配方类/方块——不产生任何禁止方向的依赖。
 * 类别 id（{@code createoreexpansion:lightning} / {@code ...:lightning_block}）与拆分前一致。</p>
 */
public final class CoeJeiCategories {

    /** 本层要展示的配方类别，按拆分前的展示顺序（雷击 → 方块雷击）。 */
    public static List<CreateRecipeCategory<?>> categories() {
        return List.of(
            new CreateRecipeCategory.Builder<>(LightningRecipe.class)
                .addTypedRecipes(CoeRecipeTypes.LIGHTNING)
                .catalyst(() -> Blocks.LIGHTNING_ROD)
                .catalyst(() -> CoeBlocks.REINFORCED_LIGHTNING_ROD.get())
                .itemIcon(CoeBlocks.REINFORCED_LIGHTNING_ROD.get())
                .emptyBackground(178, 72)
                .build("lightning", LightningCategory::new),

            new CreateRecipeCategory.Builder<>(LightningBlockRecipe.class)
                .addTypedRecipes(CoeRecipeTypes.LIGHTNING_BLOCK)
                .catalyst(() -> Blocks.LIGHTNING_ROD)
                .catalyst(() -> CoeBlocks.REINFORCED_LIGHTNING_ROD.get())
                .itemIcon(CoeBlocks.REINFORCED_LIGHTNING_ROD.get())
                .emptyBackground(178, 72)
                .build("lightning_block", LightningBlockCategory::new));
    }

    private CoeJeiCategories() {}
}
