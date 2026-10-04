package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.GrindingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * <b>角磨配方（{@code createoreexpansion:grinding}）的生成动作</b>（配方迁移 批 1）。
 *
 * <p>本族原先<b>手写</b>在 {@code coe/src/main/resources/data/createoreexpansion/recipe/grinding/}
 * （9 条），本批改由本类产出——产物落点从 {@code src/main/resources} 换到
 * {@code coe/src/generated/resources}（由 {@code LayerRecipeRouter} 按调用点绑层
 * {@code "coe"} 改道），<b>配方 id 逐字不变</b>（= 手写文件的相对路径，见
 * {@link #generate} 里每条显式给出的 {@code name}）。</p>
 *
 * <h2>为什么显式传 id、不按产物反推</h2>
 * <p>本族文件名与产物<b>并非一一对应</b>：{@code jade_small_shard} 的输出物是
 * {@code createoreexpansion:jade_small_shard}，但 {@code diamond_grinding_wheel} 的输出物同名
 * ——而全模组有 <b>12 组 basename 碰撞</b>（例如 {@code topaz_ingot} 同时在 {@code pressing/}
 * 与 {@code rolling/} 下）。按产物名反推 id 会在那些族里生成到错的路径上，所以这里每条都把
 * id 的<b>路径段</b>显式写出来。</p>
 *
 * <h2>取值口径</h2>
 * <ul>
 *   <li>配方类型：{@link CoeRecipeTypes#GRINDING}。取<b>它已注册的那个序列化器</b>的工厂
 *       （{@code StandardProcessingRecipe.Serializer#factory()}）来建配方——这样生成期用的
 *       工厂与运行期反序列化用的是同一个实例，类型写错会在 datagen 期直接抛，而不是静默生成
 *       一份玩家加载不了的 JSON。</li>
 *   <li>{@code id} 只给<b>名字</b>：{@code StandardProcessingRecipe.Builder#build(RecipeOutput)}
 *       自己会把 {@code 配方类型 id 的 path + "/"} 前缀加上（本类型 ⇒ {@code grinding/}），
 *       与手写路径逐字相同。</li>
 *   <li>时长：9 条一律 {@value #DURATION} tick。⚠ 手写版这一族原本把键写成驼峰
 *       {@code processingTime}，Create 的 codec 只认 snake_case {@code processing_time}
 *       （{@code ProcessingRecipeParams} 里 {@code optionalFieldOf("processing_time", 0)}，
 *       无别名机制）⇒ 那 200 tick 从未生效过；批 0（{@code 470aa174}）只改了键名。
 *       本类用 {@code duration(200)} 走 codec 写盘，键名不可能再写歪。</li>
 * </ul>
 *
 * <p><b>调用方</b>：{@link CoeRecipeProvider#generate}（本层的配方生成入口）。之所以挂在它里面
 * 而不是根工程的 {@code data/RecipeProvider#buildRecipes}：根的调用点已经把
 * {@code CoeRecipeProvider.generate} 的 {@code RecipeOutput} 包成了「本层」的
 * （{@code bind(output, "coe")}），本类跟着同一个 output 走就能自动拿到同一份层归属，
 * 因此<b>不需要动根工程的任何一行</b>。</p>
 */
public final class CoeGrindingRecipeProvider {

    /**
     * 本族 9 条配方的处理时长（tick）。
     *
     * <p>手写版一律 200（批 0 修键名后才真正生效）；迁移后必须仍是 200，否则就是行为变化。</p>
     */
    private static final int DURATION = 200;

    /**
     * 本族全部角磨配方，调用顺序无契约（每条一个文件、互不覆盖），这里按"轮子 → 碎片"排列。
     *
     * <p>每条的第一个参数<b>就是</b>配方 id 的路径段（{@code grinding/<name>}）——不是从产物反推的。</p>
     */
    public static void generate(RecipeOutput output) {
        // ========== 5 条角磨轮：整块材料 → 对应等级的角磨轮（单输出，无副产物） ==========
        wheel(output, "diamond_grinding_wheel", Items.DIAMOND_BLOCK, CoeItems.DIAMOND_GRINDING_WHEEL.get());
        wheel(output, "iron_grinding_wheel", Items.IRON_BLOCK, CoeItems.IRON_GRINDING_WHEEL.get());
        wheel(output, "gold_grinding_wheel", Items.GOLD_BLOCK, CoeItems.GOLD_GRINDING_WHEEL.get());
        wheel(output, "brass_grinding_wheel", com.simibubi.create.AllBlocks.BRASS_BLOCK.get(), CoeItems.BRASS_GRINDING_WHEEL.get());
        wheel(output, "zinc_grinding_wheel", com.simibubi.create.AllBlocks.ZINC_BLOCK.get(), CoeItems.ZINC_GRINDING_WHEEL.get());

        // ========== 4 条小碎片：粗矿 → 2 片必得 + 1 片 50% ==========
        shard(output, "jade_small_shard", CoeItems.RAW_JADE.get(), CoeItems.JADE_SMALL_SHARD.get());
        shard(output, "topaz_small_shard", CoeItems.RAW_TOPAZ.get(), CoeItems.TOPAZ_SMALL_SHARD.get());
        shard(output, "sapphire_small_shard", CoeItems.RAW_SAPPHIRE.get(), CoeItems.SAPPHIRE_SMALL_SHARD.get());
        shard(output, "stellarstone_small_shard", CoeItems.RAW_STELLARSTONE.get(), CoeItems.STELLARSTONE_SMALL_SHARD.get());
    }

    /** 角磨轮：整块材料 → 1 个轮子（{@code count} 是 codec 默认值 1，故 JSON 里不写）。 */
    private static void wheel(RecipeOutput output, String name, ItemLike material, ItemLike wheel) {
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc(name))
            .require(material)
            .output(wheel)
            .duration(DURATION)
            .build(output);
    }

    /**
     * 小碎片：粗矿 → 2 片必得 + 1 片 50%。
     *
     * <p>{@code output(0.5f, shard)} 的 {@code count} 是默认值 1（JSON 里省略）、{@code chance}
     * 写 {@code 0.5}；与手写版
     * {@code {"id": …, "count": 1, "chance": 0.5}} <b>语义等价</b>（手写那份把默认的 count 显式
     * 写了出来，codec 不会——{@code ProcessingOutput} 的三个可选字段都用
     * {@code optionalFieldOf(…, 默认值)}，等于默认值就不落盘）。</p>
     */
    private static void shard(RecipeOutput output, String name, ItemLike raw, ItemLike shard) {
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc(name))
            .require(raw)
            .output(shard, 2)
            .output(0.5f, shard)
            .duration(DURATION)
            .build(output);
    }

    /**
     * 本配方类型<b>已注册</b>的序列化器工厂（见类注释"取值口径"）。
     *
     * <p>{@code LayerRecipeType#getSerializer()} 的返回类型是泛型 {@code T extends RecipeSerializer<?>}，
     * 赋值处按目标类型推断出 {@code Serializer<GrindingRecipe>}；若哪天 {@code GRINDING} 换成
     * 别的序列化器，这里会抛 {@code ClassCastException}（响亮的失败，而不是静默生成错的 JSON）。</p>
     */
    private static StandardProcessingRecipe.Factory<GrindingRecipe> factory() {
        StandardProcessingRecipe.Serializer<GrindingRecipe> serializer = CoeRecipeTypes.GRINDING.getSerializer();
        return serializer.factory();
    }

    private CoeGrindingRecipeProvider() {}
}
