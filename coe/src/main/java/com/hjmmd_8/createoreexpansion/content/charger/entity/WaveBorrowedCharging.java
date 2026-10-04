package com.hjmmd_8.createoreexpansion.content.charger.entity;

import com.hjmmd_8.createoreexpansion.content.charger.wave.BorrowedChargingSource;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.items.IItemHandler;

import static com.hjmmd_8.createoreexpansion.content.charger.entity.StellarWaveEntity.craftTrace;

/**
 * <b>变体波的"借用充能"（外部条件放行）</b>（2026-10-06 行为零变化拆分，从
 * {@link StellarWaveEntity} 的 {@code borrowedChargingSource} /
 * {@code tryBorrowedItemCharging} / {@code tryBorrowedBlockCharging} 三件<b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>这枚波此刻恰好站在谁旁边</b>——全能波自身没有充能加工，
 * 但变器读取半径内若有星辉石应力充能器，就用<b>那台充能器的发射等级</b>执行一次
 * charging 配方加工。</p>
 *
 * <p><b>语义（必须守住）</b>：这是"外部条件放行"，不是"本性改变"——<b>不改变波型</b>
 * （仍是 {@code WaveTypes.OMNI}），也<b>不让 {@code WaveType#allowsChargingProcessing()}
 * 的返回值变化</b>（那个方法回答"这是什么波"，而借用回答"波此刻恰好站在谁旁边"）。
 * 因此这里只在<b>调用点</b>判断"有没有可借的充能源"，从不把结果写回波型或任何持久状态。</p>
 *
 * <p>搬运口径：方法体逐字相同，差异只有两类 —— {@code private} → 包级私有、宿主字段被搬动
 * 逼出的 {@code host.} 限定（{@code borrowedSource} / {@code borrowedSourceTick} /
 * {@code waveOrigin} / {@code carriedScanRadius}，四个字段仍住在 {@link StellarWaveEntity}，
 * 只是由 {@code private} 放宽到包级私有）。<b>两道闸门（"普通波自己就能充能，不需要借"与
 * "每 tick 至多解析一次"）、命中即绽放消散、日志文案与参数顺序一个字未动。</b></p>
 */
final class WaveBorrowedCharging {

	private WaveBorrowedCharging() {
		throw new AssertionError("This class should not be instantiated");
	}

	/**
	 * 取本次命中的借来的充能源（<b>唯一的借用判定入口</b>：判定与取值全部委托
	 * {@link BorrowedChargingSource#resolve}，实体侧不做任何等级比较或方块扫描）。
	 */
	static BorrowedChargingSource borrowedChargingSource(StellarWaveEntity host) {
		if (host.borrowedSourceTick != host.tickCount) {
			host.borrowedSourceTick = host.tickCount;
			host.borrowedSource = BorrowedChargingSource.resolve(host.level(), host.waveOrigin,
				Math.max(1, host.carriedScanRadius));
		}
		return host.borrowedSource;
	}

	/**
	 * <b>借用充能（掉落物路径）</b>：全能波自身没有充能加工，但变器读取半径内若有星辉石应力充能器，
	 * 就用<b>那台充能器的发射等级</b>执行一次 charging 配方加工。
	 *
	 * <p><b>语义（必须守住）</b>：这是"外部条件放行"，不是"本性改变"——
	 * <b>不改变波型</b>（仍是 {@code WaveTypes.OMNI}），也<b>不让
	 * {@code WaveType#allowsChargingProcessing()} 的返回值变化</b>（那个方法回答"这是什么波"，
	 * 而借用回答"波此刻恰好站在谁旁边"）。因此这里只在<b>调用点</b>判断"有没有可借的充能源"，
	 * 从不把结果写回波型或任何持久状态。</p>
	 *
	 * <p><b>为什么加工逻辑一行都不抄</b>：充能加工的实现是 {@link ChargerWaveProcessor}
	 * （普通波那套：配方匹配 → 充能/消耗输入 → 产出），这里只是"借来等级 → 造一个处理器"，
	 * 见 {@link BorrowedChargingSource#processor(net.minecraft.world.level.Level)}。</p>
	 *
	 * <p><b>命中即消散</b>：借用走的是普通波语义——命中掉落物即绽放消散
	 * （掉落物会被加工掉的充能行为，与普通波完全一致；变体波自己的远程加工路径不受影响，
	 * 见 {@link StellarWaveEntity} 的 {@code onItemHit}）。</p>
	 *
	 * @return 是否真的完成了一次充能加工（false = 无充能源 / 无匹配配方 → 本次命中不消耗波）
	 */
	static boolean tryBorrowedItemCharging(StellarWaveEntity host, ItemEntity item) {
		// 本性闸门 + 可借来源（每 tick 至多解析一次）先判，再谈加工：
		// 普通波自己就能充能，不需要借（也不该借）；攻击波既不充能也不加工。
		if (host.getWaveType().allowsChargingProcessing())
			return false;
		BorrowedChargingSource source = borrowedChargingSource(host);
		if (source == null || !source.processor(host.level())
			.processItemEntity(item))
			return false;
		craftTrace("借用充能（掉落物）：波源 {} 半径 {} 内的充能器 [{}] 发射等级 {} → 命中 {} 完成充能加工",
			host.waveOrigin, Math.max(1, host.carriedScanRadius), source.pos(), source.waveLevel(),
			item.getItem()
				.getItem());
		ChargerWaveFx.burst(host.level(), host.position(), host.trailStyle(), host.getRenderColor());
		host.discard();
		return true;
	}

	/**
	 * <b>借用充能（方块物品槽路径）</b>：与 {@link #tryBorrowedItemCharging(StellarWaveEntity, ItemEntity)} 同一口径，
	 * 只是把槽内物品交给处理器（{@link ChargerWaveProcessor#processBlockHandler}）。
	 *
	 * <p><b>返回 {@code false} 的语义与普通波一致</b>：无论是"没有可借的充能源"还是"借到了但槽里
	 * 没有匹配的 charging 配方"，都交给基类默认路径处理（基类的本性闸门会挡住它自己的充能加工），
	 * 调用方 {@code WaveHitResolver} 随后按撞墙让波绽放消散——这就是普通波命中置物台的既有行为。</p>
	 *
	 * @return true = 已用借来的波级完成一次充能加工（调用方不再走默认路径）
	 */
	static boolean tryBorrowedBlockCharging(StellarWaveEntity host, IItemHandler handler, BlockPos pos) {
		if (host.getWaveType().allowsChargingProcessing())
			return false;
		BorrowedChargingSource source = borrowedChargingSource(host);
		if (source == null || !source.processor(host.level())
			.processBlockHandler(handler, pos))
			return false;
		craftTrace("借用充能（方块槽）：波源 {} 半径 {} 内的充能器 [{}] 发射等级 {} → 命中 {} 完成充能加工",
			host.waveOrigin, Math.max(1, host.carriedScanRadius), source.pos(), source.waveLevel(), pos);
		return true;
	}
}
