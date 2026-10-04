package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.AllGemTags;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.level.ItemLike;

/**
 * <b>压片配方（{@code create:pressing}）的生成动作</b>（配方迁移 批 2）。
 *
 * <p>本族原先<b>手写</b>在
 * {@code coe/src/main/resources/data/createoreexpansion/recipe/pressing/}（4 条：四种锭各
 * 压成对应的板），本批改由本类产出——产物落点从 {@code src/main/resources} 换到
 * {@code coe/src/generated/resources}（由 {@code LayerRecipeRouter} 按调用点绑层
 * {@code "coe"} 改道），<b>配方 id 逐字不变</b>（= 手写文件的相对路径，见
 * {@link #generate} 里每条显式给出的 {@code name}）。</p>
 *
 * <h2>⚠ 本配方类型<b>不</b>允许写时长（批 1 教训的逐族核实结论）</h2>
 * <p>批 1 踩过的坑：{@code GrindingRecipe} 没覆写 {@code canSpecifyDuration()} ⇒
 * {@code ProcessingRecipe#validate()} 的
 * {@code if (processingDuration &gt; 0 &amp;&amp; !canSpecifyDuration())} 直接报错，而
 * {@code ProcessingRecipe#codec(...)} 把 {@code validate()} 包成 {@code MapCodec#validate(...)}
 * ⇒ <b>编、解码两个方向都跑它</b>（datagen 写盘抛异常、运行期 RecipeManager 整条丢弃）。</p>
 * <p>本族的核实结论（对 Create 6.0.10-280 的源码，<b>不是</b>推测）：
 * {@code create:pressing} 的注册类是 {@code PressingRecipe}，它<b>没有</b>覆写
 * {@code canSpecifyDuration()}，用的是 {@code ProcessingRecipe} 的默认实现 {@code false}。
 * ⇒ 本族<b>绝不许</b>调 {@code duration(...)}；手写版也没有 {@code processing_time} 键，
 * 所以这里是"什么都不写"而不是"写 0"。</p>
 *
 * <h2>为什么显式传 id、不按产物反推</h2>
 * <p>本族文件名与产物恰好一一对应（{@code x_ingot.json} → {@code x_sheet}），但全模组有
 * <b>12 组 basename 碰撞</b>（例如 {@code topaz_ingot} 同时在 {@code pressing/} 与
 * {@code rolling/} 下），"按产物名反推 id"在那些族里会生成到错的路径上。所以这里沿用批 1 的
 * 口径：每条都把 id 的<b>路径段</b>显式写成第一个参数。</p>
 *
 * <h2>取值口径</h2>
 * <ul>
 *   <li>配方类型：{@link AllRecipeTypes#PRESSING}。取<b>它已注册的那个序列化器</b>的工厂
 *       （{@code StandardProcessingRecipe.Serializer#factory()}）来建配方——这样生成期用的
 *       工厂与运行期反序列化用的是同一个实例，类型写错会在 datagen 期直接抛，而不是静默生成
 *       一份玩家加载不了的 JSON（批 1 的 {@code CoeGrindingRecipeProvider} 同款手法）。</li>
 *   <li>{@code id} 只给<b>名字</b>：{@code StandardProcessingRecipe.Builder#build(RecipeOutput)}
 *       自己会把 {@code 配方类型 id 的 path + "/"} 前缀加上（本类型 ⇒ {@code pressing/}），
 *       与手写路径逐字相同。</li>
 *   <li>原料用 {@link AllGemTags} 的 {@code c:ingots/<材料>} 标签（全模组标签的单一真源），
 *       不用手写字符串——这样"标签改名"只会在一处发生。</li>
 * </ul>
 *
 * <p><b>调用方</b>：{@link CoeRecipeProvider#generate} 的末尾（批 1 两族之后）。根工程的
 * {@code data/RecipeProvider#buildRecipes} 已经把 {@code CoeRecipeProvider.generate} 的
 * {@code RecipeOutput} 包成了「本层 coe」的（{@code bind(output, "coe")}），本类跟着同一个
 * output 走即可，<b>不需要动根工程的任何一行</b>（红线：根 {@code src} 不许动）。</p>
 */
public final class CoePressingRecipeProvider {

    /**
     * 本族 4 条配方，调用顺序无契约（每条一个文件、互不覆盖），这里按材料序
     * 玉 → 黄玉 → 蓝宝石 → 星辉石 排列。
     *
     * <p>每条的第一个参数<b>就是</b>配方 id 的路径段（{@code pressing/<name>}）——不是从产物反推的。</p>
     */
    public static void generate(RecipeOutput output) {
        ingot(output, "jade_ingot", AllGemTags.JADE, CoeItems.JADE_SHEET.get());
        ingot(output, "topaz_ingot", AllGemTags.TOPAZ, CoeItems.TOPAZ_SHEET.get());
        ingot(output, "sapphire_ingot", AllGemTags.SAPPHIRE, CoeItems.SAPPHIRE_SHEET.get());
        ingot(output, "stellarstone_ingot", AllGemTags.STELLARSTONE, CoeItems.STELLARSTONE_SHEET.get());
    }

    /**
     * 单条压片配方：{@code c:ingots/<材料>} 标签 → 1 块板。
     *
     * <p>{@code output(sheet)} 的 {@code count}/{@code chance} 取 codec 默认值 1（JSON 里两个键都不写），
     * 与手写版只写 {@code {"id": …}} 逐字相同。</p>
     */
    private static void ingot(RecipeOutput output, String name, AllGemTags gem, ItemLike sheet) {
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc(name))
            .require(gem.ingots)
            .output(sheet)
            .build(output);
    }

    /**
     * 本配方类型<b>已注册</b>的序列化器工厂（见类注释"取值口径"）。
     *
     * <p>{@code AllRecipeTypes#getSerializer()} 的返回类型是泛型 {@code T extends RecipeSerializer<?>}，
     * 赋值处按目标类型推断出 {@code Serializer<PressingRecipe>}；若哪天 {@code PRESSING} 换成
     * 别的序列化器，这里会抛 {@code ClassCastException}（响亮的失败，而不是静默生成错的 JSON）。</p>
     */
    private static StandardProcessingRecipe.Factory<PressingRecipe> factory() {
        StandardProcessingRecipe.Serializer<PressingRecipe> serializer = AllRecipeTypes.PRESSING.getSerializer();
        return serializer.factory();
    }

    private CoePressingRecipeProvider() {}
}
