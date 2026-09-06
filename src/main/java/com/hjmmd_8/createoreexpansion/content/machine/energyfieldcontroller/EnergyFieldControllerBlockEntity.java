package com.hjmmd_8.createoreexpansion.content.machine.energyfieldcontroller;

import com.hjmmd_8.createoreexpansion.content.machine.energyfieldcontroller.EnergyFieldControllerBlock;

import java.util.List;

import com.hjmmd_8.createoreexpansion.common.AllBlocks;
import com.hjmmd_8.createoreexpansion.content.energyfield.EnergyField;
import com.hjmmd_8.createoreexpansion.content.energyfield.EnergyFieldType;
import com.hjmmd_8.createoreexpansion.content.energyfield.EnergyFields;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SableBridges;
import com.hjmmd_8.createoreexpansion.content.wave.bridge.SubLevelBridge;
import com.hjmmd_8.createoreexpansion.util.GoggleUtil;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 能量场控制器方块实体：口对口配对产场 + 应力档位联动 + 蓝宝石机壳扩展。
 *
 * <p><b>配对产场（已接线）</b>：本机 <b>开盖</b> 且接入应力（有转速）时，周期性沿口方向
 * （FACING 正面）扫描：间隔 1~8 格<b>纯空气</b>、对端为<b>同款机口对口（FACING 相反）且
 * 开盖</b>、两端<b>场类型一致</b>、端口极性<b>异极相对</b>（正-正 / 负-负 不产场）
 * → 在两机之间的空气柱注册匀强场（护目镜可看到指示框）。任一条件破坏 → 场自动移除。</p>
 *
 * <p><b>档位 tier（1~4）与网络带动（已接线）</b>：机器<b>配对产场</b>时功耗按
 * {@code 基础 4 × tier × extraFactor × rpmFactor} 计，<b>tier 由所在动力网络能否带动决定</b>——
 * 场控始终想跑满 4 档（对应 4/8/12/16 SU × 倍率），但若总需求超过网络供给则自动降到
 * 带得动的最高档（多台场控共享同一网络时<b>均分剩余应力</b>，公平不震荡）。
 * 未配对/未开盖/未接入应力 → 待机档位 1（仅最低基础消耗）。</p>
 *
 * <p><b>蓝宝石机壳扩展场区（已接线）</b>：投影重叠扩展——沿传动轴方向看，两端各自的机器+蓝宝石机壳实体覆盖格取交集，重叠格即场截面（纵向贯穿两机之间；两端 3×3 → 场 3×3，5×5 对 3×3 → 场 3×3）。功耗占位：extraFactor = 1 + 额外场格 ÷ 16（功耗规则可另议）。</p>
 *
 * <p><b>端口极性</b>（右手螺旋定则，{@link #getPolarity()}）：按"旋转角速度向量 ω · FACING"
 * 取号——同轴同向旋转的口对口两机必然一正一负（正-正不产场自然满足）。</p>
 */
public class EnergyFieldControllerBlockEntity extends KineticBlockEntity {

	/** 配对扫描最远格数：口对口之间最多 8 格空气 → 从本机到对端方块 ≤ 9 格。 */
	private static final int MAX_SCAN = 9;

	/** 机壳腔判定：垂直场轴方向的横向半宽（横截面单侧半径，格）。 */
	private static final int CASING_RADIUS = 8;

	/** 应力档位（1~4）：配对时由所在网络带动能力自动适配；待机 = 1。 */
	private int tier = 1;

	/** 额外应力倍率（机壳腔扩展占位：1 + 额外场格 ÷ 16；无壳 = 1）。 */
	private double extraFactor = 1.0;

	/** 能量场类型（加速场 / 偏转场）：扳手右键切换，默认加速场。 */
	private EnergyFieldType fieldType = EnergyFieldType.ACCELERATION;

	/** 本机当前在能量场注册表中持有的场（配对产出；transient，不入 NBT/存档）。 */
	private EnergyField registeredField;

	/** 本机产场所在宿主：null = 真实世界（按维度注册）；非 null = Sable 结构 sub-level 对象。 */
	private Object fieldHost;

	/** 本机"想跑"的档位（仅网络带动限制、未与对端匹配取小；配对协商用，transient）。 */
	private int preferredTier = 1;

	/** 配对协商用：本机按自身网络想跑的档位（1~4）。 */
	public int getPreferredTier() {
		return preferredTier;
	}

	/** 当前接收盖是否打开（镜像 blockstate OPEN，供配对判定用）。 */
	public boolean isLidOpen() {
		return getBlockState().getValue(EnergyFieldControllerBlock.OPEN);
	}

	/** 本机当前是否正产出配对场（网络均分公平计数用；服务端每轮刷新后置位）。 */
	public boolean isFieldActive() {
		return registeredField != null;
	}

	/** 当前能量场类型。 */
	public EnergyFieldType getFieldType() {
		return fieldType;
	}

	/** 扳手切换：加速场 ↔ 偏转场，返回新类型。 */
	public EnergyFieldType cycleFieldType() {
		fieldType = fieldType == EnergyFieldType.ACCELERATION
			? EnergyFieldType.DEFLECTION
			: EnergyFieldType.ACCELERATION;
		notifyUpdate();
		return fieldType;
	}

	/**
	 * 端口极性（右手螺旋定则）——本机"口"（FACING 正面）的正负：
	 * <p>从传动轴方向看，以传动轴旋转方向握右手，拇指指向 FACING 正面 → 正极(+1)，
	 * 背离 → 负极(-1)；0 = 未接入应力（无法判定/无极性）。</p>
	 * <p>实现：转速符号 × FACING 轴向符号（角速度 ω 沿传动轴轴线，正转速的 ω 方向
	 * 与 FACING 正端同向 → +1，反向 → -1）。同轴同向旋转的口对口两机因此必为异号；
	 * 单机正/负极标签随放置朝向翻转（镜子两侧相对正负），与设计文档一致。</p>
	 *
	 * @return +1 正极 / -1 负极 / 0 无应力
	 */
	public int getPolarity() {
		float speed = getSpeed();
		if (speed == 0)
			return 0;
		BlockState state = getBlockState();
		if (!state.hasProperty(EnergyFieldControllerBlock.FACING))
			return speed > 0 ? 1 : -1;
		Direction facing = state.getValue(EnergyFieldControllerBlock.FACING);
		int axisSign = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
		return (speed > 0 ? 1 : -1) * axisSign;
	}

	public EnergyFieldControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** 当前档位（1~4）。 */
	public int getTier() {
		return tier;
	}

	/** 设置档位（钳制 1~4）并通知网络应力更新。 */
	public void setTier(int tier) {
		int next = Mth.clamp(tier, 1, 4);
		if (this.tier != next) {
			this.tier = next;
			notifyStressChange();
		}
	}

	/** 当前额外倍率（≥1）。 */
	public double getExtraFactor() {
		return extraFactor;
	}

	/** 设置额外倍率（≥1）并通知网络应力更新。 */
	public void setExtraFactor(double extraFactor) {
		double next = Math.max(1.0, extraFactor);
		if (Double.compare(this.extraFactor, next) != 0) {
			this.extraFactor = next;
			notifyStressChange();
		}
	}

	/** 应力/档位/倍率变化后：同步客户端（护目镜档位）并推送网络重算。 */
	private void notifyStressChange() {
		if (level == null || level.isClientSide)
			return;
		sendData(); // 档位/额外倍率存 BE-NBT：变化时发给客户端（护目镜显示用）
		if (hasNetwork())
			getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
	}

	/** 注册基准功耗（本机 IMPACT 注册值，默认 4.0；供能耗公式与档位适配共用）。 */
	private double impactBase() {
		return com.simibubi.create.api.stress.BlockStressValues.getImpact(getStressConfigKey());
	}

	/**
	 * 总应力消耗 = 注册基准 × 档位 × 额外倍率 × ⌈RPM ÷ 64⌉。
	 * 未接入应力（速度 ≤ 0）→ 0（对应不产场）。
	 */
	@Override
	public float calculateStressApplied() {
		float speed = Math.abs(getSpeed());
		if (speed <= 0) {
			this.lastStressApplied = 0f;
			return 0f;
		}
		int rpmFactor = Math.max(1, (int) Math.ceil(speed / 64.0));
		double impact = impactBase() * tier * extraFactor * rpmFactor;
		this.lastStressApplied = (float) impact;
		return (float) impact;
	}

	@Override
	public void tick() {
		super.tick();
		// 配对/档位/场刷新：仅服务端；每 4 tick 一次（≈200ms 响应，扫描开销可忽略）
		if (level != null && !level.isClientSide && (level.getGameTime() & 3) == 0)
			refreshPairField();
	}

	/** 方块实体所在宿主：null = 真实世界；非 null = Sable 结构 sub-level 对象（未装 Sable 恒 null）。 */
	private static Object structureHost(net.minecraft.world.level.block.entity.BlockEntity be) {
		if (be == null || be.getLevel() == null)
			return null;
		SubLevelBridge bridge = SableBridges.get();
		if (bridge == null || !bridge.isActive())
			return null;
		SubLevelBridge.Hit hit = bridge.ofBlockEntity(be);
		return hit == null ? null : hit.subLevel();
	}

	/**
	 * 一轮刷新：重算配对几何（含机壳扩展与额外倍率）→ 适配应力档位 → 幂等同步场。
	 * 配对破坏/几何变化时旧场自动移除、倍率回 1、档位回待机 1。
	 * 场按宿主注册：真实世界 → 维度表；Sable 结构 → 该结构本地表（plot 坐标）。
	 */
	private void refreshPairField() {
		PairPlan plan = computePairPlan(); // 内部会按需 setExtraFactor（变化才推网络）
		if (plan == null)
			setExtraFactor(1.0); // 配对失效：扩展倍率回退（关盖/断电/几何被拆）
		updateTierForNetwork(plan);

		EnergyField current = null;
		Object host = null;
		if (plan != null) {
			EnergyFieldControllerBlockEntity partner = plan.partner;
			host = structureHost(this);
			double strength = 4.0 * Math.min(tier, partner.getTier()); // 场强随较小档位联动
			current = new EnergyField(fieldType, plan.region, plan.direction, strength, pairSource(partner));
		}
		if (host != null)
			EnergyFields.replaceSub(host, current, registeredField);
		else
			EnergyFields.replace(level, current, registeredField);
		registeredField = current;
		fieldHost = host;
	}

	/**
	 * 配对几何计划（对配对两端完全对称：两端各自算出的结果必须逐值一致，保证
	 * {@link EnergyFields#replace} 的按值去重/变更检测成立）。
	 */
	private static final class PairPlan {
		final EnergyFieldControllerBlockEntity partner;
		final AABB region;
		final Vec3 direction;

		PairPlan(EnergyFieldControllerBlockEntity partner, AABB region, Vec3 direction) {
			this.partner = partner;
			this.region = region;
			this.direction = direction;
		}
	}

	/**
	 * 计算本机当前配对计划；条件不满足返回 null。
	 *
	 * <p>规则：本机<b>开盖</b>且<b>接入应力</b>；沿口方向 1~8 格纯空气后是<b>同款机</b>——
	 * 开盖、FACING 相反（口对口）、场类型一致、端口<b>异极相对</b>。竖直对口时再做
	 * 机壳闭合环检测：每层同口径环 → 场截面扩到环内空气区并累计额外功耗倍率。</p>
	 */
	private PairPlan computePairPlan() {
		BlockState myState = getBlockState();
		if (!(myState.getBlock() instanceof EnergyFieldControllerBlock)
			|| !myState.getValue(EnergyFieldControllerBlock.OPEN))
			return null;
		if (getSpeed() == 0)
			return null; // 无应力 → 无极性 → 不产场

		Direction dir = myState.getValue(EnergyFieldControllerBlock.FACING);
		int myPol = getPolarity();

		// 沿口扫描：首个非空气格必须是对端口（间隔纯空气 1~8 格）
		BlockPos partnerPos = null;
		for (int d = 1; d <= MAX_SCAN; d++) {
			BlockPos probe = worldPosition.relative(dir, d);
			BlockState st = level.getBlockState(probe);
			if (st.isAir())
				continue;
			if (d < 2)
				return null; // 紧贴无空气间隔 → 无场区
			if (!(st.getBlock() instanceof EnergyFieldControllerBlock)
				|| !st.getValue(EnergyFieldControllerBlock.OPEN)
				|| st.getValue(EnergyFieldControllerBlock.FACING) != dir.getOpposite())
				return null; // 中途被方块/非对口机器挡住
			partnerPos = probe;
			break;
		}
		if (partnerPos == null)
			return null; // 超过 8 格空气 → 不产场

		if (!(level.getBlockEntity(partnerPos) instanceof EnergyFieldControllerBlockEntity partner))
			return null;
		// 同侧规则（航空学兼容，2026-09）：两台机必须都真实或都在<b>同一台结构</b>上才产场——
		// 真实↔结构 / 不同结构之间不产场（场控不响应运动电磁耦合）。
		Object hostSelf = structureHost(this);
		Object hostPartner = structureHost(partner);
		if (!java.util.Objects.equals(hostSelf, hostPartner))
			return null;
		if (partner.getFieldType() != fieldType)
			return null; // 两端场类型须一致（混合类型不产场）
		int partnerPol = partner.getPolarity();
		if (partnerPol == 0 || myPol == partnerPol)
			return null; // 异极相对才产场（正-正 / 负-负 / 单端断电 → 不产）

		// ---- 基础空气柱（对称）：紧贴我口第一格 → 紧贴对端口最末格 ----
		BlockPos a = worldPosition.relative(dir, 1);
		BlockPos b = partnerPos.relative(dir, -1);
		double minX = Math.min(a.getX(), b.getX());
		double minY = Math.min(a.getY(), b.getY());
		double minZ = Math.min(a.getZ(), b.getZ());
		double maxX = Math.max(a.getX(), b.getX()) + 1;
		double maxY = Math.max(a.getY(), b.getY()) + 1;
		double maxZ = Math.max(a.getZ(), b.getZ()) + 1;

		// ---- 机壳投影重叠扩展（沿传动轴方向看：两端各自的"机器+机壳"实体覆盖取交）----
		// 场截面 = 两端投影覆盖格集合的交集（中心机器格恒在交内）；交集即场，纵向贯穿两机之间。
		int[] proj = computeCasingOverlap(dir.getAxis(), a, b,
			coordOf(worldPosition, dir.getAxis().ordinal()), coordOf(partnerPos, dir.getAxis().ordinal()));
		if (proj != null) {
			// 与基础 1×1 场柱取并（proj 的轴向范围 = 基础柱区间，并后 AABB 恰为包围盒）
			minX = Math.min(minX, proj[0]);
			minY = Math.min(minY, proj[1]);
			minZ = Math.min(minZ, proj[2]);
			maxX = Math.max(maxX, proj[3]);
			maxY = Math.max(maxY, proj[4]);
			maxZ = Math.max(maxZ, proj[5]);
			// 功耗占位（可另议）：每多 16 格扩展场格 +1 倍
			setExtraFactor(Math.max(1.0, 1.0 + proj[6] / 16.0));
		} else {
			setExtraFactor(1.0); // 无重叠扩展 → 基础 1×1 场柱
		}

		AABB region = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
		// 方向：正极机口 → 负极机口（两端同向）
		Vec3 unit = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ());
		Vec3 direction = myPol > 0 ? unit : unit.scale(-1);
		return new PairPlan(partner, region, direction);
	}

	/**
	 * 机壳投影重叠检测（用户规则：<b>沿传动轴方向看，投影重叠的部分才是场</b>）。
	 *
	 * <p>把场轴向区间（含两端机器块）内<b>所有蓝宝石机壳</b>收集起来，按"轴向离哪台机器口面
	 * 更近"归属该端；每端沿传动轴投影到横向平面得到该端"机器+机壳"的实体覆盖格集
	 * （中心机器格两端恒算）；<b>两端覆盖集的交集 = 场截面</b>，纵向贯穿两机之间的空气层。
	 * 例：两端各围 3×3 → 场 3×3（9 格）；一端 5×5 另一端 3×3 → 场 3×3。</p>
	 *
	 * @param axis      场轴
	 * @param a/b       两机口外首/末空气格（横向坐标相同，用于确定场纵向区间与横向原点）
	 * @param axSelf    本机（A 端）轴向坐标
	 * @param axPartner 对机（B 端）轴向坐标
	 * @return {@code [minX,minY,minZ,maxX,maxY,maxZ, 扩展场格数]}（AABB 含纵向空气区间；
	 *         扩展场格 = (截面重叠格数-1) × 层数）；无重叠扩展 → null
	 */
	private int[] computeCasingOverlap(Direction.Axis axis, BlockPos a, BlockPos b, int axSelf, int axPartner) {
		int ax = axis.ordinal(); // X=0 Y=1 Z=2
		int t1 = ax == 0 ? 1 : 0;
		int t2 = ax == 0 ? 2 : ax == 1 ? 2 : 1;
		int c1 = coordOf(a, t1), c2 = coordOf(a, t2); // 直射柱横向坐标（两端一致）
		int airLo = Math.min(coordOf(a, ax), coordOf(b, ax)); // 场纵向：两机之间空气层
		int airHi = Math.max(coordOf(a, ax), coordOf(b, ax));
		int scanLo = Math.min(axSelf, axPartner); // 扫描层：含两端机器块层（绕机壳圈所在）
		int scanHi = Math.max(axSelf, axPartner);
		int w = CASING_RADIUS * 2 + 1;
		int half = CASING_RADIUS;
		Block sapphire = AllBlocks.SAPPHIRE_CASING.get();

		// 每端在横向窗口上的机壳覆盖（任意扫描层出现即算）
		boolean[][] covA = new boolean[w][w];
		boolean[][] covB = new boolean[w][w];
		for (int s = scanLo; s <= scanHi; s++) {
			int dA = Math.abs(s - axSelf);
			int dB = Math.abs(s - axPartner);
			for (int i = 0; i < w; i++) {
				for (int j = 0; j < w; j++) {
					int[] wp = sliceWorld(ax, t1, t2, s, c1 + i - half, c2 + j - half);
					if (level.getBlockState(new BlockPos(wp[0], wp[1], wp[2])).getBlock() != sapphire)
						continue;
					if (dA <= dB)
						covA[i][j] = true; // 轴向离本机更近/等距 → 归本端
					if (dB <= dA)
						covB[i][j] = true;
				}
			}
		}

		// 交集 = 两端都覆盖的横向格（中心机器格两端恒在覆盖内）
		boolean[][] inter = new boolean[w][w];
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < w; j++) {
				boolean center = i == half && j == half;
				inter[i][j] = (covA[i][j] || center) && (covB[i][j] || center);
			}
		}

		// 场截面 = 交集中"含中心的最大完整矩形"：抠掉一角后形状带缺角，禁止直接按包围盒给场，
		// 收缩到仍为实心完整矩形的最大一块（面积 ≤ 重叠面积；等面积取越靠上靠左的矩形，无歧义）。
		int[] rect = largestSolidRect(inter, w, half);
		if (rect == null)
			return null; // 只剩中心 → 无扩展
		// rect = {top,bottom,left,right}（闭区间）
		int count = (rect[1] - rect[0] + 1) * (rect[3] - rect[2] + 1);
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		for (int i = rect[0]; i <= rect[1]; i++) {
			for (int j = rect[2]; j <= rect[3]; j++) {
				int[] wp = sliceWorld(ax, t1, t2, airLo, c1 + i - half, c2 + j - half);
				minX = Math.min(minX, wp[0]);
				minY = Math.min(minY, wp[1]);
				minZ = Math.min(minZ, wp[2]);
				maxX = Math.max(maxX, wp[0] + 1);
				maxY = Math.max(maxY, wp[1] + 1);
				maxZ = Math.max(maxZ, wp[2] + 1);
			}
		}
		int layers = airHi - airLo + 1;
		int extraCells = (count - 1) * layers; // 去掉中心柱后每层多出的重叠格
		return new int[] { minX, minY, minZ, maxX, maxY, maxZ, extraCells };
	}

	/**
	 * 在实心格集里找<b>包含中心</b>的最大轴对齐完整矩形（全 1 区域）。
	 *
	 * @return {@code {top,bottom,left,right}}（闭区间）；只有中心时返回 null
	 */
	private static int[] largestSolidRect(boolean[][] solid, int w, int half) {
		// 二维前缀和（rectSum == 面积 ⇔ 全实心）
		int[][] pref = new int[w + 1][w + 1];
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < w; j++) {
				pref[i + 1][j + 1] = pref[i][j + 1] + pref[i + 1][j] - pref[i][j]
					+ (solid[i][j] ? 1 : 0);
			}
		}
		int bestArea = 0;
		int[] best = null;
		// 枚举顺序自上而下、自左而右 → 等面积时取最靠上靠左的矩形（确定性、无歧义）
		for (int top = 0; top < w; top++) {
			for (int bottom = top; bottom < w; bottom++) {
				for (int left = 0; left < w; left++) {
					for (int right = left; right < w; right++) {
						if (!(top <= half && half <= bottom && left <= half && half <= right))
							continue; // 必须含中心
						int area = (bottom - top + 1) * (right - left + 1);
						if (area <= bestArea)
							continue;
						int sum = pref[bottom + 1][right + 1] - pref[top][right + 1]
							- pref[bottom + 1][left] + pref[top][left];
						if (sum == area) { // 整块全实心
							bestArea = area;
							best = new int[] { top, bottom, left, right };
						}
					}
				}
			}
		}
		if (bestArea <= 1)
			return null; // 只有中心
		return best;
	}

	/** 取 BlockPos 在指定轴（0=X/1=Y/2=Z）上的坐标分量。 */
	private static int coordOf(BlockPos p, int axisOrdinal) {
		return axisOrdinal == 0 ? p.getX() : axisOrdinal == 1 ? p.getY() : p.getZ();
	}

	/** 片坐标 → 世界坐标：s 落在场轴 ax，x1/x2 落到两个横向轴 t1/t2。 */
	private static int[] sliceWorld(int ax, int t1, int t2, int s, int x1, int x2) {
		int[] w = new int[3];
		w[ax] = s;
		w[t1] = x1;
		w[t2] = x2;
		return w;
	}

	/**
	 * 应力档位适配：功率想跑满 4 档，但总需求不得超过所在网络供给——多台正产场的场控
	 * <b>均分剩余应力</b>，其余负载先扣除。<b>配对时与对端"档位匹配"</b>：取两端各自
	 * 想跑档位的较小者（高档机自动降档；对端网络变强时两端一起升，不会卡死）。
	 * 未配对/关盖/无转速 → 待机档位 1。
	 */
	private void updateTierForNetwork(PairPlan plan) {
		if (level.isClientSide)
			return;
		boolean paired = plan != null;
		float speed = Math.abs(getSpeed());
		if (!paired || speed <= 0 || !hasNetwork()) {
			preferredTier = 1;
			if (tier != 1)
				setTier(1);
			return;
		}
		KineticNetwork net = getOrCreateNetwork();
		float cap = net.calculateCapacity();
		float otherLoad = 0f;
		int controllers = 1; // 本机
		for (KineticBlockEntity member : net.members.keySet()) {
			if (member == this)
				continue;
			if (member instanceof EnergyFieldControllerBlockEntity fc && fc.isFieldActive())
				controllers++; // 同网络其它产场场控：一起均分
			else
				otherLoad += net.getActualStressOf(member);
		}
		float budget = (cap - Math.min(otherLoad, cap)) / Math.max(1, controllers);
		double base = impactBase();
		int rpmFactor = Math.max(1, (int) Math.ceil(speed / 64.0));
		int want = 1;
		for (int t = 4; t >= 1; t--) {
			double demand = base * t * extraFactor * rpmFactor * speed; // ×转速换算成网络口径
			if (demand <= budget + 1.0E-3) {
				want = t;
				break;
			}
		}
		preferredTier = want; // 先记"自己想跑几档"（供对端读取，未做匹配取小）
		int partnerWant = Mth.clamp(plan.partner.getPreferredTier(), 1, 4);
		int target = Math.min(want, partnerWant); // 档位匹配：取较小
		if (tier != target)
			setTier(target);
	}

	/** 场源标识：两端坐标排序后拼接（两端一致；带 source 的场不入档，随 tick 重建）。 */
	private String pairSource(EnergyFieldControllerBlockEntity partner) {
		BlockPos low = worldPosition.compareTo(partner.worldPosition) <= 0 ? worldPosition : partner.worldPosition;
		BlockPos high = low == worldPosition ? partner.worldPosition : worldPosition;
		return "controller:" + low.getX() + "," + low.getY() + "," + low.getZ()
			+ "|" + high.getX() + "," + high.getY() + "," + high.getZ();
	}

	/** 机器被拆 / 区块卸载 / 存档关闭：移除本机注册的配对场（对端也会自行校正，双移除无害）。 */
	@Override
	public void invalidate() {
		if (level != null && !level.isClientSide && registeredField != null) {
			EnergyField stale = registeredField;
			Object staleHost = fieldHost;
			registeredField = null;
			fieldHost = null;
			if (staleHost != null)
				EnergyFields.replaceSub(staleHost, null, stale);
			else
				EnergyFields.replace(level, null, stale);
		}
		super.invalidate();
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.field_controller")
			.withStyle(ChatFormatting.GRAY));
		added = true;

		// 只显示接口极性 + 当前场强档位（开盖状态不显示）
		int polarity = getPolarity();
		String polKey = polarity > 0 ? "createoreexpansion.goggles.field_controller_polarity_pos"
			: polarity < 0 ? "createoreexpansion.goggles.field_controller_polarity_neg"
				: "createoreexpansion.goggles.field_controller_polarity_none";
		GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.field_controller_polarity",
				Component.translatable(polKey))
			.withStyle(polarity > 0 ? ChatFormatting.RED
				: polarity < 0 ? ChatFormatting.BLUE : ChatFormatting.DARK_GRAY));
		added = true;

		float speed = Math.abs(getSpeed());
		if (speed > 0) {
			GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.field_controller_tier", tier)
				.withStyle(ChatFormatting.AQUA));
		} else {
			GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.field_controller_idle")
				.withStyle(ChatFormatting.DARK_GRAY));
		}
		return added;
	}

	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(compound, registries, clientPacket);
		compound.putInt("Tier", tier);
		compound.putDouble("ExtraFactor", extraFactor);
		compound.putString("FieldType", fieldType.name());
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		tier = Mth.clamp(compound.getInt("Tier"), 1, 4);
		extraFactor = Math.max(1.0, compound.getDouble("ExtraFactor"));
		if (compound.contains("FieldType")) {
			try {
				fieldType = EnergyFieldType.valueOf(compound.getString("FieldType"));
			} catch (IllegalArgumentException ignored) {
				fieldType = EnergyFieldType.ACCELERATION;
			}
		}
	}
}
