package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.level.ItemLike;

/**
 * <b>锯切配方（{@code create:cutting}）的生成动作</b>（配方迁移 批 2）。
 *
 * <p>本族原先<b>手写</b>在
 * {@code coe/src/main/resources/data/createoreexpansion/recipe/cutting/}（8 条：四种材料的大/小
 * 碎片各一条，锯成对应碎粒），本批改由本类产出——产物落点从 {@code src/main/resources} 换到
 * {@code coe/src/generated/resources}，<b>配方 id 逐字不变</b>（= 手写文件的相对路径，见
 * {@link #generate} 里每条显式给出的 {@code name}）。</p>
 *
 * <h2>✅ 本配方类型<b>允许</b>写时长 —— 迁移后必须仍是 100 tick</h2>
 * <p>核实结论（对 Create 6.0.10-280 的源码，<b>不是</b>推测）：
 * {@code create:cutting} 的注册类 {@link CuttingRecipe} <b>覆写</b>了
 * {@code canSpecifyDuration()} 并返回 {@code true}。所以它不会被批 1 那个
 * {@code ProcessingRecipe#validate()} 的时长守卫打掉，
 * {@code processing_time} 会<b>真的落盘、也真的生效</b>。</p>
 * <p>8 条手写 JSON 的 {@code processing_time} <b>一律 100</b>（批 0 的 {@code 470aa174} 已把
 * 驼峰键 {@code processingTime} 改成 codec 认的 {@code processing_time}）。所以本族<b>必须</b>
 * 每条都调 {@code duration(DURATION)}（{@value #DURATION}），<b>少写一条就是行为变化</b>：
 * 键缺失 ⇒ codec 取默认 0 ⇒ 机械锯瞬时完成。逐条核对由批 2 的等价脚本负责。</p>
 *
 * <h2>为什么显式传 id、不按产物反推</h2>
 * <p>全模组有 <b>12 组 basename 碰撞</b>（例如 {@code topaz_ingot} 同时在 {@code pressing/}
 * 与 {@code rolling/} 下），"按产物名反推 id"在那些族里会生成到错的路径上。本族尤其容易踩：
 * 输入物就叫 {@code <材料>_small_shard}，而 {@code grinding/} 下已经有一条<b>同 basename 的
 * 生成配方</b>（{@code grinding/jade_small_shard}）——按输入物反推 id 会直接撞进 {@code grinding/}。
 * 所以沿用批 1 的口径：每条都把 id 的<b>路径段</b>显式写成第一个参数。</p>
 *
 * <h2>取值口径</h2>
 * <ul>
 *   <li>配方类型：{@link AllRecipeTypes#CUTTING}；工厂取法同
 *       {@link CoePressingRecipeProvider}（已注册的同一个序列化器实例）。</li>
 *   <li>多输出<b>逐条对齐</b>手写 JSON：{@code output(chance, item, count)} 的参数与
 *       {@code ProcessingOutput} 的 {@code chance}/{@code count} 字段一一对应。</li>
 * </ul>
 *
 * <p><b>调用方</b>：{@link CoeRecipeProvider#generate} 的末尾。理由见
 * {@link CoePressingRecipeProvider} 类注释末段（跟着同一个 output 走 = 自动拿到「本层 coe」的
 * 落点归属；根工程一行不用改）。</p>
 */
public final class CoeCuttingRecipeProvider {

    /**
     * 本族 8 条配方的处理时长（tick）。
     *
     * <p>手写版 8 条一律 100；迁移后必须仍是 100，否则就是行为变化（见类注释）。</p>
     */
    private static final int DURATION = 100;

    /**
     * 本族 8 条配方，调用顺序无契约（每条一个文件、互不覆盖），这里按材料序
     * 玉 → 黄玉 → 蓝宝石 → 星辉石、每种先大后小 排列。
     *
     * <p>每条的第一个参数<b>就是</b>配方 id 的路径段（{@code cutting/<name>}）。</p>
     */
    public static void generate(RecipeOutput output) {
        // 大碎片：基础产量玉=4、其余=5，另加 2(50%) + 2(25%)
        bigShard(output, "jade_big_shard", CoeItems.JADE_BIG_SHARD.get(), CoeItems.JADE_NUGGET.get(), 4);
        bigShard(output, "topaz_big_shard", CoeItems.TOPAZ_BIG_SHARD.get(), CoeItems.TOPAZ_NUGGET.get(), 5);
        bigShard(output, "sapphire_big_shard", CoeItems.SAPPHIRE_BIG_SHARD.get(), CoeItems.SAPPHIRE_NUGGET.get(), 5);
        bigShard(output, "stellarstone_big_shard", CoeItems.STELLARSTONE_BIG_SHARD.get(),
            CoeItems.STELLARSTONE_NUGGET.get(), 5);

        // 小碎片：基础产量 3，另加 1(50%)
        smallShard(output, "jade_small_shard", CoeItems.JADE_SMALL_SHARD.get(), CoeItems.JADE_NUGGET.get());
        smallShard(output, "topaz_small_shard", CoeItems.TOPAZ_SMALL_SHARD.get(), CoeItems.TOPAZ_NUGGET.get());
        smallShard(output, "sapphire_small_shard", CoeItems.SAPPHIRE_SMALL_SHARD.get(), CoeItems.SAPPHIRE_NUGGET.get());
        smallShard(output, "stellarstone_small_shard", CoeItems.STELLARSTONE_SMALL_SHARD.get(),
            CoeItems.STELLARSTONE_NUGGET.get());
    }

    /**
     * 大碎片：{@code base} 颗必得 + 2 颗 50% + 2 颗 25%，时长 {@value #DURATION}。
     *
     * <p>三个 {@code count} 都是显式写的（4/5、2、2），因为都不等于 codec 默认值 1。</p>
     */
    private static void bigShard(RecipeOutput output, String name, ItemLike shard, ItemLike nugget, int base) {
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc(name))
            .require(shard)
            .output(nugget, base)
            .output(0.5f, nugget, 2)
            .output(0.25f, nugget, 2)
            .duration(DURATION)
            .build(output);
    }

    /**
     * 小碎片：3 颗必得 + 1 颗 50%，时长 {@value #DURATION}。
     *
     * <p>手写 JSON 里第二条的 {@code count: 1} 是<b>显式写出的 codec 默认值</b>：
     * {@code ProcessingOutput} 的 {@code count} 用 {@code optionalFieldOf("count", 1)}，
     * 等于默认值就不落盘 ⇒ 生成物里只有 {@code chance}。语义等价（1 颗 50%）。</p>
     */
    private static void smallShard(RecipeOutput output, String name, ItemLike shard, ItemLike nugget) {
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc(name))
            .require(shard)
            .output(nugget, 3)
            .output(0.5f, nugget)
            .duration(DURATION)
            .build(output);
    }

    /** 本配方类型<b>已注册</b>的序列化器工厂（见类注释"取值口径"，同 {@link CoePressingRecipeProvider}）。 */
    private static StandardProcessingRecipe.Factory<CuttingRecipe> factory() {
        StandardProcessingRecipe.Serializer<CuttingRecipe> serializer = AllRecipeTypes.CUTTING.getSerializer();
        return serializer.factory();
    }

    private CoeCuttingRecipeProvider() {}
}
