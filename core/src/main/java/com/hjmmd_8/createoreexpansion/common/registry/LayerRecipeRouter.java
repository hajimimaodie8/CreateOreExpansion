package com.hjmmd_8.createoreexpansion.common.registry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.google.common.hash.HashCode;
import com.mojang.logging.LogUtils;

import net.minecraft.data.CachedOutput;

/**
 * <b>P7d：把 datagen 产出的配方按「产出它的那个 provider」改道到各模块的
 * {@code src/generated/resources}</b>。它是 {@link LayerDataProvider} 的兄弟类：同一个
 * 「把落盘路径从根输出换到本层输出」的手法，但适用对象不同。
 *
 * <h2>一、为什么配方不能像资产/标签那样交给 {@code LayerDataProvider}</h2>
 * <p>{@code LayerDataProvider} 只包<b>它自己那个 Registrate 提供器</b>的
 * {@code run(CachedOutput)}；而 221 条生成配方<b>根本不由 Registrate 产出</b>——它们出自
 * 集成层的 {@code data/RecipeProvider}（它是唯一挂到 {@code DataGenerator} 上的配方提供器，
 * 内部依次调用 {@code CoeRecipeProvider.generate} 61 条拆磨 与
 * {@code CewsRecipeProvider.generate} 160 条工具充能）。那个提供器的 {@code --output} 是
 * 根工程的 {@code src/generated/resources}，所以 221 条全落根：三个模块 jar 里一条都没有
 * （P7c §5 实测：模块 jar 的 {@code data/**&#47;recipe/**} 共 111 条 = 96 手写 + 15 嬗变，
 * 与根的 221 条<b>交集 = 0</b>）。</p>
 *
 * <h2>二、层由「哪个 provider 产出」<b>显式</b>决定，绝不按路径判层</h2>
 * <p>红线规定所有层的注册命名空间恒为 {@code createoreexpansion}，所以 CEWS 的配方路径也是
 * {@code data/createoreexpansion/recipe/tool_charge/...}，与 COE 的
 * {@code data/createoreexpansion/recipe/dismantling/...} <b>逐字同类</b>——路径里没有任何
 * 层的痕迹。⇒ 判层只能来自调用点：集成层的 {@code RecipeProvider.buildRecipes} 把
 * {@code CoeRecipeProvider.generate} 与 {@code CewsRecipeProvider.generate} 的 RecipeOutput
 * 分别包成「本层」的，那条归属就是本类 {@link #bind} 收到的 {@code layer} 参数。
 * <b>本类里没有、也不许有「注册 id → 层」或「路径 → 层」的表</b>（与标签路由的关键差别）。</p>
 *
 * <h2>三、为什么需要「路径 → 层」的一次性记录（这不是判层）</h2>
 * <p>实测 {@code DataProvider.saveStable(CachedOutput, JsonElement, Path)} 的实现是
 * {@code CompletableFuture.runAsync(() -> output.writeIfNeeded(path, bytes, hash), backgroundExecutor)} ——
 * <b>真正的 {@code writeIfNeeded} 发生在后台线程上</b>，比 {@code RecipeOutput#accept} 晚，
 * 而且多个 worker 并发。所以「当前层」既不能是字段、也不能是 {@code ThreadLocal}。
 * 解决办法是把归属与那次写盘<b>成对地记下来</b>：集成层在 {@code accept} 里同步调用
 * {@link #bind(Path, String)}（键 = 那条配方自己的落盘路径，值 = 产出它的层），
 * 后台线程上的 {@code writeIfNeeded} 再按同一个 Path 取回它。</p>
 * <p>⇒ 本类里的 map 是<b>「同一次写盘的归属」的传递</b>，不是分类器：键的层从来没有被"推断"过，
 * 它由 {@link #bind} 的调用方（= 产出的那个 provider）给出。同一路径不可能被两层的 write 命中
 * （配方 id 唯一，重复 id 会被 vanilla 的 {@code Duplicate recipe} 直接抛）。</p>
 *
 * <h2>四、与路由成对的 {@code purgeStale}（两处，缺一不可）</h2>
 * <ul>
 *   <li><b>本类自己的 {@link #purgeStale()}</b>：{@code HashCache.purgeStaleAndWrite} 只遍历根输出
 *       目录（实测 {@code Files.walkFileTree(this.rootDir, ...)}），模块目录不在它的视野里，
 *       所以"某天删掉一条配方"留下的旧文件没人清。这里补一次同语义的清理，范围
 *       <b>只限本类改写的那个子树</b>（{@code <模块>/src/generated/resources/data/<ns>/recipe}），
 *       且只在本轮真的产出过配方时执行（照 {@code LayerDataProvider} 的口径）。</li>
 *   <li><b>{@code LayerDataProvider} 的 purge 必须跳过 {@code /recipe/}</b>（已改）：那些文件由
 *       <b>另一个</b>提供器写进本模块目录，而本模块 Registrate 提供器的 {@code produced} 对它们
 *       永远不权威。不跳的话，第二次 {@code runData} 会把上一轮刚落盘的 61/160 条配方当 stale
 *       删掉（P4f 给标签踩过同一个坑，见 {@code LayerDataProvider} 类注释"四"）。</li>
 * </ul>
 *
 * <h2>五、未注入时退化成旧行为</h2>
 * <p>取不到 {@code coe.datagen.layerAssetRoots}（例如从别的入口手工造
 * {@code RecipeProvider}）时 {@link #isEnabled} 对每一层都是 {@code false}，
 * {@link #wrap} 原样返回 delegate、{@link #bind} 不记录任何东西 ⇒ 221 条照旧落根工程，
 * 与 P7d 之前<b>逐字节相同</b>。缺配置只会"没分家"，不会写错地方。</p>
 */
