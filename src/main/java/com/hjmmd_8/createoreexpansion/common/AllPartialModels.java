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

	/** 翡翠应力充能器发射头（models/block/jade_stress_charger/jade_shutter.json，蓄力时向传动轴方向位移） */
	public static final PartialModel CHARGER_SHUTTER = PartialModel.of(
		CreateOreExpansion.modLoc("block/jade_stress_charger/jade_shutter"));

	/** 翡翠应力充能器传动轴短轴（models/block/jade_stress_charger/jade_charger_axis.json，沿 Y、长度 4）。
	 * <p>JEI 展示用短轴替代 Create 全尺寸 shaft（16px）防止穿模；
	 * 模型坐标 y 12~16（xz 中心与 Create shaft 同为 0.5/0.5），在 FACING=DOWN 翻转后的
	 * 机身（绕方块中心旋转，接口落在 y=16）下方伸出 —— 轴从机身底部接口下方伸出。</p> */
	public static final PartialModel CHARGER_AXIS = PartialModel.of(
		CreateOreExpansion.modLoc("block/jade_stress_charger/jade_charger_axis"));

	/** 蓝宝石应力充能器发射头（models/block/sapphire_stress_charger/sapphire_shutter.json）——JEI 4/5 级动画用 */
	public static final PartialModel SAPPHIRE_CHARGER_SHUTTER = PartialModel.of(
		CreateOreExpansion.modLoc("block/sapphire_stress_charger/sapphire_shutter"));

	/** 星辉石应力充能器发射头（models/block/stellarstone_stress_charger/stellarstone_shutter.json，
	 * 贴星辉石发射口/快门纹理）——与蓝宝石同型复刻，供 CreateChargerRenderer 区分机型 */
	public static final PartialModel STELLARSTONE_CHARGER_SHUTTER = PartialModel.of(
		CreateOreExpansion.modLoc("block/stellarstone_stress_charger/stellarstone_shutter"));

	/** 蓝宝石应力充能器传动轴短轴（models/block/sapphire_stress_charger/sapphire_charger_axis.json，
	 * 同翡翠 4px 短轴布局）——JEI 4/5 级动画用 */
	public static final PartialModel SAPPHIRE_CHARGER_AXIS = PartialModel.of(
		CreateOreExpansion.modLoc("block/sapphire_stress_charger/sapphire_charger_axis"));

	/** 能量调级器齿轮（models/block/energy_wave_machine/jade_cogwheel.json，随应力绕 FACING 轴旋转） */
	public static final PartialModel WAVE_REGULATOR_COGWHEEL = PartialModel.of(
		CreateOreExpansion.modLoc("block/energy_wave_machine/jade_cogwheel"));

	/** 波闸侧面指示灯（models/block/energy_wave_machine/{机型}_wave_gate_lamp_{side}_{pos}.json，
	 * 4 侧面 × 上下：顶面板 open + 转速达标 → 各侧面顶部灯位亮；底面板 open → 各侧面底部灯位亮。
	 * 自发光渲染；跟随 FACING 旋转（partialFacingVertical）。
	 * 机型前缀：jade = 翡翠灯（jade_light.png）、sapphire = 蓝宝石灯（sapphire_light.png）。） */
	public static final Map<String, PartialModel> WAVE_GATE_LAMPS = buildWaveGateLamps("jade");
	/** 蓝宝石波闸侧面指示灯（sapphire_wave_gate_lamp_{side}_{pos}.json，贴 sapphire_light.png） */
	public static final Map<String, PartialModel> SAPPHIRE_WAVE_GATE_LAMPS = buildWaveGateLamps("sapphire");
	/** 星辉石波闸侧面指示灯（stellarstone_wave_gate_lamp_{side}_{pos}.json，贴 stellarstone_light.png） */
	public static final Map<String, PartialModel> STELLARSTONE_WAVE_GATE_LAMPS = buildWaveGateLamps("stellarstone");

	private static Map<String, PartialModel> buildWaveGateLamps(String prefix) {
		java.util.Map<String, PartialModel> map = new java.util.HashMap<>();
		String[] sides = { "north", "east", "south", "west" };
		String[] pos = { "top", "bottom" };
		for (String side : sides) {
			for (String p : pos) {
				map.put(side + "_" + p, PartialModel.of(CreateOreExpansion.modLoc(
					"block/energy_wave_machine/" + prefix + "_wave_gate_lamp_" + side + "_" + p)));
			}
		}
		return java.util.Map.copyOf(map);
	}

	/** 能量波差器灯位（models/block/energy_wave_machine/jade_disperser_lamp_{north,east,south,west}.json，
	 * 从 light.png 剪切 2×2 灯位，顶/底灯盘双面；随 4 侧面开闭状态动态叠加） */
	public static final PartialModel DISPERSER_LAMP_NORTH = PartialModel.of(
		CreateOreExpansion.modLoc("block/energy_wave_machine/jade_disperser_lamp_north"));
	/** 东灯位 */
	public static final PartialModel DISPERSER_LAMP_EAST = PartialModel.of(
		CreateOreExpansion.modLoc("block/energy_wave_machine/jade_disperser_lamp_east"));
	/** 南灯位 */
	public static final PartialModel DISPERSER_LAMP_SOUTH = PartialModel.of(
		CreateOreExpansion.modLoc("block/energy_wave_machine/jade_disperser_lamp_south"));
	/** 西灯位 */
	public static final PartialModel DISPERSER_LAMP_WEST = PartialModel.of(
		CreateOreExpansion.modLoc("block/energy_wave_machine/jade_disperser_lamp_west"));

	/** 六面差波器指示灯（models/block/energy_wave_machine/jade_six_face_lamp_{face}_{pos}.json，
	 * 6 面 × 4 方向 up/down/left/right，贴 light.png 光点，按相邻面开闭状态亮/灭） */
	public static final Map<String, PartialModel> SIX_FACE_LAMPS = buildSixFaceLamps();

	/** 八面差波器指示灯（models/block/octa_energy_wave_differencer/octa_energy_wave_differencer_lamp_{dir}.json，
	 * 8 方向 × 顶/底两片，贴 special_light.png，开口方向灯亮。
	 * 必须在此注册（类加载时创建 PartialModel）供烘焙收集，否则开口渲染会整块紫黑缺失方块。 */
	public static final Map<String, PartialModel> OCTA_ENERGY_WAVE_DIFFERENCER_LAMPS = buildOctaLamps();

	private static Map<String, PartialModel> buildOctaLamps() {
		java.util.Map<String, PartialModel> map = new java.util.HashMap<>();
		String[] dirs = { "n", "ne", "e", "se", "s", "sw", "w", "nw" };
		for (String dir : dirs) {
			map.put(dir, PartialModel.of(CreateOreExpansion.modLoc(
				"block/octa_energy_wave_differencer/octa_energy_wave_differencer_lamp_" + dir)));
		}
		return java.util.Map.copyOf(map);
	}

	private static Map<String, PartialModel> buildSixFaceLamps() {
		java.util.Map<String, PartialModel> map = new java.util.HashMap<>();
		String[] faces = { "up", "down", "north", "east", "south", "west" };
		String[] pos = { "up", "down", "left", "right" };
		for (String face : faces) {
			for (String p : pos) {
				map.put(face + "_" + p, PartialModel.of(CreateOreExpansion.modLoc(
					"block/energy_wave_machine/jade_six_face_lamp_" + face + "_" + p)));
			}
		}
		return java.util.Map.copyOf(map);
	}

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
