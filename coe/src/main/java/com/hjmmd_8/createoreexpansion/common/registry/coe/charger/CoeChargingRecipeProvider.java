package com.hjmmd_8.createoreexpansion.common.registry.coe.charger;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.charger.ChargingRecipeTools;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;
import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.level.ItemLike;

/**
 * <b>工具充能配方的生成动作（第一层侧）</b>（W6-c；原 {@code CewsRecipeProvider}，P3c 从
 * {@code data/RecipeProvider} 拆出）。
 *
 * <p><b>归属为什么是第一层</b>：这 160 条配方（32 个可充能物品 × 5 个充能等级）的
 * {@code type} 是 {@code createoreexpansion:charging}，而充能器那三台机器与
 * {@code CHARGING} 配方类型现在都在 {@code :coe}（Recipe Type 的层归属跟着"谁能加工它"走：
 * 充能器是第一层，所以类型也是）。留一份在 {@code :cews} 就是 P7d 那条禁令的镜像——
 * 生成物落进了一个它的配方类型不存在的 jar。</p>
 *
 * <p><b>类名</b>：{@code CewsRecipeProvider} → {@code CoeChargingRecipeProvider}
 * （方案 §8.1 ③ 的命名）。改名只发生在本类与它的唯一调用点
 * （{@code data/RecipeProvider#buildRecipes}）；产物路径、配方 id、顺序都逐字不变。</p>
 *
 * <p>物品来源为 {@link ChargingRecipeTools}（在 {@code CoeItems} 注册处统一挂接，单一数据源），
 * 每个物品 × <b>5 个充能等级</b>各一条配方（α/β/γ/ε/ω = level 1~5），
 * 等级由配方 JSON 的 {@code level} 字段区分，统一放在 {@code tool_charge/} 下，
 * 文件名后缀 _low/_high/_gamma/_epsilon/_omega 仅保证 id 唯一。</p>
 *
 * <p><b>调用方</b>：{@code data/RecipeProvider}（SHARED 层的协调入口）。它按拆分前
 * {@code buildRecipes} 的逐条顺序调用本类（拆磨在前、本类在后），
 * 所以配方生成顺序与拆分前逐字相同；W6-c 起层名从 {@code "cews"} 改成 {@code "coe"}
 * （{@code LayerRecipeRouter} 的绑定参数），产物照旧落模块目录。</p>
 */
public final class CoeChargingRecipeProvider {

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

    private CoeChargingRecipeProvider() {}
}
