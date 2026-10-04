package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import com.hjmmd_8.createoreexpansion.content.equipment.boomerang.item.BoomerangTier;

/** 宝石镖实体：只提供类型与数值档（逻辑全在 {@link AbstractBoomerangEntity}）。 */
public class SapphireRubyBoomerangEntity extends AbstractBoomerangEntity {

	public SapphireRubyBoomerangEntity(EntityType<? extends SapphireRubyBoomerangEntity> type, Level level) {
		super(type, level);
	}

	@Override
	protected BoomerangTier tier() {
		return BoomerangTier.SAPPHIRE_RUBY;
	}
}
