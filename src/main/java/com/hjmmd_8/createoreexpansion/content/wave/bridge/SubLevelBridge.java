package com.hjmmd_8.createoreexpansion.content.wave.bridge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Sable（sub-levels 物理结构库）桥接接口 —— 能量波与物理结构的坐标系统勾连。
 *
 * <p><b>背景</b>：航空学（Create Aeronautics）/ Sable 把玩家装配的物理结构（飞机/船/机械结构）
 * 传送到一个无穷远的虚拟子世界（sub-level）中进行物理计算，再通过位姿（Pose）映射回现实。
 * 而<b>能量波实体始终存在于真实世界</b>（主世界），因此波与结构上的机器（充能器/波闸/差波器）
 * 交互时，必须通过 sub-level 的位姿矩阵做<b>世界坐标 ↔ 本地坐标</b>换算：</p>
 * <ul>
 *   <li>发射：充能器在结构上时，把本地出生点/发射方向换算到世界坐标，波出生在主世界；</li>
 *   <li>飞行判定：波撞上结构（世界空间包围盒）时，把波位置/方向换算到本地坐标系，
 *       在结构的本地子世界中查方块/方块实体并复用现有机器判定逻辑。</li>
 * </ul>
 *
 * <p><b>实现与加载</b>：{@link SableSubLevelBridge} 引用 Sable 类型，仅在 Sable 已安装时
 * （{@code ModList.isLoaded("sable")}）由主类反射加载并注册；未装 Sable 时本接口无实现，
 * 调用方判空即可（与 Jade 可选集成的隔离模式一致）。</p>
 */
public interface SubLevelBridge {

	/** 一次命中的 sub-level 引用（内部持有 Sable 的 SubLevel 对象，不向外部泄漏类型）。 */
	record Hit(Object subLevel) {
	}

	/** Sable 是否激活（有实现且可用）。 */
	boolean isActive();

	/**
	 * 世界坐标查询：某世界位置是否落在某个 sub-level 的世界空间包围盒内。
	 * 用于波飞行时检测"是否撞上了物理结构"。
	 *
	 * @param worldLevel 主世界（波所在）
	 * @param worldPos   波中心（世界坐标）
	 * @return 命中的 sub-level；未命中返回 null
	 */
	Hit query(Level worldLevel, Vec3 worldPos);

	/**
	 * 方块实体查询：该 BE 是否位于某个 sub-level 上（充能器发射时判断出生坐标系）。
	 *
	 * @param be 方块实体（充能器/波闸等）
	 * @return 所属 sub-level；BE 不在任何结构上返回 null
	 */
	Hit ofBlockEntity(BlockEntity be);

	/** 本地坐标 → 世界坐标（位置变换）。 */
	Vec3 toWorld(Hit hit, Vec3 localPos);

	/** 世界坐标 → 本地坐标（位置逆变换）。 */
	Vec3 toLocal(Hit hit, Vec3 worldPos);

	/** 本地方向向量 → 世界方向向量（仅旋转/缩放，无平移）。 */
	Vec3 toWorldDir(Hit hit, Vec3 localDir);

	/** 世界方向向量 → 本地方向向量。 */
	Vec3 toLocalDir(Hit hit, Vec3 worldDir);

	/** 读取结构本地子世界中某位置的方块状态（主世界读不到被搬走的方块）。 */
	BlockState getBlockState(Hit hit, BlockPos localPos);

	/** 读取结构本地子世界中某位置的方块实体。 */
	BlockEntity getBlockEntity(Hit hit, BlockPos localPos);

	/** sub-level 对应的主世界 Level（波出生/飞行所在）。 */
	Level worldLevel(Hit hit);

	/**
	 * 枚举某主世界维度下全部 sub-level（结构句柄列表，用于"主世界波 × 结构"的
	 * 世界坐标↔本地坐标判定，如结构场作用 / 特斯拉线圈放电；无结构返回空表）。
	 */
	default java.util.List<Object> subLevels(net.minecraft.server.level.ServerLevel worldLevel) {
		return java.util.List.of();
	}
}
