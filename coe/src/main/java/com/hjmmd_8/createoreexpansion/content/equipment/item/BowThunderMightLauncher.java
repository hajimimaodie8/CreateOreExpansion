package com.hjmmd_8.createoreexpansion.content.equipment.item;

import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveDiag;
import com.hjmmd_8.createoreexpansion.content.energyfield.charge.LightningEssenceHitCharge;
import com.hjmmd_8.createoreexpansion.content.skill.config.weapon.BowThunderMightConfigs;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * <b>「以命中点为中心、把范围内生物随机染上电荷（并按概率劈一道真雷）」那一段</b> ——
 * 弓技能批 5（雷鸣弓「雷鸣神力」，作者 2026-10-05）。
 *
 * <h2>它与 {@link BowWaveShiftLauncher} 的分工（本批刻意做成<b>两个</b>类）</h2>
 * <p>批 4 那条（宝石弓「量波置换」/ 星界弓「星元波置」）发的是<b>既有能量波实体</b>，
 * 数值住在 {@code BowWaveShiftConfigs}；本批这条<b>一枚波都不发</b>（作者："不是发波，是另一套"），
 * 它做的是"电荷 + 原版闪电"，数值住在 {@code BowThunderMightConfigs}。
 * 两件事<b>没有共用的数值、也没有共用的机制</b>，硬塞进一个类只会让两张表互相污染
 * ⇒ 各自一个发射类、各自一张数值表，键位/闸门/耐久那三件事则逐字同形（见下）。</p>
 *
 * <h2>本类的形状（与批 4 的发射类逐条对齐）</h2>
 * <table border="1">
 *   <caption>{@code BowWaveShiftLauncher} vs 本类</caption>
 *   <tr><th>步骤</th><th>{@code BowWaveShiftLauncher}</th><th>本类</th></tr>
 *   <tr><td>触发</td><td>按着技能键的那一发（物品侧闸门）</td><td><b>同一个</b>键位读数、同一个闸门形状</td></tr>
 *   <tr><td>方向</td><td>玩家准心（单位向量）</td><td>玩家准心（单位向量）</td></tr>
 *   <tr><td>落点</td><td>眼睛 + 准心 × 1 格（炮口）</td>
 *       <td><b>射线命中点</b>：实体射线与方块射线取更近的那一个（照抄 {@code BoomerangImpact}
 *           那条既有口径），都打空则取射程末端</td></tr>
 *   <tr><td>主效果</td><td>一枚带重力的攻击波 + 0/1/2 枚环绕伴随波</td>
 *       <td><b>范围内生物各挂一笔随机电荷</b>（经既有唯一施加面）</td></tr>
 *   <tr><td>额外</td><td>随机魔素 / 环绕要素 / 批次号</td>
 *       <td><b>按概率劈一道原版闪电</b>（落点 = 命中点）</td></tr>
 *   <tr><td>数值</td><td>{@code BowWaveShiftConfigs}</td><td>{@code BowThunderMightConfigs}</td></tr>
 * </table>
 *
 * <h2>★ 落点是「技能那一发」算出来的（本批最重要的一条口径）</h2>
 * <p>本技能与批 4 那条一样<b>把这一发箭整个换掉</b>（在 {@code JadeTopazBowItem#shoot} 的闸门里，
 * 见那里对"为什么覆写 shoot 而不是 shootProjectile"的说明），所以"命中点"不可能等箭飞出去再读
 * ——它必须<b>在发射那一刻</b>由射线算出来：</p>
 * <ol>
 *   <li><b>实体射线 + 方块射线，取更近的那一个</b>（{@link #pickImpact}）：形状与
 *       {@code BoomerangImpact#checkImpact} 逐字同源 —— 只查方块会让"隔着墙打到墙后的生物"
 *       从另一头回来（那条注释里写着），只查实体会让"打在墙上"变成"打在墙后"；</li>
 *   <li><b>射线本身排除施放者</b>（{@code candidate != shooter}）：不排除的话，站在自己正前方
 *       一格半的生物（其实就是自己）会被判成命中目标；</li>
 *   <li><b>都打空 ⇒ 取射程末端</b>（{@link BowThunderMightConfigs#MAX_IMPACT_RANGE}），
 *       于是"朝天上打"也仍然有一个确定的落点，不会出现"什么都没有发生"。</li>
 * </ol>
 *
 * <h2>★ 电荷：经既有唯一施加面，调用点零数字</h2>
 * <p>范围内每一只生物交 {@code LightningEssenceHitCharge#applyOnHit}（它内部调
 * {@code ChargeApi#applyRandom}）—— 于是"极性随机 / 等级 1~3 随机 / 时长固定 5 秒"这三条
 * <b>与"雷魔素命中生物"那条统一标准逐字同源</b>，本类既不认 effect 类、也不写任何等级或时长，
 * 更不会出现第二个 {@code addEffect} 施加点（{@code ChargeApi} 是全仓唯一的电荷施加面）。</p>
 *
 * <h2>★ 真雷：只判一次、只落一处、绝不落在施放者身上</h2>
 * <ul>
 *   <li><b>只判一次</b>：整条判定就在这一发里跑一遍 —— <b>没有 tick 循环、没有
 *       {@code @SubscribeEvent}、不看天气</b>（"雷雨天无限刷"那条担心的落地方式就是"别把这件事
 *       挂到一个每 tick/每世界 tick 的地方"，本类不是事件订阅者、也没有任何计时器）；</li>
 *   <li><b>只落一处</b>：{@code EntityType.LIGHTNING_BOLT.create(..)} 全类<b>恰好一处</b>，
 *       而且它在"每只生物"那个循环<b>外面</b> —— 作者要的是"劈一道雷"，不是"范围内每只生物各劈一道"；</li>
 *   <li><b>绝不落在施放者身上</b>：落点坐标只从射线命中点来（{@code impact}），
 *       本类<b>不读施放者的位置</b>（没有 {@code shooter.position()} / {@code shooter.getX()}）；
 *       范围查询也把施放者排除在外（同 {@code BowHitEffects#applyDisarm} 那条"自己的范围效果不落在
 *       自己身上"的既有形状）；</li>
 *   <li><b>原版闪电</b>：刻意<b>不</b>调 {@code setVisualOnly(true)} —— 作者明确选了会点燃、
 *       会破坏地形的原版闪电（视觉版闪电什么都不做）。</li>
 * </ul>
 *
 * <h2>本类<b>不</b>做什么</h2>
 * <ul>
 *   <li><b>不</b>判"该不该放"（物品侧的两个闸门：档位表 + 技能键，见
 *       {@code JadeTopazBowItem#fireThunderMightInsteadOfArrow}）；</li>
 *   <li><b>不</b>扣能量、<b>不</b>碰冷却、<b>不</b>扣耐久（耐久与原版同一笔账在物品侧扣）；</li>
 *   <li><b>不</b>造实体类型 / 模型 / 贴图 / 渲染器 / 注册项 / 语言键（发的是既有
 *       {@code minecraft:lightning_bolt} 与既有两个电荷效果）。</li>
 * </ul>
 *
 * <h2>为什么本类<b>一个数字字符都不写</b></h2>
 * <p>连"除以二"（水平半边长）与"扫掠外扩一格"都归 {@code BowThunderMightConfigs}：本批的验收里有
 * 一条负向断言是"调用点不得出现 {@code 2/3/4}、{@code 0.2/0.4/0.6} 字面量"，把口径推到"本文件
 * 一个数字字符都没有"是最强、也最容易守住的形式（连日志文本里都不写数字，免得这条判据要靠例外
 * 才成立）。</p>
 *
 * <p>⚠ <b>与其实体同类的东西不在这里</b>：波要素（重力 / 环绕 / 命中效果）一个字都没动 ——
 * 本批<b>零</b>新增波要素，{@code AbstractChargerWaveEntity} 的形状只可能是原样。</p>
 *
 * @since 1.0.0
 */
public final class BowThunderMightLauncher {

	private BowThunderMightLauncher() {
		throw new AssertionError("This class should not be instantiated");
	}

	/**
	 * <b>放一次「雷鸣神力」</b>：以命中点为中心的水平方形内，每只生物随机染一笔电荷；
	 * 并按该等级的概率，在命中点劈一道原版闪电。
	 *
	 * <p>调用前世界必须是服务端（{@code ServerLevel}）：电荷施加、闪电生成、方块射线都只在服务端
	 * 有权威结果。</p>
	 *
	 * @param world   服务端世界
	 * @param shooter 施放者（<b>只</b>用来取视线与"把自己排除在外"；本类绝不把它当落点）
	 * @param level   技能等级（1~3，越界夹取；雷鸣弓传的是 {@code BowTier#baseSkillLevel()} = 3）
	 * @return 本次被染上电荷的生物只数（供日志 / 关卡核对；闪电劈没劈看日志那一行）
	 */
	public static int strike(ServerLevel world, LivingEntity shooter, int level) {
		Vec3 look = shooter.getLookAngle().normalize();
		Vec3 from = shooter.getEyePosition();
		Vec3 to = from.add(look.scale(BowThunderMightConfigs.MAX_IMPACT_RANGE));
		Vec3 impact = pickImpact(world, shooter, from, to);

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
			"雷鸣神力：技能 {} 级 → 落点 {}（边长 {} 格的水平方形，垂直容差 {} 格），范围内 {} 只生物已随机染电（经既有电荷施加面，时长与等级按 ChargeConfigs 的表随机），真雷概率 {} ⇒ {}",
			level, impact, BowThunderMightConfigs.areaSideFor(level),
			BowThunderMightConfigs.AREA_VERTICAL_HALF_EXTENT, targets.size(), chance,
			struck ? "已劈一道原版闪电" : "未劈");
		return targets.size();
	}

	/**
	 * <b>算命中点</b>：实体射线与方块射线<b>取更近的那一个</b>，都打空则取射线末端。
	 *
	 * <p>形状照抄 {@code BoomerangImpact#checkImpact}（那条注释逐字写着为什么不能"先查实体、
	 * 没有实体再查方块"：那样会隔着墙打到墙后的生物）：两条射线都做、按到起点的距离取近者。
	 * 唯一的差别是本类只取<b>一个点</b>（不穿刺、不循环）。</p>
	 *
	 * <p>实体射线的过滤器里排除施放者，且只认<b>活着的生物</b>：掉落物 / 箭 / 船这些都不该是
	 * "雷鸣神力的命中目标"（否则对着地上的箭射一箭，落点就跑到那支箭上了）。</p>
	 */
	private static Vec3 pickImpact(ServerLevel world, LivingEntity shooter, Vec3 from, Vec3 to) {
		AABB sweep = shooter.getBoundingBox().expandTowards(to.subtract(from))
			.inflate(BowThunderMightConfigs.PICK_SWEEP_INFLATE);
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(world, shooter, from, to, sweep,
			candidate -> candidate instanceof LivingEntity && candidate != shooter && candidate.isAlive());
		BlockHitResult blockHit = world.clip(
			new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
		if (blockHit.getType() != HitResult.Type.MISS) {
			if (entityHit == null
				|| from.distanceToSqr(blockHit.getLocation()) <= from.distanceToSqr(entityHit.getLocation())) {
				return blockHit.getLocation();
			}
		}
		if (entityHit != null) {
			return entityHit.getLocation();
		}
		return to;
	}

	/**
	 * <b>在命中点劈一道真雷</b>（原版 {@code minecraft:lightning_bolt}）。
	 *
	 * <p>刻意<b>不</b>调 {@code setVisualOnly(true)}：作者明确要"真的劈一道雷"（会点燃、会破坏地形），
	 * 而视觉版闪电什么都不做。落点坐标只来自 {@code impact}（命中点）—— 本方法<b>不看</b>施放者，
	 * 所以"把自己劈了"这件事在代码里没有入口。</p>
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
