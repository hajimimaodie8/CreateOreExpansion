package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.AllGemTags;
import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeBuilder;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

/**
 * <b>动力合成配方（{@code create:mechanical_crafting}，Create 专用类型）的生成动作</b>
 * （配方迁移 批 5）。
 *
 * <p>本族 5 条原先<b>手写</b>在
 * {@code coe/src/main/resources/data/createoreexpansion/recipe/mechanical_crafting/}
 * （翡翠剑/镐/斧/锹/锄各一条），本批改由本类产出——产物落点从 {@code src/main/resources}
 * 换到 {@code coe/src/generated/resources}（由 {@code LayerRecipeRouter} 按调用点绑层
 * {@code "coe"} 改道），<b>配方 id 逐字不变</b>（见 {@link #recipeId}）。</p>
 *
 * <h2>⚠ 必须用 {@link MechanicalCraftingRecipeBuilder}，<b>不能</b>用 {@code MechanicalCraftingRecipeGen}</h2>
 * <p>Create 给这个类型配了两条 datagen 路径，<b>只有一条能用在分层工程里</b>：</p>
 * <ul>
 *   <li>{@code MechanicalCraftingRecipeGen}（{@code api/data/recipe/}）：它<b>自己持有</b>
 *       {@code DataGenerator} / {@code PackOutput}，在构造期就地开出一份 {@code RecipeOutput}
 *       并直接写盘。走它 ⇒ 生成的 5 条<b>绕过 {@code LayerRecipeRouter} 的按调用点绑层</b>，
 *       产物落进根输出 {@code src/generated/resources}（根工程不发布）⇒ 5 条配方在三个模块
 *       jar 里<b>一条都没有</b>，而 F1（"根输出里没有配方产物"）立刻变红。</li>
 *   <li>{@link MechanicalCraftingRecipeBuilder}：一个纯 builder，末尾
 *       {@code build(RecipeOutput, ResourceLocation)} 把配方交给<b>调用方给的</b>
 *       {@code RecipeOutput}——也就是 {@link CoeRecipeProvider#generate} 收到的那一个
 *       （根调用点已经 {@code bind(output, "coe")}）⇒ 落点自然是
 *       {@code coe/src/generated/resources}。本类走这条。</li>
 * </ul>
 *
 * <h2>⚠ 必须显式传 id：{@code build(output)} 会用<b>结果物品的 id</b></h2>
 * <p>{@code MechanicalCraftingRecipeBuilder#build(RecipeOutput)} 的实现是
 * {@code this.build(output, RegisteredObjectsHelper.getKeyOrThrow(this.result))}
 * ⇒ 配方 id 直接取<b>产物物品</b>的 id。而本族手写文件的相对路径是
 * {@code mechanical_crafting/<名>.json} ⇒ 配方 id 是
 * {@code createoreexpansion:mechanical_crafting/<名>}。</p>
 * <p>本族恰好"文件名 == 产物名"（{@code jade_axe.json} → {@code createoreexpansion:jade_axe}），
 * 所以走默认 id 时<b>文件不会少、静态关卡也全绿</b>，但配方 id 会从
 * {@code createoreexpansion:mechanical_crafting/jade_axe} <b>静默变成</b>
 * {@code createoreexpansion:jade_axe}——这在数据包层面等于配方搬家：老存档里
 * {@code /recipe give} 过的 id、JEI 的收藏、任何按 id 引用它的东西全部失配。
 * 更糟的是 {@code build(output, String)} 那条重载<b>恰好</b>会拿这个情形抛异常
 * （{@code "Shaped Recipe … should remove its 'id' argument"}），说明 Create 自己把
 * "id 与产物 id 相同"当成错误——绝不能靠它来兜底。</p>
 * <p>⇒ 本类一律走 {@code build(output, ResourceLocation)}，id 由 {@link #recipeId} 显式拼出
 * {@code mechanical_crafting/} 这一段。</p>
 *
 * <h2>时长字段：<b>本类型没有这个字段</b>（批 1 教训的逐族判决）</h2>
 * <p>批 1 的坑是 {@code ProcessingRecipe#validate()} 的
 * {@code if (processingDuration > 0 &amp;&amp; !canSpecifyDuration())}——它只属于
 * <b>{@code ProcessingRecipe} 那条继承链</b>。本族的注册类是
 * {@code com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe}，其声明是
 * {@code extends ShapedRecipe}（<b>不是</b> {@code ProcessingRecipe}）⇒
 * {@code canSpecifyDuration()} 这个方法是<b>另一条链上的东西，本类型的继承层次里根本不存在</b>，
 * {@code processing_time} 键也无从编码。5 条手写版确实一条都没有该键（已逐条核对），
 * builder 也没有任何 duration 方法。</p>
 * <p>⇒ 本族的判决不是"允许/不允许写时长"，而是<b>"该类型不存在时长字段"</b>；
 * 批 1 的 {@code canSpecifyDuration()} 核实流程对本族<b>不适用</b>（不是"核实为 false"）。</p>
 *
 * <h2>镜像与通知：{@code disallowMirrored()} 恰好同时关掉两个布尔</h2>
 * <p>5 条手写版<b>全部</b>写着 {@code "accept_mirrored": false} 与
 * {@code "show_notification": false}。这两个布尔不是两个独立开关，而是
 * <b>同一个 {@code acceptMirrored} 值的两处出口</b>——对 Create 6.0.10-280 的源码：</p>
 * <pre>
 *   // MechanicalCraftingRecipeBuilder#build(output, id)
 *   new MechanicalCraftingRecipe("", CraftingBookCategory.MISC,
 *       ShapedRecipePattern.of(key, pattern), new ItemStack(result, count), acceptMirrored);
 *
 *   // MechanicalCraftingRecipe(String, CraftingBookCategory, ShapedRecipePattern, ItemStack, boolean)
 *   super(groupIn, category, pattern, recipeOutputIn, acceptMirrored);   // ← 第 5 参 = showNotification
 *   this.acceptMirrored = acceptMirrored;
 *
 *   // MechanicalCraftingRecipe.Serializer.CODEC
 *   RecipeSerializer.SHAPED_RECIPE.codec().forGetter(t -&gt; t)        // → show_notification
 *   + Codec.BOOL.fieldOf("accept_mirrored")                        // → accept_mirrored
 * </pre>
 * <p>即 {@code acceptMirrored} 被<b>同时</b>当作 {@code ShapedRecipe} 的
 * {@code showNotification} 参数传进父构造器。所以：</p>
 * <ul>
 *   <li>{@code disallowMirrored()}（{@code acceptMirrored = false}）⇒
 *       {@code accept_mirrored = false} <b>且</b> {@code show_notification = false}——
 *       与 5 条手写版<b>逐字段一致</b>。</li>
 *   <li>反之，只要不调它（默认 {@code acceptMirrored = true}）⇒ 两个布尔<b>同时</b>变 true，
 *       动力合成器就会接受竖直翻转的图案，玩家能用镜像摆法做出同样的工具——
 *       <b>这是一处真实的玩法变化</b>，不是"少写一个 cosmetic 键"。</li>
 * </ul>
 * <p>注意 builder <b>没有</b> {@code allowMirrored()} 方法（{@code public} 面只有
 * {@code disallowMirrored()}；{@code acceptMirrored = true} 是字段初值）⇒ "显式打开镜像"
 * 这件事在本类型上<b>只能靠省略</b>，不存在一个"对称的"调用。</p>
 * <p>⚠ 本判决的取证方式：把 {@code _create_src/} 下的
 * {@code MechanicalCraftingRecipeBuilder.java} / {@code MechanicalCraftingRecipe.java}
 * 与 Gradle 缓存里<b>本次依赖的那份 sources jar</b>
 * （{@code create-1.21.1-6.0.10-280-sources.jar}）逐字节比对——两份 6328 B / 4067 B
 * <b>SHA-256 完全相同</b>，所以上引源码就是本工程真正编译/运行的那一版，不是记忆或推测。</p>
 *
 * <h2>取值口径（逐条照抄手写 JSON，不做任何"顺手修复"）</h2>
 * <ul>
 *   <li><b>{@code category} 恒 {@code "misc"}</b>：5 条手写版全部如此，而 builder
 *       <b>写死</b> {@code CraftingBookCategory.MISC}（不暴露参数）⇒ 两侧一致。
 *       ⚠ 这也是本 builder 的一个"不可调"点：哪天要写非 misc 的分类，这个 builder 用不了。</li>
 *   <li><b>{@code group} 恒空串</b>：builder 传 {@code ""}，手写版也没有 {@code group} 键。</li>
 *   <li><b>图案逐行照抄</b>（含行内空格）：{@code patternLine} 的调用顺序 = JSON 里
 *       {@code pattern} 数组的顺序；每行宽度必须相等（builder 会抛），本族 5 条都满足。</li>
 *   <li><b>键 A/B/C/D 逐条相同</b>，且 {@code key} 的<b>插入顺序</b>照手写版的
 *       A → B → C → D（builder 用 {@code Maps.newLinkedHashMap()}，顺序可保）。</li>
 *   <li><b>标签用 {@link AllGemTags} 的 {@code c:} 约定标签</b>（{@code JADE.ingots} /
 *       {@code JADE.wires} / {@code JADE.rods}，全模组标签的单一真源），与手写版的
 *       {@code "tag"} 逐字相同；<b>不换成具体物品</b>（那会缩窄匹配面 = 行为变化）。</li>
 *   <li><b>外部物品用 Create 的注册项常量</b>（{@code AllItems.PRECISION_MECHANISM}，
 *       = {@code create:precision_mechanism}），与批 2 的
 *       {@code AllItems.EXP_NUGGET} 同款手法：写错名字是<b>编译期</b>失败，
 *       而不是静默生成一条指向空气的配方。</li>
 *   <li><b>产物一律 1 个</b>：5 条手写版的 {@code result} 只有 {@code id}（无 {@code count}），
 *       而 {@code ItemStack} 的 codec 是 {@code optionalFieldOf("count", 1)} ⇒
 *       count=1 时 JSON 里也不会出现该键。</li>
 * </ul>
 *
 * <p><b>调用方</b>：{@link CoeRecipeProvider#generate} 的末尾（批 1/2/3/4 九族之后）。
 * 理由同上：层的归属由 {@code LayerRecipeRouter} 在<b>调用点</b>绑定，只要跟着<b>同一个
 * output</b> 往下写，落点自然是 {@code coe/src/generated/resources}——根工程那一行调用
 * <b>不用改</b>（红线：根 {@code src} 一字节不动）。</p>
 */
