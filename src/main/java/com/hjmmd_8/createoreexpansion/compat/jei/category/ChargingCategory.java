package com.hjmmd_8.createoreexpansion.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import java.util.List;

import com.hjmmd_8.createoreexpansion.compat.jei.animation.AnimatedJadeCharger;
import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipe;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 充能配方 JEI 分类（仿冲压配方 PressingCategory）。
 *
 * <p>低/高/伽马三个充能等级共用一个分类，等级是配方自带的 {@code level} 字段，
 * 在输入槽上方以等级徽章区分（低=黄、高=绿、伽马=蓝，与能量波颜色一致）。</p>
 *
 * <p>布局：左上输入槽 = 能量工具/加工物品，充能器动画在中间（发射头朝下），
 * 蓄力满后粒子从发射头射向下方阴影处绽放消失（模拟能量波命中物品）。
 * 普通充能加工配方显示输出槽（如铁锭→金锭）；工具充能配方无产物。</p>
 */
@ParametersAreNonnullByDefault
public class ChargingCategory extends CreateRecipeCategory<ChargingRecipe> {

	private final AnimatedJadeCharger charger = new AnimatedJadeCharger();

	public ChargingCategory(Info<ChargingRecipe> info) {
		super(info);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ChargingRecipe recipe, IFocusGroup focuses) {
		builder
			.addSlot(RecipeIngredientRole.INPUT, 27, 51)
			.setBackground(getRenderedSlot(), -1, -1)
			.addIngredients(recipe.getIngredients().get(0));

		// 输出槽：普通充能加工配方有产物（如铁锭→金锭）；工具充能配方无产物（空槽不显示）。
		// 槽位整体右移 17px、下移 2px（基础右移 8px + 半个工具框宽度 9px）
		List<ProcessingOutput> results = recipe.getRollableResults();
		int i = 0;
		for (ProcessingOutput output : results) {
			int xOffset = i % 2 == 0 ? 0 : 19;
			int yOffset = (i / 2) * -19;
			builder
				.addSlot(RecipeIngredientRole.OUTPUT, 118 + xOffset + 17, 48 + yOffset + 2)
				.setBackground(getRenderedSlot(output), -1, -1)
				.addItemStack(output.getStack())
				.addRichTooltipCallback(addStochasticTooltip(output));
			i++;
		}
	}

	@Override
	public void draw(ChargingRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
		// 严格仿照注液分类（SpoutCategory）：阴影在输入槽下方（置物台位置），向下箭头连接输入→输出
		AllGuiTextures.JEI_SHADOW.render(graphics, 62, 57);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 126, 29);

		// 充能器方块状态随配方等级切换（1=低/2=高/3=伽马），与等级徽章/能量波颜色一致
		charger.mode = recipe.getLevel();
		charger.draw(graphics, getBackground().getWidth() / 2 - 13, 22);

		drawLevelBadge(graphics, recipe);
	}

	/** 等级徽章：输入槽上方画等级色块 + 等级名称（低/高/伽马），颜色与能量波一致 */
	private void drawLevelBadge(GuiGraphics graphics, ChargingRecipe recipe) {
		int level = recipe.getLevel();
		int color = 0xFF000000 | getWaveColor(recipe);
		Component label = switch (level) {
			case 2 -> Component.translatable("createoreexpansion.jei.charging.level.2");
			case 3 -> Component.translatable("createoreexpansion.jei.charging.level.3");
			default -> Component.translatable("createoreexpansion.jei.charging.level.1");
		};

		// 输入槽位于 (27, 51)：徽章色块画在其正上方
		int badgeX = 27;
		int badgeY = 51 - 13;
		graphics.fill(badgeX, badgeY, badgeX + 5, badgeY + 5, color);

		Font font = Minecraft.getInstance().font;
		graphics.drawString(font, label, badgeX + 9, badgeY - 2, 0xFFFFFFFF, true);
	}

	/** 发射粒子已移除：严格仿照注液分类，不画能量波粒子动画 */

	/** 能量波颜色（按配方等级）：低=黄、高=绿、伽马=蓝（与充能器蓄力态配色一致） */
	private int getWaveColor(ChargingRecipe recipe) {
		return switch (recipe.getLevel()) {
			case 2 -> 0x55FF55;
			case 3 -> 0x5555FF;
			default -> 0xFFFF55;
		};
	}

}
