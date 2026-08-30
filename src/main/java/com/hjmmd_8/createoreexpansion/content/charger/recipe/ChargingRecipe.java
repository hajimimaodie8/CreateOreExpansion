package com.hjmmd_8.createoreexpansion.content.charger.recipe;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.hjmmd_8.createoreexpansion.common.AllBlocks;
import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.hjmmd_8.createoreexpansion.compat.jei.subcategory.ChargingAssemblySubCategory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 充能加工配方：翡翠应力充能器的能量波击中物品时按等级匹配配方。
 *
 * <p>配方只用一个类型 {@code createoreexpansion:charging}，等级是配方自带字段
 * {@code level}（1=低、2=高、3=伽马）：</p>
 * <pre>{@code
 * {
 *   "type": "createoreexpansion:charging",
 *   "level": 2,
 *   "ingredients": [ ... ],
 *   "results": [ ... ]
 * }
 * }</pre>
 *
 * <p>机器按当前充能态（转速档位）匹配：能量波等级 ≥ 配方要求的等级即可加工。
 * 因此第三方/整合包只需在数据包里写带 {@code level} 字段的配方 JSON 即可适配
 * 本模组的充能加工，无需编写任何 Java 代码。</p>
 *
 * <p>实现 {@link IAssemblyRecipe}：充能步骤可加入序列加工配方。</p>
 */
public class ChargingRecipe extends StandardProcessingRecipe<SingleRecipeInput> implements IAssemblyRecipe {

	/** 配方要求的充能等级：1=低、2=高、3=伽马（默认 1） */
	private final int level;

	public ChargingRecipe(ProcessingRecipeParams params) {
		this(params, 1);
	}

	public ChargingRecipe(ProcessingRecipeParams params, int level) {
		super(AllRecipeTypes.CHARGING, params);
		this.level = Math.max(1, level);
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

	/** 配方要求的充能等级（1=低、2=高、3=伽马） */
	public int getLevel() {
		return level;
	}

	/** 按充能等级取充能点数：1=低 100，2=高 500，3=伽马 1000 */
	public static int energyForLevel(int level) {
		return switch (level) {
			case 2 -> 500;
			case 3 -> 1000;
			default -> 100;
		};
	}

	// ========== 序列化：ProcessingRecipeParams + level 字段 ==========

	public static class Serializer implements RecipeSerializer<ChargingRecipe> {

		public static final MapCodec<ChargingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ProcessingRecipeParams.CODEC.forGetter(ChargingRecipe::getParams),
			Codec.INT.optionalFieldOf("level", 1).forGetter(ChargingRecipe::getLevel)
		).apply(instance, ChargingRecipe::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, ChargingRecipe> STREAM_CODEC = StreamCodec.of(
			(buffer, recipe) -> {
				ProcessingRecipeParams.STREAM_CODEC.encode(buffer, recipe.getParams());
				ByteBufCodecs.INT.encode(buffer, recipe.getLevel());
			},
			buffer -> new ChargingRecipe(
				ProcessingRecipeParams.STREAM_CODEC.decode(buffer),
				ByteBufCodecs.INT.decode(buffer)));

		@Override
		public MapCodec<ChargingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, ChargingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}

	// ========== 序列加工（IAssemblyRecipe） ==========

	@Override
	public void addAssemblyIngredients(List<Ingredient> list) {}

	@Override
	@OnlyIn(Dist.CLIENT)
	public Component getDescriptionForAssembly() {
		// 悬停序列充能步骤时显示：在翡翠应力充能器进行「低/高/伽马能量充能」
		Component level = switch (getLevel()) {
			case 2 -> Component.translatable("createoreexpansion.jei.charging.level.2");
			case 3 -> Component.translatable("createoreexpansion.jei.charging.level.3");
			default -> Component.translatable("createoreexpansion.jei.charging.level.1");
		};
		return Component.translatable("createoreexpansion.recipe.assembly.charging_hover", level);
	}

	@Override
	public void addRequiredMachines(Set<ItemLike> list) {
		list.add(AllBlocks.JADE_CREATE_CHARGER.get());
	}

	@Override
	public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
		return () -> ChargingAssemblySubCategory::new;
	}
}
