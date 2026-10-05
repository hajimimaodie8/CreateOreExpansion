package com.hjmmd_8.createoreexpansion.compat.createaddition.coe;

import com.hjmmd_8.createoreexpansion.common.AllGemTags;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;
import com.mrh0.createaddition.recipe.rolling.RollingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

/**
 * <b>轧制配方（{@code createaddition:rolling}，第三方 CC&amp;A 类型）的生成动作</b>
 * （配方迁移 批 6）。
 *
 * <p>本族 8 条原先<b>手写</b>在
 * {@code coe/src/main/resources/data/createoreexpansion/recipe/rolling/}
 * （四种宝石 × {锭 → 杆, 板 → 线}），本批改由本类产出——产物落点从 {@code src/main/resources}
 * 换到 {@code coe/src/generated/resources}（由 {@code LayerRecipeRouter} 按调用点绑层
 * {@code "coe"} 改道），<b>配方 id 逐字不变</b>（见 {@link #generate} 里每条显式给出的
 * {@code name}）。</p>
 *
 * <h2>本类为什么住 {@code compat/createaddition/coe/} 而不是 {@code common/registry/coe/}</h2>
 * <p>批 1–5 的九个 provider 都住 {@code common/registry/coe/}，只有本类是<b>唯一 import 第三方
 * （CC&amp;A）配方类</b>的那一个：它必须拿到
 * {@code com.mrh0.createaddition.recipe.rolling.RollingRecipe#TYPE_INFO} 才能取得已注册的
 * 序列化器工厂。把"第三方耦合"集中在 {@code compat/} 子树是本仓既有口径（同包的
 * {@code CreateAdditionCompat} 就是 COE 侧的 CC&amp;A 桥），所以本类选这里。两点合规性说明：</p>
 * <ul>
 *   <li><b>不违反「{@code content/} 包不得 import 可选模组类」这条红线</b>：本类不在
 *       {@code content/} 下（{@code compat/createaddition/coe/**}），而且 CA 对 {@code :coe}
 *       本来就是 <b>required</b>（{@code coe/build.gradle} 的 {@code compileOnly} +
 *       {@code runtimeOnly}，{@code neoforge.mods.toml} 声明 required）——
 *       与 Curios 那种"必须 optional"的可选依赖不是一回事。</li>
 *   <li><b>分层判据不变</b>：{@code compat/createaddition/**} 在 {@code tools/check-layering.ps1}
 *       的 {@code Get-FileLayer} / {@code Get-TargetLayer} 里<b>本来就是 COE</b>
 *       （{@code ^compat/createaddition/} 早于兜底的 {@code ^compat/ ⇒ SHARED} 规则命中），
 *       所以"COE 的 provider 调用 COE 的 provider"是同层边，
 *       {@code ^common/registry/coe/} 与 {@code ^compat/createaddition/} 两条路都到 COE，
 *       本类放哪一边层级结论都相同。</li>
 * </ul>
 *
 * <p><b>调用方</b>：{@link com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRecipeProvider#generate}
 * 的末尾（批 1/2/3/4/5 十族之后），跟着参数里<b>同一个</b> {@code RecipeOutput} 往下写——
 * 根工程的 {@code data/RecipeProvider#buildRecipes} 已经把那个 output 包成了「本层 coe」的
 * （{@code bind(output, "coe")}），所以<b>根工程的调用一行都不用改</b>
 * （红线：根 {@code src} 一个字节不动）。反过来说：本类<b>绝不能</b>自己另造一个
 * {@code RecipeOutput} 或 {@code PackOutput}，否则会绕过按调用点绑层、产物落进根输出
 * （{@code check-module-selfsufficiency} 的 F1 立刻红）。</p>
 *
 * <h2>⚠ 不要用 CC&amp;A 自带的 {@code RollingRecipeGen}</h2>
 * <p>CC&amp;A 走的是 {@code com.mrh0.createaddition.datagen.RecipeGen.RollingRecipeGen}
 * （{@code extends StandardProcessingRecipeGen<RollingRecipe>}）——那是一条<b>自带
 * {@code PackOutput} 的 Gen</b>，与批 5 被否掉的 {@code MechanicalCraftingRecipeGen} 同款形状：
 * 它自己持有 datagen 输出、直接写盘，会<b>绕过 {@code LayerRecipeRouter} 的按调用点绑层</b>
 * ⇒ 8 条落进根输出（根不发布，模块 jar 里一条都没有）⇒ F1 红。本类走<b>纯 builder</b>：
 * {@code new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc(name))}（批 2 的
 * {@code CoePressingRecipeProvider} 同款手法），末尾把配方交给调用方给的 output。</p>
 *
 * <h2>⚠ 本类型<b>不允许</b>写时长（批 1 教训的逐族核实结论：核实为 false，不是"没有这个字段"）</h2>
 * <p>批 1 的坑：{@code ProcessingRecipe#validate()} 有
 * {@code if (processingDuration &gt; 0 &amp;&amp; !canSpecifyDuration())}，而
 * {@code ProcessingRecipe#codec(...)} 把 {@code validate()} 包成 {@code MapCodec#validate(...)}
 * ⇒ <b>编、解码两个方向都跑它</b>（datagen 写盘直接抛、运行期 RecipeManager 整条丢弃）。</p>
 * <p>本族的核实结论（对 <b>CC&amp;A {@code createaddition-439890-8083378}</b> 与
 * <b>Create 6.0.10-280</b> 的<b>字节码</b>逐条核过，不是推测）：</p>
 * <ul>
 *   <li>{@code RollingRecipe extends StandardProcessingRecipe<RecipeWrapper>}，
 *       而 {@code StandardProcessingRecipe extends ProcessingRecipe} ⇒ <b>本类型确实在
 *       {@code ProcessingRecipe} 那条继承链上</b>（批 5 的 {@code MechanicalCraftingRecipe}
 *       是"链上没有这个方法"，本族不是）。</li>
 *   <li>{@code ProcessingRecipe#canSpecifyDuration()} 的方法体是 {@code iconst_0; ireturn}
 *       （默认 <b>false</b>）；{@code StandardProcessingRecipe} 与 {@code RollingRecipe}
 *       <b>都没有</b>覆写它 ⇒ 本类型<b>不允许</b>写时长。</li>
 *   <li>因此本族<b>绝不许</b>调 {@code duration(...)}；手写版 8 条也确实没有
 *       {@code processing_time} 键（已逐条核对）——是"什么都不写"，不是"写 0"。</li>
 * </ul>
 * <p>标签面还有一条旁证：CC&amp;A 自己随 jar 发布的
 * {@code data/createaddition/recipe/rolling/brass_rod.json} 正是
 * {@code {type, ingredients:[{tag}], results:[{count:2,id}]}}，同样没有 {@code processing_time}。</p>
 *
 * <h2>组件（{@code components}）判决：<b>无</b></h2>
 * <p>批 3 的教训是 {@code output(ItemStack)} 会<b>静默丢组件</b>（那条链上必须显式带
 * {@code DataComponentPatch}）。本族<b>不适用</b>：8 条的产物都是普通物品（杆 / 线），
 * 手写版的 {@code results} 里<b>没有</b> {@code components} 键。本类走
 * {@code output(ItemLike, int)} ⇒ {@code new ItemStack(item, count)} ⇒
 * {@code stack.getComponentsPatch()} = {@code DataComponentPatch.EMPTY} ⇒
 * codec 的 {@code optionalFieldOf("components", EMPTY)} 也不写该键，两侧逐字段一致。</p>
 *
 * <h2>⚠ 必须显式传 id 的<b>名字段</b>（basename 碰撞）</h2>
 * <p>本族 8 个 basename 与 {@code pressing/} 下已生成的 4 条<b>同名</b>
 * （{@code topaz_ingot} 同时在 {@code pressing/} 与 {@code rolling/} 下等），
 * 全模组共 <b>12 组</b>这样的碰撞。"按产物名反推 id"会把这些生成到错的路径上，所以这里沿用
 * 批 1 的口径：每条把 id 的<b>名字段</b>显式写成第一个参数。</p>
 * <p><b>目录段由 builder 补</b>：{@code ProcessingRecipeBuilder#build(RecipeOutput)} 的实现是
 * {@code recipeId.withPrefix(recipe.getTypeInfo().getId().getPath() + "/")}；本类型的
 * {@code getTypeInfo()} 就是 {@code RollingRecipe.TYPE_INFO}，其 {@code getId()} 是
 * {@code CARecipes.ROLLING}（= 注册 id {@code createaddition:rolling}）⇒
 * 自动补上的前缀恰好是 {@code rolling/}，与手写路径逐字相同。
 * 见 {@link #requireStableTypePath()}：那个 id 是<b>第三方</b>的，所以这里额外钉一道断言。</p>
 *
 * <h2>取值口径（逐条照抄手写 JSON，不做任何"顺手修复"）</h2>
 * <ul>
 *   <li>只生成手写目录里<b>确实存在</b>的 4 种宝石 × 2 条 = 8 条：翡翠 / 黄玉 / 蓝宝石 / 星辉石。
 *       <b>不</b>给红宝石 / 圣石 / 雷魔素补 rolling（手写版没有 ⇒ 补了就是新增配方 = 行为变化）。</li>
 *   <li>原料用 {@link AllGemTags} 的 {@code c:ingots/<材料>} / {@code c:sheets/<材料>} 标签
 *       （全模组标签的单一真源），<b>不换成具体物品</b>（那会缩窄匹配面 = 行为变化）。</li>
 *   <li>产物一律 <b>2 个</b>（手写版每条都是 {@code "count": 2}）：{@code output(rod, 2)}。
 *       走 codec 默认的 chance=1F ⇒ 不写 {@code chance} 键，与手写版一致。</li>
 * </ul>
 */
