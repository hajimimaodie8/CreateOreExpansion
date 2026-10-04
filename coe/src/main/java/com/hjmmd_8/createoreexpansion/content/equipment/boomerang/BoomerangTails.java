package com.hjmmd_8.createoreexpansion.content.equipment.boomerang;

import com.hjmmd_8.createoreexpansion.common.CoeCore;

import java.util.ArrayList;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.hjmmd_8.createoreexpansion.content.equipment.boomerang.item.BoomerangItem;

/**
 * <b>回旋镖的收尾：三条尾路径 + 一次耐久结算 + 交还</b>（2026-10-03 行为零变化拆分，
 * 从 {@code AbstractBoomerangEntity} 逐字搬出）。
 *
 * <p>{@code collect}（抵达主人）/ {@code returnTimedOut}（回程超时）/ {@code ownerGone}（主人失效）
 * 三条尾路径全部汇进 {@code finishFlight}，由它调 {@code settleWear} 一次结算——
 * 这是裁定 D14 的落点，搬运后仍然是<b>唯一</b>汇合处。</p>
 *
 * <p>⚠ <b>职责收口（2026-10-04）</b>：原先本类还装着"主人是否被传送"的判据与那条兜底收尾
 * （{@code ownerTeleported} / {@code teleportRecover}）—— 那是<b>监控</b>，不是<b>收尾</b>，
 * 于本批拆到 {@link BoomerangOwnerWatch}（逐字搬出，判据与阈值一个字没改）。本类现在只剩
 * "三条尾路径 + 一次耐久结算 + 交还/落地"。</p>
 *
 * <p>全部方法是<b>无状态静态</b>，第一个参数 {@code host} 就是那只镖。</p>
 */
final class BoomerangTails {

	private BoomerangTails() {
	}

	/**
	 * 抵达主人 ⇒ 走 {@link #finishFlight(boolean)}（{@code landInWorld = false}：镖交回手里）。
	 *
	 * <p>为什么本方法只留一行：需求 §3.8 的批量结算必须<b>三条尾路径共用同一处</b>，
	 * 否则"跑得比镖快"（回程超时）与"主人没了"（ownerGone）就会绕开结算，玩家可以靠跑位
	 * 逃避爆掉。语义（乘客 playerTouch 吸收、镖走原槽→背包→掉落三步）仍与批 1 逐字一致。</p>
	 */
	static void collect(AbstractBoomerangEntity host, Entity owner) {
		finishFlight(host, false);
	}

	/** 交还三步（需求 5）：原槽 → 背包 → 掉在地上。 */
	static void giveToPlayer(AbstractBoomerangEntity host, Player player, ItemStack stack) {
		Inventory inventory = player.getInventory();
		// "原槽" = 投掷时记下的那一格（主手 = 当时的快捷栏格；副手 = 40 号副手格），
		// 不是"此刻选中的格子"——飞行途中玩家换格子是常事，换过就还错地方了。
		int slot = host.slot;
		if (slot >= 0 && slot < inventory.getContainerSize() && inventory.getItem(slot).isEmpty()) {
			inventory.setItem(slot, stack);
		} else if (!inventory.add(stack)) {
			player.drop(stack, false);
		}
	}

	/**
	 * owner 失效兜底（需求 6）：主人没了/死了 ⇒ 先把自己从墙里拔出来，再走
	 * {@link #finishFlight(boolean)}（{@code landInWorld = true}：镖落在世界上）。
	 * 宁可掉在墙上，也不让物品随实体一起消失（<b>除非耐久结算判它爆掉</b>）。
	 */
	static void ownerGone(AbstractBoomerangEntity host) {
		while (host.isInWall()) {
			host.setPos(host.getX(), host.getY() + 1.0D, host.getZ());
		}
		finishFlight(host, true);
	}

	/**
	 * 回程超时的收尾（Quark bug b 的修复落点）：就地落地 + 消散。
	 *
	 * <p>不写 {@code discard()} 之外的花样：玩家跑得比镖快时，"追不上"的正确结果是
	 * <b>东西还在世界上</b>（原地掉落），而不是永远穿墙追、永不消散。</p>
	 *
	 * <p>⚠ 但<b>耐久结算照样要走</b>（2026-10-02 批 2 / 裁定 D14）：它就是那条"玩家跑得比镖快"
	 * 的路径，绕开它等于给了一条免费躲避爆掉的捷径。</p>
	 */
	static void returnTimedOut(AbstractBoomerangEntity host) {
		// ⚠ 2026-10-02 第三次裁定改了这条尾路径的落点：作者要求"**归还给玩家本身**"。
		// 旧口径（批 1/批 2：就地落地 spawnAtLocation）已作废 —— 现在与"抵达主人"走同一支
		// finishFlight(false)：镖进原槽 → 背包 → 实在放不下才掉在玩家脚下（giveToPlayer 三步），
		// 乘客一并交给玩家。耐久结算照旧（绕开它就是免费躲避爆掉的捷径）。
		CoeCore.LOGGER.debug("[回旋镖] 回程超时（{} tick）⇒ 直接交还玩家：{}", AbstractBoomerangEntity.MAX_RETURN_TICKS, host);
		finishFlight(host, false);
	}

