package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.scan;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlockEntity;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.StellarWaveMachineRegistry;
import com.hjmmd_8.createoreexpansion.util.RadiusScan;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * <b>变器扫描器</b>（2026-09 从 {@code StellarWaveTransmuterBlockEntity} 抽出，分包整理的一部分）：
 * 半径立方体内"加热源 / 加工机 / 已蓄满避雷针"的<b>纯读取</b>实现。
 *
 * <p>抽取原则：只收<b>不依赖变器自身状态</b>的扫描（加热源、加工机、避雷针）；
 * 半径解析（读能量场档位）、载荷估算、载荷源设备计数仍留在方块实体里，
 * 因为它们要读/写变器自己的字段（后续轮次继续拆）。</p>
 *
 * <p><b>两条硬性口径</b>（都是审计/实测换来的，改前先读）：</p>
 * <ol>
 *   <li><b>未加载区块一律跳过</b>：{@code Level.getBlockState/getBlockEntity} 对未加载区块会
 *       <b>同步加载</b>区块（半径 3 的 7×7×7 最多跨 4 个区块，每 8 tick 会拽入一批），
 *       所以每个循环首行都先判 {@code level.isLoaded(pos)}（与 Create 自身口径一致）。</li>
 *   <li><b>"算不算加工机"只有一处判定</b>：{@link StellarWaveMachineRegistry#isMachinery}——
 *       目录登记机器（含 Create 注液器/物品排放器这类<b>非动能</b>机）∪ 启发式动能处理机
 *       （排除轴/齿轮箱/传送带等纯传动件）。变器扫描与波侧取料排除共用它。</li>
 * </ol>
 */
public final class TransmuterScanner {

	private TransmuterScanner() {
	}

	/** 一次加热读数（档位 + 取最高档的那个热源位置；多个燃烧室不累加）。 */
	public record HeatReading(BlazeBurnerBlock.HeatLevel level, BlockPos pos) {
	}

	/**
	 * 扫描半径内点燃的烈焰燃烧室，取<b>最高</b>档（KINDLED / SEETHING / FADING…）。
	 *
	 * <p>判据与 Create 同源（{@link BlazeBurnerBlock#getHeatLevelOf}，属性缺失安全返回 NONE），
	 * 未点燃的空燃烧室不算热源。</p>
	 */
	public static HeatReading scanHeat(Level level, BlockPos origin, int radius) {
		BlazeBurnerBlock.HeatLevel[] best = { BlazeBurnerBlock.HeatLevel.NONE };
		BlockPos[] bestPos = { null };
		RadiusScan.forEachInRadius(level, origin, radius, null, pos -> {
			BlazeBurnerBlock.HeatLevel heat;
			try {
				heat = BlazeBurnerBlock.getHeatLevelOf(level.getBlockState(pos));
			} catch (Throwable ignored) {
				return; // 单个方块判定异常（可选 mod 异常等）：跳过
			}
			if (heat == BlazeBurnerBlock.HeatLevel.NONE)
				return; // 未点燃（空燃烧室）不算热源
			if (heat.ordinal() > best[0].ordinal()) {
				best[0] = heat;
				bestPos[0] = pos.immutable();
			}
		});
		return new HeatReading(best[0], bestPos[0]);
	}

	/**
	 * 半径内认可的<b>加工机位置</b>（不含变器自身格）。
	 *
	 * <p>收录条件 = {@link StellarWaveMachineRegistry#isMachinery}：目录登记 ∪ 启发式动能处理机。
	 * <b>不要求动能机</b>——Create 注液器（{@code filling}）与物品排放器（{@code emptying}）
	 * 都是 {@code SmartBlockEntity}，旧实现写死 {@code instanceof KineticBlockEntity} 导致它们
	 * 从未被扫到（2026-09 实测：水抽到了却不给铁桶注液）。</p>
	 */
	public static List<BlockPos> collectMachines(Level level, BlockPos origin, int radius) {
		List<BlockPos> list = new ArrayList<>();
		int r = Math.max(1, radius);
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r))) {
			if (pos.equals(origin))
				continue;
			if (!level.isLoaded(pos))
				continue; // 未加载区块：读方块状态/方块实体都会触发同步加载
			if (level.getBlockEntity(pos) == null)
				continue;
			if (StellarWaveMachineRegistry.isMachinery(level, pos))
				list.add(pos.immutable());
		}
		return list;
	}

	/**
	 * 半径内<b>已蓄满待释放</b>的强化避雷针位置（不含变器自身格）。
	 *
	 * <p>"只有满的才抽"由 {@code Rod#hasReadyCharge()}（进度满格 + 有待释放次数）判定；
	 * 位置必须 {@code immutable()}——{@code betweenClosed} 复用同一个可变游标。</p>
	 */
	public static List<BlockPos> collectChargedRods(Level level, BlockPos origin, int radius) {
		List<BlockPos> rods = new ArrayList<>();
		int r = Math.max(1, radius);
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r))) {
			if (pos.equals(origin))
				continue;
			if (!level.isLoaded(pos))
				continue; // 未加载区块：getBlockEntity 会触发同步加载
			if (level.getBlockEntity(pos) instanceof ReinforcedLightningRodBlockEntity rod && rod.hasReadyCharge())
				rods.add(pos.immutable());
		}
		return rods;
	}

	/** 一次"载荷源设备"读数（护目镜展示用；只报种类与数量，<b>不报坐标</b>）。 */
	public record DeviceCounts(int itemContainers, int fluidContainers, int energyStorages, int energyStoredFe) {
	}

	/**
	 * 半径内的<b>载荷源设备</b>读数：物品容器 / 流体容器 / 储能设备各自<b>台数</b>（以及储能可读电量合计）。
	 *
	 * <p>判定一律走 NeoForge 能力（capability），故"箱子、其它模组的流体抽屉/储罐、发电机与蓄电池"
	 * 天然全覆盖，不需要逐 mod 登记方块 id；口径与载荷抽取一致：跳过自身格、加工机
	 * （{@link StellarWaveMachineRegistry#isMachinery}）与工作盆（玩家在用的加工容器）——
	 * 因为波只从"非加工机的普通容器"取料（加工机内部库存与盆里的料不该被抽走）。</p>
	 *
	 * <p><b>只读</b>：储能只调 {@code getEnergyStored()}，绝不在此处抽电——真实抽取发生在波穿过的瞬间。</p>
	 */
	public static DeviceCounts scanDeviceCounts(Level level, BlockPos origin, int radius) {
		int itemContainers = 0;
		int fluidContainers = 0;
		int energyStorages = 0;
		int energyStoredFe = 0;
		int r = Math.max(1, radius);
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r))) {
			if (pos.equals(origin))
				continue;
			if (!level.isLoaded(pos))
				continue; // 未加载区块：capability 查询会触发同步加载
			if (StellarWaveMachineRegistry.isMachinery(level, pos))
				continue; // 加工机内部库存不算载荷源
			if (level.getBlockEntity(pos) instanceof BasinBlockEntity)
				continue; // 工作盆：玩家在用的加工容器
			try {
				var items = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
					pos, null);
				if (items != null && items.getSlots() > 0)
					itemContainers++;
			} catch (Throwable ignored) {
				// 单个方块能力查询异常：跳过
			}
			try {
				var tank = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
					pos, null);
				if (tank != null && tank.getTanks() > 0)
					fluidContainers++;
			} catch (Throwable ignored) {
				// 同上
			}
			try {
				var storage = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
					pos, null);
				if (storage != null) {
					energyStorages++;
					energyStoredFe += Math.max(0, storage.getEnergyStored());
				}
			} catch (Throwable ignored) {
				// 同上
			}
		}
		return new DeviceCounts(itemContainers, fluidContainers, energyStorages, energyStoredFe);
	}
}
