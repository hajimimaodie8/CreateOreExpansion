package com.hjmmd_8.createoreexpansion.common.registry.transmutation;

import java.util.List;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.compat.jei.base.ProcessingViaFanCategory;
import com.hjmmd_8.createoreexpansion.compat.jei.transmutation.TransmutingCategory;
import com.hjmmd_8.createoreexpansion.compat.jei.category.base.CreateRecipeCategory;
import com.hjmmd_8.createoreexpansion.content.transmuting.AllTransmutingRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * <b>TRANS（机械嬗变化）自己的 JEI 配方类别</b>（P3c：从 {@code compat/jei/CreateOreExpansionJEI} 拆出）。
 *
 * <p>本层只有一个类别：<b>嬗变（风扇吹过嬗变液）</b>——配方类型
 * {@code TransmutationRecipeTypes.TRANSMUTING}（原来写的是已删除的集成层别名
 * {@code AllRecipeTypes.TRANSMUTING}），催化剂 = 鼓风机 + 嬗变液桶。</p>
 *
 * <p><b>调用方</b>（P7a 更正）：同模块的 {@code compat.jei.transmutation.TransmutationJeiPlugin}
 * （{@code :transmutation} 自己的 {@code @JeiPlugin}）。原来写的是"集成层的
 * {@code CreateOreExpansionJEI} 按拆分前的逐条顺序收集，所以 JEI 侧栏里的分类顺序一字不变"——
 * <b>那个机制描述是错的</b>：侧栏顺序由 {@code recipe-category-sort-order.ini} 对配方类型 UID
 * 排序决定，与插件、与收集顺序都无关。</p>
 *
 * <p><b>跨层引用方向</b>：本类 import 的是 core 的 {@code compat/jei/**}，以及本层的配方类——
 * 不产生任何禁止方向的依赖。类别 id（{@code createoreexpansion:fan_transmuting}）与拆分前一致。</p>
 */
public final class TransmutationJeiCategories {

    /** 本层要展示的配方类别，按拆分前的展示顺序。 */
    public static List<CreateRecipeCategory<?>> categories() {
        return List.of(
            new CreateRecipeCategory.Builder<>(AllTransmutingRecipe.class)
                .addTypedRecipes(TransmutationRecipeTypes.TRANSMUTING)
                .catalystStack(ProcessingViaFanCategory.getFan("fan_transmuting"))
                .doubleItemIcon(com.simibubi.create.AllBlocks.ENCASED_FAN.get(),
                    BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CoeCore.REGISTRY_NAMESPACE,
                        "transmutation_fluid_bucket")))
                .emptyBackground(178, 72)
                .build("fan_transmuting", TransmutingCategory::new));
    }

    private TransmutationJeiCategories() {}
}
