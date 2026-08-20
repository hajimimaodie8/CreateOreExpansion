package com.hjmmd_8.createoreexpansion.compat.jei;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllPartialModels;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.kinetics.crusher.AbstractCrushingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 高级角磨配方 JEI 分类（≥2 级轮可执行 Create 粉碎轮/石磨配方；动画显示钻石角磨轮）。
 */
@ParametersAreNonnullByDefault
public class AdvancedGrindingCategory extends CreateRecipeCategory<AbstractCrushingRecipe> {

	private final AnimatedPowerAngleGrinder grinder = new AnimatedPowerAngleGrinder(
		AllPartialModels.GRINDING_WHEELS.get(CreateOreExpansion.modLoc("diamond_grinding_wheel")));

	public AdvancedGrindingCategory(Info<AbstractCrushingRecipe> info) {
		super(info);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, AbstractCrushingRecipe recipe, IFocusGroup focuses) {
		builder
			.addSlot(RecipeIngredientRole.INPUT, 44, 5)
			.setBackground(getRenderedSlot(), -1, -1)
			.addIngredients(recipe.getIngredients().get(0));

		List<ProcessingOutput> results = recipe.getRollableResults();
		int i = 0;
		for (ProcessingOutput output : results) {
			int xOffset = i % 2 == 0 ? 0 : 19;
			int yOffset = (i / 2) * -19;
			builder
				.addSlot(RecipeIngredientRole.OUTPUT, 118 + xOffset, 48 + yOffset)
				.setBackground(getRenderedSlot(output), -1, -1)
				.addItemStack(output.getStack())
				.addRichTooltipCallback(addStochasticTooltip(output));
			i++;
		}
	}

	@Override
	public void draw(AbstractCrushingRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 70, 6);
		AllGuiTextures.JEI_SHADOW.render(graphics, 72 - 17, 42 + 13);

		grinder.draw(graphics, 72, 42);
	}

}
