package com.hjmmd_8.createoreexpansion.common.registry.coe.charger;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity;
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
 *
 * <h2>⛔ 这里<b>只许有两个</b>能量波实体类型（用户 2026-10-02 硬口径）</h2>
 * <p>作者原话：「能量波是由好几个要素定义的…<b>你不要再凭空造出一个新的能量波哈，
 * 不要造出一个攻击波哈</b>」——技能要发波，一律<b>用 {@link #CHARGER_WAVE} 这一个实体</b>，
 * 靠设置它的<b>五要素</b>（波速 / 波级 / 波载荷 / 波型 / 剩余寿命）把那发波发出去。</p>
 *
 * <p><b>为什么这条必须写在这里</b>：2026-10-02 星界轮曾经给星芒嬗震自造了一个
 * {@code star_shock_wave}（+ 一个 {@code StarShockWaveEntity} 子类），但<b>忘了注册渲染器</b> ⇒
 * 客户端第一次把它渲染进视野就
 * {@code NullPointerException: Cannot invoke "EntityRenderer.shouldRender(...)" because
 * "entityrenderer" is null}（{@code crash-reports/crash-2026-10-02_14.23.33-client.txt}）。
 * 同一个类型上「谁注册实体类型谁注册渲染器」这条不变量在
 * {@code client/WaveEntityRendererRegistration} 里也有逐条的说明。</p>
 *
 * <p>关卡 {@code tools/check-armor-sets.ps1} §28e 守着这一条：本文件里出现
 * {@code star_shock} 之类的第三个波实体类型即红。</p>
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

	public static void register(IEventBus modEventBus) {
		ENTITY_TYPES.register(modEventBus);
	}

	private AllEntityTypes() {
	}
}
