package com.hjmmd_8.createoreexpansion.client.renderer;

import com.hjmmd_8.createoreexpansion.common.AllPartialModels;
import com.hjmmd_8.createoreexpansion.content.charger.block.AbstractCreateChargerBlock;
import com.hjmmd_8.createoreexpansion.content.charger.block.AbstractCreateChargerBlockEntity;
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
 * 应力充能器渲染（翡翠/雷鸣共用）：传动轴（沿 FACING）+ 发射头（shutter，顶面朝 FACING，与传动轴同一轴线）。
 * 面向抽象基类 {@link AbstractCreateChargerBlockEntity}，子类方块实体无需各自渲染器。
 */
public class CreateChargerRenderer extends KineticBlockEntityRenderer<AbstractCreateChargerBlockEntity> {

	public CreateChargerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(AbstractCreateChargerBlockEntity be, float partialTicks, PoseStack ms,
							  MultiBufferSource buffer, int light, int overlay) {
		BlockState state = be.getBlockState();
		Direction facing = state.getValue(DirectionalKineticBlock.FACING);

		// 传动轴：半轴（SHAFT_HALF），渲染在 FACING 对面（底端），与发射头同一轴线的另一端
		SuperByteBuffer shaft = CachedBuffers.partialFacing(
			com.simibubi.create.AllPartialModels.SHAFT_HALF, state, facing.getOpposite());
		standardKineticRotationTransform(shaft, be, light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

		// 发射头（shutter）：模型顶面（+Y）朝 FACING——与传动轴同一轴线。
		// partialFacingVertical = rotateY(horizontal) + rotateX(vertical+90)，Create 专门用于顶面朝向部件
		// 蓄力动画：沿 FACING 平移（收缩为负、弹出为正）
		float offset = be.getShutterOffset(partialTicks);
		ms.pushPose();
		if (offset != 0) {
			ms.translate(facing.getNormal().getX() * offset, facing.getNormal().getY() * offset,
				facing.getNormal().getZ() * offset);
		}
		SuperByteBuffer shutter = CachedBuffers.partialFacingVertical(AllPartialModels.CHARGER_SHUTTER, state, facing)
			.light(light);
		shutter.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
		ms.popPose();
	}
}
