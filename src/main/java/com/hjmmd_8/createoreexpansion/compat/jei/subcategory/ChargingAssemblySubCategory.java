package com.hjmmd_8.createoreexpansion.compat.jei.subcategory;

import com.hjmmd_8.createoreexpansion.compat.jei.animation.AnimatedJadeCharger;
import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 序列加工 JEI 子分类：充能步骤的机器动画（翡翠应力充能器）。
 *
 * <p>充能等级信息不常驻显示——由 Create 序列组装分类的悬停提示（getTooltipStrings）
 * 在鼠标悬停到本步骤（充能器）时显示，文案见
 * {@code ChargingRecipe.getDescriptionForAssembly()}（含低/高/伽马等级）。</p>
 *
 * <p>机器动画的方块状态按配方等级切换（MODE = 配方 level）：
 * 低能量充能=1（黄）、高能量充能=2（绿）、伽马能量充能=3（蓝），
 * 加工方式直接体现在充能器方块状态上。</p>
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
		// 按配方充能等级切换方块状态（1=低/2=高/3=伽马），非充能配方兜底为 3
		if (recipe.getRecipe() instanceof ChargingRecipe charging)
			charger.mode = charging.getLevel();
		else
			charger.mode = 3;
		ms.pushPose();
		// 与部署步骤（机械手+置物台）完全一致的布局参数：置物台位置与手部物品使用时相同
		ms.translate(-7, 50, 0);
		ms.scale(.75f, .75f, .75f);
		charger.draw(graphics, getWidth() / 2, 0);
		ms.popPose();
	}

}
