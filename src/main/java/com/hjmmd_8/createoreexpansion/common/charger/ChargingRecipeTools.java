package com.hjmmd_8.createoreexpansion.common.charger;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.ItemLike;

/**
 * 工具充能配方物品注册器：可被翡翠应力充能器充能的物品（能量工具 + 凝能佩）。
 *
 * <p>在 {@code CoeItems} 注册物品处挂接（能量工具经 {@code SkillItemBuilder} 自动注册，
 * 凝能佩在注册后显式注册一行），工具充能配方生成器统一从这里收集物品——
 * 单一数据源，新增可充能物品只需在注册处声明，无需改配方生成器。</p>
 *
 * <p><b>为什么住 common（SHARED）层</b>：写入方是 COE（{@code CoeItems} 的 7 处登记），
 * 读取方是 data gen（{@link com.hjmmd_8.createoreexpansion.data.RecipeProvider}）与充能配方线；
 * 住在 CEWS 包里会让 COE 反向 import CEWS（禁止方向）。本类只是<b>纯内存登记表</b>，
 * 当前只存"能不能充"的成员集合（{@code ItemLike} 列表），<b>不带</b>每物品参数或数值
 * （等级/用量由配方生成器对每个成员统一展开 5 个充能等级）。</p>
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
