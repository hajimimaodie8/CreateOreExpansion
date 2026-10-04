package com.hjmmd_8.createoreexpansion.content.grinding.block;

import com.hjmmd_8.createoreexpansion.common.recipe.RecipeAutomation;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRecipeTypes;
import com.hjmmd_8.createoreexpansion.content.grinding.item.GrindingWheelTier;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.DismantlingRecipe;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.GrinderRecipeTypes;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.GrindingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.simibubi.create.foundation.recipe.RecipeConditions;
import com.simibubi.create.foundation.recipe.RecipeFinder;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <b>动力角磨床的"配方解析 → 产出落库"</b>（2026-10-05 行为零变化拆分，从
 * {@code PowerAngleGrinderBlockEntity} 的 {@code getRecipes} / {@code applyRecipe} /
 * {@code insertToOutput} <b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>这一台现在能匹配到哪些配方、匹配到的那一条该产出什么、
 * 产出的东西往哪儿放</b>。tick 节奏（合盖 / 轮子 / 转速 / remainingTime 推进）不在这里
 * （那是 {@link GrinderProcessing} 的事）。</p>
 *
 * <p>搬运口径：方法体逐字相同，差异只有三类 —— {@code private} → 包级私有、宿主成员改写成
 * {@code host.} 限定（{@code inventory} / {@code filtering} / {@code recipeIndex} /
 * {@code sequenceStep}，以及公开的 {@code getWheelTier()} / {@code getWheelEffect()}），
 * {@code BlockEntity} 的两个 <b>protected</b> 字段 {@code level} / {@code worldPosition} 改走
 * 它自己的公开谓词 {@code getLevel()} / {@code getBlockPos()}（跨顶层类不可见，与回旋镖那次
 * 拆分同一条纪律）。<b>配方类型的遍历顺序、过滤器语义、产出落库的堆叠与"槽满掉上方"规则
 * 一个字未动</b>；{@code insertToOutput} 仍然刻意走 {@code setStackInSlot}（绕开
 * {@code ProcessingInventory.isItemValid} 的输入区限制）。</p>
 */
final class GrinderRecipeRunner {

	private GrinderRecipeRunner() {
		throw new AssertionError("This class should not be instantiated");
	}

	/**
	 * 这一台此刻匹配到的配方（按角磨轮等级遍历注册表 + 过滤器 + 首原料 + 自动化豁免）。
	 *
	 * <p>{@code host.sequenceStep} 会在这里被写：命中序列装配步骤 ⇒ true（序列加工时禁用轮子
	 * 产出类效果）。</p>
	 */
	static List<RecipeHolder<? extends Recipe<?>>> getRecipes(PowerAngleGrinderBlockEntity host) {
		// 序列装配：物品处于某个序列加工配方中且当前步骤是角磨步骤（仿动力锯）
		Optional<RecipeHolder<GrindingRecipe>> assemblyRecipe = SequencedAssemblyRecipe.getRecipe(host.getLevel(),
			host.inventory.getStackInSlot(0), CoeRecipeTypes.GRINDING.getType(), GrindingRecipe.class);
		if (assemblyRecipe.isPresent() && host.filtering.test(assemblyRecipe.get()
			.value()
			.getResultItem(host.getLevel().registryAccess()))) {
			host.sequenceStep = true;
			return List.of(assemblyRecipe.get());
		}
		host.sequenceStep = false;

		List<RecipeHolder<? extends Recipe<?>>> recipes = new java.util.ArrayList<>();

		// 按角磨轮等级遍历配方类型注册表（开闭：新增配方类型只需 GrinderRecipeTypes.register，
		// 本方法不感知具体类型；1 级=GRINDING，2 级+=CRUSHING/MILLING，3 级+=DISMANTLING）
		GrindingWheelTier tier = host.getWheelTier();
		if (tier != null) {
			for (IRecipeTypeInfo typeInfo : GrinderRecipeTypes.getFor(tier.level)) {
				// key 用 typeInfo 对象本身（枚举常量，对象身份唯一稳定）：
				// RecipeFinder 缓存按 key 全局缓存，用 ResourceLocation 作 key 可能与其他代码缓存冲突
				recipes.addAll(RecipeFinder.get(typeInfo, host.getLevel(),
					RecipeConditions.isOfType(typeInfo.getType())));
			}
		}

		return recipes.stream()
			.filter(RecipeConditions.outputMatchesFilter(host.filtering))
			.filter(RecipeConditions.firstIngredientMatches(host.inventory.getStackInSlot(0)))
			.filter(r -> !RecipeAutomation.shouldIgnoreInAutomation(r))
			.collect(Collectors.toList());
	}

