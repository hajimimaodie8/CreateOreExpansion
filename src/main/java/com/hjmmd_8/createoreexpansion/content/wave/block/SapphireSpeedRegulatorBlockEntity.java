package com.hjmmd_8.createoreexpansion.content.wave.block;

import com.hjmmd_8.createoreexpansion.content.wave.regulation.WaveSpeedRegulation;
import com.hjmmd_8.createoreexpansion.util.GoggleUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 蓝宝石波速调节器方块实体：面板/啮合全部继承 {@link AbstractWaveGateBlockEntity}，
 * 覆写机型参数（64 RPM 起、64~256 分 6 档 0.5~3）与护目镜提示。
 */
public class SapphireSpeedRegulatorBlockEntity extends AbstractWaveGateBlockEntity {

	public SapphireSpeedRegulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** 蓝宝石波速调节器：64 RPM 起调制。 */
	@Override
	public float getModulationSpeedThreshold() {
		return 64f;
	}

	/** 转速是否达标（≥64 RPM）：覆写 Create 的 FAST 门槛判定。 */
	@Override
	public boolean isSpeedRequirementFulfilled() {
		return Math.abs(getSpeed()) >= getModulationSpeedThreshold();
	}

	/** 蓝宝石波速调节器：64~256 RPM 平均分 6 档。 */
	@Override
	public int getSpeedTierCount() {
		return 6;
	}

	/** 蓝宝石波速调节器：可承载 1~5 级波（4/5 级限制仅翡翠机型有）。 */
	@Override
	public int getMaxSupportedWaveLevel() {
		return com.hjmmd_8.createoreexpansion.content.wave.WaveLevels.SAPPHIRE_MAX;
	}

	/** 单档 0.5 格/秒（6 档 = 0.5~3）。 */
	@Override
	public float getSpeedTierStep() {
		return 0.5f;
	}

	/** 分档下界 64 RPM。 */
	@Override
	public float getSpeedTierBase() {
		return 64f;
	}

	/** 分档上界 256 RPM。 */
	@Override
	public float getSpeedTierMax() {
		return 256f;
	}

	/** 护目镜：蓝宝石波速调节器标题（基类主干）+ 6 档变速等级（I~VI）+ 调速量。 */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		// 未接入应力/转速不足（< 64 RPM）：无调制（需求提示已由基类给出）
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

	/** 护目镜标题（基类主干输出）：蓝宝石波速调节器。 */
	@Override
	protected Component getGoggleTitle() {
		return Component.translatable("createoreexpansion.goggles.sapphire_speed_regulator");
	}
}
