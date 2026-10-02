package com.hjmmd_8.createoreexpansion.content.equipment.tool.energy;

import net.minecraft.network.chat.Component;

/**
 * <b>客户端：工具能量文案的当前状态</b>（收到 {@link ToolEnergyHintPayload} 后显示若干 tick）。
 *
 * <p>刻意<b>不</b>引用任何客户端专属类型（没有 {@code Minecraft}、没有 {@code GuiGraphics}）：
 * 这样即使专用服务端因类加载顺序碰到本类也不会有问题，而真正画的那一半（窗口、字体、图层）
 * 全部留在客户端 HUD 层 {@code client.hud.ToolEnergyHintHud}。</p>
 *
 * <h2>时长口径照抄原版动作栏</h2>
 * <ul>
 *     <li>{@link #SHOW_TICKS} = 60：原版 {@code Gui#setOverlayMessage} 给动作栏的寿命就是 60 tick；</li>
 *     <li>最后 {@link #FADE_TICKS} = 20 tick 线性淡出：原版 {@code renderOverlayMessage} 用
 *         {@code f * 255 / 20} 做同一件事，效果上是"最后 1 秒慢慢消失"；</li>
 *     <li>{@link #MIN_ALPHA} = 8：原版只在 {@code i > 8} 时才画，这里同一个门槛，
 *         免得最后几帧用几乎全透明的字去糊屏幕。</li>
 * </ul>
 *
 * <p>同一 tick 又来一条就<b>直接替换</b>（与动作栏一致），不排队、不叠加。</p>
 *
 * @since 1.0.0
 */
public final class ToolEnergyHintClient {

    /** 一条文案显示多少 tick（= 原版动作栏的 60）。 */
    public static final int SHOW_TICKS = 60;

    /** 最后多少 tick 用来淡出（= 原版动作栏的 20）。 */
    public static final int FADE_TICKS = 20;

    /** 低于这个不透明度就不画（= 原版动作栏的 {@code i > 8}）。 */
    public static final int MIN_ALPHA = 8;

    /** 当前文案（没有时 null）。 */
    private static Component line;

    /** 还剩多少 tick。 */
    private static int ticksLeft;

    private ToolEnergyHintClient() {
        throw new AssertionError("This class should not be instantiated");
    }

    /** 收到服务端文案：立刻替换当前那条并重置计时。 */
    public static void show(Component hint) {
        line = hint;
        ticksLeft = SHOW_TICKS;
    }

    /** 客户端 tick 推进一步（由 {@code CoeSkillClient#onClientTick} 调用，每 tick 恰好一次）。 */
    public static void tick() {
        if (ticksLeft > 0 && --ticksLeft == 0) {
            line = null;
        }
    }

    /** 离开世界时清空（下次由服务端重新发）。 */
    public static void clear() {
        line = null;
        ticksLeft = 0;
    }

    /** 现在该画的那一行；没有/已过期返回 {@code null}。 */
    public static Component visibleLine() {
        return ticksLeft > 0 ? line : null;
    }

    /** 当前不透明度（0..255）。 */
    public static int alpha() {
        if (ticksLeft <= 0) {
            return 0;
        }
        if (ticksLeft >= FADE_TICKS) {
            return 255;
        }
        return ticksLeft * 255 / FADE_TICKS;
    }
}
