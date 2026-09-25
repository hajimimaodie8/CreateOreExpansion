package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.common.registry.cews.CewsItems;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationItems;
import com.hjmmd_8.createoreexpansion.content.equipment.item.JadeTopazBowItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.JadeStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.NetheriteStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.SapphireStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.StellarstoneStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.ThunderiteStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.TopazStressMedallionItem;
import com.hjmmd_8.createoreexpansion.content.grinding.item.GrindingWheelItem;
import com.hjmmd_8.createoreexpansion.content.wave.gauge.WaveQueryGaugeItem;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.world.item.*;

/**
 * 物品注册的<b>兼容外观（facade）</b>——P1「三层分区」改造后的落点。
 *
 * <p>真正的注册声明已按未来模块边界搬进各自的持有类：
 * 矿物拓展（COE）见 {@link CoeItems}、能量波阵学（CEWS）见 {@link CewsItems}、
 * 机械嬗化学（TRANS）见 {@link TransmutationItems}。
 * 本类只保留<b>同名同类型</b>的别名，因此仓库里所有 {@code AllItems.XXX} 引用点
 * 一个字都不用改；注册 id / 贴图路径 / 数值 / 链式调用与拆分前完全一致。</p>
 *
 * <p><b>注册顺序</b>：拆分前三类之间是「方块 → 方块实体 → 物品」的触发顺序
 * （{@code CreateOreExpansion} 依次调用三个 {@code register()}）。这里用静态块
 * 显式按固定顺序把各层持有类拉起来——读取字段本身就是该层的类初始化，
 * 而 Registrate 的注册顺序 = 字段初始化顺序，因此顺序不受别名声明顺序影响。</p>
 *
 * <p>物品侧共用的构建器（{@code grindingWheel} / {@code skillItem()} /
 * {@code SkillItemBuilder} / {@code EnergyItemBuilder}）随物品线一起搬到了
 * {@link CoeItems}；仓库内没有其它引用点，故本类不再转发。</p>
 *
 * <p>归属清单权威出处：{@code markdown_output/CEWS 能量波阵学模块（拆分方案与思索）.md} §3.2。</p>
 */
public final class AllItems {

	static {
		// 物品层：COE → CEWS → TRANS（参数求值 = 触发对应持有类的类初始化）
		triggerLayerInit(CoeItems.JADE_INGOT, CewsItems.ENERGY_MECHANISM, TransmutationItems.TRANSMUTE_MECHANISM);
	}

	/** 只为触发各层持有类的类初始化而存在；真正的动作是调用处读取各层首字段。 */
	private static void triggerLayerInit(Object... firstEntries) {
	}

	// ==================== 矿物拓展（COE） ====================

