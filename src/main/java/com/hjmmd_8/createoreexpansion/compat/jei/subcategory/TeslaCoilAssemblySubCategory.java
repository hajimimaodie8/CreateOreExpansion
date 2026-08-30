package com.hjmmd_8.createoreexpansion.compat.jei.subcategory;

import com.hjmmd_8.createoreexpansion.compat.jei.animation.TeslaCoilAnimation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 序列加工 JEI 子分类：CC&amp;A 充电步骤的机器动画（特斯拉线圈）。
 *
 * <p>使用自绘 {@link TeslaCoilAnimation}（无底部阴影）。整体<b>下移</b>对齐
 * 部署步骤（机械手+置物台）中置物台的高度：部署动画 y≈50、置物台在其下方，
 * 故本子分类整体下移至 y≈68 与置物台平齐。</p>
 */
public class TeslaCoilAssemblySubCategory extends SequencedAssemblySubCategory {

	private final TeslaCoilAnimation teslaCoil = new TeslaCoilAnimation();

	public TeslaCoilAssemblySubCategory() {
		super(20);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, SequencedRecipe<?> recipe, IFocusGroup focuses, int x) {}

	@Override
	public void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int index) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		// 下移到置物台位置（对齐部署动画中的置物台高度 y≈68）
		ms.translate(0, 68, 0);
		ms.scale(.6f, .6f, .6f);
		teslaCoil.draw(graphics, getWidth() / 2, 0);
		ms.popPose();
	}
}
