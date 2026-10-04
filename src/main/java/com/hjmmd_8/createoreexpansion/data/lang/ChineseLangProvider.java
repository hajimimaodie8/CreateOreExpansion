package com.hjmmd_8.createoreexpansion.data.lang;

import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsItems;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeBlocks;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeMachines;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import com.hjmmd_8.createoreexpansion.common.CoeCore;

/**
 * 中文语言文件 —— 全模组中文翻译的唯一定义处。
 *
 * 包含：创造标签页、物品/方块、技能名、技能类型、技能释放键、tooltip、药水效果等全部翻译。
 */
public class ChineseLangProvider extends LanguageProvider {

    public ChineseLangProvider(PackOutput output) {
        super(output, CoeCore.REGISTRY_NAMESPACE, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        // ========== 创造标签页 ==========
        add("itemGroup.createoreexpansion", "机械动力：矿物拓展");
        // CEWS（能量波阵学）**已无独立标签页**（2026-09-30 用户裁定「标签页只有一个」：
        // 该层的机器全部进矿物拓展页的「机械」分区，页与搬运逻辑 EnergyWaveStudyTab 已删除）。
        // 下面这条键保留：删它要同时改两份语言文件与四处生成产物，而它已不被任何代码引用，
        // 留着零成本；将来若真要清理请连同 en_us 与四个 src/generated 一起改。
        add("itemGroup.createoreexpansion.energy_wave_study", "机械动力：能量波阵学");
        add("createoreexpansion.mod_name", "机械动力：矿物拓展");

        // ========== 创造页分区横幅的释词 ==========
        // 三条 162×18 横幅（作者提供的素材）上不烘焙文字，释词由客户端渲染时画上去；
        // 分区规则、顺序与行号记录见 common/registry/coe/CoeCreativeSections。
        add("createoreexpansion.creative_section.ore", "矿物");
        add("createoreexpansion.creative_section.machine", "机械");
        add("createoreexpansion.creative_section.gear", "装备");

        // ========== 物品/方块 ==========
        add(CoeItems.JADE_INGOT.get(), "翡翠锭");
        add(CoeItems.RAW_JADE.get(), "粗翡翠");
        add(CoeItems.JADE_NUGGET.get(), "翡翠粒");
        add(CoeItems.CRUSHED_JADE_ORE.get(), "粉碎翡翠矿石");
        add(CoeItems.JADE_SMALL_SHARD.get(), "小块翡翠");
        add(CoeItems.JADE_BIG_SHARD.get(), "大块翡翠");
        add(CoeItems.JADE_SHEET.get(), "翡翠板");
        add(CoeItems.JADE_ROD.get(), "翡翠棍");
        add(CoeItems.JADE_WIRE.get(), "翡翠线");
        add(CoeItems.JADE_SWORD.get(), "翡翠剑");
        add(CoeItems.JADE_PICKAXE.get(), "翡翠镐");
        add(CoeItems.JADE_AXE.get(), "翡翠斧");
        add(CoeItems.JADE_SHOVEL.get(), "翡翠铲");
        add(CoeItems.JADE_HOE.get(), "翡翠锄");
        // W9：机壳归 COE（用户裁定），登记类从 CewsBlocks 改指 CoeBlocks；批 2（2026-10-04）
        // 机壳又从 CoeBlocks 分家到 CoeMachines——lang 键名不变。
        add(CoeMachines.JADE_CASING.get(), "翡翠机壳");
        add(CoeItems.TOPAZ_INGOT.get(), "黄玉锭");
        add(CoeItems.RAW_TOPAZ.get(), "粗黄玉");
        add(CoeItems.TOPAZ_NUGGET.get(), "黄玉粒");
        add(CoeItems.CRUSHED_TOPAZ_ORE.get(), "粉碎黄玉矿石");
        add(CoeItems.TOPAZ_SMALL_SHARD.get(), "小块黄玉");
        add(CoeItems.TOPAZ_BIG_SHARD.get(), "大块黄玉");
        add(CoeItems.TOPAZ_SHEET.get(), "黄玉板");
        add(CoeItems.TOPAZ_ROD.get(), "黄玉棍");
        add(CoeItems.TOPAZ_WIRE.get(), "黄玉线");
        add(CoeItems.TOPAZ_SWORD.get(), "黄玉剑");
        add(CoeItems.TOPAZ_PICKAXE.get(), "黄玉镐");
        add(CoeItems.TOPAZ_AXE.get(), "黄玉斧");
        add(CoeItems.TOPAZ_SHOVEL.get(), "黄玉铲");
        add(CoeItems.TOPAZ_HOE.get(), "黄玉锄");
        add(CoeMachines.SAPPHIRE_CASING.get(), "蓝宝石机壳");
        add(CoeMachines.STELLARSTONE_CASING.get(), "星辉石机壳");
        add(CoeItems.SAPPHIRE_INGOT.get(), "蓝宝石锭");
        add(CoeItems.RAW_SAPPHIRE.get(), "粗蓝宝石");
        add(CoeItems.SAPPHIRE_NUGGET.get(), "蓝宝石粒");
        add(CoeItems.CRUSHED_SAPPHIRE_ORE.get(), "粉碎蓝宝石矿石");
        add(CoeItems.SAPPHIRE_SMALL_SHARD.get(), "小块蓝宝石");
        add(CoeItems.SAPPHIRE_BIG_SHARD.get(), "大块蓝宝石");
        add(CoeItems.SAPPHIRE_SHEET.get(), "蓝宝石板");
        add(CoeItems.SAPPHIRE_ROD.get(), "蓝宝石棍");
        add(CoeItems.SAPPHIRE_WIRE.get(), "蓝宝石线");
        add(CoeItems.RUBY_INGOT.get(), "红宝石锭");
        add(CoeItems.RUBY_SHEET.get(), "红宝石板");
        add(CoeItems.RUBY_ROD.get(), "红宝石棍");
        add(CoeItems.RUBY_WIRE.get(), "红宝石线");
        add(CoeBlocks.RUBY_BLOCK.get(), "红宝石块");
        add(CoeBlocks.JADE_ORE.get(), "翡翠矿石");
        add(CoeBlocks.DEEPSLATE_JADE_ORE.get(), "深层翡翠矿石");
        add(CoeBlocks.RAW_JADE_BLOCK.get(), "粗翡翠块");
        add(CoeBlocks.JADE_BLOCK.get(), "翡翠块");
        // 翡翠可生长水晶
        add(CoeBlocks.JADE_BUDDING_BLOCK.get(), "翡翠水晶芽床");
        add(CoeBlocks.JADE_SMALL_BUD.get(), "翡翠水晶小芽");
        add(CoeBlocks.JADE_MEDIUM_BUD.get(), "翡翠水晶中芽");
        add(CoeBlocks.JADE_LARGE_BUD.get(), "翡翠水晶大芽");
        add(CoeBlocks.JADE_CLUSTER.get(), "翡翠水晶簇");
        add(CoeBlocks.TOPAZ_ORE.get(), "黄玉矿石");
        add(CoeBlocks.DEEPSLATE_TOPAZ_ORE.get(), "深层黄玉矿石");
        add(CoeBlocks.RAW_TOPAZ_BLOCK.get(), "粗黄玉块");
        add(CoeBlocks.TOPAZ_BLOCK.get(), "黄玉块");
        // 黄玉可生长水晶
        add(CoeBlocks.TOPAZ_BUDDING_BLOCK.get(), "黄玉水晶芽床");
        add(CoeBlocks.TOPAZ_SMALL_BUD.get(), "黄玉水晶小芽");
        add(CoeBlocks.TOPAZ_MEDIUM_BUD.get(), "黄玉水晶中芽");
        add(CoeBlocks.TOPAZ_LARGE_BUD.get(), "黄玉水晶大芽");
        add(CoeBlocks.TOPAZ_CLUSTER.get(), "黄玉水晶簇");
        add(CoeBlocks.NETHER_SAPPHIRE_ORE.get(), "下界蓝宝石矿石");
        add(CoeBlocks.RAW_SAPPHIRE_BLOCK.get(), "粗蓝宝石块");
        add(CoeBlocks.SAPPHIRE_BLOCK.get(), "蓝宝石块");
        // 蓝宝石可生长水晶
        add(CoeBlocks.SAPPHIRE_BUDDING_BLOCK.get(), "蓝宝石水晶芽床");
        add(CoeBlocks.SAPPHIRE_SMALL_BUD.get(), "蓝宝石水晶小芽");
        add(CoeBlocks.SAPPHIRE_MEDIUM_BUD.get(), "蓝宝石水晶中芽");
        add(CoeBlocks.SAPPHIRE_LARGE_BUD.get(), "蓝宝石水晶大芽");
        add(CoeBlocks.SAPPHIRE_CLUSTER.get(), "蓝宝石水晶簇");
        add(CoeItems.SAPPHIRE_SWORD.get(), "蓝宝石剑");
        add(CoeItems.SAPPHIRE_PICKAXE.get(), "蓝宝石镐");
        add(CoeItems.SAPPHIRE_AXE.get(), "蓝宝石斧");
        add(CoeItems.SAPPHIRE_SHOVEL.get(), "蓝宝石铲");
        add(CoeItems.SAPPHIRE_HOE.get(), "蓝宝石锄");
        add(CoeItems.STELLARSTONE_INGOT.get(), "星辉石锭");
        add(CoeItems.RAW_STELLARSTONE.get(), "粗星辉石");
        add(CoeItems.STELLARSTONE_NUGGET.get(), "星辉石粒");
        add(CoeItems.CRUSHED_STELLARSTONE_ORE.get(), "粉碎星辉石矿石");
        add(CoeItems.STELLARSTONE_SMALL_SHARD.get(), "小块星辉石");
        add(CoeItems.STELLARSTONE_BIG_SHARD.get(), "大块星辉石");
        add(CoeItems.STELLARSTONE_SHEET.get(), "星辉石板");
        add(CoeItems.STELLARSTONE_ROD.get(), "星辉石棍");
        add(CoeItems.STELLARSTONE_WIRE.get(), "星辉石线");
        add(CoeItems.SANCTSTONE_INGOT.get(), "星芒石锭");
        add(CoeItems.SANCTSTONE_SHEET.get(), "星芒石板");
        add(CoeItems.SANCTSTONE_ROD.get(), "星芒石棍");
        add(CoeItems.SANCTSTONE_WIRE.get(), "星芒石线");
        add(CoeBlocks.SANCTSTONE_BLOCK.get(), "星芒石块");
        add(CoeItems.STELLARSTONE_SWORD.get(), "星辉石剑");
        add(CoeItems.STELLARSTONE_PICKAXE.get(), "星辉石镐");
        add(CoeItems.STELLARSTONE_AXE.get(), "星辉石斧");
        add(CoeItems.STELLARSTONE_SHOVEL.get(), "星辉石铲");
        add(CoeItems.STELLARSTONE_HOE.get(), "星辉石锄");
        add(CoeBlocks.END_STELLARSTONE_ORE.get(), "末地星辉石矿石");
        add(CoeBlocks.RAW_STELLARSTONE_BLOCK.get(), "粗星辉石块");
        add(CoeBlocks.STELLARSTONE_BLOCK.get(), "星辉石块");
        // 星辉石可生长水晶
        add(CoeBlocks.STELLARSTONE_BUDDING_BLOCK.get(), "星辉石水晶芽床");
        add(CoeBlocks.STELLARSTONE_SMALL_BUD.get(), "星辉石水晶小芽");
        add(CoeBlocks.STELLARSTONE_MEDIUM_BUD.get(), "星辉石水晶中芽");
        add(CoeBlocks.STELLARSTONE_LARGE_BUD.get(), "星辉石水晶大芽");
        add(CoeBlocks.STELLARSTONE_CLUSTER.get(), "星辉石水晶簇");
        add(CoeItems.THUNDERITE_SCRAP.get(), "雷鸣合金碎片");
        add(CoeItems.THUNDERITE_INGOT.get(), "雷鸣合金锭");
        add(CoeItems.THUNDERITE_SHEET.get(), "雷鸣合金板");
        add(CoeItems.THUNDERITE_ROD.get(), "雷鸣合金棍");
        add(CoeItems.THUNDERITE_WIRE.get(), "雷鸣合金线");
        add(CoeItems.THUNDERITE_SWORD.get(), "雷鸣合金剑");
        add(CoeItems.THUNDERITE_PICKAXE.get(), "雷鸣合金镐");
        add(CoeItems.THUNDERITE_AXE.get(), "雷鸣合金斧");
        add(CoeItems.THUNDERITE_SHOVEL.get(), "雷鸣合金铲");
        add(CoeItems.THUNDERITE_HOE.get(), "雷鸣合金锄");
        add(CoeBlocks.THUNDERITE_BLOCK.get(), "雷鸣合金块");
        add(CoeItems.JADE_TOPAZ_BOW.get(), "翠玉之弓");
        // 2026-10-03「把弓补齐到 4 把」批 1：三把新弓（技能留后续批）。
        // 名字照预告对照文档 §二 第 12 行的写法（宝石之弓 / 星界长弓 / 雷鸣长弓）。
        add(CoeItems.SAPPHIRE_RUBY_BOW.get(), "宝石之弓");
        add(CoeItems.ASTRAL_BOW.get(), "星界长弓");
        add(CoeItems.THUNDER_BOW.get(), "雷鸣长弓");
        add(CoeItems.JADE_TOPAZ_BOOMERANG.get(), "翠玉镖");
        add(CoeItems.SAPPHIRE_RUBY_BOOMERANG.get(), "宝石镖");
        add(CoeItems.ASTRAL_BOOMERANG.get(), "星界镖");
        add(CoeItems.THUNDER_BOOMERANG.get(), "雷鸣镖");
        add(CoeItems.JADE_STRESS_MEDALLION.get(), "翡翠凝能佩");
        add(CoeItems.TOPAZ_STRESS_MEDALLION.get(), "黄玉凝能佩");
        add(CoeItems.SAPPHIRE_STRESS_MEDALLION.get(), "沧蓝凝能佩");
        add(CoeItems.NETHERITE_STRESS_MEDALLION.get(), "狱红怪佩");
        add(CoeItems.STELLARSTONE_STRESS_MEDALLION.get(), "星辉凝能佩");
        add(CoeItems.THUNDERITE_STRESS_MEDALLION.get(), "雷暴凝能佩");

        // ========== 四套盔甲（4 套 × 4 件；W13 新增，用户 2026-09-30 定名）==========
        // 套名由用户指定：翠玉 / 宝石 / 星界 / 雷鸣（不是矿物名，与凝能佩的套装效果口径一致）。
        add(CoeItems.JADE_TOPAZ_HELMET.get(), "翠玉头盔");
        add(CoeItems.JADE_TOPAZ_CHESTPLATE.get(), "翠玉胸甲");
        add(CoeItems.JADE_TOPAZ_LEGGINGS.get(), "翠玉护腿");
        add(CoeItems.JADE_TOPAZ_BOOTS.get(), "翠玉靴子");
        add(CoeItems.SAPPHIRE_RUBY_HELMET.get(), "宝石头盔");
        add(CoeItems.SAPPHIRE_RUBY_CHESTPLATE.get(), "宝石胸甲");
        add(CoeItems.SAPPHIRE_RUBY_LEGGINGS.get(), "宝石护腿");
        add(CoeItems.SAPPHIRE_RUBY_BOOTS.get(), "宝石靴子");
        add(CoeItems.ASTRAL_HELMET.get(), "星界头盔");
        add(CoeItems.ASTRAL_CHESTPLATE.get(), "星界胸甲");
        add(CoeItems.ASTRAL_LEGGINGS.get(), "星界护腿");
        add(CoeItems.ASTRAL_BOOTS.get(), "星界靴子");
        add(CoeItems.THUNDER_HELMET.get(), "雷鸣头盔");
        add(CoeItems.THUNDER_CHESTPLATE.get(), "雷鸣胸甲");
        add(CoeItems.THUNDER_LEGGINGS.get(), "雷鸣护腿");
        add(CoeItems.THUNDER_BOOTS.get(), "雷鸣靴子");

        // ========== 构件与杂项 ==========
        add(CoeItems.LUCKY_DUST.get(), "幸运之尘");
        add(CewsItems.ENERGY_MECHANISM.get(), "能量构件");
        add(CewsItems.INCOMPLETE_ENERGY_MECHANISM.get(), "未完成的能量构件");
        add(TransmutationItems.TRANSMUTE_MECHANISM.get(), "嬗化构件");
        add(TransmutationItems.INCOMPLETE_TRANSMUTE_MECHANISM.get(), "未完成的嬗化构件");

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
        // 血契置换（coe-pact 批 1）：中文名照需求 §3.3 的定名，逐字写死。
        add("skill.createoreexpansion.blood_pact", "血契置换");
        add("skill.createoreexpansion.hoe", "耕作");
        add("skill.createoreexpansion.bow_curse", "凋零诅咒");
        add("skill.createoreexpansion.bow_disarm", "缴械风暴");
        // 回旋镖技能（2026-10-02 批 3 穿刺 / 批 4 环绕）：需求 §3.5 / §3.6 的技能名。
        add("skill.createoreexpansion.pierce", "穿刺");
        add("skill.createoreexpansion.orbit", "环绕");
        // 回旋镖的两条"能量不足"提示 + 一条"本次消耗"（作者 2026-10-02 第五次裁定）。
        // ⚠ 两条"能量不足"的中文值照作者原话**逐字**（不加标点、不改字）。
        add("createoreexpansion.tool.low_energy_use", "由于能量不足无法使用回旋镖");
        add("createoreexpansion.tool.low_energy_skill", "由于能量不足无法释放技能");
        add("createoreexpansion.tool.consumed_amount", "（本次投掷消耗 %s 点）");

        // ========== 技能类型 ==========
        add("skillType.createoreexpansion.excavation_skill", "挖掘技能");
        add("skillType.createoreexpansion.hit_skill", "攻击技能");
        add("skillType.createoreexpansion.use_skill", "使用技能");

        // ========== 技能释放键 ==========
        add("createoreexpansion.keyinfo.skill_release", "技能释放1（第一技能）");
        add("createoreexpansion.keyinfo.skill_release_2", "技能释放 2（第二技能）");
        add("createoreexpansion.keyinfo.skill_release_3", "技能释放 3（第三技能）");
        add("createoreexpansion.keyinfo.skill_settings", "技能设置");
        // 机器旋转的修饰键（默认左 Ctrl；与"扳手右键"组合 = 旋转本模组无模式机器）
        add("createoreexpansion.keyinfo.rotate_modifier", "旋转机器（修饰键）");
        // 装备模式开关（默认左 Alt；2026-10-02 由"按住"改为"按一下开 / 再按一下关"）
        add("createoreexpansion.keyinfo.equipment_modifier", "装备技能开关");

        // ========== 装备技能提示层（装备模式开关打开时显示在快捷栏上方） ==========
        add("createoreexpansion.hud.equipment.hold_preview", "按住 %ss / %ss → 预计扣 %s（满额 %s）");
        // 开关语义：本层只在开关为"开"时绘制，所以状态恒为"开"（关掉时整层不显示）
        add("createoreexpansion.hud.equipment.title", "装备技能（%s 开关：开）");
        // 套名行只报套名（用户 2026-10-01 否掉"整体 LV1"的写法）；等级逐条列在技能行上
        add("createoreexpansion.hud.equipment.set_active", "已佩戴%s套");
        add("createoreexpansion.hud.equipment.set_by_enchant", "已佩戴%s套（散构聚能补齐）");
        add("createoreexpansion.hud.equipment.set_incomplete", "未成套 · 需同套四件（散构聚能可补一件）");
        add("createoreexpansion.hud.equipment.set_none", "未穿戴本模组护甲");
        add("createoreexpansion.hud.equipment.no_skill", "暂无套装技能");
        add("createoreexpansion.hud.equipment.cooldown_line", "（%s 冷却中：还需 %s 秒）");
        add("createoreexpansion.hud.equipment.skill_line", "技能%s  %s  [%s]");
        add("createoreexpansion.hud.equipment.energy", "能量 %s / %s");
        add("createoreexpansion.tooltip.armor_energy_total", "套装合计 %s / %s");
        // 套名（与护甲物品名同源；提示层用 createoreexpansion.armor_set.<setName> 拼键，键名须与 ArmorSet.setName() 一致）
        add("createoreexpansion.armor_set.jade_topaz", "翠玉");
        add("createoreexpansion.armor_set.sapphire_ruby", "宝石");
        add("createoreexpansion.armor_set.astral", "星界");
        add("createoreexpansion.armor_set.thunder", "雷鸣");

        // ========== 技能设置界面 ==========
        add("createoreexpansion.skill_settings.title", "技能设置");
        add("createoreexpansion.skill_settings.hint", "该项由服务端保存到存档，对所有玩家生效。");
        add("createoreexpansion.skill_settings.consume_creative.on", "创造模式释放技能消耗能量：开");
        add("createoreexpansion.skill_settings.consume_creative.off", "创造模式释放技能消耗能量：关");
        add("createoreexpansion.skill_settings.done", "完成");

        // ========== 工具/技能 tooltip ==========
        add("item.createoreexpansion.tool.skill_tips", "按住 [%s] 可查看技能概要");
        add("item.createoreexpansion.tool.energy", "能量");

        // ========== 附魔 ==========
        add("enchantment.createoreexpansion.reduce_consumption", "减耗");
        add("enchantment.createoreexpansion.swift_start", "迅启");
        add("enchantment.createoreexpansion.skill_boost", "技艺提升");
        add("enchantment.createoreexpansion.skill_regression", "技艺回溯");
        add("enchantment.createoreexpansion.loose_convergence", "散构聚能");
        add("skill.createoreexpansion.fall_guard", "虚衡坠护");
        add("createoreexpansion.tooltip.armor_skills", "按住 [%s] 可查看套装技能等级");
        add("createoreexpansion.equip_skill.cooldown", "技能冷却中：还需 %s 秒");
        add("createoreexpansion.equip_skill.no_energy", "护甲能量不足，无法发动");
        add("skill.createoreexpansion.charge_dash", "蓄能疾骋");
        add("skill.createoreexpansion.last_stand", "绝境守护");
        add("skill.createoreexpansion.field_charge", "临域充力");
        // 星界套三条（用户 2026-10-02）：衡元择势 / 星芒嬗震是**新 id**；
        // 临域充力 II **复用** field_charge（同一个技能 id 的高等级形态，不建新键）。
        add("skill.createoreexpansion.balance_choice", "衡元择势");
        add("skill.createoreexpansion.star_shock", "星芒嬗震");
        // 临域充力的两条"发动失败"原因（规格 §8 第 3 层：判定不过 / 放不下注入器都不扣能、不进冷却）。
        add("createoreexpansion.equip_skill.field_charge_no_source", "周围没有可赋能的动力源方块（手摇曲柄）");
        add("createoreexpansion.equip_skill.field_charge_no_space", "动力源方块旁边没有可放置应力注入器的空位");
        // 应力注入器（不可获取的内部方块，规格 §4.2）：**只在被赋能时**可能被护目镜/调试看到名字。
        // 它没有物品形态，所以这个名字永远不会出现在创造页/JEI/搜索里。
        add(CoeMachines.STRESS_INJECTOR.get(), "应力注入器（内部）");

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
        add("createoreexpansion.stellar_wave_transmuter.mode.processing", "加工波变态");
        add("createoreexpansion.stellar_wave_transmuter.mode.attack", "攻击波变态");
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
        add("createoreexpansion.goggles.stellar_wave_transmuter_items", "辅料载荷：%s/%s 个 · %s/%s 种");
        add("createoreexpansion.goggles.stellar_wave_transmuter_fluid", "流体载荷：%s mB");
        add("createoreexpansion.goggles.stellar_wave_transmuter_energy", "电量载荷：%s FE");
        add("createoreexpansion.goggles.stellar_wave_transmuter_rods", "已蓄满避雷针：%s 台（每发波抽取 1 次，波打中哪里就在哪里落雷）");
        add("createoreexpansion.goggles.stellar_wave_transmuter_last_wave", "最近波可加工：");
        // 攻击态护目镜读数（用户 2026-09 规格：攻击态按住 Shift 只显示攻击态这几行，不显示加工态读数）
        // 区间两端（128 / 256）与三档的每一条边界都由模式自报的分档表给出
        // （TransmuterMode#ATTACK_TIERS，见 SpeedBands），文案里一个转速数字都没有：
        // 改 256→320 或 3→4 档时档位行数与每行边界自动跟着变
        add("createoreexpansion.goggles.stellar_wave_transmuter_attack_rpm", "攻击转速：%s RPM（需求 %s ~ %s RPM）");
        // 档位：一档一行（用户 2026-09-25："显示太长了，你全写在一行"→ 拆行；当前档用亮色由代码上色）。
        // 区间改成闭区间（2026-09-25）：非末档"下限 ~ （下一档下限 − 1）"、末档"≥下限"，
        // 分档下限已量化为整数 RPM（见 SpeedBands#linear），所以这里不会再出现小数。
        add("createoreexpansion.goggles.stellar_wave_transmuter_attack_tier_entry", "第 %s 档（%s ~ %s RPM，%s）");
        add("createoreexpansion.goggles.stellar_wave_transmuter_attack_tier_entry_open", "第 %s 档（≥%s RPM，%s）");
        // 场盒说法（半径 0 = 场盒就是机器本体，不写成"半径 0 格"）
        add("createoreexpansion.goggles.stellar_wave_transmuter_attack_field_core", "场盒就是机器本体");
        add("createoreexpansion.goggles.stellar_wave_transmuter_attack_field_radius", "场盒半径 %s 格");
        add("createoreexpansion.goggles.stellar_wave_transmuter_attack_field_hint", "正上方展示框给出魔素时，范围内的普通波穿过即被点燃为攻击波");
        // 护目镜面板：没按住 Shift 时只有机器名 + 这一行提示（按住 Shift 一次性显示全部读数）
        add("createoreexpansion.goggles.transmuter_expand_hint", "按住 [%s] 查看机器详情");
        add("createoreexpansion.goggles.stellar_wave_transmuter_bind_hint", "已绑定 %s 台机器 · 可加工配方");
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
        add("createoreexpansion.recipe_type.compacting", "压块");
        add("createoreexpansion.recipe_type.filling", "灌注");
        add("createoreexpansion.recipe_type.emptying", "排空");
        add("createoreexpansion.recipe_type.transmuting", "转化");
        add("createoreexpansion.recipe_type.sequenced_assembly", "序列组装");
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
        add("createoreexpansion.recipe_type.charging", "充能（应力充能器）");
        add("createoreexpansion.recipe_type.charging_other", "充电（CC&A 特斯拉线圈）");
        add("createoreexpansion.recipe_type.rolling", "辊压");
        add("createoreexpansion.recipe_type.lightning", "闪电转化（由引雷触发）");
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
        add("createoreexpansion.jei.transmuter.line3", "③ 波还携带载荷：辅料物品（最多 5 个/5 种）、流体（最多 2 B）、电量（上限 = CC&A 充电配方里最贵那条的耗电量）与避雷针引雷次数（穿波时从**满充能**的强化避雷针抽取；波打中哪里就在哪里落雷，闪电加工由闪电落地统一加工接管）。");
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
        // 档位名统一"希腊字母 + 充能态"（α/β/γ/ε/ω；符号见 WaveLevels#glyph，唯一实现点）
        add("createoreexpansion.goggles.sapphire_charger_1", "α 充能态（%s~%s RPM）");
        add("createoreexpansion.goggles.sapphire_charger_2", "β 充能态（%s~%s RPM）");
        add("createoreexpansion.goggles.sapphire_charger_3", "γ 充能态（%s~%s RPM）");
        add("createoreexpansion.goggles.sapphire_charger_4", "ε 充能态（%s~%s RPM）");
        add("createoreexpansion.goggles.sapphire_charger_5", "ω 充能态（≥%s RPM）");
        add("block.createoreexpansion.energy_wave_regulator", "翡翠能量调级器");
        add("block.createoreexpansion.wave_speed_regulator", "翡翠波速调节器");
        add("createoreexpansion.goggles.wave_speed_regulator", "翡翠波速调节器");
        add("createoreexpansion.goggles.speed_regulator_tier", "变速等级：%s");
        add("createoreexpansion.goggles.speed_regulator_amount", "调速量：±%s 格/秒");
        // Jade 波实体信息（波情四项：波速 / 波级 / 波载荷 / 波型，成组显示）
        add("config.jade.plugin_createoreexpansion.wave", "能量波信息");
        // 波级只显示希腊字母（α/β/γ/ε/ω），词条里不带档位文字
        add("createoreexpansion.jade.wave_level", "波级：%s");
        add("createoreexpansion.jade.wave_speed", "波速：%s 格/秒");
        add("createoreexpansion.jade.wave_type", "波型：%s");
        add("createoreexpansion.jade.wave_payload", "波载荷：%s");
        // 波载荷行只报"物资"（物品/流体/电量）；加热·转速·引雷次数是其下的附着明细行，
        // 故物资为空的措辞要写明"未携带物品/流体/电量"，不能说成"空载"（否则与明细行自相矛盾）
        add("createoreexpansion.jade.wave_payload_none", "无（未携带物品/流体/电量）");
        add("createoreexpansion.jade.wave_lifetime", "剩余寿命：%s 秒");
        // 魔素（2026-10-03 需求 coe-ess）：攻击波专有的观感属性，只有"真的设了魔素"的波才占这一行。
        // 取值口 WaveTrailStyle#displayName()，文字颜色查 ChargerWaveFx#styleColorRgb（唯一颜色真源）
        add("createoreexpansion.jade.wave_essence", "魔素：%s");
        add("createoreexpansion.jade.wave_charge", "电荷：%s");
        add("createoreexpansion.jade.wave_charge_none", "电荷：未带电");
        add("createoreexpansion.jade.charge_positive", "正电荷");
        add("createoreexpansion.jade.charge_negative", "负电荷");
        // 波型显示名（波情④取名字口 WaveType#displayName 查这些词条；扩展模组新波型自带词条即可）
        add("createoreexpansion.wave_type.normal", "普通波");
        add("createoreexpansion.wave_type.omni", "全能波");
        add("createoreexpansion.wave_type.attack", "攻击波");
        // 魔素显示名（八种，水/火/地/风/冰/雷/毒/异；取名字口 WaveTrailStyle#displayName 查这些词条）。
        // ⚠ 只有后八个枚举值有词条：前三个（NORMAL/MECHANICAL/DAMAGE）是波型风格、不是魔素
        add("createoreexpansion.wave_essence.water", "水");
        add("createoreexpansion.wave_essence.fire", "火");
        add("createoreexpansion.wave_essence.earth", "地");
        add("createoreexpansion.wave_essence.wind", "风");
        add("createoreexpansion.wave_essence.ice", "冰");
        add("createoreexpansion.wave_essence.lightning", "雷");
        add("createoreexpansion.wave_essence.poison", "毒");
        add("createoreexpansion.wave_essence.arcane", "异");
        // 波载荷明细（概要行见 jade.wave_payload；件数/种类数与护目镜"辅料载荷"同口径）
        add("createoreexpansion.jade.stellar_wave_payload_items", "物品 %s/%s 件 · %s/%s 种");
        add("createoreexpansion.jade.stellar_wave_payload_item_list", "物品：%s");
        add("createoreexpansion.jade.stellar_wave_payload_energy", "电量 %s FE");
        add("createoreexpansion.jade.stellar_wave_payload_fluid", "流体 %s %s mB");
        add("createoreexpansion.jade.stellar_wave_payload_rods", "引雷次数：%s（打中哪里就在哪里落雷）");
        add("createoreexpansion.jade.stellar_wave_payload_heat", "加热：%s");
        add("createoreexpansion.jade.stellar_wave_payload_rpm", "转速：%s RPM");
        // 变体波可加工配方类型（能力清单，独立于载荷）
        add("createoreexpansion.jade.stellar_wave_can_process", "可加工：");
        // ========== 波情查询仪（右键查询最近能量波的波情） ==========
        add(CewsItems.WAVE_QUERY_GAUGE.get(), "波情查询仪");
        // 波情四要素成组一行：波速 / 波级 / 波载荷 / 波型。波级实参只放希腊字母；
        // join 是"按波型追加项"前的连接符（放词条里，便于中英各自控制标点）
        add("createoreexpansion.wave_gauge.readout", "波速：%s 格/秒 · 波级：%s · 波载荷：%s · 波型：%s · 剩余寿命：%s 秒");
        add("createoreexpansion.wave_gauge.join", " · ");
        // 载荷三段刻意压短（"物品×3 / 水 2000 mB / 1200 FE"）：四要素成组后整行本就偏长，
        // 动作栏在 GUI 放大档位下容易被裁，去掉"流体/电量"这类可由单位推出的字
        add("createoreexpansion.wave_gauge.payload_items", "物品×%s");
        add("createoreexpansion.wave_gauge.payload_fluid", "%s %s mB");
        add("createoreexpansion.wave_gauge.payload_energy", "%s FE");
        // 空载要占位：四要素成组显示，玩家需能分辨"没带东西"与"没读出来"
        add("createoreexpansion.wave_gauge.payload_none", "空载");
        // 按波型追加：普通波不追加；全能波给可加工配方**类型**数（与英文 "types" 同口径，
        // 不写"种类"以免被读成配方条目数）；攻击波给攻击伤害
        add("createoreexpansion.wave_gauge.tail_omni", "可加工配方类型：%s 种");
        add("createoreexpansion.wave_gauge.tail_attack", "攻击伤害：%s");
        // 魔素也是"攻击波的追加读数"之一，紧跟在攻击伤害之后（与 Jade 的行序同一条口径：
        // 五要素成组 → 附加读数）；未设魔素的攻击波这一段不出现，读数与改造前逐字相同
        add("createoreexpansion.wave_gauge.tail_essence", "魔素：%s");
        add("createoreexpansion.wave_gauge.no_wave", "附近没有能量波");
        add("createoreexpansion.wave_gauge.tooltip", "右键查询最近的波情");
        // 工作盆物品行：由 BasinLiveItemStorage 直接接管 Jade 原生行，故不再需要单独的实时行词条
        add("block.createoreexpansion.energy_wave_disperser", "能量波差器");
        add("block.createoreexpansion.six_face_disperser", "六面能量波差器");
        add("block.createoreexpansion.octa_energy_wave_differencer", "八面能量波差器");
        add("block.createoreexpansion.reinforced_lightning_rod", "强化避雷针");
        // JEI 提示：两种获取引雷能力的途径
        add("createoreexpansion.jei.lightning_rod.ways", "引雷能力获取途径：① 被真实自然闪电击中；② 接收 γ 级及以上能量波攒满进度（10/10）");
        add("createoreexpansion.tooltip.lightning_rod.charge", "γ 充能进度：");
        add("createoreexpansion.tooltip.lightning_rod.ready", "引雷充能就绪！右键释放闪电");
        add("createoreexpansion.msg.cannot_open_cover", "由于空间不足，无法开盖");
        add("createoreexpansion.msg.need_open_cover", "需要先开盖才能安装角磨轮");
        add(CoeItems.IRON_GRINDING_WHEEL.get(), "铁角磨轮");
        add(CoeItems.GOLD_GRINDING_WHEEL.get(), "金角磨轮");
        add(CoeItems.BRASS_GRINDING_WHEEL.get(), "黄铜角磨轮");
        add(CoeItems.ZINC_GRINDING_WHEEL.get(), "锌角磨轮");
        add(CoeItems.JADE_GRINDING_WHEEL.get(), "翡翠角磨轮");
        add(CoeItems.DIAMOND_GRINDING_WHEEL.get(), "钻石角磨轮");
        add(CoeItems.TOPAZ_GRINDING_WHEEL.get(), "黄玉角磨轮");
        add(CoeItems.SAPPHIRE_GRINDING_WHEEL.get(), "蓝宝石角磨轮");
        add(CoeItems.STELLARSTONE_GRINDING_WHEEL.get(), "星辉石角磨轮");
        add(CoeItems.NETHERITE_GRINDING_WHEEL.get(), "下界合金角磨轮");

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
        // 充能分类标题（α/β/γ/ε/ω 五个档位共用一个分类，档位徽章见 jei.charging.level.*）
        add("createoreexpansion.recipe.charging", "充能加工");
        add("createoreexpansion.jei.charging.level.1", "α 充能");
        add("createoreexpansion.jei.charging.level.2", "β 充能");
        add("createoreexpansion.jei.charging.level.3", "γ 充能");
        add("createoreexpansion.jei.charging.level.4", "ε 充能");
        add("createoreexpansion.jei.charging.level.5", "ω 充能");
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
        add("createoreexpansion.goggles.charger_low", "α 充能态（1~%s RPM）");
        add("createoreexpansion.goggles.charger_high", "β 充能态（%s~%s RPM）");
        add("createoreexpansion.goggles.charger_gamma", "γ 充能态（≥%s RPM）");
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

        // ========== 电荷系统（coe-charge 批 1）：着正电 / 着负电 ==========
        // 名字取需求原文的叫法（需求 §3.1 表 + 作者原话「着正电与着负电」）。
        add("effect.createoreexpansion.charged_positive", "着正电");
        add("effect.createoreexpansion.charged_negative", "着负电");

        // ========== 电荷伤害的自定义伤害类型（coe-charge 批 6，作者 2026-10-03 改判）==========
        // 与英文侧逐键对齐（id = createoreexpansion:charge ⇒ death.attack.charge）。
        add("death.attack.charge", "%1$s 被电荷杀死了");
        add("death.attack.charge.player", "%1$s 被 %2$s 的电荷杀死了");

        // ========== Create 通用 ==========
        add("create.tooltip.holdForDescription", "按住 [%1$s] 可查看概要");
        add("create.tooltip.holdForControls", "按住 [%1$s] 可查看控制方法");
        add("create.tooltip.keyShift", "Shift");
        add("create.tooltip.keyCtrl", "Ctrl");
    }
}
