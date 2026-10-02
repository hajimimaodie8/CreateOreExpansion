package com.hjmmd_8.createoreexpansion.common.registry.coe.charger;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.entity.StarShockWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.entity.StellarWaveEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * <b>能量波实体类型注册（第一层侧）</b>。
 *
 * <p><b>W6-c：包名由 {@code common.registry.cews} 改成
 * {@code common.registry.coe.charger}</b>——波实体是能量波引擎的一部分，随 {@code :coe} 发货；
 * 而 {@code common.registry.cews} 这个 Java 包现在由第二层独占（一个包不能同时属于两个 mod 文件，
 * JPMS 启动期 {@code ResolutionException}）。注册 id、命名空间、实体定义一个字都没改。</p>
 */
public final class AllEntityTypes {

	private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
		DeferredRegister.create(Registries.ENTITY_TYPE, CoeCore.REGISTRY_NAMESPACE);

	/** 能量波（1~5 级：α/β/γ/ε/ω；翡翠/蓝宝石充能器通用，不渲染模型，视觉靠粒子） */
	public static final DeferredHolder<EntityType<?>, EntityType<ChargerWaveEntity>> CHARGER_WAVE =
		ENTITY_TYPES.register("charger_wave",
			() -> EntityType.Builder.<ChargerWaveEntity>of(ChargerWaveEntity::new, MobCategory.MISC)
				// 小碰撞盒（0.2）：波是粒子状实体。0.6 盒 + setPos 底部基准会让负方向
				// 穿过调级器时外推不足（碰撞盒仍伸入方块 0.1 格）→ 二次判定湮灭
				.sized(0.2f, 0.2f)
				.noSummon()
				.noSave()
				.build("charger_wave"));

	/** 能量波变体（星辉波变器侧面穿出，携带加工属性集；不渲染模型，视觉靠粒子） */
	public static final DeferredHolder<EntityType<?>, EntityType<StellarWaveEntity>> STELLAR_WAVE =
		ENTITY_TYPES.register("stellar_wave",
			() -> EntityType.Builder.<StellarWaveEntity>of(StellarWaveEntity::new, MobCategory.MISC)
				.sized(0.2f, 0.2f)
				.noSummon()
				.noSave()
				.build("stellar_wave"));

	/**
	 * <b>星芒嬗震的能量波</b>（星界套技能专用，2026-10-02）。
	 *
	 * <p>与 {@link #CHARGER_WAVE} <b>分成两个实体类型</b>是刻意的：技能波多出来的行为
	 * （自定义伤害、环绕波、命中嬗乱、发射批次）只写在这一个类型上，机器波的字段与流程
	 * 一个字节都不动（改共享基类时机器波最容易被连带改坏）。</p>
	 *
	 * <p>碰撞盒与机器波同尺寸（0.2）——波是粒子状实体，且环绕半径 0.8 格 + 0.2 盒
	 * ⇒ 环绕波与主波之间最近仍有 0.4 格空隙，不会自己蹭到一起（同批豁免也只影响爆炸，
	 * 不影响它们各自撞墙/撞生物消散）。</p>
	 */
	public static final DeferredHolder<EntityType<?>, EntityType<StarShockWaveEntity>> STAR_SHOCK_WAVE =
		ENTITY_TYPES.register("star_shock_wave",
			() -> EntityType.Builder
				.<StarShockWaveEntity>of(StarShockWaveEntity::new, MobCategory.MISC)
				.sized(0.2f, 0.2f)
				.noSummon()
				// 不 noSave()：环绕波要靠"父波 UUID"重建编组关系，被存档丢掉的波会变成
				// 一颗永久留在世界里的孤立波（它自己的位置还要靠父波算）。
				.build("star_shock_wave"));

	public static void register(IEventBus modEventBus) {
		ENTITY_TYPES.register(modEventBus);
	}

	private AllEntityTypes() {
	}
}
