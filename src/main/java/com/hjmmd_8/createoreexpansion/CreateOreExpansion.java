package com.hjmmd_8.createoreexpansion;

import com.hjmmd_8.createoreexpansion.client.ChargerKineticTooltip;
import com.hjmmd_8.createoreexpansion.common.*;
import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsBlockEntityTypes;
import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsBlocks;
import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsItems;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlockEntityTypes;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlocks;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationItems;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.MedallionBindingRecipe;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CreateOreExpansion.MOD_ID)
public class CreateOreExpansion {

    // 声明 ModId
    public static final String MOD_ID = "createoreexpansion";

    // 创建 Logger
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static IEventBus MOD_BUS;

    // 创建Create的注册器 —— Registrate
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    static {
        // 为 Registrate 添加机械动力的应力条显示等等
        REGISTRATE.setTooltipModifierFactory(item -> {
            TooltipModifier modifier = new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                    .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
                    // 充能器：三实心方块 + 自定义区间应力提示（隐藏 Create 默认静态行，见 ChargerKineticTooltip）
                    .andThen(TooltipModifier.mapNull(ChargerKineticTooltip.create(item)));
            return modifier;
        });

        AllCreativeModeTabs.registerTabs();

        // 设置默认的创造模式标签
        // 注意不能用错了，不能用成 setCreativeModeTab
        REGISTRATE.defaultCreativeTab(AllCreativeModeTabs.BASE_TAB.key());
    }

    public CreateOreExpansion(IEventBus modEventBus, ModContainer modContainer) {
        MOD_BUS = modEventBus;
        REGISTRATE.registerEventListeners(modEventBus);

        AllCreativeModeTabs.register(modEventBus);
        // CEWS（能量波阵学）标签页的内容构建：清单见 EnergyWaveStudyTab.CONTENTS
        // （往新页放 + 从基础页剔除都在那一处，注册代码一行不动）
        modEventBus.addListener(EnergyWaveStudyTab::onBuildContents);

        AllDataComponents.register(modEventBus);
        AllEntityTypes.register(modEventBus);
        // P2a：原来的 AllBlocks / AllBlockEntityTypes 外观类已删除，这里按层显式触发类初始化。
        // 顺序必须保持"方块层 → 方块实体层"、且每层内部先 COE 再 CEWS（与拆分前逐层一致）。
        CoeBlocks.register();
        CewsBlocks.register();
        CoeBlockEntityTypes.register();
        CewsBlockEntityTypes.register();
        AllTiers.register();

        // Curios 可选联动（凝能佩/凝能之佩）：Curios 已从 required 降为 optional，因此
        // ① mods.toml 里 type=optional；② 所有 Curios API 调用只存在于 compat.curios 下的
        //    CurioMedallionBridge / CurioMedallionItems；③ 仅当 Curios 已安装时才 Class.forName
        //    触发桥接（未装时绝不触碰 Curios 类 → 不会 NoClassDefFoundError）。
        // 必须在 CoeItems.register() 之前：凝能佩的物品工厂按"桥接在不在"选饰品支线/纯物品支线两支类
        // （注册 id 与显示名两支一致，玩家侧无感）。
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            try {
                Class.forName("com.hjmmd_8.createoreexpansion.compat.curios.CurioMedallionBridge");
                LOGGER.info("[Curios] 凝能佩饰品桥接已加载（饰品支线生效，行为与 Curios required 时期一致）");
            } catch (Throwable t) {
                LOGGER.warn("[Curios] 凝能佩饰品桥接加载失败 → 凝能佩降级为普通物品（不影响游戏运行）", t);
            }
        } else {
            LOGGER.info("[Curios] 未安装 Curios：凝能佩按纯物品注册（可合成/可持有/可绑定，无饰品槽效果）");
        }

        // P2a：按层显式触发物品注册类初始化（COE → CEWS → TRANS，与拆分前一致）
        CoeItems.register();
        CewsItems.register();
        TransmutationItems.register();
        AllGemTags.register();
        AllFluids.register();
        AllModEffects.register(modEventBus);
        AllModPotions.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(AllModPotions::registerBrewingRecipes);
        AllRecipeTypes.register(modEventBus);
        AllStructureProcessors.register(modEventBus);
        MedallionBindingRecipe.register(modEventBus);
        modEventBus.addListener(com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlockEntity::registerCapabilities);
        modEventBus.addListener(com.hjmmd_8.createoreexpansion.content.energyfield.EnergyFieldSyncPayload::registerPayloads);
        // 统一交互规则第 4 条：Ctrl + 扳手右键 = 旋转本模组机器（客户端拦截 → 服务端校验并旋转）
        modEventBus.addListener(com.hjmmd_8.createoreexpansion.content.machine.MachineRotatePayload::registerPayloads);
        // 技能设置开关（"创造模式释放技能是否消耗能量"）的 C2S/S2C 包：服务端权威 + 存进存档
        modEventBus.addListener(com.hjmmd_8.createoreexpansion.integration.skiller.SkillSettingsPayload::registerPayloads);
        modEventBus.addListener(CreateOreExpansion::onRegister);
        // 技能内核（Skiller）接线：注册上下文工厂 / 技能资源 / 技能条目，并接上迁移闸门
        com.hjmmd_8.createoreexpansion.integration.skiller.SkillerIntegration.register(modEventBus);

