package com.hjmmd_8.createoreexpansion.common.registry.coe;

// 系列特性登记：物品注册链上写 .tag(AllModItemTags.STELLARSTONE_ITEMS) / .tag(AllModItemTags.THUNDERITE_ITEMS)
// （Java 没有扩展方法，链上只能写成 .tag(...)；函数版见 SeriesTraits#addStellarstoneTraits(ItemBuilder)。
//  判定口径 = 物品标签 ∪ 系列方块标签 ∪ 注册名约定，全部收口在 SeriesTraits#isStellarstone/isThunderite）
import com.hjmmd_8.createoreexpansion.foundation.util.SkillOutlineColors;
import com.hjmmd_8.createoreexpansion.common.charger.ChargingRecipeTools;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.ArmorSet;
import com.hjmmd_8.createoreexpansion.content.equipment.armor.CoeArmorItem;
import com.hjmmd_8.createoreexpansion.content.equipment.boomerang.AbstractBoomerangEntity;
import com.hjmmd_8.createoreexpansion.content.equipment.boomerang.BoomerangItem;
import com.hjmmd_8.createoreexpansion.content.equipment.boomerang.BoomerangTier;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.BaseStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.JadeStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.NetheriteStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.SapphireStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.StellarstoneStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.ThunderiteStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.TopazStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.bridge.MedallionCurios;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergyColorConfig;
import com.hjmmd_8.createoreexpansion.content.grinding.item.GrindingWheelItem;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;
import com.hjmmd_8.createoreexpansion.common.*;
import com.hjmmd_8.createoreexpansion.common.registry.coe.charger.AllEntityTypes;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static com.simibubi.create.AllTags.AllItemTags.CREATE_INGOTS;
import static com.simibubi.create.AllTags.AllItemTags.CRUSHED_RAW_MATERIALS;

/**
 * <b>COE（矿物拓展）</b>物品注册：四种宝石的锭 / 粗矿 / 粒 / 粉碎矿 / 碎片 / 板 / 杆 / 线、
 * 剑 / 镐 / 斧 / 锹 / 锄、凝能之佩（含狱红）、雷鸣合金系列、圣石系列、幸运之尘、
 * 玉黄弓、<b>回旋镖四把</b>（{@code boomerang(...)} 族级 helper，数值真源 {@code BoomerangTier}）
 * 与角磨轮；并保留物品侧共用的构建器（{@code grindingWheel} / {@code skillItem} /
 * {@code SkillItemBuilder} / {@code EnergyItemBuilder}）。<br>
 * 归属清单权威出处：{@code markdown_output/CEWS 能量波阵学模块（拆分方案与思索）.md} §3.2。
 * 本次拆分是<b>纯搬运</b>，注册 id 与链式调用一字未改，P2a 已删除 {@code AllItems} 外观类，引用直接指向本层。
 */
public final class CoeItems {

    // ===== 机械动力方式注册 =====

    // 这个变量名可以随便写，好理解就行，一般是item id的大写
    // 这里调用了MoreCreateOre类的static field(字段？ REGSITRATE？
    // ---- 翠玉族（jade）：注册链收进族级 helper，声明处一行一件 ----
    public static final ItemEntry<Item> JADE_INGOT = gemIngot("jade_ingot", AllGemTags.JADE);
    public static final ItemEntry<Item> RAW_JADE = gemRaw("raw_jade", AllGemTags.JADE);
    public static final ItemEntry<Item> JADE_NUGGET = gemNugget("jade_nugget", AllGemTags.JADE);
    public static final ItemEntry<Item> CRUSHED_JADE_ORE = gemCrushed("crushed_jade_ore", AllGemTags.JADE);
    public static final ItemEntry<Item> JADE_SMALL_SHARD = gemPart("jade_small_shard");
    public static final ItemEntry<Item> JADE_BIG_SHARD = gemPart("jade_big_shard");
    public static final ItemEntry<Item> JADE_SHEET = gemSheet("jade_sheet", AllGemTags.JADE);
    public static final ItemEntry<Item> JADE_ROD = gemRod("jade_rod", AllGemTags.JADE);
    public static final ItemEntry<Item> JADE_WIRE = gemWire("jade_wire", AllGemTags.JADE);