public final class CoeRollingRecipeProvider {

    /**
     * 本族产物在数据包里的目录段（= 注册的配方类型 id 的 path）。
     *
     * <p>它<b>不是</b>由本类写进配方 id 的（目录段由 {@code build(output)} 从
     * {@code TYPE_INFO} 补，见类注释），这里只用来做 {@link #requireStableTypePath()} 的断言基准。</p>
     */
    private static final String TYPE_PATH = "rolling";

    /**
     * 本族 8 条配方。调用顺序<b>无契约</b>（每条一个文件、互不覆盖），这里按材料序
     * 玉 → 黄玉 → 蓝宝石 → 星辉石（每种先"锭 → 杆"、再"板 → 线"）排列，便于与手写目录逐条对照。
     *
     * <p>每条的第一个参数<b>就是</b>配方 id 的名字段（{@code rolling/<name>}）——不是从产物反推的。</p>
     */
    public static void generate(RecipeOutput output) {
        requireStableTypePath();

        ingot(output, "jade_ingot", AllGemTags.JADE, CoeItems.JADE_ROD.get());
        sheet(output, "jade_sheet", AllGemTags.JADE, CoeItems.JADE_WIRE.get());
        ingot(output, "topaz_ingot", AllGemTags.TOPAZ, CoeItems.TOPAZ_ROD.get());
        sheet(output, "topaz_sheet", AllGemTags.TOPAZ, CoeItems.TOPAZ_WIRE.get());
        ingot(output, "sapphire_ingot", AllGemTags.SAPPHIRE, CoeItems.SAPPHIRE_ROD.get());
        sheet(output, "sapphire_sheet", AllGemTags.SAPPHIRE, CoeItems.SAPPHIRE_WIRE.get());
        ingot(output, "stellarstone_ingot", AllGemTags.STELLARSTONE, CoeItems.STELLARSTONE_ROD.get());
        sheet(output, "stellarstone_sheet", AllGemTags.STELLARSTONE, CoeItems.STELLARSTONE_WIRE.get());
    }

