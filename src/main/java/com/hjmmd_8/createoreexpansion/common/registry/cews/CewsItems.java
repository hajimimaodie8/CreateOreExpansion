package com.hjmmd_8.createoreexpansion.common.registry.cews;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
// 系列特性登记：物品注册链上写 .tag(AllModItemTags.STELLARSTONE_ITEMS) / .tag(AllModItemTags.THUNDERITE_ITEMS)
// （Java 没有扩展方法，链上只能写成 .tag(...)；函数版见 SeriesTraits#addStellarstoneTraits(ItemBuilder)。
//  判定口径 = 物品标签 ∪ 系列方块标签 ∪ 注册名约定，全部收口在 SeriesTraits#isStellarstone/isThunderite）
import com.hjmmd_8.createoreexpansion.foundation.util.SkillOutlineColors;
import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipeTools;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.JadeStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.NetheriteStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.SapphireStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.StellarstoneStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.ThunderiteStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.TopazStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergyColorConfig;
import com.hjmmd_8.createoreexpansion.content.grinding.item.GrindingWheelItem;
import com.hjmmd_8.createoreexpansion.content.wave.gauge.WaveQueryGaugeItem;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.DataSkill;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.SkillsComponent;
import com.hjmmd_8.createoreexpansion.foundation.item.skill.config.SkillConfig;
import com.hjmmd_8.createoreexpansion.common.*;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static com.simibubi.create.AllTags.AllItemTags.CREATE_INGOTS;
import static com.simibubi.create.AllTags.AllItemTags.CRUSHED_RAW_MATERIALS;

/**
 * <b>CEWS（能量波阵学）</b>物品注册：能量构件 / 未完成的能量构件（{@code energy_mechanism}，
 * 按方案文档 §3.2 建议①归本层）与波情查询仪。<br>
 * 本次拆分是<b>纯搬运</b>，注册 id 与链式调用一字未改，旧类 {@code AllItems} 保留同类型别名。
 */
public final class CewsItems {

    public static final ItemEntry<Item> ENERGY_MECHANISM = CreateOreExpansion.REGISTRATE
            .item("energy_mechanism", Item::new)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> INCOMPLETE_ENERGY_MECHANISM = CreateOreExpansion.REGISTRATE
            .item("incomplete_energy_mechanism", Item::new)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .removeTab(AllCreativeModeTabs.BASE_TAB.key())
            .register();

    // ========== 波情查询仪（手持静态贴图；右键查询最近能量波的波情，查询期间播动画贴图） ==========

    public static final ItemEntry<WaveQueryGaugeItem> WAVE_QUERY_GAUGE = CreateOreExpansion.REGISTRATE
            .item("wave_query_gauge", WaveQueryGaugeItem::new)
            // 模型走手写 JSON（models/item/wave_query_gauge.json：默认静态 idle + overrides 挂
            // createoreexpansion:scanning 指向动画模型），故这里给空的 datagen 模型提供者，
            // 避免 datagen 另生成一份同名模型（做法与 jade_topaz_bow 一致）
            .model((ctx, provider) -> {})
            .register();

	private CewsItems() {
	}
}
