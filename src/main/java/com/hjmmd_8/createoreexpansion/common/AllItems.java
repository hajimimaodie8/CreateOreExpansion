package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.client.tool.SkillOutlineColors;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.JadeStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.NetheriteStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.SapphireStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.StellarstoneStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.ThunderiteStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.TopazStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergyColorConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.simibubi.create.AllTags.AllItemTags.CREATE_INGOTS;
import static com.simibubi.create.AllTags.AllItemTags.CRUSHED_RAW_MATERIALS;

public final class AllItems {
    // ===== 机械动力方式注册 =====

    // 这个变量名可以随便写，好理解就行，一般是item id的大写
    // 这里调用了MoreCreateOre类的static field(字段？ REGSITRATE？
    public static final ItemEntry<Item> JADE_INGOT = CreateOreExpansion.REGISTRATE
            // 调用方式？
            .item("jade_ingot", Item::new)
            .tag(CREATE_INGOTS.tag)
            .tag(Tags.Items.INGOTS)
            .tag(AllMetalTags.JADE.ingots)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> RAW_JADE = CreateOreExpansion.REGISTRATE
            .item("raw_jade", Item::new)
            .tag(Tags.Items.RAW_MATERIALS)
            .tag(AllMetalTags.JADE.rawOres)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> JADE_NUGGET = CreateOreExpansion.REGISTRATE
            .item("jade_nugget", Item::new)
            .tag(Tags.Items.NUGGETS)
            .tag(AllMetalTags.JADE.nuggets)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> CRUSHED_JADE_ORE = CreateOreExpansion.REGISTRATE
            .item("crushed_jade_ore", Item::new)
            .tag(CRUSHED_RAW_MATERIALS.tag)
            .tag(AllMetalTags.JADE.crushedRawOres)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> JADE_SMALL_SHARD = CreateOreExpansion.REGISTRATE
            .item("jade_small_shard", Item::new)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> JADE_BIG_SHARD = CreateOreExpansion.REGISTRATE
            .item("jade_big_shard", Item::new)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> JADE_SHEET = CreateOreExpansion.REGISTRATE
            .item("jade_sheet", Item::new)
            .tag(AllMetalTags.JADE.sheets)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> JADE_ROD = CreateOreExpansion.REGISTRATE
            .item("jade_rod", Item::new)
            .tag(AllMetalTags.JADE.rods)
            .tag(AllTags.AllItemTags.RODS.tag)
            .tag(AllTags.AllItemTags.RODS_ALL_METAL.tag)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> JADE_WIRE = CreateOreExpansion.REGISTRATE
            .item("jade_wire", Item::new)
            .tag(AllMetalTags.JADE.wires)
            .tag(AllTags.AllItemTags.WIRES.tag)
            .tag(AllTags.AllItemTags.WIRES_ALL_METAL.tag)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<SwordItem> JADE_SWORD = CreateOreExpansion.REGISTRATE
            .item("jade_sword", p -> new SwordItem(AllTiers.JADE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    SwordItem.createAttributes(AllTiers.JADE, 4, -2.4F)
            ))
            // 添加横扫的标签，不然没有横扫效果
            .tag(ItemTags.SWORDS)
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

    public static final ItemEntry<PickaxeItem> JADE_PICKAXE = CreateOreExpansion.REGISTRATE
            .item("jade_pickaxe", p -> new PickaxeItem(AllTiers.JADE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    PickaxeItem.createAttributes(AllTiers.JADE, 1, -2.8F)
            ))
            .tag(ItemTags.PICKAXES)
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

    public static final ItemEntry<AxeItem> JADE_AXE = CreateOreExpansion.REGISTRATE
            .item("jade_axe", p -> new AxeItem(AllTiers.JADE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    AxeItem.createAttributes(AllTiers.JADE, 6, -3.0F)
            ))
            .tag(ItemTags.AXES)
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

