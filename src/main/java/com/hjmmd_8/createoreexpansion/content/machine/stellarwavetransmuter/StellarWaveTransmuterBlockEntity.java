package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.hjmmd_8.createoreexpansion.content.charger.payload.WavePayloadGather;
import com.hjmmd_8.createoreexpansion.content.energyfield.EnergyField;
import com.hjmmd_8.createoreexpansion.content.energyfield.EnergyFields;
import com.hjmmd_8.createoreexpansion.content.machine.energyfieldcontroller.EnergyFieldControllerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.display.TransmuterGoggles;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.StellarWaveMachineRegistry;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.payload.TransmuterPayloadCollector;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.payload.TransmuterPayloadCollector.Payload;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.scan.TransmuterScanner;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.scan.TransmuterScanner.HeatReading;
import com.hjmmd_8.createoreexpansion.util.GoggleUtil;
import com.hjmmd_8.createoreexpansion.util.HeatLevelNames;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 星辉波变器方块实体：六向应力机器。
 *
 * <p><b>当前实现（阶段一：扫描骨架）</b>——完整链路（波变体携带配方、命中执行
 * 链式加工、辅料/流体/电量抽取）分阶段接入，本类先落地机器本体与扫描底座：</p>
 * <ul>
 *   <li><b>扫描半径</b>：基础 1 格；本机处于能量场（{@link EnergyFields}）内时按
 *       <b>蓝宝石能量场控制器 128 RPM 分档</b>——场源控制器转速 ≥128 → 3 格，否则 2 格；
 *       不新增场控机器，复用现有能量场控（含 source 解析回查控制器转速）；</li>
 *   <li><b>扫描内容</b>：把半径内注册表认可的加工机器（{@code registry.StellarWaveMachineRegistry}，
 *       各模组联动注册）收集为快照：数量 + 各自实时应力消耗之和；</li>
 *   <li><b>加热读数</b>（2026-09 修复）：烈焰燃烧室不是动能机器，单独扫描半径内<b>最高热档</b>
 *       （{@link BlazeBurnerBlock#getHeatLevelOf}）并随波携带——否则"变器旁点了火"在读表 /
 *       载荷 / 波执行三处都不存在（玩家反馈的"燃烧室显示没有被读取"）；</li>
 *   <li><b>载荷源设备读数</b>：按 NeoForge 能力统计半径内的物品容器 / 流体容器 / 储能设备
 *       台数（含储能合计可读电量）——护目镜只报"读取到什么设备"，<b>不显示方块坐标</b>，
 *       与"载荷数量"分开：读数是设备侧规模，载荷是波真正会带走的量；</li>
 *   <li><b>应力消耗</b>：覆写 {@link #calculateStressApplied()}——
 *       消耗 = Σ(被扫机器应力) × (1 + 0.1 × (机器数 − 1))；无加工机时回退注册基准
 *       （本机静态 IMPACT，默认 4.0）保证机器能独立空转；</li>
 *   <li>护目镜显示半径/机器数/档位；扫描状态随 NBT 同步给客户端。</li>
 * </ul>
 */
public class StellarWaveTransmuterBlockEntity extends KineticBlockEntity {

	/** 场源解析失败（非场控产场/结构场）时按强场半径处理的最小档位判定。 */
	private static final int FIELD_RADIUS_LOW = 2;
	private static final int FIELD_RADIUS_HIGH = 3;
	/** 场控转速分档阈值（RPM）：≥ 此值视为"强场"（半径 3），否则半径 2。 */
	private static final float FIELD_SPEED_THRESHOLD = 128.0F;

	/** 扫描刷新间隔（tick；8 tick ≈ 160ms 响应）。 */
	private static final int SCAN_INTERVAL = 8;

	/**
	 * 扫描刷新间隔（tick）的<b>只读访问口</b>。
	 *
	 * <p>存在意义：攻击波变态的"攻击场"要把实体查询框按"两次扫描之间波最多能走多远"外扩
	 * （见 {@code TransmuterMode} 的行程余量），这个距离必须用<b>本类真正在用的</b>那个间隔来算，
	 * 否则一旦扫描间隔被调整，外扩量就会与实际间隔失配、场开始悄悄漏波。</p>
	 *
	 * <p>所以这里开一个只读口，而不是在模式枚举里另写一个 {@code 8}——同一个数值只有一处定义，
	 * 才谈得上"改一处、全场跟随"。</p>
	 *
	 * <p><b>与场节拍的分工</b>：这个间隔是<b>读数</b>（以及查询框外扩量）用的；各模式的"场"另按
	 * {@link TransmuterMode#fieldIntervalTicks()} 跑（可以更短，见 {@link #tick()}）。外扩量继续
	 * 按本间隔算 = 比场节拍所需更大的超集，只多捞几个候选，不改变点燃口径。</p>
	 */
	public static int scanIntervalTicks() {
		return SCAN_INTERVAL;
	}

	/**
	 * <b>当前处理模式</b>（扳手右键切换）：加工波变态（穿波转换）/ 攻击波变态（对波透明 + 攻击场）。
	 * 两态各自的行为（含"要不要场、场多久扫一次"）全部封装在 {@link TransmuterMode} 里，
	 * 本类只负责"存 + 同步 + 按该模式自报的节拍问一次"。
	 * <p>落盘并随客户端包同步：护目镜面板要显示当前模式（见 {@link #write}/{@link #read}）。</p>
	 */
	private TransmuterMode mode = TransmuterMode.PROCESSING;

	/**
	 * <b>进入"口位接管期"的模式（攻击波变态）之前，玩家设的四口状态</b>
	 * （{@link StellarWaveTransmuterBlock#portMask}；默认 0 = 四口全关，与放置默认（规格 3）一致）。
	 *
	 * <p>攻击波变态一进来就把四口强制全开（规格 1）并锁死（规格 2），玩家自己设的口位在那一刻被覆盖，
	 * 所以进入前必须先把它们记下来、离开时原样还回（见 {@link #cycleMode()}；
	 * 口径由 {@link TransmuterMode#locksWavePorts()} 定界）。少了这一步，切回加工态后机器仍是四口全开
	 * ——玩家看到的与攻击态一模一样、也拿不回自己设的口位，这就是"再用扳手就切不回其它模式了"
	 * 的观感来源（2026-09-24 实测修复）。</p>
	 *
	 * <p>落盘（{@code PortsBeforeTakeover}）：区块卸载/读档时机器可能正处在攻击态，掩码不能丢。
	 * 老存档没有该键 → 0 = 四口全关（与放置默认同一取值，不会把口位凭空打开）。</p>
	 */
	private int portsBeforeTakeover;

	/** 扫描到的加工机器数量（客户端同步用）。 */
	private int scannedCount;
	/**
	 * 扫描到的机器里<b>动能机</b>的数量（客户端同步用）。
	 *
	 * <p>应力公式只按耗应力的机器算：{@code Σ(动能机应力) × (1 + 0.1 × (动能机数 − 1))}。
	 * 注液器 / 物品排放器这类非动能加工机计"机器数"（面板读数）但不计应力，也不参与该乘子。</p>
	 */
	private int scannedKineticCount;
	/** 被扫机器实时应力之和（客户端同步用；goggles 展示）。 */
	private float scannedStress;
	/** 当前扫描半径（客户端同步用）。 */
	private int scanRadius = 1;

	/**
	 * <b>攻击场的节拍计数器</b>（服务端专用；<b>不落盘、不同步</b>——场是瞬时外部条件，
	 * 计数器只是"下一次该不该扫"的本地节流，重进世界/重新加载区块后从 0 重新起算没有任何影响）。
	 *
	 * <p>存在理由：场（攻击波变态的"点燃穿过场的普通波"）的正确性上限由"两次场扫描之间波能走多远"
	 * 决定（见 {@code TransmuterMode#applyField} 的 javadoc），而重扫描那一套读数（热源 / 机器 /
	 * 设备计数 / 载荷）8 tick 一次就够。两件事的时间常数差一个量级，所以各用各的节拍：本计数器
	 * 只管场，{@link #lazyTick()} 仍按 {@link #SCAN_INTERVAL} 管读数。</p>
	 */
	private int fieldTickCounter;

	/** 最近一轮扫描到的加工机方块 id（服务端用；穿波转换时作为波属性快照）。 */
	private List<ResourceLocation> scannedIds = new ArrayList<>();

	/** 从扫描区容器抽取的辅料物品（≤5 个 / ≤5 种；服务端用，穿波时附着）。 */
	private List<ItemStack> auxItems = new ArrayList<>();
	/** 从扫描区储罐抽取的流体（≤500 mB；服务端用）。 */
	private FluidStack auxFluid = FluidStack.EMPTY;
	/** 从扫描区储能抽取的电量（FE；服务端用）。 */
	private int auxEnergy;

	/** 最近一轮扫描按"机器实时状态"解析出的应执行配方类型（服务端用；穿波时附着）。 */
	private List<com.simibubi.create.foundation.recipe.IRecipeTypeInfo> scannedRecipeTypes = new ArrayList<>();

	/**
	 * 最近一轮扫描到的<b>最高加热档位</b>（扫描半径内点燃的烈焰燃烧室；NONE = 无热源）。
	 *
	 * <p>存在意义：烈焰燃烧室<b>不是动能机器</b>（{@code BlazeBurnerBlockEntity} 不是
	 * {@link KineticBlockEntity}），{@link #collectMachines} 永远收不到它——于是"变器旁边点了火"
	 * 这件事在读表、载荷、波执行三处都不存在（玩家反馈"范围内点了火的燃烧室显示没有被读取"）。
	 * 现在热源单独扫描：读数上护目镜、随波携带、并在波侧满足 {@code HEATED}/{@code SUPERHEATED} 配方。</p>
	 */
	private BlazeBurnerBlock.HeatLevel scannedHeat = BlazeBurnerBlock.HeatLevel.NONE;

	/** 该热源所在位置（内部状态：热源换位时触发一次重扫重发；护目镜/Jade <b>不展示坐标</b>）。 */
	private BlockPos scannedHeatPos;

	/**
	 * 扫描半径内"载荷源设备"读数（护目镜展示"读取到什么设备"，<b>只报种类与数量、不报坐标</b>）：
	 * 物品容器（箱子/桶/其它模组的抽屉等，按 {@code Capabilities.ItemHandler.BLOCK}）、
	 * 流体容器（储罐/流体抽屉等，按 {@code Capabilities.FluidHandler.BLOCK}）、
	 * 储能设备（发电机/蓄电池/线圈等，按 {@code Capabilities.EnergyStorage.BLOCK}，含合计可读电量）。
	 *
	 * <p>口径与载荷抽取一致：<b>排除加工机自身</b>（见 {@link #isMachinery}），
	 * 因为波只从"非加工机的容器"取料；计数为"设备台数"，与载荷数量/电量不是一回事
	 * （空箱子也计 1 台；不可抽取的储能计台数但不进载荷）。</p>
	 */
	private int scannedItemContainers;
	private int scannedFluidContainers;
	private int scannedEnergyStorages;
	private int scannedEnergyStoredFe;

	/** 最近一轮扫描配方类型的 id 快照（客户端同步；护目镜按住 Shift 展示"绑定机器可加工配方"逐条清单）。 */
	private List<ResourceLocation> scannedTypeIds = new ArrayList<>();

	/**
	 * 最近一次被转换穿出的变体波"当时实际可执行"的配方类型 id（transient：仅客户端同步用，
	 * 不落盘；空 = 尚无波穿出记录）。穿波瞬间由 {@code StellarWaveTransmuterPass} 写入，
	 * 护目镜面板逐条展示。
	 */
	private List<ResourceLocation> lastWaveRecipeTypeIds = new ArrayList<>();

	/** 半径内"已蓄满待释放"的强化避雷针位置（服务端用；穿波时真正抽取一次机会）。 */
	private List<BlockPos> chargedRods = new ArrayList<>();

	// ===== 载荷概览（客户端护目镜显示；随 NBT 同步的纯摘要数字） =====
	/** 载荷辅料物品总数 / 种类数（摘要）。 */
	private int payloadItemCount;
	private int payloadTypeCount;
	/** 载荷流体 mB（摘要）。 */
	private int payloadFluidMb;
	/** 载荷电量 FE（摘要）。 */
	private int payloadEnergyFe;
	/** 最近解析出的配方类型数（摘要）。 */
	private int recipeTypeCount;
	/** 已蓄满避雷针机会数（摘要）。 */
	private int rodCreditCount;

	public StellarWaveTransmuterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(SCAN_INTERVAL);
	}

	// ================= 场节拍（本类唯一一处 applyField 调用点） =================

	/**
	 * <b>每 tick 推进"场节拍"</b>：到点就按当前模式跑一次该模式的"场"
	 * （{@link TransmuterMode#applyField}）——这就是本类<b>唯一</b>一处 {@code applyField} 调用。
	 *
	 * <p><b>为什么场不能挂在重扫描上</b>：{@link #refreshScan()} 那一套（热源 / 机器 / 设备计数 /
	 * 载荷估算）是<b>读数</b>，8 tick ≈ 160ms 已足够，也确实不能提速（提速等于把那整套的代价乘几倍）。
	 * 但攻击场是<b>穿过判定</b>：观测越稀，波"整段穿过场盒却没被任何一次观测看见"的风险越大。
	 * 按最早的实现（场挂在重扫描上、判定只看"上一 tick 的位移线段"）8 tick 下空档有 7 tick，
	 * 而当时的场盒棱长只有 3 格、最高波速 12 格/秒（0.6 格/tick）时正面穿场只要 5 tick &lt; 7
	 * —— 这就是"被波速调节器加速过的波在变器上可能整段穿过而不被点燃"的根因；
	 * 加入攻击场三档后最小档的场盒棱长更是只有 1 格。现在场按模式自报的节拍
	 * （{@link TransmuterMode#fieldIntervalTicks()}，攻击波变态 = 2 tick）单独跑，
	 * 重扫描仍是 {@link #SCAN_INTERVAL} tick。</p>
	 *
	 * <p><b>正确性现在不靠节拍</b>：判定读的是波自己的路径折线
	 * （{@code content.charger.entity.WavePath}，跨度 4 tick）——一次观测覆盖"自上次观测以来的
	 * 全部位移"，<b>只要观测间隔 ≤ 折线跨度就不会漏判</b>。节拍因此退化为成本参数，
	 * 2 tick 是成本上的主动选择（见 {@code TransmuterMode#FIELD_INTERVAL_FLOOR_TICKS}）。</p>
	 *
	 * <p><b>一个诚实的旁注</b>：Create 的 {@code SmartBlockEntity#tick()} 用"先比较后自减"的计数
	 * （{@code if (lazyTickCounter-- <= 0)}），所以重扫描的实际周期是
	 * {@link #SCAN_INTERVAL} + 1 = 9 tick，而非字面的 8；旧实现空档实际是 8 tick。这既不影响
	 * 本类的分工，也不影响场节拍公式（{@link TransmuterMode#fieldIntervalTicks()} 只依赖波速表、
	 * 场盒棱长与折线跨度，与重扫描周期无关），只是"旧实现漏波"这一判断的前提数字更宽松一点。</p>
	 *
	 * <p><b>为什么这不等于"每 tick 开销"</b>：一轮场只做一次实体查询（只找波实体）+ 几个候选的
	 * 线段判定，<b>没有任何方块遍历</b>；加工波变态（节拍 0）连查询都不做。相比之下同一台机器每
	 * 8 tick 的重扫描要遍历 (2r+1)³ 个方块并逐个查能力/方块实体——把场换成每 2 tick 一次实体查询，
	 * 新增代价仍在那一套的零头之内。</p>
	 *
	 * <p><b>客户端不跑</b>：场是服务端权威的瞬时外部条件（点燃结果由实体本身同步到客户端），
	 * 且客户端不应替服务端做玩法判定。</p>
	 */
	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		int interval = mode.fieldIntervalTicks();
		if (interval <= 0) {
			// 本模式没有场（加工波变态）：连实体查询都不做，计数器回到起点备用
			fieldTickCounter = 0;
			return;
		}
		if (++fieldTickCounter < interval)
			return;
		fieldTickCounter = 0;
		// 只递转速，不递半径：场的几何（攻击态按转速分三档的场盒大小）由模式自己算
		// （TransmuterMode#attackFieldRadius），本类既不判断模式、也不知道半径从哪来。
		// getSpeed() 是网络侧缓存值（O(1)），比 refreshScan 那边每次都要遍历能量场、
		// 按场源字符串回查场控方块实体的 resolveRadius() 便宜得多。
		mode.applyField(level, worldPosition, getSpeed());
	}

	// ================= 处理模式（双态） =================

	/** 当前处理模式（客户端读同步值；护目镜面板显示，见 {@code TransmuterGoggles}）。 */
	public TransmuterMode getMode() {
		return mode;
	}

	/**
	 * 切换处理模式（扳手右键调用；<b>只在服务端执行</b>，见
	 * {@code StellarWaveTransmuterBlock#onWrenched}）。
	 *
	 * <p>模式是方块实体状态（不是 blockstate），同步走 {@link #notifyUpdate()}——
	 * 它等于 {@code setChanged() + sendData()}，与本类既有的扫描读数完全同一套手法
	 * （客户端包走 {@code write/read} 的 {@code clientPacket = true} 分支）。
	 * 多人下客户端只收包、不自行改值，不会出现两端模式不一致。</p>
	 *
	 * @return 切换后的模式
	 */
	public TransmuterMode cycleMode() {
		TransmuterMode previous = mode;
		mode = mode.next();
		if (level != null && !level.isClientSide) {
			// 口位交接（三段同属一件事，口径都由模式自报的 locksWavePorts() 定界，本类不写
			// if (mode == ATTACK)）：
			//   离开接管口位的模式 → 把进入前记下的口位原样还回去（规格 1 的逆操作：不然四口全开
			//   会永久留在机器上，切回加工态后机器看不出任何变化、也拿不回玩家自己设的口位）；
			//   进入接管口位的模式 → 先把当前口位记下来，再由下面的 onEnter 强制全开（规格 1）。
			// 顺序不能反：还原必须发生在新口位入账之前。
			if (previous.locksWavePorts())
				StellarWaveTransmuterBlock.setPortMask(level, worldPosition, portsBeforeTakeover);
			if (mode.locksWavePorts())
				portsBeforeTakeover = StellarWaveTransmuterBlock.portMask(level.getBlockState(worldPosition));
			// 进入新模式的一次性动作（规格 1：攻击波变态强制 4 个波口全开）。
			// 由模式自报（TransmuterMode#onEnter）——本类刻意不写 if (mode == ATTACK)，
			// 与 fieldIntervalTicks()/locksWavePorts() 同一约定：模式行为收在模式常量体里。
			mode.onEnter(level, worldPosition);
		}
		notifyUpdate();
		return mode;
	}

	// ================= 扫描 =================

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level == null || level.isClientSide)
			return;
		refreshScan();
	}

	/**
	 * 一轮扫描：重算半径 → 收集半径内认可加工机 → 汇总应力 → 变化时推网络并同步。
	 * 调用方：{@link #lazyTick()}（服务端每 {@link #SCAN_INTERVAL} tick）。
	 *
	 * <p><b>这里不跑"场"</b>：各模式的场有自己的节拍（{@link TransmuterMode#fieldIntervalTicks()}，
	 * 由 {@link #tick()} 里的 {@link #fieldTickCounter} 驱动），全场唯一一处
	 * {@link TransmuterMode#applyField} 调用在 {@link #tick()}——本方法只刷新读数，两者互不牵连。</p>
	 */
	private void refreshScan() {
		// 首次扫描前注册可选 mod 加工机（幂等，未安装对应 mod 静默跳过）
		StellarWaveMachineIntegrations.ensureRegistered();
		int radius = resolveRadius();
		List<BlockPos> machines = collectMachines(radius);
		List<ResourceLocation> ids = new ArrayList<>(machines.size());
		// 解析每台机器当前状态应执行的配方类型（注册中心的静态档案 + 状态选择器，
		// 如 Vintage 真空室 mode 加压/抽真空）；未注册的启发式动能机 = 空表（执行走全库）
		java.util.List<com.simibubi.create.foundation.recipe.IRecipeTypeInfo> types = new ArrayList<>();
		// 2026-09-14（性能）：id 与"当前状态类型"原本分两趟遍历 machines，每趟都为同一格做一次
		// `level.getBlockState(m).getBlock()`；合并成一趟后语义完全不变（id 对机器一律登记，
		// 只有"取不到方块实体"才跳过类型解析），半径 3 时每轮少 343 次方块状态读取。
		for (BlockPos m : machines) {
			ResourceLocation blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(m)
				.getBlock());
			ids.add(blockId);
			net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(m);
			if (be == null)
				continue;
			for (com.simibubi.create.foundation.recipe.IRecipeTypeInfo t : StellarWaveMachineRegistry.resolve(be,
				blockId))
				if (!types.contains(t))
					types.add(t);
		}
		scannedIds = ids;
		scannedRecipeTypes = types;
		// 加热读数（烈焰燃烧室）：热源不是动能机器，故在机器表之外<b>单独扫描</b>。
		// 达"普通加热"档（KINDLED 及以上，含将熄 FADING）时把 create:mixing 一并计入携带类型——
		// 工作盆搅拌只需要热源、不需要搅拌器（设计文档 §3），所以"点了火"就等于拿到了"加热搅拌"，
		// 护目镜 / Jade 的"携带配方类型"里也才看得见 搅拌。
		HeatReading heat = scanHeatSources(radius);
		if (HeatLevelNames.satisfiesHeated(heat.level())
			&& !types.contains(com.simibubi.create.AllRecipeTypes.MIXING))
			types.add(com.simibubi.create.AllRecipeTypes.MIXING);
		// 已蓄满待释放的强化避雷针：穿波时波可抽取"释放机会"执行 LIGHTNING 类加工
		chargedRods = collectChargedRods(radius);
		if (!chargedRods.isEmpty()
			&& !types.contains(com.hjmmd_8.createoreexpansion.common.AllRecipeTypes.LIGHTNING))
			types.add(com.hjmmd_8.createoreexpansion.common.AllRecipeTypes.LIGHTNING);
		// 载荷源设备读数（物品容器 / 流体容器 / 储能设备；护目镜"读取到什么"用，不显示坐标）
		TransmuterScanner.DeviceCounts devices = scanDeviceCounts(radius);
		// 只估算可携带载荷（不真抽）；真实抽取发生在波穿过的瞬间（collectPayloadForWave）
		estimatePayload(radius);
		float sum = 0f;
		int kineticCount = 0;
		for (BlockPos m : machines) {
			// 只有动能机耗应力（注液器/物品排放器这类非动能机计"机器数"但不计应力）
			if (!(level.getBlockEntity(m) instanceof KineticBlockEntity kinetic))
				continue;
			sum += stressOf(kinetic);
			kineticCount++;
		}

		// 载荷概览摘要（护目镜/网络同步用；纯数字，客户端无需完整载荷对象）
		int newTypeCount = types.size();
		int newItemCount = WavePayloadGather.totalItems(auxItems);
		int newKindCount = auxItems.size();
		int newFluidMb = auxFluid.getAmount();
		int newEnergyFe = auxEnergy;
		int newRodCount = chargedRods.size();

		// 配方类型 id 快照（客户端 Shift 清单用；类型内容变化也触发重发包）
		List<ResourceLocation> newTypeIds = new ArrayList<>();
		for (com.simibubi.create.foundation.recipe.IRecipeTypeInfo t : types) {
			ResourceLocation tid = t.getId();
			if (tid != null && !newTypeIds.contains(tid))
				newTypeIds.add(tid);
		}

		boolean changed = radius != scanRadius || machines.size() != scannedCount
			|| kineticCount != scannedKineticCount
			|| Math.abs(sum - scannedStress) > 0.5f
			|| newTypeCount != recipeTypeCount || newItemCount != payloadItemCount
			|| newKindCount != payloadTypeCount || newFluidMb != payloadFluidMb
			|| newEnergyFe != payloadEnergyFe || newRodCount != rodCreditCount
			|| heat.level() != scannedHeat || !java.util.Objects.equals(heat.pos(), scannedHeatPos)
			|| devices.itemContainers() != scannedItemContainers
			|| devices.fluidContainers() != scannedFluidContainers
			|| devices.energyStorages() != scannedEnergyStorages
			|| devices.energyStoredFe() != scannedEnergyStoredFe
			|| !newTypeIds.equals(scannedTypeIds);
		scanRadius = radius;
		scannedCount = machines.size();
		scannedKineticCount = kineticCount;
		scannedStress = sum;
		recipeTypeCount = newTypeCount;
		payloadItemCount = newItemCount;
		payloadTypeCount = newKindCount;
		payloadFluidMb = newFluidMb;
		payloadEnergyFe = newEnergyFe;
		rodCreditCount = newRodCount;
		scannedHeat = heat.level();
		scannedHeatPos = heat.pos();
		scannedItemContainers = devices.itemContainers();
		scannedFluidContainers = devices.fluidContainers();
		scannedEnergyStorages = devices.energyStorages();
		scannedEnergyStoredFe = devices.energyStoredFe();
		scannedTypeIds = newTypeIds;
		if (!changed)
			return;

		// 功耗随扫描结果变化：已接入网络时推送新消耗并让网络重算（仿充能器 MODE 档位变化）
		if (hasNetwork())
			getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
		notifyUpdate();
	}

	/** 按能量场在场与场源控制器转速解析当前扫描半径。 */
	private int resolveRadius() {
		if (level == null)
			return 1;
		Vec3 center = Vec3.atCenterOf(worldPosition);
		for (EnergyField field : EnergyFields.in(level)) {
			if (!field.contains(center))
				continue;
			float speed = fieldControllerSpeed(field.source());
			// 场源可解析出控制器转速：按 128 RPM 分档；解析失败（调试场/结构场）按低档
			return speed >= 0 ? (speed >= FIELD_SPEED_THRESHOLD ? FIELD_RADIUS_HIGH : FIELD_RADIUS_LOW)
				: FIELD_RADIUS_LOW;
		}
		return 1;
	}

	/** 解析场源字符串（形如 {@code controller:ax,ay,az|bx,by,bz}）回查任一端场控的当前转速；
	 * 非场控产场 / 无法解析返回 -1。 */
	private float fieldControllerSpeed(String source) {
		if (source == null || !source.startsWith("controller:"))
			return -1;
		String[] ends = source.substring("controller:".length())
			.split("\\|");
		for (String end : ends) {
			String[] xyz = end.split(",");
			if (xyz.length != 3)
				continue;
			try {
				BlockPos pos = new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]),
					Integer.parseInt(xyz[2]));
				if (level.getBlockEntity(pos) instanceof EnergyFieldControllerBlockEntity fc)
					return Math.abs(fc.getSpeed());
			} catch (NumberFormatException ignored) {
				// 非法坐标：忽略该端
			}
		}
		return -1;
	}

	/** 一次加热扫描结果（记录类型与扫描实现同在 {@link TransmuterScanner}）。 */

	/**
	 * 扫描半径内（与加工机同一立方体、不含自身格）的<b>加热源</b>读数。
	 *
	 * <p><b>为什么必须单独扫描</b>：Create 的烈焰燃烧室由
	 * {@code BlazeBurnerBlockEntity}（{@code SmartBlockEntity}）驱动，<b>不是</b>
	 * {@link KineticBlockEntity}，因此 {@link #collectMachines} 永远收不到它（它也没有应力）。
	 * 修复前"变器旁边点了火的燃烧室"在读表 / 载荷 / 波执行三处均不存在，玩家看到的就是
	 * "范围内有正在加热的燃烧室，却显示没有被读取"。</p>
	 *
	 * <p>读法直接取方块状态属性 {@code HEAT_LEVEL}（{@link BlazeBurnerBlock#getHeatLevelOf}）——
	 * 与 Create 机器自身读热方式同源；档位序 NONE &lt; SMOULDERING &lt; FADING &lt;
	 * KINDLED &lt; SEETHING，取最高者作为本机热读数（多个燃烧室不累加，同 Create 口径）。</p>
	 */
	/** 加热读数（实现见 {@link TransmuterScanner#scanHeat}：取半径内最高档，不累加）。 */
	private HeatReading scanHeatSources(int radius) {
		return TransmuterScanner.scanHeat(level, worldPosition, radius);
	}

	/**
	 * 扫描半径内的<b>载荷源设备</b>读数（实现见 {@link TransmuterScanner#scanDeviceCounts}）。
	 *
	 * <p>口径一律走能力（capability），故"箱子、其它模组的流体抽屉/储罐、发电机与蓄电池"天然全覆盖；
	 * 跳过自身格、加工机（含注液器这类非动能机）与工作盆——波只从"非加工机的普通容器"取料。
	 * <b>只读</b>：储能只读不抽（真实抽取发生在波穿过的瞬间）。</p>
	 */
	private TransmuterScanner.DeviceCounts scanDeviceCounts(int radius) {
		return TransmuterScanner.scanDeviceCounts(level, worldPosition, radius);
	}

	/**
	 * 收集半径立方体内（不含自身）的加工机<b>位置</b>。
	 *
	 * <p><b>自动识别（全库引擎配套）</b>：收录条件 = {@link #isMachinery(BlockPos)}——
	 * 注册表（{@code registry.StellarWaveMachineRegistry}）机器 <b>或</b> 启发式动能处理机
	 * （排除轴/齿轮/传送带等纯传动件）。任意 mod（现在或未来）的动能加工机放到变器旁
	 * 即被识别，无需逐 mod 注册；配方执行由波侧全库检索（见 {@code StellarWaveEntity}），
	 * 这里只负责机器数/应力/载荷口径。</p>
	 *
	 * <p><b>2026-09 修正：不再要求动能机</b>。旧实现写死
	 * {@code instanceof KineticBlockEntity}，于是 Create 的<b>注液器 Spout</b>（{@code filling}）与
	 * <b>物品排放器 Item Drain</b>（{@code emptying}）这类 {@code SmartBlockEntity} 永远扫不到
	 * → 波拿不到这两种类型 → 类型门把注液/排放配方挡掉。用户实测表现：
	 * "水正常抽到了，但击中后只被存进附近储罐，不给铁桶注液"（没候选 → 波消散 → 余料就近入罐）。
	 * 现在按位置收录，是否耗应力由 {@link #stressOf} 只对动能机计算。</p>
	 */
	/** 半径内认可的加工机位置（实现见 {@link TransmuterScanner#collectMachines}）。 */
	private List<BlockPos> collectMachines(int radius) {
		return TransmuterScanner.collectMachines(level, worldPosition, radius);
	}

	/**
	 * 该位置是否算"加工机"（载荷抽取时也按此排除，避免抽走加工机内部库存）：
	 * <ul>
	 *   <li>已注册（{@code registry.StellarWaveMachineRegistry#isRegistered}）的机器——原精确名单；</li>
	 *   <li>未注册但像动能处理机的方块——启发式排除纯传动件（轴/齿轮/传送带/离合/活塞头等
	 *       与 Create 常用传动结构 id），其余动能方块视为处理机候选（含未注册第三方）。</li>
	 * </ul>
	 */
	private boolean isMachinery(BlockPos pos) {
		// 口径统一在 registry（2026-09）：变器扫描与波侧取料排除共用同一判定，
		// 避免"登记的注液器/物品排放器等非动能加工机"两边判得不一样。
		return StellarWaveMachineRegistry.isMachinery(level, pos);
	}

	/** 半径内已蓄满待释放的强化避雷针列表（实现见 {@link TransmuterScanner#collectChargedRods}）。 */
	private List<BlockPos> collectChargedRods(int radius) {
		return TransmuterScanner.collectChargedRods(level, worldPosition, radius);
	}

	/**
	 * 读取一台加工机器当前的实时应力消耗：入网时取网络口径，否则按注册静态影响近似。
	 *
	 * <p><b>防 NPE（2026-09 崩溃修复）</b>：机器识别已放宽到"任意动能处理机"，扫描区内可能
	 * 有尚未在该网络 members/sources 表注册的动能方块（刚放置 / 本 tick 前网络未并入等），
	 * {@code KineticNetwork.getActualStressOf} 内部 {@code map.get()} 返回 null 再拆箱会抛
	 * NullPointerException（崩溃栈 Create KineticNetwork:170 ← 本行）。这里整体 try/catch：
	 * 网络口径查不到（含异常）一律回退注册静态影响近似，扫描不因单台机器状态中断。</p>
	 */
	private float stressOf(KineticBlockEntity machine) {
		if (machine.hasNetwork()) {
			try {
				float actual = machine.getOrCreateNetwork()
					.getActualStressOf(machine);
				if (actual > 0)
					return actual;
			} catch (Throwable ignored) {
				// 网络表中尚无该机（null 拆箱等）：回退静态影响近似
			}
		}
		return (float) com.simibubi.create.api.stress.BlockStressValues.getImpact(machine.getBlockState()
			.getBlock());
	}

	/**
	 * 从扫描区内<b>非加工机</b>容器/储罐/储能抽取波载荷快照：
	 * <ul>
	 *   <li>物品：最多 5 个、最多 5 种（优先抽不同种；同种可多抽凑满 5 个），
	 *       仿"64 铁锭抽 5 / 5 种各 1"规则——供需要第二输入的配方（机械手手持、杠杆锤锻造等）；</li>
	 *   <li>流体：≤500 mB（取扫描到的第一种流体抽满）；</li>
	 *   <li>电量：通用可抽取储能抽空；CC&amp;A 特斯拉线圈（纯输入设备）经其内部接口
	 *       {@code internalConsumeEnergy} 全抽（经 {@link StellarWaveMachineIntegrations}，未装 CC&amp;A 静默跳过）。</li>
	 * </ul>
	 */
	/**
	 * 扫描区可携带载荷的<b>估算</b>（不改变任何容器内容）：供护目镜摘要显示；真实抽取推迟到
	 * <b>波穿过变器的瞬间</b>（见 {@link #collectPayloadForWave()}）。规则与上限见
	 * {@link TransmuterPayloadCollector#estimate}。
	 */
	private void estimatePayload(int radius) {
		Payload p = TransmuterPayloadCollector.estimate(level, worldPosition, radius, payloadGatherSkip());
		auxItems = p.items();
		auxFluid = p.fluid();
		auxEnergy = p.energy();
	}

	/**
	 * 穿波瞬间<b>真实抽取</b>扫描区载荷（上限见 {@link TransmuterPayloadCollector}），
	 * 并顺手刷新摘要（护目镜显示"最近一次携带"数字）。
	 * 由 {@code StellarWaveTransmuterPass} 在把波转成变体波时调用一次。
	 */
	public Payload collectPayloadForWave() {
		Payload p = TransmuterPayloadCollector.gather(level, worldPosition, Math.max(1, scanRadius),
			payloadGatherSkip());
		auxItems = p.items();
		auxFluid = p.fluid();
		auxEnergy = p.energy();
		return p;
	}

	/**
	 * 载荷取料跳过口径：<b>加工机 + 玩家加工容器（工作盆）</b>——前者是机器不该被抽料，
	 * 后者是玩家摆的原料（口径实现见 {@link TransmuterPayloadCollector#isPlayerProcessingStation}）。
	 */
	private Predicate<BlockPos> payloadGatherSkip() {
		return pos -> isMachinery(pos) || TransmuterPayloadCollector.isPlayerProcessingStation(level, pos);
	}



	// ================= 应力 =================

	/**
	 * <b>本机是否已接入应力</b>——变器"给波赋属性"的总闸门（用户 2026-09 规格）。
	 *
	 * <p>判据：{@code hasNetwork() && getSpeed() != 0 && isSpeedRequirementFulfilled()}。
	 * 三个因子各有唯一来源：</p>
	 * <ul>
	 *   <li>{@code hasNetwork()}：本机已挂进一张动能网络（{@code network != null}）＝"接没接通"；</li>
	 *   <li>{@code getSpeed()}：<b>实际</b>转速——Create 在<b>过载</b>（{@code isOverStressed()}）与
	 *       tick 暂停时直接返回 0，只有 {@code getTheoreticalSpeed()} 保留名义值＝"转没转起来"。
	 *       判据是 {@code != 0}（用户确认："只要在转"，见{@link TransmuterMode#minimumRpm()} 的
	 *       取值语义）；</li>
	 *   <li>{@link #isSpeedRequirementFulfilled()}：<b>本模式的转速门槛</b>
	 *       （{@link TransmuterMode#minimumRpm()}：加工态 0 = 不额外设门槛，攻击态 128 RPM）——它同时是
	 *       Create 悬停提示"需求转速 / 没有达到足够的转速"的判据，所以护目镜看到的那两行
	 *       与<b>这里的实际闸门</b>永远是同一个条件。</li>
	 * </ul>
	 *
	 * <p><b>它管什么</b>：未接入应力（含转速为 0）或转速未达本模式门槛 → 变器对波
	 * <b>不做任何波形 / 属性上的改变</b>——加工波变态不转换（波原样飞过，见
	 * {@code StellarWaveTransmuterPass#tryConvert}）、攻击波变态的攻击场不点燃、连实体查询都不做
	 * （见 {@code TransmuterMode#applyField}）。机器"让不让波过"（波口开关 / 入口撞墙 / 对面遣返）
	 * 是另一回事，本闸门一行都不碰；模式切换与攻击态四口接管行为同样一行都不碰。</p>
	 *
	 * <p><b>只此一处</b>：全仓"要不要给波赋属性"的判定都读本方法（波命中解析走
	 * {@code TransmuterMode#wavePowered} → 本方法），<b>没有任何第二个闸门</b>。
	 * 若哪天要放宽成"只要接通网络就算"（停转 / 过载也照旧赋属性），把第一项之外的两项去掉即可
	 * （{@code return hasNetwork();}）——判据全仓只有这一处。</p>
	 */
	public boolean isWavePowered() {
		return hasNetwork() && getSpeed() != 0 && isSpeedRequirementFulfilled();
	}

	/**
	 * <b>本机转速是否达到"当前模式的门槛"</b>——覆写 Create 的
	 * {@code KineticBlockEntity#isSpeedRequirementFulfilled()}（Create 6.0.10 原实现是
	 * {@code |getSpeed()| >= 方块自报的 IRotate.SpeedLevel} 的门槛值；本机方块没声明档位，
	 * 原实现恒为真）。
	 *
	 * <p>覆写它就是为了<b>一个判据同时服务两件事</b>：</p>
	 * <ol>
	 *   <li>变器自己的应力闸门（{@link #isWavePowered()} 的第三个合取项）——攻击态低于 128 RPM
	 *       时攻击场一次都不跑；</li>
	 *   <li>Create 自己的悬停提示：{@code KineticBlockEntity#addToTooltip} 里
	 *       {@code notFastEnough = !isSpeedRequirementFulfilled() && getSpeed() != 0}，
	 *       为真就打出搅拌器同款的两行（金色"需求转速："+ "显然 &lt;机器名&gt; 没有达到足够的转速。"）。
	 *       <b>所以我们一行文案都不用自己写、一个新翻译键都不用加</b>，显示出来的东西与机械搅拌器
	 *       一字不差，还自动跟随 Create 的本地化与配色。</li>
	 * </ol>
	 *
	 * <p><b>门槛由模式自报</b>（{@link TransmuterMode#minimumRpm()}）：本方法<b>不判断模式</b>，
	 * 以后加"要 64 RPM 的模式"只需在模式常量体里覆写 {@code minimumRpm()}，这里一行都不用改。</p>
	 *
	 * <p>按<b>绝对值</b>判定（反转的轴同样算接通），与全仓既有的 {@code Math.abs(getSpeed())}
	 * 口径一致。只读转速、不读网络：转速 ≠ 0 必然已经挂进了网络。</p>
	 */
	@Override
	public boolean isSpeedRequirementFulfilled() {
		return Math.abs(getSpeed()) >= mode.minimumRpm();
	}

	/**
	 * 应力消耗：Σ(被扫<b>动能机</b>实时应力) × (1 + 0.1 × (动能机数 − 1))。
	 *
	 * <p>乘子按<b>动能机</b>数算（{@link #scannedKineticCount}）：注液器 / 物品排放器这类
	 * 非动能加工机不耗应力，多摆一台不该让变器变重。扫描到 0 台动能机时回退本机注册
	 * IMPACT（{@code BlockStressValues.getImpact}，受 Create 配置/数据包影响）。</p>
	 *
	 * <p><b>2026-09 审计修复（不再按自身速度提前返回 0）</b>：Create 的 {@code KineticNetwork}
	 * 只在 add/addSilently/updateStressFor 时把 {@code calculateStressApplied()} 写进
	 * {@code members} 缓存，之后 {@code getActualStressOf} 直接读缓存<b>不再重算</b>；
	 * 而 {@code KineticBlockEntity.getSpeed()} 在过载时也返回 0。旧实现遇到速度 0 就 return 0，
	 * 于是"过载一次"就被网络永久缓存成 0 耗能（只有扫描内容恰好变化才会被纠正）。
	 * Create 的普通机器从不这样做——速度倍率由网络统一乘 {@code getTheoreticalSpeed()} 承担。</p>
	 */
	@Override
	public float calculateStressApplied() {
		if (level == null) {
			this.lastStressApplied = 0f;
			return 0f;
		}
		float base = (float) com.simibubi.create.api.stress.BlockStressValues.getImpact(getStressConfigKey());
		double impact = scannedKineticCount > 0 ? scannedStress * (1.0d + 0.1d * (scannedKineticCount - 1)) : base;
		float capped = (float) Math.max(0, impact);
		this.lastStressApplied = capped;
		return capped;
	}

	/**
	 * 速度/过载状态变化后<b>主动刷新</b>本机在网络里的应力贡献（2026-09 审计修复）：
	 * 网络的 {@code members} 是缓存值，不会自己重算，必须显式 {@code updateStressFor}。
	 */
	@Override
	public void onSpeedChanged(float previousSpeed) {
		super.onSpeedChanged(previousSpeed);
		if (level == null || level.isClientSide)
			return;
		try {
			if (hasNetwork())
				getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
		} catch (Throwable ignored) {
			// 网络表尚未就绪等异常：下一次扫描/速度变化会再试，不影响主循环
		}
	}

	// ================= 展示 =================

	/** 当前扫描半径（客户端读 NBT 同步值）。 */
	public int getScanRadius() {
		return scanRadius;
	}

	/** 当前扫描到的加工机器数量。 */
	public int getScannedCount() {
		return scannedCount;
	}

	/** 被扫机器实时应力之和。 */
	public float getScannedStress() {
		return scannedStress;
	}

	/** 最近一轮扫描到的加工机方块 id（只读快照；穿波转换时附加到变体波）。 */
	public List<ResourceLocation> getScannedMachineIds() {
		return List.copyOf(scannedIds);
	}

	/** 最近一轮扫描抽到的辅料物品（只读副本；穿波时附着）。 */
	public List<ItemStack> getScannedAuxItems() {
		return List.copyOf(auxItems);
	}

	/** 最近一轮扫描抽到的流体（只读副本）。 */
	public FluidStack getScannedFluid() {
		return auxFluid.copy();
	}

	/** 最近一轮扫描抽到的电量（FE）。 */
	public int getScannedEnergy() {
		return auxEnergy;
	}

	/** 最近一轮扫描按机器状态解析的应执行配方类型（只读副本；穿波时附着）。 */
	public List<com.simibubi.create.foundation.recipe.IRecipeTypeInfo> getScannedRecipeTypes() {
		return List.copyOf(scannedRecipeTypes);
	}

	/** 半径内已蓄满待释放的强化避雷针位置（只读副本；穿波时从中抽取一次释放机会）。 */
	public List<BlockPos> getChargedRodPositions() {
		return List.copyOf(chargedRods);
	}

	/**
	 * 最近一轮扫描到的<b>最高加热档位</b>（扫描半径内点燃的烈焰燃烧室；NONE = 无热源）。
	 * 穿波时由 {@code StellarWaveTransmuterPass} 附到变体波上，波便自带加热能力。
	 */
	public BlazeBurnerBlock.HeatLevel getScannedHeat() {
		return scannedHeat;
	}

	/**
	 * 该热源位置（内部状态；用于"热源换位"触发一次重扫重发）。
	 * <b>护目镜/Jade 不展示坐标</b>——读数只报"读到了什么"（见 {@code addToGoggleTooltip}）。
	 */
	public BlockPos getScannedHeatPos() {
		return scannedHeatPos;
	}

	/**
	 * 记录最近一次穿出的变体波"当时实际可执行"的全部配方类型
	 * （取波对象 {@code StellarWaveEntity#getActiveRecipeTypes()} = 携带扫描类型 + 载荷电量额外类型）。
	 * 调用方：{@code StellarWaveTransmuterPass#tryConvert} 穿波瞬间（服务端）。
	 * 内容变化才发包同步给客户端（护目镜"最近波可加工"展示）；transient，不落盘。
	 */
	public void recordPassedWaveRecipeTypes(List<com.simibubi.create.foundation.recipe.IRecipeTypeInfo> types) {
		List<ResourceLocation> ids = new ArrayList<>();
		if (types != null)
			for (com.simibubi.create.foundation.recipe.IRecipeTypeInfo type : types)
				if (type != null && !ids.contains(type.getId()))
					ids.add(type.getId());
		if (ids.equals(lastWaveRecipeTypeIds))
			return; // 集合未变化：不重发包（波连续同属性穿过时避免无谓同步）
		lastWaveRecipeTypeIds = ids;
		notifyUpdate();
	}

	/**
	 * <b>护目镜面板</b>——行序（用户 2026-09-24 规格；<b>Create 的动能行挪到末尾</b>）：
	 * <ol>
	 *   <li><b>名称行</b>「星辉波变器」（既有键 {@code goggles.stellar_wave_transmuter}，灰色）——
	 *       <b>整个本机信息块的第一行</b>（用户要求"机器名称放在最前面"）；</li>
	 *   <li><b>模式行</b>（紧随名称行）——恒显示，不按 Shift 也能看到；颜色取
	 *       {@link TransmuterMode#displayColor()}（{@link TransmuterGoggles#appendModeLine}，
	 *       全类唯一输出点）；</li>
	 *   <li><b>状态行</b>（只在"效果不生效"时出现）：转速为 0（含未接通网络）→ 既有键
	 *       {@code goggles.stellar_wave_transmuter_idle}（"未接入应力"）；在转但未达本模式门槛
	 *       （攻击态 128 RPM）→ <b>这里一个字都不写</b>，交给 Create 的悬停提示去打
	 *       （见 {@link #addToTooltip}，与搅拌器一字不差）；</li>
	 *   <li>不按 Shift → 既有键 {@code goggles.transmuter_expand_hint}（"按住 Shift 查看机器详情"）；</li>
	 *   <li>按 Shift 且门槛满足 → <b>读数由模式自报</b>（{@link TransmuterMode#appendReadout}）：
	 *       加工态是 {@link TransmuterGoggles#append} 的全量读数（<b>从"半径"行起</b>，
	 *       模式行已在第 2 行输出过，全类只有那一处输出，不会出现两行模式）；
	 *       攻击态是攻击态自己的三行（{@link TransmuterGoggles#appendAttackReadout}）——
	 *       用户 2026-09 规格：攻击态按住 Shift <b>不该显示加工态的内容</b>；</li>
	 *   <li><b>末尾</b>：{@code super.addToGoggleTooltip}——Create 自带的动能行
	 *       （动能统计 / 应力影响）。它原先排在<b>最前</b>，用户要求名称行在最前面，故挪到这里。</li>
	 * </ol>
	 *
	 * <p><b>为什么状态判据读转速、不读 {@code hasNetwork()}</b>：护目镜在客户端渲染，而转速与网络 id
	 * 出自同一份同步 NBT（{@code KineticBlockEntity#write} 无条件写 {@code Network}），
	 * {@code getSpeed() != 0} 已蕴含"挂进了网络"——显示口径与 {@link #isWavePowered()} 等价，
	 * 又不必让客户端去碰网络表（拿到未同步的 {@code null} 会误报"未接入应力"）。</p>
	 */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		// ① 名称行：用户 2026-09-24 规格——"星辉波变器"这个机器名称<b>放在最前面</b>，
		//    它是整个本机信息块的第一行（既有翻译键，灰色）。
		GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter")
			.withStyle(ChatFormatting.GRAY));

		// ② 模式行：紧随名称行（全类唯一输出点，见 TransmuterGoggles#appendModeLine）
		TransmuterGoggles.appendModeLine(tooltip, mode);

		// ③ 状态行 / 读数行：效果不生效时才有。两态（用户 2026-09-14 定稿）：
		//    没按住 Shift → 机器名 + 模式行 + 一行"按住 Shift 查看机器详情"（框小，不挡玩家正要点的那一面）；
		//    按住 Shift   → 一次性把全部读数显示出来。
		// 注：上一版是"按第 1 次给概要、按第 2 次给全量"的三档状态机，用户实测指出那个提示行
		// 本身就藏在本该按住 Shift 才看得见的面板里（自相矛盾且不可发现），故整套撤掉。
		if (Math.abs(getSpeed()) <= 0) {
			GoggleUtil.forGoggles(tooltip,
				Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_idle")
					.withStyle(ChatFormatting.DARK_GRAY));
		} else if (!isSpeedRequirementFulfilled()) {
			// 在转、但转速没到本模式门槛（攻击态 < 128 RPM）：Create 自己的悬停块会紧跟在本机信息块
			// 之后打出"需求转速：/显然 <机器名> 没有达到足够的转速。"（见 addToTooltip）。
			// 这里刻意一个字都不重复写——要的是"与搅拌器一字不差"，那就让搅拌器那条渲染路径自己说。
		} else if (!isPlayerSneaking) {
			GoggleUtil.forGoggles(tooltip,
				Component.translatable("createoreexpansion.goggles.transmuter_expand_hint",
					Component.translatable("create.tooltip.keyShift")
						.withStyle(ChatFormatting.WHITE))
					.withStyle(ChatFormatting.DARK_GRAY));
		} else {
			// 按 Shift：读数<b>由模式自报</b>（用户 2026-09 规格：攻击态按住 Shift 不显示加工态内容）
			// ——加工态走 TransmuterGoggles.append 那套全量读数，攻击态走攻击态自己的三行。
			// 本类一行 if (mode == ATTACK) 都没有，两套读数的分派收在 TransmuterMode#appendReadout 里。
			mode.appendReadout(tooltip, new TransmuterGoggles.Readout(scanRadius, getSpeed(), scannedHeat,
				scannedItemContainers, scannedFluidContainers, scannedEnergyStorages, scannedEnergyStoredFe,
				scannedCount, scannedStress, scannedTypeIds, recipeTypeCount, payloadItemCount, payloadTypeCount,
				payloadFluidMb, payloadEnergyFe, rodCreditCount, lastWaveRecipeTypeIds));
		}

		// ④ Create 自带的动能行（动能统计 / 应力影响）——用户 2026-09-24 规格：<b>挪到末尾</b>。
		//    它从"最先"改到这里之后，名称行才是整个本机信息块的第一行。
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		// 名称行必然已加 → 恒 true。返回值必须兜住"我加过东西"（Create 侧收到 false 会丢掉整块提示）。
		return true;
	}

	/**
	 * <b>准心悬停信息</b>（Create 的 {@code IHaveHoveringInformation}，<b>不需要戴护目镜</b>）：
	 * 先补一行<b>机器名称</b>、再补一行<b>当前模式</b>，然后把转速判定的部分交给 Create
	 * （{@code KineticBlockEntity#addToTooltip}）——转速不足时它自己会打出搅拌器同款两行
	 * （金色 {@code create.tooltip.speedRequirement} + {@code create.gui.contraptions.not_fast_enough}）。
	 *
	 * <p><b>行序与护目镜一致</b>（用户 2026-09 规格）：名称行（既有键
	 * {@code goggles.stellar_wave_transmuter}）在最前，模式行紧随其后——悬停路径这两行与戴护目镜时
	 * 看到的头两行完全相同，玩家换不换护目镜都不必重新找位置。</p>
	 *
	 * <p><b>只在"没有护目镜信息"时才补这两行</b>：戴着护目镜时
	 * {@code GoggleOverlayRenderer} 会先调 {@link #addToGoggleTooltip}（那里已经输出过名称行与模式行）、
	 * 再调本方法，两边都写就会看到两行名称/模式。判据用 {@code tooltip.isEmpty()}——渲染器恰好在两者之间
	 * 插了一个空行（{@code CommonComponents.EMPTY}），所以"列表非空"就等于"护目镜已经把本机信息块
	 * 画过了"。</p>
	 *
	 * <p>返回值必须把"我加过这两行"也算进去：没戴护目镜时 Create 的悬停块多半什么都不加
	 * （转速够快、没过载），返回 false 会让渲染器把整块提示丢掉（{@code GoggleOverlayRenderer}
	 * 会因两个信息接口都没加东西而提前 return）。</p>
	 */
	@Override
	public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = false;
		if (tooltip.isEmpty()) {
			// 名称行在最前、模式行随后（与 addToGoggleTooltip 的头两行同序）
			GoggleUtil.forGoggles(tooltip,
				Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter")
					.withStyle(ChatFormatting.GRAY));
			TransmuterGoggles.appendModeLine(tooltip, mode);
			added = true;
		}
		// "过载 / 转速不足"两块都由 Create 自己渲染（它就是搅拌器走的那条路径，判据是我们覆写的
		// isSpeedRequirementFulfilled）——这里不重复实现、也不新增任何翻译键。
		return super.addToTooltip(tooltip, isPlayerSneaking) || added;
	}
	// ================= NBT =================

	@Override
	public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
		super.write(compound, registries, clientPacket);
		// 处理模式（加工波变态 / 攻击波变态）：既落盘（读档后模式不丢）也随客户端包同步（护目镜显示），
		// 故不放在下面的 clientPacket 分支里
		compound.putString("TransmuterMode", mode.key());
		// 攻击态接管口位之前记下的四口状态：随机器落盘（区块卸载时机器可能正处在攻击态，
		// 掩码丢了就会在离开攻击态时把玩家的口位还原成全关）
		compound.putInt("PortsBeforeTakeover", portsBeforeTakeover);
		compound.putInt("ScanRadius", scanRadius);
		compound.putInt("ScannedCount", scannedCount);
		compound.putInt("ScannedKineticCount", scannedKineticCount);
		compound.putFloat("ScannedStress", scannedStress);
		// 加热读数（烈焰燃烧室）：档位序号 + 热源位置（asLong 打包成 1 个 long，位置仅内部用）。
		// 客户端护目镜要显示读数，故随客户端包同步；落盘亦可（读档后护目镜重扫描前也能显示正确值）。
		compound.putInt("ScannedHeat", scannedHeat.ordinal());
		if (scannedHeatPos != null)
			compound.putLong("ScannedHeatPos", scannedHeatPos.asLong());
		// 载荷源设备读数（护目镜展示"读取到什么设备"；与坐标无关的纯计数）
		compound.putInt("ScannedItemContainers", scannedItemContainers);
		compound.putInt("ScannedFluidContainers", scannedFluidContainers);
		compound.putInt("ScannedEnergyStorages", scannedEnergyStorages);
		compound.putInt("ScannedEnergyStoredFe", scannedEnergyStoredFe);
		// 载荷概览摘要（客户端护目镜显示用）
		compound.putInt("PayloadItemCount", payloadItemCount);
		compound.putInt("PayloadTypeCount", payloadTypeCount);
		compound.putInt("PayloadFluidMb", payloadFluidMb);
		compound.putInt("PayloadEnergyFe", payloadEnergyFe);
		compound.putInt("RecipeTypeCount", recipeTypeCount);
		compound.putInt("RodCreditCount", rodCreditCount);
		// 最近波可加工类型（transient 运行态展示信息）：仅随客户端包同步，不落盘
		if (clientPacket) {
			ListTag lastWave = new ListTag();
			for (ResourceLocation id : lastWaveRecipeTypeIds)
				lastWave.add(StringTag.valueOf(id.toString()));
			compound.put("LastWaveTypeIds", lastWave);
			// 扫描配方类型 id（客户端 Shift 清单）：仅随客户端包同步
			ListTag scanned = new ListTag();
			for (ResourceLocation id : scannedTypeIds)
				scanned.add(StringTag.valueOf(id.toString()));
			compound.put("ScannedTypeIds", scanned);
		}
	}

	@Override
	protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		// 处理模式：老存档没有该键 → byKey 回退加工波变态（默认态），不会因缺键变成别的模式
		mode = TransmuterMode.byKey(compound.getString("TransmuterMode"));
		// 攻击态接管口位之前记下的四口状态（位序见 StellarWaveTransmuterBlock#portMask）：
		// 老存档没有该键 → 0 = 四口全关，与放置默认（规格 3）一致
		portsBeforeTakeover = compound.getInt("PortsBeforeTakeover");
		scanRadius = Mth.clamp(compound.getInt("ScanRadius"), 1, 3);
		scannedCount = Math.max(0, compound.getInt("ScannedCount"));
		// 旧存档没有该键（0）：退回"全部机器都算动能机"的旧口径，避免读档后应力变轻
		scannedKineticCount = compound.contains("ScannedKineticCount")
			? Math.max(0, compound.getInt("ScannedKineticCount"))
			: scannedCount;
		scannedStress = Math.max(0, compound.getFloat("ScannedStress"));
		scannedHeat = HeatLevelNames.byOrdinal(compound.getInt("ScannedHeat"));
		scannedHeatPos = compound.contains("ScannedHeatPos") ? BlockPos.of(compound.getLong("ScannedHeatPos")) : null;
		scannedItemContainers = Math.max(0, compound.getInt("ScannedItemContainers"));
		scannedFluidContainers = Math.max(0, compound.getInt("ScannedFluidContainers"));
		scannedEnergyStorages = Math.max(0, compound.getInt("ScannedEnergyStorages"));
		scannedEnergyStoredFe = Math.max(0, compound.getInt("ScannedEnergyStoredFe"));
		payloadItemCount = Math.max(0, compound.getInt("PayloadItemCount"));
		payloadTypeCount = Math.max(0, compound.getInt("PayloadTypeCount"));
		payloadFluidMb = Math.max(0, compound.getInt("PayloadFluidMb"));
		payloadEnergyFe = Math.max(0, compound.getInt("PayloadEnergyFe"));
		recipeTypeCount = Math.max(0, compound.getInt("RecipeTypeCount"));
		rodCreditCount = Math.max(0, compound.getInt("RodCreditCount"));
		// 最近波可加工类型（transient）：只在客户端包（clientPacket=true）中携带；
		// 磁盘读档没有该键，保持空 = 本次运行尚无波穿出记录
		if (clientPacket) {
			lastWaveRecipeTypeIds = new ArrayList<>();
			ListTag lastWave = compound.getList("LastWaveTypeIds", Tag.TAG_STRING);
			for (int i = 0; i < lastWave.size(); i++) {
				ResourceLocation id = ResourceLocation.tryParse(lastWave.getString(i));
				if (id != null && !lastWaveRecipeTypeIds.contains(id))
					lastWaveRecipeTypeIds.add(id);
			}
			scannedTypeIds = new ArrayList<>();
			ListTag scanned = compound.getList("ScannedTypeIds", Tag.TAG_STRING);
			for (int i = 0; i < scanned.size(); i++) {
				ResourceLocation id = ResourceLocation.tryParse(scanned.getString(i));
				if (id != null && !scannedTypeIds.contains(id))
					scannedTypeIds.add(id);
			}
		}
	}
}