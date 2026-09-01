package com.hjmmd_8.createoreexpansion;

import com.hjmmd_8.createoreexpansion.common.*;
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
                    .andThen(TooltipModifier.mapNull(KineticStats.create(item)));
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

        AllDataComponents.register(modEventBus);
        AllEntityTypes.register(modEventBus);
        AllBlocks.register();
        AllBlockEntityTypes.register();
        AllTiers.register();
        AllItems.register();
        AllGemTags.register();
        AllFluids.register();
        AllModEffects.register(modEventBus);
        AllModPotions.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(AllModPotions::registerBrewingRecipes);
        AllRecipeTypes.register(modEventBus);
        AllStructureProcessors.register(modEventBus);
        MedallionBindingRecipe.register(modEventBus);
        modEventBus.addListener(com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlockEntity::registerCapabilities);
        modEventBus.addListener(CreateOreExpansion::onRegister);

        // Jade 可选集成：仅当 Jade 已安装时才反射加载插件类（未安装时绝不触碰 Jade 类，
        // 避免"标注 optional 仍硬编码调用导致崩溃"——见 compat.jade.WaveJadePlugin 注释）
        if (net.neoforged.fml.ModList.get().isLoaded("jade")) {
            try {
                Class.forName("com.hjmmd_8.createoreexpansion.compat.jade.WaveJadePlugin");
                LOGGER.info("[Jade] 能量波信息显示插件已加载");
            } catch (Throwable t) {
                LOGGER.warn("[Jade] 能量波信息显示插件加载失败（不影响游戏运行）", t);
            }
        }

        // Sable 可选集成：仅当 Sable（航空学物理结构库）已安装时才反射加载桥接实现，
        // 让能量波与物理结构上的机器（充能器/波闸/差波器）通过位姿矩阵勾连（世界↔本地坐标）。
        // 未装 Sable 时绝不触碰 Sable 类（compat.sable.SableSubLevelBridge 直接引用 Sable 类型）。
        // 桥接注册与物理属性验证均在 SableSubLevelBridge 静态块内完成（Class.forName 触发）。
        if (net.neoforged.fml.ModList.get().isLoaded("sable")) {
            try {
                Class.forName("com.hjmmd_8.createoreexpansion.compat.sable.SableSubLevelBridge");
                LOGGER.info("[Sable] 能量波↔物理结构坐标桥接已加载");
            } catch (Throwable t) {
                LOGGER.warn("[Sable] 能量波物理结构桥接加载失败（不影响游戏运行）", t);
            }
        }

        modContainer.registerConfig(ModConfig.Type.COMMON, AllConfig.SPEC);
    }

    public static void onRegister(RegisterEvent event) {
        AllFanProcessingTypes.init();
    }

    public static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