    public static final ItemEntry<ShovelItem> JADE_SHOVEL = CreateOreExpansion.REGISTRATE
            .item("jade_shovel", p -> new ShovelItem(AllTiers.JADE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    ShovelItem.createAttributes(AllTiers.JADE, 1.5F, -3.0F)
            ))
            .tag(ItemTags.SHOVELS)
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
    public static final ItemEntry<HoeItem> JADE_HOE = CreateOreExpansion.REGISTRATE
            .item("jade_hoe", p -> new HoeItem(AllTiers.JADE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    HoeItem.createAttributes(AllTiers.JADE, -2, -1.0F)
            ))
            .tag(ItemTags.HOES)
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

    // ========== 饰品：凝能佩（Curios 项链/手饰槽位，槽位数据手写到data/curios/??==========
    public static final ItemEntry<JadeStressMedallionItem> JADE_STRESS_MEDALLION = CreateOreExpansion.REGISTRATE
            .item("jade_stress_medallion", JadeStressMedallionItem::new)
            .properties(p -> p
                    .component(AllDataComponents.ENERGY, 1000)
                    .component(AllDataComponents.MAX_ENERGY, 1000)
                    .component(AllDataComponents.ENERGY_COLOR, ToolEnergyColorConfig.JADE.light.getRGB())
                    .component(AllDataComponents.ENERGY_COLOR_DARK, ToolEnergyColorConfig.JADE.dark.getRGB())
                    .component(AllDataComponents.MEDALLION_MODE, false)
                    .component(AllDataComponents.BIND_COLOR, ToolEnergyColorConfig.JADE.light.getRGB()))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> TOPAZ_INGOT = CreateOreExpansion.REGISTRATE
            .item("topaz_ingot", Item::new)
            .tag(CREATE_INGOTS.tag)
            .tag(Tags.Items.INGOTS)
            .tag(AllMetalTags.TOPAZ.ingots)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> RAW_TOPAZ = CreateOreExpansion.REGISTRATE
            .item("raw_topaz", Item::new)
            .tag(Tags.Items.RAW_MATERIALS)
            .tag(AllMetalTags.TOPAZ.rawOres)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> TOPAZ_NUGGET = CreateOreExpansion.REGISTRATE
            .item("topaz_nugget", Item::new)
            .tag(Tags.Items.NUGGETS)
            .tag(AllMetalTags.TOPAZ.nuggets)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> CRUSHED_TOPAZ_ORE = CreateOreExpansion.REGISTRATE
            .item("crushed_topaz_ore", Item::new)
            .tag(CRUSHED_RAW_MATERIALS.tag)
            .tag(AllMetalTags.TOPAZ.crushedRawOres)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> TOPAZ_SMALL_SHARD = CreateOreExpansion.REGISTRATE
            .item("topaz_small_shard", Item::new)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> TOPAZ_BIG_SHARD = CreateOreExpansion.REGISTRATE
            .item("topaz_big_shard", Item::new)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> TOPAZ_SHEET = CreateOreExpansion.REGISTRATE
            .item("topaz_sheet", Item::new)
            .tag(AllMetalTags.TOPAZ.sheets)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> TOPAZ_ROD = CreateOreExpansion.REGISTRATE
            .item("topaz_rod", Item::new)
            .tag(AllMetalTags.TOPAZ.rods)
            .tag(AllTags.AllItemTags.RODS.tag)
            .tag(AllTags.AllItemTags.RODS_ALL_METAL.tag)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> TOPAZ_WIRE = CreateOreExpansion.REGISTRATE
            .item("topaz_wire", Item::new)
            .tag(AllMetalTags.TOPAZ.wires)
            .tag(AllTags.AllItemTags.WIRES.tag)
            .tag(AllTags.AllItemTags.WIRES_ALL_METAL.tag)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<SwordItem> TOPAZ_SWORD = CreateOreExpansion.REGISTRATE
            .item("topaz_sword", p -> new SwordItem(AllTiers.TOPAZ, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    SwordItem.createAttributes(AllTiers.TOPAZ, 4, -2.4F)
            ))
            .tag(ItemTags.SWORDS)
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

