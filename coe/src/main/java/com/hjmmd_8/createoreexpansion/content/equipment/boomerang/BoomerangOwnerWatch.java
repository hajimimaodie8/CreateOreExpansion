package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import net.minecraft.world.entity.Entity;

/**
 * <b>主人看护：一 tick 跳变判据 + 被传送时的兜底收尾</b>（2026-10-04 行为零变化拆分，
 * 从 {@link BoomerangTails} 逐字搬出 —— 那一个类当时装了两件事：三条尾路径的收尾，
 * 与"监控主人是否被传送"；后者与本类一起独立）。
 *
 * <p>口径（作者 2026-10-02 第三次裁定第 4 条）：主人被 {@code /tp}、传送门换维度、死亡重生、
 * 末影珍珠扔走时，镖既追不上也"回不到手里"，只能飞满 {@link AbstractBoomerangEntity#MAX_RETURN_TICKS}
 * 再收尾。这里改为<b>一 tick 内主人位置跳变超过阈值就当场收尾</b>，并把镖交还玩家
 * （见 {@link #teleportRecover(AbstractBoomerangEntity)}）。</p>
 *
 * <p><b>判据的取值与理由</b>（原记在 {@code AbstractBoomerangEntity#OWNER_TELEPORT_JUMP_SQR} 上，
 * 随判据一起搬到这里；阈值本身仍是实体上那个常数，只此一处取值）：
 * 判据 = 主人一 tick 内的位置跳变超过 {@code 16} 格（{@code 16² = 256}）。
 * 玩家任何正常移动都在 4 格/tick 以内（自由落体终端速度 3.92 格/tick 就是上限，
 * 冲刺 0.28 / 鞘翅+烟花约 3.5）⇒ 16 格留了 4 倍余量；而传送（{@code /tp}、传送门换维度、
 * 死亡重生、末影珍珠 ≥ 20 格）必然远超它。
 * <br>为什么不用"与主人的距离绝对值"：主人正常跑位/飞行也能在 300 tick 的回程寿命里
 * 拉开上百格（回程只有 0.7 格/tick），那样会把"正常拉开距离"误判成传送；
 * 而"一 tick 跳变"是传送的<b>充分</b>特征（没有正常移动能做到）。</p>
 *
 * <p>全部方法是<b>无状态静态</b>，第一个参数 {@code host} 就是那只镖；上一 tick 的主人位置
 * （{@code lastOwnerX/Y/Z} / {@code lastOwnerTracked}）仍然住在实体上，只在服务端维护、不进 NBT。</p>
 */
final class BoomerangOwnerWatch {

	private BoomerangOwnerWatch() {
	}

	/**
	 * <b>主人被传送走了的兜底收尾</b>（作者 2026-10-02 第三次裁定第 4 条）。
	 *
	 * <p>判据见 {@link #ownerTeleported(AbstractBoomerangEntity, Entity)}。收尾走
	 * {@link BoomerangTails#finishFlight(AbstractBoomerangEntity, boolean)}
	 * 的 {@code landInWorld = false} 那一支：<b>乘客交给玩家、镖交给玩家</b>（原槽 → 背包 →
	 * 掉在玩家脚下），然后 {@code discard()}。<b>不掉在地上、不继续飞</b>——这正是作者原话
	 * 「自动清除该实体，并将回旋镖归还给玩家本身」。</p>
	 */
	static void teleportRecover(AbstractBoomerangEntity host) {
		CoeCore.LOGGER.debug("[回旋镖] 主人被传送（换维度或一 tick 跳变超 {} 格）⇒ 清除实体并交还玩家：{}",
			Math.sqrt(AbstractBoomerangEntity.OWNER_TELEPORT_JUMP_SQR), host);
		BoomerangTails.finishFlight(host, false);
	}

	/**
	 * <b>主人这一 tick 是不是被传送了</b>（判据与理由见类注释）。
	 *
	 * <p>两种情况算传送：① 主人<b>换了维度</b>（{@code owner.level() != level()}，
	 * 传送门 / 指令 / 重生都会命中）；② 主人一 tick 内的位置跳变超过阈值。</p>
	 *
	 * <p>本方法<b>顺带推进</b>上一 tick 位置（每次调用都记录当前点）⇒ 每个服务端 tick 必须
	 * 恰好调用一次；调用点在 {@link AbstractBoomerangEntity#tick()} 顶部，owner 兜底之后。</p>
	 */
	static boolean ownerTeleported(AbstractBoomerangEntity host, Entity owner) {
		if (owner.level() != host.level()) {
			return true;
		}
		double dx = owner.getX() - host.lastOwnerX;
		double dy = owner.getY() - host.lastOwnerY;
		double dz = owner.getZ() - host.lastOwnerZ;
		boolean tracked = host.lastOwnerTracked;
		host.lastOwnerX = owner.getX();
		host.lastOwnerY = owner.getY();
		host.lastOwnerZ = owner.getZ();
		host.lastOwnerTracked = true;
		return tracked && dx * dx + dy * dy + dz * dz > AbstractBoomerangEntity.OWNER_TELEPORT_JUMP_SQR;
	}
}
