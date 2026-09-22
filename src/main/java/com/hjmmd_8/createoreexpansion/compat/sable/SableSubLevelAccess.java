package com.hjmmd_8.createoreexpansion.compat.sable;

import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge.Hit;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * sub-level 容器访问 —— 遍历、命中检测、BE 匹配与本地方块读取。
 *
 * <h2>坐标系（反编译取证，2026-09-22）</h2>
 * <p>plot（嵌入子世界）的方块坐标 = <b>真实绝对坐标</b>：plot 网格的
 * {@code plotPos} 是绝对 plot 索引（{@code SubLevelContainer.DEFAULT_ORIGIN = 10000}），
 * 一个 plot 占 {@code 2^logPlotSize} 个区块（默认 {@code DEFAULT_LOG_PLOT_SIZE = 7} → 128 区块 = 2048 方块），
 * 所以 plot 空间实测在 {@code x ≈ 2.048e7} 量级，与主角世界坐标（几千格）完全分离。</p>
 * <ul>
 *   <li>{@code LevelPlot#getChunkMin()} = {@code plotPos << logSize}，
 *       {@code getChunkMax()} = {@code ((plotPos+1) << logSize) - 1}；</li>
 *   <li>{@code LevelPlot#contains(double x, double z)} 比的正是
 *       {@code plotPos << (logSize+4)} 起的 <b>绝对水平范围</b>（不含 Y）；</li>
 *   <li>{@code EmbeddedPlotLevelAccessor#getBlockState(p)} = {@code level.getBlockState(p + getCenterBlock())}
 *       —— 即 {@link SablePose#accessorPos} 的「绝对坐标 - plot 中心」是对的。</li>
 * </ul>
 *
 * <h2>归属判据（本轮重写）</h2>
 * <p><b>权威口径</b>：Sable 自己的 {@code mixin.clip_overwrite.BlockGetterMixin#clip} 在挑
 * "命中属于哪个 sub-level"时用的就是
 * {@code Sable.HELPER.getContaining(level, localPoint) == sub}；而
 * {@code ActiveSableCompanion#getContaining(Level, x, z)} 的字节码只有两步：
 * {@code container.getPlot(Mth.floor(x) >> 4, Mth.floor(z) >> 4).getSubLevel()}。
 * 所以这里也用 {@link SubLevelContainer#getPlot(int, int)}（按区块查 plot 网格），
 * 并保留「逐结构 plot 范围 ∪ {@code LevelPlot#contains}」作为兜底。</p>
 *
 * <p><b>实测教训（2026-09-22 21:38 的真实日志）</b>：{@code locateSubLevel} 本来就是对的，
 * 真正卡住的是 {@code queryLocalBlock} 的「该处方块非空气」那一步——
 * 拾取点的 Y 正好是方块顶面（{@code loc.y = 128.00}，实心石头在 y=127），
 * {@code BlockPos.containing} 取到的是面外侧的空气块 → 判定失败。
 * 现在改为在 3×3×3 邻域里找非空气方块（与渲染器 {@code resolveLocalCenter} 同一口径）。</p>
 *
 * <p><b>加载约束</b>：本类是 common 源码，<b>故意不 import 任何 {@code net.minecraft.client.*}</b>，
 * 专用服务器上一样要能加载；容器取不到时一律返回空表/null。</p>
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
	 * <p><b>空间自适应</b>：{@code BlockGetterMixin#clip} 可能把命中留在 plot 空间
	 * （子世界分支胜出时<b>不投影回世界</b>），所以先用局部判据试一次；
	 * 局部判据不成立时才按世界坐标做位姿逆变换采样。世界坐标（几千格）不会落进
	 * plot 的绝对范围（2.048e7 量级），故主世界语义零变化。</p>
	 *
	 * @param worldLevel 主世界（波所在）或客户端世界（预览准星所在）
	 * @param worldPos   采样点（世界坐标，或已经是 plot 局部坐标）
	 * @return 命中的 sub-level；未命中返回 null
	 */
	static Hit query(Level worldLevel, Vec3 worldPos) {
		if (worldLevel == null || worldPos == null)
			return null;
		Hit local = queryLocalBlock(worldLevel, worldPos);
		if (local != null)
			return local;
		List<? extends SubLevel> subs = subLevelsOf(worldLevel);
		for (SubLevel sub : subs) {
			if (sub.isRemoved() || sub.getPlot() == null)
				continue;
			// 采样点换算到结构本地坐标（绝对 plot 坐标），反查其覆盖的方块
			Vec3 local2 = sub.logicalPose()
				.transformPositionInverse(worldPos);
			double h = 0.1;
			BlockPos min = BlockPos.containing(local2.x - h, local2.y - h, local2.z - h);
			BlockPos max = BlockPos.containing(local2.x + h, local2.y + h, local2.z + h);
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
	 * 结构<b>局部（plot）空间</b>的坐标归属查询：坐标是否落在某结构的 plot 范围内。
	 *
	 * <p><b>主判据 = Sable 自己的口径</b>：{@code SubLevelContainer#getPlot(chunkX, chunkZ)}
	 * （等价于 {@code ActiveSableCompanion#getContaining(Level, x, z)}，即
	 * {@code BlockGetterMixin#clip} 内部用来判断"这个本地点属于哪个结构"的那一句）。
	 * 它按区块索引 plot 网格，比 {@code LevelPlot#contains} 的浮点范围比较更贴近 Sable 的实际行为。</p>
	 *
	 * <p>兜底：逐个 sub-level 用 {@code LevelPlot#contains} <b>或</b>其区块范围判一次
	 * （任一成立即算命中）——主判据拿不到容器时仍然可用。</p>
	 *
	 * <p>世界坐标（几千格以内）不会落进 plot 的大数范围，故主世界调用恒为 null。</p>
	 *
	 * @param level    客户端/服务端的 Level（都实现了 SubLevelContainerHolder）
	 * @param localPos 待判定坐标（结构局部/plot 空间）
	 * @return 坐落在的结构；不在任何结构本地系内返回 null
	 */
	static Hit locateSubLevel(Level level, Vec3 localPos) {
		if (level == null || localPos == null)
			return null;
		SubLevelContainer container = SubLevelContainer.getContainer(level);
		if (container != null) {
			LevelPlot plot = container.getPlot(Mth.floor(localPos.x) >> 4, Mth.floor(localPos.z) >> 4);
			SubLevel sub = plot == null ? null : plot.getSubLevel();
			if (sub != null && !sub.isRemoved())
				return new Hit(sub);
		}
		for (SubLevel sub : subLevelsOf(level)) {
			if (sub.isRemoved() || sub.getPlot() == null)
				continue;
			LevelPlot plot = sub.getPlot();
			if (plot.contains(localPos) || inPlotChunks(plot, localPos))
				return new Hit(sub);
		}
		return null;
	}

	/**
	 * 结构<b>局部（plot）空间</b>的方块命中查询：该局部坐标附近确实压着非空气方块。
	 *
	 * <p>与 {@link #query} 的区别只在坐标系：本方法把入参当作已经在 plot 空间
	 * （{@code player.pick} 命中结构时的形态），因此<b>不做</b>位姿逆变换。</p>
	 *
	 * <p><b>邻域扫描是必须的</b>：命中点落在方块<b>面</b>上（{@code BlockHitResult#getLocation()}
	 * 的某个分量正好是整数边界），{@code BlockPos.containing} 会取到面外侧的空气邻块。
	 * 实测：{@code loc=20481030.28/128.00/20485128.14}、实心石头在 {@code y=127}，
	 * 取整得到 {@code y=128} 的空气块 → 只判一格会把有效命中丢掉。</p>
	 */
	static Hit queryLocalBlock(Level level, Vec3 localPos) {
		Hit hit = locateSubLevel(level, localPos);
		if (hit == null)
			return null;
		BlockPos guess = BlockPos.containing(localPos);
		if (!getBlockState(hit, guess).isAir())
			return hit;
		for (BlockPos p : BlockPos.betweenClosed(guess.offset(-1, -1, -1), guess.offset(1, 1, 1))) {
			if (!getBlockState(hit, p).isAir())
				return hit;
		}
		return null;
	}

	/** 某坐标的区块是否落在该 plot 的区块范围内（与 {@code LevelPlot#contains} 同源的区块口径）。 */
	private static boolean inPlotChunks(LevelPlot plot, Vec3 p) {
		ChunkPos min = plot.getChunkMin();
		ChunkPos max = plot.getChunkMax();
		int cx = Mth.floor(p.x) >> 4;
		int cz = Mth.floor(p.z) >> 4;
		return cx >= min.x && cx <= max.x && cz >= min.z && cz <= max.z;
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
