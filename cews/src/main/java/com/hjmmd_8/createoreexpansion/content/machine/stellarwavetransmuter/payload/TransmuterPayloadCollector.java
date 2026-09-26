package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.payload;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.content.charger.payload.WavePayloadGather;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 星辉波变器的<b>载荷收集</b>：把扫描区容器的物品 / 流体 / 电量打包成"波可携带的载荷"。
 *
 * <p>取料行为<b>刻意很傻</b>（用户口径）：周围有多少就抽多少，直到抽到上限为止，
 * 不做任何"按加工特点智能挑选"的判断——上限与抽法是统一口径，实现在
 * {@link WavePayloadGather}（"傻抽"铁律写在那个类的注释里），本类只负责
 * "扫哪里、跳过什么、上限是多少、估算还是真抽"。</p>
 *
 * <p><b>估算与真抽分离</b>：{@link #estimate} 只做 simulate（不动容器，供护目镜摘要），
 * 真实抽取只在波穿过变器的瞬间由 {@link #gather} 做一次。</p>
 */
public final class TransmuterPayloadCollector {

	/**
	 * 一次载荷收集结果（物品 / 流体 / 电量 / <b>取料来源方块位置</b>）。
	 *
	 * <p>{@code sources} 用于"载荷消散时还回原容器"：波把剩余载荷还回这些位置，
	 * 而不是丢在消散点（消散点常常就是波正在加工的工作盆上方，会被盆吸进去）。</p>
	 */
	public record Payload(List<ItemStack> items, FluidStack fluid, int energy, List<BlockPos> sources) {
	}

	private TransmuterPayloadCollector() {
	}

	/**
	 * 扫描区可携带载荷的<b>估算</b>（不改变任何容器内容）：供护目镜摘要显示。
	 *
	 * <p><b>2026-09 行为修正</b>：原实现每 8 tick 就<b>真实抽取</b>一次扫描区容器
	 * （{@code extractItem/drain/extractEnergy} 全部实抽），导致玩家往箱子里放东西后
	 * 会被"漏斗式"连续抽空。现在扫描阶段只做 simulate 估算，真实抽取推迟到
	 * <b>波穿过变器的瞬间</b>（见 {@link #gather}）。</p>
	 *
	 * @param skip 取料跳过口径（加工机 / 玩家加工容器等），由调用方给出
	 */
	public static Payload estimate(Level level, BlockPos origin, int radius, Predicate<BlockPos> skip) {
		return collect(level, origin, radius, skip, true);
	}

	/**
	 * 穿波瞬间<b>真实抽取</b>扫描区载荷（上限：物品 / 种类 / 流体见 {@link #maxPayloadItems()} 一族，
	 * 电量上限由 {@link WavePayloadGather#resolveEnergyCap} 决定）。
	 * 由 {@code StellarWaveTransmuterPass} 在把波转成变体波时调用一次。
	 */
	public static Payload gather(Level level, BlockPos origin, int radius, Predicate<BlockPos> skip) {
		return collect(level, origin, radius, skip, false);
	}

	/**
	 * 扫描区内<b>非加工机、非玩家加工容器</b>的载荷收集。
	 *
	 * <p>取料顺序与"波命中后就地补料"共用同一套实现（{@link WavePayloadGather}）：
	 * <b>先每种各 1（铺开种类），再按存量补数量</b>；流体取第一种抽到上限；电量抽干可抽取储能
	 * （特斯拉线圈走内部接口）。</p>
	 *
	 * @param simulate true = 只估算（不动容器）；false = 真实抽取
	 */
	private static Payload collect(Level level, BlockPos origin, int radius, Predicate<BlockPos> skip,
		boolean simulate) {
		List<ItemStack> items = new ArrayList<>();
		List<BlockPos> sources = new ArrayList<>();
		int r = Math.max(1, radius);
		WavePayloadGather.gatherItems(level, origin, r, items, sources, maxPayloadItems(), maxPayloadTypes(),
			simulate, skip);
		FluidStack fluid = WavePayloadGather.gatherFluid(level, origin, r, FluidStack.EMPTY, sources,
			maxPayloadFluid(), simulate, skip);
		// 电量上限：配置固定值，或（默认）CC&A 充电配方里最贵那条的耗电量——见 WavePayloadGather#resolveEnergyCap
		int energy = WavePayloadGather.gatherEnergy(level, origin, r, sources, simulate, skip,
			WavePayloadGather.resolveEnergyCap(level));
		return new Payload(items, fluid, energy, sources);
	}

	/**
	 * 该位置是否为"玩家正在加工的容器"（工作盆等）：这类容器虽然带物品/流体能力，
	 * <b>但不是载荷源</b>——盆里是玩家摆的原料与配方流体，波不该把它们当辅料抽走
	 * （2026-09 修正：旧实现把工作盆也当普通容器抽料，导致盆里的料会莫名少掉几个）。
	 *
	 * <p>波命中工作盆时走的是<b>另一条路</b>：命中容器的槽位与盆内流体作为"通道 B"直接参与加工
	 * （见 {@code StellarWaveEntity#evalCandidate}），根本不需要载荷。</p>
	 */
	public static boolean isPlayerProcessingStation(Level level, BlockPos pos) {
		try {
			return level.getBlockEntity(pos) instanceof BasinBlockEntity;
		} catch (Throwable ignored) {
			return false; // 判定异常：按"不是"处理（保守，不影响正常容器）
		}
	}

	/**
	 * 载荷上限：辅料物品总数 / 种类数 / 流体 mB。<b>数值来自配置</b>
	 * （{@code [wave] maxPayloadItems / maxPayloadKinds / maxPayloadFluidMb}，默认 5 / 5 / 2000；
	 * 见 {@link AllConfig}），与"波命中后就地补料"共用同一口径，所以两处都必须读配置而不是常量。
	 */
	private static int maxPayloadItems() {
		return AllConfig.waveMaxPayloadItems;
	}

	private static int maxPayloadTypes() {
		return AllConfig.waveMaxPayloadKinds;
	}

	private static int maxPayloadFluid() {
		return AllConfig.waveMaxPayloadFluidMb;
	}
}
