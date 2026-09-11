package com.hjmmd_8.createoreexpansion.content.charger.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * <b>波载荷取料工具</b>：从某个中心点周围的立方体里"抽辅料/流体/电量"的<b>唯一实现</b>，
 * 供两处共用（口径必须一致，否则"变器抽到什么"和"波在半路补到什么"会对不上）：</p>
 * <ol>
 *   <li><b>变器</b>：穿波瞬间在自身扫描半径内抽载荷（{@code StellarWaveTransmuterBlockEntity#collectPayloadForWave}）；</li>
 *   <li><b>波</b>：命中目标后做"就地补料"（{@code StellarWaveEntity#refillPayloadAround}），
 *       范围 = <b>变器当时的扫描半径</b>（用户 2026-09 定义：补料范围与变器读取范围一致）。</li>
 * </ol>
 *
 * <p><b>取料顺序（用户 2026-09 明确）——先铺种类，再补数量</b>：</p>
 * <ol>
 *   <li>第一轮：按扫描顺序，每种物品只取 1 个（最多 {@code maxKinds} 种）——"小齿轮/大齿轮/铁粒各 1"；</li>
 *   <li>第二轮：还没到 {@code maxItems} 个时，从"当前存量最多的那种"继续各抽 1 个。</li>
 * </ol>
 * <p>旧实现是"从第一个槽抽到上限为止"，第一个槽放着一组小齿轮时会把 5 个全抽走，
 * 导致多辅料组合永远凑不齐（实测反馈）。</p>
 *
 * <p><b>估算与真抽同序</b>：{@code simulate=true} 时不动任何容器，第二轮靠本地虚拟余量收敛，
 * 所以"护目镜上的载荷预估值"与"穿波时真抽到的量"一致。</p>
 */
public final class WavePayloadGather {

	/** 载荷物品总量上限（个）——变器与"命中后补料"共用同一口径。 */
	public static final int MAX_ITEMS = 5;
	/** 载荷物品种类上限（种）。 */
	public static final int MAX_KINDS = 5;
	/** 载荷流体上限（mB）。 */
	public static final int MAX_FLUID_MB = 500;

	/** 一个候选槽（容器 + 槽号；同一容器可能出现多次）。 */
	private record SlotRef(IItemHandler handler, int slot) {
	}

	private WavePayloadGather() {
	}

	/**
	 * 物品取料：向 {@code into} 追加物品（同种并入已有条目），直到 {@code into} 达到
	 * {@code maxItems} 个 / {@code maxKinds} 种或四周没得抽为止。
	 *
	 * @param skip 该位置是否跳过（加工机 / 玩家正在加工的容器 / 命中点自身等，由调用方定义）
	 */
	public static void gatherItems(Level level, BlockPos center, int radius, List<ItemStack> into, int maxItems,
		int maxKinds, boolean simulate, Predicate<BlockPos> skip) {
		if (level == null || center == null)
			return;
		List<SlotRef> slots = new ArrayList<>();
		int r = Math.max(1, radius);
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			if (pos.equals(center))
				continue;
			if (skip != null && skip.test(pos))
				continue;
			IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (handler == null)
				continue;
			for (int slot = 0; slot < handler.getSlots(); slot++) {
				ItemStack stack = handler.getStackInSlot(slot);
				if (stack == null || stack.isEmpty())
					continue;
				slots.add(new SlotRef(handler, slot));
			}
		}
		if (slots.isEmpty())
			return;

		// —— 第一轮：每种各 1（最多 maxKinds 种）——
		for (SlotRef ref : slots) {
			if (into.size() >= maxKinds || totalItems(into) >= maxItems)
				break;
			ItemStack stack = ref.handler()
				.getStackInSlot(ref.slot());
			if (stack.isEmpty() || hasKind(into, stack))
				continue;
			ItemStack one = ref.handler()
				.extractItem(ref.slot(), 1, simulate);
			if (!one.isEmpty())
				insertItem(into, one);
		}
		if (into.isEmpty() || totalItems(into) >= maxItems)
			return;

		// —— 第二轮：从"存量最多的那种"补数量（只补已经携带到的种类）——
		Map<ResourceLocation, Integer> remaining = new HashMap<>();
		for (SlotRef ref : slots) {
			ItemStack stack = ref.handler()
				.getStackInSlot(ref.slot());
			if (stack.isEmpty() || !hasKind(into, stack))
				continue;
			remaining.merge(itemId(stack), stack.getCount(), Integer::sum);
		}
		while (totalItems(into) < maxItems) {
			ResourceLocation bestId = null;
			int bestCount = 0;
			for (Map.Entry<ResourceLocation, Integer> e : remaining.entrySet()) {
				if (e.getValue() > bestCount) {
					bestCount = e.getValue();
					bestId = e.getKey();
				}
			}
			if (bestId == null || bestCount <= 0)
				break;
			SlotRef target = null;
			int targetCount = 0;
			for (SlotRef ref : slots) {
				ItemStack stack = ref.handler()
					.getStackInSlot(ref.slot());
				if (stack.isEmpty() || !bestId.equals(itemId(stack)))
					continue;
				if (stack.getCount() > targetCount) {
					targetCount = stack.getCount();
					target = ref;
				}
			}
			if (target == null)
				break;
			ItemStack one = target.handler()
				.extractItem(target.slot(), 1, simulate);
			if (one.isEmpty())
				break;
			insertItem(into, one);
			remaining.put(bestId, bestCount - 1); // 估算模式靠它收敛；真抽模式与容器扣减同步
		}
	}

	/**
	 * 流体取料：只取<b>第一种</b>流体并抽到 {@code maxMb} 为止（与变器同口径；
	 * 目标已有同类流体时继续累加，多余部分由调用方处理）。
	 */
	public static FluidStack gatherFluid(Level level, BlockPos center, int radius, FluidStack current, int maxMb,
		boolean simulate, Predicate<BlockPos> skip) {
		FluidStack fluid = current == null || current.isEmpty() ? FluidStack.EMPTY : current.copy();
		if (level == null || center == null)
			return fluid;
		int r = Math.max(1, radius);
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			if (pos.equals(center))
				continue;
			if (skip != null && skip.test(pos))
				continue;
			IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
			if (tank == null)
				continue;
			for (int i = 0; i < tank.getTanks(); i++) {
				FluidStack fs = tank.getFluidInTank(i);
				if (fs.isEmpty())
					continue;
				if (!fluid.isEmpty() && fluid.getFluid() != fs.getFluid())
					continue;
				int want = maxMb - fluid.getAmount();
				if (want <= 0)
					break;
				FluidStack drained = tank.drain(new FluidStack(fs.getFluid(), want),
					simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
				if (drained.isEmpty())
					continue;
				if (fluid.isEmpty())
					fluid = drained.copy();
				else
					fluid.grow(drained.getAmount());
				if (fluid.getAmount() >= maxMb)
					break;
			}
			if (fluid.getAmount() >= maxMb)
				break;
		}
		return fluid;
	}

	/**
	 * 电量取料：抽干四周<b>可抽取</b>的储能；CC&amp;A 特斯拉线圈对外是"纯输入"，真抽时走其内部接口全抽。
	 *
	 * @return 本次取到的 FE（{@code simulate=true} 时只估算，不改动任何储能）
	 */
	public static int gatherEnergy(Level level, BlockPos center, int radius, boolean simulate,
		Predicate<BlockPos> skip) {
		if (level == null || center == null)
			return 0;
		int energy = 0;
		int r = Math.max(1, radius);
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			if (pos.equals(center))
				continue;
			if (skip != null && skip.test(pos))
				continue;
			IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
			if (storage != null && storage.canExtract()) {
				energy += storage.extractEnergy(Integer.MAX_VALUE, simulate);
			} else if (!simulate) {
				// 特斯拉线圈（CC&A）对外 canExtract=false：走其内部接口全抽（未装 CC&A 时静默 0）
				energy += com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveMachineIntegrations
					.drainTeslaCoilFully(level, pos);
			} else if (storage != null) {
				energy += storage.getEnergyStored(); // 估算：纯输入储能按当前储量计
			}
		}
		return energy;
	}

	/** 载荷物品总量（count 和）。 */
	public static int totalItems(List<ItemStack> list) {
		int n = 0;
		for (ItemStack s : list)
			n += s.getCount();
		return n;
	}

	/** 追加 1 个物品：同种（物品+组件）并入已有条目，否则新增条目。 */
	public static void insertItem(List<ItemStack> list, ItemStack one) {
		for (ItemStack s : list) {
			if (ItemStack.isSameItemSameComponents(s, one)) {
				s.grow(one.getCount());
				return;
			}
		}
		list.add(one.copy());
	}

	/** 该物品（按物品+组件）是否已经在列表里。 */
	private static boolean hasKind(List<ItemStack> list, ItemStack stack) {
		for (ItemStack carried : list)
			if (ItemStack.isSameItemSameComponents(carried, stack))
				return true;
		return false;
	}

	private static ResourceLocation itemId(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem());
	}
}
