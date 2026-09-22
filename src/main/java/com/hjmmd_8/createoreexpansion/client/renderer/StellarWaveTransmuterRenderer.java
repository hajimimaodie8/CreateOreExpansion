package com.hjmmd_8.createoreexpansion.client.renderer;

import com.hjmmd_8.createoreexpansion.common.AllBlocks;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveTransmuterBlock;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveTransmuterBlockEntity;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.TransmuterMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 星辉波变器渲染：在 FACING 的<b>反面（底部）</b>渲染传动轴（Create SHAFT_HALF），
 * 随转速旋转——与能量场控制器/应力充能器同款轴视觉（本机轴口仅此一面）。
 * 方块本体用 blockstate 静态模型（沿用能量场控制器同族模型），无需额外渲染。
 *
 * <p><b>本渲染器只画"轴"这一件东西</b>，且没有任何按模式/开口状态提前返回的分支
 * （唯一 return 是"方块不是 DirectionalKineticBlock"这一恒假分支）。灯盘面、4 盏 2×2 指示灯、
 * 接收面、轴口底座全部在 16 个 blockstate 变体模型里（见 {@code AllBlocks} datagen）。</p>
 */
public class StellarWaveTransmuterRenderer extends KineticBlockEntityRenderer<StellarWaveTransmuterBlockEntity> {

	/**
	 * 临时诊断开关（<b>默认开</b>，排查"变器看起来是素面方块"用）。
	 *
	 * <p><b>调试用，定案后恢复 false 或整段删除</b>（关掉 = 把下面的初值改回
	 * {@code Boolean.getBoolean("createoreexpansion.transmuterDiag")}）。
	 * 开启后<b>看着变器就会在动作栏</b>
	 * 每 20 tick 打一行，报"BER 到没到、这台机器的开口状态、当前变体号、轴速"，
	 * 由此把三种"素面"成因分开：</p>
	 * <ul>
	 *   <li>动作栏<b>完全没出现</b> → BER 根本没被调用（注册/类型问题）；</li>
	 *   <li>出现且 {@code BER=OK}、但画面上仍无轴 → 渲染被覆盖/深度问题；</li>
	 *   <li>出现且变体号与开口状态一致（N=+8 / S=+4 / W=+2 / E=+1）→ 素面是模型自身外观，
	 *       不是渲染缺失。</li>
	 * </ul>
	 */
	// 临时诊断开关：调试用，定案后恢复 Boolean.getBoolean("createoreexpansion.transmuterDiag") 或整段删除。
	private static final boolean DIAG = true;

	/** 诊断节流（渲染每帧都会调，这里只每 20 tick 打一行）。 */
	private static final int DIAG_INTERVAL = 20;

	/** 上次诊断输出的渲染时刻（给 {@link #DIAG} 的节流用）。 */
	private float diagLastTick = Float.NaN;

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

		if (DIAG)
			renderDiag(be, state, facing);

		SuperByteBuffer shaft = CachedBuffers.partialFacing(
			com.simibubi.create.AllPartialModels.SHAFT_HALF, state, facing.getOpposite());
		standardKineticRotationTransform(shaft, be, light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
	}

	/**
	 * 一次性诊断（见 {@link #DIAG}）：动作栏一行，报"BER 到没到、当前开口状态与变体号"。
	 *
	 * <p>只读状态，不改任何东西；关掉开关后本方法不会被调用。定位完直接删掉本方法与
	 * {@link #DIAG} 相关字段即可。</p>
	 */
	private void renderDiag(StellarWaveTransmuterBlockEntity be, BlockState state, Direction facing) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null)
			return;
		float now = AnimationTickHolder.getRenderTime(mc.level);
		if (!Float.isNaN(diagLastTick) && now - diagLastTick < DIAG_INTERVAL)
			return;
		diagLastTick = now;

		boolean north = state.getValue(StellarWaveTransmuterBlock.NORTH);
		boolean south = state.getValue(StellarWaveTransmuterBlock.SOUTH);
		boolean west = state.getValue(StellarWaveTransmuterBlock.WEST);
		boolean east = state.getValue(StellarWaveTransmuterBlock.EAST);
		int variant = (north ? 8 : 0) | (south ? 4 : 0) | (west ? 2 : 0) | (east ? 1 : 0);

		mc.player.displayClientMessage(Component.literal("[变器诊断] BER=OK 机器=" + AllBlocks.STELLAR_WAVE_TRANSMUTER.getId()
			+ " 模式=" + TransmuterMode.at(mc.level, be.getBlockPos())
				.key()
			+ " facing=" + facing + " N/S/W/E=" + north + "/" + south + "/" + west + "/" + east
			+ " → 变体模型 " + variant + " 轴速=" + be.getSpeed())
			.withStyle(ChatFormatting.AQUA), true);
	}
}
