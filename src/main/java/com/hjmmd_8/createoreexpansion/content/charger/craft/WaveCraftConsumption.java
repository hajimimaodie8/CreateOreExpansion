package com.hjmmd_8.createoreexpansion.content.charger.craft;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveCraftResults.DebugLog;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.AuxRef;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.AuxSource;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.EnergyDraw;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.EnergySource;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.FluidRef;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.FluidSource;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 变体波的<b>资源扣减</b>（"消耗侧"，与 {@link WaveCandidateEvaluator} 的"门槛侧"严格对称）：
 * 一次加工确定要消耗什么之后，按来源把辅料 / 流体 / 电量真正扣掉。
 *
 * <p><b>为什么与门槛分开</b>：门槛阶段必须零副作用（估算通过才消耗），扣减阶段必须真扣；
 * 两者的口径（同一来源、同一数量、同样按"已认领量"记账）由 {@link Candidate} 里的引用记录
 * 保证一致——推断出的量与扣减的量来自同一份 {@link WaveResources} 引用。</p>
 *
 * <p><b>状态由 {@link Host} 提供</b>：载荷是<b>可变实体状态</b>（列表就地增删、流体就地 shrink、
 * 电量整数改写），本类不持有它，只通过 Host 读写。Host 给的是<b>实体内部那个对象本身</b>
 * （不是 {@code StellarWaveEntity#getPayloadItems()} 那种给 Jade 看的只读副本）。</p>
 *
 * <p><b>并发缺口回流</b>（2026-09 口径）：门槛解析与实际扣减之间容器可能变了——流体少扣、
 * 邻域少取电，都<b>由载荷补齐缺口</b>，避免"门槛过了却没真扣到"的白嫖加工。</p>
 */
public final class WaveCraftConsumption {

	/** 扣减所需的实体侧状态（实现方须返回<b>实体内部对象本身</b>，不可返回副本）。 */
	public interface Host {

		/** 载荷物品列表（实体内部那只可变列表；实现方就地对它增删）。 */
		List<ItemStack> livePayloadItems();

		/** 载荷流体（实体内部字段本身，可安全就地 {@code shrink}；可能为 {@link FluidStack#EMPTY}）。 */
		FluidStack livePayloadFluid();

		/** 覆写载荷流体（不可为 null；用 {@link FluidStack#EMPTY} 表示无）。 */
		void setPayloadFluid(FluidStack fluid);

		/** 载荷剩余电量（FE）。 */
		int payloadEnergyValue();

		/** 覆写载荷电量（FE）。 */
		void setPayloadEnergy(int energy);

		/** 辅料解析器（用于"邻域真取电"）。 */
		WaveAuxResolver auxResolver();
	}

	private final Host host;
	private final DebugLog debug;

	public WaveCraftConsumption(Host host, DebugLog debug) {
		this.host = host;
		this.debug = debug;
	}

	/**
	 * 消耗一次加工的资源（辅料/流体/电量），掉落物路径与方块槽路径共用。
	 *
	 * @param handler        命中容器物品能力（扣减 CONTAINER 来源辅料用；掉落物路径传 null）
	 * @param containerFluid 命中容器流体能力（扣减 CONTAINER 来源流体用；掉落物路径/无流体能力传 null）
	 */
	public void consumeForCraft(Candidate candidate, IItemHandler handler, IFluidHandler containerFluid) {
		consumeAux(candidate, handler);
		consumeCraftFluid(candidate, containerFluid);
		consumeCraftEnergy(candidate);
	}

	/**
	 * 按来源扣减电量需求（"电量是一种特殊辅料"的扣减侧）：
	 * <ul>
	 *   <li>{@code NEARBY} → 从该方块位置真取（通用储能 {@code extractEnergy(need,false)} /
	 *       线圈内部按需扣）；</li>
	 *   <li>{@code PAYLOAD} → 从载荷电量扣。</li>
	 * </ul>
	 * 邻域取电若因并发变化少于计划量（估算与实际不符），缺口<b>退回载荷电量补齐</b>，
	 * 避免"门槛过了却没真扣到电"的白嫖加工；载荷也不够时按尽力而为扣到 0（估算阶段已排除该情形）。
	 */
	private void consumeCraftEnergy(Candidate candidate) {
		if (candidate.energies.isEmpty())
			return;
		// 直接用自守卫的 craftDebug（不要写 if (CRAFT_DEBUG)：常量 false 会让 javac 整块消除，
		// 打开开关后必须重新编译才生效，容易误导排查）
		debug.log("电量扣减：本配方合计 {} FE，来源 {}（载荷余量 {} FE）", candidate.energyRequired(),
			WaveAuxResolver.describeEnergies(candidate.energies), host.payloadEnergyValue());
		for (EnergyDraw draw : candidate.energies) {
			int amount = draw.amount();
			if (amount <= 0)
				continue;
			if (draw.source() == EnergySource.PAYLOAD) {
				host.setPayloadEnergy(Math.max(0, host.payloadEnergyValue() - amount));
				continue;
			}
			int taken = host.auxResolver()
				.extractNearbyEnergy(draw.pos(), amount);
			if (taken < amount) {
				int shortfall = amount - taken;
				host.setPayloadEnergy(Math.max(0, host.payloadEnergyValue() - shortfall));
				debug.log("电量扣减：邻域 {} 仅取到 {} / {} FE，缺口 {} FE 转由载荷电量补",
					draw.pos() == null ? "?" : draw.pos()
						.toShortString(),
					taken, amount, shortfall);
			}
		}
	}

	/**
	 * 按来源扣减流体输入：
	 * <ul>
	 *   <li>{@code CONTAINER} → {@code containerFluid.drain(该流体×需求量, EXECUTE)}
	 *       （按种类+组件取，跨罐由能力实现自行分配）；容器缺失/抽空则安全跳过；</li>
	 *   <li>{@code PAYLOAD} → 从载荷流体扣该量（多条载荷流体按顺序累计扣减）。</li>
	 * </ul>
	 *
	 * <p><b>并发缺口回流补齐（2026-09，与电量侧同口径）</b>：门槛解析与实际扣减之间容器可能变了
	 * （同 tick 内别的机器抽走了盆里的水、或能力实现按罐限流只给一半），此时若"少扣流体却照常出产物"
	 * 就是白嫖加工。所以缺口 {Amount − drained} 由<b>载荷流体补扣</b>——与
	 * {@link #consumeCraftEnergy} 的"邻域取电不足 → 缺口转由载荷电量补"完全对称。</p>
	 */
	private void consumeCraftFluid(Candidate candidate, IFluidHandler containerFluid) {
		for (FluidRef ref : candidate.fluids) {
			int amount = ref.fluid()
				.getAmount();
			if (amount <= 0)
				continue;
			if (ref.source() == FluidSource.CONTAINER) {
				if (containerFluid == null)
					continue;
				FluidStack drained = containerFluid.drain(ref.fluid()
					.copy(), IFluidHandler.FluidAction.EXECUTE);
				int shortfall = amount - drained.getAmount();
				if (shortfall > 0) {
					int covered = Math.min(shortfall, Math.max(0, host.livePayloadFluid()
						.getAmount()));
					if (covered > 0) {
						if (host.livePayloadFluid()
							.getAmount() <= covered)
							host.setPayloadFluid(FluidStack.EMPTY);
						else
							host.livePayloadFluid()
								.shrink(covered);
					}
					debug.log("流体扣减：容器仅抽出 {} mB / 需 {} mB，缺口 {} mB 转由载荷流体补（实补 {} mB）",
						drained.getAmount(), amount, shortfall, covered);
				}
			} else {
				if (host.livePayloadFluid()
					.getAmount() <= amount)
					host.setPayloadFluid(FluidStack.EMPTY);
				else
					host.livePayloadFluid()
						.shrink(amount);
			}
		}
	}

	/**
	 * 逐个消耗候选的全部辅料（每个 ingredient 扣 1 件），<b>按来源分别扣</b>：
	 * <ul>
	 *   <li>{@code PAYLOAD} → 从载荷物品扣 1；扣空即 {@code remove(idx)}，
	 *       会让<b>更大</b>的下标前移，故载荷下标先收集再<b>降序</b>处理
	 *       （排序只在同类来源内比较，容器槽号不参与，槽号扣减本身不引起下标漂移）；</li>
	 *   <li>{@code CONTAINER} → {@code handler.extractItem(slot, 1, false)}（槽号互不干扰，无需排序）；
	 *       取空/越界/无容器（掉落物路径）一律安全跳过；</li>
	 * </ul>
	 *
	 * <p><b>手持物类配方（deployer / ManualApplication）默认照样扣</b>（2026-09-11 修正，用户实测反馈）：
	 * 实物机械手确实不消耗手持物（一台机械手夹着一个齿轮能盖很久），但<b>波不是机械手</b>——它没有"手"，
	 * 每一击都得把辅料从容器/载荷里<b>物化</b>出来，所以必须真扣。旧实现直接跳过整条辅料扣减，
	 * 后果是：波抓着一份载荷辅料反复盖章，箱子里的原料永远不少（用户："直接也不从箱子里边抽物品，
	 * 就逮着一个…往机器里边加工"；也是更早那句"消耗了一份产物进行加工后，原材料却没有被消耗"的根因）。
	 * 需要回到"手持物免费"的旧口径时把配置 {@code wave.consumeHeldItemAux} 设为 false。</p>
	 *
	 * @param handler 命中容器（CONTAINER 来源必需；null 时该来源静默跳过）
	 */
	private void consumeAux(Candidate candidate, IItemHandler handler) {
		if (candidate.auxes.isEmpty())
			return;
		if (isHeldItemRecipe(candidate.recipe)
			&& !com.hjmmd_8.createoreexpansion.common.AllConfig.waveConsumeHeldItemAux) {
			debug.log("辅料扣减：{} 属手持物类配方且 wave.consumeHeldItemAux=false → 辅料不消耗",
				candidate.id);
			return;
		}
		// 通道 A：载荷条目（降序扣减，防 remove 引起下标错位）
		List<ItemStack> payloadItems = host.livePayloadItems();
		List<Integer> payloadOrder = new ArrayList<>(2);
		for (AuxRef ref : candidate.auxes)
			if (ref.source() == AuxSource.PAYLOAD)
				payloadOrder.add(ref.index());
		payloadOrder.sort(null); // 自然升序；下面倒序遍历 = 降序处理
		for (int k = payloadOrder.size() - 1; k >= 0; k--) {
			int idx = payloadOrder.get(k);
			if (idx < 0 || idx >= payloadItems.size())
				continue; // 下标越界（并发变化）：跳过该件，不让异常扩散
			ItemStack aux = payloadItems.get(idx);
			aux.shrink(1);
			if (aux.isEmpty())
				payloadItems.remove(idx);
		}
		// 通道 B：命中容器槽（逐槽真取 1；取空表示槽已变，跳过）
		if (handler == null)
			return;
		for (AuxRef ref : candidate.auxes) {
			if (ref.source() != AuxSource.CONTAINER)
				continue;
			int slot = ref.index();
			if (slot < 0 || slot >= handler.getSlots())
				continue;
			ItemStack taken = handler.extractItem(slot, 1, false);
			if (taken.isEmpty())
				debug.log("辅料扣减：容器槽 {} 已空（并发变化），跳过", slot);
		}
	}

	/** 是否"手持物不消耗"类配方（deployer 手部/机械手类，类名兜底判定）。 */
	private static boolean isHeldItemRecipe(Recipe<?> recipe) {
		String name = recipe.getClass()
			.getName();
		return name.contains("ManualApplicationRecipe") || name.contains("DeployerApplicationRecipe");
	}
}
