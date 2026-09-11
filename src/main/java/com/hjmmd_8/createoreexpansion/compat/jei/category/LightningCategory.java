package com.hjmmd_8.createoreexpansion.compat.jei.category;

import com.hjmmd_8.createoreexpansion.content.lightning.LightningRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

public class LightningCategory extends ProcessingViaFanCategory.MultiOutput<LightningRecipe> {

	public LightningCategory(Info<LightningRecipe> info) {
		super(info);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, LightningRecipe recipe, IFocusGroup focuses) {
		// 多输入配方：每个 ingredient 一个输入槽（2 列向上堆叠），单输入配方与原来一致
		int index = 0;
		for (Ingredient ingredient : recipe.getIngredients()) {
			int col = index % 2;
			int row = index / 2;
			builder
				.addSlot(RecipeIngredientRole.INPUT, 21 + col * 19, 48 - row * 19)
				.setBackground(getRenderedSlot(), -1, -1)
				.addIngredients(ingredient);
			index++;
		}
		// 输出槽：沿用多输出布局
		var results = recipe.getRollableResults();
		int xOffsetAmount = 1 - Math.min(3, results.size());
		int i = 0;
		boolean excessive = results.size() > 9;
		for (var output : results) {
			int xOffset = (i % 3) * 19 + 9 * xOffsetAmount;
			int yOffset = (i / 3) * -19 + (excessive ? 8 : 0);
			builder
				.addSlot(RecipeIngredientRole.OUTPUT, 141 + xOffset, 48 + yOffset)
				.setBackground(getRenderedSlot(output), -1, -1)
				.addItemStack(output.getStack())
				.addRichTooltipCallback(addStochasticTooltip(output));
			i++;
		}
	}

	@Override
	protected void renderWidgets(GuiGraphics graphics, LightningRecipe recipe, double mouseX, double mouseY) {
		// 只保留避雷针下方阴影（右下），移除左上角风扇阴影
		int size = recipe.getRollableResultsAsItemStacks().size();
		int xOffsetAmount = 1 - Math.min(3, size);
		getBlockShadow().render(graphics, 65, 39);
		AllGuiTextures.JEI_LONG_ARROW.render(graphics, 7 * xOffsetAmount + 54, 51);
	}

	@Override
	public void draw(LightningRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX,
		double mouseY) {
		renderWidgets(graphics, recipe, mouseX, mouseY);

		PoseStack matrixStack = graphics.pose();
		matrixStack.pushPose();
		matrixStack.translate(56, 33, 0);
		matrixStack.mulPose(Axis.XP.rotationDegrees(-12.5f));
		matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));

		renderAttachedBlock(graphics);
		matrixStack.popPose();
	}

	@Override
	protected void renderAttachedBlock(GuiGraphics graphics) {
		GuiGameElement.of(com.hjmmd_8.createoreexpansion.common.AllBlocks.REINFORCED_LIGHTNING_ROD.getDefaultState())
			.scale(SCALE)
			.atLocal(0, 0, 2)
			.lighting(AnimatedKinetics.DEFAULT_LIGHTING)
			.render(graphics);
	}

}
