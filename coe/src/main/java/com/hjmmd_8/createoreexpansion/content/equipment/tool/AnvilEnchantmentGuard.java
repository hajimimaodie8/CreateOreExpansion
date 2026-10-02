package com.hjmmd_8.createoreexpansion.content.equipment.tool;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

/**
 * 铁砧附魔书应用守卫。
 *
 * <p>NeoForge 的铁砧<b>自己会</b>检查"这条附魔支不支持左槽"（{@code AnvilMenu} 的
 * {@code itemstack.supportsEnchantment(holder)} 补丁），但它是<b>逐条静默忽略</b>：
 * 不支持的附魔被跳过、支持的照旧写上去，不报错、也不清空输出（只有书上<b>全部</b>附魔都不支持时才清空），
 * 而且<b>创造模式一律放行</b>。本守卫的净作用因此只有两条：① 书上只要有<b>任意一条</b>本模组附魔
 * 不被左槽支持，<b>整本</b>取消（NeoForge 的取消语义＝输出槽清空 + 代价归零）；
 * ② 创造模式下同样拦。两条都与本次改动之前一致，是<b>刻意保留</b>的语义（守卫要留、不许删）。</p>
 *
 * <p>判据：<b>只对本模组命名空间（{@code createoreexpansion}）的附魔</b>，直接问附魔自己
 * 「你支不支持这件物品」——{@link Enchantment#canEnchant(ItemStack)}，其内部就是附魔 JSON 的
 * {@code supported_items}。这就是「哪些物品能吃这个附魔」的<b>唯一真源</b>：铁砧与附魔台共用它。</p>
 *
 * <p>新增加附魔时的标准：</p>
 * <ul>
 *     <li>只写附魔 JSON 的 {@code supported_items} 一处，铁砧与附魔台都会自动跟随；</li>
 *     <li><b>不要</b>在本类里再登记「附魔注册键 → 允许的物品 tag」这种第二份表示。
 *         2026-10-02 删掉的那张硬编码表就是这样漂移的：表里只有工具（{@code #skill_tools} /
 *         {@code #cooldown_tools}，0 件盔甲），而附魔 JSON 经 {@code skill_boostable} 已含盔甲
 *         ⇒ 盔甲在铁砧里被一律取消，且没有任何告警。</li>
 * </ul>
 *
 * <p>被取消时 NeoForge 会把输出槽清空、代价归零（{@code CommonHooks} 的取消语义）：
 * 混装书（含本模组附魔 + 其它附魔）里只要有任意一条本模组附魔不支持左槽，<b>整本</b>被拦
 * ——这与改前一致，是刻意保留的语义。</p>
 *
 * <p><b>P3l 为什么它搬来 COE</b>：它守卫的是<b>工具附魔</b>（2026-10-02 之前四个附魔的
 * allowed tag 是 {@code COOLDOWN_TOOLS} / {@code SKILL_TOOLS}），而附魔声明住在工具能量契约里
 * （当时是 {@code common/energy}，P12 起包名还原为 {@code content/equipment/tool/energy}，
 * 两份都仍在共享库 core）⇒ 留在 {@code common} 顶层就是一个 {@code CORE → 根侧 SHARED} 的
 * 引用，core 编译期看不见（探针实测 3 个「找不到符号」：{@code ToolEnchantments}）。
 * 搬到 COE 后 {@code content → 共享库} 是合法方向（{@code CORE} 是各层都能引用的底层）。</p>
 */
// modid 恒为 "createoreexpansion"（值未变），从共享库取是因为库不能反向依赖根 @Mod 入口
// CreateOreExpansion。
@EventBusSubscriber(modid = CoeCore.MOD_ID)
public class AnvilEnchantmentGuard {

    /** 本模组的注册命名空间：只有这个命名空间的附魔才走本守卫的校验 */
    private static final String NAMESPACE = CoeCore.REGISTRY_NAMESPACE;

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
            // 只校验本模组自己的附魔：给原版/他模组附魔也加一道铁砧校验属于行为变更，超出范围
            if (!NAMESPACE.equals(key.location().getNamespace())) {
                continue;
            }
            // 唯一判据：附魔自己说支不支持左槽（= supported_items），不再有第二张表。
            // ⚠ NeoForge 21.1.228 已把 canEnchant 标为 @Deprecated（替代品是 ItemStack#supportsEnchantment，
            // 两者只在"左槽是附魔书"与"物品覆写了 supportsEnchantment"时不同）；本轮按需求原文保留 canEnchant，
            // 若要换成 supportsEnchantment，必须同步改 tools/check-armor-sets.ps1 §11b 的断言（它钉着 canEnchant(）。
            if (!holder.value().canEnchant(left)) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
