package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity;
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

	/** 能量波（1~5 级：低/高/伽马/伊普西龙/欧米伽；翡翠/蓝宝石充能器通用，不渲染模型，视觉靠粒子） */
	public static final DeferredHolder<EntityType<?>, EntityType<ChargerWaveEntity>> CHARGER_WAVE =
		ENTITY_TYPES.register("charger_wave",
			() -> EntityType.Builder.<ChargerWaveEntity>of(ChargerWaveEntity::new, MobCategory.MISC)
				// 小碰撞盒（0.2）：波是粒子状实体。0.6 盒 + setPos 底部基准会让负方向
				// 穿过调级器时外推不足（碰撞盒仍伸入方块 0.1 格）→ 二次判定湮灭
				.sized(0.2f, 0.2f)
				.noSummon()
				.noSave()
				.build("charger_wave"));

	public static void register(IEventBus modEventBus) {
		ENTITY_TYPES.register(modEventBus);
	}

	private AllEntityTypes() {
	}
}
