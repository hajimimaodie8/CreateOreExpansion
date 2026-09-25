package com.hjmmd_8.createoreexpansion.data;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
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
 * <p>产物零变化：注册命名空间仍是 {@code createoreexpansion}；三层各写自己"路径带条目名"的产物
 * （方块状态 / 物品模型 / 战利品表 / 配方…），而<b>路径不含条目名</b>的<b>标签与语言</b>
 * 只在主层那个提供器里按 COE → CEWS → TRANS 的顺序跑三层并集——
 * 与前缀、路径、条目顺序都与拆分前的"单 Registrate"逐字节一致
 * （见 {@code LayerRegistrate#genData}）。</p>
 */
@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
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
        LayerRegistrate.attachDataGenerator(CoeRegistrate.REGISTRATE, generator, CoeCore.REGISTRY_NAMESPACE, null, event);
        LayerRegistrate.attachDataGenerator(CewsRegistrate.REGISTRATE, generator, CoeCore.REGISTRY_NAMESPACE, "cews", event);
        LayerRegistrate.attachDataGenerator(TransmutationRegistrate.REGISTRATE, generator, CoeCore.REGISTRY_NAMESPACE, "transmutation", event);

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
}
