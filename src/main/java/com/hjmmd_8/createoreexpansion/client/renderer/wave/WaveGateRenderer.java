package com.hjmmd_8.createoreexpansion.client.renderer.wave;

import com.hjmmd_8.createoreexpansion.common.AllPartialModels;
import com.hjmmd_8.createoreexpansion.content.wave.block.AbstractWaveGateBlock;
import com.hjmmd_8.createoreexpansion.content.wave.block.AbstractWaveGateBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/**
 * 能量波闸渲染（能量调级器 / 波速调节器共用）：在方块本体（blockstate 机座模型）之上叠加
 * <ul>
 *   <li><b>齿轮</b>：随应力绕 FACING 轴旋转（转速达标转、不足静止）；</li>
 *   <li><b>侧面指示灯</b>：4 个侧面（齿轮面）各带上下两个灯位——
 *       顶面板 open + 转速达标 → 各侧面上方灯亮；底面板 open + 转速达标 → 各侧面下方灯亮
 *       （自发光，深夜里同样明亮）。</li>
 * </ul>
 *
 * <p><b>顶/底接收面板 open/close 贴图</b>由 blockstate 变体模型整面切换
 * （4 个变体模型，无需渲染器叠加）。</p>
 *
 * <p><b>关键修正</b>：`partialFacingVertical` 的朝向方向必须用<b>方块实际的 FACING</b>，
 * 而不是 {@code Direction.fromAxisAndDirection(axis, POSITIVE)}——后者在 FACING=DOWN
 * （轴 Y 负方向）时会取到 UP（正方向），模型被倒转 180°，再叠加
 * {@code standardKineticRotationTransform} 的中心旋转 → 出现"一正一歪"的双重渲染。</p>
 */
public class WaveGateRenderer<T extends AbstractWaveGateBlockEntity>
	extends KineticBlockEntityRenderer<T> {

	/** 齿轮与指示灯共用 cutoutMipped 渲染层（与本体一致） */
	private static final RenderType RENDER_TYPE = RenderType.cutoutMipped();

	public WaveGateRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(T be, float partialTicks, PoseStack ms,
							  MultiBufferSource buffer, int light, int overlay) {
		// 不调用 super.renderSafe：其会在 Flywheel 可视化激活时直接 return（跳过旧式渲染），
		// 我们没有注册 Flywheel visual，因此直接绘制。

		// 用方块实际 FACING（不是从轴重算方向）：姿态对齐 + 旋转轴都跟随放置方向
		Direction facing = be.getBlockState().getValue(DirectionalKineticBlock.FACING);

		// 1. 齿轮：随应力旋转（转速达标转、不足静止）
		SuperByteBuffer cogwheel = CachedBuffers.partialFacingVertical(AllPartialModels.WAVE_REGULATOR_COGWHEEL,
			be.getBlockState(), facing);
		if (be.isSpeedRequirementFulfilled()) {
			standardKineticRotationTransform(cogwheel, be, light)
				.renderInto(ms, buffer.getBuffer(RENDER_TYPE));
		} else {
			cogwheel.light(light)
				.renderInto(ms, buffer.getBuffer(RENDER_TYPE));
		}

		// 2. 侧面指示灯：面板 open + 转速达标才亮（自发光）
		var state = be.getBlockState();
		boolean fulfilled = be.isSpeedRequirementFulfilled();
		boolean topOpen = state.getValue(AbstractWaveGateBlock.RECEIVER_TOP);
		boolean bottomOpen = state.getValue(AbstractWaveGateBlock.RECEIVER_BOTTOM);
		if (!fulfilled)
			return; // 转速不足：灯全灭（面板状态仍由 blockstate 贴图显示）

		for (String side : new String[] { "north", "east", "south", "west" }) {
			if (topOpen)
				renderLamp(AllPartialModels.WAVE_GATE_LAMPS.get(side + "_top"), state, facing, ms, buffer);
			if (bottomOpen)
				renderLamp(AllPartialModels.WAVE_GATE_LAMPS.get(side + "_bottom"), state, facing, ms, buffer);
		}
	}

	/** 侧面指示灯：partialFacingVertical 对齐朝向 + 自发光（光照拉满，不受环境光影响）。 */
	private static void renderLamp(dev.engine_room.flywheel.lib.model.baked.PartialModel model,
								   net.minecraft.world.level.block.state.BlockState state,
								   Direction facing, PoseStack ms, MultiBufferSource buffer) {
		SuperByteBuffer lamp = CachedBuffers.partialFacingVertical(model, state, facing);
		lamp.light(LightTexture.pack(15, 15))
			.renderInto(ms, buffer.getBuffer(RENDER_TYPE));
	}
}
