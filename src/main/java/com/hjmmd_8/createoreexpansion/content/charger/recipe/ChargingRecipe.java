package com.hjmmd_8.createoreexpansion.content.charger.recipe;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import com.hjmmd_8.createoreexpansion.common.AllBlocks;
import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.hjmmd_8.createoreexpansion.compat.jei.subcategory.ChargingAssemblySubCategory;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

/**
 * 充能加工配方：翡翠/蓝宝石应力充能器的能量波击中物品时按等级匹配配方。
 *
 * <p>配方只用一个类型 {@code createoreexpansion:charging}，等级是配方自带字段
 * {@code level}（1=低 … 5=欧米伽）：</p>
 * <pre>{@code
 * {
 *   "type": "createoreexpansion:charging",
 *   "level": 2,
 *   "ingredients": [ ... ],
 *   "results": [ ... ]
 * }
 * }</pre>
 *
 * <p>实现照 Create 6 标准（参考 LaserCutting 等）：<b>继承
 * {@link ProcessingRecipe}&lt;{@link RecipeWrapper}, {@link ChargingRecipeParams}&gt;</b>，
 * 额外字段收在 Params 里，Serializer 用 {@link ProcessingRecipe#codec}
 * 组合 Params.CODEC，不再手写配方级编解码。配方 JSON 带 {@code level} 字段，
 * 第三方数据包即可适配本模组充能加工，无需写 Java。</p>
 */
@ParametersAreNonnullByDefault
public class ChargingRecipe extends ProcessingRecipe<RecipeWrapper, ChargingRecipeParams>
	implements IAssemblyRecipe {

	public ChargingRecipe(ChargingRecipeParams params) {
		super(AllRecipeTypes.CHARGING, params);
	}

	/** 配方要求的充能等级（1=低 … 5=欧米伽）。 */
	public int getLevel() {
		return params.level();
	}

	@Override
	public boolean matches(RecipeWrapper inv, Level worldIn) {
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

	/** 按充能等级取充能点数：1=低 100，2=高 500，3=伽马 1000，4=伊普西龙 1500，5=欧米伽 2000 */
	public static int energyForLevel(int level) {
		return switch (level) {
			case 2 -> 500;
			case 3 -> 1000;
			case 4 -> 1500;
			case 5 -> 2000;
			default -> 100;
		};
	}

	@FunctionalInterface
	public interface Factory<R extends ChargingRecipe> extends ProcessingRecipe.Factory<ChargingRecipeParams, R> {
		R create(ChargingRecipeParams params);
	}

	/** 序列化：ProcessingRecipe.codec 组合 Params.CODEC/STREAM_CODEC（Create 6 标准，勿手写）。 */
	public static class Serializer<R extends ChargingRecipe> implements RecipeSerializer<R> {
		private final MapCodec<R> codec;
		private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

		public Serializer(Factory<R> factory) {
			this.codec = ProcessingRecipe.codec(factory, ChargingRecipeParams.CODEC);
			this.streamCodec = ProcessingRecipe.streamCodec(factory, ChargingRecipeParams.STREAM_CODEC);
		}

		@Override
		public MapCodec<R> codec() {
			return codec;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
			return streamCodec;
		}
	}

	/** datagen 构建器：ProcessingRecipeBuilder + ChargingRecipeParams 的薄封装（Create 6 标准）。 */
	public static class Builder extends ProcessingRecipeBuilder<ChargingRecipeParams, ChargingRecipe, Builder> {
		public Builder(net.minecraft.resources.ResourceLocation id) {
			super(ChargingRecipe::new, id);
		}

		@Override
		protected ChargingRecipeParams createParams() {
			return new ChargingRecipeParams();
		}

		@Override
		public Builder self() {
			return this;
		}

		/** 设置配方充能等级（1=低 … 5=欧米伽）。 */
		public Builder withLevel(int level) {
			params.level = Math.max(1, level);
			return this;
		}
	}

	// ========== 序列加工（IAssemblyRecipe） ==========

	@Override
	public void addAssemblyIngredients(List<Ingredient> list) {}

	@Override
	@OnlyIn(Dist.CLIENT)
	public Component getDescriptionForAssembly() {
		// 悬停序列充能步骤时显示：在对应应力充能器进行「低/高/伽马/超载/终极能量充能」。
		// 机型按等级分流：1~3 级（低/高/伽马）= 翡翠充能器；4/5 级（超载/终极）= 蓝宝石充能器
		Component level = switch (getLevel()) {
			case 2 -> Component.translatable("createoreexpansion.jei.charging.level.2");
			case 3 -> Component.translatable("createoreexpansion.jei.charging.level.3");
			case 4 -> Component.translatable("createoreexpansion.jei.charging.level.4");
			case 5 -> Component.translatable("createoreexpansion.jei.charging.level.5");
			default -> Component.translatable("createoreexpansion.jei.charging.level.1");
		};
		boolean sapphire = getLevel() > com.hjmmd_8.createoreexpansion.content.wave.WaveLevels.JADE_MAX;
		return Component.translatable(sapphire
			? "createoreexpansion.recipe.assembly.charging_hover_sapphire"
			: "createoreexpansion.recipe.assembly.charging_hover", level);
	}

	@Override
	public void addRequiredMachines(Set<ItemLike> list) {
		// 按配方等级分流所需机器（JEI/装配显示）：
		// 1~3 级（低/高/伽马）由翡翠充能器产出；4/5 级（超载/终极）只有蓝宝石充能器能产出
		if (getLevel() <= com.hjmmd_8.createoreexpansion.content.wave.WaveLevels.JADE_MAX)
			list.add(AllBlocks.JADE_STRESS_CHARGER.get());
		else
			list.add(AllBlocks.SAPPHIRE_STRESS_CHARGER.get());
	}

	@Override
	public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
		return () -> ChargingAssemblySubCategory::new;
	}
}
