package com.hjmmd_8.createoreexpansion.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllPartialModels;
import com.hjmmd_8.createoreexpansion.compat.jei.animation.AnimatedPowerAngleGrinder;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.DismantlingRecipe;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 拆磨配方 JEI 分类（三级角磨轮专属；动画显示蓝宝石角磨轮）。
 */
@ParametersAreNonnullByDefault
public class DismantlingCategory extends CreateRecipeCategory<DismantlingRecipe> {

	private final AnimatedPowerAngleGrinder grinder = new AnimatedPowerAngleGrinder(
		AllPartialModels.GRINDING_WHEELS.get(CreateOreExpansion.modLoc("sapphire_grinding_wheel")));

	public DismantlingCategory(Info<DismantlingRecipe> info) {
		super(info);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, DismantlingRecipe recipe, IFocusGroup focuses) {
		builder
			.addSlot(RecipeIngredientRole.INPUT, 44, 5)
			.setBackground(getRenderedSlot(), -1, -1)
			.addItemStack(recipe.getItem());

		// 输出数量随装备剩余耐久变化：用概率槽样式提示，悬停显示说明
		builder
			.addSlot(RecipeIngredientRole.OUTPUT, 118, 48)
			.setBackground(getRenderedSlot(0.5f), -1, -1)
			.addItemStack(recipe.getResult())
			.addRichTooltipCallback((view, tooltip) -> tooltip.add(
				Component.translatable("createoreexpansion.recipe.dismantling.output")
					.withStyle(ChatFormatting.GRAY)));
	}

	@Override
	public void draw(DismantlingRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 70, 6);
		AllGuiTextures.JEI_SHADOW.render(graphics, 72 - 17, 42 + 13);

		grinder.draw(graphics, 72, 42);
	}

}
