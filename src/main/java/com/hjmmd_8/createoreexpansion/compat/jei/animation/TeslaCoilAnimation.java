package com.hjmmd_8.createoreexpansion.compat.jei.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrh0.createaddition.blocks.tesla_coil.TeslaCoilBlock;
import com.mrh0.createaddition.index.CABlocks;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * JEI 动画：CC&amp;A 特斯拉线圈（仅方块本体，不画底部阴影）。
 *
 * <p>仿 CC&amp;A 的 {@code AnimatedTeslaCoil} 绘制逻辑（FACING=DOWN、POWERED=true、
 * 绕 Y 22.5°、缩放 22），但<b>去掉</b> CC&amp;A 版本里在 (0, 18) 处渲染的
 * {@code JEI_SHADOW} 底部阴影——序列装配 JEI 中该阴影与置物台动画叠在一起显得脏。</p>
 */
public class TeslaCoilAnimation extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 0);

		// 线圈本体整体位移（-2, 18）：与 CC&A 原动画一致，仅去掉其阴影（阴影在 (0,18) 处绘制）
		ms.translate(-2, 18, 0);

		// 线圈本体：顶部朝下、通电，绕 Y 轴 22.5° 旋转（与 CC&A 原动画一致，无阴影）
		BlockState coil = CABlocks.TESLA_COIL.getDefaultState()
			.setValue(TeslaCoilBlock.FACING, Direction.DOWN)
			.setValue(TeslaCoilBlock.POWERED, true);
		GuiGameElement.of(coil)
			.rotateBlock(22.5, 22.5, 0)
			.scale(22)
			.render(graphics);

		ms.popPose();
	}
}
