package com.hjmmd_8.createoreexpansion.common;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.content.transmuting.AllTransmutingRecipe;
import com.hjmmd_8.createoreexpansion.content.lightning.LightningRecipe;
import com.hjmmd_8.createoreexpansion.content.lightning.LightningBlockRecipe;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.DismantlingRecipe;
import com.hjmmd_8.createoreexpansion.content.grinding.recipe.GrindingRecipe;
import com.hjmmd_8.createoreexpansion.content.charger.recipe.ChargingRecipe;
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
import net.minecraft.world.item.ItemStack;
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

	TRANSMUTING(AllTransmutingRecipe::new),
	LIGHTNING(LightningRecipe::new),
	LIGHTNING_BLOCK(LightningBlockRecipe::new),
	GRINDING(GrindingRecipe::new),
	DISMANTLING(() -> new DismantlingRecipe.Serializer()),
	CHARGING(() -> new ChargingRecipe.Serializer<>(ChargingRecipe::new));

	public static final Predicate<RecipeHolder<?>> CAN_BE_AUTOMATED = r -> !r.id()
		.getPath()
		.endsWith("_manual_only");

	/**
	 * 该配方是否应被"自动化加工"忽略（波的全库候选池按此过滤，见设计文档 §1.1 第 5 条）。
	 *
	 * <p>判据有两半（2026-09 审计修复）：</p>
	 * <ol>
	 *   <li>Create 的 <b>serializer tag 分支</b>：{@code AllTags.AllRecipeSerializerTags.AUTOMATION_IGNORE}
	 *       （原版 Create 只标了 occultism 那两条）——旧实现漏了这一半，导致带该标签的配方仍会进波的全库池；</li>
	 *   <li>本模组自有约定：配方 id 以 {@code _manual_only} 结尾 = 只能手动。</li>
	 * </ol>
	 *
	 * <p>Create 侧读取失败（版本差异等）时退化为本模组约定，不影响主流程。</p>
	 */
	public static boolean shouldIgnoreInAutomation(RecipeHolder<?> recipe) {
		if (!CAN_BE_AUTOMATED.test(recipe))
			return true;
		try {
			return com.simibubi.create.AllRecipeTypes.shouldIgnoreInAutomation(recipe);
		} catch (Throwable ignored) {
			return false;
		}
	}

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

	/** 单物品 → RecipeWrapper（配方匹配统一走机械动力的 RecipeWrapper）。 */
	public static net.neoforged.neoforge.items.wrapper.RecipeWrapper wrap(ItemStack stack) {
		net.neoforged.neoforge.items.ItemStackHandler handler = new net.neoforged.neoforge.items.ItemStackHandler(1);
		handler.setStackInSlot(0, stack);
		return new net.neoforged.neoforge.items.wrapper.RecipeWrapper(handler);
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
