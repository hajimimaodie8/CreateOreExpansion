package com.hjmmd_8.createoreexpansion.content.charger.craft;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.common.recipe.RecipeAutomation;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveCraftResults.DebugLog;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.AuxRef;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.EnergyDraw;
import com.hjmmd_8.createoreexpansion.content.charger.craft.WaveResources.FluidRef;
import com.hjmmd_8.createoreexpansion.content.charger.craft.family.WaveRecipeFamilies;
import com.hjmmd_8.createoreexpansion.content.wave.api.WaveMachineIntegrationPoints;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
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
 * <p><b>检索面</b>：{@link WaveRecipeSearch#allWaveRecipes}（Create {@code RecipeFinder} + 缓存）内的
 * ProcessingRecipe 族与 {@link WaveRecipeFamilies} 登记的非 ProcessingRecipe 族；闪电类
 * （{@link WaveRecipeSearch#isLightningRecipe}）不入全库，交由"波在命中点引雷 → 闪电落地统一加工"承担
 * （引雷本体仍留在实体 {@code StellarWaveEntity#summonLightningAt}）。</p>
 *
 * <p><b>本类的形状（2026-10-06 行为零变化拆分）</b>：本类保留<b>逐条门槛的编排</b>
 * （{@link #collect} 的类型门/族分派/近似配方摘要与 {@link #evalCandidate} 的七道门槛），
 * 按职责域拆出的同包类只做搬运：</p>
 * <ul>
 *   <li>{@link WaveRecipeSearch} —— 全库检索面与配方类型门（专属缓存键 + RecipeFinder 谓词 +
 *       LIGHTNING 排除 + 携带类型白名单）；</li>
 *   <li>{@link WaveMaterialMatch} —— 材料匹配（BasinRecipe 手工判定 → 单槽 → 双槽 → 机器上下文兜底）；</li>
 *   <li>{@link WaveBasinFilter} —— 工作盆配方过滤器闸门（命中盆才生效，判定不掷随机）。</li>
 * </ul>
 * <p>本类仍不持世界引用、无静态状态（检索缓存键随 {@link WaveRecipeSearch} 一起搬走，
 * 身份仍是同一个私有新对象）。</p>
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
	 * 不要跨载荷变更缓存复用）。逐字段说明见构造器的 {@code @param}。
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

		/**
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
		// 「近似配方被挡」摘要（2026-09-14）：玩家最常问的是"我明明能做，为什么波没加工"。
		// 逐条淘汰原因原本只在 CRAFT_DEBUG（编译期常量，默认 false）里，日志里什么都看不到；
		// 这里把"主料能匹配上、却被某道门槛挡掉"的前几条记下来，一条候选都没选出来时随轨迹输出
		// （轨迹日志默认开启）——于是"是没带这个配方类型、还是流体/材料不够"在日志里直接可读。
		List<String> nearMiss = new ArrayList<>(NEAR_MISS_LIMIT);
		// ===== 配方类型门（2026-09 修复"拆掉机器仍能加工"）=====
		// 波携带的类型 = 变器扫描半径内读到的机器能力快照（见 activeRecipeTypes）。
		// 旧实现只按材料做全库匹配，于是"把卷簧机拆了，波照样能卷簧"——因为执行侧根本不看类型。
		// 现在：类型不在携带集合里的配方一律不参与（含 {@link WaveRecipeFamilies} 登记的非
		// ProcessingRecipe 族，如拆解要靠三级角磨轮才带得动）。
		// 兼容开关：AllConfig.waveRequireCarriedType = false 时回到旧的全库行为。
		java.util.Set<ResourceLocation> allowedTypeIds = ctx.allowedTypeIds;
		for (RecipeHolder<?> holder : WaveRecipeSearch.allWaveRecipes(ctx)) {
			if (RecipeAutomation.shouldIgnoreInAutomation(holder))
				continue;
			Recipe<?> candidateRecipe = holder.value();
			if (!WaveRecipeSearch.isTypeAllowed(candidateRecipe, allowedTypeIds)) {
				noteNearMiss(nearMiss, candidateRecipe, input,
					"类型未携带（本波只带了 " + allowedTypeIds.size() + " 种）");
				ctx.debug.log("淘汰 {} [{}]：配方类型不在波携带范围内（携带 {} 种）", holder.id(),
					WaveCraftResults.typeKeyString(candidateRecipe), allowedTypeIds.size());
				continue;
			}
			// 族 0｜ProcessingRecipe 主路径：既有全库管线（辅料/流体/电量/环境/材料三段式）
			if (candidateRecipe instanceof ProcessingRecipe<?, ?> recipe) {
				Candidate c = evalCandidate(ctx, recipe, input, holder.id(), around, handler, mainSlot, containerFluid, null,
					nearMiss);
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
						containerFluid, candidateRecipe, nearMiss);
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
		FilteringBehaviour recipeFilter = WaveBasinFilter.recipeFilterAt(ctx, around);
		if (recipeFilter != null) {
			int before = candidates.size();
			candidates.removeIf(c -> !WaveBasinFilter.recipeOutputAllowed(ctx, recipeFilter, c, input, handler, around));
			if (before > 0 && candidates.isEmpty())
				ctx.trace.log("工作盆过滤器把 {} 的 {} 条候选全部挡掉（只有过滤器列出的产物才放行）", input.getItem(), before);
		}
		ctx.debug.log("命中 {}（容器物品{} 主料槽 {} 容器流体{}）→ 候选 {} 条{}", input.getItem(),
			handler == null ? "无" : "有", mainSlot, containerFluid == null ? "无" : "有", candidates.size(),
			recipeFilter == null ? "" : "（已过工作盆过滤器闸门）");
		// 一条候选都没选出来、但有"主料匹配却被打回"的配方 → 把原因写进默认开启的轨迹日志
		if (candidates.isEmpty() && !nearMiss.isEmpty())
			ctx.trace.log("命中 {} 无可用候选；被打回的近似配方：{}", input.getItem(), String.join("；", nearMiss));
		return candidates;
	}

	/** 近似配方摘要上限：只留前几条，避免一条日志刷屏。 */
	private static final int NEAR_MISS_LIMIT = 3;

	/**
	 * 记一条"主料能匹配、却被门槛挡掉"的近似配方（用于"为什么没加工"的日志）。
	 * 只记前 {@link #NEAR_MISS_LIMIT} 条、只记主料确实匹配的配方（否则全库无关配方会塞满日志）。
	 */
	private static void noteNearMiss(List<String> nearMiss, Recipe<?> recipe, ItemStack input, String reason) {
		if (nearMiss == null || nearMiss.size() >= NEAR_MISS_LIMIT || !matchesFirstIngredient(recipe, input))
			return;
		nearMiss.add(WaveCraftResults.typeKeyString(recipe) + " " + reason);
	}

	/** 该配方的主料（{@code ingredients[0]}）是否匹配这个物品——"玩家本以为能做"的判定依据。 */
	private static boolean matchesFirstIngredient(Recipe<?> recipe, ItemStack input) {
		try {
			java.util.List<net.minecraft.world.item.crafting.Ingredient> ingredients = recipe.getIngredients();
			return !ingredients.isEmpty() && ingredients.get(0)
				.test(input);
		} catch (Throwable ignored) {
			return false;
		}
	}

	/** 配方流体需求的可读摘要（形如 {@code 1000 mB}）——用于"流体不足"那条日志。 */
	private static String describeFluidNeeds(ProcessingRecipe<?, ?> recipe) {
		try {
			StringBuilder sb = new StringBuilder();
			for (net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient need : recipe.getFluidIngredients()) {
				if (sb.length() > 0)
					sb.append(" + ");
				sb.append(need.amount())
					.append(" mB");
			}
			return sb.length() == 0 ? "0 mB" : sb.toString();
		} catch (Throwable ignored) {
			return "?";
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
		Recipe<?> familyOwner, java.util.List<String> nearMiss) {
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
			noteNearMiss(nearMiss, recipe, input,
				"流体不足（需 " + describeFluidNeeds(recipe) + "；命中容器 "
					+ (containerFluid == null ? "无流体能力" : containerFluid.getTanks() + " 罐")
					+ "、波载荷 " + ctx.payloadFluid.getAmount() + " mB）");
			ctx.debug.log("淘汰 {} [{}]：流体输入不满足（容器流体 {} / 载荷 {}）", id, WaveCraftResults.typeKeyString(recipe),
				containerFluid == null ? "无" : containerFluid.getTanks() + " 罐", ctx.payloadFluid);
			return null;
		}
		// 电量需求（CC&A charging / Vintage 激光 energy 等耗电条目）：按"特殊辅料"双通道解析
		// （邻域储能优先 → 载荷电量兜底），合计不足才淘汰。仅在配方确实要电时才做邻域扫描。
		int energyRequired = 0;
		try {
			energyRequired = WaveMachineIntegrationPoints.recipeEnergyRequired(recipe);
		} catch (Throwable ignored) {
			// 联动读取异常：按无电量需求处理
		}
		List<EnergyDraw> energies = ctx.aux.resolveEnergyDraws(energyRequired, around);
		if (energies == null) {
			ctx.debug.log("淘汰 {} [{}]：需电量 {} FE，邻域储能 + 载荷电量（{} FE）合计不足", id, WaveCraftResults.typeKeyString(recipe),
				energyRequired, ctx.payloadEnergy);
			return null;
		}
		if (!WaveMaterialMatch.matches(ctx, recipe, input, auxes, handler)) {
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

}
