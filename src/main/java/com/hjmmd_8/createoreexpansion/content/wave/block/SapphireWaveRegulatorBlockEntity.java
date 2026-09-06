package com.hjmmd_8.createoreexpansion.content.wave.block;

import com.hjmmd_8.createoreexpansion.content.wave.WaveLevels;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 蓝宝石能量调级器方块实体：面板/啮合全部继承 {@link AbstractWaveGateBlockEntity}，
 * 仅覆写机型参数：64 RPM 起调制、最大可把波提升至 5 级（欧米伽，终极充能态）。
 */
public class SapphireWaveRegulatorBlockEntity extends AbstractWaveGateBlockEntity {

	public SapphireWaveRegulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** 蓝宝石调级器：64 RPM 起调制（翡翠为 FAST 100）。 */
	@Override
	public float getModulationSpeedThreshold() {
		return 64f;
	}

	/** 转速是否达标（≥64 RPM）：覆写 Create 的 FAST 门槛判定，供护目镜/渲染使用。 */
	@Override
	public boolean isSpeedRequirementFulfilled() {
		return Math.abs(getSpeed()) >= getModulationSpeedThreshold();
	}

	/** 蓝宝石调级器：最大可升等级 5（欧米伽）——1~4 级波顺向逐级 +1，最高到 5（翡翠封顶 3）。 */
	@Override
	public int getMaxBoostLevel() {
		return WaveLevels.SAPPHIRE_MAX;
	}

	/** 蓝宝石调级器：可承载/输出 1~5 级波（4/5 级限制仅翡翠机型有）。 */
	@Override
	public int getMaxSupportedWaveLevel() {
		return WaveLevels.SAPPHIRE_MAX;
	}

	/** 护目镜标题（基类主干输出）：蓝宝石能量调级器。 */
	@Override
	protected Component getGoggleTitle() {
		return Component.translatable("createoreexpansion.goggles.sapphire_wave_regulator");
	}
}
