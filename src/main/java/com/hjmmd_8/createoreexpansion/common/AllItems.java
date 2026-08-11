package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.client.tool.SkillRendererConfig;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergyColorConfig;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.ItemSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;
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
    // 这里调用了MoreCreateOre类的static field(字段) REGSITRATE。
    public static final ItemEntry<Item> JADE_INGOT = CreateOreExpansion.REGISTRATE
            // 调用方法
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
            // 添加 剑 的标签，不然没有横扫效果
            .tag(ItemTags.SWORDS)
            .transform(skillItem())
            .addSkills(AllSkills.SKIN)
            .skillColor(SkillRendererConfig.JADE_GREEN)
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
            .addSkills(AllSkills.SHATTER)
            .skillColor(SkillRendererConfig.JADE_GREEN)
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
            .addSkills(AllSkills.FELL)
            .skillColor(SkillRendererConfig.JADE_GREEN)
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
            .addSkills(AllSkills.CHANNEL)
            .skillColor(SkillRendererConfig.JADE_GREEN)
            .build()
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
            .defaultEnergy(450)
            .maxEnergy(1000)
            .color(ToolEnergyColorConfig.TOPAZ)
            .build()
            .addSkills(AllSkills.GREAT_SKIN)
            .skillColor(SkillRendererConfig.TOPAZ_GOLD)
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
            .defaultEnergy(450)
            .maxEnergy(1000)
            .color(ToolEnergyColorConfig.TOPAZ)
            .build()
            .addSkills(AllSkills.GREAT_SHATTER)
            .skillColor(SkillRendererConfig.TOPAZ_GOLD)
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
            .defaultEnergy(450)
            .maxEnergy(1000)
            .color(ToolEnergyColorConfig.TOPAZ)
            .build()
            .addSkills(AllSkills.GREAT_CHANNEL)
            .skillColor(SkillRendererConfig.TOPAZ_GOLD)
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
            .defaultEnergy(450)
            .maxEnergy(1000)
            .color(ToolEnergyColorConfig.TOPAZ)
            .build()
            .addSkills(AllSkills.GREAT_FELL)
            .skillColor(SkillRendererConfig.TOPAZ_GOLD)
            .build()
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
            .defaultEnergy(10000)
            .maxEnergy(25000)
            .color(ToolEnergyColorConfig.SAPPHIRE)
            .build()
            .addSkills(AllSkills.GRAND_SKIN)
            .skillColor(SkillRendererConfig.SAPPHIRE_BLUE)
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
            .defaultEnergy(10000)
            .maxEnergy(25000)
            .color(ToolEnergyColorConfig.SAPPHIRE)
            .build()
            .addSkills(AllSkills.GRAND_SHATTER)
            .skillColor(SkillRendererConfig.SAPPHIRE_BLUE)
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
            .defaultEnergy(10000)
            .maxEnergy(25000)
            .color(ToolEnergyColorConfig.SAPPHIRE)
            .build()
            .addSkills(AllSkills.GRADE)
            .skillColor(SkillRendererConfig.SAPPHIRE_BLUE)
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
            .defaultEnergy(10000)
            .maxEnergy(25000)
            .color(ToolEnergyColorConfig.SAPPHIRE)
            .build()
            .addSkills(AllSkills.GRAND_FELL)
            .skillColor(SkillRendererConfig.SAPPHIRE_BLUE)
            .build()
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

        public SkillItemBuilder(ItemBuilder<T, P> builder) {
            this.builder = builder;
        }

        public SkillItemBuilder<T, P> addSkills(ItemSkill... skills) {
            skillData.addAll(Arrays.stream(skills)
                    .map(DataSkill::fromSkill)
                    .toList());
            return this;
        }

        public SkillItemBuilder<T, P> addSkills(DataSkill... skills) {
            skillData.addAll(Arrays.stream(skills)
                    .toList());
            return this;
        }

        public SkillItemBuilder<T, P> skillColor(float[] colorArray) {
            skillData.forEach(data -> {
                // 获取或创建 NBT
                CompoundTag nbt = data.getOrCreateNbt();

                // 创建颜色标签
                CompoundTag colorTag = new CompoundTag();
                colorTag.putFloat("r", colorArray[0]);
                colorTag.putFloat("g", colorArray[1]);
                colorTag.putFloat("b", colorArray[2]);

                // 保存到 NBT
                nbt.put("OutlineColor", colorTag);
                data.nbt = nbt;
            });
            return this;
        }

        public EnergyItemBuilder<T, P> addEnergy() {
            return new EnergyItemBuilder<>(this);
        }

        @Override
        public @NotNull RegistryEntry<Item, T> register() {
            return builder.register();
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
