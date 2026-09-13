package com.hjmmd_8.createoreexpansion.content.charger.payload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.StellarWaveMachineRegistry;
import com.hjmmd_8.createoreexpansion.util.RadiusScan;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * <b>变体波载荷的"余料处置"实现</b>（2026-09 从 {@code StellarWaveEntity} 抽出成类，分包整理的一部分）。
 *
 * <p>口径由配置 {@code wave.payloadRelease} 决定（见设计文档 §11.1）：</p>
 * <ul>
 *   <li><b>{@code NEAREST_CONTAINER}（默认）</b>：物品存进<b>最近加工过的方块周围、变器读取半径之内</b>
 *       最近的可存容器（按距离由近到远，装满一个接着下一个）；流体/电量就近注入同一范围内的储罐/储能；</li>
 *   <li>{@code SOURCE_CONTAINER}：还回实际取料的那几个容器/储罐/储能；</li>
 *   <li>{@code DROP_AT_DISSIPATION}：物品全部爆落在消散点。</li>
 * </ul>
 *
 * <p><b>三处硬性口径</b>（都是实测反馈换来的，改前先读）：</p>
 * <ol>
 *   <li><b>绝不放进正在加工的工作盆</b>——判定收敛在 {@link #isStoreTarget}：工作盆、目录登记的加工机
 *       （含注液器/物品排放器这类非动能机）、动能方块一律不算可存目标（2026-09 统一口径）；</li>
 *   <li><b>未加载区块不碰</b>：读方块状态/能力会触发同步加载，故一律先 {@code level.isLoaded(pos)}；</li>
 *   <li><b>逐目标异常隔离</b>：第三方容器/储能的能力实现可能抛异常，单个目标失败按"放不下"处理，
 *       由调用方（{@code StellarWaveEntity#remove/finishAndDiscard}）再包一层，绝不让异常冒出 tick。</li>
 * </ol>
 *
 * <p><b>性能</b>（2026-09 审计修复）：候选容器列表<b>一次结算</b>（{@link #sortedStoreTargets}）后
 * 供整轮释放复用；旧实现是"每一件余料都重建并排序整块半径立方体"（最坏 64 件 × 343 位置）。</p>
 */
public final class WavePayloadRelease {

	private WavePayloadRelease() {
	}

	/**
	 * 一次释放所需的全部输入（助手类不反向依赖波实体的私有状态）。
	 *
	 * @param mode    处置口径（{@code wave.payloadRelease}）
	 * @param wavePos 波当前位置（兜底掉落点 / "就近"扫描中心）
	 * @param center  半径圆心：最近加工过的方块（无则用 {@code wavePos}）
	 * @param radius  范围半径 = 变器读取半径（{@code carriedScanRadius}）
	 */
	public record Request(Level level, AllConfig.PayloadRelease mode, Vec3 wavePos,
		List<ItemStack> items, FluidStack fluid, int energy, List<BlockPos> sources,
		BlockPos center, int radius) {
	}

	/** 释放一次（调用方负责清空载荷状态；本方法内部逐目标 try/catch）。 */
	public static void release(Request req) {
		Level level = req.level();
		if (level == null || level.isClientSide)
			return;
		BlockPos center = req.center() != null ? req.center() : BlockPos.containing(req.wavePos());
		// 候选容器一次结算（跳过工作盆/加工机/未加载区块），整轮复用
		List<BlockPos> targets = sortedStoreTargets(level, center, Math.max(1, req.radius()));

		for (ItemStack stack : req.items()) {
			if (stack == null || stack.isEmpty())
				continue;
			ItemStack rest;
			switch (req.mode()) {
				case NEAREST_CONTAINER -> {
					rest = storeInNearest(level, targets, stack);
					if (!rest.isEmpty())
						rest = returnItemsToSources(level, req.sources(), rest); // 现场放不下：退回原箱
				}
				case SOURCE_CONTAINER -> rest = returnItemsToSources(level, req.sources(), stack);
				default -> rest = stack.copy(); // DROP_AT_DISSIPATION
			}
			if (rest.isEmpty())
				continue;
			BlockPos at = lastSource(req.sources());
			if (req.mode() != AllConfig.PayloadRelease.DROP_AT_DISSIPATION && at != null)
				dropAt(level, at.above(), rest); // 落在来源方块上方：绝不落进正在加工的容器
			else
				dropAt(level, BlockPos.containing(req.wavePos()), rest);
		}

		if (req.fluid() != null && !req.fluid()
			.isEmpty()) {
			FluidStack rest = switch (req.mode()) {
				case NEAREST_CONTAINER -> fillNearestTank(level, targets, req.fluid());
				case SOURCE_CONTAINER -> returnFluidToSources(level, req.sources(), req.fluid());
				default -> req.fluid();
			};
			if (!rest.isEmpty())
				fillNearby(level, req.wavePos(), rest); // 仍放不下：就近尽力而为（无处可存则浪费）
		}

		if (req.energy() > 0) {
			int left = switch (req.mode()) {
				case NEAREST_CONTAINER -> chargeNearestStorage(level, targets, req.energy());
				case SOURCE_CONTAINER -> returnEnergyToSources(level, req.sources(), req.energy());
				default -> req.energy();
			};
			if (left > 0)
				chargeNearby(level, req.wavePos(), left);
		}
	}

	/**
	 * 该位置是否可以作为"载荷存储目标"——<b>排除口径的唯一实现</b>（取料与余料入库共用）。
	 *
	 * <p>返回 false 的情形：{@code null} / 未加载区块 / 工作盆（玩家在用的加工容器）/
	 * 目录登记的加工机与各类动能方块（含注液器、物品排放器这类非动能机）内部库存。</p>
	 */
	public static boolean isStoreTarget(Level level, BlockPos pos) {
		if (level == null || pos == null)
			return false;
		try {
			if (!level.isLoaded(pos))
				return false; // 未加载区块：读方块状态/能力会触发同步加载
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof BasinBlockEntity)
				return false; // 工作盆：余料进盆是明确的 bug（2026-09 实测反馈）
			return !StellarWaveMachineRegistry.isMachinery(level, pos);
		} catch (Throwable ignored) {
			return false; // 判定异常：保守按"不可存"处理
		}
	}

	/** 圆心半径内的可存目标，按到圆心距离由近到远排序（供整轮释放复用）。 */
	private static List<BlockPos> sortedStoreTargets(Level level, BlockPos center, int radius) {
		List<BlockPos> list = new ArrayList<>();
		// 排除口径交给 RadiusScan 的 skip（中心格与未加载区块由工具自身排除）
		RadiusScan.forEachInRadius(level, center, radius, pos -> !isStoreTarget(level, pos), pos -> list.add(pos.immutable()));
		list.sort(Comparator.comparingDouble(center::distSqr));
		return list;
	}

	/** 把余料物品塞进最近的可存容器（装满一个接着下一个）；返回没塞进去的剩余。 */
	private static ItemStack storeInNearest(Level level, List<BlockPos> targets, ItemStack stack) {
		ItemStack rest = stack.copy();
		for (BlockPos pos : targets) {
			if (rest.isEmpty())
				break;
			IItemHandler handler = handlerAt(level, pos);
			if (handler == null)
				continue;
			for (int slot = 0; slot < handler.getSlots() && !rest.isEmpty(); slot++) {
				try {
					rest = handler.insertItem(slot, rest, false);
				} catch (Throwable ignored) {
					break; // 该容器异常：换下一个
				}
			}
		}
		return rest;
	}

	/** 把余料流体注进最近的储罐；返回没注进去的部分。 */
	private static FluidStack fillNearestTank(Level level, List<BlockPos> targets, FluidStack stack) {
		int left = stack.getAmount();
		for (BlockPos pos : targets) {
			if (left <= 0)
				break;
			IFluidHandler tank = tankAt(level, pos);
			if (tank == null)
				continue;
			try {
				left -= tank.fill(new FluidStack(stack.getFluid(), left), IFluidHandler.FluidAction.EXECUTE);
			} catch (Throwable ignored) {
				// 该储罐异常：换下一个
			}
		}
		return left <= 0 ? FluidStack.EMPTY : new FluidStack(stack.getFluid(), Math.max(0, left));
	}

	/** 把余料电量充进最近的储能；返回没充进去的 FE。 */
	private static int chargeNearestStorage(Level level, List<BlockPos> targets, int fe) {
		int left = fe;
		for (BlockPos pos : targets) {
			if (left <= 0)
				break;
			IEnergyStorage storage = storageAt(level, pos);
			if (storage == null || !storage.canReceive())
				continue;
			try {
				left -= storage.receiveEnergy(left, false);
			} catch (Throwable ignored) {
				// 该储能异常：换下一个
			}
		}
		return Math.max(0, left);
	}

	/** 把物品还回取料容器（最新来源优先，逐槽全扫）；同样只接受"可存目标"。 */
	private static ItemStack returnItemsToSources(Level level, List<BlockPos> sources, ItemStack stack) {
		ItemStack rest = stack.copy();
		for (int i = sources.size() - 1; i >= 0 && !rest.isEmpty(); i--) {
			BlockPos src = sources.get(i);
			if (!isStoreTarget(level, src))
				continue; // 来源方块可能已被换成工作盆/机器（2026-09 审计修复）
			IItemHandler handler = handlerAt(level, src);
			if (handler == null)
				continue;
			for (int slot = 0; slot < handler.getSlots() && !rest.isEmpty(); slot++) {
				try {
					rest = handler.insertItem(slot, rest, false);
				} catch (Throwable ignored) {
					break;
				}
			}
		}
		return rest;
	}

	/** 把流体还回取料储罐；返回剩余量（EMPTY = 全部还回）。 */
	private static FluidStack returnFluidToSources(Level level, List<BlockPos> sources, FluidStack stack) {
		int left = stack.getAmount();
		for (int i = sources.size() - 1; i >= 0 && left > 0; i--) {
			BlockPos src = sources.get(i);
			if (!isStoreTarget(level, src))
				continue;
			IFluidHandler tank = tankAt(level, src);
			if (tank == null)
				continue;
			try {
				left -= tank.fill(new FluidStack(stack.getFluid(), left), IFluidHandler.FluidAction.EXECUTE);
			} catch (Throwable ignored) {
				// 该储罐异常：换下一个
			}
		}
		return left <= 0 ? FluidStack.EMPTY : new FluidStack(stack.getFluid(), left);
	}

	/** 把电量充回取料储能；返回没充进去的剩余 FE。 */
	private static int returnEnergyToSources(Level level, List<BlockPos> sources, int fe) {
		int left = fe;
		for (int i = sources.size() - 1; i >= 0 && left > 0; i--) {
			BlockPos src = sources.get(i);
			if (!isStoreTarget(level, src))
				continue;
			IEnergyStorage storage = storageAt(level, src);
			if (storage == null || !storage.canReceive())
				continue;
			try {
				left -= storage.receiveEnergy(left, false);
			} catch (Throwable ignored) {
				// 该储能异常：换下一个
			}
		}
		return Math.max(0, left);
	}

	/**
	 * 波当前位置 1 格邻域"就近"注入流体（最后的兜底）。
	 *
	 * <p><b>2026-09 审计修复</b>：也要过 {@link #isStoreTarget} —— 否则兜底会把余料流体注进
	 * 波正贴着的那口工作盆（正是 §11.1 明文要避免的现象）。</p>
	 */
	private static void fillNearby(Level level, Vec3 wavePos, FluidStack stack) {
		int left = stack.getAmount();
		for (BlockPos pos : nearby(wavePos)) {
			if (left <= 0)
				return;
			if (!isStoreTarget(level, pos))
				continue;
			IFluidHandler tank = tankAt(level, pos);
			if (tank == null)
				continue;
			try {
				left -= tank.fill(new FluidStack(stack.getFluid(), left), IFluidHandler.FluidAction.EXECUTE);
			} catch (Throwable ignored) {
				// 该储罐异常：换下一个
			}
		}
	}

	/** 波当前位置 1 格邻域"就近"充电（最后的兜底）；同样过排除口径。 */
	private static void chargeNearby(Level level, Vec3 wavePos, int fe) {
		int left = fe;
		for (BlockPos pos : nearby(wavePos)) {
			if (left <= 0)
				return;
			if (!isStoreTarget(level, pos))
				continue;
			IEnergyStorage storage = storageAt(level, pos);
			if (storage == null || !storage.canReceive())
				continue;
			try {
				left -= storage.receiveEnergy(left, false);
			} catch (Throwable ignored) {
				// 该储能异常：换下一个
			}
		}
	}

	/** 波当前位置周围 1 格（不含自身所在格）。 */
	private static List<BlockPos> nearby(Vec3 wavePos) {
		BlockPos c = BlockPos.containing(wavePos);
		List<BlockPos> list = new ArrayList<>(26);
		for (int dx = -1; dx <= 1; dx++)
			for (int dy = -1; dy <= 1; dy++)
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos bp = c.offset(dx, dy, dz);
					if (!bp.equals(c))
						list.add(bp);
				}
		return list;
	}

	private static BlockPos lastSource(List<BlockPos> sources) {
		return sources == null || sources.isEmpty() ? null : sources.get(sources.size() - 1);
	}

	/** 方块上方掉落（不落进方块内部）。 */
	private static void dropAt(Level level, BlockPos pos, ItemStack stack) {
		ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, stack);
		drop.setDeltaMovement(Vec3.ZERO);
		level.addFreshEntity(drop);
	}

	private static IItemHandler handlerAt(Level level, BlockPos pos) {
		try {
			return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static IFluidHandler tankAt(Level level, BlockPos pos) {
		try {
			return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static IEnergyStorage storageAt(Level level, BlockPos pos) {
		try {
			return level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
		} catch (Throwable ignored) {
			return null;
		}
	}
}
