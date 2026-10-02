package com.hjmmd_8.createoreexpansion.common.registry.coe.charger;

import com.hjmmd_8.createoreexpansion.common.CoeCore;
import com.hjmmd_8.createoreexpansion.content.charger.entity.ChargerWaveEntity;
import com.hjmmd_8.createoreexpansion.content.charger.entity.StellarWaveEntity;
import com.hjmmd_8.createoreexpansion.content.equipment.boomerang.AstralBoomerangEntity;
import com.hjmmd_8.createoreexpansion.content.equipment.boomerang.JadeTopazBoomerangEntity;
import com.hjmmd_8.createoreexpansion.content.equipment.boomerang.SapphireRubyBoomerangEntity;
import com.hjmmd_8.createoreexpansion.content.equipment.boomerang.ThunderBoomerangEntity;
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
 * <h2>⛔ 这里<b>只许有两个能量波实体类型</b>（用户 2026-10-02 硬口径）</h2>
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
 * <p>关卡 {@code tools/check-armor-sets.ps1} §28e 守着这一条：本文件里的<b>波</b>实体类型
 * （名字里带 {@code wave} 的那两个）既不得超过两个，也不许出现 {@code star_shock} 之类的第三个；
 * 同时 §28e-2 / §29 要求本文件里<b>每一个</b>注册的实体类型都有对应的
 * {@code EntityRenderers.register(...)} 行。</p>
 *
 * <h2>2026-10-02 起本文件也是<b>本层实体类型的唯一注册处</b></h2>
 * <p>回旋镖四把镖的实体类型（{@link #JADE_TOPAZ_BOOMERANG} / {@link #SAPPHIRE_RUBY_BOOMERANG} /
 * {@link #ASTRAL_BOOMERANG} / {@link #THUNDER_BOOMERANG}）与两个波共用同一个
 * {@code DeferredRegister}——同一个注册器就不会出现"两个 DeferredRegister 抢同一注册表"的形状，
 * 而且渲染器覆盖关卡（§28e-2）<b>扫的是本文件全部注册项</b>，新实体天然被它罩住。
 * 它们<b>不是波</b>：{@code AbstractBoomerangEntity extends Projectile}，与波的五要素毫无关系。</p>
 *
 * <p>⚠ 四把镖都<b>不许</b> {@code noSave()}：镖要落盘（区块重载后 owner、镖本身、回程段都要还在）。
 * 这是关卡 §29 的一条断言 —— 两个波实体当初是 {@code noSave()}，照抄过来会让镖重载即丢。</p>
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

	// ================= 回旋镖四把（2026-10-02 第一批） =================
	// 注册参数**照抄 Quark Pickarang**：sized(0.4F, 0.4F) + clientTrackingRange(4) + updateInterval(10)。
	// ⛔ 绝不 noSave()：镖要落盘（owner 由 Projectile 存，镖本身/回程段由
	// AbstractBoomerangEntity 存）；照抄上面两个波实体的 noSave() 会让镖一重载就丢。
	// 渲染器在 client/WaveEntityRendererRegistration（同一个改动里加的行；§28e-2 扫全部注册项）。

	/** 翠玉镖（jade_topaz_boomerang）投掷物。 */
	public static final DeferredHolder<EntityType<?>, EntityType<JadeTopazBoomerangEntity>> JADE_TOPAZ_BOOMERANG =
		ENTITY_TYPES.register("jade_topaz_boomerang",
			() -> EntityType.Builder.<JadeTopazBoomerangEntity>of(JadeTopazBoomerangEntity::new, MobCategory.MISC)
				.sized(0.4F, 0.4F)
				.clientTrackingRange(4)
				.updateInterval(10)
				.build("jade_topaz_boomerang"));

	/** 宝石镖（sapphire_ruby_boomerang）投掷物。 */
	public static final DeferredHolder<EntityType<?>, EntityType<SapphireRubyBoomerangEntity>> SAPPHIRE_RUBY_BOOMERANG =
		ENTITY_TYPES.register("sapphire_ruby_boomerang",
			() -> EntityType.Builder.<SapphireRubyBoomerangEntity>of(SapphireRubyBoomerangEntity::new, MobCategory.MISC)
				.sized(0.4F, 0.4F)
				.clientTrackingRange(4)
				.updateInterval(10)
				.build("sapphire_ruby_boomerang"));

	/** 星界镖（astral_boomerang）投掷物。 */
	public static final DeferredHolder<EntityType<?>, EntityType<AstralBoomerangEntity>> ASTRAL_BOOMERANG =
		ENTITY_TYPES.register("astral_boomerang",
			() -> EntityType.Builder.<AstralBoomerangEntity>of(AstralBoomerangEntity::new, MobCategory.MISC)
				.sized(0.4F, 0.4F)
				.clientTrackingRange(4)
				.updateInterval(10)
				.build("astral_boomerang"));

	/** 雷鸣镖（thunder_boomerang）投掷物。 */
	public static final DeferredHolder<EntityType<?>, EntityType<ThunderBoomerangEntity>> THUNDER_BOOMERANG =
		ENTITY_TYPES.register("thunder_boomerang",
			() -> EntityType.Builder.<ThunderBoomerangEntity>of(ThunderBoomerangEntity::new, MobCategory.MISC)
				.sized(0.4F, 0.4F)
				.clientTrackingRange(4)
				.updateInterval(10)
				.build("thunder_boomerang"));

	public static void register(IEventBus modEventBus) {
		ENTITY_TYPES.register(modEventBus);
	}

	private AllEntityTypes() {
	}
}
