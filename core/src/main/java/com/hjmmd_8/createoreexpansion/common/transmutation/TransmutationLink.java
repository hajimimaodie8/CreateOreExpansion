package com.hjmmd_8.createoreexpansion.common.transmutation;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;

/**
 * <b>嬗变线（TRANS）与其它层之间的窄契约</b>（P3t：core 侧的唯一入口，具体实现由 TRANS 层注入）。
 *
 * <p><b>为什么需要它</b>：星辉石系列的两处被动住在 COE 层——{@code MedallionClientHandler}
 * 的客户端粒子（物品在虚空/岩浆/嬗变液中发光）与 {@code MedallionEffectHandler} 的嬗乱免疫 +
 * 液体处理——可它们要读的两个声明都在 TRANS 层：嬗变液的 {@code FluidType} 与嬗乱
 * {@code MobEffect}。而分层表里 <b>{@code COE -> TRANS} 是禁止方向</b>
 * （{@code tools/check-layering.ps1} 的 {@code Test-Forbidden}：允许 {@code TRANS -> COE}，
 * 反向不行；理由见 AGENTS「破环的设计规则」）。</p>
 *
 * <p><b>为什么不直接把那两个声明搬进 core</b>：流体声明（{@code TransmutationFluids} 的
 * Registrate 链）<b>必须</b>挂在某一层的 Registrate 实例上，否则它的 {@code .lang("Transmutation
 * Fluid")} 条目不会被任何一个"真正被执行的" datagen 提供器收集——{@code LayerRegistrate} 只为
 * 三层的实例挂提供器，core 里新建的 Registrate 没人挂，{@code runData} 会把语言文件里那一行
 * 删掉（{@code src/generated} 出现 diff）。所以走本仓已定型的<b>注入</b>手法
 * （形状与 {@code common.energy.MedallionLink}、{@code common.registry.LayerCreativeTab} 的
 * {@code installTabRegistrar} 相同）：把「COE 真正需要的那两件事」提成这个契约，
 * TRANS 在声明初始化时把实现注入进来。</p>
 *
 * <p><b>为什么契约暴露的是"查询"而不是"那两个对象"</b>：若返回 {@code FluidType} / {@code MobEffect}，
 * 调用方在未注入（或注册表尚未绑定）时就得自己处理 {@code null}，而
 * {@code Entity#getFluidTypeHeight} 对 {@code null} 参数会直接 NPE（fastutil 的
 * {@code Object2DoubleMap} 要对 key 取哈希）。提成查询后，未注入时一律得到 {@code false}，
 * 调用点的行为与"没有嬗变线"逐字一致，也不需要任何 {@code null} 分支。</p>
 *
 * <p><b>行为等价性</b>：{@link #isInTransmutationFluid} 用的还是旧的
 * {@code entity.getFluidTypeHeight(TRANSMUTATION_FLUID.get().getFluidType()) > 0.0D} 判定，
 * 逐字未变；{@link #isTransmutationDisorder} 的判据在本轮按用户裁定修正（见下两段），
 * 那是刻意的玩法改动，不是搬运误差。</p>
 *
 * <p><b>{@link #isTransmutationDisorder} 的入参与调用点逐字一致</b>：旧代码写的是
 * {@code event.getEffectInstance().getEffect() == AllModEffects.TRANSMUTATION_DISORDER.get()}，
 * 左边是 {@code Holder<MobEffect>}（1.21 起 {@code MobEffectInstance#getEffect()} 的返回类型），
 * 右边是 {@code MobEffect}。契约因此把入参定成 {@code Holder<MobEffect>}，调用点递进来的
 * 就是 {@code getEffect()} 的返回值，不需要任何转换。</p>
 *
 * <p><b>P3u：那个恒假的判据已按用户裁定修掉</b>。上面那个"Holder 与 MobEffect 的身份比较"
 * 编译能过（非 final 类可以转型成接口）却<b>恒为 {@code false}</b>，于是
 * {@code MedallionEffectHandler} 那条 {@code MobEffectEvent.Applicable} 兜底从来没有生效过：
 * 嬗乱只在"接触嬗变液"这一路被 {@code TransmutationEventHandler} 的早退拦住，
 * <b>非流体源</b>（雷鸣合金工具命中、黄玉弓的转化紊乱等）从未被凝能佩拦下。
 * 修法就是去掉右侧的 {@code .get()}——改成比较同一个 {@code Holder}：
 * {@code effect == TransmutationEffects.TRANSMUTATION_DISORDER}，实现见
 * {@code TransmutationFluids.Link#isTransmutationDisorder}。入参类型不变。
 * 这是本轮唯一的玩法改动：修后星辉石佩豁免<b>所有来源</b>的嬗乱。</p>
 *
 * <p><b>未注入时的可观测性</b>：{@link #NONE} 的两个查询都返回 {@code false} ⇒ "星辉石物品在
 * 嬗变液里不发光/不免疫嬗乱"。而唯一会读本契约的两处都发生在开档之后的 tick / 事件里，
 * 注入（{@code TransmutationFluids} 的静态块，由根构造器里原本那句 {@code AllFluids.register()}
 * 触发）远早于它们，所以这条路径只用于"契约本身能不能用"的探针与将来的模块裁剪。</p>
 */
