package com.hjmmd_8.createoreexpansion.content.charger.entity;

import java.util.List;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.content.charger.payload.WavePayloadRelease;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.fluids.FluidStack;

import static com.hjmmd_8.createoreexpansion.content.charger.entity.StellarWaveEntity.craftDebug;

/**
 * <b>变体波的载荷来源表与剩余载荷释放</b>（2026-10-06 行为零变化拆分，从
 * {@link StellarWaveEntity} 的 {@code rememberPayloadSources} / {@code releasePayload} /
 * {@code payloadGatherSkip} 与其专属上限常量 {@code MAX_PAYLOAD_SOURCES} <b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>载荷从哪些容器来、剩余载荷怎么还回去</b>——</p>
 * <ol>
 *   <li>{@link #rememberSources}：去重、越靠后越新、上限 16 条的来源表（变器穿波取料与命中后就地
 *       补料共用）；</li>
 *   <li>{@link #release}：把运行态打包成一次 {@link WavePayloadRelease} 请求，并在<b>任何情况下</b>
 *       清空载荷状态（{@code finally}）——链尽消散 / 撞墙 / 寿命耗尽等任何消散路径共用；</li>
 *   <li>{@link #gatherSkip}：取料 / 余料入库 / 退还原箱共用的排除口径。</li>
 * </ol>
 *
 * <p>搬运口径：方法体逐字相同，差异只有两类 —— {@code private} → 包级私有、宿主字段被搬动
 * 逼出的 {@code host.} 限定（{@code payloadItems} / {@code payloadFluid} / {@code payloadEnergy} /
 * {@code payloadSources} / {@code lastProcessedBlock} / {@code carriedScanRadius}，
 * 六个字段仍住在 {@link StellarWaveEntity}，只是由 {@code private} 放宽到包级私有）。
 * <b>三种释放模式、排除口径、异常隔离（"绝不把异常从实体移除流程抛到服务端 tick"）与
 * {@code finally} 的兜底清理一个字未动。</b></p>
 */
final class WavePayloadDisposal {

	private WavePayloadDisposal() {
		throw new AssertionError("This class should not be instantiated");
	}

	/** 记忆的来源方块上限（超出后丢弃最旧的）。 */
	private static final int MAX_PAYLOAD_SOURCES = 16;

	/** 记住本批取料来源（去重、越靠后越新、上限 {@link #MAX_PAYLOAD_SOURCES}）。 */
	static void rememberSources(StellarWaveEntity host, List<BlockPos> taken) {
		if (taken == null || taken.isEmpty())
			return;
		for (BlockPos pos : taken) {
			BlockPos p = pos.immutable();
			host.payloadSources.remove(p);
			host.payloadSources.add(p);
		}
		while (host.payloadSources.size() > MAX_PAYLOAD_SOURCES)
			host.payloadSources.remove(0);
	}

	/**
	 * 剩余载荷释放（链尽消散 / 撞墙 / 寿命耗尽等任何消散路径共用）：把运行态打包成一次请求交给
	 * {@link WavePayloadRelease}，并在<b>任何情况下</b>清空载荷状态（{@code finally}）。
	 *
	 * <p>口径（三种模式）、排除口径（绝不进工作盆/加工机）、异常隔离都在那个类里，见其类注释
	 * 与设计文档 §11.1。这里只做"取值 + 打包 + 兜底清理"。</p>
	 */
	static void release(StellarWaveEntity host) {
		if (host.level().isClientSide)
			return;
		try {
			WavePayloadRelease.release(new WavePayloadRelease.Request(host.level(), AllConfig.wavePayloadRelease,
				host.position(), host.payloadItems, host.payloadFluid, host.payloadEnergy, host.payloadSources,
				host.lastProcessedBlock != null ? host.lastProcessedBlock : host.blockPosition(),
				Math.max(1, host.carriedScanRadius)));
		} catch (Throwable t) {
			// 第三方容器/储能的能力实现抛异常：绝不把异常从实体移除流程抛到服务端 tick
			// （2026-09 审计修复）。余料按"这次没放进去"处理。
			craftDebug("载荷释放异常（余料未能全部处置）：{}", t);
		} finally {
			host.payloadItems.clear();
			host.payloadFluid = FluidStack.EMPTY;
			host.payloadEnergy = 0;
			host.payloadSources.clear();
			host.lastProcessedBlock = null;
		}
	}

	/**
	 * "取料 / 余料入库 / 退还原箱"共用的排除口径：不打正在加工的那个方块（{@code center}）的主意；
	 * 工作盆、目录登记的加工机（含注液器/物品排放器这类<b>非动能</b>机）与动能方块一律不算可存目标。
	 * 判定实现收敛在 {@link WavePayloadRelease#isStoreTarget}（变器扫描与波侧同源）。
	 */
	static java.util.function.Predicate<BlockPos> gatherSkip(StellarWaveEntity host, BlockPos center) {
		return pos -> pos.equals(center) || !WavePayloadRelease.isStoreTarget(host.level(), pos);
	}
}
