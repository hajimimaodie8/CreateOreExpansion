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
 * <p><b>实现与加载</b>：
 * {@link com.hjmmd_8.createoreexpansion.compat.sable.SableSubLevelBridge} 引用 Sable 类型，仅在 Sable 已安装时
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
	 * 世界坐标查询：某世界位置是否被某个 sub-level 的实体方块占据。
	 *
	 * <p>实现口径是"位姿逆变换到结构本地系后采样该处方块是否非空气"（不是包围盒判定），
	 * 因此命中点在方块面上也能命中——采样窗口覆盖面上的整数边界。</p>
	 *
	 * <p><b>入参必须是世界坐标</b>（结构本体会被位姿变换到世界空间）。若手上拿到的是
	 * {@code player.pick(...)} 的结构命中结果，它可能已经是<b>局部坐标</b>——
	 * 先过 {@link #locateSubLevel}/{@link #queryLocalBlock} 判别，别直接喂进来。</p>
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

	/**
	 * <b>结构局部（plot）空间</b>的坐标查询：该坐标是否落在某个 sub-level 的本地系内。
	 *
	 * <p><b>为什么需要它</b>：Sable 的 {@code level.clip(...)}（即 {@code player.pick(...)} 的底层）
	 * 一旦命中物理结构，返回的 {@link net.minecraft.world.phys.BlockHitResult} 里的
	 * 位置/方块坐标是<b>结构局部坐标</b>（plot 空间的大数坐标），不是世界坐标——
	 * 它内部是把射线逆变换到 plot 空间后调 {@code originalClip} 得出命中的，
	 * 全程没有把结果投影回世界。站在结构上的玩家自身位置、视线方向同样在局部空间。
	 * 因此调用方拿到"可能是局部系"的坐标时，先用本方法判定它属于哪个结构。</p>
	 *
	 * <p>判定只看 plot 的水平范围（与 {@code LevelPlot#contains} 同口径），
	 * <b>不</b>要求该处有方块；世界坐标（几千格以内）不会落在任何 plot 范围内，
	 * 所以主世界逻辑不受影响。</p>
	 *
	 * @param level    该坐标所在的 Level（客户端即 ClientLevel）
	 * @param localPos 待判定坐标（结构局部/plot 空间）
	 * @return 坐落在的结构；不在任何结构本地系内返回 null
	 */
	default Hit locateSubLevel(Level level, Vec3 localPos) {
		return null;
	}

	/**
	 * <b>结构局部（plot）空间</b>的方块命中查询：该局部坐标处确实压着结构方块。
	 *
	 * <p>用于识别"准星拾取结果已经是结构局部坐标"（Sable 的 clip 命中结构时就是这个形态），
	 * 从而把拾取点换算回世界坐标、复用统一的世界路径。</p>
	 *
	 * @param level    该坐标所在的 Level（客户端即 ClientLevel）
	 * @param localPos 待判定坐标（结构局部/plot 空间）
	 * @return 命中且该处非空气时返回所属结构；否则 null
	 */
	default Hit queryLocalBlock(Level level, Vec3 localPos) {
		return null;
	}

	/** 本地坐标 → 世界坐标（位置变换）。 */
	Vec3 toWorld(Hit hit, Vec3 localPos);

	/**
	 * 本地坐标 → 世界坐标（<b>渲染用</b>，含亚刻插值）。
	 *
	 * <p>结构在移动时，客户端每 tick 只更新一次"逻辑位姿"（{@link #toWorld} 用的就是它），
	 * 而结构本体是按<b>渲染位姿</b>（在上一 tick 与当前 tick 之间插值）画出来的。
	 * 预览框若用逻辑位姿，会稳定地领先/落后结构最多一个 tick 的位移——结构越快越明显。
	 * 所以画框要跟结构本体用同一条位姿。</p>
	 *
	 * <p>默认实现忽略插值、退化为 {@link #toWorld(Hit, Vec3)}：服务端、无客户端插值实现、
	 * 以及任何只实现本接口子集的场景，行为与不调用本方法完全一致。</p>
	 *
	 * @param partialTick 当前帧的部分刻（{@code DeltaTracker#getGameTimeDeltaPartialTick(true)}）
	 */
	default Vec3 toWorld(Hit hit, Vec3 localPos, float partialTick) {
		return toWorld(hit, localPos);
	}

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
