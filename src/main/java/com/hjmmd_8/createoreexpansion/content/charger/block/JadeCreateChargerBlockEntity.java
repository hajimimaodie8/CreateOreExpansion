package com.hjmmd_8.createoreexpansion.content.charger.block;

import com.hjmmd_8.createoreexpansion.common.AllEntityTypes;
import com.hjmmd_8.createoreexpansion.content.charger.entity.JadeChargerWaveEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 翡翠应力充能器方块实体：复用 {@link AbstractCreateChargerBlockEntity} 全部通用行为，
 * 仅定制翡翠配色（低=黄、高=绿、伽马=蓝）与翡翠充能波实体。
 */
public class JadeCreateChargerBlockEntity extends AbstractCreateChargerBlockEntity {

	public JadeCreateChargerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected JadeChargerWaveEntity createWave(Level level, Vec3 start, Vec3 movementDir, int mode) {
		return new JadeChargerWaveEntity(level, start, movementDir, mode);
	}

	/** 翡翠充能波颜色（RGB 0-1）：低=黄、高=绿、伽马=蓝 */
	@Override
	protected int getWaveColor(int mode) {
		return switch (mode) {
			case 2 -> 0x55FF55;
			case 3 -> 0x5555FF;
			case 1 -> 0xFFFF55;
			default -> 0xAAAAAA;
		};
	}

	@Override
	protected Component getMachineName() {
		return Component.translatable("createoreexpansion.goggles.jade_charger");
	}
}
