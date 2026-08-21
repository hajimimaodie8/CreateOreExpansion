package com.hjmmd_8.createoreexpansion.compat.jei;

import javax.annotation.ParametersAreNonnullByDefault;

import java.util.List;

import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipe;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.createmod.catnip.animation.AnimationTickHolder;
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

	/** 阴影图标左上角（与冲压分类一致） */
	private static final int SHADOW_ICON_X = 61;
	private static final int SHADOW_ICON_Y = 41;
	/** 粒子飞行终点：阴影图标中心（右移 12px 对齐发射头） */
	private static final int SHADOW_X = SHADOW_ICON_X + 9 + 12;
	private static final int SHADOW_Y = SHADOW_ICON_Y + 6;
	/** 粒子飞行起点：充能器发射头（屏幕下方，机器底部；右移 12px 与终点保持同向） */
	private static final int WAVE_START_X = 70 + 12;
	private static final int WAVE_START_Y = 32;

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
		// 槽位整体右移 8px、下移 2px（修正 JEI 内物品显示偏左上）
		List<ProcessingOutput> results = recipe.getRollableResults();
		int i = 0;
		for (ProcessingOutput output : results) {
			int xOffset = i % 2 == 0 ? 0 : 19;
			int yOffset = (i / 2) * -19;
			builder
				.addSlot(RecipeIngredientRole.OUTPUT, 118 + xOffset + 8, 48 + yOffset + 2)
				.setBackground(getRenderedSlot(output), -1, -1)
				.addItemStack(output.getStack())
				.addRichTooltipCallback(addStochasticTooltip(output));
			i++;
		}
	}

	@Override
	public void draw(ChargingRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, SHADOW_ICON_X, SHADOW_ICON_Y);
		AllGuiTextures.JEI_LONG_ARROW.render(graphics, 52, 54);

		charger.draw(graphics, getBackground().getWidth() / 2 - 17, 22);

		drawWave(graphics, recipe);
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

	/** 发射粒子：蓄力满后从发射头射向阴影，遇到阴影向外绽放并消失（循环） */
	private void drawWave(GuiGraphics graphics, ChargingRecipe recipe) {
		// 与充能器动画同周期（48）：蓄力 0~36、弹出 36~42、粒子飞行 42~46、绽放 46~48
		int cycle = (int) (AnimationTickHolder.getRenderTime() - charger.offset * 8) % 48;
		if (cycle < 42)
			return;

		int color = getWaveColor(recipe);

		if (cycle < 46) {
			// 飞行：从发射头底部斜射向阴影中心
			float t = (cycle - 42) / 4f; // 0 → 1
			int x = WAVE_START_X + (int) ((SHADOW_X - WAVE_START_X) * t);
			int y = WAVE_START_Y + (int) ((SHADOW_Y - WAVE_START_Y) * t);
			graphics.fill(x - 1, y - 1, x + 1, y + 1, color);
			return;
		}

		// 绽放：向外扩散的粒子环（随 cycle 扩散并淡出）
		float burst = (cycle - 46) / 2f; // 0 → 1
		int radius = (int) (burst * 6);
		int alpha = (int) (255 * (1 - burst));
		int col = (alpha << 24) | (color & 0xFFFFFF);
		for (int i = 0; i < 8; i++) {
			double a = i / 8.0 * Math.PI * 2;
			int px = SHADOW_X + (int) (Math.cos(a) * radius);
			int py = SHADOW_Y + (int) (Math.sin(a) * radius);
			graphics.fill(px, py, px + 1, py + 1, col);
		}
	}

	/** 能量波颜色（按配方等级）：低=黄、高=绿、伽马=蓝（与充能器蓄力态配色一致） */
	private int getWaveColor(ChargingRecipe recipe) {
		return switch (recipe.getLevel()) {
			case 2 -> 0x55FF55;
			case 3 -> 0x5555FF;
			default -> 0xFFFF55;
		};
	}

}
