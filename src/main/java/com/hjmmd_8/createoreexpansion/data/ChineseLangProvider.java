package com.hjmmd_8.createoreexpansion.data;

import com.hjmmd_8.createoreexpansion.common.AllModEffects;
import com.hjmmd_8.createoreexpansion.common.AllMyBlocks;
import com.hjmmd_8.createoreexpansion.common.AllItems;
import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ChineseLangProvider extends LanguageProvider {

    public ChineseLangProvider(PackOutput output) {
        super(output, CreateOreExpansion.MOD_ID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        COELangProvider.INSTANCE.addTranslations(this);
        add(AllItems.JADE_INGOT.get(), "翡翠锭");
        add(AllItems.RAW_JADE.get(), "粗翡翠");
        add(AllItems.JADE_NUGGET.get(), "翡翠粒");
        add(AllItems.CRUSHED_JADE_ORE.get(), "粉碎翡翠矿石");
        add(AllItems.JADE_SMALL_SHARD.get(), "小块翡翠");
        add(AllItems.JADE_BIG_SHARD.get(), "大块翡翠");
        add(AllItems.JADE_SHEET.get(), "翡翠板");
        add(AllItems.JADE_ROD.get(), "翡翠棍");
        add(AllItems.JADE_WIRE.get(), "翡翠线");
        add(AllItems.JADE_SWORD.get(), "翡翠剑");
        add(AllItems.JADE_PICKAXE.get(), "翡翠镐");
        add(AllItems.JADE_AXE.get(), "翡翠斧");
        add(AllItems.JADE_SHOVEL.get(), "翡翠铲");
        add(AllMyBlocks.JADE_CASING.get(), "翡翠机壳");
        add(AllItems.TOPAZ_INGOT.get(), "黄玉锭");
        add(AllItems.RAW_TOPAZ.get(), "粗黄玉");
        add(AllItems.TOPAZ_NUGGET.get(), "黄玉粒");
        add(AllItems.CRUSHED_TOPAZ_ORE.get(), "粉碎黄玉矿石");
        add(AllItems.TOPAZ_SMALL_SHARD.get(), "小块黄玉");
        add(AllItems.TOPAZ_BIG_SHARD.get(), "大块黄玉");
        add(AllItems.TOPAZ_SHEET.get(), "黄玉板");
        add(AllItems.TOPAZ_ROD.get(), "黄玉棍");
        add(AllItems.TOPAZ_WIRE.get(), "黄玉线");
        add(AllItems.TOPAZ_SWORD.get(), "黄玉剑");
        add(AllItems.TOPAZ_PICKAXE.get(), "黄玉镐");
        add(AllItems.TOPAZ_AXE.get(), "黄玉斧");
        add(AllItems.TOPAZ_SHOVEL.get(), "黄玉铲");
        add(AllItems.SAPPHIRE_INGOT.get(), "蓝宝石锭");
        add(AllItems.RAW_SAPPHIRE.get(), "粗蓝宝石");
        add(AllItems.SAPPHIRE_NUGGET.get(), "蓝宝石粒");
        add(AllItems.CRUSHED_SAPPHIRE_ORE.get(), "粉碎蓝宝石矿石");
        add(AllItems.SAPPHIRE_SMALL_SHARD.get(), "小块蓝宝石");
        add(AllItems.SAPPHIRE_BIG_SHARD.get(), "大块蓝宝石");
        add(AllItems.SAPPHIRE_SHEET.get(), "蓝宝石板");
        add(AllItems.SAPPHIRE_ROD.get(), "蓝宝石棍");
        add(AllItems.SAPPHIRE_WIRE.get(), "蓝宝石线");
        add(AllMyBlocks.JADE_ORE.get(), "翡翠矿石");
        add(AllMyBlocks.DEEPSLATE_JADE_ORE.get(), "深层翡翠矿石");
        add(AllMyBlocks.RAW_JADE_BLOCK.get(), "粗翡翠块");
        add(AllMyBlocks.JADE_BLOCK.get(), "翡翠块");
        add(AllMyBlocks.TOPAZ_ORE.get(), "黄玉矿石");
        add(AllMyBlocks.DEEPSLATE_TOPAZ_ORE.get(), "深层黄玉矿石");
        add(AllMyBlocks.RAW_TOPAZ_BLOCK.get(), "粗黄玉块");
        add(AllMyBlocks.TOPAZ_BLOCK.get(), "黄玉块");
        add(AllMyBlocks.NETHER_SAPPHIRE_ORE.get(), "下界蓝宝石矿石");
        add(AllMyBlocks.RAW_SAPPHIRE_BLOCK.get(), "粗蓝宝石块");
        add(AllMyBlocks.SAPPHIRE_BLOCK.get(), "蓝宝石块");
        add(AllItems.SAPPHIRE_SWORD.get(), "蓝宝石剑");
        add(AllItems.SAPPHIRE_PICKAXE.get(), "蓝宝石镐");
        add(AllItems.SAPPHIRE_AXE.get(), "蓝宝石斧");
        add(AllItems.SAPPHIRE_SHOVEL.get(), "蓝宝石铲");

        add("fluid_type.createoreexpansion.transmutation_fluid", "嬗变液");
        add("fluid.createoreexpansion.transmutation_fluid", "嬗变液");
        add("item.createoreexpansion.transmutation_fluid_bucket", "嬗变液桶");
        add("createoreexpansion.recipe.fan_transmuting", "批量嬗化");
        add("createoreexpansion.recipe.fan_transmuting.fan", "鼓风机");

//        add(AllItems.JADE_TOPAZ_BOW.get(), "翠玉之弓");
//        add("item.createoreexpansion.tool.energy", "能量");
//        add("item.createoreexpansion.jade_topaz_bow.tooltip.summary", "按住_左Shift_键_攻击_，箭矢造成_2倍伤害_并附加随机_负面效果_；_Shift_释放_技能A_，_Ctrl_释放_技能B_，均消耗_能量_");
//        add("item.createoreexpansion.jade_topaz_bow.tooltip.energy", "能量");
//        add("item.createoreexpansion.jade_pickaxe.tooltip.summary", "在使用_翡翠镐_时，按住_左Shift_键，可以_挖掘_面前_3×1范围_的方块");
//        add("item.createoreexpansion.jade_shovel.tooltip.summary", "在使用_翡翠铲_时，按住_左Shift_键，可以_挖掘_面前至多_6块泥土、沙子_等方块");
//        add("item.createoreexpansion.jade_axe.tooltip.summary", "在使用_翡翠斧_时，按住_左Shift_键，可以_砍伐树干_，但这需要一定的时间");
//        add("item.createoreexpansion.topaz_pickaxe.tooltip.summary", "在使用_黄玉镐_时，按住_左Shift_键，可以_挖掘_面前_3×3范围_的方块");
//        add("item.createoreexpansion.topaz_shovel.tooltip.summary", "在使用_黄玉铲_时，按住_左Shift_键，可以_挖掘_面前至多_9块泥土、沙子_等方块");
//        add("item.createoreexpansion.topaz_axe.tooltip.summary", "在使用_黄玉斧_时，按住_左Shift_键，可以_砍伐_面前的_树木_，但这需要一定的时间");
//        add("item.createoreexpansion.sapphire_pickaxe.tooltip.summary", "在使用_蓝宝石镐_时，按住_左Shift_键，可以_挖掘_面前_5×5范围_的方块");
//        add("item.createoreexpansion.sapphire_shovel.tooltip.summary", "在使用_蓝宝石铲_时，按住_左Shift_键，可以_挖掘_底部_7×7范围_的方块泥土、沙子等方块");
//        add("item.createoreexpansion.sapphire_axe.tooltip.summary", "在使用_蓝宝石斧_时，按住_左Shift_键，可以_快速砍伐_面前的_树木_，但这需要一定的时间");
//        add("item.createoreexpansion.jade_sword.tooltip.summary", "在使用_翡翠剑_时，按住_左Shift_键_攻击_，会造成_额外伤害_，并有_75%%_概率使目标_主手物品掉落_");
//        add("item.createoreexpansion.topaz_sword.tooltip.summary", "在使用_黄玉剑_时，按住_左Shift_键_攻击_，必定使目标_主手或装备掉落_，并有_50%%_概率直接_偷取至背包_");
//        add("item.createoreexpansion.sapphire_sword.tooltip.summary", "在使用_蓝宝石剑_时，按住_左Shift_键_攻击_，会将目标_全部武器装备转移至背包_，并从目标身上_吸取4点生命_");
        add(AllModEffects.TRANSMUTATION_DISORDER.get(), "嬗乱");
        add("item.minecraft.potion.effect.transmutation_disorder", "嬗乱药水");
        add("item.minecraft.splash_potion.effect.transmutation_disorder", "喷溅型嬗乱药水");
        add("item.minecraft.lingering_potion.effect.transmutation_disorder", "滞留型嬗乱药水");
        add("item.minecraft.tipped_arrow.effect.transmutation_disorder", "嬗乱之箭");
        add("item.minecraft.potion.effect.strong_transmutation_disorder", "强效嬗乱药水");
        add("item.minecraft.splash_potion.effect.strong_transmutation_disorder", "喷溅型强效嬗乱药水");
        add("item.minecraft.lingering_potion.effect.strong_transmutation_disorder", "滞留型强效嬗乱药水");
        add("item.minecraft.tipped_arrow.effect.strong_transmutation_disorder", "强效嬗乱之箭");
        add("item.minecraft.potion.effect.long_transmutation_disorder", "漫长的嬗乱药水");
        add("item.minecraft.splash_potion.effect.long_transmutation_disorder", "喷溅型漫长的嬗乱药水");
        add("item.minecraft.lingering_potion.effect.long_transmutation_disorder", "滞留型漫长的嬗乱药水");
        add("item.minecraft.tipped_arrow.effect.long_transmutation_disorder", "漫长的嬗乱之箭");
        add("create.tooltip.holdForDescription", "按住 [%1$s] 可查看概要");
        add("create.tooltip.holdForControls", "按住 [%1$s] 可查看控制方法");
        add("create.tooltip.keyShift", "Shift");
        add("create.tooltip.keyCtrl", "Ctrl");
    }
}
