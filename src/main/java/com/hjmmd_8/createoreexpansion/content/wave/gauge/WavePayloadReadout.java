package com.hjmmd_8.createoreexpansion.content.wave.gauge;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.content.charger.entity.AbstractChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.entity.StellarWaveEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 波载荷读数：一只能量波"带了什么"（物品件数 / 流体 / 电量）的取值与摘要文案。
 *
 * <p><b>为什么单独一个类</b>：波情四要素里只有载荷需要"多段拼装 + 空载占位"，
 * 它与"波速/波级/波型"那种单一取值不是一类事；把这段放进物品类会变成堆砌的巨型方法。
 * {@link WaveReadout} 只管四要素的成组与追加，本类只管载荷这一段。</p>
 *
 * <p><b>载荷只属于全能波（变体波）</b>：{@code getPayloadItems/Fluid/Energy} 定义在
 * {@link StellarWaveEntity} 上，普通波与攻击波恒为空载——"有没有载荷"的判定只在本类做一次。</p>
 *
 * @param itemCount 携带物品的总件数（各条目 count 求和，空条目不计）
 * @param fluid     携带流体（无 = {@link FluidStack#EMPTY}；构造时深拷贝，不与他人共享实例）
 * @param energy    携带电量（FE）
 */
public record WavePayloadReadout(int itemCount, FluidStack fluid, int energy) {

	/** 本物品全部词条前缀（中英词条见 data/lang 的两个 LangProvider）。 */
	private static final String LANG = "createoreexpansion.wave_gauge.";

	/** 空载读数（普通波 / 攻击波 / 空手的全能波共用同一个不可变实例）。 */
	public static final WavePayloadReadout EMPTY = new WavePayloadReadout(0, FluidStack.EMPTY, 0);

	/** 拷贝流体（波实体给的是只读副本，这里再存一份以免调用方改到波的运行态）。 */
	public WavePayloadReadout {
		fluid = fluid == null || fluid.isEmpty() ? FluidStack.EMPTY : fluid.copy();
	}

	/**
	 * 取一只能量波的载荷读数：变体波读它携带的载荷，其余波型一律 {@link #EMPTY}。
	 *
	 * <p>只读，不改动波的任何状态（本物品是"查询仪"，不是"搬运器"）。</p>
	 */
	public static WavePayloadReadout of(AbstractChargerWaveEntity wave) {
		if (!(wave instanceof StellarWaveEntity stellar))
			return EMPTY;
		int count = 0;
		for (ItemStack stack : stellar.getPayloadItems())
			count += stack.getCount();
		return new WavePayloadReadout(count, stellar.getPayloadFluid(), stellar.getPayloadEnergy());
	}

	/** 是否空载（物品件数、流体、电量三者皆无）。 */
	public boolean isEmpty() {
		return itemCount <= 0 && fluid.isEmpty() && energy <= 0;
	}

	/**
	 * 载荷摘要文案："物品 3 件 · 流体 水 500 mB · 电量 1200 FE"
	 * （三段各自判空、有才拼；一段都没有时给"空载"占位——四要素成组显示，
	 * 空载也要占位，否则玩家分不清"没带东西"和"没读出来"）。
	 */
	public Component summary() {
		List<Component> parts = new ArrayList<>(3);
		if (itemCount > 0)
			parts.add(Component.translatable(LANG + "payload_items", itemCount));
		if (!fluid.isEmpty())
			parts.add(Component.translatable(LANG + "payload_fluid",
				fluid.getFluid()
					.getFluidType()
					.getDescription(),
				fluid.getAmount()));
		if (energy > 0)
			parts.add(Component.translatable(LANG + "payload_energy", energy));
		if (parts.isEmpty())
			return Component.translatable(LANG + "payload_none");

		MutableComponent summary = Component.empty();
		for (int i = 0; i < parts.size(); i++) {
			if (i > 0)
				summary.append(Component.translatable(LANG + "join"));
			summary.append(parts.get(i));
		}
		return summary;
	}
}
