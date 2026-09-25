package com.hjmmd_8.createoreexpansion.common.energy;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * <b>凝能佩联动契约</b>（P3p：core 侧的唯一入口，具体实现由 COE 层注入）。
 *
 * <p><b>为什么需要它</b>：工具能量门面 {@link ToolEnergy} 已搬进共享库（core），
 * 而 {@code content.equipment.medallion.IMedallion}（佩的能力抽象）属于矿物拓展层的内容——
 * 库绝不能反向 import 层里的类。于是把「能量门面真正用到的那 5 件事」提成这个契约：
 * 找佩、判佩、判充能模式、扣能、补满绑定工具。层里实现它，并在 mod 构造期
 * {@linkplain #install(MedallionLink) 注入}。</p>
 *
 * <p><b>行为等价性</b>：{@link #NONE} 的默认实现（找不到佩 / 不是佩 / 非充能 / 不扣不减）
 * 与"未装 Curios 或工具没绑佩"时的老路径逐字相同；注入实现后，五个方法的语义
 * 与旧 {@code ToolEnergy} 直接调 {@code IMedallion} 的写法一一对应，无任何玩法差异。</p>
 */
public interface MedallionLink {

    /** 未注入时的空实现（等价于「没有凝能佩体系」）。 */
    MedallionLink NONE = new None();

    /** 当前实现（永不返回 {@code null}；未注入时为 {@link #NONE}）。 */
    static MedallionLink get() {
        return Holder.INSTANCE;
    }

    /**
     * 注入实现。只应由矿物拓展层的 {@code MedallionEnergyLink#install()} 在 mod 构造期调用一次。
     * 重复注入即覆盖（幂等语义由调用方保证）。
     */
    static void install(MedallionLink link) {
        Holder.INSTANCE = link == null ? NONE : link;
    }

    /** 从玩家（Curios 库存）中查找与工具绑定的凝能佩；找不到返回 {@link ItemStack#EMPTY}。 */
    default ItemStack findBound(Player player, ItemStack tool) {
        return ItemStack.EMPTY;
    }

    /** 该物品是不是凝能佩（旧写法：{@code item instanceof IMedallion}）。 */
    default boolean isMedallion(ItemStack stack) {
        return false;
    }

    /** 该佩是否处于充能模式（true=充能，false=供应）。 */
    default boolean isChargeMode(ItemStack medallion) {
        return false;
    }

    /** 供应/充能两种模式下的扣能（由佩自己实现）。 */
    default void consumeToolEnergy(ItemStack medallion, ItemStack tool, int amount) {
    }

    /** 充能模式：扣费后把玩家背包内绑定本佩的工具补满。 */
    default void chargeBoundTools(Player player, ItemStack medallion) {
    }

    /** 空实现：所有查询都返回"没有佩"。 */
    final class None implements MedallionLink {
        private None() {
        }
    }

    /** 当前实现的可变持有者（接口字段是 final，静态状态只能放这里）。 */
    final class Holder {
        private Holder() {
        }

        static volatile MedallionLink INSTANCE = NONE;
    }
}
