package com.hjmmd_8.createoreexpansion.content.charger.craft;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveCraftResults.DebugLog;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 变体波的<b>机器环境前置条件</b>判定（④）：配方要求的环境在命中点附近是否满足。
 *
 * <p>配方级判定见 {@link WaveCraftResults}（产物）与 {@link Candidate}（材料/辅料/流体/电量），
 * 这里只管"环境"这一层：加热、压弯机头、鼓风机媒介。三类判定都<b>只读世界、不产生副作用</b>。</p>
 *
 * <p><b>状态由调用方传入</b>：变器携带的热档（{@code carriedHeat}）与波源位置（{@code waveOrigin}）
 * 都是波携带的状态，本类不持有字段；轨迹日志经 {@link DebugLog} 回调写出。</p>
 */
public final class WaveEnvironmentChecks {

	private WaveEnvironmentChecks() {
	}

	/**
	 * 配方所需"机器环境"是否在命中点附近满足（④）：
	 * <ul>
	 *   <li><b>加热</b>：{@code ProcessingRecipe.getRequiredHeat()} = HEATED/SUPERHEATED 的配方
	 *       （搅拌加热、真空室加热、硫磺→SO₂ 等）→ <b>变器携带的热档</b>（扫描半径内的烈焰燃烧室，
	 *       即 {@code carriedHeat}）<b>或</b>命中点/波源邻域内的烈焰人达标即可
	 *       （HEATED 需 KINDLED+，SUPERHEATED 需 SEETHING，复用 {@link HeatCondition#testBlazeBurner} 口径）；</li>
	 *   <li><b>压弯机头</b>：Vintage curving 配方 → 附近需有已装对应头的冲压机（{@link #nearbyCurvingPressSatisfies}）；</li>
	 *   <li><b>鼓风机媒介</b>：Splashing(水)/Haunting(灵魂火)/本模组嬗化(嬗变液) fan 配方 →
	 *       附近需有对应媒介（复用 Create {@link FanProcessingType#isValidAt} 判定入口，不另造规则）；</li>
	 * </ul>
	 * 普通配方恒通过；任何内部异常按"通过"保守处理（环境判定不应让波莫名停摆）。
	 */
	public static boolean satisfied(Level level, BlockPos around, BlockPos waveOrigin, Recipe<?> recipe,
		BlazeBurnerBlock.HeatLevel carriedHeat, DebugLog trace) {
		try {
			// 判定中心（任一满足即通过）：
			//   ① 命中点（被加工方块，通常是工作盆本身）
			//   ② 波源（变器出射面）——玩家会把烈焰人摆在变器旁边
			//   ③ 搅拌器所在格（若盆上有机械搅拌器）——"搅拌配方的热源贴着搅拌器放也行"
			List<BlockPos> centers = new ArrayList<>(3);
			centers.add(around);
			if (waveOrigin != null && !waveOrigin.equals(around))
				centers.add(waveOrigin);
			// ③ 搅拌器所在格：仅当命中点确实是工作盆、且盆上压着机械搅拌器时追加。
			// 注意：搅拌器<b>不是</b>加工前置条件（用户 2026-09 明确），这里只用它扩大热源覆盖范围。
			BlockPos mixer = isBasinAt(level, around) ? mixerPosAbove(level, around) : null;
			if (mixer != null && !centers.contains(mixer))
				centers.add(mixer);
			for (int i = 0; i < centers.size(); i++) {
				boolean last = i == centers.size() - 1;
				if (environmentSatisfiedAt(level, centers.get(i), waveOrigin, recipe, last, carriedHeat, trace))
					return true;
			}
			return false;
		} catch (Throwable ignored) {
			// 判定异常（可选 mod 类缺失/未注册等）：按环境满足放行
			return true;
		}
	}

	/**
	 * 命中方块是否为<b>工作盆</b>（{@code BasinBlockEntity}）。
	 * 仅用于判断"该不该把盆上的搅拌器也算作一个环境判定中心"——搅拌器<b>不是</b>加工前置条件。
	 */
	private static boolean isBasinAt(Level level, BlockPos pos) {
		try {
			return pos != null && !level.isClientSide && level.getBlockEntity(pos) instanceof BasinBlockEntity;
		} catch (Throwable ignored) {
			return false;
		}
	}

	/**
	 * 工作盆<b>正上方 1 格</b>的机械搅拌器位置；不是搅拌器返回 null。
	 *
	 * <p>只认正上方 1 格：Create 里搅拌器就是压在盆上的，用 3×3×3 会让一排搅拌器把整片区域
	 * 都变成环境判定中心，与摆法直觉不符。</p>
	 */
	private static BlockPos mixerPosAbove(Level level, BlockPos basinPos) {
		if (basinPos == null)
			return null;
		try {
			BlockPos above = basinPos.above();
			return level.getBlockState(above)
				.is(com.simibubi.create.AllBlocks.MECHANICAL_MIXER.get()) ? above : null;
		} catch (Throwable ignored) {
			return null;
		}
	}

