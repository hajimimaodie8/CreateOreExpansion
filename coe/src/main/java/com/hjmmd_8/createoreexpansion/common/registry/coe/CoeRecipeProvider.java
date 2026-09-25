package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.DismantlingRecipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * <b>COE（矿物拓展）自己的配方生成动作</b>（P3c：从 {@code data/RecipeProvider} 拆出）。
 *
 * <p>本层负责<b>拆磨配方</b>（{@code createoreexpansion:dismantling/...}）：原版 4 组装备
 * （钻/铁/金/下界合金，每组 9 条）+ 本模组 5 组工具（翡翠/黄玉/蓝宝石/星辉石/雷鸣，每组 5 条），
 * 共 61 条——三级角磨轮专属。</p>
 *
 * <p><b>调用方</b>：{@code data/RecipeProvider}（SHARED 层的协调入口，也是唯一挂到
 * {@code DataGenerator} 上的那个提供器）。它按"拆分前 {@code buildRecipes} 里的<b>逐条顺序</b>"
 * 调用本类，所以配方文件的生成顺序与拆分前逐字相同。</p>
 *
 * <p><b>为什么生成顺序要紧</b>：配方落盘的文件名带条目名（互不覆盖），但 {@code runData} 的
 * 产物清单（{@code src/generated}）是要逐条目比对的——保持调用顺序就是保持产物的<b>集合与顺序</b>
 * 都不变，这是本轮"产物零变化"的硬指标之一。</p>
 */
public final class CoeRecipeProvider {

    /** 本层的全部拆磨配方，调用顺序与拆分前的 {@code buildRecipes} 逐字相同。 */
    public static void generate(RecipeOutput output) {
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
        dismantleTools(output, CoeItems.JADE_INGOT.get(), "jade",
            CoeItems.JADE_SWORD.get(), CoeItems.JADE_PICKAXE.get(), CoeItems.JADE_AXE.get(), CoeItems.JADE_SHOVEL.get(), CoeItems.JADE_HOE.get());
        dismantleTools(output, CoeItems.TOPAZ_INGOT.get(), "topaz",
            CoeItems.TOPAZ_SWORD.get(), CoeItems.TOPAZ_PICKAXE.get(), CoeItems.TOPAZ_AXE.get(), CoeItems.TOPAZ_SHOVEL.get(), CoeItems.TOPAZ_HOE.get());
        dismantleTools(output, CoeItems.SAPPHIRE_INGOT.get(), "sapphire",
            CoeItems.SAPPHIRE_SWORD.get(), CoeItems.SAPPHIRE_PICKAXE.get(), CoeItems.SAPPHIRE_AXE.get(), CoeItems.SAPPHIRE_SHOVEL.get(), CoeItems.SAPPHIRE_HOE.get());
        dismantleTools(output, CoeItems.STELLARSTONE_INGOT.get(), "stellarstone",
            CoeItems.STELLARSTONE_SWORD.get(), CoeItems.STELLARSTONE_PICKAXE.get(), CoeItems.STELLARSTONE_AXE.get(), CoeItems.STELLARSTONE_SHOVEL.get(), CoeItems.STELLARSTONE_HOE.get());
        dismantleTools(output, CoeItems.THUNDERITE_INGOT.get(), "thunderite",
            CoeItems.THUNDERITE_SWORD.get(), CoeItems.THUNDERITE_PICKAXE.get(), CoeItems.THUNDERITE_AXE.get(), CoeItems.THUNDERITE_SHOVEL.get(), CoeItems.THUNDERITE_HOE.get());
    }

    /** 一套材料：5 工具 + 4 装备的拆磨配方 */
    private static void dismantleSet(RecipeOutput output, ItemLike material, String materialName,
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
    private static void dismantleTools(RecipeOutput output, ItemLike material, String materialName,
                                       ItemLike sword, ItemLike pickaxe, ItemLike axe, ItemLike shovel, ItemLike hoe) {
        dismantling(output, sword, material, 2, materialName + "_sword");
        dismantling(output, pickaxe, material, 3, materialName + "_pickaxe");
        dismantling(output, axe, material, 3, materialName + "_axe");
        dismantling(output, shovel, material, 1, materialName + "_shovel");
        dismantling(output, hoe, material, 2, materialName + "_hoe");
    }

    /** 单条拆磨配方：装备 → 材料 × 权重系数 */
    private static void dismantling(RecipeOutput output, ItemLike item, ItemLike result, int materialCount, String name) {
        DismantlingRecipe recipe = new DismantlingRecipe(new ItemStack(item), new ItemStack(result), materialCount);
        output.accept(CoeCore.modLoc("dismantling/" + name), recipe, null, new ICondition[0]);
    }

    private CoeRecipeProvider() {}
}
