package com.hjmmd_8.createoreexpansion;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.common.registry.coe.AllDataComponents;
import com.hjmmd_8.createoreexpansion.common.AllGemTags;
import com.hjmmd_8.createoreexpansion.common.registry.coe.AllStructureProcessors;
import com.hjmmd_8.createoreexpansion.common.registry.coe.AllTiers;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeArmorMaterials;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.LayerBootstrap;
import com.hjmmd_8.createoreexpansion.common.registry.LayerCreativeTab;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlockEntityTypes;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlocks;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeCreativeTabs;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeEffects;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeMachines;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRegistrate;
import com.hjmmd_8.createoreexpansion.common.registry.coe.charger.AllEntityTypes;
import com.hjmmd_8.createoreexpansion.common.registry.coe.charger.CoeChargerBlockEntityTypes;
import com.hjmmd_8.createoreexpansion.common.registry.coe.charger.CoeChargerBlocks;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.AllFanProcessingTypes;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.AllModPotions;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationEffects;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationFluids;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationItems;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationRegistrate;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.MedallionBindingRecipe;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.MedallionEnergyLink;
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
 *   <li>{@code AllEntityTypes.register} → {@code CewsMod} 构造器（CEWS 层自己的东西）；
 *       <b>W6-c 起又搬回本类</b>——两个波实体类型随波引擎进 {@code :coe}；</li>
 *   <li>{@code AllModPotions.register} + {@code AllModPotions::registerBrewingRecipes} +
 *       {@code AllFanProcessingTypes.init()} → {@code TransmutationMod} 构造器与其自己的
 *       {@code RegisterEvent} 监听器（TRANS 层自己的东西）；
 *       <b>W6-b2 起又搬回本类</b>——嬗化整块进 {@code :coe}，见下条；</li>
 *   <li><b>P7a 起</b>：共享注册表的挂载、旋转载荷的注册、三层配方类型声明类的唤醒顺序 →
 *       core 的幂等入口 {@code common.registry.LayerBootstrap#ensureAttached}
 *       （本构造器第一条语句）。原先那五处 hub 聚合入口
 *       （{@code LayerCreativeTab.installTabRegistrar} / {@code AllCreativeModeTabs.register} /
 *       {@code AllFluids.register} / {@code AllModEffects.register} / {@code AllRecipeTypes.register}）
 *       已<b>整体删除</b>。</li>
 * </ul>
 * <p><b>更正一条旧注释</b>：P3w 时期这里写着"hub 触发由根侧 {@code IntegrationBootstrap} 负责，
 * 由 CEWS / TRANS 的 {@code @Mod} 构造器<b>幂等地</b>调用"——代码里从来没有模块调用过它，
 * 真实机制一直是 {@code IntegrationBootstrap} 自己挂 {@code FMLConstructModEvent}。
 * P7a 之后这条也不再需要：根工程降级为 dev-only，发布形态只有三个模块 jar，
 * 共享接线只剩 {@code LayerBootstrap} 这一条路。</p>
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
 *   <li>CEWS 的机器、页签内容、Sable 桥接 → {@code CewsMod}；
 *       <b>W6-c 起下列东西回到本类</b>：两个能量波实体类型（{@code AllEntityTypes}）、
 *       三台应力充能器的方块/方块实体登记（{@code common.registry.coe.charger}）、
 *       能量场载荷的挂载点、以及 Jade 波插件（{@code compat.jade.WaveJadePlugin}，W6-d 换回
 *       pre-split 包名）的加载点；</li>
 *   <li>TRANS 的物品 / 药水 / 风扇加工类型 → 曾经搬去 {@code TransmutationMod}，
 *       <b>W6-b2 起全部回到本类</b>：嬗化的 16 个 Java 文件 + 24 个资源文件整块搬进 {@code :coe}，
 *       {@code TransmutationMod} 只剩空壳（{@code :transmutation} 的 mod id 保留，作为"以后加新配方"
 *       的容器）。六处注册触发的落点与理由见本构造器里的 W6-b2 注释块。</li>
 * </ul>
 *
 * <p><b>Registrate 分家</b>：本层（{@code common/registry/coe/**} + {@code SeriesTraits}）的注册引用
 * {@link CoeRegistrate#REGISTRATE}。
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
        // ── P7a：共享接线（必须是构造器的第一条语句，见 LayerBootstrap 类注释"四"）───────────
        // 幂等地做三件"必须恰好发生一次"的事：挂两张共享配方类型注册表 + 一张共享创造页注册表、
        // 注册跨层共用的旋转载荷（Ctrl+扳手）、按固定顺序（TRANS → COE → CEWS）唤醒三层的
        // XxxRecipeTypes，让条目进注册表的顺序与拆分前逐字相同。
        // 这三个模块谁先构造都成立；先到的做实事，其余直接返回。
        LayerBootstrap.ensureAttached(modEventBus);

        // P7a：本层自己的创造页。登记动作不再由根侧注入（根工程已降级为 dev-only），
        // 而是每层自持；页的显示顺序由 withTabsBefore 的拓扑排序决定，与登记先后无关。
        LayerCreativeTab.registerAll(CoeCreativeTabs.tabs());

        // P3w：原先在这里的 `LayerCreativeTab.installTabRegistrar(() -> AllCreativeModeTabs.registerTabs())`
        // 与四个 hub 聚合入口（AllCreativeModeTabs / AllFluids / AllModEffects / AllRecipeTypes）
        // 都已在 P7a 删除，职责由 LayerBootstrap + 各层自持的登记动作取代。

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

        // ── 双色盔甲（4 套）的「盔甲材质」注册 ───────────────────────────────────────
        // 1.21.1 的 ArmorMaterial 住原版注册表 Registries.ARMOR_MATERIAL，不是 Registrate 管的
        // Item/Block，所以走 DeferredRegister（与上一行的 AllDataComponents 同一手法）。
        // 位置：必须在 CoeItems.register()（第 208 行附近）之前。它只构造 DeferredHolder、
        // 注册条目要等 RegisterEvent 才发生，所以"物品构造期读 Holder"是安全的。
        // ⚠ 只在这里触发一次：同一个 DeferredRegister 挂两次总线会在 RegisterEvent 上重复注册。
        CoeArmorMaterials.register(modEventBus);

        // ── W6-b2：嬗化线（原 :transmutation 层）的注册触发搬回本构造器 ──────────────────
        // 用户 2026-09-27 裁定：嬗化全部内容（16 个 Java + 24 个资源文件）整块进 :coe，
        // TransmutationMod 空壳化；那六处注册触发必须落到"内容真正所在的那一层"，否则
        // 只装 coe.jar 时嬗变液 / 嬗乱 / 药水 / 风扇加工类型一个都不会注册
        // （W6-b 实测结论，见 build/patch/w6b-EVIDENCE.txt §5.1）。
        // 位置 = 拆分前 CreateOreExpansion 里的相对位置（数据组件 → 药水 → 风扇加工钩子 → …）。
        // ⚠ 只在这里触发一次：同一个 DeferredRegister 挂两次总线会在 RegisterEvent 上重复注册。
        AllModPotions.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(AllModPotions::registerBrewingRecipes);
        modEventBus.addListener(CreateOreExpansion::onRegisterFanProcessingTypes);
        // P3w/P7a 的两条旧注释已被这一块取代（原写"这两个钩子搬去 TransmutationMod"）。
        // 仍然成立的一条旧注记：`AllEntityTypes.register(modEventBus)`（CEWS 层）搬去 CewsMod 构造器；
        // `MachineRotatePayload::registerPayloads` 已从本类搬到 LayerBootstrap 的幂等块
        //（单装 cews.jar 时原先没人注册它 ⇒ Ctrl+扳手在 CEWS 机器上完全没反应）。

        // 配置：目标容器<b>必须是</b> createoreexpansion（本 mod 自己的容器）。
        // 文件名 = <modid>-common.toml，挂到别的容器上会让老玩家的
        // createoreexpansion-common.toml 被静默弃用（设置"丢一次"）。
        // P7a：配置值的订阅方不再是 core 的 AllConfig（core 在发布形态里没有 ModContainer，
        // 注解永远不会被注入），而是本模块 foundation/AllConfigSubscriber —— 它的 modid 与
        // 本文件 id 天然配对，调 AllConfig.refresh()。
        modContainer.registerConfig(ModConfig.Type.COMMON, AllConfig.SPEC);

        // 本层 Registrate 的事件接线。静态块里已经设好 tooltip 工厂与默认创造页（基础页）；
        // 见 CoeRegistrate 的类注释（顺序由类初始化保证）。
        CoeRegistrate.REGISTRATE.registerEventListeners(modEventBus);
        // W6-b2：嬗化的 Registrate（TransmutationRegistrate）现在<b>没有任何条目</b>（嬗化构件两个物品
        // 已改挂 CoeRegistrate，理由见 TransmutationItems 的类注释），但它的接线保留在这里：
        // 将来往它里面加新条目时，"条目进注册表"这件事不会再静默落空（壳模块的唯一用途就是这个）。
        TransmutationRegistrate.REGISTRATE.registerEventListeners(modEventBus);

        // P7a：创造页注册表的挂载与页的登记都在本构造器开头（LayerBootstrap + LayerCreativeTab.registerAll）。

        // 按层显式触发类初始化（顺序与拆分前逐层一致）。
        // W6-c：能量波<b>引擎</b>（波实体类型 + 三台应力充能器的方块/方块实体 + 波引擎的
        // 机器命中登记表）已随 67 个 L1 文件进本层，所以它们的类初始化触发点也从 CewsMod
        // 搬回这里。触发点的位置刻意排在 Coe 的那三条之后（与拆分前的相对顺序一致：
        // CoeBlocks → CoeBlockEntityTypes → AllTiers → …），保证条目进注册表的先后不变。
        // ⚠ 只在这里触发一次：AllEntityTypes 的 DeferredRegister 挂两次总线会在 RegisterEvent
        //   上重复注册；CoeChargerBlocks / CoeChargerBlockEntityTypes 只做类初始化 + 槽位注入。
        AllEntityTypes.register(modEventBus);
        // 剩下的 CEWS 机器（差波器/调级器/波速调节器/星辉波变器/能量场控制器/机壳/查询仪）
        // 仍由 :cews 自己的 @Mod 构造器触发（见 CewsMod）；嬗化（原 TRANS 层）的
        // 流体 / 效果 / 物品由<b>本构造器</b>触发（W6-b2：内容已进 :coe，见上面的注释块）。
        CoeBlocks.register();
        // 批 2（2026-10-04）：机壳与机器类方块从 CoeBlocks 分家到同包的 CoeMachines（作者指定）。
        // 本行是本仓「注册触发顺序必须显式写死」的口径落点；**顺序锚点不在这一行**——
        // CoeMachines 的类初始化已由 CoeBlocks 里那个 static{} 块在**原机壳槽位**（矿物块之后、
        // 水晶之前）触发，因此这一行运行时是幂等空操作，登记顺序与拆分前逐条一致。
        // 反过来，若把这一行当成唯一触发点（哪怕放在 CoeBlocks.register() 之前/之后），
        // 这 6 件都会排到水晶之后，创造页里同族条目的位次会变（见 CoeBlocks 的 static{} 注释）。
        CoeMachines.register();
        CoeBlockEntityTypes.register();
        AllTiers.register();
        CoeChargerBlocks.register();
        CoeChargerBlockEntityTypes.register();

        // ── 宝石套 · 临域充力（规格 §8 第 3 层）：登记"第一个动力源方块" ──────────────
        // 用户 2026-10-01 规格 §2.1 第 4 条要求"必须暴露公开接口"给后续扩展登记动力源方块；
        // 本模组内建的第一个实现就是 Create 的手摇曲柄。
        // ⚠ **Create 的类只出现在 HandCrankStressSource 一个文件里**（判定/驱动/续期/静止/注入器候选位），
        // 技能侧只通过 StressSourceRegistry 拿接口 ⇒ 别的模组照同一个入口登记自己的动力源即可。
        // 这里显式触发它的类初始化（不写这一行，"扫曲柄"会永远找不到任何动力源）。
        com.hjmmd_8.createoreexpansion.content.equipment.armor.field.HandCrankStressSource.register();

        // Curios 可选联动（凝能佩/凝能之佩）：Curios 已从 required 降为 optional，因此
        // ① mods.toml 里 type=optional；② 所有 Curios API 调用只存在于 compat.curios 下的
        //    CurioMedallionBridge / CurioMedallionItems；③ 仅当 Curios 已安装时才 Class.forName
        //    触发桥接（未装时绝不触碰 Curios 类 → 不会 NoClassDefFoundError）。
        // 必须在 CoeItems.register() 之前：凝能佩的物品工厂按"桥接在不在"选饰品支线/纯物品支线两支类
        // （注册 id 与显示名两支一致，玩家侧无感）。
        bootstrapCurios();

        // P3p：工具能量门面（P12 起包名还原为 content.equipment.tool.energy.ToolEnergy）已搬进
        // core，不能再 import 层里的 IMedallion；共享库那边留了契约 common.energy.MedallionLink，
        // 具体实现是本层
        // content.equipment.medallion.MedallionEnergyLink。库没有生命周期，
        // 「谁来实现契约」必须由根侧在这里写死（与 P3o 的 MachineRotatePayload 载荷接线同一配方）。
        // 必须在任何游戏内逻辑（tooltip / 技能扣能）之前完成，故放在物品注册之前。
        MedallionEnergyLink.install();

        CoeItems.register();
        AllGemTags.register();
        // W6-b2：嬗化线的流体与效果（原 :transmutation 层）—— 这两处原先在 hub / TRANS 构造器里，
        // 位置保持拆分前"物品之后、结构处理器之前"。流体仍挂 CoeRegistrate.REGISTRATE
        //（实现纪律 X7：绝不换成 TransmutationRegistrate，否则 W5 A1 的 c:buckets 缺陷原地复活）；
        // 嬗化构件两个物品也在这里触发类初始化（在 CoeItems 之后 = 与拆分前的条目先后一致）。
        TransmutationFluids.register();
        TransmutationEffects.register(modEventBus);
        // coe-charge 批 1：电荷系统的两个负面效果（着正电 / 着负电）——COE 层自己的
        // MobEffect 注册器，触发点全仓只此一处（重复挂总线会在 RegisterEvent 上重复注册）。
        CoeEffects.register(modEventBus);
        TransmutationItems.register();
        AllStructureProcessors.register(modEventBus);
        MedallionBindingRecipe.register(modEventBus);
        modEventBus.addListener(com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlockEntity::registerCapabilities);

        // 技能设置开关（"创造模式释放技能是否消耗能量"）的 C2S/S2C 包：服务端权威 + 存进存档
        modEventBus.addListener(com.hjmmd_8.createoreexpansion.integration.skiller.settings.SkillSettingsPayload::registerPayloads);
        // 装备技能冷却同步（用户 2026-10-01）：服务端起冷却时通知客户端，HUD 才能显示剩余秒数
        modEventBus.addListener(com.hjmmd_8.createoreexpansion.content.equipment.armor.energy.EquipCooldownPayload::registerPayloads);
        // W6-c：能量场（加速/偏转/赋能）同步载荷的挂载点从 CewsMod 搬到这里 —— 载荷类
        // EnergyFieldSyncPayload 随能量场子系统（content.energyfield 包）进了 :coe，
        // 而 check-module-selfsufficiency 的 D3 要求"模块只挂载属于自己模块的 payload"。
        // 方向与语义都没变：RegisterPayloadHandlersEvent 是发往每个 mod 容器的，
        // 挂哪条总线都能注册；区别只是"谁在场谁负责"——只装 coe.jar 时场同步依旧成立。
        modEventBus.addListener(com.hjmmd_8.createoreexpansion.content.energyfield.EnergyFieldSyncPayload::registerPayloads);
        // 技能内核（Skiller）接线：注册上下文工厂 / 技能资源 / 技能条目，并接上迁移闸门
        com.hjmmd_8.createoreexpansion.integration.skiller.SkillerIntegration.register(modEventBus);

        bootstrapJade();

        CoeCore.LOGGER.info("[COE] mod 初始化完成（mod id={}，注册命名空间={}）：矿物拓展内容已注册，"
                + "共享接线（共享注册表/旋转载荷/配方类型唤醒顺序）由 LayerBootstrap 幂等完成，"
                + "配置缓存由 AllConfigSubscriber 刷新",
            MOD_ID, CoeCore.REGISTRY_NAMESPACE);
    }

    /**
     * {@code RegisterEvent} 上的风扇加工类型注册钩子（W6-b2：从空壳化的
     * {@code TransmutationMod#onRegister} 原样搬来，方法体一字未改）。
     *
     * <p><b>语义与拆分前逐字相同</b>：每次 {@code RegisterEvent} 触发都调
     * {@code AllFanProcessingTypes.init()}（自身幂等），由它的类初始化把 {@code transmuting}
     * 注册进 Create 的 {@code FAN_PROCESSING_TYPE}。挂哪条 mod 总线不影响结果 ——
     * {@code RegisterEvent} 是发往每个 mod 总线的，所以这个监听器与
     * {@code CewsMod} / 空壳 {@code TransmutationMod} 各自的监听器互不干扰。</p>
     */
    private static void onRegisterFanProcessingTypes(RegisterEvent event) {
        AllFanProcessingTypes.init();
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
     * （矿物拓展加工线）；{@code WaveJadePlugin}（能量波/波情显示）在 W6-c 之后也住本层
     * —— 它显示的是两个<b>波实体</b>，而波实体随波引擎进了 {@code :coe}（见类注释"两处反射桥接"）。</p>
     *
     * <p><b>P3w</b>：与 {@link #bootstrapCurios()} 同一理由留在本类——它只是两个
     * {@code Class.forName} 字面量（不产生编译边），而 Jade 插件的发现由 NeoForge 的
     * {@code @WailaPlugin} 扫描负责，与本构造器的时序无关；搬到别处只会多绕一次跨 mod 调用。</p>
     *
     * <p><b>W6-c</b>：这一句原先在 {@code CewsMod#bootstrapJade()}（那时 {@code WaveJadePlugin}
     * 住 {@code compat.jei.cews}）。类随波引擎进 {@code :coe} 之后，加载方也必须跟着走——
     * 否则只装 {@code coe.jar} 时没有任何人会去触发它（{:cews} 不在场），
     * 波实体就没有 Jade 提示。这与 P7a 对 {@code CurioMedallionBridge} /
     * {@code BasinLiveJadePlugin} 的处理<b>完全同形</b>。</p>
     *
     * <p><b>W6-d</b>：W6-c 当时把 {@code WaveJadePlugin} 折进了 {@code :coe} 已有的
     * {@code compat.jei} 包（代价是 {@code check-package-heritage} 多一条 NON-FORCED）。
     * 它是 <b>Jade</b> 插件而不是 JEI 插件，而 {@code compat.jade} 本来就在 {@code :coe}
     * （{@code BasinLiveJadePlugin} 就住那儿）——于是本轮换回 {@code compat.jade}
     * （= 拆分前的包名），两个 {@code Class.forName} 字面量现在同包不同类。</p>
     */
    private static void bootstrapJade() {
        if (ModList.get().isLoaded("jade")) {
            try {
                Class.forName("com.hjmmd_8.createoreexpansion.compat.jade.BasinLiveJadePlugin");
                CoeCore.LOGGER.info("[Jade] 工作盆物品行实时化插件已加载（COE）");
            } catch (Throwable t) {
                CoeCore.LOGGER.warn("[Jade] 工作盆物品行实时化插件加载失败（不影响游戏运行）", t);
            }
            try {
                Class.forName("com.hjmmd_8.createoreexpansion.compat.jade.WaveJadePlugin");
                CoeCore.LOGGER.info("[Jade] 能量波信息显示插件已加载（随波引擎进 COE）");
            } catch (Throwable t) {
                CoeCore.LOGGER.warn("[Jade] 能量波信息显示插件加载失败（不影响游戏运行）", t);
            }
        }
    }
}
