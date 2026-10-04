package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.AllGemTags;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;

/**
 * <b>洗涤配方（{@code create:splashing}）的生成动作</b>（配方迁移 批 2）。
 *
 * <p>本族原先<b>手写</b>在
 * {@code coe/src/main/resources/data/createoreexpansion/recipe/splashing/}（4 条：四种
 * {@code c:crushed_raw_materials/<材料>} 各洗出一串碎粒/额外掉落），本批改由本类产出——
 * 产物落点从 {@code src/main/resources} 换到 {@code coe/src/generated/resources}，
 * <b>配方 id 逐字不变</b>（= 手写文件的相对路径，见 {@link #generate} 里每条显式给出的
 * {@code name}）。</p>
 *
 * <h2>命名绕口令：{@code create:splashing} 的 Gen 抽象类叫 {@code WashingRecipeGen}</h2>
 * <p>Create 自己的 datagen 基类是 {@code api/data/recipe/WashingRecipeGen}（"洗涤"的英文在
 * Create 里叫 washing），而注册的配方类型 id 与注册类分别是 {@code create:splashing} /
 * {@link SplashingRecipe}。本类<b>不用</b>那个 Gen 抽象类（理由见 {@link CoeRecipeProvider}
 * 类注释的"⛔ 不用 Create 自带 *RecipeGen"一段），只借 {@link AllRecipeTypes#SPLASHING}
 * 已注册的序列化器；这里点出名字差异，免得下一个人按 {@code SplashingRecipeGen} 去找而找不到。</p>
 *
 * <h2>⚠ 本配方类型<b>不</b>允许写时长（批 1 教训的逐族核实结论）</h2>
 * <p>{@code SplashingRecipe} <b>没有</b>覆写 {@code canSpecifyDuration()}，用
 * {@code ProcessingRecipe} 的默认实现 {@code false}。而
 * {@code ProcessingRecipe#codec(...)} 把 {@code validate()} 包成 {@code MapCodec#validate(...)}
 * ⇒ 编、解码两个方向都会跑那句
 * {@code if (processingDuration &gt; 0 &amp;&amp; !canSpecifyDuration())}。
 * ⇒ 本族<b>绝不许</b>调 {@code duration(...)}；手写版也没有 {@code processing_time} 键。</p>
 *
 * <h2>为什么显式传 id、不按产物反推</h2>
 * <p>全模组有 <b>12 组 basename 碰撞</b>（例如 {@code topaz_ingot} 同时在 {@code pressing/}
 * 与 {@code rolling/} 下），"按产物名反推 id"在那些族里会生成到错的路径上。所以沿用批 1 的口径：
 * 每条都把 id 的<b>路径段</b>显式写成第一个参数。</p>
 *
 * <h2>取值口径</h2>
 * <ul>
 *   <li>配方类型：{@link AllRecipeTypes#SPLASHING}；工厂取法同
 *       {@link CoePressingRecipeProvider}（已注册的同一个序列化器实例）。</li>
 *   <li>原料用 {@link AllGemTags} 的 {@code c:crushed_raw_materials/<材料>} 标签。</li>
 *   <li>多输出<b>逐条对齐</b>手写 JSON：{@code output(chance, item, count)} 三个参数与
 *       {@code ProcessingOutput} 的 {@code chance}/{@code count} 字段一一对应；
 *       {@code chance} 传 1（不写）与手写版"没有 chance 键"等价，
 *       {@code count} 传 1 时 codec 省略该键（默认值）。</li>
 *   <li>外部物品用 Create / 原版的注册项常量（{@code AllItems.COPPER_NUGGET}、
 *       {@code Items.GOLD_NUGGET} …），不手写 {@code ResourceLocation} 字符串。</li>
 * </ul>
 *
 * <p><b>调用方</b>：{@link CoeRecipeProvider#generate} 的末尾。理由见
 * {@link CoePressingRecipeProvider} 类注释末段（跟着同一个 output 走 = 自动拿到「本层 coe」的
 * 落点归属；根工程一行不用改）。</p>
 */
