package com.hjmmd_8.createoreexpansion;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.common.hub.AllCreativeModeTabs;
import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.common.registry.cews.AllEntityTypes;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.AllFanProcessingTypes;
import com.hjmmd_8.createoreexpansion.common.hub.AllFluids;
import com.hjmmd_8.createoreexpansion.common.AllGemTags;
import com.hjmmd_8.createoreexpansion.common.AllModEffects;
import com.hjmmd_8.createoreexpansion.common.AllModPotions;
import com.hjmmd_8.createoreexpansion.common.hub.AllRecipeTypes;
import com.hjmmd_8.createoreexpansion.common.registry.coe.AllStructureProcessors;
import com.hjmmd_8.createoreexpansion.common.registry.coe.AllTiers;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.machine.MachineRotatePayload;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlockEntityTypes;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlocks;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRegistrate;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.MedallionBindingRecipe;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * <b>COE（矿物拓展）的 {@code @Mod} 入口</b>。
 *
 * <p><b>P3b 之后这个类的身份变了</b>：它<b>不再</b>代表"整个模组"，只代表
 * <b>矿物拓展（mod id 仍是 {@code createoreexpansion}）这一个模块</b>。
 * 同一份 jar 里现在有三个 {@code @Mod}：本类、{@code cews}（{@code common.registry.cews.CewsMod}）、
 * {@code transmutation}（{@code common.registry.transmutation.TransmutationMod}）；
 * {@code common.CoeCore} <b>不是</b> {@code @Mod}（P3d 起 core 是 JarJar 嵌套的普通库）。</p>
 *
 * <p><b>哪些东西<b>不</b>在这里了</b>（它们随各自的模块搬走，但注册命名空间一个字没变）：</p>
 * <ul>
 *   <li>命名空间常量 / {@code modLoc} / 日志器 → {@link CoeCore}（共享库，命名空间 {@code createoreexpansion}）；
 *       而<b>它们原先的注册触发</b>（配置 / 数据组件 / 实体类型 / 风扇加工类型 / 旋转载荷）
 *       在 P3d 反过来<b>搬回到本类构造器</b>——库没有生命周期，那本就是 mod 的职责；</li>
 *   <li>CEWS 的机器、页签内容、能量场载荷、Jade 波插件、Sable 桥接 → {@code CewsMod}；</li>
 *   <li>TRANS 的物品 → {@code TransmutationMod}。</li>
 * </ul>
 *
 * <p><b>Registrate 分家</b>：本层（{@code common/registry/coe/**} + {@code AllFluids} +
 * {@code SeriesTraits}）的注册引用 {@link CoeRegistrate#REGISTRATE}。
 * 那个实例的 {@code CreateRegistrate} 命名空间参数<b>仍是</b> {@code createoreexpansion}
 * （见 {@code common/registry/LayerRegistrate}），所以注册 id 与拆分前逐字一致。</p>
 */
@Mod(CreateOreExpansion.MOD_ID)
public class CreateOreExpansion {

    /**
     * COE 模块的 mod id —— <b>转发</b>共享库里的同名常量（P3k：真身已搬到 {@link CoeCore}）。
     *
     * <p>保留这个字段是为了让 {@code @Mod} 注解与其余 15 处
     * {@code @EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)} 一行不改：
     * 初始化式是另一个 {@code static final String} 常量，所以它仍是<b>编译期常量表达式</b>
     * （JLS 15.29），可以继续出现在注解里。</p>
     *
     * <p><b>别再拿它当命名空间用</b>：注册命名空间是 {@link CoeCore#REGISTRY_NAMESPACE}
     * （值恰好相同，但语义不同——CEWS / TRANS 两个模块也用它）。
     * 需要拼 {@code ResourceLocation} / 注册 id / 语言键前缀时一律用那个常量。</p>
     */
    public static final String MOD_ID = CoeCore.MOD_ID;

    public CreateOreExpansion(IEventBus modEventBus, ModContainer modContainer) {
        // P3k：MOD 事件总线随「mod 身份」常量一起搬到共享库（库没有生命周期，
        // 也不该反向依赖 @Mod 入口）；本类只负责在构造时把它填上。全仓无消费者。
        CoeCore.MOD_BUS = modEventBus;

        // ── 共享地基的注册触发（P3d：原 CoeCore 的 @Mod 构造器整体搬到这里）─────────────
        // core 现在是普通库、没有自己的 mod 生命周期，而这些动作本来就是「createoreexpansion
        // 这个 mod 的初始化」，所以目标容器/事件总线都还是本 mod 的，一个字没变。
        // 顺序与拆分前 CoeCore 构造器内逐条一致（数据组件 → 实体类型 → 风扇加工钩子 →
        // 旋转载荷 → 配置），并且整块排在本类其余注册触发<b>之前</b>，
        // 保持它们相对既有注册触发顺序的先后关系不变。
        AllDataComponents.register(modEventBus);
        AllEntityTypes.register(modEventBus);
        // 风扇加工类型（Create 的注册表）：沿用拆分前的写法，RegisterEvent 每次触发都调用
        // （init() 自身幂等），行为与拆分前逐字一致。
        modEventBus.addListener(CreateOreExpansion::onRegister);
        // 统一交互规则第 4 条：Ctrl + 扳手右键 = 旋转本模组机器（客户端拦截 → 服务端校验并旋转）
        modEventBus.addListener(MachineRotatePayload::registerPayloads);

        // 配置：目标容器<b>必须是</b> createoreexpansion（本 mod 自己的容器）。
        // 文件名 = <modid>-common.toml，挂到别的容器上会让老玩家的
        // createoreexpansion-common.toml 被静默弃用（设置"丢一次"）。
        // 另一个理由：AllConfig 自身是 @EventBusSubscriber(modid = createoreexpansion)，
        // 只有挂在本容器上，ModConfigEvent 才会送达它。
        modContainer.registerConfig(ModConfig.Type.COMMON, AllConfig.SPEC);

        // 本层 Registrate 的事件接线。静态块里已经设好 tooltip 工厂与默认创造页（基础页）；
        // 见 CoeRegistrate 的类注释（顺序由类初始化保证）。
        CoeRegistrate.REGISTRATE.registerEventListeners(modEventBus);

        AllCreativeModeTabs.register(modEventBus);

        // 按层显式触发类初始化（顺序与拆分前逐层一致）。
        // CEWS / TRANS 两层的方块与物品由它们各自的 @Mod 构造器触发（见 CewsMod / TransmutationMod）。
        CoeBlocks.register();
        CoeBlockEntityTypes.register();
        AllTiers.register();

        // Curios 可选联动（凝能佩/凝能之佩）：Curios 已从 required 降为 optional，因此
        // ① mods.toml 里 type=optional；② 所有 Curios API 调用只存在于 compat.curios 下的
        //    CurioMedallionBridge / CurioMedallionItems；③ 仅当 Curios 已安装时才 Class.forName
        //    触发桥接（未装时绝不触碰 Curios 类 → 不会 NoClassDefFoundError）。
        // 必须在 CoeItems.register() 之前：凝能佩的物品工厂按"桥接在不在"选饰品支线/纯物品支线两支类
        // （注册 id 与显示名两支一致，玩家侧无感）。
        bootstrapCurios();

        CoeItems.register();
        AllGemTags.register();
        AllFluids.register();
        AllModEffects.register(modEventBus);
        AllModPotions.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(AllModPotions::registerBrewingRecipes);
        AllRecipeTypes.register(modEventBus);
        AllStructureProcessors.register(modEventBus);
        MedallionBindingRecipe.register(modEventBus);
        modEventBus.addListener(com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlockEntity::registerCapabilities);

        // 技能设置开关（"创造模式释放技能是否消耗能量"）的 C2S/S2C 包：服务端权威 + 存进存档
        modEventBus.addListener(com.hjmmd_8.createoreexpansion.integration.skiller.SkillSettingsPayload::registerPayloads);
        // 技能内核（Skiller）接线：注册上下文工厂 / 技能资源 / 技能条目，并接上迁移闸门
        com.hjmmd_8.createoreexpansion.integration.skiller.SkillerIntegration.register(modEventBus);

        bootstrapJade();

        CoeCore.LOGGER.info("[COE] mod 初始化完成（mod id={}，注册命名空间={}）：矿物拓展内容已注册，"
                + "共享地基（配置/数据组件/实体类型/风扇加工类型/旋转载荷）已由本构造器接线",
            MOD_ID, CoeCore.REGISTRY_NAMESPACE);
    }

    /**
     * {@code RegisterEvent} 上的风扇加工类型注册钩子。
     *
     * <p>P3d 之前住在 {@code CoeCore#onRegister}（那时 core 还是 {@code @Mod}）；
     * core 改成普通库后随注册动作一起搬回本 mod，<b>语义与拆分前逐字相同</b>：
     * 每次 {@code RegisterEvent} 触发都调 {@code AllFanProcessingTypes.init()}（自身幂等），
     * 由它的类初始化把 {@code transmuting} 注册进 Create 的 {@code FAN_PROCESSING_TYPE}。</p>
     */
    private static void onRegister(RegisterEvent event) {
        AllFanProcessingTypes.init();
    }

    /** Curios 可选联动引导：只在 Curios 在场时加载桥接类（未装则凝能佩降级为纯物品，不崩）。 */
    private static void bootstrapCurios() {
        if (ModList.get().isLoaded("curios")) {
            try {
                Class.forName("com.hjmmd_8.createoreexpansion.compat.curios.CurioMedallionBridge");
                CoeCore.LOGGER.info("[Curios] 凝能佩饰品桥接已加载（饰品支线生效，行为与 Curios required 时期一致）");
            } catch (Throwable t) {
                CoeCore.LOGGER.warn("[Curios] 凝能佩饰品桥接加载失败 → 凝能佩降级为普通物品（不影响游戏运行）", t);
            }
        } else {
            CoeCore.LOGGER.info("[Curios] 未安装 Curios：凝能佩按纯物品注册（可合成/可持有/可绑定，无饰品槽效果）");
        }
    }

    /**
     * Jade 可选集成：仅当 Jade 已安装时才反射加载插件类（未安装时绝不触碰 Jade 类，
     * 避免"标注 optional 仍硬编码调用导致崩溃"——见 compat.jade.BasinLiveJadePlugin 注释）。
     *
     * <p><b>归属判定</b>：{@code BasinLiveJadePlugin} 接管的是<b>工作盆</b>的物品行实时化
     * （矿物拓展加工线），所以留在 COE；另一个 Jade 插件 {@code WaveJadePlugin}
     * （能量波/波情显示）随 CEWS 模块走。</p>
     */
    private static void bootstrapJade() {
        if (ModList.get().isLoaded("jade")) {
            try {
                Class.forName("com.hjmmd_8.createoreexpansion.compat.jade.BasinLiveJadePlugin");
                CoeCore.LOGGER.info("[Jade] 工作盆物品行实时化插件已加载（COE）");
            } catch (Throwable t) {
                CoeCore.LOGGER.warn("[Jade] 工作盆物品行实时化插件加载失败（不影响游戏运行）", t);
            }
        }
    }
}
