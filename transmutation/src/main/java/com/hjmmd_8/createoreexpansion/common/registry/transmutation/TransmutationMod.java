package com.hjmmd_8.createoreexpansion.common.registry.transmutation;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.LayerBootstrap;
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
 * <p><b>P7a：hub 的注册触发已不存在</b>。四个聚合入口（{@code AllRecipeTypes} /
 * {@code AllCreativeModeTabs} / {@code AllFluids} / {@code AllModEffects}）已整体删除；
 * 共享注册表的挂载、旋转载荷、三层配方类型唤醒顺序改由 core 的幂等入口
 * {@code common.registry.LayerBootstrap} 完成，嬗变液与嬗乱效果由本构造器自己触发。</p>
 *
 * <p>本层不引入可选桥接（Jade 的两个插件分别归 CEWS 与 COE，Sable 归 CEWS，
 * Curios 归 COE 的凝能佩线）。</p>
 */
@Mod(TransmutationMod.MOD_ID)
public class TransmutationMod {

    /** TRANS 模块的 mod id（<b>不是</b>注册命名空间——命名空间恒为 {@code createoreexpansion}）。 */
    public static final String MOD_ID = "transmutation";

    public TransmutationMod(IEventBus modEventBus, ModContainer modContainer) {
        // ── P7a：共享接线（必须是构造器的第一条语句，见 LayerBootstrap 类注释"四"）───────────
        LayerBootstrap.ensureAttached(modEventBus);

        // P7a：TRANS 自己**没有**创造页（它的物品默认进 COE 的基础页，见 TransmutationRegistrate），
        // 所以这里不调 LayerCreativeTab.registerAll。共享的页注册表已由 LayerBootstrap 挂好。

        // P7a：原先藏在根侧聚合入口里的两处本层注册，按"谁的东西谁注册"搬回来：
        //   AllFluids.register()        -> TransmutationFluids.register()（嬗变液，含 TransmutationLink 注入）
        //   AllModEffects.register(bus) -> TransmutationEffects.register(bus)（嬗乱效果）
        // 不搬的话，单装 transmutation.jar 时这两个声明根本不会进注册表。
        TransmutationFluids.register();
        TransmutationEffects.register(modEventBus);

        // P3w：原先在 CreateOreExpansion 构造器里的三处 TRANS 触发点搬到这里 ——
        // 它们是本层自己的东西，而 :coe 已经看不到根工程的这个包。
        //   ① AllModPotions.register(modEventBus)              （POTION 注册表）
        //   ② NeoForge.EVENT_BUS.addListener(AllModPotions::registerBrewingRecipes)
        //   ③ AllFanProcessingTypes.init()（挂在 mod 总线的 RegisterEvent 上，自身幂等）
        // （P7a 更正一条旧注释：这里曾写"hub 的五个注册触发住 IntegrationBootstrap、
        //   本层不许 import 它"——那四个聚合入口已整体删除，共享接线改走 core 的 LayerBootstrap。）
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
