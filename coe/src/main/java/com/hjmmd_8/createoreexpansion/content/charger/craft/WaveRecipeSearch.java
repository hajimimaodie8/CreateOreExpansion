package com.hjmmd_8.createoreexpansion.content.charger.craft;

import java.util.List;

import com.hjmmd_8.createoreexpansion.common.AllConfig;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRecipeTypes;
import com.hjmmd_8.createoreexpansion.content.charger.craft.family.WaveRecipeFamilies;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * <b>变体波的全库检索面与配方类型门</b>（2026-10-06 行为零变化拆分，从
 * {@link WaveCandidateEvaluator} 的 {@code allWaveRecipes} / {@code isLightningRecipe} /
 * {@code isTypeAllowed} 与其专属缓存键<b>逐字搬出</b>）。
 *
 * <p>它回答的<b>只有一件事</b>：<b>这一次命中能看哪些配方</b>——</p>
 * <ol>
 *   <li>{@link #allWaveRecipes}：Create {@code RecipeFinder} 全库检索（带本模组<b>专属</b>缓存键）
 *       ＋按谓词裁掉闪电类（闪电加工不走全库命中，只能由"波在命中点引雷 → 闪电落地统一加工"承担）；</li>
 *   <li>{@link #isTypeAllowed}：该配方是否属于波携带到的类型（{@link AllConfig#waveRequireCarriedType}
 *       为 false 时恒放行 = 旧全库行为）。</li>
 * </ol>
 *
 * <p>搬运口径：方法体逐字相同，差异只有两类 —— {@code private} → 包级私有、参数类型由
 * 内层类名 {@code Context} 改写为 {@link WaveCandidateEvaluator.Context}（同一个值类型，只是它的
 * 宿主是 {@link WaveCandidateEvaluator}）。<b>谓词、缓存键的身份、类型 id 的取法与兜底返回值
 * 一个字未动</b>；"谁在什么时候问它"仍由 {@link WaveCandidateEvaluator#collect} 决定。</p>
 */
final class WaveRecipeSearch {

	private WaveRecipeSearch() {
		throw new AssertionError("This class should not be instantiated");
	}

	/**
	 * 全库检索的<b>专属缓存键</b>（本模组唯一持有者）。
	 *
	 * <p><b>为什么不用 {@code RecipeFinder.class} 之类"看起来唯一"的现成对象</b>：Create 的
	 * {@code RecipeFinder.CACHED_SEARCHES} 是一个<b>进程级全局</b> Guava 缓存，命中条件<b>只有 key</b>——
	 * 既不比较 {@code level} 也不比较谓词（源码注释原文："using the same object instance as the cacheKey
	 * will retrieve the cached result from the first search"）。所以只要另一个模组也拿
	 * {@code RecipeFinder.class}（一个谁都写得出、且语义上很自然的字面量）当 key，两边就会互相拿到
	 * 对方的配方表，且症状是"某类配方莫名不加工"，极难排查。这里用一个<b>本类私有的新对象</b>做键：
	 * 身份唯一（{@code Object} 用 == 语义），外部不可能撞上，也不需要靠"别人不会这么写"来保证正确性。</p>
	 *
	 * <p>（本仓另一处 {@code PowerAngleGrinderBlockEntity} 按 {@code typeInfo} 对象作键，同样唯一；
	 * 数据包重载时 Create 自己的 {@code LISTENER} 会 {@code invalidateAll()}，故缓存不会跨数据包陈旧。）</p>
	 */
	private static final Object WAVE_RECIPE_CACHE_KEY = new Object();

	/** 当前世界全部"<b>波可执行</b>"配方（RecipeFinder 带缓存；数据包重载后自动失效重查）。
	 *  范围 = Create ProcessingRecipe 族（主路径全库管线）∪ {@link WaveRecipeFamilies} 登记的非
	 *  ProcessingRecipe 族（拆解等）；仍排除本 mod 闪电类——闪电加工不走全库命中，
	 *  只能由"波在命中点引雷 → 本模组闪电落地统一加工"承担（见 {@code StellarWaveEntity#summonLightningAt}）。
	 *
	 *  <p>缓存键见 {@link #WAVE_RECIPE_CACHE_KEY}（私有新对象 = 身份唯一，不与任何外部调用方冲突）；
	 *  谓词固定，故同一会话内每个 key 只会构建一次缓存。</p> */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static List<RecipeHolder<?>> allWaveRecipes(WaveCandidateEvaluator.Context ctx) {
		try {
			return (List) com.simibubi.create.foundation.recipe.RecipeFinder.get(WAVE_RECIPE_CACHE_KEY, ctx.level,
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
		return t == CoeRecipeTypes.LIGHTNING.getType() || t == CoeRecipeTypes.LIGHTNING_BLOCK.getType();
	}

	/**
	 * 配方类型门：该配方是否属于波携带到的类型。
	 *
	 * <p>{@link AllConfig#waveRequireCarriedType} 为 false 时恒放行（旧全库行为）。
	 * 类型 id 由 {@link WaveCraftResults#typeKeyOf(Recipe)} 取注册表键，因此"同类型不同 mod 的配方"
	 * 共用一次携带（与展示口径一致）。</p>
	 */
	static boolean isTypeAllowed(Recipe<?> recipe, java.util.Set<ResourceLocation> allowedTypeIds) {
		if (!AllConfig.waveRequireCarriedType)
			return true; // 兼容开关：关闭时回到全库检索
		ResourceLocation key = WaveCraftResults.typeKeyOf(recipe);
		return key != null && allowedTypeIds.contains(key);
	}
}
