package com.hjmmd_8.createoreexpansion.client.renderer;

import java.util.Random;

import com.hjmmd_8.createoreexpansion.common.AllPartialModels;
import com.hjmmd_8.createoreexpansion.content.grinding.block.PowerAngleGrinderBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.logistics.depot.DepotRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 动力角磨床渲染：在方块本体（blockstate 模型）之上叠加
 * 传动轴（短轴模型，绕旋转轴旋转）与角磨轮槽（随轴同步旋转），
 * 以及加工物品（仿置物台：未加工优先，堆叠效果参照 Create Depot）。
 */
public class GrinderRenderer extends KineticBlockEntityRenderer<PowerAngleGrinderBlockEntity> {

	public GrinderRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(PowerAngleGrinderBlockEntity be, float partialTicks, PoseStack ms,
							  MultiBufferSource buffer, int light, int overlay) {
		// 不调用 super.renderSafe：其会在 Flywheel 可视化激活时直接 return（跳过旧式渲染），
		// 我们没有注册 Flywheel visual，因此直接绘制传动轴与槽。

		// 传动轴：模型局部轴在 z+，应力从齿轮箱背板侧（FACING 方向）接入——
		// partialFacing 按接入方向对齐，使渲染轴与传动轴位置一致
		Direction facing = be.getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
		SuperByteBuffer axis = CachedBuffers.partialFacing(AllPartialModels.GRINDER_AXIS, be.getBlockState(), facing);
		standardKineticRotationTransform(axis, be, light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

		// 角磨轮槽：同样按接入方向对齐，随传动轴同步旋转
		SuperByteBuffer spindle = CachedBuffers.partialFacing(AllPartialModels.GRINDING_SPINDLE, be.getBlockState(), facing);
		standardKineticRotationTransform(spindle, be, light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

		// 已安装的角磨轮：始终渲染（开盖可见；合盖后仍在机器内部随轴旋转，供加工动画使用）
		ResourceLocation wheelId = be.getWheel();
		PartialModel wheelModel = wheelId != null ? AllPartialModels.GRINDING_WHEELS.get(wheelId) : null;
		if (wheelModel != null) {
			SuperByteBuffer wheel = CachedBuffers.partialFacing(wheelModel, be.getBlockState(), facing);
			standardKineticRotationTransform(wheel, be, light)
				.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
		}

		// 加工物品（仿置物台渲染）：未加工优先；堆叠效果（1 个/多个/一组）参照 Create Depot
		ItemStack renderStack = be.getRenderedItem();
		if (!renderStack.isEmpty()) {
			Vec3 n = Vec3.atLowerCornerOf(facing.getNormal());
			ms.pushPose();
			// 位置：机器中心偏盖子侧（-FACING 0.5 格）+ 沿传动轴方向（+FACING）位移 3px（0.1875 格），
			// y 底端 4px（0.25 格）：Depot.renderItem 对普通物品（非方块）内部有 -3/16 向下偏移 + 平放，
			// 若底端只设 2px 会被 Base（y 0~2px）完全遮挡不显示，故提到 Base 顶面上方 2px
			ms.translate(.5f - (float) n.x * .3125f, .25f, .5f - (float) n.z * .3125f);
			// 整体再缩 0.7（叠加 Depot 的 0.5 → 总 0.35），防止物品略大穿模
			ms.scale(.7f, .7f, .7f);
			Random random = new Random(0);
			DepotRenderer.renderItem(ms, buffer, light, overlay, renderStack,
				(int) (360 * random.nextFloat()), random, VecHelper.getCenterOf(be.getBlockPos()), false);
			ms.popPose();
		}

		// 配方过滤器槽（含放置的过滤物品）
		FilteringRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay);
	}
}
