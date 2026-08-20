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

	/** 角磨轮物品 id → PartialModel */
	public static final Map<ResourceLocation, PartialModel> GRINDING_WHEELS = Map.of(
		CreateOreExpansion.modLoc("iron_grinding_wheel"), IRON_GRINDING_WHEEL,
		CreateOreExpansion.modLoc("jade_grinding_wheel"), JADE_GRINDING_WHEEL,
		CreateOreExpansion.modLoc("diamond_grinding_wheel"), DIAMOND_GRINDING_WHEEL,
		CreateOreExpansion.modLoc("topaz_grinding_wheel"), TOPAZ_GRINDING_WHEEL,
		CreateOreExpansion.modLoc("sapphire_grinding_wheel"), SAPPHIRE_GRINDING_WHEEL,
		CreateOreExpansion.modLoc("stellarstone_grinding_wheel"), STELLARSTONE_GRINDING_WHEEL);

	private AllPartialModels() {
	}

	/** 触发类加载（PartialModel 静态字段初始化），在客户端初始化早期调用 */
	public static void init() {
	}
}