    public static final ItemEntry<SwordItem> JADE_SWORD = CoeRegistrate.REGISTRATE
            .item("jade_sword", p -> new SwordItem(AllTiers.JADE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    SwordItem.createAttributes(AllTiers.JADE, 4, -2.4F)
            ))
            // 添加横扫的标签，不然没有横扫效果
            .tag(ItemTags.SWORDS)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .tag(AllTags.AllItemTags.COOLDOWN_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(600)
            .maxEnergy(600)
            .color(ToolEnergyColorConfig.JADE)
            .build()
            .addSkills(AllSkills.SKIN, 1)
            .skillColor(SkillOutlineColors.JADE_GREEN)
            .build()
            .register();

    public static final ItemEntry<PickaxeItem> JADE_PICKAXE = CoeRegistrate.REGISTRATE
            .item("jade_pickaxe", p -> new PickaxeItem(AllTiers.JADE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    PickaxeItem.createAttributes(AllTiers.JADE, 1, -2.8F)
            ))
            .tag(ItemTags.PICKAXES)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(600)
            .maxEnergy(600)
            .color(ToolEnergyColorConfig.JADE)
            .build()
            .addSkills(AllSkills.SHATTER, 1)
            .skillColor(SkillOutlineColors.JADE_GREEN)
            .build()
            .register();

    public static final ItemEntry<AxeItem> JADE_AXE = CoeRegistrate.REGISTRATE
            .item("jade_axe", p -> new AxeItem(AllTiers.JADE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    AxeItem.createAttributes(AllTiers.JADE, 6, -3.0F)
            ))
            .tag(ItemTags.AXES)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .tag(AllTags.AllItemTags.COOLDOWN_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(600)
            .maxEnergy(600)
            .color(ToolEnergyColorConfig.JADE)
            .build()
            .addSkills(AllSkills.FELL, 1)
            .skillColor(SkillOutlineColors.JADE_GREEN)
            .build()
            .register();

    public static final ItemEntry<ShovelItem> JADE_SHOVEL = CoeRegistrate.REGISTRATE
            .item("jade_shovel", p -> new ShovelItem(AllTiers.JADE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    ShovelItem.createAttributes(AllTiers.JADE, 1.5F, -3.0F)
            ))
            .tag(ItemTags.SHOVELS)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(600)
            .maxEnergy(600)
            .color(ToolEnergyColorConfig.JADE)
            .build()
            .addSkills(AllSkills.CHANNEL, 1)
            .skillColor(SkillOutlineColors.JADE_GREEN)
            .build()
            .register();

    public static final ItemEntry<HoeItem> JADE_HOE = CoeRegistrate.REGISTRATE
            .item("jade_hoe", p -> new HoeItem(AllTiers.JADE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    HoeItem.createAttributes(AllTiers.JADE, -2, -1.0F)
            ))
            .tag(ItemTags.HOES)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(600)
            .maxEnergy(600)
            .color(ToolEnergyColorConfig.JADE)
            .build()
            .addSkills(AllSkills.HOE, 1)
            .skillColor(SkillOutlineColors.JADE_GREEN)
            .build()
            .register();

    // ========== 饰品：凝能佩（Curios 项链/手饰槽位，槽位数据手写到 data/curios/；Curios 为 optional）==========
    // 注册期按"Curios 在不在"选两支物品类之一（MedallionCurios.item）：装了 Curios → 饰品支线
    // （compat.curios.CurioMedallionItems，implements ICurioItem）；没装 → 纯物品支线（本包里的基类）。
    // 两支的注册 id / 显示名 / 贴图 / 组件完全相同 —— 玩家侧无感，存档读进来还是同一个物品。
    public static final ItemEntry<JadeStressMedallionItem> JADE_STRESS_MEDALLION = stressMedallion(
        "jade_stress_medallion", MedallionCurios.KIND_JADE, JadeStressMedallionItem::new,
        1000, ToolEnergyColorConfig.JADE, null);

    // ---- 黄玉族（topaz）：注册链收进族级 helper，声明处一行一件 ----
    public static final ItemEntry<Item> TOPAZ_INGOT = gemIngot("topaz_ingot", AllGemTags.TOPAZ);
    public static final ItemEntry<Item> RAW_TOPAZ = gemRaw("raw_topaz", AllGemTags.TOPAZ);
    public static final ItemEntry<Item> TOPAZ_NUGGET = gemNugget("topaz_nugget", AllGemTags.TOPAZ);
    public static final ItemEntry<Item> CRUSHED_TOPAZ_ORE = gemCrushed("crushed_topaz_ore", AllGemTags.TOPAZ);
    public static final ItemEntry<Item> TOPAZ_SMALL_SHARD = gemPart("topaz_small_shard");
    public static final ItemEntry<Item> TOPAZ_BIG_SHARD = gemPart("topaz_big_shard");
    public static final ItemEntry<Item> TOPAZ_SHEET = gemSheet("topaz_sheet", AllGemTags.TOPAZ);
    public static final ItemEntry<Item> TOPAZ_ROD = gemRod("topaz_rod", AllGemTags.TOPAZ);
    public static final ItemEntry<Item> TOPAZ_WIRE = gemWire("topaz_wire", AllGemTags.TOPAZ);

    public static final ItemEntry<SwordItem> TOPAZ_SWORD = CoeRegistrate.REGISTRATE
            .item("topaz_sword", p -> new SwordItem(AllTiers.TOPAZ, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    SwordItem.createAttributes(AllTiers.TOPAZ, 4, -2.4F)
            ))
            .tag(ItemTags.SWORDS)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .tag(AllTags.AllItemTags.COOLDOWN_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(1500)
            .maxEnergy(1500)
            .color(ToolEnergyColorConfig.TOPAZ)
            .build()
            .addSkills(AllSkills.SKIN, 2) // 剑技能一：剥取Lv2（键一，一技能多等级：addSkills(SKIN,2)）
            .addSkills(AllSkills.PLUNDER, 1) // 剑技能二：夺取Lv1（键二，一技能多等级：addSkills(PLUNDER,1)）
            .skillColor(SkillOutlineColors.TOPAZ_GOLD)
            .build()
            .register();

    public static final ItemEntry<PickaxeItem> TOPAZ_PICKAXE = CoeRegistrate.REGISTRATE
            .item("topaz_pickaxe", p -> new PickaxeItem (AllTiers.TOPAZ, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    PickaxeItem.createAttributes(AllTiers.TOPAZ, 1.5F, -2.3F)
            ))
            .tag(ItemTags.PICKAXES)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(1500)
            .maxEnergy(1500)
            .color(ToolEnergyColorConfig.TOPAZ)
            .build()
            .addSkills(AllSkills.SHATTER, 2)
            .skillColor(SkillOutlineColors.TOPAZ_GOLD)
            .build()
            .register();

    public static final ItemEntry<ShovelItem> TOPAZ_SHOVEL= CoeRegistrate.REGISTRATE
            .item("topaz_shovel", p -> new ShovelItem (AllTiers.TOPAZ, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    ShovelItem.createAttributes(AllTiers.TOPAZ, 1.5F, -2.8F)
            ))
            .tag(ItemTags.SHOVELS)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(1500)
            .maxEnergy(1500)
            .color(ToolEnergyColorConfig.TOPAZ)
            .build()
            .addSkills(AllSkills.CHANNEL, 2)
            .skillColor(SkillOutlineColors.TOPAZ_GOLD)
            .build()
            .register();

    public static final ItemEntry<AxeItem> TOPAZ_AXE = CoeRegistrate.REGISTRATE
            .item("topaz_axe", p -> new AxeItem (AllTiers.TOPAZ, p))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .properties(p -> p.attributes(
                    AxeItem.createAttributes(AllTiers.TOPAZ, 6.5F, -3.2F)
            ))
            .tag(ItemTags.AXES)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .tag(AllTags.AllItemTags.COOLDOWN_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(1500)
            .maxEnergy(1500)
            .color(ToolEnergyColorConfig.TOPAZ)
            .build()
            .addSkills(AllSkills.FELL, 2) // 伐树Lv2（黄玉斧，连叶带干，一技能多等级：addSkills(FELL,2)）
            .skillColor(SkillOutlineColors.TOPAZ_GOLD)
            .build()
            .register();

    public static final ItemEntry<HoeItem> TOPAZ_HOE = CoeRegistrate.REGISTRATE
            .item("topaz_hoe", p -> new HoeItem(AllTiers.TOPAZ, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    HoeItem.createAttributes(AllTiers.TOPAZ, -1.5F, 0.0F)
            ))
            .tag(ItemTags.HOES)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(1500)
            .maxEnergy(1500)
            .color(ToolEnergyColorConfig.TOPAZ)
            .build()
            .addSkills(AllSkills.HOE, 2) // 耕作Lv2????，一技能多等级：addSkills(HOE,2)）
            .skillColor(SkillOutlineColors.TOPAZ_GOLD)
            .build()
            .register();

    public static final ItemEntry<TopazStressMedallionItem> TOPAZ_STRESS_MEDALLION = stressMedallion(
        "topaz_stress_medallion", MedallionCurios.KIND_TOPAZ, TopazStressMedallionItem::new,
        2500, ToolEnergyColorConfig.TOPAZ, null);

    // ---- 蓝宝石族（sapphire）：注册链收进族级 helper，声明处一行一件 ----
    public static final ItemEntry<Item> SAPPHIRE_INGOT = gemIngot("sapphire_ingot", AllGemTags.SAPPHIRE);
    public static final ItemEntry<Item> RAW_SAPPHIRE = gemRaw("raw_sapphire", AllGemTags.SAPPHIRE);
    public static final ItemEntry<Item> SAPPHIRE_NUGGET = gemNugget("sapphire_nugget", AllGemTags.SAPPHIRE);
    public static final ItemEntry<Item> CRUSHED_SAPPHIRE_ORE = gemCrushed("crushed_sapphire_ore", AllGemTags.SAPPHIRE);
    public static final ItemEntry<Item> SAPPHIRE_SMALL_SHARD = gemPart("sapphire_small_shard");
    public static final ItemEntry<Item> SAPPHIRE_BIG_SHARD = gemPart("sapphire_big_shard");
    public static final ItemEntry<Item> SAPPHIRE_SHEET = gemSheet("sapphire_sheet", AllGemTags.SAPPHIRE);
    public static final ItemEntry<Item> SAPPHIRE_ROD = gemRod("sapphire_rod", AllGemTags.SAPPHIRE);
    public static final ItemEntry<Item> SAPPHIRE_WIRE = gemWire("sapphire_wire", AllGemTags.SAPPHIRE);

    public static final ItemEntry<SwordItem> SAPPHIRE_SWORD = CoeRegistrate.REGISTRATE
            .item("sapphire_sword", p -> new SwordItem(AllTiers.SAPPHIRE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    SwordItem.createAttributes(AllTiers.SAPPHIRE, 5, -2.4F)
            ))
            .tag(ItemTags.SWORDS)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .tag(AllTags.AllItemTags.COOLDOWN_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(3000)
            .maxEnergy(3000)
            .color(ToolEnergyColorConfig.SAPPHIRE)
            .build()
            .addSkills(AllSkills.SKIN, 3) // 剑技能一：剥取Lv3（键一，一技能多等级：addSkills(SKIN,3)）
            .addSkills(AllSkills.PLUNDER, 2) // 剑技能二：夺取Lv2（键二，一技能多等级：addSkills(PLUNDER,2)）
            .skillColor(SkillOutlineColors.SAPPHIRE_BLUE)
            .build()
            .register();

    public static final ItemEntry<PickaxeItem> SAPPHIRE_PICKAXE = CoeRegistrate.REGISTRATE
            .item("sapphire_pickaxe", p -> new PickaxeItem (AllTiers.SAPPHIRE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    PickaxeItem.createAttributes(AllTiers.SAPPHIRE, 2, -2.5F)
            ))
            .tag(ItemTags.PICKAXES)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(3000)
            .maxEnergy(3000)
            .color(ToolEnergyColorConfig.SAPPHIRE)
            .build()
            .addSkills(AllSkills.SHATTER, 3) // 开岩Lv3????，一技能多等级：addSkills(SHATTER,3)）
            .skillColor(SkillOutlineColors.SAPPHIRE_BLUE)
            .build()
            .register();

    public static final ItemEntry<ShovelItem> SAPPHIRE_SHOVEL= CoeRegistrate.REGISTRATE
            .item("sapphire_shovel", p -> new ShovelItem (AllTiers.SAPPHIRE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    ShovelItem.createAttributes(AllTiers.SAPPHIRE, 1.5F, -2.8F)
            ))
            .tag(ItemTags.SHOVELS)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(3000)
            .maxEnergy(3000)
            .color(ToolEnergyColorConfig.SAPPHIRE)
            .build()
            .addSkills(AllSkills.CHANNEL, 3) // 引渠Lv3（一技能多等级：addSkills(CHANNEL,3) 自动（CHANNEL_3 = 7 格）
            .addSkills(AllSkills.GRADE, 1) // 平场Lv1???? 平面，数值（SkillAoeConfigs.GRADE_1）
            .skillColor(SkillOutlineColors.SAPPHIRE_BLUE)
            .build()
            .register();

    public static final ItemEntry<AxeItem> SAPPHIRE_AXE = CoeRegistrate.REGISTRATE
            .item("sapphire_axe", p -> new AxeItem (AllTiers.SAPPHIRE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    AxeItem.createAttributes(AllTiers.SAPPHIRE, 6.5F, -3.4F)
            ))
            .tag(ItemTags.AXES)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .tag(AllTags.AllItemTags.COOLDOWN_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(3000)
            .maxEnergy(3000)
            .color(ToolEnergyColorConfig.SAPPHIRE)
            .build()
            .addSkills(AllSkills.FELL, 3) // 伐树Lv3（蓝宝石斧，范围扩大，一技能多等级：addSkills(FELL,3)）
            .skillColor(SkillOutlineColors.SAPPHIRE_BLUE)
            .build()
            .register();

    public static final ItemEntry<HoeItem> SAPPHIRE_HOE = CoeRegistrate.REGISTRATE
            .item("sapphire_hoe", p -> new HoeItem(AllTiers.SAPPHIRE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    HoeItem.createAttributes(AllTiers.SAPPHIRE, -1, 0.0F)
            ))
            .tag(ItemTags.HOES)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(3000)
            .maxEnergy(3000)
            .color(ToolEnergyColorConfig.SAPPHIRE)
            .build()
            .addSkills(AllSkills.HOE, 3) // 耕作Lv3????，一技能多等级：addSkills(HOE,3)）
            .skillColor(SkillOutlineColors.SAPPHIRE_BLUE)
            .build()
            .register();

    public static final ItemEntry<SapphireStressMedallionItem> SAPPHIRE_STRESS_MEDALLION = stressMedallion(
        "sapphire_stress_medallion", MedallionCurios.KIND_SAPPHIRE, SapphireStressMedallionItem::new,
        5000, ToolEnergyColorConfig.SAPPHIRE, null);

    public static final ItemEntry<NetheriteStressMedallionItem> NETHERITE_STRESS_MEDALLION = stressMedallion(
        "netherite_stress_medallion", MedallionCurios.KIND_NETHERITE, NetheriteStressMedallionItem::new,
        5000, ToolEnergyColorConfig.NETHERITE, null);

    // ---- 红宝石族（ruby，无粗矿/粒/粉碎/碎片形态）：一行一件 ----
    public static final ItemEntry<Item> RUBY_INGOT = gemIngot("ruby_ingot", AllGemTags.RUBY);
    public static final ItemEntry<Item> RUBY_SHEET = gemSheet("ruby_sheet", AllGemTags.RUBY);
    public static final ItemEntry<Item> RUBY_ROD = gemRod("ruby_rod", AllGemTags.RUBY);
    public static final ItemEntry<Item> RUBY_WIRE = gemWire("ruby_wire", AllGemTags.RUBY);

    // ---- 星辉石族（stellarstone）：系列族（每件都挂系列标签 + 少见稀有度）⇒ GemDecor.series(...) ----
    public static final ItemEntry<Item> STELLARSTONE_INGOT = gemIngot(
        "stellarstone_ingot", AllGemTags.STELLARSTONE, GemDecor.series(AllModItemTags.STELLARSTONE_ITEMS));
    public static final ItemEntry<Item> RAW_STELLARSTONE = gemRaw(
        "raw_stellarstone", AllGemTags.STELLARSTONE, GemDecor.series(AllModItemTags.STELLARSTONE_ITEMS));
    public static final ItemEntry<Item> STELLARSTONE_NUGGET = gemNugget(
        "stellarstone_nugget", AllGemTags.STELLARSTONE, GemDecor.series(AllModItemTags.STELLARSTONE_ITEMS));
    public static final ItemEntry<Item> CRUSHED_STELLARSTONE_ORE = gemCrushed(
        "crushed_stellarstone_ore", AllGemTags.STELLARSTONE, GemDecor.series(AllModItemTags.STELLARSTONE_ITEMS));
    public static final ItemEntry<Item> STELLARSTONE_SMALL_SHARD = gemPart(
        "stellarstone_small_shard", GemDecor.series(AllModItemTags.STELLARSTONE_ITEMS));
    public static final ItemEntry<Item> STELLARSTONE_BIG_SHARD = gemPart(
        "stellarstone_big_shard", GemDecor.series(AllModItemTags.STELLARSTONE_ITEMS));
    public static final ItemEntry<Item> STELLARSTONE_SHEET = gemSheet(
        "stellarstone_sheet", AllGemTags.STELLARSTONE, GemDecor.series(AllModItemTags.STELLARSTONE_ITEMS));
    public static final ItemEntry<Item> STELLARSTONE_ROD = gemRod(
        "stellarstone_rod", AllGemTags.STELLARSTONE, GemDecor.series(AllModItemTags.STELLARSTONE_ITEMS));
    public static final ItemEntry<Item> STELLARSTONE_WIRE = gemWire(
        "stellarstone_wire", AllGemTags.STELLARSTONE, GemDecor.series(AllModItemTags.STELLARSTONE_ITEMS));

    public static final ItemEntry<SwordItem> STELLARSTONE_SWORD = CoeRegistrate.REGISTRATE
            .item("stellarstone_sword", p -> new SwordItem(AllTiers.STELLARSTONE, p))
            .tag(AllModItemTags.STELLARSTONE_ITEMS)
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    SwordItem.createAttributes(AllTiers.STELLARSTONE, 4, -2.4F)
            ))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get)).tag(ItemTags.SWORDS)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .tag(AllTags.AllItemTags.COOLDOWN_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(4500)
            .maxEnergy(4500)
            .color(ToolEnergyColorConfig.STELLARSTONE)
            .build()
            .addSkills(AllSkills.SKIN, 4) // 剑技能一：剥取Lv4（键一，一技能多等级：addSkills(SKIN,4)）
            .addSkills(AllSkills.PLUNDER, 3) // 剑技能二：夺取Lv3（键二，一技能多等级：addSkills(PLUNDER,3)）
            .skillColor(SkillOutlineColors.STELLARSTONE_PINK)
            .build()
            .register();

    public static final ItemEntry<PickaxeItem> STELLARSTONE_PICKAXE = CoeRegistrate.REGISTRATE
            .item("stellarstone_pickaxe", p -> new PickaxeItem(AllTiers.STELLARSTONE, p))
            .tag(AllModItemTags.STELLARSTONE_ITEMS)
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    PickaxeItem.createAttributes(AllTiers.STELLARSTONE, 1.5F, -2.3F)
            ))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get)).tag(ItemTags.PICKAXES)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(4500)
            .maxEnergy(4500)
            .color(ToolEnergyColorConfig.STELLARSTONE)
            .build()
            .addSkills(AllSkills.SHATTER, 4) // 开岩Lv4，一技能多等级：addSkills(SHATTER,4)）
            .skillColor(SkillOutlineColors.STELLARSTONE_PINK)
            .build()
            .register();

    public static final ItemEntry<ShovelItem> STELLARSTONE_SHOVEL= CoeRegistrate.REGISTRATE
            .item("stellarstone_shovel", p -> new ShovelItem(AllTiers.STELLARSTONE, p))
            .tag(AllModItemTags.STELLARSTONE_ITEMS)
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    ShovelItem.createAttributes(AllTiers.STELLARSTONE, 1.5F, -2.8F)
            ))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get)).tag(ItemTags.SHOVELS)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(4500)
            .maxEnergy(4500)
            .color(ToolEnergyColorConfig.STELLARSTONE)
            .build()
            .addSkills(AllSkills.CHANNEL, 4) // 引渠Lv4（一技能多等级：addSkills(CHANNEL,4) 自动（CHANNEL_4 = 8 格）
            .addSkills(AllSkills.GRADE, 2) // 平场Lv2???? 平面，数值（SkillAoeConfigs.GRADE_2）
            .skillColor(SkillOutlineColors.STELLARSTONE_PINK)
            .build()
            .register();

    public static final ItemEntry<AxeItem> STELLARSTONE_AXE = CoeRegistrate.REGISTRATE
            .item("stellarstone_axe", p -> new AxeItem(AllTiers.STELLARSTONE, p))
            .tag(AllModItemTags.STELLARSTONE_ITEMS)
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    AxeItem.createAttributes(AllTiers.STELLARSTONE, 6.5F, -3.2F)
            ))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get)).tag(ItemTags.AXES)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .tag(AllTags.AllItemTags.COOLDOWN_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(4500)
            .maxEnergy(4500)
            .color(ToolEnergyColorConfig.STELLARSTONE)
            .build()
            .addSkills(AllSkills.FELL, 4) // 伐树Lv4（蓝宝石斧，范围扩大，一技能多等级：addSkills(FELL,4   )??
            .skillColor(SkillOutlineColors.STELLARSTONE_PINK)
            .build()
            .register();

    public static final ItemEntry<HoeItem> STELLARSTONE_HOE = CoeRegistrate.REGISTRATE
            .item("stellarstone_hoe", p -> new HoeItem(AllTiers.STELLARSTONE, p))
            .tag(AllModItemTags.STELLARSTONE_ITEMS)
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    HoeItem.createAttributes(AllTiers.STELLARSTONE, -1.5F, 0.0F)
            ))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get)).tag(ItemTags.HOES)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(4500)
            .maxEnergy(4500)
            .color(ToolEnergyColorConfig.STELLARSTONE)
            .build()
            .addSkills(AllSkills.HOE, 4) // 耕作Lv4????，一技能多等级：addSkills(HOE,4)）
            .skillColor(SkillOutlineColors.STELLARSTONE_PINK)
            .build()
            .register();

    public static final ItemEntry<StellarstoneStressMedallionItem> STELLARSTONE_STRESS_MEDALLION = stressMedallion(
        "stellarstone_stress_medallion", MedallionCurios.KIND_STELLARSTONE, StellarstoneStressMedallionItem::new,
        10000, ToolEnergyColorConfig.STELLARSTONE, AllModItemTags.STELLARSTONE_ITEMS);

    // ---- 灵石族（sanctstone，4 个形态）：**星芒石与星辉石同待遇**（用户 2026-10-01 裁定）----
    // 与星辉石共用 stellarstone_items 标签 ⇒ 善化抗性 / 岩浆不销毁 / 虚空浮回三档全部继承。
    public static final ItemEntry<Item> SANCTSTONE_INGOT = gemIngot("sanctstone_ingot", AllGemTags.SANCTSTONE, GemDecor.series(AllModItemTags.STELLARSTONE_ITEMS));
    public static final ItemEntry<Item> SANCTSTONE_SHEET = gemSheet("sanctstone_sheet", AllGemTags.SANCTSTONE, GemDecor.series(AllModItemTags.STELLARSTONE_ITEMS));
    public static final ItemEntry<Item> SANCTSTONE_ROD = gemRod("sanctstone_rod", AllGemTags.SANCTSTONE, GemDecor.series(AllModItemTags.STELLARSTONE_ITEMS));
    public static final ItemEntry<Item> SANCTSTONE_WIRE = gemWire("sanctstone_wire", AllGemTags.SANCTSTONE, GemDecor.series(AllModItemTags.STELLARSTONE_ITEMS));

    // ---- 雷鸣合金族（thunderite）：系列族（系列标签 + 少见稀有度）；"碎屑"是无形态标签的中间产物 ----
    public static final ItemEntry<Item> THUNDERITE_INGOT = gemIngot(
        "thunderite_ingot", AllGemTags.THUNDERITE, GemDecor.series(AllModItemTags.THUNDERITE_ITEMS));
    public static final ItemEntry<Item> THUNDERITE_SCRAP = gemPart(
        "thunderite_scrap", GemDecor.series(AllModItemTags.THUNDERITE_ITEMS));
    public static final ItemEntry<Item> THUNDERITE_SHEET = gemSheet(
        "thunderite_sheet", AllGemTags.THUNDERITE, GemDecor.series(AllModItemTags.THUNDERITE_ITEMS));
    public static final ItemEntry<Item> THUNDERITE_ROD = gemRod(
        "thunderite_rod", AllGemTags.THUNDERITE, GemDecor.series(AllModItemTags.THUNDERITE_ITEMS));
    public static final ItemEntry<Item> THUNDERITE_WIRE = gemWire(
        "thunderite_wire", AllGemTags.THUNDERITE, GemDecor.series(AllModItemTags.THUNDERITE_ITEMS));


    public static final ItemEntry<SwordItem> THUNDERITE_SWORD = CoeRegistrate.REGISTRATE
            .item("thunderite_sword", p -> new SwordItem(AllTiers.THUNDERITE, p))
            .tag(AllModItemTags.THUNDERITE_ITEMS)
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    SwordItem.createAttributes(AllTiers.THUNDERITE, 4, -2.4F)
            ))
            .tag(ItemTags.SWORDS)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .tag(AllTags.AllItemTags.COOLDOWN_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(5000)
            .maxEnergy(5000)
            .color(ToolEnergyColorConfig.THUNDERITE)
            .build()
            .addSkills(AllSkills.SKIN, 5) // 剑技能一：剥取Lv5（键一，一技能多等级：addSkills(SKIN,5)）
            .addSkills(AllSkills.PLUNDER, 3) // 剑技能二：夺取Lv3（键二，一技能多等级：addSkills(PLUNDER,3)）
            .skillColor(SkillOutlineColors.THUNDER_PURPLE)
            .build()
            .register();

    public static final ItemEntry<PickaxeItem> THUNDERITE_PICKAXE = CoeRegistrate.REGISTRATE
            .item("thunderite_pickaxe", p -> new PickaxeItem(AllTiers.THUNDERITE, p))
            .tag(AllModItemTags.THUNDERITE_ITEMS)
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    PickaxeItem.createAttributes(AllTiers.THUNDERITE, 1.5F, -2.3F)
            ))
            .tag(ItemTags.PICKAXES)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(5000)
            .maxEnergy(5000)
            .color(ToolEnergyColorConfig.THUNDERITE)
            .build()
            .addSkills(AllSkills.SHATTER, 5) // 开岩Lv5（一技能多等级：addSkills(SHATTER,5)）
            .skillColor(SkillOutlineColors.THUNDER_PURPLE)
            .build()
            .register();

    public static final ItemEntry<ShovelItem> THUNDERITE_SHOVEL = CoeRegistrate.REGISTRATE
            .item("thunderite_shovel", p -> new ShovelItem(AllTiers.THUNDERITE, p))
            .tag(AllModItemTags.THUNDERITE_ITEMS)
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    ShovelItem.createAttributes(AllTiers.THUNDERITE, 1.5F, -2.8F)
            ))
            .tag(ItemTags.SHOVELS)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(5000)
            .maxEnergy(5000)
            .color(ToolEnergyColorConfig.THUNDERITE)
            .build()
            .addSkills(AllSkills.CHANNEL, 5) // 引渠Lv5（一技能多等级：addSkills(CHANNEL,5)）
            .addSkills(AllSkills.GRADE, 3) // 平场Lv3（一技能多等级：addSkills(GRADE,3)）
            .skillColor(SkillOutlineColors.THUNDER_PURPLE)
            .build()
            .register();

    public static final ItemEntry<AxeItem> THUNDERITE_AXE = CoeRegistrate.REGISTRATE
            .item("thunderite_axe", p -> new AxeItem(AllTiers.THUNDERITE, p))
            .tag(AllModItemTags.THUNDERITE_ITEMS)
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    AxeItem.createAttributes(AllTiers.THUNDERITE, 6.5F, -3.2F)
            ))
            .tag(ItemTags.AXES)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .tag(AllTags.AllItemTags.COOLDOWN_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(5000)
            .maxEnergy(5000)
            .color(ToolEnergyColorConfig.THUNDERITE)
            .build()
            .addSkills(AllSkills.FELL, 5) // 伐树Lv5（一技能多等级：addSkills(FELL,5)）
            .skillColor(SkillOutlineColors.THUNDER_PURPLE)
            .build()
            .register();

    public static final ItemEntry<HoeItem> THUNDERITE_HOE = CoeRegistrate.REGISTRATE
            .item("thunderite_hoe", p -> new HoeItem(AllTiers.THUNDERITE, p))
            .tag(AllModItemTags.THUNDERITE_ITEMS)
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    HoeItem.createAttributes(AllTiers.THUNDERITE, -1.5F, 0.0F)
            ))
            .tag(ItemTags.HOES)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(5000)
            .maxEnergy(5000)
            .color(ToolEnergyColorConfig.THUNDERITE)
            .build()
            .addSkills(AllSkills.HOE, 5) // 耕作Lv5（一技能多等级：addSkills(HOE,5)）
            .skillColor(SkillOutlineColors.THUNDER_PURPLE)
            .build()
            .register();

    public static final ItemEntry<ThunderiteStressMedallionItem> THUNDERITE_STRESS_MEDALLION = stressMedallion(
        "thunderite_stress_medallion", MedallionCurios.KIND_THUNDERITE, ThunderiteStressMedallionItem::new,
        10000, ToolEnergyColorConfig.THUNDERITE, AllModItemTags.THUNDERITE_ITEMS);

    public static final ItemEntry<Item> LUCKY_DUST = CoeRegistrate.REGISTRATE
            .item("lucky_dust", Item::new)
            .tag(AllTags.AllItemTags.DUSTS.tag)
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<JadeTopazBowItem> JADE_TOPAZ_BOW = CoeRegistrate.REGISTRATE
            .item("jade_topaz_bow", JadeTopazBowItem::new)
            .tag(AllTags.AllItemTags.SKILL_TOOLS.tag)
            .tag(AllTags.AllItemTags.COOLDOWN_TOOLS.tag)
            .model((ctx, provider) -> {})
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(2000)
            .maxEnergy(2000)
            .color(ToolEnergyColorConfig.TOPAZ)
            .build()
            .addSkills(AllSkills.BOW_CURSE, 1)
            .addSkills(AllSkills.BOW_DISARM, 1)
            .skillColor(SkillOutlineColors.TOPAZ_GOLD)
            .skillCooldown(5 * 20)
            .build()
            .register();

    // ========== 回旋镖四把（2026-10-02 第一批：投掷 / 去程 / 回程 / 捡物 / 挖方块） ==========
    // 声明处一行一件（族级 helper boomerang(...) 见本段末尾）；
    // 数值真源 = BoomerangTier（伤害/能量/挖掘等级/最大硬度/冷却/能量费全在那里），
    // 实体类型 = AllEntityTypes（渲染器在 client/WaveEntityRendererRegistration，同一次改动）。

    public static final ItemEntry<BoomerangItem> JADE_TOPAZ_BOOMERANG = boomerang(
            "jade_topaz_boomerang", BoomerangTier.JADE_TOPAZ, () -> AllEntityTypes.JADE_TOPAZ_BOOMERANG.get());

    public static final ItemEntry<BoomerangItem> SAPPHIRE_RUBY_BOOMERANG = boomerang(
            "sapphire_ruby_boomerang", BoomerangTier.SAPPHIRE_RUBY, () -> AllEntityTypes.SAPPHIRE_RUBY_BOOMERANG.get());

    public static final ItemEntry<BoomerangItem> ASTRAL_BOOMERANG = boomerang(
            "astral_boomerang", BoomerangTier.ASTRAL, () -> AllEntityTypes.ASTRAL_BOOMERANG.get());

    public static final ItemEntry<BoomerangItem> THUNDER_BOOMERANG = boomerang(
            "thunder_boomerang", BoomerangTier.THUNDER, () -> AllEntityTypes.THUNDER_BOOMERANG.get());

    /**
     * <b>回旋镖族级注册链</b>（形态照 {@code armor(...)} / {@code grindingWheel(...)}：
     * 声明处一行一件，链只写一次）。
     *
     * <p>链上每件事各自有唯一出口：</p>
     * <ul>
     *   <li>{@code .properties(p -> p.stacksTo(1))} —— 镖不叠堆：能量是<b>逐堆</b>的组件，
     *       两把不同能量的镖叠在一起只会剩一个能量值（说不清是哪把的）。</li>
     *   <li>{@code .model(provider.generated(...))} —— 物品模型 JSON <b>一律 datagen 生成</b>
     *       （{@code parent: item/generated} + {@code layer0 = item/<id>}），不手写；
     *       贴图就是作者给的那四张（逐字节复制，未改一像素）。</li>
     *   <li>{@code .transform(skillItem())} —— 进模组统一的物品构建链（创造页分区由它的
     *       {@code build()} 声明为 {@code GEAR}）。<b>批 3 起绑技能</b>：
     *       {@code .addSkills(AllSkills.PIERCE, tier.baseSkillLevel())} 就在两次 {@code build()}
     *       之间那一行（基准等级 1/2/3/3 来自档位表，这一行不出现等级字面量）。</li>
     *   <li>{@code .addEnergy()} —— 能量走现成路径（{@code defaultEnergy == maxEnergy} = 初始即满），
     *       注册时自动挂进工具充能配方（{@code ChargingRecipeTools}）。</li>
     *   <li><b>耐久（2026-10-02 批 1）</b> —— <b>不在这条链上</b>：它由 {@link BoomerangItem} 的构造器
     *       写进 {@code Item.Properties#durability(tier.durability())}（形态照
     *       {@code JadeTopazBowItem:76}）⇒ 上限随档走（1000 / 2000 / 3500 / 3500）、出生即满耐久。
     *       读写在 {@code BoomerangItem#getDurability/setDurability/addWear}（<b>不用
     *       {@code hurtAndBreak}</b>）；扣减时机与回程批量结算是批 2 的事。</li>
     * </ul>
     *
     * @param id         注册 id（同时是图标贴图名 {@code item/<id>}）
     * @param tier       数值档（见 {@link BoomerangTier}）
     * @param entityType 投掷物实体类型（延迟取值：注册期不能触发 DeferredHolder 解析）
     */
    private static ItemEntry<BoomerangItem> boomerang(String id, BoomerangTier tier,
            Supplier<EntityType<? extends AbstractBoomerangEntity>> entityType) {
        return CoeRegistrate.REGISTRATE
            .item(id, p -> new BoomerangItem(tier, entityType, p))
            .properties(p -> p.stacksTo(1))
            .model((ctx, provider) -> provider.generated(ctx::get, CoeCore.modLoc("item/" + id)))
            .transform(skillItem())
            .addEnergy()
            .defaultEnergy(tier.energy())
            .maxEnergy(tier.energy())
            .color(tier.color())
            // 两次 build()：第一次收尾能量段（EnergyItemBuilder → SkillItemBuilder），
            // 第二次收尾技能段（SkillItemBuilder → ItemBuilder，同时把创造页分区与充能配方挂上），
            // 于是链末尾的 .register() 是 ItemBuilder 的 ⇒ 拿到的才是 ItemEntry（与剑/弓同一个形状）。
            .build()
            // 技能段（2026-10-02 批 3）：穿刺。**基准等级来自档位**（BoomerangTier#baseSkillLevel()
            // = 翠玉 1 / 宝石 2 / 星界 3 / 雷鸣 3，需求 §3.1 给死 ⇒ 不在这一行写字面量）；
            // 有效等级 = 基准 ± 技艺提升/技艺回溯 再钳到 5，读取点唯一
            // （BoomerangItem#effectiveSkillLevel）。批 4 的环绕加在旁边同一段里。
            .addSkills(AllSkills.PIERCE, tier.baseSkillLevel())
            .build()
            .register();
    }

    // ========== 角磨轮（动力角磨床配件，开盖后安装） ==========

    /**
     * 组件化注册角磨轮：注册时选用「材质来源方块」与「等级 tag」。
     * 材质方块以 Supplier 延迟取值（避免静态初始化顺序问题）；
     * 模型由 datagen 动态生成（parent wheel_base + 方块材质），无需手写轮子模型 JSON。
     */
    private static ItemEntry<GrindingWheelItem> grindingWheel(String name, AllTags.AllItemTags tierTag, Supplier<Block> materialBlock) {
        return CoeRegistrate.REGISTRATE
            .item(name, GrindingWheelItem::new)
            // 创造页分区：角磨轮按用户 2026-09-30 裁定归「机械」、且排在**本区最后**。
            // 分区由这一行声明（固定套路）；"排最后"由 CoeCreativeSections 的 MACHINE_WHEEL 族序负责。
            .transform(CoeCreativeSections.section(CoeCreativeSections.CreativeSection.MACHINE))
            .tag(AllTags.AllItemTags.GRINDING_WHEELS.tag)
            .tag(tierTag.tag)
            .model((ctx, prov) -> {
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(materialBlock.get());
                // 用 ResourceLocation 重载的 texture()（不校验纹理是否存在于 datagen 资源包，
                // 允许引用 Create 等依赖模组的纹理）
                ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(),
                    "block/" + blockId.getPath());
                prov.getBuilder(name)
                    .parent(new ModelFile.UncheckedModelFile(
                        "createoreexpansion:block/power_angle_grinder/power_angle_wheel/wheel_base"))
                    .texture("0", texture)
                    .texture("particle", texture);
            })
            .register();
    }

    public static final ItemEntry<GrindingWheelItem> IRON_GRINDING_WHEEL = grindingWheel(
            "iron_grinding_wheel",
            AllTags.AllItemTags.GRINDING_WHEELS_TIER_1,
            () -> Blocks.IRON_BLOCK);

    public static final ItemEntry<GrindingWheelItem> GOLD_GRINDING_WHEEL = grindingWheel(
            "gold_grinding_wheel",
            AllTags.AllItemTags.GRINDING_WHEELS_TIER_1,
            () -> Blocks.GOLD_BLOCK);

    public static final ItemEntry<GrindingWheelItem> BRASS_GRINDING_WHEEL = grindingWheel(
            "brass_grinding_wheel",
            AllTags.AllItemTags.GRINDING_WHEELS_TIER_1,
            com.simibubi.create.AllBlocks.BRASS_BLOCK);

    public static final ItemEntry<GrindingWheelItem> ZINC_GRINDING_WHEEL = grindingWheel(
            "zinc_grinding_wheel",
            AllTags.AllItemTags.GRINDING_WHEELS_TIER_1,
            com.simibubi.create.AllBlocks.ZINC_BLOCK);

    public static final ItemEntry<GrindingWheelItem> JADE_GRINDING_WHEEL = grindingWheel(
            "jade_grinding_wheel",
            AllTags.AllItemTags.GRINDING_WHEELS_TIER_2,
            CoeBlocks.JADE_BLOCK);

    public static final ItemEntry<GrindingWheelItem> DIAMOND_GRINDING_WHEEL = grindingWheel(
            "diamond_grinding_wheel",
            AllTags.AllItemTags.GRINDING_WHEELS_TIER_2,
            () -> Blocks.DIAMOND_BLOCK);

    public static final ItemEntry<GrindingWheelItem> TOPAZ_GRINDING_WHEEL = grindingWheel(
            "topaz_grinding_wheel",
            AllTags.AllItemTags.GRINDING_WHEELS_TIER_2,
            CoeBlocks.TOPAZ_BLOCK);

    public static final ItemEntry<GrindingWheelItem> SAPPHIRE_GRINDING_WHEEL = grindingWheel(
            "sapphire_grinding_wheel",
            AllTags.AllItemTags.GRINDING_WHEELS_TIER_3,
            CoeBlocks.SAPPHIRE_BLOCK);

    public static final ItemEntry<GrindingWheelItem> STELLARSTONE_GRINDING_WHEEL = grindingWheel(
            "stellarstone_grinding_wheel",
            AllTags.AllItemTags.GRINDING_WHEELS_TIER_3,
            CoeBlocks.STELLARSTONE_BLOCK);

    public static final ItemEntry<GrindingWheelItem> NETHERITE_GRINDING_WHEEL = grindingWheel(
            "netherite_grinding_wheel",
            AllTags.AllItemTags.GRINDING_WHEELS_TIER_3,
            () -> Blocks.NETHERITE_BLOCK);

    // ========== 宝石族物品的注册辅助 ==========
    // 用户 2026-10-01：「别一个代码复制粘贴用一堆，能继承就继承」——同一族物品的注册链只写一遍，
    // 声明处变成一行。原来这段链在文件里逐字重复了 57 次（审计计数）。
    // 每个 helper 顺带把「矿物」分区声明掉（分区归属写在注册处，见 CoeCreativeSections#section）。

    /**
     * <b>族级装饰</b>：一个族里"每件都有的额外待遇"（系列物品标签、少见稀有度）。
     *
     * <p>为什么要这个值对象：直接给 8 个 helper 各加两个参数会变成 16 个签名；
     * 收成一个值对象后，每个 helper 只需"短签名（无装饰）+ 长签名（带装饰）"两个，
     * 调用处读起来也自解释：{@code GemDecor.series(...)} / {@code GemDecor.UNCOMMON}。</p>
     *
     * @param seriesTag 系列物品标签（可空：不是"系列"的族传 null）
     * @param uncommon  是否把稀有度设为 {@code UNCOMMON}
     */
    private record GemDecor(@Nullable TagKey<Item> seriesTag, boolean uncommon) {
        /** 无装饰（绝大多数族）。 */
        static final GemDecor NONE = new GemDecor(null, false);
        /** 只提稀有度（灵石族：每件 UNCOMMON，但没有系列标签）。 */
        static final GemDecor UNCOMMON = new GemDecor(null, true);
        /** 系列族（星辉石 / 雷鸣）：挂系列物品标签 + 少见。 */
        static GemDecor series(TagKey<Item> tag) {
            return new GemDecor(tag, true);
        }
    }

    /** 普通宝石物品的公共收尾：装饰 → 基础模型 → 「矿物」分区声明。 */
    private static <T extends Item, P> ItemBuilder<T, P> oreItem(ItemBuilder<T, P> builder, GemDecor decor) {
        if (decor.seriesTag() != null) {
            builder = builder.tag(decor.seriesTag());
        }
        if (decor.uncommon()) {
            builder = builder.properties(p -> p.rarity(Rarity.UNCOMMON));
        }
        return builder
            .model((ctx, provider) -> provider.basicItem(ctx.get()))
            .transform(CoeCreativeSections.section(CoeCreativeSections.CreativeSection.ORE));
    }

    // 下面每个形态都是"短签名（无装饰）+ 长签名（带装饰）"：已迁移的普通族调用点因此一行都不用改。

    /** 锭（Create 锭 + 原版锭 + 本族锭标签）。 */
    private static ItemEntry<Item> gemIngot(String id, AllGemTags gem) {
        return gemIngot(id, gem, GemDecor.NONE);
    }

    private static ItemEntry<Item> gemIngot(String id, AllGemTags gem, GemDecor decor) {
        return oreItem(CoeRegistrate.REGISTRATE.item(id, Item::new)
            .tag(CREATE_INGOTS.tag).tag(Tags.Items.INGOTS).tag(gem.ingots), decor).register();
    }

    /** 粗矿（原版粗矿 + 本族粗矿标签）。 */
    private static ItemEntry<Item> gemRaw(String id, AllGemTags gem) {
        return gemRaw(id, gem, GemDecor.NONE);
    }

    private static ItemEntry<Item> gemRaw(String id, AllGemTags gem, GemDecor decor) {
        return oreItem(CoeRegistrate.REGISTRATE.item(id, Item::new)
            .tag(Tags.Items.RAW_MATERIALS).tag(gem.rawOres), decor).register();
    }

    /** 粒（原版粒 + 本族粒标签）。 */
    private static ItemEntry<Item> gemNugget(String id, AllGemTags gem) {
        return gemNugget(id, gem, GemDecor.NONE);
    }

    private static ItemEntry<Item> gemNugget(String id, AllGemTags gem, GemDecor decor) {
        return oreItem(CoeRegistrate.REGISTRATE.item(id, Item::new)
            .tag(Tags.Items.NUGGETS).tag(gem.nuggets), decor).register();
    }

    /** 粉碎矿（Create 粉碎原料 + 本族粉碎标签）。 */
    private static ItemEntry<Item> gemCrushed(String id, AllGemTags gem) {
        return gemCrushed(id, gem, GemDecor.NONE);
    }

    private static ItemEntry<Item> gemCrushed(String id, AllGemTags gem, GemDecor decor) {
        return oreItem(CoeRegistrate.REGISTRATE.item(id, Item::new)
            .tag(CRUSHED_RAW_MATERIALS.tag).tag(gem.crushedRawOres), decor).register();
    }

    /** 无标签的宝石零件（小碎片 / 大碎片这类中间产物）。 */
    private static ItemEntry<Item> gemPart(String id) {
        return gemPart(id, GemDecor.NONE);
    }

    private static ItemEntry<Item> gemPart(String id, GemDecor decor) {
        return oreItem(CoeRegistrate.REGISTRATE.item(id, Item::new), decor).register();
    }

    /** 板。 */
    private static ItemEntry<Item> gemSheet(String id, AllGemTags gem) {
        return gemSheet(id, gem, GemDecor.NONE);
    }

    private static ItemEntry<Item> gemSheet(String id, AllGemTags gem, GemDecor decor) {
        return oreItem(CoeRegistrate.REGISTRATE.item(id, Item::new).tag(gem.sheets), decor).register();
    }

    /** 杆（Create 杆标签一并挂上，参与 Create 的配方识别）。 */
    private static ItemEntry<Item> gemRod(String id, AllGemTags gem) {
        return gemRod(id, gem, GemDecor.NONE);
    }

    private static ItemEntry<Item> gemRod(String id, AllGemTags gem, GemDecor decor) {
        return oreItem(CoeRegistrate.REGISTRATE.item(id, Item::new)
            .tag(gem.rods)
            .tag(AllTags.AllItemTags.RODS.tag)
            .tag(AllTags.AllItemTags.RODS_ALL_METAL.tag), decor).register();
    }

    /** 线（Create 线标签一并挂上）。 */
    private static ItemEntry<Item> gemWire(String id, AllGemTags gem) {
        return gemWire(id, gem, GemDecor.NONE);
    }

    private static ItemEntry<Item> gemWire(String id, AllGemTags gem, GemDecor decor) {
        return oreItem(CoeRegistrate.REGISTRATE.item(id, Item::new)
            .tag(gem.wires)
            .tag(AllTags.AllItemTags.WIRES.tag)
            .tag(AllTags.AllItemTags.WIRES_ALL_METAL.tag), decor).register();
    }

    /**
     * <b>凝能佩</b>（Curios 项链/手饰槽位；Curios 为 optional，注册期按"在不在"选两支物品类之一
     * —— 见 {@link MedallionCurios}，玩家侧无感、存档里还是同一个物品）。
     *
     * <p>六族<b>同形</b>，只有五处不同：物品类 / 佩种标识 / 能量上限 / 配色 / 可选的系列物品标签。
     * 原来每族各写 15 行（共 ~90 行），现在收进这一个 helper，声明处三行。</p>
     *
     * <p>「加入工具充能配方」从过去的 `static { ChargingRecipeTools.register(X); }` 改成
     * {@code onRegister} 回调：语义相同（注册即登记），但不必再为每件写一个静态块，
     * 也不依赖"静态块与字段初始化的先后"这种隐式约定。</p>
     */
    private static <T extends BaseStressMedallionItem> ItemEntry<T> stressMedallion(
            String id, String kind, NonNullFunction<Item.Properties, T> ctor,
            int energy, ToolEnergyColorConfig color, @Nullable TagKey<Item> seriesTag) {
        var builder = CoeRegistrate.REGISTRATE.item(id, MedallionCurios.item(kind, ctor));
        if (seriesTag != null) {
            builder = builder.tag(seriesTag);
        }
        return builder
            .properties(p -> p
                .component(AllDataComponents.ENERGY, energy)
                .component(AllDataComponents.MAX_ENERGY, energy)
                .component(AllDataComponents.ENERGY_COLOR, color.light.getRGB())
                .component(AllDataComponents.ENERGY_COLOR_DARK, color.dark.getRGB())
                .component(AllDataComponents.MEDALLION_MODE, false)
                .component(AllDataComponents.BIND_COLOR, color.light.getRGB()))
            .model((ctx, provider) -> provider.basicItem(ctx.get()))
            .transform(CoeCreativeSections.section(CoeCreativeSections.CreativeSection.GEAR))
            .onRegister(ChargingRecipeTools::register)
            .register();
    }

    public static <T extends Item, P> NonNullFunction<ItemBuilder<T, P>, SkillItemBuilder<T, P>> skillItem() {
        return SkillItemBuilder::new;
    }

    // ========== 四套盔甲（翠玉 / 宝石 / 星界 / 雷鸣，4 套 × 4 件 = 16 件） ==========
    // 用户 2026-10-01 裁定：**护甲物品的注册也住在 CoeItems**（与工具/佩/弓、以及今后的回旋镖
    // 同一个文件、同一条注册路径）。在此之前它被单独放在 CoeArmorItems 里（W13 我自己的分家决定，
    // 没有任何裁定依据）；后果是"弓与工具的能量注册在 CoeItems、护甲的在别处"，同一件事两处口径。
    // 现在统一：声明在这里，构建器 armor(...) 与耐久度表 Durability 也在本类里。
    //
    // 位置刻意放在**最后一个物品块之后**：合并前护甲是由 CoeItems.register() 末尾触发
    // CoeArmorItems.register() 注册的 ⇒ 注册顺序本来就排在最后。放在这里才与合并前**逐位一致**
    // （物品注册顺序影响创造页内的排列次序，纯搬运不该顺手改掉它）。
    // 素材与装备层出处见各套注释（贴图未改一像素）。

    // ---- 翠玉盔甲（jade_topaz）：组1（亮绿 + 金）----
    // 装备层 1790668020120(layer_1) / 1790670229300(layer_2)

    public static final ItemEntry<CoeArmorItem> JADE_TOPAZ_HELMET = armor(
        "jade_topaz_helmet", CoeArmorMaterials.JADE_TOPAZ, ArmorItem.Type.HELMET, "jade_topaz_armor", Durability.JADE_TOPAZ_HELMET);

    public static final ItemEntry<CoeArmorItem> JADE_TOPAZ_CHESTPLATE = armor(
        "jade_topaz_chestplate", CoeArmorMaterials.JADE_TOPAZ, ArmorItem.Type.CHESTPLATE, "jade_topaz_armor", Durability.JADE_TOPAZ_CHESTPLATE);

    public static final ItemEntry<CoeArmorItem> JADE_TOPAZ_LEGGINGS = armor(
        "jade_topaz_leggings", CoeArmorMaterials.JADE_TOPAZ, ArmorItem.Type.LEGGINGS, "jade_topaz_armor", Durability.JADE_TOPAZ_LEGGINGS);

    public static final ItemEntry<CoeArmorItem> JADE_TOPAZ_BOOTS = armor(
        "jade_topaz_boots", CoeArmorMaterials.JADE_TOPAZ, ArmorItem.Type.BOOTS, "jade_topaz_armor", Durability.JADE_TOPAZ_BOOTS);

    // ---- 宝石盔甲（sapphire_ruby）：组2（深蓝紫 + 红，饱和度最高）----
    // 装备层 1790670637200(layer_1) / 1790670820040(layer_2)

    public static final ItemEntry<CoeArmorItem> SAPPHIRE_RUBY_HELMET = armor(
        "sapphire_ruby_helmet", CoeArmorMaterials.SAPPHIRE_RUBY, ArmorItem.Type.HELMET, "sapphire_ruby_armor", Durability.SAPPHIRE_RUBY_HELMET);

    public static final ItemEntry<CoeArmorItem> SAPPHIRE_RUBY_CHESTPLATE = armor(
        "sapphire_ruby_chestplate", CoeArmorMaterials.SAPPHIRE_RUBY, ArmorItem.Type.CHESTPLATE, "sapphire_ruby_armor", Durability.SAPPHIRE_RUBY_CHESTPLATE);

    public static final ItemEntry<CoeArmorItem> SAPPHIRE_RUBY_LEGGINGS = armor(
        "sapphire_ruby_leggings", CoeArmorMaterials.SAPPHIRE_RUBY, ArmorItem.Type.LEGGINGS, "sapphire_ruby_armor", Durability.SAPPHIRE_RUBY_LEGGINGS);

    public static final ItemEntry<CoeArmorItem> SAPPHIRE_RUBY_BOOTS = armor(
        "sapphire_ruby_boots", CoeArmorMaterials.SAPPHIRE_RUBY, ArmorItem.Type.BOOTS, "sapphire_ruby_armor", Durability.SAPPHIRE_RUBY_BOOTS);

    // ---- 星界盔甲（astral）：组3（淡蓝灰 + 粉/青，低饱和）----
    // 装备层 1790676421592(layer_1) / 1790676959906(layer_2)

    public static final ItemEntry<CoeArmorItem> ASTRAL_HELMET = armor(
        "astral_helmet", CoeArmorMaterials.ASTRAL, ArmorItem.Type.HELMET, "astral_armor", Durability.ASTRAL_HELMET);

    public static final ItemEntry<CoeArmorItem> ASTRAL_CHESTPLATE = armor(
        "astral_chestplate", CoeArmorMaterials.ASTRAL, ArmorItem.Type.CHESTPLATE, "astral_armor", Durability.ASTRAL_CHESTPLATE);

    public static final ItemEntry<CoeArmorItem> ASTRAL_LEGGINGS = armor(
        "astral_leggings", CoeArmorMaterials.ASTRAL, ArmorItem.Type.LEGGINGS, "astral_armor", Durability.ASTRAL_LEGGINGS);

    public static final ItemEntry<CoeArmorItem> ASTRAL_BOOTS = armor(
        "astral_boots", CoeArmorMaterials.ASTRAL, ArmorItem.Type.BOOTS, "astral_armor", Durability.ASTRAL_BOOTS);

    // ---- 雷鸣盔甲（thunder）：E:\mc\mc资料\雷鸣合金系列和玉牌（light periwinkle，avgB 175）----
    // 装备层 1786950423536(layer_1) / 1786950487011(layer_2)

    public static final ItemEntry<CoeArmorItem> THUNDER_HELMET = armor(
        "thunder_helmet", CoeArmorMaterials.THUNDER, ArmorItem.Type.HELMET, "thunder_armor", Durability.THUNDER_HELMET);

    public static final ItemEntry<CoeArmorItem> THUNDER_CHESTPLATE = armor(
        "thunder_chestplate", CoeArmorMaterials.THUNDER, ArmorItem.Type.CHESTPLATE, "thunder_armor", Durability.THUNDER_CHESTPLATE);

    public static final ItemEntry<CoeArmorItem> THUNDER_LEGGINGS = armor(
        "thunder_leggings", CoeArmorMaterials.THUNDER, ArmorItem.Type.LEGGINGS, "thunder_armor", Durability.THUNDER_LEGGINGS);

    public static final ItemEntry<CoeArmorItem> THUNDER_BOOTS = armor(
        "thunder_boots", CoeArmorMaterials.THUNDER, ArmorItem.Type.BOOTS, "thunder_armor", Durability.THUNDER_BOOTS);

    // ========== 盔甲注册辅助（与 grindingWheel(...) 同一种"辅助方法 + 一行声明"写法） ==========

    /**
     * <b>四套盔甲的逐件耐久度</b>——数值集中在这里，改平衡只改这一段。
     *
     * <p>换算依据（1.21.1 实测字节码）：原版把「基准 × 槽位倍率」写进 {@code Item.Properties.durability}，
     * 基准取自 {@code ArmorItem.Type.getDurability(int)}，<b>下界合金基准 = 37</b>，
     * 槽位倍率 = 头盔 11 / 胸甲 16 / 护腿 15 / 靴子 13。于是：</p>
     * <pre>
     * 材质   基准  头盔(×11)  胸甲(×16)  护腿(×15)  靴子(×13)   与下界合金之比
     * 钻石    33      363       528       495       429        0.89x（原版对照）
     * 下界合金 37      407       592       555       481        1.00x（基准）
     * 翠玉    36      396       576       540       468        0.97x（= 钻/合金 3/4 分界点）
     * 宝石    47      517       752       705       611        1.27x（≥1.25x）
     * 星界    74      814      1184      1110       962        2.00x（恰为 2 倍）
     * 雷鸣    74      814      1184      1110       962        2.00x（与星界同）
     * </pre>
     */
    public static final class Durability {
        /** 原版钻石盔甲的耐久基准（对照用，不参与注册）。 */
        public static final int VANILLA_DIAMOND_BASE = 33;
        /** 原版下界合金盔甲的耐久基准（本模组四套的换算基准）。 */
        public static final int VANILLA_NETHERITE_BASE = 37;

        // 翠玉：钻石与下界合金的 3/4 分界点 = 33 + (37-33)*3/4 = 36
        public static final int JADE_TOPAZ_HELMET = 36 * 11;
        public static final int JADE_TOPAZ_CHESTPLATE = 36 * 16;
        public static final int JADE_TOPAZ_LEGGINGS = 36 * 15;
        public static final int JADE_TOPAZ_BOOTS = 36 * 13;

        // 宝石：下界合金 1.25 倍（37*1.25 = 46.25 → 47，保证不低于 1.25 倍）
        public static final int SAPPHIRE_RUBY_HELMET = 47 * 11;
        public static final int SAPPHIRE_RUBY_CHESTPLATE = 47 * 16;
        public static final int SAPPHIRE_RUBY_LEGGINGS = 47 * 15;
        public static final int SAPPHIRE_RUBY_BOOTS = 47 * 13;

        // 星界：下界合金 2 倍
        public static final int ASTRAL_HELMET = 74 * 11;
        public static final int ASTRAL_CHESTPLATE = 74 * 16;
        public static final int ASTRAL_LEGGINGS = 74 * 15;
        public static final int ASTRAL_BOOTS = 74 * 13;

        // 雷鸣：与星界相同（下界合金 2 倍）
        public static final int THUNDER_HELMET = 74 * 11;
        public static final int THUNDER_CHESTPLATE = 74 * 16;
        public static final int THUNDER_LEGGINGS = 74 * 15;
        public static final int THUNDER_BOOTS = 74 * 13;

        private Durability() {}
    }

    /**
     * 注册一件盔甲（四件事集中在这一处）。
     *
     * <p>① 材质（{@code Holder<ArmorMaterial>}）；② 槽位与贴图基名（{@link CoeArmorItem} 用后者拼
     * {@code _layer_N} 路径）；③ 逐件耐久度；④ 护甲附魔标签（原版按槽位分 4 个 tag）+ 盔甲纹饰标签；
     * ⑤ 物品图标模型（{@code item/generated}，贴图 {@code item/<id>}）；⑥ <b>能量</b>。</p>
     *
     * <p>⑥ 的口径（用户 2026-10-01）：<b>初始即满</b> —— ENERGY 与 MAX_ENERGY 写同一个值，与工具那套
     * （{@code EnergyItemBuilder} 的 {@code defaultEnergy == maxEnergy}）同形。容量真源 =
     * {@code ArmorSet.perPieceEnergy()}（翠玉 250 / 宝石 1000 / 星界·雷鸣 2500），按材质查套
     * （注册期没有 ItemStack，故走 {@code ArmorSet.byMaterial}）。组件是逐堆的 ⇒ 老存档里已存在的
     * 那件保留自己存的值，不回溯、不需要迁移。</p>
     *
     * @param id           注册 id（同时是图标贴图名）
     * @param material     盔甲材质 holder（见 {@link CoeArmorMaterials}）
     * @param type         槽位
     * @param textureBase  装备层贴图基名（不带命名空间，不带 {@code _layer_N} 后缀）
     * @param durability   该件的耐久度（见上面 {@link Durability} 的换算表）
     */
    private static ItemEntry<CoeArmorItem> armor(String id, Holder<ArmorMaterial> material,
                                                 ArmorItem.Type type, String textureBase, int durability) {
        ResourceLocation textureLoc = CoeCore.modLoc(textureBase);
        ArmorSet set = ArmorSet.byMaterial(material);
        int perPiece = set == null ? 0 : set.perPieceEnergy();
        // ⑦ 系列归属（用户 2026-10-01："护甲也算系列成员，享受全部特性"）：
        //    星界套 = 星辉石系列（岩浆/善化液不销毁 + 虚空浮回 + 雷击充满）
        //    雷鸣套 = 雷鸣合金系列（岩浆不销毁 + 雷击充满）
        //    判定与行为都读 SeriesTraits（唯一入口），这里只写"这件东西属于哪个系列"。
        TagKey<Item> seriesTag = set == null ? null : switch (set) {
            case ASTRAL -> AllModItemTags.STELLARSTONE_ITEMS;
            case THUNDER -> AllModItemTags.THUNDERITE_ITEMS;
            default -> null;
        };
        var builder = CoeRegistrate.REGISTRATE
            .item(id, p -> new CoeArmorItem(material, type, p, textureLoc))
            // 创造页分区：盔甲属「装备」——注册处一行声明（用户 2026-10-01 的固定套路）
            .transform(CoeCreativeSections.section(CoeCreativeSections.CreativeSection.GEAR));
        if (seriesTag != null) {
            builder = builder.tag(seriesTag);
        }
        return builder
            .properties(p -> p.durability(durability)
                .component(AllDataComponents.ENERGY, perPiece)
                .component(AllDataComponents.MAX_ENERGY, perPiece))
            .tag(armorTag(type))
            .tag(ItemTags.TRIMMABLE_ARMOR)
            .model((ctx, provider) -> provider.generated(ctx::get, CoeCore.modLoc("item/" + id)))
            .register();
    }

    /** 槽位 → 原版护甲标签（决定护甲类附魔能否附上）。 */
    private static net.minecraft.tags.TagKey<Item> armorTag(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> ItemTags.HEAD_ARMOR;
            case CHESTPLATE -> ItemTags.CHEST_ARMOR;
            case LEGGINGS -> ItemTags.LEG_ARMOR;
            case BOOTS -> ItemTags.FOOT_ARMOR;
            default -> ItemTags.HEAD_ARMOR; // BODY（狼/马甲）本模组不使用
        };
    }

    public static class SkillItemBuilder<T extends Item, P> implements
            Builder<Item, T, ItemBuilder<T, P>, SkillItemBuilder<T, P>> {
        public List<DataSkill> skillData = new ArrayList<>();

        private final ItemBuilder<T, P> builder;
        /** 武器技能发光颜色（延迟到register 时注册到SkillOutlineColors）*/
        private SkillOutlineColors.SkillColor outlineColor;
        /** 武器技能冷却时长（tick，延迟到 register 时注册到 SkillCooldowns；1 表示未设置）*/
        private int cooldownTicks = -1;
        /** 是否为能量物品（调用了 addEnergy()）：注册后自动加入工具充能配方 */
        private boolean energyItem;

        public SkillItemBuilder(ItemBuilder<T, P> builder) {
            this.builder = builder;
        }

        /**
         * 绑定技能（使用技能注册时的默认等级）。
         */
        public SkillItemBuilder<T, P> addSkills(DataSkill... skills) {
            skillData.addAll(Arrays.stream(skills)
                    .toList());
            return this;
        }

        /**
         * 绑定技能并指定等级后，等级同时决定显示等级与实际技能数值等级（一技能多等级）。
         *
         * 内部复制技能数据后写入等级，并按等级从注册表取出对应配置（configForLevel）。
         * 不同物品可绑同一技能但使用不同等级的实际效果。
         *
         * @param skill 技能数据（来自 AllSkills）
         * @param level 技能等级 1~5）。
         */
        public SkillItemBuilder<T, P> addSkills(DataSkill skill, int level) {
            DataSkill copy = skill.copy();
            copy.getOrCreateNbt().putInt("Level", level);

            // 一技能多等级：按等级取实际配置并写入副本（未设置映射时退回注册默认配置）。
            if (skill instanceof AllSkills.RegisteredDataSkill registered) {
                SkillConfig levelConfig = registered.configForLevel(level);
                if (levelConfig != null) {
                    copy.config = levelConfig;
                    levelConfig.accept(copy.getOrCreateNbt()); // 配置写入 NBT，保证存??网络恢复一??
                }
            }

            skillData.add(copy);
            return this;
        }

        /**
         * 设置武器技能发光颜色，并注册到 {@link SkillOutlineColors}??
         *
         * 注册表为渲染端的统一颜色来源（无需依赖 NBT）。
         */
        public SkillItemBuilder<T, P> skillColor(SkillOutlineColors.SkillColor color) {
            this.outlineColor = color;
            skillData.forEach(data -> {
                CompoundTag nbt = data.getOrCreateNbt();
                CompoundTag colorTag = new CompoundTag();
                colorTag.putFloat("r", color.r());
                colorTag.putFloat("g", color.g());
                colorTag.putFloat("b", color.b());
                nbt.put("OutlineColor", colorTag);
                data.nbt = nbt;
            });
            return this;
        }

        /**
         * 设置武器技能冷却时长，并注册到 {@link SkillCooldowns}??
         *
         * 注册表为触发端的统一冷却来源。未设置时使用默认值 1 秒）。
         *
         * @param cooldownTicks 冷却时长（tick；20 tick = 1 秒）。
         */
        public SkillItemBuilder<T, P> skillCooldown(int cooldownTicks) {
            this.cooldownTicks = cooldownTicks;
            return this;
        }

        public EnergyItemBuilder<T, P> addEnergy() {
            return new EnergyItemBuilder<>(this);
        }

        @Override
        public @NotNull RegistryEntry<Item, T> register() {
            // 颜色已由skillColor() 写入技能NBT（渲染端直接读取），此处只需注册冷却；
            // 能量工具的充电配方挂接在 build() 的 onRegister（链末尾 .register() 是 ItemBuilder 的）
            RegistryEntry<Item, T> entry = builder.register();
            if (cooldownTicks > 0) {
                SkillCooldowns.register(entry.get(), cooldownTicks);
            }
            return entry;
        }

        @Override
        public @NotNull AbstractRegistrate<?> getOwner() {
            return builder.getOwner();
        }

        @Override
        public @NotNull ItemBuilder<T, P> getParent() {
            return builder;
        }

        @Override
        public @NotNull String getName() {
            return builder.getName();
        }

        @Override
        public @NotNull ResourceKey<? extends Registry<Item>> getRegistryKey() {
            return builder.getRegistryKey();
        }

        @Override
        public @NotNull NonNullSupplier<T> asSupplier() {
            return builder.asSupplier();
        }

        public @NotNull ItemBuilder<T, P> build() {
            builder.properties(p -> p.component(AllDataComponents.SKILLS, new SkillsComponent(skillData)));
            // 能量工具（调用了 addEnergy）：物品实际注册时加入工具充能配方（单一数据源）。
            // 注意：链末尾的 .register() 是 ItemBuilder 的（build() 返回 builder），
            // 所以在这里用 onRegister 挂接，而不是本类 register()
            if (energyItem) {
                builder.onRegister(item -> ChargingRecipeTools.register(item));
            }
            // 创造页分区：**走 skillItem() 的都是装备**（工具/弓）⇒ 在这里一处声明，26 件全覆盖。
            // 这就是用户 2026-10-01 要的"注册处一行即归栏"（机制见 CoeCreativeSections 的 section(...)）。
            builder.onRegister(item -> CoeCreativeSections.declare(
                item, CoeCreativeSections.CreativeSection.GEAR));
            return builder;
        }

        public static class EnergyItemBuilder<T extends Item, P> {
            int defaultEnergy = 100;
            int maxEnergy = 1000;
            ToolEnergyColorConfig config = ToolEnergyColorConfig.DEFAULT;

            private final SkillItemBuilder<T, P> builder;

            public EnergyItemBuilder(SkillItemBuilder<T, P> builder) {
                this.builder = builder;
                // 标记为能量物品（可被充能器充能，注册时加入工具充能配方）
                builder.energyItem = true;
            }

            public EnergyItemBuilder<T, P> defaultEnergy(int energy) {
                this.defaultEnergy = energy;
                return this;
            }

            public EnergyItemBuilder<T, P> maxEnergy(int energy) {
                this.maxEnergy = energy;
                return this;
            }

            public EnergyItemBuilder<T, P> color(ToolEnergyColorConfig config) {
                this.config = config;
                return this;
            }

            public SkillItemBuilder<T, P> build() {
                builder.builder.properties(p -> p
                        .component(AllDataComponents.ENERGY, defaultEnergy)
                        .component(AllDataComponents.MAX_ENERGY, maxEnergy)
                        .component(AllDataComponents.ENERGY_COLOR, config.light.getRGB())
                        .component(AllDataComponents.ENERGY_COLOR_DARK, config.dark.getRGB()));
                return builder;
            }
        }
    }

/** 触发本层注册类的类初始化：Registrate 的注册动作就是字段初始化，因此方法体为空。 */
	public static void register() {
		// 唯一线索：物品注册的先后顺序 = 本类字段的书写顺序。
		// 2026-10-01 起四套盔甲的声明也在本类（原先分在 CoeArmorItems，用户裁定要统一）；
		// 弓 / 回旋镖 / 佩 / 工具同样都在这里，因此**不要再为本模组的物品另开注册文件**。
	}

	private CoeItems() {
	}
}
