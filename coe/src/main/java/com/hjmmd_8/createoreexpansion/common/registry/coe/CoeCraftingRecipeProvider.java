package com.hjmmd_8.createoreexpansion.common.registry.coe;

import java.util.Map;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;

/**
 * <b>原版合成族（{@code minecraft:crafting_shaped} / {@code minecraft:crafting_shapeless}）的生成动作</b>
 * （配方迁移 批 4）。
 *
 * <p>本族 26 条原先<b>手写</b>在
 * {@code coe/src/main/resources/data/createoreexpansion/recipe/crafting/materials/}
 * （14 条有序合成 + 12 条无序合成），本批改由本类产出——产物落点从 {@code src/main/resources}
 * 换到 {@code coe/src/generated/resources}，<b>配方 id 逐字不变</b>。层绑定的机制与批 1/2/3 相同：
 * 跟着 {@link CoeRecipeProvider#generate} 收到的同一个 {@code RecipeOutput}
 * （= 根调用点已经 {@code bind(output, "coe")} 过的那个）写，落点自然是
 * {@code coe/src/generated/resources}。</p>
 *
 * <h2>⚠ 陷阱一：id 的 {@code crafting/materials/} 前缀必须显式写</h2>
 * <p>原版两个 builder（{@code ShapedRecipeBuilder} / {@code ShapelessRecipeBuilder}）在
 * {@code save(output)} 时用的默认 id 是 <b>{@code <命名空间>:<名字>}</b>——<b>不带任何目录前缀</b>
 * （{@code ShapedRecipeBuilder#save(RecipeOutput)} 走的是 {@code RecipeProvider.getDefaultRecipeId(item)}
 * 或 {@code save(output, id)} 里显式给的那个 id）。而本族手写文件的相对路径是
 * {@code crafting/materials/<名>.json}，也就是说配方 id 是
 * {@code createoreexpansion:crafting/materials/<名>}。</p>
 * <p>⇒ 若按"默认 id"产出，26 条配方会<b>集体改名</b>（{@code createoreexpansion:<名>}），
 * 这在数据包层面等于<b>配方搬家</b>：老存档里 {@code /recipe give} 过的 id、JEI 的收藏、
 * 任何按 id 引用它的东西全部失配，而<b>文件数一个不少、静态关卡全绿</b>。
 * 本类因此<b>不用</b>任何 builder 的默认 id：{@link #recipeId} 每次把
 * {@code crafting/materials/} 显式拼进 {@code CoeCore.modLoc(...)}。</p>
 *
 * <h2>⚠ 陷阱二：不许用 builder 的 {@code save(output, id)}——它会额外产出 advancement</h2>
 * <p>{@code ShapedRecipeBuilder#save(RecipeOutput, ResourceLocation)} /
 * {@code ShapelessRecipeBuilder#save(...)} 除了配方，还会往<b>同一个 {@code RecipeOutput}</b> 里
 * 再 {@code accept} 一个 advancement——实测 {@code ShapedRecipeBuilder.save} 的最后一行是
 * {@code recipeOutput.accept(id, shapedrecipe, advancement$builder.build(id.withPrefix("recipes/" + this.category.getFolderName() + "/")))}，
 * 即 {@code data/createoreexpansion/advancement/recipes/misc/crafting/materials/<名>.json}
 * （"第一次做出该物品"的配方解锁进度）。</p>
 * <p>而 {@code LayerRecipeRouter#relocate} <b>只改道 {@code data/<ns>/recipe/} 这一个子树</b>
 * （见该类字段 {@code relativeDir}），advancement 不在它的改写范围内 ⇒ 那些 advancement 会
 * <b>留在根输出</b> {@code src/generated/resources}。根工程不发布（{@code F1}/{@code E2} 正是
 * 断言根输出里没有产物），所以这些 advancement 会<b>静默消失</b>——而且<b>不会被 F1 抓到</b>
 * （F1 只扫 {@code /recipe/} 叶子），也不会被任何其他关卡抓到。</p>
 * <p>⇒ 正确做法是<b>直接构造配方对象</b>、用
 * {@code output.accept(id, recipe, null)} 的形态（{@code advancement = null} ⇒ 一个
 * advancement 都不产出）。这正是 {@code CoeRecipeProvider.dismantling} 既有的形状。
 * 签名已用 {@code javap} 对
 * {@code build/moddev/artifacts/neoforge-21.1.228-merged.jar} 实测：</p>
 * <pre>
 *   ShapedRecipe(String, CraftingBookCategory, ShapedRecipePattern, ItemStack[, boolean])
 *   ShapelessRecipe(String, CraftingBookCategory, ItemStack, NonNullList&lt;Ingredient&gt;)
 *   ShapedRecipePattern.of(Map&lt;Character, Ingredient&gt;, String...)
 * </pre>
 * <p>（{@code ShapelessRecipe} 只有 4 参构造器，<b>没有</b> {@code showNotification} 参数——
 * 它的序列化器根本不编码该字段。）</p>
 *
 * <h2>取值口径（逐条照抄手写 JSON，不做任何"顺手修复"）</h2>
 * <ul>
 *   <li><b>{@code category} 一律 {@code MISC}</b>：26 条手写版全部是 {@code "category": "misc"}。</li>
 *   <li><b>{@code group} 一律空串</b>：手写版没有 {@code group} 键，而两个序列化器的
 *       {@code group} 都是 {@code Codec.STRING.optionalFieldOf("group", "")}
 *       （{@code ShapedRecipe.java:96} / {@code ShapelessRecipe.java:87}）⇒ 空串不回写，逐字对应。</li>
 *   <li><b>{@code showNotification} 不传</b>：用 4 参构造器，上游实现是
 *       {@code this(..., true)}（{@code ShapedRecipe.java:29-31}），而序列化器是
 *       {@code optionalFieldOf("show_notification", true)} ⇒ 手写版没有该键、生成版也不会写。</li>
 *   <li><b>有序合成的图案恒为 {@code "###" × 3}、键恒为 {@code '#'}</b>：14 条手写版逐字相同。</li>
 *   <li><b>无序合成的原料<b>顺序</b>逐条照抄</b>：{@code ShapelessRecipe} 的
 *       {@code ingredients} 是有序的 {@code NonNullList}。语义上无序合成不看顺序
 *       （{@code matches()} 走 {@code RecipeMatcher}/{@code StackedContents}），但本批按
 *       "顺序也照抄"的更严口径迁移——{@link #thunderiteIngot} 那条 8 原料的
 *       {@code 4×雷石碎块 + 4×钻石} 尤其要保序，以免日后被误读成"顺序无关、随便写"。</li>
 *   <li><b>标签一律用 {@code c:} 约定标签</b>（{@code c:ingots/...} / {@code c:nuggets/...} /
 *       {@code c:storage_blocks/...} / {@code c:raw_materials/...}），与手写版的
 *       {@code "tag"} 逐字相同；<b>不换成具体物品</b>（那会缩窄匹配面 = 行为变化）。</li>
 * </ul>
 *
 * <h2>⚠ 两处"手写版就是这样的"——刻意保留，不是笔误</h2>
 * <ol>
 *   <li><b>{@code ruby_block_from_compacting}</b>：文件名说红宝石块，但手写版里的
 *       {@code key} 是 {@code c:ingots/jade}、{@code result} 是
 *       {@code createoreexpansion:jade_block}——<b>与 {@code jade_block_from_compacting} 完全重复</b>。
 *       这几乎肯定是一处笔误，但本批的红线是<b>行为零变化</b>，所以
 *       {@link #generate} 里<b>逐字照抄</b>它、并把这条重复如实登记在报告中，交给作者决定；
 *       <b>绝不在这里"顺手修好"</b>（改了就是内容变化，且会让"26 条语义相等"的证明失去意义）。</li>
 *   <li><b>行尾符 / 排版</b>：工作区里这 26 条手写文件曾以 <b>CRLF</b> 落盘，但仓库内容不是 CRLF
 *       ——{@code .gitattributes} 有 {@code *.json text eol=lf}，git blob 里它们本来就是 LF，
 *       而生成器产出的 JSON 也恒为 LF（{@code DataProvider} 的固定格式）。所以本批的<b>字节层面
 *       差异极小</b>：实测 26 条里 <b>25 条与迁移前的 git blob 逐字节相同</b>；唯一不同的一条是
 *       {@code thunderite_ingot.json}——它的 8 个原料在手写版里是<b>压缩成一行</b>（{@code {"item":"..."}}），
 *       生成器则逐个对象换行展开，属于<b>无意义的 JSON 空白差异</b>（同一棵解析树）。
 *       这条差异在等价证明里<b>单独成类</b>列出（不混进"键序"/"省略默认值"两类）。</li>
 * </ol>
 *
 * <p><b>调用方</b>：{@link CoeRecipeProvider#generate} 的末尾（批 1/2/3 八族之后）。理由同上。</p>
 */
