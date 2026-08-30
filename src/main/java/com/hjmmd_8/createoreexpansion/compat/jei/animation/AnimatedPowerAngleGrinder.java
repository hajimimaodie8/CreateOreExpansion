package com.hjmmd_8.createoreexpansion.compat.jei.animation;

import com.hjmmd_8.createoreexpansion.common.AllBlocks;
import com.hjmmd_8.createoreexpansion.common.AllPartialModels;
import com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * JEI 分类动画：装有指定角磨轮的动力角磨床（轮随应力旋转，仿动力锯 JEI 分类）。
 *
 * <p>机器用 facing=SOUTH（模型不旋转）：齿轮箱背板与 GRINDER_AXIS 短轴都在模型 z+ 侧，
 * 与轮轴（Z）一致；轮绕饼中心垂直轴 Z 自转，主轴槽随轮同步旋转。</p>
 */
public class AnimatedPowerAngleGrinder extends AnimatedKinetics {

	private final PartialModel wheelModel;

	public AnimatedPowerAngleGrinder(PartialModel wheelModel) {
		this.wheelModel = wheelModel;
	}

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack matrixStack = graphics.pose();
		matrixStack.pushPose();
		matrixStack.translate(xOffset, yOffset, 0);
		matrixStack.translate(0, 0, 200);
		matrixStack.translate(2, 22, 0);
		matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
		matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f + 90));
		int scale = 25;

		BlockState machine = AllBlocks.POWER_ANGLE_GRINDER.getDefaultState()
			.setValue(PowerAngleGrinderBlock.HORIZONTAL_FACING, Direction.SOUTH);

		// 完整的机器（关盖）
		blockElement(machine)
			.rotateBlock(0, 0, 0)
			.scale(scale)
			.render(graphics);

		// 主轴槽（角磨轮安装处，随轮同步旋转）
		blockElement(AllPartialModels.GRINDING_SPINDLE)
			.rotateBlock(0, 0, -getCurrentAngle())
			.scale(scale)
			.render(graphics);

		// 角磨轮（装在主轴上；绕饼中心垂直轴 Z 自转，像磨盘一样）
		blockElement(wheelModel)
			.rotateBlock(0, 0, -getCurrentAngle())
			.scale(scale)
			.render(graphics);

		matrixStack.popPose();
	}

}
