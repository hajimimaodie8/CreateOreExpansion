package com.hjmmd_8.createoreexpansion.common.registry.transmutation;

import com.hjmmd_8.createoreexpansion.common.registry.LayerRecipeType;
import com.hjmmd_8.createoreexpansion.common.registry.WaveRecipeCapabilities;
import com.hjmmd_8.createoreexpansion.common.registry.WaveRecipeCapabilities.LayerOrder;
import com.hjmmd_8.createoreexpansion.content.transmuting.AllTransmutingRecipe;

/**
 * <b>TRANS（机械嬗变化）自己的配方类型</b>（P3c：从 {@code common/AllRecipeTypes} 拆出）。
 *
 * <p>本层目前只有一个配方类型：嬗变（{@code createoreexpansion:transmuting}）——
 * 鼓风机吹过嬗变液时的工作盆加工。注册 id、序列化器、配方类型与拆分前逐字一致。</p>
 *
 * <p><b>本类不做注册动作</b>：两张注册表住在 {@link LayerRecipeType}（core），
 * 由 {@code LayerBootstrap.ensureAttached(modEventBus)} 统一挂到事件总线——六个条目共用同一对注册表
 * （且"恰挂一次"），这是"注册顺序不变"的前提。本类会被 {@code LayerBootstrap} 的固定名单
 * <b>第一个</b>唤醒，所以 {@code transmuting} 仍是六个条目里的第一个。</p>
 */
public final class TransmutationRecipeTypes {

    /** 嬗变加工：{@code createoreexpansion:transmuting}。 */
    public static final LayerRecipeType TRANSMUTING =
        LayerRecipeType.processing("TRANSMUTING", AllTransmutingRecipe::new);

    /**
     * <b>本层的波加工能力登记</b>（P3i）：把自己这一份配方类型交给
     * {@link WaveRecipeCapabilities}。
     *
     * <p><b>只装 CEWS 时本类根本不会被初始化</b> ⇒ 登记表里就没有 {@code transmuting}
     * ⇒（波变器的鼓风机少一项可加工类型），但没有任何一层因此崩掉。这是本方案刻意要的性质：
     * "少一项能力" 而不是 "少一层就报错"。</p>
     */
    static {
        WaveRecipeCapabilities.addOrdered(LayerOrder.TRANS).add(TRANSMUTING);
    }

    private TransmutationRecipeTypes() {}
}
