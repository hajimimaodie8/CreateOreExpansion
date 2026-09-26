package com.hjmmd_8.createoreexpansion.common.registry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.RegistrateDataProvider;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * 给"同一个命名空间下的多个 Registrate"用的数据提供器。它做三件事：
 *
 * <h2>一、给提供器名字加一个层标签</h2>
 * {@code RegistrateDataProvider#getName()} 是
 * {@code "Registrate Provider for " + modid + " [" + 各子提供器名字 + "]"}，而三层 Registrate 的
 * modid 与子提供器集合<b>完全相同</b>（这是刻意的：命名空间必须都是 {@code createoreexpansion}）
 * → 三个提供器<b>同名</b>；而 {@code DataGenerator.addProvider} 对重名提供器是<b>直接抛异常</b>
 * （{@code IllegalStateException: Duplicate provider: ...}，实测 runData 第二行就炸）。
 * 条目落盘的路径由各子提供器自己决定（与这个名字无关），所以这里改的只是"标签"。
 * COE（{@code layerId == null}）刻意保持与拆分前<b>逐字相同</b>的提供器名字，
 * 连 {@code .cache} 的文件名都不变。
 *
 * <h2>二、P4a/P4b/P5：把本层产物改写到本模块的 {@code src/generated/resources}</h2>
 * <p><b>要解决的问题</b>：datagen 的 {@code --output} 只有一个根（根工程的
 * {@code src/generated/resources}），三层 Registrate 的子提供器全都拿到同一个
 * {@code PackOutput}，于是三层的方块状态 / 物品模型始终落在根工程里——"资源按模块分家"这件事
 * 靠改 {@code --output} 做不到，因为 NeoForge 只给 {@code --mod} 指定的那个 mod 跑生成器，
 * 而三层的注册命名空间<b>必须</b>都叫 {@code createoreexpansion}。</p>
 *
 * <p><b>为什么不是"给每层一个自己的 PackOutput"</b>（这条路试过、走不通）：
 * 子提供器是由 {@code ProviderType.create(...)} 在基类构造器里按
 * {@code parent.getDataGenInitializer().getSortedProviders()} 一次性建好的，而
 * {@code getSortedProviders()} 是 {@code protected}（跨包不可见），
 * {@code ProviderType.Context} 里那个 {@code output} 参数没有第二条公开注入路径。
 * 让整层共用一个改过的 {@code PackOutput} 也不行：{@code LANG} 的落盘路径不含条目名、
 * 是三层并集（P3b 起的硬规则），而且根工程的两个 {@code LanguageProvider}
 * <b>不合并</b>、是直接覆盖同一个 {@code en_us.json}——把 LANG 挪进某一层会让最终
 * {@code en_us.json} 少掉一半键。</p>
 *
 * <p><b>实际做法：只改写落盘路径，不碰提供器的构造</b>。
 * {@code DataProvider.saveStable} 把最终 {@code Path} 与<b>最终 JSON 字节</b>一起交给
 * {@code CachedOutput#writeIfNeeded}（已核源码：{@code DataProvider.saveStable} 用
 * {@code JsonWriter} + {@code GsonHelper.writeValue(writer, json, KEY_COMPARATOR)} 序列化完
 * 才调用 {@code writeIfNeeded}），所以这里覆写 {@link #run(CachedOutput)}，用一个
 * <b>把路径从根输出换到本层输出</b>的 {@link CachedOutput} 包一层再交给基类。
 * 被改写的路径（P4a 加前两条，P4b 加第三条，P5 改第三条的语义）：</p>
 * <ul>
 *   <li>{@code assets/<命名空间>/blockstates/**}</li>
 *   <li>{@code assets/<命名空间>/models/**}</li>
 *   <li>{@code data/**}&nbsp;——含 {@code /tags/} 段的路径走<b>逐文件归属路由</b>（见"四"）</li>
 * </ul>
 * <p>语言不在这里改写：{@code assets/<命名空间>/lang/**} 是三层并集、且与根工程的两个
 * {@code LanguageProvider} 共写同一个 {@code en_us.json}，所以原样留在根工程的
 * {@code src/generated/resources}；模块侧那份子集由集成层的
 * {@code data/lang/LayerLangSplitter} 另写（路径不同，不冲突）。</p>
 *
 * <p>副产物三点，都已在实现里考虑：① 传给 {@code CachedOutput} 的是<b>改写后</b>的路径，
 * 所以 {@code .cache} 里记的也是改写后的路径；② {@code HashCache.purgeStaleAndWrite} 只遍历根输出
 * 目录，模块目录里的旧文件不会被它删（见下"三"）；③ 未配置层输出根时
 * （{@link #layerAssetRoot} 为 {@code null}）行为与 P4a 之前<b>逐字节相同</b>——
 * 缺配置只会"没分家"，不会写错地方。</p>
 *
 * <h2>三、"模块目录里的 stale 清理"是补出来的，以及一个已知的无害副作用</h2>
 * <p>{@code HashCache} 把路径按 {@code rootDir.relativize(path)} 存进 {@code .cache}，读回时用
 * {@code rootDir.resolve(...)}——对<b>根输出目录之外</b>的路径这一步不能往返
 * （存进去是 {@code ../../../coe/src/generated/resources/...}，读出来带 {@code ..} 的 Path
 * 不等于提供器给的规范化 Path）。两个后果：</p>
 * <ul>
 *   <li><b>无害但可见</b>：改写过的那些资产每次 runData 都会被重写一遍
 *       （{@code HashCache} 打印 {@code written: 238} 而不是 0）。内容逐字节相同，
 *       {@code git status} 干净；判据是"内容"，不是这个计数。</li>
 *   <li><b>必须补</b>：{@code purgeStaleAndWrite} 用 {@code Files.walkFileTree(rootDir, ...)}
 *       找 stale 文件，模块目录<b>不在它的视野里</b>——某个方块被删掉之后，它的方块状态不会
 *       再被自动清掉（拆分前会）。所以这里在 {@code run} 完成后补一次同语义的清理：
 *       只在本层这次真的产出了资产（{@code produced} 非空）时执行，只在两个改写子树里走，
 *       删掉本次没产出的文件；删除失败只记日志不抛（与上游同口径）。</li>
 * </ul>
 *
 * <h2>四、P5：标签按「逐文件归属」路由（含跨层文件按层各写一份）</h2>
 * <p><b>为什么标签不能像资产那样按路径前缀判层</b>：标签路径形如
 * {@code data/<标签命名空间>/tags/<注册表>/<路径>.json}，<b>里面既没有条目名、也没有层的任何痕迹</b>
 * （{@code data/c/tags/item/ingots.json} 的 {@code ingots.json} 是标签路径、不是注册名）。
 * 所以判定只能读<b>内容</b>：解析 {@code values} 数组，逐元素取 {@code namespace:path} 查一张
 * 「注册 id → 层」表。</p>
 *
 * <p><b>跨层文件为什么可以拆成"每层各写一份"</b>（这是本轮实测的结论，不是推断）：
 * vanilla {@code TagLoader#load} 用的是
 * {@code filetoidconverter.listMatchingResourceStacks(resourceManager)}（=
 * {@code ResourceManager#listResourceStacks}，返回 {@code Map<ResourceLocation, List<Resource>>}，
 * 遍历<b>所有</b>资源包）而不是 {@code listResources}（只取最高优先级那一份），然后
 * {@code for (Resource resource : entry.getValue()) { ... list.add(...) }} 逐份<b>累加</b>；
 * 只有文件里写了 {@code "replace": true} 才会先 {@code list.clear()}。
 * 本仓 108 个生成标签 {@code "replace"} 命中为 0 ⇒ 同一个数据包路径在两个 mod 文件里各有一份时
 * 是<b>并集合并</b>。因此</p>
 * <ul>
 *   <li><b>恰好 1 层</b> → 整份文件原字节搬到该模块根（{@code git} 记成纯 rename，字节零改动）；</li>
 *   <li><b>≥2 层</b> → <b>按层各写一份</b>，每份只含该层自己的 {@code values}，运行期由游戏合并回并集。
 *       这样"只装 CEWS 单模块 jar"的玩家才不会丢 {@code mineable/**}（缺失 = 工具判定不通过 = 真玩法 bug）；</li>
 *   <li><b>含外部命名空间、或表里查不到的 id</b> → <b>整份留根</b>（无法机械归属的东西不许拆）。</li>
 * </ul>
 *
 * <p><b>「注册 id → 层」表由集成层注入</b>（{@link #installTagRouter}，形态照
 * {@code LayerRegistrate#installOwnerChain}）：本类住 {@code core}，<b>不许出现任何层引用</b>
 * （{@code tools/check-layering.ps1} 会抓）。表由 {@code CreateOreExpansionDatagen} 用三层各自的
 * {@code Registrate#getAll(BuiltInRegistries.BLOCK / ITEM)} 建，零手工维护——
 * 与 {@code data/lang/LayerLangSplitter} 同一手法。传入的是<b>惰性查询</b>：表在提供器运行时
 * 才真正构建（那时注册已完成）。未注入（{@code tagElementLayer == null}）时标签一律留根，
 * 退化成 P5 之前的行为。</p>
 *
 * <p><b>⚠️ 与路由成对的那个修改：{@code purgeStale} 必须跳过 {@code /tags/}</b>。
 * 提供器执行顺序恒为 COE → CEWS → TRANS，而<b>所有</b>标签（含跨层文件的 CEWS 那半）都由主层
 * COE 那个提供器写；CEWS 提供器的 {@code purgeStale} 会遍历
 * {@code cews/src/generated/resources/data/}，此时它的 {@code produced} 非空（有自己的战利品表），
 * 于是会把 COE 刚路由进它名下的标签<b>当垃圾删掉</b>。语义上豁免完全正当：标签只由主层提供器产出，
 * 非主层提供器的 {@code produced} 对 {@code /tags/} 永远不权威。代价要写明并接受：
 * <b>模块目录里的标签失去自动 stale 清理</b>（某天删掉一个标签注册，旧文件会留下）。</p>
 *
 * <p>另外：本路由做在 {@code writeIfNeeded} <b>之内</b>而不是做成"后置拷贝 pass"，
 * 是因为标签必须<b>从不在根落盘</b>——若先写根、再把根删掉，下一轮 {@code HashCache} 会以为
 * "这个根路径我写过、哈希没变"而跳过重写，模块侧一删那些标签就<b>再也补不回来</b>
 * （{@code LayerLangSplitter} 那个坑的反面；lang 能那么做是因为根文件<b>保留</b>、是超集）。</p>
 */
public class LayerDataProvider extends RegistrateDataProvider {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 由集成层注入的「注册 id → 层」查询；{@code null} = 未注入 → 标签一律留根。
     *
     * <p>刻意是 {@link Function}（而不是一张现成的表）：表的构建要等注册完成，
     * 而本类住 {@code core}、不许知道"有哪三层、每层怎么取注册对象"。集成层传进来的实现
     * 自己惰性建表。</p>
     */
    @Nullable
    private static Function<ResourceLocation, String> tagElementLayer;

    /** 由集成层注入的「层 → 该模块的 {@code src/generated/resources}」；空表 = 不路由标签。 */
    private static Map<String, Path> tagLayerRoots = Map.of();

    private final String layerId;

    /** 注册命名空间（恒为 {@code createoreexpansion}）：改写路径时的 {@code assets/<namespace>/} 前缀。 */
    private final String namespace;

    /** 根工程的 datagen 输出根（{@code --output}）；{@code null} = 不启用路径改写。 */
    @Nullable
    private final Path rootOutput;

    /** 本层模块的 datagen 输出根（{@code <module>/src/generated/resources}）。 */
    @Nullable
    private final Path layerAssetRoot;

    /**
     * @param parent         目标 Registrate
     * @param namespace      注册命名空间（恒为 {@code createoreexpansion}）
     * @param layerId        层标签（{@code cews} / {@code transmutation}）；{@code null} = 不加前缀（COE）
     * @param event          datagen 事件
     * @param layerAssetRoot 本层模块的 {@code src/generated/resources}；{@code null} = 不改写（旧行为）
     */
    public LayerDataProvider(AbstractRegistrate<?> parent, String namespace, @Nullable String layerId,
                             GatherDataEvent event, @Nullable Path layerAssetRoot) {
        super(parent, namespace, event);
        this.layerId = layerId;
        this.namespace = namespace;
        this.rootOutput = event.getGenerator().getPackOutput().getOutputFolder();
        this.layerAssetRoot = layerAssetRoot;
    }

    /**
     * 注入标签路由（见类注释"四"）。由集成层（{@code data/CreateOreExpansionDatagen}）调用；
     * 必须在任何提供器 {@code run} 之前调用。不调用 = 标签一律留根（退化成 P5 之前的行为）。
     *
     * @param elementLayer 「注册 id → 层名」查询；返回 {@code null} = 该 id 不归属任何层
     * @param layerRoots   层名 → 该模块的 {@code src/generated/resources}（缺某一层 = 该层的标签留根）
     */
    public static void installTagRouter(Function<ResourceLocation, String> elementLayer, Map<String, Path> layerRoots) {
        tagElementLayer = elementLayer;
        tagLayerRoots = Map.copyOf(layerRoots);
    }

    @Override
    public String getName() {
        return layerId == null ? super.getName() : "[layer " + layerId + "] " + super.getName();
    }

    /**
     * 见类注释"二"：只包一层 {@link CachedOutput}，基类的提供器集合、执行顺序、
     * 哈希缓存语义全部不变。跑完之后补一次"模块目录内的 stale 清理"（见类注释"三"）。
     */
    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        if (rootOutput == null || layerAssetRoot == null) {
            return super.run(cache);
        }
        LayerAssetRelocatingCache relocating = new LayerAssetRelocatingCache(cache, rootOutput, layerAssetRoot,
            layerId == null ? "coe" : layerId, namespace,
            "assets/" + namespace + "/blockstates/", "assets/" + namespace + "/models/",
            "data/");
        return super.run(relocating).thenRun(relocating::purgeStale);
    }

    /** 把"本层资产的落盘路径"从根输出根换到本层输出根，其余路径原样透传。 */
    private static final class LayerAssetRelocatingCache implements CachedOutput {

        private final CachedOutput delegate;
        private final Path rootOutput;
        private final Path layerAssetRoot;
        private final String label;
        private final String namespace;
        private final String[] relocatingPrefixes;
        /** 本次运行里本层真正产出的资产路径（改写后的）。见 {@link #purgeStale()}。 */
        private final Set<Path> produced = ConcurrentHashMap.newKeySet();

        LayerAssetRelocatingCache(CachedOutput delegate, Path rootOutput, Path layerAssetRoot, String label,
                                  String namespace, String... relocatingPrefixes) {
            this.delegate = delegate;
            this.rootOutput = rootOutput;
            this.layerAssetRoot = layerAssetRoot;
            this.label = label;
            this.namespace = namespace;
            this.relocatingPrefixes = relocatingPrefixes;
        }

        @Override
        public void writeIfNeeded(Path path, byte[] data, com.google.common.hash.HashCode hashCode) throws IOException {
            Map<Path, byte[]> targets = relocate(path, data);
            if (targets == null) {
                delegate.writeIfNeeded(path, data, hashCode);
                return;
            }
            for (Map.Entry<Path, byte[]> target : targets.entrySet()) {
                Path out = target.getKey();
                byte[] payload = target.getValue();
                if (!out.equals(path)) {
                    produced.add(out);
                }
                // 只有"按层拆分"会换字节；拆分件用自己那一份的哈希（与 saveStable 同算法），
                // 否则 HashCache 会拿整份文件的哈希去比。未换字节时原样透传提供器的哈希。
                delegate.writeIfNeeded(out, payload, payload == data ? hashCode : Hashing.sha1().hashBytes(payload));
            }
        }

        /**
         * 计算一次写的全部落点（见类注释"二"/"四"）。
         *
         * @param data {@code DataProvider.saveStable} 传来的<b>最终 JSON 字节</b>
         * @return {@code null} = 不改写（按原路径、原字节透传）；否则"目标路径 → 目标要写的字节"
         *         （资产与单层标签恒为一条；跨层标签是"每层一条"）
         */
        @Nullable
        private Map<Path, byte[]> relocate(Path path, byte[] data) throws IOException {
            if (!path.startsWith(rootOutput)) {
                return null;
            }
            Path relative = rootOutput.relativize(path);
            String key = slash(relative);
            // P5: TAGS are routed per file by CONTENT (they are never relocated by path prefix —
            // a tag path carries no entry name). See the class javadoc section 4.
            if (key.contains("/tags/")) {
                return relocateTag(relative, data);
            }
            for (String prefix : relocatingPrefixes) {
                if (key.startsWith(prefix)) {
                    return Map.of(layerAssetRoot.resolve(relative), data);
                }
            }
            return null;
        }

        /**
         * 标签的逐文件归属路由（类注释"四"）：解析 {@code values}，逐元素查注入的「id → 层」表。
         *
         * @return {@code null} = 留根（未注入表 / 不是可解析的标签 / 含有无法归属的元素）
         */
        @Nullable
        private Map<Path, byte[]> relocateTag(Path relative, byte[] data) throws IOException {
            Function<ResourceLocation, String> lookup = tagElementLayer;
            if (lookup == null || tagLayerRoots.isEmpty()) {
                return null;
            }
            JsonObject root = parseObject(data);
            if (root == null) {
                return null;
            }
            JsonElement rawValues = root.get("values");
            if (rawValues == null || !rawValues.isJsonArray()) {
                return null;
            }
            JsonArray values = rawValues.getAsJsonArray();
            if (values.isEmpty()) {
                return null;
            }

            // 逐元素归桶，保持原始顺序（标签顺序是玩家可观测的，也是逐字节比对的依据）。
            // 出现非本模组命名空间、或表里查不到的 id -> 整份留根：无法机械归属的东西不许拆。
            Map<String, JsonArray> buckets = new LinkedHashMap<>();
            for (JsonElement element : values) {
                ResourceLocation id = elementId(element);
                if (id == null || !id.getNamespace().equals(namespace)) {
                    return null;
                }
                String layer = lookup.apply(id);
                if (layer == null || !tagLayerRoots.containsKey(layer)) {
                    return null;
                }
                buckets.computeIfAbsent(layer, k -> new JsonArray()).add(element);
            }

            if (buckets.size() == 1) {
                // 单层：整份文件原字节搬走（git 记成纯 rename，字节零改动）。
                return Map.of(tagLayerRoots.get(buckets.keySet().iterator().next()).resolve(relative), data);
            }

            // ≥2 层：按层各写一份，每份只含该层自己的 values。
            // 运行期由 TagLoader 的多资源包"累加"语义合并回并集（见类注释"四"）。
            Map<Path, byte[]> targets = new LinkedHashMap<>();
            for (Map.Entry<String, JsonArray> bucket : buckets.entrySet()) {
                JsonObject subset = root.deepCopy();
                subset.add("values", bucket.getValue());
                targets.put(tagLayerRoots.get(bucket.getKey()).resolve(relative), serialize(subset));
            }
            return targets;
        }

        /**
         * 类注释"三"：{@code HashCache.purgeStaleAndWrite} 只遍历根输出目录，
         * 模块目录不在它的视野里，所以"某个方块被删掉之后它留下的方块状态"不会被自动清掉
         * （拆分前会）。这里补上与它<b>同语义</b>的一次清理：只在本层这次真的产出了资产时执行，
         * 只在 {@code assets/<ns>/blockstates|models} 与 {@code data/} 这些改写子树里走，
         * 删掉本次没产出的文件。
         *
         * <p><b>⚠️ 必须跳过 {@code /tags/}</b>（类注释"四"）：跨层标签里属于别的层的那一份
         * 由主层（COE）提供器写进本模块目录，本提供器的 {@code produced} 对它永远不权威，
         * 不跳过就会把刚路由进来的标签当 stale 删掉。</p>
         *
         * <p>目录清理失败只记日志、不抛（与上游 purgeStaleAndWrite 同口径）。</p>
         */
        private void purgeStale() {
            if (produced.isEmpty()) {
                return;
            }
            for (String prefix : relocatingPrefixes) {
                Path dir = layerAssetRoot.resolve(prefix);
                if (!Files.isDirectory(dir)) {
                    continue;
                }
                try (Stream<Path> walk = Files.walk(dir)) {
                    for (Path file : walk.filter(Files::isRegularFile).toList()) {
                        if (slash(layerAssetRoot.relativize(file)).contains("/tags/")) {
                            continue;
                        }
                        if (!produced.contains(file)) {
                            try {
                                Files.delete(file);
                                LOGGER.info("[layer {}] removed stale generated asset {}", label, file);
                            } catch (IOException e) {
                                LOGGER.warn("Failed to delete stale generated asset {}", file, e);
                            }
                        }
                    }
                } catch (IOException e) {
                    LOGGER.warn("Failed to walk {} for stale generated assets", dir, e);
                }
            }
        }
    }

    /** 标签元素 → 注册 id：支持 {@code "ns:path"}、{@code "#ns:path"}（嵌套标签）与 {@code {"id": ...}} 三种写法。 */
    @Nullable
    private static ResourceLocation elementId(JsonElement element) {
        String raw;
        if (element.isJsonPrimitive()) {
            raw = element.getAsString();
        } else if (element.isJsonObject()) {
            JsonElement id = element.getAsJsonObject().get("id");
            if (id == null || !id.isJsonPrimitive()) {
                return null;
            }
            raw = id.getAsString();
        } else {
            return null;
        }
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        }
        if (raw.isEmpty()) {
            return null;
        }
        return ResourceLocation.tryParse(raw);
    }

    @Nullable
    private static JsonObject parseObject(byte[] data) {
        try {
            JsonElement parsed = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 与 {@code DataProvider.saveStable} <b>逐字节同一套</b>序列化（同一 {@code JsonWriter}
     * 参数、同一 {@code GsonHelper.writeValue} + {@code KEY_COMPARATOR}），
     * 这样拆分件与提供器直接产出的文件格式一致。
     */
    private static byte[] serialize(JsonElement json) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(bytes, StandardCharsets.UTF_8))) {
            writer.setSerializeNulls(false);
            writer.setIndent(" ".repeat(Math.max(0, DataProvider.INDENT_WIDTH.get())));
            GsonHelper.writeValue(writer, json, DataProvider.KEY_COMPARATOR);
        }
        return bytes.toByteArray();
    }

    private static String slash(Path path) {
        return path.toString().replace('\\', '/');
    }
}
