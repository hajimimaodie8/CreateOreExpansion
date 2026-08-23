package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 战利品注入集中登记（维护入口，不生成、不改数据文件）。
 *
 * <p>全部战利品注入均用<b>新增独立 pool</b>（NeoForge {@code neoforge:add_table} + 
 * {@code LootTableIdCondition} 限定目标表），数据 JSON 位于
 * {@code data/createoreexpansion/loot_modifiers/}（modifier）与
 * {@code data/createoreexpansion/loot_table/inject/}（子表）。</p>
 *
 * <p>本类只做<b>文档化登记</b>：把"哪个 modifier 注入哪些目标表、产出什么"集中描述，
 * 改权重/数量/概率只需改对应子表 JSON（路径见 {@link #table}），新增目标位置
 * 只需新增 modifier JSON 并注册到
 * {@code data/neoforge/loot_modifiers/global_loot_modifiers.json}。</p>
 *
 * <p>开闭原则：对扩展开放——新增箱子/生物目标 = 新增 modifier JSON + 注册一行；
 * 对修改封闭——既有注入不破坏原版战利品池。</p>
 */
public final class AllLootTables {

	private AllLootTables() {
	}

	/**
	 * 一次战利品注入描述：modifier 文件名（相对 loot_modifiers/，不含 .json）→
	 * 注入的子表（相对 loot_table/inject/）+ 目标位置说明。
	 */
	public record LootInjection(String modifier, String table, String targets) {

		/** modifier 完整 id（global_loot_modifiers.json 注册用） */
		public ResourceLocation modifierId() {
			return CreateOreExpansion.modLoc(modifier);
		}

		/** 子表完整资源 id */
		public ResourceLocation tableId() {
			return CreateOreExpansion.modLoc("inject/" + table);
		}
	}

	// ==================== 硬通货：翡翠锭（地位≈钻石，主世界/下界/末地） ====================

	/** 翡翠锭：主世界宝箱（地牢/废弃矿井/铁匠/神殿）与埋藏宝藏 */
	public static final List<LootInjection> JADE_INGOT = List.of(
		new LootInjection("chests/jade_ingot_dungeon", "chests/jade_ingot", "简单地牢"),
		new LootInjection("chests/jade_ingot_mineshaft", "chests/jade_ingot", "废弃矿井"),
		new LootInjection("chests/jade_ingot_village", "chests/jade_ingot", "村庄铁匠（武器匠/工具匠）"),
		new LootInjection("chests/jade_ingot_temple", "chests/jade_ingot", "神殿（沙漠/丛林）"),
		new LootInjection("chests/jade_ingot_buried", "buried_treasure/jade_ingot", "埋藏的宝藏（更多）"),
		new LootInjection("chests/jade_ingot_nether_end", "chests/jade_ingot", "下界要塞/堡垒宝藏室/末地城")
	);

	// ==================== 硬通货：黄玉锭（≈钻石但略少见） ====================

	/** 黄玉锭：同翡翠锭目标，权重略低 */
	public static final List<LootInjection> TOPAZ_INGOT = List.of(
		new LootInjection("chests/topaz_ingot_dungeon", "chests/topaz_ingot", "简单地牢"),
		new LootInjection("chests/topaz_ingot_mineshaft", "chests/topaz_ingot", "废弃矿井"),
		new LootInjection("chests/topaz_ingot_village", "chests/topaz_ingot", "村庄铁匠（武器匠/工具匠）"),
		new LootInjection("chests/topaz_ingot_temple", "chests/topaz_ingot", "神殿（沙漠/丛林）"),
		new LootInjection("chests/topaz_ingot_buried", "buried_treasure/topaz_ingot", "埋藏的宝藏"),
		new LootInjection("chests/topaz_ingot_nether_end", "chests/topaz_ingot", "下界要塞/堡垒宝藏室/末地城")
	);

	// ==================== 蓝宝石锭（下界稀有，末地城/下界要塞稍多） ====================

	/** 蓝宝石锭：堡垒宝藏室/普通房间/下界要塞/埋藏宝藏/末地城 */
	public static final List<LootInjection> SAPPHIRE_INGOT = List.of(
		new LootInjection("chests/sapphire_ingot_bastion_treasure", "chests/bastion_treasure_sapphire", "堡垒宝藏室（3-6）"),
		new LootInjection("chests/sapphire_ingot_bastion_rooms", "chests/bastion_rooms_sapphire", "堡垒普通房间（极低）"),
		new LootInjection("chests/sapphire_ingot_nether_bridge", "chests/nether_bridge_sapphire", "下界要塞（稍多）"),
		new LootInjection("chests/sapphire_ingot_buried", "buried_treasure/sapphire_ingot", "埋藏宝藏（欧皇）"),
		new LootInjection("chests/sapphire_ingot_end_city", "chests/end_city_sapphire", "末地城（比星辉石稍多）")
	);

	// ==================== 星辉石锭（末地，主要靠结构宝箱） ====================

	/** 星辉石锭：末地城塔楼（主产出）；末影船鞘翅房箱子专属子表由 mixin 指定 */
	public static final List<LootInjection> STELLARSTONE_INGOT = List.of(
		new LootInjection("chests/stellarstone_ingot_end_city", "chests/end_city_stellarstone", "末地城塔楼")
	);

	// ==================== 工具注入（翡翠/黄玉为主，蓝宝石极少） ====================

	/** 翡翠/黄玉工具：末地城/林地府邸/猪灵堡垒宝藏室（35% 概率触发） */
	public static final List<LootInjection> JADE_TOPAZ_TOOLS = List.of(
		new LootInjection("chests/jade_topaz_tools_end_city", "chests/jade_topaz_tools", "末地城"),
		new LootInjection("chests/jade_topaz_tools_mansion", "chests/jade_topaz_tools", "林地府邸"),
		new LootInjection("chests/jade_topaz_tools_bastion", "chests/jade_topaz_tools", "猪灵堡垒宝藏室")
	);

	/** 蓝宝石工具：仅末地城（35% 概率触发，偶尔出现） */
	public static final List<LootInjection> SAPPHIRE_TOOLS = List.of(
		new LootInjection("chests/sapphire_tools_end_city", "chests/sapphire_tools", "末地城（偶尔）")
	);

	/** 远古之城：本模组矿物工具 + 附魔书（概率稍大） */
	public static final List<LootInjection> ANCIENT_CITY = List.of(
		new LootInjection("chests/ancient_city_treasures", "chests/ancient_city_treasures", "远古之城")
	);

	// ==================== 海底神殿（守卫者掉落蓝宝石，远古守卫者概率更高） ====================

	/** 守卫者/远古守卫者掉落蓝宝石锭 */
	public static final List<LootInjection> GUARDIAN = List.of(
		new LootInjection("entities/guardian_sapphire", "entities/guardian_sapphire", "守卫者（约 29%）"),
		new LootInjection("entities/elder_guardian_sapphire", "entities/elder_guardian_sapphire", "远古守卫者（约 57%）")
	);

	// ==================== 猪灵以物易物 ====================

	/** 猪灵以物易物：蓝宝石锭/蓝宝石锄（极小概率，1.16 锄头彩蛋） */
	public static final List<LootInjection> PIGLIN_BARTER = List.of(
		new LootInjection("gameplay/sapphire_barter", "gameplay/sapphire_barter", "猪灵以物易物")
	);

	/** 全部注入（统计用） */
	public static final List<LootInjection> ALL = List.of(
		JADE_INGOT, TOPAZ_INGOT, SAPPHIRE_INGOT, STELLARSTONE_INGOT,
		JADE_TOPAZ_TOOLS, SAPPHIRE_TOOLS, ANCIENT_CITY, GUARDIAN, PIGLIN_BARTER
	).stream().flatMap(List::stream).toList();
}
