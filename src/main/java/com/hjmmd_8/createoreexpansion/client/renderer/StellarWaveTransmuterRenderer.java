package com.hjmmd_8.createoreexpansion.client.renderer;

import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveTransmuterBlockEntity;
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
 * 星辉波变器渲染：在 FACING 的<b>反面（底部）</b>渲染传动轴（Create SHAFT_HALF），
 * 随转速旋转——与能量场控制器/应力充能器同款轴视觉（本机轴口仅此一面）。
 * 方块本体用 blockstate 静态模型（沿用能量场控制器同族模型），无需额外渲染。
 */
public class StellarWaveTransmuterRenderer extends KineticBlockEntityRenderer<StellarWaveTransmuterBlockEntity> {

	public StellarWaveTransmuterRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(StellarWaveTransmuterBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!(state.getBlock() instanceof DirectionalKineticBlock))
			return;
		Direction facing = state.getValue(DirectionalKineticBlock.FACING);

		SuperByteBuffer shaft = CachedBuffers.partialFacing(
			com.simibubi.create.AllPartialModels.SHAFT_HALF, state, facing.getOpposite());
		standardKineticRotationTransform(shaft, be, light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
	}
}
