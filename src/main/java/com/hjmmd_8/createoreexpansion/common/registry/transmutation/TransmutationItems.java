package com.hjmmd_8.createoreexpansion.common.registry.transmutation;

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
 * <b>TRANS（机械嬗化学）</b>物品注册：嬗化构件 / 未完成的嬗化构件（{@code transmute_mechanism}）。
 * 嬗化相关的配方类型、效果与流体不在本类（见 {@code AllTransmutingRecipe} / {@code AllModEffects} / {@code AllFluids}）。<br>
 * 本次拆分是<b>纯搬运</b>，注册 id 与链式调用一字未改，P2a 已删除 {@code AllItems} 外观类，引用直接指向本层。
 */
public final class TransmutationItems {

    public static final ItemEntry<Item> TRANSMUTE_MECHANISM = CreateOreExpansion.REGISTRATE
            .item("transmute_mechanism", Item::new)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .register();

    public static final ItemEntry<Item> INCOMPLETE_TRANSMUTE_MECHANISM = CreateOreExpansion.REGISTRATE
            .item("incomplete_transmute_mechanism", Item::new)
            .model((ctx, provider) ->
                    provider.basicItem(ctx.get()))
            .removeTab(AllCreativeModeTabs.BASE_TAB.key())
            .register();

/** 触发本层注册类的类初始化：Registrate 的注册动作就是字段初始化，因此方法体为空。 */
	public static void register() {
	}

	private TransmutationItems() {
	}
}
