package com.hjmmd_8.createoreexpansion.content.equipment.armor;

import java.util.List;

import com.hjmmd_8.createoreexpansion.content.skill.tooltip.SkillsTooltipHandler;
import com.hjmmd_8.createoreexpansion.integration.skiller.ArmorSkillProvider;
import com.hjmmd_8.createoreexpansion.integration.skiller.client.CoeSkillClient;
import com.hjmmd_8.createoreexpansion.content.skill.input.AllKeys;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * <b>护甲 / 套装技能 tooltip</b>（用户 2026-10-01）：
 * 「工具那些 tooltip 有"按住左 Alt 查看技能等级"，装备也应该有 —— 要在单独的装备栏里单独展示。」
 *
 * <h2>与工具那一套的关系</h2>
 * <p>等级配色<b>不在这里写</b>：一律复用 {@link SkillsTooltipHandler#levelColor(int)}
 * （工具 tooltip 的唯一出处），所以"2 级绿、3 级蓝"两边永远一致。
 * 技能行也复用 {@link SkillsTooltipHandler#skillLine(Component, Component, int)}。</p>
 *
 * <h2>等级从哪来（用户 2026-10-02 修正：穿着⇒真实等级／未穿⇒按该件附魔<b>预测</b>）</h2>
 * <p>装备技能等级是<b>逐技能</b>的（规格 §0.1/§0.2）：每个技能有自己的基准等级
 * （{@link ArmorSkillLevels}），对每个技能逐件算 {@code 基准 + 技艺提升 − 记忆回溯} 取最大值
 * （{@code ArmorSkillRuntime#effectiveLevel}）；这里对<b>每一条技能行</b>各自问一次等级，
 * 所以两行可能显示不同的罗马数字与不同的颜色。三条分支：</p>
 * <ol>
 *   <li><b>正穿着这件所属的那一套</b>（{@code ArmorSet.effectiveSet(player) == set}）⇒
 *       <b>真实等级</b> {@code ArmorSkillRuntime#levelOf(Player, String)}
 *       —— 与 HUD 同一个入口，逐值一致；</li>
 *   <li><b>没穿整套 / 穿的是别的套 / 根本没穿</b> ⇒ <b>预测等级</b>
 *       {@code ArmorSkillRuntime#predictedLevelOf(ItemStack, String)} =
 *       {@code clamp(该套该技能的基准 + 该件的技艺提升 − 该件的记忆回溯, 1, 4)}。
 *       ⚠ 它<b>只读被悬停的这一件</b>（没穿就取不到四件），且<b>不参与任何真实技能结算</b> ——
 *       纯粹是"穿上之后大概几级"的预告；</li>
 *   <li><b>创造页搜索栏</b>（{@code event.getEntity()} 为 null）⇒ 只留提示行，不猜等级
 *       （也避免再出现"0 级橙色"那种怪东西）。</li>
 * </ol>
 * <p><b>修的是取值入口，不是色表</b>：旧实现每行统一问 {@code levelOf}，没穿整套时它返回 0 ⇒
 * {@link SkillsTooltipHandler#levelColor(int)} 落进 {@code default} 橙色、{@code toRoman(0)} 又把它
 * 伪装成"I"。现在没穿时取到的是 ≥1 的预测等级，橙色在 tooltip 上不再出现。</p>
 *
 * @since 1.0.0
 */
public final class ArmorSkillTooltipHandler {

    /** 「按住 [Alt] 可查看套装技能等级」提示行。 */
    private static final String TIP_TRANSLATE_KEY = "createoreexpansion.tooltip.armor_skills";

    private ArmorSkillTooltipHandler() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 若该物品属于本模组的某一套护甲，往后追加<b>装备技能区</b>（独立于工具技能区）。
     *
     * @param startIndex 现有内容之后的下一个可用 index
     * @return 插入完成后的下一个可用 index（不适用时原样返回）
     */
    public static int addArmorSkillTooltip(ItemTooltipEvent event, int startIndex) {
        ItemStack stack = event.getItemStack();
        ArmorSet set = ArmorSet.of(stack);
        if (set == null) {
            return startIndex;
        }
        List<Component> tip = event.getToolTip();
        int index = startIndex;
        if (index > 1) {
            tip.add(index++, CommonComponents.EMPTY);
        }
        tip.add(index++, Component.translatable(TIP_TRANSLATE_KEY, altKey()).withStyle(ChatFormatting.GRAY));
        if (!net.minecraft.client.gui.screens.Screen.hasAltDown()) {
            return index;
        }
        Player player = event.getEntity();
        if (player == null) {
            // 创造页搜索栏没有玩家上下文：只留提示行，不猜等级（避免显示成"0 级橙色"这种怪东西）
            return index;
        }
        // 分支判据只算一次（用户 2026-10-02 三分支口径）：
        // 玩家此刻**生效**的那一套正好是这件所属的套 ⇒ 走真实等级；否则走"这一件的预测等级"。
        ArmorSet worn = ArmorSet.effectiveSet(player);
        List<ResourceLocation> ids = ArmorSkillProvider.skillIdsOf(set);
        for (int slotIndex = 0; slotIndex < ids.size(); slotIndex++) {
            ResourceLocation id = ids.get(slotIndex);
            AllKeys key = CoeSkillClient.skillKeyForEquipmentSlot(ArmorSkillProvider.SLOT_BASE + slotIndex);
            Component keyName = key == null ? Component.empty() : key.getKeybind().getTranslatedKeyMessage();
            Component name = Component.translatable("skill." + id.getNamespace() + "." + id.getPath());
            // **逐技能**取等级：每条技能行用自己的等级。
            //   穿着本套 ⇒ levelOf（与 HUD 同一个入口）；
            //   没穿     ⇒ predictedLevelOf（公式与 1..4 钳位都在 ArmorSkillRuntime，本类不拼公式）。
            int level = worn == set
                ? ArmorSkillRuntime.levelOf(player, id.getPath())
                : ArmorSkillRuntime.predictedLevelOf(stack, id.getPath());
            tip.add(index++, SkillsTooltipHandler.skillLine(keyName, name, level));
        }
        return index;
    }

    /** Alt 键帽：按住时白色高亮，否则跟随外层灰色（与工具那一套的观感一致）。 */
    private static Component altKey() {
        Component alt = Component.literal("Alt");
        return net.minecraft.client.gui.screens.Screen.hasAltDown() ? alt.copy().withStyle(ChatFormatting.WHITE) : alt;
    }
}
