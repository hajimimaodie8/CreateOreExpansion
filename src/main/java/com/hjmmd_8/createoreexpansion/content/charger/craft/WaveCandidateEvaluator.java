package com.hjmmd_8.createoreexpansion.content.charger.craft;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveCraftResults.DebugLog;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.AuxRef;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.EnergyDraw;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.FluidRef;
import com.hjmmd_8.createoreexpansion.content.charger.craft.family.WaveRecipeFamilies;
import com.hjmmd_8.createoreexpansion.content.machine.stellarwavetransmuter.StellarWaveMachineIntegrations;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * <b>变体波的"候选检索与逐条门槛"</b>（2026-09 从 {@code StellarWaveEntity} 结构性抽出）。
 *
 * <p>职责：把命中物品能执行的全部配方找出来，并对每条配方逐条过关——配方类型门 → 输入规模 →
 * 辅料齐备 → 流体齐备 → 电量齐备 → 材料匹配 → 机器环境（④）。全过者成为一条
 * {@link Candidate}（三类资源引用由 {@link WaveAuxResolver} 解析，产物推导交给
 * {@link WaveCraftResults}）。</p>
 *
 * <p><b>检索面</b>：{@link #allWaveRecipes}（Create {@code RecipeFinder} + 缓存）内的
 * ProcessingRecipe 族与 {@link WaveRecipeFamilies} 登记的非 ProcessingRecipe 族；闪电类
 * （{@link #isLightningRecipe}）不入全库，交由"波在命中点引雷 → 闪电落地统一加工"承担
 * （引雷本体仍留在实体 {@code StellarWaveEntity#summonLightningAt}）。</p>
 *
 * <p><b>状态经 {@link Context} 注入</b>：世界 / 辅料解析器 / 变器携带状态（热档、波源、
 * 配方类型集、载荷三件套）与两个诊断日志出口全部由实体侧现场打包传入——本类不持世界引用、
 * 无静态状态。日志文本与参数顺序与原实现一字不差（仅出口由实体的 {@code craftDebug} /
 * {@code craftTrace} 改为 {@link DebugLog#log}；两者是<b>不同的</b>日志开关与日志前缀，
 * 故 {@link Context} 分别持有 {@code debug} 与 {@code trace} 两个出口）。</p>
 *
 * <p><b>环境判定不在此处重复实现</b>：门槛里的环境检查直接调
 * {@link WaveEnvironmentChecks#satisfied}。配方类型判定一律走 id 字符串 / 原版类，
 * 不 import 任何可选模组（CC&amp;A / Vintage / Jade / JEI / Optical / Sable）。</p>
 */
public final class WaveCandidateEvaluator {

	private WaveCandidateEvaluator() {
	}

	/**
	 * 候选检索所需的<b>实体侧状态</b>（每次检索现场构造一个；载荷字段持调用方集合的引用，
	 * 不要跨载荷变更缓存复用）。
	 *
	 * @param level          世界（BlockEntity 查过滤器 / 配方检索 / 材料匹配 / 环境判定用，即实体的 {@code level()}）
	 * @param aux            辅料解析器（即实体每次现场构造的 {@code auxResolver()}）
	 * @param carriedHeat    变器携带的加热档位（环境判定用，即实体字段 {@code carriedHeat}）
	 * @param waveOrigin     波源位置（环境判定的第二中心，即实体字段 {@code waveOrigin}；可为 null）
	 * @param debug          [变体波加工] 诊断日志出口（实体的静态方法 {@code craftDebug}）
	 * @param trace          [变体波轨迹] 轨迹日志出口（实体的静态方法 {@code craftTrace}）
	 * @param allowedTypeIds 波当前携带到的配方类型 id 集合（配方类型门白名单，即实体的 {@code allowedTypeIds()}）
	 * @param payloadItems   波载荷物品（按引用读取；仅诊断日志需要其条数）
	 * @param payloadFluid   波载荷流体（仅诊断日志需要）
	 * @param payloadEnergy  波载荷电量（仅诊断日志需要）
	 */
	public static final class Context {
		public final Level level;
		public final WaveAuxResolver aux;
		public final BlazeBurnerBlock.HeatLevel carriedHeat;
		public final BlockPos waveOrigin;
		public final DebugLog debug;
		public final DebugLog trace;
		public final java.util.Set<ResourceLocation> allowedTypeIds;
		public final List<ItemStack> payloadItems;
		public final FluidStack payloadFluid;
		public final int payloadEnergy;

		public Context(Level level, WaveAuxResolver aux, BlazeBurnerBlock.HeatLevel carriedHeat, BlockPos waveOrigin,
			DebugLog debug, DebugLog trace, java.util.Set<ResourceLocation> allowedTypeIds, List<ItemStack> payloadItems,
			FluidStack payloadFluid, int payloadEnergy) {
			this.level = level;
			this.aux = aux;
			this.carriedHeat = carriedHeat;
			this.waveOrigin = waveOrigin;
			this.debug = debug;
			this.trace = trace;
			this.allowedTypeIds = allowedTypeIds;
			this.payloadItems = payloadItems;
			this.payloadFluid = payloadFluid;
			this.payloadEnergy = payloadEnergy;
		}
	}

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
	public static final int MAX_ITEM_INPUTS = 9;

	/**
	 * 单条配方允许的<b>流体输入</b>数上限（= 2，与 Create {@code BasinRecipe.getMaxFluidInputCount()}
	 * 一致，javap 证实）。两条流体输入各自独立解析来源与需求量，并按"同种流体已认领量"记账，
	 * 防止两条流体 ingredient 各自"独立通过"、合计却超过实际存量。
	 */
	public static final int MAX_FLUID_INPUTS = 2;

	// ================= 候选检索入口 =================

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
	 * <b>优先</b>，通道 A = 波载荷 {@link Context#payloadItems} 兜底；流体输入同理（{@code containerFluid}
	 * 的盆内流体优先，载荷流体兜底）。掉落物路径没有容器，两个容器参数都传 {@code null}
	 * （此时只能用载荷，与改动前一致）。</p>
	 *
	 * @param ctx            实体侧上下文（世界 / 辅料解析器 / 携带状态 / 调试出口）
	 * @param handler        命中方块的物品容器（掉落物路径传 null = 无容器物品通道）
	 * @param mainSlot       主料所在槽号（容器通道须排除它；掉落物路径传 -1）
	 * @param containerFluid 命中方块的流体容器（工作盆盆内流体；掉落物路径/无流体能力传 null）
	 */
	public static List<Candidate> collect(Context ctx, ItemStack input, BlockPos around, IItemHandler handler,
		int mainSlot, IFluidHandler containerFluid) {
		List<Candidate> candidates = new ArrayList<>();
		if (input.isEmpty())
			return candidates;
		// ===== 配方类型门（2026-09 修复"拆掉机器仍能加工"）=====
		// 波携带的类型 = 变器扫描半径内读到的机器能力快照（见 activeRecipeTypes）。
		// 旧实现只按材料做全库匹配，于是"把卷簧机拆了，波照样能卷簧"——因为执行侧根本不看类型。
		// 现在：类型不在携带集合里的配方一律不参与（含 {@link WaveRecipeFamilies} 登记的非
		// ProcessingRecipe 族，如拆解要靠三级角磨轮才带得动）。
		// 兼容开关：AllConfig.waveRequireCarriedType = false 时回到旧的全库行为。
		java.util.Set<ResourceLocation> allowedTypeIds = ctx.allowedTypeIds;
		for (RecipeHolder<?> holder : allWaveRecipes(ctx)) {
			if (AllRecipeTypes.shouldIgnoreInAutomation(holder))
				continue;
			Recipe<?> candidateRecipe = holder.value();
			if (!isTypeAllowed(candidateRecipe, allowedTypeIds)) {
				ctx.debug.log("淘汰 {} [{}]：配方类型不在波携带范围内（携带 {} 种）", holder.id(),
					WaveCraftResults.typeKeyString(candidateRecipe), allowedTypeIds.size());
				continue;
			}
			// 族 0｜ProcessingRecipe 主路径：既有全库管线（辅料/流体/电量/环境/材料三段式）
			if (candidateRecipe instanceof ProcessingRecipe<?, ?> recipe) {
				Candidate c = evalCandidate(ctx, recipe, input, holder.id(), around, handler, mainSlot, containerFluid, null);
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
					Candidate stepCandidate = evalCandidate(ctx, step, input, holder.id(), around, handler, mainSlot,
						containerFluid, candidateRecipe);
					if (stepCandidate != null)
						candidates.add(stepCandidate);
					continue;
				}
			}
			// 单输入族：材料判定与产物推导都由族负责，这里只做"认领 → 门槛 → 环境"三步
			Candidate familyCandidate = evalFamilyCandidate(ctx, holder, input, around);
			if (familyCandidate != null)
				candidates.add(familyCandidate);
		}
		// 工作盆配方过滤器（用户 2026 要求）：命中方块是带配方过滤器的工作盆时，只加工过滤器
		// 允许产出的配方——不再对"可做的其它配方"随机串烧（如 铁锭 → 压板/辊棍混出）。
		FilteringBehaviour recipeFilter = recipeFilterAt(ctx, around);
		if (recipeFilter != null) {
			int before = candidates.size();
			candidates.removeIf(c -> !recipeOutputAllowed(ctx, recipeFilter, c, input, handler, around));
			if (before > 0 && candidates.isEmpty())
				ctx.trace.log("工作盆过滤器把 {} 的 {} 条候选全部挡掉（只有过滤器列出的产物才放行）", input.getItem(), before);
		}
		ctx.debug.log("命中 {}（容器物品{} 主料槽 {} 容器流体{}）→ 候选 {} 条{}", input.getItem(),
			handler == null ? "无" : "有", mainSlot, containerFluid == null ? "无" : "有", candidates.size(),
			recipeFilter == null ? "" : "（已过工作盆过滤器闸门）");
		return candidates;
	}

	// ================= 工作盆配方过滤器（用户 2026 要求） =================

	/**
	 * 命中方块是否为<b>工作盆（Basin）</b>：是则返回其配方过滤器（{@code getFilter()}），
	 * 否则返回 null = 不做配方过滤（掉落物/置物台等维持全库行为）。
	 */
	private static FilteringBehaviour recipeFilterAt(Context ctx, BlockPos pos) {
		if (pos == null || ctx.level.isClientSide)
			return null;
		if (ctx.level.getBlockEntity(pos) instanceof BasinBlockEntity basin)
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
	private static boolean recipeOutputAllowed(Context ctx, FilteringBehaviour filter, Candidate candidate, ItemStack input,
		IItemHandler handler, BlockPos around) {
		if (filter == null)
			return true;
		Recipe<?> recipe = candidate.recipe;
		try {
			// ① 配方声明的产物（getResultItem = 首个可滚结果）
			ItemStack declared = recipe.getResultItem(ctx.level.registryAccess());
			if (filterAllows(filter, declared))
				return true;
			// ② results 为空的"产物推导"类（auto_upgrade / auto_smithing）：用与执行时同一套
			//    WaveCraftResults.compute 算出真实产物再测。注意这里**不再以 declared 为空为前提**——
			//    否则"声明了产物但推导产物才是真产物"的配方一旦 declared 不在过滤器内就直接被判死，
			//    表现为"设了过滤器就不加工，清空过滤器又好了"（用户 2026-09 实测反馈）。
			if (input != null && !input.isEmpty()) {
				ItemStack probe = input.copy();
				probe.setCount(1);
				List<ItemStack> derived = WaveCraftResults.compute(craftResultsContext(ctx), candidate, probe, handler,
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
		ctx.trace.log("工作盆过滤器挡掉候选 {} [{}]：其产物不在过滤器内（若确需该产物，请把它加进过滤器，"
			+ "或把过滤器切到白名单/关闭“匹配数据”）", candidate.id, WaveCraftResults.typeKeyString(recipe));
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

	// ================= 单条配方评估（逐条门槛） =================

	/**
	 * 单条<b>非 ProcessingRecipe 族</b>配方评估（{@link WaveRecipeFamilies} 登记：拆解等）：
	 * 认领 → 材料判定 → 环境 → 产出空辅料/流体/电量的候选。
	 *
	 * <p>族配方的辅料/流体/电量一律为空：当前登记的族都是"单输入"族（见
	 * {@link WaveRecipeFamilies} 的契约边界说明）；多输入族要扩契约，别在这里偷偷扣物品。</p>
	 */
	private static Candidate evalFamilyCandidate(Context ctx, RecipeHolder<?> holder, ItemStack input, BlockPos around) {
		Recipe<?> recipe = holder.value();
		WaveRecipeFamilies.Family family = WaveRecipeFamilies.familyOf(recipe);
		if (family == null)
			return null; // 没有族认领（且不是 ProcessingRecipe）：不参与
		try {
			if (!family.matches(holder.id(), recipe, input)) {
				ctx.debug.log("淘汰族配方 {} [{}]：材料不匹配（主料 {}）", holder.id(), WaveCraftResults.typeKeyString(recipe), input.getItem());
				return null;
			}
		} catch (Throwable ignored) {
			return null; // 族判定异常：跳过该条，不影响其它候选
		}
		if (!WaveEnvironmentChecks.satisfied(ctx.level, around, ctx.waveOrigin, recipe, ctx.carriedHeat, ctx.trace)) {
			ctx.debug.log("淘汰族配方 {} [{}]：机器环境前置不满足（命中点 {}）", holder.id(), WaveCraftResults.typeKeyString(recipe), around);
			return null;
		}
		ctx.debug.log("命中族候选 {} [{}]：主料 {}", holder.id(), WaveCraftResults.typeKeyString(recipe), input.getItem());
		return new Candidate(holder.id(), recipe, List.of(), List.of(), List.of(), null);
	}

	/** 单条配方评估：门槛全过返回候选，否则 null（供全库循环调用）。
	 *  {@code id} = 配方数据包 id（批次锁定的稳定标识）；{@code around} = 命中点（环境判定中心）；
	 *  {@code handler}/{@code mainSlot} = 命中容器与其主料槽（辅料通道 B；掉落物路径传 null / -1）。
	 *
	 *  @param familyOwner 步骤族路径下"本候选实际属于哪条族配方"（序列装配）；普通配方传 null */
	private static Candidate evalCandidate(Context ctx, ProcessingRecipe<?, ?> recipe, ItemStack input,
		ResourceLocation id, BlockPos around, IItemHandler handler, int mainSlot, IFluidHandler containerFluid,
		Recipe<?> familyOwner) {
		int fluidInputs = recipe.getFluidIngredients()
			.size();
		if (fluidInputs > MAX_FLUID_INPUTS) {
			ctx.debug.log("淘汰 {} [{}]：流体输入 {} > {}（超出 Create 盆口径）", id, WaveCraftResults.typeKeyString(recipe), fluidInputs,
				MAX_FLUID_INPUTS);
			return null;
		}
		int itemInputs = recipe.getIngredients()
			.size();
		if (itemInputs < 1 || itemInputs > MAX_ITEM_INPUTS) {
			ctx.debug.log("淘汰 {} [{}]：物品输入数 {} 不在 1..{} 内", id, WaveCraftResults.typeKeyString(recipe), itemInputs,
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
			AuxRef aux = ctx.aux.findAux(recipe.getIngredients()
				.get(i), auxes, handler, mainSlot);
			if (aux == null) {
				ctx.debug.log("淘汰 {} [{}]：辅料 {}（第 {} 号输入）在容器（{} 槽）与载荷（{} 件）中都缺货", id,
					WaveCraftResults.typeKeyString(recipe),
					recipe.getIngredients()
						.get(i),
					i, handler == null ? 0 : handler.getSlots(), ctx.payloadItems.size());
				return null;
			}
			auxes.add(aux);
		}
		// 流体输入（≤2）：同样"容器流体槽优先 → 载荷流体兜底"，各自独立解析并记账防超领
		List<FluidRef> fluidRefs = ctx.aux.resolveFluidRefs(recipe, containerFluid);
		if (fluidRefs == null) {
			ctx.debug.log("淘汰 {} [{}]：流体输入不满足（容器流体 {} / 载荷 {}）", id, WaveCraftResults.typeKeyString(recipe),
				containerFluid == null ? "无" : containerFluid.getTanks() + " 罐", ctx.payloadFluid);
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
		List<EnergyDraw> energies = ctx.aux.resolveEnergyDraws(energyRequired, around);
		if (energies == null) {
			ctx.debug.log("淘汰 {} [{}]：需电量 {} FE，邻域储能 + 载荷电量（{} FE）合计不足", id, WaveCraftResults.typeKeyString(recipe),
				energyRequired, ctx.payloadEnergy);
			return null;
		}
		if (!matches(ctx, recipe, input, auxes, handler)) {
			ctx.debug.log("淘汰 {} [{}]：材料不匹配（主料 {}）", id, WaveCraftResults.typeKeyString(recipe), input.getItem());
			return null;
		}
		// 机器环境前置条件（④）：需加热/需压弯机头/需鼓风机媒介的配方，命中点附近必须存在对应环境
		if (!WaveEnvironmentChecks.satisfied(ctx.level, around, ctx.waveOrigin, recipe, ctx.carriedHeat, ctx.trace)) {
			ctx.debug.log("淘汰 {} [{}]：机器环境前置不满足（命中点 {}）", id, WaveCraftResults.typeKeyString(recipe), around);
			return null;
		}
		ctx.debug.log("命中候选 {} [{}]：主料 {} + 辅料 {}（流体 {} / 电量 {}）", id, WaveCraftResults.typeKeyString(recipe),
			input.getItem(), WaveAuxResolver.describeAuxes(auxes), WaveAuxResolver.describeFluids(fluidRefs), WaveAuxResolver.describeEnergies(energies));
		return new Candidate(id, recipe, List.copyOf(auxes), List.copyOf(fluidRefs), List.copyOf(energies), familyOwner);
	}

	// ================= 材料匹配（含机器上下文兜底） =================

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
	 *       鼓风机媒介）仍由 {@link #evalCandidate} 与 {@link WaveEnvironmentChecks#satisfied} 先行把关。</li>
	 * </ol>
	 */
	private static boolean matches(Context ctx, Recipe<?> recipe, ItemStack input, List<AuxRef> auxes,
		IItemHandler handler) {
		// BasinRecipe 系（工作盆/真空室等机器上下文配方，如 Vintage 加压/抽真空）：
		// Create 6 的 BasinRecipe.matches() 恒返回 false（真实匹配走机器静态 match，
		// 绑定 BasinBlockEntity 的过滤器/加热状态）。变体波是"远程能量执行器"，没有
		// 机器实体上下文——这里按配方自身材料需求手工判定（忽略机器过滤器与热需求，
		// 波自带加工能量），否则真空室配方永远无法远程执行。
		if (recipe instanceof com.simibubi.create.content.processing.basin.BasinRecipe basin) {
			return genericIngredientsMatch(ctx, basin, input, auxes, handler);
		}
		// 先单槽：主流单输入配方（压机/喷洗/闹鬼…）的输入类型
		net.minecraft.world.item.crafting.SingleRecipeInput single =
			new net.minecraft.world.item.crafting.SingleRecipeInput(input);
		if (matchesQuietly(ctx, recipe, single))
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
			ItemStack aux = ctx.aux.resolveAux(auxes.get(k), handler);
			if (!aux.isEmpty())
				handlerProbe.setStackInSlot(k + 1, aux.copy());
		}
		net.neoforged.neoforge.items.wrapper.RecipeWrapper wrapper =
			new net.neoforged.neoforge.items.wrapper.RecipeWrapper(handlerProbe);
		if (matchesQuietly(ctx, recipe, wrapper))
			return true;
		// 两种标准输入类型都判定失败 → 通用兜底：机器上下文配方族（matches 恒 false）按材料手工判定
		if (!(recipe instanceof ProcessingRecipe<?, ?>))
			return false; // 本引擎只执行 ProcessingRecipe 族；非该族一律不兜底
		return genericIngredientsMatch(ctx, recipe, input, auxes, handler);
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
	private static boolean genericIngredientsMatch(Context ctx, Recipe<?> recipe, ItemStack input, List<AuxRef> auxes,
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
			ItemStack stack = ctx.aux.resolveAux(auxes.get(slot), handler);
			if (stack.isEmpty() || !ingredients.get(i)
				.test(stack))
				return false;
		}
		return true;
	}

	/** 静默调配方 matches：输入类型不符（CCE）或桥接异常按"不匹配"处理，不向调用方抛。 */
	@SuppressWarnings("unchecked")
	private static boolean matchesQuietly(Context ctx, Recipe<?> recipe, net.minecraft.world.item.crafting.RecipeInput input) {
		try {
			return ((Recipe<net.minecraft.world.item.crafting.RecipeInput>) recipe).matches(input, ctx.level);
		} catch (ClassCastException ignored) {
			return false; // 输入类型与配方要求不符（如给 SingleRecipeInput 型配方传了双槽包装）
		} catch (Throwable ignored) {
			return false; // 个别配方桥接异常：当作不匹配
		}
	}

	// ================= 全库检索与类型门 =================

	/** 当前世界全部"<b>波可执行</b>"配方（RecipeFinder 带缓存；数据包重载后自动失效重查）。
	 *  范围 = Create ProcessingRecipe 族（主路径全库管线）∪ {@link WaveRecipeFamilies} 登记的非
	 *  ProcessingRecipe 族（拆解等）；仍排除本 mod 闪电类——闪电加工不走全库命中，
	 *  只能由"波在命中点引雷 → 本模组闪电落地统一加工"承担（见 {@code StellarWaveEntity#summonLightningAt}）。
	 *
	 *  <p>缓存 key 仍用 {@code RecipeFinder.class}（本类独占该 key，见
	 *  {@code PowerAngleGrinderBlockEntity} 的注释：对方按 typeInfo 对象作 key，互不冲突）；
	 *  谓词放宽后同一 JVM 会话内只会以新谓词构建一次缓存。</p> */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static List<RecipeHolder<?>> allWaveRecipes(Context ctx) {
		try {
			return (List) com.simibubi.create.foundation.recipe.RecipeFinder.get(
				com.simibubi.create.foundation.recipe.RecipeFinder.class, ctx.level,
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
	 * 配方类型门：该配方是否属于波携带到的类型。
	 *
	 * <p>{@link AllConfig#waveRequireCarriedType} 为 false 时恒放行（旧全库行为）。
	 * 类型 id 由 {@link WaveCraftResults#typeKeyOf(Recipe)} 取注册表键，因此"同类型不同 mod 的配方"
	 * 共用一次携带（与展示口径一致）。</p>
	 */
	private static boolean isTypeAllowed(Recipe<?> recipe, java.util.Set<ResourceLocation> allowedTypeIds) {
		if (!AllConfig.waveRequireCarriedType)
			return true; // 兼容开关：关闭时回到全库检索
		ResourceLocation key = WaveCraftResults.typeKeyOf(recipe);
		return key != null && allowedTypeIds.contains(key);
	}

	// ================= 接线 =================

	/** 产物推导上下文（世界 + 辅料解析器 + 诊断日志出口）：与实体侧 {@code craftResultsContext()} 同构。 */
	private static WaveCraftResults.Context craftResultsContext(Context ctx) {
		return new WaveCraftResults.Context(ctx.level, ctx.aux, ctx.debug);
	}
}
