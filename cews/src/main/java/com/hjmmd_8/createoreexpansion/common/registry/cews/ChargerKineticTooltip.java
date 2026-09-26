package com.hjmmd_8.createoreexpansion.common.registry.cews;

import java.util.List;

import com.hjmmd_8.createoreexpansion.content.charger.block.JadeStressChargerBlock;
import com.hjmmd_8.createoreexpansion.content.charger.block.SapphireStressChargerBlock;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * 应力充能器（翡翠/蓝宝石）物品悬停应力块：
 * 空行 + 原生键标签行（tooltip.stressImpact，灰）；
 * 数值行 = 三个实心方块（TooltipHelper.makeProgressBar 满格，Create 机件同款）
 * + 自定义区间提示（消耗 = 一级 4x × 蓄力等级：翡翠 1~3 → 4x-12x；蓝宝石 1~5 → 4x-20x），整行青色。
 *
 * 充能器 Block 以 hideStressImpact() 隐藏 Create 默认静态块，避免两行重复。
 *
 * <p><b>P3o 为何从 {@code client} 搬进本包</b>：原先 {@code common/registry/RegistrateTooltips}
 * 用一个 {@code chargers} 布尔量决定"要不要接上本 modifier"，于是共享库必须 import 本类，
 * 而本类认的是 CEWS 的两台充能器（{@code JadeStressChargerBlock} / {@code SapphireStressChargerBlock}，
 * 都是 CEWS 的机器）⇒ 库知道了内容模块。搬进 CEWS 包后由 {@code CewsRegistrate} 自己
 * <b>串联</b>（见 {@link #withChargers(CreateRegistrate)}），库侧只剩通用的描述/动能两级。
 * 本类的显示逻辑一行未改。</p>
 */
public class ChargerKineticTooltip implements TooltipModifier {

	private final boolean sapphire;

	private ChargerKineticTooltip(boolean sapphire) {
		this.sapphire = sapphire;
	}

	/** 仅应力充能器物品需要本 modifier；其它机件返回 null（走 Create 默认 KineticStats）。 */
	public static ChargerKineticTooltip create(Item item) {
		if (item instanceof BlockItem blockItem) {
			Block block = blockItem.getBlock();
			if (block instanceof SapphireStressChargerBlock)
				return new ChargerKineticTooltip(true);
			if (block instanceof JadeStressChargerBlock)
				return new ChargerKineticTooltip(false);
		}
		return null;
	}

	/**
	 * <b>把本 modifier 接在既有的 tooltip 工厂之后</b>（P3o：原先由共享库的一个
	 * {@code chargers} 布尔量代劳，现改由 CEWS 自己串联）。
	 *
	 * <p>组合顺序与拆分前逐字相同：原来共享库是
	 * {@code ItemDescription → KineticStats → ChargerKineticTooltip}，这里先读回它装好的
	 * 工厂、再 {@code andThen} 同一段本类 modifier；{@code TooltipModifier.mapNull(...)}
	 * 把"非充能器物品返回 null"折叠成恒等，与旧的 {@code if (chargers)} 分支语义相同。</p>
	 *
	 * @param registrate 本层 Registrate（**须已由共享库装过通用两级**，见 {@code CewsRegistrate} 静态块）
	 */
	public static void withChargers(CreateRegistrate registrate) {
		java.util.function.Function<Item, TooltipModifier> base = registrate.getTooltipModifierFactory();
		registrate.setTooltipModifierFactory(item ->
			base.apply(item).andThen(TooltipModifier.mapNull(create(item))));
	}

	@Override
	public void modify(ItemTooltipEvent context) {
		List<Component> tooltip = context.getToolTip();
		tooltip.add(CommonComponents.EMPTY);
		// 标签行：Create 原生键（"Kinetic Stress Impact:"）
		tooltip.add(CreateLang.translateDirect("tooltip.stressImpact")
			.withStyle(ChatFormatting.GRAY));
		// 数值行：三个实心方块 + 自定义提示文字，整行红色
		tooltip.add(Component.literal(TooltipHelper.makeProgressBar(3, 3))
			.append(Component.literal(sapphire
				? "4-20x RPM 该倍率是变化的"
				: "4-12x RPM 该倍率是变化的"))
			.withStyle(ChatFormatting.RED));
	}
}