public final class CoeSplashingRecipeProvider {

    /**
     * 本族 4 条配方，调用顺序无契约（每条一个文件、互不覆盖），这里按材料序
     * 玉 → 黄玉 → 蓝宝石 → 星辉石 排列。
     *
     * <p>每条的第一个参数<b>就是</b>配方 id 的路径段（{@code splashing/<name>}）。
     * 每条的输出链按手写 JSON 的 {@code results} 数组<b>逐项、逐序</b>写成，方便逐条核对
     * chance 与 count。</p>
     */
    public static void generate(RecipeOutput output) {
        // 玉：3+3(75%)+3(50%) 碎粒 + 3(50%) 金粒 + 1(25%) 钻石
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc("crushed_jade_ore"))
            .require(AllGemTags.JADE.crushedRawOres)
            .output(CoeItems.JADE_NUGGET.get(), 3)
            .output(0.75f, CoeItems.JADE_NUGGET.get(), 3)
            .output(0.5f, CoeItems.JADE_NUGGET.get(), 3)
            .output(0.5f, Items.GOLD_NUGGET, 3)
            .output(0.25f, Items.DIAMOND)
            .build(output);

        // 黄玉：3+3(75%)+3(50%) 碎粒 + 3(50%) 铜粒 + 1(25%) 绿宝石
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc("crushed_topaz_ore"))
            .require(AllGemTags.TOPAZ.crushedRawOres)
            .output(CoeItems.TOPAZ_NUGGET.get(), 3)
            .output(0.75f, CoeItems.TOPAZ_NUGGET.get(), 3)
            .output(0.5f, CoeItems.TOPAZ_NUGGET.get(), 3)
            .output(0.5f, AllItems.COPPER_NUGGET.get(), 3)
            .output(0.25f, Items.EMERALD)
            .build(output);

        // 蓝宝石：3+3(75%)+3(50%) 碎粒 + 3(50%) 金粒 + 1(50%) 下界合金碎片
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc("crushed_sapphire_ore"))
            .require(AllGemTags.SAPPHIRE.crushedRawOres)
            .output(CoeItems.SAPPHIRE_NUGGET.get(), 3)
            .output(0.75f, CoeItems.SAPPHIRE_NUGGET.get(), 3)
            .output(0.5f, CoeItems.SAPPHIRE_NUGGET.get(), 3)
            .output(0.5f, Items.GOLD_NUGGET, 3)
            .output(0.5f, Items.NETHERITE_SCRAP)
            .build(output);

        // 星辉石：3+3(75%)+3(50%) 碎粒 + 3(25%) 玉粒 + 3(25%) 黄玉粒 + 3(12.5%) 蓝宝石粒 + 3(12.5%) 下界合金碎片
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc("crushed_stellarstone_ore"))
            .require(AllGemTags.STELLARSTONE.crushedRawOres)
            .output(CoeItems.STELLARSTONE_NUGGET.get(), 3)
            .output(0.75f, CoeItems.STELLARSTONE_NUGGET.get(), 3)
            .output(0.5f, CoeItems.STELLARSTONE_NUGGET.get(), 3)
            .output(0.25f, CoeItems.JADE_NUGGET.get(), 3)
            .output(0.25f, CoeItems.TOPAZ_NUGGET.get(), 3)
            .output(0.125f, CoeItems.SAPPHIRE_NUGGET.get(), 3)
            .output(0.125f, Items.NETHERITE_SCRAP, 3)
            .build(output);
    }

    /** 本配方类型<b>已注册</b>的序列化器工厂（见类注释"取值口径"，同 {@link CoePressingRecipeProvider}）。 */
    private static StandardProcessingRecipe.Factory<SplashingRecipe> factory() {
        StandardProcessingRecipe.Serializer<SplashingRecipe> serializer = AllRecipeTypes.SPLASHING.getSerializer();
        return serializer.factory();
    }

    private CoeSplashingRecipeProvider() {}
}