	/**
	 * 单个判定中心的环境检查（逻辑同旧版 {@code environmentSatisfied}）。
	 *
	 * @param report 本次是否为"最后一次尝试"：是才写加热不满足的轨迹日志，避免两个中心各刷一条
	 */
	private static boolean environmentSatisfiedAt(Level level, BlockPos around, BlockPos waveOrigin, Recipe<?> recipe,
		boolean report, BlazeBurnerBlock.HeatLevel carriedHeat, DebugLog trace) {
		if (recipe instanceof ProcessingRecipe<?, ?> pr && pr.getRequiredHeat() != HeatCondition.NONE) {
			if (!nearbyHeatSatisfies(level, around, waveOrigin, pr.getRequiredHeat(), report, carriedHeat, trace))
				return false;
		}
		ResourceLocation typeKey = WaveCraftResults.typeKeyOf(recipe);
		if (typeKey == null)
			return true;
		// Vintage 冲压（curving）：需要装了对应头的压弯机
		if ("vintageimprovements".equals(typeKey.getNamespace()) && "curving".equals(typeKey.getPath()))
			return nearbyCurvingPressSatisfies(level, around, recipe);
		// 鼓风机 fan 系（媒介在环境、不在配方字段）：水 / 灵魂火 / 嬗变液
		FanProcessingType fanType = fanMediaFor(typeKey);
		if (fanType != null && !nearbyMediaSatisfies(level, around, fanType))
			return false;
		return true;
	}

	/**
	 * 附近是否存在满足目标热档位的烈焰人（状态 HEAT_LEVEL ≥ 所需档位，与 Create 机器口径一致）。
	 *
	 * <p><b>两条来源，任一满足即可</b>：</p>
	 * <ol>
	 *   <li><b>变器携带的热档</b>（{@code carriedHeat}）——变器扫描半径内点燃的烈焰燃烧室。
	 *       变器的读取半径最大 3 格，而本方法原先只找命中点/波源邻域 3×3×3（半径 1）；
	 *       燃烧室只要不是紧贴波的命中点或出射面就一律"查无烈焰人"，玩家看到的现象就是
	 *       "范围内明明点了火，波却说不满足加热"；</li>
	 *   <li><b>命中点邻域现找</b>（原有行为，保留）：命中点/波源/搅拌器格 3×3×3 内有达标烈焰人。</li>
	 * </ol>
	 */
	private static boolean nearbyHeatSatisfies(Level level, BlockPos center, BlockPos waveOrigin,
		HeatCondition required, boolean report, BlazeBurnerBlock.HeatLevel carriedHeat, DebugLog trace) {
		// ① 变器携带的加热能力（把周围机器的能力"整合进波"：变器读到了火，波就带火）
		if (required.testBlazeBurner(carriedHeat))
			return true;
		BlazeBurnerBlock.HeatLevel best = BlazeBurnerBlock.HeatLevel.NONE;
		for (BlockPos bp : WaveAuxResolver.envBlocks(center)) {
			try {
				BlazeBurnerBlock.HeatLevel heat = BlazeBurnerBlock.getHeatLevelOf(level.getBlockState(bp));
				if (heat == BlazeBurnerBlock.HeatLevel.NONE)
					continue;
				if (required.testBlazeBurner(heat))
					return true;
				if (heat.ordinal() > best.ordinal())
					best = heat; // 记录邻域内最高档位，供热不满足时报告
			} catch (Throwable ignored) {
				// 单个方块判定异常：跳过
			}
		}
		// 走到这里 = 携带热档与两个中心邻域内都没有达标的烈焰人：把"哪里、携带了什么、需要什么、
		// 实际最高只有什么"写进轨迹，免得"搅拌加热配方不生效"到底是环境没过还是选了别的配方只能靠猜。
		if (report)
			trace.log("加热环境不满足：命中点 {} 与波源 {} 邻域 3×3×3 内均无达标烈焰人"
				+ "（变器携带热档 = {}，此处最高热档 = {}）", center, waveOrigin, carriedHeat, best);
		return false;
	}

	/**
	 * 附近是否存在可承载该配方所需媒介的鼓风机 fan 判定位
	 * （逐一以命中点邻域方块调用 Create 现成 {@code FanProcessingType.isValidAt}）。
	 */
	private static boolean nearbyMediaSatisfies(Level level, BlockPos center, FanProcessingType type) {
		for (BlockPos bp : WaveAuxResolver.envBlocks(center)) {
			try {
				if (type.isValidAt(level, bp))
					return true;
			} catch (Throwable ignored) {
				// 单个位置判定异常：跳过
			}
		}
		return false;
	}

