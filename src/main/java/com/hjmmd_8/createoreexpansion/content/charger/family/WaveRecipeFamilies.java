package com.hjmmd_8.createoreexpansion.content.charger.family;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.DismantlingRecipe;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe.SequencedAssembly;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * <b>变体波"可执行配方族"登记表</b>——波引擎能执行的<b>全部配方族</b>都在这一个文件里声明。
 *
 * <h2>两段式结构</h2>
 * <ol>
 *   <li><b>族 0｜Create {@code ProcessingRecipe} 族</b>（不在本表内登记，它是引擎主路径）：
 *       压片/切割/研磨/粉碎/搅拌/压实/喷洗/闹鬼/部署/聚焦/卷绕/弯折/打磨/离心/振动/加压/抽真空/
 *       车削/激光/辊压/锤锻/自动锻造升级……凡继承 {@code ProcessingRecipe} 的配方都由
 *       {@code StellarWaveEntity#collectCandidates} 的全库管线处理：多输入辅料、流体、电量、
 *       加热/介质/机头环境、产物推导（锻造融合/变形升级）等等全在那边；</li>
 *   <li><b>族 1..N｜非 {@code ProcessingRecipe} 族</b>（<b>本表</b>）：这些配方的
 *       {@code matches} 不是 Create 的盆/机器上下文语义，产物也未必能经 {@code RecipeApplier}
 *       推导，所以各自登记一条 {@link Family}，由族自己负责"材料判定 + 产物推导"。</li>
 * </ol>
 *
 * <h2>当前已登记的族（本文件即完整清单）</h2>
 * <pre>
 * 拆解 dismantling  {@link DismantlingFamily}
 *   配方类：{@code com.hjmmd_8.createoreexpansion.content.grinding.recipe.DismantlingRecipe}
 *           （{@code implements Recipe<SingleRecipeInput>}，<b>不是</b> ProcessingRecipe）
 *   机器：本模组动力角磨床（三级角磨轮专属）→ 见 {@code StellarWaveMachineCatalog}
 *   语义：单输入；产物按剩余耐久比例 = {@code floor(materialCount × 剩余耐久比)}，
 *         耐久耗尽/不足 1 份材料 → 无产物（该候选被跳过，不消耗）
 *
 * 序列装配 create:sequenced_assembly  {@link SequencedAssemblyFamily}
 *   配方类：{@code SequencedAssemblyRecipe}（{@code implements Recipe<RecipeWrapper>}，非 ProcessingRecipe）
 *   机器：机械手（F3 也认压机/注液器/锯等会出现在装配步骤里的机器）→ 见 {@code StellarWaveMachineCatalog}
 *   语义：按物品上的 {@code create:sequenced_assembly} 进度取"下一步"步骤配方（它本身就是
 *         ProcessingRecipe，故辅料/流体/电量全走族 0 门槛）；推进到 loops×步数 后按权重抽最终产物
 * </pre>
 *
 * <h2>如何再接入一个族（开放点）</h2>
 * <pre>{@code
 * // 1) 在下面 static 块里加一行：
 * register(new MyFamily());
 *
 * // 2) 实现 Family：认领 + 材料判定 + 产物（单输入族直接实现 3 个方法即可）
 * //    若"这一步"等价于一条 ProcessingRecipe（多输入/流体/电量），覆写 currentStepRecipe 即可，
 * //    引擎会替你把辅料/流体/电量从容器与载荷里解析并扣减，不需要族自己动手。
 * private static final class MyFamily implements Family {
 *     public IRecipeTypeInfo typeInfo() { return MyRecipeTypes.MY_TYPE; }
 *     public boolean accepts(Recipe<?> r) { return r instanceof MyRecipe; }
 *     public boolean matches(ResourceLocation id, Recipe<?> r, ItemStack in) { ... }
 *     public ProcessingRecipe<?, ?> currentStepRecipe(ResourceLocation id, Recipe<?> r, ItemStack in) { ... }
 *     public List<ItemStack> results(Level lv, BlockPos pos, ResourceLocation id, Recipe<?> r, ItemStack in) { ... }
 * }
 * }</pre>
 *
 * <p><b>契约边界</b>：<b>单输入族</b>由引擎给一个"辅料/流体/电量皆空"的 {@code Candidate}；
 * <b>步骤族</b>（覆写 {@link Family#currentStepRecipe}）则走族 0 的全套门槛，辅料/流体/电量的解析与扣减
 * 全部由引擎负责——族只负责"推不出产物就返回空表"。<b>任何族都不要自己扣物品</b>，
 * 否则"先算产物再消耗"的事务性会被破坏（波是远程执行器，一次加工要么全成要么全不动）。</p>
 *
 * <p><b>展示口径</b>：{@link #typeInfos()} 给出全部族的配方类型，供护目镜/Jade"可加工类型"对齐
 * （机器侧登记见 {@code StellarWaveMachineCatalog}：机器 → 配方类型）。</p>
 */
public final class WaveRecipeFamilies {

	/**
	 * 一条"非 ProcessingRecipe 配方族"的接入契约。
	 *
	 * <p><b>两条路径</b>：</p>
	 * <ul>
	 *   <li><b>单输入族</b>：只实现 {@link #matches}/{@link #results}，引擎给一个"辅料/流体/电量皆空"的
	 *       候选（拆解即此类）；</li>
	 *   <li><b>步骤族</b>：覆写 {@link #currentStepRecipe}——当本族某条配方"当前这一步"其实等价于一条
	 *       {@link ProcessingRecipe}（序列装配的下一步就是这种）时，引擎会<b>改走族 0 的全套门槛</b>
	 *       （辅料解析、流体、电量、材料三段式），再由 {@link #results} 负责把进度推进/收尾。
	 *       这样多输入族不必自己扣物品，事务性统一由引擎保证。</li>
	 * </ul>
	 */
	public interface Family {

		/** 本族对应的配方类型（展示/口径用；也会进"可加工类型"清单）。 */
		IRecipeTypeInfo typeInfo();

		/** 认领：该配方实例是否由本族执行（实现内自行 {@code instanceof}）。 */
		boolean accepts(Recipe<?> recipe);

		/**
		 * 材料判定：命中物品能否作为该配方的主料（族自己的语义）。
		 *
		 * @param recipeId 配方数据包 id（序列装配靠它区分"同一个中间产物的不同装配配方"）
		 */
		boolean matches(ResourceLocation recipeId, Recipe<?> recipe, ItemStack input);

		/**
		 * 本族"当前这一步"等价的 {@link ProcessingRecipe}；返回非 null 时引擎按该步骤配方走族 0 门槛。
		 *
		 * <p>典型实现：序列装配 → 读物品上的装配进度 → 取 {@code sequence.get(step % size).getRecipe()}。</p>
		 */
		default ProcessingRecipe<?, ?> currentStepRecipe(ResourceLocation recipeId, Recipe<?> recipe, ItemStack input) {
			return null;
		}

		/**
		 * 产物推导：返回物品产物；<b>空表 = 推不出产物</b>（调用方跳过该候选且不消耗任何东西），
		 * 返回 {@code null} 与空表同义（防御性写法）。
		 *
		 * @param level    世界（需要查询世界状态的族用；不需要可忽略）
		 * @param around   命中点（同上）
		 * @param recipeId 配方数据包 id（步骤族需要写回进度时用）
		 */
		List<ItemStack> results(Level level, BlockPos around, ResourceLocation recipeId, Recipe<?> recipe,
			ItemStack input);

		/** 流体产物（默认无；需要时覆写）。 */
		default List<FluidStack> fluidResults(Level level, BlockPos around, ResourceLocation recipeId,
			Recipe<?> recipe, ItemStack input) {
			return List.of();
		}
	}

	/** 已登记的族（登记顺序即遍历顺序；族之间不应认领同一条配方）。 */
	private static final List<Family> FAMILIES = new ArrayList<>();

	static {
		register(new DismantlingFamily());
		register(new SequencedAssemblyFamily());
	}

	private WaveRecipeFamilies() {
	}

	/** 登记一条族（幂等：同一 {@link Family#typeInfo()} 类型只登记一次）。 */
	public static void register(Family family) {
		if (family == null || family.typeInfo() == null)
			return;
		for (Family existing : FAMILIES)
			if (existing.typeInfo() == family.typeInfo())
				return;
		FAMILIES.add(family);
	}

	/** 认领该配方的族；没有族认领（或属于 ProcessingRecipe 主路径）返回 null。 */
	public static Family familyOf(Recipe<?> recipe) {
		if (recipe == null || recipe instanceof ProcessingRecipe<?, ?>)
			return null; // 主路径配方不归本表管
		for (Family family : FAMILIES) {
			try {
				if (family.accepts(recipe))
					return family;
			} catch (Throwable ignored) {
				// 单条族判定异常：跳过该族，不影响其它族与主路径
			}
		}
		return null;
	}

	/** 该配方是否在波的可执行范围内（ProcessingRecipe 主路径 ∪ 本表登记的族）。 */
	public static boolean isExecutable(Recipe<?> recipe) {
		return recipe instanceof ProcessingRecipe<?, ?> || familyOf(recipe) != null;
	}

	/** 全部族的配方类型（展示口径；护目镜/Jade"可加工类型"用）。 */
	public static List<IRecipeTypeInfo> typeInfos() {
		List<IRecipeTypeInfo> out = new ArrayList<>(FAMILIES.size());
		for (Family family : FAMILIES)
			out.add(family.typeInfo());
		return out;
	}

	// ================= 族 1：本模组「拆解」 =================

	/**
	 * 拆解族（{@link DismantlingRecipe}）：把装备/武器拆回构成材料，输出量随剩余耐久比例变化。
	 *
	 * <p>与角磨床自身口径完全一致（{@code DismantlingRecipe#matches / #getResult} 是同一个方法）；
	 * 这里不重复实现语义，只做"认领 + 转发"。</p>
	 */
	private static final class DismantlingFamily implements Family {

		@Override
		public IRecipeTypeInfo typeInfo() {
			return AllRecipeTypes.DISMANTLING;
		}

		@Override
		public boolean accepts(Recipe<?> recipe) {
			return recipe instanceof DismantlingRecipe;
		}

		@Override
		public boolean matches(ResourceLocation recipeId, Recipe<?> recipe, ItemStack input) {
			return recipe instanceof DismantlingRecipe dismantling
				&& !input.isEmpty()
				&& input.is(dismantling.getItem()
					.getItem());
		}

		@Override
		public List<ItemStack> results(Level level, BlockPos around, ResourceLocation recipeId, Recipe<?> recipe,
			ItemStack input) {
			if (!(recipe instanceof DismantlingRecipe dismantling))
				return List.of();
			ItemStack out = dismantling.getResult(input); // 按剩余耐久比例算份数
			return out.isEmpty() ? List.of() : List.of(out);
		}
	}

	// ================= 族 2：Create「序列装配」 =================

	/**
	 * <b>序列装配族</b>（{@code create:sequenced_assembly}）：把"金板 → 精密构件"这类
	 * <b>N 步 × 循环 M 次</b>的装配交给波执行。
	 *
	 * <p><b>Create 的语义</b>（逐条对照 {@code SequencedAssemblyRecipe}，因为它的
	 * {@code getStep/advance/rollResult} 都是 private，这里按同一算法重写）：</p>
	 * <ul>
	 *   <li>起步：物品命中装配配方的 {@code ingredient}（金板）→ 第 0 步；</li>
	 *   <li>进行中：物品带 {@code create:sequenced_assembly} 数据组件（中间产物 + 进度）；
	 *       组件里的 {@code id} 必须等于本装配配方的数据包 id；</li>
	 *   <li>当前步骤 = {@code sequence.get(step % sequence.size()).getRecipe()}——这就是一条
	 *       {@link ProcessingRecipe}（如 {@code create:deploying}），且 Create 已把它的
	 *       {@code ingredients[0]} 换成中间产物（见 {@code SequencedRecipe#initFromSequencedAssembly}），
	 *       所以直接把它交给引擎的门槛链即可（辅料 = 小齿轮/大齿轮/铁粒 会从容器或载荷里解析并扣减）；</li>
	 *   <li>推进：{@code (step+1) / sequence.size() >= loops} → <b>收尾</b>，按 {@code resultPool} 权重
	 *       抽最终产物；否则产出<b>带新进度的中间产物</b>（进度 = {@code (step+1)/(size*loops)}）。</li>
	 * </ul>
	 *
	 * <p><b>与真机的差别（诚实说明）</b>：真机把中间产物放在置物台/传送带上、由机械手逐步加工；
	 * 波是远程执行器，只要命中点容器里有一件"中间产物"且有对应辅料，就直接推进一步——
	 * 所以在波上"一条链路能连冲几步"取决于波等级（{@code chainLeft}），整条装配需要多发波。</p>
	 */
	private static final class SequencedAssemblyFamily implements Family {

		@Override
		public IRecipeTypeInfo typeInfo() {
			return com.simibubi.create.AllRecipeTypes.SEQUENCED_ASSEMBLY;
		}

		@Override
		public boolean accepts(Recipe<?> recipe) {
			return recipe instanceof SequencedAssemblyRecipe;
		}

		@Override
		public boolean matches(ResourceLocation recipeId, Recipe<?> recipe, ItemStack input) {
			return recipe instanceof SequencedAssemblyRecipe assembly && !input.isEmpty()
				&& appliesTo(assembly, recipeId, input);
		}

		@Override
		public ProcessingRecipe<?, ?> currentStepRecipe(ResourceLocation recipeId, Recipe<?> recipe, ItemStack input) {
			if (!(recipe instanceof SequencedAssemblyRecipe assembly) || !appliesTo(assembly, recipeId, input))
				return null;
			List<SequencedRecipe<?>> sequence = assembly.getSequence();
			if (sequence.isEmpty())
				return null;
			SequencedRecipe<?> step = sequence.get(stepOf(input) % sequence.size());
			return step == null ? null : step.getRecipe();
		}

		@Override
		public List<ItemStack> results(Level level, BlockPos around, ResourceLocation recipeId, Recipe<?> recipe,
			ItemStack input) {
			if (!(recipe instanceof SequencedAssemblyRecipe assembly))
				return List.of();
			RandomSource random = level == null ? RandomSource.create() : level.random;
			List<SequencedRecipe<?>> sequence = assembly.getSequence();
			int length = Math.max(1, sequence.size());
			int step = stepOf(input);
			// 与 Create 同款判定（整数除法）：走完 (loops × 步数) 就收尾抽产物
			if ((step + 1) / length >= assembly.getLoops())
				return rollResult(assembly, random);
			ItemStack advanced = assembly.getTransitionalItem()
				.copyWithCount(1);
			advanced.set(AllDataComponents.SEQUENCED_ASSEMBLY, new SequencedAssembly(recipeId, step + 1,
				(step + 1f) / (length * Math.max(1, assembly.getLoops()))));
			return List.of(advanced);
		}

		/** 该物品是否归属这条装配配方：带进度 → 中间产物 + id 相符；不带 → 起步料命中。 */
		private static boolean appliesTo(SequencedAssemblyRecipe assembly, ResourceLocation recipeId, ItemStack input) {
			if (input.has(AllDataComponents.SEQUENCED_ASSEMBLY)) {
				SequencedAssembly progress = input.get(AllDataComponents.SEQUENCED_ASSEMBLY);
				if (progress == null)
					return false;
				return assembly.getTransitionalItem()
					.getItem() == input.getItem()
					&& progress.id()
						.equals(recipeId);
			}
			return assembly.getIngredient()
				.test(input);
		}

		/** 当前步骤号（无进度组件 = 第 0 步）。 */
		private static int stepOf(ItemStack input) {
			SequencedAssembly progress = input.get(AllDataComponents.SEQUENCED_ASSEMBLY);
			return progress == null ? 0 : progress.step();
		}

		/** 收尾：按 {@code resultPool} 的机会值加权抽一件（与 Create 同算法）。 */
		private static List<ItemStack> rollResult(SequencedAssemblyRecipe assembly, RandomSource random) {
			float totalWeight = 0;
			for (ProcessingOutput entry : assembly.resultPool)
				totalWeight += entry.getChance();
			if (totalWeight <= 0)
				return List.of();
			float number = random.nextFloat() * totalWeight;
			for (ProcessingOutput entry : assembly.resultPool) {
				number -= entry.getChance();
				if (number < 0) {
					ItemStack out = entry.getStack()
						.copy();
					return out.isEmpty() ? List.of() : List.of(out);
				}
			}
			return List.of();
		}
	}
}
