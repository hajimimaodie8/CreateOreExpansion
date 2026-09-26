package com.hjmmd_8.createoreexpansion.common.registry.transmutation;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * <b>TRANS（Create: Mechanical Transmutation，机械嬗化学）的 {@code @Mod} 入口</b>
 * （P3b：四模块拆分）。
 *
 * <p>本层目前只有物品（{@link TransmutationItems}）+ 药水 + 风扇加工类型，职责很薄——
 * 但 mod 类与 Registrate 都必须独立，否则 TRANS 永远无法脱离 COE 单独编译。
 * 注册命名空间仍是 {@code createoreexpansion}，所以注册 id / 语言键 / 数据包路径一个都没变。</p>
 *
 * <p><b>P3w</b>：本类所在的根工程 mod 文件声明 {@code cews} / {@code transmutation}
 * （COE 的 {@code [[mods]]} 搬去了 Gradle 子模块 {@code :coe}）；原先挂在
 * {@code CreateOreExpansion} 构造器里的三处 TRANS 触发点搬回了本构造器
 * （见构造器内注释）。hub 的注册触发由 {@code common/hub/IntegrationBootstrap} 负责，
 * 本层<b>不</b>引用它 —— 那会造成 {@code TRANS -> 根侧 SHARED} 的源码边。</p>
 *
 * <p>本层不引入可选桥接（Jade 的两个插件分别归 CEWS 与 COE，Sable 归 CEWS，
 * Curios 归 COE 的凝能佩线）。</p>
 */
@Mod(TransmutationMod.MOD_ID)
public class TransmutationMod {

    /** TRANS 模块的 mod id（<b>不是</b>注册命名空间——命名空间恒为 {@code createoreexpansion}）。 */
    public static final String MOD_ID = "transmutation";

    public TransmutationMod(IEventBus modEventBus, ModContainer modContainer) {
        // P3w：原先在 CreateOreExpansion 构造器里的三处 TRANS 触发点搬到这里 ——
        // 它们是本层自己的东西，而 :coe 已经看不到根工程的这个包。
        //   ① AllModPotions.register(modEventBus)              （POTION 注册表）
        //   ② NeoForge.EVENT_BUS.addListener(AllModPotions::registerBrewingRecipes)
        //   ③ AllFanProcessingTypes.init()（挂在 mod 总线的 RegisterEvent 上，自身幂等）
        // hub 的五个注册触发不在这里：它们住 common/hub/IntegrationBootstrap（SHARED），
        // 由 FML 在 CEWS 构造完后的 FMLConstructModEvent 上触发。**本层不许 import 它**
        // —— 那会造出 TRANS -> 根侧 SHARED 的源码边，让 layer-usage 的 LAYER-NO 从 0 变 1
        // （见 IntegrationBootstrap 类注释"二"）。它只做"挂 DeferredRegister"，
        // 早于 RegisterEvent 即可。
        AllModPotions.register(modEventBus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(AllModPotions::registerBrewingRecipes);
        modEventBus.addListener(TransmutationMod::onRegister);

        TransmutationRegistrate.REGISTRATE.registerEventListeners(modEventBus);

        // 按层显式触发物品注册类初始化（拆分前本层排在 CEWS 物品之后）
        TransmutationItems.register();

        CoeCore.LOGGER.info("[TRANS] mod 初始化完成（mod id={}，注册命名空间={}）：嬗变化合物件已注册",
            MOD_ID, CoeCore.REGISTRY_NAMESPACE);
    }

    /**
     * {@code RegisterEvent} 上的风扇加工类型注册钩子（P3w 从 {@code CreateOreExpansion#onRegister}
     * 原样搬来；P3d 之前它住在 {@code CoeCore#onRegister}，那时 core 还是 {@code @Mod}）。
     *
     * <p><b>语义与拆分前逐字相同</b>：每次 {@code RegisterEvent} 触发都调
     * {@code AllFanProcessingTypes.init()}（自身幂等），由它的类初始化把 {@code transmuting}
     * 注册进 Create 的 {@code FAN_PROCESSING_TYPE}。挂哪条 mod 总线不影响结果 ——
     * {@code RegisterEvent} 是发往每个 mod 总线的。</p>
     */
    private static void onRegister(net.neoforged.neoforge.registries.RegisterEvent event) {
        AllFanProcessingTypes.init();
    }
}
