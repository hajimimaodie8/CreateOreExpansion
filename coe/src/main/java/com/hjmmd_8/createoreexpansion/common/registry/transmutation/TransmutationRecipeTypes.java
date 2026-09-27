package com.hjmmd_8.createoreexpansion.common.registry.transmutation;

import com.hjmmd_8.createoreexpansion.common.registry.LayerRecipeType;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRecipeTypes;

/**
 * <b>嬗变配方类型（{@code createoreexpansion:transmuting}）的转发别名</b> —— W6-b2。
 *
 * <p><b>它现在只剩一行转发</b>：声明本体（{@code LayerRecipeType.processing("TRANSMUTING", …)}）
 * 搬进了 {@link CoeRecipeTypes#TRANSMUTING}，因为嬗化机制整块搬进第一层之后，
 * {@code LayerBootstrap} 的唤醒名单按方案 §2 收短成 {@code {coe.CoeRecipeTypes, cews.CewsRecipeTypes}}
 * ——{@code transmuting} 必须由 {@code CoeRecipeTypes} 的<b>第一个字段</b>创建，
 * 才能保住六个条目进 {@code BuiltInRegistries.RECIPE_TYPE} 的顺序（数值注册 id 7..12；
 * 反向的"这里的字段去转发 {@code CoeRecipeTypes}"写法实测会把 {@code charging} 挤到第 8 位，
 * 机理见 {@code CoeRecipeTypes#TRANSMUTING} 的注释）。</p>
 *
 * <p><b>为什么这个类还留着</b>：它的三个调用方
 * （{@code AllTransmutingRecipe} / {@code AllTransmutingType} / {@code TransmutationJeiCategories}）
 * 都通过 {@code TransmutationRecipeTypes.TRANSMUTING} 取这个常量，保留同名转发 = 那三处
 * <b>一行都不用改</b>（注册 id、序列化器、配方类型实例全部是同一个对象）。</p>
 *
 * <p>本类随嬗化整块进 {@code :coe}，<b>包名与类名一个字都没改</b>，所以
 * {@code WaveRecipeCapabilities} 与 {@code LayerBootstrap} 里可能出现过的类名字符串
 * （{@code prefix + "transmutation.TransmutationRecipeTypes"}）今天仍然解析得到——
 * 这也是本轮"零改包名"的直接结果。</p>
 */
public final class TransmutationRecipeTypes {

    /**
     * 嬗变加工：{@code createoreexpansion:transmuting}。同一个 {@link LayerRecipeType} 实例，
     * 声明住在 {@link CoeRecipeTypes#TRANSMUTING}（读本字段会先把那个类完整初始化）。
     */
    public static final LayerRecipeType TRANSMUTING = CoeRecipeTypes.TRANSMUTING;

    private TransmutationRecipeTypes() {}
}
