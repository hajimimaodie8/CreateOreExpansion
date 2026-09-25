package com.hjmmd_8.createoreexpansion;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.common.registry.coe.AllDataComponents;
import com.hjmmd_8.createoreexpansion.common.AllGemTags;
import com.hjmmd_8.createoreexpansion.common.registry.coe.AllStructureProcessors;
import com.hjmmd_8.createoreexpansion.common.registry.coe.AllTiers;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.machine.MachineRotatePayload;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlockEntityTypes;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlocks;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRegistrate;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.MedallionBindingRecipe;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.MedallionEnergyLink;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * <b>COE（矿物拓展）的 {@code @Mod} 入口</b>。
 *
 * <p><b>P3w 之后这个类住在 Gradle 子模块 {@code :coe} 里</b>（源码位置 {@code coe/src/main/java}），
 * 并且本模块自己的 {@code coe/src/main/templates/META-INF/neoforge.mods.toml} 声明
 * {@code createoreexpansion}；根工程的模板<b>不再</b>声明它（两个 mod 文件同声明一个 id 会被
 * {@code UniqueModListBuilder} 判 {@code duplicate_mod} 硬崩）。</p>
 *
 * <p><b>P3w 搬走了什么（本类原先的"集成职责"）</b>：{@code :coe} 只允许编译期依赖
 * {@code :core} 共享库，<b>看不到根工程</b>（root → :coe 已经是单向边，反向再加一条就是
 * Gradle 构图期的 {@code Circular dependency between the following tasks}）。所以下列
 * 跨层 / 跨 hub 的注册触发全部搬出本类：</p>
 * <ul>
 *   <li>{@code AllEntityTypes.register} → {@code CewsMod} 构造器（CEWS 层自己的东西）；</li>
 *   <li>{@code AllModPotions.register} + {@code AllModPotions::registerBrewingRecipes} +
 *       {@code AllFanProcessingTypes.init()} → {@code TransmutationMod} 构造器与其自己的
 *       {@code RegisterEvent} 监听器（TRANS 层自己的东西）；</li>
 *   <li>hub 的五个触发点（{@code LayerCreativeTab.installTabRegistrar} /
 *       {@code AllCreativeModeTabs.register} / {@code AllFluids.register} /
 *       {@code AllModEffects.register} / {@code AllRecipeTypes.register}）→
 *       根侧集成类 {@code common/hub/IntegrationBootstrap}，由 CEWS / TRANS 的
 *       {@code @Mod} 构造器<b>幂等地</b>调用（hub 是集成层，永远不许进 core，也永远不该进 :coe）。</li>
 * </ul>
 * <p>时序上只有 {@code installTabRegistrar} 敏感，而它对"注入之前就来过的请求"会<b>补跑</b>
 * （见 {@code LayerCreativeTab} 类注释），所以 FML 的构造顺序（COE → TRANS → CEWS）不影响结果；
 * 其余几处只是往事件总线挂 {@code DeferredRegister}，只要早于 {@code RegisterEvent} 即可。</p>
 *
 * <p><b>两处反射桥接为什么留在这里</b>（{@link #bootstrapCurios()} / {@link #bootstrapJade()}）：
 * 它们只含 {@code Class.forName} 的字符串字面量，<b>不产生任何编译边</b>；而
 * {@code bootstrapCurios()} 有硬时序要求——{@code MedallionCurios.item(...)} 在 {@code CoeItems}
 * 的静态块（物品工厂构造期）里<b>立刻</b>读桥接实现，注册桥接必须早于 {@code CoeItems.register()}，
 * 也就是必须发生在本构造器内部。根侧的 {@code IntegrationBootstrap} 由 CEWS / TRANS 构造器调用，
 * 那两个构造器都在 COE <b>之后</b>，搬过去就等于静默丢失 Curios 饰品支线。
 * 运行期 :coe 与根工程同处 GAME 层，自动模块互相可读，{@code Class.forName} 解析得到
 * （runData 日志里的 {@code [Curios]} / {@code [Jade]} 行即取证）。</p>
 *
 * <p><b>哪些东西<b>不</b>在这里了</b>（它们随各自的模块搬走，但注册命名空间一个字没变）：</p>
 * <ul>
 *   <li>命名空间常量 / {@code modLoc} / 日志器 → {@link CoeCore}（共享库，命名空间 {@code createoreexpansion}）；
 *       而<b>它们原先的注册触发</b>（配置 / 数据组件 / 实体类型 / 风扇加工类型 / 旋转载荷）
 *       在 P3d 反过来<b>搬回到本类构造器</b>——库没有生命周期，那本就是 mod 的职责；</li>
 *   <li>CEWS 的机器、页签内容、能量场载荷、Jade 波插件、Sable 桥接 → {@code CewsMod}；</li>
 *   <li>TRANS 的物品 / 药水 / 风扇加工类型 → {@code TransmutationMod}。</li>
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
        // P3w：原先在这里的 `LayerCreativeTab.installTabRegistrar(() -> AllCreativeModeTabs.registerTabs())`
        // 是 9 处触发点之一（hub 的 AllCreativeModeTabs 住根工程，:coe 看不到），已搬到根侧集成类
        // common/hub/IntegrationBootstrap，由 CEWS / TRANS 的 @Mod 构造器幂等调用。
        // 时序安全：installTabRegistrar 对"注入之前就来过的请求"会补跑（LayerCreativeTab 类注释），
        // 而 CoeRegistrate 的静态块正是在本构造器第 8 步发出请求。

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
        // P3w：`AllEntityTypes.register(modEventBus)`（CEWS 层）搬去 CewsMod 构造器；
        // `AllFanProcessingTypes.init()` 的 RegisterEvent 钩子（TRANS 层）搬去 TransmutationMod。
        // 统一交互规则第 4 条：Ctrl + 扳手右键 = 旋转本模组机器（客户端拦截 → 服务端校验并旋转）
        // P3o：载荷类（common/machine/MachineRotatePayload）已搬进 core 库，**不 import 本 @Mod 入口**，
        // 所以"谁来接 mod bus"这件事必须由根侧显式写死（库没有生命周期）。行为与逐个引用写法逐字一致。
        modEventBus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) ->
                MachineRotatePayload.registerPayloads(event));

        // 配置：目标容器<b>必须是</b> createoreexpansion（本 mod 自己的容器）。
        // 文件名 = <modid>-common.toml，挂到别的容器上会让老玩家的
        // createoreexpansion-common.toml 被静默弃用（设置"丢一次"）。
        // 另一个理由：AllConfig 自身是 @EventBusSubscriber(modid = createoreexpansion)，
        // 只有挂在本容器上，ModConfigEvent 才会送达它。
        modContainer.registerConfig(ModConfig.Type.COMMON, AllConfig.SPEC);

        // 本层 Registrate 的事件接线。静态块里已经设好 tooltip 工厂与默认创造页（基础页）；
        // 见 CoeRegistrate 的类注释（顺序由类初始化保证）。
        CoeRegistrate.REGISTRATE.registerEventListeners(modEventBus);

        // P3w：`AllCreativeModeTabs.register(modEventBus)`（hub）已随上面的集成职责一起搬到
        // 根侧 IntegrationBootstrap。它只往事件总线挂一个 DeferredRegister，
        // 只要早于 RegisterEvent 即可（TRANS / CEWS 构造器都远早于它）。

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

        // P3p：工具能量门面（common.energy.ToolEnergy）已搬进 core，不能再 import 层里的
        // IMedallion；共享库那边留了契约 common.energy.MedallionLink，具体实现是本层
        // content.equipment.medallion.MedallionEnergyLink。库没有生命周期，
        // 「谁来实现契约」必须由根侧在这里写死（与 P3o 的 MachineRotatePayload 载荷接线同一配方）。
        // 必须在任何游戏内逻辑（tooltip / 技能扣能）之前完成，故放在物品注册之前。
        MedallionEnergyLink.install();

        CoeItems.register();
        AllGemTags.register();
        // P3w：底下这五处原先就在这里，全是跨层 / hub 触发点，整块搬去根侧：
        //   AllFluids.register()          -> IntegrationBootstrap（hub -> TRANS 转发）
        //   AllModEffects.register(bus)   -> IntegrationBootstrap（hub -> TRANS 转发）
        //   AllModPotions.register(bus)   -> TransmutationMod 构造器（TRANS 层自己的东西）
        //   AllModPotions::registerBrewingRecipes -> TransmutationMod 的 game bus 监听
        //   AllRecipeTypes.register(bus)  -> IntegrationBootstrap（hub 聚合三层的配方类型）
        AllStructureProcessors.register(modEventBus);
        MedallionBindingRecipe.register(modEventBus);
        modEventBus.addListener(com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlockEntity::registerCapabilities);

        // 技能设置开关（"创造模式释放技能是否消耗能量"）的 C2S/S2C 包：服务端权威 + 存进存档
        modEventBus.addListener(com.hjmmd_8.createoreexpansion.integration.skiller.SkillSettingsPayload::registerPayloads);
        // 技能内核（Skiller）接线：注册上下文工厂 / 技能资源 / 技能条目，并接上迁移闸门
        com.hjmmd_8.createoreexpansion.integration.skiller.SkillerIntegration.register(modEventBus);

        bootstrapJade();

        CoeCore.LOGGER.info("[COE] mod 初始化完成（mod id={}，注册命名空间={}）：矿物拓展内容已注册，"
                + "共享地基（配置/数据组件/旋转载荷）已由本构造器接线",
            MOD_ID, CoeCore.REGISTRY_NAMESPACE);
    }

    /**
     * Curios 可选联动引导：只在 Curios 在场时加载桥接类（未装则凝能佩降级为纯物品，不崩）。
     *
     * <p><b>P3w：为什么它没有跟着别的集成触发点一起去根侧 IntegrationBootstrap</b>——
     * 它必须在 {@link CoeItems#register()} 的静态块<b>之前</b>跑完：{@code MedallionCurios.item(...)}
     * 在物品工厂构造期立刻读桥接实现，晚一步就整条 Curios 支线静默降级。
     * 而根侧的集成触发器由 CEWS / TRANS 构造器调用，那两个构造器都在本构造器<b>之后</b>。
     * 这个 {@code Class.forName} 只是字符串字面量，不产生编译边，所以 :coe 仍然只依赖 core。</p>
     */
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
     *
     * <p><b>P3w</b>：与 {@link #bootstrapCurios()} 同一理由留在本类——它只是一个
     * {@code Class.forName} 字面量（不产生编译边），而 Jade 插件的发现由 NeoForge 的
     * {@code @WailaPlugin} 扫描负责，与本构造器的时序无关；搬到根侧只会多绕一次跨 mod 调用。</p>
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
