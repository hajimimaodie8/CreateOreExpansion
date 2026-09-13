package com.hjmmd_8.createoreexpansion.content.charger.block;

import com.hjmmd_8.createoreexpansion.common.AllEntityTypes;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveLevels;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 翡翠应力充能器方块实体：复用 {@link AbstractCreateChargerBlockEntity} 全部通用行为，
 * 仅定制翡翠配色（α 黄/β 绿/γ 蓝，封顶 γ）与翡翠充能波实体。
 */
public class JadeStressChargerBlockEntity extends AbstractCreateChargerBlockEntity {

	public JadeStressChargerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected ChargerWaveEntity createWave(Level level, Vec3 start, Vec3 movementDir, int mode) {
		return new ChargerWaveEntity(level, start, movementDir, mode);
	}

	/**
	 * 翡翠充能波颜色（ARGB）：<b>标准 5 档表里只认 α~γ</b>（翡翠线封顶 3 级）——
	 * 色值查 {@link WaveLevels#indicatorColor(int)}，ε/ω 与未接入一律回落基类的未接入灰。
	 */
	@Override
	protected int getWaveColor(int mode) {
		return mode >= WaveLevels.LOW && mode <= WaveLevels.JADE_MAX
			? WaveLevels.indicatorColor(mode)
			: IDLE_INDICATOR_COLOR;
	}

	@Override
	protected Component getMachineName() {
		return Component.translatable("createoreexpansion.goggles.jade_charger");
	}
}
