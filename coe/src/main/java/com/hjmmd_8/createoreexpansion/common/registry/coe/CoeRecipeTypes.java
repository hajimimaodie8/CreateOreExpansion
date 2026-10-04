package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.registry.LayerRecipeType;
import com.hjmmd_8.createoreexpansion.common.registry.WaveRecipeCapabilities;
import com.hjmmd_8.createoreexpansion.common.registry.WaveRecipeCapabilities.LayerOrder;
import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipe;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.DismantlingRecipe;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.GrindingRecipe;
import com.hjmmd_8.createoreexpansion.content.lightning.recipe.LightningBlockRecipe;
import com.hjmmd_8.createoreexpansion.content.lightning.recipe.LightningRecipe;
import com.hjmmd_8.createoreexpansion.content.transmuting.AllTransmutingRecipe;

/**
 * <b>COE（矿物拓展）自己的配方类型</b>（P3c：从 {@code common/AllRecipeTypes} 拆出）。
 *
 * <p>本层的配方类型——雷击加工（{@code lightning}）、方块雷击加工（{@code lightning_block}）、
 * 角磨（{@code grinding}）、拆解（{@code dismantling}）——原先声明在共享枚举里，
 * 现在按"谁的东西写在哪"搬到本层包下。注册 id、序列化器、配方类型与拆分前逐字一致
 * （id 由常量名 {@code Lang.asId} 得到，命名空间恒为 {@code createoreexpansion}）。</p>
 *
 * <p><b>声明顺序 = 拆分前枚举里的相对顺序</b>（LIGHTNING → LIGHTNING_BLOCK → GRINDING →
 * DISMANTLING → <b>CHARGING</b>）。层间顺序由 core 的 {@code common.registry.LayerBootstrap}
 * 用固定名单唤醒（W6-c 起只剩本类一条）来保证，所以本类里的字段顺序请勿改动。</p>
 *
 * <p><b>W6-b2</b>：嬗化机制整块搬进本层后，本类的<b>第一个</b>字段变成
 * {@link #TRANSMUTING} 的<b>声明本体</b>（{@link TransmutationRecipeTypes} 现在只是它的同名
 * 转发别名），于是六个条目进注册表的顺序仍是拆分前的
 * {@code transmuting → lightning → lightning_block → grinding → dismantling → charging}。
 * 为什么"第一行"是承重的、以及本轮实测踩到的错误形态 → 见 {@link #TRANSMUTING} 自己的注释。</p>
 *
 * <p><b>W6-c</b>：{@link #CHARGING} 随三台应力充能器从 {@code CewsRecipeTypes}（已删除）回到本类，
 * 且<b>声明在最后一个字段</b>——"最后一行"与"第一行"同样承重，理由见 {@link #CHARGING} 的注释。</p>
 *
 * <p><b>本类不做注册动作</b>：两张注册表住在 {@link LayerRecipeType}（core），
 * 由 {@code LayerBootstrap.ensureAttached(modEventBus)} 统一挂到事件总线——六个条目共用同一对注册表
 * （且"恰挂一次"，二次挂会抛 {@code IllegalStateException}），这是"注册顺序不变"的前提。</p>
 */
public final class CoeRecipeTypes {

    /**
     * <b>嬗变加工（{@code createoreexpansion:transmuting}）—— 必须是本类的第一个字段</b>（W6-b2）。
     *
     * <p>嬗化机制整块搬进 {@code :coe} 之后，{@code LayerBootstrap} 的唤醒名单按方案 §2 收短成
     * {@code {coe.CoeRecipeTypes, cews.CewsRecipeTypes}}（原名单的第一项
     * {@code transmutation.TransmutationRecipeTypes} 不再单独唤醒），于是 {@code transmuting}
     * 的<b>声明</b>搬到这里，并由本类的第一个字段创建。{@code TransmutationRecipeTypes} 保留为
     * <b>同名转发别名</b>（它的调用方 {@code AllTransmutingRecipe} / {@code AllTransmutingType} /
     * {@code TransmutationJeiCategories} 一行都不用改）。</p>
     *
     * <p><b>为什么"第一行"是承重的</b>：{@code BuiltInRegistries.RECIPE_TYPE} /
     * {@code RECIPE_SERIALIZER} 的条目顺序 = 这些 {@code LayerRecipeType} 的<b>创建顺序</b>
     * （六个条目共用 {@code LayerRecipeType} 里那一对 {@code DeferredRegister}）。
     * 类初始化时字段初始化器按文本顺序执行，所以本字段先创建 ⇒ {@code transmuting} 仍排在
     * {@code lightning} 之前。</p>
     *
     * <p><b>⚠ 不要改成"转发 {@code TransmutationRecipeTypes.TRANSMUTING} 字段"</b>（本轮实测过的错误形态）：
     * 那样会由本字段的初始化去触发 {@code TransmutationRecipeTypes} 的类初始化，而它的静态块会立刻
     * 初始化 {@code WaveRecipeCapabilities}，后者在自己的静态块里唤醒
     * {@code CewsRecipeTypes} —— 于是 {@code charging} 抢在 {@code lightning} 之前注册，
     * 数值注册 id 从 {@code 7..12}（transmuting, lightning, lightning_block, grinding, dismantling,
     * charging）变成 {@code 7,8=charging,...}。这是<b>玩家不可见但口径已断</b>的顺序改动，
     * 只有 {@code build/patch/w6b2-EVIDENCE.txt} 里那种"打印数值注册 id"的探针能发现它。
     * 声明式写法不会踩这个坑：本字段的初始化式里只有一个方法引用
     * （{@code AllTransmutingRecipe::new}，不触发任何类初始化）。</p>
     */
    public static final LayerRecipeType TRANSMUTING =
        LayerRecipeType.processing("TRANSMUTING", AllTransmutingRecipe::new);