public final class CoeCraftingRecipeProvider {

    /** 26 条手写版全部是 {@code "category": "misc"}。 */
    private static final CraftingBookCategory CATEGORY = CraftingBookCategory.MISC;

    /** 14 条有序合成的图案：手写版逐字相同的 {@code "###" × 3}。 */
    private static final String[] COMPACT_PATTERN = { "###", "###", "###" };

    /** 有序合成里那个唯一的键字符。 */
    private static final char KEY = '#';

    /**
     * 本族 26 条配方（14 有序 + 12 无序）。调用顺序<b>无契约</b>（每条一个文件、互不覆盖），
     * 这里按"压块 → 解块/粒化"分成两段，段内按材料字母序，便于与手写目录逐条对照。
     */
    public static void generate(RecipeOutput output) {
        // ================= 14 条有序合成：标签 ×9 → 1 块 / 1 锭 =================
        compact(output, "jade_block_from_compacting", "ingots/jade", CoeBlocks.JADE_BLOCK.get());
        compact(output, "jade_ingot_from_compacting", "nuggets/jade", CoeItems.JADE_INGOT.get());
        compact(output, "raw_jade_block_from_compacting", "raw_materials/jade", CoeBlocks.RAW_JADE_BLOCK.get());
        compact(output, "raw_sapphire_block_from_compacting", "raw_materials/sapphire", CoeBlocks.RAW_SAPPHIRE_BLOCK.get());
        compact(output, "raw_topaz_block_from_compacting", "raw_materials/topaz", CoeBlocks.RAW_TOPAZ_BLOCK.get());
        // ⚠ 手写版就是这样（键与产物都是翡翠，与上面 jade_block_from_compacting 重复）：逐字照抄，见类注释。
        compact(output, "ruby_block_from_compacting", "ingots/jade", CoeBlocks.JADE_BLOCK.get());
        compact(output, "sanctstone_block_from_compacting", "ingots/sanctstone", CoeBlocks.SANCTSTONE_BLOCK.get());
        compact(output, "sapphire_block_from_compacting", "ingots/sapphire", CoeBlocks.SAPPHIRE_BLOCK.get());
        compact(output, "sapphire_ingot_from_compacting", "nuggets/sapphire", CoeItems.SAPPHIRE_INGOT.get());
        compact(output, "stellarstone_block_from_compacting", "ingots/stellarstone", CoeBlocks.STELLARSTONE_BLOCK.get());
        compact(output, "stellarstone_ingot_from_compacting", "nuggets/stellarstone", CoeItems.STELLARSTONE_INGOT.get());
        compact(output, "thunderite_block_from_compacting", "ingots/thunderite", CoeBlocks.THUNDERITE_BLOCK.get());
        compact(output, "topaz_block_from_compacting", "ingots/topaz", CoeBlocks.TOPAZ_BLOCK.get());
        compact(output, "topaz_ingot_from_compacting", "nuggets/topaz", CoeItems.TOPAZ_INGOT.get());

        // ================= 12 条无序合成：1 个存储块/锭 → 9 个锭/粒 =================
        decompact(output, "jade_ingot_from_decompacting", "storage_blocks/jade", CoeItems.JADE_INGOT.get());
        nuggetize(output, "jade_nugget_from_decompacting", "ingots/jade", CoeItems.JADE_NUGGET.get());
        decompact(output, "ruby_ingot_from_decompacting", "storage_blocks/ruby", CoeItems.RUBY_INGOT.get());
        decompact(output, "sanctstone_ingot_from_decompacting", "storage_blocks/sanctstone", CoeItems.SANCTSTONE_INGOT.get());
        decompact(output, "sapphire_ingot_from_decompacting", "storage_blocks/sapphire", CoeItems.SAPPHIRE_INGOT.get());
        nuggetize(output, "sapphire_nugget_from_decompacting", "ingots/sapphire", CoeItems.SAPPHIRE_NUGGET.get());
        decompact(output, "stellarstone_ingot_from_decompacting", "storage_blocks/stellarstone", CoeItems.STELLARSTONE_INGOT.get());
        nuggetize(output, "stellarstone_nugget_from_decompacting", "ingots/stellarstone", CoeItems.STELLARSTONE_NUGGET.get());
        thunderiteIngot(output);
        decompact(output, "thunderite_ingot_from_decompacting", "storage_blocks/thunderite", CoeItems.THUNDERITE_INGOT.get());
        decompact(output, "topaz_ingot_from_decompacting", "storage_blocks/topaz", CoeItems.TOPAZ_INGOT.get());
        nuggetize(output, "topaz_nugget_from_decompacting", "ingots/topaz", CoeItems.TOPAZ_NUGGET.get());
    }

