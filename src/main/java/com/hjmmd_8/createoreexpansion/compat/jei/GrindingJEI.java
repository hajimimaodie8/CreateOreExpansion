package com.hjmmd_8.createoreexpansion.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.common.AllBlocks;
import com.hjmmd_8.createoreexpansion.common.AllItems;
import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.DismantlingRecipe;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.GrindingRecipe;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.kinetics.crusher.AbstractCrushingRecipe;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * 角磨配方 JEI 集成（分类仿照机械动力 CreateJEI 的动力锯分类注册方式）。
 */
@JeiPlugin
public class GrindingJEI implements IModPlugin {

	private static final ResourceLocation ID = CreateOreExpansion.modLoc("jei_plugin");

	private final List<CreateRecipeCategory<?>> allCategories = new ArrayList<>();

	@Override
	public ResourceLocation getPluginUid() {
		return ID;
	}

	private void loadCategories() {
		allCategories.clear();

		CreateRecipeCategory<?> grinding = builder(GrindingRecipe.class)
			.addTypedRecipes(AllRecipeTypes.GRINDING)
			.catalyst(AllBlocks.POWER_ANGLE_GRINDER::get)
			.doubleItemIcon(AllBlocks.POWER_ANGLE_GRINDER.get(), Items.IRON_INGOT)
			.emptyBackground(177, 70)
			.build(CreateOreExpansion.modLoc("grinding"), GrindingCategory::new);

		CreateRecipeCategory<?> advanced = builder(AbstractCrushingRecipe.class)
			.addTypedRecipes(com.simibubi.create.AllRecipeTypes.CRUSHING)
			.addTypedRecipes(com.simibubi.create.AllRecipeTypes.MILLING)
			.catalyst(AllBlocks.POWER_ANGLE_GRINDER::get)
			.doubleItemIcon(AllBlocks.POWER_ANGLE_GRINDER.get(), Items.DIAMOND)
			.emptyBackground(177, 70)
			.build(CreateOreExpansion.modLoc("advanced_grinding"), AdvancedGrindingCategory::new);

		CreateRecipeCategory<?> dismantling = builder(DismantlingRecipe.class)
			.addTypedRecipes(AllRecipeTypes.DISMANTLING)
			.catalyst(AllBlocks.POWER_ANGLE_GRINDER::get)
			.doubleItemIcon(AllBlocks.POWER_ANGLE_GRINDER.get(), AllItems.SAPPHIRE_INGOT.get())
			.emptyBackground(177, 70)
			.build(CreateOreExpansion.modLoc("dismantling"), DismantlingCategory::new);

		allCategories.add(grinding);
		allCategories.add(advanced);
		allCategories.add(dismantling);
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
