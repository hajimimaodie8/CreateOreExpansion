package com.hjmmd_8.createoreexpansion.data;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.LayerRegistrate;
import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsRegistrate;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRegistrate;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationRegistrate;
import com.hjmmd_8.createoreexpansion.data.lang.ChineseLangProvider;
import com.hjmmd_8.createoreexpansion.data.lang.EnglishLangProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
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
        // models/** 会被改写到那里（`assets/<ns>/lang/**` 与 `data/**` 仍留在根工程，
        // 理由见 LayerDataProvider 类注释"二"——LANG 是三层并集、且与根工程
        // LanguageProvider 共写同一个 en_us.json）。路径由根 build.gradle 的 data run
        // 通过系统属性 coe.datagen.layerAssetRoots 传来：`coe=<abs>;cews=<abs>;transmutation=<abs>`。
        // 取不到时返回 null ⇒ 不改写（退化成 P4a 之前的行为，不会写错地方）。
        LayerRegistrate.attachDataGenerator(CoeRegistrate.REGISTRATE, generator, CoeCore.REGISTRY_NAMESPACE, null, event, layerAssetRoot("coe"));
        LayerRegistrate.attachDataGenerator(CewsRegistrate.REGISTRATE, generator, CoeCore.REGISTRY_NAMESPACE, "cews", event, layerAssetRoot("cews"));
        LayerRegistrate.attachDataGenerator(TransmutationRegistrate.REGISTRATE, generator, CoeCore.REGISTRY_NAMESPACE, "transmutation", event, layerAssetRoot("transmutation"));

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
        }
        if (event.includeServer()) {
            generator.addProvider(true, new RecipeProvider(output, event.getLookupProvider()));
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
}
