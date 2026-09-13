package com.hjmmd_8.createoreexpansion.content.charger.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.common.AllEntityTypes;
import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.hjmmd_8.createoreexpansion.content.charger.craft.family.WaveRecipeFamilies;
import com.hjmmd_8.createoreexpansion.content.charger.craft.Candidate;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveAuxResolver;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveCandidateOrdering;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveCraftResults;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveEnvironmentChecks;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.AuxRef;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.AuxSource;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.EnergyDraw;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.EnergySource;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.FluidRef;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.FluidSource;
import com.hjmmd_8.createoreexpansion.content.charger.payload.WavePayloadGather;
import com.hjmmd_8.createoreexpansion.content.charger.payload.WavePayloadRelease;
import com.hjmmd_8.createoreexpansion.content.lightning.ReinforcedLightningRodEffects;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveMachineIntegrations;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.StellarWaveMachineRegistry;
import com.hjmmd_8.createoreexpansion.util.HeatLevelNames;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
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

	/** 配方类型 id 字符串（诊断日志用；取不到返回 {@code "?"}）。实现见 {@link WaveCraftResults#typeKeyString}。 */
	private static String typeKeyString(Recipe<?> recipe) {
		return WaveCraftResults.typeKeyString(recipe);
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
			// 电量按"载荷已有量"算上限（与物品/流体同口径，2026-09 审计修复）：
			// 旧实现把上限当成"本次新增额度"，于是每次命中补料都能再吃满一个上限，载荷可超上限。
			int energyCap = WavePayloadGather.resolveEnergyCap(level());
			int energyRoom = energyCap < 0 ? -1 : Math.max(0, energyCap - payloadEnergy);
			if (energyRoom != 0)
				payloadEnergy += WavePayloadGather.gatherEnergy(level(), center, carriedScanRadius, taken, false, skip,
					energyRoom);
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
		if (items != null) {
			// 深拷贝（2026-09 审计修复）：只包一层列表会让波与变器的 auxItems 共用同一批可变 ItemStack，
			// 波消耗载荷时会同步改小变器的护目镜读数（"辅料载荷 n 个"在两轮扫描之间偏小）。
			this.payloadItems = new ArrayList<>(items.size());
			for (ItemStack stack : items)
				if (stack != null && !stack.isEmpty())
					this.payloadItems.add(stack.copy());
		}
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

		// 强化避雷针释放机会：本次命中最多引一道雷（2026-09 审计修复）。
		// 旧实现逐件掉落物都调一次：一次 sweep 命中多件时会把引雷额度全烧光、在多处各落一道雷，
		// 且逐件复位 strikeOwnsTypes 会让"同一件物品只被雷/波加工一遍"的护栏对后续掉落物失效。
		// 这里用 strikeOwnsTypes 兼作"本次命中已引雷"，不再逐件复位（方块路径每次命中仍会复位）。
		if (!strikeOwnsTypes)
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
			//    WaveCraftResults.compute 算出真实产物再测。注意这里**不再以 declared 为空为前提**——
			//    否则"声明了产物但推导产物才是真产物"的配方一旦 declared 不在过滤器内就直接被判死，
			//    表现为"设了过滤器就不加工，清空过滤器又好了"（用户 2026-09 实测反馈）。
			if (input != null && !input.isEmpty()) {
				ItemStack probe = input.copy();
				probe.setCount(1);
				List<ItemStack> derived = WaveCraftResults.compute(craftResultsContext(), candidate, probe, handler,
					around);
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
			AuxRef aux = auxResolver().findAux(recipe.getIngredients()
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
		List<FluidRef> fluidRefs = auxResolver().resolveFluidRefs(recipe, containerFluid);
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
		List<EnergyDraw> energies = auxResolver().resolveEnergyDraws(energyRequired, around);
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
			input.getItem(), WaveAuxResolver.describeAuxes(auxes), WaveAuxResolver.describeFluids(fluidRefs), WaveAuxResolver.describeEnergies(energies));
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
	public void onSolidBlockHit(BlockPos pos) {
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
			ItemStack aux = auxResolver().resolveAux(auxes.get(k), handler);
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

	/** 配方类型 id（BuiltIn 注册表查 key；查不到返回 null）。实现见 {@link WaveCraftResults#typeKeyOf}。 */
	private static ResourceLocation typeKeyOf(Recipe<?> recipe) {
		return WaveCraftResults.typeKeyOf(recipe);
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
			ItemStack stack = auxResolver().resolveAux(auxes.get(slot), handler);
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
		List<ItemStack> results = WaveCraftResults.compute(craftResultsContext(), candidate, single, null,
			item.blockPosition());
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
			for (FluidStack fr : WaveCraftResults.familyFluidResults(level(), candidate, single, item.blockPosition()))
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
	public boolean handleItemInventoryBlock(IItemHandler handler, BlockPos pos) {
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
			WaveCraftResults.describeHandler(handler), attributes.size(), recipeTypes.size());
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
			List<ItemStack> results = WaveCraftResults.compute(craftResultsContext(), chosen, probe.copy(), handler, pos);
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
			int target = WaveCraftResults.productSlotHint(chosen, slot);
			if (target < 0 || target >= handler.getSlots())
				target = slot; // 防御：槽号失效则回主料槽
			if (target != slot)
				craftDebug("产物回位：优先槽 {}（辅料槽），主料槽 {}；辅料 {}", target, slot, WaveAuxResolver.describeAuxes(chosen.auxes));
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
				for (FluidStack fr : WaveCraftResults.familyFluidResults(level(), chosen, probe, pos))
					if (!fr.isEmpty()) {
						FluidStack rest = fillBlockOrNearby(pos, fr.copy());
						if (!rest.isEmpty())
							fillNearbyOnly(rest);
					}
			}
			craftTrace("加工 {} → 选中 {} [{}]（候选 {} 条，辅料 {}）产出 {}{}", input.getItem(), chosen.id,
				typeKeyString(chosen.recipe), ordered.size(), WaveAuxResolver.describeAuxes(chosen.auxes),
				WaveCraftResults.describeResults(results),
				describeHeat(pos, chosen.recipe));
			chainLeft--;
			return chosen;
		}
		craftTrace("槽 {} 的 {} 匹配到 {} 条候选但全部推不出产物 → 本槽放弃", slot, probe.getItem(), ordered.size());
		return null;
	}

	// 产物列表描述（describeResults）/ 容器内容摘要（describeHandler）/ 产物回位提示（productSlotHint）/
	// 配方类型判定（isUpgradeTransformationRecipe 等）已随"产物推导"一族搬入 WaveCraftResults。

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
		for (BlockPos bp : WaveAuxResolver.envBlocks(center)) {
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

	// 产物回位目标槽（⑥，productSlotHint）的实现已搬入 WaveCraftResults（见其 javadoc：
	// "辅料即产物来源"的推导类配方——auto_upgrade / auto_smithing——产物优先放回首辅料所在的容器槽）。

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
			WaveAuxResolver.describeEnergies(candidate.energies), payloadEnergy);
		for (EnergyDraw draw : candidate.energies) {
			int amount = draw.amount();
			if (amount <= 0)
				continue;
			if (draw.source() == EnergySource.PAYLOAD) {
				payloadEnergy = Math.max(0, payloadEnergy - amount);
				continue;
			}
			int taken = auxResolver().extractNearbyEnergy(draw.pos(), amount);
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
	 *
	 * <p><b>并发缺口回流补齐（2026-09，与电量侧同口径）</b>：门槛解析与实际扣减之间容器可能变了
	 * （同 tick 内别的机器抽走了盆里的水、或能力实现按罐限流只给一半），此时若"少扣流体却照常出产物"
	 * 就是白嫖加工。所以缺口 {Amount − drained} 由<b>载荷流体补扣</b>——与
	 * {@link #consumeCraftEnergy} 的"邻域取电不足 → 缺口转由载荷电量补"完全对称。</p>
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
				int shortfall = amount - drained.getAmount();
				if (shortfall > 0) {
					int covered = Math.min(shortfall, Math.max(0, payloadFluid.getAmount()));
					if (covered > 0) {
						if (payloadFluid.getAmount() <= covered)
							payloadFluid = FluidStack.EMPTY;
						else
							payloadFluid.shrink(covered);
					}
					craftDebug("流体扣减：容器仅抽出 {} mB / 需 {} mB，缺口 {} mB 转由载荷流体补（实补 {} mB）",
						drained.getAmount(), amount, shortfall, covered);
				}
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

	/** 命中点周围 1 格邻域（不含自身所在格）；产物流体"就近注罐"与载荷兜底共用。 */
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

	/** 产物流体"就近注罐"：命中点 1 格邻域逐个填，剩余无处可存即浪费（产物不需要"排除口径"）。 */
	private void nearestFluidHandlerFill(FluidStack stack) {
		int amount = stack.getAmount();
		for (BlockPos bp : radiusBlocks()) {
			IFluidHandler handler = level().getCapability(Capabilities.FluidHandler.BLOCK, bp, null);
			if (handler == null)
				continue;
			try {
				amount -= handler.fill(new FluidStack(stack.getFluid(), amount), IFluidHandler.FluidAction.EXECUTE);
			} catch (Throwable ignored) {
				// 单个储罐异常：换下一个（2026-09 审计修复：不让第三方能力异常冒到 tick）
			}
			if (amount <= 0)
				return;
		}
	}

	/** 产物电量"就近注入"：命中点 1 格邻域逐个充，剩余无处可存即浪费。 */
	private void nearestEnergyReceive(int fe) {
		int left = fe;
		for (BlockPos bp : radiusBlocks()) {
			IEnergyStorage storage = level().getCapability(Capabilities.EnergyStorage.BLOCK, bp, null);
			if (storage == null || !storage.canReceive())
				continue;
			try {
				left -= storage.receiveEnergy(left, false);
			} catch (Throwable ignored) {
				// 单个储能异常：换下一个
			}
			if (left <= 0)
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
	 * 候选排序的<b>状态入参</b>：该输入种类最近一次成功的配方（有界 LRU，状态保存在本类）。
	 * 排序规则本身是纯策略，见 {@link WaveCandidateOrdering}（不持字段、可单独推理与验证）。
	 */
	private ResourceLocation rememberedRecipeId(ItemStack input) {
		String key = inputKey(input);
		return key.isEmpty() ? null : lastRecipeByInputKey.get(key);
	}

	/** 挑选本次要执行的候选（排序策略见 {@link WaveCandidateOrdering#pick}）。 */
	private Candidate pickCandidate(List<Candidate> candidates, ItemStack input, Candidate preferred) {
		return WaveCandidateOrdering.pick(candidates, preferred, rememberedRecipeId(input), waveSpeedMode());
	}

	/** 候选排序（排序策略见 {@link WaveCandidateOrdering#order}）。 */
	private List<Candidate> orderCandidates(List<Candidate> candidates, ItemStack input, Candidate preferred) {
		return WaveCandidateOrdering.order(candidates, preferred, rememberedRecipeId(input), waveSpeedMode());
	}

	/** 成功加工后记录该输入种类的配方锁（供后续同种输入复用）。 */
	private void rememberCraftLock(ItemStack input, Candidate chosen) {
		String key = inputKey(input);
		if (!key.isEmpty() && chosen != null && chosen.id != null)
			lastRecipeByInputKey.put(key, chosen.id);
	}

	// 变形升级 / 锻造融合配方（③b auto_upgrade / ③c auto_smithing）的判定与产物推导已随
	// "产物推导"一族搬入 WaveCraftResults（isUpgradeTransformationRecipe / upgradeResultOf /
	// isAutoSmithingRecipe / smithingBridgeResult）。

	// ================= 机器环境前置条件（④） =================

	/**
	 * 配方所需"机器环境"是否在命中点附近满足（④）：加热 / 压弯机头 / 鼓风机媒介。
	 *
	 * <p>判定规则、"两个中心 + 搅拌器格"的取法、以及压弯机头与 fan 媒介的判定全部在
	 * {@link WaveEnvironmentChecks}（纯静态；携带热档与波源位置由这里传入）。</p>
	 */
	private boolean environmentSatisfied(BlockPos around, Recipe<?> recipe) {
		return WaveEnvironmentChecks.satisfied(level(), around, waveOrigin, recipe, carriedHeat,
			(msg, args) -> craftTrace(msg, args));
	}

	/** 链用尽：释放剩余载荷（先入容器/储罐/储能，否则掉落/浪费）并消散。 */
	private void finishAndDiscard() {
		if (!payloadReleased) {
			payloadReleased = true;
			try {
				releasePayload();
			} catch (Throwable t) {
				// 同 remove()：第三方能力异常不得打断消散流程（2026-09 审计修复）
				craftDebug("载荷释放异常（余料未能全部处置）：{}", t);
			}
		}
		ChargerWaveFx.burst(level(), position(), getRenderColor());
		discard();
	}

	// ================= 载荷释放（实现见 payload/WavePayloadRelease） =================

	/**
	 * 剩余载荷释放（链尽消散 / 撞墙 / 寿命耗尽等任何消散路径共用）：把运行态打包成一次请求交给
	 * {@link WavePayloadRelease}，并在<b>任何情况下</b>清空载荷状态（{@code finally}）。
	 *
	 * <p>口径（三种模式）、排除口径（绝不进工作盆/加工机）、异常隔离都在那个类里，见其类注释
	 * 与设计文档 §11.1。这里只做"取值 + 打包 + 兜底清理"。</p>
	 */
	private void releasePayload() {
		if (level().isClientSide)
			return;
		try {
			WavePayloadRelease.release(new WavePayloadRelease.Request(level(), AllConfig.wavePayloadRelease,
				position(), payloadItems, payloadFluid, payloadEnergy, payloadSources,
				lastProcessedBlock != null ? lastProcessedBlock : blockPosition(),
				Math.max(1, carriedScanRadius)));
		} catch (Throwable t) {
			// 第三方容器/储能的能力实现抛异常：绝不把异常从实体移除流程抛到服务端 tick
			// （2026-09 审计修复）。余料按"这次没放进去"处理。
			craftDebug("载荷释放异常（余料未能全部处置）：{}", t);
		} finally {
			payloadItems.clear();
			payloadFluid = FluidStack.EMPTY;
			payloadEnergy = 0;
			payloadSources.clear();
			lastProcessedBlock = null;
		}
	}

	/**
	 * "取料 / 余料入库 / 退还原箱"共用的排除口径：不打正在加工的那个方块（{@code center}）的主意；
	 * 工作盆、目录登记的加工机（含注液器/物品排放器这类<b>非动能</b>机）与动能方块一律不算可存目标。
	 * 判定实现收敛在 {@link WavePayloadRelease#isStoreTarget}（变器扫描与波侧同源）。
	 */
	/** 杈呮枡瑙ｆ瀽鍣細鎸夊綋鍓嶈浇鑽风幇鍦烘瀯閫狅紙杞借嵎鍒楄〃鎸夊紩鐢ㄨ鍙栵紝鏁呭垪琛ㄥ唴瀹瑰彉鍖栫珛鍗冲彲瑙侊級銆?*/
	private WaveAuxResolver auxResolver() {
		return new WaveAuxResolver(level(), payloadItems, payloadFluid, payloadEnergy);
	}

	/** 产物推导的实体侧上下文（每次现场构造：世界 + 辅料解析器 + 调试日志出口）。
	 *  {@code craftDebug} 是静态方法，故调试出口用 lambda 绑定（{@code this::craftDebug} 对静态方法不合法）。 */
	private WaveCraftResults.Context craftResultsContext() {
		return new WaveCraftResults.Context(level(), auxResolver(), (msg, args) -> craftDebug(msg, args));
	}

	private java.util.function.Predicate<BlockPos> payloadGatherSkip(BlockPos center) {
		return pos -> pos.equals(center) || !WavePayloadRelease.isStoreTarget(level(), pos);
	}
	@Override
	public void remove(RemovalReason reason) {
		if (reason == RemovalReason.DISCARDED && !level().isClientSide && !payloadReleased) {
			payloadReleased = true;
			try {
				releasePayload(); // 寿命/撞墙等任何消散：剩余载荷也要落地
			} catch (Throwable t) {
				// 第三方容器/储能的能力实现抛异常时，绝不把异常从实体移除流程里抛出去
				// （2026-09 审计修复：否则会从 tick 冒到服务端 tick）。余料按"这次没放进去"处理。
				craftDebug("载荷释放异常（余料未能全部处置）：{}", t);
			}
		}
		super.remove(reason);
	}

	// ================= 分裂继承（能力继承、物质均摊） =================

	/**
	 * 差波器分裂：生成变体子波。<b>能力整份继承，物质（载荷/链式次数）按份均摊</b>。
	 *
	 * <p><b>2026-09 修复（载荷复制）</b>：分裂的语义是"母波 discard + 每个开口一个子波"，
	 * 所以载荷必须<b>分成 total 份</b>发放——旧实现给每个子波整份拷贝，
	 * 2~3 开口 = 同一批物品/流体/电量被复制 2~3 份（每个子波消散时又各自归还来源容器，净赚）。
	 * 这与差器既有的"能量<b>均摊</b>分发"口径一致。</p>
	 *
	 * <ul>
	 *   <li><b>整份继承</b>（"能力"，不因分裂而缩水）：属性集、配方类型、加热档、转速、读取半径；</li>
	 *   <li><b>按份均摊</b>（"物质"）：载荷物品（按每种的数量分份，余数给靠前的子波）、
	 *       载荷流体、载荷电量、链式剩余次数（{@code chainLeft}）；</li>
	 *   <li><b>不可分割</b>：避雷针引雷次数整份给第 0 个子波（其余子波为 0）——
	 *       否则一次满充能避雷针会变成多次落雷；</li>
	 *   <li><b>取料来源表</b>整份继承（只是"归还目标"，物资已按份分开，不会因此多出东西）。</li>
	 * </ul>
	 */
	@Override
	protected AbstractChargerWaveEntity createChildWave(Vec3 pos, Vec3 dir, int level, int index, int total) {
		StellarWaveEntity child = new StellarWaveEntity(level(), pos, dir, level);
		child.attributes = new ArrayList<>(attributes);
		child.recipeTypes = new ArrayList<>(recipeTypes);
		child.carriedHeat = carriedHeat;
		child.carriedRpm = carriedRpm;
		child.carriedScanRadius = carriedScanRadius;
		int n = Math.max(1, total);
		int i = Math.max(0, Math.min(index, n - 1));
		child.payloadItems = splitItems(payloadItems, i, n);
		int fluidAmount = splitShare(payloadFluid.isEmpty() ? 0 : payloadFluid.getAmount(), i, n);
		child.payloadFluid = fluidAmount <= 0 ? FluidStack.EMPTY : payloadFluid.copyWithAmount(fluidAmount);
		child.payloadEnergy = splitShare(payloadEnergy, i, n);
		child.payloadSources = new ArrayList<>(payloadSources);
		child.rodCharges = i == 0 ? rodCharges : 0; // 引雷次数不可分割
		child.chainLeft = splitShare(chainLeft, i, n);
		return child;
	}

	/**
	 * <b>分裂时"载荷已分发"标记</b>（基类钩子 {@code onPayloadDistributedToChildren} 的覆写）。
	 *
	 * <p>母波分裂后会被 {@code discard()}，而 {@code remove(DISCARDED)} 会调 {@link #releasePayload()} ——
	 * 若不标记，母波会把<b>整份</b>载荷（子波拿到的只是份额）再释放回容器一次，等于凭空多出一份。
	 * 标为"已处置"后，母波消散时不再重复释放。</p>
	 */
	@Override
	protected void onPayloadDistributedToChildren() {
		payloadReleased = true;
	}

	/** n 等份中的第 i 份（余数优先给靠前的子波）：{@code share(5,1,2) == 2}、{@code share(5,0,2) == 3}。 */
	private static int splitShare(int amount, int index, int total) {
		if (amount <= 0)
			return 0;
		if (total <= 1)
			return amount;
		int base = amount / total;
		return base + (index < amount % total ? 1 : 0);
	}

	/**
	 * 载荷物品按"<b>每个子波拿到尽量不同的种类、数量尽量均匀</b>"发放（2026-09 用户口径）。
	 *
	 * <p><b>做法：把载荷摊平成"每件一个位置"的序列，子波 i 取所有 {@code 位置 % 子波数 == i}</b>
	 * （同一物料的若干件在序列里连续，于是被轮转着分给不同子波）。四条性质：</p>
	 * <ol>
	 *   <li><b>绝不凭空制造 / 丢失</b>：每件物品只落进一个子波，各子波之和恒等于母波原量；</li>
	 *   <li><b>种类最大化分散</b>：相邻物品总是分给不同子波，所以"每种各 1 件"时各子波拿到的是
	 *       <b>互不相同</b>的种类。旧实现按"每种数量等分 + 余数给靠前的子波"，会让<b>第 0 个子波
	 *       独吞全部种类、其余子波空手</b>（每种 count=1 时余数全落在 index 0）；</li>
	 *   <li><b>数量最均匀</b>：任一子波的件数与平均值的差不超过 1 件；</li>
	 *   <li><b>确定性</b>：每个子波各自调用都能独立算出自己那一份，无需在子波间共享状态
	 *       （分裂是"逐个开口调用 createChildWave"，没有统一的分发时机）。</li>
	 * </ol>
	 *
	 * <p>同一物料件数大于子波数时（例如 A×5、2 个子波）无法做到"种类互不相同"，此时退化为
	 * "该物料在各子波间尽量均匀"（3 / 2），仍有界且守恒。</p>
	 */
	private static List<ItemStack> splitItems(List<ItemStack> items, int index, int total) {
		int n = Math.max(1, total);
		int me = Math.max(0, Math.min(index, n - 1));
		List<ItemStack> out = new ArrayList<>(items.size());
		int position = 0; // 摊平后的位置游标：第 p 件对应"第 p 个位置"
		for (ItemStack stack : items) {
			if (stack == null || stack.isEmpty())
				continue;
			int count = stack.getCount();
			int take = 0;
			for (int p = 0; p < count; p++)
				if ((position + p) % n == me)
					take++;
			position += count;
			if (take > 0)
				out.add(stack.copyWithCount(take));
		}
		return out;
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
		// ===== 2026-09 审计修复：补全"半个波"问题所需的三个字段 =====
		// 旧实现只存 attributes/heat/rpm/radius，读档后 chainLeft 落到默认 0 → 波在第一个容器前
		// 无声自毁；recipeTypes 为空 → 类型门退回"按机器 id 展开静态档案"，状态选择器（真空室 mode、
		// 杠杆锤锤下方块、角磨轮等级）全部丢失；waveOrigin 为 null → 环境判定的"波源"中心消失。
		tag.putInt("WaveChain", chainLeft);
		ListTag typeList = new ListTag();
		for (IRecipeTypeInfo type : recipeTypes)
			if (type != null && type.getId() != null)
				typeList.add(StringTag.valueOf(type.getId()
					.toString()));
		tag.put("WaveRecipeTypeIds", typeList);
		if (waveOrigin != null)
			tag.putLong("WaveOrigin", waveOrigin.asLong());
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		attributes = new ArrayList<>();
		ListTag list = tag.getList("WaveAttributes", Tag.TAG_STRING);
		for (int i = 0; i < list.size(); i++) {
			// 非法/空 id 一律跳过：旧实现直接 add(tryParse(...))，遇到被外部工具改过的存档会塞进 null，
			// 而 getAttributes() 的 List.copyOf 拒绝 null 元素（NPE）（2026-09 审计修复）
			ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
			if (id != null)
				attributes.add(id);
		}
		carriedHeat = HeatLevelNames.byOrdinal(tag.getInt("WaveCarriedHeat"));
		carriedRpm = Math.abs(tag.getFloat("WaveCarriedRpm"));
		carriedScanRadius = Math.max(1, tag.getInt("WaveCarriedRadius"));
		// ===== 2026-09 审计修复（与写入端成对）：链式次数 / 类型快照 / 波源位置 =====
		chainLeft = Math.max(0, tag.getInt("WaveChain"));
		recipeTypes = new ArrayList<>();
		ListTag typeList = tag.getList("WaveRecipeTypeIds", Tag.TAG_STRING);
		for (int i = 0; i < typeList.size(); i++) {
			ResourceLocation tid = ResourceLocation.tryParse(typeList.getString(i));
			if (tid == null)
				continue;
			IRecipeTypeInfo info = recipeTypeById(tid);
			if (info != null)
				recipeTypes.add(info);
		}
		waveOrigin = tag.contains("WaveOrigin") ? BlockPos.of(tag.getLong("WaveOrigin")) : null;
	}

	/** 按注册表 id 找回配方类型档案（读档恢复用；找不到的类型跳过，不影响其它字段）。 */
	private static IRecipeTypeInfo recipeTypeById(ResourceLocation id) {
		for (IRecipeTypeInfo type : StellarWaveMachineRegistry.allRecipeTypes())
			if (type != null && id.equals(type.getId()))
				return type;
		return null;
	}
}