	/** 应用配方产出成品；无匹配配方或结果为空返回 false（由调用方吞掉输入） */
	static boolean applyRecipe(PowerAngleGrinderBlockEntity host) {
		List<RecipeHolder<? extends Recipe<?>>> recipes = getRecipes(host);
		if (recipes.isEmpty())
			return false;
		if (host.recipeIndex >= recipes.size())
			host.recipeIndex = 0;

		Recipe<?> recipe = recipes.get(host.recipeIndex).value();
		List<ItemStack> results = new java.util.ArrayList<>();
		if (recipe instanceof ProcessingRecipe<?, ?> processing) {
			processing.rollResults(host.getLevel().random)
				.forEach(stack -> results.add(stack));
		} else if (recipe instanceof DismantlingRecipe dismantling) {
			ItemStack out = dismantling.getResult(host.inventory.getStackInSlot(0));
			if (!out.isEmpty())
				results.add(out);
		}
		if (results.isEmpty())
			return false;

		// 一次加工消耗 1 个输入（槽 0）
		ItemStack input = host.inventory.getStackInSlot(0);
		input.shrink(1);
		if (input.isEmpty())
			host.inventory.setStackInSlot(0, ItemStack.EMPTY);

		// 轮子特殊效果：额外产出/数量提升/双倍等（序列加工步骤不应用，避免序列配方产出爆炸）
		if (!host.sequenceStep)
			host.getWheelEffect().onProcessCompleted(input, results, host.getLevel().random);

		// 成品统一存入库存（槽 1+）：有输出漏斗由漏斗抽取，无漏斗时玩家空手右键取出
		// （机器本质是容器；手动放入与漏斗输入走同一套输出逻辑，行为一致）
		for (ItemStack result : results) {
			insertToOutput(host, result);
		}
		return true;
	}

	/** 成品插入槽 1+（堆叠或空槽）；全部槽满时剩余部分掉落到机器上方，不再静默丢失。
	 *
	 * <p>用 {@code setStackInSlot} 直接存入：父类 {@code ProcessingInventory.isItemValid}
	 * 只允许物品插入槽 0（输入区），走 {@code insertItem} 到成品区会被拒绝并返回原物品，
	 * 导致成品静默消失；直接设置槽位绕过该输入验证（成品区为内部输出，外部漏斗
	 * 的插入通道仍受 isItemValid 限制，不会污染输出槽）。</p> */
	static void insertToOutput(PowerAngleGrinderBlockEntity host, ItemStack stack) {
		for (int slot = 1; slot < host.inventory.getSlots(); slot++) {
			ItemStack existing = host.inventory.getStackInSlot(slot);
			if (existing.isEmpty()) {
				host.inventory.setStackInSlot(slot, stack);
				return;
			}
			if (ItemStack.isSameItemSameComponents(existing, stack)) {
				int space = existing.getMaxStackSize() - existing.getCount();
				if (stack.getCount() <= space) {
					existing.grow(stack.getCount());
					host.inventory.setStackInSlot(slot, existing);
					return;
				}
				existing.grow(space);
				host.inventory.setStackInSlot(slot, existing);
				stack = stack.copy();
				stack.shrink(space);
			}
		}
		// 全部槽满：剩余部分掉落到机器上方（不静默丢失）
		if (!stack.isEmpty()) {
			ItemEntity drop = new ItemEntity(host.getLevel(), host.getBlockPos().getX() + .5,
				host.getBlockPos().getY() + 1, host.getBlockPos().getZ() + .5, stack);
			drop.setDeltaMovement(Vec3.ZERO);
			host.getLevel().addFreshEntity(drop);
		}
	}
}