        // Jade 可选集成：仅当 Jade 已安装时才反射加载插件类（未安装时绝不触碰 Jade 类，
        // 避免"标注 optional 仍硬编码调用导致崩溃"——见 compat.jade.WaveJadePlugin 注释）
        if (net.neoforged.fml.ModList.get().isLoaded("jade")) {
            try {
                Class.forName("com.hjmmd_8.createoreexpansion.compat.jade.WaveJadePlugin");
                // 第二个 Jade 插件：工作盆物品行实时化——整条接管 Jade 原生物品行
                // （BasinLiveItemStorage 注册在 BasinBlock 上、优先级压过通用物品存储，逐 tick 取数）
                Class.forName("com.hjmmd_8.createoreexpansion.compat.jade.BasinLiveJadePlugin");
                LOGGER.info("[Jade] 能量波信息显示 + 工作盆物品行实时化插件已加载");
            } catch (Throwable t) {
                LOGGER.warn("[Jade] 能量波信息显示插件加载失败（不影响游戏运行）", t);
            }
        }

        // Sable 可选集成：仅当 Sable（航空学物理结构库）已安装时才反射加载桥接实现，
        // 让能量波与物理结构上的机器（充能器/波闸/差波器）通过位姿矩阵勾连（世界↔本地坐标）。
        // 未装 Sable 时绝不触碰 Sable 类（compat.sable.SableSubLevelBridge 直接引用 Sable 类型）。
        // 桥接注册与物理属性验证均在 SableSubLevelBridge 静态块内完成（Class.forName 触发）。
        //
        // 判据（2026-09-20 修正）：原先只看 modId "sable"，但实测 jar 里
        //   libs/sable-companion-common-1.21.1-1.6.0.jar 的 modId 是 "sablecompanion"，
        //   而 libs/aeronautics-neoforge-1.21.1-1.3.0.jar 才声明依赖 modId "sable"；
        //   我们真正使用的类是 dev.ryanhcode.sable.companion.math.Pose3dc（来自前者）。
        // 用户实例里主 sable 未加载（或被 bundled 进嵌套 jar）→ 旧判据为假 → 整条物理结构链路惰性。
        // 新判据：① 先看类在不在（首选，直接对应我们依赖的东西）；② 再退化为任一 modId 命中。
        String sableByClass = detectSableByClass();
        String sableByModId = (sableByClass != null) ? null : detectSableByModId();
        String sableCriterion = (sableByClass != null) ? sableByClass : sableByModId;
        if (sableCriterion != null) {
            try {
                Class.forName("com.hjmmd_8.createoreexpansion.compat.sable.SableSubLevelBridge");
                LOGGER.info("[Sable] 物理结构桥接已加载（判据：{}）", sableCriterion);
            } catch (Throwable t) {
                LOGGER.warn("[Sable] 物理结构桥接加载失败（判据：{} 已命中，不影响游戏运行）", sableCriterion, t);
            }
        } else {
            LOGGER.warn("[Sable] 物理结构桥接未加载：类 dev.ryanhcode.sable.companion.math.Pose3dc 不在场，"
                + "且 modId sable / sablecompanion / aeronautics 均未加载（哪条判据都没命中）");
        }

        modContainer.registerConfig(ModConfig.Type.COMMON, AllConfig.SPEC);
    }

    /**
     * 判据①：我们真正依赖的 Sable 类在不在（运行期反射探测，编译期无需该类在场）。
     *
     * <p>用 {@code Class.forName(name, false, loader)} <b>不初始化</b>目标类，避免副作用；
     * 依次尝试上下文 / 本模组 / Minecraft（即游戏层，聚合了所有 mod jar）的类加载器，
     * 任一能加载到即视为"物理结构库在场"。</p>
     *
     * @return 命中时的判据文案（写进日志），未命中返回 {@code null}
     */
    private static String detectSableByClass() {
        // 与 compat.sable.SablePose 的 import 一致：dev.ryanhcode.sable.companion.math.Pose3dc
        final String probe = "dev.ryanhcode.sable.companion.math.Pose3dc";
        ClassLoader[] candidates = new ClassLoader[] {
            Thread.currentThread().getContextClassLoader(),
            CreateOreExpansion.class.getClassLoader(),
            net.minecraft.world.level.Level.class.getClassLoader(),
            ClassLoader.getSystemClassLoader(),
        };
        for (ClassLoader loader : candidates) {
            if (loader == null) {
                continue;
            }
            try {
                Class.forName(probe, false, loader);
                return "类 " + probe + " 在场";
            } catch (Throwable ignored) {
                // 换下一个类加载器继续探测
            }
        }
        return null;
    }

    /** 判据②（兜底）：Sable 主 jar / companion / 航空学 任一 modId 已加载。未命中返回 {@code null}。 */
    private static String detectSableByModId() {
        net.neoforged.fml.ModList modList = net.neoforged.fml.ModList.get();
        String[] modIds = { "sable", "sablecompanion", "aeronautics" };
        for (String modId : modIds) {
            if (modList.isLoaded(modId)) {
                return "modId " + modId + " 已加载";
            }
        }
        return null;
    }

    public static void onRegister(RegisterEvent event) {
        AllFanProcessingTypes.init();
    }

    public static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
