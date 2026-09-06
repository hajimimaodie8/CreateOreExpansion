package com.hjmmd_8.createoreexpansion.compat.jei.subcategory;

import com.hjmmd_8.createoreexpansion.compat.jei.animation.AnimatedJadeCharger;
import com.hjmmd_8.createoreexpansion.compat.jei.animation.AnimatedSapphireCharger;
import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipe;
import com.hjmmd_8.createoreexpansion.content.wave.WaveLevels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 序列加工 JEI 子分类：充能步骤的机器动画。
 *
 * <p><b>按充能等级选机型动画</b>：1~3 级（低/高/伽马）= 翡翠应力充能器；
 * 4/5 级（超载/终极）= 蓝宝石应力充能器（只有蓝宝石机能产出 4/5 级波）。</p>
 *
 * <p>充能等级信息不常驻显示——由 Create 序列组装分类的悬停提示（getTooltipStrings）
 * 在鼠标悬停到本步骤（充能器）时显示，文案见
 * {@code ChargingRecipe.getDescriptionForAssembly()}（含各等级名称）。</p>
 */
public class ChargingAssemblySubCategory extends SequencedAssemblySubCategory {

	private final AnimatedJadeCharger jadeCharger = new AnimatedJadeCharger();
	private final AnimatedSapphireCharger sapphireCharger = new AnimatedSapphireCharger();

	public ChargingAssemblySubCategory() {
		super(25);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, SequencedRecipe<?> recipe, IFocusGroup focuses, int x) {}

	@Override
	public void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int index) {
		int level = recipe.getRecipe() instanceof ChargingRecipe charging ? charging.getLevel() : WaveLevels.GAMMA;
		AnimatedJadeCharger charger = level >= WaveLevels.EPSILON ? sapphireCharger : jadeCharger;
		charger.mode = level;
		charger.offset = index;
		PoseStack ms = graphics.pose();
		ms.pushPose();
		// 与部署步骤（机械手+置物台）完全一致的布局参数：置物台位置与手部物品使用时相同
		ms.translate(-7, 50, 0);
		ms.scale(.75f, .75f, .75f);
		charger.draw(graphics, getWidth() / 2, 0);
		ms.popPose();
	}

}
