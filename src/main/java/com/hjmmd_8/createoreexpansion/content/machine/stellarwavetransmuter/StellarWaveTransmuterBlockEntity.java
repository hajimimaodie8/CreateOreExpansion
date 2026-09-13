package com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.hjmmd_8.createoreexpansion.content.charger.payload.WavePayloadGather;
import com.hjmmd_8.createoreexpansion.content.energyfield.EnergyField;
import com.hjmmd_8.createoreexpansion.content.energyfield.EnergyFields;
import com.hjmmd_8.createoreexpansion.content.lightning.block.ReinforcedLightningRodBlockEntity;
import com.hjmmd_8.createoreexpansion.content.machine.energyfieldcontroller.EnergyFieldControllerBlockEntity;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.display.TransmuterGoggles;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.StellarWaveMachineRegistry;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.scan.TransmuterScanner;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.scan.TransmuterScanner.HeatReading;
import com.hjmmd_8.createoreexpansion.util.GoggleUtil;
import com.hjmmd_8.createoreexpansion.util.HeatLevelNames;
import com.hjmmd_8.createoreexpansion.util.RecipeTypeNames;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

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

	/** 载荷上限：辅料物品总数 / 种类数 / 流体 mB。<b>数值来自配置</b>（{@code [wave] maxPayloadItems /
	 *  maxPayloadKinds / maxPayloadFluidMb}，默认 5 / 5 / 500；见 {@link AllConfig}），
	 *  与"波命中后就地补料"共用同一口径，所以两处都必须读配置而不是常量。 */
	private static int maxPayloadItems() {
		return com.hjmmd_8.createoreexpansion.common.AllConfig.waveMaxPayloadItems;
	}

	private static int maxPayloadTypes() {
		return com.hjmmd_8.createoreexpansion.common.AllConfig.waveMaxPayloadKinds;
	}

	private static int maxPayloadFluid() {
		return com.hjmmd_8.createoreexpansion.common.AllConfig.waveMaxPayloadFluidMb;
	}

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
	 * 调用方：{@link #lazyTick()}（服务端每 8 tick）。
	 */
	private void refreshScan() {
		// 首次扫描前注册可选 mod 加工机（幂等，未安装对应 mod 静默跳过）
		StellarWaveMachineIntegrations.ensureRegistered();
		int radius = resolveRadius();
		List<BlockPos> machines = collectMachines(radius);
		List<ResourceLocation> ids = new ArrayList<>(machines.size());
		for (BlockPos m : machines)
			ids.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(m)
				.getBlock()));
		scannedIds = ids;
		// 解析每台机器当前状态应执行的配方类型（注册中心的静态档案 + 状态选择器，
		// 如 Vintage 真空室 mode 加压/抽真空）；未注册的启发式动能机 = 空表（执行走全库）
		java.util.List<com.simibubi.create.foundation.recipe.IRecipeTypeInfo> types = new ArrayList<>();
		for (BlockPos m : machines) {
			ResourceLocation blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(m)
				.getBlock());
			net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(m);
			if (be == null)
				continue;
			for (com.simibubi.create.foundation.recipe.IRecipeTypeInfo t : StellarWaveMachineRegistry.resolve(be,
				blockId))
				if (!types.contains(t))
					types.add(t);
		}
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
	 * 扫描区可携带载荷的<b>估算</b>（不改变任何容器内容）：供护目镜摘要显示。
	 *
	 * <p><b>2026-09 行为修正</b>：原实现每 8 tick 就<b>真实抽取</b>一次扫描区容器
	 * （{@code extractItem/drain/extractEnergy} 全部实抽），导致玩家往箱子里放东西后
	 * 会被"漏斗式"连续抽空。现在扫描阶段只做 simulate 估算，真实抽取推迟到
	 * <b>波穿过变器的瞬间</b>（见 {@link #collectPayloadForWave()}）。</p>
	 */
	private void estimatePayload(int radius) {
		Payload p = collectPayload(radius, true);
		auxItems = p.items();
		auxFluid = p.fluid();
		auxEnergy = p.energy();
	}

	/**
	 * 穿波瞬间<b>真实抽取</b>扫描区载荷（上限：物品 5 个/5 种、流体 500 mB、可抽电量全抽）。
	 * 由 {@code StellarWaveTransmuterPass} 在把波转成变体波时调用一次。
	 */
	public Payload collectPayloadForWave() {
		Payload p = collectPayload(Math.max(1, scanRadius), false);
		// 摘要同步更新（护目镜显示"最近一次携带"数字）
		auxItems = p.items();
		auxFluid = p.fluid();
		auxEnergy = p.energy();
		return p;
	}

	/**
	 * 一次载荷收集结果（物品 / 流体 / 电量 / <b>取料来源方块位置</b>）。
	 *
	 * <p>{@code sources} 用于"载荷消散时还回原容器"：波把剩余载荷还回这些位置，
	 * 而不是丢在消散点（消散点常常就是波正在加工的工作盆上方，会被盆吸进去）。</p>
	 */
	public record Payload(List<ItemStack> items, FluidStack fluid, int energy, List<BlockPos> sources) {
	}

	/**
	 * 扫描区内<b>非加工机、非玩家加工容器</b>的载荷收集。
	 *
	 * <p>取料顺序与"波命中后就地补料"共用同一套实现
	 * （{@link com.hjmmd_8.createoreexpansion.content.charger.entity.WavePayloadGather}）：
	 * <b>先每种各 1（铺开种类），再按存量补数量</b>；流体取第一种抽到 500 mB；电量抽干可抽取储能
	 * （特斯拉线圈走内部接口）。</p>
	 *
	 * @param simulate true = 只估算（不动容器）；false = 真实抽取
	 */
	private Payload collectPayload(int radius, boolean simulate) {
		List<ItemStack> items = new ArrayList<>();
		List<BlockPos> sources = new ArrayList<>();
		int r = Math.max(1, radius);
		Predicate<BlockPos> skip = pos -> isMachinery(pos) || isPlayerProcessingStation(pos);
		WavePayloadGather.gatherItems(level, worldPosition, r, items, sources, maxPayloadItems(), maxPayloadTypes(),
			simulate, skip);
		FluidStack fluid = WavePayloadGather.gatherFluid(level, worldPosition, r, FluidStack.EMPTY, sources,
			maxPayloadFluid(), simulate, skip);
		// 电量上限：配置固定值，或（默认）CC&A 充电配方里最贵那条的耗电量——见 WavePayloadGather#resolveEnergyCap
		int energy = WavePayloadGather.gatherEnergy(level, worldPosition, r, sources, simulate, skip,
			WavePayloadGather.resolveEnergyCap(level));
		return new Payload(items, fluid, energy, sources);
	}


	/**
	 * 该位置是否为"玩家正在加工的容器"（工作盆等）：这类容器虽然带物品/流体能力，
	 * <b>但不是载荷源</b>——盆里是玩家摆的原料与配方流体，波不该把它们当辅料抽走
	 * （2026-09 修正：旧实现把工作盆也当普通容器抽料，导致盆里的料会莫名少掉几个）。
	 *
	 * <p>波命中工作盆时走的是<b>另一条路</b>：命中容器的槽位与盆内流体作为"通道 B"直接参与加工
	 * （见 {@code StellarWaveEntity#evalCandidate}），根本不需要载荷。</p>
	 */
	private boolean isPlayerProcessingStation(BlockPos pos) {
		try {
			return level.getBlockEntity(pos) instanceof com.simibubi.create.content.processing.basin.BasinBlockEntity;
		} catch (Throwable ignored) {
			return false; // 判定异常：按"不是"处理（保守，不影响正常容器）
		}
	}


	// ================= 应力 =================

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

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		GoggleUtil.forGoggles(tooltip, Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter")
			.withStyle(ChatFormatting.GRAY));
		added = true;

		if (Math.abs(getSpeed()) <= 0) {
			GoggleUtil.forGoggles(tooltip,
				Component.translatable("createoreexpansion.goggles.stellar_wave_transmuter_idle")
					.withStyle(ChatFormatting.DARK_GRAY));
			return true;
		}
		TransmuterGoggles.append(tooltip, new TransmuterGoggles.Readout(scanRadius, getSpeed(), scannedHeat,
			scannedItemContainers, scannedFluidContainers, scannedEnergyStorages, scannedEnergyStoredFe, scannedCount,
			scannedStress, scannedTypeIds, recipeTypeCount, payloadItemCount, payloadTypeCount, payloadFluidMb,
			payloadEnergyFe, rodCreditCount, lastWaveRecipeTypeIds), isPlayerSneaking);
		return true;
	}
	// ================= NBT =================

	@Override
	public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
		super.write(compound, registries, clientPacket);
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
