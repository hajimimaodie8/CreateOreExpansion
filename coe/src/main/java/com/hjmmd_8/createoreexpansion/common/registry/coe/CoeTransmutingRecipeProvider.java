package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.transmuting.AllTransmutingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * <b>嬗变加工（{@code createoreexpansion:transmuting}）的生成动作</b>（配方迁移 批 3）。
 *
 * <p>本族原先<b>手写</b>在
 * {@code coe/src/main/resources/data/createoreexpansion/recipe/transmuting/}（15 条），本批改由本类
 * 产出——产物落点从 {@code src/main/resources} 换到 {@code coe/src/generated/resources}，配方 id
 * 逐字不变（= 手写文件的相对路径，见 {@link #generate} 里每条显式给出的 {@code name}）。</p>
 *
 * <h2>层绑定 = {@code "coe"}（作者裁定，本批<b>不</b>搬去第三层）</h2>
 * <p>这 15 条按<b>它们现在所在的位置</b>绑第一层 {@code coe}：类型声明
 * {@code CoeRecipeTypes.TRANSMUTING} 是 {@code LayerRecipeType.processing("TRANSMUTING",
 * AllTransmutingRecipe::new)}（<b>类型住在 :coe</b>），生成动作也写在 {@code :coe} 的
 * {@link CoeRecipeProvider} 末尾。第三层 {@code transmutation} 只负责"善化"系列内容的扩展，
 * 本批没有把任何一条搬过去。落点由 {@code LayerRecipeRouter} 按调用点绑层决定：跟着
 * {@link CoeRecipeProvider#generate} 收到的同一个 {@code RecipeOutput}（= 根调用点已经
 * {@code bind(output, "coe")} 过的那个）写，落点自然是 {@code coe/src/generated/resources}。</p>
 *
 * <h2>✅ 本配方类型<b>不</b>允许写时长（逐族判决，批 1 教训）</h2>
 * <p>批 1 踩过的坑：{@code GrindingRecipe} 没覆写 {@code canSpecifyDuration()}，而
 * {@code ProcessingRecipe#validate()} 里有
 * {@code if (processingDuration &gt; 0 &amp;&amp; !canSpecifyDuration())} 直接报错；
 * {@code ProcessingRecipe#codec(...)} 把 {@code validate()} 包成 {@code MapCodec#validate(...)}，
 * 于是<b>编、解码两个方向都跑它</b>（datagen 写盘抛异常、运行期 {@code RecipeManager} 整条丢弃）。</p>
 * <p>本族的核实结论（对 Create 6.0.10-280 的<b>源码</b>，不是推测）：{@code createoreexpansion:transmuting}
 * 的注册类是 {@link AllTransmutingRecipe}，它<b>没有</b>覆写 {@code canSpecifyDuration()}，用的是
 * {@code ProcessingRecipe} 的默认实现 {@code false}（{@code ProcessingRecipe.java:72-74}）；
 * 它只覆写了 {@code getMaxInputCount() = 1} 与 {@code getMaxOutputCount() = 12}。
 * ⇒ 本族<b>绝不许</b>调 {@code duration(...)}；手写版 15 条里也没有任何 {@code processing_time}
 * 键，所以这里是"什么都不写"而不是"写 0"。（同理不写 {@code requiresHeat(...)}：
 * {@code canRequireHeat()} 默认也是 {@code false}。）</p>
 *
 * <h2>概率输出的写法</h2>
 * <p>本族是"单结果 + 可选第二概率结果"的族：3 条是"主产物 + 25% 追加一份"，2 条是"唯一产物但只有
 * 90% / 50% 概率"，其余 10 条是必得单产物。概率用 {@code output(chance, item)} 写；
 * {@code chance = 1} 与 {@code count = 1} 都由 codec 的 {@code optionalFieldOf} 省略
 * （{@code ProcessingOutput.java:127-139}），所以手写版里显式写的 {@code "count": 1} 不会回写
 * ——那属于允许的"codec 省略显式默认值"差异类。</p>
 *
 * <h2>为什么显式传 id、不按产物反推</h2>
 * <p>本族文件名全部是 {@code A_to_B} 形式，按产物反推会丢掉 {@code A_to} 那半截；而全模组另有
 * <b>12 组 basename 碰撞</b>。所以沿用批 1/批 2 的口径：每条都把 id 的<b>路径段</b>显式写成第一个参数。</p>
 *
 * <h2>取值口径</h2>
 * <ul>
 *   <li>配方类型工厂：{@code CoeRecipeTypes.TRANSMUTING.getSerializer().factory()} —— 取
 *       <b>已注册的那个序列化器实例</b>的工厂（生成期与运行期共用同一个工厂；类型写错在 datagen 期
 *       即抛）。注意 {@code TransmutationRecipeTypes.TRANSMUTING} 只是它的同名转发别名，
 *       <b>是同一个对象</b>；这里按"类型声明住哪"取 {@code CoeRecipeTypes} 那一个。</li>
 *   <li>{@code id} 只给<b>名字</b>：{@code ProcessingRecipeBuilder#build(RecipeOutput)} 自己会加上
 *       {@code 配方类型 id 的 path + "/"} 前缀（本类型 ⇒ {@code transmuting/}），与手写路径逐字相同。</li>
 *   <li>原料一律用<b>具体物品</b>（{@link CoeItems} / {@link CoeBlocks} 的注册项），不用标签
 *       ——手写版就是 {@code "item"} 而不是 {@code "tag"}，改成标签会扩大匹配面（= 行为变化）。</li>
 * </ul>
 *
 * <p><b>调用方</b>：{@link CoeRecipeProvider#generate} 的末尾（批 1、批 2 六族与批 3 的雷击族之后）。
 * 理由同 {@link CoeLightningRecipeProvider}。</p>
 */
public final class CoeTransmutingRecipeProvider {

    /**
     * 本族 15 条配方，调用顺序无契约（每条一个文件、互不覆盖），这里按
     * 三条"主 + 25% 副" → 五条蓝宝石→红宝石 → 一条 90% → 五条星辉石→圣石 → 一条 50% 排列。
     *
     * <p>每条的第一个参数<b>就是</b>配方 id 的路径段（{@code transmuting/<name>}）。</p>
     */
    public static void generate(RecipeOutput output) {
        // ===== 主产物必得 + 25% 追加一份（三种原版材料） =====
        bonus(output, "amethyst_shard_to_quartz", Items.AMETHYST_SHARD, Items.QUARTZ);
        bonus(output, "diamond_to_emerald", Items.DIAMOND, Items.EMERALD);
        bonus(output, "redstone_to_glowstone_dust", Items.REDSTONE, Items.GLOWSTONE_DUST);

        // ===== 蓝宝石 → 红宝石（5 条，全部必得单产物） =====
        single(output, "sapphire_block_to_ruby_block", CoeBlocks.SAPPHIRE_BLOCK.get(), CoeBlocks.RUBY_BLOCK.get());
        single(output, "sapphire_ingot_to_ruby_ingot", CoeItems.SAPPHIRE_INGOT.get(), CoeItems.RUBY_INGOT.get());
        single(output, "sapphire_rod_to_ruby_rod", CoeItems.SAPPHIRE_ROD.get(), CoeItems.RUBY_ROD.get());
        single(output, "sapphire_sheet_to_ruby_sheet", CoeItems.SAPPHIRE_SHEET.get(), CoeItems.RUBY_SHEET.get());
        single(output, "sapphire_wire_to_ruby_wire", CoeItems.SAPPHIRE_WIRE.get(), CoeItems.RUBY_WIRE.get());

        // ===== 概率单产物：骷髅头 → 凋灵骷髅头（90%） =====
        chance(output, "skeleton_skull_to_wither_skeleton_skull",
            Items.SKELETON_SKULL, Items.WITHER_SKELETON_SKULL, 0.9f);

        // ===== 星辉石 → 圣石（5 条，全部必得单产物） =====
        single(output, "stellarstone_block_to_sanctstone_block",
            CoeBlocks.STELLARSTONE_BLOCK.get(), CoeBlocks.SANCTSTONE_BLOCK.get());
        single(output, "stellarstone_ingot_to_sanctstone_ingot",
            CoeItems.STELLARSTONE_INGOT.get(), CoeItems.SANCTSTONE_INGOT.get());
        single(output, "stellarstone_rod_to_sanctstone_rod",
            CoeItems.STELLARSTONE_ROD.get(), CoeItems.SANCTSTONE_ROD.get());
        single(output, "stellarstone_sheet_to_sanctstone_sheet",
            CoeItems.STELLARSTONE_SHEET.get(), CoeItems.SANCTSTONE_SHEET.get());
        single(output, "stellarstone_wire_to_sanctstone_wire",
            CoeItems.STELLARSTONE_WIRE.get(), CoeItems.SANCTSTONE_WIRE.get());

        // ===== 概率单产物：糖 → 火药（50%） =====
        chance(output, "sugar_to_gunpowder", Items.SUGAR, Items.GUNPOWDER, 0.5f);
    }

    /**
     * 必得单产物：{@code from} → 1 份 {@code to}（{@code count}/{@code chance} 两个键都不写，
     * 取 codec 默认值 1）。
     */
    private static void single(RecipeOutput output, String name, ItemLike from, ItemLike to) {
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc(name))
            .require(from)
            .output(to)
            .build(output);
    }

    /**
     * 唯一产物但带概率：{@code from} → 1 份 {@code to}，只有 {@code chance} 的概率出料。
     *
     * <p>手写版这种形态是"一个 result、带 chance"（不是"两个 result"）；这里用
     * {@code output(chance, item)} 逐字对应，<b>不要</b>改成多输出。</p>
     */
    private static void chance(RecipeOutput output, String name, ItemLike from, ItemLike to, float chance) {
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc(name))
            .require(from)
            .output(chance, to)
            .build(output);
    }

    /**
     * 主产物必得 + 25% 概率追加一份同样的产物（两个独立 result，主产物在下标 0）。
     *
     * <p>顺序有意义：{@code results[0]} 是主产物，{@code ProcessingRecipe#rollResults} 对下标 0
     * 有 {@code forcedResult} 特判。概率写死 25% 是因为本族三条手写版全是 0.25。</p>
     */
    private static void bonus(RecipeOutput output, String name, ItemLike from, ItemLike to) {
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc(name))
            .require(from)
            .output(to)
            .output(0.25f, to)
            .build(output);
    }

    /**
     * 本配方类型<b>已注册</b>的序列化器工厂（见类注释"取值口径"）。
     *
     * <p>{@code CoeRecipeTypes.TRANSMUTING} 与 {@code TransmutationRecipeTypes.TRANSMUTING} 是
     * 同一个对象（后者是同名转发别名），所以这里取哪个都等价；按"类型声明住哪"取前者。</p>
     */
    private static StandardProcessingRecipe.Factory<AllTransmutingRecipe> factory() {
        StandardProcessingRecipe.Serializer<AllTransmutingRecipe> serializer =
            CoeRecipeTypes.TRANSMUTING.getSerializer();
        return serializer.factory();
    }

    private CoeTransmutingRecipeProvider() {}
}