public final class CoeMechanicalCraftingRecipeProvider {

    /** 图案里 4 个键字符：A = 翡翠锭标签，B = 精密构件，C = 翡翠线标签，D = 翡翠棒标签。 */
    private static final char KEY_INGOT = 'A';
    private static final char KEY_MECHANISM = 'B';
    private static final char KEY_WIRE = 'C';
    private static final char KEY_ROD = 'D';

    /**
     * 本族 5 条配方。调用顺序<b>无契约</b>（每条一个文件、互不覆盖），这里按产物物品的字母序
     * jade_axe → jade_hoe → jade_pickaxe → jade_shovel → jade_sword 排列，便于与手写目录逐条对照。
     *
     * <p>每条的第一个参数<b>就是</b>配方 id 的路径段（{@code mechanical_crafting/<name>}）——
     * 不是从产物反推的，见类注释"必须显式传 id"。</p>
     */
    public static void generate(RecipeOutput output) {
        tool(output, "jade_axe", CoeItems.JADE_AXE.get(),
            " AA ",
            "AAAA",
            "A BC",
            "  D ",
            "  D ");
        tool(output, "jade_hoe", CoeItems.JADE_HOE.get(),
            "AAAA",
            "  BC",
            "  D ",
            "  D ");
        tool(output, "jade_pickaxe", CoeItems.JADE_PICKAXE.get(),
            " ABA ",
            "AACAA",
            "  D  ",
            "  D  ",
            "  D  ");
        tool(output, "jade_shovel", CoeItems.JADE_SHOVEL.get(),
            "ABA",
            "ACA",
            " D ",
            " D ",
            " D ");
        tool(output, "jade_sword", CoeItems.JADE_SWORD.get(),
            "  A  ",
            " AAA ",
            " CAC ",
            "DCBCD",
            "  D  ");
    }

