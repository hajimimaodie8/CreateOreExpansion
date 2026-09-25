package com.hjmmd_8.createoreexpansion.data;

import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsRecipeProvider;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRecipeProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;

import java.util.concurrent.CompletableFuture;

/**
 * <b>配方生成的协调入口</b>（P3c：四个"多层枢纽文件"按层拆分后保留的同名入口，住 SHARED 层）。
 *
 * <p><b>拆分前</b>：本类一个类里同时写着 COE 的拆磨配方（61 条）与 CEWS 的工具充能配方
 * （每个可充能物品 × 5 个充能等级）——两层的东西混在一处。</p>
 *
 * <p><b>拆分后</b>：两层的配方<b>生成动作</b>搬进各自层的类
 * （{@link CoeRecipeProvider#generate} / {@link CewsRecipeProvider#generate}），
 * 本类只负责"按拆分前的顺序调用它们"，并且继续是<b>唯一</b>被挂到 {@code DataGenerator} 上的
 * 那个提供器（见 {@code data/CreateOreExpansionDatagen}）。</p>
 *
 * <p><b>为什么只保留这一个提供器、而各层不是"各挂一个"</b>：datagen 的四条硬规则第一条就是
 * "提供器同名会抛 {@code Duplicate provider}"，而 {@code DataProvider#getName()} 默认取类名——
 * 拆成三个同名类必然撞车。本轮不需要 per-mod 的 datagen（NeoForge 只为 {@code --mod} 指定的那个
 * mod 执行生成器），所以各层只提供"生成动作"、由本入口按序调用，提供器仍是与拆分前同名同序的那一个。</p>
 *
 * <p><b>顺序</b>：{@code buildRecipes} 里的调用顺序与拆分前逐字相同——
 * COE 拆磨（原版 4 组装备 → 本模组 5 组工具）→ CEWS 工具充能。
 * 配方落盘文件名带条目名（互不覆盖），但调用顺序决定产物的生成顺序，
 * 保持它就能保证 {@code src/generated} 逐条目不变。</p>
 *
 * <p>粗矿序列加工配方（切割 → 压片 → 角磨）手写在 {@code resources/data}（不走生成器）；
 * 高级角磨（≥2 级轮执行 Create 粉碎轮/石磨配方）是<b>代码动态匹配</b>
 * （{@code GrinderRecipeTypes} 注册表），不在此生成——加入其他模组（同类型或自行注册的类型）自动生效。</p>
 */
public class RecipeProvider extends net.minecraft.data.recipes.RecipeProvider {
    public RecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // 层顺序 = 拆分前逐字相同：COE（拆磨：原版 4 组 + 本模组 5 组）→ CEWS（工具充能）
        CoeRecipeProvider.generate(output);
        CewsRecipeProvider.generate(output);
    }
}
