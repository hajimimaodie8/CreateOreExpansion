package com.hjmmd_8.createoreexpansion.common.registry.coe;

import com.hjmmd_8.createoreexpansion.common.registry.LayerRecipeType;
import com.hjmmd_8.createoreexpansion.common.registry.WaveRecipeCapabilities;
import com.hjmmd_8.createoreexpansion.common.registry.WaveRecipeCapabilities.LayerOrder;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.DismantlingRecipe;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.GrindingRecipe;
import com.hjmmd_8.createoreexpansion.content.lightning.LightningBlockRecipe;
import com.hjmmd_8.createoreexpansion.content.lightning.LightningRecipe;

/**
 * <b>COE（矿物拓展）自己的配方类型</b>（P3c：从 {@code common/AllRecipeTypes} 拆出）。
 *
 * <p>本层的四个配方类型——雷击加工（{@code lightning}）、方块雷击加工（{@code lightning_block}）、
 * 角磨（{@code grinding}）、拆解（{@code dismantling}）——原先声明在共享枚举里，
 * 现在按"谁的东西写在哪"搬到本层包下。注册 id、序列化器、配方类型与拆分前逐字一致
 * （id 由常量名 {@code Lang.asId} 得到，命名空间恒为 {@code createoreexpansion}）。</p>
 *
 * <p><b>声明顺序 = 拆分前枚举里的相对顺序</b>（LIGHTNING → LIGHTNING_BLOCK → GRINDING → DISMANTLING）。
 * 协调入口 {@code common/AllRecipeTypes} 按拆分前的<b>整体</b>顺序读这些常量，
 * 所以本类里的字段顺序请勿改动。</p>
 *
 * <p><b>本类不做注册动作</b>：两张注册表住在 {@link LayerRecipeType}（SHARED），
 * 由 {@code AllRecipeTypes.register(modEventBus)} 统一挂到事件总线——六个条目共用同一对注册表，
 * 这是"注册顺序不变"的前提。</p>
 */
public final class CoeRecipeTypes {

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
     * <b>本层的波加工能力登记</b>（P3i）：把自己这一份配方类型交给
     * {@link WaveRecipeCapabilities}，顺序即下面 {@code add(...)} 的参数顺序。
     *
     * <p>方向是「COE → 登记表（SHARED）」——登记表不 import 任何层、本类也不 import 别层的配方类型，
     * 所以它不产生任何跨层引用。放在静态块里 = "谁被类初始化，谁就登记自己"。</p>
     */
    static {
        WaveRecipeCapabilities.addOrdered(LayerOrder.COE)
            .add(LIGHTNING, LIGHTNING_BLOCK, GRINDING, DISMANTLING);
    }

    private CoeRecipeTypes() {}
}
