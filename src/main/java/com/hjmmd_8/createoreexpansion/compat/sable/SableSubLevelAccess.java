package com.hjmmd_8.createoreexpansion.compat.sable;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge.Hit;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * sub-level 容器访问 —— 遍历、命中检测、BE 匹配与本地方块读取。
 *
 * <p><b>命中检测</b>（query）：把波中心换算到结构本地坐标后反查波盒覆盖的方块是否非空气——
 * 不依赖 {@code boundingBox()}（结构旋转/移动时滞后），也不依赖 {@code plot.contains}
 * 以外的范围 API。命中 = 波盒确实压着结构方块，与主世界判定的几何语义一致。</p>
 *
 * <p><b>BE 匹配</b>（ofBlockEntity）：BE 位置落在该 sub-level 的 plot 水平范围内即匹配——
 * 不能用 {@code beLevel == sub.getLevel()}（sub.getLevel() 就是主世界，误匹配所有主世界 BE）。</p>
 */
final class SableSubLevelAccess {

	/** 调试：query 探测日志节流（毫秒） */
	private static long lastProbeLog = 0;

	private SableSubLevelAccess() {
	}

	/** 某主世界维度的全部 sub-level（无容器时返回空列表）。 */
	static List<ServerSubLevel> allSubLevels(ServerLevel serverLevel) {
		SubLevelContainer container = SubLevelContainer.getContainer(serverLevel);
		if (container instanceof ServerSubLevelContainer serverContainer)
			return serverContainer.getAllSubLevels();
		return List.of();
	}

	/**
	 * 世界坐标命中检测：波中心落在某结构（sub-level）的实体方块上。
	 *
	 * @param worldLevel 主世界（波所在）
	 * @param worldPos   波中心（世界坐标）
	 * @return 命中的 sub-level；未命中返回 null
	 */
	static Hit query(Level worldLevel, Vec3 worldPos) {
		if (!(worldLevel instanceof ServerLevel serverLevel))
			return null;
		List<ServerSubLevel> subs = allSubLevels(serverLevel);
		long now = System.currentTimeMillis();
		boolean logProbe = now - lastProbeLog > 5000;
		if (logProbe && !subs.isEmpty()) {
			lastProbeLog = now;
			CreateOreExpansion.LOGGER.info("[Sable] query 探测：主世界 {} 有 {} 个 sub-level，波世界坐标 {}",
				serverLevel.dimension()
					.location(), subs.size(), worldPos);
		}
		for (ServerSubLevel sub : subs) {
			if (sub.isRemoved())
				continue;
			// 波中心换算到结构本地坐标（绝对 plot 坐标），反查波盒（半宽 0.1，波 sized 0.2）覆盖的方块
			Vec3 local = sub.logicalPose()
				.transformPositionInverse(worldPos);
			double h = 0.1;
			BlockPos min = BlockPos.containing(local.x - h, local.y - h, local.z - h);
			BlockPos max = BlockPos.containing(local.x + h, local.y + h, local.z + h);
			boolean anySolid = false;
			for (BlockPos p : BlockPos.betweenClosed(min, max)) {
				BlockState st = sub.getPlot()
					.getEmbeddedLevelAccessor()
					.getBlockState(p.subtract(SablePose.centerOf(sub)));
				if (logProbe) {
					CreateOreExpansion.LOGGER.info("[Sable]   sub {}：波本地 {}（偏移 {}）→ 方块 {} = {}",
						sub.getUniqueId(), local, p.subtract(SablePose.centerOf(sub)), p, st.getBlock()
							.getDescriptionId());
				}
				if (!st.isAir()) {
					anySolid = true;
					break;
				}
			}
			if (anySolid)
				return new Hit(sub);
		}
		return null;
	}

	/**
	 * BE 匹配：方块实体是否位于某 sub-level 上（充能器发射时判断出生坐标系）。
	 *
	 * @param be 方块实体（充能器/波闸等）
	 * @return 所属 sub-level；BE 不在任何结构上返回 null
	 */
	static Hit ofBlockEntity(BlockEntity be) {
		Level beLevel = be.getLevel();
		if (beLevel == null)
			return null;
		MinecraftServer server = beLevel.getServer();
		if (server == null)
			return null;
		for (ServerLevel serverLevel : server.getAllLevels()) {
			for (ServerSubLevel sub : allSubLevels(serverLevel)) {
				if (sub.isRemoved())
					continue;
				if (sub.getPlot() != null && sub.getPlot()
					.contains(Vec3.atCenterOf(be.getBlockPos()))) {
					CreateOreExpansion.LOGGER.info("[Sable] ofBlockEntity：BE@{} 落在 plot 内 → 匹配 sub {}",
						be.getBlockPos(), sub.getUniqueId());
					return new Hit(sub);
				}
			}
		}
		// 调试：未匹配（发射退化为主世界逻辑）——打印 BE 坐标与第一个 sub 的 plot 范围，便于定位
		for (ServerLevel serverLevel : server.getAllLevels()) {
			List<ServerSubLevel> subs = allSubLevels(serverLevel);
			if (!subs.isEmpty()) {
				ServerSubLevel sub = subs.get(0);
				CreateOreExpansion.LOGGER.warn("[Sable] ofBlockEntity 未匹配：BE@{} plot范围 chunk[{},{}]-[{},{}] 中心={}",
					be.getBlockPos(),
					sub.getPlot()
						.getChunkMin().x,
					sub.getPlot()
						.getChunkMin().z,
					sub.getPlot()
						.getChunkMax().x,
					sub.getPlot()
						.getChunkMax().z,
					sub.getPlot()
						.getCenterBlock());
				break;
			}
		}
		return null;
	}

	/** 读取结构本地子世界中某位置的方块状态（需减 plot 中心，见 {@link SablePose#accessorPos}）。 */
	static BlockState getBlockState(Hit hit, BlockPos localPos) {
		return SablePose.sub(hit)
			.getPlot()
			.getEmbeddedLevelAccessor()
			.getBlockState(SablePose.accessorPos(hit, localPos));
	}

	/** 读取结构本地子世界中某位置的方块实体。 */
	static BlockEntity getBlockEntity(Hit hit, BlockPos localPos) {
		return SablePose.sub(hit)
			.getPlot()
			.getEmbeddedLevelAccessor()
			.getBlockEntity(SablePose.accessorPos(hit, localPos));
	}
}
