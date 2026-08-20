package com.hjmmd_8.createoreexpansion.data;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllItems;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.DismantlingRecipe;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.GrindingRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipeBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.concurrent.CompletableFuture;

/**
 * 配方生成器：生成角磨床的衍生配方（拆磨配方 + 粗矿序列加工配方）。
 */
public class RecipeProvider extends net.minecraft.data.recipes.RecipeProvider {
    public RecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // ========== 原版装备/武器（权重：剑2 镐3 斧3 铲1 锄2 / 头盔5 胸甲8 护腿7 靴子4） ==========
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

        // ========== 本模组工具（动力合成产出，均有合成表） ==========
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

        // ========== 粗矿序列加工：切割 → 压片 → 角磨，循环 2 遍，产出大块/小块碎片（加权 25% 各） ==========
        // 注：createaddition 的 RollingRecipe 未实现 IAssemblyRecipe 无法作序列步骤，以 Create 原生压片（PressingRecipe）代替棍压
        sequencedOre(output, AllItems.RAW_JADE.get(), AllItems.JADE_BIG_SHARD.get(), AllItems.JADE_SMALL_SHARD.get(), "jade");
        sequencedOre(output, AllItems.RAW_TOPAZ.get(), AllItems.TOPAZ_BIG_SHARD.get(), AllItems.TOPAZ_SMALL_SHARD.get(), "topaz");
        sequencedOre(output, AllItems.RAW_SAPPHIRE.get(), AllItems.SAPPHIRE_BIG_SHARD.get(), AllItems.SAPPHIRE_SMALL_SHARD.get(), "sapphire");
        sequencedOre(output, AllItems.RAW_STELLARSTONE.get(), AllItems.STELLARSTONE_BIG_SHARD.get(), AllItems.STELLARSTONE_SMALL_SHARD.get(), "stellarstone");
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

    /** 粗矿序列配方：切割 → 压片 → 角磨，整个流程循环 2 遍，过渡物为粗矿本身，结果池 4 项各 25% */
    private void sequencedOre(RecipeOutput output, ItemLike raw, ItemLike big, ItemLike small, String name) {
        new SequencedAssemblyRecipeBuilder(CreateOreExpansion.modLoc(name))
            .require(raw)
            .transitionTo(raw)
            .loops(2)
            .addStep(CuttingRecipe::new, rb -> rb)
            .addStep(PressingRecipe::new, rb -> rb)
            .addStep(GrindingRecipe::new, rb -> rb)
            .addOutput(new ItemStack(big, 3), 25)
            .addOutput(new ItemStack(big, 2), 25)
            .addOutput(new ItemStack(small, 3), 25)
            .addOutput(new ItemStack(small, 2), 25)
            .build(output);
    }
}
