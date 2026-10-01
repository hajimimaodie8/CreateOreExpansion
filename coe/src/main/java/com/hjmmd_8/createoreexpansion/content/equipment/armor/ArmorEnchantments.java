package com.hjmmd_8.createoreexpansion.content.equipment.armor;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * <b>盔甲附魔领域</b>：读取"散构聚能"在本模组四套盔甲上的等级。
 *
 * <p>与 {@code ToolEnchantments}（工具附魔：减耗 / 迅启 / 技艺提升 / 技艺回溯）同模式：
 * 附魔本身是<b>手写数据</b>（{@code data/createoreexpansion/enchantment/loose_convergence.json}），
 * 本类只负责"按注册键读等级"，不发散到各处去翻组件。</p>
 *
 * <h2>散构聚能是什么（用户 2026-10-01 定义）</h2>
 * <p>它是<b>宝藏附魔</b>（不进附魔台、可交易，见 {@code data/minecraft/tags/enchantment/} 下的
 * {@code treasure.json} 与 {@code tradeable.json}），<b>只有 1 级</b>。效果不是附魔自带的效果组件，
 * 而是<b>放宽"必须佩戴全套"这条门槛</b>：</p>
 * <ul>
 *   <li>身上有同套 <b>3 件</b>（就只差一件）时，<b>穿戴中任意一件护甲</b>带该附魔
 *       ⇒ 这一套视为<b>齐全</b>（那一件附魔补上缺的那件）；</li>
 *   <li>只有 <b>2 件</b>同套 ⇒ <b>不生效</b>（用户明确："如果身上只有两件配套装备，这个是不起作用的"）；</li>
 *   <li>4 件同套 ⇒ 本来就是全套，附魔无事可做。</li>
 * </ul>
 * <p>判定落在 {@link ArmorSet#wornLevel(net.minecraft.world.entity.player.Player)}（唯一生效入口），
 * 本类只提供"这件物品上有没有"。<b>能附在任意护甲上</b>（含原版/他模组）—— 用户 2026-10-01 选定，
 * 因为 JSON 的 {@code supported_items} 就是 {@code #minecraft:enchantable/armor}。</p>
 *
 * @since 1.0.0
 */
public final class ArmorEnchantments {

    /**
     * 「散构聚能」的注册键。
     *
     * <p>数据文件：{@code data/createoreexpansion/enchantment/loose_convergence.json}。
     * 名字含义：散开的构件仍能把能量聚起来（补齐缺的那一件）。</p>
     */
    public static final ResourceKey<Enchantment> LOOSE_CONVERGENCE = ResourceKey.create(
        Registries.ENCHANTMENT, CoeCore.modLoc("loose_convergence"));

    private ArmorEnchantments() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 该物品堆上「散构聚能」的等级。
     *
     * <p>读法与 {@code ToolEnchantments} 一致：遍历物品自身 {@code ENCHANTMENTS} 组件的
     * {@code keySet()} 用 {@link Holder#is(ResourceKey)} 比对 —— 这样<b>不需要</b>从注册表取
     * {@code Holder}，因此服务端/客户端/尚未加载数据包的时机都能安全调用。</p>
     *
     * @param stack 任意物品堆（护甲与否都行；空堆返回 0）
     * @return 附魔等级；未附魔返回 {@code 0}
     */
    public static int looseConvergenceLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) {
            return 0;
        }
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            if (holder.is(LOOSE_CONVERGENCE)) {
                return enchantments.getLevel(holder);
            }
        }
        return 0;
    }
}
