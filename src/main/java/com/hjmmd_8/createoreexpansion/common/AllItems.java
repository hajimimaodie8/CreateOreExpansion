package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergyConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.Tags;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.simibubi.create.AllTags.AllItemTags.CREATE_INGOTS;
import static com.simibubi.create.AllTags.AllItemTags.CRUSHED_RAW_MATERIALS;

public final class AllItems {
    // ===== 机械动力方式注册 =====

    // 这个变量名可以随便写，好理解就行，一般是item id的大写
    // 这里调用了MoreCreateOre类的static field(字段) REGSITRATE。
    public static final ItemEntry<Item> JADE_INGOT = CreateOreExpansion.REGISTRATE
            // 调用方法
            .item("jade_ingot", Item::new)
            .tag(CREATE_INGOTS.tag)
            .tag(Tags.Items.INGOTS)
            .tag(AllMetal.JADE.ingots)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> RAW_JADE = CreateOreExpansion.REGISTRATE
            .item("raw_jade", Item::new)
            .tag(Tags.Items.RAW_MATERIALS)
            .tag(AllMetal.JADE.rawOres)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> JADE_NUGGET = CreateOreExpansion.REGISTRATE
            .item("jade_nugget", Item::new)
            .tag(Tags.Items.NUGGETS)
            .tag(AllMetal.JADE.nuggets)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> CRUSHED_JADE_ORE = CreateOreExpansion.REGISTRATE
            .item("crushed_jade_ore", Item::new)
            .tag(CRUSHED_RAW_MATERIALS.tag)
            .tag(AllMetal.JADE.crushedRawOres)
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
            .tag(AllMetal.JADE.sheets)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> JADE_ROD = CreateOreExpansion.REGISTRATE
            .item("jade_rod", Item::new)
            .tag(AllMetal.JADE.rods)
            .tag(AllTags.AllItemTags.RODS.tag)
            .tag(AllTags.AllItemTags.RODS_ALL_METAL.tag)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> JADE_WIRE = CreateOreExpansion.REGISTRATE
            .item("jade_wire", Item::new)
            .tag(AllMetal.JADE.wires)
            .tag(AllTags.AllItemTags.WIRES.tag)
            .tag(AllTags.AllItemTags.WIRES_ALL_METAL.tag)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<SwordItem> JADE_SWORD = CreateOreExpansion.REGISTRATE
            .item("jade_sword", p -> new SwordItem(AllTiers.JADE, p))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .properties(p -> p.attributes(
                    SwordItem.createAttributes(AllTiers.JADE, 4, -2.4F)
            ))
            // 添加 剑 的标签，不然没有横扫效果
            .tag(ItemTags.SWORDS)
            .register();

    public static final ItemEntry<PickaxeItem> JADE_PICKAXE = CreateOreExpansion.REGISTRATE
            .item("jade_pickaxe", p -> new PickaxeItem(AllTiers.JADE, p))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .properties(p -> p.attributes(
                    PickaxeItem.createAttributes(AllTiers.JADE, 1, -2.8F)
            ))
            .tag(ItemTags.PICKAXES)
            .transform(addSkills(AllSkills.JADE_PICKAXE_AOE))
            .register();

    public static final ItemEntry<AxeItem> JADE_AXE = CreateOreExpansion.REGISTRATE
            .item("jade_axe", p -> new AxeItem(AllTiers.JADE, p))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .properties(p -> p.attributes(
                    AxeItem.createAttributes(AllTiers.JADE, 6, -3.0F)
            ))
            .tag(ItemTags.AXES)
            .transform(addSkills(AllSkills.JADE_AXE_AOE))
            .register();

    public static final ItemEntry<ShovelItem> JADE_SHOVEL = CreateOreExpansion.REGISTRATE
            .item("jade_shovel", p -> new ShovelItem(AllTiers.JADE, p))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .properties(p -> p.attributes(
                    ShovelItem.createAttributes(AllTiers.JADE, 1.5F, -3.0F)
            ))
            .tag(ItemTags.SHOVELS)
            .transform(addSkills(AllSkills.JADE_SHOVEL_AOE))
            .register();

