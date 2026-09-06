package com.hjmmd_8.createoreexpansion.compat.sable;

import com.hjmmd_8.createoreexpansion.content.wave.bridge.SableBridges;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Sable（sub-levels 物理结构）桥接实现 —— 实现 {@link SubLevelBridge} 接口的<b>编排入口</b>。
 *
 * <p><b>职责拆分</b>（本类只做接口编排，逻辑委托单一职责工具类）：</p>
 * <ul>
 *   <li>{@link SablePose} —— 位姿变换（世界↔本地坐标、plot 中心偏移）；</li>
 *   <li>{@link SableSubLevelAccess} —— 容器遍历/命中检测/BE 匹配/本地方块读取。</li>
 * </ul>
 *
 * <p><b>加载约束</b>：本类直接引用 Sable 类型，<b>只能在 Sable 已安装时被加载</b>——
 * 主类在 {@code ModList.isLoaded("sable")} 时反射 {@code Class.forName} 触发静态块注册；
 * 未装 Sable 的环境绝不触碰本类（与 {@code WaveJadePlugin} 的可选集成隔离模式一致）。</p>
 */
public final class SableSubLevelBridge implements SubLevelBridge {

	public static final SableSubLevelBridge INSTANCE = new SableSubLevelBridge();

	static {
		// 类被加载（主类 Class.forName 触发）即注册到桥接注册表
		SableBridges.set(INSTANCE);
	}

	private SableSubLevelBridge() {
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public Hit query(Level worldLevel, Vec3 worldPos) {
		return SableSubLevelAccess.query(worldLevel, worldPos);
	}

	@Override
	public Hit ofBlockEntity(BlockEntity be) {
		return SableSubLevelAccess.ofBlockEntity(be);
	}

	@Override
	public Vec3 toWorld(Hit hit, Vec3 localPos) {
		return SablePose.toWorld(hit, localPos);
	}

	@Override
	public Vec3 toLocal(Hit hit, Vec3 worldPos) {
		return SablePose.toLocal(hit, worldPos);
	}

	@Override
	public Vec3 toWorldDir(Hit hit, Vec3 localDir) {
		return SablePose.toWorldDir(hit, localDir);
	}

	@Override
	public Vec3 toLocalDir(Hit hit, Vec3 worldDir) {
		return SablePose.toLocalDir(hit, worldDir);
	}

	@Override
	public BlockState getBlockState(Hit hit, BlockPos localPos) {
		return SableSubLevelAccess.getBlockState(hit, localPos);
	}

	@Override
	public BlockEntity getBlockEntity(Hit hit, BlockPos localPos) {
		return SableSubLevelAccess.getBlockEntity(hit, localPos);
	}

	@Override
	public Level worldLevel(Hit hit) {
		return SablePose.worldLevel(hit);
	}

	@Override
	public java.util.List<Object> subLevels(net.minecraft.server.level.ServerLevel worldLevel) {
		return SableSubLevelAccess.allSubLevels(worldLevel)
			.stream()
			.map(s -> (Object) s)
			.collect(java.util.stream.Collectors.toList());
	}
}
