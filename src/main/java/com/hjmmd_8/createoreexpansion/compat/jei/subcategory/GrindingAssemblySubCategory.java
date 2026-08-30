package com.hjmmd_8.createoreexpansion.compat.jei.subcategory;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllPartialModels;
import com.hjmmd_8.createoreexpansion.compat.jei.animation.AnimatedPowerAngleGrinder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 序列加工 JEI 子分类：角磨步骤的机器动画（装有铁角磨轮的动力角磨床）。
 */
public class GrindingAssemblySubCategory extends SequencedAssemblySubCategory {

	private final AnimatedPowerAngleGrinder grinder = new AnimatedPowerAngleGrinder(
		AllPartialModels.GRINDING_WHEELS.get(CreateOreExpansion.modLoc("iron_grinding_wheel")));

	public GrindingAssemblySubCategory() {
		super(25);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, SequencedRecipe<?> recipe, IFocusGroup focuses, int x) {}

	@Override
	public void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int index) {
		PoseStack ms = graphics.pose();
		grinder.offset = index;
		ms.pushPose();
		// 整体下移，与动力锯（AssemblyCutting）的机器位置对齐（y=71，比之前下移 3px）
		ms.translate(-5, 71, 0);
		ms.scale(.6f, .6f, .6f);
		grinder.draw(graphics, getWidth() / 2, 0);
		ms.popPose();
	}

}
