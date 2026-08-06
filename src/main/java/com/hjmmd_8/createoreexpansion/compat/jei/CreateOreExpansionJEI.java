package com.hjmmd_8.createoreexpansion.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.NotNull;

import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.hjmmd_8.createoreexpansion.content.transmuting.AllTransmutingRecipe;
import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.compat.jei.category.CreateRecipeCategory;
import com.hjmmd_8.createoreexpansion.compat.jei.category.CreateRecipeCategory.Factory;
import com.hjmmd_8.createoreexpansion.compat.jei.category.ProcessingViaFanCategory;
import com.hjmmd_8.createoreexpansion.compat.jei.category.TransmutingCategory;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;

@JeiPlugin
@ParametersAreNonnullByDefault
public class CreateOreExpansionJEI implements IModPlugin {

	private static final ResourceLocation ID =
		ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, "jei_plugin");

	private final List<CreateRecipeCategory<?>> allCategories = new ArrayList<>();

	private void loadCategories() {
		allCategories.clear();

		builder(AllTransmutingRecipe.class)
			.addTypedRecipes(AllRecipeTypes.TRANSMUTING)
			.catalystStack(ProcessingViaFanCategory.getFan("fan_transmuting"))
			.doubleItemIcon(com.simibubi.create.AllBlocks.ENCASED_FAN.get(),
				BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID,
					"transmutation_fluid_bucket")))
			.emptyBackground(178, 72)
			.build("fan_transmuting", TransmutingCategory::new);
	}

	private <T extends Recipe<? extends RecipeInput>> CategoryBuilder<T> builder(Class<T> recipeClass) {
		return new CategoryBuilder<>(recipeClass);
	}

	@Override
	@NotNull
	public ResourceLocation getPluginUid() {
		return ID;
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T extends Recipe<?>> void consumeTypedRecipes(Consumer<RecipeHolder<?>> consumer,
		RecipeType<?> type) {
		List<? extends RecipeHolder<?>> map = Minecraft.getInstance()
			.getConnection()
			.getRecipeManager()
			.getAllRecipesFor((RecipeType) type);
		if (!map.isEmpty())
			map.forEach(consumer);
	}

	private class CategoryBuilder<T extends Recipe<? extends RecipeInput>> extends CreateRecipeCategory.Builder<T> {

		public CategoryBuilder(Class<? extends T> recipeClass) {
			super(recipeClass);
		}

		@Override
		public CreateRecipeCategory<T> build(ResourceLocation id, Factory<T> factory) {
			CreateRecipeCategory<T> category = super.build(id, factory);
			allCategories.add(category);
			return category;
		}
	}

}
