package com.hjmmd_8.createoreexpansion.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllBlocks;
import com.hjmmd_8.createoreexpansion.compat.jei.category.ChargingCategory;
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

/**
 * 充能配方 JEI 集成：低/高/伽马三个充能等级共用**一个**分类
 * （{@code createoreexpansion:charging}），等级是配方自带的 {@code level} 字段，
 * 在配方卡片上以等级徽章区分（低=黄、高=绿、伽马=蓝）。
 *
 * <p>第三方适配本模组的充能加工只需在数据包添加带 {@code level} 字段的配方 JSON，
 * 无需新增配方类型或分类。</p>
 */
@JeiPlugin
public class ChargingJEI implements IModPlugin {

	private static final ResourceLocation ID = CreateOreExpansion.modLoc("charging_jei");

	private final List<CreateRecipeCategory<?>> allCategories = new ArrayList<>();

	@Override
	public ResourceLocation getPluginUid() {
		return ID;
	}

	private void loadCategories() {
		allCategories.clear();

		// 全部充能等级共用一个分类：配方按卡片内等级徽章区分
		allCategories.add(builder(ChargingRecipe.class)
			.addAllRecipesIf(recipe -> recipe.value() instanceof ChargingRecipe)
			.catalyst(AllBlocks.JADE_CREATE_CHARGER::get)
			.itemIcon(AllBlocks.JADE_CREATE_CHARGER.get())
			.emptyBackground(177, 70)
			.build(CreateOreExpansion.modLoc("charging"), ChargingCategory::new));
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
