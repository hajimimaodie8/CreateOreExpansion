package com.hjmmd_8.createoreexpansion.client.hud;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.equipment.tool.energy.ToolEnergyHintClient;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * <b>工具能量提示层</b>（用户 2026-10-02 第 3 条：「能量消耗在快捷栏上的提示，离快捷栏的距离都太大了」）。
 *
 * <p>原来是原版<b>动作栏</b>（{@code displayClientMessage(..., true)}）。动作栏的 y 写死在原版
 * {@code Gui} 里（{@code guiHeight - 68}，文字再 {@code -4} ⇒ 文字正好落在
 * {@code H-72 .. H-64}，H = {@code guiHeight}），模组<b>没法只挪自己那一条</b>；改成自绘图层后
 * 位置完全由下面的 {@link #MARGIN_ABOVE_HOTBAR} 决定。文案与颜色仍由服务端给出
 * （{@code ToolEnergyHintPayload}），本层只负责画。</p>
 *
 * <h2>基线为什么是 59（几何口径，逐条对着原版源码量过）</h2>
 * <p>快捷栏正上方那一片在原版里是<b>排满的</b>，所有数字都以屏幕底部 H 为基准
 * （{@code net.minecraft.client.gui.Gui}，1.21.1 / NeoForge 21.1.228）：</p>
 * <ul>
 *     <li>{@code H-22 .. H}：快捷栏贴图本体（{@code renderItemHotbar} 的 blit y = H-22，
 *         选中框再往上 1 px）；</li>
 *     <li>{@code H-39 .. H-31}：生命（左）/ 饥饿（右）/ 经验条那一排
 *         （{@code leftHeight = rightHeight = 39}，{@code renderPlayerHealth}/{@code renderFood}）；
 *         经验等级数字在 {@code H-35}；</li>
 *     <li>{@code H-49 .. H-41}：护甲图标行（{@code renderArmor} 内部再减 10）与氧气行；
 *         <b>注意它占的是左右两侧各 80 px</b>，中间是空的；</li>
 *     <li>{@code H-59 .. H-51}：原版"切换物品名"弹窗那一行（{@code renderSelectedItemName}，
 *         {@code k = guiHeight - max(yShift, 59)}），背景条（若玩家开了文字背景）是
 *         {@code H-61 .. H-49}；</li>
 *     <li>{@code H-72 .. H-64}：动作栏（{@code renderOverlayMessage}），背景条 {@code H-74 .. H-62}。</li>
 * </ul>
 * <p>⇒ 一整行<b>居中</b>文案（本提示宽 150~250 px，必然横跨屏幕中线）唯一既压不到生命/饥饿/经验排、
 * 又压不到护甲图标行的位置，就是 {@code H-59 .. H-51} 这一带；取文字顶行 = {@code H-59}，
 * 于是 {@code MARGIN_ABOVE_HOTBAR = 59}。</p>
 *
 * <h2>代价（必须知道，别当缺陷修）</h2>
 * <p>这一带<b>正是原版物品名弹窗的位置</b>：刚切换快捷栏格子（弹窗 2 秒）又马上放技能时，两者会重叠。
 * 这是"更贴近快捷栏"与"原版这一片已经排满"之间的<b>唯一</b>交集 —— 再往下 10 px 就是护甲图标行
 * （重叠宽度 ~80 px，比物品名重合更难看），再往上就回到动作栏那一带（等于没改）。重叠时原版弹窗
 * 画在本层<b>之后</b>（图层顺序：HOTBAR → 本层 → … → SELECTED_ITEM_NAME），所以原版文案压在上面、
 * 依然可读；两条文案又都含工具名，视觉上不至于糊成一团。</p>
 *
 * @since 1.0.0
 */
@EventBusSubscriber(modid = CoeCore.MOD_ID, value = Dist.CLIENT)
public final class ToolEnergyHintHud {

    /**
     * 文字顶行距屏幕底部的像素数（= {@code guiHeight - 59}）。
     *
     * <p>取值理由见类注释的几何清单：这一行与原版物品名弹窗同带，往上就回到动作栏
     * （{@code H-72}，用户嫌远），往下就压到护甲图标行（{@code H-49}）。
     * 相对改动前的动作栏（文字顶行 {@code H-72}）<b>近了 13 px</b>，也是"不压原版任何一排"的极限。</p>
     */
    private static final int MARGIN_ABOVE_HOTBAR = 59;

    private ToolEnergyHintHud() {
        throw new AssertionError("This class should not be instantiated");
    }

    /** 与 {@code EquipmentSkillHud} 同一写法：注册到快捷栏之上。 */
    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, CoeCore.modLoc("tool_energy_hint"),
            ToolEnergyHintHud::render);
    }

    /**
     * 图层本体：有文案、且这一帧还没淡到看不见时才画。
     *
     * <p>与装备面板一致：界面打开时不画（否则会和背包/机器界面糊在一起）。</p>
     */
    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }
        Component line = ToolEnergyHintClient.visibleLine();
        if (line == null) {
            return;
        }
        int alpha = ToolEnergyHintClient.alpha();
        if (alpha <= ToolEnergyHintClient.MIN_ALPHA) {
            return;
        }
        int width = minecraft.font.width(line);
        int x = (minecraft.getWindow().getGuiScaledWidth() - width) / 2;
        int y = minecraft.getWindow().getGuiScaledHeight() - MARGIN_ABOVE_HOTBAR;
        // 淡出只能走着色器色（组件里的颜色是逐段 TextColor，没有 alpha 通道可调）；
        // 画完必须还原，否则后面的图层会跟着一起变透明（原版 Gui 也是这么收尾的）。
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha / 255.0F);
        graphics.drawString(minecraft.font, line, x, y, 0xFFFFFFFF, true);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
