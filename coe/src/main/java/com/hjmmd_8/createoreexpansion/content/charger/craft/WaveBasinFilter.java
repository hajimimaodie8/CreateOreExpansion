package com.hjmmd_8.createoreexpansion.content.charger.craft;

import java.util.List;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * <b>工作盆配方过滤器闸门</b>（2026-10-06 行为零变化拆分，从 {@link WaveCandidateEvaluator} 的
 * {@code recipeFilterAt} / {@code recipeOutputAllowed} / {@code filterAllows} 三件与其专用的
 * 产物推导上下文 {@code craftResultsContext} <b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>命中方块的配方过滤器放不放行这条候选的产物</b>——
 * 命中方块是带配方过滤器的工作盆时，只加工过滤器允许产出的配方——不再对"可做的其它配方"
 * 随机串烧（如 铁锭 → 压板/辊棍混出）。</p>
 *
 * <p>搬运口径：方法体逐字相同，差异只有两类 —— {@code private} → 包级私有（对外只留
 * {@link #recipeFilterAt} 与 {@link #recipeOutputAllowed} 两个入口）、参数类型由内层类名
 * {@code Context} 改写为 {@link WaveCandidateEvaluator.Context}（同一个值类型，只是它的宿主是
 * {@link WaveCandidateEvaluator}）。<b>判定顺序（声明首个产物 → 声明产物全集 → 流体产物 →
 * 产物推导族）与三处"异常保守放行"一个字未动</b>。本类<b>不掷随机</b>——
 * 2026-09-14 的修复（判定与执行解耦、同一状态永远同一结论）就在这里，见
 * {@link #recipeOutputAllowed} 的说明。</p>
 */
final class WaveBasinFilter {

	private WaveBasinFilter() {
		throw new AssertionError("This class should not be instantiated");
	}

	/**
	 * 命中方块是否为<b>工作盆（Basin）</b>：是则返回其配方过滤器（{@code getFilter()}），
	 * 否则返回 null = 不做配方过滤（掉落物/置物台等维持全库行为）。
	 */
	static FilteringBehaviour recipeFilterAt(WaveCandidateEvaluator.Context ctx, BlockPos pos) {
		if (pos == null || ctx.level.isClientSide)
			return null;
		if (ctx.level.getBlockEntity(pos) instanceof BasinBlockEntity basin)
			return basin.getFilter();
		return null;
	}

	/**
	 * 配方产物是否被工作盆配方过滤器放行（语义对齐 Create {@code BasinRecipe.match}）。
	 *
	 * <p><b>Create 真机的口径（去混淆源码实证）</b>：{@code BasinRecipe.match} 只测
	 * {@code filter.test(recipe.getResultItem(registryAccess()))}——即<b>声明的首个结果</b>，
	 * <b>不摇随机</b>（权重产物只按声明值参与判定）；仅当该配方<b>没有物品产物</b>而只有流体产物时，
	 * 改测第一个流体产物。本方法主路径与之一致。</p>
	 *
	 * <p><b>2026-09-14 修复：判定不再"现场摇一次随机"</b>。旧实现在声明产物没命中时会调
	 * {@code WaveCraftResults.compute} 试算真实产物，而那条路径内部是
	 * {@code RecipeApplier.applyRecipeOn → pr.rollResults(outputs, level.random)}——<b>当场掷一次</b>；
	 * 执行时又掷一次（不同一次随机）。于是"设了过滤器"的机器表现为<b>好时坏时</b>（掷中就被挡、
	 * 掷不中就放行）。现在改为测<b>声明产物全集</b>（{@code getRollableResults()} 的 {@code getStack()}，
	 * 取声明值、不摇随机）：只要"这种配方<b>可能</b>产出玩家要的东西"就放行——判定与执行因此
	 * 解耦且确定，同一状态永远同一结论。</p>
	 *
	 * <p>「产物推导」族（变形升级 / 锻造融合：声明产物为空且<b>不是</b> {@code ProcessingRecipe}）
	 * 仍按与执行时同一套推导试算再测：这类配方的产物由输入决定、不含权重抽样，所以不会引入随机。</p>
	 *
	 * <p>判定异常按放行处理，避免过滤器干扰让波无故停摆。</p>
	 *
	 * @param input 命中物品（产物推导需要"主料"参与，如锻造融合的模板/盔甲/材料三件套）
	 * @param handler 命中容器（推导产物时解析辅料真实物品用；掉落物路径传 null）
	 * @param around 命中点（透传给非 ProcessingRecipe 族）
	 */
	static boolean recipeOutputAllowed(WaveCandidateEvaluator.Context ctx, FilteringBehaviour filter,
		Candidate candidate, ItemStack input, IItemHandler handler, BlockPos around) {
		if (filter == null)
			return true;
		Recipe<?> recipe = candidate.recipe;
		try {
			// ① 声明的首个产物（与 Create BasinRecipe.match 同款，确定性）
			ItemStack declared = recipe.getResultItem(ctx.level.registryAccess());
			if (filterAllows(filter, declared))
				return true;
			// ② 声明产物全集（确定性；覆盖权重随机产物）：只要有一种可能的产出被放行就算通过。
			//    注意用 getStack() 而不是 rollOutput(random)——判定不掷随机，见上方 javadoc。
			if (recipe instanceof ProcessingRecipe<?, ?> pr) {
				for (ProcessingOutput output : pr.getRollableResults())
					if (output != null && filterAllows(filter, output.getStack()))
						return true;
				// ③ 流体产物（Create 只在"无物品产物"时才看流体；这里放宽为"物品都没匹配上就再看流体"）
				if (!pr.getFluidResults()
					.isEmpty()) {
					FluidStack fluid = pr.getFluidResults()
						.get(0);
					if (!fluid.isEmpty() && filter.test(fluid))
						return true;
				}
			} else if (input != null && !input.isEmpty()) {
				// ④「产物推导」族（声明为空、且不是 ProcessingRecipe：变形升级 / 锻造融合）：
				//    用与执行时同一套推导算出真实产物再测。这类配方产物由输入决定、无权重抽样。
				//    旧实现对本分支不加类型限制，导致带权重产物的 ProcessingRecipe 也走这里而被摇了一次。
				ItemStack probe = input.copy();
				probe.setCount(1);
				List<ItemStack> derived = WaveCraftResults.compute(craftResultsContext(ctx), candidate, probe, handler,
					around);
				if (derived != null)
					for (ItemStack out : derived)
						if (filterAllows(filter, out))
							return true;
			}
		} catch (Throwable ignored) {
			return true; // 异常保守放行
		}
		ctx.trace.log("工作盆过滤器挡掉候选 {} [{}]：其声明产物都不在过滤器内（若确需该产物，请把它加进过滤器，"
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

	/** 产物推导上下文（世界 + 辅料解析器 + 诊断日志出口）：与实体侧 {@code craftResultsContext()} 同构。 */
	private static WaveCraftResults.Context craftResultsContext(WaveCandidateEvaluator.Context ctx) {
		return new WaveCraftResults.Context(ctx.level, ctx.aux, ctx.debug);
	}
}
