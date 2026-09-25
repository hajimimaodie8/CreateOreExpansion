package com.hjmmd_8.createoreexpansion.common.registry.coe;

import java.util.List;

import com.hjmmd_8.createoreexpansion.compat.jei.category.LightningBlockCategory;
import com.hjmmd_8.createoreexpansion.compat.jei.category.LightningCategory;
import com.hjmmd_8.createoreexpansion.compat.jei.category.base.CreateRecipeCategory;
import com.hjmmd_8.createoreexpansion.content.lightning.LightningBlockRecipe;
import com.hjmmd_8.createoreexpansion.content.lightning.LightningRecipe;

import net.minecraft.world.level.block.Blocks;

/**
 * <b>COE（矿物拓展）自己的 JEI 配方类别</b>（P3c：从 {@code compat/jei/CreateOreExpansionJEI} 拆出）。
 *
 * <p>本层有两个类别：<b>雷击加工</b>（{@code lightning}）与<b>方块雷击加工</b>
 * （{@code lightning_block}），催化剂 = 原版避雷针 + 本模组的强化避雷针。</p>
 *
 * <p><b>调用方</b>：{@link com.hjmmd_8.createoreexpansion.compat.jei.CreateOreExpansionJEI}
 * （SHARED 层的 JEI 协调入口，也是唯一的 {@code @JeiPlugin}）。它按拆分前
 * {@code loadCategories()} 里的逐条顺序收集各层类别，所以 JEI 侧栏里的分类顺序一字不变。</p>
 *
 * <p><b>跨层引用方向</b>：本类 import 的是 SHARED 层的 {@code compat/jei/**} 与
 * {@code common/AllRecipeTypes}，以及本层的配方类/方块——不产生任何禁止方向的依赖。
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
