package com.hjmmd_8.createoreexpansion.compat.jei.category;

import com.hjmmd_8.createoreexpansion.compat.jei.animation.AnimatedPowerAngleGrinder;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.minecraft.client.gui.GuiGraphics;

/**
 * <b>研磨家族 JEI 类别的固定背景</b>（下箭头 + 阴影 + 角磨机模型）。
 *
 * <p>{@code GrindingCategory} / {@code AdvancedGrindingCategory} / {@code DismantlingCategory}
 * 三个类别的 {@code draw(...)} 原本逐字重复这 4 行（含 70 / 6 / 72 / 42 这组布局常量）。
 * 三者的<b>槽位布局本身</b>也共用同一组坐标（44,5 输入 / 118,48 输出），所以背景只留一处，
 * 免得改布局时漏改一个类别。</p>
 *
 * <p>只搬背景，<b>不搬</b>各分类自己的 {@code setRecipe}（它们的输出槽排布确实不同：
 * 拆磨是单槽概率槽，另外两个是多输出循环）—— 那是"看起来像"而不是同一件事。</p>
 *
 * @since 1.0.0
 */
public final class GrinderCategoryBackground {

    private GrinderCategoryBackground() {
        throw new AssertionError("This class should not be instantiated");
    }

    /**
     * 画固定背景：先下箭头、再阴影，最后把该类别自己的角磨机模型摆上去
     * （坐标逐字沿用三个类别原来的 70/6 与 72/42，未做任何"顺手改成常量"）。
     *
     * @param graphics 绘制上下文
     * @param grinder  该类别持有的角磨机动画（各分类用不同的轮模型，故由调用方传入）
     */
    public static void render(GuiGraphics graphics, AnimatedPowerAngleGrinder grinder) {
        AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 70, 6);
        AllGuiTextures.JEI_SHADOW.render(graphics, 72 - 17, 42 + 13);

        grinder.draw(graphics, 72, 42);
    }
}