    public static final ItemEntry<PickaxeItem> TOPAZ_PICKAXE = CreateOreExpansion.REGISTRATE
            .item("topaz_pickaxe", p -> new PickaxeItem (AllTiers.TOPAZ, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    PickaxeItem.createAttributes(AllTiers.TOPAZ, 1.5F, -2.3F)
            ))
            .tag(ItemTags.PICKAXES)
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

    public static final ItemEntry<ShovelItem> TOPAZ_SHOVEL= CreateOreExpansion.REGISTRATE
            .item("topaz_shovel", p -> new ShovelItem (AllTiers.TOPAZ, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    ShovelItem.createAttributes(AllTiers.TOPAZ, 1.5F, -2.8F)
            ))
            .tag(ItemTags.SHOVELS)
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

    public static final ItemEntry<AxeItem> TOPAZ_AXE = CreateOreExpansion.REGISTRATE
            .item("topaz_axe", p -> new AxeItem (AllTiers.TOPAZ, p))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .properties(p -> p.attributes(
                    AxeItem.createAttributes(AllTiers.TOPAZ, 6.5F, -3.2F)
            ))
            .tag(ItemTags.AXES)
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
    public static final ItemEntry<HoeItem> TOPAZ_HOE = CreateOreExpansion.REGISTRATE
            .item("topaz_hoe", p -> new HoeItem(AllTiers.TOPAZ, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    HoeItem.createAttributes(AllTiers.TOPAZ, -1.5F, 0.0F)
            ))
            .tag(ItemTags.HOES)
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

    public static final ItemEntry<TopazStressMedallionItem> TOPAZ_STRESS_MEDALLION = CreateOreExpansion.REGISTRATE
            .item("topaz_stress_medallion", TopazStressMedallionItem::new)
            .properties(p -> p
                    .component(AllDataComponents.ENERGY, 2500)
                    .component(AllDataComponents.MAX_ENERGY, 2500)
                    .component(AllDataComponents.ENERGY_COLOR, ToolEnergyColorConfig.TOPAZ.light.getRGB())
                    .component(AllDataComponents.ENERGY_COLOR_DARK, ToolEnergyColorConfig.TOPAZ.dark.getRGB())
                    .component(AllDataComponents.MEDALLION_MODE, false)
                    .component(AllDataComponents.BIND_COLOR, ToolEnergyColorConfig.TOPAZ.light.getRGB()))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> SAPPHIRE_INGOT = CreateOreExpansion.REGISTRATE
            .item("sapphire_ingot", Item::new)
            .tag(CREATE_INGOTS.tag)
            .tag(Tags.Items.INGOTS)
            .tag(AllMetalTags.SAPPHIRE.ingots)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> RAW_SAPPHIRE = CreateOreExpansion.REGISTRATE
            .item("raw_sapphire", Item::new)
            .tag(Tags.Items.RAW_MATERIALS)
            .tag(AllMetalTags.SAPPHIRE.rawOres)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> SAPPHIRE_NUGGET = CreateOreExpansion.REGISTRATE
            .item("sapphire_nugget", Item::new)
            .tag(Tags.Items.NUGGETS)
            .tag(AllMetalTags.SAPPHIRE.nuggets)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> CRUSHED_SAPPHIRE_ORE = CreateOreExpansion.REGISTRATE    
            .item("crushed_sapphire_ore", Item::new)
            .tag(CRUSHED_RAW_MATERIALS.tag)
            .tag(AllMetalTags.SAPPHIRE.crushedRawOres)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> SAPPHIRE_SMALL_SHARD = CreateOreExpansion.REGISTRATE
            .item("sapphire_small_shard", Item::new)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> SAPPHIRE_BIG_SHARD = CreateOreExpansion.REGISTRATE
            .item("sapphire_big_shard", Item::new)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> SAPPHIRE_SHEET = CreateOreExpansion.REGISTRATE
            .item("sapphire_sheet", Item::new)
            .tag(AllMetalTags.SAPPHIRE.sheets)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> SAPPHIRE_ROD = CreateOreExpansion.REGISTRATE
            .item("sapphire_rod", Item::new)
            .tag(AllMetalTags.SAPPHIRE.rods)
            .tag(AllTags.AllItemTags.RODS.tag)
            .tag(AllTags.AllItemTags.RODS_ALL_METAL.tag)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> SAPPHIRE_WIRE = CreateOreExpansion.REGISTRATE
            .item("sapphire_wire", Item::new)
            .tag(AllMetalTags.SAPPHIRE.wires)
            .tag(AllTags.AllItemTags.WIRES.tag)
            .tag(AllTags.AllItemTags.WIRES_ALL_METAL.tag)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<SwordItem> SAPPHIRE_SWORD = CreateOreExpansion.REGISTRATE
            .item("sapphire_sword", p -> new SwordItem(AllTiers.SAPPHIRE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    SwordItem.createAttributes(AllTiers.SAPPHIRE, 5, -2.4F)
            ))
            .tag(ItemTags.SWORDS)
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

