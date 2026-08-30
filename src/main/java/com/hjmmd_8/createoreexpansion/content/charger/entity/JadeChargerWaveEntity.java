package com.hjmmd_8.createoreexpansion.content.charger.entity;

import com.hjmmd_8.createoreexpansion.common.AllEntityTypes;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 翡翠充能器能量波：复用 {@link AbstractChargerWaveEntity} 全部飞行/命中/加工逻辑，
 * 仅定制翡翠配色（低=黄、高=绿、伽马=蓝）。
 */
public class JadeChargerWaveEntity extends AbstractChargerWaveEntity {

	public JadeChargerWaveEntity(EntityType<?> type, Level level) {
		super(type, level);
	}

	public JadeChargerWaveEntity(Level level, Vec3 pos, Direction facing, int waveLevel) {
		super(AllEntityTypes.JADE_CHARGER_WAVE.get(), level, pos, facing, waveLevel);
	}

	/** 能量波颜色（RGB 0-1）：低=黄、高=绿、伽马=蓝 */
	@Override
	protected Vec3 getWaveColor() {
		return getWaveColorForLevel(waveLevel);
	}

	/** 指定等级的能量波颜色：低=黄、高=绿、伽马=蓝（供调级器渐变提前取下一等级色） */
	@Override
	protected Vec3 getWaveColorForLevel(int level) {
		return switch (level) {
			case 2 -> new Vec3(0, 1, 0);
			case 3 -> new Vec3(0, 0.5f, 1);
			default -> new Vec3(1, 1, 0);
		};
	}
}
