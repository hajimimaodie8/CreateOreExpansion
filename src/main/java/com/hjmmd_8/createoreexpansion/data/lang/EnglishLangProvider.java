package com.hjmmd_8.createoreexpansion.data.lang;

import com.hjmmd_8.createoreexpansion.common.AllBlocks;
import com.hjmmd_8.createoreexpansion.common.AllItems;
import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * English language file — single source of all English translations.
 *
 * Includes: creative tabs, items/blocks, skill names, skill types, keybinds, tooltips, potion effects.
 */
public class EnglishLangProvider extends LanguageProvider {

    public EnglishLangProvider(PackOutput output) {
        super(output, CreateOreExpansion.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // ========== 创造标签页 ==========
        add("itemGroup.createoreexpansion", "Create: Ore Expansion");
        add("createoreexpansion.mod_name", "Create: Ore Expansion");

        // ========== 物品/方块 ==========
        add(AllItems.JADE_INGOT.get(), "Jade Ingot");
        add(AllItems.RAW_JADE.get(), "Raw Jade");
        add(AllItems.JADE_NUGGET.get(), "Jade Nugget");
        add(AllItems.CRUSHED_JADE_ORE.get(), "Crushed Jade Ore");
        add(AllItems.JADE_SMALL_SHARD.get(), "Small Jade Shard");
        add(AllItems.JADE_BIG_SHARD.get(), "Big Jade Shard");
        add(AllItems.JADE_SHEET.get(), "Jade Sheet");
        add(AllItems.JADE_ROD.get(), "Jade Rod");
        add(AllItems.JADE_WIRE.get(), "Jade Wire");
        add(AllItems.JADE_SWORD.get(), "Jade Sword");
        add(AllItems.JADE_PICKAXE.get(), "Jade Pickaxe");
        add(AllItems.JADE_AXE.get(), "Jade Axe");
        add(AllItems.JADE_SHOVEL.get(), "Jade Shovel");
        add(AllItems.JADE_HOE.get(), "Jade Hoe");
        add(AllItems.TOPAZ_INGOT.get(), "Topaz Ingot");
        add(AllItems.RAW_TOPAZ.get(), "Raw Topaz");
        add(AllItems.TOPAZ_NUGGET.get(), "Topaz Nugget");
        add(AllItems.CRUSHED_TOPAZ_ORE.get(), "Crushed Topaz Ore");
        add(AllItems.TOPAZ_SMALL_SHARD.get(), "Small Topaz Shard");
        add(AllItems.TOPAZ_BIG_SHARD.get(), "Big Topaz Shard");
        add(AllItems.TOPAZ_SHEET.get(), "Topaz Sheet");
        add(AllItems.TOPAZ_ROD.get(), "Topaz Rod");
        add(AllItems.TOPAZ_WIRE.get(), "Topaz Wire");
        add(AllItems.TOPAZ_SWORD.get(), "Topaz Sword");
        add(AllItems.TOPAZ_PICKAXE.get(), "Topaz Pickaxe");
        add(AllItems.TOPAZ_AXE.get(), "Topaz Axe");
        add(AllItems.TOPAZ_SHOVEL.get(), "Topaz Shovel");
        add(AllItems.TOPAZ_HOE.get(), "Topaz Hoe");
        add(AllItems.RAW_SAPPHIRE.get(), "Raw Sapphire");
        add(AllItems.SAPPHIRE_INGOT.get(), "Sapphire Ingot");
        add(AllItems.SAPPHIRE_NUGGET.get(), "Sapphire Nugget");
        add(AllItems.CRUSHED_SAPPHIRE_ORE.get(), "Crushed Sapphire Ore");
        add(AllItems.SAPPHIRE_SMALL_SHARD.get(), "Small Sapphire Shard");
        add(AllItems.SAPPHIRE_BIG_SHARD.get(), "Big Sapphire Shard");
        add(AllItems.SAPPHIRE_SHEET.get(), "Sapphire Sheet");
        add(AllItems.SAPPHIRE_ROD.get(), "Sapphire Rod");
        add(AllItems.SAPPHIRE_WIRE.get(), "Sapphire Wire");
        add(AllBlocks.JADE_ORE.get(), "Jade Ore");
        add(AllBlocks.DEEPSLATE_JADE_ORE.get(), "Deepslate Jade Ore");
        add(AllBlocks.RAW_JADE_BLOCK.get(), "Raw Jade Block");
        add(AllBlocks.JADE_BLOCK.get(), "Jade Block");
        // Jade growable crystal
        add(AllBlocks.JADE_BUDDING_BLOCK.get(), "Jade Crystal Budding Block");
        add(AllBlocks.JADE_SMALL_BUD.get(), "Small Jade Crystal Bud");
        add(AllBlocks.JADE_MEDIUM_BUD.get(), "Medium Jade Crystal Bud");
        add(AllBlocks.JADE_LARGE_BUD.get(), "Large Jade Crystal Bud");
        add(AllBlocks.JADE_CLUSTER.get(), "Jade Crystal Cluster");
        add(AllBlocks.TOPAZ_ORE.get(), "Topaz Ore");
        add(AllBlocks.DEEPSLATE_TOPAZ_ORE.get(), "Deepslate Topaz Ore");
        add(AllBlocks.RAW_TOPAZ_BLOCK.get(), "Raw Topaz Block");
        add(AllBlocks.TOPAZ_BLOCK.get(), "Topaz Block");
        // Topaz growable crystal
        add(AllBlocks.TOPAZ_BUDDING_BLOCK.get(), "Topaz Crystal Budding Block");
        add(AllBlocks.TOPAZ_SMALL_BUD.get(), "Small Topaz Crystal Bud");
        add(AllBlocks.TOPAZ_MEDIUM_BUD.get(), "Medium Topaz Crystal Bud");
        add(AllBlocks.TOPAZ_LARGE_BUD.get(), "Large Topaz Crystal Bud");
        add(AllBlocks.TOPAZ_CLUSTER.get(), "Topaz Crystal Cluster");
        add(AllBlocks.NETHER_SAPPHIRE_ORE.get(), "Nether Sapphire Ore");
        add(AllBlocks.RAW_SAPPHIRE_BLOCK.get(), "Raw Sapphire Block");
        add(AllBlocks.SAPPHIRE_BLOCK.get(), "Sapphire Block");
        // Sapphire growable crystal
        add(AllBlocks.SAPPHIRE_BUDDING_BLOCK.get(), "Sapphire Crystal Budding Block");
        add(AllBlocks.SAPPHIRE_SMALL_BUD.get(), "Small Sapphire Crystal Bud");
        add(AllBlocks.SAPPHIRE_MEDIUM_BUD.get(), "Medium Sapphire Crystal Bud");
        add(AllBlocks.SAPPHIRE_LARGE_BUD.get(), "Large Sapphire Crystal Bud");
        add(AllBlocks.SAPPHIRE_CLUSTER.get(), "Sapphire Crystal Cluster");
        add(AllItems.SAPPHIRE_SWORD.get(), "Sapphire Sword");
        add(AllItems.SAPPHIRE_PICKAXE.get(), "Sapphire Pickaxe");
        add(AllItems.SAPPHIRE_AXE.get(), "Sapphire Axe");
        add(AllItems.SAPPHIRE_SHOVEL.get(), "Sapphire Shovel");
        add(AllItems.SAPPHIRE_HOE.get(), "Sapphire Hoe");
        add(AllItems.SANCTSTONE_INGOT.get(), "Sanctstone Ingot");
        add(AllItems.SANCTSTONE_SHEET.get(), "Sanctstone Sheet");
        add(AllItems.SANCTSTONE_ROD.get(), "Sanctstone Rod");
        add(AllItems.SANCTSTONE_WIRE.get(), "Sanctstone Wire");
        add(AllBlocks.SANCTSTONE_BLOCK.get(), "Sanctstone Block");
        add(AllItems.STELLARSTONE_INGOT.get(), "Stellarstone Ingot");
        add(AllItems.RAW_STELLARSTONE.get(), "Raw Stellarstone");
        add(AllItems.STELLARSTONE_NUGGET.get(), "Stellarstone Nugget");
        add(AllItems.CRUSHED_STELLARSTONE_ORE.get(), "Crushed Stellarstone Ore");
        add(AllItems.STELLARSTONE_SMALL_SHARD.get(), "Small Stellarstone Shard");
        add(AllItems.STELLARSTONE_BIG_SHARD.get(), "Big Stellarstone Shard");
        add(AllItems.STELLARSTONE_SHEET.get(), "Stellarstone Sheet");
        add(AllItems.STELLARSTONE_ROD.get(), "Stellarstone Rod");
        add(AllItems.STELLARSTONE_WIRE.get(), "Stellarstone Wire");
        add(AllItems.STELLARSTONE_SWORD.get(), "Stellarstone Sword");
        add(AllItems.STELLARSTONE_PICKAXE.get(), "Stellarstone Pickaxe");
        add(AllItems.STELLARSTONE_AXE.get(), "Stellarstone Axe");
        add(AllItems.STELLARSTONE_SHOVEL.get(), "Stellarstone Shovel");
        add(AllItems.STELLARSTONE_HOE.get(), "Stellarstone Hoe");
        add(AllBlocks.END_STELLARSTONE_ORE.get(), "End Stellarstone Ore");
        add(AllBlocks.RAW_STELLARSTONE_BLOCK.get(), "Raw Stellarstone Block");
        add(AllBlocks.STELLARSTONE_BLOCK.get(), "Stellarstone Block");
        // Stellarstone growable crystal
        add(AllBlocks.STELLARSTONE_BUDDING_BLOCK.get(), "Stellarstone Crystal Budding Block");
        add(AllBlocks.STELLARSTONE_SMALL_BUD.get(), "Small Stellarstone Crystal Bud");
        add(AllBlocks.STELLARSTONE_MEDIUM_BUD.get(), "Medium Stellarstone Crystal Bud");
        add(AllBlocks.STELLARSTONE_LARGE_BUD.get(), "Large Stellarstone Crystal Bud");
        add(AllBlocks.STELLARSTONE_CLUSTER.get(), "Stellarstone Crystal Cluster");
        add(AllBlocks.POWER_ANGLE_GRINDER.get(), "Power Angle Grinder");
        add(AllBlocks.JADE_CREATE_CHARGER.get(), "Jade Create Charger");
        add(AllBlocks.ENERGY_WAVE_REGULATOR.get(), "Energy Wave Regulator");
        add(AllBlocks.ENERGY_WAVE_DISPERSER.get(), "Energy Wave Disperser");
        add(AllBlocks.SIX_FACE_DISPERSER.get(), "Six-Face Energy Wave Disperser");
        add(AllBlocks.REINFORCED_LIGHTNING_ROD.get(), "Reinforced Lightning Rod");
        add(AllItems.RUBY_INGOT.get(), "Ruby Ingot");
        add(AllItems.RUBY_SHEET.get(), "Ruby Sheet");
        add(AllItems.RUBY_ROD.get(), "Ruby Rod");
        add(AllItems.RUBY_WIRE.get(), "Ruby Wire");
        add(AllBlocks.RUBY_BLOCK.get(), "Ruby Block");
        // ========== Mechanism ==========
        add(AllItems.LUCKY_DUST.get(), "Lucky Dust");
        add(AllItems.ENERGY_MECHANISM.get(), "Energy Mechanism");
        add(AllItems.INCOMPLETE_ENERGY_MECHANISM.get(), "Incomplete Energy Mechanism");
        add(AllItems.TRANSMUTE_MECHANISM.get(), "Transmute Mechanism");
        add(AllItems.INCOMPLETE_TRANSMUTE_MECHANISM.get(), "Incomplete Transmute Mechanism");
        // JEI tooltip: two ways to obtain a lightning strike
        add("createoreexpansion.jei.lightning_rod.ways", "Ways to gain a lightning strike: ① Be struck by a real natural lightning bolt; ② Absorb Gamma energy waves to fill the progress (10/10)");
        add("createoreexpansion.tooltip.lightning_rod.charge", "Gamma Charge: ");
        add("createoreexpansion.tooltip.lightning_rod.ready", "Lightning ready! Right-click to release");
        add("createoreexpansion.msg.cannot_open_cover", "Cannot open the cover: not enough space");
        add("createoreexpansion.msg.need_open_cover", "Open the cover first to install a grinding wheel");
        add(AllItems.IRON_GRINDING_WHEEL.get(), "Iron Grinding Wheel");
        add(AllItems.GOLD_GRINDING_WHEEL.get(), "Gold Grinding Wheel");
        add(AllItems.BRASS_GRINDING_WHEEL.get(), "Brass Grinding Wheel");
        add(AllItems.ZINC_GRINDING_WHEEL.get(), "Zinc Grinding Wheel");
        add(AllItems.JADE_GRINDING_WHEEL.get(), "Jade Grinding Wheel");
        add(AllItems.DIAMOND_GRINDING_WHEEL.get(), "Diamond Grinding Wheel");
        add(AllItems.TOPAZ_GRINDING_WHEEL.get(), "Topaz Grinding Wheel");
        add(AllItems.SAPPHIRE_GRINDING_WHEEL.get(), "Sapphire Grinding Wheel");
        add(AllItems.STELLARSTONE_GRINDING_WHEEL.get(), "Stellarstone Grinding Wheel");
        add(AllItems.NETHERITE_GRINDING_WHEEL.get(), "Netherite Grinding Wheel");

        // ========== Grinding wheel effects (parametrized: %s filled by effect class, no translation change needed) ==========
        add("createoreexpansion.wheel_effect.bonus", "Effect: %s%% chance to output an extra result");
        add("createoreexpansion.wheel_effect.speed", "Effect: Processing time reduced by %s%%");
        add("createoreexpansion.wheel_effect.quantity", "Effect: +%s to each result amount");
        add("createoreexpansion.wheel_effect.double", "Effect: %s%% chance to double results");
        add(AllItems.JADE_TOPAZ_BOW.get(), "Jade Topaz Bow");
        add(AllItems.JADE_STRESS_MEDALLION.get(), "Jade Stress Medallion");
        add(AllItems.TOPAZ_STRESS_MEDALLION.get(), "Topaz Stress Medallion");
        add(AllItems.SAPPHIRE_STRESS_MEDALLION.get(), "Sapphire Stress Medallion");
        add(AllItems.NETHERITE_STRESS_MEDALLION.get(), "Netherite Stress Medallion");
        add(AllItems.STELLARSTONE_STRESS_MEDALLION.get(), "Stellarstone Stress Medallion");
        add(AllItems.THUNDERITE_STRESS_MEDALLION.get(), "Thunderite Stress Medallion");

        // ========== Medallion Shift summary ==========
        add("item.createoreexpansion.medallion.hold_shift", "Hold [%1$s] for Summary");
        add("item.createoreexpansion.medallion.summary", "A medallion that _stores_ stress energy to _power_ _bound_ tools.");
        add("item.createoreexpansion.medallion.bind", "Binding: Right-click while holding this medallion with an energy tool in the other hand to _bind_, or craft with a tool; craft a bound tool alone to _unbind_, craft with another medallion to _rebind_");
        add("item.createoreexpansion.medallion.mode", "Mode: Right-click with this medallion (no tool in other hand) to switch between _Supply_ and _Charge_ mode");
        add("item.createoreexpansion.medallion.supply", "Supply: While worn, skill casts _drain the medallion_ first; Charge mode _refills bound tools_ after casting");

        // ========== 技能名（各等级共用基础键，等级由 tooltip 罗马数字显示） ==========
        add("skill.createoreexpansion.fell", "Fell");
        add("skill.createoreexpansion.shatter", "Shatter");
        add("skill.createoreexpansion.channel", "Channel");
        add("skill.createoreexpansion.grade", "Grade");
        add("skill.createoreexpansion.skin", "Skin");
        add("skill.createoreexpansion.plunder", "Plunder");
        add("skill.createoreexpansion.hoe", "Tend");
        add("skill.createoreexpansion.bow_curse", "Wither Curse");
        add("skill.createoreexpansion.bow_disarm", "Disarm Storm");

        // ========== 技能类型 ==========
        add("skillType.createoreexpansion.excavation_skill", "Excavation Skill");
        add("skillType.createoreexpansion.hit_skill", "Hit Skill");
        add("skillType.createoreexpansion.use_skill", "Use Skill");

        // ========== 技能释放键 ==========
        add("createoreexpansion.keyinfo.skill_release", "Release skill");
        add("createoreexpansion.keyinfo.skill_release_2", "Release skill 2 (2nd skill)");
        add("createoreexpansion.keyinfo.skill_release_3", "Release skill 3 (3rd skill)");

        // ========== 工具/技能 tooltip ==========
        add("item.createoreexpansion.tool.skill_tips", "Hold [%s] for Skills Summary");
        add("item.createoreexpansion.tool.energy", "Energy");

        // ========== Enchantments ==========
        add("enchantment.createoreexpansion.reduce_consumption", "Reduced Consumption");
        add("enchantment.createoreexpansion.swift_start", "Swift Start");
        add("enchantment.createoreexpansion.skill_boost", "Artistry Boost");
        add("enchantment.createoreexpansion.skill_regression", "Artistry Regression");

        // ========== 流体/配方 ==========
        add("fluid_type.createoreexpansion.transmutation_fluid", "Transmutation Fluid");
        add("item.createoreexpansion.transmutation_fluid_bucket", "Transmutation Fluid Bucket");
        add("createoreexpansion.recipe.fan_transmuting", "Transmuting");
        add("createoreexpansion.recipe.fan_transmuting.fan", "Encased Fan");
        add("createoreexpansion.recipe.lightning", "Lightning Transformation");
        add("createoreexpansion.recipe.lightning_block", "Lightning Transformation (Block)");
        add("createoreexpansion.recipe.grinding", "Grinding");
        add("createoreexpansion.recipe.advanced_grinding", "Advanced Grinding");
        add("createoreexpansion.recipe.dismantling", "Dismantling");
        add("createoreexpansion.recipe.dismantling.output", "Output amount depends on remaining durability");
        add("createoreexpansion.recipe.assembly.grinding", "Grind in a Power Angle Grinder");
        add("createoreexpansion.recipe.assembly.charging", "Charge in a Jade Create Charger");
        add("createoreexpansion.recipe.assembly.charging_hover", "Charge in a Jade Create Charger: %s");
        add("createoreexpansion.recipe.assembly.cca_charging", "Charge in a Tesla Coil or Strike by Lightning");
        // Charging category title (all three levels share one category; level badge keys below)
        add("createoreexpansion.recipe.charging", "Charging");
        add("createoreexpansion.jei.charging.level.1", "Low Energy");
        add("createoreexpansion.jei.charging.level.2", "High Energy");
        add("createoreexpansion.jei.charging.level.3", "Gamma Energy");
        add("entity.createoreexpansion.jade_charger_wave", "Charger Wave");

        // ========== Power Angle Grinder goggles ==========
        add("createoreexpansion.goggles.angle_grinder", "Power Angle Grinder");
        add("createoreexpansion.goggles.jade_charger", "Jade Create Charger");
        add("createoreexpansion.goggles.charger_idle", "No stress");
        // Tiers scale with Create's maxRotationSpeed config (default 256);
        // RPM range is read from config and passed in as args (%s) at runtime
        add("createoreexpansion.goggles.charger_low", "Low Charge (1-%s RPM)");
        add("createoreexpansion.goggles.charger_high", "High Charge (%s-%s RPM)");
        add("createoreexpansion.goggles.charger_gamma", "Gamma Charge (%s+ RPM)");
        add("createoreexpansion.goggles.no_wheel", "No grinding wheel installed!");
        add("createoreexpansion.goggles.installed_wheel", "Wheel: %s");
        add("createoreexpansion.goggles.required_speed", "Required speed: ≥ %s RPM");
        add("createoreexpansion.goggles.speed_too_low", "Speed too low to process!");
        add("createoreexpansion.goggles.processing_time", "Processing time: %s s");

        // ========== Power Angle Grinder hover tooltip (supported types) ==========
        add("createoreexpansion.tooltip.no_wheel_type", "No wheel installed, cannot process");
        add("createoreexpansion.tooltip.supported_types", "Supported processing:");
        add("createoreexpansion.tooltip.type_grinding", "· Basic grinding");
        add("createoreexpansion.tooltip.type_advanced", "· Crushing & Milling (Advanced)");
        add("createoreexpansion.tooltip.type_dismantling", "· Gear/Weapon disassembly (Dismantling)");

        // ========== 药水效果 ==========
        add("effect.createoreexpansion.transmutation_disorder", "Transmutation Disorder");
        add("item.minecraft.potion.effect.transmutation_disorder", "Transmutation Potion");
        add("item.minecraft.splash_potion.effect.transmutation_disorder", "Splash Transmutation Potion");
        add("item.minecraft.lingering_potion.effect.transmutation_disorder", "Lingering Transmutation Potion");
        add("item.minecraft.tipped_arrow.effect.transmutation_disorder", "Arrow of Transmutation Disorder");
        add("item.minecraft.potion.effect.strong_transmutation_disorder", "Strong Transmutation Potion");
        add("item.minecraft.splash_potion.effect.strong_transmutation_disorder", "Strong Splash Transmutation Potion");
        add("item.minecraft.lingering_potion.effect.strong_transmutation_disorder", "Strong Lingering Transmutation Potion");
        add("item.minecraft.tipped_arrow.effect.strong_transmutation_disorder", "Arrow of Strong Transmutation Disorder");
        add("item.minecraft.potion.effect.long_transmutation_disorder", "Long Transmutation Potion");
        add("item.minecraft.splash_potion.effect.long_transmutation_disorder", "Long Splash Transmutation Potion");
        add("item.minecraft.lingering_potion.effect.long_transmutation_disorder", "Long Lingering Transmutation Potion");
        add("item.minecraft.tipped_arrow.effect.long_transmutation_disorder", "Arrow of Long Transmutation Disorder");
    }
}
