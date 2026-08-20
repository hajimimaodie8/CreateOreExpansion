package com.hjmmd_8.createoreexpansion.content.grinding.recipe;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.hjmmd_8.createoreexpansion.common.AllBlocks;
import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.hjmmd_8.createoreexpansion.compat.jei.GrindingAssemblySubCategory;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 角磨配方：动力角磨床加工（物品在角磨轮上缓慢移动并处理）。
 * 配方 JSON 形态：{@code createoreexpansion:grinding/xxx.json}。
 *
 * <p>实现 {@link IAssemblyRecipe}：角磨步骤可加入序列加工配方
 * （Create 6.0 序列步骤 = 任意实现 IAssemblyRecipe 的 ProcessingRecipe）。</p>
 */
public class GrindingRecipe extends StandardProcessingRecipe<SingleRecipeInput> implements IAssemblyRecipe {

	public GrindingRecipe(ProcessingRecipeParams params) {
		super(AllRecipeTypes.GRINDING, params);
	}

	@Override
	public boolean matches(SingleRecipeInput inv, Level level) {
		if (inv.isEmpty())
			return false;
		return ingredients.get(0)
			.test(inv.getItem(0));
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 4;
	}

	// ========== 序列加工（IAssemblyRecipe） ==========

	@Override
	public void addAssemblyIngredients(List<Ingredient> list) {}

	@Override
	@OnlyIn(Dist.CLIENT)
	public Component getDescriptionForAssembly() {
		return Component.translatable("createoreexpansion.recipe.assembly.grinding");
	}

	@Override
	public void addRequiredMachines(Set<ItemLike> list) {
		list.add(AllBlocks.POWER_ANGLE_GRINDER.get());
	}

	@Override
	public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
		return () -> GrindingAssemblySubCategory::new;
	}
}