    public static final ItemEntry<Item> TOPAZ_INGOT = CreateOreExpansion.REGISTRATE
            .item("topaz_ingot", Item::new)
            .tag(CREATE_INGOTS.tag)
            .tag(Tags.Items.INGOTS)
            .tag(AllMetal.TOPAZ.ingots)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> RAW_TOPAZ = CreateOreExpansion.REGISTRATE
            .item("raw_topaz", Item::new)
            .tag(Tags.Items.RAW_MATERIALS)
            .tag(AllMetal.TOPAZ.rawOres)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> TOPAZ_NUGGET = CreateOreExpansion.REGISTRATE
            .item("topaz_nugget", Item::new)
            .tag(Tags.Items.NUGGETS)
            .tag(AllMetal.TOPAZ.nuggets)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> CRUSHED_TOPAZ_ORE = CreateOreExpansion.REGISTRATE
            .item("crushed_topaz_ore", Item::new)
            .tag(CRUSHED_RAW_MATERIALS.tag)
            .tag(AllMetal.TOPAZ.crushedRawOres)
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
            .tag(AllMetal.TOPAZ.sheets)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> TOPAZ_ROD = CreateOreExpansion.REGISTRATE
            .item("topaz_rod", Item::new)
            .tag(AllMetal.TOPAZ.rods)
            .tag(AllTags.AllItemTags.RODS.tag)
            .tag(AllTags.AllItemTags.RODS_ALL_METAL.tag)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> TOPAZ_WIRE = CreateOreExpansion.REGISTRATE
            .item("topaz_wire", Item::new)
            .tag(AllMetal.TOPAZ.wires)
            .tag(AllTags.AllItemTags.WIRES.tag)
            .tag(AllTags.AllItemTags.WIRES_ALL_METAL.tag)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<SwordItem> TOPAZ_SWORD = CreateOreExpansion.REGISTRATE
            .item("topaz_sword", p -> new SwordItem(AllTiers.TOPAZ, p))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .properties(p -> p.attributes(
                    SwordItem.createAttributes(AllTiers.TOPAZ, 4, -2.4F)
            ))
            .tag(ItemTags.SWORDS)
            .transform(addEnergy(200, 1000, ToolEnergyConfig.TOPAZ_COLOR))
            .register();

    public static final ItemEntry<PickaxeItem> TOPAZ_PICKAXE = CreateOreExpansion.REGISTRATE
            .item("topaz_pickaxe", p -> new PickaxeItem (AllTiers.TOPAZ, p))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .properties(p -> p.attributes(
                    PickaxeItem.createAttributes(AllTiers.TOPAZ, 1.5F, -2.3F)
            ))
            .tag(ItemTags.PICKAXES)
            .transform(addSkills(AllSkills.TOPAZ_PICKAXE_AOE))
            .transform(addEnergy(200, 1000, ToolEnergyConfig.TOPAZ_COLOR))
            .register();

    public static final ItemEntry<ShovelItem> TOPAZ_SHOVEL= CreateOreExpansion.REGISTRATE
            .item("topaz_shovel", p -> new ShovelItem (AllTiers.TOPAZ, p))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .properties(p -> p.attributes(
                    ShovelItem.createAttributes(AllTiers.TOPAZ, 1.5F, -2.8F)
            ))
            .tag(ItemTags.SHOVELS)
            .transform(addSkills(AllSkills.TOPAZ_SHOVEL_AOE))
            .transform(addEnergy(200, 1000, ToolEnergyConfig.TOPAZ_COLOR))
            .register();

    public static final ItemEntry<AxeItem> TOPAZ_AXE = CreateOreExpansion.REGISTRATE
            .item("topaz_axe", p -> new AxeItem (AllTiers.TOPAZ, p))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .properties(p -> p.attributes(
                    AxeItem.createAttributes(AllTiers.TOPAZ, 6.5F, -3.2F)
            ))
            .tag(ItemTags.AXES)
            .transform(addSkills(AllSkills.TOPAZ_AXE_AOE))
            .transform(addEnergy(200, 1000, ToolEnergyConfig.TOPAZ_COLOR))
            .register();

    public static final ItemEntry<Item> SAPPHIRE_INGOT = CreateOreExpansion.REGISTRATE
            .item("sapphire_ingot", Item::new)
            .tag(CREATE_INGOTS.tag)
            .tag(Tags.Items.INGOTS)
            .tag(AllMetal.SAPPHIRE.ingots)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> RAW_SAPPHIRE = CreateOreExpansion.REGISTRATE
            .item("raw_sapphire", Item::new)
            .tag(Tags.Items.RAW_MATERIALS)
            .tag(AllMetal.SAPPHIRE.rawOres)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> SAPPHIRE_NUGGET = CreateOreExpansion.REGISTRATE
            .item("sapphire_nugget", Item::new)
            .tag(Tags.Items.NUGGETS)
            .tag(AllMetal.SAPPHIRE.nuggets)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> CRUSHED_SAPPHIRE_ORE = CreateOreExpansion.REGISTRATE    
            .item("crushed_sapphire_ore", Item::new)
            .tag(CRUSHED_RAW_MATERIALS.tag)
            .tag(AllMetal.SAPPHIRE.crushedRawOres)
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
            .tag(AllMetal.SAPPHIRE.sheets)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> SAPPHIRE_ROD = CreateOreExpansion.REGISTRATE
            .item("sapphire_rod", Item::new)
            .tag(AllMetal.SAPPHIRE.rods)
            .tag(AllTags.AllItemTags.RODS.tag)
            .tag(AllTags.AllItemTags.RODS_ALL_METAL.tag)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> SAPPHIRE_WIRE = CreateOreExpansion.REGISTRATE
            .item("sapphire_wire", Item::new)
            .tag(AllMetal.SAPPHIRE.wires)
            .tag(AllTags.AllItemTags.WIRES.tag)
            .tag(AllTags.AllItemTags.WIRES_ALL_METAL.tag)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<SwordItem> SAPPHIRE_SWORD = CreateOreExpansion.REGISTRATE
            .item("sapphire_sword", p -> new SwordItem(AllTiers.SAPPHIRE, p))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .properties(p -> p.attributes(
                    SwordItem.createAttributes(AllTiers.SAPPHIRE, 5, -2.4F)
            ))
            .tag(ItemTags.SWORDS)
            .transform(addEnergy(1000, 25000, ToolEnergyConfig.SAPPHIRE_COLOR))
            .register();

