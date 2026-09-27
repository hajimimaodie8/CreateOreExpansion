package com.hjmmd_8.createoreexpansion.client.renderer.wave;

import com.hjmmd_8.createoreexpansion.common.AllPartialModels;
import com.hjmmd_8.createoreexpansion.content.wave.block.OctaEnergyWaveDifferencerBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.OctaEnergyWaveDifferencerBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.math.Axis;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 八面能量波差器指示灯渲染：8 个方向（N/NE/E/SE/S/SW/W/NW）各自有一组
 * 顶片+底片灯模型（{@code octa_energy_wave_differencer_lamp_{dir}.json}，
 * 贴 {@code special_light}，注册见 {@link AllPartialModels#OCTA_ENERGY_WAVE_DIFFERENCER_LAMPS}，
 * 必须在烘焙前注册，否则开口渲染会整块紫黑缺失方块）。某方向开口 open 时该方向灯自发光亮起。
 *
 * <p>灯模型按<b>本地(站姿)坐标</b>贴在顶面 y16 与底面 y0 外侧；对 axis=X/Z 躺姿，
 * 渲染前把整组灯绕方块中心施加与 blockstate 相同的 rotationX/rotationY，使灯随方块姿态
 * 贴到朝墙的顶/底面上（axis=Y 不转）。</p>
 */
public class OctaEnergyWaveDifferencerRenderer implements BlockEntityRenderer<OctaEnergyWaveDifferencerBlockEntity> {

	private static final RenderType RENDER_TYPE = RenderType.cutoutMipped();

	private static final String[] DIRS = { "n", "ne", "e", "se", "s", "sw", "w", "nw" };

	public OctaEnergyWaveDifferencerRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(OctaEnergyWaveDifferencerBlockEntity be, float partialTicks, PoseStack ms,
					   MultiBufferSource buffer, int light, int overlay) {
		BlockState state = be.getBlockState();
		Direction.Axis axis = state.getValue(OctaEnergyWaveDifferencerBlock.AXIS);
		// 姿态旋转（与 blockstate 相同源；绕方块中心 0.5）：
		// axis=X: rotationX 90 后 rotationY 90；axis=Z: rotationX 90；axis=Y: 不转
		ms.pushPose();
		if (axis == Direction.Axis.X) {
			// 与 blockstate axis=X(x:90,y:90) 同源 = MC BlockModelRotation:
			// rotateYXZ(-y,-x,0) 先绕 X 转 -90 再绕 Y 转 -90（顶→东）。
			// PoseStack 右乘，后 mul 的靠右先作用于顶点：先 mul YP 再 mul XP。
			ms.translate(0.5, 0.5, 0.5);
			ms.mulPose(Axis.YP.rotationDegrees(-90));
			ms.mulPose(Axis.XP.rotationDegrees(-90));
			ms.translate(-0.5, -0.5, -0.5);
		} else if (axis == Direction.Axis.Z) {
			// 与 blockstate axis=Z(x:90,y:0) 同源：绕 X 转 -90（顶→北）
			ms.translate(0.5, 0.5, 0.5);
			ms.mulPose(Axis.XP.rotationDegrees(-90));
			ms.translate(-0.5, -0.5, -0.5);
		}
		int idx = 0;
		for (String dir : DIRS) {
			if (isOpen(state, idx))
				renderLamp(be, dir, ms, buffer);
			idx++;
		}
		ms.popPose();
	}

	private static boolean isOpen(BlockState state, int idx) {
		return switch (idx) {
			case 0 -> state.getValue(OctaEnergyWaveDifferencerBlock.NORTH);
			case 1 -> state.getValue(OctaEnergyWaveDifferencerBlock.NORTH_EAST);
			case 2 -> state.getValue(OctaEnergyWaveDifferencerBlock.EAST);
			case 3 -> state.getValue(OctaEnergyWaveDifferencerBlock.SOUTH_EAST);
			case 4 -> state.getValue(OctaEnergyWaveDifferencerBlock.SOUTH);
			case 5 -> state.getValue(OctaEnergyWaveDifferencerBlock.SOUTH_WEST);
			case 6 -> state.getValue(OctaEnergyWaveDifferencerBlock.WEST);
			default -> state.getValue(OctaEnergyWaveDifferencerBlock.NORTH_WEST);
		};
	}

	/** 灯位自发光渲染（天空+方块光照均拉满，恒定明亮）。 */
	private static void renderLamp(OctaEnergyWaveDifferencerBlockEntity be, String dir, PoseStack ms,
								   MultiBufferSource buffer) {
		SuperByteBuffer lamp = CachedBuffers.partial(AllPartialModels.OCTA_ENERGY_WAVE_DIFFERENCER_LAMPS.get(dir),
			be.getBlockState());
		lamp.light(LightTexture.pack(15, 15))
			.renderInto(ms, buffer.getBuffer(RENDER_TYPE));
	}
}
