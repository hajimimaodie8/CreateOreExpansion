package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import org.joml.Vector3f;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * <b>回旋镖的飞行段</b>（2026-10-03 行为零变化拆分，从 {@code AbstractBoomerangEntity} 逐字搬出）。
 *
 * <p>本类只管"镖怎么走"：点按的直线去程、长按的花瓣曲线、投掷原点、距离判据、回程。
 * 命中判定与掉头规则在 {@link BoomerangImpact}，破坏方块在 {@link BoomerangMining}。
 * 全部方法是<b>无状态静态</b>，第一个参数 {@code host} 就是那只镖。</p>
 *
 * <h2>五、批 2（2026-10-02）：两种飞行模式 · 花瓣曲线 · 耐久只累计、回程一次结算</h2>
 *
 * <p><b>a. 点按 = 逐字沿用现状（直线）</b>。去程仍是"实体射线 + 方块射线取近者 ⇒ 命中方块即
 * {@link AbstractBoomerangEntity#setReturning}、命中生物只伤不回头（靠 {@code entitiesHit} 去重）"，主判据仍是
 * {@link #outboundRangeExceeded}。<b>这一支一个字都没改</b>（需求 §3.2 / §5.3 陷阱 #6）。</p>
 *
 * <p><b>b. 长按 = 花瓣曲线</b>（需求 §3.4；数学与常数全在 {@link BoomerangCurveConfigs}）。
 * 分支在 {@link #tickOutbound()} 的第一行：{@code isPetalFlight() ⇒ tickPetal()}。
 * 曲线用<b>弧长参数化</b>推进（{@code Δs = v(s)/L_total}，每 tick 一次），
 * <b>s 走到 1 才 {@code setReturning(true)}</b>（"必须飞完一瓣才能返回"）；
 * 花瓣段<b>不受</b>距离判据约束（否则主人一挪步就会把花瓣从中间掐断），只剩
 * {@link AbstractBoomerangEntity#MAX_OUTBOUND_TICKS} 兜底。
 * <br>⚠ <b>批 3 修正 2</b>：花瓣段<b>也要</b>走命中判定（{@link #tickPetal} 调
 * {@link BoomerangImpact#checkImpact()} 这一处，与点按共用），所以长按同样能伤生物、挖方块、吃穿刺额度；
 * 但"必须飞完一瓣才能返回"是硬优先级 ⇒ 花瓣段<b>永不</b>因命中而提前掉头
 * （{@link BoomerangImpact#onHitBlock} / {@link BoomerangImpact#onHitEntity} 里的 {@code isPetalFlight()} 分支）。</p>
 *
 * <p>曲线的锚点是<b>出手那一刻的位置</b>（{@link AbstractBoomerangEntity#startPetalFlight} 由物品传入，
 * 走同步数据 {@link AbstractBoomerangEntity#DATA_PETAL_ORIGIN} 所以客户端也能自己算出同一条曲线），
 * 基准角 ψ 是出手那一刻玩家水平朝向的数学角 ⇒ 与需求 §3.4.3 的
 * {@code x = P.x + r(φ)·cos(ψ+φ)} 逐字同形。⚠ 需求把 {@code P} 写成"玩家位置"这个常量：
 * 出手之后玩家再走动，花瓣<b>不会</b>跟着平移（想改成跟随主人只需在 {@link #tickPetal}
 * 里把锚点换成主人的当前位置，一行）。</p>
 *
 * <p><b>c. 耐久：飞行期间只累计、不写回</b>（需求 §3.8，本类最容易做错的一处）。
 * 损耗累计在 {@link #flightWear} 上：投掷那次由物品给（点按 −2 / 长按 −5，
 * {@link BoomerangTier#throwWear(boolean)}），此后每命中一个生物 / 每挖掉一个方块各
 * {@code +}{@link BoomerangTier#WEAR_PER_HIT}。<b>整段飞行里一次都不碰物品的 {@code DAMAGE}</b>
 * （逐次写回会把耐久提前打到 0，正是 §3.8 禁止的"当场归零"）。</p>
 *
 * <p><b>d. 结算只有一处</b>（裁定 D14）：{@link BoomerangTails#collect}（交还）/
 * {@link BoomerangTails#returnTimedOut}（回程超时）/ {@link BoomerangTails#ownerGone}（主人失效）三条尾路径
 * <b>全部只调</b> {@link BoomerangTails#finishFlight(boolean)}，由它调 {@link BoomerangTails#settleWear(ItemStack)}：
 * 累计 &lt; 剩余 ⇒ 扣一次写回（活下来的镖恒有 ≥ 1 点耐久，绝不留下 0 耐久物品）；
 * 累计 ≥ 剩余 ⇒ <b>爆掉</b>（播放 {@code ITEM_BREAK} + <b>不 {@code spawnAtLocation}</b>，
 * 物品就此消失）。<b>爆掉时乘客/并入物照样先交给玩家</b>（需求 §六 推断值 #5，
 * "不给会白丢一次挖掘收益"），拿不到玩家时照旧落地、绝不销毁。冷却不受爆掉影响：
 * 冷却是在投掷那一刻就上好的（{@code BoomerangItem#releaseUsing}）。</p>
 *
 */
final class BoomerangFlight {

	private BoomerangFlight() {
	}

	// ================= 飞行数值（2026-10-04 从 AbstractBoomerangEntity 逐字搬来） =================
	//
	// 为什么住这里：这七个数字<b>只有本类用</b>（搬运前已核实：实体侧一处引用都没有，
	// 全部引用都在 {@link #tickOutbound} / {@link #tickReturning} 里）——按本仓的
	// "谁用它，它住在谁那里"判据，飞行的数就住在飞行的类。
	// 可见性同时收窄为包级（原来是 public static final，包外无引用方）。

	/** 陆地阻力。 */
	static final double AIR_DRAG = 0.99D;

	/** 水中阻力。 */
	static final double WATER_DRAG = 0.8D;

	/** 回程速度。 */
	static final double RETURN_SPEED = 0.7D;

	/** 回程速度的效率加成系数（Quark 用 Efficiency 附魔等级；本模组第一批没有附魔通道 ⇒ 恒 0）。 */
	static final double RETURN_SPEED_PER_EFFICIENCY = 0.325D;

	/**
	 * 抵达判定阈值 —— <b>「与主人的距离²」</b>（3.25 = 1.8²）。
	 *
	 * <p>需求原文写的是 {@code motion.lengthSqr() < 3.25}。逐字照抄会坏：回程速度恒为
	 * {@value #RETURN_SPEED} ⇒ {@code |motion|² = 0.49 < 3.25} 恒成立，镖会在<b>第一个回程 tick
	 * 就判定"已到达"</b>（捡不到路上的东西、也回不到玩家手上）。因此按同一组常数的几何含义读作
	 * "离主人还剩多远"（平方比较，省一次开方）。</p>
	 */
	static final double RETURN_ARRIVE_SQR = 3.25D;

	/** 抵达判定的效率加成系数（同上，恒 0）。 */
	static final double RETURN_ARRIVE_SQR_PER_EFFICIENCY = 0.25D;

	/** 本模组第一批的效率加成恒 0（没有附魔通道；留着是为了让公式与 Quark 逐字对应）。 */
	static final double RETURN_EFFICIENCY = 0.0D;

	/**
	 * <b>花瓣段专用的"能力范围"判据</b>（作者 2026-10-02 第三次裁定第 3 条）：离主人
	 * <b>不超过本档"能力范围"</b>（{@link BoomerangTier#capabilityRange()}）⇒ 近程，破坏与伤害无限制。
	 *
	 * <p>⚠ <b>阈值是待作者确认的暂定值</b>：现在 {@code capabilityRange() == returnDistance()}
	 * （5/10/15/20 格）。它是<b>唯一一处</b>取值点 —— 作者回话后只改
	 * {@code BoomerangTier#capabilityRange()} 那一行，本方法一个字不用动。</p>
	 *
	 * <p>参照点与 {@link #outboundRangeExceeded} <b>同一个</b>（{@code owner.position() + (0,1,0)}）：
	 * 两条距离判据（点按的"飞太远就掉头"与花瓣的"飞远了回到上限"）必须用同一把尺子，
	 * 否则"多远算远"会有两个答案。</p>
	 */
	static boolean withinCapabilityRange(AbstractBoomerangEntity host) {
		Entity owner = host.getOwner();
		if (owner == null) {
			return false; // 拿不到主人 ⇒ 按"超出"处理（保守：不再无限制破坏）
		}
		int limit = host.tier().capabilityRange();
		return host.position().distanceToSqr(owner.position().add(0.0D, 1.0D, 0.0D)) <= (double) limit * limit;
	}

	/**
	 * 去程：命中判定 + 位移（<b>两端各跑一次</b>）。返回 true 表示"本 tick 已转入回程，别再前进"。
	 *
	 * <p><b>第一行就是模式分岔</b>（2026-10-02 批 2）：长按（花瓣曲线）走 {@link #tickPetal()}，
	 * 点按逐字沿用下面这一支（射线命中 + 阻力位移）。</p>
	 *
	 * <p>⚠ <b>批 3 修正</b>：命中判定<b>两种模式共用</b> {@link BoomerangImpact#checkImpact()} 这一处入口
	 * （花瓣段由 {@link #tickPetal()} 调它，不再"穿过生物/方块无效果"）——"掉头"与"穿过去"
	 * 的分歧只在 {@link BoomerangImpact#onHitBlock} / {@link BoomerangImpact#onHitEntity} 内部，按
	 * {@code isPetalFlight()} 与穿刺额度决定（见那两个方法）。这里点按那一支一个字没动。</p>
	 */
	static boolean tickOutbound(AbstractBoomerangEntity host) {
		if (host.isPetalFlight()) {
			return tickPetal(host);
		}
		if (!host.level().isClientSide && BoomerangImpact.checkImpact(host)) {
			return true;
		}
		Vec3 motion = host.getDeltaMovement();
		double drag = host.isInWater() ? WATER_DRAG : AIR_DRAG;
		host.setDeltaMovement(motion.scale(drag));
		host.setPos(host.getX() + motion.x, host.getY() + motion.y, host.getZ() + motion.z);
		host.rotateToMotion();
		return false;
	}

	/**
	 * <b>长按的花瓣曲线飞行</b>（需求 §3.4；数学与常数全在 {@link BoomerangCurveConfigs}）。
	 *
	 * <p>每 tick 四步，顺序固定：</p>
	 * <ol>
	 *   <li><b>命中判定</b>（<b>服务端</b>）：调<b>与点按同一处</b>的 {@link BoomerangImpact#checkImpact()}
	 *       ——花瓣段也要能伤生物、挖方块、吃穿刺额度（2026-10-02 批 3 修正 2；
	 *       批 2 这里什么都不做，等于"镖穿过生物/方块无效果"）。
	 *       ⚠ <b>掉头不归它管</b>：{@link BoomerangImpact#onHitBlock} / {@link BoomerangImpact#onHitEntity} 内部看到
	 *       {@code isPetalFlight()} 就不会掉头（"必须飞完一瓣才能返回"优先于"额度用完"），
	 *       所以这里的返回值被<b>刻意忽略</b>——花瓣段不会因为命中而提前结束；</li>
	 *   <li><b>推进弧长</b> {@code s += v(s)/L_total}（{@code Δt = 1 tick}；v(s) 关于 s=0.5 对称）；</li>
	 *   <li>{@code s ≥ 1} ⇒ <b>飞完一瓣</b>，服务端 {@link AbstractBoomerangEntity#setReturning(boolean)} 转回程，本 tick 不再前进
	 *       （"必须飞完一瓣才能返回"；客户端不写同步值，等服务端那一份推回来）；</li>
	 *   <li>否则把 {@code s} 反查成 φ（{@link BoomerangCurveConfigs#phiAt}），按
	 *       {@code P + r(φ)·(cos(ψ+φ), 0, sin(ψ+φ))} 求新位置，位移写进 {@code deltaMovement}
	 *       并 {@code setPos}（{@code updateRotation} 靠这个位移反算朝向）。</li>
	 * </ol>
	 *
	 * <p>锚点用同步数据里的 {@link AbstractBoomerangEntity#DATA_PETAL_ORIGIN}（<b>不是</b> {@link #originX}）：
	 * 前者出手时就同步给了客户端，两端才画得出同一条曲线；后者是服务端第一条 tick 记的、
	 * 给"距离判据改基准"留的后路（见 {@link #outboundRangeExceeded}）。</p>
	 *
	 * <p>命中判定必须在<b>位移之前</b>：它用的是"上一 tick 的位移向量"
	 * （点按那一支的 {@code motion} 也来自 {@code getDeltaMovement()}，同一口径）。
	 * 出手第一 tick 还没有位移（长按不走 {@code shootFromRotation}）⇒ {@code checkImpact} 里的
	 * "位移太小直接返回"会把它挡掉，这是预期行为。</p>
	 */
	static boolean tickPetal(AbstractBoomerangEntity host) {
		if (!host.level().isClientSide) {
			// 与点按共用同一处命中入口。返回值（= 是否已转入回程）在这里被刻意忽略：
			// 花瓣段唯一允许的掉头是"飞完一瓣"（下面那一支），额度用完不掉头。
			BoomerangImpact.checkImpact(host);
		}
		BoomerangCurveConfigs.Petal petal = BoomerangCurveConfigs.petal(host.tier().returnDistance());
		double next = BoomerangCurveConfigs.stepProgress(host.petalProgress, petal);
		if (next >= 1.0D) {
			host.petalProgress = 1.0D;
			if (!host.level().isClientSide) {
				host.setReturning(true); // 一瓣走完 ⇒ 回程（不是消失、也不是掉地上）
			}
			return true;
		}
		host.petalProgress = next;
		Vector3f origin = host.getEntityData().get(AbstractBoomerangEntity.DATA_PETAL_ORIGIN);
		double baseAngle = host.getEntityData().get(AbstractBoomerangEntity.DATA_PETAL_ANGLE);
		Vec3 target = new Vec3(
			origin.x() + BoomerangCurveConfigs.offsetX(next, petal.radius(), baseAngle),
			origin.y(), // 不抬升：整瓣在同一水平面内（需求 §六 推断值 #3）
			origin.z() + BoomerangCurveConfigs.offsetZ(next, petal.radius(), baseAngle));
		host.setDeltaMovement(target.subtract(host.position()));
		host.setPos(target.x, target.y, target.z);
		host.rotateToMotion();
		return false;
	}

	/**
	 * 记下投掷原点（<b>只调一次</b>：服务端第一条去程 tick）。
	 *
	 * <p>取"第一条去程 tick 的位置"而不是"投掷那一瞬间的那一点"：实体的出生点<b>就是</b>投掷点
	 * （{@code BoomerangItem#use} 里的 {@code setPos(player.getX(), player.getEyeY() - 0.1, player.getZ())}），
	 * 出生到第一次 tick 之间没有任何位移，两者等价 —— 这样<b>不必改物品类</b>，
	 * 也让"重载后不丢"只靠 NBT 这一处。</p>
	 */
	static void recordThrowOrigin(AbstractBoomerangEntity host) {
		host.originX = host.getX();
		host.originY = host.getY();
		host.originZ = host.getZ();
		host.originRecorded = true;
	}

	/**
	 * 去程"飞太远就掉头"的判据（<b>唯一判据处</b>；作者 2026-10-02 报的"扔远了会自动消失"）。
	 *
	 * <p><b>基准 = 与主人的距离</b>（作者裁定）：主人往后退，镖更早回头 —— 这是"收回距离"的
	 * 直觉读法。基准点取 {@code owner.position() + (0,1,0)}，与回程的目标点（{@link #tickReturning}）
	 * <b>是同一点</b>，于是"距离² &lt; 3.25 ⇒ 已到家"与"距离 &gt; 该档收回距离 ⇒ 掉头"共用同一参照物。</p>
	 *
	 * <p>阈值一律来自 {@link BoomerangTier#returnDistance()}（5/10/15/20 格）——<b>这里不许出现
	 * 距离字面量</b>。{@code owner} 非空由 {@link AbstractBoomerangEntity#tick()} 顶部的兜底保证（主人没了/死了先走
	 * {@code ownerGone()}：落地 + 消散，<b>不</b>走这里）。</p>
	 *
	 * <p><b>若要改成按投掷原点判</b>（原点已在 {@link #recordThrowOrigin} 记下并进 NBT）：
	 * 只改下面那一行 {@code distSqr}，换成 {@code position().distanceToSqr(originX, originY, originZ)} 即可。</p>
	 *
	 * <p><b>长按（花瓣）不参与这条判据</b>（2026-10-02 批 2）：花瓣的最远点离锚点恰好 R，
	 * 而锚点是"出手那一刻的主人"——主人但凡挪一步，这条判据就会在花瓣飞到一半时判超距、
	 * 把"必须飞完一瓣"当场掐断。所以长按直接返回 {@code false}，交给
	 * {@link AbstractBoomerangEntity#MAX_OUTBOUND_TICKS} 兜底（一瓣 ≈ 70 tick，远在 200 以内）。</p>
	 */
	static boolean outboundRangeExceeded(AbstractBoomerangEntity host, Entity owner) {
		if (host.isPetalFlight()) {
			return false;
		}
		int limit = host.tier().returnDistance();
		// ⇩ 基准行（要改成按投掷原点判，只改这一行）
		double distSqr = host.position().distanceToSqr(owner.position().add(0.0D, 1.0D, 0.0D));
		return distSqr > (double) limit * limit;
	}

	/** 回程：朝主人头顶归一化转向 + 位移；抵达就交还。 */
	static void tickReturning(AbstractBoomerangEntity host, Entity owner) {
		if (owner == null) {
			return; // 客户端可能暂时解析不到主人；服务端的 null 已在 tick() 里走兜底
		}
		Vec3 target = owner.position().add(0.0D, 1.0D, 0.0D);
		Vec3 delta = target.subtract(host.position());
		if (delta.lengthSqr() < RETURN_ARRIVE_SQR + RETURN_EFFICIENCY * RETURN_ARRIVE_SQR_PER_EFFICIENCY) {
			if (!host.level().isClientSide) {
				BoomerangTails.collect(host, owner);
			}
			return;
		}
		Vec3 step = delta.normalize().scale(RETURN_SPEED + RETURN_EFFICIENCY * RETURN_SPEED_PER_EFFICIENCY);
		host.setDeltaMovement(step);
		host.setPos(host.getX() + step.x, host.getY() + step.y, host.getZ() + step.z);
		host.rotateToMotion();
	}
}
