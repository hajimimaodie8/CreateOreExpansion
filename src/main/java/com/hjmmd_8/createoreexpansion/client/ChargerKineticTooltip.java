package com.hjmmd_8.createoreexpansion.client;

import java.util.List;

import com.hjmmd_8.createoreexpansion.content.charger.block.JadeStressChargerBlock;
import com.hjmmd_8.createoreexpansion.content.charger.block.SapphireStressChargerBlock;
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