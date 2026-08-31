package com.hjmmd_8.createoreexpansion.client.renderer.wave;

import com.hjmmd_8.createoreexpansion.common.AllPartialModels;
import com.hjmmd_8.createoreexpansion.content.wave.block.EnergyWaveRegulatorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/**
 * 能量调级器渲染：在方块本体（blockstate 机座模型）之上叠加齿轮 partial model。
 *
 * <p><b>关键修正</b>：`partialFacingVertical` 的朝向方向必须用<b>方块实际的 FACING</b>，
 * 而不是 {@code Direction.fromAxisAndDirection(axis, POSITIVE)}——后者在 FACING=DOWN
 * （轴 Y 负方向）时会取到 UP（正方向），模型被倒转 180°，再叠加
 * {@code standardKineticRotationTransform} 的中心旋转 → 出现"一正一歪"的双重渲染。</p>
 *
 * <p>直接传 FACING：模型姿态按放置方向对齐，旋转也按 FACING 轴，三向跟随、单一渲染。</p>
 */
public class EnergyWaveRegulatorRenderer extends KineticBlockEntityRenderer<EnergyWaveRegulatorBlockEntity> {

	/** 齿轮用 cutoutMipped 渲染层（与本体一致，静态复用避免每帧取类型） */
	private static final RenderType GEAR_RENDER_TYPE = RenderType.cutoutMipped();

	public EnergyWaveRegulatorRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(EnergyWaveRegulatorBlockEntity be, float partialTicks, PoseStack ms,
							  MultiBufferSource buffer, int light, int overlay) {
		// 不调用 super.renderSafe：其会在 Flywheel 可视化激活时直接 return（跳过旧式渲染），
		// 我们没有注册 Flywheel visual，因此直接绘制齿轮。

		// 用方块实际 FACING（不是从轴重算方向）：姿态对齐 + 旋转轴都跟随放置方向
		Direction facing = be.getBlockState().getValue(DirectionalKineticBlock.FACING);
		SuperByteBuffer cogwheel = CachedBuffers.partialFacingVertical(AllPartialModels.WAVE_REGULATOR_COGWHEEL,
			be.getBlockState(), facing);

		if (be.isSpeedRequirementFulfilled()) {
			// 转速达标（≥ FAST，默认 100 RPM）：齿轮随应力旋转
			standardKineticRotationTransform(cogwheel, be, light)
				.renderInto(ms, buffer.getBuffer(GEAR_RENDER_TYPE));
		} else {
			// 转速不足（< 100 RPM）：齿轮静止不转（保持光照，停在默认姿态）
			cogwheel.light(light)
				.renderInto(ms, buffer.getBuffer(GEAR_RENDER_TYPE));
		}
	}
}
