package com.hjmmd_8.createoreexpansion.mixin;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.hjmmd_8.createoreexpansion.compat.jei.subcategory.TeslaCoilAssemblySubCategory;
import com.mrh0.createaddition.index.CABlocks;
import com.mrh0.createaddition.recipe.charging.ChargingRecipe;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/**
 * 让 CC&A 的充电配方（特斯拉线圈）可以作为序列加工步骤使用。
 *
 * <p>Create 的序列装配分类（JEI）对每个步骤调用
 * {@code SequencedRecipe.getAsAssemblyRecipe()}，内部直接
 * {@code checkcast IAssemblyRecipe} —— CC&A 的 ChargingRecipe 只继承
 * ProcessingRecipe、未实现该接口，一旦放进 sequence 就抛 ClassCastException，
 * 整个序列装配 JEI 分类崩溃消失。</p>
 *
 * <p>本 Mixin 给 CC&A ChargingRecipe 注入 IAssemblyRecipe 的全部抽象方法
 * （supportsAssembly 用接口默认值 true）：</p>
 * <ul>
 *     <li>{@link #getDescriptionForAssembly} —— 序列步骤悬停文字（特斯拉线圈充电）；</li>
 *     <li>{@link #addRequiredMachines} —— 标记需要特斯拉线圈；</li>
 *     <li>{@link #addAssemblyIngredients} —— 无额外原料（物品自身被充能）；</li>
 *     <li>{@link #getJEISubCategory} —— 序列分类中绘制特斯拉线圈动画。</li>
 * </ul>
 */
@Mixin(ChargingRecipe.class)
public abstract class ChargingRecipeAssemblyMixin implements IAssemblyRecipe {

	@Unique
	private static final Supplier<Supplier<SequencedAssemblySubCategory>> TESLA_SUBCATEGORY =
		() -> TeslaCoilAssemblySubCategory::new;

	@Override
	public Component getDescriptionForAssembly() {
		return Component.translatable("createoreexpansion.recipe.assembly.cca_charging");
	}

	@Override
	public void addRequiredMachines(Set<ItemLike> list) {
		list.add(CABlocks.TESLA_COIL.get());
	}

	@Override
	public void addAssemblyIngredients(List<Ingredient> list) {}

	@Override
	public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
		return TESLA_SUBCATEGORY;
	}
}
