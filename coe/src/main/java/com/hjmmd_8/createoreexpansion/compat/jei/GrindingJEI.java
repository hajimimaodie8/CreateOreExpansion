package com.hjmmd_8.createoreexpansion.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeItems;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeMachines;
import com.hjmmd_8.createoreexpansion.common.registry.coe.CoeRecipeTypes;
import com.hjmmd_8.createoreexpansion.compat.jei.category.AdvancedGrindingCategory;
import com.hjmmd_8.createoreexpansion.compat.jei.category.DismantlingCategory;
import com.hjmmd_8.createoreexpansion.compat.jei.category.GrindingCategory;
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
import com.hjmmd_8.createoreexpansion.common.CoeCore;

/**
 * 角磨配方 JEI 集成（分类仿照机械动力 CreateJEI 的动力锯分类注册方式）。
 *
 * <p><b>P7a</b>：本类整体从集成层（{@code :coe} 之外的根工程 {@code compat/jei/GrindingJEI}）
 * 搬进 {@code :coe}（JPMS 红线：同一个 Java 包不能同时属于两个 mod 文件，而三个模块都要有
 * 自己的 {@code @JeiPlugin}）。三个插件里只有本类与 {@code CreateOreExpansionJEI} 是 COE 的，
 * P12 还原包名时由它们继承老包名 {@code compat.jei}；{@code :cews} 的 ChargingJEI 留在
 * {@code compat.jei.cews}，{@code :transmutation} 的插件留在 {@code compat.jei.transmutation}。
 * 插件 UID {@code createoreexpansion:grinding_jei} <b>一字未改</b>。</p>
 *
 * <p><b>W6-c 更正</b>：充能器随波引擎回到 {@code :coe}，所以 {@code ChargingJEI} 也回来了 ——
 * 它现在与本类<b>同住 {@code compat.jei}</b>（旧的 {@code compat.jei.cews} 那一份不再存在），
 * 上面那句"留在 {@code compat.jei.cews}"因此过时。三个 COE 插件
 * （{@code core_jei} / {@code grinding_jei} / {@code charging_jei}）+ 嬗化的
 * {@code transmutation_jei} 共用一个包。</p>
 */
@JeiPlugin
public class GrindingJEI implements IModPlugin {

	private static final ResourceLocation ID = CoeCore.modLoc("grinding_jei");

	private final List<CreateRecipeCategory<?>> allCategories = new ArrayList<>();

	@Override
	public ResourceLocation getPluginUid() {
		return ID;
	}

	private void loadCategories() {
		allCategories.clear();

		CreateRecipeCategory<?> grinding = builder(GrindingRecipe.class)
			.addTypedRecipes(CoeRecipeTypes.GRINDING)
			.catalyst(CoeMachines.POWER_ANGLE_GRINDER::get)
			.doubleItemIcon(CoeMachines.POWER_ANGLE_GRINDER.get(), Items.IRON_INGOT)
			.emptyBackground(177, 70)
			.build(CoeCore.modLoc("grinding"), GrindingCategory::new);

		CreateRecipeCategory<?> advanced = builder(AbstractCrushingRecipe.class)
			.addTypedRecipes(com.simibubi.create.AllRecipeTypes.CRUSHING)
			.addTypedRecipes(com.simibubi.create.AllRecipeTypes.MILLING)
			.catalyst(CoeMachines.POWER_ANGLE_GRINDER::get)
			.doubleItemIcon(CoeMachines.POWER_ANGLE_GRINDER.get(), Items.DIAMOND)
			.emptyBackground(177, 70)
			.build(CoeCore.modLoc("advanced_grinding"), AdvancedGrindingCategory::new);

		CreateRecipeCategory<?> dismantling = builder(DismantlingRecipe.class)
			.addTypedRecipes(CoeRecipeTypes.DISMANTLING)
			.catalyst(CoeMachines.POWER_ANGLE_GRINDER::get)
			.doubleItemIcon(CoeMachines.POWER_ANGLE_GRINDER.get(), CoeItems.SAPPHIRE_INGOT.get())
			.emptyBackground(177, 70)
			.build(CoeCore.modLoc("dismantling"), DismantlingCategory::new);

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
