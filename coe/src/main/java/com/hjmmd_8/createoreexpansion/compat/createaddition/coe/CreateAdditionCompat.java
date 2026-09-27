package com.hjmmd_8.createoreexpansion.compat.createaddition.coe;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

/**
 * <b>P3r</b>：CC&amp;A（Create Addition）联动里<b>只与矿物拓展线有关</b>的那一组口径。
 *
 * <p>原先这些方法都住在 {@code CreateAdditionTransmuterSupport}（波变器载荷侧的大杂烩）。
 * P3r 给整棵 {@code compat/**} 分层时暴露了真相：那个类同时被
 * <b>COE</b>（{@code content/lightning/LightningEventHandler}、{@code LightningStrikeProcessor}
 * 要「雷击落地顺带加工 CC&amp;A 充电配方」）与 <b>CEWS</b>（变器的机器注册 / 抽电 / 载荷上限）使用，
 * 而 COE 不能依赖 CEWS。于是最小拆分：本类留在 COE，方法体<b>逐字搬自</b>原
 * {@code CreateAdditionTransmuterSupport}（行为零变化）；波变器侧那一半仍在 CEWS 的
 * {@code compat.createaddition.CreateAdditionTransmuterSupport}（P12 还原老包名时本类让出
 * {@code compat.createaddition}，搬进 {@code compat.createaddition.coe}）。</p>
 *
 * <p>与 CEWS 侧那个类一样：未安装 CC&amp;A 时不触碰任何 CC&amp;A 类（{@code ModList.isLoaded}
 * + {@code try/catch} 隔离，任何异常都退化成"没有额外配方"）。调用方（核心层）只见
 * {@code RecipeHolder} / {@code Recipe} 这类中立类型。</p>
 *
 * <p><b>口径（用户 2026-09 明确，勿混淆）</b>：CC&amp;A 的充电（{@code createaddition:charging}）
 * 与本模组的雷电加工（{@code createoreexpansion:lightning} / {@code lightning_block}）是<b>两码事</b>——
 * 两套独立机制、两个不同配方类型。这里之所以要"顺带执行 / 摘掉"，仅仅因为
 * {@code LightningEventHandler} 在落点<b>顺带兼容执行</b> CC&amp;A 的充电配方，
 * 会和同样带这个类型的波抢同一件物品。</p>
 */
public final class CreateAdditionCompat {

	private CreateAdditionCompat() {}

	/** CC&amp;A 是否已加载（核心层只问这个，不接触 CC&amp;A 类）。 */
	public static boolean isLoaded() {
		return ModList.get() != null && ModList.get().isLoaded("createaddition");
	}

	/**
	 * <b>雷击转化用：在 CC&amp;A 充电配方里找匹配该物品的配方</b>（首个 ingredient 命中即返回）。
	 *
	 * <p>口径与"闪电落地统一加工"一致：单物品输入；未装 CC&amp;A / 无匹配 / 异常一律返回空，
	 * 由调用方降级为本模组配方。</p>
	 */
	public static Optional<RecipeHolder<? extends Recipe<?>>> findChargingRecipe(Level level, ItemStack stack) {
		if (level == null || stack == null || stack.isEmpty() || !isLoaded())
			return Optional.empty();
		try {
			for (RecipeHolder<?> holder : level.getRecipeManager()
				.getAllRecipesFor(com.mrh0.createaddition.index.CARecipes.CHARGING_TYPE.get())) {
				Recipe<?> recipe = holder.value();
				if (recipe instanceof com.mrh0.createaddition.recipe.charging.ChargingRecipe charging
					&& !charging.getIngredients().isEmpty()
					&& charging.getIngredients().get(0).test(stack))
					return Optional.of(holder);
			}
		} catch (Throwable ignored) {
			// CC&A 异常/缺失：按"没有该配方"处理
		}
		return Optional.empty();
	}

	/** <b>雷击转化用：把 CC&amp;A 充电配方追加进候选表</b>（核心层只拿到 {@code RecipeHolder} 列表）。 */
	public static void addChargingRecipes(Level level, List<RecipeHolder<? extends Recipe<?>>> into) {
		if (level == null || into == null || !isLoaded())
			return;
		try {
			into.addAll(level.getRecipeManager()
				.getAllRecipesFor(com.mrh0.createaddition.index.CARecipes.CHARGING_TYPE.get()));
		} catch (Throwable ignored) {
			// CC&A 异常/缺失：静默降级为本模组配方
		}
	}

	/**
	 * <b>雷击转化用：CC&amp;A 充电配方的贪心多槽匹配</b>（每个 ingredient 需在输入池中找到可消耗物品；
	 * 池由调用方按"逐槽复制"准备，匹配时递减）。非 CC&amp;A 充电配方一律返回 false。
	 */
	public static boolean matchesChargingRecipe(Recipe<?> recipe, List<ItemStack> pool) {
		if (!(recipe instanceof com.mrh0.createaddition.recipe.charging.ChargingRecipe charging)
			|| pool == null || pool.isEmpty())
			return false;
		for (var ingredient : charging.getIngredients()) {
			boolean found = false;
			for (var stack : pool) {
				if (ingredient.test(stack)) {
					stack.shrink(1);
					found = true;
					break;
				}
			}
			if (!found)
				return false;
		}
		return true;
	}

	/** <b>雷击转化用：抽出 CC&amp;A 充电配方的产物</b>（非该类型返回空表）。 */
	public static List<ItemStack> rollChargingResults(Recipe<?> recipe, RandomSource random) {
		if (!(recipe instanceof com.mrh0.createaddition.recipe.charging.ChargingRecipe charging))
			return List.of();
		try {
			return charging.rollResults(random);
		} catch (Throwable ignored) {
			return List.of();
		}
	}

	/**
	 * 携带电量的变体波可执行的"额外配方类型"（CC&amp;A charging = 特斯拉线圈放电加工）。
	 * 仅在 CC&amp;A 安装且波确已携带 FE（&gt;0）时给出；否则空表——避免未带电的波
	 * 把普通物品误当"充电对象"逐条尝试 charging 配方。
	 */
	public static List<IRecipeTypeInfo> extraEnergyRecipeTypes(boolean payloadHasEnergy) {
		if (!payloadHasEnergy || !isLoaded())
			return List.of();
		try {
			return List.of(com.mrh0.createaddition.recipe.charging.ChargingRecipe.TYPE_INFO);
		} catch (Throwable ignored) {
			return List.of();
		}
	}

	/**
	 * "雷击落地统一加工也会顺带执行"的配方类型 id（CC&amp;A charging）——波引雷时据此类 id
	 * 把这些类型从自己的类型门里摘掉，保证同一件物品不会被"雷"和"波"各加工一遍。
	 * 未安装 CC&amp;A 时返回空集。
	 */
	public static Set<ResourceLocation> strikeHandledTypeIds() {
		if (!isLoaded())
			return Set.of();
		try {
			return Set.of(com.mrh0.createaddition.recipe.charging.ChargingRecipe.TYPE_INFO.getId());
		} catch (Throwable ignored) {
			return Set.of();
		}
	}
}
