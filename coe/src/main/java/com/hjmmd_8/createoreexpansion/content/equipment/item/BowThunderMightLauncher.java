package com.hjmmd_8.createoreexpansion.content.equipment.item;

import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveDiag;
import com.hjmmd_8.createoreexpansion.content.energyfield.charge.LightningEssenceHitCharge;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowThunderMightConfigs;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * <b>「以箭命中点为中心、把范围内生物随机染上电荷（并按概率劈一道真雷）」那一段</b> ——
 * 弓技能批 5（雷鸣弓「雷鸣神力」，作者 2026-10-05）；<b>批 19（2026-10-07）改触发时机</b>。
 *
 * <h2>批 19 改了什么：落点由调用方给，本类不再算落点</h2>
 * <p>作者原话（批 19 裁定）：<i>「先给它暂时改成那种特殊的剑，击中生物之后会释放电荷效果」</i>
 * ⇒ 这一发<b>不再在松手那一刻用射线 hitscan 结算</b>，而是<b>照常飞出一支可见的箭</b>，
 * 等那支箭<b>命中生物</b>时，在<b>箭的命中点</b>结算。</p>
 * <p>因此本类的入口从 {@code strike(ServerLevel, LivingEntity, int)}（自己用
 * {@code ProjectileUtil} + {@code world.clip} 算命中点）换成
 * {@link #strikeAt(ServerLevel, LivingEntity, Vec3, int)}（<b>落点作为形参收进来</b>）：</p>
 * <ul>
 *   <li><b>落点唯一来源 = 那支箭的实体命中点</b>
 *       （{@code JadeTopazBowEventHandler#onProjectileImpact} 的
 *       {@code EntityHitResult#getLocation()}，经箭上 {@code TAG_SKILL} 分发到这里）；</li>
 *   <li><b>打空 / 打墙 ⇒ 什么都不发生</b>：本类已经没有"射线打空就取射程末端"那条回落，
 *       因为"触发条件 = 命中生物"是作者这一批的原话（箭会照原版那样飞完 / 插在地上）；</li>
 *   <li><b>退役并删除的旧机制</b>：{@code pickImpact(..)}（实体射线 + 方块射线取更近者的那条
 *       {@code BoomerangImpact} 形状）与只为它存在的两个常量
 *       （{@code BowThunderMightConfigs.MAX_IMPACT_RANGE} / {@code PICK_SWEEP_INFLATE}）——
 *       留着它们就是留<b>第二个</b>算落点的地方，而本批的全部内容恰恰是"落点只有一个来源"。</li>
 * </ul>
 *
 * <h2>它与 {@link BowWaveShiftLauncher} 的分工（两个类，两张表）</h2>
 * <p>批 4 那条（宝石弓「量波置换」/ 星界弓「星元波置」）发的是<b>既有能量波实体</b>，
 * 数值住在 {@code BowWaveShiftConfigs}；本批这条<b>一枚波都不发</b>（作者："不是发波，是另一套"），
 * 它做的是"电荷 + 原版闪电"，数值住在 {@code BowThunderMightConfigs}。
 * 两件事<b>没有共用的数值、也没有共用的机制</b>，硬塞进一个类只会让两张表互相污染
 * ⇒ 各自一个发射类、各自一张数值表。</p>
 *
 * <h2>★ 电荷：经既有唯一施加面，调用点零数字</h2>
 * <p>范围内每一只生物交 {@code LightningEssenceHitCharge#applyOnHit}（它内部调
 * {@code ChargeApi#applyRandom}）—— 于是"极性随机 / 等级 1~3 随机 / 时长固定 5 秒"这三条
 * <b>与"雷魔素命中生物"那条统一标准逐字同源</b>，也与<b>雷鸣合金剑</b>那条既有途径
 * （{@code ThunderiteHitChargeHandler}）落在<b>同一个施加面</b>上；本类既不认 effect 类、
 * 也不写任何等级或时长，更不会出现第二个 {@code addEffect} 施加点
 * （{@code ChargeApi} 是全仓唯一的电荷施加面）。</p>
 * <p>⚠ 与那把剑的差别<b>只有等级取哪里</b>：剑读"武器上攻击类技能的最高有效等级"
 * （{@code AllSkills#effectiveLevel}），弓读<b>自己槽 2 那条技能的有效等级</b>
 * （{@code BowTier#thirdSkillEffectiveLevel}，批 14 裁定：效果与费用同源）——
 * 两者都走同一个"技能有效等级"口径，只是各自问的是自己那件武器的那条技能。</p>
 *
 * <h2>★ 范围：仍是"命中点为心的水平方形"（批 19 未改）</h2>
 * <p>作者批 19 的原话只说"击中生物之后会释放电荷效果"，字面上像是"只有被击中的那一只"；
 * 但这条技能<b>既有的范围口径</b>是"边长 2 / 3 / 4 的水平方形内每只生物各染一笔"，
 * 而本批改的是<b>触发时机</b>（松手 hitscan ⇒ 箭命中点），<b>不是范围</b> ⇒ 方形逐位保留。
 * 改成"只那一只"属于<b>玩法变化</b>，需要作者点头，本批不做。</p>
 *
 * <h2>★ 真雷：只判一次、只落一处、绝不落在施放者身上</h2>
 * <ul>
 *   <li><b>只判一次</b>：整条判定就在这一发里跑一遍 —— <b>没有 tick 循环、没有
 *       {@code @SubscribeEvent}、不看天气</b>（"雷雨天无限刷"那条担心的落地方式就是"别把这件事
 *       挂到一个每 tick/每世界 tick 的地方"，本类不是事件订阅者、也没有任何计时器）；</li>
 *   <li><b>只落一处</b>：{@code EntityType.LIGHTNING_BOLT.create(..)} 全类<b>恰好一处</b>，
 *       而且它在"每只生物"那个循环<b>外面</b> —— 作者要的是"劈一道雷"，不是"范围内每只生物各劈一道"；</li>
 *   <li><b>绝不落在施放者身上</b>：落点坐标只来自形参 {@code impact}（箭的命中点），
 *       本类<b>不读施放者的位置</b>（没有 {@code shooter.position()} / {@code shooter.getX()}）；
 *       范围查询也把施放者排除在外（同 {@code BowHitEffects#applyDisarm} 那条"自己的范围效果不落在
 *       自己身上"的既有形状）。⚠ 批 19 起"落点"是箭打中的那只生物 ⇒ 若箭打中的<b>正是施放者本人</b>
 *       （例如朝正上方射、箭落回自己头上），落点就会是他自己 —— 那条极端形状批 19 <b>没有</b>加判据
 *       （作者 2026-10-07 实机踩到），<b>批 20 已补上</b>：闸门落在<b>调用方</b>
 *       （{@code JadeTopazBowEventHandler#onProjectileImpact} 在"本技能的箭命中主人"时直接
 *       {@code setCanceled(true)} 并返回 ⇒ 本类根本不会被调到），本类因此仍然只需要"看 impact"
 *       这一个动作；</li>
 *   <li><b>原版闪电</b>：刻意<b>不</b>调 {@code setVisualOnly(true)} —— 作者明确选了会点燃、
 *       会破坏地形的原版闪电（视觉版闪电什么都不做）。</li>
 * </ul>
 *
 * <h2>本类<b>不</b>做什么</h2>
 * <ul>
 *   <li><b>不</b>判"该不该放"（物品侧的两个闸门：档位表 + 技能键，见
 *       {@code JadeTopazBowItem#stampThunderMightForArrow}）；</li>
 *   <li><b>不</b>造箭、<b>不</b>算落点、<b>不</b>碰触发时机（"这一发照常飞出箭"与"命中生物时才
 *       结算"两件事分别落在弓物品类与命中处理器里）；</li>
 *   <li><b>不</b>扣能量、<b>不</b>碰冷却、<b>不</b>扣耐久（耐久与原版同一笔账，在
 *       {@code ProjectileWeaponItem#shoot} 里扣一次）；</li>
 *   <li><b>不</b>造实体类型 / 模型 / 贴图 / 渲染器 / 注册项 / 语言键（发的是既有
 *       {@code minecraft:lightning_bolt} 与既有两个电荷效果）。</li>
 * </ul>
 *
 * <h2>为什么本类<b>一个数字字符都不写</b></h2>
 * <p>连"除以二"（水平半边长）都归 {@code BowThunderMightConfigs}：本批的验收里
 * 有一条负向断言是"调用点不得出现 {@code 2/3/4}、{@code 0.2/0.4/0.6} 字面量"，把口径推到"本文件
 * 一个数字字符都没有"是最强、也最容易守住的形式（连日志文本里都不写数字，免得这条判据要靠例外
 * 才成立）。</p>
 *
 * @since 1.0.0
 */
public final class BowThunderMightLauncher {

	private BowThunderMightLauncher() {
		throw new AssertionError("This class should not be instantiated");
	}

	/**
	 * <b>在箭的命中点放一次「雷鸣神力」</b>：以该点为中心的水平方形内，每只生物随机染一笔电荷；
	 * 并按该等级的概率，在该点劈一道原版闪电。
	 *
	 * <p>调用前世界必须是服务端（{@code ServerLevel}）：电荷施加与闪电生成都只在服务端有权威结果。
	 * 调用点唯一 —— {@code JadeTopazBowEventHandler#onProjectileImpact} 里那支带
	 * {@code TAG_SKILL} 的雷鸣箭<b>命中生物</b>的那一支（打空 / 打墙走不到这里）。</p>
	 *
	 * @param world   服务端世界
	 * @param shooter 施放者（<b>只</b>用来"把自己排除在范围之外"；本类绝不把它当落点）
	 * @param impact  <b>箭的命中点</b>（由命中处理器把 {@code EntityHitResult#getLocation()} 交进来；
	 *                本类只落在这儿，绝不取施放者坐标）
	 * @param level   技能等级（1~3，越界夹取；雷鸣弓传的是
	 *                {@code BowTier#thirdSkillEffectiveLevel} 的有效等级）⇒
	 *                Lv1/2/3 = 2×2/3×3/4×4 与 20%/40%/60%
	 * @return 本次被染上电荷的生物只数（供日志 / 关卡核对；闪电劈没劈看日志那一行）
	 */
	public static int strikeAt(ServerLevel world, LivingEntity shooter, Vec3 impact, int level) {
		double half = BowThunderMightConfigs.areaHalfExtentFor(level);
		AABB area = new AABB(
			impact.x - half, impact.y - BowThunderMightConfigs.AREA_VERTICAL_HALF_EXTENT, impact.z - half,
			impact.x + half, impact.y + BowThunderMightConfigs.AREA_VERTICAL_HALF_EXTENT, impact.z + half);
		List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, area,
			candidate -> candidate != shooter && candidate.isAlive());
		for (LivingEntity target : targets) {
			// 施加面唯一：那一处内部调 ChargeApi#applyRandom（极性随机 / 等级随机 / 时长固定）。
			LightningEssenceHitCharge.applyOnHit(target);
		}

		float chance = BowThunderMightConfigs.realBoltChanceFor(level);
		// 只判一次：整条技能里就这一处掷骰子、就这一处召唤闪电（且不在上面那个循环里）。
		boolean struck = world.random.nextFloat() < chance && spawnRealBolt(world, impact);

		// 日志走 WaveDiag（全系统唯一出口；这一行让"落点在哪、范围内几只、劈没劈"在日志里可查）。
		WaveDiag.trace(
			"雷鸣神力：技能 {} 级 → 箭命中点 {}（边长 {} 格的水平方形，垂直容差 {} 格），范围内 {} 只生物已随机染电（经既有电荷施加面，时长与等级按 ChargeConfigs 的表随机），真雷概率 {} ⇒ {}",
			level, impact, BowThunderMightConfigs.areaSideFor(level),
			BowThunderMightConfigs.AREA_VERTICAL_HALF_EXTENT, targets.size(), chance,
			struck ? "已劈一道原版闪电" : "未劈");
		return targets.size();
	}

	/**
	 * <b>在命中点劈一道真雷</b>（原版 {@code minecraft:lightning_bolt}）。
	 *
	 * <p>刻意<b>不</b>调 {@code setVisualOnly(true)}：作者明确要"真的劈一道雷"（会点燃、会破坏地形），
	 * 而视觉版闪电什么都不做。落点坐标只来自 {@code impact}（箭的命中点）—— 本方法<b>不看</b>施放者，
	 * 所以"把施放者劈了"这件事在<b>本方法</b>里没有入口。</p>
	 *
	 * @return 闪电是否真的进了世界（{@code create} 返回 null 或 {@code addFreshEntity} 失败时为 false）
	 */
	private static boolean spawnRealBolt(ServerLevel world, Vec3 impact) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(world);
		if (bolt == null) {
			return false;
		}
		bolt.moveTo(impact.x, impact.y, impact.z);
		return world.addFreshEntity(bolt);
	}
}