    /**
     * 单条"锭 → 杆"配方：{@code c:ingots/<材料>} 标签 → 2 根杆。
     */
    private static void ingot(RecipeOutput output, String name, AllGemTags gem, ItemLike rod) {
        recipe(output, name, gem.ingots, rod);
    }

    /**
     * 单条"板 → 线"配方：{@code c:sheets/<材料>} 标签 → 2 根线。
     */
    private static void sheet(RecipeOutput output, String name, AllGemTags gem, ItemLike wire) {
        recipe(output, name, gem.sheets, wire);
    }

    /**
     * 单条轧制配方：{@code c:} 标签 → 2 个产物。
     *
     * <p>{@code output(item, 2)} 的 {@code count=2} 与手写版逐字相同；{@code chance} 取 codec 默认
     * 1F、{@code components} 取默认空补丁 ⇒ 两个键都不写，与手写版只写
     * {@code {"count": 2, "id": …}} 逐字段一致。见类注释"组件判决"。</p>
     *
     * <p>⚠ 不调 {@code duration(…)}：本类型 {@code canSpecifyDuration()} 核实为 false，见类注释。</p>
     */
    private static void recipe(RecipeOutput output, String name, TagKey<Item> tag, ItemLike result) {
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc(name))
            .require(tag)
            .output(result, 2)
            .build(output);
    }

    /**
     * 本配方类型<b>已注册</b>的序列化器工厂（与运行期反序列化用的是同一个实例）。
     *
     * <p>{@code RollingRecipe.TYPE_INFO} 是 CC&amp;A 的公开常量，{@code getSerializer()} 返回的是
     * {@code CARecipes.ROLLING}（一个 {@code StandardProcessingRecipe.Serializer<RollingRecipe>}）。
     * 若哪天换成别的序列化器，这里的赋值会抛 {@code ClassCastException}——响亮的失败，
     * 而不是静默生成一份玩家加载不了的 JSON（批 1 的 {@code CoeGrindingRecipeProvider} 同款手法）。</p>
     */
    private static StandardProcessingRecipe.Factory<RollingRecipe> factory() {
        StandardProcessingRecipe.Serializer<RollingRecipe> serializer = RollingRecipe.TYPE_INFO.getSerializer();
        return serializer.factory();
    }

    /**
     * <b>本类自己加的一道守卫</b>（不在批 1–5 的九个兄弟 provider 里，故单列报备）。
     *
     * <p>本族 8 条的<b>目录段</b>完全来自第三方：{@code build(output)} 用
     * {@code recipe.getTypeInfo().getId().getPath()} 当前缀，而那个 id 是 CC&amp;A 注册的
     * {@code createaddition:rolling}。第三方把这个类型 id 改名（或注册成别的 path）⇒ 8 条配方会
     * <b>静默搬到另一个目录</b>：文件数一个不少、F1/F2/F3/F4 全绿、本批"手写已被生成器取代"
     * 也照样成立，只有数据包层面的<b>配方 id 变了</b>——老存档 {@code /recipe give} 过的 id、
     * JEI 收藏、任何按 id 引用它的东西全部失配。</p>
     *
     * <p>所以把它钉成一次显式断言：对不上就在 <b>datagen 期直接抛</b>，而不是静默改道。</p>
     */
    private static void requireStableTypePath() {
        ResourceLocation expected = ResourceLocation.fromNamespaceAndPath("createaddition", TYPE_PATH);
        ResourceLocation actual = RollingRecipe.TYPE_INFO.getId();
        if (!expected.equals(actual)) {
            throw new IllegalStateException("createaddition:rolling moved: RollingRecipe.TYPE_INFO.getId() = " + actual
                + ", but this provider relies on the '" + TYPE_PATH + "/' path segment that build(output) derives from it."
                + " Update CoeRollingRecipeProvider (and the 8 recipe ids) deliberately instead of letting the"
                + " recipes relocate silently.");
        }
    }

    private CoeRollingRecipeProvider() {}
}
