package com.hjmmd_8.createoreexpansion.data;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.LayerDataProvider;
import com.hjmmd_8.createoreexpansion.common.registry.LayerRegistrate;
import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsRegistrate;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRegistrate;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationRegistrate;
import com.hjmmd_8.createoreexpansion.data.lang.ChineseLangProvider;
import com.hjmmd_8.createoreexpansion.data.lang.EnglishLangProvider;
import com.hjmmd_8.createoreexpansion.data.lang.LayerLangSplitter;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * 数据生成入口（{@code runData}）。
 *
 * <p><b>P3b 之后它多了一件事：驱动三个 Registrate。</b></p>
 *
 * <p>背景：NeoForge 的 {@code DatagenModLoader} 会给<b>每个</b> mod 容器各建一个
 * {@code DataGenerator}，但只有 {@code --mod <id>} 指定的那个 mod 的生成器会被执行
 * （其余 {@code shouldExecute=false}，连 {@code generators} 列表都进不去；见
 * {@code GatherDataEvent.DataGeneratorConfig#makeGenerator}）。本工程的 datagen 固定以
 * {@code --mod createoreexpansion} 运行，所以 <b>COE / CEWS / TRANS 三个 Registrate 的数据提供器
 * 都必须挂在 createoreexpansion 的这个生成器上</b>，否则 CEWS 与 TRANS 两层的
 * 方块状态 / 物品模型 / 战利品表 / 标签会整片消失。</p>
 *
 * <p>三个 Registrate 自身不再自动挂提供器
 * （{@code common/registry/LayerRegistrate#onData} 是空实现，原因见其类注释），
 * 以保证"谁驱动 datagen"这件事只有<b>这一处</b>。</p>
 *
 * <p>P3e 起它还多担一件事：把<b>三层并集顺序与主层</b>注入 {@link LayerRegistrate}
 * （{@link LayerRegistrate#installOwnerChain}）。原先这三层实例是 {@code LayerRegistrate}
 * 自己去读的，那让一个 SHARED 基础设施反向依赖三个层专属类（将来进不了 core 库）；
 * 而"哪三层、谁是主层"本来就是集成层的知识，放在这里与上面三行 {@code attachDataGenerator} 同源。</p>
 *
 * <p>产物零变化：注册命名空间仍是 {@code createoreexpansion}；三层各写自己"路径带条目名"的产物
 * （方块状态 / 物品模型 / 战利品表 / 配方…），而<b>路径不含条目名</b>的<b>标签与语言</b>
 * 只在主层那个提供器里按 COE → CEWS → TRANS 的顺序跑三层并集——
 * 与前缀、路径、条目顺序都与拆分前的"单 Registrate"逐字节一致
 * （见 {@code LayerRegistrate#genData}）。</p>
 *
 * <p><b>P5：标签的"落点"分家</b>。并集照旧在<b>一个</b>提供器里组装完，但写盘时按
 * {@code values} 的逐元素归属改道：单层标签整份搬到该模块，跨层标签<b>按层各写一份</b>
 * （每份只含该层元素）。跨层能拆的前提是实测确认了 vanilla {@code TagLoader} 对同一数据包路径
 * 的多资源包是<b>累加合并</b>（{@code listMatchingResourceStacks} + 无 {@code "replace"}），
 * 不是"后者覆盖"——详见 {@code LayerDataProvider} 类注释"四"。</p>
 *
 * <p><b>P7d：367 条生成配方的落点分家</b>。配方<b>不由 Registrate 产出</b>
 * （{@code data/RecipeProvider} 才是唯一挂到生成器上的那个配方提供器），所以上面那套路径改写
 * 覆盖不到它们——P7c §5 实测三个模块 jar 的 {@code data/**&#47;recipe/**} 与根的 367 条交集 = 0。
 * 修法是把本类解析出的「层名 → 模块输出根」一并交给 {@code RecipeProvider}，由它在
 * {@code Coe/CewsRecipeProvider} 的调用点显式绑层（{@code LayerRecipeRouter}）。
 * 层不按路径推断：两层的配方路径都形如 {@code data/createoreexpansion/recipe/...}。</p>
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public class CreateOreExpansionDatagen {

    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        // —— 三层 Registrate 的 datagen：一层一个提供器，名字各不相同 ——
        // 详细理由见 common/registry/LayerRegistrate 类注释：
        //   * 三层提供器同名会让 DataGenerator.addProvider 直接抛 Duplicate provider；
        //   * 标签/语言的落盘路径不含条目名，三层各写一份会互相整文件覆盖，
        //     所以这两类只在主层（COE，layerId=null，名字与拆分前逐字相同）那个提供器里跑三层并集。
        // 层顺序 COE → CEWS → TRANS 与拆分前的注册触发顺序一致，保证条目顺序逐字节不变。
        //
        // P4a：第四个参数是本层模块的 src/generated/resources。本层的 blockstates/** 与
        // models/** 会被改写到那里；`data/**` 里的**标签**从 P5 起也按逐文件归属改道
        // （见下面 installTagRouter），非标签的 data 与 `assets/<ns>/lang/**` 仍留在根工程
        // （LANG 是三层并集、且与根工程 LanguageProvider 共写同一个 en_us.json）。
        // 路径由根 build.gradle 的 data run 通过系统属性 coe.datagen.layerAssetRoots 传来：
        // `coe=<abs>;cews=<abs>;transmutation=<abs>`。
        // 取不到时返回 null ⇒ 不改写（退化成 P4a 之前的行为，不会写错地方）。
        Path coeRoot = layerAssetRoot("coe");
        Path cewsRoot = layerAssetRoot("cews");
        Path transmutationRoot = layerAssetRoot("transmutation");

        LayerRegistrate.attachDataGenerator(CoeRegistrate.REGISTRATE, generator, CoeCore.REGISTRY_NAMESPACE, null, event, coeRoot);
        LayerRegistrate.attachDataGenerator(CewsRegistrate.REGISTRATE, generator, CoeCore.REGISTRY_NAMESPACE, "cews", event, cewsRoot);
        LayerRegistrate.attachDataGenerator(TransmutationRegistrate.REGISTRATE, generator, CoeCore.REGISTRY_NAMESPACE, "transmutation", event, transmutationRoot);

        // 层名 → 该模块 src/generated/resources。标签路由（P5）与 P7d 的配方路由共用这一张表。
        Map<String, Path> layerRoots = layerRoots(coeRoot, cewsRoot, transmutationRoot);

        // —— P5：标签按「逐文件归属」路由到各模块（见 LayerDataProvider 类注释"四"）——
        // 表用三层各自的 getAll(BLOCK / ITEM) 建，零手工维护（与 LayerLangSplitter 同一手法）。
        // 传的是**惰性查询**：真正的表在提供器运行时才构建（那时注册已完成，见 tagElementLayer）。
        // 与路由成对的另一处修改在 core 的 LayerDataProvider#purgeStale（跳过 /tags/ 与 /recipe/），
        // 不跳的话非主层提供器的 stale 清理会把主层刚路由进来的标签与配方删掉。
        LayerDataProvider.installTagRouter(CreateOreExpansionDatagen::tagElementLayer, layerRoots);

        // —— 三层并集顺序 + 主层（P3e 从 LayerRegistrate 挪到这里）——
        // LayerRegistrate 住 SHARED、将来要原样搬进 core 库，不许 import 任何层专属类；
        // 而"哪三层、谁是主层"本来就是驱动 datagen 的集成层才知道的事。
        // 顺序恒为 COE → CEWS → TRANS（标签/语言的条目顺序靠它），主层 = COE（命名空间代表）。
        LayerRegistrate.installOwnerChain(
            List.of(CoeRegistrate.REGISTRATE, CewsRegistrate.REGISTRATE, TransmutationRegistrate.REGISTRATE),
            CoeRegistrate.REGISTRATE);

        if (event.includeClient()) {
            generator.addProvider(true, new ChineseLangProvider(output));
            System.out.println("========== CreateOreExpansion ChineseLangProvider START ==========");
            generator.addProvider(true, new EnglishLangProvider(output));
            System.out.println("========== CreateOreExpansion EnglishLangProvider START ==========");

            // —— P4c：三层各自的 lang 子集，必须排在上面两份 LanguageProvider **之后** ——
            // 它只读根文件、只写模块侧（assets/<模块>/lang/**），从不写根路径，理由见类注释"二"。
            // 顺序是承重的：DataGenerator 按注册顺序串行跑提供器，而 saveStable 在 run 内同步落盘，
            // 所以轮到这里时根文件已经是本轮最终内容（值逐条复制）。
            generator.addProvider(true, new LayerLangSplitter(output, coeRoot, cewsRoot, transmutationRoot));
        }
        if (event.includeServer()) {
            // ── P7d：367 条生成配方按「产出它的 provider」改道到模块目录 ────────────────────
            // 配方不由 Registrate 产出（它们是本工程自己的 RecipeProvider：COE 172 条［含拆磨 61］
            // + 工具充能 195 条，W6-c 起两段都属 :coe），
            // 所以 LayerDataProvider 那套改道覆盖不到它们 —— P7c §5 实测：三个模块 jar 的
            // data/**/recipe/** 与根的 367 条交集 = 0，单装任一模块 jar 一条生成配方都没有。
            // 这里把「层名 → 模块输出根」交给它，由它按调用点（Coe/CewsRecipeProvider）逐层绑定；
            // 层绝不按路径推断（CEWS 的配方路径也是 data/createoreexpansion/recipe/...）。
            generator.addProvider(true, new RecipeProvider(output, event.getLookupProvider(), layerRoots));
        }
    }

    /**
     * 本层模块的 datagen 输出根（{@code <module>/src/generated/resources}），来自根
     * {@code build.gradle} 里 data run 的系统属性 {@code coe.datagen.layerAssetRoots}
     * （格式 {@code coe=<绝对路径>;cews=<绝对路径>;transmutation=<绝对路径>}）。
     *
     * <p>为什么用系统属性而不是在 Java 里推算：模块目录是**构建期**的知识（Gradle 才知道
     * 工程根在哪），这里的类只在 runData 里活着；写成"从 {@code --output} 上推三级目录"
     * 会在 {@code --output} 被改动时静默把文件写错地方。取不到属性时返回 {@code null}，
     * {@link LayerRegistrate#attachDataGenerator} 于是不改写路径——退化成"没分家"，
     * 不会写错。</p>
     */
    @Nullable
    private static Path layerAssetRoot(String module) {
        String spec = System.getProperty("coe.datagen.layerAssetRoots");
        if (spec == null) {
            return null;
        }
        for (String part : spec.split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0 && part.substring(0, eq).trim().equals(module)) {
                return Paths.get(part.substring(eq + 1).trim()).toAbsolutePath();
            }
        }
        return null;
    }

    /**
     * "层名 → 该模块的 {@code src/generated/resources}"；取不到的层不进表（那层的标签留根、
     * 那层产出的配方也留根）。标签路由（P5）与配方路由（P7d）共用这一张表。
     */
    private static Map<String, Path> layerRoots(@Nullable Path coeRoot, @Nullable Path cewsRoot,
                                                @Nullable Path transmutationRoot) {
        Map<String, Path> roots = new LinkedHashMap<>();
        if (coeRoot != null) {
            roots.put("coe", coeRoot);
        }
        if (cewsRoot != null) {
            roots.put("cews", cewsRoot);
        }
        if (transmutationRoot != null) {
            roots.put("transmutation", transmutationRoot);
        }
        return roots;
    }

    /**
     * P5 标签路由用的「注册 id → 层」表。惰性建：{@code LayerDataProvider} 在提供器运行时
     * 才第一次调用它，那时三层注册已经完成（{@link LayerLangSplitter} 用的是同一个时机）。
     *
     * <p>为什么是"查表函数"而不是"一张现成的表"：{@code LayerDataProvider} 住 {@code core}，
     * <b>不许出现任何层引用</b>（{@code tools/check-layering.ps1} 会抓），所以"有哪三层、
     * 每层的注册对象从哪来"只能是集成层的知识——本方法就是注入给它的那一半。</p>
     */
    @Nullable
    private static volatile Map<ResourceLocation, String> tagElementLayers;

    /** 见 {@link #tagElementLayers}。返回 {@code null} = 该 id 不归属任何层 → 整份标签留根。 */
    @Nullable
    private static String tagElementLayer(ResourceLocation id) {
        Map<ResourceLocation, String> table = tagElementLayers;
        if (table == null) {
            // 双检锁：提供器的写盘发生在 DataProvider.saveStable 的后台线程上（实测并发 14 个 worker
            // 同时进来），不锁的话表会被重复构建十几次——结果一样（纯函数），但日志会很吵。
            synchronized (CreateOreExpansionDatagen.class) {
                table = tagElementLayers;
                if (table == null) {
                    table = buildTagElementLayers();
                    tagElementLayers = table;
                }
            }
        }
        return table.get(id);
    }

    private static Map<ResourceLocation, String> buildTagElementLayers() {
        Map<ResourceLocation, String> layers = new LinkedHashMap<>();
        collectTagLayers(layers, "coe", CoeRegistrate.REGISTRATE);
        collectTagLayers(layers, "cews", CewsRegistrate.REGISTRATE);
        collectTagLayers(layers, "transmutation", TransmutationRegistrate.REGISTRATE);
        CoeCore.LOGGER.info("[Layer Tag Router] element table built: {} ids", layers.size());
        return layers;
    }

    /**
     * 一层的方块与物品注册 id 入表。撞 id 只记日志、先到先得（层顺序 COE → CEWS → TRANS）；
     * 实测三层 id 集合两两不交（标签引用的 139 个 id 里 0 歧义）。
     */
    private static void collectTagLayers(Map<ResourceLocation, String> layers, String layerId,
                                         AbstractRegistrate<?> registrate) {
        collectTagLayers(layers, layerId, registrate, Registries.BLOCK);
        collectTagLayers(layers, layerId, registrate, Registries.ITEM);
    }

    private static <R> void collectTagLayers(Map<ResourceLocation, String> layers, String layerId,
                                             AbstractRegistrate<?> registrate,
                                             ResourceKey<? extends Registry<R>> registry) {
        for (RegistryEntry<R, ?> entry : registrate.getAll(registry)) {
            ResourceLocation id = entry.getId();
            String previous = layers.putIfAbsent(id, layerId);
            if (previous != null && !previous.equals(layerId)) {
                CoeCore.LOGGER.warn("[Layer Tag Router] id '{}' claimed by both '{}' and '{}'; keeping '{}'",
                    id, previous, layerId, previous);
            }
        }
    }
}
