package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.registry.LayerRecipeType;
import com.hjmmd_8.createoreexpansion.common.registry.WaveRecipeCapabilities;
import com.hjmmd_8.createoreexpansion.common.registry.WaveRecipeCapabilities.LayerOrder;
import com.hjmmd_8.createoreexpansion.common.registry.transmutation.TransmutationRecipeTypes;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.DismantlingRecipe;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.GrindingRecipe;
import com.hjmmd_8.createoreexpansion.content.lightning.LightningBlockRecipe;
import com.hjmmd_8.createoreexpansion.content.lightning.LightningRecipe;
import com.hjmmd_8.createoreexpansion.content.transmuting.AllTransmutingRecipe;

/**
 * <b>COE（矿物拓展）自己的配方类型</b>（P3c：从 {@code common/AllRecipeTypes} 拆出）。
 *
 * <p>本层的四个配方类型——雷击加工（{@code lightning}）、方块雷击加工（{@code lightning_block}）、
 * 角磨（{@code grinding}）、拆解（{@code dismantling}）——原先声明在共享枚举里，
 * 现在按"谁的东西写在哪"搬到本层包下。注册 id、序列化器、配方类型与拆分前逐字一致
 * （id 由常量名 {@code Lang.asId} 得到，命名空间恒为 {@code createoreexpansion}）。</p>
 *
 * <p><b>声明顺序 = 拆分前枚举里的相对顺序</b>（LIGHTNING → LIGHTNING_BLOCK → GRINDING → DISMANTLING）。
 * 层间顺序由 core 的 {@code common.registry.LayerBootstrap} 用固定名单（W6-b2 起 =
 * {@code CoeRecipeTypes → CewsRecipeTypes}）唤醒各层来保证，所以本类里的字段顺序请勿改动。</p>
 *
 * <p><b>W6-b2</b>：嬗化机制整块搬进本层后，本类的<b>第一个</b>字段变成
 * {@link #TRANSMUTING} 的<b>声明本体</b>（{@link TransmutationRecipeTypes} 现在只是它的同名
 * 转发别名），于是六个条目进注册表的顺序仍是拆分前的
 * {@code transmuting → lightning → lightning_block → grinding → dismantling → charging}。
 * 为什么"第一行"是承重的、以及本轮实测踩到的错误形态 → 见 {@link #TRANSMUTING} 自己的注释。</p>
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
     * <b>波加工能力登记</b>（P3i）：把本层这五个配方类型交给 {@link WaveRecipeCapabilities}，
     * 顺序即下面两条 {@code add(...)} 的参数顺序。
     *
     * <p>方向是「COE → 登记表（SHARED）」——登记表不 import 任何层、本类也不 import 别层的配方类型，
     * 所以它不产生任何跨层引用。放在静态块里 = "谁被类初始化，谁就登记自己"。</p>
     *
     * <p><b>W6-b2</b>：{@link #TRANSMUTING} 的登记也在这里，但排序键仍是
     * {@link LayerOrder#TRANS}（<b>不是</b> {@code COE}）—— 组权值决定"波优先加工哪种配方"，
     * 它与"这个类住在哪个 Gradle 模块"是两件事。{@code WaveRecipeCapabilities.all()} 的展开顺序
     * 因此与拆分前逐字相同：{@code transmuting → lightning → lightning_block → grinding → dismantling
     * → charging}。</p>
     *
     * <p>静态块在全部字段初始化器<b>之后</b>执行，所以这里读到的五个常量都已创建；
     * 而"条目进注册表的顺序"在那个时刻已经定下（见 {@link #TRANSMUTING} 的注释）。</p>
     */
    static {
        WaveRecipeCapabilities.addOrdered(LayerOrder.TRANS).add(TRANSMUTING);
        WaveRecipeCapabilities.addOrdered(LayerOrder.COE)
            .add(LIGHTNING, LIGHTNING_BLOCK, GRINDING, DISMANTLING);
    }

    private CoeRecipeTypes() {}
}
