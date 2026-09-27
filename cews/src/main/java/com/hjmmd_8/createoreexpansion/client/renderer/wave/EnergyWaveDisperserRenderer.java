package com.hjmmd_8.createoreexpansion.client.renderer.wave;

import com.hjmmd_8.createoreexpansion.common.AllPartialModels;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveDisperserBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;

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
 * 能量波差器渲染：在方块本体（blockstate 基础模型）之上，按 4 侧面开闭状态
 * 在顶/底灯盘叠加对应的灯位（{@code light.png} 剪切出的 2×2 灯位）。
 *
 * <p><b>灯位与状态对应</b>：{@code open_north}/{@code open_east}/{@code open_south}/{@code open_west}
 * 为 true 时渲染对应方向的灯位 partial model（北/东/南/西），false 不渲染（灭灯）。
 * 灯位模型每个含 up/down 两面（顶/底灯盘对称），由 {@code partialFacingVertical} 按
 * 方块实际 FACING 旋转（与 blockstate 同款旋转逻辑，三向对齐）。</p>
 *
 * <p>渲染层用 cutoutMipped 与本体一致；不调用任何 kinetic 旋转（差波器无应力）。</p>
 */
public class EnergyWaveDisperserRenderer implements BlockEntityRenderer<EnergyWaveDisperserBlockEntity> {

	private static final RenderType LAMP_RENDER_TYPE = RenderType.cutoutMipped();

	public EnergyWaveDisperserRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(EnergyWaveDisperserBlockEntity be, float partialTicks, PoseStack ms,
					   MultiBufferSource buffer, int light, int overlay) {
		var state = be.getBlockState();
		Direction facing = state.getValue(DirectionalKineticBlock.FACING);

		renderIfOpen(be, state, facing, AllPartialModels.DISPERSER_LAMP_NORTH, EnergyWaveDisperserBlock.NORTH,
			ms, buffer, light);
		renderIfOpen(be, state, facing, AllPartialModels.DISPERSER_LAMP_EAST, EnergyWaveDisperserBlock.EAST,
			ms, buffer, light);
		renderIfOpen(be, state, facing, AllPartialModels.DISPERSER_LAMP_SOUTH, EnergyWaveDisperserBlock.SOUTH,
			ms, buffer, light);
		renderIfOpen(be, state, facing, AllPartialModels.DISPERSER_LAMP_WEST, EnergyWaveDisperserBlock.WEST,
			ms, buffer, light);
	}

	private static void renderIfOpen(EnergyWaveDisperserBlockEntity be, BlockState state, Direction facing,
									 dev.engine_room.flywheel.lib.model.baked.PartialModel model,
									 net.minecraft.world.level.block.state.properties.BooleanProperty property,
									 PoseStack ms, MultiBufferSource buffer, int light) {
		if (!state.getValue(property))
			return; // 灭灯：不渲染灯位
		SuperByteBuffer lamp = CachedBuffers.partialFacingVertical(model, be.getBlockState(), facing);
		// 全亮光照（天空 15 + 方块 15）：指示灯恒定发光，不随环境变暗
		lamp.light(LightTexture.pack(15, 15))
			.renderInto(ms, buffer.getBuffer(LAMP_RENDER_TYPE));
	}
}