	/** fan 系配方的媒介类型（依配方类型 id；非 fan 系返回 null = 无环境要求）。 */
	private static FanProcessingType fanMediaFor(ResourceLocation typeKey) {
		String ns = typeKey.getNamespace();
		String path = typeKey.getPath();
		if ("create".equals(ns)) {
			if ("splashing".equals(path))
				return com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes.SPLASHING;
			if ("haunting".equals(path))
				return com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes.HAUNTING;
			return null;
		}
		if ("createoreexpansion".equals(ns) && "transmuting".equals(path))
			return com.hjmmd_8.createoreexpansion.common.AllFanProcessingTypes.TRANSMUTING;
		return null;
	}

	/**
	 * 附近是否存在装了<b>对应头</b>的 Vintage 冲压机（curving press）：
	 * 优先精确比对（配方 itemAsHead → 机器自定义头槽；否则配方 mode → 机器 mode）；
	 * 反射读取失败时保守降级为"存在已装任何头的压机即通过"（任务允许的保守退路）。
	 * 全程按类名/反射判定，不 import Vintage 类。
	 */
	private static boolean nearbyCurvingPressSatisfies(Level level, BlockPos center, Recipe<?> recipe) {
		Item requiredHeadItem = null;
		Integer requiredMode = null;
		try {
			Object item = recipe.getClass()
				.getMethod("getItemAsHead")
				.invoke(recipe);
			if (item instanceof Item it && it != Items.AIR)
				requiredHeadItem = it;
		} catch (Throwable ignored) {
			// 无自定义头字段/反射失败
		}
		try {
			requiredMode = (Integer) recipe.getClass()
				.getMethod("getMode")
				.invoke(recipe);
		} catch (Throwable ignored) {
			// 无 mode 字段/反射失败
		}

		for (BlockPos bp : WaveAuxResolver.envBlocks(center)) {
			BlockEntity be = level.getBlockEntity(bp);
			if (be == null || !isCurvingPressEntity(be))
				continue;
			Integer machineMode = readPublicInt(be, "mode");
			if (machineMode == null || machineMode <= 0)
				continue; // 未装头
			// 需要特定物品头（nether_star 等"以物为头"配方）：mode=5（自定义头槽）且槽内物品一致
			if (requiredHeadItem != null) {
				if (machineMode == 5 && requiredHeadItem.equals(readHeadSlotItem(be)))
					return true;
				continue;
			}
			// 普通形状头配方：机器模式与配方 mode 一致即满足
			if (requiredMode != null && machineMode == requiredMode.intValue())
				return true;
			// 配方 mode 读不到（保守）：装了头就算满足
			if (requiredMode == null)
				return true;
		}
		// 附近没有与配方匹配的压机头
		return false;
	}

	/** 该方块实体是否为 Vintage 冲压机（仅按运行时类名判定，不加载其类）。 */
	private static boolean isCurvingPressEntity(BlockEntity be) {
		try {
			String name = be.getClass()
				.getName();
			return name.endsWith("CurvingPressBlockEntity")
				|| name.contains(".curving_press.CurvingPressBlockEntity");
		} catch (Throwable ignored) {
			return false;
		}
	}

	/** 反射读机器 public int 字段（mode）；失败返回 null。 */
	private static Integer readPublicInt(BlockEntity be, String field) {
		try {
			return (Integer) be.getClass()
				.getField(field)
				.get(be);
		} catch (Throwable ignored) {
			return null;
		}
	}

	/** 反射读机器自定义头槽（itemAsHead 槽 0）物品；失败返回 null。 */
	private static Item readHeadSlotItem(BlockEntity be) {
		try {
			Object inv = be.getClass()
				.getField("itemAsHead")
				.get(be);
			if (inv == null)
				return null;
			Object stack = inv.getClass()
				.getMethod("getStackInSlot", int.class)
				.invoke(inv, 0);
			if (stack instanceof ItemStack is && !is.isEmpty())
				return is.getItem();
			return null;
		} catch (Throwable ignored) {
			// SmartInventory 可能以 getItem(int) 暴露
			try {
				Object inv = be.getClass()
					.getField("itemAsHead")
					.get(be);
				Object stack = inv.getClass()
					.getMethod("getItem", int.class)
					.invoke(inv, 0);
				if (stack instanceof ItemStack is && !is.isEmpty())
					return is.getItem();
			} catch (Throwable ignored2) {
				// 反射失败
			}
			return null;
		}
	}
}
