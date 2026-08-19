package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

import java.util.Map;

/**
 * 铁砧附魔书应用守卫。
 *
 * <p>原版铁砧合并附魔书时不会检查附魔的 supported_items（附魔台/指令才会），
 * 这里手动拦截：每个模组附魔限定只能应用到指定的工具 tag。</p>
 *
 * <p>新增加附魔时的标准：</p>
 * <ul>
 *     <li>附魔 JSON 的 supported_items 写对应 tag；</li>
 *     <li>在本类 {@link #RESTRICTIONS} 映射中登记「附魔注册键 → 允许的工具 tag」，铁砧即自动生效。</li>
 * </ul>
 */
@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public class AnvilEnchantmentGuard {

    /** 有技能的工具（减耗可附） */
    private static final TagKey<Item> SKILL_TOOLS = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("createoreexpansion", "skill_tools"));

    /** 释放技能有冷却的工具（迅启可附） */
    private static final TagKey<Item> COOLDOWN_TOOLS = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("createoreexpansion", "cooldown_tools"));

    /** 附魔注册键 → 允许附上的工具 tag（铁砧合并时校验） */
    private static final Map<ResourceKey<Enchantment>, TagKey<Item>> RESTRICTIONS = Map.of(
            ToolEnchantments.SWIFT_START, COOLDOWN_TOOLS,
            ToolEnchantments.REDUCE_CONSUMPTION, SKILL_TOOLS);

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (left.isEmpty() || !right.is(Items.ENCHANTED_BOOK)) {
            return;
        }
        ItemEnchantments stored = right.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (stored.isEmpty()) {
            return;
        }
        for (Holder<Enchantment> holder : stored.keySet()) {
            ResourceKey<Enchantment> key = holder.unwrapKey().orElse(null);
            if (key == null) {
                continue;
            }
            TagKey<Item> required = RESTRICTIONS.get(key);
            if (required != null && !left.is(required)) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
