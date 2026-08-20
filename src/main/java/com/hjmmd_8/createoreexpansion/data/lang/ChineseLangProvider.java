package com.hjmmd_8.createoreexpansion.data.lang;

import com.hjmmd_8.createoreexpansion.common.AllBlocks;
import com.hjmmd_8.createoreexpansion.common.AllItems;
import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * 中文语言文件 —— 全模组中文翻译的唯一定义处。
 *
 * 包含：创造标签页、物品/方块、技能名、技能类型、技能释放键、tooltip、药水效果等全部翻译。
 */
public class ChineseLangProvider extends LanguageProvider {

    public ChineseLangProvider(PackOutput output) {
        super(output, CreateOreExpansion.MOD_ID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        // ========== 创造标签页 ==========
        add("itemGroup.createoreexpansion", "机械动力：矿物拓展");
        add("createoreexpansion.mod_name", "机械动力：矿物拓展");

        // ========== 物品/方块 ==========
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
        add(AllItems.JADE_HOE.get(), "翡翠锄");
        add(AllBlocks.JADE_CASING.get(), "翡翠机壳");
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
        add(AllItems.TOPAZ_HOE.get(), "黄玉锄");
        add(AllItems.SAPPHIRE_INGOT.get(), "蓝宝石锭");
        add(AllItems.RAW_SAPPHIRE.get(), "粗蓝宝石");
        add(AllItems.SAPPHIRE_NUGGET.get(), "蓝宝石粒");
        add(AllItems.CRUSHED_SAPPHIRE_ORE.get(), "粉碎蓝宝石矿石");
        add(AllItems.SAPPHIRE_SMALL_SHARD.get(), "小块蓝宝石");
        add(AllItems.SAPPHIRE_BIG_SHARD.get(), "大块蓝宝石");
        add(AllItems.SAPPHIRE_SHEET.get(), "蓝宝石板");
        add(AllItems.SAPPHIRE_ROD.get(), "蓝宝石棍");
        add(AllItems.SAPPHIRE_WIRE.get(), "蓝宝石线");
        add(AllBlocks.JADE_ORE.get(), "翡翠矿石");
        add(AllBlocks.DEEPSLATE_JADE_ORE.get(), "深层翡翠矿石");
        add(AllBlocks.RAW_JADE_BLOCK.get(), "粗翡翠块");
        add(AllBlocks.JADE_BLOCK.get(), "翡翠块");
        add(AllBlocks.TOPAZ_ORE.get(), "黄玉矿石");
        add(AllBlocks.DEEPSLATE_TOPAZ_ORE.get(), "深层黄玉矿石");
        add(AllBlocks.RAW_TOPAZ_BLOCK.get(), "粗黄玉块");
        add(AllBlocks.TOPAZ_BLOCK.get(), "黄玉块");
        add(AllBlocks.NETHER_SAPPHIRE_ORE.get(), "下界蓝宝石矿石");
        add(AllBlocks.RAW_SAPPHIRE_BLOCK.get(), "粗蓝宝石块");
        add(AllBlocks.SAPPHIRE_BLOCK.get(), "蓝宝石块");
        add(AllItems.SAPPHIRE_SWORD.get(), "蓝宝石剑");
        add(AllItems.SAPPHIRE_PICKAXE.get(), "蓝宝石镐");
        add(AllItems.SAPPHIRE_AXE.get(), "蓝宝石斧");
        add(AllItems.SAPPHIRE_SHOVEL.get(), "蓝宝石铲");
        add(AllItems.SAPPHIRE_HOE.get(), "蓝宝石锄");
        add(AllItems.STELLARSTONE_INGOT.get(), "星辉石锭");
        add(AllItems.RAW_STELLARSTONE.get(), "粗星辉石");
        add(AllItems.STELLARSTONE_NUGGET.get(), "星辉石粒");
        add(AllItems.CRUSHED_STELLARSTONE_ORE.get(), "粉碎星辉石矿石");
        add(AllItems.STELLARSTONE_SMALL_SHARD.get(), "小块星辉石");
        add(AllItems.STELLARSTONE_BIG_SHARD.get(), "大块星辉石");
        add(AllItems.STELLARSTONE_SHEET.get(), "星辉石板");
        add(AllItems.STELLARSTONE_ROD.get(), "星辉石棍");
        add(AllItems.STELLARSTONE_WIRE.get(), "星辉石线");
        add(AllItems.STELLARSTONE_SWORD.get(), "星辉石剑");
        add(AllItems.STELLARSTONE_PICKAXE.get(), "星辉石镐");
        add(AllItems.STELLARSTONE_AXE.get(), "星辉石斧");
        add(AllItems.STELLARSTONE_SHOVEL.get(), "星辉石铲");
        add(AllItems.STELLARSTONE_HOE.get(), "星辉石锄");
        add(AllBlocks.END_STELLARSTONE_ORE.get(), "末地星辉石矿石");
        add(AllBlocks.RAW_STELLARSTONE_BLOCK.get(), "粗星辉石块");
        add(AllBlocks.STELLARSTONE_BLOCK.get(), "星辉石块");
        add(AllItems.THUNDERITE_SCRAP.get(), "雷鸣合金碎片");
        add(AllItems.THUNDERITE_INGOT.get(), "雷鸣合金锭");
        add(AllItems.THUNDERITE_SHEET.get(), "雷鸣合金板");
        add(AllItems.THUNDERITE_ROD.get(), "雷鸣合金棍");
        add(AllItems.THUNDERITE_WIRE.get(), "雷鸣合金线");
        add(AllItems.THUNDERITE_SWORD.get(), "雷鸣合金剑");
        add(AllItems.THUNDERITE_PICKAXE.get(), "雷鸣合金镐");
        add(AllItems.THUNDERITE_AXE.get(), "雷鸣合金斧");
        add(AllItems.THUNDERITE_SHOVEL.get(), "雷鸣合金铲");
        add(AllItems.THUNDERITE_HOE.get(), "雷鸣合金锄");
        add(AllBlocks.THUNDERITE_BLOCK.get(), "雷鸣合金块");
        add(AllItems.JADE_TOPAZ_BOW.get(), "翠玉之弓");
        add(AllItems.JADE_STRESS_MEDALLION.get(), "翡翠凝能佩");
        add(AllItems.TOPAZ_STRESS_MEDALLION.get(), "黄玉凝能佩");
        add(AllItems.SAPPHIRE_STRESS_MEDALLION.get(), "沧蓝凝能佩");
        add(AllItems.NETHERITE_STRESS_MEDALLION.get(), "狱红怪佩");
        add(AllItems.STELLARSTONE_STRESS_MEDALLION.get(), "星辉凝能佩");
        add(AllItems.THUNDERITE_STRESS_MEDALLION.get(), "雷暴凝能佩");

        // ========== 凝能佩 Shift 概要 ==========
        add("item.createoreexpansion.medallion.hold_shift", "按住 [%1$s] 查看概要");
        add("item.createoreexpansion.medallion.summary", "凝能之佩，_储存_应力能量并为_绑定_工具_供能_。");
        add("item.createoreexpansion.medallion.bind", "绑定：手持本佩右键（另一手为能量工具）可_绑定_，或在合成格与工具合成；已绑定工具单独合成可_解绑_，与另一枚佩合成可_换绑_");
        add("item.createoreexpansion.medallion.mode", "模式：手持本佩右键（另一手非工具）在_供应_与_充能_模式间切换");
        add("item.createoreexpansion.medallion.supply", "供能：_供应模式_穿戴后释放技能_优先消耗_本佩能量；_充能模式_会在释放后把_绑定工具_补满");

        // ========== 技能名（各等级共用基础键，等级由 tooltip 罗马数字显示） ==========
        add("skill.createoreexpansion.fell", "伐树");
        add("skill.createoreexpansion.shatter", "开岩");
        add("skill.createoreexpansion.channel", "引渠");
        add("skill.createoreexpansion.grade", "平场");
        add("skill.createoreexpansion.skin", "剥取");
        add("skill.createoreexpansion.plunder", "夺取");
        add("skill.createoreexpansion.hoe", "耕作");

        // ========== 技能类型 ==========
        add("skillType.createoreexpansion.excavation_skill", "挖掘技能");
        add("skillType.createoreexpansion.hit_skill", "攻击技能");
        add("skillType.createoreexpansion.use_skill", "使用技能");

        // ========== 技能释放键 ==========
        add("createoreexpansion.keyinfo.skill_release", "技能释放1（第一技能）");
        add("createoreexpansion.keyinfo.skill_release_2", "技能释放 2（第二技能）");
        add("createoreexpansion.keyinfo.skill_release_3", "技能释放 3（第三技能）");

        // ========== 工具/技能 tooltip ==========
        add("item.createoreexpansion.tool.skill_tips", "按住 [%s] 可查看技能概要");
        add("item.createoreexpansion.tool.energy", "能量");

        // ========== 附魔 ==========
        add("enchantment.createoreexpansion.reduce_consumption", "减耗");
        add("enchantment.createoreexpansion.swift_start", "迅启");
        add("enchantment.createoreexpansion.skill_boost", "技艺提升");
        add("enchantment.createoreexpansion.skill_regression", "技艺回溯");

        // ========== 流体/配方 ==========
        add("fluid_type.createoreexpansion.transmutation_fluid", "嬗变液");
        add("fluid.createoreexpansion.transmutation_fluid", "嬗变液");
        add("block.createoreexpansion.transmutation_fluid", "嬗变液");
        add("block.createoreexpansion.power_angle_grinder", "动力角磨床");
        add("createoreexpansion.msg.cannot_open_cover", "由于空间不足，无法开盖");
        add("createoreexpansion.msg.need_open_cover", "需要先开盖才能安装角磨轮");
        add(AllItems.IRON_GRINDING_WHEEL.get(), "铁角磨轮");
        add(AllItems.JADE_GRINDING_WHEEL.get(), "翡翠角磨轮");
        add(AllItems.DIAMOND_GRINDING_WHEEL.get(), "钻石角磨轮");
        add(AllItems.TOPAZ_GRINDING_WHEEL.get(), "黄玉角磨轮");
        add(AllItems.SAPPHIRE_GRINDING_WHEEL.get(), "蓝宝石角磨轮");
        add(AllItems.STELLARSTONE_GRINDING_WHEEL.get(), "星辉石角磨轮");
        add("item.createoreexpansion.transmutation_fluid_bucket", "嬗变液桶");
        add("createoreexpansion.recipe.fan_transmuting", "批量嬗化");
        add("createoreexpansion.recipe.fan_transmuting.fan", "鼓风机");
        add("createoreexpansion.recipe.lightning", "闪电转化");
        add("createoreexpansion.recipe.lightning_block", "闪电转化·方块");
        add("createoreexpansion.recipe.grinding", "角磨加工");
        add("createoreexpansion.recipe.advanced_grinding", "高级角磨");
        add("createoreexpansion.recipe.dismantling", "拆磨");
        add("createoreexpansion.recipe.dismantling.output", "输出数量取决于装备剩余耐久");
        add("createoreexpansion.recipe.assembly.grinding", "在动力角磨床中角磨");

        // ========== 动力角磨床护目镜提示 ==========
        add("createoreexpansion.goggles.angle_grinder", "动力角磨床");
        add("createoreexpansion.goggles.no_wheel", "未安装角磨轮！");
        add("createoreexpansion.goggles.installed_wheel", "角磨轮：%s");
        add("createoreexpansion.goggles.required_speed", "所需转速：≥ %s RPM");
        add("createoreexpansion.goggles.speed_too_low", "转速不足，无法加工！");
        add("createoreexpansion.goggles.processing_time", "当前加工耗时：%s 秒");

        // ========== 动力角磨床悬停提示（支持的加工类型） ==========
        add("createoreexpansion.tooltip.no_wheel_type", "未安装角磨轮，无法加工");
        add("createoreexpansion.tooltip.supported_types", "支持的加工类型：");
        add("createoreexpansion.tooltip.type_grinding", "· 基础角磨");
        add("createoreexpansion.tooltip.type_advanced", "· 粉碎、研磨（高级角磨）");
        add("createoreexpansion.tooltip.type_dismantling", "· 装备/武器拆解（拆磨）");

        // ========== 药水效果 ==========
        add("effect.createoreexpansion.transmutation_disorder", "嬗乱");
        add("item.minecraft.potion.effect.transmutation_disorder", "嬗乱药水");
        add("item.minecraft.splash_potion.effect.transmutation_disorder", "喷溅型嬗乱药水");
        add("item.minecraft.lingering_potion.effect.transmutation_disorder", "滞留型嬗乱药水");
        add("item.minecraft.tipped_arrow.effect.transmutation_disorder", "嬗乱之箭");
        add("item.minecraft.potion.effect.strong_transmutation_disorder", "强效嬗乱药水");
        add("item.minecraft.splash_potion.effect.strong_transmutation_disorder", "强效喷溅型嬗乱药水");
        add("item.minecraft.lingering_potion.effect.strong_transmutation_disorder", "强效滞留型嬗乱药水");
        add("item.minecraft.tipped_arrow.effect.strong_transmutation_disorder", "强效嬗乱之箭");
        add("item.minecraft.potion.effect.long_transmutation_disorder", "漫长的嬗乱药水");
        add("item.minecraft.splash_potion.effect.long_transmutation_disorder", "漫长喷溅型嬗乱药水");
        add("item.minecraft.lingering_potion.effect.long_transmutation_disorder", "漫长滞留型嬗乱药水");
        add("item.minecraft.tipped_arrow.effect.long_transmutation_disorder", "漫长的嬗乱之箭");

        // ========== Create 通用 ==========
        add("create.tooltip.holdForDescription", "按住 [%1$s] 可查看概要");
        add("create.tooltip.holdForControls", "按住 [%1$s] 可查看控制方法");
        add("create.tooltip.keyShift", "Shift");
        add("create.tooltip.keyCtrl", "Ctrl");
    }
}
