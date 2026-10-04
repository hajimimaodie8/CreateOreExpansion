package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.lightning.recipe.LightningBlockRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;

/**
 * <b>方块雷击配方（{@code createoreexpansion:lightning_block}）的生成动作</b>（配方迁移 批 1）。
 *
 * <p>本族原先<b>手写</b>在
 * {@code coe/src/main/resources/data/createoreexpansion/recipe/lightning_block/}（1 条），
 * 本批改由本类产出——产物落点从 {@code src/main/resources} 换到
 * {@code coe/src/generated/resources}，配方 id 逐字不变
 * （{@code createoreexpansion:lightning_block/stone_to_diamond_ore}）。</p>
 *
 * <h2>为什么这条配方<b>不</b>调 {@code duration(...)}</h2>
 * <p>手写文件里没有 {@code processing_time} 键，也就是该字段取 codec 的默认值
 * {@code 0}（{@code ProcessingRecipeParams}：{@code optionalFieldOf("processing_time", 0)}）。
 * {@code ProcessingRecipeParams#processingDuration} 初值本来就是 0，所以这里<b>刻意不写</b>
 * {@code duration(0)} 或 {@code duration(任何值)}：不写 ⇒ 落盘的 JSON 里没有该键，与手写版
 * 逐字相同；写一个非 0 值就是凭空给这条配方加时长。</p>
 *
 * <p>要不要显式写 {@code duration(0)} 是风格问题（结果相同）；这里选择"不写 + 注释说明"，
 * 让"这条配方根本没有时长概念"这件事在读代码时是可见的。</p>
 *
 * <p><b>调用方</b>：{@link CoeRecipeProvider#generate}。理由同
 * {@link CoeGrindingRecipeProvider}：根工程的 {@code buildRecipes} 已经把
 * {@code CoeRecipeProvider.generate} 的 {@code RecipeOutput} 包成「本层」的
 * （{@code bind(output, "coe")}），本类跟着同一个 output 走即可，<b>不需要动根工程</b>。</p>
 */
public final class CoeLightningBlockRecipeProvider {

    /**
     * 本族唯一的配方：石头 → 钻石矿。
     *
     * <p>第一个参数是 id 的路径段（{@code lightning_block/stone_to_diamond_ore}），显式给出、
     * 不按产物反推——全模组 12 组 basename 碰撞就是"按产物反推"会踩的坑。</p>
     */
    public static void generate(RecipeOutput output) {
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc("stone_to_diamond_ore"))
            .require(Items.STONE)
            .output(Items.DIAMOND_ORE)
            .build(output);
    }

    /**
     * 本配方类型<b>已注册</b>的序列化器工厂（与 {@link CoeGrindingRecipeProvider} 同一手法）：
     * 生成期与运行期共用同一个工厂；类型不符则在 datagen 期即抛。
     */
    private static StandardProcessingRecipe.Factory<LightningBlockRecipe> factory() {
        StandardProcessingRecipe.Serializer<LightningBlockRecipe> serializer = CoeRecipeTypes.LIGHTNING_BLOCK.getSerializer();
        return serializer.factory();
    }

    private CoeLightningBlockRecipeProvider() {}
}
