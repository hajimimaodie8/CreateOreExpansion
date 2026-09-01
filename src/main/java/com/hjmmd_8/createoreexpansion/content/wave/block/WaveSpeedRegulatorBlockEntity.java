package com.hjmmd_8.createoreexpansion.content.wave.block;

import com.hjmmd_8.createoreexpansion.content.wave.regulation.WaveSpeedRegulation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 波速调节器方块实体：面板状态管理与齿轮啮合传播全部继承
 * {@link AbstractWaveGateBlockEntity}（SimpleKineticBlockEntity），
 * 本类仅提供自身的 {@code BlockEntityType} 构造器，并覆写护目镜提示。
 *
 * <p><b>护目镜提示</b>：显示当前变速等级 I/II/III/IV
 * （100~256 RPM 平均分 4 档，见 {@link WaveSpeedRegulation#tierForSpeed(float)}）。</p>
 */
public class WaveSpeedRegulatorBlockEntity extends AbstractWaveGateBlockEntity {

	public WaveSpeedRegulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		tooltip.add(Component.translatable("createoreexpansion.goggles.wave_speed_regulator")
			.withStyle(ChatFormatting.GRAY));
		added = true;

		// 未接入应力/转速不足（< FAST 100 RPM）：无调制，不显示变速等级
		if (!isSpeedRequirementFulfilled())
			return added;

		// 变速等级：按当前转速分档（I/II/III/IV）
		int tier = WaveSpeedRegulation.tierForSpeed(getSpeed());
		Component tierText = Component.translatable("createoreexpansion.goggles.speed_regulator_tier",
			WaveSpeedRegulation.romanForTier(tier))
			.withStyle(tier == 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.AQUA);
		tooltip.add(tierText);

		// 各档调速量（±格/秒），供参考
		tooltip.add(Component.translatable("createoreexpansion.goggles.speed_regulator_amount",
			WaveSpeedRegulation.offsetForSpeed(getSpeed()))
			.withStyle(ChatFormatting.GRAY));

		return added;
	}
}
