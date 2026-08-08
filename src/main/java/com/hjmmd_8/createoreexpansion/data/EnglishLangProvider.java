package com.hjmmd_8.createoreexpansion.data;

import com.hjmmd_8.createoreexpansion.common.AllMyBlocks;
import com.hjmmd_8.createoreexpansion.common.AllItems;
import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class EnglishLangProvider extends LanguageProvider {

    public EnglishLangProvider(PackOutput output) {
        super(output, CreateOreExpansion.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        COELangProvider.INSTANCE.addTranslations(this);
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
        add(AllItems.RAW_SAPPHIRE.get(), "Raw Sapphire");
        add(AllItems.SAPPHIRE_INGOT.get(), "Sapphire Ingot");
        add(AllItems.SAPPHIRE_NUGGET.get(), "Sapphire Nugget");
        add(AllItems.CRUSHED_SAPPHIRE_ORE.get(), "Crushed Sapphire Ore");
        add(AllItems.SAPPHIRE_SMALL_SHARD.get(), "Small Sapphire Shard");
        add(AllItems.SAPPHIRE_BIG_SHARD.get(), "Big Sapphire Shard");
        add(AllItems.SAPPHIRE_SHEET.get(), "Sapphire Sheet");
        add(AllItems.SAPPHIRE_ROD.get(), "Sapphire Rod");
        add(AllItems.SAPPHIRE_WIRE.get(), "Sapphire Wire");
        add(AllMyBlocks.JADE_ORE.get(), "Jade Ore");
        add(AllMyBlocks.DEEPSLATE_JADE_ORE.get(), "Deepslate Jade Ore");
        add(AllMyBlocks.RAW_JADE_BLOCK.get(), "Raw Jade Block");
        add(AllMyBlocks.JADE_BLOCK.get(), "Jade Block");
        add(AllMyBlocks.TOPAZ_ORE.get(), "Topaz Ore");
        add(AllMyBlocks.DEEPSLATE_TOPAZ_ORE.get(), "Deepslate Topaz Ore");
        add(AllMyBlocks.RAW_TOPAZ_BLOCK.get(), "Raw Topaz Block");
        add(AllMyBlocks.TOPAZ_BLOCK.get(), "Topaz Block");
        add(AllMyBlocks.NETHER_SAPPHIRE_ORE.get(), "Nether Sapphire Ore");
        add(AllMyBlocks.RAW_SAPPHIRE_BLOCK.get(), "Raw Sapphire Block");
        add(AllMyBlocks.SAPPHIRE_BLOCK.get(), "Sapphire Block");
        add(AllItems.SAPPHIRE_SWORD.get(), "Sapphire Sword");
        add(AllItems.SAPPHIRE_PICKAXE.get(), "Sapphire Pickaxe");
        add(AllItems.SAPPHIRE_AXE.get(), "Sapphire Axe");
        add(AllItems.SAPPHIRE_SHOVEL.get(), "Sapphire Shovel");

        add("fluid_type.createoreexpansion.transmutation_fluid", "Transmutation Fluid");
        add("item.createoreexpansion.transmutation_fluid_bucket", "Transmutation Fluid Bucket");
        add("createoreexpansion.recipe.fan_transmuting", "Transmuting");
        add("createoreexpansion.recipe.fan_transmuting.fan", "Encased Fan");

        add(AllItems.JADE_TOPAZ_BOW.get(), "Jade Topaz Bow");
        add("item.createoreexpansion.tool.energy", "Energy");
//        add("item.createoreexpansion.jade_topaz_bow.tooltip.summary", "Holding _Left Shift_ while attacking fires arrows with _2x damage_ and random _debuffs_; _Shift_ casts _Skill A_, _Ctrl_ casts _Skill B_, both consuming _energy_");
//        add("item.createoreexpansion.jade_topaz_bow.tooltip.energy", "Energy");
//        add("item.createoreexpansion.jade_pickaxe.tooltip.summary",
//                "While holding _Left Shift_, allows you to _mine_ a _3x1 area_ of blocks in front of you");
//        add("item.createoreexpansion.jade_shovel.tooltip.summary",
//                "While holding _Left Shift_, allows you to _dig_ up to _6 blocks_ of dirt, sand, and similar blocks");
//        add("item.createoreexpansion.jade_axe.tooltip.summary",
//                "While holding _Left Shift_, allows you to _chop tree trunks_, but requires additional time");
//        add("item.createoreexpansion.topaz_pickaxe.tooltip.summary",
//                "While holding _Left Shift_, allows you to _mine_ a _3x3 area_ of blocks in front of you");
//        add("item.createoreexpansion.topaz_shovel.tooltip.summary",
//                "While holding _Left Shift_, allows you to _dig_ up to _9 blocks_ of dirt, sand, and similar blocks");
//        add("item.createoreexpansion.topaz_axe.tooltip.summary",
//                "While holding _Left Shift_, allows you to _chop down trees_ in front of you, but requires additional time");
//        add("item.createoreexpansion.sapphire_pickaxe.tooltip.summary",
//                "While holding _Left Shift_, allows you to _mine_ a _5x5 area_ of blocks in front of you");
//        add("item.createoreexpansion.sapphire_shovel.tooltip.summary",
//                "While holding _Left Shift_, allows you to _dig_ a _7x7 area_ of dirt, sand, and similar blocks beneath you");
//        add("item.createoreexpansion.sapphire_axe.tooltip.summary",
//                "While holding _Left Shift_, allows you to _quickly chop down trees_ in front of you, but requires additional time");
//        add("item.createoreexpansion.jade_sword.tooltip.summary",
//                "While holding _Left Shift_, attacking deals _bonus damage_ and has a _75%%_ chance to drop the target's _main hand item_");
//        add("item.createoreexpansion.topaz_sword.tooltip.summary",
//                "While holding _Left Shift_, attacking makes the target drop its _main hand item_ or _equipment_, with a _50%%_ chance to _steal_ it into your inventory");
//        add("item.createoreexpansion.sapphire_sword.tooltip.summary",
//                "While holding _Left Shift_, attacking moves all of the target's _weapons and equipment_ into your inventory and _drains 4 HP_ from the target");
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
