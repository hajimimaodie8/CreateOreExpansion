package com.hjmmd_8.createoreexpansion.content.charger.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.common.AllEntityTypes;
import com.hjmmd_8.createoreexpansion.content.charger.craft.Candidate;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveTransmuterPass;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveAuxResolver;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveCraftConsumption;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveCraftExecutor;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveCandidateEvaluator;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveCandidateOrdering;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveCraftResults;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveEnvironmentChecks;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveType;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveTypes;
import com.hjmmd_8.createoreexpansion.content.charger.payload.WavePayloadGather;
import com.hjmmd_8.createoreexpansion.content.charger.payload.WavePayloadRelease;
import com.hjmmd_8.createoreexpansion.content.charger.wave.BorrowedChargingSource;
import com.hjmmd_8.createoreexpansion.content.charger.wave.WaveDiag;
import com.hjmmd_8.createoreexpansion.content.lightning.ReinforcedLightningRodEffects;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveMachineIntegrations;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.registry.StellarWaveMachineRegistry;
import com.hjmmd_8.createoreexpansion.util.HeatLevelNames;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
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
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

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
 *       其余物品输入（1~{@link WaveCandidateEvaluator#MAX_ITEM_INPUTS} 输入配方）的辅料、以及流体输入（≤2 条），
 *       都<b>优先取命中容器自身</b>（工作盆/置物台里和主料同批的物品、盆内流体），
 *       <b>只有在容器里凑不齐时才回退波载荷</b>——玩家把原料摆进盆里，就不会去动扫描圈箱子的东西；
 *       两通道互不重复占用（槽号/载荷下标两套占用集合），消耗后按来源各自扣减；</li>
 *   <li><b>链式加工</b>：{@code chainLeft} = 波等级（每次成功加工 −1，0 即结束释放载荷）；
 *       命中物品 → 批量测属性 → 恰 1 种直行 / 多种随机 → "主料 + 0~8 辅料 + 0~2 流体"执行
 *       → 产物继续可续链。</li>
 * </ul>
 */
public class StellarWaveEntity extends AbstractChargerWaveEntity implements WaveCraftConsumption.Host, WaveCraftExecutor.Host {

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
	 * 逐条淘汰诊断日志（本类唯一入口，实现见 {@link WaveDiag#debug}）：默认关闭，
	 * 量级与配方库规模成正比，排查"命中物品为何不加工 / 走了哪条配方"时才打开。
	 */
	private static void craftDebug(String msg, Object... args) {
		WaveDiag.debug(msg, args);
	}

	/**
	 * 低量级加工轨迹（本类唯一入口，实现见 {@link WaveDiag#trace}）：默认开启，
	 * 每次实际加工最多输出一行，量级与"发生了多少次加工"成正比，与配方库规模无关。
	 *
	 * <p>用途：定位"明明放了料却没变成预期产物"——日志会明确写出
	 * 「命中哪种输入 → 有几条候选 → 最终选了哪条配方 → 产出什么」，
	 * 于是"是没匹配上、还是匹配上了却选了别的配方"一眼可辨。
	 * 开关与日志前缀现在只有 {@link WaveDiag} 一处（穿波转换、场点燃等跨类事件也写同一本账）。</p>
	 */
	private static void craftTrace(String msg, Object... args) {
		WaveDiag.trace(msg, args);
	}

	// ================= 借用充能（全能波的外部条件放行） =================

	/**
	 * 本次命中已解析出的<b>借来的充能源</b>（{@code null} = 本次命中没有可借的充能器）。
	 *
	 * <p><b>为什么要有这个字段</b>：解析要扫一遍方块，而波每 tick 都在飞且每 tick 都可能命中——
	 * 若在命中处理里对每件掉落物、每次方块命中各扫一遍，同一 tick 的同一批命中会重复付出扫描成本。
	 * 所以本字段只做<b>单次命中生命周期内的复用</b>：见
	 * {@link #borrowedChargingSource()}（每个 tick 至多解析一次）。</p>
	 */
	private BorrowedChargingSource borrowedSource;

	/**
	 * {@link #borrowedSource} 对应的 tick（{@code -1} = 本 tick 还没解析过）。
	 *
	 * <p><b>失效策略：每个 tick 重算一次</b>（tick 一变即失效）。为什么选"每 tick"而不是
	 * "命中一次算一次"或"N tick 失效"：</p>
	 * <ul>
	 *   <li>扫描<b>只在真的有命中时</b>才发生——没命中的 tick 连本方法都不会被调用，
	 *       所以"每 tick 重算"在最坏情况下也只等于"每 tick 一次命中扫一遍"，不会变成每 tick 扫；</li>
	 *   <li>同一 tick 内同一批掉落物/同一次方块命中要试多件物品，复用结果把它们合并成一次扫描；</li>
	 *   <li>不跨 tick 复用 = 玩家拆掉/挪走充能器、或给它改等级后，<b>下一个 tick 就生效</b>，
	 *       不需要额外写"方块变化时让缓存失效"的监听，也就没有一处可能漏掉的失效路径。</li>
	 * </ul>
	 *
	 * <p><b>不落盘、不同步</b>：这两个字段都是纯运行态，{@code addAdditionalSaveData} /
	 * {@code defineSynchedData} 里都没有它们（借用充能是每命中的瞬时外部条件）。</p>
	 */
	private int borrowedSourceTick = -1;

	/**
	 * 取本次命中的借来的充能源（<b>本类唯一的借用判定入口</b>：判定与取值全部委托
	 * {@link BorrowedChargingSource#resolve}，实体侧不做任何等级比较或方块扫描）。
	 */
	private BorrowedChargingSource borrowedChargingSource() {
		if (borrowedSourceTick != tickCount) {
			borrowedSourceTick = tickCount;
			borrowedSource = BorrowedChargingSource.resolve(level(), waveOrigin, Math.max(1, carriedScanRadius));
		}
		return borrowedSource;
	}

	/**
	 * <b>借用充能（掉落物路径）</b>：全能波自身没有充能加工，但变器读取半径内若有星辉石应力充能器，
	 * 就用<b>那台充能器的发射等级</b>执行一次 charging 配方加工。
	 *
	 * <p><b>语义（必须守住）</b>：这是"外部条件放行"，不是"本性改变"——
	 * <b>不改变波型</b>（仍是 {@link WaveTypes#OMNI}），也<b>不让
	 * {@link WaveType#allowsChargingProcessing()} 的返回值变化</b>（那个方法回答"这是什么波"，
	 * 而借用回答"波此刻恰好站在谁旁边"）。因此这里只在<b>调用点</b>判断"有没有可借的充能源"，
	 * 从不把结果写回波型或任何持久状态。</p>
	 *
	 * <p><b>为什么加工逻辑一行都不抄</b>：充能加工的实现是 {@link ChargerWaveProcessor}
	 * （普通波那套：配方匹配 → 充能/消耗输入 → 产出），这里只是"借来等级 → 造一个处理器"，
	 * 见 {@link BorrowedChargingSource#processor(Level)}。</p>
	 *
	 * <p><b>命中即消散</b>：借用走的是普通波语义——命中掉落物即绽放消散
	 * （掉落物会被加工掉的充能行为，与普通波完全一致；变体波自己的远程加工路径不受影响，
	 * 见 {@link #onItemHit}）。</p>
	 *
	 * @return 是否真的完成了一次充能加工（false = 无充能源 / 无匹配配方 → 本次命中不消耗波）
	 */
	private boolean tryBorrowedItemCharging(ItemEntity item) {
		// 本性闸门 + 可借来源（每 tick 至多解析一次）先判，再谈加工：
		// 普通波自己就能充能，不需要借（也不该借）；攻击波既不充能也不加工。
		if (getWaveType().allowsChargingProcessing())
			return false;
		BorrowedChargingSource source = borrowedChargingSource();
		if (source == null || !source.processor(level())
			.processItemEntity(item))
			return false;
		craftTrace("借用充能（掉落物）：波源 {} 半径 {} 内的充能器 [{}] 发射等级 {} → 命中 {} 完成充能加工",
			waveOrigin, Math.max(1, carriedScanRadius), source.pos(), source.waveLevel(),
			item.getItem()
				.getItem());
		ChargerWaveFx.burst(level(), position(), getWaveType().trailStyle(), getRenderColor());
		discard();
		return true;
	}

	/**
	 * <b>借用充能（方块物品槽路径）</b>：与 {@link #tryBorrowedItemCharging(ItemEntity)} 同一口径，
	 * 只是把槽内物品交给处理器（{@link ChargerWaveProcessor#processBlockHandler}）。
	 *
	 * <p><b>返回 {@code false} 的语义与普通波一致</b>：无论是"没有可借的充能源"还是"借到了但槽里
	 * 没有匹配的 charging 配方"，都交给基类默认路径处理（基类的本性闸门会挡住它自己的充能加工），
	 * 调用方 {@code WaveHitResolver} 随后按撞墙让波绽放消散——这就是普通波命中置物台的既有行为。</p>
	 *
	 * @return true = 已用借来的波级完成一次充能加工（调用方不再走默认路径）
	 */
	private boolean tryBorrowedBlockCharging(IItemHandler handler, BlockPos pos) {
		if (getWaveType().allowsChargingProcessing())
			return false;
		BorrowedChargingSource source = borrowedChargingSource();
		if (source == null || !source.processor(level())
			.processBlockHandler(handler, pos))
			return false;
		craftTrace("借用充能（方块槽）：波源 {} 半径 {} 内的充能器 [{}] 发射等级 {} → 命中 {} 完成充能加工",
			waveOrigin, Math.max(1, carriedScanRadius), source.pos(), source.waveLevel(), pos);
		return true;
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
		// 变体波即"变体态／全能态"：构造即定型（一生只变一次，此后不再受任何场/变器影响）
		trySetWaveType(WaveTypes.OMNI);
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
	public void refillPayloadAround(BlockPos center) {
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
		// 命中掉落物后同样"就地补料"（范围 = 变器读取半径）——无携带加工信息时也照做（与原实现一致）
		if (!items.isEmpty())
			refillPayloadAround(items.get(0)
				.blockPosition());
		// ① 自身携带的远程加工优先（属性集/配方类型集非空时；链已尽则释放载荷消散，与原有语义一致）
		if (!(attributes.isEmpty() && recipeTypes.isEmpty())) {
			if (level().isClientSide)
				return;
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
		// ② 借用充能（外部条件放行）：变器读取半径内有星辉石应力充能器时，用那台充能器的
		// 发射等级做一次普通波式的充能加工（详见 tryBorrowedItemCharging 的注释）。
		// 顺序理由：远程加工是变体态"本职工作"、链式且有载荷/辅料/流体/环境一整套判定，
		// 先跑它才能保证"能远程加工的波不被充能加工抢走"；借用充能是兜底——
		// 只有当本波<b>没有</b>任何携带能力（如变器扫描圈里一台机器都没有，退化波）
		// 或远程加工对这些物品一件都不匹配时，才轮得到它。
		if (level().isClientSide)
			return;
		for (ItemEntity item : items)
			if (tryBorrowedItemCharging(item))
				return;
		// ③ 两者都无：退化到普通波行为（基类的波型闸门会挡住它自己的充能加工）
		super.onItemHit(items);
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

	/** 单次加工尝试（掉落物路径）：全库检索候选，选 1 条执行；返回是否成功加工。编排见 `WaveCraftExecutor`。 */
	private boolean tryCraft(ItemEntity item) {
		return executor().tryCraft(item);
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
	/**
	 * 候选检索与逐条门槛（配方类型门 → 输入规模 → 辅料齐备 → 流体齐备 → 电量齐备 → 材料匹配 → 机器环境）。
	 *
	 * <p><b>规则全部在 {@link WaveCandidateEvaluator}</b>（纯静态 + {@link WaveCandidateEvaluator.Context}）；
	 * 本类只负责把"波携带的状态"打包进去，不保留任何检索/门槛逻辑。</p>
	 *
	 * @param handler       命中容器（解析 CONTAINER 来源辅料用；掉落物路径传 null）
	 * @param containerFluid 命中容器的流体槽（判定中心的"盆内流体"来源；无则 null）
	 */
	private List<Candidate> collectCandidates(ItemStack input, BlockPos around, IItemHandler handler, int mainSlot,
		IFluidHandler containerFluid) {
		return WaveCandidateEvaluator.collect(candidateContext(), input, around, handler, mainSlot, containerFluid);
	}

	/**
	 * 候选评估所需状态（现场构造一个：载荷按引用读取，故不跨载荷变更缓存复用）。
	 * 两个诊断出口分别是变体的 {@code craftDebug}（加工诊断）与 {@code craftTrace}（轨迹），二者开关与前缀不同。
	 */
	public WaveCandidateEvaluator.Context candidateContext() {
		return new WaveCandidateEvaluator.Context(level(), auxResolver(), carriedHeat, waveOrigin,
			(msg, args) -> craftDebug(msg, args), (msg, args) -> craftTrace(msg, args), allowedTypeIds(),
			payloadItems, payloadFluid, payloadEnergy);
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
	public boolean summonLightningAt(BlockPos pos) {
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

	/** 配方类型 id（BuiltIn 注册表查 key；查不到返回 null）。实现见 {@link WaveCraftResults#typeKeyOf}。 */
	private static ResourceLocation typeKeyOf(Recipe<?> recipe) {
		return WaveCraftResults.typeKeyOf(recipe);
	}

	// 掉落物路径的一次加工执行（applyCraft：产物推导 → 主料 −1 → 消耗 → 掉落产物 → 流体产物就近注罐 → 链 −1）
	// 已随"加工编排"一族搬入 WaveCraftExecutor。

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
		// 编排（无加工能力退化 / 引雷 / 就地补料 / 批次循环与批锁 / 链尽释放）全在
		// WaveCraftExecutor；本类只作为它的 Host 提供状态与动作。
		boolean handled = executor().handleInventoryBlock(handler, pos);
		if (handled)
			return true;
		// 远程加工编排判定"槽内无一可加工"（返回 false）时的**唯一**补充判定：借用充能。
		// 放在编排之后而不是之前，同样是为了不让借用抢走远程加工的机会：编排内部已经
		// 记过"最近加工方块"、引过雷、补过料，并把槽内容按携带能力逐条试过；
		// 只有它明确说"这里没有我可加工的"之后，才轮到借来的充能器出手。
		// 借用成功 → 返回 true（本 tick 已处理，波不在此消散）；
		// 借用失败（无可借来源 / 槽内无匹配 charging 配方）→ 返回 false，维持"撞墙消散"。
		// 链数不受影响：走到这里时编排要么一步没加工（槽内无一可加工），要么根本没进加工循环
		// （无携带能力时 Executor 直接把本方法交回基类实现，见 WaveCraftExecutor#handleInventoryBlock）。
		if (tryBorrowedBlockCharging(handler, pos))
			return true;
		// 无借用来源（或借了也不匹配）时，与改动前完全一致：按普通波行为收尾（基类的波型闸门
		// 会挡住它自己的充能加工，本波不重复做任何加工）
		return super.handleItemInventoryBlock(handler, pos);
	}

	/** 加工编排器（每次现场构造：它不持状态，状态全经 Host 回到本类）。 */
	private WaveCraftExecutor executor() {
		return new WaveCraftExecutor(this);
	}

	// ========== WaveCraftExecutor.Host：编排所需的实体侧状态与动作 ==========

	@Override
	public BlockPos wavePos() {
		return blockPosition();
	}

	@Override
	public Vec3 waveVec() {
		return position();
	}

	@Override
	public WaveCraftConsumption consumption() {
		if (consumption == null)
			consumption = new WaveCraftConsumption(this, (msg, args) -> craftDebug(msg, args));
		return consumption;
	}

	/** 资源扣减器实例（惰性持有：其构造引用了本类，不能放在字段初始化器里）。 */
	private WaveCraftConsumption consumption;

	@Override
	public int chainLeft() {
		return chainLeft;
	}

	@Override
	public void setChainLeft(int chainLeft) {
		this.chainLeft = chainLeft;
	}

	@Override
	public boolean strikeOwnsTypes() {
		return strikeOwnsTypes;
	}

	@Override
	public void setStrikeOwnsTypes(boolean owns) {
		this.strikeOwnsTypes = owns;
	}

	@Override
	public void setLastProcessedBlock(BlockPos pos) {
		this.lastProcessedBlock = pos;
	}

	@Override
	public boolean hasCraftingAbility() {
		return !(attributes.isEmpty() && recipeTypes.isEmpty());
	}

	@Override
	public int carriedMachineCount() {
		return attributes.size();
	}

	@Override
	public int carriedRecipeTypeCount() {
		return recipeTypes.size();
	}

	@Override
	public boolean handleInventoryBlockAsPlainWave(IItemHandler handler, BlockPos pos) {
		return super.handleItemInventoryBlock(handler, pos);
	}

	@Override
	public BlockPos waveOrigin() {
		return waveOrigin;
	}

	@Override
	public WaveCraftResults.DebugLog debug() {
		return (msg, args) -> craftDebug(msg, args);
	}

	@Override
	public WaveCraftResults.DebugLog trace() {
		return (msg, args) -> craftTrace(msg, args);
	}

	// 方块槽路径的编排（找槽 findCraftableSlot / 逐条试执行 craftFromSlot）与掉落物路径的执行
	// （applyCraft）已随"加工编排"一族搬入 WaveCraftExecutor。

	// 从槽取主料执行配方（craftFromSlot：逐条候选试执行、产物回位优先辅料槽、流体产物"方块储罐优先→就近"）
	// 已随"加工编排"一族搬入 WaveCraftExecutor。

	// 产物列表描述（describeResults）/ 容器内容摘要（describeHandler）/ 产物回位提示（productSlotHint）/
	// 配方类型判定（isUpgradeTransformationRecipe 等）已随"产物推导"一族搬入 WaveCraftResults；
	// 加热档位说明（describeHeat / heatSourceLabel）属"环境读数"，已随环境判定搬入 WaveEnvironmentChecks。

	// 产物回位目标槽（⑥，productSlotHint）的实现已搬入 WaveCraftResults（见其 javadoc：
	// "辅料即产物来源"的推导类配方——auto_upgrade / auto_smithing——产物优先放回首辅料所在的容器槽）。

	/**
	 * 消耗一次加工的资源（辅料/流体/电量），掉落物路径与方块槽路径共用。
	 *
	 * <p>扣减规则（按来源真扣、缺口回流、手持物配方开关）全在 {@link WaveCraftConsumption}；
	 * 本类只作为它的 {@link WaveCraftConsumption.Host} 提供载荷状态。</p>
	 *
	 * @param handler        命中容器物品能力（扣减 CONTAINER 来源辅料用；掉落物路径传 null）
	 * @param containerFluid 命中容器流体能力（扣减 CONTAINER 来源流体用；掉落物路径/无流体能力传 null）
	 */
	private void consumeForCraft(Candidate candidate, IItemHandler handler, IFluidHandler containerFluid) {
		new WaveCraftConsumption(this, (msg, args) -> craftDebug(msg, args))
			.consumeForCraft(candidate, handler, containerFluid);
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
	public Candidate pickCandidate(List<Candidate> candidates, ItemStack input, Candidate preferred) {
		return WaveCandidateOrdering.pick(candidates, preferred, rememberedRecipeId(input), waveSpeedMode());
	}

	/** 候选排序（排序策略见 {@link WaveCandidateOrdering#order}）。 */
	public List<Candidate> orderCandidates(List<Candidate> candidates, ItemStack input, Candidate preferred) {
		return WaveCandidateOrdering.order(candidates, preferred, rememberedRecipeId(input), waveSpeedMode());
	}

	/** 成功加工后记录该输入种类的配方锁（供后续同种输入复用）。 */
	public void rememberCraftLock(ItemStack input, Candidate chosen) {
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
	public void finishAndDiscard() {
		if (!payloadReleased) {
			payloadReleased = true;
			try {
				releasePayload();
			} catch (Throwable t) {
				// 同 remove()：第三方能力异常不得打断消散流程（2026-09 审计修复）
				craftDebug("载荷释放异常（余料未能全部处置）：{}", t);
			}
		}
		ChargerWaveFx.burst(level(), position(), getWaveType().trailStyle(), getRenderColor());
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
	/** 辅料解析器：按当前载荷现场构造（载荷列表按引用读取，故列表内容变化立即可见）。 */
	public WaveAuxResolver auxResolver() {
		return new WaveAuxResolver(level(), payloadItems, payloadFluid, payloadEnergy);
	}

	/** 产物推导的实体侧上下文（每次现场构造：世界 + 辅料解析器 + 调试日志出口）。
	 *  {@code craftDebug} 是静态方法，故调试出口用 lambda 绑定（{@code this::craftDebug} 对静态方法不合法）。 */
	public WaveCraftResults.Context craftResultsContext() {
		return new WaveCraftResults.Context(level(), auxResolver(), (msg, args) -> craftDebug(msg, args));
	}

	// ========== WaveCraftConsumption.Host：把"扣减所需的那几项载荷状态"直通出去 ==========
	// 注意：这里给的是<b>实体内部对象本身</b>，不是 getPayloadItems()/getPayloadFluid() 那种
	// 给 Jade 客户端看的只读副本——扣减必须就地改动载荷。

	@Override
	public List<ItemStack> livePayloadItems() {
		return payloadItems;
	}

	@Override
	public FluidStack livePayloadFluid() {
		return payloadFluid;
	}

	@Override
	public void setPayloadFluid(FluidStack fluid) {
		this.payloadFluid = fluid;
	}

	@Override
	public int payloadEnergyValue() {
		return payloadEnergy;
	}

	@Override
	public void setPayloadEnergy(int energy) {
		this.payloadEnergy = energy;
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
	public AbstractChargerWaveEntity createChildWave(Vec3 pos, Vec3 dir, int level, int index, int total) {
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
		// 变体波永远是"全能波"：老存档没有 WaveType 键时兜住（新存档由基类按 id 还原）
		if (getWaveType() == WaveTypes.NORMAL)
			trySetWaveType(WaveTypes.OMNI);
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