public final class LayerRecipeRouter {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 根工程的 datagen 输出根（{@code --output}），配方路径都是它的子路径。 */
    private final Path rootOutput;

    /** 层名 → 该模块的 {@code src/generated/resources}。缺某一层 = 该层不参与改道。 */
    private final Map<String, Path> layerRoots;

    /** 被改写的相对目录（平台无关、`/` 分隔），形如 {@code data/createoreexpansion/recipe}。 */
    private final String relativeDir;

    /** 本次运行里真正落到模块目录的配方路径（改写后的绝对路径）。见 {@link #purgeStale()}。 */
    private final Set<Path> produced = ConcurrentHashMap.newKeySet();

    /** 一次写盘的归属：落盘路径（根输出下的原路径）→ 产出它的层。见类注释"三"。 */
    private final Map<Path, String> layerByPath = new ConcurrentHashMap<>();

    /**
     * @param rootOutput datagen 的根输出目录（{@code PackOutput#getOutputFolder()}）
     * @param layerRoots 层名 → 该模块的 {@code src/generated/resources}（空表 = 完全不改道）
     * @param namespace  注册命名空间（恒为 {@code createoreexpansion}）
     */
    public LayerRecipeRouter(Path rootOutput, Map<String, Path> layerRoots, String namespace) {
        this.rootOutput = rootOutput;
        this.layerRoots = Map.copyOf(layerRoots);
        this.relativeDir = "data/" + namespace + "/recipe";
        LOGGER.info("[Layer Recipe Router] active layers: {} (dir '{}')", this.layerRoots.keySet(), this.relativeDir);
    }

    /** {@code false} = 该层没有配置输出根 → 它产出的配方留根（退化成旧行为）。 */
    public boolean isEnabled(String layer) {
        return layerRoots.containsKey(layer);
    }

    /**
     * 记下一次写盘的归属（见类注释"三"）。由集成层在 {@code RecipeOutput#accept} 里<b>同步</b>调用，
     * 键必须与 {@code DataProvider.saveStable} 即将传给 {@code writeIfNeeded} 的那个 Path 相等。
     *
     * @param path  该配方在<b>根输出</b>下的落盘路径（{@code recipePathProvider.json(id)}）
     * @param layer 产出它的层（由调用点显式给出，不是推断）
     */
    public void bind(Path path, String layer) {
        if (isEnabled(layer)) {
            layerByPath.put(path, layer);
        }
    }

    /**
     * 包一层 {@link CachedOutput}：把<b>已归属的</b>配方写的路径从根输出换到那层的输出根，
     * 其余路径（含 advancement、以及没归属过的任何东西）原样透传。
     */
    public CachedOutput wrap(CachedOutput delegate) {
        if (layerRoots.isEmpty()) {
            return delegate;
        }
        return new CachedOutput() {
            @Override
            public void writeIfNeeded(Path path, byte[] data, HashCode hashCode) throws IOException {
                Path target = relocate(path);
                if (target == null) {
                    delegate.writeIfNeeded(path, data, hashCode);
                    return;
                }
                produced.add(target);
                // 字节零改动 ⇒ 原样透传提供器的哈希（与 saveStable 同算法），HashCache 语义不变。
                delegate.writeIfNeeded(target, data, hashCode);
            }
        };
    }

    /**
     * @return 改写后的目标路径；{@code null} = 不改写（没有归属、或不是配方路径）
     */
    @Nullable
    private Path relocate(Path path) {
        String layer = layerByPath.get(path);
        if (layer == null) {
            return null;
        }
        Path layerRoot = layerRoots.get(layer);
        if (layerRoot == null || !path.startsWith(rootOutput)) {
            return null;
        }
        Path relative = rootOutput.relativize(path);
        if (!slash(relative).startsWith(relativeDir + "/")) {
            return null;
        }
        return layerRoot.resolve(relative);
    }

    /**
     * 见类注释"四"：补上模块配方目录的 stale 清理（{@code HashCache} 只走根输出，看不见这里）。
     * 只在本轮真的产出过配方时执行；删除失败只记日志、不抛（与上游 {@code purgeStaleAndWrite} 同口径）。
     */
    public void purgeStale() {
        if (produced.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Path> entry : layerRoots.entrySet()) {
            Path dir = entry.getValue().resolve(relativeDir);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(dir)) {
                for (Path file : walk.filter(Files::isRegularFile).toList()) {
                    if (produced.contains(file)) {
                        continue;
                    }
                    try {
                        Files.delete(file);
                        LOGGER.info("[layer {}] removed stale generated recipe {}", entry.getKey(), file);
                    } catch (IOException e) {
                        LOGGER.warn("Failed to delete stale generated recipe {}", file, e);
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to walk {} for stale generated recipes", dir, e);
            }
        }
    }

    private static String slash(Path path) {
        return path.toString().replace('\\', '/');
    }
}
