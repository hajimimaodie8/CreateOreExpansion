package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.lightning.recipe.LightningRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * <b>雷击加工（{@code createoreexpansion:lightning}）的生成动作</b>（配方迁移 批 3）。
 *
 * <p>本族原先<b>手写</b>在
 * {@code coe/src/main/resources/data/createoreexpansion/recipe/lightning/}（8 条），本批改由本类
 * 产出——产物落点从 {@code src/main/resources} 换到 {@code coe/src/generated/resources}（由
 * {@code LayerRecipeRouter} 按调用点绑层 {@code "coe"} 改道），<b>配方 id 逐字不变</b>
 * （= 手写文件的相对路径，见 {@link #generate} 里每条显式给出的 {@code name}）。</p>
 *
 * <h2>✅ 本配方类型<b>不</b>允许写时长（逐族判决，批 1 教训）</h2>
 * <p>批 1 踩过的坑：{@code GrindingRecipe} 没覆写 {@code canSpecifyDuration()}，而
 * {@code ProcessingRecipe#validate()} 里有
 * {@code if (processingDuration &gt; 0 &amp;&amp; !canSpecifyDuration())} 直接报错；
 * {@code ProcessingRecipe#codec(...)} 把 {@code validate()} 包成 {@code MapCodec#validate(...)}，
 * 于是<b>编、解码两个方向都跑它</b>（datagen 写盘抛异常、运行期 {@code RecipeManager} 整条丢弃）。</p>
 * <p>本族的核实结论（对 Create 6.0.10-280 的<b>源码</b>，不是推测）：{@code createoreexpansion:lightning}
 * 的注册类是 {@link LightningRecipe}，它<b>没有</b>覆写 {@code canSpecifyDuration()}，用的是
 * {@code ProcessingRecipe} 的默认实现 {@code false}（{@code ProcessingRecipe.java:72-74}）。
 * ⇒ 本族<b>绝不许</b>调 {@code duration(...)}；手写版也没有 {@code processing_time} 键，
 * 所以这里是"什么都不写"而不是"写 0"。</p>
 *
 * <h2>⚠ 五条充能配方的输出带数据组件（必须逐值核对）</h2>
 * <p>8 条里有 6 条是"同一件物品进、同一件物品出、但输出声明了一个能量组件"
 * （{@code "components": {"createoreexpansion:energy": 5000}}，凝能佩是 10000）。</p>
 *
 * <p><b>⚠ 这里有一条实测踩到的坑，别再走回去</b>：直觉上的写法
 * {@code output(new ItemStack(item))}（或 {@code output(ItemStack)} 重载）
 * <b>保不住</b>这个组件。原因是 {@code ProcessingOutput#ProcessingOutput(ItemStack, float)}
 * 取的是 {@code stack.getComponentsPatch()}（{@code ProcessingOutput.java:43-45}），而
 * {@code ItemStack} 的组件是"原型 + 补丁"两层：雷鸣工具在 {@link CoeItems} 注册时就
 * {@code .defaultEnergy(5000).maxEnergy(5000)}（凝能佩 {@code stressMedallion(..., 10000, ...)}），
 * 也就是<b>原型里本来就是同一个值</b>；{@code ItemStack#set} 把"与原型相同的值"写回去时，
 * {@code PatchedDataComponentMap} 会把它从补丁里<b>摘掉</b> ⇒ {@code getComponentsPatch()} 返回
 * {@code EMPTY} ⇒ codec 的 {@code optionalFieldOf("components", EMPTY)} 直接省掉这个键
 * （实测：第一版生成器跑出来的 6 条输出里 {@code components} 全部消失，被等价脚本 PART 2c 抓住）。</p>
 * <p>所以本类<b>显式构造</b> {@code DataComponentPatch} 并交给
 * {@code ProcessingOutput(item, count, patch, chance)}：那条路把补丁<b>原样存下</b>、不做归一化，
 * 于是"配方自己声明的 5000 / 10000"真正落进 JSON，而不是靠物品默认值兜着
 * （物品默认值将来一变，手写配方仍会写回 5000，生成版也必须如此）。</p>
 * <p>组件类型取 {@link AllDataComponents#ENERGY}（注册名 {@code createoreexpansion:energy}），
 * 不手写字符串。</p>
 *
 * <h2>为什么显式传 id、不按产物反推</h2>
 * <p>5 条充能配方的<b>输入与输出同名</b>（都指向同一件雷鸣工具），"按产物反推"只会得到
 * {@code thunderite_axe} 而丢掉 {@code _charge} 后缀；而全模组另有 <b>12 组 basename 碰撞</b>。
 * 所以沿用批 1/批 2 的口径：每条都把 id 的<b>路径段</b>显式写成第一个参数。</p>
 *
 * <h2>取值口径</h2>
 * <ul>
 *   <li>配方类型工厂：{@code CoeRecipeTypes.LIGHTNING.getSerializer().factory()} —— 取
 *       <b>已注册的那个序列化器实例</b>的工厂，于是生成期建配方与运行期反序列化共用同一个工厂；
 *       类型写错会在 datagen 期直接抛，而不是静默生成一份玩家加载不了的 JSON
 *       （批 1 的 {@link CoeLightningBlockRecipeProvider} 同款手法）。</li>
 *   <li>{@code id} 只给<b>名字</b>：{@code ProcessingRecipeBuilder#build(RecipeOutput)} 自己会
 *       把 {@code 配方类型 id 的 path + "/"} 前缀加上（本类型 ⇒ {@code lightning/}），
 *       与手写路径逐字相同。</li>
 * </ul>
 *
 * <p><b>调用方</b>：{@link CoeRecipeProvider#generate} 的末尾（批 1、批 2 六族之后）。根工程的
 * {@code data/RecipeProvider#buildRecipes} 已经把 {@code CoeRecipeProvider.generate} 的
 * {@code RecipeOutput} 包成了「本层 coe」的（{@code bind(output, "coe")}），本类跟着同一个
 * output 走即可，<b>不需要动根工程的任何一行</b>（红线：根 {@code src} 不许动）。</p>
 */
public final class CoeLightningRecipeProvider {

    /** 雷击给工具充满的能量（与手写版逐值相同）。 */
    private static final int TOOL_CHARGE = 5000;

    /** 雷击给凝能佩充满的能量：容量是工具的两倍（与手写版逐值相同）。 */
    private static final int MEDALLION_CHARGE = 10000;

    /**
     * 本族 8 条配方，调用顺序无契约（每条一个文件、互不覆盖），这里按
     * 物质转化（2 条）→ 五条工具充能 → 凝能佩充能 排列。
     *
     * <p>每条的第一个参数<b>就是</b>配方 id 的路径段（{@code lightning/<name>}）——不是从产物反推的。</p>
     */
    public static void generate(RecipeOutput output) {
        // ===== 物质转化 =====
        // 铁锭 → 金锭（1 : 1）
        single(output, "iron_to_gold", Items.IRON_INGOT, Items.GOLD_INGOT);

        // 雷鸣合金碎片：金锭 + 玉锭 + 蓝宝石锭 + 下界合金碎片 → 2 份
        // ⚠ 原料顺序有意义（codec 按 ingredients 数组逐项落盘），与手写 JSON 逐项一致。
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc("thunderite_scrap"))
            .require(Items.GOLD_INGOT)
            .require(CoeItems.JADE_INGOT.get())
            .require(CoeItems.SAPPHIRE_INGOT.get())
            .require(Items.NETHERITE_SCRAP)
            .output(CoeItems.THUNDERITE_SCRAP.get(), 2)
            .build(output);

        // ===== 五条雷鸣工具充能：原物品 → 原物品 + 5000 能量 =====
        charge(output, "thunderite_sword_charge", CoeItems.THUNDERITE_SWORD.get(), TOOL_CHARGE);
        charge(output, "thunderite_pickaxe_charge", CoeItems.THUNDERITE_PICKAXE.get(), TOOL_CHARGE);
        charge(output, "thunderite_axe_charge", CoeItems.THUNDERITE_AXE.get(), TOOL_CHARGE);
        charge(output, "thunderite_shovel_charge", CoeItems.THUNDERITE_SHOVEL.get(), TOOL_CHARGE);
        charge(output, "thunderite_hoe_charge", CoeItems.THUNDERITE_HOE.get(), TOOL_CHARGE);

        // ===== 雷鸣凝能佩充能：原物品 → 原物品 + 10000 能量（容量翻倍） =====
        charge(output, "thunderite_medallion_charge", CoeItems.THUNDERITE_STRESS_MEDALLION.get(), MEDALLION_CHARGE);
    }

    /**
     * 单条"原样转化"配方：{@code from} → 1 份 {@code to}。
     *
     * <p>{@code output(to)} 的 {@code count}/{@code chance} 取 codec 默认值 1（JSON 里两个键都不写），
     * 手写版是显式写 {@code "count": 1}——语义相同，文本差异属"codec 省略显式默认值"那一类。</p>
     */
    private static void single(RecipeOutput output, String name, ItemLike from, ItemLike to) {
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc(name))
            .require(from)
            .output(to)
            .build(output);
    }

    /**
     * 单条"充能"配方：{@code item} → 同一件物品，但输出<b>声明</b>
     * {@code createoreexpansion:energy = energy}。
     *
     * <p>必须显式构造补丁（见类注释"五条充能配方的输出带数据组件"里的实测坑）：雷鸣工具的原型
     * 本来就带同一个能量值，所以 {@code new ItemStack(item)} + {@code set(...)} 得到的
     * {@code getComponentsPatch()} 是 {@code EMPTY}，codec 会把 {@code components} 整个省掉。
     * {@code ProcessingOutput(item, count, patch, chance)} 这条路把补丁原样保留。</p>
     */
    private static void charge(RecipeOutput output, String name, ItemLike item, int energy) {
        DataComponentPatch patch = DataComponentPatch.builder()
            .set(AllDataComponents.ENERGY, energy)
            .build();
        new StandardProcessingRecipe.Builder<>(factory(), CoeCore.modLoc(name))
            .require(item)
            .output(new ProcessingOutput(item.asItem(), 1, patch, 1.0F))
            .build(output);
    }

    /**
     * 本配方类型<b>已注册</b>的序列化器工厂（见类注释"取值口径"，同 {@link CoeLightningBlockRecipeProvider}）。
     */
    private static StandardProcessingRecipe.Factory<LightningRecipe> factory() {
        StandardProcessingRecipe.Serializer<LightningRecipe> serializer = CoeRecipeTypes.LIGHTNING.getSerializer();
        return serializer.factory();
    }

    private CoeLightningRecipeProvider() {}
}
