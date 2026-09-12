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
        add(AllBlocks.JADE_CASING.get(), "Jade Casing");
        add(AllBlocks.SAPPHIRE_CASING.get(), "Sapphire Casing");
        // Stellarstone growable crystal
        add(AllBlocks.STELLARSTONE_BUDDING_BLOCK.get(), "Stellarstone Crystal Budding Block");
        add(AllBlocks.STELLARSTONE_SMALL_BUD.get(), "Small Stellarstone Crystal Bud");
        add(AllBlocks.STELLARSTONE_MEDIUM_BUD.get(), "Medium Stellarstone Crystal Bud");
        add(AllBlocks.STELLARSTONE_LARGE_BUD.get(), "Large Stellarstone Crystal Bud");
        add(AllBlocks.STELLARSTONE_CLUSTER.get(), "Stellarstone Crystal Cluster");
        add(AllBlocks.POWER_ANGLE_GRINDER.get(), "Power Angle Grinder");
        add(AllBlocks.JADE_STRESS_CHARGER.get(), "Jade Create Charger");
        add(AllBlocks.STELLARSTONE_STRESS_CHARGER.get(), "Stellarstone Stress Charger");
        add(AllBlocks.STELLAR_WAVE_TRANSMUTER.get(), "Stellar Wave Transmuter");
        add(AllBlocks.STELLARSTONE_WAVE_REGULATOR.get(), "Stellarstone Wave Regulator");
        add(AllBlocks.STELLARSTONE_SPEED_REGULATOR.get(), "Stellarstone Speed Regulator");
        add("createoreexpansion.goggles.stellarstone_wave_regulator", "Stellarstone Wave Regulator");
        add("createoreexpansion.goggles.stellarstone_speed_regulator", "Stellarstone Speed Regulator");
        add("createoreexpansion.goggles.stellarstone_wave_boost", "Level Boost: +%s per pass");
        add(AllBlocks.ENERGY_WAVE_REGULATOR.get(), "Jade Energy Wave Regulator");
        add(AllBlocks.WAVE_SPEED_REGULATOR.get(), "Jade Wave Speed Regulator");
        add("createoreexpansion.goggles.wave_speed_regulator", "Jade Wave Speed Regulator");
        add("createoreexpansion.goggles.speed_regulator_tier", "Speed Tier: %s");
        add("createoreexpansion.goggles.speed_regulator_amount", "Speed Change: ±%s blocks/s");
        // Jade wave entity info
        add("config.jade.plugin_createoreexpansion.wave", "Wave Information");
        add("createoreexpansion.jade.wave_level", "Wave Level: %s");
        add("createoreexpansion.jade.wave_speed", "Speed: %s blocks/s");
        add("createoreexpansion.jade.wave_lifetime", "Lifetime: %s s");
        add("createoreexpansion.jade.wave_charge", "Charge: %s");
        add("createoreexpansion.jade.wave_charge_none", "Charge: none");
        add("createoreexpansion.jade.charge_positive", "Positive");
        add("createoreexpansion.jade.charge_negative", "Negative");
        // Stellar (variant) wave carried payload
        add("createoreexpansion.jade.stellar_wave_payload_items", "Carried Items (%s/%s pcs · %s/%s kinds): %s");
        add("createoreexpansion.jade.stellar_wave_payload_energy", "Carried Energy: %s FE");
        add("createoreexpansion.jade.stellar_wave_payload_fluid", "Carried Fluid: %s %s mB");
        add("createoreexpansion.jade.stellar_wave_payload_rods", "Lightning Strikes: %s (strikes wherever it hits)");
        add("createoreexpansion.jade.stellar_wave_payload_heat", "Carried Heat: %s");
        add("createoreexpansion.jade.stellar_wave_payload_rpm", "Carried Speed: %s RPM");
        // Variant wave craftable recipe types (capability list, independent of payload)
        add("createoreexpansion.jade.stellar_wave_can_process", "Can process:");
        add(AllBlocks.ENERGY_WAVE_DISPERSER.get(), "Energy Wave Disperser");
        add(AllBlocks.SIX_FACE_DISPERSER.get(), "Six-Face Energy Wave Disperser");
        add(AllBlocks.OCTA_ENERGY_WAVE_DIFFERENCER.get(), "Octa Energy Wave Disperser");
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
        add("createoreexpansion.recipe.assembly.charging_hover_sapphire", "Charge in a Sapphire Create Charger: %s");
        add("createoreexpansion.recipe.assembly.cca_charging", "Charge in a Tesla Coil or Strike by Lightning");
        // Charging category title (all three levels share one category; level badge keys below)
        add("createoreexpansion.recipe.charging", "Charging");
        add("createoreexpansion.jei.charging.level.1", "Low Energy");
        add("createoreexpansion.jei.charging.level.2", "High Energy");
        add("createoreexpansion.jei.charging.level.3", "Gamma Energy");
        add("createoreexpansion.jei.charging.level.4", "Overload Energy");
        add("createoreexpansion.jei.charging.level.5", "Ultimate Energy");
        add("entity.createoreexpansion.charger_wave", "Charger Wave");
        add("entity.createoreexpansion.jade_charger_wave", "Charger Wave");
        add("entity.createoreexpansion.stellar_wave", "Variant Wave");

        // ========== Power Angle Grinder goggles ==========
        add("createoreexpansion.goggles.angle_grinder", "Power Angle Grinder");
        add("createoreexpansion.goggles.jade_charger", "Jade Stress Charger");
        add("createoreexpansion.goggles.sapphire_charger", "Sapphire Stress Charger");
        add("createoreexpansion.goggles.stellarstone_charger", "Stellarstone Stress Charger");
        add("createoreexpansion.goggles.stellarstone_charger_manual_level", "Manual Wave Level: %s");
        add("createoreexpansion.goggles.stellarstone_charger_normal", "Mode: Normal (continuous fire)");
        add("createoreexpansion.goggles.stellarstone_charger_storing", "Mode: Storing (click/redstone when full)");
        add("createoreexpansion.goggles.stellarstone_charger_store_layers", "Charge layers: %s/%s");
        add("createoreexpansion.goggles.stellarstone_charger_store_full", "Full: right-click to release");
        add("createoreexpansion.charger.manual_level", "Manual Wave Level");
        add("createoreexpansion.charger.level_row", "Level");
        add("createoreexpansion.goggles.stellar_wave_transmuter", "Stellar Wave Transmuter");
        add("createoreexpansion.goggles.stellar_wave_transmuter_idle", "No stress");
        add("createoreexpansion.goggles.stellar_wave_transmuter_radius", "Scan radius: %s blocks");
        add("createoreexpansion.goggles.stellar_wave_transmuter_heat", "Heat read: %s (Blaze Burner)");
        add("createoreexpansion.goggles.stellar_wave_transmuter_heat_none", "Heat read: none (no lit Blaze Burner within scan radius)");
        add("createoreexpansion.goggles.stellar_wave_transmuter_item_containers", "Item containers read: %s (chests/basins/drawers)");
        add("createoreexpansion.goggles.stellar_wave_transmuter_fluid_containers", "Fluid containers read: %s (tanks/fluid drawers)");
        add("createoreexpansion.goggles.stellar_wave_transmuter_energy_storages", "Energy storages read: %s (generators/batteries, %s FE total)");
        add("createoreexpansion.goggles.stellar_wave_transmuter_wave_rpm", "Wave processing speed: %s RPM (from this machine)");
        add("createoreexpansion.goggles.stellar_wave_transmuter_machines", "Machines linked: %s");
        add("createoreexpansion.goggles.stellar_wave_transmuter_stress", "Linked machine stress: %s");
        add("createoreexpansion.goggles.stellar_wave_transmuter_types", "Recipe types loaded: %s");
        add("createoreexpansion.goggles.stellar_wave_transmuter_items", "Item payload: %s items, %s kinds");
        add("createoreexpansion.goggles.stellar_wave_transmuter_fluid", "Fluid payload: %s mB");
        add("createoreexpansion.goggles.stellar_wave_transmuter_energy", "Energy payload: %s FE");
        add("createoreexpansion.goggles.stellar_wave_transmuter_rods", "Lightning rod strikes: %s (only taken when fully charged; the wave strikes wherever it hits)");
        add("createoreexpansion.goggles.stellar_wave_transmuter_last_wave", "Recent wave can process:");
        add("createoreexpansion.goggles.stellar_wave_transmuter_bind_hint", "Linked to %s machines. Hold [%s] for recipes");
        add("createoreexpansion.goggles.stellar_wave_transmuter_bind_hint_shift", "Linked to %s machines. Holding [%s] shows recipes");
        // Single-line recipe list: separator and overflow suffix (comma-joined, "etc" past 10)
        add("createoreexpansion.goggles.list_separator", ", ");
        add("createoreexpansion.goggles.list_etc", " etc.");
        // Recipe-type display names (goggles "recent wave" panel; Create/Vintage/CCA/Optical/own)
        add("createoreexpansion.recipe_type.pressing", "Pressing");
        add("createoreexpansion.recipe_type.cutting", "Cutting");
        add("createoreexpansion.recipe_type.milling", "Milling");
        add("createoreexpansion.recipe_type.crushing", "Crushing");
        add("createoreexpansion.recipe_type.splashing", "Splashing");
        add("createoreexpansion.recipe_type.haunting", "Haunting");
        add("createoreexpansion.recipe_type.deploying", "Deploying");
        add("createoreexpansion.recipe_type.item_application", "Item Application");
        add("createoreexpansion.recipe_type.mixing", "Mixing");
        add("createoreexpansion.recipe_type.compacting", "Compacting");
        add("createoreexpansion.recipe_type.filling", "Filling");
        add("createoreexpansion.recipe_type.emptying", "Emptying");
        add("createoreexpansion.recipe_type.transmuting", "Transmuting");
        add("createoreexpansion.recipe_type.sequenced_assembly", "Sequenced Assembly");
        add("createoreexpansion.recipe_type.focusing", "Focusing");
        add("createoreexpansion.recipe_type.coiling", "Coiling");
        add("createoreexpansion.recipe_type.curving", "Curving");
        add("createoreexpansion.recipe_type.polishing", "Polishing");
        add("createoreexpansion.recipe_type.centrifugation", "Centrifugation");
        add("createoreexpansion.recipe_type.vibrating", "Vibrating");
        add("createoreexpansion.recipe_type.leaves_vibrating", "Leaves Vibrating");
        add("createoreexpansion.recipe_type.pressurizing", "Pressurizing");
        add("createoreexpansion.recipe_type.vacuumizing", "Vacuumizing");
        add("createoreexpansion.recipe_type.hammering", "Hammering");
        add("createoreexpansion.recipe_type.auto_smithing", "Auto Smithing");
        add("createoreexpansion.recipe_type.auto_upgrade", "Auto Upgrade");
        add("createoreexpansion.recipe_type.turning", "Turning");
        add("createoreexpansion.recipe_type.laser_cutting", "Laser Cutting");
        add("createoreexpansion.recipe_type.charging", "Charging");
        add("createoreexpansion.recipe_type.rolling", "Rolling");
        add("createoreexpansion.recipe_type.lightning", "Lightning Conversion");
        add("createoreexpansion.recipe_type.grinding", "Grinding");
        add("createoreexpansion.recipe_type.dismantling", "Dismantling");
        // Blaze Burner heat level display names (goggles heat read / Jade carried heat)
        add("createoreexpansion.heat_level.none", "None");
        add("createoreexpansion.heat_level.smouldering", "Smouldering");
        add("createoreexpansion.heat_level.fading", "Fading");
        add("createoreexpansion.heat_level.kindled", "Heated");
        add("createoreexpansion.heat_level.seething", "Superheated");
        add("createoreexpansion.recipe.stellar_wave_transmuter", "Stellar Wave Transmuter");
        add("createoreexpansion.jei.transmuter.line1", "1) A horizontal energy wave entering the side is converted into a variant wave that exits on the opposite side; hitting the top/bottom face behaves like a wall.");
        add("createoreexpansion.jei.transmuter.line2", "2) The variant carries the scanned machines' recipe types plus the heat read from lit Blaze Burners in range (heated/superheated, enabling heated mixing and other heat-gated recipes); chain steps = wave level (minus 1 per successful craft).");
        add("createoreexpansion.jei.transmuter.line3", "3) Payload: up to 5 items (5 kinds), 2 B fluid, FE energy (cap = the most expensive CC&A charging recipe) and lightning strikes (taken from FULLY CHARGED reinforced lightning rods; the wave strikes wherever it hits, and the strike is processed by the lightning strike system).");
        add("createoreexpansion.jei.transmuter.line4", "4) On item hit, matching recipes run: dual-input spends auxiliary items, fluid/FE entries deduct the payload; when the chain ends the leftovers return to nearby storage or drop/waste.");
        add("createoreexpansion.goggles.field_controller", "Energy Field Controller");
        add("createoreexpansion.goggles.field_controller_idle", "No stress (cannot generate a field)");
        add("createoreexpansion.goggles.field_controller_polarity", "Port polarity: %s");
        add("createoreexpansion.goggles.field_controller_polarity_pos", "Positive");
        add("createoreexpansion.goggles.field_controller_polarity_neg", "Negative");
        add("createoreexpansion.goggles.field_controller_polarity_none", "None (no stress)");
        add("createoreexpansion.goggles.field_controller_tier", "Field tier: %s");
        add("createoreexpansion.field_controller.lid_opened", "Receiver lid opened");
        add("createoreexpansion.field_controller.lid_closed", "Receiver lid closed");
        add("createoreexpansion.field_controller.type_accel", "Field type: Acceleration");
        add("createoreexpansion.field_controller.type_deflect", "Field type: Deflection");
        add("createoreexpansion.goggles.sapphire_speed_regulator", "Sapphire Speed Regulator");
        add("createoreexpansion.goggles.sapphire_charger_normal", "Mode: Normal (continuous fire)");
        add("createoreexpansion.goggles.sapphire_charger_storing", "Mode: Storing (click/redstone when full)");
        add("createoreexpansion.charger.mode", "Charger Mode");
        add("createoreexpansion.charger_mode.normal", "Normal");
        add("createoreexpansion.charger_mode.store", "Store");
        add("createoreexpansion.goggles.sapphire_charger_store_progress", "Charge stored: %s%%");
        add("createoreexpansion.goggles.sapphire_charger_store_layers", "Charge layers: %s/%s");
        add("createoreexpansion.goggles.sapphire_charger_store_full", "Full: right-click to release");
        add("createoreexpansion.goggles.sapphire_charger_store_line", "Layer progress: %s/%s");
        add("createoreexpansion.goggles.sapphire_charger_1", "Low Charge (%s-%s RPM)");
        add("createoreexpansion.goggles.sapphire_charger_2", "High Charge (%s-%s RPM)");
        add("createoreexpansion.goggles.sapphire_charger_3", "Gamma Charge (%s-%s RPM)");
        add("createoreexpansion.goggles.sapphire_charger_4", "Overload Charge (%s-%s RPM)");
        add("createoreexpansion.goggles.sapphire_charger_5", "Ultimate Charge (%s+ RPM)");
        add("createoreexpansion.goggles.charger_idle", "No stress");
        add("createoreexpansion.goggles.energy_wave_regulator", "Jade Energy Wave Regulator");
        add("createoreexpansion.goggles.sapphire_wave_regulator", "Sapphire Wave Regulator");
        add("createoreexpansion.goggles.gate_need_speed", "Requires %s+ RPM to modulate");
        add("createoreexpansion.goggles.gate_speed_ok", "Modulating at %s RPM");
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
