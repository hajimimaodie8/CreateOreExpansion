package com.hjmmd_8.createoreexpansion.content.charger.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.common.AllEntityTypes;
import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.hjmmd_8.createoreexpansion.content.charger.family.WaveRecipeFamilies;
import com.hjmmd_8.createoreexpansion.content.lightning.ReinforcedLightningRodEffects;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveMachineIntegrations;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.StellarWaveMachineRegistry;
import com.hjmmd_8.createoreexpansion.util.HeatLevelNames;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.simibubi.create.foundation.recipe.RecipeApplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 能量波"变体"：经星辉波变器（侧面穿入）后的能量波实体。
 *
 * <p>与普通波共用飞行/命中骨架，扩展职责：</p>
 * <ul>
 *   <li><b>加工属性集</b>（{@code attributes}）：变器扫描半径内加工机的方块 id 快照；
 *       命中时经 {@code registry.StellarWaveMachineRegistry} 展开为当前世界具体配方并批量检测；</li>
 *   <li><b>加热能力</b>（{@code carriedHeat}）：变器扫描半径内<b>点燃的烈焰燃烧室</b>的
 *       最高热档快照——命中时满足 {@code HEATED}/{@code SUPERHEATED} 类配方
 *       （加热搅拌、真空室加热等），不再要求热源必须紧贴命中点或波的出射面；</li>
 *   <li><b>载荷</b>：变器发射前从扫描范围容器抽取的<b>辅料物品</b>（≤5 个 / ≤5 种）、
 *       <b>流体</b>（≤500 mB）、<b>电量</b>（FE）附着波上——供需要第二输入/流体/电量的配方；
 *       加工结束剩余载荷优先存入附近容器/储罐/储能，否则掉落/浪费；</li>
 *   <li><b>物品/流体输入双通道（2026-09）</b>：主料 = 命中物品（掉落物或容器槽内 1 件），
 *       其余物品输入（1~{@link #MAX_ITEM_INPUTS} 输入配方）的辅料、以及流体输入（≤2 条），
 *       都<b>优先取命中容器自身</b>（工作盆/置物台里和主料同批的物品、盆内流体），
 *       <b>只有在容器里凑不齐时才回退波载荷</b>——玩家把原料摆进盆里，就不会去动扫描圈箱子的东西；
 *       两通道互不重复占用（槽号/载荷下标两套占用集合），消耗后按来源各自扣减；</li>
 *   <li><b>链式加工</b>：{@code chainLeft} = 波等级（每次成功加工 −1，0 即结束释放载荷）；
 *       命中物品 → 批量测属性 → 恰 1 种直行 / 多种随机 → "主料 + 0~8 辅料 + 0~2 流体"执行
 *       → 产物继续可续链。</li>
 * </ul>
 */
public class StellarWaveEntity extends AbstractChargerWaveEntity {

	/** 变器赋予的加工机属性（方块 id 快照）；可为空 = 波未携带加工属性（退化为普通波）。 */
	private List<ResourceLocation> attributes = new ArrayList<>();

	/**
	 * 变器按扫描时机器<b>实时状态</b>解析出的"当前应执行配方类型"快照
	 * （状态相关机器如 Vintage 真空室按 mode 只带 PRESSURIZING 或 VACUUMIZING 一套）。
	 */
	private List<IRecipeTypeInfo> recipeTypes = new ArrayList<>();

	/**
	 * 变器携带的<b>加热档位</b>快照（扫描半径内最高热档的烈焰燃烧室；NONE = 未携带加热）。
	 *
	 * <p><b>2026-09 修复</b>：加热原先只在<b>命中点 / 波源 / 搅拌器</b>三处的 3×3×3
	 * （= 半径 1）里现找烈焰人，而变器的读取半径可达 3 格——玩家把燃烧室摆在"变器读得到、
	 * 波够不着"的位置时，一律被判"邻域内无达标烈焰人"（实测日志：
	 * {@code 加热环境不满足：…邻域 3×3×3 内均无达标烈焰人（此处最高热档 = NONE）}）。
	 * 现在变器扫描到的热档随波携带，波自带加热能力：命中点邻域有热源仍然认，
	 * 变器那边点着火同样认（与"波把周围机器的加工能力整合进自己"的设计一致）。</p>
	 */
	private BlazeBurnerBlock.HeatLevel carriedHeat = BlazeBurnerBlock.HeatLevel.NONE;

	/**
	 * 变器携带的<b>加工转速</b>（RPM，取变器自身转速的绝对值；0 = 未携带）。
	 *
	 * <p><b>口径（用户 2026-09 拍板）</b>：波执行配方时的"加工转速"取自<b>变器自身转速</b>——
	 * 变器本就是动能机器（护目镜里就有它的转速/应力），"注入多少转速就得到多少加工力"最直观、
	 * 也最好预测（不依赖波此刻飞在谁的旁边）。</p>
	 *
	 * <p>当前唯一消费方是<b>转速档优先级</b>：Vintage 砂带打磨机的抛光配方每条自带
	 * {@code speed_limits}（低/中/高），Vintage 自身是"档位匹配的先进优选桶，一条都不匹配才兜底"
	 * （见 {@code compat.vintageimprovements.VintageRecipeSpeed}），本类排序时按同一口径
	 * 让匹配波转速档的配方排前面。它是<b>优先级而非硬门槛</b>（与 Vintage 行为一致）。</p>
	 */
	private float carriedRpm;

	/**
	 * 变器当时的<b>读取半径</b>（1~3 格；穿波瞬间写入）。
	 *
	 * <p><b>用途（用户 2026-09 定义）</b>：波命中目标后会在命中点周围"就地补料"——
	 * 扫描周围的容器/储罐/储能，把物品/流体/电量补进载荷，<b>补料范围与变器本身的读取范围一致</b>
	 * （见 {@link #refillPayloadAround(BlockPos)}）。</p>
	 */
	private int carriedScanRadius = 1;

	/** 上次"就地补料"的 tick（节流，避免掉落物链上每 tick 全扫一遍）。 */
	private int lastRefillTick = -100;

	/** 就地补料的最小间隔（tick）。 */
	private static final int REFILL_COOLDOWN_TICKS = 5;

	/** 链式剩余次数（= 波等级；每次成功加工 −1）。 */
	private int chainLeft;

	/** 携带辅料物品：总个数 ≤5、种类 ≤5（每种一个条目，count 可 >1 以便"64 铁锭取 5"）。 */
	private List<ItemStack> payloadItems = new ArrayList<>();

	/** 携带流体（≤500 mB；无 = EMPTY）——流体输入的<b>兜底</b>来源：命中容器自身流体槽优先。 */
	private FluidStack payloadFluid = FluidStack.EMPTY;

	/** 携带电量（FE；特斯拉线圈类全抽/余电回填用）。 */
	private int payloadEnergy;

	/**
	 * 载荷的<b>取料来源方块位置</b>（去重、按取料先后；越靠后 = 越新的来源）。
	 *
	 * <p>来源有两处：① 变器穿波瞬间在扫描圈抽载荷的容器；② 波命中后"就地补料"抽到的容器
	 * （见 {@link #refillPayloadAround(BlockPos)}）。载荷消散时按"最新的来源优先"把剩余物
	 * <b>还回这些容器</b>，而不是丢在消散点——消散点常常就在波刚加工过的工作盆上方，
	 * 掉落物会被工作盆吸进去（用户 2026-09 实测反馈："携带的物品也会被同时转移到工作盆里面去"）。</p>
	 */
	private List<BlockPos> payloadSources = new ArrayList<>();

	/** 记忆的来源方块上限（超出后丢弃最旧的）。 */
	private static final int MAX_PAYLOAD_SOURCES = 16;

	/**
	 * <b>最近一次被加工过的方块</b>（工作盆/置物台等）：余料处置
	 * （{@code wave.payloadRelease = NEAREST_CONTAINER}）以它为圆心，在
	 * {@link #carriedScanRadius}（= 变器读取半径）之内找"最近的可存容器"。
	 *
	 * <p>2026-09-11 用户口径："用不完的余料在击中目标方块之后，不返回原来的箱子，
	 * 而是保存在击中方块周围、变器所对应范围之内的最近的可保存容器中"。</p>
	 */
	private BlockPos lastProcessedBlock;

	/**
	 * 本次命中是否已经引了一道雷（{@link #summonLightningAt} 成功时置位）。
	 *
	 * <p>用途：那道雷的"闪电落地统一加工"会就地处理它负责的配方类型（CC&amp;A charging 等），
	 * 所以本波要把这些类型从自己的类型门里摘掉（见 {@link #allowedTypeIds()}），
	 * 否则同一件物品会被执行两遍。每次命中处理开始时复位（见 {@code handleItemInventoryBlock} /
	 * {@code tryCraft}）。</p>
	 */
	private boolean strikeOwnsTypes;

	/** 携带强化避雷针"释放机会"（1 次 = 可执行一次 LIGHTNING 类加工；波本身不引雷）。 */
	private int rodCharges;

	/** 载荷是否已释放（防 remove 重复释放）。 */
	private boolean payloadReleased;

	/**
	 * 单条配方允许的物品输入数上限（= 9：主料 1 + 辅料 8）。
	 * <p>取值依据（实证，非估计）：</p>
	 * <ul>
	 *   <li>Create {@code BasinRecipe.getMaxInputCount() = 64}、{@code getMaxFluidInputCount() = 2}
	 *       （javap 证实）——64 是"工作盆容量口径"上限，不代表真实配方规模；</li>
	 *   <li>Create 自带配方实测（扫描 jar 内全部 {@code data/create/recipe/{mixing,compacting}}，
	 *       共 21 条）：1 输入 5 条、2 输入 7 条、3 输入 3 条、4 输入 4 条、5 输入 1 条、
	 *       <b>9 输入 1 条</b>（{@code create:compacting/ice} = 9×{@code minecraft:snow_block} → 冰）。
	 *       取 9 = 一次性覆盖 Create 现有一切搅拌/压实配方的输入数；</li>
	 *   <li>容量校验：工作盆的物品能力是"输入 9 槽 + 输出 9 槽"合并视图
	 *       （javap：两段 {@code bipush 9} + {@code CombinedInvWrapper}），主料占 1 槽后仍有
	 *       ≥8 槽可当辅料来源，故 9 输入在单个盆内可满足；</li>
	 *   <li>代价上界：每个候选做 ≤(k−1) 次辅料查找，每次 O(载荷 ≤5 + 容器槽 ≤18) 且首个缺料即
	 *       短路返回；9 输入最坏约 8×23 次 {@code Ingredient.test}，而声明 4 输入以上的配方按
	 *       上面的统计本就稀少，不会造成每 tick 的爆炸式扫描。</li>
	 * </ul>
	 * <p>注意：辅料去重规则是"同一槽/同一载荷条目不得被两个 ingredient 复用"，故
	 * "N 件同种物品"式配方（如 9×雪块）需要 N 个不同来源（容器槽或载荷条目），
	 * 而不是从单个 count≥N 的堆里取 N 件（见汇报的未竟边界）。</p>
	 */
	private static final int MAX_ITEM_INPUTS = 9;

	/**
	 * 单条配方允许的<b>流体输入</b>数上限（= 2，与 Create {@code BasinRecipe.getMaxFluidInputCount()}
	 * 一致，javap 证实）。两条流体输入各自独立解析来源与需求量，并按"同种流体已认领量"记账，
	 * 防止两条流体 ingredient 各自"独立通过"、合计却超过实际存量。
	 */
	private static final int MAX_FLUID_INPUTS = 2;

	/** 远程加工诊断日志开关（默认关闭，不刷屏）：排查"命中物品为何不加工 / 走了哪条配方"时置 true。 */
	private static final boolean CRAFT_DEBUG = false;

	/** 诊断日志（仅在 {@link #CRAFT_DEBUG} 打开时输出；关时零开销）。 */
	private static void craftDebug(String msg, Object... args) {
		if (CRAFT_DEBUG)
			com.hjmmd_8.createoreexpansion.CreateOreExpansion.LOGGER.info("[变体波加工] " + msg, args);
	}

	/**
	 * <b>低量级加工轨迹</b>（默认开启）：每次实际加工最多输出一行，量级与"发生了多少次加工"成正比，
	 * 与配方库规模无关（区别于 {@link #CRAFT_DEBUG} 的逐条淘汰日志）。
	 *
	 * <p>用途：定位"明明放了料却没变成预期产物"——日志会明确写出
	 * 「命中哪种输入 → 有几条候选 → 最终选了哪条配方 → 产出什么」，
	 * 于是"是没匹配上、还是匹配上了却选了别的配方"一眼可辨。
	 * 排查完毕后把本常量改回 {@code false} 即可。</p>
	 */
	private static final boolean CRAFT_TRACE = true;

	private static void craftTrace(String msg, Object... args) {
		if (CRAFT_TRACE)
			com.hjmmd_8.createoreexpansion.CreateOreExpansion.LOGGER.info("[变体波轨迹] " + msg, args);
	}

	/** 配方类型 id 字符串（诊断日志用；取不到返回 {@code "?"}）。 */
	private static String typeKeyString(Recipe<?> recipe) {
		ResourceLocation key = typeKeyOf(recipe);
		return key == null ? "?" : key.toString();
	}

	/**
	 * <b>批次锁定（修复：同一批同种物品被随机分配到不同配方产出）</b>：
	 * 全库检索下同一物品可能命中多个配方（如铁锭 → pressing 铁板 与 rolling 铁棍），
	 * 每件/tick 重新 random 会把一批铁锭混成板+棍。这里按<b>输入物品 key</b> 记住最近一次
	 * 成功加工所选配方 id（有界 LRU），后续同种物品命中时若该配方仍可执行则直接复用——
	 * 一批同种物品稳定产出同一种产物，直到输入种类变化才重新选择。
	 */
	private final Map<String, ResourceLocation> lastRecipeByInputKey = new LinkedHashMap<>(8, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, ResourceLocation> eldest) {
			return size() > 8;
		}
	};

	public StellarWaveEntity(EntityType<?> type, Level level) {
		super(type, level);
	}

	public StellarWaveEntity(Level level, Vec3 pos, Vec3 movementDir, int waveLevel) {
		super(AllEntityTypes.STELLAR_WAVE.get(), level, pos, movementDir, waveLevel);
		this.waveOrigin = BlockPos.containing(pos);
	}

	/**
	 * 波的<b>机器原点</b>（生成位置 = 变器出射面）。
	 *
	 * <p>用途：机器环境判定（加热 / 鼓风机媒介 / 压弯机头）不再只认"命中点附近"，
	 * 而是<b>命中点或波源，二者任一满足即可</b>。理由：玩家的直觉是"给变器配好加热就能加工"，
	 * 把烈焰人摆在变器旁边是最自然的摆法；只认目标方块邻域会让人明明点了火却被告知
	 * "没有检测到加热条件"（用户 2026-09 实测反馈）。</p>
	 */
	private BlockPos waveOrigin;

	/** 设定加工机属性快照（变器转换时调用）。 */
	public void setAttributes(List<ResourceLocation> machineIds) {
		this.attributes = machineIds == null ? new ArrayList<>() : new ArrayList<>(machineIds);
	}

	/** 设定按机器实时状态解析出的应执行配方类型（变器转换时调用；旧波无此字段则回退按机器 id 展开）。 */
	public void setRecipeTypes(List<IRecipeTypeInfo> types) {
		this.recipeTypes = types == null ? new ArrayList<>() : new ArrayList<>(types);
	}

	/** 设定变器携带的加热档位（变器转换时调用；null 视为 NONE）。 */
	public void setCarriedHeat(BlazeBurnerBlock.HeatLevel heat) {
		this.carriedHeat = heat == null ? BlazeBurnerBlock.HeatLevel.NONE : heat;
	}

	/** 变器携带的加热档位（只读；NONE = 未携带加热）。 */
	public BlazeBurnerBlock.HeatLevel getCarriedHeat() {
		return carriedHeat;
	}

	/** 设定变器携带的加工转速（RPM；取绝对值，null/负值按绝对值处理）。 */
	public void setCarriedRpm(float rpm) {
		this.carriedRpm = Math.abs(rpm);
	}

	/** 变器携带的加工转速（RPM；0 = 未携带）。 */
	public float getCarriedRpm() {
		return carriedRpm;
	}

	/** 波当前转速档（Vintage 口径：0 停转 / 1 低 / 2 中 / 3 高；未装 Vintage 返回 0 = 无档位概念）。 */
	private int waveSpeedMode() {
		return StellarWaveMachineIntegrations.speedModeFor(carriedRpm);
	}

	/** 设定变器当时的读取半径（穿波瞬间由变器写入；供"命中后就地补料"用）。 */
	public void setCarriedRadius(int radius) {
		this.carriedScanRadius = Math.max(1, radius);
	}

	/** 变器当时的读取半径（也即"就地补料"的生效半径）。 */
	public int getCarriedRadius() {
		return carriedScanRadius;
	}

	/**
	 * <b>命中后就地补料</b>：能量波打中目标后，扫描<b>命中点周围</b>的方块，
	 * 把物品 / 流体 / 电量补进本波携带的载荷。
	 *
	 * <p><b>2026-09-11 起默认关闭</b>（配置 {@code wave.refillPayloadOnHit = false}，用户拍板）：
	 * 载荷只在<b>变器穿波那一刻</b>取一次（取自变器读取半径内的容器——也就是玩家自己摆的箱子），
	 * 波不再自作主张在命中点搬东西。此前的默认行为是"命中后再按读取半径补一次"，
	 * 玩家反馈"包太自作聪明"（前面那发包里的东西不对，这发包就自己再去拿）。</p>
	 *
	 * <p><b>生效范围</b>（开关打开时）：半径 = {@link #carriedScanRadius}（变器穿波时的读取半径
	 * 1~3 格，与能量场档位同源）——即"变器读多大范围，波在命中点就补多大范围"。</p>
	 *
	 * <p><b>取料口径</b>（开关打开时）：与变器穿波时完全相同（{@link WavePayloadGather}）——
	 * 物品先每种各 1（最多 {@code wave.maxPayloadKinds} 种）、再按存量补到
	 * {@code wave.maxPayloadItems} 个；流体第一种补到 {@code wave.maxPayloadFluidMb}；电量抽干四周可抽取储能。
	 * 上限按"载荷已有量"算，所以已经背满的波不会再多拿。</p>
	 *
	 * <p><b>不抽的东西</b>：命中点自身所在方块（那是正在加工的目标）、工作盆（玩家在用的加工容器）、
	 * 各类动能机器（内部库存）。</p>
	 */
	private void refillPayloadAround(BlockPos center) {
		if (!com.hjmmd_8.createoreexpansion.common.AllConfig.waveRefillPayloadOnHit)
			return; // 默认关闭：载荷只在变器穿波时取一次（见方法注释）
		if (level().isClientSide || center == null || carriedScanRadius <= 0)
			return;
		if (tickCount - lastRefillTick < REFILL_COOLDOWN_TICKS)
			return; // 节流：连续命中不重复扫全场
		lastRefillTick = tickCount;
		// 排除口径与"余料入库"共用（见 payloadGatherSkip）：命中点自身 / 工作盆 / 动能机器内部库存
		java.util.function.Predicate<BlockPos> skip = payloadGatherSkip(center);
		try {
			int beforeItems = WavePayloadGather.totalItems(payloadItems);
			int beforeFluid = payloadFluid.getAmount();
			int beforeEnergy = payloadEnergy;
			List<BlockPos> taken = new ArrayList<>();
			// 上限走配置（与变器同一口径：wave.maxPayloadItems / maxPayloadKinds / maxPayloadFluidMb）
			WavePayloadGather.gatherItems(level(), center, carriedScanRadius, payloadItems, taken,
				com.hjmmd_8.createoreexpansion.common.AllConfig.waveMaxPayloadItems,
				com.hjmmd_8.createoreexpansion.common.AllConfig.waveMaxPayloadKinds, false, skip);
			payloadFluid = WavePayloadGather.gatherFluid(level(), center, carriedScanRadius, payloadFluid, taken,
				com.hjmmd_8.createoreexpansion.common.AllConfig.waveMaxPayloadFluidMb, false, skip);
			payloadEnergy += WavePayloadGather.gatherEnergy(level(), center, carriedScanRadius, taken, false, skip,
				WavePayloadGather.resolveEnergyCap(level()));
			rememberPayloadSources(taken);
			int gotItems = WavePayloadGather.totalItems(payloadItems) - beforeItems;
			int gotFluid = payloadFluid.getAmount() - beforeFluid;
			int gotEnergy = payloadEnergy - beforeEnergy;
			if (gotItems > 0 || gotFluid > 0 || gotEnergy > 0)
				craftTrace("命中点 {} 就地补料（半径 {}）：物品 +{} 件 / 流体 +{} mB / 电量 +{} FE", center,
					carriedScanRadius, gotItems, gotFluid, gotEnergy);
		} catch (Throwable ignored) {
			// 补料异常：不打断本次命中处理（载荷保持原样）
		}
	}

	/** 记住本批取料来源（去重、越靠后越新、上限 {@link #MAX_PAYLOAD_SOURCES}）。 */
	private void rememberPayloadSources(List<BlockPos> taken) {
		if (taken == null || taken.isEmpty())
			return;
		for (BlockPos pos : taken) {
			BlockPos p = pos.immutable();
			payloadSources.remove(p);
			payloadSources.add(p);
		}
		while (payloadSources.size() > MAX_PAYLOAD_SOURCES)
			payloadSources.remove(0);
	}

	/** 设定链式剩余次数（默认 = 波等级）。 */
	public void setChain(int chain) {
		this.chainLeft = Math.max(0, chain);
	}

	/** 设定携带载荷（变器转换时把扫描范围抽到的辅料/流体/电量附上）及其取料来源位置。 */
	public void attachPayload(List<ItemStack> items, FluidStack fluid, int energy, List<BlockPos> sources) {
		if (items != null)
			this.payloadItems = new ArrayList<>(items);
		this.payloadFluid = fluid == null || fluid.isEmpty() ? FluidStack.EMPTY : fluid.copy();
		this.payloadEnergy = Math.max(0, energy);
		if (sources != null)
			rememberPayloadSources(sources);
	}

	/** 设定避雷针释放机会数（变器转换时已真正抽取避雷针储层）。 */
	public void setRodCharges(int charges) {
		this.rodCharges = Math.max(0, charges);
	}

	/** 当前加工机属性（只读）。 */
	public List<ResourceLocation> getAttributes() {
		return List.copyOf(attributes);
	}

	/** 链式剩余次数。 */
	public int getChainLeft() {
		return chainLeft;
	}

	/**
	 * 当前"实际可执行"的配方类型全集（只读快照，= {@link #activeRecipeTypes()}）：
	 * 携带的扫描类型（缺省按属性展开）＋ 载荷电量带来的额外类型。
	 * 供外部只读使用——星辉波变器在穿波瞬间以此记录"最近波携带的可加工属性"。
	 */
	public List<IRecipeTypeInfo> getActiveRecipeTypes() {
		return List.copyOf(activeRecipeTypes());
	}

	/**
	 * 携带辅料物品（只读快照：每种一个条目、count 可能 &gt;1；空条目不包含）。
	 * 载荷为<b>服务端运行态</b>（不落盘、不进 SynchedEntityData），仅服务端实体实例有值——
	 * Jade 等客户端读取需经服务端数据通道（见 {@code compat.jade.WaveJadePlugin}）。
	 */
	public List<ItemStack> getPayloadItems() {
		List<ItemStack> copy = new ArrayList<>(payloadItems.size());
		for (ItemStack stack : payloadItems)
			if (stack != null && !stack.isEmpty())
				copy.add(stack);
		return copy;
	}

	/** 携带流体（只读副本；无 = EMPTY）。 */
	public FluidStack getPayloadFluid() {
		return payloadFluid.copy();
	}

	/** 携带电量（FE，只读）。 */
	public int getPayloadEnergy() {
		return payloadEnergy;
	}

	/** 携带避雷针"释放机会"数（只读；1 次 = 可执行一次 LIGHTNING 类加工）。 */
	public int getRodCharges() {
		return rodCharges;
	}

	// ================= 命中：链式远程加工 =================

	@Override
	protected void onItemHit(List<ItemEntity> items) {
		if (attributes.isEmpty() && recipeTypes.isEmpty()) {
			// 未携带任何加工信息：退化为普通波行为（charging 配方加工后绽放消散）
			super.onItemHit(items);
			return;
		}
		if (level().isClientSide)
			return;
		// 命中掉落物后同样"就地补料"（范围 = 变器读取半径）
		if (!items.isEmpty())
			refillPayloadAround(items.get(0)
				.blockPosition());
		for (ItemEntity item : items) {
			if (chainLeft <= 0) {
				finishAndDiscard();
				return;
			}
			if (tryCraft(item)) {
				if (chainLeft <= 0)
					finishAndDiscard();
				return; // 本 tick 只加工一个物品（链式在多 tick 内逐级推进）
			}
		}
	}

	/**
	 * 当前应执行的配方类型：优先扫描快照（含机器实时状态选择）；旧波无快照则按机器 id
	 * 展开静态档案。携带电量的波（{@code payloadEnergy > 0}）额外补入 CC&amp;A charging
	 * 等耗电配方类型（"飞行特斯拉线圈"角色，见 compat 联动）——各类配方条目的电量需求
	 * 仍由逐条候选检测另行门控（见 {@link #tryCraft}）。
	 */
	private List<IRecipeTypeInfo> activeRecipeTypes() {
		List<IRecipeTypeInfo> types = new ArrayList<>();
		if (!recipeTypes.isEmpty()) {
			types.addAll(recipeTypes);
		} else {
			for (ResourceLocation machineId : attributes)
				for (IRecipeTypeInfo t : StellarWaveMachineRegistry.typesFor(machineId))
					if (!types.contains(t))
						types.add(t);
		}
		if (payloadEnergy > 0) {
			try {
				for (IRecipeTypeInfo extra : StellarWaveMachineIntegrations.energyExtraRecipeTypes(true))
					if (!types.contains(extra))
						types.add(extra);
			} catch (Throwable ignored) {
				// CC&amp;A 缺失等异常：不追加额外类型
			}
		}
		return types;
	}

	/** 单次加工尝试：全库检索候选，选 1 条执行；返回是否成功加工。 */
	private boolean tryCraft(ItemEntity item) {
		ItemStack input = item.getItem();
		if (input.isEmpty())
			return false;
		StellarWaveMachineIntegrations.ensureRegistered();

		// 强化避雷针释放机会：携带时，命中这个掉落物就在它所在位置引一道雷
		// （闪电加工由本模组"闪电落地统一加工"接管，见 summonLightningAt 注释）
		strikeOwnsTypes = false; // 每次命中复位（见字段注释）
		summonLightningAt(item.blockPosition());

		// 环境判定以命中点为中心（掉落物自身位置），供配方机器环境前置条件使用。
		// 掉落物路径没有容器上下文 → 物品容器/流体容器都传 null、主料槽 = -1
		// （辅料与流体只可能来自波载荷）。
		List<Candidate> candidates = collectCandidates(input, item.blockPosition(), null, -1, null);
		if (candidates.isEmpty())
			return false;

		Candidate chosen = pickCandidate(candidates, input, null);
		if (!applyCraft(item, chosen))
			return false;
		rememberCraftLock(input, chosen); // 同种物品批次锁定：下次同输入直接复用该配方
		return true;
	}

	/**
	 * 收集命中给定物品的全部可执行候选（含辅料/流体/电量门槛 + 机器环境前置条件）。
	 * 掉落物与方块槽（工作盆/置物台）加工共用；{@code around} 为命中点（环境判定中心）。
	 *
	 * <p><b>全库自动检索（用户 2026-09 定义）</b>：能力来源不再依赖逐 mod 注册的
	 * "机器→配方类型"档案表——只要该变体波携带了至少一台被识别加工机
	 * （{@code attributes}/{@code recipeTypes} 非空），命中物品时经 Create
	 * {@code RecipeFinder} 检索<b>当前世界全部</b> ProcessingRecipe 族配方，
	 * 由材料判定（本方法门槛 + {@link #matches}）选出可执行者。任何 mod——现在或
	 * 未来——的处理配方自动可用，无需登记。</p>
	 *
	 * <p><b>物品/流体输入双通道（2026-09 追加 + 优先级反转）</b>：辅料（ingredients[1..n-1]）来源有两条——
	 * <b>通道 B = 命中容器自身的其它槽</b>（{@code handler} 非空时可用，排除主料槽 {@code mainSlot}）
	 * <b>优先</b>，通道 A = 波载荷 {@link #payloadItems} 兜底；流体输入同理（{@code containerFluid}
	 * 的盆内流体优先，载荷流体兜底）。掉落物路径没有容器，两个容器参数都传 {@code null}
	 * （此时只能用载荷，与改动前一致）。</p>
	 *
	 * @param handler        命中方块的物品容器（掉落物路径传 null = 无容器物品通道）
	 * @param mainSlot       主料所在槽号（容器通道须排除它；掉落物路径传 -1）
	 * @param containerFluid 命中方块的流体容器（工作盆盆内流体；掉落物路径/无流体能力传 null）
	 */
	private List<Candidate> collectCandidates(ItemStack input, BlockPos around, IItemHandler handler, int mainSlot,
		IFluidHandler containerFluid) {
		List<Candidate> candidates = new ArrayList<>();
		if (input.isEmpty())
			return candidates;
		// ===== 配方类型门（2026-09 修复"拆掉机器仍能加工"）=====
		// 波携带的类型 = 变器扫描半径内读到的机器能力快照（见 activeRecipeTypes）。
		// 旧实现只按材料做全库匹配，于是"把卷簧机拆了，波照样能卷簧"——因为执行侧根本不看类型。
		// 现在：类型不在携带集合里的配方一律不参与（含 {@link WaveRecipeFamilies} 登记的非
		// ProcessingRecipe 族，如拆解要靠三级角磨轮才带得动）。
		// 兼容开关：AllConfig.waveRequireCarriedType = false 时回到旧的全库行为。
		java.util.Set<ResourceLocation> allowedTypeIds = allowedTypeIds();
		for (RecipeHolder<?> holder : allWaveRecipes()) {
			if (AllRecipeTypes.shouldIgnoreInAutomation(holder))
				continue;
			Recipe<?> candidateRecipe = holder.value();
			if (!isTypeAllowed(candidateRecipe, allowedTypeIds)) {
				craftDebug("淘汰 {} [{}]：配方类型不在波携带范围内（携带 {} 种）", holder.id(),
					typeKeyString(candidateRecipe), allowedTypeIds.size());
				continue;
			}
			// 族 0｜ProcessingRecipe 主路径：既有全库管线（辅料/流体/电量/环境/材料三段式）
			if (candidateRecipe instanceof ProcessingRecipe<?, ?> recipe) {
				Candidate c = evalCandidate(recipe, input, holder.id(), around, handler, mainSlot, containerFluid, null);
				if (c != null)
					candidates.add(c);
				continue;
			}
			// ===== 族 1..N｜非 ProcessingRecipe 族（{@link WaveRecipeFamilies}）=====
			WaveRecipeFamilies.Family family = WaveRecipeFamilies.familyOf(candidateRecipe);
			if (family != null) {
				// 步骤族（序列装配）：本族"这一走"等价于一条 ProcessingRecipe →
				// 照走族 0 的全套门槛（辅料解析 / 流体 / 电量 / 材料三段式），
				// 产物由族负责（推进装配进度，或到达 loops 后按权重抽最终产物）
				ProcessingRecipe<?, ?> step = null;
				try {
					step = family.currentStepRecipe(holder.id(), candidateRecipe, input);
				} catch (Throwable ignored) {
					step = null; // 步骤判定异常：按"本族这条不可执行"处理
				}
				if (step != null) {
					Candidate stepCandidate = evalCandidate(step, input, holder.id(), around, handler, mainSlot,
						containerFluid, candidateRecipe);
					if (stepCandidate != null)
						candidates.add(stepCandidate);
					continue;
				}
			}
			// 单输入族：材料判定与产物推导都由族负责，这里只做"认领 → 门槛 → 环境"三步
			Candidate familyCandidate = evalFamilyCandidate(holder, input, around);
			if (familyCandidate != null)
				candidates.add(familyCandidate);
		}
		// 工作盆配方过滤器（用户 2026 要求）：命中方块是带配方过滤器的工作盆时，只加工过滤器
		// 允许产出的配方——不再对"可做的其它配方"随机串烧（如 铁锭 → 压板/辊棍混出）。
		FilteringBehaviour recipeFilter = recipeFilterAt(around);
		if (recipeFilter != null) {
			int before = candidates.size();
			candidates.removeIf(c -> !recipeOutputAllowed(recipeFilter, c, input, handler, around));
			if (before > 0 && candidates.isEmpty())
				craftTrace("工作盆过滤器把 {} 的 {} 条候选全部挡掉（只有过滤器列出的产物才放行）", input.getItem(), before);
		}
		craftDebug("命中 {}（容器物品{} 主料槽 {} 容器流体{}）→ 候选 {} 条{}", input.getItem(),
			handler == null ? "无" : "有", mainSlot, containerFluid == null ? "无" : "有", candidates.size(),
			recipeFilter == null ? "" : "（已过工作盆过滤器闸门）");
		return candidates;
	}

	/**
	 * 命中方块是否为<b>工作盆（Basin）</b>：是则返回其配方过滤器（{@code getFilter()}），
	 * 否则返回 null = 不做配方过滤（掉落物/置物台等维持全库行为）。
	 */
	private FilteringBehaviour recipeFilterAt(BlockPos pos) {
		if (pos == null || level().isClientSide)
			return null;
		if (level().getBlockEntity(pos) instanceof BasinBlockEntity basin)
			return basin.getFilter();
		return null;
	}

	/**
	 * 配方产物是否被工作盆配方过滤器放行（语义对齐 Create {@code BasinRecipe.match}：
	 * 主物品产物 = {@code getResultItem}（首个可滚结果）；无物品产物但有流体产物时测第一个流体产物；
	 * results 为空的"产物推导"类配方（变形升级 / 锻造融合）按<b>与执行时同一套推导</b>
	 * 实时算出真实产物再测；过滤器为空时 FilteringBehaviour 本身放行一切）。
	 * 判定异常按放行处理，避免过滤器干扰让波无故停摆。
	 *
	 * @param input 命中物品（产物推导需要"主料"参与，如锻造融合的模板/盔甲/材料三件套）
	 * @param handler 命中容器（推导产物时解析辅料真实物品用；掉落物路径传 null）
	 * @param around 命中点（透传给非 ProcessingRecipe 族）
	 */
	private boolean recipeOutputAllowed(FilteringBehaviour filter, Candidate candidate, ItemStack input,
		IItemHandler handler, BlockPos around) {
		if (filter == null)
			return true;
		Recipe<?> recipe = candidate.recipe;
		try {
			// ① 配方声明的产物（getResultItem = 首个可滚结果）
			ItemStack declared = recipe.getResultItem(level().registryAccess());
			if (filterAllows(filter, declared))
				return true;
			// ② results 为空的"产物推导"类（auto_upgrade / auto_smithing）：用与执行时同一套
			//    computeCraftResults 算出真实产物再测。注意这里**不再以 declared 为空为前提**——
			//    否则"声明了产物但推导产物才是真产物"的配方一旦 declared 不在过滤器内就直接被判死，
			//    表现为"设了过滤器就不加工，清空过滤器又好了"（用户 2026-09 实测反馈）。
			if (input != null && !input.isEmpty()) {
				ItemStack probe = input.copy();
				probe.setCount(1);
				List<ItemStack> derived = computeCraftResults(probe, candidate, handler, around);
				if (derived != null)
					for (ItemStack out : derived)
						if (filterAllows(filter, out))
							return true;
			}
			// ③ 流体产物
			if (recipe instanceof ProcessingRecipe<?, ?> pr && !pr.getFluidResults()
				.isEmpty()) {
				FluidStack fluid = pr.getFluidResults()
					.get(0);
				if (!fluid.isEmpty() && filter.test(fluid))
					return true;
			}
		} catch (Throwable ignored) {
			return true; // 异常保守放行
		}
		craftTrace("工作盆过滤器挡掉候选 {} [{}]：其产物不在过滤器内（若确需该产物，请把它加进过滤器，"
			+ "或把过滤器切到白名单/关闭“匹配数据”）", candidate.id, typeKeyString(recipe));
		return false; // 过滤器非空但配方产物不在其中 → 不可执行
	}

	/**
	 * 过滤器是否放行该产物：先按原样测，再按<b>裸物品</b>（清空数据组件）测一次。
	 *
	 * <p>后者是必需的：过滤器是用来挑"做哪个产物"的，不应因为产出物带有伤害/附魔等组件就判不匹配
	 * ——升级类配方的产物会<b>继承被改造物的组件</b>（如钻石剑的附魔/耐久 → 下界合金剑），
	 * 若玩家过滤器里放的是干净的下界合金剑、且列表过滤器开着"匹配数据"，原样测必然失败。</p>
	 */
	private static boolean filterAllows(FilteringBehaviour filter, ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return false;
		try {
			if (filter.test(stack))
				return true;
			ItemStack bare = new ItemStack(stack.getItem());
			return !ItemStack.isSameItemSameComponents(bare, stack) && filter.test(bare);
		} catch (Throwable ignored) {
			return true; // 判定异常：保守放行，别让过滤器把波卡死
		}
	}

	/**
	 * 单条<b>非 ProcessingRecipe 族</b>配方评估（{@link WaveRecipeFamilies} 登记：拆解等）：
	 * 认领 → 材料判定 → 环境 → 产出空辅料/流体/电量的候选。
	 *
	 * <p>族配方的辅料/流体/电量一律为空：当前登记的族都是"单输入"族（见
	 * {@link WaveRecipeFamilies} 的契约边界说明）；多输入族要扩契约，别在这里偷偷扣物品。</p>
	 */
	private Candidate evalFamilyCandidate(RecipeHolder<?> holder, ItemStack input, BlockPos around) {
		Recipe<?> recipe = holder.value();
		WaveRecipeFamilies.Family family = WaveRecipeFamilies.familyOf(recipe);
		if (family == null)
			return null; // 没有族认领（且不是 ProcessingRecipe）：不参与
		try {
			if (!family.matches(holder.id(), recipe, input)) {
				craftDebug("淘汰族配方 {} [{}]：材料不匹配（主料 {}）", holder.id(), typeKeyString(recipe), input.getItem());
				return null;
			}
		} catch (Throwable ignored) {
			return null; // 族判定异常：跳过该条，不影响其它候选
		}
		if (!environmentSatisfied(around, recipe)) {
			craftDebug("淘汰族配方 {} [{}]：机器环境前置不满足（命中点 {}）", holder.id(), typeKeyString(recipe), around);
			return null;
		}
		craftDebug("命中族候选 {} [{}]：主料 {}", holder.id(), typeKeyString(recipe), input.getItem());
		return new Candidate(holder.id(), recipe, List.of(), List.of(), List.of(), null);
	}

	/** 单条配方评估：门槛全过返回候选，否则 null（供全库循环调用）。
	 *  {@code id} = 配方数据包 id（批次锁定的稳定标识）；{@code around} = 命中点（环境判定中心）；
	 *  {@code handler}/{@code mainSlot} = 命中容器与其主料槽（辅料通道 B；掉落物路径传 null / -1）。
	 *
	 *  @param familyOwner 步骤族路径下"本候选实际属于哪条族配方"（序列装配）；普通配方传 null */
	private Candidate evalCandidate(ProcessingRecipe<?, ?> recipe, ItemStack input, ResourceLocation id, BlockPos around,
		IItemHandler handler, int mainSlot, IFluidHandler containerFluid, Recipe<?> familyOwner) {
		int fluidInputs = recipe.getFluidIngredients()
			.size();
		if (fluidInputs > MAX_FLUID_INPUTS) {
			craftDebug("淘汰 {} [{}]：流体输入 {} > {}（超出 Create 盆口径）", id, typeKeyString(recipe), fluidInputs,
				MAX_FLUID_INPUTS);
			return null;
		}
		int itemInputs = recipe.getIngredients()
			.size();
		if (itemInputs < 1 || itemInputs > MAX_ITEM_INPUTS) {
			craftDebug("淘汰 {} [{}]：物品输入数 {} 不在 1..{} 内", id, typeKeyString(recipe), itemInputs,
				MAX_ITEM_INPUTS);
			return null;
		}

		// 辅料确认（支持 1~9 输入）：ingredients[0] = 主料（命中物品/槽内物品），
		// ingredients[1..n-1] 逐个找互不重复的辅料，来源优先级为
		//   <b>通道 B 命中容器自身的其它槽</b>（排除主料槽 mainSlot）→ <b>通道 A 波载荷</b>。
		// 优先容器是本次反转的关键语义：玩家把原料都放在盆/置物台里时，绝不会去动扫描圈箱子的
		// 载荷物品；只有容器里凑不齐辅料，才回退用载荷。同一槽/同一载荷条目不重复占用
		// （否则"2 辅料"配方只凭 1 件物品即可过门槛，会被白嫖执行）。
		// auxes 按 ingredient 升序压缩存储：auxes.get(k) 对应 ingredients.get(k+1)。
		List<AuxRef> auxes = new ArrayList<>(Math.max(0, itemInputs - 1));
		for (int i = 1; i < itemInputs; i++) {
			AuxRef aux = findAux(recipe.getIngredients()
				.get(i), auxes, handler, mainSlot);
			if (aux == null) {
				craftDebug("淘汰 {} [{}]：辅料 {}（第 {} 号输入）在容器（{} 槽）与载荷（{} 件）中都缺货", id,
					typeKeyString(recipe),
					recipe.getIngredients()
						.get(i),
					i, handler == null ? 0 : handler.getSlots(), payloadItems.size());
				return null;
			}
			auxes.add(aux);
		}
		// 流体输入（≤2）：同样"容器流体槽优先 → 载荷流体兜底"，各自独立解析并记账防超领
		List<FluidRef> fluidRefs = resolveFluidRefs(recipe, containerFluid);
		if (fluidRefs == null) {
			craftDebug("淘汰 {} [{}]：流体输入不满足（容器流体 {} / 载荷 {}）", id, typeKeyString(recipe),
				containerFluid == null ? "无" : containerFluid.getTanks() + " 罐", payloadFluid);
			return null;
		}
		// 电量需求（CC&A charging / Vintage 激光 energy 等耗电条目）：按"特殊辅料"双通道解析
		// （邻域储能优先 → 载荷电量兜底），合计不足才淘汰。仅在配方确实要电时才做邻域扫描。
		int energyRequired = 0;
		try {
			energyRequired = StellarWaveMachineIntegrations.recipeEnergyRequired(recipe);
		} catch (Throwable ignored) {
			// 联动读取异常：按无电量需求处理
		}
		List<EnergyDraw> energies = resolveEnergyDraws(energyRequired, around);
		if (energies == null) {
			craftDebug("淘汰 {} [{}]：需电量 {} FE，邻域储能 + 载荷电量（{} FE）合计不足", id, typeKeyString(recipe),
				energyRequired, payloadEnergy);
			return null;
		}
		if (!matches(recipe, input, auxes, handler)) {
			craftDebug("淘汰 {} [{}]：材料不匹配（主料 {}）", id, typeKeyString(recipe), input.getItem());
			return null;
		}
		// 机器环境前置条件（④）：需加热/需压弯机头/需鼓风机媒介的配方，命中点附近必须存在对应环境
		if (!environmentSatisfied(around, recipe)) {
			craftDebug("淘汰 {} [{}]：机器环境前置不满足（命中点 {}）", id, typeKeyString(recipe), around);
			return null;
		}
		craftDebug("命中候选 {} [{}]：主料 {} + 辅料 {}（流体 {} / 电量 {}）", id, typeKeyString(recipe),
			input.getItem(), describeAuxes(auxes), describeFluids(fluidRefs), describeEnergies(energies));
		return new Candidate(id, recipe, List.copyOf(auxes), List.copyOf(fluidRefs), List.copyOf(energies), familyOwner);
	}

	/**
	 * <b>引雷</b>：波携带"强化避雷针释放机会"（{@link #rodCharges}，来自"已蓄满"的强化避雷针，
	 * 见 {@code StellarWaveTransmuterPass#drainRodCredit}）时，<b>打中哪个地方就在那个地方引一道雷</b>。
	 *
	 * <p>用户 2026-09 口径："强化避雷针满了之后波会把它抽取（只有满的时候才抽），
	 * 这时候波就有了一种引雷的能力，它打中哪个地方，哪个地方就会引雷。"</p>
	 *
	 * <p><b>为什么这样就够了</b>：本模组已有"闪电落地统一加工"
	 * （{@code content.lightning.LightningEventHandler}）——任何闪电落地的首个 tick，
	 * 都会收集<b>落点周围 1 格</b>内的输入（掉落物 / 置物台 / 弹射置物台 / 工作盆），
	 * 按 {@code createoreexpansion:lightning}（多输入优先，含 CC&amp;A 充电配方）与闪电方块配方加工。
	 * 所以波只要在命中点引一道雷，闪电加工就<b>物理地</b>发生在那里——旧实现是在命中掉落物上
	 * "模拟执行一次单输入闪电配方"（代码注释当年写的是"波不引雷"），既只支持单输入、
	 * 也看不到雷，与这条口径相反。</p>
	 *
	 * <p><b>安全性</b>：用 {@code ReinforcedLightningRodEffects#spawnLightning}（纯视觉闪电——
	 * 不点火、不伤害生物、不生成骷髅陷阱马），但闪电加工照常生效。若你要"真落雷"（会点火），
	 * 说一声即可改成非 visualOnly。</p>
	 *
	 * @return 是否真的引了一道雷（没机会 / 客户端 / 位置为空则 false）
	 */
	private boolean summonLightningAt(BlockPos pos) {
		if (rodCharges <= 0 || pos == null || level().isClientSide)
			return false;
		rodCharges--;
		strikeOwnsTypes = true; // 这道雷负责它自己的加工类型，本波不再重复执行（见 allowedTypeIds）
		ReinforcedLightningRodEffects.spawnLightning(level(), pos);
		craftTrace("引雷：命中点 {} 落雷（避雷针释放机会 -1，剩余 {}）；本次命中的雷击类配方交给那道雷执行",
			pos, rodCharges);
		return true;
	}

	/**
	 * 撞到不带物品槽的普通方块（撞墙前）时的引雷钩子（基类 {@code onSolidBlockHit} 的覆写）：
	 * 波带着避雷针引雷次数时，撞到哪里就在哪里落雷——这样"闪电方块转化"
	 * （{@code createoreexpansion:lightning_block}）也能远程执行。波仍按原逻辑在此消散。
	 */
	@Override
	protected void onSolidBlockHit(BlockPos pos) {
		summonLightningAt(pos);
	}

	/** 当前世界全部"<b>波可执行</b>"配方（RecipeFinder 带缓存；数据包重载后自动失效重查）。
	 *  范围 = Create ProcessingRecipe 族（主路径全库管线）∪ {@link WaveRecipeFamilies} 登记的非
	 *  ProcessingRecipe 族（拆解等）；仍排除本 mod 闪电类——闪电加工不走全库命中，
	 *  只能由"波在命中点引雷 → 本模组闪电落地统一加工"承担（见 {@link #summonLightningAt}）。
	 *
	 *  <p>缓存 key 仍用 {@code RecipeFinder.class}（本类独占该 key，见
	 *  {@code PowerAngleGrinderBlockEntity} 的注释：对方按 typeInfo 对象作 key，互不冲突）；
	 *  谓词放宽后同一 JVM 会话内只会以新谓词构建一次缓存。</p> */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<RecipeHolder<?>> allWaveRecipes() {
		try {
			return (List) com.simibubi.create.foundation.recipe.RecipeFinder.get(
				com.simibubi.create.foundation.recipe.RecipeFinder.class, level(),
				r -> r.value() instanceof Recipe<?> recipe
					&& WaveRecipeFamilies.isExecutable(recipe)
					&& !isLightningRecipe(r));
		} catch (Throwable ignored) {
			return List.of();
		}
	}

	/** LIGHTNING / LIGHTNING_BLOCK 类型判定（专属机制，不入全库）。 */
	private static boolean isLightningRecipe(RecipeHolder<?> holder) {
		if (!(holder.value() instanceof Recipe<?> r))
			return false;
		RecipeType<?> t = r.getType();
		return t == AllRecipeTypes.LIGHTNING.getType() || t == AllRecipeTypes.LIGHTNING_BLOCK.getType();
	}

	/**
	 * 找到能命中给定 Ingredient 的辅料（按<b>双通道优先级</b>；找不到返回 null）。
	 *
	 * <p><b>搜索顺序（2026-09 反转）</b>：先通道 B <b>命中容器自身的其它槽</b>
	 * （{@code handler != null} 时；排除主料槽 {@code mainSlot}），再通道 A 波载荷
	 * {@link #payloadItems}。语义：玩家把原料摆在盆/置物台里时就用盆里的，
	 * <b>绝不会</b>去动扫描圈箱子的载荷物品；只有容器里凑不齐辅料才回退用载荷。
	 * （掉落物路径无容器 → 仍然只有载荷可用。）</p>
	 *
	 * <p><b>去重</b>：{@code occupied} 已收录本次候选已占用的辅料引用（容器槽号 + 载荷下标两套），
	 * 同一个容器槽 / 同一条载荷条目不会被两个 ingredient 重复占用——否则多输入配方可用 1 件辅料
	 * 冒充多件，门槛形同虚设。（同一物品类型的两个不同槽仍可分别充当两个 ingredient，
	 * 这在实际场景里是必要的，故按"槽/条目"而非按"物品种类"去重。）</p>
	 *
	 * @param occupied 本次候选已占用的辅料引用集合（可为空表，不可为 null）
	 */
	private AuxRef findAux(Ingredient ingredient, List<AuxRef> occupied, IItemHandler handler, int mainSlot) {
		// 通道 B（优先）：命中容器自身的其它槽（排除主料槽与已占用槽）
		if (handler != null) {
			for (int slot = 0; slot < handler.getSlots(); slot++) {
				if (slot == mainSlot)
					continue;
				if (isOccupied(occupied, AuxSource.CONTAINER, slot))
					continue;
				ItemStack stack = handler.getStackInSlot(slot);
				if (stack == null || stack.isEmpty())
					continue;
				if (ingredient.test(stack))
					return new AuxRef(AuxSource.CONTAINER, slot);
			}
		}
		// 通道 A（兜底）：波载荷
		for (int i = 0; i < payloadItems.size(); i++) {
			if (isOccupied(occupied, AuxSource.PAYLOAD, i))
				continue;
			ItemStack stack = payloadItems.get(i);
			if (!stack.isEmpty() && ingredient.test(stack))
				return new AuxRef(AuxSource.PAYLOAD, i);
		}
		return null;
	}

	/** 该来源+下标是否已被本次候选占用。 */
	private static boolean isOccupied(List<AuxRef> occupied, AuxSource source, int index) {
		for (AuxRef ref : occupied)
			if (ref.source() == source && ref.index() == index)
				return true;
		return false;
	}

	// ================= 流体输入双通道（容器流体槽优先 → 载荷流体兜底） =================

	/**
	 * 解析配方的全部流体输入（≤{@link #MAX_FLUID_INPUTS} 条），每条独立选来源与需求量。
	 *
	 * <p><b>来源优先级（2026-09 反转）</b>：先命中容器的流体槽（工作盆的盆内流体），
	 * 不足才回退波载荷流体。语义与物品输入一致——玩家把水倒进盆里就该用盆里的水。</p>
	 *
	 * <p><b>防超领记账</b>：同一流体种类可能被多条 ingredient 引用（或容器有多个同种流体的罐），
	 * 故容器侧按"已认领量"累计后与实际可用量比较；载荷侧同理用 {@code payloadFluidReserved}
	 * 累计。这样两条流体 ingredient 各自"独立通过"也不会合计超过实际存量。</p>
	 *
	 * @return 全部流体输入的来源清单；任一 unsatified 返回 null（调用方按不可执行淘汰）
	 */
	private List<FluidRef> resolveFluidRefs(ProcessingRecipe<?, ?> recipe, IFluidHandler containerFluid) {
		List<net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient> needs = recipe.getFluidIngredients();
		if (needs.isEmpty())
			return List.of();
		List<FluidRef> refs = new ArrayList<>(needs.size());
		int payloadReserved = 0;
		for (net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient need : needs) {
			int amount = need.amount();
			FluidRef ref = null;
			// 通道 B（优先）：命中容器的流体槽
			FluidStack tankType = firstMatchingTankFluid(containerFluid, need);
			if (!tankType.isEmpty()) {
				FluidStack demand = tankType.copyWithAmount(amount);
				int available = fluidAmountIn(containerFluid, demand) - claimedFluidAmount(refs, FluidSource.CONTAINER, demand);
				if (available >= amount)
					ref = new FluidRef(FluidSource.CONTAINER, demand);
			}
			// 通道 A（兜底）：波载荷流体
			if (ref == null && !payloadFluid.isEmpty() && need.test(payloadFluid)
				&& payloadFluid.getAmount() - payloadReserved >= amount) {
				ref = new FluidRef(FluidSource.PAYLOAD, payloadFluid.copyWithAmount(amount));
				payloadReserved += amount;
			}
			if (ref == null)
				return null;
			refs.add(ref);
		}
		return refs;
	}

	/** 容器流体槽里第一个满足该流体 ingredient 的流体（返回其真实种类+组件，数量置为需求量）；无则 EMPTY。 */
	private static FluidStack firstMatchingTankFluid(IFluidHandler fluids,
		net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient need) {
		if (fluids == null)
			return FluidStack.EMPTY;
		try {
			for (int tank = 0; tank < fluids.getTanks(); tank++) {
				FluidStack in = fluids.getFluidInTank(tank);
				if (!in.isEmpty() && need.test(in))
					return in.copyWithAmount(need.amount());
			}
		} catch (Throwable ignored) {
			// 个别流体能力实现异常：按"无匹配流体"处理
		}
		return FluidStack.EMPTY;
	}

	/** 容器内所有罐中与 {@code want} 同种（含组件）的流体总量。 */
	private static int fluidAmountIn(IFluidHandler fluids, FluidStack want) {
		if (fluids == null || want == null || want.isEmpty())
			return 0;
		int total = 0;
		try {
			for (int tank = 0; tank < fluids.getTanks(); tank++) {
				FluidStack in = fluids.getFluidInTank(tank);
				if (!in.isEmpty() && FluidStack.isSameFluidSameComponents(in, want))
					total += in.getAmount();
			}
		} catch (Throwable ignored) {
			return 0;
		}
		return total;
	}

	/** 已解析的流体输入里，来自同一来源且同种（含组件）流体的已认领量。 */
	private static int claimedFluidAmount(List<FluidRef> refs, FluidSource source, FluidStack want) {
		int claimed = 0;
		for (FluidRef ref : refs)
			if (ref.source() == source && FluidStack.isSameFluidSameComponents(ref.fluid(), want))
				claimed += ref.fluid()
					.getAmount();
		return claimed;
	}

	/** 流体输入清单的可读描述（诊断日志用）。 */
	private static String describeFluids(List<FluidRef> refs) {
		if (refs == null || refs.isEmpty())
			return "无";
		StringBuilder sb = new StringBuilder();
		for (FluidRef ref : refs) {
			if (sb.length() > 0)
				sb.append('+');
			sb.append(ref.source())
				.append('#')
				.append(ref.fluid()
					.getAmount())
				.append("mB")
				.append(ref.fluid()
					.getHoverName()
					.getString());
		}
		return sb.toString();
	}

	// ================= 电量双通道（邻域储能优先 → 载荷电量兜底；用户："电量也是一种特殊辅料"） =================

	/**
	 * 解析配方电量需求的来源清单（<b>把电量当"特殊辅料"处理</b>，与物品/流体来源完全对称）：
	 * <ol>
	 *   <li><b>通道 1｜命中点邻域储能（优先）</b>：{@link #envBlocks}／{@link #ENV_RADIUS}（3×3×3，
	 *       与"加热烈焰人 / 压弯机头 / fan 媒介"同一口径）内的可抽储能，按扫描顺序<b>贪心取电</b>——
	 *       多个源可以合力凑够需求（"之和 ≥ 需求"）。两类源：
	 *       <ul>
	 *         <li>通用 {@code IEnergyStorage}（需 {@code canExtract()}）；</li>
	 *         <li>CC&amp;A <b>特斯拉线圈</b>——对外 {@code canExtract=false}（{@code getMaxOut()==0}），
	 *             只能走其内部储能（{@link StellarWaveMachineIntegrations#teslaCoilEnergy} 只读反射）。</li>
	 *       </ul>
	 *   </li>
	 *   <li><b>通道 2｜波载荷电量（兜底）</b>：{@link #payloadEnergy}（穿波瞬间从变器扫描圈抽来的）。</li>
	 * </ol>
	 *
	 * <p><b>估算阶段零副作用</b>：通用储能用 {@code extractEnergy(need, true)}（模拟）估，
	 * 线圈用只读的 {@code getEnergyStored()} 估；<b>绝不</b>在估算时调用
	 * {@code extractEnergy(need, false)} 或 {@code drainTeslaCoilFully}（会真扣电）。
	 * 不可抽且非线圈的储能（机器内部缓冲等）一律按 0 计——它们本来也拿不出来，
	 * 计进去会导致"门槛过了却抽不到电"的白嫖加工。</p>
	 *
	 * @return 电量来源清单；合计仍不足返回 {@code null}（候选淘汰，不消耗任何东西）
	 */
	private List<EnergyDraw> resolveEnergyDraws(int required, BlockPos around) {
		if (required <= 0)
			return List.of();
		List<EnergyDraw> draws = new ArrayList<>(2);
		int remaining = required;
		if (around != null) {
			for (BlockPos bp : envBlocks(around)) {
				if (remaining <= 0)
					break;
				int available = availableEnergyAt(bp, remaining);
				if (available <= 0)
					continue;
				int take = Math.min(available, remaining);
				// betweenClosed 会复用可变游标，必须 immutable() 后长期持有
				draws.add(new EnergyDraw(EnergySource.NEARBY, take, bp.immutable()));
				remaining -= take;
			}
		}
		if (remaining > 0 && payloadEnergy > 0) {
			int take = Math.min(payloadEnergy, remaining);
			draws.add(new EnergyDraw(EnergySource.PAYLOAD, take, null));
			remaining -= take;
		}
		return remaining <= 0 ? draws : null;
	}

	/**
	 * 估算某方块当前可供抽取的电量（<b>只读/模拟，绝不真扣</b>）：
	 * 通用可抽储能 → {@code extractEnergy(need, true)} 模拟（按 NeoForge 约定不真扣电，
	 * 且能尊重该储能的 {@code maxExtract} 限流），再与只读的 {@code getEnergyStored()} 取小，
	 * 双保险地避免"估算虚高 → 门槛过了却抽不到电"；非可抽者 → 仅当是 CC&amp;A 特斯拉线圈时
	 * 读其内部储能（只读反射）；其余（机器输入缓冲等不可抽储能）→ 0。
	 */
	private int availableEnergyAt(BlockPos pos, int need) {
		if (pos == null || need <= 0 || level().isClientSide)
			return 0;
		try {
			IEnergyStorage storage = level().getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
			if (storage != null && storage.canExtract()) {
				int simulated = Math.max(0, storage.extractEnergy(need, true)); // simulate = true：不扣电
				int stored = Math.max(0, storage.getEnergyStored());
				return Math.min(simulated, stored);
			}
		} catch (Throwable ignored) {
			// 单个储能异常：按不可用处理
		}
		// 对外不可抽的储能：只认 CC&amp;A 特斯拉线圈（内部只读反射；非线圈/未安装返回 0）
		try {
			return StellarWaveMachineIntegrations.teslaCoilEnergy(level(), pos);
		} catch (Throwable ignored) {
			return 0;
		}
	}

	/**
	 * 从指定位置真取电（消耗阶段）：通用可抽储能 → {@code extractEnergy(need, false)}；
	 * 否则走 CC&amp;A 线圈内部<b>按需</b>扣减（{@code internalConsumeEnergy} 自带钳制，取多少扣多少，
	 * 不会像"全抽"那样掏空线圈）。返回实取 FE。
	 */
	private int extractNearbyEnergy(BlockPos pos, int need) {
		if (pos == null || need <= 0)
			return 0;
		try {
			IEnergyStorage storage = level().getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
			if (storage != null && storage.canExtract()) {
				int got = storage.extractEnergy(need, false);
				if (got > 0)
					return got;
			}
		} catch (Throwable ignored) {
			// 单个储能异常：继续尝试线圈路径
		}
		try {
			return StellarWaveMachineIntegrations.consumeTeslaCoil(level(), pos, need);
		} catch (Throwable ignored) {
			return 0;
		}
	}

	/** 电量来源清单的可读描述（诊断日志用）。 */
	private static String describeEnergies(List<EnergyDraw> draws) {
		if (draws == null || draws.isEmpty())
			return "无";
		StringBuilder sb = new StringBuilder();
		for (EnergyDraw draw : draws) {
			if (sb.length() > 0)
				sb.append('+');
			sb.append(draw.source())
				.append('#')
				.append(draw.amount())
				.append("FE");
			if (draw.pos() != null)
				sb.append('@')
					.append(draw.pos()
						.toShortString());
		}
		return sb.toString();
	}

	/**
	 * 按辅料引用取出<b>真实物品</b>（读操作，不改动来源）：
	 * {@code PAYLOAD} → 载荷条目；{@code CONTAINER} → 容器槽（索引自身即槽号）。
	 * 取不到（越界 / 空槽 / 无容器）一律返回 {@link ItemStack#EMPTY}，绝不抛异常。
	 * 容器实现可能返回活对象（vanilla {@code Container#getStackInSlot}），调用方只读不写。
	 */
	private ItemStack resolveAux(AuxRef ref, IItemHandler handler) {
		if (ref == null)
			return ItemStack.EMPTY;
		if (ref.source() == AuxSource.PAYLOAD) {
			int i = ref.index();
			return i >= 0 && i < payloadItems.size() ? payloadItems.get(i) : ItemStack.EMPTY;
		}
		if (handler == null)
			return ItemStack.EMPTY;
		int slot = ref.index();
		if (slot < 0 || slot >= handler.getSlots())
			return ItemStack.EMPTY;
		ItemStack stack = handler.getStackInSlot(slot);
		return stack == null ? ItemStack.EMPTY : stack;
	}

	/** 辅料引用的简短可读描述（诊断日志用）。 */
	private static String describeAux(AuxRef ref) {
		return ref == null ? "无" : ref.source() + "#" + ref.index();
	}

	/** 辅料引用列表的可读描述（诊断日志用）。 */
	private static String describeAuxes(List<AuxRef> auxes) {
		if (auxes == null || auxes.isEmpty())
			return "无";
		StringBuilder sb = new StringBuilder();
		for (AuxRef ref : auxes) {
			if (sb.length() > 0)
				sb.append('+');
			sb.append(describeAux(ref));
		}
		return sb.toString();
	}

	/**
	 * 判定配方是否命中当前物品（兼容 Create 6.0.10 各配方输入类型）。
	 *
	 * <p>Create 6.0.x 起不同配方类要求的 {@link RecipeInput} 子类不一致：
	 * <ul>
	 *   <li>{@code SingleRecipeInput} 型——压机 Pressing / 鼓风机 Splashing / Haunting
	 *       （{@code StandardProcessingRecipe<SingleRecipeInput>}）；</li>
	 *   <li>{@code RecipeWrapper} 型——锯 Cutting / 物品应用 ItemApplication、
	 *       Vintage 卷绕 Coiling / 车床 Turning（可带第二槽 = 辅料）；</li>
	 *   <li>{@code RecipeInput} 型——粉碎轮 Crushing / 石磨 Milling、杵锤 Hammering。</li>
	 * </ul>
	 * 若按 {@code Recipe<RecipeInput>} 统一强转调用,`SingleRecipeInput` 型配方的桥接
	 * {@code matches(RecipeInput)} 内部会 {@code checkcast SingleRecipeInput}——传
	 * {@code RecipeWrapper} 即抛 ClassCastException,导致压片等永久匹配失败（波"穿过"物品）。
	 * 故先以单槽 {@code SingleRecipeInput} 试,失败（类型不符或确实不匹配）再回退
	 * 双槽 {@code RecipeWrapper}（槽0=主料、槽1=可选辅料）。</p>
	 *
	 * <p><b>机器上下文配方兜底（③a，2026-09 通用化）</b>：Create 6 中另有一族"机器上下文"
	 * ProcessingRecipe（非 BasinRecipe 子类）的 {@code matches(RecipeInput)} <b>恒返回 false</b>
	 * ——真实匹配走绑定机器实体（如 Vintage 杵锤 {@code HammeringRecipe.matches} 字节码即
	 * {@code iconst_0; ireturn}，真实判定在静态 {@code HammeringRecipe.match(HelveBlockEntity,…)}，
	 * 依赖机器实体上的砧座/锤击数上下文）。变体波没有机器实体上下文，故在<b>标准两种输入类型
	 * 判定均失败之后</b>再做一次手工材料判定（主料命中 ingredient[0]、其余 ingredient 逐条对应
	 * 已确认的辅料——辅料可来自波载荷或命中容器其它槽），否则这类配方永远无法远程执行。</p>
	 *
	 * <p><b>为何不再按"类型 id 白名单"限定</b>：此前仅放行 {@code vintageimprovements:hammering}，
	 * 于是"任何 mod 的 matches 恒 false 机器上下文配方"都要逐个加白名单才能用。现在改为
	 * <b>通用兜底</b>——只要配方是 ProcessingRecipe 且两种标准判定都失败，就按材料手工判定，
	 * 未来任何 mod 的同类配方自动可用（无需登记）。</p>
	 *
	 * <p><b>为何该兜底安全（不会误命中普通配方）</b>：判定顺序决定了它是"最后手段"——
	 * <ol>
	 *   <li>BasinRecipe 已在上方单独手工判定；</li>
	 *   <li>普通单输入配方（压机/喷洗/闹鬼）走 {@code SingleRecipeInput} 一定成功，
	 *       根本不会走到兜底；</li>
	 *   <li>普通双/三输入配方（卷绕/车削/物品应用/变形升级）走 {@code RecipeWrapper} 成功，
	 *       也不会走到兜底；</li>
	 *   <li>只有"标准 matches 恒 false"的机器上下文族才会落到此处，而此时仍要求
	 *       {@code ingredients[0].test(input)} 成立、且其余 ingredient 逐条有对应辅料
	 *       （可来自波载荷或命中容器其它槽；辅料在 {@link #evalCandidate} 中已先行确认）。
	 *       也就是说，材料不符的普通配方在 {@code ingredients[0].test} 这一步就会失败，
	 *       绝不会被兜底误放行；</li>
	 *   <li>兜底不放宽任何其它门槛：流体输入数/辅料齐备/流体量/电量/机器环境（加热、压弯头、
	 *       鼓风机媒介）仍由 {@link #evalCandidate} 与 {@link #environmentSatisfied} 先行把关。</li>
	 * </ol>
	 */
	private boolean matches(Recipe<?> recipe, ItemStack input, List<AuxRef> auxes, IItemHandler handler) {
		// BasinRecipe 系（工作盆/真空室等机器上下文配方，如 Vintage 加压/抽真空）：
		// Create 6 的 BasinRecipe.matches() 恒返回 false（真实匹配走机器静态 match，
		// 绑定 BasinBlockEntity 的过滤器/加热状态）。变体波是"远程能量执行器"，没有
		// 机器实体上下文——这里按配方自身材料需求手工判定（忽略机器过滤器与热需求，
		// 波自带加工能量），否则真空室配方永远无法远程执行。
		if (recipe instanceof com.simibubi.create.content.processing.basin.BasinRecipe basin) {
			return genericIngredientsMatch(basin, input, auxes, handler);
		}
		// 先单槽：主流单输入配方（压机/喷洗/闹鬼…）的输入类型
		net.minecraft.world.item.crafting.SingleRecipeInput single =
			new net.minecraft.world.item.crafting.SingleRecipeInput(input);
		if (matchesQuietly(recipe, single))
			return true;
		// 回退多槽：RecipeWrapper 型配方（含 2~3 输入需辅料槽者；AutoSmithing/AutoUpgrade 的
		// matches(RecipeWrapper) 只校验槽 0，辅料齐备性由 evalCandidate 的 findAux 先行确认）；
		// 单槽型配方传此会 CCE → 静默 false。槽数 = 1 主料 + 辅料数（至少 2，兼容旧双槽形状）；
		// 槽 1..n 填<b>按来源解析出的辅料真实物品副本</b>（载荷 / 命中容器槽皆可），
		// 用<b>临时</b> ItemStackHandler 装配——绝不对命中容器做 setStackInSlot（避免改动物品）。
		net.neoforged.neoforge.items.ItemStackHandler handlerProbe =
			new net.neoforged.neoforge.items.ItemStackHandler(Math.max(2, 1 + auxes.size()));
		handlerProbe.setStackInSlot(0, input);
		for (int k = 0; k < auxes.size(); k++) {
			ItemStack aux = resolveAux(auxes.get(k), handler);
			if (!aux.isEmpty())
				handlerProbe.setStackInSlot(k + 1, aux.copy());
		}
		net.neoforged.neoforge.items.wrapper.RecipeWrapper wrapper =
			new net.neoforged.neoforge.items.wrapper.RecipeWrapper(handlerProbe);
		if (matchesQuietly(recipe, wrapper))
			return true;
		// 两种标准输入类型都判定失败 → 通用兜底：机器上下文配方族（matches 恒 false）按材料手工判定
		if (!(recipe instanceof ProcessingRecipe<?, ?>))
			return false; // 本引擎只执行 ProcessingRecipe 族；非该族一律不兜底
		return genericIngredientsMatch(recipe, input, auxes, handler);
	}

	/** 配方类型 id（BuiltIn 注册表查 key；查不到返回 null）。 */
	private static ResourceLocation typeKeyOf(Recipe<?> recipe) {
		try {
			return BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
		} catch (Throwable ignored) {
			return null;
		}
	}

	/**
	 * 手工材料判定（机器上下文兜底 + BasinRecipe 共用）：主料 = 命中物品须命中
	 * {@code ingredients[0]}；其余 ingredient 逐条对应 {@link #evalCandidate} 已确认的辅料引用
	 * （{@code auxes.get(k)} ↔ {@code ingredients.get(k+1)}，按 ingredient 升序压缩存储），
	 * 并按来源解析出真实物品<b>再验一次</b>（防"确认后被消耗/槽位变化"）；
	 * 流体/电量/环境门槛由调用方先行检查。
	 *
	 * @param handler 命中容器（解析 CONTAINER 来源辅料用；掉落物路径传 null）
	 */
	private boolean genericIngredientsMatch(Recipe<?> recipe, ItemStack input, List<AuxRef> auxes,
		IItemHandler handler) {
		var ingredients = recipe.getIngredients();
		if (ingredients.isEmpty())
			return false;
		if (!ingredients.get(0)
			.test(input))
			return false;
		for (int i = 1; i < ingredients.size(); i++) {
			int slot = i - 1;
			if (auxes == null || slot >= auxes.size())
				return false; // 辅料数不足：该 ingredient 无料可对应
			ItemStack stack = resolveAux(auxes.get(slot), handler);
			if (stack.isEmpty() || !ingredients.get(i)
				.test(stack))
				return false;
		}
		return true;
	}

	/** 静默调配方 matches：输入类型不符（CCE）或桥接异常按"不匹配"处理，不向调用方抛。 */
	@SuppressWarnings("unchecked")
	private boolean matchesQuietly(Recipe<?> recipe, net.minecraft.world.item.crafting.RecipeInput input) {
		try {
			return ((Recipe<net.minecraft.world.item.crafting.RecipeInput>) recipe).matches(input, level());
		} catch (ClassCastException ignored) {
			return false; // 输入类型与配方要求不符（如给 SingleRecipeInput 型配方传了双槽包装）
		} catch (Throwable ignored) {
			return false; // 个别配方桥接异常：当作不匹配
		}
	}

	/** 执行一次加工：主料 −1；命中第二/第三输入时逐个消耗对应辅料（手持物类配方不消耗；产物推导类
	 * 辅料被"变形/锻入"产物）；流体输入按需消耗；产物生成在命中点（流体产物就近注罐，注不进浪费）；链 −1。 */
	private boolean applyCraft(ItemEntity item, Candidate candidate) {
		ItemStack input = item.getItem();
		ItemStack single = input.copy();
		single.setCount(1);

		// 产物计算：产物推导类（③b auto_upgrade 变形升级 / ③c auto_smithing 锻造融合：results 为空）
		// 产物需从当前候选主料+辅料推导；普通类走 RecipeApplier 滚结果。
		// 掉落物路径没有容器上下文，故 handler 传 null（辅料只可能来自波载荷）。
		List<ItemStack> results = computeCraftResults(single, candidate, null, item.blockPosition());
		if (results == null)
			return false; // 无可执行产物（不消耗任何东西）

		input.shrink(1);
		if (input.isEmpty())
			item.discard();
		// 消耗资源（辅料逐个扣 / 流体 / 电量）——与方块槽路径共用同一实现
		// （此路径无容器上下文：辅料与流体都只可能来自载荷）
		consumeForCraft(candidate, null, null);

		Vec3 pos = item.isRemoved() ? position() : item.position();
		for (ItemStack result : results) {
			if (result.isEmpty())
				continue;
			ItemEntity out = new ItemEntity(level(), pos.x, pos.y, pos.z, result);
			out.setDeltaMovement(Vec3.ZERO);
			level().addFreshEntity(out);
		}
		// 流体产物就近注入储罐（无处可存则浪费，不回波）
		if (candidate.recipe instanceof ProcessingRecipe<?, ?> pr && !pr.getFluidResults()
			.isEmpty()) {
			for (FluidStack fr : pr.getFluidResults())
				if (!fr.isEmpty())
					nearestFluidHandlerFill(fr.copy());
		} else {
			// 非 ProcessingRecipe 族（拆解等）的流体产物：掉落物路径无容器上下文，只能就近注入
			for (FluidStack fr : familyFluidResults(candidate, single, item.blockPosition()))
				if (!fr.isEmpty())
					nearestFluidHandlerFill(fr.copy());
		}
		chainLeft--;
		return true;
	}

	/**
	 * 波当前"携带到的"配方类型 id 集合（= 配方类型门的白名单）。
	 *
	 * <p>来源：{@link #activeRecipeTypes()}——变器扫描半径内机器能力快照（含状态选择器裁剪）
	 * ＋ 载荷电量带来的额外类型（如 CC&amp;A 充电）。空集合 = 该波没有任何加工能力
	 * （退化波，根本不会走到候选检索）。</p>
	 */
	private java.util.Set<ResourceLocation> allowedTypeIds() {
		java.util.Set<ResourceLocation> ids = new java.util.HashSet<>();
		try {
			for (IRecipeTypeInfo type : activeRecipeTypes()) {
				if (type != null && type.getId() != null)
					ids.add(type.getId());
			}
		} catch (Throwable ignored) {
			// 类型表读取异常：返回空表（保守：本 tick 不执行任何配方，而不是放行全库）
		}
		// 本次命中已经引了一道雷 → 闪电落地统一加工负责的配方类型（CC&A charging 等）从本波
		// 的类型门里摘掉：同一件物品只由"雷"加工一遍，不再由波再来一遍（用户 2026-09 指出的重复执行）。
		if (strikeOwnsTypes) {
			try {
				ids.removeAll(StellarWaveMachineIntegrations.strikeHandledTypeIds());
			} catch (Throwable ignored) {
				// 排除失败：按不排除处理（宁可留旧行为，也不要因异常吃掉整张类型表）
			}
		}
		return ids;
	}

	/**
	 * 配方类型门：该配方是否属于波携带到的类型。
	 *
	 * <p>{@link AllConfig#waveRequireCarriedType} 为 false 时恒放行（旧全库行为）。
	 * 类型 id 由 {@link #typeKeyOf(Recipe)} 取注册表键，因此"同类型不同 mod 的配方"
	 * 共用一次携带（与展示口径一致）。</p>
	 */
	private static boolean isTypeAllowed(Recipe<?> recipe, java.util.Set<ResourceLocation> allowedTypeIds) {
		if (!AllConfig.waveRequireCarriedType)
			return true; // 兼容开关：关闭时回到全库检索
		ResourceLocation key = typeKeyOf(recipe);
		return key != null && allowedTypeIds.contains(key);
	}

	/** 非 ProcessingRecipe 族的流体产物（无族认领 / 族实现异常时返回空表）。
	 *
	 *  @param around 命中点（透传给族，供需要世界上下文的族使用） */
	private List<FluidStack> familyFluidResults(Candidate candidate, ItemStack input, BlockPos around) {
		Recipe<?> owner = candidate.familyTarget();
		WaveRecipeFamilies.Family family = WaveRecipeFamilies.familyOf(owner);
		if (family == null)
			return List.of();
		try {
			List<FluidStack> fluids = family.fluidResults(level(), around, candidate.id, owner, input);
			return fluids == null ? List.of() : fluids;
		} catch (Throwable ignored) {
			return List.of();
		}
	}

	/**
	 * 计算一次加工的产物列表；返回 {@code null} = 无可执行产物（调用方须放弃且不消耗任何东西）。
	 * <ul>
	 *   <li><b>变形升级</b>（③b，{@code vintageimprovements:auto_upgrade}）→ {@link #upgradeResultOf}
	 *       （辅料钻石工具原位升级为下界合金版）；</li>
	 *   <li><b>锻造融合</b>（③c，{@code vintageimprovements:auto_smithing}）→ {@link #smithingBridgeResult}
	 *       （借原版锻造配方产出纹饰盔甲）；</li>
	 *   <li>其余普通类 → RecipeApplier 滚结果（杵锤 hammering 等 results 非空者即走此路）。</li>
	 * </ul>
	 * @param handler 命中容器（解析 CONTAINER 来源辅料的真实物品用；掉落物路径传 null）
	 * @param around  命中点（透传给非 ProcessingRecipe 族，见 {@link WaveRecipeFamilies}）
	 */
	private List<ItemStack> computeCraftResults(ItemStack single, Candidate candidate, IItemHandler handler,
		BlockPos around) {
		// ===== 非 ProcessingRecipe 族（{@link WaveRecipeFamilies} 登记：拆解 / 序列装配）=====
		// 产物推导交给族自己（拆解 = floor(材料数 × 剩余耐久比)；序列装配 = 推进进度或收尾抽产物）；
		// 空表/null = 推不出产物 → 跳过该候选。若族只产流体（无物品产物）但声明了流体产物，仍算成功。
		Recipe<?> familyRecipe = candidate.familyTarget();
		WaveRecipeFamilies.Family family = WaveRecipeFamilies.familyOf(familyRecipe);
		if (family != null) {
			List<ItemStack> familyResults;
			try {
				familyResults = family.results(level(), around, candidate.id, familyRecipe, single);
			} catch (Throwable ignored) {
				craftDebug("无产物：族配方 {} 推导异常", candidate.id);
				return null;
			}
			if (familyResults != null && !familyResults.isEmpty())
				return familyResults;
			if (!familyFluidResults(candidate, single, around).isEmpty())
				return new ArrayList<>(); // 只产流体也算成功（物品产物为空表）
			craftDebug("无产物：族配方 {} [{}] 推不出产物（输入 {}）", candidate.id, typeKeyString(familyRecipe),
				single.getItem());
			return null;
		}
		if (isUpgradeTransformationRecipe(candidate.recipe)) {
			AuxRef ref = candidate.firstAux();
			ItemStack auxStack = resolveAux(ref, handler);
			if (auxStack.isEmpty())
				return null;
			ItemStack upgraded = upgradeResultOf(auxStack);
			if (upgraded.isEmpty()) {
				craftDebug("无产物：变形升级辅料 {} 无法映射为下界合金版", auxStack.getItem());
				return null;
			}
			List<ItemStack> one = new ArrayList<>(1);
			one.add(upgraded);
			return one;
		}
		if (isAutoSmithingRecipe(candidate.recipe))
			return smithingBridgeResult(single, candidate, handler);
		List<ItemStack> results = RecipeApplier.applyRecipeOn(level(), single, candidate.recipe, false);
		if (results.isEmpty() && !hasFluidOutput(candidate.recipe) && candidate.fluids.isEmpty()) {
			craftDebug("无产物：{} [{}] 的 RecipeApplier 结果为空且无流体产物", candidate.id,
				typeKeyString(candidate.recipe));
			return null;
		}
		return results;
	}

	/** 配方是否有流体产物（纯流体产物配方如 硫磺→SO₂：物品结果为空但可执行）。 */
	private static boolean hasFluidOutput(Recipe<?> recipe) {
		return recipe instanceof ProcessingRecipe<?, ?> pr && !pr.getFluidResults()
			.isEmpty();
	}

	// ================= 方块槽（工作盆/置物台/弹射置物台）链式加工 =================

	/**
	 * 覆写方块物品槽命中钩子：变体波携带加工配方类型时，按链式远程加工处理槽内物品
	 * （产物放回槽位/掉落），一次命中加工一件、链尽才消散——工作盆/置物台里的物品
	 * 不再走普通波 charging 处理器（那只会加工充能配方，导致铁锭等永不处理、波直接消失）。
	 *
	 * <p>无携带属性/链已尽时按基类默认语义（无可加工 → 调用方按撞墙消散，保持普通波
	 * 撞空置物台的原行为）。</p>
	 *
	 * <p><b>容器流体通道</b>：命中点若具备流体能力（工作盆的盆内流体），一并取出沿调用链透传——
	 * 这样"盆里放原料 + 盆里倒水"的搅拌类配方能直接在盆内满足，无需扫描圈箱子提供流体。</p>
	 *
	 * @return true = 本 tick 已处理（加工成功并继续/已消散）；false = 无可加工，撞墙消散
	 */
	@Override
	protected boolean handleItemInventoryBlock(IItemHandler handler, BlockPos pos) {
		if (level().isClientSide)
			return true;
		if (attributes.isEmpty() && recipeTypes.isEmpty())
			return super.handleItemInventoryBlock(handler, pos); // 无加工能力：维持普通波行为
		if (chainLeft <= 0) {
			finishAndDiscard();
			return true;
		}
		StellarWaveMachineIntegrations.ensureRegistered();

		// 记录"最近加工过的方块"：余料处置（配置 wave.payloadRelease=NEAREST_CONTAINER）以它为圆心，
		// 在变器读取半径内找最近的可存容器（见 releasePayload）
		lastProcessedBlock = pos.immutable();

		// 引雷：携带强化避雷针释放机会时，命中哪个方块就在那里落雷
		// （闪电加工由本模组"闪电落地统一加工"接管该落点 1 格内的掉落物/置物台/工作盆）
		strikeOwnsTypes = false; // 每次命中复位：本命中是否引雷，决定要不要摘掉雷击类类型
		summonLightningAt(pos);

		// 命中后先"就地补料"（范围 = 变器读取半径）：让波能在加工现场补到辅料/流体/电量
		refillPayloadAround(pos);

		// 命中点流体能力（可空：置物台/无流体能力的方块返回 null → 流体只能来自载荷）
		IFluidHandler containerFluid = level().getCapability(Capabilities.FluidHandler.BLOCK, pos, null);

		// 循环加工：链允许时尽量把该方块槽内可加工的物品处理完（一次碰撞处理一"批"），
		// 链尽才消散；与掉落物"逐 tick 一件"不同——方块槽静止，波一次扫过应清空可加工项。
		// batchLock：<b>单次方块命中处理生命周期内的候选锁</b>（不跨 tick）——同一批同种物品
		// 反复进循环时优先复用首次成功的配方，杜绝一锅混出两种产物（①修复）。
		boolean any = false;
		Candidate batchLock = null;
		while (chainLeft > 0) {
			int slot = findCraftableSlot(handler, pos, containerFluid);
			if (slot < 0)
				break;
			Candidate done = craftFromSlot(handler, slot, pos, batchLock, containerFluid);
			if (done == null)
				break; // 该槽无可执行（如环境不满足/无产物/并发变化）：不再原地空转，结束本批处理
			batchLock = done; // 记住本批本次使用的配方，同批后续同种物品优先复用
			any = true;
		}
		if (any) {
			if (chainLeft <= 0)
				finishAndDiscard(); // 链尽：释放载荷并消散
			return true; // 已处理（波不在此消散，继续飞行/穿出）
		}
		craftTrace("命中 {} 无任何可加工候选 → 按撞墙消散；容器内容 {}（属性 {} 个 / 配方类型 {} 个）", pos,
			describeHandler(handler), attributes.size(), recipeTypes.size());
		return false; // 槽内无一可加工 → 调用方按撞墙消散
	}

	/** 找第一个存在可加工候选（含环境前置）的槽（不真取）。
	 *  候选评估带上本容器（物品槽 + 流体槽）与主料槽：辅料优先取<b>本容器其它槽</b>、载荷兜底；
	 *  流体优先取<b>本容器流体槽</b>、载荷兜底。 */
	private int findCraftableSlot(IItemHandler handler, BlockPos pos, IFluidHandler containerFluid) {
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			if (stack.isEmpty())
				continue;
			ItemStack probe = stack.copy();
			probe.setCount(1);
			if (!collectCandidates(probe, pos, handler, slot, containerFluid).isEmpty())
				return slot;
		}
		return -1;
	}

	/**
	 * 从槽取 1 个主料执行一次配方（产物优先放回原槽；"辅料即产物来源"类放回辅料槽）。
	 *
	 * @param preferred 本批方块处理已锁定的候选（可为 null = 尚无锁）；当前槽物品仍匹配它时直接复用，
	 *                  否则重新按输入种类锁定（参见 {@link #pickCandidate}）
	 * @param containerFluid 命中点流体能力（可空）
	 * @return 成功返回所用候选（调用方用于批锁）；无可执行（含环境/产物问题）返回 null
	 */
	private Candidate craftFromSlot(IItemHandler handler, int slot, BlockPos pos, Candidate preferred,
		IFluidHandler containerFluid) {
		ItemStack stack = handler.getStackInSlot(slot);
		if (stack.isEmpty())
			return null;
		ItemStack probe = stack.copy();
		probe.setCount(1);
		List<Candidate> candidates = collectCandidates(probe, pos, handler, slot, containerFluid);
		if (candidates.isEmpty())
			return null;

		// 逐条候选试执行（按 批锁 → 输入种类 LRU → 专用度 排序，见 orderCandidates）：
		// 材料匹配但"推不出产物"的候选（某些 mod 的 results 为空配方、辅料在派生期间被并发取走、
		// 或升级映射不到目标物品）只跳过该条，不再像以前那样直接中断整批——
		// "命中一条空产物候选 → 整锅罢工"是"放了料却什么都没发生"的来源之一。
		List<Candidate> ordered = orderCandidates(candidates, probe, preferred);
		for (Candidate chosen : ordered) {
			List<ItemStack> results = computeCraftResults(probe.copy(), chosen, handler, pos);
			if (results == null) {
				craftTrace("跳过候选 {} [{}]：材料匹配但推不出产物（输入 {}）", chosen.id, typeKeyString(chosen.recipe),
					probe.getItem());
				continue;
			}

			// 真取 1 个主料；取空（并发变化）则跳过此槽
			ItemStack input = handler.extractItem(slot, 1, false);
			if (input.isEmpty())
				return null;

			consumeForCraft(chosen, handler, containerFluid);
			rememberCraftLock(probe, chosen); // 批锁（同输入种类后续复用）
			// 产物回位（⑥）：产物优先放回<b>被消耗辅料所在的槽</b>（变形升级/锻造融合的产物在语义上
			// 就是"辅料被加工成的新物"——如下界合金工具/带纹饰盔甲），主料槽作次选，都放不下才掉落在
			// 方块上方。这样"钻石工具/盔甲那一个盆槽"里直接出现产物，而不是堆到主料槽。
			int target = productSlotHint(chosen, slot);
			if (target < 0 || target >= handler.getSlots())
				target = slot; // 防御：槽号失效则回主料槽
			if (target != slot)
				craftDebug("产物回位：优先槽 {}（辅料槽），主料槽 {}；辅料 {}", target, slot, describeAuxes(chosen.auxes));
			for (ItemStack result : results) {
				if (result.isEmpty())
					continue;
				ItemStack leftover = insertBack(handler, target, result);
				if (!leftover.isEmpty() && target != slot)
					leftover = insertBack(handler, slot, leftover); // 次选：主料槽
				if (!leftover.isEmpty())
					dropAtBlock(pos, leftover);
			}
			// 流体产物：优先注入命中方块自身储罐（真空室等），放不下/无处可存再就近
			if (chosen.recipe instanceof ProcessingRecipe<?, ?> pr && !pr.getFluidResults()
				.isEmpty()) {
				for (FluidStack fr : pr.getFluidResults())
					if (!fr.isEmpty()) {
						FluidStack rest = fillBlockOrNearby(pos, fr.copy());
						if (!rest.isEmpty())
							fillNearbyOnly(rest); // 尽力而为，剩余浪费
					}
			} else {
				// 非 ProcessingRecipe 族（拆解等）的流体产物：同样"命中方块储罐优先 → 就近"
				for (FluidStack fr : familyFluidResults(chosen, probe, pos))
					if (!fr.isEmpty()) {
						FluidStack rest = fillBlockOrNearby(pos, fr.copy());
						if (!rest.isEmpty())
							fillNearbyOnly(rest);
					}
			}
			craftTrace("加工 {} → 选中 {} [{}]（候选 {} 条，辅料 {}）产出 {}{}", input.getItem(), chosen.id,
				typeKeyString(chosen.recipe), ordered.size(), describeAuxes(chosen.auxes), describeResults(results),
				describeHeat(pos, chosen.recipe));
			chainLeft--;
			return chosen;
		}
		craftTrace("槽 {} 的 {} 匹配到 {} 条候选但全部推不出产物 → 本槽放弃", slot, probe.getItem(), ordered.size());
		return null;
	}

	/** 产物列表的简短描述（轨迹日志用）。 */
	private static String describeResults(List<ItemStack> results) {
		if (results == null || results.isEmpty())
			return "（无物品产物）";
		StringBuilder sb = new StringBuilder();
		for (ItemStack s : results) {
			if (s.isEmpty())
				continue;
			if (sb.length() > 0)
				sb.append(" + ");
			sb.append(s.getItem()).append('×').append(s.getCount());
		}
		return sb.length() == 0 ? "（无物品产物）" : sb.toString();
	}

	/** 容器内容摘要（轨迹日志用；最多列 6 个非空槽）。 */
	private static String describeHandler(IItemHandler handler) {
		if (handler == null)
			return "（无容器）";
		StringBuilder sb = new StringBuilder();
		int shown = 0;
		for (int i = 0; i < handler.getSlots() && shown < 6; i++) {
			ItemStack s = handler.getStackInSlot(i);
			if (s.isEmpty())
				continue;
			if (sb.length() > 0)
				sb.append(", ");
			sb.append('[').append(i).append(']').append(s.getItem()).append('×').append(s.getCount());
			shown++;
		}
		return sb.length() == 0 ? "（空）" : sb.toString();
	}

	/**
	 * 供轨迹日志展示的"加热档位说明"：本条配方需要什么热档、以及当前是哪一处、
	 * 什么档位的烈焰人在满足它（不需要加热的配方返回空串）。
	 *
	 * <p>存在意义：把「普通搅拌 / 普通加热搅拌 / 超级加热搅拌」这套阶梯做成<b>可见</b>的——
	 * 玩家一眼能看到"当前实际可用档位是多少、来源在哪"，而不是只在不满足时看到一句失败。</p>
	 */
	private String describeHeat(BlockPos around, Recipe<?> recipe) {
		if (!(recipe instanceof ProcessingRecipe<?, ?> pr) || pr.getRequiredHeat() == HeatCondition.NONE)
			return "";
		return "，需加热 " + pr.getRequiredHeat() + "（命中点 " + heatSourceLabel(around) + " / 波源 "
			+ heatSourceLabel(waveOrigin) + " / 变器携带 " + carriedHeat + "）";
	}

	/** 某中心邻域内最强烈焰人的档位与位置（无则返回"无烈焰人"）。 */
	private String heatSourceLabel(BlockPos center) {
		if (center == null)
			return "无";
		BlazeBurnerBlock.HeatLevel best = BlazeBurnerBlock.HeatLevel.NONE;
		BlockPos bestPos = null;
		for (BlockPos bp : envBlocks(center)) {
			try {
				BlazeBurnerBlock.HeatLevel heat = BlazeBurnerBlock.getHeatLevelOf(level().getBlockState(bp));
				if (heat != BlazeBurnerBlock.HeatLevel.NONE && heat.ordinal() > best.ordinal()) {
					best = heat;
					bestPos = bp;
				}
			} catch (Throwable ignored) {
				// 单个方块判定异常：跳过
			}
		}
		return best == BlazeBurnerBlock.HeatLevel.NONE ? "无烈焰人"
			: best + "@" + (bestPos == null ? "?" : bestPos.toShortString());
	}

	/**
	 * 产物回位目标槽（⑥）：对"辅料即产物来源"的推导类配方——{@code auto_upgrade}（钻石工具 → 下界合金版）
	 * 与 {@code auto_smithing}（盔甲 + 纹饰 → 带纹饰盔甲）——产物在语义上就是<b>辅料被加工成的新物</b>，
	 * 故优先放回<b>首个辅料所在的容器槽</b>（{@code auxes.get(0)} 恰好是配方 JSON 里承担"被改造物"角色的
	 * ingredient[1]：auto_upgrade = 工具、auto_smithing = 可锻造盔甲）。
	 * 该辅料来自波载荷（通道 A）或没有容器上下文（掉落物路径）时，回退主料槽 {@code mainSlot}。
	 * 普通配方（产物 = 全新物品）一律回主料槽，行为与改动前一致。
	 *
	 * @param mainSlot 主料槽（回退目标）
	 * @return 产物优先插入的槽号
	 */
	private int productSlotHint(Candidate candidate, int mainSlot) {
		if (!isUpgradeTransformationRecipe(candidate.recipe) && !isAutoSmithingRecipe(candidate.recipe))
			return mainSlot;
		AuxRef first = candidate.firstAux();
		if (first != null && first.source() == AuxSource.CONTAINER && first.index() >= 0)
			return first.index();
		return mainSlot;
	}

	/** 消耗一次加工的资源（辅料/流体/电量），掉落物路径与方块槽路径共用。
	 *  @param handler        命中容器物品能力（扣减 CONTAINER 来源辅料用；掉落物路径传 null）
	 *  @param containerFluid 命中容器流体能力（扣减 CONTAINER 来源流体用；掉落物路径/无流体能力传 null） */
	private void consumeForCraft(Candidate candidate, IItemHandler handler, IFluidHandler containerFluid) {
		consumeAux(candidate, handler);
		consumeCraftFluid(candidate, containerFluid);
		consumeCraftEnergy(candidate);
	}

	/**
	 * 按来源扣减电量需求（"电量是一种特殊辅料"的扣减侧）：
	 * <ul>
	 *   <li>{@code NEARBY} → 从该方块位置真取（通用储能 {@code extractEnergy(need,false)} /
	 *       线圈内部按需扣）；</li>
	 *   <li>{@code PAYLOAD} → 从 {@link #payloadEnergy} 扣。</li>
	 * </ul>
	 * 邻域取电若因并发变化少于计划量（估算与实际不符），缺口<b>退回载荷电量补齐</b>，
	 * 避免"门槛过了却没真扣到电"的白嫖加工；载荷也不够时按尽力而为扣到 0（估算阶段已排除该情形）。
	 */
	private void consumeCraftEnergy(Candidate candidate) {
		if (candidate.energies.isEmpty())
			return;
		// 直接用自守卫的 craftDebug（不要写 if (CRAFT_DEBUG)：常量 false 会让 javac 整块消除，
		// 打开开关后必须重新编译才生效，容易误导排查）
		craftDebug("电量扣减：本配方合计 {} FE，来源 {}（载荷余量 {} FE）", candidate.energyRequired(),
			describeEnergies(candidate.energies), payloadEnergy);
		for (EnergyDraw draw : candidate.energies) {
			int amount = draw.amount();
			if (amount <= 0)
				continue;
			if (draw.source() == EnergySource.PAYLOAD) {
				payloadEnergy = Math.max(0, payloadEnergy - amount);
				continue;
			}
			int taken = extractNearbyEnergy(draw.pos(), amount);
			if (taken < amount) {
				int shortfall = amount - taken;
				payloadEnergy = Math.max(0, payloadEnergy - shortfall);
				craftDebug("电量扣减：邻域 {} 仅取到 {} / {} FE，缺口 {} FE 转由载荷电量补",
					draw.pos() == null ? "?" : draw.pos()
						.toShortString(),
					taken, amount, shortfall);
			}
		}
	}

	/**
	 * 按来源扣减流体输入：
	 * <ul>
	 *   <li>{@code CONTAINER} → {@code containerFluid.drain(该流体×需求量, EXECUTE)}
	 *       （按种类+组件取，跨罐由能力实现自行分配）；容器缺失/抽空则安全跳过；</li>
	 *   <li>{@code PAYLOAD} → 从 {@link #payloadFluid} 扣该量（多条载荷流体按顺序累计扣减）。</li>
	 * </ul>
	 */
	private void consumeCraftFluid(Candidate candidate, IFluidHandler containerFluid) {
		for (FluidRef ref : candidate.fluids) {
			int amount = ref.fluid()
				.getAmount();
			if (amount <= 0)
				continue;
			if (ref.source() == FluidSource.CONTAINER) {
				if (containerFluid == null)
					continue;
				FluidStack drained = containerFluid.drain(ref.fluid()
					.copy(), IFluidHandler.FluidAction.EXECUTE);
				if (drained.getAmount() < amount)
					craftDebug("流体扣减：容器仅抽出 {} mB / 需 {} mB（并发变化）", drained.getAmount(), amount);
			} else {
				if (payloadFluid.getAmount() <= amount)
					payloadFluid = FluidStack.EMPTY;
				else
					payloadFluid.shrink(amount);
			}
		}
	}

	/**
	 * 逐个消耗候选的全部辅料（每个 ingredient 扣 1 件），<b>按来源分别扣</b>：
	 * <ul>
	 *   <li>{@code PAYLOAD} → 从 {@link #payloadItems} 扣 1；扣空即 {@code remove(idx)}，
	 *       会让<b>更大</b>的下标前移，故载荷下标先收集再<b>降序</b>处理
	 *       （排序只在同类来源内比较，容器槽号不参与，槽号扣减本身不引起下标漂移）；</li>
	 *   <li>{@code CONTAINER} → {@code handler.extractItem(slot, 1, false)}（槽号互不干扰，无需排序）；
	 *       取空/越界/无容器（掉落物路径）一律安全跳过；</li>
	 * </ul>
	 *
	 * <p><b>手持物类配方（deployer / ManualApplication）默认照样扣</b>（2026-09-11 修正，用户实测反馈）：
	 * 实物机械手确实不消耗手持物（一台机械手夹着一个齿轮能盖很久），但<b>波不是机械手</b>——它没有"手"，
	 * 每一击都得把辅料从容器/载荷里<b>物化</b>出来，所以必须真扣。旧实现直接跳过整条辅料扣减，
	 * 后果是：波抓着一份载荷辅料反复盖章，箱子里的原料永远不少（用户："直接也不从箱子里边抽物品，
	 * 就逮着一个…往机器里边加工"；也是更早那句"消耗了一份产物进行加工后，原材料却没有被消耗"的根因）。
	 * 需要回到"手持物免费"的旧口径时把配置 {@code wave.consumeHeldItemAux} 设为 false。</p>
	 *
	 * @param handler 命中容器（CONTAINER 来源必需；null 时该来源静默跳过）
	 */
	private void consumeAux(Candidate candidate, IItemHandler handler) {
		if (candidate.auxes.isEmpty())
			return;
		if (isHeldItemRecipe(candidate.recipe)
			&& !com.hjmmd_8.createoreexpansion.common.AllConfig.waveConsumeHeldItemAux) {
			craftDebug("辅料扣减：{} 属手持物类配方且 wave.consumeHeldItemAux=false → 辅料不消耗",
				candidate.id);
			return;
		}
		// 通道 A：载荷条目（降序扣减，防 remove 引起下标错位）
		List<Integer> payloadOrder = new ArrayList<>(2);
		for (AuxRef ref : candidate.auxes)
			if (ref.source() == AuxSource.PAYLOAD)
				payloadOrder.add(ref.index());
		payloadOrder.sort(null); // 自然升序；下面倒序遍历 = 降序处理
		for (int k = payloadOrder.size() - 1; k >= 0; k--) {
			int idx = payloadOrder.get(k);
			if (idx < 0 || idx >= payloadItems.size())
				continue; // 下标越界（并发变化）：跳过该件，不让异常扩散
			ItemStack aux = payloadItems.get(idx);
			aux.shrink(1);
			if (aux.isEmpty())
				payloadItems.remove(idx);
		}
		// 通道 B：命中容器槽（逐槽真取 1；取空表示槽已变，跳过）
		if (handler == null)
			return;
		for (AuxRef ref : candidate.auxes) {
			if (ref.source() != AuxSource.CONTAINER)
				continue;
			int slot = ref.index();
			if (slot < 0 || slot >= handler.getSlots())
				continue;
			ItemStack taken = handler.extractItem(slot, 1, false);
			if (taken.isEmpty())
				craftDebug("辅料扣减：容器槽 {} 已空（并发变化），跳过", slot);
		}
	}

	/**
	 * 尝试把物品插回槽位；<b>首选槽放不下时扫描整个容器</b>找空位/可叠加槽；仍失败返回剩余（调用方掉落）。
	 *
	 * <p>为什么必须扫整容器：主料槽里往往还剩着一堆原料（典型：一个槽里 64 个原木），
	 * 而产物与原料<b>不是同种物品</b>（去皮原木 ≠ 原木），{@code insertItem} 到该槽必然失败。
	 * 旧实现只试这一个槽 → 产物被直接掉到地上，玩家看到"加工出来的东西爆了一地"，
	 * 更糟的是<b>连锁加工因此断掉</b>——下一步（去皮原木 + 铜锭 → 铜机壳）需要在盆里
	 * 找到上一步的产物，而它已经不在盆里了（用户 2026-09 实测反馈）。</p>
	 */
	private static ItemStack insertBack(IItemHandler handler, int slot, ItemStack stack) {
		if (stack == null || stack.isEmpty())
			return ItemStack.EMPTY;
		ItemStack rest = handler.insertItem(slot, stack, false);
		if (rest.isEmpty())
			return ItemStack.EMPTY;
		for (int i = 0; i < handler.getSlots(); i++) {
			if (i == slot)
				continue;
			rest = handler.insertItem(i, rest, false);
			if (rest.isEmpty())
				return ItemStack.EMPTY;
		}
		return rest;
	}

	/** 方块上方掉落（产物放不下时）。 */
	private void dropAtBlock(BlockPos pos, ItemStack stack) {
		ItemEntity drop = new ItemEntity(level(), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, stack);
		drop.setDeltaMovement(Vec3.ZERO);
		level().addFreshEntity(drop);
	}

	/** 优先注入命中方块自身的流体槽；未耗尽部分返回。 */
	private FluidStack fillBlockOrNearby(BlockPos pos, FluidStack stack) {
		IFluidHandler self = level().getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
		if (self != null) {
			int done = self.fill(new FluidStack(stack.getFluid(), stack.getAmount()),
				IFluidHandler.FluidAction.EXECUTE);
			if (done >= stack.getAmount())
				return FluidStack.EMPTY;
			stack.shrink(done);
		}
		return stack;
	}

	/** 就近（命中点 1 格邻域）注罐：剩余无处可存则浪费。 */
	private void fillNearbyOnly(FluidStack stack) {
		int amount = stack.getAmount();
		for (BlockPos bp : radiusBlocks()) {
			IFluidHandler handler = level().getCapability(Capabilities.FluidHandler.BLOCK, bp, null);
			if (handler == null)
				continue;
			amount -= handler.fill(new FluidStack(stack.getFluid(), amount), IFluidHandler.FluidAction.EXECUTE);
			if (amount <= 0)
				return;
		}
	}

	/** 是否"手持物不消耗"类配方（deployer 手部/机械手类，类名兜底判定）。 */
	private static boolean isHeldItemRecipe(Recipe<?> recipe) {
		String name = recipe.getClass()
			.getName();
		return name.contains("ManualApplicationRecipe") || name.contains("DeployerApplicationRecipe");
	}

	// ================= 批次锁定（①：同一批同种物品产出稳定） =================

	/** 输入物品"种类 key"（注册 id 字符串；同种物品无论堆叠数量都视为同一批）。 */
	private static String inputKey(ItemStack stack) {
		if (stack.isEmpty())
			return "";
		ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return key == null ? "" : key.toString();
	}

	/**
	 * 从候选集中挑选本次要执行的候选（= {@link #orderCandidates} 的首位）。
	 *
	 * @see #orderCandidates 排序规则（批锁 → 输入种类 LRU → 专用度，绝不再随机）
	 */
	private Candidate pickCandidate(List<Candidate> candidates, ItemStack input, Candidate preferred) {
		List<Candidate> ordered = orderCandidates(candidates, input, preferred);
		return ordered.isEmpty() ? null : ordered.get(0);
	}

	/**
	 * 候选排序：把本次要执行的那条排在最前，其余按"<b>转速档匹配 → 专用度</b>"降序
	 * （见 {@link #compareCandidates} / {@link #compareSpecificity}）。
	 *
	 * <p>顺序规则：{@code preferred}（本次方块命中的批锁）→ 该输入种类最近一次成功的配方
	 * （波实体生命期内的有界 LRU）→ <b>转速档匹配者</b>（Vintage 抛光 {@code speed_limits}
	 * ↔ 波携带的变器转速档）→ <b>专用度最高者</b>。</p>
	 *
	 * <p><b>任何情况下都不再随机抽取</b>。原实现无锁时用 {@code random.nextInt} 随机挑一条，
	 * 而"同一输入同时匹配多条配方"恰恰是最常见的场景：铜锭既匹配"压成铜板"（1 输入）
	 * 又匹配"铜+锌→黄铜"（2 输入，需加热）；下界合金锭既匹配"辊成下界合金板"（1 输入）
	 * 又匹配"下界合金锭+钻石工具→下界合金工具"（2 输入）。随机抽取让后者大概率抽不到，
	 * 玩家看到的就是"放齐了料却搓不出黄铜／升级不生效"（用户 2026-09 实测反馈的两例）。</p>
	 */
	private List<Candidate> orderCandidates(List<Candidate> candidates, ItemStack input, Candidate preferred) {
		List<Candidate> rest = new ArrayList<>(candidates);
		List<Candidate> ordered = new ArrayList<>(candidates.size());
		if (preferred != null) {
			Candidate locked = candidateById(rest, preferred.id);
			if (locked != null) {
				ordered.add(locked);
				rest.remove(locked);
			}
		}
		if (ordered.isEmpty()) {
			String key = inputKey(input);
			Candidate locked = candidateById(rest, key.isEmpty() ? null : lastRecipeByInputKey.get(key));
			if (locked != null) {
				ordered.add(locked);
				rest.remove(locked);
			}
		}
		rest.sort(this::compareCandidates);
		ordered.addAll(rest);
		return ordered;
	}

	/**
	 * 候选比较（排序用，越小越优先）：<b>① 转速档匹配优先</b> → ② 专用度（{@link #compareSpecificity}）。
	 *
	 * <p>① 与 Vintage 自身口径一致：抛光配方 {@code speed_limits} 匹配当前转速档的进优选桶，
	 * 一条都不匹配时才用兜底桶（{@code GrinderBlockEntity} 的桶选择逻辑，去混淆字节码核对）。
	 * 这里用的是波携带的<b>变器转速</b>算出的档位；无转速要求的配方一律与"不匹配"同档位
	 * （Vintage 里它们同样落在兜底桶），于是不影响其它任何排序结论。</p>
	 */
	private int compareCandidates(Candidate a, Candidate b) {
		int bySpeedBand = Integer.compare(speedBandRank(a), speedBandRank(b));
		if (bySpeedBand != 0)
			return bySpeedBand;
		return compareSpecificity(a, b);
	}

	/** 候选的转速档排序档位：0 = 与波转速档匹配（优先）；1 = 无要求或档位不匹配（兜底）。 */
	private int speedBandRank(Candidate candidate) {
		int required;
		try {
			required = StellarWaveMachineIntegrations.recipeSpeedMode(candidate.recipe);
		} catch (Throwable ignored) {
			return 1; // 读取异常：按兜底处理，绝不让联动异常影响排序
		}
		if (required <= 0)
			return 1;
		return required == waveSpeedMode() ? 0 : 1;
	}

	/**
	 * 候选"专用度"比较（越小越优先）：
	 * <ol>
	 *   <li><b>物品输入数多者优先</b>——2 输入的搅拌/升级压过 1 输入的压制/辊压；</li>
	 *   <li>其次流体输入数多者优先；</li>
	 *   <li>最后按配方数据包 id 字典序——保证"同种输入 → 同一种产物"，不随 random 抖动。</li>
	 * </ol>
	 * 若玩家确实要走"不那么专用"的那条（例如只想把铜锭压成铜板），用工作盆的配方过滤器
	 * 把目标产物列进去即可：过滤器闸门在候选集生成之后、排序之前生效，语义不变。
	 */
	private static int compareSpecificity(Candidate a, Candidate b) {
		int byItems = Integer.compare(b.recipe.getIngredients()
			.size(), a.recipe.getIngredients()
				.size());
		if (byItems != 0)
			return byItems;
		int byFluids = Integer.compare(b.fluids.size(), a.fluids.size());
		if (byFluids != 0)
			return byFluids;
		String idA = a.id == null ? "" : a.id.toString();
		String idB = b.id == null ? "" : b.id.toString();
		return idA.compareTo(idB);
	}

	/** 成功加工后记录该输入种类的配方锁（供后续同种输入复用）。 */
	private void rememberCraftLock(ItemStack input, Candidate chosen) {
		String key = inputKey(input);
		if (!key.isEmpty() && chosen != null && chosen.id != null)
			lastRecipeByInputKey.put(key, chosen.id);
	}

	/** 按配方数据包 id 在候选集中找同一候选（找不到 = 该配方当前已不可执行 → null）。 */
	private static Candidate candidateById(List<Candidate> candidates, ResourceLocation id) {
		if (id == null)
			return null;
		for (Candidate c : candidates)
			if (id.equals(c.id))
				return c;
		return null;
	}

	// ================= 变形升级配方（③b：Vintage auto_upgrade） =================

	/**
	 * 是否"变形升级"配方：results 为空、产物 = 辅料原位升级（钻石工具 → 下界合金版）。
	 * Vintage 杵锤的 {@code auto_upgrade} 配方即此形态（RecipeApplier 拿不到物品结果）。
	 * 用配方类型 id 判定（Vintage 可选 mod，content 不得 import 其类）。
	 */
	private static boolean isUpgradeTransformationRecipe(Recipe<?> recipe) {
		if (!(recipe instanceof ProcessingRecipe<?, ?>))
			return false;
		ResourceLocation typeKey = typeKeyOf(recipe);
		return typeKey != null && "vintageimprovements".equals(typeKey.getNamespace())
			&& "auto_upgrade".equals(typeKey.getPath());
	}

	/**
	 * 由辅料（钻石工具）推导升级产物：把物品注册 id 路径中的 {@code diamond_} 替换为
	 * {@code netherite_}（helmet/chestplate/leggings/boots/sword/pickaxe/axe/shovel/hoe 全覆盖），
	 * 保留附魔等数据组件；映射不到有效物品返回 EMPTY（该候选不可执行）。
	 */
	@SuppressWarnings("deprecation") // BuiltInRegistries.ITEM.get(ResourceLocation)：NeoForge 旧注册表 API（仅有取用方式）
	private static ItemStack upgradeResultOf(ItemStack auxTool) {
		if (auxTool == null || auxTool.isEmpty())
			return ItemStack.EMPTY;
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(auxTool.getItem());
		if (id == null || !"minecraft".equals(id.getNamespace()))
			return ItemStack.EMPTY;
		String path = id.getPath();
		if (!path.startsWith("diamond_"))
			return ItemStack.EMPTY;
		ResourceLocation targetId =
			ResourceLocation.fromNamespaceAndPath("minecraft", "netherite_" + path.substring("diamond_".length()));
		Item target = BuiltInRegistries.ITEM.get(targetId); // 默认注册表：缺失项返回默认值 AIR
		if (target == null || target == Items.AIR)
			return ItemStack.EMPTY;
		// 等价"锻造升级"：换物品但保留附魔/自定义名等组件
		return new ItemStack(target.builtInRegistryHolder(), 1, auxTool.getComponentsPatch());
	}

	// ================= 锻造融合配方（③c：Vintage auto_smithing → 借原版锻造配方产出） =================

	/**
	 * 是否"锻造融合"配方（{@code vintageimprovements:auto_smithing}）：3 输入
	 * （锻造模板 → 可锻造盔甲 → 锻造材料），<b>results 为空</b>——真实产物来自原版锻造配方。
	 * 用配方类型 id 判定（Vintage 可选 mod，content 不得 import 其类）。
	 */
	private static boolean isAutoSmithingRecipe(Recipe<?> recipe) {
		if (!(recipe instanceof ProcessingRecipe<?, ?>))
			return false;
		ResourceLocation typeKey = typeKeyOf(recipe);
		return typeKey != null && "vintageimprovements".equals(typeKey.getNamespace())
			&& "auto_smithing".equals(typeKey.getPath());
	}

	/**
	 * 角色排列表：{@code {templateStacksIdx, baseStacksIdx, additionStacksIdx}}——
	 * {@code stacks} 内的 3 件物品在 {@link SmithingRecipeInput} 三个角色上的<b>全部 6 种分配</b>。
	 */
	private static final int[][] SMITHING_ROLE_ORDERS = {
		{ 0, 1, 2 }, { 0, 2, 1 }, { 1, 0, 2 }, { 1, 2, 0 }, { 2, 0, 1 }, { 2, 1, 0 }
	};

	/**
	 * <b>锻造融合（auto_smithing）真实产物推导</b>：该配方 JSON 的 {@code results} 为空，
	 * Vintage 的真实产物逻辑在机器实体里——{@code HelveBlockEntity} 持有 {@code lastSmithingRecipe}
	 * （原版 {@code net.minecraft.world.item.crafting.SmithingRecipe}），并构造原版
	 * {@link SmithingRecipeInput} 调 {@code assemble} 产出（就是"给盔甲加纹饰"）。变体波没有
	 * 机器实体，这里等价复用<b>原版锻造配方</b>本身：遍历 {@link RecipeType#SMITHING} 全部配方
	 * （含 {@code smithing_transform} 与 {@code smithing_trim} 两类），找到能匹配的就 assemble 出产物。
	 *
	 * <p><b>三件物品的角色映射依据（实证，非猜测）</b>：</p>
	 * <ol>
	 *   <li>{@code javap} 确认构造器签名为
	 *       {@code SmithingRecipeInput(ItemStack template, ItemStack base, ItemStack addition)}
	 *       （record 访问器 {@code template()/base()/addition()} 同序），即"模板、底材、附加材料"；</li>
	 *   <li>{@code auto_smithing.json} 的 ingredients 顺序为
	 *       {@code #minecraft:trim_templates} → {@code #minecraft:trimmable_armor} →
	 *       {@code #minecraft:trim_materials}，与上述 template/base/addition 顺序<b>一一对应</b>；
	 *       原版 {@code *_smithing_trim.json} 的字段顺序（template/base/addition）也完全一致；</li>
	 *   <li>Vintage 自己的 {@code HelveBlockEntity.processSmith()} 字节码显示：它<b>不按槽位顺序</b>，
	 *       而是用配方的 {@code isTemplateIngredient / isBaseIngredient / isAdditionIngredient}
	 *       逐件归类（归到 bufInv 的 0/1/2 号槽），再
	 *       {@code new SmithingRecipeInput(buf0, buf1, buf2)} + {@code assemble(input, level.registryAccess())}
	 *       + {@code matches(input, level())} 复核——即角色由<b>配方判定</b>决定，与来源槽位无关。</li>
	 * </ol>
	 * <p>因此本方法采用"<b>排列穷举 + matches 复核</b>"：把 {@code stacks = [主料, 辅料1, 辅料2]}
	 * 的 6 种角色分配逐个代入原版 {@code SmithingRecipe.matches(...)}（该方法即
	 * {@code template.test(t) && base.test(b) && addition.test(a)}），只要有一种成立即为正确映射，
	 * 比硬编码下标顺序更稳（等价于 Vintage 的角色判定，且以原版 matches 为准绳，不会张冠李戴）。</p>
	 *
	 * <p>找不到任何匹配配方（或 API 异常）→ 返回 {@code null}：调用方按"无产物可执行"放弃，
	 * <b>不消耗任何物品</b>，绝不硬造产物、不抛异常。</p>
	 *
	 * @param single   主料（命中物品/槽内物品，count 已置 1）
	 * @param candidate 已确认的候选（{@code auxes} 指向载荷或命中容器中的其余 2 件）
	 * @param handler  命中容器（解析 CONTAINER 来源辅料用；掉落物路径传 null）
	 */
	private List<ItemStack> smithingBridgeResult(ItemStack single, Candidate candidate, IItemHandler handler) {
		List<ItemStack> stacks = new ArrayList<>(1 + candidate.auxes.size());
		stacks.add(single);
		for (AuxRef ref : candidate.auxes) {
			ItemStack aux = resolveAux(ref, handler);
			if (aux.isEmpty())
				return null;
			ItemStack one = aux.copy();
			one.setCount(1);
			stacks.add(one);
		}
		if (stacks.size() < 3) {
			craftDebug("无产物：锻造融合需要 3 件（模板/盔甲/材料），实得 {} 件", stacks.size());
			return null;
		}
		try {
			for (RecipeHolder<SmithingRecipe> holder : level().getRecipeManager()
				.getAllRecipesFor(RecipeType.SMITHING)) {
				SmithingRecipe smithing = holder.value();
				if (smithing == null)
					continue;
				for (int[] roles : SMITHING_ROLE_ORDERS) {
					if (roles[0] >= stacks.size() || roles[1] >= stacks.size() || roles[2] >= stacks.size())
						continue;
					SmithingRecipeInput input = new SmithingRecipeInput(stacks.get(roles[0]), stacks.get(roles[1]),
						stacks.get(roles[2]));
					if (!smithing.matches(input, level()))
						continue; // 该角色分配不成立（原版 matches 即三角色 ingredient 全测）
					ItemStack out = smithing.assemble(input, level().registryAccess());
					if (out.isEmpty())
						continue;
					craftDebug("锻造融合产物：原版配方 {} 命中（角色分配 {}/{}/{}）→ {} ×{}", holder.id(), roles[0],
						roles[1], roles[2], out.getItem(), out.getCount());
					List<ItemStack> one = new ArrayList<>(1);
					one.add(out);
					return one;
				}
			}
		} catch (Throwable ignored) {
			// 配方管理器不可用 / 个别原版锻造配方 assemble 异常：按"无可执行产物"放弃（不消耗、不抛）
			return null;
		}
		craftDebug("无产物：原版 SMITHING 配方中找不到能匹配当前 3 件物品者（模板/盔甲/材料组合无效）");
		return null;
	}

	// ================= 机器环境前置条件（④） =================

	/** 环境判定扫描半径（格）：命中点 3×3×3 立方体（含自身格）。 */
	private static final int ENV_RADIUS = 1;

	/**
	 * 配方所需"机器环境"是否在命中点附近满足（④）：
	 * <ul>
	 *   <li><b>加热</b>：{@code ProcessingRecipe.getRequiredHeat()} = HEATED/SUPERHEATED 的配方
	 *       （搅拌加热、真空室加热、硫磺→SO₂ 等）→ <b>变器携带的热档</b>（扫描半径内的烈焰燃烧室，
	 *       见 {@link #carriedHeat}）<b>或</b>命中点/波源邻域内的烈焰人达标即可
	 *       （HEATED 需 KINDLED+，SUPERHEATED 需 SEETHING，复用 {@link HeatCondition#testBlazeBurner} 口径）；</li>
	 *   <li><b>压弯机头</b>：Vintage curving 配方 → 附近需有已装对应头的冲压机（{@link #nearbyCurvingPressSatisfies}）；</li>
	 *   <li><b>鼓风机媒介</b>：Splashing(水)/Haunting(灵魂火)/本模组嬗化(嬗变液) fan 配方 →
	 *       附近需有对应媒介（复用 Create {@link FanProcessingType#isValidAt} 判定入口，不另造规则）；</li>
	 * </ul>
	 * 普通配方恒通过；任何内部异常按"通过"保守处理（环境判定不应让波莫名停摆）。
	 */
	private boolean environmentSatisfied(BlockPos around, Recipe<?> recipe) {
		try {
			// 判定中心（任一满足即通过）：
			//   ① 命中点（被加工方块，通常是工作盆本身）
			//   ② 波源（变器出射面）——玩家会把烈焰人摆在变器旁边
			//   ③ 搅拌器所在格（若盆上有机械搅拌器）——"搅拌配方的热源贴着搅拌器放也行"
			List<BlockPos> centers = new ArrayList<>(3);
			centers.add(around);
			if (waveOrigin != null && !waveOrigin.equals(around))
				centers.add(waveOrigin);
			// ③ 搅拌器所在格：仅当命中点确实是工作盆、且盆上压着机械搅拌器时追加。
			// 注意：搅拌器<b>不是</b>加工前置条件（用户 2026-09 明确），这里只用它扩大热源覆盖范围。
			BlockPos mixer = isBasinAt(around) ? mixerPosAbove(around) : null;
			if (mixer != null && !centers.contains(mixer))
				centers.add(mixer);
			for (int i = 0; i < centers.size(); i++) {
				boolean last = i == centers.size() - 1;
				if (environmentSatisfiedAt(centers.get(i), recipe, last))
					return true;
			}
			return false;
		} catch (Throwable ignored) {
			// 判定异常（可选 mod 类缺失/未注册等）：按环境满足放行
			return true;
		}
	}

	/**
	 * 命中方块是否为<b>工作盆</b>（{@code BasinBlockEntity}）。
	 * 仅用于判断"该不该把盆上的搅拌器也算作一个环境判定中心"——搅拌器<b>不是</b>加工前置条件。
	 */
	private boolean isBasinAt(BlockPos pos) {
		try {
			return pos != null && !level().isClientSide && level().getBlockEntity(pos) instanceof BasinBlockEntity;
		} catch (Throwable ignored) {
			return false;
		}
	}

	/**
	 * 工作盆<b>正上方 1 格</b>的机械搅拌器位置；不是搅拌器返回 null。
	 *
	 * <p>只认正上方 1 格：Create 里搅拌器就是压在盆上的，用 3×3×3 会让一排搅拌器把整片区域
	 * 都变成环境判定中心，与摆法直觉不符。</p>
	 */
	private BlockPos mixerPosAbove(BlockPos basinPos) {
		if (basinPos == null)
			return null;
		try {
			BlockPos above = basinPos.above();
			return level().getBlockState(above)
				.is(com.simibubi.create.AllBlocks.MECHANICAL_MIXER.get()) ? above : null;
		} catch (Throwable ignored) {
			return null;
		}
	}

	/**
	 * 单个判定中心的环境检查（逻辑同旧版 {@code environmentSatisfied}）。
	 *
	 * @param report 本次是否为"最后一次尝试"：是才写加热不满足的轨迹日志，避免两个中心各刷一条
	 */
	private boolean environmentSatisfiedAt(BlockPos around, Recipe<?> recipe, boolean report) {
		if (recipe instanceof ProcessingRecipe<?, ?> pr && pr.getRequiredHeat() != HeatCondition.NONE) {
			if (!nearbyHeatSatisfies(around, pr.getRequiredHeat(), report))
				return false;
		}
		ResourceLocation typeKey = typeKeyOf(recipe);
		if (typeKey == null)
			return true;
		// Vintage 冲压（curving）：需要装了对应头的压弯机
		if ("vintageimprovements".equals(typeKey.getNamespace()) && "curving".equals(typeKey.getPath()))
			return nearbyCurvingPressSatisfies(around, recipe);
		// 鼓风机 fan 系（媒介在环境、不在配方字段）：水 / 灵魂火 / 嬗变液
		FanProcessingType fanType = fanMediaFor(typeKey);
		if (fanType != null && !nearbyMediaSatisfies(around, fanType))
			return false;
		return true;
	}

	/**
	 * 附近是否存在满足目标热档位的烈焰人（状态 HEAT_LEVEL ≥ 所需档位，与 Create 机器口径一致）。
	 *
	 * <p><b>两条来源，任一满足即可</b>：</p>
	 * <ol>
	 *   <li><b>变器携带的热档</b>（{@link #carriedHeat}）——变器扫描半径内点燃的烈焰燃烧室。
	 *       变器的读取半径最大 3 格，而本方法原先只找命中点/波源邻域 3×3×3（半径 1）；
	 *       燃烧室只要不是紧贴波的命中点或出射面就一律"查无烈焰人"，玩家看到的现象就是
	 *       "范围内明明点了火，波却说不满足加热"（见 {@link #carriedHeat} 的说明）；</li>
	 *   <li><b>命中点邻域现找</b>（原有行为，保留）：命中点/波源/搅拌器格 3×3×3 内有达标烈焰人。</li>
	 * </ol>
	 */
	private boolean nearbyHeatSatisfies(BlockPos center, HeatCondition required, boolean report) {
		// ① 变器携带的加热能力（把周围机器的能力"整合进波"：变器读到了火，波就带火）
		if (required.testBlazeBurner(carriedHeat))
			return true;
		BlazeBurnerBlock.HeatLevel best = BlazeBurnerBlock.HeatLevel.NONE;
		for (BlockPos bp : envBlocks(center)) {
			try {
				BlazeBurnerBlock.HeatLevel heat = BlazeBurnerBlock.getHeatLevelOf(level().getBlockState(bp));
				if (heat == BlazeBurnerBlock.HeatLevel.NONE)
					continue;
				if (required.testBlazeBurner(heat))
					return true;
				if (heat.ordinal() > best.ordinal())
					best = heat; // 记录邻域内最高档位，供热不满足时报告
			} catch (Throwable ignored) {
				// 单个方块判定异常：跳过
			}
		}
		// 走到这里 = 携带热档与两个中心邻域内都没有达标的烈焰人：把"哪里、携带了什么、需要什么、
		// 实际最高只有什么"写进轨迹，免得"搅拌加热配方不生效"到底是环境没过还是选了别的配方只能靠猜。
		if (report)
			craftTrace("加热环境不满足：命中点 {} 与波源 {} 邻域 3×3×3 内均无达标烈焰人"
				+ "（变器携带热档 = {}，此处最高热档 = {}）", center, waveOrigin, carriedHeat, best);
		return false;
	}

	/**
	 * 附近是否存在可承载该配方所需媒介的鼓风机 fan 判定位
	 * （逐一以命中点邻域方块调用 Create 现成 {@code FanProcessingType.isValidAt}）。
	 */
	private boolean nearbyMediaSatisfies(BlockPos center, FanProcessingType type) {
		for (BlockPos bp : envBlocks(center)) {
			try {
				if (type.isValidAt(level(), bp))
					return true;
			} catch (Throwable ignored) {
				// 单个位置判定异常：跳过
			}
		}
		return false;
	}

	/** fan 系配方的媒介类型（依配方类型 id；非 fan 系返回 null = 无环境要求）。 */
	private static FanProcessingType fanMediaFor(ResourceLocation typeKey) {
		String ns = typeKey.getNamespace();
		String path = typeKey.getPath();
		if ("create".equals(ns)) {
			if ("splashing".equals(path))
				return com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes.SPLASHING;
			if ("haunting".equals(path))
				return com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes.HAUNTING;
			return null;
		}
		if ("createoreexpansion".equals(ns) && "transmuting".equals(path))
			return com.hjmmd_8.createoreexpansion.common.AllFanProcessingTypes.TRANSMUTING;
		return null;
	}

	/**
	 * 附近是否存在装了<b>对应头</b>的 Vintage 冲压机（curving press）：
	 * 优先精确比对（配方 itemAsHead → 机器自定义头槽；否则配方 mode → 机器 mode）；
	 * 反射读取失败时保守降级为"存在已装任何头的压机即通过"（任务允许的保守退路）。
	 * 全程按类名/反射判定，不 import Vintage 类。
	 */
	private boolean nearbyCurvingPressSatisfies(BlockPos center, Recipe<?> recipe) {
		Item requiredHeadItem = null;
		Integer requiredMode = null;
		try {
			Object item = recipe.getClass()
				.getMethod("getItemAsHead")
				.invoke(recipe);
			if (item instanceof Item it && it != Items.AIR)
				requiredHeadItem = it;
		} catch (Throwable ignored) {
			// 无自定义头字段/反射失败
		}
		try {
			requiredMode = (Integer) recipe.getClass()
				.getMethod("getMode")
				.invoke(recipe);
		} catch (Throwable ignored) {
			// 无 mode 字段/反射失败
		}

		for (BlockPos bp : envBlocks(center)) {
			BlockEntity be = level().getBlockEntity(bp);
			if (be == null || !isCurvingPressEntity(be))
				continue;
			Integer machineMode = readPublicInt(be, "mode");
			if (machineMode == null || machineMode <= 0)
				continue; // 未装头
			// 需要特定物品头（nether_star 等"以物为头"配方）：mode=5（自定义头槽）且槽内物品一致
			if (requiredHeadItem != null) {
				if (machineMode == 5 && requiredHeadItem.equals(readHeadSlotItem(be)))
					return true;
				continue;
			}
			// 普通形状头配方：机器模式与配方 mode 一致即满足
			if (requiredMode != null && machineMode == requiredMode.intValue())
				return true;
			// 配方 mode 读不到（保守）：装了头就算满足
			if (requiredMode == null)
				return true;
		}
		// 附近没有与配方匹配的压机头
		return false;
	}

	/** 该方块实体是否为 Vintage 冲压机（仅按运行时类名判定，不加载其类）。 */
	private static boolean isCurvingPressEntity(BlockEntity be) {
		try {
			String name = be.getClass()
				.getName();
			return name.endsWith("CurvingPressBlockEntity")
				|| name.contains(".curving_press.CurvingPressBlockEntity");
		} catch (Throwable ignored) {
			return false;
		}
	}

	/** 反射读机器 public int 字段（mode）；失败返回 null。 */
	private static Integer readPublicInt(BlockEntity be, String field) {
		try {
			return (Integer) be.getClass()
				.getField(field)
				.get(be);
		} catch (Throwable ignored) {
			return null;
		}
	}

	/** 反射读机器自定义头槽（itemAsHead 槽 0）物品；失败返回 null。 */
	private static Item readHeadSlotItem(BlockEntity be) {
		try {
			Object inv = be.getClass()
				.getField("itemAsHead")
				.get(be);
			if (inv == null)
				return null;
			Object stack = inv.getClass()
				.getMethod("getStackInSlot", int.class)
				.invoke(inv, 0);
			if (stack instanceof ItemStack is && !is.isEmpty())
				return is.getItem();
			return null;
		} catch (Throwable ignored) {
			// SmartInventory 可能以 getItem(int) 暴露
			try {
				Object inv = be.getClass()
					.getField("itemAsHead")
					.get(be);
				Object stack = inv.getClass()
					.getMethod("getItem", int.class)
					.invoke(inv, 0);
				if (stack instanceof ItemStack is && !is.isEmpty())
					return is.getItem();
			} catch (Throwable ignored2) {
				// 反射失败
			}
			return null;
		}
	}

	/** 环境扫描立方体（3×3×3，含中心格）。 */
	private static Iterable<BlockPos> envBlocks(BlockPos center) {
		return BlockPos.betweenClosed(center.offset(-ENV_RADIUS, -ENV_RADIUS, -ENV_RADIUS),
			center.offset(ENV_RADIUS, ENV_RADIUS, ENV_RADIUS));
	}

	// ================= 载荷释放 =================

	/** 链用尽：释放剩余载荷（先入容器/储罐/储能，否则掉落/浪费）并消散。 */
	private void finishAndDiscard() {
		if (!payloadReleased) {
			releasePayload();
			payloadReleased = true;
		}
		ChargerWaveFx.burst(level(), position(), renderColor);
		discard();
	}

	/**
	 * 剩余载荷释放（链尽消散 / 撞墙 / 寿命耗尽等任何消散路径共用），口径由配置
	 * {@code wave.payloadRelease} 决定：
	 *
	 * <ul>
	 *   <li><b>{@code NEAREST_CONTAINER}（默认，用户 2026-09-11 拍板）</b>：物品存进
	 *       <b>击中方块周围、变器读取半径之内</b>最近的可存容器（按距离由近到远逐个试，
	 *       装满一个接着下一个）；流体就近注入同一范围内的储罐；电量就近充入同一范围内的储能。
	 *       范围取 {@link #carriedScanRadius}（= 变器穿波时的读取半径），圆心取
	 *       {@link #lastProcessedBlock}（最近加工过的方块；没有则用消散点）。
	 *       <b>工作盆与正在加工的那个方块不算容器</b>（2026-09 实测反馈：余料进盆是 bug）。</li>
	 *   <li>{@code SOURCE_CONTAINER}：物品还回实际取料的那几个容器（原箱），流体注回原储罐，
	 *       电量充回原储能；还不上时才落在来源方块上方（或消散点）。</li>
	 *   <li>{@code DROP_AT_DISSIPATION}：物品全部爆落在消散点，流体/电量就近注入。</li>
	 * </ul>
	 *
	 * <p><b>历史脉络</b>：最早"全爆在消散点"→ 掉落物被工作盆吸进去（用户实测反馈）；
	 * 改为"还回取料容器"→ 用户再定口径：余料应留在<b>加工现场</b>（范围内最近的容器），
	 * 而不是千里迢迢回原箱。</p>
	 */
	private void releasePayload() {
		if (level().isClientSide)
			return;
		com.hjmmd_8.createoreexpansion.common.AllConfig.PayloadRelease mode =
			com.hjmmd_8.createoreexpansion.common.AllConfig.wavePayloadRelease;
		BlockPos center = lastProcessedBlock != null ? lastProcessedBlock : blockPosition();
		for (ItemStack stack : payloadItems) {
			if (stack.isEmpty())
				continue;
			ItemStack rest;
			switch (mode) {
				case NEAREST_CONTAINER -> {
					rest = storeInNearestContainer(stack, center);
					if (!rest.isEmpty())
						rest = returnItemsToSources(rest); // 现场放不下：退回原箱
				}
				case SOURCE_CONTAINER -> rest = returnItemsToSources(stack);
				default -> rest = stack.copy();
			}
			if (rest.isEmpty())
				continue;
			// 还剩下：能定位到容器就落在它上方（绝不落进正在加工的容器），否则落在消散点
			BlockPos at = payloadSources.isEmpty() ? null : payloadSources.get(payloadSources.size() - 1);
			if (mode != com.hjmmd_8.createoreexpansion.common.AllConfig.PayloadRelease.DROP_AT_DISSIPATION
				&& at != null)
				dropAtBlock(at.above(), rest);
			else
				dropAtPosition(rest);
		}
		payloadItems.clear();
		if (!payloadFluid.isEmpty()) {
			FluidStack rest = switch (mode) {
				case NEAREST_CONTAINER -> fillNearestTank(payloadFluid, center);
				case SOURCE_CONTAINER -> returnFluidToSources(payloadFluid);
				default -> payloadFluid;
			};
			if (!rest.isEmpty())
				nearestFluidHandlerFill(rest); // 还放不下：就近尽力而为（仍无处可存则浪费）
		}
		payloadFluid = FluidStack.EMPTY;
		if (payloadEnergy > 0) {
			int left = switch (mode) {
				case NEAREST_CONTAINER -> chargeNearestStorage(payloadEnergy, center);
				case SOURCE_CONTAINER -> returnEnergyToSources(payloadEnergy);
				default -> payloadEnergy;
			};
			if (left > 0)
				nearestEnergyReceive(left);
		}
		payloadEnergy = 0;
		payloadSources.clear();
		lastProcessedBlock = null;
	}

	/** 在消散点落一件（老的"爆落"口径）。 */
	private void dropAtPosition(ItemStack stack) {
		ItemEntity drop = new ItemEntity(level(), getX(), getY(), getZ(), stack);
		drop.setDeltaMovement(Vec3.ZERO);
		level().addFreshEntity(drop);
	}

	/**
	 * 把余料存进"{@code center} 周围、{@link #carriedScanRadius} 之内"的<b>最近可存容器</b>：
	 * 按距离由近到远逐个试，能塞多少塞多少（塞满一个接着下一个），返回最终没塞进去的剩余。
	 *
	 * <p>跳过：{@code center} 自身（正在加工的那个方块）、工作盆、各类动能机器内部库存
	 * ——与"取料"同一套排除口径（{@link #payloadGatherSkip}），保证余料不会进盆。</p>
	 */
	private ItemStack storeInNearestContainer(ItemStack stack, BlockPos center) {
		ItemStack rest = stack.copy();
		if (center == null || rest.isEmpty())
			return rest;
		int radius = Math.max(1, carriedScanRadius);
		java.util.function.Predicate<BlockPos> skip = payloadGatherSkip(center);
		for (BlockPos pos : sortedByDistance(center, radius)) {
			if (skip.test(pos))
				continue;
			IItemHandler handler = level().getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (handler == null)
				continue;
			for (int slot = 0; slot < handler.getSlots() && !rest.isEmpty(); slot++)
				rest = handler.insertItem(slot, rest, false);
			if (rest.isEmpty())
				return ItemStack.EMPTY; // 最近的容器已经装下全部余料
		}
		return rest;
	}

	/** 把余料流体注进范围内最近的储罐；返回没注进去的部分。 */
	private FluidStack fillNearestTank(FluidStack stack, BlockPos center) {
		int left = stack.getAmount();
		if (center == null || left <= 0)
			return stack;
		java.util.function.Predicate<BlockPos> skip = payloadGatherSkip(center);
		for (BlockPos pos : sortedByDistance(center, Math.max(1, carriedScanRadius))) {
			if (skip.test(pos))
				continue;
			IFluidHandler tank = level().getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
			if (tank == null)
				continue;
			left -= tank.fill(new FluidStack(stack.getFluid(), left), IFluidHandler.FluidAction.EXECUTE);
			if (left <= 0)
				return FluidStack.EMPTY;
		}
		return new FluidStack(stack.getFluid(), Math.max(0, left));
	}

	/** 把余料电量充进范围内最近的储能；返回没充进去的 FE。 */
	private int chargeNearestStorage(int fe, BlockPos center) {
		int left = fe;
		if (center == null || left <= 0)
			return left;
		java.util.function.Predicate<BlockPos> skip = payloadGatherSkip(center);
		for (BlockPos pos : sortedByDistance(center, Math.max(1, carriedScanRadius))) {
			if (skip.test(pos))
				continue;
			IEnergyStorage storage = level().getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
			if (storage == null || !storage.canReceive())
				continue;
			left -= storage.receiveEnergy(left, false);
			if (left <= 0)
				return 0;
		}
		return Math.max(0, left);
	}

	/**
	 * "取料 / 余料入库"共用的排除口径：不打正在加工的那个方块（{@code center}）的主意，
	 * 工作盆与各类动能机器内部库存也不算可存容器（2026-09 实测：余料进盆/进机器是 bug）。
	 */
	private java.util.function.Predicate<BlockPos> payloadGatherSkip(BlockPos center) {
		return pos -> {
			if (pos.equals(center))
				return true;
			try {
				net.minecraft.world.level.block.entity.BlockEntity be = level().getBlockEntity(pos);
				return be instanceof BasinBlockEntity
					|| be instanceof com.simibubi.create.content.kinetics.base.KineticBlockEntity;
			} catch (Throwable ignored) {
				return true; // 判定异常：保守跳过
			}
		};
	}

	/** 以 {@code center} 为圆心、半径 {@code r} 的立方体，按到圆心的距离由近到远排序。 */
	private static List<BlockPos> sortedByDistance(BlockPos center, int r) {
		List<BlockPos> list = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r)))
			list.add(pos.immutable());
		list.sort(java.util.Comparator.comparingDouble(center::distSqr));
		return list;
	}

	/** 把载荷物品还回取料容器（最新来源优先，逐槽全扫）；返回还没还进去的剩余。 */
	private ItemStack returnItemsToSources(ItemStack stack) {
		ItemStack rest = stack.copy();
		for (int i = payloadSources.size() - 1; i >= 0 && !rest.isEmpty(); i--) {
			IItemHandler handler = level().getCapability(Capabilities.ItemHandler.BLOCK, payloadSources.get(i), null);
			if (handler == null)
				continue;
			for (int slot = 0; slot < handler.getSlots() && !rest.isEmpty(); slot++)
				rest = handler.insertItem(slot, rest, false);
		}
		return rest;
	}

	/** 把载荷流体注回取料储罐（最新来源优先）；返回还剩余量（EMPTY = 全部还回）。 */
	private FluidStack returnFluidToSources(FluidStack stack) {
		int left = stack.getAmount();
		for (int i = payloadSources.size() - 1; i >= 0 && left > 0; i--) {
			IFluidHandler tank = level().getCapability(Capabilities.FluidHandler.BLOCK, payloadSources.get(i), null);
			if (tank == null)
				continue;
			left -= tank.fill(new FluidStack(stack.getFluid(), left), IFluidHandler.FluidAction.EXECUTE);
		}
		return left <= 0 ? FluidStack.EMPTY : new FluidStack(stack.getFluid(), left);
	}

	/** 把载荷电量充回取料储能（最新来源优先）；返回没充进去的剩余 FE。 */
	private int returnEnergyToSources(int fe) {
		int left = fe;
		for (int i = payloadSources.size() - 1; i >= 0 && left > 0; i--) {
			IEnergyStorage storage = level().getCapability(Capabilities.EnergyStorage.BLOCK, payloadSources.get(i), null);
			if (storage == null || !storage.canReceive())
				continue;
			left -= storage.receiveEnergy(left, false);
		}
		return Math.max(0, left);
	}

	/** 尝试把物品插入命中点邻域容器（当前仅用于"产物放回容器"等场景，载荷释放不再使用）。 */
	private ItemStack insertIntoNearestContainer(ItemStack stack) {
		ItemStack leftover = stack.copy();
		for (BlockPos bp : radiusBlocks()) {
			IItemHandler handler = level().getCapability(Capabilities.ItemHandler.BLOCK, bp, null);
			if (handler == null)
				continue;
			leftover = handler.insertItem(0, leftover, false);
			if (leftover.isEmpty())
				return ItemStack.EMPTY;
		}
		return leftover;
	}

	private void nearestFluidHandlerFill(FluidStack stack) {
		int amount = stack.getAmount();
		for (BlockPos bp : radiusBlocks()) {
			IFluidHandler handler = level().getCapability(Capabilities.FluidHandler.BLOCK, bp, null);
			if (handler == null)
				continue;
			amount -= handler.fill(new FluidStack(stack.getFluid(), amount), IFluidHandler.FluidAction.EXECUTE);
			if (amount <= 0)
				return;
		}
	}

	private void nearestEnergyReceive(int fe) {
		int left = fe;
		for (BlockPos bp : radiusBlocks()) {
			IEnergyStorage storage = level().getCapability(Capabilities.EnergyStorage.BLOCK, bp, null);
			if (storage == null || !storage.canReceive())
				continue;
			left -= storage.receiveEnergy(left, false);
			if (left <= 0)
				return;
		}
	}

	/** 命中点周围 1 格邻域（不含自身所在格）。 */
	private List<BlockPos> radiusBlocks() {
		List<BlockPos> list = new ArrayList<>();
		BlockPos c = blockPosition();
		for (int dx = -1; dx <= 1; dx++)
			for (int dy = -1; dy <= 1; dy++)
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos bp = c.offset(dx, dy, dz);
					if (!bp.equals(c))
						list.add(bp);
				}
		return list;
	}

	@Override
	public void remove(RemovalReason reason) {
		if (reason == RemovalReason.DISCARDED && !level().isClientSide && !payloadReleased) {
			payloadReleased = true;
			releasePayload(); // 寿命/撞墙等任何消散：剩余载荷也要落地
		}
		super.remove(reason);
	}

	// ================= 分裂继承 =================

	/** 差波器分裂：生成继承属性集与载荷的变体子波。 */
	@Override
	protected AbstractChargerWaveEntity createChildWave(Vec3 pos, Vec3 dir, int level) {
		StellarWaveEntity child = new StellarWaveEntity(level(), pos, dir, level);
		child.attributes = new ArrayList<>(attributes);
		child.recipeTypes = new ArrayList<>(recipeTypes);
		child.carriedHeat = carriedHeat;
		child.carriedRpm = carriedRpm;
		child.carriedScanRadius = carriedScanRadius;
		child.payloadItems = new ArrayList<>(payloadItems);
		child.payloadFluid = payloadFluid.copy();
		child.payloadEnergy = payloadEnergy;
		child.payloadSources = new ArrayList<>(payloadSources);
		child.rodCharges = rodCharges;
		child.chainLeft = chainLeft;
		return child;
	}

	// ========== NBT（属性集随波实体保存/恢复；载荷为运行态，不落盘） ==========

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		ListTag list = new ListTag();
		for (ResourceLocation id : attributes)
			list.add(StringTag.valueOf(id.toString()));
		tag.put("WaveAttributes", list);
		// 变器携带的加热档位（序号；与 attributes 同为"能力快照"，随实体存读）
		tag.putInt("WaveCarriedHeat", carriedHeat.ordinal());
		// 变器携带的加工转速（RPM；转速档优先级的依据）
		tag.putFloat("WaveCarriedRpm", carriedRpm);
		// 变器当时的读取半径（= 命中后"就地补料"的生效半径）
		tag.putInt("WaveCarriedRadius", carriedScanRadius);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		attributes = new ArrayList<>();
		ListTag list = tag.getList("WaveAttributes", Tag.TAG_STRING);
		for (int i = 0; i < list.size(); i++)
			attributes.add(ResourceLocation.tryParse(list.getString(i)));
		carriedHeat = HeatLevelNames.byOrdinal(tag.getInt("WaveCarriedHeat"));
		carriedRpm = Math.abs(tag.getFloat("WaveCarriedRpm"));
		carriedScanRadius = Math.max(1, tag.getInt("WaveCarriedRadius"));
	}

	/**
	 * 辅料来源通道（2026-09 双通道；同轮<b>优先级反转</b>为"容器优先、载荷兜底"）：
	 * <ul>
	 *   <li>{@link #CONTAINER}：<b>命中容器自身的其它槽</b>（工作盆/置物台里和主料同一批的物品）——
	 *       仅方块槽路径可用（{@code handler != null}），索引即槽号，<b>优先使用</b>；</li>
	 *   <li>{@link #PAYLOAD}：波载荷 {@link #payloadItems}——变器扫描圈内非加工机容器的抽取物，
	 *       掉落物路径唯一来源，容器凑不齐时兜底。</li>
	 * </ul>
	 */
	private enum AuxSource {
		CONTAINER, PAYLOAD
	}

	/** 一条辅料引用：来源通道 + 该通道内的下标（容器槽号 / 载荷条目下标）。 */
	private record AuxRef(AuxSource source, int index) {
	}

	/** 流体输入来源通道：{@link #CONTAINER} = 命中容器的流体槽（优先），{@link #PAYLOAD} = 波载荷流体（兜底）。 */
	private enum FluidSource {
		CONTAINER, PAYLOAD
	}

	/**
	 * 一条流体输入需求：来源通道 + 所需流体（<b>种类与组件 + 需求量</b>，用 {@code copyWithAmount}
	 * 从命中的罐内流体/载荷流体复制而来，故带组件时也能精确 drain）。
	 */
	private record FluidRef(FluidSource source, FluidStack fluid) {
	}

	/**
	 * 电量来源通道（"电量类比成一种特殊的辅料"，2026-09）：{@link #NEARBY} = 命中点邻域的可抽储能
	 * （通用 {@code IEnergyStorage} / CC&amp;A 特斯拉线圈；<b>优先</b>），{@link #PAYLOAD} = 波载荷电量（兜底）。
	 * 与 {@code AuxSource}/{@code FluidSource} 的"就近优先、载荷兜底"优先级完全对称。
	 */
	private enum EnergySource {
		NEARBY, PAYLOAD
	}

	/**
	 * 一条电量需求：来源通道 + 需求量（FE）+ 供能方块位置（{@code NEARBY} 专用，需长期持有故为
	 * {@code immutable()}；{@code PAYLOAD} 为 null）。
	 * <p>与 {@link AuxRef}（物品：来源+下标）、{@link FluidRef}（流体：来源+种类/量）构成三类
	 * "资源引用"——同一套"就近优先、载荷兜底 + 按来源扣减"的模型。</p>
	 */
	private record EnergyDraw(EnergySource source, int amount, BlockPos pos) {
	}

	/**
	 * 命中候选：配方数据包 id（批次锁定稳定标识）+ 配方 + <b>辅料引用列表</b> + <b>流体输入引用列表</b>
	 * + <b>电量需求引用列表</b>——三类"资源引用"同构，都是"就近来源优先、波载荷兜底、按来源扣减"。
	 *
	 * <p><b>辅料引用列表</b>（2026-09 由单个 {@code int auxIndex} → 载荷下标列表 → 双通道引用列表）：
	 * 单条配方可有 1~{@link #MAX_ITEM_INPUTS} 个物品输入，主料取命中物品，其余每个 ingredient
	 * 各需 1 件辅料，可分别来自命中容器其它槽（优先）或波载荷。列表<b>按 ingredient 升序压缩存储</b>：
	 * {@code auxes.get(k)} 即 {@code recipe.getIngredients().get(k+1)} 所用的那件辅料——
	 * 例如 3 输入锻造融合里 {@code get(0)} = 可锻造盔甲、{@code get(1)} = 锻造材料，主料 = 锻造模板。
	 * 空列表 = 单输入无辅料。</p>
	 *
	 * <p><b>流体输入引用列表</b>（2026-09 新增）：每条流体 ingredient 一条记录，来源同样是
	 * "容器流体槽优先、载荷流体兜底"，按 ingredient 升序存储；空列表 = 无流体输入。</p>
	 *
	 * <p><b>电量需求引用列表</b>（2026-09 新增）：配方电量需求是个总量（{@code recipeEnergyRequired}），
	 * 但可以由<b>多个邻域储能合力 + 载荷兜底</b>凑出，故按来源拆成若干条
	 * {@link EnergyDraw}（各带位置与额度）；空列表 = 该配方不耗电。</p>
	 *
	 * <p><b>族配方归属</b>（2026-09 新增）：{@code familyRecipe} 非空表示本候选来自"步骤族"路径
	 * （序列装配）——{@code recipe} 是该装配的<b>下一步成品配方</b>（用于辅料/流体/电量门槛），
	 * 而产物推导与后续排序口径仍按族配方（{@link WaveRecipeFamilies}）走。</p>
	 */
	private record Candidate(ResourceLocation id, Recipe<?> recipe, List<AuxRef> auxes, List<FluidRef> fluids,
		List<EnergyDraw> energies, Recipe<?> familyRecipe) {

		/** 产物推导应走的"族配方"：步骤族候选返回装配配方，普通候选返回自身。 */
		Recipe<?> familyTarget() {
			return familyRecipe == null ? recipe : familyRecipe;
		}

		/** 首个辅料引用（"辅料即产物来源"的变形升级/锻造融合推导入口；无辅料返回 null）。 */
		AuxRef firstAux() {
			return auxes.isEmpty() ? null : auxes.get(0);
		}

		/** 合计电量需求（FE；0 = 不耗电）。 */
		int energyRequired() {
			int sum = 0;
			for (EnergyDraw draw : energies)
				sum += draw.amount();
			return sum;
		}
	}
}
