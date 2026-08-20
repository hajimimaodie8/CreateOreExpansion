package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllTags.AllItemTags;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
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
 *     <li>附魔 JSON 的 supported_items 写对应 tag（tag 由 {@link AllTags} 声明 + Registrate datagen 生成）；</li>
 *     <li>在本类 {@link #RESTRICTIONS} 映射中登记「附魔注册键 → 允许的工具 tag」，铁砧即自动生效。</li>
 * </ul>
 */
@EventBusSubscriber(modid = CreateOreExpansion.MOD_ID)
public class AnvilEnchantmentGuard {

    /** 附魔注册键 → 允许附上的工具 tag（铁砧合并时校验） */
    private static final Map<ResourceKey<Enchantment>, TagKey<Item>> RESTRICTIONS = Map.of(
            ToolEnchantments.SWIFT_START, AllItemTags.COOLDOWN_TOOLS.tag,
            ToolEnchantments.REDUCE_CONSUMPTION, AllItemTags.SKILL_TOOLS.tag,
            ToolEnchantments.SKILL_BOOST, AllItemTags.SKILL_TOOLS.tag,
            ToolEnchantments.SKILL_REGRESSION, AllItemTags.SKILL_TOOLS.tag);

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        // 左槽为空、或左槽本身也是附魔书（书+书合并升级）、或右槽不是附魔书：不拦截
        if (left.isEmpty() || left.is(Items.ENCHANTED_BOOK) || !right.is(Items.ENCHANTED_BOOK)) {
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