    /** 雷击加工（物品）：{@code createoreexpansion:lightning}。 */
    public static final LayerRecipeType LIGHTNING =
        LayerRecipeType.processing("LIGHTNING", LightningRecipe::new);

    /** 方块雷击加工：{@code createoreexpansion:lightning_block}。 */
    public static final LayerRecipeType LIGHTNING_BLOCK =
        LayerRecipeType.processing("LIGHTNING_BLOCK", LightningBlockRecipe::new);

    /** 动力角磨床打磨：{@code createoreexpansion:grinding}。 */
    public static final LayerRecipeType GRINDING =
        LayerRecipeType.processing("GRINDING", GrindingRecipe::new);

    /** 拆解（装备/工具 → 材料）：{@code createoreexpansion:dismantling}。 */
    public static final LayerRecipeType DISMANTLING =
        LayerRecipeType.serializer("DISMANTLING", () -> new DismantlingRecipe.Serializer());

    /**
     * <b>充能加工（{@code createoreexpansion:charging}）—— 必须是本类的最后一个字段</b>（W6-c）。
     *
     * <p>它原先声明在 {@code common/registry/cews/CewsRecipeTypes}（第二层），W6-c 随三台应力充能器
     * 一起回到第一层：Recipe Type 的层归属跟着"谁能加工它"走，而充能器现在是 {@code :coe} 的内容。
     * {@code CewsRecipeTypes} 整个类因此消失（第二层一个配方类型都不剩）。</p>
     *
     * <p><b>为什么"最后一行"与 {@link #TRANSMUTING} 的"第一行"同样是承重的</b>：
     * {@code BuiltInRegistries.RECIPE_TYPE} / {@code RECIPE_SERIALIZER} 的条目顺序 =
     * 这些 {@code LayerRecipeType} 的<b>创建顺序</b>（六个条目共用 {@code LayerRecipeType} 里那一对
     * {@code DeferredRegister}），类初始化时字段初始化器按文本顺序执行。声明在末尾 ⇒ 数值注册 id
     * 仍是拆分前的 {@code 7..12}（transmuting, lightning, lightning_block, grinding, dismantling,
     * charging）。{@code LayerBootstrap} 的唤醒名单 W6-c 起只剩本类一条。</p>
     *
     * <p>初始化式与 {@link #DISMANTLING} 同形（只有方法引用与 lambda，不触发任何类初始化），
     * 所以不会复现 {@link #TRANSMUTING} 注释里那条"由转发字段把 {@code charging} 挤到前面"的坑。</p>
     */
    public static final LayerRecipeType CHARGING =
        LayerRecipeType.serializer("CHARGING", () -> new ChargingRecipe.Serializer<>(ChargingRecipe::new));

    /**
     * <b>波加工能力登记</b>（P3i）：把本层这六个配方类型交给 {@link WaveRecipeCapabilities}，
     * 顺序即下面两条 {@code add(...)} 的参数顺序。
     *
     * <p>方向是「COE → 登记表（SHARED）」——登记表不 import 任何层、本类也不 import 别层的配方类型，
     * 所以它不产生任何跨层引用。放在静态块里 = "谁被类初始化，谁就登记自己"。</p>
     *
     * <p><b>W6-b2</b>：{@link #TRANSMUTING} 的登记也在这里，但排序键仍是
     * {@link LayerOrder#TRANS}（<b>不是</b> {@code COE}）—— 组权值决定"波优先加工哪种配方"，
     * 它与"这个类住在哪个 Gradle 模块"是两件事。</p>
     *
     * <p><b>W6-c</b>：{@link #CHARGING} 追加在 {@link LayerOrder#COE} 组的<b>末尾</b>
     * （不是新开一个 {@code addOrdered} 调用、也不是插在 {@code GRINDING} 之前），
     * 于是 {@code WaveRecipeCapabilities.all()} 的展开顺序与拆分前逐字相同：
     * {@code transmuting → lightning → lightning_block → grinding → dismantling → charging}。
     * 这条顺序是<b>玩家可见</b>的（波优先加工哪一种配方），而三个静态关卡都看不见它
     * —— 因此 {@code tools/check-module-selfsufficiency.ps1} 的 {@code X2} 把它冻结成断言。</p>
     *
     * <p>静态块在全部字段初始化器<b>之后</b>执行，所以这里读到的六个常量都已创建；
     * 而"条目进注册表的顺序"在那个时刻已经定下（见 {@link #TRANSMUTING} 的注释）。</p>
     */
    static {
        WaveRecipeCapabilities.addOrdered(LayerOrder.TRANS).add(TRANSMUTING);
        WaveRecipeCapabilities.addOrdered(LayerOrder.COE)
            .add(LIGHTNING, LIGHTNING_BLOCK, GRINDING, DISMANTLING, CHARGING);
    }

    private CoeRecipeTypes() {}
}
