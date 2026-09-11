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
        add(AllBlocks.SAPPHIRE_CASING.get(), "蓝宝石机壳");
        add(AllItems.SAPPHIRE_INGOT.get(), "蓝宝石锭");
        add(AllItems.RAW_SAPPHIRE.get(), "粗蓝宝石");
        add(AllItems.SAPPHIRE_NUGGET.get(), "蓝宝石粒");
        add(AllItems.CRUSHED_SAPPHIRE_ORE.get(), "粉碎蓝宝石矿石");
        add(AllItems.SAPPHIRE_SMALL_SHARD.get(), "小块蓝宝石");
        add(AllItems.SAPPHIRE_BIG_SHARD.get(), "大块蓝宝石");
        add(AllItems.SAPPHIRE_SHEET.get(), "蓝宝石板");
        add(AllItems.SAPPHIRE_ROD.get(), "蓝宝石棍");
        add(AllItems.SAPPHIRE_WIRE.get(), "蓝宝石线");
        add(AllItems.RUBY_INGOT.get(), "红宝石锭");
        add(AllItems.RUBY_SHEET.get(), "红宝石板");
        add(AllItems.RUBY_ROD.get(), "红宝石棍");
        add(AllItems.RUBY_WIRE.get(), "红宝石线");
        add(AllBlocks.RUBY_BLOCK.get(), "红宝石块");
        add(AllBlocks.JADE_ORE.get(), "翡翠矿石");
        add(AllBlocks.DEEPSLATE_JADE_ORE.get(), "深层翡翠矿石");
        add(AllBlocks.RAW_JADE_BLOCK.get(), "粗翡翠块");
        add(AllBlocks.JADE_BLOCK.get(), "翡翠块");
        // 翡翠可生长水晶
        add(AllBlocks.JADE_BUDDING_BLOCK.get(), "翡翠水晶芽床");
        add(AllBlocks.JADE_SMALL_BUD.get(), "翡翠水晶小芽");
        add(AllBlocks.JADE_MEDIUM_BUD.get(), "翡翠水晶中芽");
        add(AllBlocks.JADE_LARGE_BUD.get(), "翡翠水晶大芽");
        add(AllBlocks.JADE_CLUSTER.get(), "翡翠水晶簇");
        add(AllBlocks.TOPAZ_ORE.get(), "黄玉矿石");
        add(AllBlocks.DEEPSLATE_TOPAZ_ORE.get(), "深层黄玉矿石");
        add(AllBlocks.RAW_TOPAZ_BLOCK.get(), "粗黄玉块");
        add(AllBlocks.TOPAZ_BLOCK.get(), "黄玉块");
        // 黄玉可生长水晶
        add(AllBlocks.TOPAZ_BUDDING_BLOCK.get(), "黄玉水晶芽床");
        add(AllBlocks.TOPAZ_SMALL_BUD.get(), "黄玉水晶小芽");
        add(AllBlocks.TOPAZ_MEDIUM_BUD.get(), "黄玉水晶中芽");
        add(AllBlocks.TOPAZ_LARGE_BUD.get(), "黄玉水晶大芽");
        add(AllBlocks.TOPAZ_CLUSTER.get(), "黄玉水晶簇");
        add(AllBlocks.NETHER_SAPPHIRE_ORE.get(), "下界蓝宝石矿石");
        add(AllBlocks.RAW_SAPPHIRE_BLOCK.get(), "粗蓝宝石块");
        add(AllBlocks.SAPPHIRE_BLOCK.get(), "蓝宝石块");
        // 蓝宝石可生长水晶
        add(AllBlocks.SAPPHIRE_BUDDING_BLOCK.get(), "蓝宝石水晶芽床");
        add(AllBlocks.SAPPHIRE_SMALL_BUD.get(), "蓝宝石水晶小芽");
        add(AllBlocks.SAPPHIRE_MEDIUM_BUD.get(), "蓝宝石水晶中芽");
        add(AllBlocks.SAPPHIRE_LARGE_BUD.get(), "蓝宝石水晶大芽");
        add(AllBlocks.SAPPHIRE_CLUSTER.get(), "蓝宝石水晶簇");
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
        add(AllItems.SANCTSTONE_INGOT.get(), "星芒石锭");
        add(AllItems.SANCTSTONE_SHEET.get(), "星芒石板");
        add(AllItems.SANCTSTONE_ROD.get(), "星芒石棍");
        add(AllItems.SANCTSTONE_WIRE.get(), "星芒石线");
        add(AllBlocks.SANCTSTONE_BLOCK.get(), "星芒石块");
        add(AllItems.STELLARSTONE_SWORD.get(), "星辉石剑");
        add(AllItems.STELLARSTONE_PICKAXE.get(), "星辉石镐");
        add(AllItems.STELLARSTONE_AXE.get(), "星辉石斧");
        add(AllItems.STELLARSTONE_SHOVEL.get(), "星辉石铲");
        add(AllItems.STELLARSTONE_HOE.get(), "星辉石锄");
        add(AllBlocks.END_STELLARSTONE_ORE.get(), "末地星辉石矿石");
        add(AllBlocks.RAW_STELLARSTONE_BLOCK.get(), "粗星辉石块");
        add(AllBlocks.STELLARSTONE_BLOCK.get(), "星辉石块");
        // 星辉石可生长水晶
        add(AllBlocks.STELLARSTONE_BUDDING_BLOCK.get(), "星辉石水晶芽床");
        add(AllBlocks.STELLARSTONE_SMALL_BUD.get(), "星辉石水晶小芽");
        add(AllBlocks.STELLARSTONE_MEDIUM_BUD.get(), "星辉石水晶中芽");
        add(AllBlocks.STELLARSTONE_LARGE_BUD.get(), "星辉石水晶大芽");
        add(AllBlocks.STELLARSTONE_CLUSTER.get(), "星辉石水晶簇");
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

        // ========== 构件与杂项 ==========
        add(AllItems.LUCKY_DUST.get(), "幸运之尘");
        add(AllItems.ENERGY_MECHANISM.get(), "能量构件");
        add(AllItems.INCOMPLETE_ENERGY_MECHANISM.get(), "未完成的能量构件");
        add(AllItems.TRANSMUTE_MECHANISM.get(), "嬗化构件");
        add(AllItems.INCOMPLETE_TRANSMUTE_MECHANISM.get(), "未完成的嬗化构件");

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
        add("skill.createoreexpansion.bow_curse", "凋零诅咒");
        add("skill.createoreexpansion.bow_disarm", "缴械风暴");

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
        add("block.createoreexpansion.jade_stress_charger", "翡翠应力充能器");
        add("block.createoreexpansion.sapphire_stress_charger", "蓝宝石应力充能器");
        add("block.createoreexpansion.stellarstone_stress_charger", "星辉石应力充能器");
        add("block.createoreexpansion.stellar_wave_transmuter", "星辉波变器");
        add("createoreexpansion.goggles.stellar_wave_transmuter", "星辉波变器");
        add("createoreexpansion.goggles.stellar_wave_transmuter_idle", "未接入应力");
        add("createoreexpansion.goggles.stellar_wave_transmuter_radius", "扫描半径：%s 格");
        add("createoreexpansion.goggles.stellar_wave_transmuter_heat", "读取到加热：%s（烈焰燃烧室）");
        add("createoreexpansion.goggles.stellar_wave_transmuter_heat_none", "读取到加热：无（扫描半径内没有点燃的烈焰燃烧室）");
        add("createoreexpansion.goggles.stellar_wave_transmuter_item_containers", "读取到物品容器：%s 台（箱子/工作盆/抽屉等）");
        add("createoreexpansion.goggles.stellar_wave_transmuter_fluid_containers", "读取到流体容器：%s 台（储罐/流体抽屉等）");
        add("createoreexpansion.goggles.stellar_wave_transmuter_energy_storages", "读取到储能设备：%s 台（发电机/蓄电池等，合计 %s FE）");
        add("createoreexpansion.goggles.stellar_wave_transmuter_wave_rpm", "波加工转速：%s RPM（取自本机转速）");
        add("createoreexpansion.goggles.stellar_wave_transmuter_machines", "已接入加工机：%s");
        add("createoreexpansion.goggles.stellar_wave_transmuter_stress", "加工机应力合计：%s");
        add("createoreexpansion.goggles.stellar_wave_transmuter_types", "携带配方类型：%s");
        add("createoreexpansion.goggles.stellar_wave_transmuter_items", "辅料载荷：%s 个 / %s 种");
        add("createoreexpansion.goggles.stellar_wave_transmuter_fluid", "流体载荷：%s mB");
        add("createoreexpansion.goggles.stellar_wave_transmuter_energy", "电量载荷：%s FE");
        add("createoreexpansion.goggles.stellar_wave_transmuter_rods", "避雷针机会：%s");
        add("createoreexpansion.goggles.stellar_wave_transmuter_last_wave", "最近波可加工：");
        add("createoreexpansion.goggles.stellar_wave_transmuter_bind_hint", "已绑定 %s 台机器。按 [%s] 显示加工配方");
        add("createoreexpansion.goggles.stellar_wave_transmuter_bind_hint_shift", "已绑定 %s 台机器。按住 [%s] 显示加工配方");
        // 配方清单单行汇总：分隔符与超量省略（顿号连接，超 10 项补"等"）
        add("createoreexpansion.goggles.list_separator", "、");
        add("createoreexpansion.goggles.list_etc", "等");
        // 配方类型显示名（护目镜"最近波可加工"面板；Create/Vintage/CCA/光学/本模组）
        add("createoreexpansion.recipe_type.pressing", "压片");
        add("createoreexpansion.recipe_type.cutting", "切割");
        add("createoreexpansion.recipe_type.milling", "研磨");
        add("createoreexpansion.recipe_type.crushing", "粉碎");
        add("createoreexpansion.recipe_type.splashing", "喷洗");
        add("createoreexpansion.recipe_type.haunting", "闹鬼");
        add("createoreexpansion.recipe_type.deploying", "部署");
        add("createoreexpansion.recipe_type.item_application", "物品应用");
        add("createoreexpansion.recipe_type.mixing", "搅拌");
        add("createoreexpansion.recipe_type.focusing", "聚焦");
        add("createoreexpansion.recipe_type.coiling", "卷绕");
        add("createoreexpansion.recipe_type.curving", "弯折");
        add("createoreexpansion.recipe_type.polishing", "打磨");
        add("createoreexpansion.recipe_type.centrifugation", "离心");
        add("createoreexpansion.recipe_type.vibrating", "振动");
        add("createoreexpansion.recipe_type.leaves_vibrating", "落叶振动");
        add("createoreexpansion.recipe_type.pressurizing", "加压");
        add("createoreexpansion.recipe_type.vacuumizing", "抽真空");
        add("createoreexpansion.recipe_type.hammering", "锤锻");
        add("createoreexpansion.recipe_type.auto_smithing", "自动锻造");
        add("createoreexpansion.recipe_type.auto_upgrade", "自动升级");
        add("createoreexpansion.recipe_type.turning", "车削");
        add("createoreexpansion.recipe_type.laser_cutting", "激光切割");
        add("createoreexpansion.recipe_type.charging", "充能");
        add("createoreexpansion.recipe_type.rolling", "辊压");
        add("createoreexpansion.recipe_type.lightning", "闪电转化");
        add("createoreexpansion.recipe_type.grinding", "打磨");
        add("createoreexpansion.recipe_type.dismantling", "拆解");
        // 烈焰燃烧室热档显示名（护目镜加热读数 / Jade 携带加热；序与 Create HeatLevel 一致）
        add("createoreexpansion.heat_level.none", "无");
        add("createoreexpansion.heat_level.smouldering", "余烬");
        add("createoreexpansion.heat_level.fading", "将熄");
        add("createoreexpansion.heat_level.kindled", "普通加热");
        add("createoreexpansion.heat_level.seething", "超级加热");
        add("createoreexpansion.recipe.stellar_wave_transmuter", "星辉波变器");
        add("createoreexpansion.jei.transmuter.line1", "① 水平飞行的能量波从侧面射入，被转成“变体波”并从对侧穿出；竖直撞上下面会像撞墙一样消散。");
        add("createoreexpansion.jei.transmuter.line2", "② 变体波携带扫描到的机器配方类型，以及半径内点燃的烈焰燃烧室提供的加热能力（普通/超级加热 → 加热搅拌等加热配方可用）；链式加工次数 = 波等级（每次成功加工 −1）。");
        add("createoreexpansion.jei.transmuter.line3", "③ 波还携带载荷：辅料物品（最多 5 个/5 种）、流体（最多 500 mB）、电量（如特斯拉线圈全抽）与避雷针释放机会（穿波时抽取 1 次）。");
        add("createoreexpansion.jei.transmuter.line4", "④ 命中物品执行匹配配方：双输入消耗辅料，流体/电量型条目按需扣减；链尽或消散时剩余载荷存回附近容器/储罐/储能，放不下则掉落/浪费。");
        add("createoreexpansion.goggles.stellarstone_charger", "星辉石应力充能器");
        add("createoreexpansion.goggles.stellarstone_charger_manual_level", "手动发射波等级：%s");
        add("createoreexpansion.goggles.stellarstone_charger_normal", "模式：普通（连续发射）");
        add("createoreexpansion.goggles.stellarstone_charger_storing", "模式：储存（满后点击/红石触发簇射）");
        add("createoreexpansion.goggles.stellarstone_charger_store_layers", "充能层数：%s/%s");
        add("createoreexpansion.goggles.stellarstone_charger_store_full", "已满：右键释放");
        add("createoreexpansion.charger.manual_level", "发射波等级");
        // 等级槽调整面板的左侧行标（Create 原版对 ScrollValueBehaviour 硬编码英文 "Value"）
        add("createoreexpansion.charger.level_row", "等级");
        add("block.createoreexpansion.energy_field_controller", "能量场控制器");
        add("block.createoreexpansion.sapphire_wave_regulator", "蓝宝石能量调级器");
        add("block.createoreexpansion.sapphire_speed_regulator", "蓝宝石波速调节器");
        add("createoreexpansion.goggles.sapphire_speed_regulator", "蓝宝石波速调节器");
        add("block.createoreexpansion.stellarstone_wave_regulator", "星辉石能量调级器");
        add("block.createoreexpansion.stellarstone_speed_regulator", "星辉石波速调节器");
        add("createoreexpansion.goggles.stellarstone_wave_regulator", "星辉石能量调级器");
        add("createoreexpansion.goggles.stellarstone_speed_regulator", "星辉石波速调节器");
        add("createoreexpansion.goggles.stellarstone_wave_boost", "单次提升级数：+%s 级");
        add("createoreexpansion.goggles.sapphire_charger_normal", "模式：普通（连续发射）");
        add("createoreexpansion.goggles.sapphire_charger_storing", "模式：储存（满后点击/红石触发簇射）");
        add("createoreexpansion.charger.mode", "充能模式");
        add("createoreexpansion.charger_mode.normal", "普通");
        add("createoreexpansion.charger_mode.store", "储存");
        add("createoreexpansion.goggles.sapphire_charger_store_progress", "能量储备：%s%%");
        add("createoreexpansion.goggles.sapphire_charger_store_layers", "充能层数：%s/%s");
        add("createoreexpansion.goggles.sapphire_charger_store_full", "已满：右键释放");
        add("createoreexpansion.goggles.sapphire_charger_store_line", "本层进度：%s/%s");
        add("createoreexpansion.goggles.sapphire_charger_1", "低充能态（%s~%s RPM）");
        add("createoreexpansion.goggles.sapphire_charger_2", "高充能态（%s~%s RPM）");
        add("createoreexpansion.goggles.sapphire_charger_3", "伽马充能态（%s~%s RPM）");
        add("createoreexpansion.goggles.sapphire_charger_4", "超载充能态（%s~%s RPM）");
        add("createoreexpansion.goggles.sapphire_charger_5", "终极充能态（≥%s RPM）");
        add("block.createoreexpansion.energy_wave_regulator", "翡翠能量调级器");
        add("block.createoreexpansion.wave_speed_regulator", "翡翠波速调节器");
        add("createoreexpansion.goggles.wave_speed_regulator", "翡翠波速调节器");
        add("createoreexpansion.goggles.speed_regulator_tier", "变速等级：%s");
        add("createoreexpansion.goggles.speed_regulator_amount", "调速量：±%s 格/秒");
        // Jade 波实体信息
        add("config.jade.plugin_createoreexpansion.wave", "能量波信息");
        add("createoreexpansion.jade.wave_level", "波等级：%s");
        add("createoreexpansion.jade.wave_speed", "运行速度：%s 格/秒");
        add("createoreexpansion.jade.wave_lifetime", "剩余寿命：%s 秒");
        add("createoreexpansion.jade.wave_charge", "电荷：%s");
        add("createoreexpansion.jade.wave_charge_none", "电荷：未带电");
        add("createoreexpansion.jade.charge_positive", "正电荷");
        add("createoreexpansion.jade.charge_negative", "负电荷");
        // 变体波（星辉波变器产物）携带载荷
        add("createoreexpansion.jade.stellar_wave_payload_items", "携带物品: %s");
        add("createoreexpansion.jade.stellar_wave_payload_energy", "携带电量: %s FE");
        add("createoreexpansion.jade.stellar_wave_payload_fluid", "携带流体: %s %s mB");
        add("createoreexpansion.jade.stellar_wave_payload_rods", "释放机会: %s");
        add("createoreexpansion.jade.stellar_wave_payload_heat", "携带加热: %s");
        add("createoreexpansion.jade.stellar_wave_payload_rpm", "携带转速: %s RPM");
        // 变体波可加工配方类型（能力清单，独立于载荷）
        add("createoreexpansion.jade.stellar_wave_can_process", "可加工：");
        // 工作盆内容的实时行（本模组第二个 Jade 插件：逐 tick 取数，跟随波加工结果刷新）
        add("createoreexpansion.jade.basin_live", "工作盆内容（实时）:");
        add("block.createoreexpansion.energy_wave_disperser", "能量波差器");
        add("block.createoreexpansion.six_face_disperser", "六面能量波差器");
        add("block.createoreexpansion.octa_energy_wave_differencer", "八面能量波差器");
        add("block.createoreexpansion.reinforced_lightning_rod", "强化避雷针");
        // JEI 提示：两种获取引雷能力的途径
        add("createoreexpansion.jei.lightning_rod.ways", "引雷能力获取途径：① 被真实自然闪电击中；② 接收伽马能量波攒满进度（10/10）");
        add("createoreexpansion.tooltip.lightning_rod.charge", "伽马充能进度：");
        add("createoreexpansion.tooltip.lightning_rod.ready", "引雷充能就绪！右键释放闪电");
        add("createoreexpansion.msg.cannot_open_cover", "由于空间不足，无法开盖");
        add("createoreexpansion.msg.need_open_cover", "需要先开盖才能安装角磨轮");
        add(AllItems.IRON_GRINDING_WHEEL.get(), "铁角磨轮");
        add(AllItems.GOLD_GRINDING_WHEEL.get(), "金角磨轮");
        add(AllItems.BRASS_GRINDING_WHEEL.get(), "黄铜角磨轮");
        add(AllItems.ZINC_GRINDING_WHEEL.get(), "锌角磨轮");
        add(AllItems.JADE_GRINDING_WHEEL.get(), "翡翠角磨轮");
        add(AllItems.DIAMOND_GRINDING_WHEEL.get(), "钻石角磨轮");
        add(AllItems.TOPAZ_GRINDING_WHEEL.get(), "黄玉角磨轮");
        add(AllItems.SAPPHIRE_GRINDING_WHEEL.get(), "蓝宝石角磨轮");
        add(AllItems.STELLARSTONE_GRINDING_WHEEL.get(), "星辉石角磨轮");
        add(AllItems.NETHERITE_GRINDING_WHEEL.get(), "下界合金角磨轮");

        // ========== 角磨轮特殊效果（参数化模板：%s 由效果类按数值填充，改数值无需动翻译） ==========
        add("createoreexpansion.wheel_effect.bonus", "效果：%s%% 概率额外产出一份结果");
        add("createoreexpansion.wheel_effect.speed", "效果：加工时间减少 %s%%");
        add("createoreexpansion.wheel_effect.quantity", "效果：每种产物数量 +%s");
        add("createoreexpansion.wheel_effect.double", "效果：%s%% 概率产物翻倍");
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
        add("createoreexpansion.recipe.assembly.charging", "在翡翠应力充能器中充能");
        add("createoreexpansion.recipe.assembly.charging_hover", "在翡翠应力充能器进行%s");
        add("createoreexpansion.recipe.assembly.charging_hover_sapphire", "在蓝宝石应力充能器进行%s");
        add("createoreexpansion.recipe.assembly.cca_charging", "在特斯拉线圈中充能或雷击");
        // 充能分类标题（低/高/伽马三个等级共用一个分类，等级徽章见 jei.charging.level.*）
        add("createoreexpansion.recipe.charging", "充能加工");
        add("createoreexpansion.jei.charging.level.1", "低能量充能");
        add("createoreexpansion.jei.charging.level.2", "高能量充能");
        add("createoreexpansion.jei.charging.level.3", "伽马能量充能");
        add("createoreexpansion.jei.charging.level.4", "超载能量充能");
        add("createoreexpansion.jei.charging.level.5", "终极能量充能");
        add("entity.createoreexpansion.charger_wave", "充能能量波");
        add("entity.createoreexpansion.jade_charger_wave", "充能能量波");
        add("entity.createoreexpansion.stellar_wave", "变体能量波");

        // ========== 动力角磨床护目镜提示 ==========
        add("createoreexpansion.goggles.angle_grinder", "动力角磨床");
        add("createoreexpansion.goggles.jade_charger", "翡翠应力充能器");
        add("createoreexpansion.goggles.sapphire_charger", "蓝宝石应力充能器");
        add("createoreexpansion.goggles.charger_idle", "未接入应力");
        add("createoreexpansion.goggles.field_controller", "能量场控制器");
        add("createoreexpansion.goggles.field_controller_idle", "未接入应力（无法产生能量场）");
        add("createoreexpansion.goggles.field_controller_polarity", "接口极性：%s");
        add("createoreexpansion.goggles.field_controller_polarity_pos", "正极");
        add("createoreexpansion.goggles.field_controller_polarity_neg", "负极");
        add("createoreexpansion.goggles.field_controller_polarity_none", "无（未接入应力）");
        add("createoreexpansion.goggles.field_controller_tier", "场强档位：%s");
        add("createoreexpansion.field_controller.lid_opened", "接收盖已打开");
        add("createoreexpansion.field_controller.lid_closed", "接收盖已关闭");
        add("createoreexpansion.field_controller.type_accel", "能量场类型：加速场");
        add("createoreexpansion.field_controller.type_deflect", "能量场类型：偏转场");
        add("createoreexpansion.goggles.energy_wave_regulator", "翡翠能量调级器");
        add("createoreexpansion.goggles.sapphire_wave_regulator", "蓝宝石能量调级器");
        add("createoreexpansion.goggles.gate_need_speed", "需要转速 ≥%s RPM 才调制");
        add("createoreexpansion.goggles.gate_speed_ok", "%s RPM 调制中");
        // 档位按 Create 配置的转速上限（maxRotationSpeed，默认 256）等比划分，
        // RPM 区间由代码读取配置后作为参数传入（%s），他人修改上限时显示自动跟随
        add("createoreexpansion.goggles.charger_low", "低充能态（1~%s RPM）");
        add("createoreexpansion.goggles.charger_high", "高充能态（%s~%s RPM）");
        add("createoreexpansion.goggles.charger_gamma", "伽马充能态（≥%s RPM）");
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