    /**
     * 那唯一一条"真·合成"的无序配方：{@code 4×雷石碎块 + 4×钻石 → 1×雷石锭}。
     *
     * <p>原料顺序照抄手写版（先 4 个碎块、再 4 个钻石）。语义上无序合成不看顺序，
     * 但本批按更严口径保序迁移，见类注释"取值口径"。</p>
     */
    private static void thunderiteIngot(RecipeOutput output) {
        shapeless(output, "thunderite_ingot", new ItemStack(CoeItems.THUNDERITE_INGOT.get()),
            Ingredient.of(CoeItems.THUNDERITE_SCRAP.get()),
            Ingredient.of(CoeItems.THUNDERITE_SCRAP.get()),
            Ingredient.of(CoeItems.THUNDERITE_SCRAP.get()),
            Ingredient.of(CoeItems.THUNDERITE_SCRAP.get()),
            Ingredient.of(Items.DIAMOND),
            Ingredient.of(Items.DIAMOND),
            Ingredient.of(Items.DIAMOND),
            Ingredient.of(Items.DIAMOND));
    }

    /** 有序合成：{@code 9× c:<tagPath>} → 1 个 {@code result}。 */
    private static void compact(RecipeOutput output, String name, String tagPath, ItemLike result) {
        ShapedRecipePattern pattern = ShapedRecipePattern.of(Map.of(KEY, Ingredient.of(commonTag(tagPath))), COMPACT_PATTERN);
        // showNotification 走 4 参构造器的默认 true = 手写版省略该键时的 codec 默认值。
        output.accept(recipeId(name), new ShapedRecipe("", CATEGORY, pattern, new ItemStack(result)), null);
    }