    /**
     * 单条动力合成配方：4 个键 + 逐行图案 → 1 个产物。
     *
     * <p>⚠ {@link MechanicalCraftingRecipeBuilder#disallowMirrored()} <b>必须</b>调（5 条手写版
     * 全部 {@code accept_mirrored=false}）：它同时把 {@code show_notification} 也置 false，
     * 两个布尔逐字段一致；漏掉它 = 动力合成器开始接受镜像图案 = 玩法变化。见类注释。</p>
     *
     * <p>⚠ id 走 {@link #recipeId}，<b>不</b>走 {@code build(output)}——后者会用产物物品的 id。</p>
     */
    private static void tool(RecipeOutput output, String name, ItemLike result, String... pattern) {
        MechanicalCraftingRecipeBuilder builder = MechanicalCraftingRecipeBuilder.shapedRecipe(result, 1)
            .key(KEY_INGOT, AllGemTags.JADE.ingots)
            .key(KEY_MECHANISM, AllItems.PRECISION_MECHANISM.get())
            .key(KEY_WIRE, AllGemTags.JADE.wires)
            .key(KEY_ROD, AllGemTags.JADE.rods)
            .disallowMirrored();
        for (String line : pattern) {
            builder.patternLine(line);
        }
        builder.build(output, recipeId(name));
    }

    /**
     * 本族的配方 id：<b>{@code createoreexpansion:mechanical_crafting/<名>}</b>
     * （= 手写文件的相对路径）。
     *
     * <p>见类注释：{@code mechanical_crafting/} 这一段<b>必须</b>在这里显式给出，
     * 因为 builder 的默认 id 是产物物品的 id、不带任何目录前缀（而
     * {@code build(output, String)} 那条重载在"id == 产物 id"时会直接抛异常）。</p>
     */
    private static ResourceLocation recipeId(String name) {
        return CoeCore.modLoc("mechanical_crafting/" + name);
    }

    private CoeMechanicalCraftingRecipeProvider() {}
}
