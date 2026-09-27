package com.hjmmd_8.createoreexpansion.content.charger.craft;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 变体波的<b>产物落点</b>：一次加工算出的产物该放回哪里、放不下时怎么退让。
 *
 * <p>与"载荷余料处置"（{@code payload/WavePayloadRelease}）刻意分开：余料要考虑
 * "排除口径"（工作盆/加工机不是存放目标），而<b>产物是玩家要的东西</b>，
 * 直接就近投放即可——命中容器自身优先，其次 1 格邻域，最后掉落/浪费。</p>
 *
 * <p>只读世界、只写目标容器，不持有任何状态；波的位置由调用方传入（{@code center}）。</p>
 */
public final class WaveOutputPlacer {

	private WaveOutputPlacer() {
	}

	/** 方块上方掉落（产物放不下时）。 */
	public static void dropAtBlock(Level level, BlockPos pos, ItemStack stack) {
		ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, stack);
		drop.setDeltaMovement(Vec3.ZERO);
		level.addFreshEntity(drop);
	}

	/**
	 * 尝试把物品插回槽位；<b>首选槽放不下时扫描整个容器</b>找空位/可叠加槽；仍失败返回剩余（调用方掉落）。
	 *
	 * <p>为什么必须扫整容器：主料槽里往往还剩着一堆原料（典型：一个槽里 64 个原木），
	 * 而产物与原料<b>不是同种物品</b>（去皮原木 ≠ 原木），{@code insertItem} 到该槽必然失败。
	 * 旧实现只试这一个槽 → 产物被直接掉到地上，玩家看到"加工出来的东西爆了一地"，
	 * 更糟的是<b>连锁加工因此断掉</b>——下一步（去皮原木 + 铜锭 → 铜机壳）需要在盆里
	 * 找到上一步的产物，而它已经不在盆里了（用户 2026-09 实测反馈）。</p>
	 */
	public static ItemStack insertBack(IItemHandler handler, int slot, ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return ItemStack.EMPTY;
		ItemStack rest = handler.insertItem(slot, stack, false);
		if (rest.isEmpty())
			return ItemStack.EMPTY;
		for (int i = 0; i < handler.getSlots(); i++) {
			if (i == slot)
				continue;
			rest = handler.insertItem(i, rest, false);
			if (rest.isEmpty())
				return ItemStack.EMPTY;
		}
		return rest;
	}

	/** 优先注入命中方块自身的流体槽；未耗尽部分返回。 */
	public static FluidStack fillBlock(Level level, BlockPos pos, FluidStack stack) {
		IFluidHandler self = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
		if (self != null) {
			int done = self.fill(new FluidStack(stack.getFluid(), stack.getAmount()),
				IFluidHandler.FluidAction.EXECUTE);
			if (done >= stack.getAmount())
				return FluidStack.EMPTY;
			stack.shrink(done);
		}
		return stack;
	}

	/** 就近（中心 1 格邻域）注罐：剩余无处可存则浪费。 */
	public static void fillNearby(Level level, BlockPos center, FluidStack stack) {
		int amount = stack.getAmount();
		for (BlockPos bp : radiusBlocks(center)) {
			IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, bp, null);
			if (handler == null)
				continue;
			amount -= handler.fill(new FluidStack(stack.getFluid(), amount), IFluidHandler.FluidAction.EXECUTE);
			if (amount <= 0)
				return;
		}
	}

	/** 中心周围 1 格邻域（不含自身所在格）；产物流体"就近注罐"与载荷兜底共用。 */
	public static List<BlockPos> radiusBlocks(BlockPos center) {
		List<BlockPos> list = new ArrayList<>();
		for (int dx = -1; dx <= 1; dx++)
			for (int dy = -1; dy <= 1; dy++)
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos bp = center.offset(dx, dy, dz);
					if (!bp.equals(center))
						list.add(bp);
				}
		return list;
	}

	/** 产物流体"就近注罐"：中心 1 格邻域逐个填，剩余无处可存即浪费（产物不需要"排除口径"）。 */
	public static void fillNearestTank(Level level, BlockPos center, FluidStack stack) {
		int amount = stack.getAmount();
		for (BlockPos bp : radiusBlocks(center)) {
			IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, bp, null);
			if (handler == null)
				continue;
			try {
				amount -= handler.fill(new FluidStack(stack.getFluid(), amount), IFluidHandler.FluidAction.EXECUTE);
			} catch (Throwable ignored) {
				// 单个储罐异常：换下一个（2026-09 审计修复：不让第三方能力异常冒到 tick）
			}
			if (amount <= 0)
				return;
		}
	}

	/** 产物电量"就近注入"：中心 1 格邻域逐个充，剩余无处可存即浪费。 */
	public static void chargeNearest(Level level, BlockPos center, int fe) {
		int left = fe;
		for (BlockPos bp : radiusBlocks(center)) {
			IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, bp, null);
			if (storage == null || !storage.canReceive())
				continue;
			try {
				left -= storage.receiveEnergy(left, false);
			} catch (Throwable ignored) {
				// 单个储能异常：换下一个
			}
			if (left <= 0)
				return;
		}
	}
}