    public static final ItemEntry<PickaxeItem> SAPPHIRE_PICKAXE = CreateOreExpansion.REGISTRATE
            .item("sapphire_pickaxe", p -> new PickaxeItem (AllTiers.SAPPHIRE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    PickaxeItem.createAttributes(AllTiers.SAPPHIRE, 2, -2.5F)
            ))
            .tag(ItemTags.PICKAXES)
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

    public static final ItemEntry<ShovelItem> SAPPHIRE_SHOVEL= CreateOreExpansion.REGISTRATE
            .item("sapphire_shovel", p -> new ShovelItem (AllTiers.SAPPHIRE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    ShovelItem.createAttributes(AllTiers.SAPPHIRE, 1.5F, -2.8F)
            ))
            .tag(ItemTags.SHOVELS)
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

    public static final ItemEntry<AxeItem> SAPPHIRE_AXE = CreateOreExpansion.REGISTRATE
            .item("sapphire_axe", p -> new AxeItem (AllTiers.SAPPHIRE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    AxeItem.createAttributes(AllTiers.SAPPHIRE, 6.5F, -3.4F)
            ))
            .tag(ItemTags.AXES)
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

    public static final ItemEntry<HoeItem> SAPPHIRE_HOE = CreateOreExpansion.REGISTRATE
            .item("sapphire_hoe", p -> new HoeItem(AllTiers.SAPPHIRE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p.attributes(
                    HoeItem.createAttributes(AllTiers.SAPPHIRE, -1, 0.0F)
            ))
            .tag(ItemTags.HOES)
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

    public static final ItemEntry<SapphireStressMedallionItem> SAPPHIRE_STRESS_MEDALLION = CreateOreExpansion.REGISTRATE
            .item("sapphire_stress_medallion", SapphireStressMedallionItem::new)
            .properties(p -> p
                    .component(AllDataComponents.ENERGY, 5000)
                    .component(AllDataComponents.MAX_ENERGY, 5000)
                    .component(AllDataComponents.ENERGY_COLOR, ToolEnergyColorConfig.SAPPHIRE.light.getRGB())
                    .component(AllDataComponents.ENERGY_COLOR_DARK, ToolEnergyColorConfig.SAPPHIRE.dark.getRGB())
                    .component(AllDataComponents.MEDALLION_MODE, false)
                    .component(AllDataComponents.BIND_COLOR, ToolEnergyColorConfig.SAPPHIRE.light.getRGB()))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<NetheriteStressMedallionItem> NETHERITE_STRESS_MEDALLION = CreateOreExpansion.REGISTRATE
            .item("netherite_stress_medallion", NetheriteStressMedallionItem::new)
            .properties(p -> p
                    .component(AllDataComponents.ENERGY, 5000)
                    .component(AllDataComponents.MAX_ENERGY, 5000)
                    .component(AllDataComponents.ENERGY_COLOR, ToolEnergyColorConfig.NETHERITE.light.getRGB())
                    .component(AllDataComponents.ENERGY_COLOR_DARK, ToolEnergyColorConfig.NETHERITE.dark.getRGB())
                    .component(AllDataComponents.MEDALLION_MODE, false)
                    .component(AllDataComponents.BIND_COLOR, ToolEnergyColorConfig.NETHERITE.light.getRGB()))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> STELLARSTONE_INGOT = CreateOreExpansion.REGISTRATE
            .item("stellarstone_ingot", Item::new)
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .tag(CREATE_INGOTS.tag)
            .tag(Tags.Items.INGOTS)
            .tag(AllMetalTags.STELLARSTONE.ingots)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> RAW_STELLARSTONE = CreateOreExpansion.REGISTRATE
            .item("raw_stellarstone", Item::new)
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .tag(Tags.Items.RAW_MATERIALS)
            .tag(AllMetalTags.STELLARSTONE.rawOres)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> STELLARSTONE_NUGGET = CreateOreExpansion.REGISTRATE
            .item("stellarstone_nugget", Item::new)
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .tag(Tags.Items.NUGGETS)
            .tag(AllMetalTags.STELLARSTONE.nuggets)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> CRUSHED_STELLARSTONE_ORE = CreateOreExpansion.REGISTRATE
            .item("crushed_stellarstone_ore", Item::new)
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .tag(CRUSHED_RAW_MATERIALS.tag)
            .tag(AllMetalTags.STELLARSTONE.crushedRawOres)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    /*public static final ItemEntry<Item> STELLARSTONE_SMALL_SHARD = CreateOreExpansion.REGISTRATE
            .item("stellarstone_small_shard", Item::new)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();*/

    public static final ItemEntry<Item> STELLARSTONE_BIG_SHARD = CreateOreExpansion.REGISTRATE
            .item("stellarstone_big_shard", Item::new)
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> STELLARSTONE_SHEET = CreateOreExpansion.REGISTRATE
            .item("stellarstone_sheet", Item::new)
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .tag(AllMetalTags.STELLARSTONE.sheets)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> STELLARSTONE_ROD = CreateOreExpansion.REGISTRATE
            .item("stellarstone_rod", Item::new)
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .tag(AllMetalTags.STELLARSTONE.rods)
            .tag(AllTags.AllItemTags.RODS.tag)
            .tag(AllTags.AllItemTags.RODS_ALL_METAL.tag)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> STELLARSTONE_WIRE = CreateOreExpansion.REGISTRATE
            .item("stellarstone_wire", Item::new)
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .tag(AllMetalTags.STELLARSTONE.wires)
            .tag(AllTags.AllItemTags.WIRES.tag)
            .tag(AllTags.AllItemTags.WIRES_ALL_METAL.tag)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<SwordItem> STELLARSTONE_SWORD = CreateOreExpansion.REGISTRATE
            .item("stellarstone_sword", p -> new SwordItem(AllTiers.STELLARSTONE, p))
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    SwordItem.createAttributes(AllTiers.STELLARSTONE, 4, -2.4F)
            ))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get)).tag(ItemTags.SWORDS)
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

    public static final ItemEntry<PickaxeItem> STELLARSTONE_PICKAXE = CreateOreExpansion.REGISTRATE
            .item("stellarstone_pickaxe", p -> new PickaxeItem(AllTiers.STELLARSTONE, p))
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    PickaxeItem.createAttributes(AllTiers.STELLARSTONE, 1.5F, -2.3F)
            ))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get)).tag(ItemTags.PICKAXES)
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

    public static final ItemEntry<ShovelItem> STELLARSTONE_SHOVEL= CreateOreExpansion.REGISTRATE
            .item("stellarstone_shovel", p -> new ShovelItem(AllTiers.STELLARSTONE, p))
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    ShovelItem.createAttributes(AllTiers.STELLARSTONE, 1.5F, -2.8F)
            ))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get)).tag(ItemTags.SHOVELS)
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

    public static final ItemEntry<AxeItem> STELLARSTONE_AXE = CreateOreExpansion.REGISTRATE
            .item("stellarstone_axe", p -> new AxeItem(AllTiers.STELLARSTONE, p))
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    AxeItem.createAttributes(AllTiers.STELLARSTONE, 6.5F, -3.2F)
            ))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get)).tag(ItemTags.AXES)
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

    public static final ItemEntry<HoeItem> STELLARSTONE_HOE = CreateOreExpansion.REGISTRATE
            .item("stellarstone_hoe", p -> new HoeItem(AllTiers.STELLARSTONE, p))
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    HoeItem.createAttributes(AllTiers.STELLARSTONE, -1.5F, 0.0F)
            ))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get)).tag(ItemTags.HOES)
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

    public static final ItemEntry<StellarstoneStressMedallionItem> STELLARSTONE_STRESS_MEDALLION = CreateOreExpansion.REGISTRATE
            .item("stellarstone_stress_medallion", StellarstoneStressMedallionItem::new)
            .properties(p -> p
                    .component(AllDataComponents.ENERGY, 10000)
                    .component(AllDataComponents.MAX_ENERGY, 10000)
                    .component(AllDataComponents.ENERGY_COLOR, ToolEnergyColorConfig.STELLARSTONE.light.getRGB())
                    .component(AllDataComponents.ENERGY_COLOR_DARK, ToolEnergyColorConfig.STELLARSTONE.dark.getRGB())
                    .component(AllDataComponents.MEDALLION_MODE, false)
                    .component(AllDataComponents.BIND_COLOR, ToolEnergyColorConfig.STELLARSTONE.light.getRGB()))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> THUNDERITE_INGOT = CreateOreExpansion.REGISTRATE
            .item("thunderite_ingot", Item::new)
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .tag(CREATE_INGOTS.tag)
            .tag(Tags.Items.INGOTS)
            .tag(AllMetalTags.THUNDERITE.ingots)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> THUNDERITE_SCRAP = CreateOreExpansion.REGISTRATE
            .item("thunderite_scrap", Item::new)
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> THUNDERITE_SHEET = CreateOreExpansion.REGISTRATE
            .item("thunderite_sheet", Item::new)
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .tag(AllMetalTags.THUNDERITE.sheets)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> THUNDERITE_ROD = CreateOreExpansion.REGISTRATE
            .item("thunderite_rod", Item::new)
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .tag(AllMetalTags.THUNDERITE.rods)
            .tag(AllTags.AllItemTags.RODS.tag)
            .tag(AllTags.AllItemTags.RODS_ALL_METAL.tag)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> THUNDERITE_WIRE = CreateOreExpansion.REGISTRATE
            .item("thunderite_wire", Item::new)
            .properties(p -> p.rarity(Rarity.UNCOMMON))
            .tag(AllMetalTags.THUNDERITE.wires)
            .tag(AllTags.AllItemTags.WIRES.tag)
            .tag(AllTags.AllItemTags.WIRES_ALL_METAL.tag)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<SwordItem> THUNDERITE_SWORD = CreateOreExpansion.REGISTRATE
            .item("thunderite_sword", p -> new SwordItem(AllTiers.THUNDERITE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    SwordItem.createAttributes(AllTiers.THUNDERITE, 4, -2.4F)
            ))
            .tag(ItemTags.SWORDS)
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

    public static final ItemEntry<PickaxeItem> THUNDERITE_PICKAXE = CreateOreExpansion.REGISTRATE
            .item("thunderite_pickaxe", p -> new PickaxeItem(AllTiers.THUNDERITE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    PickaxeItem.createAttributes(AllTiers.THUNDERITE, 1.5F, -2.3F)
            ))
            .tag(ItemTags.PICKAXES)
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

    public static final ItemEntry<ShovelItem> THUNDERITE_SHOVEL = CreateOreExpansion.REGISTRATE
            .item("thunderite_shovel", p -> new ShovelItem(AllTiers.THUNDERITE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    ShovelItem.createAttributes(AllTiers.THUNDERITE, 1.5F, -2.8F)
            ))
            .tag(ItemTags.SHOVELS)
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

    public static final ItemEntry<AxeItem> THUNDERITE_AXE = CreateOreExpansion.REGISTRATE
            .item("thunderite_axe", p -> new AxeItem(AllTiers.THUNDERITE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    AxeItem.createAttributes(AllTiers.THUNDERITE, 6.5F, -3.2F)
            ))
            .tag(ItemTags.AXES)
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

    public static final ItemEntry<HoeItem> THUNDERITE_HOE = CreateOreExpansion.REGISTRATE
            .item("thunderite_hoe", p -> new HoeItem(AllTiers.THUNDERITE, p))
            .model((ctx, provider) ->
                    provider.handheld(ctx::get))
            .properties(p -> p
                    .rarity(Rarity.UNCOMMON)
                    .attributes(
                    HoeItem.createAttributes(AllTiers.THUNDERITE, -1.5F, 0.0F)
            ))
            .tag(ItemTags.HOES)
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

    public static final ItemEntry<ThunderiteStressMedallionItem> THUNDERITE_STRESS_MEDALLION = CreateOreExpansion.REGISTRATE
            .item("thunderite_stress_medallion", ThunderiteStressMedallionItem::new)
            .properties(p -> p
                    .component(AllDataComponents.ENERGY, 10000)
                    .component(AllDataComponents.MAX_ENERGY, 10000)
                    .component(AllDataComponents.ENERGY_COLOR, ToolEnergyColorConfig.THUNDERITE.light.getRGB())
                    .component(AllDataComponents.ENERGY_COLOR_DARK, ToolEnergyColorConfig.THUNDERITE.dark.getRGB())
                    .component(AllDataComponents.MEDALLION_MODE, false)
                    .component(AllDataComponents.BIND_COLOR, ToolEnergyColorConfig.THUNDERITE.light.getRGB()))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<JadeTopazBowItem> JADE_TOPAZ_BOW = CreateOreExpansion.REGISTRATE
            .item("jade_topaz_bow", JadeTopazBowItem::new)
            .model((ctx, provider) -> {})
            .register();

    public static <T extends Item, P> NonNullFunction<ItemBuilder<T, P>, SkillItemBuilder<T, P>> skillItem() {
        return SkillItemBuilder::new;
    }

    public static class SkillItemBuilder<T extends Item, P> implements
            Builder<Item, T, ItemBuilder<T, P>, SkillItemBuilder<T, P>> {
        public List<DataSkill> skillData = new ArrayList<>();

        private final ItemBuilder<T, P> builder;
        /** 武器技能发光颜色（延迟到register 时注册到SkillOutlineColors）*/
        private SkillOutlineColors.SkillColor outlineColor;
        /** 武器技能冷却时长（tick，延迟到 register 时注册到 SkillCooldowns；1 表示未设置）*/
        private int cooldownTicks = -1;

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
            // 颜色已由skillColor() 写入技???NBT（渲染端直接读取），此处只需注册冷却
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
            return builder;
        }

        public static class EnergyItemBuilder<T extends Item, P> {
            int defaultEnergy = 100;
            int maxEnergy = 1000;
            ToolEnergyColorConfig config = ToolEnergyColorConfig.DEFAULT;

            private final SkillItemBuilder<T, P> builder;

            public EnergyItemBuilder(SkillItemBuilder<T, P> builder) {
                this.builder = builder;
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

    public static void register() {}
}
