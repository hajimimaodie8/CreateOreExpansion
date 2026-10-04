package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import net.minecraft.world.entity.Entity;

/**
 * <b>回旋镖每 tick 的阶段编排</b>（2026-10-04 行为零变化拆分，从
 * {@code AbstractBoomerangEntity#tick()} 逐字搬出）。
 *
 * <p>{@code super.tick()} 留在实体里（{@code Projectile} 在那里补 {@code gameEvent(射出了)} 与
 * {@code leftOwner}，{@code Entity} 在那里补 {@code baseTick}），本类接手的是它之后的<b>阶段序列</b>。
 * 顺序固定，每一步的理由都写在下面对应的注释里（原样搬来，未改一字）：</p>
 * <ol>
 *   <li>{@code liveTime++}；</li>
 *   <li><b>主人失效兜底</b>（服务端）⇒ {@link BoomerangTails#ownerGone(AbstractBoomerangEntity)}：落地 + 消散；</li>
 *   <li><b>主人被传送兜底</b>（服务端）⇒ {@link BoomerangOwnerWatch#teleportRecover(AbstractBoomerangEntity)}：
 *       当场清除并交还玩家；</li>
 *   <li><b>回程段</b>（读同步的 {@code DATA_RETURNING}）⇒ 记 {@code returnTicks}、超
 *       {@link AbstractBoomerangEntity#MAX_RETURN_TICKS} 走 {@link BoomerangTails#returnTimedOut(AbstractBoomerangEntity)}，
 *       否则 {@link BoomerangFlight#tickReturning(AbstractBoomerangEntity, Entity)}；
 *       <b>去程段</b>⇒ 只记一次投掷原点，然后按"距离判据 → 时间上限 → 真的前进"三级走；</li>
 *   <li><b>唯一吸附点</b>{@link BoomerangPickup#pickUpItems(AbstractBoomerangEntity)} —— 去程与回程都经过它，
 *       中间<b>没有</b>早退（批 6 的修复处，理由见 {@link BoomerangPickup} 的类注释第八节）。</li>
 * </ol>
 *
 * <p>全部方法是<b>无状态静态</b>，第一个参数 {@code host} 就是那只镖。</p>
 */
final class BoomerangTick {

	private BoomerangTick() {
	}

	/**
	 * 一个服务端/客户端 tick 的完整阶段序列（{@code super.tick()} 之后）。
	 *
	 * <p>搬运口径（2026-10-04）：方法体与原 {@code AbstractBoomerangEntity#tick()} <b>逐字相同</b>，
	 * 差异只有三类 —— {@code this.} → {@code host.}、受保护字段 {@code entityData} 改走实体的公开谓词
	 * {@link AbstractBoomerangEntity#isReturning()}（受保护成员跨类不可见）、以及被搬走的私有分派壳
	 * 换成对 helper 的直呼（壳本身已删；原来的壳只是"一行转发"）。</p>
	 */
	static void tick(AbstractBoomerangEntity host) {
		host.liveTime++;

		Entity owner = host.getOwner();
		if (!host.level().isClientSide && (owner == null || !owner.isAlive())) {
			BoomerangTails.ownerGone(host);
			return;
		}

		// ★ 主人瞬移兜底（作者 2026-10-02 第三次裁定第 4 条）：/tp、传送门换维度、死亡重生…
		// 一旦发现（换维度，或一 tick 跳变超过 OWNER_TELEPORT_JUMP_SQR）就**当场收尾**：
		// 清除实体 + 把镖交还玩家（finishFlight(false)，绝不掉在地上、也不继续飞）。
		if (!host.level().isClientSide && owner != null && BoomerangOwnerWatch.ownerTeleported(host, owner)) {
			BoomerangOwnerWatch.teleportRecover(host);
			return;
		}

		if (host.isReturning()) {
			host.noPhysics = true; // 客户端也置上：它只驱动 isInWall 之类的原版分支，与我们的手写位移无关
			if (!host.level().isClientSide) {
				host.returnTicks++;
				if (host.returnTicks > AbstractBoomerangEntity.MAX_RETURN_TICKS) {
					BoomerangTails.returnTimedOut(host);
					return;
				}
			}
			BoomerangFlight.tickReturning(host, owner);
		} else {
			if (!host.level().isClientSide && !host.originRecorded) {
				BoomerangFlight.recordThrowOrigin(host); // 投掷原点只在服务端记一次，进 NBT（客户端用不到它）
			}
			if (!host.level().isClientSide && BoomerangFlight.outboundRangeExceeded(host, owner)) {
				// 主判据（作者 2026-10-02 报的"扔远了会自动消失"）：飞过本档收回距离 ⇒ 立刻掉头。
				// 不是消失、也不是掉在地上 —— 交给既有回程段（RETURNING）把镖送回主人手里。
				host.setReturning(true);
			} else if (!host.level().isClientSide && host.liveTime > AbstractBoomerangEntity.MAX_OUTBOUND_TICKS) {
				// 兜底（分工见 MAX_OUTBOUND_TICKS 的注释）：距离判据万一失效，也不许永远飞下去。
				host.setReturning(true);
			} else {
				// ⚠ 2026-10-03（批 6，作者报"长按的掉落物/经验没被带回"）：这里**不再早退**。
				// 旧写法是 `else if (去程那一步) { return; }`，而那一早退唯一的作用就是跳过
				// 下面的吸附点；"本 tick 不再前进（免得钻进墙里）"已经由去程那一步内部的位移决定。
				BoomerangFlight.tickOutbound(host);
			}
		}

		// ★★ <b>唯一吸附点</b>（批 6 的修复处，作者报的 bug）：**去程与回程都经过这里**。
		// 旧写法把它关在 `DATA_RETURNING` 里（只有回程吸附）⇒ 点按（直线去、原路回）看起来正常，
		// 长按却漏：花瓣的回程是"从花瓣终点直线飞回主人"，而花瓣终点**就是出手点 P**
		// （r(±Δθ/2) = 0，见 BoomerangCurveConfigs）⇒ 回程几乎没有路程、第一个回程 tick 就
		// collect（距离 < RETURN_ARRIVE_SQR），花瓣弧上的掉落物与经验永远等不到吸附 ——
		// 正是作者原话「掉落物都存在原地，经验也没有回」。
		// 现在两种模式共用这一个点：点按去程也顺手吸（同一条路径、同一份实现，没有第二份）。
		// `!isRemoved()`：collect → finishFlight → discard 之后旧写法还会再扫一次，
		// 那一窗口里上船的掉落物会挂到一个已被移除、再也不会 tick 的载具上（顺手堵掉这条旧缝）。
		if (!host.level().isClientSide && !host.isRemoved()) {
			BoomerangPickup.pickUpItems(host);
		}
	}
}