	public static final ItemEntry<Item> JADE_INGOT = CoeItems.JADE_INGOT;
	public static final ItemEntry<Item> RAW_JADE = CoeItems.RAW_JADE;
	public static final ItemEntry<Item> JADE_NUGGET = CoeItems.JADE_NUGGET;
	public static final ItemEntry<Item> CRUSHED_JADE_ORE = CoeItems.CRUSHED_JADE_ORE;
	public static final ItemEntry<Item> JADE_SMALL_SHARD = CoeItems.JADE_SMALL_SHARD;
	public static final ItemEntry<Item> JADE_BIG_SHARD = CoeItems.JADE_BIG_SHARD;
	public static final ItemEntry<Item> JADE_SHEET = CoeItems.JADE_SHEET;
	public static final ItemEntry<Item> JADE_ROD = CoeItems.JADE_ROD;
	public static final ItemEntry<Item> JADE_WIRE = CoeItems.JADE_WIRE;
	public static final ItemEntry<SwordItem> JADE_SWORD = CoeItems.JADE_SWORD;
	public static final ItemEntry<PickaxeItem> JADE_PICKAXE = CoeItems.JADE_PICKAXE;
	public static final ItemEntry<AxeItem> JADE_AXE = CoeItems.JADE_AXE;
	public static final ItemEntry<ShovelItem> JADE_SHOVEL = CoeItems.JADE_SHOVEL;
	public static final ItemEntry<HoeItem> JADE_HOE = CoeItems.JADE_HOE;
	public static final ItemEntry<JadeStressMedallionItem> JADE_STRESS_MEDALLION = CoeItems.JADE_STRESS_MEDALLION;
	public static final ItemEntry<Item> TOPAZ_INGOT = CoeItems.TOPAZ_INGOT;
	public static final ItemEntry<Item> RAW_TOPAZ = CoeItems.RAW_TOPAZ;
	public static final ItemEntry<Item> TOPAZ_NUGGET = CoeItems.TOPAZ_NUGGET;
	public static final ItemEntry<Item> CRUSHED_TOPAZ_ORE = CoeItems.CRUSHED_TOPAZ_ORE;
	public static final ItemEntry<Item> TOPAZ_SMALL_SHARD = CoeItems.TOPAZ_SMALL_SHARD;
	public static final ItemEntry<Item> TOPAZ_BIG_SHARD = CoeItems.TOPAZ_BIG_SHARD;
	public static final ItemEntry<Item> TOPAZ_SHEET = CoeItems.TOPAZ_SHEET;
	public static final ItemEntry<Item> TOPAZ_ROD = CoeItems.TOPAZ_ROD;
	public static final ItemEntry<Item> TOPAZ_WIRE = CoeItems.TOPAZ_WIRE;
	public static final ItemEntry<SwordItem> TOPAZ_SWORD = CoeItems.TOPAZ_SWORD;
	public static final ItemEntry<PickaxeItem> TOPAZ_PICKAXE = CoeItems.TOPAZ_PICKAXE;
	public static final ItemEntry<ShovelItem> TOPAZ_SHOVEL = CoeItems.TOPAZ_SHOVEL;
	public static final ItemEntry<AxeItem> TOPAZ_AXE = CoeItems.TOPAZ_AXE;
	public static final ItemEntry<HoeItem> TOPAZ_HOE = CoeItems.TOPAZ_HOE;
	public static final ItemEntry<TopazStressMedallionItem> TOPAZ_STRESS_MEDALLION = CoeItems.TOPAZ_STRESS_MEDALLION;
	public static final ItemEntry<Item> SAPPHIRE_INGOT = CoeItems.SAPPHIRE_INGOT;
	public static final ItemEntry<Item> RAW_SAPPHIRE = CoeItems.RAW_SAPPHIRE;
	public static final ItemEntry<Item> SAPPHIRE_NUGGET = CoeItems.SAPPHIRE_NUGGET;
	public static final ItemEntry<Item> CRUSHED_SAPPHIRE_ORE = CoeItems.CRUSHED_SAPPHIRE_ORE;
	public static final ItemEntry<Item> SAPPHIRE_SMALL_SHARD = CoeItems.SAPPHIRE_SMALL_SHARD;
	public static final ItemEntry<Item> SAPPHIRE_BIG_SHARD = CoeItems.SAPPHIRE_BIG_SHARD;
	public static final ItemEntry<Item> SAPPHIRE_SHEET = CoeItems.SAPPHIRE_SHEET;
	public static final ItemEntry<Item> SAPPHIRE_ROD = CoeItems.SAPPHIRE_ROD;
	public static final ItemEntry<Item> SAPPHIRE_WIRE = CoeItems.SAPPHIRE_WIRE;
	public static final ItemEntry<SwordItem> SAPPHIRE_SWORD = CoeItems.SAPPHIRE_SWORD;
	public static final ItemEntry<PickaxeItem> SAPPHIRE_PICKAXE = CoeItems.SAPPHIRE_PICKAXE;
	public static final ItemEntry<ShovelItem> SAPPHIRE_SHOVEL = CoeItems.SAPPHIRE_SHOVEL;
	public static final ItemEntry<AxeItem> SAPPHIRE_AXE = CoeItems.SAPPHIRE_AXE;
	public static final ItemEntry<HoeItem> SAPPHIRE_HOE = CoeItems.SAPPHIRE_HOE;
	public static final ItemEntry<SapphireStressMedallionItem> SAPPHIRE_STRESS_MEDALLION = CoeItems.SAPPHIRE_STRESS_MEDALLION;
	public static final ItemEntry<NetheriteStressMedallionItem> NETHERITE_STRESS_MEDALLION = CoeItems.NETHERITE_STRESS_MEDALLION;
	public static final ItemEntry<Item> RUBY_INGOT = CoeItems.RUBY_INGOT;
	public static final ItemEntry<Item> RUBY_SHEET = CoeItems.RUBY_SHEET;
	public static final ItemEntry<Item> RUBY_ROD = CoeItems.RUBY_ROD;
	public static final ItemEntry<Item> RUBY_WIRE = CoeItems.RUBY_WIRE;
	public static final ItemEntry<Item> STELLARSTONE_INGOT = CoeItems.STELLARSTONE_INGOT;
	public static final ItemEntry<Item> RAW_STELLARSTONE = CoeItems.RAW_STELLARSTONE;
	public static final ItemEntry<Item> STELLARSTONE_NUGGET = CoeItems.STELLARSTONE_NUGGET;
	public static final ItemEntry<Item> CRUSHED_STELLARSTONE_ORE = CoeItems.CRUSHED_STELLARSTONE_ORE;
	public static final ItemEntry<Item> STELLARSTONE_SMALL_SHARD = CoeItems.STELLARSTONE_SMALL_SHARD;
	public static final ItemEntry<Item> STELLARSTONE_BIG_SHARD = CoeItems.STELLARSTONE_BIG_SHARD;
	public static final ItemEntry<Item> STELLARSTONE_SHEET = CoeItems.STELLARSTONE_SHEET;
	public static final ItemEntry<Item> STELLARSTONE_ROD = CoeItems.STELLARSTONE_ROD;
	public static final ItemEntry<Item> STELLARSTONE_WIRE = CoeItems.STELLARSTONE_WIRE;
	public static final ItemEntry<SwordItem> STELLARSTONE_SWORD = CoeItems.STELLARSTONE_SWORD;
	public static final ItemEntry<PickaxeItem> STELLARSTONE_PICKAXE = CoeItems.STELLARSTONE_PICKAXE;
	public static final ItemEntry<ShovelItem> STELLARSTONE_SHOVEL = CoeItems.STELLARSTONE_SHOVEL;
	public static final ItemEntry<AxeItem> STELLARSTONE_AXE = CoeItems.STELLARSTONE_AXE;
	public static final ItemEntry<HoeItem> STELLARSTONE_HOE = CoeItems.STELLARSTONE_HOE;
	public static final ItemEntry<StellarstoneStressMedallionItem> STELLARSTONE_STRESS_MEDALLION = CoeItems.STELLARSTONE_STRESS_MEDALLION;
	public static final ItemEntry<Item> SANCTSTONE_INGOT = CoeItems.SANCTSTONE_INGOT;
	public static final ItemEntry<Item> SANCTSTONE_SHEET = CoeItems.SANCTSTONE_SHEET;
	public static final ItemEntry<Item> SANCTSTONE_ROD = CoeItems.SANCTSTONE_ROD;
	public static final ItemEntry<Item> SANCTSTONE_WIRE = CoeItems.SANCTSTONE_WIRE;
	public static final ItemEntry<Item> THUNDERITE_INGOT = CoeItems.THUNDERITE_INGOT;
	public static final ItemEntry<Item> THUNDERITE_SCRAP = CoeItems.THUNDERITE_SCRAP;
	public static final ItemEntry<Item> THUNDERITE_SHEET = CoeItems.THUNDERITE_SHEET;
	public static final ItemEntry<Item> THUNDERITE_ROD = CoeItems.THUNDERITE_ROD;
	public static final ItemEntry<Item> THUNDERITE_WIRE = CoeItems.THUNDERITE_WIRE;
	public static final ItemEntry<SwordItem> THUNDERITE_SWORD = CoeItems.THUNDERITE_SWORD;
	public static final ItemEntry<PickaxeItem> THUNDERITE_PICKAXE = CoeItems.THUNDERITE_PICKAXE;
	public static final ItemEntry<ShovelItem> THUNDERITE_SHOVEL = CoeItems.THUNDERITE_SHOVEL;
	public static final ItemEntry<AxeItem> THUNDERITE_AXE = CoeItems.THUNDERITE_AXE;
	public static final ItemEntry<HoeItem> THUNDERITE_HOE = CoeItems.THUNDERITE_HOE;
	public static final ItemEntry<ThunderiteStressMedallionItem> THUNDERITE_STRESS_MEDALLION = CoeItems.THUNDERITE_STRESS_MEDALLION;
	public static final ItemEntry<Item> LUCKY_DUST = CoeItems.LUCKY_DUST;
	public static final ItemEntry<JadeTopazBowItem> JADE_TOPAZ_BOW = CoeItems.JADE_TOPAZ_BOW;
	public static final ItemEntry<GrindingWheelItem> IRON_GRINDING_WHEEL = CoeItems.IRON_GRINDING_WHEEL;
	public static final ItemEntry<GrindingWheelItem> GOLD_GRINDING_WHEEL = CoeItems.GOLD_GRINDING_WHEEL;
	public static final ItemEntry<GrindingWheelItem> BRASS_GRINDING_WHEEL = CoeItems.BRASS_GRINDING_WHEEL;
	public static final ItemEntry<GrindingWheelItem> ZINC_GRINDING_WHEEL = CoeItems.ZINC_GRINDING_WHEEL;
	public static final ItemEntry<GrindingWheelItem> JADE_GRINDING_WHEEL = CoeItems.JADE_GRINDING_WHEEL;
	public static final ItemEntry<GrindingWheelItem> DIAMOND_GRINDING_WHEEL = CoeItems.DIAMOND_GRINDING_WHEEL;
	public static final ItemEntry<GrindingWheelItem> TOPAZ_GRINDING_WHEEL = CoeItems.TOPAZ_GRINDING_WHEEL;
	public static final ItemEntry<GrindingWheelItem> SAPPHIRE_GRINDING_WHEEL = CoeItems.SAPPHIRE_GRINDING_WHEEL;
	public static final ItemEntry<GrindingWheelItem> STELLARSTONE_GRINDING_WHEEL = CoeItems.STELLARSTONE_GRINDING_WHEEL;
	public static final ItemEntry<GrindingWheelItem> NETHERITE_GRINDING_WHEEL = CoeItems.NETHERITE_GRINDING_WHEEL;

	// ==================== 能量波阵学（CEWS） ====================

	public static final ItemEntry<Item> ENERGY_MECHANISM = CewsItems.ENERGY_MECHANISM;
	public static final ItemEntry<Item> INCOMPLETE_ENERGY_MECHANISM = CewsItems.INCOMPLETE_ENERGY_MECHANISM;
	public static final ItemEntry<WaveQueryGaugeItem> WAVE_QUERY_GAUGE = CewsItems.WAVE_QUERY_GAUGE;

	// ==================== 机械嬗化学（TRANS） ====================

	public static final ItemEntry<Item> TRANSMUTE_MECHANISM = TransmutationItems.TRANSMUTE_MECHANISM;
	public static final ItemEntry<Item> INCOMPLETE_TRANSMUTE_MECHANISM = TransmutationItems.INCOMPLETE_TRANSMUTE_MECHANISM;

	/** 保留旧调用点：Registrate 的注册由字段初始化完成，本方法本身不做事。 */
	public static void register() {
	}
}
