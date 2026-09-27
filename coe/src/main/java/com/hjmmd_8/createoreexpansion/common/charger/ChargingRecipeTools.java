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
 * <p><b>为什么现在住 {@code :coe}</b>（W6-d，方案 §2.4 的 P2）：它是第一层的<b>内部登记表</b>
 * ——写入方 {@code CoeItems}（7 处登记）与唯一读取方 {@code CoeChargingRecipeProvider}
 * 以及 data gen 的 {@code RecipeProvider} 全都在 {@code :coe}。曾经它住 {@code core}：
 * 那时读取方是 CEWS 的充能配方生成器，而"工具"归第一层，表放中间层才不用反向 import；
 * W6-c 把充能器与 {@code charging} 配方类型整体收回第一层之后，这张表在 {@code core}
 * 里就只剩"底层库认识充能配方工具"这一条说不通的关系了。包名不变
 * （{@code common.charger}），所以所有 import 逐字不变。</p>
 *
 * <p>本类只是<b>纯内存登记表</b>，当前只存"能不能充"的成员集合（{@code ItemLike} 列表），
 * <b>不带</b>每物品参数或数值（等级/用量由配方生成器对每个成员统一展开 5 个充能等级）。</p>
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
