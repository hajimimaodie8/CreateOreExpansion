package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.AllGemTags;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * <b>粉碎配方（{@code create:crushing}）的生成动作</b>（配方迁移 批 2）。
 *
 * <p>本族原先<b>手写</b>在
 * {@code coe/src/main/resources/data/createoreexpansion/recipe/crushing/}（13 条：5 条矿物方块 +
 * 4 条粗矿 + 4 条粗矿块），本批改由本类产出——产物落点从 {@code src/main/resources} 换到
 * {@code coe/src/generated/resources}，<b>配方 id 逐字不变</b>（= 手写文件的相对路径，见
 * {@link #generate} 里每条显式给出的 {@code name}）。</p>
 *
 * <h2>✅ 本配方类型<b>允许</b>写时长 —— 350 / 400 / 450 逐条保留</h2>
 * <p>核实结论（对 Create 6.0.10-280 的源码，<b>不是</b>推测）：{@code create:crushing} 的注册类
 * {@link CrushingRecipe} 本身没写，但它的父类
 * {@code AbstractCrushingRecipe} <b>覆写</b>了 {@code canSpecifyDuration()} 并返回
 * {@code true}。所以它不会被批 1 那个 {@code ProcessingRecipe#validate()} 的时长守卫打掉，
 * {@code processing_time} 会真的落盘、也真的生效。</p>
 * <p>三档时长不是随手取的：<b>矿物方块 350</b>（深板岩变体 <b>450</b>）、<b>粗矿与粗矿块 400</b>。
 * ⇒ 本族每条都必须显式调 {@code duration(...)}，<b>少写一条就是行为变化</b>
 * （键缺失 ⇒ codec 取默认 0 ⇒ 粉碎轮瞬时出料）。逐条核对由批 2 的等价脚本负责。</p>
 *
 * <h2>⚠ 带 chance 的多输出必须逐条对齐</h2>
 * <p>本族是四族里唯一"多输出 + 概率 + 多档 count"齐全的族（最多 4 个输出，含
 * {@code 0.125} 这种三位小数）。手写 JSON 的 {@code count} 与 {@code chance} 是<b>逐项</b>
 * 写着的，所以这里也用 {@code output(chance, item, count)} 逐项写出来，方便与手写 JSON 的
 * {@code results} 数组<b>按下标一一对照</b>；顺序也有意义（{@code results[0]} 是主产物，
 * {@code ProcessingRecipe#rollResults} 对下标 0 有 {@code forcedResult} 特判）。</p>
 *
 * <h2>为什么显式传 id、不按产物反推</h2>
 * <p>本族是"按产物反推必错"的典型：{@code raw_jade} 与 {@code raw_jade_block} 的
 * <b>主产物同是</b> {@code crushed_jade_ore}；而输入方块 {@code topaz_ore} 的名字又与
 * {@code ore/} 下别的族同名（全模组 <b>12 组 basename 碰撞</b>）。所以沿用批 1 的口径：
 * 每条都把 id 的<b>路径段</b>显式写成第一个参数。</p>
 *
 * <h2>取值口径</h2>
 * <ul>
 *   <li>配方类型：{@link AllRecipeTypes#CRUSHING}；工厂取法同
 *       {@link CoePressingRecipeProvider}（已注册的同一个序列化器实例）。</li>
 *   <li>粗矿/粗矿块原料用 {@link AllGemTags} 的 {@code c:raw_materials/<材料>} 与
 *       {@code c:storage_blocks/raw_<材料>} 标签；矿物方块用 {@link CoeBlocks} 的注册项。</li>
 *   <li>经验粒用 Create 的 {@code AllItems.EXP_NUGGET}（{@code create:experience_nugget}），
 *       不手写 id 字符串。</li>
 * </ul>
 *
 * <p><b>调用方</b>：{@link CoeRecipeProvider#generate} 的末尾。理由见
 * {@link CoePressingRecipeProvider} 类注释末段（跟着同一个 output 走 = 自动拿到「本层 coe」的
 * 落点归属；根工程一行不用改）。</p>
 */
public final class CoeCrushingRecipeProvider {

    /** 矿物方块（含下界变体）的时长：未变质的 350 tick。 */
    private static final int DURATION_ORE = 350;

    /** 深板岩矿物方块的时长：450 tick（比石头变体更硬）。 */
    private static final int DURATION_DEEPSLATE_ORE = 450;

    /** 粗矿与粗矿块的时长：400 tick。 */
    private static final int DURATION_RAW = 400;

    /**
     * 本族 13 条配方，调用顺序无契约（每条一个文件、互不覆盖），这里按
     * 五种矿物方块（先石头/下界再看深板岩变体）→ 四种粗矿 → 四种粗矿块 排列。
     *
     * <p>每条的第一个参数<b>就是</b>配方 id 的路径段（{@code crushing/<name>}）。</p>
     */
    public static void generate(RecipeOutput output) {
        // ===== 矿物方块：主产物 + 概率副产物 + 经验粒 + 概率石渣 =====
        // 玉矿（石头变体）：主 1 必得、次 1 颗 75%、经验 75%、圆石 12.5%
        ore(output, "jade_ore", CoeBlocks.JADE_ORE.get(), CoeItems.CRUSHED_JADE_ORE.get(),
            1, 0.75f, Items.COBBLESTONE, DURATION_ORE);
        // 玉矿（深板岩变体）：主 2 必得、次 1 颗 25%、经验 75%、深板岩圆石 12.5%
        ore(output, "deepslate_jade_ore", CoeBlocks.DEEPSLATE_JADE_ORE.get(), CoeItems.CRUSHED_JADE_ORE.get(),
            2, 0.25f, Items.COBBLED_DEEPSLATE, DURATION_DEEPSLATE_ORE);
        // 黄玉矿（石头变体）：主 2 必得、次 1 颗 75%
        ore(output, "topaz_ore", CoeBlocks.TOPAZ_ORE.get(), CoeItems.CRUSHED_TOPAZ_ORE.get(),
            2, 0.75f, Items.COBBLESTONE, DURATION_ORE);
        // 黄玉矿（深板岩变体）：主 2 必得、次 1 颗 25%
        ore(output, "deepslate_topaz_ore", CoeBlocks.DEEPSLATE_TOPAZ_ORE.get(), CoeItems.CRUSHED_TOPAZ_ORE.get(),
            2, 0.25f, Items.COBBLED_DEEPSLATE, DURATION_DEEPSLATE_ORE);
        // 蓝宝石矿（下界变体）：主 1 必得、次 1 颗 75%、经验 75%、下界岩 12.5%
        ore(output, "nether_sapphire_ore", CoeBlocks.NETHER_SAPPHIRE_ORE.get(), CoeItems.CRUSHED_SAPPHIRE_ORE.get(),
            1, 0.75f, Items.NETHERRACK, DURATION_ORE);

        // ===== 粗矿：1 份粗矿 → 1 份碎矿 + 75% 经验粒 =====
        raw(output, "raw_jade", AllGemTags.JADE, CoeItems.CRUSHED_JADE_ORE.get());
        raw(output, "raw_topaz", AllGemTags.TOPAZ, CoeItems.CRUSHED_TOPAZ_ORE.get());
        raw(output, "raw_sapphire", AllGemTags.SAPPHIRE, CoeItems.CRUSHED_SAPPHIRE_ORE.get());
        raw(output, "raw_stellarstone", AllGemTags.STELLARSTONE, CoeItems.CRUSHED_STELLARSTONE_ORE.get());

        // ===== 粗矿块：9 份碎矿必得 + 18 颗经验粒的 75% =====
        rawBlock(output, "raw_jade_block", AllGemTags.JADE, CoeItems.CRUSHED_JADE_ORE.get());
        rawBlock(output, "raw_topaz_block", AllGemTags.TOPAZ, CoeItems.CRUSHED_TOPAZ_ORE.get());
        rawBlock(output, "raw_sapphire_block", AllGemTags.SAPPHIRE, CoeItems.CRUSHED_SAPPHIRE_ORE.get());
        rawBlock(output, "raw_stellarstone_block", AllGemTags.STELLARSTONE, CoeItems.CRUSHED_STELLARSTONE_ORE.get());
    }

    /**
     * 矿物方块：{@code primaryCount} 份必得主产物 + 1 份 {@code secondaryChance} 概率的追加主产物
     * + 1 颗 75% 经验粒 + 1 份 12.5% 的 {@code byproduct} 石渣。
     *
     * <p>{@code chance = 1} 的输出不写 {@code chance} 键（codec 默认值），
     * {@code count = 1} 的输出不写 {@code count} 键——两处都与手写 JSON 的省略形态一致。</p>
     */
    private static void ore(RecipeOutput output, String name, ItemLike oreBlock, ItemLike crushed,
                            int primaryCount, float secondaryChance, ItemLike byproduct, int duration) {
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc(name))
            .require(oreBlock)
            .output(crushed, primaryCount)
            .output(secondaryChance, crushed)
            .output(0.75f, AllItems.EXP_NUGGET.get())
            .output(0.125f, byproduct)
            .duration(duration)
            .build(output);
    }

    /** 粗矿：{@code c:raw_materials/<材料>} 标签 → 1 份碎矿 + 1 颗 75% 经验粒。 */
    private static void raw(RecipeOutput output, String name, AllGemTags gem, ItemLike crushed) {
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc(name))
            .require(gem.rawOres)
            .output(crushed)
            .output(0.75f, AllItems.EXP_NUGGET.get())
            .duration(DURATION_RAW)
            .build(output);
    }

    /** 粗矿块：{@code c:storage_blocks/raw_<材料>} 标签 → 9 份碎矿 + 18 颗 75% 经验粒。 */
    private static void rawBlock(RecipeOutput output, String name, AllGemTags gem, ItemLike crushed) {
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc(name))
            .require(gem.itemStorageRawBlocks)
            .output(crushed, 9)
            .output(0.75f, AllItems.EXP_NUGGET.get(), 18)
            .duration(DURATION_RAW)
            .build(output);
    }

    /** 本配方类型<b>已注册</b>的序列化器工厂（见类注释"取值口径"，同 {@link CoePressingRecipeProvider}）。 */
    private static StandardProcessingRecipe.Factory<CrushingRecipe> factory() {
        StandardProcessingRecipe.Serializer<CrushingRecipe> serializer = AllRecipeTypes.CRUSHING.getSerializer();
        return serializer.factory();
    }

    private CoeCrushingRecipeProvider() {}
}
