package com.hjmmd_8.createoreexpansion.content.wave.block;

import com.hjmmd_8.createoreexpansion.content.wave.WaveLevels;
import com.hjmmd_8.createoreexpansion.content.wave.regulation.WaveSpeedRegulation;
import com.hjmmd_8.createoreexpansion.util.GoggleUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 星辉石波速调节器方块实体：面板/啮合全部继承 {@link AbstractWaveGateBlockEntity}，
 * 覆写机型参数（32 RPM 起、32~256 分 8 档 0.5~4）与护目镜提示。
 */
public class StellarstoneSpeedRegulatorBlockEntity extends AbstractWaveGateBlockEntity {

	public StellarstoneSpeedRegulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** 星辉石波速调节器：32 RPM 起调制（蓝宝石为 64）。 */
	@Override
	public float getModulationSpeedThreshold() {
		return 32f;
	}

	/** 转速是否达标（≥32 RPM）：覆写 Create 的 FAST 门槛判定。 */
	@Override
	public boolean isSpeedRequirementFulfilled() {
		return Math.abs(getSpeed()) >= getModulationSpeedThreshold();
	}

	/** 星辉石波速调节器：32~256 RPM 平均分 8 档。 */
	@Override
	public int getSpeedTierCount() {
		return 8;
	}

	/** 星辉石波速调节器：可承载 1~5 级波（同蓝宝石，不受翡翠 3 级限制）。 */
	@Override
	public int getMaxSupportedWaveLevel() {
		return WaveLevels.MAX_LEVEL;
	}

	/** 单档 0.5 格/秒（8 档 = 0.5~4）。 */
	@Override
	public float getSpeedTierStep() {
		return 0.5f;
	}

	/** 分档下界 32 RPM。 */
	@Override
	public float getSpeedTierBase() {
		return 32f;
	}

	/** 分档上界 256 RPM。 */
	@Override
	public float getSpeedTierMax() {
		return 256f;
	}

	/** 护目镜：星辉石波速调节器标题（基类主干）+ 8 档变速等级（I~VIII）+ 调速量。 */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		// 未接入应力/转速不足（< 32 RPM）：无调制（需求提示已由基类给出）
		if (!isSpeedRequirementFulfilled())
			return added;

		int tier = WaveSpeedRegulation.tierForSpeed(getSpeed(), getSpeedTierBase(), getSpeedTierMax(),
			getSpeedTierCount());
		GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.speed_regulator_tier",
			WaveSpeedRegulation.romanForTier(tier))
			.withStyle(tier == 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.AQUA));
		GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.speed_regulator_amount",
			WaveSpeedRegulation.offsetForSpeed(getSpeed(), getSpeedTierBase(), getSpeedTierMax(),
				getSpeedTierCount(), getSpeedTierStep()))
			.withStyle(ChatFormatting.GRAY));

		return added;
	}

	/** 护目镜标题（基类主干输出）：星辉石波速调节器。 */
	@Override
	protected Component getGoggleTitle() {
		return Component.translatable("createoreexpansion.goggles.stellarstone_speed_regulator");
	}
}
