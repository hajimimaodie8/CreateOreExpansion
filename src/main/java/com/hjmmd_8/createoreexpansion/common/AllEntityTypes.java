package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.charger.entity.JadeChargerWaveEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 实体类型注册。
 */
public final class AllEntityTypes {

	private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
		DeferredRegister.create(Registries.ENTITY_TYPE, CreateOreExpansion.MOD_ID);

	/** 充能器能量波（不渲染模型，视觉靠粒子） */
	public static final DeferredHolder<EntityType<?>, EntityType<JadeChargerWaveEntity>> JADE_CHARGER_WAVE =
		ENTITY_TYPES.register("jade_charger_wave",
			() -> EntityType.Builder.<JadeChargerWaveEntity>of(JadeChargerWaveEntity::new, MobCategory.MISC)
				.sized(0.6f, 0.6f)
				.noSummon()
				.noSave()
				.build("jade_charger_wave"));

	public static void register(IEventBus modEventBus) {
		ENTITY_TYPES.register(modEventBus);
	}

	private AllEntityTypes() {
	}
}