    /** 无序合成：{@code 1× c:<blockTagPath>} → 9 个 {@code result}（块 → 锭）。 */
    private static void decompact(RecipeOutput output, String name, String blockTagPath, ItemLike result) {
        shapeless(output, name, new ItemStack(result, 9), Ingredient.of(commonTag(blockTagPath)));
    }

    /** 无序合成：{@code 1× c:<ingotTagPath>} → 9 个 {@code result}（锭 → 粒）。 */
    private static void nuggetize(RecipeOutput output, String name, String ingotTagPath, ItemLike result) {
        shapeless(output, name, new ItemStack(result, 9), Ingredient.of(commonTag(ingotTagPath)));
    }

    /** 无序合成的公共落地：原料顺序 = 参数顺序。 */
    private static void shapeless(RecipeOutput output, String name, ItemStack result, Ingredient... ingredients) {
        NonNullList<Ingredient> list = NonNullList.of(Ingredient.EMPTY, ingredients);
        output.accept(recipeId(name), new ShapelessRecipe("", CATEGORY, result, list), null);
    }

    /** {@code c:} 命名空间下的约定标签（与手写版的 {@code "tag"} 逐字相同）。 */
    private static TagKey<Item> commonTag(String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", path));
    }

    /**
     * 本族的配方 id：<b>{@code createoreexpansion:crafting/materials/<名>}</b>。
     *
     * <p>见类注释"陷阱一"：{@code crafting/materials/} 这一段<b>必须</b>在这里显式给出，
     * 因为原版 builder 的默认 id 不会带上它。</p>
     */
    private static ResourceLocation recipeId(String name) {
        return CoeCore.modLoc("crafting/materials/" + name);
    }

    private CoeCraftingRecipeProvider() {}
}
