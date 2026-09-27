package com.hjmmd_8.createoreexpansion.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.compat.jei.category.ChargingCategory;
import com.hjmmd_8.createoreexpansion.content.charger.block.ChargerBlockSlots;
import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipe;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import com.hjmmd_8.createoreexpansion.common.CoeCore;

/**
 * 充能配方 JEI 集成：α/β/γ 三个充能等级共用**一个**分类
 * （{@code createoreexpansion:charging}），等级是配方自带的 {@code level} 字段，
 * 在配方卡片上以等级徽章区分（α=黄、β=绿、γ=蓝）。
 *
 * <p>第三方适配本模组的充能加工只需在数据包添加带 {@code level} 字段的配方 JSON，
 * 无需新增配方类型或分类。</p>
 *
 * <p><b>P7a</b>：本类整体从集成层（根工程 {@code compat/jei/ChargingJEI}）搬进 {@code :cews}，
 * 包名随之改为 {@code compat.jei.cews}（JPMS：同一个包不能跨两个 mod 文件）。
 * 插件 UID {@code createoreexpansion:charging_jei} <b>一字未改</b>。</p>
 *
 * <p><b>W6-c</b>：充能器是波<b>引擎</b>的一部分，随 67 个 L1 文件回到 {@code :coe}，
 * 所以本类也回来 —— 包名从 {@code compat.jei.cews} 折进 {@code :coe} 已有的
 * {@code compat.jei}（同包的 {@code CreateOreExpansionJEI} / {@code GrindingJEI} 就在那里），
 * 不新建 {@code compat.jei.coe} 子包。UID 仍未改；{@code :cews} 的 {@code compat.jei.cews}
 * 里只剩 {@code CewsJeiPlugin}（UID {@code cews_jei}）与 {@code StellarWaveTransmuterCategory}。
 * 后果：只装 {@code coe.jar} 时充能配方的 JEI 分类**在场**（以前不在）。</p>
 */
@JeiPlugin
public class ChargingJEI implements IModPlugin {

	private static final ResourceLocation ID = CoeCore.modLoc("charging_jei");

	private final List<CreateRecipeCategory<?>> allCategories = new ArrayList<>();

	@Override
	public ResourceLocation getPluginUid() {
		return ID;
	}

	private void loadCategories() {
		allCategories.clear();

		// 全部充能等级共用一个分类：配方按卡片内等级徽章区分。
		// W6-a：机器来自第一层自己的槽位表（ChargerBlockSlots），由登记方注入；
		// 未注入时 catalyst/itemIcon 供应商返回 null 的原对象即"没有这台机器"，与被注入时逐字同物。
		allCategories.add(builder(ChargingRecipe.class)
			.addAllRecipesIf(recipe -> recipe.value() instanceof ChargingRecipe)
			.catalyst(ChargerBlockSlots::jadeBlock)
			.catalyst(ChargerBlockSlots::sapphireBlock)
			.itemIcon(ChargerBlockSlots.jadeBlock())
			.emptyBackground(177, 70)
			.build(CoeCore.modLoc("charging"), ChargingCategory::new));
	}

	private static <T extends Recipe<? extends RecipeInput>> CreateRecipeCategory.Builder<T> builder(Class<T> recipeClass) {
		return new CreateRecipeCategory.Builder<>(recipeClass);
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		loadCategories();
		registration.addRecipeCategories(allCategories.toArray(IRecipeCategory[]::new));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		allCategories.forEach(c -> c.registerRecipes(registration));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		allCategories.forEach(c -> c.registerCatalysts(registration));
	}

}
