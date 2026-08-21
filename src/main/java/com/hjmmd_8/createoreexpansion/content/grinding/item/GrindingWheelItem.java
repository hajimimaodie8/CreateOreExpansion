package com.hjmmd_8.createoreexpansion.content.grinding.item;

import java.util.List;

import com.hjmmd_8.createoreexpansion.content.grinding.effect.GrindingWheelEffects;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 角磨轮物品：悬停 tooltip 显示该轮子的特殊效果描述
 * （背包与 JEI 物品列表悬停均可看到）。
 */
public class GrindingWheelItem extends Item {

	public GrindingWheelItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(this);
		Component description = GrindingWheelEffects.get(id)
			.getDescription();
		if (!description.getString()
			.isEmpty())
			tooltip.add(description.copy()
				.withStyle(ChatFormatting.AQUA));
	}
}
