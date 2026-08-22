package com.hjmmd_8.createoreexpansion.content.charger.recipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.ItemLike;

/**
 * 工具充能配方物品注册器：可被翡翠应力充能器充能的物品（能量工具 + 凝能佩）。
 *
 * <p>在 {@code AllItems} 注册物品处挂接（能量工具经 {@code SkillItemBuilder} 自动注册，
 * 凝能佩在注册后显式注册一行），工具充能配方生成器统一从这里收集物品——
 * 单一数据源，新增可充能物品只需在注册处声明，无需改配方生成器。</p>
 */
public final class ChargingRecipeTools {

	private static final List<ItemLike> TOOLS = new ArrayList<>();

	private ChargingRecipeTools() {
	}

	/** 注册一个可被充能器充能的物品（加入工具充能配方） */
	public static void register(ItemLike tool) {
		TOOLS.add(tool);
	}

	/** 全部可充能物品（工具充能配方生成用） */
	public static List<ItemLike> getTools() {
		return List.copyOf(TOOLS);
	}
}
