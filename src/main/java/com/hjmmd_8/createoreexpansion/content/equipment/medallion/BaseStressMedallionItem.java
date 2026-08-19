package com.hjmmd_8.createoreexpansion.content.equipment.medallion;

import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 凝能佩抽象基类（OOP 抽象/封装）：
 * 绑定、供能（供应/充能）、模式切换、Shift 概要等通用逻辑集中于此；
 * 子类仅通过 {@link #refundRatio}（回收比率）或覆盖 {@link #onSupplyConsumed} 特化能量回收，
 * 或叠加各自被动效果（如下界合金抗火）。
 */
public abstract class BaseStressMedallionItem extends Item implements ICurioItem, IMedallion {

    /** 概要主体（普通色）：#C29149（偏橙褐，Create 风格） */
    private static final Style BODY_STYLE = Style.EMPTY.withColor(0xC29149);
    /** 概要强调（_下划线_ 标记）：#EBD777（淡黄，仅变色不渲染下划线） */
    private static final Style EMPHASIS_STYLE = Style.EMPTY.withColor(0xEBD777);
    /** 每行前缀（xx:）与 Shift 键帽：灰/白（仿 Create condition 行与 ItemDescription 键帽） */
    private static final Style PREFIX_STYLE = Style.EMPTY.withColor(0xAAAAAA);
    private static final Style SHIFT_ACTIVE = Style.EMPTY.withColor(0xFFFFFF);
    private static final Style SHIFT_IDLE = Style.EMPTY.withColor(0xAAAAAA);
    /** 提示行前后缀：暗灰（照搬 Create DARK_GRAY） */
    private static final Style HINT_STYLE = Style.EMPTY.withColor(0x555555);

    /** 供应后能量回收比率（百分比；0 = 无回收） */
    protected final int refundRatio;

    protected BaseStressMedallionItem(Properties properties, int refundRatio) {
        super(properties);
        this.refundRatio = refundRatio;
    }

    // ========== IMedallion 实现（封装：能量/模式/绑定数据全部经由组件读写，逻辑归属本对象） ==========

    @Override
    public void bindTool(ItemStack self, ItemStack tool) {
        if (self.isEmpty() || tool.isEmpty())
            return;
        ResourceLocation selfId = self.getItemHolder()
            .unwrapKey()
            .map(key -> key.location())
            .orElse(null);
        ResourceLocation toolId = tool.getItemHolder()
            .unwrapKey()
            .map(key -> key.location())
            .orElse(null);
        if (selfId == null || toolId == null)
            return;

        tool.set(AllDataComponents.BOUND_MEDALLION, selfId);
        // 佩记录绑定的工具（去重，一枚佩可绑定多个工具）
        List<ResourceLocation> tools = new ArrayList<>(self.getOrDefault(AllDataComponents.BOUND_TOOL, List.of()));
        if (!tools.contains(toolId)) {
            tools.add(toolId);
        }
        self.set(AllDataComponents.BOUND_TOOL, tools);

        // 充能模式：绑定瞬间工具能量为空 → 佩补满
        if (isChargeMode(self) && ToolEnergy.getEnergy(tool) == 0) {
            refillTool(self, tool);
        }
    }

    @Override
    public void consumeToolEnergy(ItemStack self, ItemStack tool, int cost) {
        int selfEnergy = ToolEnergy.getEnergy(self);
        if (isChargeMode(self)) {
            // 充能：工具扣费（可扣到 0），佩立刻把工具补满（佩能量不足则补到耗尽）
            ToolEnergy.setEnergy(tool, Math.max(0, ToolEnergy.getEnergy(tool) - cost));
            refillTool(self, tool);
        } else {
            // 供应：先扣佩，佩耗尽再扣工具（工具能量不被提前改动）
            int fromSelf = Math.min(cost, selfEnergy);
            ToolEnergy.setEnergy(self, selfEnergy - fromSelf);
            if (fromSelf < cost) {
                ToolEnergy.setEnergy(tool, ToolEnergy.getEnergy(tool) - (cost - fromSelf));
            }
            // 供应后的能量回收（由子类比率/特化决定）
            onSupplyConsumed(self, fromSelf);
        }
    }

    /** 供应后回收：恢复消耗的 refundRatio%，且 50% 概率补回本次消耗（免单） */
    @Override
    public void onSupplyConsumed(ItemStack self, int consumed) {
        if (refundRatio <= 0 || consumed <= 0)
            return;
        int refund = consumed * refundRatio / 100;
        if (Math.random() < 0.5) {
            refund = consumed;
        }
        ToolEnergy.setEnergy(self, ToolEnergy.getEnergy(self) + refund);
    }

    @Override
    public void chargeBoundTools(Player player, ItemStack self) {
        if (player == null || !isChargeMode(self))
            return;
        if (ToolEnergy.getEnergy(self) <= 0)
            return;
        ResourceLocation selfId = self.getItemHolder()
            .unwrapKey()
            .map(key -> key.location())
            .orElse(null);
        if (selfId == null)
            return;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack tool = inventory.getItem(i);
            if (tool.isEmpty() || !ToolEnergy.hasEnergy(tool))
                continue;
            if (!selfId.equals(tool.get(AllDataComponents.BOUND_MEDALLION)))
                continue;
            refillTool(self, tool);
            if (ToolEnergy.getEnergy(self) <= 0)
                break;
        }
    }

    @Override
    public void toggleMode(Player player, ItemStack self) {
        boolean next = !isChargeMode(self);
        self.set(AllDataComponents.MEDALLION_MODE, next);
        player.displayClientMessage(Component.literal(next ? "凝能佩：已切换为充能模式" : "凝能佩：已切换为供应模式")
            .withStyle(ChatFormatting.GRAY), true);
    }

    /** 充能模式：消耗佩能量把工具补满 */
    private void refillTool(ItemStack self, ItemStack tool) {
        int need = ToolEnergy.getMaxEnergy(tool) - ToolEnergy.getEnergy(tool);
        int have = ToolEnergy.getEnergy(self);
        int give = Math.min(need, have);
        if (give <= 0)
            return;
        ToolEnergy.setEnergy(self, have - give);
        ToolEnergy.setEnergy(tool, ToolEnergy.getEnergy(tool) + give);
    }

    // ========== 物品交互 ==========

    /** 手持佩右键：另一手为能量工具时绑定（佩保留），否则切换供应/充能模式 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            ItemStack self = player.getItemInHand(hand);
            ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND);
            if (ToolEnergy.hasEnergy(other)) {
                bindTool(self, other);
                player.displayClientMessage(
                    Component.literal("已绑定：凝能佩 ↔ " + other.getHoverName().getString())
                        .withStyle(ChatFormatting.GRAY),
                    true);
            } else {
                toggleMode(player, self);
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    /** 右键不自动装备到 Curios 槽位（右键保留给模式切换），玩家拖拽放入槽位 */
    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return false;
    }

    /** 按住 Shift 查看概要（仿 Create：主体 #C29149，_下划线_ 标记部分 #EBD777；文本定义于 lang provider） */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
        TooltipFlag flag) {
        boolean shift = Screen.hasShiftDown();
        // 提示行：前后缀暗灰，Shift 键帽按住白色、否则灰色（照搬 Create ItemDescription）
        tooltip.add(Component.translatable("item.createoreexpansion.medallion.hold_shift",
            Component.literal("Shift").withStyle(shift ? SHIFT_ACTIVE : SHIFT_IDLE))
            .withStyle(HINT_STYLE));
        if (shift) {
            // 仿 Create：主题（summary）行 + 行为（behaviour）行
            tooltip.add(formatSummary("item.createoreexpansion.medallion.summary"));
            tooltip.add(formatSummary("item.createoreexpansion.medallion.bind"));
            tooltip.add(formatSummary("item.createoreexpansion.medallion.mode"));
            tooltip.add(formatSummary("item.createoreexpansion.medallion.supply"));
        }
    }

    /** 概要正文（照搬 Create TooltipHelper）：行首「xx:」前缀灰色，其余以主体色开始、_强调_ 段与普通段交替 */
    private static Component formatSummary(String langKey) {
        String raw = Component.translatable(langKey).getString();
        MutableComponent line = Component.empty();
        int colon = raw.indexOf('：');
        if (colon == -1)
            colon = raw.indexOf(':');
        if (colon > 0) {
            line.append(Component.literal(raw.substring(0, colon + 1)).withStyle(PREFIX_STYLE));
            raw = raw.substring(colon + 1);
        }
        boolean highlighted = false;
        for (String part : raw.split("_", -1)) {
            line.append(Component.literal(part).withStyle(highlighted ? EMPHASIS_STYLE : BODY_STYLE));
            highlighted = !highlighted;
        }
        return line;
    }
}
