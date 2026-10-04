package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import com.hjmmd_8.createoreexpansion.content.equipment.boomerang.item.BoomerangTier;

/** 雷鸣镖实体：只提供类型与数值档（逻辑全在 {@link AbstractBoomerangEntity}）。 */
public class ThunderBoomerangEntity extends AbstractBoomerangEntity {

	public ThunderBoomerangEntity(EntityType<? extends ThunderBoomerangEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected BoomerangTier tier() {
		return BoomerangTier.THUNDER;
	}
}
