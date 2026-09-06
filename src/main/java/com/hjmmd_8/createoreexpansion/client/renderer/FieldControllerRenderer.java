package com.hjmmd_8.createoreexpansion.client.renderer;

import com.hjmmd_8.createoreexpansion.content.machine.energyfieldcontroller.EnergyFieldControllerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 能量场控制器渲染：在 FACING 的<b>反面（底部）</b>渲染传动轴（Create SHAFT_HALF），
 * 随转速旋转——与应力充能器同款轴视觉。方块本体用 blockstate 静态模型，无需额外渲染。
 */
public class FieldControllerRenderer extends KineticBlockEntityRenderer<EnergyFieldControllerBlockEntity> {

	public FieldControllerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(EnergyFieldControllerBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!(state.getBlock() instanceof DirectionalKineticBlock))
			return;
		Direction facing = state.getValue(DirectionalKineticBlock.FACING);

		// 传动轴：半轴渲染在 FACING 对面（底端），与充能器同轴线另一端
		SuperByteBuffer shaft = CachedBuffers.partialFacing(
			com.simibubi.create.AllPartialModels.SHAFT_HALF, state, facing.getOpposite());
		standardKineticRotationTransform(shaft, be, light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
	}
}
