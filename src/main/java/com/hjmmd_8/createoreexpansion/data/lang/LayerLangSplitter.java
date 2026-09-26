package com.hjmmd_8.createoreexpansion.data.lang;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsRegistrate;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRegistrate;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationRegistrate;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * <b>P4c：把语言文件按模块再产出一次（"每层自己产出自己那份"）。</b>
 *
 * <p>它<b>不改</b>根工程的语言文件，只在每个模块的
 * {@code assets/<模块>/lang/<locale>.json} 下<b>新增</b>一份本模块拥有的键值子集。
 * 两级命名空间并存是有意的：根文件（{@code assets/createoreexpansion/lang/…}）与模块文件
 * （{@code assets/coe|cews|transmutation/lang/…}）路径不同，MC 的 {@code LanguageManager}
 * 会把所有命名空间下的 {@code assets/&lt;任意命名空间&gt;/lang/&lt;locale&gt;.json} 并进同一张
 * {@code 键 -&gt; 值} 表，所以<b>键一个都不用改</b>，合并后的结果与拆分前逐键相同。</p>
 *
 * <h2>一、归属靠"注册对象"，不靠字符串猜</h2>
 * <p>键到模块的映射由 {@code AbstractRegistrate#getAll(Registries.BLOCK / Registries.ITEM)}
 * 逐条取注册对象、再取 {@code getDescriptionId()} 建立（方块物品的 {@code Item#getDescriptionId()}
 * 本来就返回它那个方块的 {@code block.…} 键，所以两侧都不会错位）。
 * 这正是"能机械归属"的那一批；根文件里剩下的裸字符串键（{@code itemGroup.*}、tooltip、
 * 键位、技能名、药水效果…）没有机械归属，<b>只能留根</b>——本类不碰它们。</p>
 *
 * <h2>二、⛔ 根文件一个字节都不许动（这是上一轮踩出来的坑）</h2>
 * <p>直觉上更干净的做法是"把已归层的键从根文件里删掉"，但那会<b>静默损坏产物</b>：
 * {@code HashCache} 记的是<b>提供器自己的哈希</b>，它看不到"别的提供器（本类）随后重写了这个
 * 同一路径的文件"；下一轮 {@code runData} 里 {@code LanguageProvider} 的重写会被哈希命中而
 * <b>跳过</b>，根文件就永远停在"已拆走"的形态，而模块侧生成目录一旦被删，那些键
 * <b>再也补不回来</b>（datagen 不自愈）。另外<b>同一轮里对同一路径的第二次写本来就不可靠</b>
 * （实测 en_us 的改写落地、同一轮的 zh_cn 改写没落地，要跑第二轮才收敛）。</p>
 * <p>所以本类<b>只写模块侧、从不写根路径</b>：等价性是构造性的（值逐条从根文件复制），
 * 也不会与任何提供器争同一路径。代价是那批键在根与模块各存一份——可接受，
 * lang 是覆盖式合并、同值不冲突。</p>
 *
 * <h2>三、为什么必须换资源命名空间</h2>
 * <p>只换工程根（{@code coe/src/generated/resources}）而不换命名空间，会让多个 mod 携带
 * <b>同一路径</b> {@code assets/createoreexpansion/lang/en_us.json}，资源包里同路径只有一个
 * 胜出者 ⇒ <b>随机遮蔽、丢一半键</b>。换命名空间后路径互不相同，MC 却仍把它们并进同一张表。</p>
 *
 * <h2>四、读值的方式与执行时机</h2>
 * <p>它在两份根 {@code LanguageProvider} <b>之后</b>注册（见
 * {@code data/CreateOreExpansionDatagen#includeClient}）。{@code DataGenerator} 是
 * <b>按注册顺序逐个跑</b>提供器的（日志里 {@code Starting provider:} 是串行的），而
 * {@code DataProvider.saveStable} 在 {@code run} 内部就同步落盘，所以本类开始时根文件已经写完，
 * 直接读盘即可拿到本轮的最终值（"值逐条复制"就是字面意思）。</p>
 *
 * <p>本类只处理 {@code en_us} 与 {@code zh_cn}：{@code en_ud} 是 Registrate 按三层并集
 * 自动倒写的、不属于任何一层，原样留在根工程。</p>
 */
public class LayerLangSplitter implements DataProvider {

    /** 逐层处理的 locale（{@code en_ud} 刻意不在列，见类注释"四"）。 */
    private static final List<String> LOCALES = List.of("en_us", "zh_cn");

    /** 根工程的语言目录（{@code <--output>/assets/<命名空间>/lang}），只读。 */
    private final Path rootLangDir;

    /** 三层：模块 id（= 资源命名空间）→ Registrate → 该模块的 datagen 输出根。 */
    private final List<Layer> layers;

    /**
     * @param output            根工程的 datagen 输出（{@code --output}）
     * @param coeRoot           {@code coe/src/generated/resources}；{@code null} = 不产出该层（只警告）
     * @param cewsRoot          {@code cews/src/generated/resources}；同上
     * @param transmutationRoot {@code transmutation/src/generated/resources}；同上
     */
    public LayerLangSplitter(PackOutput output, @Nullable Path coeRoot, @Nullable Path cewsRoot,
                             @Nullable Path transmutationRoot) {
        this.rootLangDir = output.getOutputFolder()
            .resolve("assets").resolve(CoeCore.REGISTRY_NAMESPACE).resolve("lang");
        this.layers = List.of(
            new Layer("coe", CoeRegistrate.REGISTRATE, coeRoot),
            new Layer("cews", CewsRegistrate.REGISTRATE, cewsRoot),
            new Layer("transmutation", TransmutationRegistrate.REGISTRATE, transmutationRoot));
    }

    @Override
    public String getName() {
        return "Layer Lang Splitter";
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Map<String, String> owners = buildOwnerTable();
        List<CompletableFuture<?>> writes = new ArrayList<>();

        for (String locale : LOCALES) {
            Map<String, String> root = readRootLang(rootLangDir.resolve(locale + ".json"));
            if (root == null) {
                continue;
            }

            // 逐层收集本层拥有的键；键排序固定（TreeMap）保证产物逐字节稳定。
            Map<String, TreeMap<String, String>> buckets = new LinkedHashMap<>();
            for (Layer layer : layers) {
                buckets.put(layer.id, new TreeMap<>());
            }
            int owned = 0;
            for (Map.Entry<String, String> entry : root.entrySet()) {
                String owner = owners.get(entry.getKey());
                if (owner == null || !buckets.containsKey(owner)) {
                    continue;
                }
                buckets.get(owner).put(entry.getKey(), entry.getValue());
                owned++;
            }

            StringBuilder summary = new StringBuilder();
            for (Layer layer : layers) {
                if (summary.length() > 0) {
                    summary.append(", ");
                }
                summary.append(layer.id).append('=').append(buckets.get(layer.id).size());
            }
            CoeCore.LOGGER.info("[Layer Lang Splitter] {}: root keys={}, assigned={}, buckets=[{}], left-in-root={}",
                locale, root.size(), owned, summary, root.size() - owned);

            for (Layer layer : layers) {
                TreeMap<String, String> subset = buckets.get(layer.id);
                if (subset.isEmpty()) {
                    continue;
                }
                if (layer.assetRoot == null) {
                    CoeCore.LOGGER.warn("[Layer Lang Splitter] {}: module root unknown (system property "
                        + "coe.datagen.layerAssetRoots missing '{}') -> {} keys NOT written",
                        locale, layer.id, subset.size());
                    continue;
                }
                Path target = layer.assetRoot
                    .resolve("assets").resolve(layer.id).resolve("lang").resolve(locale + ".json");
                JsonObject json = new JsonObject();
                subset.forEach(json::addProperty);
                writes.add(DataProvider.saveStable(cache, json, target));
                CoeCore.LOGGER.info("[Layer Lang Splitter] wrote {} -> keys={}", target, subset.size());
            }
        }

        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    /**
     * 建"语言键 → 模块"表：三层各自的 {@code getAll(Registries.BLOCK)} 与
     * {@code getAll(Registries.ITEM)}，逐条取 {@code getDescriptionId()}。
     * 先到先得（顺序 COE → CEWS → TRANS），撞键只记警告——实测三层键集两两不交。
     */
    private Map<String, String> buildOwnerTable() {
        Map<String, String> owners = new LinkedHashMap<>();
        Map<String, Integer> perLayer = new LinkedHashMap<>();
        for (Layer layer : layers) {
            int before = owners.size();
            collect(owners, layer.id, layer.registrate, Registries.BLOCK, Block::getDescriptionId);
            collect(owners, layer.id, layer.registrate, Registries.ITEM, Item::getDescriptionId);
            perLayer.put(layer.id, owners.size() - before);
        }
        CoeCore.LOGGER.info("[Layer Lang Splitter] owner table: {} keys ({})", owners.size(), perLayer);
        return owners;
    }

    private static <R> void collect(Map<String, String> owners, String layerId, AbstractRegistrate<?> registrate,
                                    ResourceKey<? extends Registry<R>> registry,
                                    Function<R, String> descriptionId) {
        for (RegistryEntry<R, ?> entry : registrate.getAll(registry)) {
            String key = descriptionId.apply(entry.get());
            String previous = owners.putIfAbsent(key, layerId);
            if (previous != null && !previous.equals(layerId)) {
                CoeCore.LOGGER.warn("[Layer Lang Splitter] key '{}' is claimed by both '{}' and '{}'; keeping '{}'",
                    key, previous, layerId, previous);
            }
        }
    }

    /**
     * 读根工程本次生成的语言文件（{@code null} = 不存在，调用方跳过该 locale）。
     * 读盘而不是读提供器内存是刻意的：{@code DataProvider.saveStable} 在
     * {@code LanguageProvider#run} 内部同步落盘，本提供器排在它之后（见类注释"四"）。
     */
    @Nullable
    private static Map<String, String> readRootLang(Path path) {
        if (!Files.isRegularFile(path)) {
            CoeCore.LOGGER.warn("[Layer Lang Splitter] root language file not found, locale skipped: {}", path);
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new IllegalStateException("not a JSON object: " + path);
            }
            Map<String, String> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : parsed.getAsJsonObject().entrySet()) {
                map.put(e.getKey(), e.getValue().getAsString());
            }
            return map;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + path, e);
        }
    }

    /** 一层：模块 id（= 资源命名空间）、它的 Registrate、它的 datagen 输出根。 */
    private record Layer(String id, AbstractRegistrate<?> registrate, @Nullable Path assetRoot) {}
}
