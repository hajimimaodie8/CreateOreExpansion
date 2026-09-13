package com.hjmmd_8.createoreexpansion.content.charger.craft;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.AuxRef;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.AuxSource;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.EnergyDraw;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.EnergySource;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.FluidRef;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.FluidSource;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveMachineIntegrations;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 变体波的<b>辅料解析器</b>：把配方的三类输入（物品辅料 / 流体 / 电量）统一解析成"来源引用"。
 *
 * <p>三类辅料共用同一副骨架——<b>就近优先、载荷兜底</b>：</p>
 * <ol>
 *   <li>物品：命中容器自身的其它槽 → 波载荷物品（{@link #findAux}）；</li>
 *   <li>流体：命中容器的流体槽 → 波载荷流体（{@link #resolveFluidRefs}）；</li>
 *   <li>电量：命中点邻域储能 → 波载荷电量（{@link #resolveEnergyDraws}）。</li>
 * </ol>
 *
 * <p><b>载荷按引用读取</b>：{@link #payloadItems} 持的是调用方列表本身的引用（列表内容由调用方
 * 就地增删，故始终是最新视图），{@link #payloadFluid}／{@link #payloadEnergy} 是构造时的值快照，
 * 因此调用方应在<b>每次取料前现场构造</b>一个实例，不要跨载荷变更缓存复用。</p>
 *
 * <p><b>估算阶段零副作用</b>：本类所有解析/估算方法只读不写（模拟抽取、只读反射），
 * 真扣取料一律由调用方在确认配方可执行后调用 {@link #extractNearbyEnergy} 或载荷侧自行完成。</p>
 */
public final class WaveAuxResolver {

	/** 环境扫描立方体半径（3×3×3，含中心格）：与"加热烈焰人 / 压弯机头 / fan 媒介"同一口径。 */
	public static final int ENV_RADIUS = 1;

	private final Level level;
	private final List<ItemStack> payloadItems;
	private final FluidStack payloadFluid;
	private final int payloadEnergy;

	public WaveAuxResolver(Level level, List<ItemStack> payloadItems, FluidStack payloadFluid, int payloadEnergy) {
		this.level = level;
		this.payloadItems = payloadItems == null ? List.of() : payloadItems;
		this.payloadFluid = payloadFluid == null ? FluidStack.EMPTY : payloadFluid;
		this.payloadEnergy = payloadEnergy;
	}

	// ================= 物品辅料双通道（容器槽优先 → 载荷兜底） =================

	/**
	 * 找到能命中给定 Ingredient 的辅料（按<b>双通道优先级</b>；找不到返回 null）。
	 *
	 * <p><b>搜索顺序（2026-09 反转）</b>：先通道 B <b>命中容器自身的其它槽</b>
	 * （{@code handler != null} 时；排除主料槽 {@code mainSlot}），再通道 A 波载荷
	 * {@link #payloadItems}。语义：玩家把原料摆在盆/置物台里时就用盆里的，
	 * <b>绝不会</b>去动扫描圈箱子的载荷物品；只有容器里凑不齐辅料才回退用载荷。
	 * （掉落物路径无容器 → 仍然只有载荷可用。）</p>
	 *
	 * <p><b>去重</b>：{@code occupied} 已收录本次候选已占用的辅料引用（容器槽号 + 载荷下标两套），
	 * 同一个容器槽 / 同一条载荷条目不会被两个 ingredient 重复占用——否则多输入配方可用 1 件辅料
	 * 冒充多件，门槛形同虚设。（同一物品类型的两个不同槽仍可分别充当两个 ingredient，
	 * 这在实际场景里是必要的，故按"槽/条目"而非按"物品种类"去重。）</p>
	 *
	 * @param occupied 本次候选已占用的辅料引用集合（可为空表，不可为 null）
	 */
	public AuxRef findAux(Ingredient ingredient, List<AuxRef> occupied, IItemHandler handler, int mainSlot) {
		// 通道 B（优先）：命中容器自身的其它槽（排除主料槽与已占用槽）
		if (handler != null) {
			for (int slot = 0; slot < handler.getSlots(); slot++) {
				if (slot == mainSlot)
					continue;
				if (isOccupied(occupied, AuxSource.CONTAINER, slot))
					continue;
				ItemStack stack = handler.getStackInSlot(slot);
				if (stack == null || stack.isEmpty())
					continue;
				if (ingredient.test(stack))
					return new AuxRef(AuxSource.CONTAINER, slot);
			}
		}
		// 通道 A（兜底）：波载荷
		for (int i = 0; i < payloadItems.size(); i++) {
			if (isOccupied(occupied, AuxSource.PAYLOAD, i))
				continue;
			ItemStack stack = payloadItems.get(i);
			if (!stack.isEmpty() && ingredient.test(stack))
				return new AuxRef(AuxSource.PAYLOAD, i);
		}
		return null;
	}

	/** 该来源+下标是否已被本次候选占用。 */
	public static boolean isOccupied(List<AuxRef> occupied, AuxSource source, int index) {
		for (AuxRef ref : occupied)
			if (ref.source() == source && ref.index() == index)
				return true;
		return false;
	}

	/**
	 * 按辅料引用取出<b>真实物品</b>（读操作，不改动来源）：
	 * {@code PAYLOAD} → 载荷条目；{@code CONTAINER} → 容器槽（索引自身即槽号）。
	 * 取不到（越界 / 空槽 / 无容器）一律返回 {@link ItemStack#EMPTY}，绝不抛异常。
	 * 容器实现可能返回活对象（vanilla {@code Container#getStackInSlot}），调用方只读不写。
	 */
	public ItemStack resolveAux(AuxRef ref, IItemHandler handler) {
		if (ref == null)
			return ItemStack.EMPTY;
		if (ref.source() == AuxSource.PAYLOAD) {
			int i = ref.index();
			return i >= 0 && i < payloadItems.size() ? payloadItems.get(i) : ItemStack.EMPTY;
		}
		if (handler == null)
			return ItemStack.EMPTY;
		int slot = ref.index();
		if (slot < 0 || slot >= handler.getSlots())
			return ItemStack.EMPTY;
		ItemStack stack = handler.getStackInSlot(slot);
		return stack == null ? ItemStack.EMPTY : stack;
	}

	// ================= 流体输入双通道（容器流体槽优先 → 载荷流体兜底） =================

	/**
	 * 解析配方的全部流体输入，每条独立选来源与需求量。
	 *
	 * <p><b>来源优先级（2026-09 反转）</b>：先命中容器的流体槽（工作盆的盆内流体），
	 * 不足才回退波载荷流体。语义与物品输入一致——玩家把水倒进盆里就该用盆里的水。</p>
	 *
	 * <p><b>防超领记账</b>：同一流体种类可能被多条 ingredient 引用（或容器有多个同种流体的罐），
	 * 故容器侧按"已认领量"累计后与实际可用量比较；载荷侧同理用 {@code payloadReserved}
	 * 累计。这样两条流体 ingredient 各自"独立通过"也不会合计超过实际存量。</p>
	 *
	 * @return 全部流体输入的来源清单；任一 unsatified 返回 null（调用方按不可执行淘汰）
	 */
	public List<FluidRef> resolveFluidRefs(ProcessingRecipe<?, ?> recipe, IFluidHandler containerFluid) {
		List<SizedFluidIngredient> needs = recipe.getFluidIngredients();
		if (needs.isEmpty())
			return List.of();
		List<FluidRef> refs = new ArrayList<>(needs.size());
		int payloadReserved = 0;
		for (SizedFluidIngredient need : needs) {
			int amount = need.amount();
			FluidRef ref = null;
			// 通道 B（优先）：命中容器的流体槽
			FluidStack tankType = firstMatchingTankFluid(containerFluid, need);
			if (!tankType.isEmpty()) {
				FluidStack demand = tankType.copyWithAmount(amount);
				int available = fluidAmountIn(containerFluid, demand) - claimedFluidAmount(refs, FluidSource.CONTAINER, demand);
				if (available >= amount)
					ref = new FluidRef(FluidSource.CONTAINER, demand);
			}
			// 通道 A（兜底）：波载荷流体
			if (ref == null && !payloadFluid.isEmpty() && need.test(payloadFluid)
				&& payloadFluid.getAmount() - payloadReserved >= amount) {
				ref = new FluidRef(FluidSource.PAYLOAD, payloadFluid.copyWithAmount(amount));
				payloadReserved += amount;
			}
			if (ref == null)
				return null;
			refs.add(ref);
		}
		return refs;
	}

	/** 容器流体槽里第一个满足该流体 ingredient 的流体（返回其真实种类+组件，数量置为需求量）；无则 EMPTY。 */
	public static FluidStack firstMatchingTankFluid(IFluidHandler fluids, SizedFluidIngredient need) {
		if (fluids == null)
			return FluidStack.EMPTY;
		try {
			for (int tank = 0; tank < fluids.getTanks(); tank++) {
				FluidStack in = fluids.getFluidInTank(tank);
				if (!in.isEmpty() && need.test(in))
					return in.copyWithAmount(need.amount());
			}
		} catch (Throwable ignored) {
			// 个别流体能力实现异常：按"无匹配流体"处理
		}
		return FluidStack.EMPTY;
	}

	/** 容器内所有罐中与 {@code want} 同种（含组件）的流体总量。 */
	public static int fluidAmountIn(IFluidHandler fluids, FluidStack want) {
		if (fluids == null || want == null || want.isEmpty())
			return 0;
		int total = 0;
		try {
			for (int tank = 0; tank < fluids.getTanks(); tank++) {
				FluidStack in = fluids.getFluidInTank(tank);
				if (!in.isEmpty() && FluidStack.isSameFluidSameComponents(in, want))
					total += in.getAmount();
			}
		} catch (Throwable ignored) {
			return 0;
		}
		return total;
	}

	/** 已解析的流体输入里，来自同一来源且同种（含组件）流体的已认领量。 */
	public static int claimedFluidAmount(List<FluidRef> refs, FluidSource source, FluidStack want) {
		int claimed = 0;
		for (FluidRef ref : refs)
			if (ref.source() == source && FluidStack.isSameFluidSameComponents(ref.fluid(), want))
				claimed += ref.fluid()
					.getAmount();
		return claimed;
	}

	// ================= 电量双通道（邻域储能优先 → 载荷电量兜底；用户："电量也是一种特殊辅料"） =================

	/**
	 * 解析配方电量需求的来源清单（<b>把电量当"特殊辅料"处理</b>，与物品/流体来源完全对称）：
	 * <ol>
	 *   <li><b>通道 1｜命中点邻域储能（优先）</b>：{@link #envBlocks}／{@link #ENV_RADIUS}（3×3×3，
	 *       与"加热烈焰人 / 压弯机头 / fan 媒介"同一口径）内的可抽储能，按扫描顺序<b>贪心取电</b>——
	 *       多个源可以合力凑够需求（"之和 ≥ 需求"）。两类源：
	 *       <ul>
	 *         <li>通用 {@code IEnergyStorage}（需 {@code canExtract()}）；</li>
	 *         <li>CC&amp;A <b>特斯拉线圈</b>——对外 {@code canExtract=false}（{@code getMaxOut()==0}），
	 *             只能走其内部储能（{@link StellarWaveMachineIntegrations#teslaCoilEnergy} 只读反射）。</li>
	 *       </ul>
	 *   </li>
	 *   <li><b>通道 2｜波载荷电量（兜底）</b>：{@link #payloadEnergy}（穿波瞬间从变器扫描圈抽来的）。</li>
	 * </ol>
	 *
	 * <p><b>估算阶段零副作用</b>：通用储能用 {@code extractEnergy(need, true)}（模拟）估，
	 * 线圈用只读的 {@code getEnergyStored()} 估；<b>绝不</b>在估算时调用
	 * {@code extractEnergy(need, false)} 或 {@code drainTeslaCoilFully}（会真扣电）。
	 * 不可抽且非线圈的储能（机器内部缓冲等）一律按 0 计——它们本来也拿不出来，
	 * 计进去会导致"门槛过了却抽不到电"的白嫖加工。</p>
	 *
	 * @return 电量来源清单；合计仍不足返回 {@code null}（候选淘汰，不消耗任何东西）
	 */
	public List<EnergyDraw> resolveEnergyDraws(int required, BlockPos around) {
		if (required <= 0)
			return List.of();
		List<EnergyDraw> draws = new ArrayList<>(2);
		int remaining = required;
		if (around != null) {
			for (BlockPos bp : envBlocks(around)) {
				if (remaining <= 0)
					break;
				int available = availableEnergyAt(bp, remaining);
				if (available <= 0)
					continue;
				int take = Math.min(available, remaining);
				// betweenClosed 会复用可变游标，必须 immutable() 后长期持有
				draws.add(new EnergyDraw(EnergySource.NEARBY, take, bp.immutable()));
				remaining -= take;
			}
		}
		if (remaining > 0 && payloadEnergy > 0) {
			int take = Math.min(payloadEnergy, remaining);
			draws.add(new EnergyDraw(EnergySource.PAYLOAD, take, null));
			remaining -= take;
		}
		return remaining <= 0 ? draws : null;
	}

	/**
	 * 估算某方块当前可供抽取的电量（<b>只读/模拟，绝不真扣</b>）：
	 * 通用可抽储能 → {@code extractEnergy(need, true)} 模拟（按 NeoForge 约定不真扣电，
	 * 且能尊重该储能的 {@code maxExtract} 限流），再与只读的 {@code getEnergyStored()} 取小，
	 * 双保险地避免"估算虚高 → 门槛过了却抽不到电"；非可抽者 → 仅当是 CC&amp;A 特斯拉线圈时
	 * 读其内部储能（只读反射）；其余（机器输入缓冲等不可抽储能）→ 0。
	 */
	public int availableEnergyAt(BlockPos pos, int need) {
		if (pos == null || need <= 0 || level == null || level.isClientSide)
			return 0;
		try {
			IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
			if (storage != null && storage.canExtract()) {
				int simulated = Math.max(0, storage.extractEnergy(need, true)); // simulate = true：不扣电
				int stored = Math.max(0, storage.getEnergyStored());
				return Math.min(simulated, stored);
			}
		} catch (Throwable ignored) {
			// 单个储能异常：按不可用处理
		}
		// 对外不可抽的储能：只认 CC&amp;A 特斯拉线圈（内部只读反射；非线圈/未安装返回 0）
		try {
			return StellarWaveMachineIntegrations.teslaCoilEnergy(level, pos);
		} catch (Throwable ignored) {
			return 0;
		}
	}

	/**
	 * 从指定位置真取电（消耗阶段）：通用可抽储能 → {@code extractEnergy(need, false)}；
	 * 否则走 CC&amp;A 线圈内部<b>按需</b>扣减（{@code internalConsumeEnergy} 自带钳制，取多少扣多少，
	 * 不会像"全抽"那样掏空线圈）。返回实取 FE。
	 */
	public int extractNearbyEnergy(BlockPos pos, int need) {
		if (pos == null || need <= 0 || level == null)
			return 0;
		try {
			IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
			if (storage != null && storage.canExtract()) {
				int got = storage.extractEnergy(need, false);
				if (got > 0)
					return got;
			}
		} catch (Throwable ignored) {
			// 单个储能异常：继续尝试线圈路径
		}
		try {
			return StellarWaveMachineIntegrations.consumeTeslaCoil(level, pos, need);
		} catch (Throwable ignored) {
			return 0;
		}
	}

	// ================= 环境扫描与诊断描述 =================

	/** 环境扫描立方体（3×3×3，含中心格）。 */
	public static Iterable<BlockPos> envBlocks(BlockPos center) {
		return BlockPos.betweenClosed(center.offset(-ENV_RADIUS, -ENV_RADIUS, -ENV_RADIUS),
			center.offset(ENV_RADIUS, ENV_RADIUS, ENV_RADIUS));
	}

	/** 流体输入清单的可读描述（诊断日志用）。 */
	public static String describeFluids(List<FluidRef> refs) {
		if (refs == null || refs.isEmpty())
			return "无";
		StringBuilder sb = new StringBuilder();
		for (FluidRef ref : refs) {
			if (sb.length() > 0)
				sb.append('+');
			sb.append(ref.source())
				.append('#')
				.append(ref.fluid()
					.getAmount())
				.append("mB")
				.append(ref.fluid()
					.getHoverName()
					.getString());
		}
		return sb.toString();
	}

	/** 电量来源清单的可读描述（诊断日志用）。 */
	public static String describeEnergies(List<EnergyDraw> draws) {
		if (draws == null || draws.isEmpty())
			return "无";
		StringBuilder sb = new StringBuilder();
		for (EnergyDraw draw : draws) {
			if (sb.length() > 0)
				sb.append('+');
			sb.append(draw.source())
				.append('#')
				.append(draw.amount())
				.append("FE");
			if (draw.pos() != null)
				sb.append('@')
					.append(draw.pos()
						.toShortString());
		}
		return sb.toString();
	}

	/** 辅料引用的简短可读描述（诊断日志用）。 */
	public static String describeAux(AuxRef ref) {
		return ref == null ? "无" : ref.source() + "#" + ref.index();
	}

	/** 辅料引用列表的可读描述（诊断日志用）。 */
	public static String describeAuxes(List<AuxRef> auxes) {
		if (auxes == null || auxes.isEmpty())
			return "无";
		StringBuilder sb = new StringBuilder();
		for (AuxRef ref : auxes) {
			if (sb.length() > 0)
				sb.append('+');
			sb.append(describeAux(ref));
		}
		return sb.toString();
	}
}
