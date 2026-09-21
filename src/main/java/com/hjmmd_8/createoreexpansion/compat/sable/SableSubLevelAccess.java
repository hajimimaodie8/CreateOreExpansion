package com.hjmmd_8.createoreexpansion.compat.sable;

import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge.Hit;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;

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
 * <p><b>命中检测</b>（query）：把波中心/准星命中点换算到结构本地坐标后反查覆盖的方块是否非空气——
 * 不依赖 {@code boundingBox()}（结构旋转/移动时滞后），也不依赖 {@code plot.contains}
 * 以外的范围 API。命中 = 采样点确实压着结构方块，与主世界判定的几何语义一致。</p>
 *
 * <p><b>双端</b>：服务端走服务端容器（波飞行判定），客户端走客户端容器
 * （方块挖掘预览的准星命中判定）——两端都经 {@code SubLevelContainer.getContainer(Level)}
 * 取容器（服务端/客户端容器都是它的子类），遍历的成员统一按基类 {@link SubLevel} 处理；
 * <b>故意不 import 任何 {@code net.minecraft.client.*}</b>：本类是 common 源码，
 * 专用服务器上不允许出现客户端类引用（未装/未就绪时容器为 null，返回空表）。</p>
 *
 * <p><b>BE 匹配</b>（ofBlockEntity）：BE 位置落在该 sub-level 的 plot 水平范围内即匹配——
 * 不能用 {@code beLevel == sub.getLevel()}（sub.getLevel() 就是主世界，误匹配所有主世界 BE）。
 * 该用途只发生在服务端，仍只遍历服务端容器。</p>
 */
final class SableSubLevelAccess {

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
	 * 任意端（服务端/客户端）的全部 sub-level；容器取不到时返回空表。
	 *
	 * <p>成员按共同基类 {@link SubLevel} 返回：位姿换算、plot 读写都只用到基类 API，
	 * 因此客户端预览能与服务端波判定共用下面同一段命中逻辑。</p>
	 */
	static List<? extends SubLevel> subLevelsOf(Level level) {
		if (level == null)
			return List.of();
		SubLevelContainer container = SubLevelContainer.getContainer(level);
		return container == null ? List.of() : container.getAllSubLevels();
	}

	/**
	 * 世界坐标命中检测：采样点落在某结构（sub-level）的实体方块上。
	 *
	 * @param worldLevel 主世界（波所在）或客户端世界（预览准星所在）
	 * @param worldPos   采样点（世界坐标）
	 * @return 命中的 sub-level；未命中返回 null
	 */
	static Hit query(Level worldLevel, Vec3 worldPos) {
		if (worldLevel == null || worldPos == null)
			return null;
		List<? extends SubLevel> subs = subLevelsOf(worldLevel);
		for (SubLevel sub : subs) {
			if (sub.isRemoved() || sub.getPlot() == null)
				continue;
			// 采样点换算到结构本地坐标（绝对 plot 坐标），反查其覆盖的方块
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
					return new Hit(sub);
				}
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
