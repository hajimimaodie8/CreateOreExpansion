package com.hjmmd_8.createoreexpansion.content.grinding.recipe;

import com.hjmmd_8.createoreexpansion.common.AllRecipeTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * 拆磨配方（三级角磨轮专属）：把装备/武器/马鞍拆解为构成材料。
 *
 * <p>配方定义三个数值：{@code item}（可拆物品）、{@code result}（拆后材料）、
 * {@code materialCount}（权重系数 = 合成该物品消耗的材料数，如头盔 5、胸甲 8）。
 * 输出数量 = {@code floor(materialCount × 剩余耐久比例)}：钻石剑（2 钻石）剩 3/4 耐久
 * → 输出 1 钻石；钻石头盔（5 钻石）剩 80% 耐久 → 输出 4 钻石。</p>
 */
public class DismantlingRecipe implements Recipe<SingleRecipeInput> {

	private final ItemStack item;
	private final ItemStack result;
	private final int materialCount;
	private final NonNullList<Ingredient> ingredients;

	public DismantlingRecipe(ItemStack item, ItemStack result, int materialCount) {
		this.item = item;
		this.result = result;
		this.materialCount = materialCount;
		this.ingredients = NonNullList.withSize(1, Ingredient.of(item));
	}

	/** 按输入装备的耐久比例计算拆解输出；耐久耗尽或比例不足 1 份材料时返回空 */
	public ItemStack getResult(ItemStack input) {
		float durability = 1;
		if (input.isDamageableItem() && input.getMaxDamage() > 0) {
			durability = 1 - input.getDamageValue() / (float) input.getMaxDamage();
		}
		int count = (int) Math.floor(materialCount * durability);
		if (count <= 0)
			return ItemStack.EMPTY;
		return result.copyWithCount(count);
	}

	@Override
	public boolean matches(SingleRecipeInput input, Level level) {
		return input.getItem(0)
			.is(item.getItem());
	}

	@Override
	public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
		return getResult(input.getItem(0));
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return result.copyWithCount(materialCount);
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return ingredients;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return AllRecipeTypes.DISMANTLING.getSerializer();
	}

	@Override
	public RecipeType<?> getType() {
		return AllRecipeTypes.DISMANTLING.getType();
	}

	public ItemStack getItem() {
		return item;
	}

	public ItemStack getResult() {
		return result;
	}

	public int getMaterialCount() {
		return materialCount;
	}

	public static class Serializer implements RecipeSerializer<DismantlingRecipe> {

		public static final MapCodec<DismantlingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
			ItemStack.OPTIONAL_CODEC.fieldOf("item").forGetter(DismantlingRecipe::getItem),
			ItemStack.OPTIONAL_CODEC.fieldOf("result").forGetter(DismantlingRecipe::getResult),
			Codec.INT.fieldOf("materialCount").forGetter(DismantlingRecipe::getMaterialCount)
		).apply(inst, DismantlingRecipe::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, DismantlingRecipe> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC, DismantlingRecipe::getItem,
			ItemStack.STREAM_CODEC, DismantlingRecipe::getResult,
			ByteBufCodecs.INT, DismantlingRecipe::getMaterialCount,
			DismantlingRecipe::new
		);

		@Override
		public MapCodec<DismantlingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, DismantlingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