	/**
	 * ★ <b>三条收尾路径的唯一汇合处</b>（{@link #collect} / {@link #returnTimedOut} / {@link #ownerGone}）
	 * —— 耐久在这里<b>一次</b>结算，乘客与镖本身在这里按同一条口径交付。
	 *
	 * <p>顺序固定：<b>先结算耐久</b>（判爆与写回都在 {@link #settleWear(ItemStack)}），
	 * <b>再交付乘客</b>，最后交付镖本身。</p>
	 *
	 * @param landInWorld {@code true} = 镖落在世界上（超时 / 主人没了）；{@code false} = 交还到手里
	 */
	static void finishFlight(AbstractBoomerangEntity host, boolean landInWorld) {
		ItemStack stack = host.getItemStack().copy();
		boolean exploded = settleWear(host, stack);
		Player player = host.getOwner() instanceof Player owner ? owner : null;
		if (player != null && (exploded || !landInWorld)) {
			// 交还路径：乘客交给玩家（原有语义：playerTouch 吸收）。
			// 爆掉：**并入物也必须先交给玩家**（需求 §3.8 第 4 条 + §六 推断值 #5：
			// "返回时被玩家吸收"，爆掉就不给等于白丢一次挖掘收益）。
			handPassengersToPlayer(host, player);
		} else {
			// 落地路径（或拿不到玩家）：乘客照旧落地，绝不销毁。
			dropPassengers(host);
		}
		if (!exploded && !stack.isEmpty()) {
			if (player == null || landInWorld) {
				host.spawnAtLocation(stack, 0.0F);
			} else {
				giveToPlayer(host, player, stack);
			}
		}
		// 爆掉时 stack 被丢弃在这里（**不** spawnAtLocation）：物品就此消失，见 settleWear。
		host.discard();
	}

	/**
	 * ★ <b>本次飞行的耐久结算 —— 全程唯一一次写回</b>（需求 §3.8；裁定 D14）。
	 *
	 * <p>公式：{@code remaining = 耐久上限 − DAMAGE}（= {@code BoomerangItem#getDurability}），
	 * 与累计损耗 {@link #flightWear} 相比：</p>
	 * <ul>
	 *   <li>{@code 累计 < 剩余} ⇒ {@link BoomerangItem#addWear(ItemStack, int)} 写回一次
	 *       ⇒ 结果恒 <b>≥ 1</b>（不留 0 耐久物品）；</li>
	 *   <li>{@code 累计 >= 剩余} ⇒ <b>爆掉</b>：播放 {@code ITEM_BREAK} + 返回 {@code true}。
	 *       调用方负责让物品消失（<b>不</b> {@code spawnAtLocation}）。
	 *       <br>⚠ 判据取 {@code >=} 而不是需求字面的 {@code >}：需求同一段里还钉着
	 *       "不要留下一个耐久为 0 的物品"，而 {@code 累计 == 剩余} 恰好会造出那个 0。
	 *       两条要求在这里只能保一条 ⇒ 保"绝不留 0 耐久"（活着的镖恒有 ≥ 1 点），
	 *       偏差只有"恰好扣完"这一个点，见报告 §⑥ 与待作者确认清单。</li>
	 * </ul>
	 *
	 * @return {@code true} = 镖因耐久不足爆掉（物品必须消失）
	 */
	static boolean settleWear(AbstractBoomerangEntity host, ItemStack stack) {
		if (host.flightWear <= 0 || stack.isEmpty() || !(stack.getItem() instanceof BoomerangItem boomerang)) {
			return false; // 没磨损 / 不是本模组的镖（老存档兜底）：原样交还
		}
		int remaining = boomerang.getDurability(stack);
		if (host.flightWear >= remaining) {
			host.level().playSound(null, host.getX(), host.getY(), host.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
			CoeCore.LOGGER.debug("[回旋镖] 耐久不足爆掉：累计损耗 {} ≥ 剩余 {}：{}", host.flightWear, remaining, host);
			return true;
		}
		boomerang.addWear(stack, host.flightWear); // ← 唯一一处写回（结果 ≥ 1）
		return false;
	}

	/** 把船上的乘客逐个交给玩家（{@code playerTouch} 吸收；掉落物先清掉拾取延迟，否则它什么都不做）。 */
	static void handPassengersToPlayer(AbstractBoomerangEntity host, Player player) {
		for (Entity passenger : new ArrayList<>(host.getPassengers())) {
			passenger.stopRiding();
			if (passenger instanceof ItemEntity item) {
				// 刚上船时设了拾取延迟；这里是我们主动交付，先把延迟清掉，
				// 否则 playerTouch 会因为延迟而什么都不做、东西就跟着镖一起消失了。
				item.setPickUpDelay(0);
			}
			passenger.playerTouch(player);
		}
	}

	/** 把还在船上的掉落物放回世界（别让镖一消散，乘客跟着一起蒸发）。 */
	static void dropPassengers(AbstractBoomerangEntity host) {
		for (Entity passenger : new ArrayList<>(host.getPassengers())) {
			passenger.stopRiding();
			if (passenger instanceof ItemEntity item && !item.getItem().isEmpty()) {
				host.spawnAtLocation(item.getItem(), 0.0F);
				item.discard();
			}
		}
	}
}