    public static final ItemEntry<PickaxeItem> SAPPHIRE_PICKAXE = CreateOreExpansion.REGISTRATE
            .item("sapphire_pickaxe", p -> new PickaxeItem (AllTiers.SAPPHIRE, p))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .properties(p -> p.attributes(
                    PickaxeItem.createAttributes(AllTiers.SAPPHIRE, 2, -2.5F)
            ))
            .tag(ItemTags.PICKAXES)
            .transform(addSkills(AllSkills.SAPPHIRE_PICKAXE_AOE))
            .transform(addEnergy(1000, 25000, ToolEnergyConfig.SAPPHIRE_COLOR))
            .register();

    public static final ItemEntry<ShovelItem> SAPPHIRE_SHOVEL= CreateOreExpansion.REGISTRATE
            .item("sapphire_shovel", p -> new ShovelItem (AllTiers.SAPPHIRE, p))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .properties(p -> p.attributes(
                    ShovelItem.createAttributes(AllTiers.SAPPHIRE, 1.5F, -2.8F)
            ))
            .tag(ItemTags.SHOVELS)
            .transform(addSkills(AllSkills.SAPPHIRE_SHOVEL_AOE))
            .transform(addEnergy(1000, 25000, ToolEnergyConfig.SAPPHIRE_COLOR))
            .register();

    public static final ItemEntry<AxeItem> SAPPHIRE_AXE = CreateOreExpansion.REGISTRATE
            .item("sapphire_axe", p -> new AxeItem (AllTiers.SAPPHIRE, p))
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .properties(p -> p.attributes(
                    AxeItem.createAttributes(AllTiers.SAPPHIRE, 6.5F, -3.4F)
            ))
            .tag(ItemTags.AXES)
            .transform(addSkills(AllSkills.SAPPHIRE_AXE_AOE))
            .transform(addEnergy(1000, 25000, ToolEnergyConfig.SAPPHIRE_COLOR))
            .register();

    public static final ItemEntry<JadeTopazBowItem> JADE_TOPAZ_BOW = CreateOreExpansion.REGISTRATE
            .item("jade_topaz_bow", JadeTopazBowItem::new)
            .model((ctx, provider) -> {})
            .register();

    public static <T extends Item, P> ItemBuilder<T, P> addSkills(ItemBuilder<T, P> builder, ItemSkill... skills) {
        if (skills == null) return builder;
        return builder.properties(p -> p.component(AllDataComponents.SKILLS,
                Arrays.stream(skills).map(AllSkills::getId)
                        .filter(Objects::nonNull).map(ResourceLocation::toString).collect(Collectors.toList())));
    }

    public static <T extends Item, P> NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> addSkills(ItemSkill... skills) {
        return builder -> addSkills(builder, skills);
    }

    public static <T extends Item, P> ItemBuilder<T, P> addEnergy(ItemBuilder<T, P> builder,
                                                                  int defaultEnergy, int maxEnergy, int energyColor) {
        return builder.properties(p -> p
                .component(AllDataComponents.ENERGY, defaultEnergy)
                .component(AllDataComponents.MAX_ENERGY, maxEnergy)
                .component(AllDataComponents.ENERGY_COLOR, energyColor));
    }

    public static <T extends Item, P> ItemBuilder<T, P> addEnergy(ItemBuilder<T, P> builder,
                                                                  int defaultEnergy, int maxEnergy) {
        return addEnergy(builder, defaultEnergy, maxEnergy, 0x5555FF);
    }

    public static <T extends Item, P> NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> addEnergy(int defaultEnergy, int maxEnergy) {
        return builder -> addEnergy(builder, defaultEnergy, maxEnergy);
    }

    public static <T extends Item, P> NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> addEnergy(int defaultEnergy, int maxEnergy, int energyColor) {
        return builder -> addEnergy(builder, defaultEnergy, maxEnergy, energyColor);
    }

    public static void register() {}
}