public interface TransmutationLink {

    /** 未注入时的空实现（等价于「没有嬗变线」）。 */
    TransmutationLink NONE = new None();

    /** 当前实现（永不返回 {@code null}；未注入时为 {@link #NONE}）。 */
    static TransmutationLink get() {
        return LinkHolder.INSTANCE;
    }

    /**
     * 注入实现。只应由 TRANS 层的 {@code TransmutationFluids} 在自己的静态块里调用一次
     * （与 {@code MedallionEnergyLink#install()} 一样是"实现方自报"）。重复注入即覆盖
     * （幂等语义由调用方保证）。
     */
    static void install(TransmutationLink link) {
        LinkHolder.INSTANCE = link == null ? NONE : link;
    }

    /**
     * 该实体是否浸在嬗变液里。
     *
     * <p>旧写法：{@code entity.getFluidTypeHeight(嬗变液的 FluidType) > 0.0D}。
     * {@code entity} 为 {@code null} 时返回 {@code false}（不抛异常——契约的查询不该因为空入参爆掉）。</p>
     */
    default boolean isInTransmutationFluid(Entity entity) {
        return false;
    }

    /**
     * 该效果引用是不是嬗乱。
     *
     * <p>入参是 {@code Holder<MobEffect>}（{@code MobEffectInstance#getEffect()} 的返回类型），
     * 与调用点逐字一致。实现由 TRANS 层注入；未注入时返回 {@code false}
     * （等价于「没有嬗变线」= 凝能佩不豁免嬗乱）。P3u 修掉了旧实现里
     * "Holder 与 MobEffect 做身份比较"那个恒假判据，详见类注释。</p>
     */
    default boolean isTransmutationDisorder(Holder<MobEffect> effect) {
        return false;
    }

    /** 空实现：两个查询都返回 {@code false}。 */
    final class None implements TransmutationLink {
        private None() {
        }
    }

    /**
     * 当前实现的可变持有者（接口字段是 final，静态状态只能放这里）。
     *
     * <p><b>为什么不叫 {@code Holder}</b>：本接口的方法签名要用 {@code net.minecraft.core.Holder}
     * （{@code MobEffectInstance#getEffect()} 的返回类型），嵌套类若同名就会把它遮蔽掉
     * （实测：{@code 类型 Holder 不带有参数}）。{@code MedallionLink.Holder} 那个先例的签名里
     * 没有 MC 的 {@code Holder}，所以没有这个问题。</p>
     */
    final class LinkHolder {
        private LinkHolder() {
        }

        static volatile TransmutationLink INSTANCE = NONE;
    }
}
