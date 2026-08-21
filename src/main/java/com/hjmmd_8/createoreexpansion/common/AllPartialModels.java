package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * 模组 PartialModel 注册（仿照机械动力 AllPartialModels）。
 *
 * <p>必须在客户端初始化早期调用 {@link #init()}（触发类加载），
 * 确保 Flywheel 在模型烘焙前收集到这些部件模型，否则渲染会变成紫黑缺失方块。</p>
 */
public final class AllPartialModels {

	/** 角磨轮槽（models/block/power_angle_grinder/grinding_spindle.json，随传动轴旋转） */
	public static final PartialModel GRINDING_SPINDLE = PartialModel.of(
		CreateOreExpansion.modLoc("block/power_angle_grinder/grinding_spindle"));

	/** 传动轴短轴（models/block/power_angle_grinder/axis.json，沿 Z、长度 4） */
	public static final PartialModel GRINDER_AXIS = PartialModel.of(
		CreateOreExpansion.modLoc("block/power_angle_grinder/axis"));

	/** 铁角磨轮 */
	public static final PartialModel IRON_GRINDING_WHEEL = PartialModel.of(
		CreateOreExpansion.modLoc("block/power_angle_grinder/power_angle_wheel/iron_grinding_wheel"));
	/** 金角磨轮 */
	public static final PartialModel GOLD_GRINDING_WHEEL = PartialModel.of(
		CreateOreExpansion.modLoc("block/power_angle_grinder/power_angle_wheel/gold_grinding_wheel"));
	/** 黄铜角磨轮 */
	public static final PartialModel BRASS_GRINDING_WHEEL = PartialModel.of(
		CreateOreExpansion.modLoc("block/power_angle_grinder/power_angle_wheel/brass_grinding_wheel"));
	/** 锌角磨轮 */
	public static final PartialModel ZINC_GRINDING_WHEEL = PartialModel.of(
		CreateOreExpansion.modLoc("block/power_angle_grinder/power_angle_wheel/zinc_grinding_wheel"));
	/** 翡翠角磨轮 */
	public static final PartialModel JADE_GRINDING_WHEEL = PartialModel.of(
		CreateOreExpansion.modLoc("block/power_angle_grinder/power_angle_wheel/jade_grinding_wheel"));
	/** 钻石角磨轮 */
	public static final PartialModel DIAMOND_GRINDING_WHEEL = PartialModel.of(
		CreateOreExpansion.modLoc("block/power_angle_grinder/power_angle_wheel/diamond_grinding_wheel"));
	/** 黄玉角磨轮 */
	public static final PartialModel TOPAZ_GRINDING_WHEEL = PartialModel.of(
		CreateOreExpansion.modLoc("block/power_angle_grinder/power_angle_wheel/topaz_grinding_wheel"));
	/** 蓝宝石角磨轮 */
	public static final PartialModel SAPPHIRE_GRINDING_WHEEL = PartialModel.of(
		CreateOreExpansion.modLoc("block/power_angle_grinder/power_angle_wheel/sapphire_grinding_wheel"));
	/** 星辉石角磨轮 */
	public static final PartialModel STELLARSTONE_GRINDING_WHEEL = PartialModel.of(
		CreateOreExpansion.modLoc("block/power_angle_grinder/power_angle_wheel/stellarstone_grinding_wheel"));
	/** 下界合金角磨轮 */
	public static final PartialModel NETHERITE_GRINDING_WHEEL = PartialModel.of(
		CreateOreExpansion.modLoc("block/power_angle_grinder/power_angle_wheel/netherite_grinding_wheel"));

	/** 翡翠应力充能器发射头（models/block/jade_create_charger/shutter.json，蓄力时向传动轴方向位移） */
	public static final PartialModel CHARGER_SHUTTER = PartialModel.of(
		CreateOreExpansion.modLoc("block/jade_create_charger/shutter"));

	/** 翡翠应力充能器传动轴短轴（models/block/jade_create_charger/charger_axis.json，沿 Y、长度 4）。
	 * <p>JEI 展示用短轴替代 Create 全尺寸 shaft（16px）防止穿模；
	 * 模型坐标 y 12~16（xz 中心与 Create shaft 同为 0.5/0.5），在 FACING=DOWN 翻转后的
	 * 机身（绕方块中心旋转，接口落在 y=16）下方伸出 —— 轴从机身底部接口下方伸出。</p> */
	public static final PartialModel CHARGER_AXIS = PartialModel.of(
		CreateOreExpansion.modLoc("block/jade_create_charger/charger_axis"));

	/** 角磨轮物品 id → PartialModel */
	public static final Map<ResourceLocation, PartialModel> GRINDING_WHEELS = Map.ofEntries(
		Map.entry(CreateOreExpansion.modLoc("iron_grinding_wheel"), IRON_GRINDING_WHEEL),
		Map.entry(CreateOreExpansion.modLoc("gold_grinding_wheel"), GOLD_GRINDING_WHEEL),
		Map.entry(CreateOreExpansion.modLoc("brass_grinding_wheel"), BRASS_GRINDING_WHEEL),
		Map.entry(CreateOreExpansion.modLoc("zinc_grinding_wheel"), ZINC_GRINDING_WHEEL),
		Map.entry(CreateOreExpansion.modLoc("jade_grinding_wheel"), JADE_GRINDING_WHEEL),
		Map.entry(CreateOreExpansion.modLoc("diamond_grinding_wheel"), DIAMOND_GRINDING_WHEEL),
		Map.entry(CreateOreExpansion.modLoc("topaz_grinding_wheel"), TOPAZ_GRINDING_WHEEL),
		Map.entry(CreateOreExpansion.modLoc("sapphire_grinding_wheel"), SAPPHIRE_GRINDING_WHEEL),
		Map.entry(CreateOreExpansion.modLoc("stellarstone_grinding_wheel"), STELLARSTONE_GRINDING_WHEEL),
		Map.entry(CreateOreExpansion.modLoc("netherite_grinding_wheel"), NETHERITE_GRINDING_WHEEL));

	private AllPartialModels() {
	}

	/** 触发类加载（PartialModel 静态字段初始化），在客户端初始化早期调用 */
	public static void init() {
	}
}
