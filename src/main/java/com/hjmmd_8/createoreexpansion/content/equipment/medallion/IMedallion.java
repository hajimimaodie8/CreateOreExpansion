package com.hjmmd_8.createoreexpansion.content.equipment.medallion;

import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.bridge.MedallionCurios;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 凝能佩能力抽象（多态入口）：绑定、供能、模式切换。
 * 能量工具供能体系只依赖本接口 —— 新增佩种实现本接口即可接入，无需改动既有代码（开闭原则）。
 *
 * <p><b>Curios 可选</b>：本接口不含任何 Curios 引用，"找佩"一律经
 * {@link MedallionCurios}（未装 Curios 时恒返回空 → 佩的能力不生效，但物品照旧存在）。</p>
 */
public interface IMedallion {

    /**
     * 从玩家 Curios 库存中查找与工具绑定的凝能佩（找不到返回空）。
     *
     * <p>未装 Curios 时恒为 {@link ItemStack#EMPTY}：工具退回"只吃自身 FE"的老路，不报错。</p>
     */
    static ItemStack findBoundMedallion(Player player, ItemStack tool) {
        if (player == null || tool == null || tool.isEmpty())
            return ItemStack.EMPTY;
        ResourceLocation id = tool.get(AllDataComponents.BOUND_MEDALLION);
        if (id == null)
            return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null || item == Items.AIR)
            return ItemStack.EMPTY;
        return findEquipped(player, item);
    }

    /** 玩家 Curios 槽位中佩戴的指定物品（未装 Curios / 未佩戴 → 空） */
    static ItemStack findEquipped(Player player, Item item) {
        return MedallionCurios.findEquipped(player, item);
    }

    /** 该佩是否为充能模式（true=充能，false=供应） */
    default boolean isChargeMode(ItemStack self) {
        return Boolean.TRUE.equals(self.get(AllDataComponents.MEDALLION_MODE));
    }

    /** 玩家是否在 Curios 任意槽位佩戴了指定佩（未装 Curios 恒 false） */
    static boolean isWearing(Player player, Item item) {
        return !findEquipped(player, item).isEmpty();
    }

    /** 供应模式消耗后的能量回收钩子（各佩按自身特性覆盖：概率/比例返还佩能量） */
    default void onSupplyConsumed(ItemStack self, int consumed) {
    }

    /** 绑定工具（佩保留）：双向写入绑定组件；充能模式且工具空能量时佩补满 */
    void bindTool(ItemStack self, ItemStack tool);

    /** 工具释放技能时的能量供给（供应/充能两种模式由实现自身处理） */
    void consumeToolEnergy(ItemStack self, ItemStack tool, int cost);

    /** 充能模式：检查玩家背包内所有绑定本佩的工具，没满的补满（不在背包中的工具不补） */
    void chargeBoundTools(Player player, ItemStack self);

    /** 切换供应/充能模式 */
    void toggleMode(Player player, ItemStack self);
}
