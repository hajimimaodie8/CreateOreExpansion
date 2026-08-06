package com.hjmmd_8.createoreexpansion;

import com.hjmmd_8.createoreexpansion.common.*;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergyTooltip;
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

    // 创建Create的注册器 —— Registrate
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    static {
        // 为 Registrate 添加机械动力的应力条显示等等
        REGISTRATE.setTooltipModifierFactory(item -> {
            TooltipModifier modifier = new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                    .andThen(TooltipModifier.mapNull(KineticStats.create(item)));
            modifier = modifier.andThen(new ToolEnergyTooltip());
            return modifier;
        });

        AllCreativeModeTabs.registerTabs();

        // 设置默认的创造模式标签
        // 注意不能用错了，不能用成 setCreativeModeTab
        REGISTRATE.defaultCreativeTab(AllCreativeModeTabs.BASE_TAB.key());
    }

    public CreateOreExpansion(IEventBus modEventBus, ModContainer modContainer) {
        // 让 Registrate 知道自己应该什么时候该干什么
        // 如果不加这行代码，Registrate 的功能将会失效
        REGISTRATE.registerEventListeners(modEventBus);

        AllSkills.register();

        AllCreativeModeTabs.register(modEventBus);

        // 主动触发类加载，让Java加载静态字段
        AllDataComponents.register(modEventBus);
        AllMyBlocks.register();
        AllTiers.register();
        AllItems.register();
        AllMetal.register();
        AllMyFluids.register();
        AllModEffects.register(modEventBus);
        AllModPotions.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(AllModPotions::registerBrewingRecipes);
        AllRecipeTypes.register(modEventBus);
        modEventBus.addListener(CreateOreExpansion::onRegister);

        modContainer.registerConfig(ModConfig.Type.COMMON, AllConfig.SPEC);
    }

    public static void onRegister(RegisterEvent event) {
        AllFanProcessingTypes.init();
    }

    public static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
