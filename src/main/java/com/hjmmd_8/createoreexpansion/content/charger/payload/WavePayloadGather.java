package com.hjmmd_8.createoreexpansion.content.charger.payload;

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
 *   <li><b>波</b>：命中目标后做"就地补料"（{@code StellarWaveEntity#refillPayloadAround}，
 *       默认<b>关闭</b>：配置 {@code wave.refillPayloadOnHit}），范围 = 变器当时的扫描半径。</li>
 * </ol>
 *
 * <h2>⚠ 设计铁律：取料是"<b>傻抽</b>"，不许做需求匹配（用户 2026-09 明确，勿"优化"）</h2>
 *
 * <p>变器处理之后的波，吸收辅料/流体/电量的行为<b>本身就是非常傻的</b>：
 * <b>周围有多少就抽多少，一直到抽饱（抽到上限）为止</b>——
 * 它<b>不会</b>根据"这次要做什么加工"来决定抽什么、抽几个。</p>
 *
 * <p>所以以下"智能化"改动一律<b>不要做</b>（都是用户明确否决过的方向）：</p>
 * <ul>
 *   <li>❌ 按当前配方需要挑选辅料 / 只抽"用得上的那几种"；</li>
 *   <li>❌ 抽之前先判断"有没有活干""值不值得抽"；</li>
 *   <li>❌ 按配方的辅料数量精确抽（例如"这个配方只要 1 个齿轮就只抽 1 个"）。</li>
 * </ul>
 * <p>唯一的"规则"就是容量与顺序（见下），以及"不抽工作盆/正在加工的那个方块/动能机器内部库存"
 * 这条排除口径。</p>
 *
 * <p><b>容量（上限；数值在配置里，改配置即可，勿写死）</b>：
 * {@code wave.maxPayloadItems}（默认 {@value #MAX_ITEMS} 件）
 * / {@code wave.maxPayloadKinds}（默认 {@value #MAX_KINDS} 种）
 * / {@code wave.maxPayloadFluidMb}（默认 {@value #MAX_FLUID_MB} mB）；电量 = 抽干范围内<b>可抽取</b>的储能。</p>
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
 *
 * <p><b>取料来源回执</b>：真抽时会顺带把"被抽过的容器/储罐/储能位置"记进 {@code sources}——
 * 载荷消散时按配置 {@code wave.payloadRelease} 处置余料（默认存进击中方块周围最近的容器；
 * 也可退回这些来源容器）。</p>
 */
public final class WavePayloadGather {

	/**
	 * 载荷物品总量上限的<b>默认值</b>（个）——实际取值见配置 {@code wave.maxPayloadItems}，
	 * 调用方（变器 / 波的命中后补料）从 {@code AllConfig} 读当前值传进来（两处口径必须一致）。
	 */
	public static final int MAX_ITEMS = 5;
	/** 载荷物品种类上限的<b>默认值</b>（种）——实际取值见配置 {@code wave.maxPayloadKinds}。 */
	public static final int MAX_KINDS = 5;
	/** 载荷流体上限的<b>默认值</b>（mB）——实际取值见配置 {@code wave.maxPayloadFluidMb}（默认 2 B）。 */
	public static final int MAX_FLUID_MB = 2000;

	/** 一个候选槽（容器 + 槽号 + 容器位置；同一容器可能出现多次）。 */
	private record SlotRef(IItemHandler handler, int slot, BlockPos pos) {
	}

	private WavePayloadGather() {
	}

	/**
	 * 物品取料：向 {@code into} 追加物品（同种并入已有条目），直到 {@code into} 达到
	 * {@code maxItems} 个 / {@code maxKinds} 种或四周没得抽为止。
	 *
	 * @param sources 真抽时记录<b>实际被抽过的容器位置</b>（去重、按取料先后追加）；
	 *                载荷消散时据此把剩余物<b>还回取料容器</b>，而不是丢进正在加工的容器。
	 *                估算（{@code simulate=true}）不改变任何容器，故不记录。
	 * @param skip    该位置是否跳过（加工机 / 玩家正在加工的容器 / 命中点自身等，由调用方定义）
	 */
	public static void gatherItems(Level level, BlockPos center, int radius, List<ItemStack> into,
		List<BlockPos> sources, int maxItems, int maxKinds, boolean simulate, Predicate<BlockPos> skip) {
		if (level == null || center == null)
			return;
		List<SlotRef> slots = new ArrayList<>();
		int r = Math.max(1, radius);
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			if (pos.equals(center))
				continue;
			if (!level.isLoaded(pos))
				continue; // 未加载区块：capability 查询会触发同步加载（2026-09 审计修复）
			if (skip != null && skip.test(pos))
				continue;
			IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (handler == null)
				continue;
			BlockPos immutable = pos.immutable();
			for (int slot = 0; slot < handler.getSlots(); slot++) {
				ItemStack stack = handler.getStackInSlot(slot);
				if (stack == null || stack.isEmpty())
					continue;
				slots.add(new SlotRef(handler, slot, immutable));
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
			if (!one.isEmpty()) {
				insertItem(into, one);
				if (!simulate)
					recordSource(sources, ref.pos());
			}
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
			if (!simulate)
				recordSource(sources, target.pos());
			remaining.put(bestId, bestCount - 1); // 估算模式靠它收敛；真抽模式与容器扣减同步
		}
	}

	/**
	 * 流体取料：只取<b>第一种</b>流体并抽到 {@code maxMb} 为止（与变器同口径；
	 * 目标已有同类流体时继续累加，多余部分由调用方处理）。
	 *
	 * @param sources 真抽时记录实际被抽过的储罐位置（载荷消散时优先还回这些罐）
	 */
	public static FluidStack gatherFluid(Level level, BlockPos center, int radius, FluidStack current,
		List<BlockPos> sources, int maxMb, boolean simulate, Predicate<BlockPos> skip) {
		FluidStack fluid = current == null || current.isEmpty() ? FluidStack.EMPTY : current.copy();
		if (level == null || center == null)
			return fluid;
		int r = Math.max(1, radius);
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			if (pos.equals(center))
				continue;
			if (!level.isLoaded(pos))
				continue; // 未加载区块：capability 查询会触发同步加载
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
				if (!simulate)
					recordSource(sources, pos);
				if (fluid.getAmount() >= maxMb)
					break;
			}
			if (fluid.getAmount() >= maxMb)
				break;
		}
		return fluid;
	}

	/**
	 * 电量取料：在半径内取四周<b>可抽取</b>的储能，<b>抽到上限为止</b>；CC&amp;A 特斯拉线圈对外是
	 * "纯输入"，真抽时走其内部接口全抽（按剩余额度截断）。
	 *
	 * @param sources 真抽时记录实际被抽过的储能位置（载荷消散时优先还回这些储能）
	 * @param maxFe   电量上限（FE）：{@code < 0} = 不设上限（旧行为：抽干）；
	 *                {@code >= 0} = 只取到这么多为止。调用方用 {@link #resolveEnergyCap(Level)}
	 *                解析配置/CC&amp;A 口径后传入。
	 * @return 本次取到的 FE（{@code simulate=true} 时只估算，不改动任何储能）
	 */
	public static int gatherEnergy(Level level, BlockPos center, int radius, List<BlockPos> sources,
		boolean simulate, Predicate<BlockPos> skip, int maxFe) {
		if (level == null || center == null)
			return 0;
		int energy = 0;
		int r = Math.max(1, radius);
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			int room = roomLeft(energy, maxFe);
			if (room == 0)
				break; // 已取满
			if (pos.equals(center))
				continue;
			if (!level.isLoaded(pos))
				continue; // 未加载区块：capability 查询会触发同步加载
			if (skip != null && skip.test(pos))
				continue;
			IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
			if (storage != null && storage.canExtract()) {
				int got = storage.extractEnergy(room < 0 ? Integer.MAX_VALUE : room, simulate);
				energy += got;
				if (!simulate && got > 0)
					recordSource(sources, pos);
			} else if (!simulate) {
				// 特斯拉线圈（CC&A）对外 canExtract=false：走其内部接口抽取；有上限时按剩余额度截断
				// （consumeTeslaCoil 内部自带钳制，max = Integer.MAX_VALUE 等价于旧的"全抽"）
				int got = com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveMachineIntegrations
					.consumeTeslaCoil(level, pos, room < 0 ? Integer.MAX_VALUE : room);
				energy += got;
				if (got > 0)
					recordSource(sources, pos);
			} else if (storage != null) {
				// 估算：纯输入储能按当前储量计（同样受上限截断）
				int stored = storage.getEnergyStored();
				energy += room < 0 ? stored : Math.min(stored, room);
			}
		}
		return energy;
	}

	/** 还能取多少：{@code maxFe < 0} 返回 -1（不设上限），取满返回 0。 */
	private static int roomLeft(int got, int maxFe) {
		if (maxFe < 0)
			return -1;
		return Math.max(0, maxFe - got);
	}

	/**
	 * <b>电量载荷上限的解析</b>（用户 2026-09 口径）：配置 {@code wave.maxPayloadEnergyFe}
	 * 给了非负值就照它；默认 {@code -1}（自动）= <b>CC&amp;A 充电配方里最贵那条的耗电量</b>
	 * （{@code CreateAdditionTransmuterSupport#maxChargingEnergy}）。
	 *
	 * <p>取不到（未装 CC&amp;A / 没有此类配方）时返回 {@code -1} = <b>不设上限</b>，
	 * 保持"抽干可抽取储能"的旧行为，避免因为没装 CC&amp;A 就把电量载荷清零。</p>
	 */
	public static int resolveEnergyCap(Level level) {
		int configured = com.hjmmd_8.createoreexpansion.common.AllConfig.waveMaxPayloadEnergyFe;
		if (configured >= 0)
			return configured;
		int caMax = com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveMachineIntegrations
			.maxStrikeChargingEnergyFe(level);
		return caMax > 0 ? caMax : -1;
	}

	/** 记录一个"取料来源"方块位置（去重、保序；null 列表 = 不记录）。 */
	private static void recordSource(List<BlockPos> sources, BlockPos pos) {
		if (sources == null || pos == null)
			return;
		for (BlockPos p : sources)
			if (p.equals(pos))
				return;
		sources.add(pos.immutable());
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
