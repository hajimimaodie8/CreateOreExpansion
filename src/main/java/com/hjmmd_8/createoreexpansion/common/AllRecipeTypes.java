package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.transmuting.AllTransmutingRecipe;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.createmod.catnip.lang.Lang;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public enum AllRecipeTypes implements IRecipeTypeInfo, StringRepresentable {

	TRANSMUTING(AllTransmutingRecipe::new);

	public static final Predicate<RecipeHolder<?>> CAN_BE_AUTOMATED = r -> !r.id()
		.getPath()
		.endsWith("_manual_only");

	public final ResourceLocation id;
	public final Supplier<RecipeSerializer<?>> serializerSupplier;
	private final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> serializerObject;
	@Nullable
	private final DeferredHolder<RecipeType<?>, RecipeType<?>> typeObject;
	private final Supplier<RecipeType<?>> type;

	private boolean isProcessingRecipe;

	AllRecipeTypes(StandardProcessingRecipe.Factory<?> processingFactory) {
		this(() -> new StandardProcessingRecipe.Serializer<>(processingFactory));
		isProcessingRecipe = true;
	}

	AllRecipeTypes(Supplier<RecipeSerializer<?>> serializerSupplier) {
		String name = Lang.asId(name());
		id = ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, name);
		this.serializerSupplier = serializerSupplier;
		serializerObject = Registers.SERIALIZER_REGISTER.register(name, serializerSupplier);
		typeObject = Registers.TYPE_REGISTER.register(name, () -> RecipeType.simple(id));
		type = typeObject;
		isProcessingRecipe = false;
	}

	public static void register(IEventBus modEventBus) {
		Registers.SERIALIZER_REGISTER.register(modEventBus);
		Registers.TYPE_REGISTER.register(modEventBus);
	}

	@Override
	public ResourceLocation getId() {
		return id;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends RecipeSerializer<?>> T getSerializer() {
		return (T) serializerObject.get();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
		return (RecipeType<R>) type.get();
	}

	public <I extends RecipeInput, R extends Recipe<I>> Optional<RecipeHolder<R>> find(I inv, Level world) {
		return world.getRecipeManager()
			.getRecipeFor(getType(), inv, world);
	}

	@Override
	public @NotNull String getSerializedName() {
		return id.toString();
	}

	private static class Registers {
		private static final DeferredRegister<RecipeSerializer<?>> SERIALIZER_REGISTER =
			DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, CreateOreExpansion.MOD_ID);
		private static final DeferredRegister<RecipeType<?>> TYPE_REGISTER =
			DeferredRegister.create(Registries.RECIPE_TYPE, CreateOreExpansion.MOD_ID);
	}

}
