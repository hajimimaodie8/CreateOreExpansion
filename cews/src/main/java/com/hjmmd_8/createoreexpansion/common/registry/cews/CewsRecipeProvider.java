package com.hjmmd_8.createoreexpansion.common.registry.cews;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.charger.ChargingRecipeTools;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;
import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.level.ItemLike;

/**
 * <b>CEWS（能量波阵学）自己的配方生成动作</b>（P3c：从 {@code data/RecipeProvider} 拆出）。
 *
 * <p>本层负责<b>工具充能配方</b>（{@code createoreexpansion:tool_charge/...}）：
 * 翡翠/蓝宝石/星辉石应力充能器的能量波给能量工具/凝能佩充能。
 * 物品来源为 {@link ChargingRecipeTools}（在 {@code CoeItems} 注册处统一挂接，单一数据源），
 * 每个物品 × <b>5 个充能等级</b>各一条配方（α/β/γ/ε/ω = level 1~5），
 * 等级由配方 JSON 的 {@code level} 字段区分，统一放在 {@code tool_charge/} 下，
 * 文件名后缀 _low/_high/_gamma/_epsilon/_omega 仅保证 id 唯一。</p>
 *
 * <p><b>调用方</b>：{@code data/RecipeProvider}（SHARED 层的协调入口）。它按拆分前
 * {@code buildRecipes} 的逐条顺序调用本类（COE 拆磨在前、本类在后），
 * 所以配方生成顺序与拆分前逐字相同。</p>
 *
 * <p><b>跨层引用说明</b>：本类 import 了 COE 的 {@code CoeItems}（触发其类初始化，把可充能物品
 * 登记进 {@link ChargingRecipeTools}）——方向是 <b>CEWS → COE</b>，正是分层约定允许的方向
 * （拆分前这段代码也在同一个 {@code RecipeProvider} 里，位置一字未动）。</p>
 */
public final class CewsRecipeProvider {

    /** 本层的全部工具充能配方，调用时机与拆分前的 {@code toolCharging(output)} 相同。 */
    public static void generate(RecipeOutput output) {
        // 触发 CoeItems 类加载（能量工具/凝能佩经其静态初始化注册进 ChargingRecipeTools），
        // 避免 data gen 时注册器为空导致配方生成 0 条
        CoeItems.register();
        for (ItemLike item : ChargingRecipeTools.getTools()) {
            String name = BuiltInRegistries.ITEM.getKey(item.asItem())
                .getPath();
            charging(output, item, "tool_charge/" + name + "_low", 1);
            charging(output, item, "tool_charge/" + name + "_high", 2);
            charging(output, item, "tool_charge/" + name + "_gamma", 3);
            charging(output, item, "tool_charge/" + name + "_epsilon", 4);
            charging(output, item, "tool_charge/" + name + "_omega", 5);
        }
    }

    /** 单条工具充能配方：输入单个能量物品，输出=输入工具本身（充能后仍是该工具，JEI 直观显示）。
     * @param level 配方要求的充能等级（1=α、2=β、3=γ、4=ε、5=ω） */
    private static void charging(RecipeOutput output, ItemLike item, String name, int level) {
        new ChargingRecipe.Builder(CoeCore.modLoc(name))
            .withLevel(level)
            .require(item)
            .output(item, 1)
            .build(output);
    }

    private CewsRecipeProvider() {}
}
