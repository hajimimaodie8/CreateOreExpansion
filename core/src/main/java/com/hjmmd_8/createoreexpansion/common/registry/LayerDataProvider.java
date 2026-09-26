package com.hjmmd_8.createoreexpansion.common.registry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.RegistrateDataProvider;

import net.minecraft.data.CachedOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * 给"同一个命名空间下的多个 Registrate"用的数据提供器。它做两件事：
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
 * <h2>二、P4a：把本层的 assets 产物改写到本模块的 {@code src/generated/resources}</h2>
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
 * {@code DataProvider.saveStable} 把最终 {@code Path} 交给 {@code CachedOutput#writeIfNeeded}，
 * 所以这里覆写 {@link #run(CachedOutput)}，用一个<b>把路径从根输出换到本层输出</b>的
 * {@link CachedOutput} 包一层再交给基类。被改写的只有两类路径：</p>
 * <ul>
 *   <li>{@code assets/<命名空间>/blockstates/**}</li>
 *   <li>{@code assets/<命名空间>/models/**}</li>
 * </ul>
 * <p>其余一律原样落在根工程的 {@code src/generated/resources}：
 * {@code lang/**}（三层并集 + 与根工程 LanguageProvider 共写同一文件）、
 * {@code data/**}（本轮范围只有 assets；标签/配方的布局不在这一轮改）。</p>
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
 */
public class LayerDataProvider extends RegistrateDataProvider {

    private static final Logger LOGGER = LogUtils.getLogger();

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
            layerId == null ? "coe" : layerId,
            "assets/" + namespace + "/blockstates/", "assets/" + namespace + "/models/");
        return super.run(relocating).thenRun(relocating::purgeStale);
    }

    /** 把"本层资产的落盘路径"从根输出根换到本层输出根，其余路径原样透传。 */
    private static final class LayerAssetRelocatingCache implements CachedOutput {

        private final CachedOutput delegate;
        private final Path rootOutput;
        private final Path layerAssetRoot;
        private final String label;
        private final String[] relocatingPrefixes;
        /** 本次运行里本层真正产出的资产路径（改写后的）。见 {@link #purgeStale()}。 */
        private final Set<Path> produced = ConcurrentHashMap.newKeySet();

        LayerAssetRelocatingCache(CachedOutput delegate, Path rootOutput, Path layerAssetRoot, String label, String... relocatingPrefixes) {
            this.delegate = delegate;
            this.rootOutput = rootOutput;
            this.layerAssetRoot = layerAssetRoot;
            this.label = label;
            this.relocatingPrefixes = relocatingPrefixes;
        }

        @Override
        public void writeIfNeeded(Path path, byte[] data, com.google.common.hash.HashCode hashCode) throws IOException {
            Path target = relocate(path);
            if (target != path) {
                produced.add(target);
            }
            delegate.writeIfNeeded(target, data, hashCode);
        }

        private Path relocate(Path path) {
            if (!path.startsWith(rootOutput)) {
                return path;
            }
            Path relative = rootOutput.relativize(path);
            String key = relative.toString().replace('\\', '/');
            for (String prefix : relocatingPrefixes) {
                if (key.startsWith(prefix)) {
                    return layerAssetRoot.resolve(relative);
                }
            }
            return path;
        }

        /**
         * 类注释"三"：{@code HashCache.purgeStaleAndWrite} 只遍历根输出目录，
         * 模块目录不在它的视野里，所以"某个方块被删掉之后它留下的方块状态"不会被自动清掉
         * （拆分前会）。这里补上与它<b>同语义</b>的一次清理：只在本层这次真的产出了资产时执行，
         * 只在 {@code assets/<ns>/blockstates|models} 这两个改写子树里走，删掉本次没产出的文件。
         * 目录清理失败只记日志、不抛（与上游 purgeStaleAndWrite 同口径）。
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
}
