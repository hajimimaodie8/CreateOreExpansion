package com.hjmmd_8.createoreexpansion.compat.jei.category;

import static mezz.jei.api.recipe.RecipeType.createRecipeHolderType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.NotNull;

import com.hjmmd_8.createoreexpansion.CreateOreExpansion;
import com.hjmmd_8.createoreexpansion.compat.jei.CreateOreExpansionJEI;
import com.simibubi.create.compat.jei.DoubleItemIcon;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.compat.jei.ItemIcon;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.simibubi.create.foundation.utility.CreateLang;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.ItemLike;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CreateRecipeCategory<T extends Recipe<?>> implements IRecipeCategory<RecipeHolder<T>> {
	private static final IDrawable BASIC_SLOT = asDrawable(AllGuiTextures.JEI_SLOT);
	private static final IDrawable CHANCE_SLOT = asDrawable(AllGuiTextures.JEI_CHANCE_SLOT);

	protected final RecipeType<RecipeHolder<T>> type;
	protected final Component title;
	protected final IDrawable background;
	protected final IDrawable icon;

	private final Supplier<List<RecipeHolder<T>>> recipes;
	private final List<Supplier<? extends ItemStack>> catalysts;

	public CreateRecipeCategory(Info<T> info) {
		this.type = info.recipeType();
		this.title = info.title();
		this.background = info.background();
		this.icon = info.icon();
		this.recipes = info.recipes();
		this.catalysts = info.catalysts();
	}

	@NotNull
	@Override
	public RecipeType<RecipeHolder<T>> getRecipeType() {
		return type;
	}

	@Override
	public Component getTitle() {
		return title;
	}

	@Override
	@SuppressWarnings("removal")
	public IDrawable getBackground() {
		return background;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<T> holder, IFocusGroup focuses) {
		setRecipe(builder, holder.value(), focuses);
	}

	@Override
	public void draw(RecipeHolder<T> holder, IRecipeSlotsView recipeSlotsView, GuiGraphics gui, double mouseX, double mouseY) {
		draw(holder.value(), recipeSlotsView, gui, mouseX, mouseY);
	}

	protected abstract void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses);

	protected abstract void draw(T recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics gui, double mouseX, double mouseY);

	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(type, recipes.get());
	}

	public void registerCatalysts(IRecipeCatalystRegistration registration) {
		catalysts.forEach(s -> registration.addRecipeCatalyst(s.get(), type));
	}

	public static IDrawable getRenderedSlot() {
		return BASIC_SLOT;
	}

	public static IDrawable getRenderedSlot(ProcessingOutput output) {
		return getRenderedSlot(output.getChance());
	}

	public static IDrawable getRenderedSlot(float chance) {
		if (chance == 1)
			return BASIC_SLOT;
		return CHANCE_SLOT;
	}

	public static ItemStack getResultItem(Recipe<?> recipe) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null)
			return ItemStack.EMPTY;
		return recipe.getResultItem(level.registryAccess());
	}

	public static IRecipeSlotRichTooltipCallback addStochasticTooltip(ProcessingOutput output) {
		return (view, tooltip) -> {
			float chance = output.getChance();
			if (chance != 1)
				tooltip.add(CreateLang.translateDirect("recipe.processing.chance", chance < 0.01 ? "<1" : (int) (chance * 100))
					.withStyle(ChatFormatting.GOLD));
		};
	}

	protected static IDrawable asDrawable(AllGuiTextures texture) {
		return new IDrawable() {
			@Override
			public int getWidth() {
				return texture.getWidth();
			}

			@Override
			public int getHeight() {
				return texture.getHeight();
			}

			@Override
			public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
				texture.render(graphics, xOffset, yOffset);
			}
		};
	}

	public record Info<T extends Recipe<?>>(RecipeType<RecipeHolder<T>> recipeType, Component title, IDrawable background,
											IDrawable icon, Supplier<List<RecipeHolder<T>>> recipes,
											List<Supplier<? extends ItemStack>> catalysts) {
	}

	public interface Factory<T extends Recipe<?>> {
		CreateRecipeCategory<T> create(Info<T> info);
	}

	public static class Builder<T extends Recipe<? extends RecipeInput>> {
		private final Class<? extends T> recipeClass;
		private final List<Consumer<List<RecipeHolder<T>>>> recipeListConsumers = new ArrayList<>();
		private final List<Supplier<? extends ItemStack>> catalysts = new ArrayList<>();

		private IDrawable background;
		private IDrawable icon;

		public Builder(Class<? extends T> recipeClass) {
			this.recipeClass = recipeClass;
		}

		public Builder<T> addRecipeListConsumer(Consumer<List<RecipeHolder<T>>> consumer) {
			recipeListConsumers.add(consumer);
			return this;
		}

		@SuppressWarnings("unchecked")
		public Builder<T> addTypedRecipes(IRecipeTypeInfo recipeTypeEntry) {
			return addRecipeListConsumer(recipes -> CreateOreExpansionJEI.<T>consumeTypedRecipes(recipe -> {
				if (recipeClass.isInstance(recipe.value()))
					//noinspection unchecked - checked by if statement above
					recipes.add((RecipeHolder<T>) recipe);
			}, recipeTypeEntry.getType()));
		}

		public Builder<T> catalystStack(Supplier<ItemStack> supplier) {
			catalysts.add(supplier);
			return this;
		}

		public Builder<T> catalyst(Supplier<ItemLike> supplier) {
			return catalystStack(() -> new ItemStack(supplier.get().asItem()));
		}

		public Builder<T> icon(IDrawable icon) {
			this.icon = icon;
			return this;
		}

		public Builder<T> itemIcon(ItemLike item) {
			icon(new ItemIcon(() -> new ItemStack(item)));
			return this;
		}

		public Builder<T> doubleItemIcon(ItemLike item1, ItemLike item2) {
			icon(new DoubleItemIcon(() -> new ItemStack(item1), () -> new ItemStack(item2)));
			return this;
		}

		public Builder<T> background(IDrawable background) {
			this.background = background;
			return this;
		}

		public Builder<T> emptyBackground(int width, int height) {
			background(new EmptyBackground(width, height));
			return this;
		}

		public CreateRecipeCategory<T> build(String name, Factory<T> factory) {
			return build(ResourceLocation.fromNamespaceAndPath(CreateOreExpansion.MOD_ID, name), factory);
		}

		public CreateRecipeCategory<T> build(ResourceLocation id, Factory<T> factory) {
			Supplier<List<RecipeHolder<T>>> recipesSupplier = () -> {
				List<RecipeHolder<T>> recipes = new ArrayList<>();
				for (Consumer<List<RecipeHolder<T>>> consumer : recipeListConsumers)
					consumer.accept(recipes);
				return recipes;
			};

			Info<T> info = new Info<>(
				createRecipeHolderType(id),
				Component.translatable(id.getNamespace() + ".recipe." + id.getPath()),
				background,
				icon,
				recipesSupplier,
				catalysts
			);
			return factory.create(info);
		}
	}
}
