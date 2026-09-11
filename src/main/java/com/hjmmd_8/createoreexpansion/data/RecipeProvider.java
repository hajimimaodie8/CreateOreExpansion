package com.hjmmd_8.createoreexpansion.data;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllItems;
import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipe;
import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipeTools;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.DismantlingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.concurrent.CompletableFuture;

/**
 * 配方生成器：只生成<b>重复性/机械化</b>的批量配方，其余（合成表、序列加工、特殊配方等）手写 JSON：
 * <ul>
 *     <li>拆磨配方（61 条：原版 4 组装备 + 本模组 5 组工具）——三级角磨轮专属；</li>
 *     <li>工具充能（31 物品 × 3 等级 = 93 条）——翡翠应力充能器。</li>
 * </ul>
 * 粗矿序列加工配方（切割 → 压片 → 角磨）手写在 {@code resources/data}（不走生成器）；
 * 高级角磨（≥2 级轮执行 Create 粉碎轮/石磨配方）是<b>代码动态匹配</b>（{@code GrinderRecipeTypes}
 * 注册表），不在此生成——加入其他模组（同类型或自行注册的类型）自动生效。
 */
public class RecipeProvider extends net.minecraft.data.recipes.RecipeProvider {
    public RecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // ========== 原版装备/武器拆磨（权重：剑2 镐3 斧3 铲1 锄2 / 头盔5 胸甲8 护腿7 靴子4） ==========
        dismantleSet(output, Items.DIAMOND, "diamond",
                Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE,
                Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
        dismantleSet(output, Items.IRON_INGOT, "iron",
                Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE,
                Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        dismantleSet(output, Items.GOLD_INGOT, "gold",
                Items.GOLDEN_SWORD, Items.GOLDEN_PICKAXE, Items.GOLDEN_AXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE,
                Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);
        dismantleSet(output, Items.NETHERITE_INGOT, "netherite",
                Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE,
                Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);

        // ========== 本模组工具拆磨（5 组 × 5 工具） ==========
        dismantleTools(output, AllItems.JADE_INGOT.get(), "jade",
                AllItems.JADE_SWORD.get(), AllItems.JADE_PICKAXE.get(), AllItems.JADE_AXE.get(), AllItems.JADE_SHOVEL.get(), AllItems.JADE_HOE.get());
        dismantleTools(output, AllItems.TOPAZ_INGOT.get(), "topaz",
                AllItems.TOPAZ_SWORD.get(), AllItems.TOPAZ_PICKAXE.get(), AllItems.TOPAZ_AXE.get(), AllItems.TOPAZ_SHOVEL.get(), AllItems.TOPAZ_HOE.get());
        dismantleTools(output, AllItems.SAPPHIRE_INGOT.get(), "sapphire",
                AllItems.SAPPHIRE_SWORD.get(), AllItems.SAPPHIRE_PICKAXE.get(), AllItems.SAPPHIRE_AXE.get(), AllItems.SAPPHIRE_SHOVEL.get(), AllItems.SAPPHIRE_HOE.get());
        dismantleTools(output, AllItems.STELLARSTONE_INGOT.get(), "stellarstone",
                AllItems.STELLARSTONE_SWORD.get(), AllItems.STELLARSTONE_PICKAXE.get(), AllItems.STELLARSTONE_AXE.get(), AllItems.STELLARSTONE_SHOVEL.get(), AllItems.STELLARSTONE_HOE.get());
        dismantleTools(output, AllItems.THUNDERITE_INGOT.get(), "thunderite",
                AllItems.THUNDERITE_SWORD.get(), AllItems.THUNDERITE_PICKAXE.get(), AllItems.THUNDERITE_AXE.get(), AllItems.THUNDERITE_SHOVEL.get(), AllItems.THUNDERITE_HOE.get());

        // ========== 工具充能配方：翡翠应力充能器能量波给能量工具/凝能佩充能 ==========
        toolCharging(output);
    }

    /** 一套材料：5 工具 + 4 装备的拆磨配方 */
    private void dismantleSet(RecipeOutput output, ItemLike material, String materialName,
                              ItemLike sword, ItemLike pickaxe, ItemLike axe, ItemLike shovel, ItemLike hoe,
                              ItemLike helmet, ItemLike chestplate, ItemLike leggings, ItemLike boots) {
        dismantling(output, sword, material, 2, materialName + "_sword");
        dismantling(output, pickaxe, material, 3, materialName + "_pickaxe");
        dismantling(output, axe, material, 3, materialName + "_axe");
        dismantling(output, shovel, material, 1, materialName + "_shovel");
        dismantling(output, hoe, material, 2, materialName + "_hoe");
        dismantling(output, helmet, material, 5, materialName + "_helmet");
        dismantling(output, chestplate, material, 8, materialName + "_chestplate");
        dismantling(output, leggings, material, 7, materialName + "_leggings");
        dismantling(output, boots, material, 4, materialName + "_boots");
    }

    /** 一套材料：5 工具的拆磨配方（本模组） */
    private void dismantleTools(RecipeOutput output, ItemLike material, String materialName,
                                ItemLike sword, ItemLike pickaxe, ItemLike axe, ItemLike shovel, ItemLike hoe) {
        dismantling(output, sword, material, 2, materialName + "_sword");
        dismantling(output, pickaxe, material, 3, materialName + "_pickaxe");
        dismantling(output, axe, material, 3, materialName + "_axe");
        dismantling(output, shovel, material, 1, materialName + "_shovel");
        dismantling(output, hoe, material, 2, materialName + "_hoe");
    }

    /** 单条拆磨配方：装备 → 材料 × 权重系数 */
    private void dismantling(RecipeOutput output, ItemLike item, ItemLike result, int materialCount, String name) {
        DismantlingRecipe recipe = new DismantlingRecipe(new ItemStack(item), new ItemStack(result), materialCount);
        output.accept(CreateOreExpansion.modLoc("dismantling/" + name), recipe, null, new ICondition[0]);
    }

    /** 粗矿序列配方：切割 → 压片 → 角磨，整个流程循环 2 遍，过渡物为粗矿本身，结果池 4 项各 25%。
     * 手写在 resources/data（不走生成器），此处不再生成 */

    /** 工具充能配方：翡翠/蓝宝石应力充能器能量波给能量工具/凝能佩充能。
     * <p>物品来源为 {@link ChargingRecipeTools}（在 AllItems 注册处统一挂接，单一数据源）。
     * 每个物品 × <b>5 个充能等级</b>各一条配方（低/高/伽马/伊普西龙/欧米伽 = level 1~5），
     * 等级由配方 JSON 的 {@code level} 字段区分，统一放在 {@code tool_charge/} 下，
     * 文件名后缀 _low/_high/_gamma/_epsilon/_omega 仅保证 id 唯一。</p> */
    private void toolCharging(RecipeOutput output) {
        // 触发 AllItems 类加载（能量工具/凝能佩经其静态初始化注册进 ChargingRecipeTools），
        // 避免 data gen 时注册器为空导致配方生成 0 条
        AllItems.register();
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
     * @param level 配方要求的充能等级（1=低、2=高、3=伽马、4=伊普西龙、5=欧米伽） */
    private void charging(RecipeOutput output, ItemLike item, String name, int level) {
        new ChargingRecipe.Builder(CreateOreExpansion.modLoc(name))
            .withLevel(level)
            .require(item)
            .output(item, 1)
            .build(output);
    }
}
