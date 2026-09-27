package com.hjmmd_8.createoreexpansion.data;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.LayerRecipeRouter;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRecipeProvider;
import com.hjmmd_8.createoreexpansion.common.registry.coe.charger.CoeChargingRecipeProvider;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import org.jetbrains.annotations.Nullable;

import net.neoforged.neoforge.common.conditions.ICondition;

import java.nio.file.Path;
import java.util.Map;
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
 * <p><b>P7d：两层产出的<b>落点</b>分家</b>。datagen 只有一个 {@code --output}（根工程的
 * {@code src/generated/resources}），所以"代码已经分家、产物却全落根"是 P7c §5 实测的发布阻断：
 * 三个模块 jar 的 {@code data/**&#47;recipe/**} 与根的 221 条<b>交集 = 0</b>——单装任一模块 jar
 * 一条生成配方都没有。修法<b>不是</b>按路径判层（CEWS 的配方路径也是
 * {@code data/createoreexpansion/recipe/...}，与 COE 逐字同类），而是把<b>调用点</b>的层交给
 * {@link LayerRecipeRouter}：本类每个 {@code generate} 各包一层「本层」的 {@code RecipeOutput}
 * （见 {@link #bind}），由它把那次写盘的归属传到后台线程上的 {@code writeIfNeeded}。
 * 细节与"为什么需要一次路径记录"见 {@code LayerRecipeRouter} 类注释。</p>
 *
 * <p>粗矿序列加工配方（切割 → 压片 → 角磨）手写在 {@code resources/data}（不走生成器）；
 * 高级角磨（≥2 级轮执行 Create 粉碎轮/石磨配方）是<b>代码动态匹配</b>
 * （{@code GrinderRecipeTypes} 注册表），不在此生成——加入其他模组（同类型或自行注册的类型）自动生效。</p>
 */
public class RecipeProvider extends net.minecraft.data.recipes.RecipeProvider {

    /** 把本层配方改道到模块目录的路由器；{@code null} = 无层配置 → 221 条照旧落根（旧行为）。 */
    @Nullable
    private final LayerRecipeRouter router;

    /**
     * @param output      datagen 的根输出（{@code --output}）
     * @param registries  注册表查询
     * @param layerRoots  层名 → 该模块的 {@code src/generated/resources}；空表 = 不改道。
     *                    由集成层从系统属性 {@code coe.datagen.layerAssetRoots} 解析后传入
     *                    （路径是构建期知识，理由见 {@code CreateOreExpansionDatagen#layerAssetRoot}）。
     */
    public RecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                          Map<String, Path> layerRoots) {
        super(output, registries);
        this.router = layerRoots.isEmpty()
            ? null
            : new LayerRecipeRouter(output.getOutputFolder(), layerRoots, CoeCore.REGISTRY_NAMESPACE);
    }

    /**
     * 见类注释"P7d"：把根输出包一层改道缓存再交给基类（{@code DataProvider#run(CachedOutput)} 是
     * {@code final}，只有这个两参重载可覆写），并在全部写盘完成后补一次模块目录的 stale 清理。
     */
    @Override
    protected CompletableFuture<?> run(CachedOutput output, HolderLookup.Provider registries) {
        LayerRecipeRouter active = this.router;
        if (active == null) {
            return super.run(output, registries);
        }
        return super.run(active.wrap(output), registries).thenRun(active::purgeStale);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // 层顺序 = 拆分前逐字相同：拆磨（原版 4 组 + 本模组 5 组）→ 工具充能。
        // W6-c：第二段不再是"另一个模块的东西"——160 条工具充能配方的 type
        // （createoreexpansion:charging）随充能器进了 :coe，所以它的层名也从 "cews" 改成 "coe"，
        // 产物从 cews/src/generated 移到 coe/src/generated（配方 id 与文件名一字未改）。
        CoeRecipeProvider.generate(bind(output, "coe"));
        CoeChargingRecipeProvider.generate(bind(output, "coe"));
    }

    /**
     * 把 {@code delegate} 包成一个"本层"的 {@link RecipeOutput}：每次 {@code accept} 先<b>同步</b>
     * 记下那条配方的落盘路径属于哪一层，再交给 vanilla 的实现去写。
     *
     * <p><b>为什么必须这样</b>：{@code DataProvider.saveStable} 实测是
     * {@code CompletableFuture.runAsync(() -> output.writeIfNeeded(path, ...), backgroundExecutor)} ——
     * 写盘发生在后台线程、且并发，所以"当前层"既不能是字段也不能是 {@code ThreadLocal}。
     * 归属必须与那次 {@code accept} 成对地记在路径上（{@link LayerRecipeRouter#bind}），
     * 后台线程再用同一个 Path 取回。层由<b>调用点</b>显式给出，不是从路径推断——
     * 路径里也没有任何层的痕迹（两层命名空间都是 {@code createoreexpansion}）。</p>
     *
     * <p>{@code recipePathProvider} 是 vanilla 的 {@code protected} 字段，vanilla 的
     * {@code accept} 内部用<b>同一个</b> PathProvider 算路径，所以这里算出的 Path 与
     * {@code writeIfNeeded} 收到的那个<b>相等</b>（{@code Path} 按字符串比较）。</p>
     */
    private RecipeOutput bind(RecipeOutput delegate, String layer) {
        LayerRecipeRouter active = this.router;
        if (active == null || !active.isEnabled(layer)) {
            return delegate;
        }
        return new RecipeOutput() {
            @Override
            public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement,
                               ICondition... conditions) {
                active.bind(recipePathProvider.json(id), layer);
                delegate.accept(id, recipe, advancement, conditions);
            }

            @Override
            public Advancement.Builder advancement() {
                return delegate.advancement();
            }
        };
    }
}
