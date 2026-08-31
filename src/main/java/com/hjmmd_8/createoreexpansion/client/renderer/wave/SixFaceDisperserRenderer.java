package com.hjmmd_8.createoreexpansion.client.renderer.wave;

import com.hjmmd_8.createoreexpansion.common.AllPartialModels;
import com.hjmmd_8.createoreexpansion.content.wave.block.SixFaceDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.SixFaceDisperserBlockEntity;

import com.mojang.blaze3d.vertex.PoseStack;

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
 * 六面能量波差器指示灯渲染：每个面上按相邻 4 面的开闭状态渲染指示灯——
 * 该面的上/右/下/左相邻面 open 时，对应方向的灯位亮起（贴 light.png 光点）。
 *
 * <p>灯位模型（six_face_lamp_{face}_{pos}）已按世界方向放置在该面外侧，直接用
 * 不旋转的 {@code partial} 渲染；无 FACING，面坐标 = 世界坐标。</p>
 */
public class SixFaceDisperserRenderer implements BlockEntityRenderer<SixFaceDisperserBlockEntity> {

	private static final RenderType RENDER_TYPE = RenderType.cutoutMipped();

	public SixFaceDisperserRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(SixFaceDisperserBlockEntity be, float partialTicks, PoseStack ms,
					   MultiBufferSource buffer, int light, int overlay) {
		BlockState state = be.getBlockState();
		for (Direction face : Direction.values()) {
			Direction up = SixFaceDisperserBlock.upOf(face);
			Direction right = SixFaceDisperserBlock.rightOf(face, up);
			Direction down = up.getOpposite();
			Direction left = right.getOpposite();
			String faceKey = face.getName();
			renderLamp(be, faceKey, "up", up, state, ms, buffer, light);
			renderLamp(be, faceKey, "down", down, state, ms, buffer, light);
			renderLamp(be, faceKey, "left", left, state, ms, buffer, light);
			renderLamp(be, faceKey, "right", right, state, ms, buffer, light);
		}
	}

	/** 相邻面 open 时渲染对应方向灯位（自发光：光照拉满，不受周围方块环境光影响）。 */
	private static void renderLamp(SixFaceDisperserBlockEntity be, String faceKey, String posKey,
								   Direction neighbor, BlockState state,
								   PoseStack ms, MultiBufferSource buffer, int light) {
		if (!SixFaceDisperserBlock.isOpen(state, neighbor))
			return; // 相邻面关闭：灯灭
		SuperByteBuffer lamp = CachedBuffers.partial(AllPartialModels.SIX_FACE_LAMPS.get(faceKey + "_" + posKey),
			be.getBlockState());
		// 全亮光照（天空 15 + 方块 15）：指示灯恒定发光，不随环境变暗
		lamp.light(LightTexture.pack(15, 15))
			.renderInto(ms, buffer.getBuffer(RENDER_TYPE));
	}
}