package com.hjmmd_8.createoreexpansion.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 序列加工 JEI 子分类：充能步骤的机器动画（翡翠应力充能器）。
 */
public class ChargingAssemblySubCategory extends SequencedAssemblySubCategory {

	private final AnimatedJadeCharger charger = new AnimatedJadeCharger();

	public ChargingAssemblySubCategory() {
		super(25);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, SequencedRecipe<?> recipe, IFocusGroup focuses, int x) {}

	@Override
	public void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int index) {
		PoseStack ms = graphics.pose();
		charger.offset = index;
		ms.pushPose();
		// 整体下移，与动力锯（AssemblyCutting）的机器位置对齐
		ms.translate(-5, 68, 0);
		ms.scale(.6f, .6f, .6f);
		charger.draw(graphics, getWidth() / 2, 0);
		ms.popPose();
	}

}
