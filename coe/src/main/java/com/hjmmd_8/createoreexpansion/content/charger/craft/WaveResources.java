package com.hjmmd_8.createoreexpansion.content.charger.craft;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * <b>变体波加工的"资源引用"模型</b>（2026-09 从 2700 行的 {@code StellarWaveEntity} 尾部抽出成独立类型）。
 *
 * <p>物品 / 流体 / 电量三类输入在波引擎里是<b>同一套模型</b>：每条需求记录"<b>来源通道 + 需求量</b>"，
 * 来源一律"<b>就近优先、载荷兜底</b>"，扣减时按来源分别扣。</p>
 *
 * <table border="1">
 *   <caption>三类资源引用</caption>
 *   <tr><th>资源</th><th>来源枚举</th><th>引用类型</th><th>来源含义（优先 → 兜底）</th></tr>
 *   <tr><td>辅料物品</td><td>{@link AuxSource}</td><td>{@link AuxRef}</td>
 *       <td>CONTAINER = 命中容器其它槽（槽号）→ PAYLOAD = 波载荷条目（下标）</td></tr>
 *   <tr><td>流体</td><td>{@link FluidSource}</td><td>{@link FluidRef}</td>
 *       <td>CONTAINER = 命中容器流体槽 → PAYLOAD = 载荷流体</td></tr>
 *   <tr><td>电量</td><td>{@link EnergySource}</td><td>{@link EnergyDraw}</td>
 *       <td>NEARBY = 命中点邻域可抽储能 → PAYLOAD = 载荷电量</td></tr>
 * </table>
 *
 * <p><b>为什么用"public final 字段 + 访问器"而不是 record</b>：这些类型原来作为<b>私有嵌套 record</b>
 * 存在于波实体内部，调用点（40+ 处）写的是字段式访问 {@code ref.source} / {@code draw.amount}；
 * 搬成顶层 <b>record</b> 后字段变私有，调用点全部编译不过。这里保留"不可变数据载体"语义
 * （字段 public final、无 setter），同时提供与 record 同名的访问器方法，既有调用点零改动。
 * 它们都是纯数据（不持有世界/方块实体引用），可安全跨 tick 保存在候选里。</p>
 */
public final class WaveResources {

	private WaveResources() {
	}

	/**
	 * 辅料物品来源通道。
	 *
	 * <p>{@link #CONTAINER} = <b>命中容器自身的其它槽</b>（工作盆/置物台里与主料同一批的物品），
	 * 索引即槽号，<b>优先使用</b>；{@link #PAYLOAD} = 波载荷条目，掉落物路径唯一来源，容器凑不齐时兜底。</p>
	 */
	public enum AuxSource {
		CONTAINER, PAYLOAD
	}

	/** 一条辅料引用：来源通道 + 该通道内的下标（容器槽号 / 载荷条目下标）。 */
	public static final class AuxRef {
		public final AuxSource source;
		public final int index;

		public AuxRef(AuxSource source, int index) {
			this.source = source;
			this.index = index;
		}

		public AuxSource source() {
			return source;
		}

		public int index() {
			return index;
		}
	}

	/** 流体输入来源通道：{@link #CONTAINER} = 命中容器的流体槽（优先），{@link #PAYLOAD} = 波载荷流体（兜底）。 */
	public enum FluidSource {
		CONTAINER, PAYLOAD
	}

	/**
	 * 一条流体输入需求：来源通道 + 所需流体（<b>种类与组件 + 需求量</b>，用 {@code copyWithAmount}
	 * 从命中的罐内流体/载荷流体复制而来，故带组件时也能精确 drain）。
	 */
	public static final class FluidRef {
		public final FluidSource source;
		public final FluidStack fluid;

		public FluidRef(FluidSource source, FluidStack fluid) {
			this.source = source;
			this.fluid = fluid;
		}

		public FluidSource source() {
			return source;
		}

		public FluidStack fluid() {
			return fluid;
		}
	}

	/**
	 * 电量来源通道（"电量类比成一种特殊的辅料"）：{@link #NEARBY} = 命中点邻域的可抽储能
	 * （通用 {@code IEnergyStorage} / CC&amp;A 特斯拉线圈；<b>优先</b>），{@link #PAYLOAD} = 波载荷电量（兜底）。
	 */
	public enum EnergySource {
		NEARBY, PAYLOAD
	}

	/**
	 * 一条电量需求：来源通道 + 需求量（FE）+ 供能方块位置（{@code NEARBY} 专用，需长期持有故为
	 * {@code immutable()}；{@code PAYLOAD} 为 null）。
	 */
	public static final class EnergyDraw {
		public final EnergySource source;
		public final int amount;
		public final BlockPos pos;

		public EnergyDraw(EnergySource source, int amount, BlockPos pos) {
			this.source = source;
			this.amount = amount;
			this.pos = pos;
		}

		public EnergySource source() {
			return source;
		}

		public int amount() {
			return amount;
		}

		public BlockPos pos() {
			return pos;
		}
	}

	/** 汇总一组电量需求的合计（FE）。 */
	public static int totalEnergy(List<EnergyDraw> draws) {
		int sum = 0;
		for (EnergyDraw draw : draws)
			sum += draw.amount;
		return sum;
	}

	/** 该配方是否可作为候选载体（防御性判空；不做配方语义判断）。 */
	public static boolean isValidRecipe(Recipe<?> recipe) {
		return recipe != null;
	}
}
