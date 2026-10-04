package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * <b>回旋镖的命中判定：射线取近 + 命中之后的两条处理</b>（2026-10-03 行为零变化拆分，从
 * {@code AbstractBoomerangEntity} 逐字搬出；2026-10-04 把穿刺那一半拆到 {@link BoomerangPierce}）。
 *
 * <p>本类管两件事：① 两种飞行模式共用的<b>唯一</b>命中入口（实体射线 ⇄ 方块射线取近，
 * {@link #checkImpact(AbstractBoomerangEntity)}）；② 命中生物 / 命中方块之后的耐久记账与容器处理
 * （{@link #onHitEntity(AbstractBoomerangEntity, Entity)} / {@link #onHitBlock(AbstractBoomerangEntity, BlockPos)}）。</p>
 *
 * <p>⚠ <b>"穿过去还是掉头"与穿刺额度的初始化不在本类</b>（2026-10-04 拆出）：唯一判据
 * {@link BoomerangPierce#turnAroundIfNotPiercing(AbstractBoomerangEntity, int, boolean)} 与额度的惰性
 * 初始化都住在 {@link BoomerangPierce} —— 本类的两条命中路径各自把结果交给它，所以"花瓣段优先"
 * 这条规则在代码里仍然<b>只有一处</b>。批 3 的完整口径（额度 {@code 3L} / {@code 5L}、三条规则、
 * "撞上挖不动的方块不掉额度但会掉头"）也随它搬到了那一类的类注释第六节。</p>
 *
 * <p>实际破坏方块（单格 / 十字）在 {@link BoomerangMining}，容器取物在
 * {@link BoomerangContainerLoot}。全部方法是<b>无状态静态</b>，第一个参数 {@code host} 就是那只镖。</p>
 */
final class BoomerangImpact {

	private BoomerangImpact() {
	}

	/**
	 * 去程命中判定（需求 2）：实体射线 + 方块射线，<b>取更近的那一个</b>。
	 *
	 * <p>⚠ 与需求原文的差异（我独创的一处）：原文是"先查实体，没实体再查方块"。那样会
	 * <b>隔着墙打到墙后的生物</b>（Quark 的镖本来不伤害生物，所以它无所谓；我们加了伤害就有关了）。
	 * 两条射线都做、按距离取近的，才既打得到生物又不穿墙。</p>
	 *
	 * <p>循环（"交替"）：打完一只生物后从命中点继续往前查，一 tick 内可以连续穿刺多只，
	 * 上限 {@link AbstractBoomerangEntity#MAX_IMPACT_LOOPS}；同一只生物靠 {@code entitiesHit} 去重。</p>
	 *
	 * <p><b>批 3：这是两种飞行模式共用的唯一命中入口</b>（点按的 {@link BoomerangFlight#tickOutbound} 与
	 * 花瓣段的 {@link BoomerangFlight#tickPetal} 都调它）——"穿过还是掉头""吃不吃额度""花瓣段要不要提前返回"
	 * 这些分歧全部落在 {@link #onHitBlock} 与 {@link #onHitEntity} 内部，
	 * 这里<b>没有第二份射线/命中代码</b>（关卡 §29n-4 钉着这一点）。</p>
	 *
	 * @return true = 本 tick 别再前进（命中了方块，或某次命中把镖掉头了）
	 */
	static boolean checkImpact(AbstractBoomerangEntity host) {
		Vec3 motion = host.getDeltaMovement();
		if (motion.lengthSqr() < 1.0E-7D) {
			return false;
		}
		Vec3 start = host.position();
		for (int loop = 0; loop < AbstractBoomerangEntity.MAX_IMPACT_LOOPS; loop++) {
			Vec3 end = start.add(motion);
			AABB box = host.getBoundingBox().expandTowards(motion).inflate(1.0D);
			EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
				host.level(), host, start, end, box, host::canHitEntity);
			BlockHitResult blockHit = host.level().clip(
				new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host));

			boolean hasBlock = blockHit.getType() != HitResult.Type.MISS;
			boolean blockIsCloser = hasBlock && (entityHit == null
				|| start.distanceToSqr(blockHit.getLocation()) <= start.distanceToSqr(entityHit.getLocation()));

			if (blockIsCloser) {
				// 命中方块 ⇒ 由 onHitBlock 决定"穿过去（挖掉了且还有额度）"还是"停下（额度用完/挖不动）"。
				return onHitBlock(host, blockHit.getBlockPos());
			}
			if (entityHit == null) {
				return false;
			}
			if (!onHitEntity(host, entityHit.getEntity())) {
				return false; // 已经打过 ⇒ 本 tick 收手（否则同一个命中点会无限重来）
			}
			if (host.isReturning()) {
				// 这一只把生物额度用完了 ⇒ 掉头（点按段；花瓣段不会置位，见 onHitEntity）
				return true;
			}
			start = entityHit.getLocation();
		}
		// 需求 2：超上限"打日志别崩"（不抛、不递归）
		CoeCore.LOGGER.warn("[回旋镖] 单 tick 命中判定超过 {} 次，结束本次去程判定：{}", AbstractBoomerangEntity.MAX_IMPACT_LOOPS, host);
		return false;
	}

	/**
	 * 命中生物：只伤一次、记个数、吃一份穿刺额度。返回 false 表示"这只已经打过了"。
	 *
	 * <p>耐久（2026-10-02 批 2：需求 §3.8）：每命中一个生物<b>额外记一笔 −1</b>
	 * （{@link BoomerangTier#WEAR_PER_HIT}）。<b>只记账</b>——见 {@link #flightWear}：
	 * 飞行期间一次都不写回物品，回到玩家手里才由 {@link BoomerangTails#settleWear(ItemStack)} 一次结算。</p>
	 *
	 * <p><b>穿刺额度（批 3，需求 §3.5）</b>：每命中一只生物消耗一份<b>生物额度</b>
	 * （{@code 3 × 等级}，见 {@link BoomerangPierce#ensurePierceQuota(AbstractBoomerangEntity)}）。额度用完 ⇒ <b>掉头</b>
	 * （需求："额度用完即掉头"）——但<b>花瓣段除外</b>：{@code isPetalFlight()} 时只消耗额度、
	 * 绝不掉头（"必须飞完一瓣才能返回"优先）。两条判据都只在下面这一段里。</p>
	 */
	static boolean onHitEntity(AbstractBoomerangEntity host, Entity target) {
		if (target == host.getOwner() || !host.entitiesHit.add(target.getId())) {
			return false;
		}
		// 伤害源：跟能量波同一处口径（indirectMagic(this, null)），**不引 mixin、也不冒充玩家攻击**
		// （需求 9：要"玩家攻击"语义的话必须先问作者）。
		target.hurt(host.damageSources().indirectMagic(host, null), host.tier().damage());
		host.hitCount++;
		host.addFlightWear(BoomerangTier.WEAR_PER_HIT);
		// 需求 §六 推断值 #4：穿透命中同样扣耐久（上面那行已扣）。
		// ⚠ 这里照旧**无条件**记账（不在命中路径里判花瓣/点按 —— 那条分歧只属于
		// turnAroundIfNotPiercing 一处，关卡 §29n-4 钉着这条）。花瓣段不读这份额度，
		// 所以"记了但没用"是零影响（花瓣段的判据是能力范围，见那个 helper）。
		BoomerangPierce.ensurePierceQuota(host);
		host.setPierceMobsLeft(Math.max(0, host.pierceMobsLeft() - 1));
		// 额度用完即掉头（生物永远允许"穿过"⇒ mayPierceThrough = true）；
		// 花瓣段那一支由 turnAroundIfNotPiercing 内部挡掉（近程无限制）。
		BoomerangPierce.turnAroundIfNotPiercing(host, host.pierceMobsLeft(), true);
		return true;
	}

	/**
	 * 命中方块：能挖就挖（{@link BoomerangMining#mineBlock}），然后按模式与额度决定"穿过去还是掉头"
	 * （2026-10-02 批 3：需求 §3.5）。
	 *
	 * <p>本方法只做两件事：<b>尝试挖</b>（{@code destroyed = tier().crossMine() ? mineCross(pos)
	 * : mineBlock(pos)}，见类注释第十一节）与
	 * <b>扣额度</b>；"穿过去还是掉头"整条判据交给
	 * {@link BoomerangPierce#turnAroundIfNotPiercing(AbstractBoomerangEntity, int, boolean)}
	 * 那一处（生物那条路径用的是同一个方法）——于是"花瓣段优先"这条规则<b>在代码里只有一处</b>。</p>
	 *
	 * <p>⚠ <b>2026-10-03 批 7 加了前置一支</b>（作者需求：开箱取物），<b>同一天第二轮又改了它的口径</b>
	 * （见类注释第十节）：若这个方块是容器（{@link BoomerangContainerLoot#containerAt(BlockPos)} 非空）⇒
	 * <b>先</b> {@link BoomerangContainerLoot#unpackLootTables(BlockPos, Player)} 按投掷者填一次战利品表、
	 * <b>再</b> {@link BoomerangContainerLoot#lootContainer(Container)} 搬空、<b>最后</b>才
	 * {@link BoomerangMining#mineBlock(BlockPos)} 把容器方块本身也挖走；每一步的先后都有理由（见下方代码注释）。
	 * 判容器的顺序必须在挖掘<b>之前</b>。</p>
	 *
	 * <p>行为对照（四种情形 + 批 4 的第五条附加）：</p>
	 * <ol>
	 *   <li><b>容器（点按与长按都会走到）</b>：填表 → 取空 → 挖掉方块本身（挖不动则不挖），
	 *       但"穿过去还是掉头"照旧按 {@code mayPierceThrough = false} 算
	 *       ⇒ 点按掉头；花瓣近程穿过去继续飞（判定与"挖不动的方块"共用一处），
	 *       取到东西才 −1 耐久且<b>整次命中只记一次</b>、不吃方块额度；</li>
	 *   <li><b>花瓣段</b>：挖掉了就吃一份额度，然后<b>一律不掉头</b>（"必须飞完一瓣才能返回"
	 *       优先于"额度用完"）；挖不动的方块也穿过——花瓣曲线是固定路径，与批 2 的花瓣段行为一致；</li>
	 *   <li><b>点按 + 挖不动</b>：{@code mayPierceThrough = false} ⇒ 撞墙，照旧掉头（批 1/2 的行为）；</li>
	 *   <li><b>点按 + 挖掉了</b>：额度没用完就<b>穿过去继续飞</b>，用完则<b>掉头</b>。</li>
	 *   <li><b>批 4 附加（只影响星界 / 雷鸣的普通支）</b>：判据 {@link BoomerangTier#crossMine()}
	 *       为 true 时上面的 {@code mineBlock(pos)} 换成 {@link BoomerangMining#mineCross(BlockPos)} ——
	 *       它多挖垂直面上的 4 个邻格，但<b>返回的仍是中心那一格的结果</b>，
	 *       所以下面这四条行为（含"额度只扣 1 份"）逐字不变；中心格是容器时走上面那一支，
	 *       十字<b>不介入</b>（邻格是容器则跳过，不挖也不开箱）。</li>
	 * </ol>
	 *
	 * <p>额度只在<b>真的挖掉</b>时消耗：挖不动的方块不消耗额度（否则"额度被挖不穿的墙吃掉"
	 * 会让玩家少穿透几个能挖的方块）。</p>
	 *
	 * @return {@code true} = 已转入回程（调用方不得再前进）；挖穿且还有额度时是 {@code false}
	 */
	static boolean onHitBlock(AbstractBoomerangEntity host, BlockPos pos) {
		// ★ 2026-10-03 批 7（作者需求：开箱取物）+ 同日第二轮改口径（见类注释第十节）：
		// **先判容器、再谈挖掘** —— 顺序不能反：反了就会先把箱子挖掉，而 ChestBlock#onRemove →
		// Containers.dropContentsOnDestroy 会把里面**还没取走**的东西撒一地（而且那条路会走
		// getItem(...) → unpackLootTable(null)，等于把战利品表按"没有玩家"roll 一遍）。
		// 本功能要的是"先按投掷者本人填一次战利品表 ⇒ 取空 ⇒ 再把容器方块本身也挖走"。
		// 点按与长按都走这一处（两种模式共用 checkImpact()，批 3 起就是同一条命中入口）。
		Container container = BoomerangContainerLoot.containerAt(host, pos);
		if (container != null) {
			BoomerangPierce.ensurePierceQuota(host);
			Player thrower = host.getOwner() instanceof Player owner ? owner : null;
			// ★ ① 主动填一次战利品表（**以投掷者本人为玩家**）：1.21.1 只在"玩家打开容器"
			// 那一刻才 unpackLootTable，镖是隔着老远开箱的，所以这一步必须自己来。
			BoomerangContainerLoot.unpackLootTables(host, pos, thrower);
			// ★ ② 逐槽取空（**取物先于挖掘**：见本方法开头那段顺序依据）。
			boolean took = BoomerangContainerLoot.lootContainer(host, container);
			// ★ ③ 取空之后才轮到"把容器方块本身也挖走"（作者第二轮要求）：走既有唯一挖掘入口
			// mineBlock(pos) ⇒ 箱子作为方块掉落物生成 ⇒ 由既有吸附带回（与掉落物同一条链）。
			// ⚠ 每玩家各自战利品表的模组容器**只取不挖**（作者第三轮要求）：见 perPlayerLoot(...)。
			boolean destroyed = false;
			if (!BoomerangContainerLoot.perPlayerLoot(host, pos)) {
				destroyed = BoomerangMining.mineBlock(host, pos);
			}
			if (took && !destroyed) {
				// ★ 耐久**只记一次**（作者 2026-10-03 裁定）：mineBlock 已经为"挖掉了一个方块"
				// 记过一笔，这里只在**这次没能挖掉**（挖不动 ⇒ 不挖 / 每玩家战利品模组 ⇒ 不挖）
				// 时补记"取物"那一笔 —— 同一次命中绝不出现两笔。
				host.addFlightWear(BoomerangTier.WEAR_PER_HIT);
			}
			// 容器对"穿过去还是掉头"这条既有判定而言照旧按"挖不动的方块"算
			// （mayPierceThrough = false，且不消耗方块额度 —— 批 7 的作者默认值不变）：
			// 点按 ⇒ 撞到即回；花瓣近程 ⇒ 穿过去继续飞完这一瓣。判定仍然只有 turnAroundIfNotPiercing 一处。
			return BoomerangPierce.turnAroundIfNotPiercing(host, host.pierceBlocksLeft(), false);
		}
		// ★ 批 4（需求 coe-boom2 §3.3）：星界 / 雷鸣镖的**固有特性** —— 十字挖掘。
		// 判据只有 BoomerangTier#crossMine() 一处；true 就走十字 helper（它内部对**每一格**
		// 仍是同一个 mineBlock 入口 ⇒ "每格各扣 1 耐久""挖不动就跳过、不记账"两条既有规则
		// 一个字不改）。下面的额度记账与掉头判定**照旧只按中心这一格的结果**算 ⇒ 额度仍只扣 1 份。
		// ⚠ 容器支（上面那一支）一个字没动：中心格是容器时走既有开箱取物，十字不介入。
		boolean destroyed = host.tier().crossMine() ? BoomerangMining.mineCross(host, pos) : BoomerangMining.mineBlock(host, pos);
		// 照旧**无条件**记账（与 onHitEntity 同形，不在命中路径里判模式：那条分歧只在
		// turnAroundIfNotPiercing 一处）。花瓣段不读这份额度 ⇒ 记了也不影响花瓣行为。
		BoomerangPierce.ensurePierceQuota(host);
		if (destroyed) {
			host.setPierceBlocksLeft(Math.max(0, host.pierceBlocksLeft() - 1));
		}
		// 唯一一处"要不要掉头"：点按段挖不动 ⇒ 掉头 / 额度用完 ⇒ 掉头 / 挖掉了且还有额度 ⇒ 穿过去；
		// 花瓣段近程一律不掉头（无限制），飞远了遇挖不动的方块才掉头（见上面那个 helper）。
		return BoomerangPierce.turnAroundIfNotPiercing(host, host.pierceBlocksLeft(), destroyed);
	}
}
