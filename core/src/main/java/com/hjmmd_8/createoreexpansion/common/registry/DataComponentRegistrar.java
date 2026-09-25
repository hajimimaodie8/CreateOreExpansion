package com.hjmmd_8.createoreexpansion.common.registry;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * <b>全模组数据组件（{@code DataComponentType}）唯一的 {@link DeferredRegister}</b>。
 *
 * <p><b>为什么它单独住一个类</b>（P3p）：数据组件按「声明住哪一层」拆成了两处——
 * 共享的一批在 {@code common.energy.ToolDataComponents}（core），
 * 技能线专属的 {@code skills} 仍在 COE 的
 * {@code common.registry.coe.AllDataComponents}。两处必须用<b>同一个</b>
 * {@code DeferredRegister} 实例，才能保证条目进注册表的顺序与拆分前逐字一致
 * （{@code DeferredRegister} 内部是 {@code LinkedHashMap}，顺序 = {@code register(...)} 的调用顺序）。</p>
 *
 * <p>而「谁先被类初始化」不能拿来当顺序保证：{@code AllDataComponents.SKILLS} 的字段初始化器
 * 要调用注册方法，那一刻就会触发本类的类初始化——所以本类<b>绝不能</b>声明任何组件字段，
 * 否则那些字段会抢在 {@code skills} 之前进注册表。顺序的触发点见 {@code AllDataComponents}
 * 的字段文本顺序（{@code skills} → 触发 {@code ToolDataComponents} 的 2..10）。</p>
 *
 * <p>注册命名空间仍是 {@link CoeCore#REGISTRY_NAMESPACE}（{@code createoreexpansion}），
 * 与拆分前完全一致。</p>
 */
public final class DataComponentRegistrar {

    /** 唯一的数据组件注册器（{@code AllDataComponents#register(IEventBus)} 负责挂到 mod 事件总线）。 */
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CoeCore.REGISTRY_NAMESPACE);

    private DataComponentRegistrar() {
    }
}
