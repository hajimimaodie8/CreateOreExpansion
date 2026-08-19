package com.hjmmd_8.createoreexpansion.client;

import com.hjmmd_8.createoreexpansion.client.tool.SkillsStrategyRenderer;
import com.hjmmd_8.createoreexpansion.common.AllDataComponents;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.IMedallion;
import com.hjmmd_8.createoreexpansion.content.equipment.medallion.handler.MedallionEffectHandler;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.EnergyTooltipHandler;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergy;
import com.hjmmd_8.createoreexpansion.content.skill.tooltip.SkillsTooltipHandler;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * AOE 范围挖掘 — 客户端预选框渲染注册
 *
 * <p>在 {@link RenderLevelStageEvent.Stage#AFTER_CUTOUT_BLOCKS} 阶段，
 * 调用所有工具的 outline render 绘制范围线框。</p>
 *
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onWorldRenderLast(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            Minecraft instance = Minecraft.getInstance();

            // 统一调用所有渲染器
            SkillsStrategyRenderer.INSTANCE.schedule(
                    instance.level,
                    event.getCamera(),
                    event.getPoseStack(),
                    DefaultSuperRenderTypeBuffer.getInstance()
            );
        }

        SkillsStrategyRenderer.INSTANCE.render(event.getStage());
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        // 技能区在前（index 1 起），能量条紧跟技能区 —— 技能信息显示在能量上方
        int index = SkillsTooltipHandler.addSkillsTooltip(event);
        index = EnergyTooltipHandler.addEnergyTooltip(event, index);
        // 绑定行位于能量区之后（能量标题行、能量条、再下一行）
        EnergyTooltipHandler.addBoundTooltip(event, index);
        // 星辉石/雷鸣工具与佩：按住 Alt 或 Shift 查看概要时，概要顶部追加祝福行（物品自身颜色）
        addBlessingLine(event);
    }

    /** 星辉/雷鸣祝福行：常驻显示在 tooltip 顶部（物品名下方第一行） */
    private static void addBlessingLine(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        String text = null;
        if (MedallionEffectHandler.isStellarstoneItem(stack)) {
            boolean isMedallion = stack.getItem() instanceof IMedallion;
            text = isMedallion
                ? "星辉的祝福：免疫岩浆、虚空与嬗化液，佩戴时可无视嬗乱效果"
                : "星辉的祝福：免疫岩浆、虚空与嬗化液，手持工具时可无视嬗乱效果";
        } else if (MedallionEffectHandler.isThunderiteItem(stack)) {
            text = "雷鸣之保佑：免疫雷击效果与岩浆，在受到雷击后可补满能量";
        }
        if (text == null)
            return;
        // 只对工具（含能量组件）与佩显示；星辉石/雷鸣石材料不显示祝福行
        if (!(stack.getItem() instanceof IMedallion) && !ToolEnergy.hasEnergy(stack))
            return;
        int color = stack.getOrDefault(AllDataComponents.ENERGY_COLOR, 0xDF98A7);
        // 常驻显示：不依赖按住 Shift/Alt
        List<Component> tip = event.getToolTip();
        tip.add(1, Component.literal(text).withStyle(style -> style.withColor(color)));
    }
}
